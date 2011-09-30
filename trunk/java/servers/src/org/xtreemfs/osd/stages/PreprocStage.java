/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import com.google.protobuf.Message;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ReusableBufferInputStream;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.AdvisoryLock;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.OpenFileTable;
import org.xtreemfs.osd.OpenFileTable.OpenFileTableEntry;
import org.xtreemfs.osd.operations.EventCloseFile;
import org.xtreemfs.osd.operations.EventCreateFileVersion;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.CowPolicy.cowMode;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

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
    private final LRUCache<String, XLocations>              xLocCache;
    
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
        
        public void parseComplete(OSDRequest result, ErrorResponse error);
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
            ErrorResponse err = processAuthenticate(request);
            if (err != null) {
                callback.parseComplete(request, err);
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "authentication of request failed: %s", ErrorUtils.formatError(err));
                }
                return;
            }
        }
        
        String fileId = request.getFileId();
        if (fileId != null) {
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "STAGEOP OPEN");
            
            // check if snasphots are enabled and a write operation is executed;
            // this is required to create new snapshots when files open for
            // writing are closed, even if the same files are still open for
            // reading
            boolean write = request.getCapability() != null
                && request.getCapability().getSnapConfig() != SnapConfig.SNAP_CONFIG_SNAPS_DISABLED
                && ((SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber() | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber()) & request
                        .getCapability().getAccessMode()) > 0;
            
            CowPolicy cowPolicy;
            if (oft.contains(fileId)) {
                cowPolicy = oft.refresh(fileId, TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION, write);
            } else {
                
                // find out which COW mode to use, depending on the capability
                if (request.getCapability() == null
                    || request.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_SNAPS_DISABLED)
                    cowPolicy = CowPolicy.PolicyNoCow;
                else
                    cowPolicy = new CowPolicy(cowMode.COW_ONCE);
                
                oft.openFile(fileId, TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION, cowPolicy, write);
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
        
        // TODO: check if the file was opened for writing
        oft.refresh(fileId, TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION, false);
        
    }
    
    public void checkDeleteOnClose(String fileId, DeleteOnCloseCallback listener) {
        this.enqueueOperation(STAGEOP_OFT_DELETE, new Object[] { fileId }, null, listener);
    }
    
    public static interface DeleteOnCloseCallback {
        
        public void deleteOnCloseResult(boolean isDeleteOnClose, ErrorResponse error);
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
        
        public void parseComplete(Lock result, ErrorResponse error);
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
                callback.parseComplete(null, ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, "no entry in OFT, programmatic error"));
                return;
            }
            
            AdvisoryLock l = e.acquireLock(clientUuid, pid, offset, length, exclusive);
            if (l != null) {
                Lock lock = Lock.newBuilder().setClientPid(l.getClientPid()).setClientUuid(l.getClientUuid()).setLength(l.getLength()).setOffset(l.getOffset()).setExclusive(l.isExclusive()).build();
                callback.parseComplete(lock, null);
            }
            else
                callback.parseComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EAGAIN, "conflicting lock"));
            
        } catch (Exception ex) {
            callback.parseComplete(null, ErrorUtils.getInternalServerError(ex));
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
                callback.parseComplete(null, ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, "no entry in OFT, programmatic error"));
                return;
            }
            
            AdvisoryLock l = e.checkLock(clientUuid, pid, offset, length, exclusive);
            Lock lock = Lock.newBuilder().setClientPid(l.getClientPid()).setClientUuid(l.getClientUuid()).setLength(l.getLength()).setOffset(l.getOffset()).setExclusive(l.isExclusive()).build();
            callback.parseComplete(lock, null);
            
        } catch (Exception ex) {
            callback.parseComplete(null, ErrorUtils.getInternalServerError(ex));
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
                callback.parseComplete(null, ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, "no entry in OFT, programmatic error"));
                return;
            }
            
            e.unlock(clientUuid, pid);
            callback.parseComplete(null, null);
            
        } catch (Exception ex) {
            callback.parseComplete(null, ErrorUtils.getInternalServerError(ex));
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
            
            long currentTime = TimeSync.getLocalSystemTime();
            
            // do OFT clean
            List<OpenFileTable.OpenFileTableEntry> closedFiles = oft.clean(currentTime);
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
            
            // check if written files need to be closed; if no files were closed
            // completely, generate close events w/o discarding the open state
            List<OpenFileTable.OpenFileTableEntry> closedWrittenFiles = oft.cleanWritten(currentTime);
            if (closedFiles.size() == 0) {
                for (OpenFileTable.OpenFileTableEntry entry : closedWrittenFiles) {
                    OSDOperation createVersionEvent = master.getInternalEvent(EventCreateFileVersion.class);
                    createVersionEvent.startInternalEvent(new Object[] { entry.getFileId(),
                        metadataCache.getFileInfo(entry.getFileId()) });
                }
            }
            
            timeToNextOFTclean = OFT_CLEAN_INTERVAL;
        }
        lastOFTcheck = TimeSync.getLocalSystemTime();
    }
    
    protected void processMethod(StageRequest m) {
        
        final int requestedMethod = m.getStageMethod();
        
        switch (requestedMethod) {
        case STAGEOP_PARSE_AUTH_OFTOPEN:
            doPrepareRequest(m);
            break;
        case STAGEOP_OFT_DELETE:
            doCheckDeleteOnClose(m);
            break;
        case STAGEOP_ACQUIRE_LOCK:
            doAcquireLock(m);
            break;
        case STAGEOP_CHECK_LOCK:
            doCheckLock(m);
            break;
        case STAGEOP_UNLOCK:
            doUnlock(m);
            break;
        case STAGEOP_PING_FILE:
            doPingFile(m);
            break;
        default:
            Logging.logMessage(Logging.LEVEL_ERROR, this, "unknown stageop called: %d", requestedMethod);
            break;
        }
        
    }
    
    private boolean parseRequest(OSDRequest rq) {

        RPCHeader hdr = rq.getRpcRequest().getHeader();

        if (hdr.getMessageType() != MessageType.RPC_REQUEST) {
            rq.sendError(ErrorType.GARBAGE_ARGS, POSIXErrno.POSIX_ERROR_EIO, "expected RPC request message type but got "+hdr.getMessageType());
            return false;
        }

        RPCHeader.RequestHeader rqHdr = hdr.getRequestHeader();

        if (rqHdr.getInterfaceId() != OSDServiceConstants.INTERFACE_ID) {
            rq.sendError(ErrorType.INVALID_INTERFACE_ID, POSIXErrno.POSIX_ERROR_EIO, "invalid interface id. Maybe wrong service address/port configured?");
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "invalid version requested (requested=%d avail=%d)", rqHdr.getInterfaceId(),
                    OSDServiceConstants.INTERFACE_ID);
            }
            return false;
        }
        
        // everything ok, find the right operation
        OSDOperation op = master.getOperation(rqHdr.getProcId());
        if (op == null) {
            rq.sendError(ErrorType.INVALID_PROC_ID, POSIXErrno.POSIX_ERROR_EINVAL, "requested operation is not available on this OSD (proc # "
                    + rqHdr.getProcId() + ")");
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                    "requested operation is not available on this OSD (proc #%d)", rqHdr.getProcId());
            }
            return false;
        }
        rq.setOperation(op);
        
        try {
            final Message rqPrototype = OSDServiceConstants.getRequestMessage(rqHdr.getProcId());
            if (rqPrototype == null) {
                rq.setRequestArgs(null);
                if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "received request with empty message");
            } else {
                if (rq.getRPCRequest().getMessage() != null) {
                    rq.setRequestArgs(rqPrototype.newBuilderForType().mergeFrom(new ReusableBufferInputStream(rq.getRPCRequest().getMessage())).build());
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "received request of type %s",
                            rq.getRequestArgs().getClass().getName());
                    }
                } else {
                    rq.setRequestArgs(rqPrototype.getDefaultInstanceForType());
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "received request of type %s (empty message)",
                            rq.getRequestArgs().getClass().getName());
                    }
                }
            }
            ErrorResponse err = op.parseRPCMessage(rq);
            if (err != null) {
                rq.getRpcRequest().sendError(err);
                return false;
            }
            
        } catch (Throwable ex) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, OutputUtils
                        .stackTraceToString(ex));
            rq.getRpcRequest().sendError(ErrorUtils.getInternalServerError(ex));
            return false;
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "request parsed: %d", rq
                    .getRequestId());
        }
        return true;
    }
    
    private ErrorResponse processAuthenticate(OSDRequest rq) {
        
        final Capability rqCap = rq.getCapability();
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "capability: %s", rqCap.getXCap().toString().replace('\n', '/'));
        }
        
        // check capability args
        if (rqCap.getFileId().length() == 0) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "invalid capability. file_id must not be empty");
        }
        
        if (rqCap.getEpochNo() < 0) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "invalid capability. epoch must not be < 0");
        }
        
        if (ignoreCaps)
            return null;
        
        if (!rqCap.getFileId().equals(rq.getFileId())) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, "capability was issued for another file than the one requested");
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
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, "capability is not valid (timed out)");

            if (!rqCap.hasValidSignature())
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, "capability is not valid (invalid signature)");

            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, "capability is not valid (unknown cause)");
        }
        return null;
    }
    
    public int getNumOpenFiles() {
        return oft.getNumOpenFiles();
    }
    
    public long getNumRequests() {
        return numRequests;
    }
    
}
