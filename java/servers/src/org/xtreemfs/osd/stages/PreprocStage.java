/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.common.olp.AugmentedInternalRequest;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.olp.OverloadProtectedStage;
import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
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

public class PreprocStage extends OverloadProtectedStage<AugmentedRequest> {
    
    private final static int                                NUM_RQ_TYPES               = 28;
    private final static int                                NUM_INTERNAL_RQ_TYPES      = 2;
    private final static int                                STAGE_ID                   = 3;
    private final static int                                NUM_SUB_SEQ_STAGES         = 2;
    
    public final static int                                 STAGEOP_PROCESS            = -1;
    public final static int                                 STAGEOP_OFT_DELETE         = 0;
    public final static int                                 STAGEOP_PING_FILE          = 1;
    
    private final static long                               OFT_CLEAN_INTERVAL         = 1000 * 60;
    private final static long                               OFT_OPEN_EXTENSION         = 1000 * 30;
    
    private final Map<String, LRUCache<String, Capability>> capCache;
    
    private final OpenFileTable                             oft;
            
    private volatile long                                   numRequests;
    
    /**
     * X-Location cache XXX dead code
     */
    //private final LRUCache<String, XLocations>              xLocCache;
    
    private final MetadataCache                             metadataCache;
    
    private final OSDRequestDispatcher                      master;
    
    private final boolean                                   ignoreCaps;
    
    private static final int                                MAX_CAP_CACHE              = 20;
        
    public PreprocStage(OSDRequestDispatcher master, MetadataCache metadataCache, int numStorageThreads) {
        super("OSD PreProcSt", STAGE_ID + numStorageThreads, NUM_RQ_TYPES, NUM_INTERNAL_RQ_TYPES, 
                NUM_SUB_SEQ_STAGES + numStorageThreads, OFT_CLEAN_INTERVAL);
        
        capCache = new HashMap<String, LRUCache<String, Capability>>();
        oft = new OpenFileTable();
        //xLocCache = new LRUCache<String, XLocations>(10000);
        this.master = master;
        this.metadataCache = metadataCache;
        this.ignoreCaps = master.getConfig().isIgnoreCaps();
    }
    
    public void processRequest(OSDRequest request, RPCRequestCallback callback) {
        
        enter(STAGEOP_PROCESS, null, request, callback);
    }
    
