/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.osd.stages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.Request;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.osd.ConcurrentFileMap;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.common.ClientLease;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;
import org.xtreemfs.osd.OpenFileTable;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDetails;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.ops.Operation;
import org.xtreemfs.osd.storage.CowPolicy;

public class AuthenticationStage extends Stage {
    
    public final static int                STAGEOP_AUTHENTICATE    = 1 << 0;
    
    public final static int                STAGEOP_OFT_OPEN        = 1 << 1;
    
    public final static int                STAGEOP_OFT_DELETE      = 1 << 2;
    
    public final static int                STAGEOP_ACQUIRE_LEASE   = 1 << 4;
    
    public final static int                STAGEOP_RETURN_LEASE    = 1 << 5;
    
    public final static int                STAGEOP_VERIFIY_CLEANUP = 1 << 6;
    
    private final static long              OFT_CLEAN_INTERVAL      = 1000 * 60;
    
    private final static long              OFT_OPEN_EXTENSION      = 1000 * 30;
    
    private final Map<String, Set<String>> capCache;
    
    private final OpenFileTable            oft;
    
    // time left to next clean op
    private long                           timeToNextOFTclean;
    
    // last check of the OFT
    private long                           lastOFTcheck;
    
    private final Operation                closeOp;
    
    /** Creates a new instance of AuthenticationStage */
    public AuthenticationStage(RequestDispatcher master) {
        
        super("OSD Auth Stage");
        
        capCache = new HashMap<String, Set<String>>();
        oft = new OpenFileTable();
        closeOp = master.getOperation(RequestDispatcher.Operations.CLOSE_FILE);
    }
    
