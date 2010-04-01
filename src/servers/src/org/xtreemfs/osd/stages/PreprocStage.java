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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.interfaces.Lock;
import org.xtreemfs.interfaces.SnapConfig;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.OSDInterface.ProtocolException;
import org.xtreemfs.interfaces.OSDInterface.errnoException;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCRequestHeader;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCResponseHeader;
import org.xtreemfs.osd.AdvisoryLock;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.LocationsCache;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.OpenFileTable;
import org.xtreemfs.osd.OpenFileTable.OpenFileTableEntry;
import org.xtreemfs.osd.operations.EventCloseFile;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.CowPolicy.cowMode;

public class PreprocStage extends Stage {
    
    public final static int                                 STAGEOP_PARSE_AUTH_OFTOPEN = 1;
    
    public final static int                                 STAGEOP_OFT_DELETE         = 2;
    
    public final static int                                 STAGEOP_ACQUIRE_LEASE      = 3;
    
    public final static int                                 STAGEOP_RETURN_LEASE       = 4;
    
    public final static int                                 STAGEOP_VERIFIY_CLEANUP    = 5;
    
    public final static int                                 STAGEOP_ACQUIRE_LOCK       = 10;
    
    public final static int                                 STAGEOP_CHECK_LOCK         = 11;
    
    public final static int                                 STAGEOP_UNLOCK             = 12;

    public final static int                                 STAGEOP_PING_FILE          = 14;
    
    private final static long                               OFT_CLEAN_INTERVAL         = 1000 * 60;
    
    private final static long                               OFT_OPEN_EXTENSION         = 1000 * 30;
    
    private final Map<String, LRUCache<String, Capability>> capCache;
    
    private final OpenFileTable                             oft;
    
    // time left to next clean op
    private long                                            timeToNextOFTclean;
    
    // last check of the OFT
    private long                                            lastOFTcheck;
    
    private volatile long                                   numRequests;
    
    /**
     * X-Location cache
     */
    private final LRUCache<String,XLocations>               xLocCache;
    
    private final MetadataCache                             metadataCache;
    
    private final OSDRequestDispatcher                      master;
    
    private final boolean                                   ignoreCaps;
    
    private static final int                                MAX_CAP_CACHE              = 20;
    
    /** Creates a new instance of AuthenticationStage */
    public PreprocStage(OSDRequestDispatcher master, MetadataCache metadataCache) {
        
        super("OSD PreProcSt");
        
        capCache = new HashMap();
        oft = new OpenFileTable();
        xLocCache = new LRUCache<String, XLocations>(10000);
        this.master = master;
        this.metadataCache = metadataCache;
        this.ignoreCaps = master.getConfig().isIgnoreCaps();
    }
    
