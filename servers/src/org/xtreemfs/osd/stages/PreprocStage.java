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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.Exceptions.ProtocolException;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.mrc.ErrNo;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.LocationsCache;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.OpenFileTable;
import org.xtreemfs.osd.operations.EventCloseFile;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.storage.CowPolicy;

public class PreprocStage extends Stage {

    public final static int STAGEOP_PARSE_AUTH_OFTOPEN = 1;

    public final static int STAGEOP_OFT_DELETE = 2;

    public final static int STAGEOP_ACQUIRE_LEASE = 3;

    public final static int STAGEOP_RETURN_LEASE = 4;

    public final static int STAGEOP_VERIFIY_CLEANUP = 5;

    private final static long OFT_CLEAN_INTERVAL = 1000 * 60;

    private final static long OFT_OPEN_EXTENSION = 1000 * 30;

    private final Map<String, Set<String>> capCache;

    private final OpenFileTable oft;

    // time left to next clean op
    private long timeToNextOFTclean;

    // last check of the OFT
    private long lastOFTcheck;

    private volatile long numRequests;

    /**
     * X-Location cache
     */
    private final LocationsCache xLocCache;

    private final OSDRequestDispatcher master;

    private final boolean ignoreCaps;

    /** Creates a new instance of AuthenticationStage */
    public PreprocStage(OSDRequestDispatcher master) {

        super("OSD PreProcSt");

        capCache = new HashMap<String, Set<String>>();
        oft = new OpenFileTable();
        xLocCache = new LocationsCache(10000);
        this.master = master;
        this.ignoreCaps = master.getConfig().isIgnoreCaps();
    }

    public void prepareRequest(OSDRequest request, ParseCompleteCallback listener) {
        this.enqueueOperation(STAGEOP_PARSE_AUTH_OFTOPEN, new Object[]{request}, null, listener);
    }

    public static interface ParseCompleteCallback {

        public void parseComplete(OSDRequest result, Exception error);
    }

    private void doPrepareRequest(StageRequest rq) {
        final OSDRequest request = (OSDRequest) rq.getArgs()[0];
        final ParseCompleteCallback callback = (ParseCompleteCallback) rq.getCallback();

        numRequests++;

        if (parseRequest(request) == false)
            return;

        if (request.getOperation().requiresCapability()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "STAGEOP AUTH");
            try {
                processAuthenticate(request);
            } catch (OSDException ex) {
                callback.parseComplete(request, ex);
                return;
            }
        }

