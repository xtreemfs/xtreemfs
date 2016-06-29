/*
 * Copyright (c) 2008-2015 by Bjoern Kolbeck, Christian Lorenz,
 *                            Jan Stender, Robert Bärhold,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.xtreemfs.common.quota.FinalizeVoucherResponseHelper;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.quota.OSDVoucherManager;
import org.xtreemfs.osd.quota.VoucherErrorException;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StorageStage.CachesFlushedCallback;
import org.xtreemfs.osd.stages.StorageStage.CreateFileVersionCallback;
import org.xtreemfs.osd.stages.StorageStage.DeleteObjectsCallback;
import org.xtreemfs.osd.stages.StorageStage.FinalizeVoucherCallback;
import org.xtreemfs.osd.stages.StorageStage.GetFileIDListCallback;
import org.xtreemfs.osd.stages.StorageStage.GetFileSizeCallback;
import org.xtreemfs.osd.stages.StorageStage.GetObjectListCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetGmaxCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetMaxObjectNoCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetReplicaStateCallback;
import org.xtreemfs.osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.osd.stages.StorageStage.TruncateCallback;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;
import org.xtreemfs.osd.storage.VersionTable.Version;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDFinalizeVouchersResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalGmax;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersion;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateLog;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateRecord;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_broadcast_gmaxRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public class StorageThread extends Stage {
    
    public static final int      STAGEOP_READ_OBJECT           = 1;
    
    public static final int      STAGEOP_WRITE_OBJECT          = 2;
    
    public static final int      STAGEOP_TRUNCATE              = 3;
    
    public static final int      STAGEOP_FLUSH_CACHES          = 4;
    
    public static final int      STAGEOP_GMAX_RECEIVED         = 5;
    
    public static final int      STAGEOP_GET_GMAX              = 6;
    
    public static final int      STAGEOP_GET_FILE_SIZE         = 7;
    
    public static final int      STAGEOP_GET_OBJECT_SET        = 8;
    
    public static final int      STAGEOP_INSERT_PADDING_OBJECT = 9;
    
    public static final int      STAGEOP_GET_MAX_OBJNO         = 10;
    
    public static final int      STAGEOP_CREATE_FILE_VERSION   = 11;
    
    public static final int      STAGEOP_GET_REPLICA_STATE     = 12;
    
    public static final int      STAGEOP_GET_FILEID_LIST       = 13;
    
    public static final int      STAGEOP_DELETE_OBJECTS        = 14;

    public static final int            STAGEOP_FINALIZE_VOUCHERS     = 15;

    private final MetadataCache        cache;

    private final StorageLayout        layout;
    
    private final OSDRequestDispatcher master;
    
    private final boolean        checksumsEnabled;
    
    public StorageThread(int id, OSDRequestDispatcher dispatcher, MetadataCache cache, StorageLayout layout,
        int maxQueueLength) {
        
        super("OSD StThr " + id, maxQueueLength);
        
        this.cache = cache;
        this.layout = layout;
        this.master = dispatcher;
        this.checksumsEnabled = master.getConfig().isUseChecksums();
    }
    
    @Override
    protected void processMethod(StageRequest method) {
        
        try {
            
            switch (method.getStageMethod()) {
            case STAGEOP_READ_OBJECT:
                processRead(method);
                break;
            case STAGEOP_WRITE_OBJECT:
                processWrite(method);
                break;
            case STAGEOP_TRUNCATE:
                processTruncate(method);
                break;
            case STAGEOP_FLUSH_CACHES:
                processFlushCaches(method);
                break;
            case STAGEOP_GMAX_RECEIVED:
                processGmax(method);
                break;
            case STAGEOP_GET_GMAX:
                processGetGmax(method);
                break;
            case STAGEOP_GET_FILE_SIZE:
                processGetFileSize(method);
                break;
            case STAGEOP_GET_OBJECT_SET:
                processGetObjectSet(method);
                break;
            case STAGEOP_INSERT_PADDING_OBJECT:
                processInsertPaddingObject(method);
                break;
            case STAGEOP_GET_MAX_OBJNO:
                processGetMaxObjNo(method);
                break;
            case STAGEOP_CREATE_FILE_VERSION:
                processCreateFileVersion(method);
                break;
            case STAGEOP_GET_REPLICA_STATE:
                processGetReplicaState(method);
                break;
            case STAGEOP_GET_FILEID_LIST:
                processGetFileIDList(method);
                break;
            case STAGEOP_DELETE_OBJECTS:
                processDeleteObjects(method);
                break;
            case STAGEOP_FINALIZE_VOUCHERS:
                processFinalizeVouchers(method);
                break;
            }
            
        } catch (Exception ex) {
            method.sendInternalServerError(ex);
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void processGetMaxObjNo(StageRequest rq) {
        final InternalGetMaxObjectNoCallback cback = (InternalGetMaxObjectNoCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            
            long currentMax = -1l;
            
            for (int i = 0; i < fi.getLastObjectNumber(); i++) {
                final long objVer = fi.getLargestObjectVersion(i);
                if (objVer > currentMax)
                    currentMax = objVer;
            }
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "getmaxobjno for fileId %s: %d",
                    fileId, currentMax);
            }
            
            cback.maxObjectNoCompleted(currentMax, fi.getFilesize(), fi.getTruncateEpoch(), null);
        } catch (Exception ex) {
            cback.maxObjectNoCompleted(0l, 0l, 0l, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
    }
    
    private void processGmax(StageRequest rq) {
        try {
            final String fileId = (String) rq.getArgs()[0];
            final long epoch = (Long) rq.getArgs()[1];
            final long lastObject = (Long) rq.getArgs()[2];
            
            FileMetadata fi = cache.getFileInfo(fileId);
            
            if (fi == null) {
                // file is not open, discard GMAX
                return;
            }
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "received new GMAX: %d/%d for %s", lastObject, epoch, fileId);
            
            if ((epoch == fi.getTruncateEpoch() && fi.getLastObjectNumber() < lastObject)
                || epoch > fi.getTruncateEpoch()) {
                
                // valid file size update
                fi.setGlobalLastObjectNumber(lastObject);
                
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "received GMAX is valid; for %s, current (fs, epoch) = (%d, %d)", fileId, fi
                                .getFilesize(), fi.getTruncateEpoch());
                
            } else {
                
                // outdated file size udpate
                
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "received GMAX is outdated; for %s, current (fs, epoch) = (%d, %d)", fileId, fi
                                .getFilesize(), fi.getTruncateEpoch());
            }
            
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            return;
        }
    }
    
    private void processFlushCaches(StageRequest rq) {
        final CachesFlushedCallback cback = (CachesFlushedCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            FileMetadata md = cache.removeFileInfo(fileId);
            if (md != null)
                layout.closeFile(null);
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "removed file info from cache for file %s", fileId);
            
            cback.cachesFlushed(null, md);
        } catch (Exception ex) {
            rq.sendInternalServerError(ex);
            return;
        }
    }
    
    private void processGetGmax(StageRequest rq) {
        final InternalGetGmaxCallback cback = (InternalGetGmaxCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
            final long snapTimestamp = (Long) rq.getArgs()[2];
            
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            // final boolean rangeRequested = (offset > 0) || (length <
            // stripeSize);
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "GET GMAX: %s", fileId);
            }
            
            long fileSize = -1;
            long lastObj = -1;
            
            // determine last object number and file size
            
            // in case of a previous file version, determine the last object +
            // file size from the version table / data on disk
            if (snapTimestamp > 0) {
                Version v = fi.getVersionTable().getLatestVersionBefore(snapTimestamp);
                lastObj = v.getObjCount() - 1;
                fileSize = v.getFileSize();
            }

            // in case of the current version, retrieve last object + file size
            // from the cached file metadata
            else {
                lastObj = fi.getLastObjectNumber();
                fileSize = fi.getFilesize();
            }
            
            InternalGmax gmax = InternalGmax.newBuilder().setEpoch(fi.getTruncateEpoch()).setFileSize(
                fileSize).setLastObjectId(lastObj).build();
            
            cback.gmaxComplete(gmax, null);
        } catch (IOException ex) {
            cback.gmaxComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                ex.toString()));
        }
        
    }
    
    private void processGetReplicaState(StageRequest rq) {
        final InternalGetReplicaStateCallback cback = (InternalGetReplicaStateCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
            final long remoteMaxObjVer = (Long) rq.getArgs()[2];

            // Do not assume that objects exist on the remote side based on the maxObjVer.
            // The reason for that is the remote side may not have seen all consecutive writes
            // up to maxObjVer. As an optimization, one could implement a marker which contains
            // the minimal version which was actually seen by all replicas. Until then,
            // remoteMaxObjVer must not be > 0.
            assert remoteMaxObjVer == 0 : "Received a request with remoteMaxObjVer != 0." +
            		" This probably means that you run OSDs with different versions." +
            		" Please update all OSDs to the same version.";
            
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            // final boolean rangeRequested = (offset > 0) || (length <
            // stripeSize);
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "GET replica state: %s, remote max: %d", fileId, remoteMaxObjVer);
            }
            
            ReplicaStatus.Builder result = ReplicaStatus.newBuilder();
            
            result.setFileSize(fi.getFilesize());
            result.setTruncateEpoch(fi.getTruncateEpoch());
            
            long localMaxObjVer = 0;
            for (Entry<Long, Long> e : fi.getLatestObjectVersions()) {
                if (e.getValue() > remoteMaxObjVer) {
                    result.addObjectVersions(ObjectVersion.newBuilder().setObjectNumber(e.getKey())
                            .setObjectVersion(e.getValue()));
                    if (e.getValue() > localMaxObjVer)
                        localMaxObjVer = e.getValue();
                }
            }
            result.setMaxObjVersion(localMaxObjVer);
            result.setPrimaryEpoch(0);
            result.setTruncateLog(layout.getTruncateLog(fileId));
            
            cback.getReplicaStateComplete(result.build(), null);
        } catch (IOException ex) {
            cback.getReplicaStateComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
        
    }
    
    /**
     * Reads an object from disk and checks the checksum
     * 
     * @param rq
     */
    private void processRead(StageRequest rq) {
        final ReadObjectCallback cback = (ReadObjectCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final long objNo = (Long) rq.getArgs()[1];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[2];
            final int offset = (Integer) rq.getArgs()[3];
            final int length = (Integer) rq.getArgs()[4];
            final long versionTimestamp = (Long) rq.getArgs()[5];
            
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            // final boolean rangeRequested = (offset > 0) || (length <
            // stripeSize);
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "READ: %s-%d offset=%d, length=%d", fileId, objNo, offset, length);
            }
            
            // if a snapshot is supposed to be read, read the corresponding
            // object version; otherwise, read the latest object version
            long objVer = versionTimestamp != 0 ? fi.getVersionTable().getLatestVersionBefore(
                versionTimestamp).getObjVersion(objNo) : fi.getLatestObjectVersion(objNo);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "getting objVer %d", objVer);
            }
            
            long objChksm = fi.getObjectChecksum(objNo, objVer);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "checksum is %d", objChksm);
            }
            
            ObjectInformation obj = layout.readObject(fileId, fi, objNo, offset, length, objVer);
            
            if (versionTimestamp != 0) {
                int lastObj = fi.getVersionTable().getLatestVersionBefore(versionTimestamp).getObjCount() - 1;
                obj.setLastLocalObjectNo(lastObj);
                obj.setGlobalLastObjectNo(lastObj);
            } else {
                obj.setLastLocalObjectNo(fi.getLastObjectNumber());
                obj.setGlobalLastObjectNo(fi.getGlobalLastObjectNumber());
            }
            
            cback.readComplete(obj, null);
        } catch (IOException ex) {
            cback.readComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                ex.toString()));
        }
        
    }
    
    /**
     * returns the OSDs view of a files size
     * 
     * @param rq
     */
    private void processGetFileSize(StageRequest rq) {
        final GetFileSizeCallback cback = (GetFileSizeCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
            final long snapTimestamp = (Long) rq.getArgs()[2];
            
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            // final boolean rangeRequested = (offset > 0) || (length <
            // stripeSize);
            
            // in case of a previous file version, determine
            // file size from the version table / data on disk
            if (snapTimestamp > 0) {
                Version v = fi.getVersionTable().getLatestVersionBefore(snapTimestamp);
                cback.getFileSizeComplete(v.getFileSize(), null);
            }

            else
                cback.getFileSizeComplete(fi.getFilesize(), null);
        } catch (IOException ex) {
            cback.getFileSizeComplete(-1, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
        
    }
    
    private void processInsertPaddingObject(StageRequest rq) {
        final WriteObjectCallback cback = (WriteObjectCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final long objNo = (Long) rq.getArgs()[1];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[2];
            final int size = (Integer) rq.getArgs()[3];
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            final long version = fi.getLatestObjectVersion(objNo) + 1;
            
            layout.createPaddingObject(fileId, fi, objNo, version, size);
            
            OSDWriteResponse response = OSDWriteResponse.newBuilder().build();
            cback.writeComplete(response, null);
            
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "Failed to create a padded object due to the following IOException:");
            Logging.logError(Logging.LEVEL_ERROR, this, ex);

            cback.writeComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
    }
    
    private void processWrite(StageRequest rq) {
        final WriteObjectCallback cback = (WriteObjectCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final long objNo = (Long) rq.getArgs()[1];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[2];
            int offset = (Integer) rq.getArgs()[3];
            final ReusableBuffer data = (ReusableBuffer) rq.getArgs()[4];
            final CowPolicy cow = (CowPolicy) rq.getArgs()[5];
            final XLocations xloc = (XLocations) rq.getArgs()[6];
            final boolean gMaxOff = (Boolean) rq.getArgs()[7];
            final boolean syncWrite = (Boolean) rq.getArgs()[8];
            // use only if != null
            final Long newVersionArg = (Long) rq.getArgs()[9];
            
            final int dataLength = data.remaining();
            final int stripeSize = sp.getStripeSizeForObject(objNo);
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "WRITE: %s-%d. last objNo=%d dataSize=%d at offset=%d", fileId, objNo, fi
                            .getLastObjectNumber(), dataLength, offset);
            }
            final int dataCapacity = data.capacity();
            if (offset + dataCapacity > stripeSize) {
                BufferPool.free(data);
                cback.writeComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                    POSIXErrno.POSIX_ERROR_EINVAL, "offset+data.length must be <= stripe size (offset="
                        + offset + " data.length=" + dataCapacity + " stripe size=" + stripeSize + ")"));
                return;
            }
            
            // check quota
            if (rq.getRequest() != null
                    && !master.getOsdVoucherManager().checkMaxVoucherSize(fileId,
                    rq.getRequest().getCapability().getClientIdentity(), rq.getRequest().getCapability().getExpireMs(),
                    sp.getObjectStartOffset(objNo) + offset + dataCapacity)) {
                BufferPool.free(data);
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "Stop WRITE due to an insufficient voucher!", fileId, objNo, fi.getLastObjectNumber(),
                        dataLength, offset);
                cback.writeComplete(null, master.getOsdVoucherManager().getInsufficientVoucherErrorResponse());
                return;
            }

            // assign the number of objects to the COW policy if necessary (this
            // is e.g. needed for the COW_ONCE policy)
            cow.initCowFlagsIfRequired(fi.getLastObjectNumber() + 1);
            
            // determine the largest known version of the object
            long largestV = fi.getLargestObjectVersion(objNo);
            
            // determine the object version to write
            final boolean isCow = cow.isCOW((int) objNo);
            
            long newVersion = (isCow || checksumsEnabled) ? largestV + 1 : Math.max(1, largestV);
            if (newVersionArg != null) {
                // new version passed via arg always prevails
                newVersion = newVersionArg;
            }
            assert (data != null);
            
            // make sure last object is set correctly!
            if (objNo > fi.getLastObjectNumber()) {
                fi.setLastObjectNumber(objNo);
            }
            
            layout.writeObject(fileId, fi, data, objNo, offset, newVersion, syncWrite, isCow);
            
            // if a new version was created, update the "latest versions" file
            if (cow.cowEnabled() && (isCow || largestV == 0))
                layout.updateCurrentObjVersion(fileId, objNo, newVersion);
            
            if (isCow)
                cow.objectChanged((int) objNo);
            
            OSDWriteResponse.Builder response = OSDWriteResponse.newBuilder();
            
            // if the write refers to the last known object or to an object
            // beyond, i.e. the file size and globalMax are potentially
            // affected:
            if (objNo >= fi.getLastObjectNumber()) {
                
                long newObjSize = dataLength + offset;
                
                // calculate new filesize...
                long newFS = 0;
                if (objNo > 0) {
                    newFS = sp.getObjectEndOffset(objNo - 1) + 1 + newObjSize;
                } else {
                    newFS = newObjSize;
                }
                if (newFS < fi.getFilesize()) {
                    newFS = fi.getFilesize();
                }
                
                // check whether the file size might have changed; in this case,
                // ensure that the X-New-Filesize header will be set
                if (newFS > fi.getFilesize() && objNo >= fi.getLastObjectNumber()
                    && objNo >= fi.getGlobalLastObjectNumber()) {
                    // Metadata meta = info.getMetadata();
                    // meta.putKnownSize(newFS);
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "new filesize: %d",
                            newFS);
                    response.setSizeInBytes(newFS);
                    response.setTruncateEpoch((int) fi.getTruncateEpoch());
                } else {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                            "no new filesize: %d/%d, %d/%d", newFS, fi.getFilesize(), fi
                                    .getLastObjectNumber(), objNo);
                }
                
                // update file size and last object number
                fi.setFilesize(newFS);
                
                // if the written object has a larger ID than the largest
                // locally-known object of the file, send 'globalMax' messages
                // to all other OSDs and update local globalMax
                if (objNo > fi.getLastObjectNumber() && !gMaxOff) {
                    if (objNo > fi.getGlobalLastObjectNumber()) {
                        // send UDP packets...
                        final List<ServiceUUID> osds = xloc.getLocalReplica().getOSDs();
                        final ServiceUUID localUUID = master.getConfig().getUUID();
                        if (osds.size() > 1) {
                            
                            RPCHeader.RequestHeader rqHdr = RPCHeader.RequestHeader.newBuilder().setAuthData(
                                RPCAuthentication.authNone).setUserCreds(RPCAuthentication.userService)
                                    .setInterfaceId(OSDServiceConstants.INTERFACE_ID).setProcId(
                                        OSDServiceConstants.PROC_ID_XTREEMFS_BROADCAST_GMAX).build();
                            RPCHeader header = RPCHeader.newBuilder().setCallId(0).setMessageType(
                                MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();
                            xtreemfs_broadcast_gmaxRequest gmaxRq = xtreemfs_broadcast_gmaxRequest
                                    .newBuilder().setFileId(fileId).setTruncateEpoch(fi.getTruncateEpoch())
                                    .setLastObject(objNo).build();
                            
                            for (ServiceUUID osd : osds) {
                                if (!osd.equals(localUUID)) {
                                    master.sendUDPMessage(header, gmaxRq, osd.getAddress());
                                }
                            }
                        }
                    }
                }
            }
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "new last object=%d gmax=%d", fi
                        .getLastObjectNumber(), fi.getGlobalLastObjectNumber());
            // BufferPool.free(data);
            cback.writeComplete(response.build(), null);
            
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this, "Failed to process write() request due to the following IOException:");
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            
            cback.writeComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        } catch (VoucherErrorException ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                        "Failed to process write() request due to the following VoucherErrorException:");
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }

            cback.writeComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, ex.toString(), ex));
        }
    }

    private void processDeleteObjects(StageRequest rq) throws IOException {

        final DeleteObjectsCallback cback = (DeleteObjectsCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
            final long epochNumber = (Long) rq.getArgs()[2];
            final Map<Long,Long> objectsToBeDeleted = (Map<Long,Long>) rq.getArgs()[3];

            final FileMetadata fi = layout.getFileMetadata(sp, fileId);

            // Delete objects.
            for (Entry<Long,Long> obj : objectsToBeDeleted.entrySet()) {
                layout.deleteObject(fileId, fi, obj.getKey(), obj.getValue());
            }
            layout.setTruncateEpoch(fileId, epochNumber);

            // Remove file info from cache to make sure it is reloaded with the next operation.
            cache.removeFileInfo(fileId);
            cback.deleteObjectsComplete(null);
        } catch (Exception ex) {
            cback.deleteObjectsComplete(ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
    }
    
    private void processTruncate(StageRequest rq) throws IOException {
        
        final TruncateCallback cback = (TruncateCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final long newFileSize = (Long) rq.getArgs()[1];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[2];
            final Replica currentReplica = (Replica) rq.getArgs()[3];
            final long epochNumber = (Long) rq.getArgs()[4];
            final CowPolicy cow = (CowPolicy) rq.getArgs()[5];
            final Long newObjVer = (Long) rq.getArgs()[6];
            final Boolean createTLogEntry = (Boolean) rq.getArgs()[7];
            
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            
            if (fi.getTruncateEpoch() >= epochNumber) {
                cback.truncateComplete(
                        OSDWriteResponse.newBuilder().setSizeInBytes(fi.getFilesize())
                                .setTruncateEpoch((int) fi.getTruncateEpoch()).build(), null);
                /*
                 * cback.truncateComplete(null, new
                 * OSDException(ErrorCodes.EPOCH_OUTDATED,
                 * "invalid truncate epoch for file " + fileId + ": " +
                 * epochNumber + ", current one is " + fi.getTruncateEpoch(),
                 * ""));
                 */
                return;
            }
            
            // assign the number of objects to the COW policy if necessary (this
            // is e.g. needed for the COW_ONCE policy)
            cow.initCowFlagsIfRequired(fi.getLastObjectNumber() + 1);
            
            // find the offset of the local OSD in the current replica's
            // locations list
            // FIXME: unify OSD IDs
            final int relativeOSDNumber = currentReplica.getOSDs().indexOf(master.getConfig().getUUID());
            
            long newLastObject = -1;
            long newGlobalLastObject = -1;
            
            if (newFileSize == 0) {
                // truncate file to zero length
                
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "truncate to 0");
                
                // if copy-on-write is enabled ...
                if (cow.cowEnabled()) {
                    
                    // only delete those objects that make up the latest
                    // version of the file and are not part of former file
                    // versions
                    
                    for (Entry<Long, Long> entry : fi.getLatestObjectVersions()) {
                        
                        long objNo = entry.getKey();
                        long objVer = entry.getValue();
                        
                        if (!fi.getVersionTable().isContained(objNo, objVer))
                            layout.deleteObject(fileId, fi, objNo, objVer);
                    }
                }

                // otherwise ...
                else
                    // delete all objects of the file (but not the metadata)
                    layout.deleteFile(fileId, false);
                
                fi.clearLatestObjectVersions();
                
            } else if (fi.getFilesize() > newFileSize) {
                // shrink file
                newLastObject = truncateShrink(fileId, newFileSize, epochNumber, sp, fi, relativeOSDNumber,
                    cow, newObjVer);
                newGlobalLastObject = sp.getObjectNoForOffset(newFileSize - 1);
            } else if (fi.getFilesize() < newFileSize) {
                // check quota
                if (rq.getRequest() != null
                        && !master.getOsdVoucherManager().checkMaxVoucherSize(fileId,
                        rq.getRequest().getCapability().getClientIdentity(),
                        rq.getRequest().getCapability().getExpireMs(), newFileSize)) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                                "Insufficient voucher for an extending truncate");
                    }

                    cback.truncateComplete(null, master.getOsdVoucherManager().getInsufficientVoucherErrorResponse());
                    return;
                }

                // extend file
                newLastObject = truncateExtend(fileId, newFileSize, epochNumber, sp, fi, relativeOSDNumber,
                    cow, newObjVer);
                newGlobalLastObject = sp.getObjectNoForOffset(newFileSize - 1);
            } else if (fi.getFilesize() == newFileSize) {
                // file size remains unchanged
                
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "truncate to local size: %d", newFileSize);
                
                newLastObject = fi.getLastObjectNumber();
                newGlobalLastObject = fi.getGlobalLastObjectNumber();
            }
            
            // set the new file size and last object number
            fi.setFilesize(newFileSize);
            fi.setLastObjectNumber(newLastObject);
            fi.setTruncateEpoch(epochNumber);
            
            fi.setGlobalLastObjectNumber(newGlobalLastObject);
            
            // store the truncate epoch persistently
            layout.setTruncateEpoch(fileId, epochNumber);
            
            // if copy-on-write is enabled, set the new object count for the
            // latest version
            if (cow.cowEnabled())
                layout.updateCurrentVersionSize(fileId, newLastObject);

            if (createTLogEntry) {
                TruncateLog log = layout.getTruncateLog(fileId);
                log = log.toBuilder().addRecords(TruncateRecord.newBuilder().setVersion(newObjVer).setLastObjectNumber(newLastObject)).build();
                layout.setTruncateLog(fileId, log);
            }
            
            // append the new file size and epoch number to the response
            OSDWriteResponse response = OSDWriteResponse.newBuilder().setSizeInBytes(newFileSize)
                    .setTruncateEpoch((int) epochNumber).build();
            cback.truncateComplete(response, null);
            
        } catch (VoucherErrorException ex) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.storage, this,
                        "Failed to process write() request due to the following VoucherErrorException:");
                Logging.logError(Logging.LEVEL_DEBUG, this, ex);
            }

            cback.truncateComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EACCES, ex.toString(), ex));
        } catch (Exception ex) {
            cback.truncateComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
        
    }

    /**
     * @param method
     */
    private void processFinalizeVouchers(StageRequest rq) {

        final FinalizeVoucherCallback cback = (FinalizeVoucherCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final String clientId = (String) rq.getArgs()[1];
        final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[2];
        @SuppressWarnings("unchecked")
        final Set<Long> expireTimeSet = (Set<Long>) rq.getArgs()[3];

        OSDVoucherManager osdVoucherManager = master.getOsdVoucherManager();
        try {
            osdVoucherManager.invalidateFileVouchers(fileId, clientId, expireTimeSet);

            FileMetadata fileMetadata = layout.getFileMetadata(sp, fileId);
            String uuid = master.getConfig().getUUID().toString();

            FinalizeVoucherResponseHelper helper = new FinalizeVoucherResponseHelper(master.getConfig()
                    .getCapabilitySecret());
            String signature = helper.createSignature(uuid, fileMetadata.getFilesize(),
                    fileMetadata.getTruncateEpoch(), expireTimeSet);

            OSDFinalizeVouchersResponse response = OSDFinalizeVouchersResponse.newBuilder().setOsdUuid(uuid)
                    .setServerSignature(signature).setSizeInBytes(fileMetadata.getFilesize())
                    .setTruncateEpoch(fileMetadata.getTruncateEpoch()).build();

            cback.finalizeVoucherComplete(response, null);
        } catch (Exception ex) {
            cback.finalizeVoucherComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }

    }

    private void processGetObjectSet(StageRequest rq) {
        final GetObjectListCallback cback = (GetObjectListCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
        
        try {
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            ObjectSet objectSet = layout.getObjectSet(fileId, fi);
            cback.getObjectSetComplete(objectSet, null);
        } catch (Exception ex) {
            cback.getObjectSetComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
    }

    private void processCreateFileVersion(StageRequest rq) {
        
        final CreateFileVersionCallback cback = (CreateFileVersionCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        
        FileMetadata fi = (FileMetadata) rq.getArgs()[1];
        
        try {
            
            if (fi == null)
                fi = layout.getFileMetadataNoCaching(null, fileId);
            
            Set<Entry<Long, Long>> objVersions = fi.getLatestObjectVersions();
            long fileSize = fi.getFilesize();
            
            // convert the set of object versions into an array
            
            // first, determine the last object
            long maxKey = -1;
            for (Entry<Long, Long> ver : objVersions) {
                if (ver.getKey() > maxKey)
                    maxKey = ver.getKey();
            }
            
            // instantiate a sufficiently large array
            assert (maxKey < Integer.MAX_VALUE);
            int[] versions = new int[(int) maxKey + 1];
            
            // set all object versions in the array
            for (Entry<Long, Long> ver : objVersions)
                versions[ver.getKey().intValue()] = ver.getValue().intValue();
            
            // create and save the new version
            fi.getVersionTable().addVersion(TimeSync.getGlobalTime(), versions, fileSize);
            try {
                fi.getVersionTable().save();
            } catch (IOException e) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, OutputUtils
                        .stackTraceToString(e));
            }
            
            cback.createFileVersionComplete(fileSize, null);
            
        } catch (Exception ex) {
            
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            cback.createFileVersionComplete(0, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
        
    }
    
    private void processGetFileIDList(StageRequest rq) {
        final GetFileIDListCallback cback = (GetFileIDListCallback) rq.getCallback();
        ArrayList<String> fileIDList = null;
        try {
            
            if (layout != null) {
                fileIDList = layout.getFileIDList();
            }
            
            cback.createGetFileIDListComplete(fileIDList, null);
            
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            cback.createGetFileIDListComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO,
                POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
            
        }
    }
    
    private long truncateShrink(String fileId, long fileSize, long epoch, StripingPolicyImpl sp,
        FileMetadata fi, int relOsdId, CowPolicy cow, Long newObjVer) throws IOException {
        // first find out which is the new "last object"
        final long newLastObject = sp.getObjectNoForOffset(fileSize - 1);
        final long oldLastObject = fi.getLastObjectNumber();
        assert (newLastObject <= oldLastObject) : "new= " + newLastObject + " old=" + oldLastObject;
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                "truncate shrink to: %d old last: %d   new last: %d", fileSize, fi.getLastObjectNumber(),
                newLastObject);
        
        // if copy-on-write enabled ...
        if (cow.cowEnabled()) {
            
            // only remove objects that are no longer bound to former file
            // versions
            final long oldRow = sp.getRow(oldLastObject);
            final long lastRow = sp.getRow(newLastObject);
            
            for (long r = oldRow; r >= lastRow; r--) {
                
                final long rowObj = r * sp.getWidth() + relOsdId;
                
                if (rowObj == newLastObject) {
                    // currently examined object is new last object and local:
                    // shrink it
                    final int newObjSize = (int) (fileSize - sp.getObjectStartOffset(newLastObject));
                    truncateObject(fileId, newLastObject, sp, newObjSize, relOsdId, cow, newObjVer);
                    
                } else if (rowObj > newLastObject) {
                    
                    // currently examined object is larger than new last object
                    // and not contained in any previous version of the file:
                    // delete it
                    final long v = fi.getLatestObjectVersion(rowObj);
                    if (!fi.getVersionTable().isContained(rowObj, v))
                        layout.deleteObject(fileId, fi, rowObj, v);
                    
                    fi.discardObject(rowObj, v);
                }
            }
            
        }

        // otherwise ...
        else {
            
            // remove all unnecessary objects
            final long oldRow = sp.getRow(oldLastObject);
            final long lastRow = sp.getRow(newLastObject);
            
            for (long r = oldRow; r >= lastRow; r--) {
                final long rowObj = r * sp.getWidth() + relOsdId;
                
                if (rowObj == newLastObject) {
                    // currently examined object is new last object and local:
                    // shrink it
                    final int newObjSize = (int) (fileSize - sp.getObjectStartOffset(newLastObject));
                    truncateObject(fileId, newLastObject, sp, newObjSize, relOsdId, cow, newObjVer);
                    
                } else if (rowObj > newLastObject) {
                    
                    // currently examined object is larger than new last object:
                    // delete it
                    final long v = fi.getLatestObjectVersion(rowObj);
                    layout.deleteObject(fileId, fi, rowObj, v);
                    
                    fi.discardObject(rowObj, v);
                }
            }
            
        }
        
        // make sure that new last object exists
        for (long obj = newLastObject - 1; obj > newLastObject - sp.getWidth(); obj--) {
            if (obj > 0 && sp.isLocalObject(obj, relOsdId)) {
                long v = fi.getLatestObjectVersion(obj);
                if (v == 0) {
                    // does not exist
                    createPaddingObject(fileId, obj, sp, fi.getLargestObjectVersion(obj) + 1, sp
                            .getStripeSizeForObject(obj), fi);
                }
            }
        }
        
        return newLastObject;
    }
    
    private long truncateExtend(String fileId, long fileSize, long epoch, StripingPolicyImpl sp,
        FileMetadata fi, int relOsdId, CowPolicy cow, Long newObjVer) throws IOException {
        // first find out which is the new "last object"
        final long newLastObject = sp.getObjectNoForOffset(fileSize - 1);
        final long oldLastObject = fi.getLastObjectNumber();
        assert (newLastObject >= oldLastObject);
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                "truncate extend to: %d old last: %d   new last: %d", fileSize, oldLastObject, newLastObject);
        
        // if no objects need to be added and the last object is stored locally
        // ...
        if ((sp.getOSDforObject(newLastObject) == relOsdId) && newLastObject == oldLastObject) {
            // ... simply extend the old one
            truncateObject(fileId, newLastObject, sp, (int) (fileSize - sp
                    .getObjectStartOffset(newLastObject)), relOsdId, cow, newObjVer);
        }

        // otherwise ...
        else {
            
            // extend the old last object to full object size
            if ((oldLastObject > -1) && (sp.isLocalObject(oldLastObject, relOsdId))) {
                truncateObject(fileId, oldLastObject, sp, sp.getStripeSizeForObject(oldLastObject), relOsdId,
                    cow, newObjVer);
            }
            
            // if the new last object is a local object, create a padding
            // object to ensure that it exists
            if (sp.isLocalObject(newLastObject, relOsdId)) {
                
                long version = newObjVer != null ? newObjVer : (cow.isCOW((int) newLastObject) ? fi
                        .getLargestObjectVersion(newLastObject) + 1 : Math.max(1, fi
                        .getLargestObjectVersion(newLastObject)));
                int objSize = (int) (fileSize - sp.getObjectStartOffset(newLastObject));
                
                createPaddingObject(fileId, newLastObject, sp, version, objSize, fi);
            }
            
            // make sure that new last objects also exist on all other OSDs
            for (long obj = newLastObject - 1; obj > newLastObject - sp.getWidth(); obj--) {
                if (obj > 0 && sp.isLocalObject(obj, relOsdId)) {
                    long v = fi.getLatestObjectVersion(obj);
                    if (v == 0) {
                        // does not exist
                        final boolean isCow = cow.isCOW((int) obj);
                        final long newVersion = newObjVer != null ? newObjVer : (isCow ? fi
                                .getLargestObjectVersion(obj) + 1 : Math.max(1, fi
                                .getLargestObjectVersion(obj)));
                        createPaddingObject(fileId, obj, sp, newVersion, sp.getStripeSizeForObject(obj), fi);
                    }
                }
            }
            
        }
        
        return newLastObject;
    }
    
    private void truncateObject(String fileId, long objNo, StripingPolicyImpl sp, int newSize, long relOsdId,
        CowPolicy cow, Long newObjVer) throws IOException {
        
        assert (newSize > 0) : "new size is " + newSize + " but should be > 0";
        assert (newSize <= sp.getStripeSizeForObject(objNo));
        assert (objNo >= 0) : "objNo is " + objNo;
        assert (sp.getOSDforObject(objNo) == relOsdId);
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "truncate object to %d", newSize);
        
        final FileMetadata fi = layout.getFileMetadata(sp, fileId);
        final boolean isCow = cow.isCOW((int) objNo);
        final long newVersion = newObjVer != null ? newObjVer
            : (isCow ? fi.getLargestObjectVersion(objNo) + 1 : Math.max(1, fi.getLatestObjectVersion(objNo)));
        
        layout.truncateObject(fileId, fi, objNo, newSize, newVersion, isCow);
        
    }
    
    private void createPaddingObject(String fileId, long objNo, StripingPolicyImpl sp, long version,
        int size, FileMetadata fi) throws IOException {
        layout.createPaddingObject(fileId, fi, objNo, version, size);
    }
}