    private void doProcessRequest(OLPStageRequest<AugmentedRequest> stageRequest) {
        
        final OSDRequest request = (OSDRequest) stageRequest.getRequest();
        final RPCRequestCallback callback = (RPCRequestCallback) stageRequest.getCallback();
        final OSDOperation op = request.getOperation();
        
        ErrorResponse err = null;
        
        numRequests++;
        
        // parse request
        err = parseRequest(request, op);
        if (err != null) {
            
            stageRequest.voidMeasurments();
            callback.failed(err);
            return;
        }
        
        // authenticate request
        if (request.getOperation().requiresCapability()) {
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "STAGEOP AUTH");
            }
            err = processAuthenticate(request);
            if (err != null) {
                
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                        "authentication of request failed: %s", ErrorUtils.formatError(err));
                }
                stageRequest.voidMeasurments();
                callback.failed(err);
                return;
            }
        }
        
        // check COW
        final String fileId = request.getFileId();
        if (fileId != null) {
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "STAGEOP OPEN");
            
            // check if snasphots are enabled and a write operation is executed;
            // this is required to create new snapshots when files open for
            // writing are closed, even if the same files are still open for
            // reading
            boolean write = request.getCapability() != null
                && request.getCapability().getSnapConfig() != SnapConfig.SNAP_CONFIG_SNAPS_DISABLED
                && ((SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber() | 
                     SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber() | 
                     SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber()) & 
                     request.getCapability().getAccessMode()) > 0;
            
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
        
        // process request
        err = op.startRequest(request, callback);
        if (err != null) {
            
            stageRequest.voidMeasurments();
            callback.failed(err);
        }
    }
    
    public void pingFile(String fileId) {
        
        enter(STAGEOP_PING_FILE, new Object[] { fileId }, new AugmentedInternalRequest(STAGEOP_PING_FILE), null);
    }
    
    private void doPingFile(OLPStageRequest<AugmentedRequest> m) throws ErrorResponseException {
        
        final String fileId = (String) m.getArgs()[0];
        
        // TODO: check if the file was opened for writing
        final long time = TimeSync.getLocalSystemTime();
        oft.refresh(fileId, time + OFT_OPEN_EXTENSION, false);  
        
        m.getCallback().success(time, m);
    }
    
    public void checkDeleteOnClose(String fileId, Callback callback) {
        
        enter(STAGEOP_OFT_DELETE, new Object[] { fileId }, new AugmentedInternalRequest(STAGEOP_OFT_DELETE), callback);
    }
    
    /**
     * <p>Synchronous call that has to be executed by the PreprocStage-Thread. May be executed within 
     * operation-processing to prevent requests from being re-queued.</p>
     * 
     * @param fileId
     * @return true, if the file to the corresponding fileId is set deleteOnClose.
     */
    public final boolean doCheckDeleteOnClose(String fileId) {
        
        assert(Thread.currentThread().getId() == this.getId());
        
        boolean deleteOnClose = oft.contains(fileId);
        if (deleteOnClose) {
            oft.setDeleteOnClose(fileId);
        }
        
        return deleteOnClose;
    }
    
    /**
     * <p>Synchronous call that has to be executed by the PreprocStage-Thread. May be executed within 
     * operation-processing to prevent requests from being re-queued.</p>
     * 
     * @param clientUuid
     * @param pid
     * @param fileId
     * @param offset
     * @param length
     * @param exclusive
     * 
     * @return the acquired {@link Lock}, or an {@link ErrorResponse} if this method fails.
     */
    public final Message doAcquireLock(String clientUuid, int pid, String fileId, long offset, long length, 
            boolean exclusive) {
        
        assert(Thread.currentThread().getId() == this.getId());
        
        try {
            
            OpenFileTableEntry e = oft.getEntry(fileId);
            if (e == null) {
                
                return ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, 
                        POSIXErrno.POSIX_ERROR_EIO, "no entry in OFT, programmatic error");
            }
            
            AdvisoryLock l = e.acquireLock(clientUuid, pid, offset, length, exclusive);
            if (l != null) {
                
                return Lock.newBuilder().setClientPid(l.getClientPid()).setClientUuid(l.getClientUuid()).setLength(
                        l.getLength()).setOffset(l.getOffset()).setExclusive(l.isExclusive()).build();
            } else {
                
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EAGAIN, 
                        "conflicting lock");
            }
        } catch (Exception ex) {
            
            return ErrorUtils.getInternalServerError(ex);
        }
    }
    
    /**
     * <p>Synchronous call that has to be executed by the PreprocStage-Thread. May be executed within 
     * operation-processing to prevent requests from being re-queued.</p>
     * 
     * @param clientUuid
     * @param pid
     * @param fileId
     * @param offset
     * @param length
     * @param exclusive
     * 
     * @return the acquired {@link Lock}, or an {@link ErrorResponse} if this method fails.
     */
    public final Message doCheckLock(String clientUuid, int pid, String fileId, long offset, long length,
            boolean exclusive) {
        
        assert(Thread.currentThread().getId() == this.getId());
        
        try {
            
            OpenFileTableEntry e = oft.getEntry(fileId);
            if (e == null) {
                
                return ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, 
                        "no entry in OFT, programmatic error");
            }
            
            AdvisoryLock l = e.checkLock(clientUuid, pid, offset, length, exclusive);
            return Lock.newBuilder().setClientPid(l.getClientPid()).setClientUuid(l.getClientUuid()).setLength(
                    l.getLength()).setOffset(l.getOffset()).setExclusive(l.isExclusive()).build();
            
        } catch (Exception ex) {
            
            return ErrorUtils.getInternalServerError(ex);
        }
    }
    
    /**
     * <p>Synchronous call that has to be executed by the PreprocStage-Thread. May be executed within 
     * operation-processing to prevent requests from being re-queued.</p>
     * 
     * @param clientUuid
     * @param pid
     * @param fileId
     * @param offset
     * @param length
     * @param exclusive
     * 
     * @return an {@link ErrorResponse} if this method fails, null otherwise.
     */
    public final ErrorResponse doUnlock(String clientUuid, int pid, String fileId) {
        
        assert(Thread.currentThread().getId() == this.getId());
        
        try {
            
            OpenFileTableEntry e = oft.getEntry(fileId);
            if (e == null) {
                
                return ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, 
                        "no entry in OFT, programmatic error");
            }
            
            e.unlock(clientUuid, pid);
            return null;
            
        } catch (Exception ex) {
            
            return ErrorUtils.getInternalServerError(ex);
        }
    }
    
    /**
     * <p>Clean up the open-file-table (OFT).</p>
     */
    @Override
    protected final void chronJob() {
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "OpenFileTable clean");
        }
            
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
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectedStage#_processMethod(org.xtreemfs.common.olp.OLPStageRequest)
     */
    @Override
    protected final boolean _processMethod(OLPStageRequest<AugmentedRequest> stageRequest) {
        
        final int requestedMethod = stageRequest.getStageMethod();
        final Callback callback = stageRequest.getCallback();
        
        try {
            
            switch (requestedMethod) {
            case STAGEOP_PROCESS:
                doProcessRequest(stageRequest);
                break;
            case STAGEOP_OFT_DELETE:
                callback.success(doCheckDeleteOnClose((String) stageRequest.getArgs()[0]), stageRequest);
                break;
            case STAGEOP_PING_FILE:
                doPingFile(stageRequest);
                break;
            default:
                Logging.logMessage(Logging.LEVEL_ERROR, this, "unknown stageop called: %d", requestedMethod);
                break;
            }
        } catch (ErrorResponseException e) {
            
            callback.failed(e);
        }
        
        return true;
    }
    
    private ErrorResponse parseRequest(OSDRequest rq, OSDOperation op) {
        
        ErrorResponse result = null;
                
        try {
            
            final Message rqPrototype = OSDServiceConstants.getRequestMessage(op.getProcedureId());
            if (rqPrototype == null) {
                rq.setRequestArgs(null);
                if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "received request with empty message");
            } else {
                if (rq.getRPCRequest().getMessage() != null) {
                    rq.setRequestArgs(rqPrototype.newBuilderForType().mergeFrom(new ReusableBufferInputStream(
                            rq.getRPCRequest().getMessage())).build());
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "received request of type %s",
                            rq.getRequestArgs().getClass().getName());
                    }
                } else {
                    rq.setRequestArgs(rqPrototype.getDefaultInstanceForType());
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, 
                                "received request of type %s (empty message)", 
                                rq.getRequestArgs().getClass().getName());
                    }
                }
            }
            result = op.parseRPCMessage(rq);
            
            if (Logging.isDebug() && result == null) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "request parsed: %d", rq.getRequestId());
            }
        } catch (Throwable ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, OutputUtils.stackTraceToString(ex));
            }
            result = ErrorUtils.getInternalServerError(ex);
        }

        return result;
    }
    
    private ErrorResponse processAuthenticate(OSDRequest rq) {
        
        final Capability rqCap = rq.getCapability();
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "capability: %s", 
                    rqCap.getXCap().toString().replace('\n', '/'));
        }
        
        // check capability args
        if (rqCap.getFileId().length() == 0) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "invalid capability. file_id must not be empty");
        }
        
        if (rqCap.getEpochNo() < 0) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "invalid capability. epoch must not be < 0");
        }
        
        if (ignoreCaps) return null;
        
        if (!rqCap.getFileId().equals(rq.getFileId())) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, 
                    "capability was issued for another file than the one requested");
        }
        
        // look in capCache
        boolean isValid = false;
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
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, 
                        "capability is not valid (timed out)");

            if (!rqCap.hasValidSignature())
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, 
                        "capability is not valid (invalid signature)");

            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, 
                    "capability is not valid (unknown cause)");
        }
        return null;
    }
    
/*
 * Monitoring
 */
    
    public int getNumOpenFiles() {
        
        return oft.getNumOpenFiles();
    }
    
    public long getNumRequests() {
        
        return numRequests;
    }
}
