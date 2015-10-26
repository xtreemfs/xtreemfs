/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
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
import org.xtreemfs.osd.quota.OSDVoucherManager;
import org.xtreemfs.osd.quota.VoucherErrorException;
import org.xtreemfs.osd.rwre.ReplicaUpdatePolicy;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.CowPolicy.cowMode;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.LeaseState;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.XLocSetVersionState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

import com.google.protobuf.Message;

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
    
    public final static int                                 STAGEOP_CLOSE_FILE         = 15;
    
    public final static int                                 STAGEOP_INVALIDATE_XLOC    = 16;

    public final static int                                 STAGEOP_UPDATE_XLOC        = 17;

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
    
    private final StorageLayout                             layout;

    private final OSDRequestDispatcher                      master;
    
    private final boolean                                   ignoreCaps;
    
    private static final int                                MAX_CAP_CACHE              = 20;
    
    /** Creates a new instance of AuthenticationStage */
    public PreprocStage(OSDRequestDispatcher master, MetadataCache metadataCache, StorageLayout layout,
            int maxRequestsQueueLength) {
        
        super("OSD PreProcSt", maxRequestsQueueLength);
        
        capCache = new HashMap();
        oft = new OpenFileTable();
        xLocCache = new LRUCache<String, XLocations>(10000);
        this.master = master;
        this.metadataCache = metadataCache;
        this.layout = layout;
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
        
        // Check if the request is from the same view (same XLocationSet version) and install newer one.
        if (!request.getOperation().bypassViewValidation() && request.getLocationList() != null) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "STAGEOP VIEW");
            ErrorResponse error = processValidateView(request);
            if (error != null) {
                callback.parseComplete(request, error);
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "request failed with an invalid view: %s", ErrorUtils.formatError(error));
                }
                return;
            }
        }

        String fileId = request.getFileId();
        if (fileId != null) {
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "STAGEOP OPEN");
            
            boolean writeAccess = request.getCapability() != null
                    && ((SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber() | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY
                                .getNumber()) & request.getCapability().getAccessMode()) > 0;

            // configure quota, if valid
            if (writeAccess) {
                OSDVoucherManager osdVoucherManager = master.getOsdVoucherManager();
                Capability capability = request.getCapability();
                try {
                    osdVoucherManager.registerFileVoucher(fileId, capability.getClientIdentity(),
                            capability.getExpireMs(), capability.getVoucherSize());
                } catch (VoucherErrorException ex) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                                "Failed to process doPrepareRequest() request due to the following VoucherErrorException:");
                        Logging.logError(Logging.LEVEL_DEBUG, this, ex);
                    }

                    callback.parseComplete(request, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                            POSIXErrno.POSIX_ERROR_EACCES, ex.toString(), ex));
                    return;
                } catch (IOException ex) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                                "Failed to process doPrepareRequest() request due to the following IOException:");
                    }
                    Logging.logError(Logging.LEVEL_ERROR, this, ex);

                    callback.parseComplete(request, ErrorUtils.getErrorResponse(ErrorType.IO_ERROR,
                            POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
                }
            }

            CowPolicy cowPolicy = CowPolicy.PolicyNoCow;

            // check if snasphots are enabled and a write operation is executed;
            // this is required to create new snapshots when files open for
            // writing are closed, even if the same files are still open for
            // reading
            boolean snapShotWrite = writeAccess
                    && request.getCapability().getSnapConfig() != SnapConfig.SNAP_CONFIG_SNAPS_DISABLED;

            if (oft.contains(fileId)) {
                cowPolicy = oft.refresh(fileId, TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION, snapShotWrite);
            } else {

                // find out which COW mode to use, depending on the capability
                if (request.getCapability() == null
                        || request.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_SNAPS_DISABLED)
                    cowPolicy = CowPolicy.PolicyNoCow;
                else
                    cowPolicy = new CowPolicy(cowMode.COW_ONCE);

                oft.openFile(fileId, TimeSync.getLocalSystemTime() + OFT_OPEN_EXTENSION, cowPolicy, snapShotWrite);
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
    
    /**
     * Closing the file clears the capability cache and removes the entry for fileId from the {@link OpenFileTable} if
     * it exists. <br>
     * Attention: This will not trigger {@link EventCloseFile} or {@link EventCreateFileVersion} by itself.
     * TODO(jdillmann): Discuss if this should trigger the event.
     * 
     * @param fileId
     * @param listener
     */
    public void close(String fileId, CloseCallback listener) {
        this.enqueueOperation(STAGEOP_CLOSE_FILE, new Object[] { fileId }, null, listener);
    }

    public static interface CloseCallback {
        public void closeResult( OpenFileTableEntry entry, ErrorResponse error);
    }

    private void doClose(StageRequest m) {

        final String fileId = (String) m.getArgs()[0];
        final CloseCallback callback = (CloseCallback) m.getCallback();

        OpenFileTableEntry entry = oft.close(fileId);

        if(entry != null && entry.getFileId() != null) {
            LRUCache<String, Capability> cachedCaps = capCache.remove(entry.getFileId());
            callback.closeResult(entry, null);
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
                
                checkOpenFileTable(false);
                
                if (op == null) {
                    // Logging.logMessage(Logging.LEVEL_DEBUG,this,"no request
                    // -- timer only");
                    continue;
                }
                
                processMethod(op);
                
            } catch (InterruptedException ex) {
                break;
            } catch (Throwable ex) {
                notifyCrashed(ex);
                break;
            }
        }
        
        notifyStopped();
    }
    
    /**
     * Removes all open files from the {@link OpenFileTable} whose time has expired and triggers for each file
     * the internal event {@link EventCloseFile} or {@link EventCreateFileVersion}.
     * 
     * @param force
     *            If true, force the cleaning and do not respect the cleaning interval.
     */
    private void checkOpenFileTable(boolean force) {
        final long tPassed = TimeSync.getLocalSystemTime() - lastOFTcheck;
        timeToNextOFTclean = timeToNextOFTclean - tPassed;
        // Logging.logMessage(Logging.LEVEL_DEBUG,this,"time to next OFT:
        // "+timeToNextOFTclean);
        if (force || timeToNextOFTclean <= 0) {
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "OpenFileTable clean");
            
            long currentTime = TimeSync.getLocalSystemTime();
            
            // do OFT clean
            List<OpenFileTableEntry> closedFiles = oft.clean(currentTime);
            // Logging.logMessage(Logging.LEVEL_DEBUG,this,"closing
            // "+closedFiles.size()+" files");
            for (OpenFileTableEntry entry : closedFiles) {
                
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                        "send internal close event for %s, deleteOnClose=%b", entry.getFileId(), entry
                                .isDeleteOnClose());
                
                // Remove the cached capabilities.
                capCache.remove(entry.getFileId());
                
                // Send close event (creates a new file version if necessary).
                OSDOperation closeEvent = master.getInternalEvent(EventCloseFile.class);
                closeEvent.startInternalEvent(new Object[] { entry.getFileId(), entry.isDeleteOnClose(),
                        entry.getCowPolicy().cowEnabled(), entry.isWrite() });
                  
            }
            
            
            // Check if written files need to be versioned (copied on write). If the file has been already closed it
            // unnecessary to create another version because EventCloseFile already did.
            List<OpenFileTableEntry> closedWrittenFiles = oft.cleanWritten(currentTime);
            for (OpenFileTableEntry entry : closedWrittenFiles) {
                if (!entry.isClosed() && entry.isWrite()) {
                    entry.clearWrite();

                    OSDOperation createVersionEvent = master.getInternalEvent(EventCreateFileVersion.class);
                    createVersionEvent.startInternalEvent(new Object[] { entry.getFileId(),
                            metadataCache.getFileInfo(entry.getFileId()) });
                }
            }

            timeToNextOFTclean = OFT_CLEAN_INTERVAL;
        }
        lastOFTcheck = TimeSync.getLocalSystemTime();
    }
    
    @Override
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
        case STAGEOP_CLOSE_FILE:
            doClose(m);
            break;
        case STAGEOP_INVALIDATE_XLOC:
            doInvalidateXLocSet(m);
            break;
        case STAGEOP_UPDATE_XLOC:
            doUpdateXLocSetFromFlease(m);
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
        
        // check if the capability has valid arguments
        if (rqCap.getFileId().length() == 0) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "invalid capability. file_id must not be empty");
        }
        
        if (rqCap.getEpochNo() < 0) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "invalid capability. epoch must not be < 0");
        }
        
        if (ignoreCaps)
            return null;
        
        // check if the capability is valid
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
        
        // check if the capability was issued for the requested file
        if (!rqCap.getFileId().equals(rq.getFileId())) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, "capability was issued for another file than the one requested");
        }
        
        // check if the capability provides sufficient access rights for requested operation
        if (rq.getOperation().getProcedureId() == OSDServiceConstants.PROC_ID_READ) {
            
            if ((rqCap.getAccessMode() & (SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber())) != 0)
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES,
                        "capability does not allow read access to file " + rqCap.getFileId());
            
        } else if (rq.getOperation().getProcedureId() == OSDServiceConstants.PROC_ID_WRITE) {
            
            if ((rqCap.getAccessMode() & (SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                    | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber() | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR
                        .getNumber())) == 0)
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES,
                        "capability does not allow write access to file " + rqCap.getFileId());
            
        } else if (rq.getOperation().getProcedureId() == OSDServiceConstants.PROC_ID_TRUNCATE) {
            
            if ((rqCap.getAccessMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()) == 0)
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES,
                        "capability does not allow truncate access to file " + rqCap.getFileId());
            
        } else if (rq.getOperation().getProcedureId() == OSDServiceConstants.PROC_ID_UNLINK) {
            
            // TODO: replace numeric flag with constant
            if ((rqCap.getAccessMode() & 010000000) == 0)
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES,
                        "capability does not allow delete access to file " + rqCap.getFileId());
            
        }

        return null;
    }
    
    private ErrorResponse processValidateView(OSDRequest request) {
        String fileId = request.getFileId();
        if (fileId == null || fileId.length() == 0) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL,
                    "Invalid view. file_id must not be empty.");
        }

        XLocSetVersionState state;
        try {
            state = layout.getXLocSetVersionState(fileId);
        } catch (IOException e) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    "Invalid view. Local version could not be read.");
        }

        XLocations locset = request.getLocationList();
        if (state.getVersion() == locset.getVersion() && !state.getInvalidated()) {
            // The request is based on the same (valid) view.
            return null;
        } else if (locset.getVersion() > state.getVersion()) {
            XLocSetVersionState newstate = state.toBuilder()
                    .setInvalidated(false)
                    .setVersion(locset.getVersion())
                    .setModifiedTime(TimeSync.getGlobalTime())
                    .build();
            
            try {
                // Persist the view.
                layout.setXLocSetVersionState(fileId, newstate);
                // Inform flease about the new view.
                // TODO(jdillmann): Use centralized method to check if a lease is required.
                if (locset.getNumReplicas() > 1
                        && ReplicaUpdatePolicies.isRwReplicated(locset.getReplicaUpdatePolicy())) {
                    ASCIIString cellId = ReplicaUpdatePolicy.fileToCellId(fileId);
                    master.getRWReplicationStage().setFleaseView(fileId, cellId, newstate);
                }
            } catch (IOException e) {
                return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                        "Invalid view. Local version could not be written.");
            }

            // The request is valid, because it is based on a newer view.
            return null;
        }

        // The request is either based on an outdated view, or the replica is invalidated.
        String errorMessage = state.getInvalidated() ? "Replica is invalidated."
                : "The request is based on an outdated view" 
                        + "(" + locset.getVersion() + " < " + state.getVersion() + ").";
        return ErrorUtils.getErrorResponse(ErrorType.INVALID_VIEW, POSIXErrno.POSIX_ERROR_NONE, 
                "View is not valid. " + errorMessage);
    }

    /**
     * Process a viewIdChangeEvent from flease and update the persistent version/state
     */
    public void updateXLocSetFromFlease(ASCIIString cellId, int version) {
        enqueueOperation(STAGEOP_UPDATE_XLOC, new Object[] { cellId, version }, null, null);
    }

    private void doUpdateXLocSetFromFlease(StageRequest m) {
        final ASCIIString cellId = (ASCIIString) m.getArgs()[0];
        final int version = (Integer) m.getArgs()[1];
        final String fileId = ReplicaUpdatePolicy.cellToFileId(cellId);

        XLocSetVersionState state;
        try {
            state = layout.getXLocSetVersionState(fileId);
        } catch (IOException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                    "VersionState could not be read for fileId: %s", fileId);
            return;
        }

        // If a response from a newer View is encountered, we have to install it and leave the invalidated state.
        if (state.getVersion() < version) {
            state = state.toBuilder()
                        .setInvalidated(false)
                        .setVersion(version)
                        .setModifiedTime(TimeSync.getGlobalTime())
                        .build();
            try {
                // persist the version
                layout.setXLocSetVersionState(fileId, state);
                // and pass it back to flease
                master.getRWReplicationStage().setFleaseView(fileId, cellId, state);
            } catch (IOException e) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                        "VersionState could not be written for fileId: %s", fileId);
                return;
            }

        }

        // If the local version is greater then the one flease got from it responses, the other replicas have
        // to update their version. There exists no path to decrement the version if it has been seen once.
        return;
    }

    /**
     * Invalidate the current XLocSet. The replica will not respond to read/write/truncate or flease operations until a
     * new XLocSet is installed.<br>
     * If the request is based on a newer XLocSet, the local XLocSet version will be updated. If the request is from an
     * older one, an error is returned.
     */
    public void invalidateXLocSet(OSDRequest request, FileCredentials fileCreds, boolean validateView,
            InvalidateXLocSetCallback listener) {
        enqueueOperation(STAGEOP_INVALIDATE_XLOC, new Object[] { fileCreds, validateView }, request, listener);
    }

    private void doInvalidateXLocSet(StageRequest m) {
        final OSDRequest request = m.getRequest();
        final String fileId = request.getFileId();
        final XLocations xLoc = request.getLocationList();
        final FileCredentials fileCreds = (FileCredentials) m.getArgs()[0];
        final boolean validateView = (Boolean) m.getArgs()[1];
        final InvalidateXLocSetCallback callback = (InvalidateXLocSetCallback) m.getCallback();
        
        XLocSetVersionState state;
        try {
            XLocSetVersionState.Builder stateBuilder = layout.getXLocSetVersionState(fileId).toBuilder();
            
            // Return an error if the local version is newer then the requested one and the replica is not already
            // invalidated.
            if (validateView && !stateBuilder.getInvalidated() && stateBuilder.getVersion() > xLoc.getVersion()) {
                throw new InvalidXLocationsException("View is not valid. The requests is based on an outdated view.");
            }

            // Update the local version if the request is newer.
            if (stateBuilder.getVersion() < xLoc.getVersion()) {
                stateBuilder.setVersion(xLoc.getVersion());
            }
            
            // Invalidate the replica.
            stateBuilder.setInvalidated(true)
                        .setModifiedTime(TimeSync.getGlobalTime());

            state = stateBuilder.build();
            layout.setXLocSetVersionState(fileId, state);
            
            // TODO(jdillmann): Use centralized method to check if a lease is required.
            if (xLoc.getNumReplicas() > 1 && ReplicaUpdatePolicies.isRwReplicated(xLoc.getReplicaUpdatePolicy())) {
                master.getRWReplicationStage().invalidateReplica(fileId, fileCreds, xLoc, callback);
            } else {
                callback.invalidateComplete(LeaseState.NONE, null);
            }

        } catch (InvalidXLocationsException e) {
            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.INVALID_VIEW, POSIXErrno.POSIX_ERROR_NONE,
                    e.getMessage(), e);
            callback.invalidateComplete(LeaseState.NONE, error);
        } catch (IOException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                    "VersionState could not be written for fileId: %s", fileId);
            ErrorResponse error = ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    "Invalid view. Local version could not be written.");
            callback.invalidateComplete(LeaseState.NONE, error);
        }
    }

    public static interface InvalidateXLocSetCallback {
        public void invalidateComplete(LeaseState leaseState, ErrorResponse error);
    }

    public int getNumOpenFiles() {
        return oft.getNumOpenFiles();
    }
    
    public long getNumRequests() {
        return numRequests;
    }
    
}