    public void prepareRequest(OSDRequest request, ParseCompleteCallback listener) {
        this.enqueueOperation(STAGEOP_PARSE_AUTH_OFTOPEN, new Object[] { request }, null, listener);
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
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "STAGEOP AUTH");
            try {
                processAuthenticate(request);
            } catch (OSDException ex) {
                callback.parseComplete(request, ex);
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "authentication of request failed: %s", ex.getError_message());
                }
                return;
            }
        }
        
        String fileId = request.getFileId();
        if (fileId != null) {
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "STAGEOP OPEN");
            
            CowPolicy cowPolicy;
            if (oft.contains(fileId)) {
                cowPolicy = oft.refresh(fileId, TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION);
            } else {
                
                // find out which COW mode to use, depending on the capability
                if (request.getCapability() == null || request.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_SNAPS_DISABLED)
                    cowPolicy = CowPolicy.PolicyNoCow;
                else
                    cowPolicy = new CowPolicy(cowMode.COW_ONCE);
                
                oft.openFile(fileId, TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION, cowPolicy);
                request.setFileOpen(true);
            }
            request.setCowPolicy(cowPolicy);
        }
        callback.parseComplete(request, null);
    }

    public void pingFile(String fileId) {
        this.enqueueOperation(STAGEOP_PING_FILE, new Object[] { fileId }, null, null);
    }

    private void doPingFile(StageRequest m) {

        final String fileId = (String) m.getArgs()[0];

        oft.refresh(fileId,TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION);

    }
    
    public void checkDeleteOnClose(String fileId, DeleteOnCloseCallback listener) {
        this.enqueueOperation(STAGEOP_OFT_DELETE, new Object[] { fileId }, null, listener);
    }
    
    public static interface DeleteOnCloseCallback {
        
        public void deleteOnCloseResult(boolean isDeleteOnClose, Exception error);
    }
    
    private void doCheckDeleteOnClose(StageRequest m) {
        
        final String fileId = (String) m.getArgs()[0];
        final DeleteOnCloseCallback callback = (DeleteOnCloseCallback) m.getCallback();
        
        final boolean deleteOnClose = oft.contains(fileId);
        if (deleteOnClose)
            oft.setDeleteOnClose(fileId);
        
        callback.deleteOnCloseResult(deleteOnClose, null);
    }
    
    public static interface LockOperationCompleteCallback {
        
        public void parseComplete(Lock result, Exception error);
    }
    
    public void acquireLock(String clientUuid, int pid, String fileId, long offset, long length,
        boolean exclusive, OSDRequest request, LockOperationCompleteCallback listener) {
        this.enqueueOperation(STAGEOP_ACQUIRE_LOCK, new Object[] { clientUuid, pid, fileId, offset, length,
            exclusive }, request, listener);
    }
    
    public void doAcquireLock(StageRequest m) {
        final LockOperationCompleteCallback callback = (LockOperationCompleteCallback) m.getCallback();
        try {
            final String clientUuid = (String) m.getArgs()[0];
            final Integer pid = (Integer) m.getArgs()[1];
            final String fileId = (String) m.getArgs()[2];
            final Long offset = (Long) m.getArgs()[3];
            final Long length = (Long) m.getArgs()[4];
            final Boolean exclusive = (Boolean) m.getArgs()[5];
            
            OpenFileTableEntry e = oft.getEntry(fileId);
            if (e == null) {
                callback.parseComplete(null, new RuntimeException("no entry in OFT, programmatic erro"));
                return;
            }
            
            AdvisoryLock l = e.acquireLock(clientUuid, pid, offset, length, exclusive);
            if (l != null)
                callback.parseComplete(new Lock(l.getClientPid(), l.getClientUuid(), l.getLength(), l
                        .getOffset()), null);
            else
                callback.parseComplete(null, new OSDException(ErrNo.EAGAIN, "conflicting lock", ""));
            
        } catch (Exception ex) {
            callback.parseComplete(null, ex);
        }
    }
    
    public void checkLock(String clientUuid, int pid, String fileId, long offset, long length,
        boolean exclusive, OSDRequest request, LockOperationCompleteCallback listener) {
        this.enqueueOperation(STAGEOP_CHECK_LOCK, new Object[] { clientUuid, pid, fileId, offset, length,
            exclusive }, request, listener);
    }
    
    public void doCheckLock(StageRequest m) {
        final LockOperationCompleteCallback callback = (LockOperationCompleteCallback) m.getCallback();
        try {
            final String clientUuid = (String) m.getArgs()[0];
            final Integer pid = (Integer) m.getArgs()[1];
            final String fileId = (String) m.getArgs()[2];
            final Long offset = (Long) m.getArgs()[3];
            final Long length = (Long) m.getArgs()[4];
            final Boolean exclusive = (Boolean) m.getArgs()[5];
            
            OpenFileTableEntry e = oft.getEntry(fileId);
            if (e == null) {
                callback.parseComplete(null, new RuntimeException("no entry in OFT, programmatic erro"));
                return;
            }
            
            AdvisoryLock l = e.checkLock(clientUuid, pid, offset, length, exclusive);
            callback.parseComplete(
                new Lock(l.getClientPid(), l.getClientUuid(), l.getLength(), l.getOffset()), null);
            
        } catch (Exception ex) {
            callback.parseComplete(null, ex);
        }
    }
    
    public void unlock(String clientUuid, int pid, String fileId, OSDRequest request,
        LockOperationCompleteCallback listener) {
        this.enqueueOperation(STAGEOP_UNLOCK, new Object[] { clientUuid, pid, fileId }, request, listener);
    }
    
    public void doUnlock(StageRequest m) {
        final LockOperationCompleteCallback callback = (LockOperationCompleteCallback) m.getCallback();
        try {
            final String clientUuid = (String) m.getArgs()[0];
            final Integer pid = (Integer) m.getArgs()[1];
            final String fileId = (String) m.getArgs()[2];
            
            OpenFileTableEntry e = oft.getEntry(fileId);
            if (e == null) {
                callback.parseComplete(null, new RuntimeException("no entry in OFT, programmatic erro"));
                return;
            }
            
            e.unlock(clientUuid, pid);
            callback.parseComplete(null, null);
            
        } catch (Exception ex) {
            callback.parseComplete(null, ex);
        }
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
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "OpenFileTable clean");
            
            // do OFT clean
            List<OpenFileTable.OpenFileTableEntry> closedFiles = oft.clean(TimeSync.getLocalSystemTime());
            // Logging.logMessage(Logging.LEVEL_DEBUG,this,"closing
            // "+closedFiles.size()+" files");
            for (OpenFileTable.OpenFileTableEntry entry : closedFiles) {
                
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                        "send internal close event for %s, deleteOnClose=%b", entry.getFileId(), entry
                                .isDeleteOnClose());
                
                LRUCache<String, Capability> cachedCaps = capCache.remove(entry.getFileId());
                
                // send close event
                OSDOperation closeEvent = master.getInternalEvent(EventCloseFile.class);
                closeEvent.startInternalEvent(new Object[] { entry.getFileId(), entry.isDeleteOnClose(),
                    metadataCache.getFileInfo(entry.getFileId()), entry.getCowPolicy(), cachedCaps });
            }
            timeToNextOFTclean = OFT_CLEAN_INTERVAL;
        }
        lastOFTcheck = TimeSync.getLocalSystemTime();
    }
    
    protected void processMethod(StageRequest m) {
        
        final int requestedMethod = m.getStageMethod();

        switch (requestedMethod) {
            case STAGEOP_PARSE_AUTH_OFTOPEN : doPrepareRequest(m); break;
            case STAGEOP_OFT_DELETE : doCheckDeleteOnClose(m); break;
            case STAGEOP_ACQUIRE_LOCK : doAcquireLock(m); break;
            case STAGEOP_CHECK_LOCK : doCheckLock(m); break;
            case STAGEOP_UNLOCK : doUnlock(m); break;
            case STAGEOP_PING_FILE : doPingFile(m); break;
            default : Logging.logMessage(Logging.LEVEL_ERROR, this,"unknown stageop called: %d",requestedMethod); break;
        }
        
    }
    
    private boolean parseRequest(OSDRequest rq) {
        final ONCRPCRequest rpcRq = rq.getRPCRequest();
        
        final ONCRPCRequestHeader hdr = rpcRq.getRequestHeader();
        
        // assemble stuff
        if (hdr.getInterfaceVersion() != OSDInterface.getVersion()) {
            rq.sendProtocolException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROG_MISMATCH,
                ErrNo.EINVAL, "invalid version requested (requested=" + hdr.getInterfaceVersion() + " avail="
                    + OSDInterface.getVersion() + ")"));
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "invalid version requested (requested=%d avail=%d)", hdr.getInterfaceVersion(),
                    OSDInterface.getVersion());
            }
            return false;
        }
        
        // everything ok, find the right operation
        OSDOperation op = master.getOperation(hdr.getProcedure());
        if (op == null) {
            rq.sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROC_UNAVAIL,
                ErrNo.EINVAL, "requested operation is not available on this OSD (proc # "
                    + hdr.getProcedure() + ")"));
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "requested operation is not available on this OSD (proc #%d)", hdr.getProcedure());
            }
            return false;
        }
        rq.setOperation(op);
        
        try {
            final yidl.runtime.Object requestArgs = op.parseRPCMessage(rpcRq.getRequestFragment(), rq);
            rq.setRequestArgs(requestArgs);
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "received request of type %s",
                    requestArgs.getTypeName());
        } catch (InvalidXLocationsException ex) {
            OSDException osdex = new OSDException(ErrorCodes.NOT_IN_XLOC, ex.getMessage(), "");
            rpcRq.sendException(osdex);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "this OSD is not in the Xloc sent by the client: %s", ex.getMessage());
            }
            return false;
        } catch (Throwable ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, OutputUtils
                        .stackTraceToString(ex));
            rpcRq.sendException(new errnoException(ErrNo.EINVAL, "invalid arguments: "+ex.toString(), OutputUtils
                        .stackTraceToString(ex)));
            return false;
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "request parsed: %d", rq
                    .getRequestId());
        }
        return true;
    }
    
    private void processAuthenticate(OSDRequest rq) throws OSDException {
        
        final Capability rqCap = rq.getCapability();
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "capability: %s", rqCap.getXCap());
        }
        
        // check capability args
        if (rqCap.getFileId().length() == 0) {
            throw new OSDException(ErrorCodes.INVALID_FILEID,
                "invalid capability. file_id must not be empty", "");
        }
        
        if (rqCap.getEpochNo() < 0) {
            throw new OSDException(ErrorCodes.INVALID_FILEID, "invalid capability. epoch must not be < 0", "");
        }
        
        if (ignoreCaps)
            return;
        
        if (!rqCap.getFileId().equals(rq.getFileId())) {
            throw new OSDException(ErrorCodes.AUTH_FAILED,
                "capability was issued for another file than the one requested", "");
        }
        
        boolean isValid = false;
        // look in capCache
        LRUCache<String, Capability> cachedCaps = capCache.get(rqCap.getFileId());
        if (cachedCaps != null) {
            final Capability cap = cachedCaps.get(rqCap.getSignature());
            if (cap != null) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "using cached cap: %s %s", cap.getFileId(),
                        cap.getSignature());
                }
                isValid = !cap.hasExpired();
            }
        }
        
        // TODO: check access mode (read, write, ...)
        
        if (!isValid) {
            isValid = rqCap.isValid();
            if (isValid) {
                // add to cache
                if (cachedCaps == null) {
                    cachedCaps = new LRUCache<String, Capability>(MAX_CAP_CACHE);
                    capCache.put(rqCap.getFileId(), cachedCaps);
                }
                cachedCaps.put(rqCap.getSignature(), rqCap);
            }
        }
        
        // depending on the result the event listener is sent
        if (!isValid) {
            if (rqCap.hasExpired())
                throw new OSDException(ErrorCodes.AUTH_FAILED, "capability is not valid (timed out)", "");
            if (rqCap.hasValidSignature())
                throw new OSDException(ErrorCodes.AUTH_FAILED, "capability is not valid (invalid signature)",
                    "");
            
            throw new OSDException(ErrorCodes.AUTH_FAILED, "capability is not valid (unknown cause)", "");
        }
    }
    
    public int getNumOpenFiles() {
        return oft.getNumOpenFiles();
    }
    
    public long getNumRequests() {
        return numRequests;
    }
    
}