        String fileId = request.getFileId();
        if (fileId != null) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "STAGEOP OPEN");
            CowPolicy cowPolicy;
            if (oft.contains(fileId)) {
                cowPolicy = oft.refresh(fileId, TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION);
            } else {
                //find out which COW mode to use
                //currently everything is no COW
                oft.openFile(fileId, TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION, CowPolicy.PolicyNoCow);
                cowPolicy = CowPolicy.PolicyNoCow;
            }
            request.setCowPolicy(cowPolicy);
        }
        callback.parseComplete(request, null);
    }

    public void checkDeleteOnClose(String fileId, DeleteOnCloseCallback listener) {
        this.enqueueOperation(STAGEOP_OFT_DELETE, new Object[]{fileId}, null, listener);
    }

    public static interface DeleteOnCloseCallback {

        public void deleteOnCloseResult(boolean isDeleteOnClose, Exception error);
    }

    private void doCheckDeleteOnClose(StageRequest m) {

        final String fileId = (String)m.getArgs()[0];
        final DeleteOnCloseCallback callback = (DeleteOnCloseCallback)m.getCallback();

        final boolean deleteOnClose = oft.contains(fileId);

        callback.deleteOnCloseResult(deleteOnClose, null);
    }

    @Override
    public void run() {

        notifyStarted();

        // interval to check the OFT

        timeToNextOFTclean = OFT_CLEAN_INTERVAL;
        lastOFTcheck = TimeSync.getLocalSystemTime();

        while (!quit) {
            try {
                final StageRequest op = q.poll(timeToNextOFTclean, TimeUnit.MILLISECONDS);

                checkOpenFileTable();

                if (op == null) {
                    // Logging.logMessage(Logging.LEVEL_DEBUG,this,"no request
                    // -- timer only");
                    continue;
                }

                processMethod(op);

            } catch (InterruptedException ex) {
                break;
            } catch (Exception ex) {
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
            List<OpenFileTable.OpenFileTableEntry> closedFiles = oft.clean(TimeSync.getLocalSystemTime());
            // Logging.logMessage(Logging.LEVEL_DEBUG,this,"closing
            // "+closedFiles.size()+" files");
            for (OpenFileTable.OpenFileTableEntry entry : closedFiles) {

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "send internal close event for " + entry.getFileId() + ", deleteOnClose=" + entry.isDeleteOnClose());
                capCache.remove(entry.getFileId());

                //send close event
                OSDOperation closeEvent = master.getInternalEvent(EventCloseFile.class);
                closeEvent.startInternalEvent(new Object[]{entry.getFileId(),entry.isDeleteOnClose()});
            }
            timeToNextOFTclean = OFT_CLEAN_INTERVAL;
        }
        lastOFTcheck = TimeSync.getLocalSystemTime();
    }

    protected void processMethod(StageRequest m) {

        final int requestedMethod = m.getStageMethod();

        if (requestedMethod == STAGEOP_PARSE_AUTH_OFTOPEN) {
            doPrepareRequest(m);
        } else if (requestedMethod == STAGEOP_OFT_DELETE) {
            doCheckDeleteOnClose(m);
        }


    }

    private boolean parseRequest(OSDRequest rq) {
        final ONCRPCRequest rpcRq = rq.getRPCRequest();

        final ONCRPCRequestHeader hdr = rpcRq.getRequestHeader();

        //assemble stuff
        if (hdr.getInterfaceVersion() != OSDInterface.getVersion()) {
            rq.sendProtocolException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROG_MISMATCH,
                    ErrNo.EINVAL,"invalid version requested"));
            return false;
        }

        // everything ok, find the right operation
        OSDOperation op = master.getOperation(hdr.getOperationNumber());
        if (op == null) {
            rq.sendProtocolException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROC_UNAVAIL,
                    ErrNo.EINVAL,"requested operation is not available on this OSD (proc # "+hdr.getOperationNumber()+")"));
            return false;
        }
        rq.setOperation(op);

        try {
            rq.setRequestArgs(op.parseRPCMessage(rpcRq.getRequestFragment(), rq));
        } catch (InvalidXLocationsException ex) {
            OSDException osdex = new OSDException(ErrorCodes.NOT_IN_XLOC, ex.getMessage(), "");
            rpcRq.sendGenericException(osdex);
            return false;
        } catch (Throwable ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,ex);
            rpcRq.sendGarbageArgs(ex.toString());
            return false;
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "request parsed: " + rq.getRequestId());
        }
        return true;
    }

    private void processAuthenticate(OSDRequest rq) throws OSDException {

        final Capability rqCap = rq.getCapability();

        //check capability args
        if (rqCap.getFileId().length() == 0) {
            throw new OSDException(ErrorCodes.INVALID_FILEID, "invalid capability. file_id must not be empty", "");
        }

        if (rqCap.getEpochNo() < 0) {
            throw new OSDException(ErrorCodes.INVALID_FILEID, "invalid capability. epoch must not be < 0", "");
        }

        if (ignoreCaps)
            return;


        if (!rqCap.getFileId().equals(rq.getFileId())) {
            throw new OSDException(ErrorCodes.AUTH_FAILED, "capability was issued for another file than the one requested", "");
        }

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
        if (!isValid) {
            throw new OSDException(ErrorCodes.AUTH_FAILED, "capability is not valid", "");
        }
    }

    public int getNumOpenFiles() {
        return oft.getNumOpenFiles();
    }

    public long getNumRequests() {
        return numRequests;
    }
    
}