    @Override
    public void run() {
        
        notifyStarted();
        
        // interval to check the OFT
        
        timeToNextOFTclean = OFT_CLEAN_INTERVAL;
        lastOFTcheck = TimeSync.getLocalSystemTime();
        
        while (!quit) {
            Request rq = null;
            try {
                final StageMethod op = q.poll(timeToNextOFTclean, TimeUnit.MILLISECONDS);
                
                checkOpenFileTable();
                
                if (op == null) {
                    // Logging.logMessage(Logging.LEVEL_DEBUG,this,"no request
                    // -- timer only");
                    continue;
                }
                
                rq = op.getRq();
                
                if (Logging.tracingEnabled())
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "processing request #"
                        + rq.getRequestId());
                
                processMethod(op);
                
            } catch (InterruptedException ex) {
                break;
            } catch (Exception ex) {
                if (rq != null)
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "exception occurred while processing:" + rq);
                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                notifyCrashed(ex);
                break;
            }
        }
        
        notifyStopped();
    }
    
    private void checkOpenFileTable() {
        final long tPassed = TimeSync.getLocalSystemTime() - lastOFTcheck;
        timeToNextOFTclean = timeToNextOFTclean - tPassed;
        // Logging.logMessage(Logging.LEVEL_DEBUG,this,"time to next OFT:
        // "+timeToNextOFTclean);
        if (timeToNextOFTclean <= 0) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "OpenFileTable clean");
            // do OFT clean
            List<OpenFileTable.OpenFileTableEntry> closedFiles = oft.clean(TimeSync
                    .getLocalSystemTime());
            // Logging.logMessage(Logging.LEVEL_DEBUG,this,"closing
            // "+closedFiles.size()+" files");
            for (OpenFileTable.OpenFileTableEntry entry : closedFiles) {
                
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "send internal close event for "
                    + entry.getFileId() + ", deleteOnClose=" + entry.isDeleteOnClose());
                capCache.remove(entry.getFileId());
                OSDRequest closeEvent = new OSDRequest(0);
                closeEvent.getDetails().setFileId(entry.getFileId());
                closeEvent.setOperation(closeOp);
                closeEvent.setAttachment(Boolean.valueOf(entry.isDeleteOnClose()));
                closeOp.startRequest(closeEvent);
            }
            timeToNextOFTclean = OFT_CLEAN_INTERVAL;
        }
        lastOFTcheck = TimeSync.getLocalSystemTime();
    }
    
    protected void processMethod(StageMethod m) {
        
        final int requestedMethod = m.getStageMethod();
        
        if ((requestedMethod & STAGEOP_AUTHENTICATE) != 0) {
            //for quicker responses
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"STAGEOP AUTH");
            final boolean ok = processAuthenticate(m);
            if (!ok)
                return;
        }
        
        if ((requestedMethod & STAGEOP_OFT_OPEN) != 0) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"STAGEOP OPEN");
            final String fId = m.getRq().getDetails().getFileId();
            CowPolicy cowPolicy;
            if (oft.contains(fId)) {
                cowPolicy = oft.refresh(fId, TimeSync.getLocalSystemTime()
                    + OFT_OPEN_EXTENSION);
            } else {
                //find out which COW mode to use
                //currently everything is no COW
                oft.openFile(fId, TimeSync.getLocalSystemTime()
                    + OFT_OPEN_EXTENSION,CowPolicy.PolicyNoCow);
                cowPolicy = CowPolicy.PolicyNoCow;
            }
            m.getRq().getDetails().setCowPolicy(cowPolicy);
        }
        
        if ((requestedMethod & STAGEOP_OFT_DELETE) != 0) {
            processOFTDelete(m);
        }
        
        if (requestedMethod == STAGEOP_ACQUIRE_LEASE) {
            processAcquireLease(m);
        }
        
        if (requestedMethod == STAGEOP_RETURN_LEASE) {
            processReturnLease(m);
        }
        
        if (requestedMethod == STAGEOP_VERIFIY_CLEANUP) {
            processVerifyCleanup(m);
        }
        
        this.methodExecutionSuccess(m, Stage.StageResponseCode.OK);
    }
    
    /**
     * Checks a list of fileIDs in the attachment of the request against the
     * open-file-table.
     * 
     * @param m
     * @throws JSONException
     */
    private void processVerifyCleanup(StageMethod m) {
        // check against the o-f-t
        ConcurrentFileMap fileList = (ConcurrentFileMap) m.getRq().getAttachment();
        for (String volId : fileList.resolvedVolumeIDSet()) {         
            for (String fileId : fileList.getFileIDSet(volId)){
                if (oft.isDeleteOnClose(fileId))
                    fileList.remove(volId,fileId);
            }
        }
        
        Logging.logMessage(Logging.LEVEL_TRACE, this, "CleanUp: all done sending back to client!");
        
        // return the files to the application
        try {
            if (!fileList.isEmpty()){
                m.getRq().setData(
                        ReusableBuffer.wrap(JSONParser.writeJSON(fileList.getJSONCompatible()).getBytes()),
                        DATA_TYPE.JSON);
                Logging.logMessage(Logging.LEVEL_INFO, this, "\nThere are '" + fileList.size()
                        + "' Zombies on this OSD.");
                methodExecutionSuccess(m, StageResponseCode.FINISH);
            }else{
                Logging.logMessage(Logging.LEVEL_INFO, this, "\nThere are no Zombies on this OSD.");
                methodExecutionSuccess(m, StageResponseCode.FINISH);
            }
        } catch (JSONException e) {
            methodExecutionSuccess(m, StageResponseCode.FAILED);
        }
    }
    
    private boolean processAuthenticate(StageMethod m) {

        final OSDRequest rq = m.getRq();
        final RequestDetails rqDetails = rq.getDetails();
        final Capability rqCap = rqDetails.getCapability();
        
        if (rqCap == null) {
            // The request does not need a capability (if so, the ParserStage
            // would have added it)
            
            // @todo FIXME server/client authorization
            
            Logging.logMessage(Logging.LEVEL_WARN, this,
                "Request without capability requirements were authenticated.");
            return true;
            // rq.tAuth = System.currentTimeMillis();
        } else {
            
            try {
                
                boolean isValid = false;
                // look in capCache
                Set<String> cachedCaps = capCache.get(rqCap.getFileId());
                if (cachedCaps != null) {
                    if (cachedCaps.contains(rqCap.getSignature())) {
                        isValid = true;
                    }
                }
                
                // TODO: check access mode (read, write, ...)

                if (!isValid) {
                    isValid = rqCap.isValid();
                    if (isValid) {
                        // add to cache
                        if (cachedCaps == null) {
                            cachedCaps = new HashSet<String>();
                            capCache.put(rqCap.getFileId(), cachedCaps);
                        }
                        cachedCaps.add(rqCap.getSignature());
                    }
                }
                
                // depending on the result the event listener is sent
                if (isValid) {
                    // rq.tAuth = System.currentTimeMillis();
                    // requestAuthenticated(rq, HTTPUtils.SC_OKAY, null);
                    return true;
                } else {
                    // rq.tAuth = System.currentTimeMillis();
                    this.methodExecutionFailed(m, new ErrorRecord(ErrorClass.USER_EXCEPTION,
                        ErrorCodes.AUTH_FAILED, "invalid capability"));
                    return false;
                }
                
            } catch (ClassCastException ex) {
                // invalid capability string
                // rq.tAuth = System.currentTimeMillis();
                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                    "X-Capability header is not valid JSON: " + ex.getMessage());
                this.methodExecutionFailed(m, new ErrorRecord(
                    ErrorRecord.ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                    HTTPHeaders.HDR_XCAPABILITY + " is not valid JSON", ex));
                return false;
            }
        }
        
    }
    
    private void processAcquireLease(StageMethod m) {
        ClientLease l = m.getRq().getDetails().getLease();
        
        if (l == null) {
            this.methodExecutionFailed(m, new ErrorRecord(ErrorClass.BAD_REQUEST,
                ErrorCodes.INVALID_RPC, "expected a lease object"));
        }
        if ((l.getFileId() == null) || (l.getFileId().length() == 0)) {
            this.methodExecutionFailed(m, new ErrorRecord(ErrorClass.BAD_REQUEST,
                ErrorCodes.INVALID_RPC, "fileId is required"));
        }
        final List<ClientLease> leases = oft.getLeases(l.getFileId());
        
        final long leaseId = l.getSequenceNo();
        
        Iterator<ClientLease> iter = leases.iterator();
        while (iter.hasNext()) {
            final ClientLease other = iter.next();
            if (other.getExpires() < TimeSync.getGlobalTime())
                iter.remove();
            if (other.getSequenceNo() == leaseId) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "known lease Id");
                // renew lease
                if (other.getClientId().equals(l.getClientId())) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "renew");
                    l = other;
                    l.setExpires(TimeSync.getGlobalTime() + ClientLease.LEASE_VALIDITY);
                    sendLease(m, l, false);
                    return;
                } else {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "not owner: "
                        + other.getClientId());
                    methodExecutionFailed(m, new ErrorRecord(ErrorClass.USER_EXCEPTION,
                        ErrorCodes.NOT_LEASE_OWNER, "only the owner can renew a lease (owner is "
                            + other.getClientId() + ")"));
                    return;
                }
            } else {
                if (l.isConflicting(other)) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "conflicting");
                    // lease cannot be granted, other client has lease
                    sendLease(m, other, true);
                    return;
                }
            }
        }
        // everything is fine, grant lease
        l.setSequenceNo(TimeSync.getGlobalTime());
        l.setExpires(TimeSync.getGlobalTime() + ClientLease.LEASE_VALIDITY);
        leases.add(l);
        sendLease(m, l, false);
        
    }
    
    private void processReturnLease(StageMethod m) {
        ClientLease l = m.getRq().getDetails().getLease();
        
        if (l == null) {
            this.methodExecutionFailed(m, new ErrorRecord(ErrorClass.BAD_REQUEST,
                ErrorCodes.INVALID_RPC, "expected a lease object"));
        }
        if ((l.getFileId() == null) || (l.getFileId().length() == 0)) {
            this.methodExecutionFailed(m, new ErrorRecord(ErrorClass.BAD_REQUEST,
                ErrorCodes.INVALID_RPC, "fileId is required"));
        }
        final List<ClientLease> leases = oft.getLeases(l.getFileId());
        
        final long leaseId = l.getSequenceNo();
        
        Iterator<ClientLease> iter = leases.iterator();
        while (iter.hasNext()) {
            final ClientLease other = iter.next();
            if (other.getExpires() < TimeSync.getGlobalTime())
                iter.remove();
            if (other.getSequenceNo() == leaseId) {
                if (other.getClientId().equals(l.getClientId())) {
                    iter.remove();
                } else {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "not owner: "
                        + other.getClientId());
                    methodExecutionFailed(m, new ErrorRecord(ErrorClass.USER_EXCEPTION,
                        ErrorCodes.NOT_LEASE_OWNER, "only the owner can renew a lease (owner is "
                            + other.getClientId() + ")"));
                    return;
                }
            }
        }
        methodExecutionSuccess(m, StageResponseCode.FINISH);
    }
    
    private void sendLease(StageMethod m, ClientLease lease, boolean failed) {
        try {
            ClientLease tmp = lease;
            if (failed) {
                tmp = lease.clone();
                tmp.setClientId(null);
                tmp.setSequenceNo(0);
            }
            List<Object> rv = new ArrayList(1);
            rv.add(tmp.encodeAsMap());
            final String result = JSONParser.writeJSON(rv);
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Lease: " + result);
            m.getRq().setData(ReusableBuffer.wrap(result.getBytes(HTTPUtils.ENC_UTF8)),
                HTTPUtils.DATA_TYPE.JSON);
            methodExecutionSuccess(m, StageResponseCode.FINISH);
        } catch (JSONException ex) {
            this.methodExecutionFailed(m, new ErrorRecord(
                ErrorRecord.ErrorClass.INTERNAL_SERVER_ERROR, "cannot encode Lease object", ex));
        }
    }
    
    private void processOFTDelete(StageMethod m) {
        
        final boolean deleteOnClose = oft.contains(m.getRq().getDetails().getFileId());
        
        // if the file is still open, mark it for a deletion on close
        if (deleteOnClose)
            oft.setDeleteOnClose(m.getRq().getDetails().getFileId());
        
        // set a flag for the Deletion Stage that indicates whether the
        // deletion has to be deferred
        ((Request) m.getRq().getAttachment()).setAttachment(deleteOnClose);

        this.methodExecutionSuccess(m, Stage.StageResponseCode.OK);
    }
    
    public int getNumOpenFiles() {
        return oft.getNumOpenFiles();
    }
}
