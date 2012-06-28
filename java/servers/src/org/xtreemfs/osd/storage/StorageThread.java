/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Christian Lorenz,
 *                            Jan Stender,
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
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StorageStage.CachesFlushedCallback;
import org.xtreemfs.osd.stages.StorageStage.CreateFileVersionCallback;
import org.xtreemfs.osd.stages.StorageStage.DeleteObjectsCallback;
import org.xtreemfs.osd.stages.StorageStage.GetFileIDListCallback;
import org.xtreemfs.osd.stages.StorageStage.GetFileSizeCallback;
import org.xtreemfs.osd.stages.StorageStage.GetObjectListCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetGmaxCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetMaxObjectNoCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetReplicaStateCallback;
import org.xtreemfs.osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.osd.stages.StorageStage.TruncateCallback;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;
import org.xtreemfs.osd.storage.FileVersionLog.FileVersion;
import org.xtreemfs.osd.storage.VersionManager.ObjectVersionInfo;
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

    private MetadataCache        cache;

    private StorageLayout        layout;

    private OSDRequestDispatcher master;

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
            }

        } catch (Throwable th) {
            method.sendInternalServerError(th);
            Logging.logError(Logging.LEVEL_ERROR, this, th);
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
                final long objVer = fi.getVersionManager().getLargestObjectVersion(i).version;
                if (objVer > currentMax)
                    currentMax = objVer;
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "getmaxobjno for fileId %s: %d", fileId,
                        currentMax);
            }

            cback.maxObjectNoCompleted(currentMax, fi.getFilesize(), fi.getTruncateEpoch(), null);
        } catch (Exception ex) {
            cback.maxObjectNoCompleted(0l, 0l, 0l,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
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
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "received new GMAX: %d/%d for %s",
                        lastObject, epoch, fileId);

            if ((epoch == fi.getTruncateEpoch() && fi.getLastObjectNumber() < lastObject)
                    || epoch > fi.getTruncateEpoch()) {

                // valid file size update
                fi.setGlobalLastObjectNumber(lastObject);

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                            "received GMAX is valid; for %s, current (fs, epoch) = (%d, %d)", fileId, fi.getFilesize(),
                            fi.getTruncateEpoch());

            } else {

                // outdated file size udpate

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                            "received GMAX is outdated; for %s, current (fs, epoch) = (%d, %d)", fileId,
                            fi.getFilesize(), fi.getTruncateEpoch());
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
                layout.closeFile(md);

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
                FileVersion fv = fi.getVersionManager().getLatestFileVersionBefore(snapTimestamp);
                lastObj = fv.getNumObjects() - 1;
                fileSize = fv.getFileSize();
            }

            // in case of the current version, retrieve last object + file size
            // from the cached file metadata
            else {
                lastObj = fi.getLastObjectNumber();
                fileSize = fi.getFilesize();
            }

            InternalGmax gmax = InternalGmax.newBuilder().setEpoch(fi.getTruncateEpoch()).setFileSize(fileSize)
                    .setLastObjectId(lastObj).build();

            cback.gmaxComplete(gmax, null);
        } catch (IOException ex) {
            cback.gmaxComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }

    }

    private void processGetReplicaState(StageRequest rq) {

        final InternalGetReplicaStateCallback cback = (InternalGetReplicaStateCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
            final long remoteMaxObjVer = (Long) rq.getArgs()[2];

            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            // final boolean rangeRequested = (offset > 0) || (length <
            // stripeSize);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "GET replica state: %s, remote max: %d",
                        fileId, remoteMaxObjVer);
            }

            ReplicaStatus.Builder result = ReplicaStatus.newBuilder();

            result.setFileSize(fi.getFilesize());
            result.setTruncateEpoch(fi.getTruncateEpoch());

            long localMaxObjVer = 0;

            // determine the largest overall object version number in the file, which reflects the locally
            // stored version of the file content
            for (int objNo = 0; objNo <= fi.getLastObjectNumber(); objNo++) {
                ObjectVersionInfo v = fi.getVersionManager().getLargestObjectVersion(objNo);
                if (v.version > remoteMaxObjVer) {
                    result.addObjectVersions(ObjectVersion.newBuilder().setObjectNumber(objNo)
                            .setObjectVersion(v.version));
                    if (v.version > localMaxObjVer)
                        localMaxObjVer = v.version;
                }
            }

            result.setMaxObjVersion(localMaxObjVer);
            result.setPrimaryEpoch(0);
            result.setTruncateLog(layout.getTruncateLog(fileId));

            cback.getReplicaStateComplete(result.build(), null);
        } catch (IOException ex) {
            cback.getReplicaStateComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
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
            final long snapTimestamp = (Long) rq.getArgs()[5];

            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            // final boolean rangeRequested = (offset > 0) || (length <
            // stripeSize);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "READ: %s-%d offset=%d, length=%d",
                        fileId, objNo, offset, length);
            }

            VersionManager vm = fi.getVersionManager();

            // determine relevant version info
            ObjectVersionInfo objVer = ObjectVersionInfo.MISSING;

            // if current version is supposed to be read ...
            if (snapTimestamp == 0) {
                if (vm.isVersioningEnabled())
                    objVer = vm.getLatestObjectVersionBefore(objNo, Long.MAX_VALUE, fi.getLastObjectNumber() + 1);
                else
                    objVer = vm.getLargestObjectVersion(objNo);
            }

            // if old version is supposed to be read ...
            else {
                if (vm.isVersioningEnabled())
                    objVer = vm.getLatestObjectVersionBefore(objNo, snapTimestamp, fi.getLastObjectNumber() + 1);
                else
                    ; // return 'missing' for robustness if an attempt is made to read a version of an
                      // unversioned file
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "getting objVer %s", objVer);
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "checksum is %d", objVer.checksum);
            }

            ObjectInformation obj = layout.readObject(fileId, fi, objNo, offset, length, objVer);

            if (vm.isVersioningEnabled() && snapTimestamp != 0) {
                int lastObj = (int) vm.getLatestFileVersionBefore(snapTimestamp).getNumObjects() - 1;
                obj.setLastLocalObjectNo(lastObj);
                obj.setGlobalLastObjectNo(lastObj);
            } else {
                obj.setLastLocalObjectNo(fi.getLastObjectNumber());
                obj.setGlobalLastObjectNo(fi.getGlobalLastObjectNumber());
            }

            cback.readComplete(obj, null);
        } catch (IOException ex) {
            cback.readComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }

    }

    /**
     * Reads an object from disk and checks the checksum
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
                FileVersion v = fi.getVersionManager().getLatestFileVersionBefore(snapTimestamp);
                cback.getFileSizeComplete(v.getFileSize(), null);
            }

            else
                cback.getFileSizeComplete(fi.getFilesize(), null);
        } catch (IOException ex) {
            cback.getFileSizeComplete(-1,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
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
            final long version = fi.getVersionManager().getLargestObjectVersion(objNo).version + 1;

            layout.createPaddingObject(fileId, fi, objNo, version, TimeSync.getGlobalTime(), size);

            OSDWriteResponse response = OSDWriteResponse.newBuilder().build();
            cback.writeComplete(response, null);

        } catch (IOException ex) {
            ex.printStackTrace();
            cback.writeComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
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
            final Long receivedServerTimestamp = (Long) rq.getArgs()[10];

            final int dataLength = data.remaining();
            final int stripeSize = sp.getStripeSizeForObject(objNo);
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "WRITE: %s-%d. last objNo=%d dataSize=%d at offset=%d", fileId, objNo,
                        fi.getLastObjectNumber(), dataLength, offset);
            }
            final int dataCapacity = data.capacity();
            if (offset + dataCapacity > stripeSize) {
                BufferPool.free(data);
                cback.writeComplete(null, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL,
                        "offset+data.length must be <= stripe size (offset=" + offset + " data.length=" + dataCapacity
                                + " stripe size=" + stripeSize + ")"));
                return;
            }

            // determine the metadata of the largest object version
            ObjectVersionInfo versionInfo = fi.getVersionManager().getLargestObjectVersion(objNo);

            // if COW is enabled, check if the received server timestamp is in the future and wait if
            // necessary, so as to ensure snapshot consistency
            long newTimestamp = cow.cowEnabled() ? ensureSnapshotConsistency(receivedServerTimestamp) : -1;

            long newVersion = Math.max(1, versionInfo.version);
            if (newVersionArg != null) {
                // new version passed via arg always prevails
                newVersion = newVersionArg;
            }
            assert (data != null);

            // assign the number of objects to the COW policy if necessary (this
            // is e.g. needed for the COW_ONCE policy)
            cow.initCowFlagsIfRequired(fi.getLastObjectNumber() + 1);

            // write the object
            layout.writeObject(fileId, fi, data, objNo, offset, newVersion, newTimestamp, syncWrite, cow);

            // make sure last object is set correctly!
            if (objNo > fi.getLastObjectNumber())
                fi.setLastObjectNumber(objNo);

            OSDWriteResponse.Builder response = OSDWriteResponse.newBuilder();

            // if the write refers to the last known object or to an object
            // beyond, i.e. the file size and globalMax are potentially
            // affected:
            if (objNo >= fi.getLastObjectNumber() && !gMaxOff) {

                long newObjSize = dataLength + offset;

                // calculate new filesize...
                long newFS = 0;
                if (objNo > 0) {
                    newFS = sp.getObjectEndOffset(objNo - 1) + 1 + newObjSize;
                } else {
                    newFS = newObjSize;
                }

                // check whether the file size might have changed; in this case,
                // ensure that the X-New-Filesize header will be set
                if (newFS > fi.getFilesize() && objNo >= fi.getLastObjectNumber()
                        && objNo >= fi.getGlobalLastObjectNumber()) {
                    // Metadata meta = info.getMetadata();
                    // meta.putKnownSize(newFS);
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "new filesize: %d", newFS);
                    response.setSizeInBytes(newFS);
                    response.setTruncateEpoch((int) fi.getTruncateEpoch());
                } else {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "no new filesize: %d/%d, %d/%d",
                                newFS, fi.getFilesize(), fi.getLastObjectNumber(), objNo);
                }

                // update file size and last object number
                fi.setFilesize(newFS);

                // if the written object has a larger ID than the largest
                // locally-known object of the file, send 'globalMax' messages
                // to all other OSDs and update local globalMax
                if (objNo > fi.getLastObjectNumber()) {
                    if (objNo > fi.getGlobalLastObjectNumber()) {
                        // send UDP packets...
                        final List<ServiceUUID> osds = xloc.getLocalReplica().getOSDs();
                        final ServiceUUID localUUID = master.getConfig().getUUID();
                        if (osds.size() > 1) {

                            RPCHeader.RequestHeader rqHdr = RPCHeader.RequestHeader.newBuilder()
                                    .setAuthData(RPCAuthentication.authNone)
                                    .setUserCreds(RPCAuthentication.userService)
                                    .setInterfaceId(OSDServiceConstants.INTERFACE_ID)
                                    .setProcId(OSDServiceConstants.PROC_ID_XTREEMFS_BROADCAST_GMAX).build();
                            RPCHeader header = RPCHeader.newBuilder().setCallId(0)
                                    .setMessageType(MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();
                            xtreemfs_broadcast_gmaxRequest gmaxRq = xtreemfs_broadcast_gmaxRequest.newBuilder()
                                    .setFileId(fileId).setTruncateEpoch(fi.getTruncateEpoch()).setLastObject(objNo)
                                    .build();

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
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "new last object=%d gmax=%d",
                        fi.getLastObjectNumber(), fi.getGlobalLastObjectNumber());
            // BufferPool.free(data);
            response.setServerTimestamp(newTimestamp);
            cback.writeComplete(response.build(), null);

        } catch (IOException ex) {
            ex.printStackTrace();
            cback.writeComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
    }

    private void processDeleteObjects(StageRequest rq) throws IOException {

        final DeleteObjectsCallback cback = (DeleteObjectsCallback) rq.getCallback();
        try {
            final String fileId = (String) rq.getArgs()[0];
            final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];
            final long epochNumber = (Long) rq.getArgs()[2];
            final Map<Long, ObjectVersionInfo> objectsToBeDeleted = (Map<Long, ObjectVersionInfo>) rq.getArgs()[3];

            final FileMetadata fi = layout.getFileMetadata(sp, fileId);

            // Delete objects.
            for (Entry<Long, ObjectVersionInfo> obj : objectsToBeDeleted.entrySet()) {
                layout.deleteObject(fileId, fi, obj.getKey(), obj.getValue().version, obj.getValue().timestamp);
            }
            layout.setTruncateEpoch(fileId, epochNumber);

            // Remove file info from cache to make sure it is reloaded with the
            // next operation.
            cache.removeFileInfo(fileId);
            cback.deleteObjectsComplete(null);
        } catch (Exception ex) {
            cback.deleteObjectsComplete(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                    ex.toString()));
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
            final Long receivedServerTimestamp = (Long) rq.getArgs()[8];

            final FileMetadata fi = layout.getFileMetadata(sp, fileId);

            if (fi.getTruncateEpoch() >= epochNumber) {
                cback.truncateComplete(
                        OSDWriteResponse.newBuilder().setSizeInBytes(fi.getFilesize())
                                .setTruncateEpoch((int) fi.getTruncateEpoch()).build(), null);
                /*
                 * cback.truncateComplete(null, new OSDException(ErrorCodes.EPOCH_OUTDATED,
                 * "invalid truncate epoch for file " + fileId + ": " + epochNumber + ", current one is " +
                 * fi.getTruncateEpoch(), ""));
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

            // if COW is enabled, check if the received server timestamp is in the future and wait if
            // necessary, so as to ensure snapshot consistency
            long newTimestamp = cow.cowEnabled() ? ensureSnapshotConsistency(receivedServerTimestamp) : -1;

            if (fi.getFilesize() == newFileSize) {
                // file size remains unchanged

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "truncate to local size: %d",
                            newFileSize);

                newLastObject = fi.getLastObjectNumber();
                newGlobalLastObject = fi.getGlobalLastObjectNumber();

            } else if (newFileSize == 0) {
                // truncate file to zero length

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "truncate to 0");

                // // if copy-on-write is enabled, perform a 'truncateShrink' in order to create a new version
                // of
                // // the first object if necessary
                // if (cow.cowEnabled())
                // truncateShrink(fileId, newFileSize, epochNumber, sp, fi, relativeOSDNumber, cow,
                // newObjVer);

                // otherwise, delete all objects of the file (but not the metadata)
                if (!cow.cowEnabled()) {
                    fi.getVersionManager().clearObjectVersions();
                    layout.deleteFile(fileId, false);
                }

            } else if (fi.getFilesize() > newFileSize) {
                // shrink file
                newLastObject = truncateShrink(fileId, newFileSize, epochNumber, sp, fi, relativeOSDNumber, cow,
                        newObjVer, newTimestamp);
                newGlobalLastObject = sp.getObjectNoForOffset(newFileSize - 1);
            } else if (fi.getFilesize() < newFileSize) {
                // extend file
                newLastObject = truncateExtend(fileId, newFileSize, epochNumber, sp, fi, relativeOSDNumber, cow,
                        newObjVer, newTimestamp);
                newGlobalLastObject = sp.getObjectNoForOffset(newFileSize - 1);
            }

            // set the new file size and last object number
            fi.setFilesize(newFileSize);
            fi.setLastObjectNumber(newLastObject);
            fi.setTruncateEpoch(epochNumber);

            fi.setGlobalLastObjectNumber(newGlobalLastObject);

            // store the truncate epoch persistently
            layout.setTruncateEpoch(fileId, epochNumber);

            // If COW is enabled, record the truncate operation in the version manager. This is necessary to
            // ensure proper read results if multiple truncates are performed within a single open-close
            // period.
            if (cow.cowEnabled())
                fi.getVersionManager().recordTruncate(newTimestamp, newFileSize, newLastObject + 1);

            if (createTLogEntry) {
                TruncateLog log = layout.getTruncateLog(fileId);
                log = log
                        .toBuilder()
                        .addRecords(
                                TruncateRecord.newBuilder().setVersion(newObjVer).setLastObjectNumber(newLastObject))
                        .build();
                layout.setTruncateLog(fileId, log);
            }

            // append the new file size and epoch number to the response
            OSDWriteResponse response = OSDWriteResponse.newBuilder().setSizeInBytes(newFileSize)
                    .setTruncateEpoch((int) epochNumber).setServerTimestamp(newTimestamp).build();
            cback.truncateComplete(response, null);

        } catch (Exception ex) {
            cback.truncateComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }

    }

    /**
     * @param method
     */
    private void processGetObjectSet(StageRequest rq) {
        final GetObjectListCallback cback = (GetObjectListCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];
        final StripingPolicyImpl sp = (StripingPolicyImpl) rq.getArgs()[1];

        try {
            final FileMetadata fi = layout.getFileMetadata(sp, fileId);
            ObjectSet objectSet = layout.getObjectSet(fileId, fi);
            cback.getObjectSetComplete(objectSet, null);
        } catch (Exception ex) {
            cback.getObjectSetComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
        }
    }

    /**
     * @param method
     */
    private void processCreateFileVersion(StageRequest rq) {

        final CreateFileVersionCallback cback = (CreateFileVersionCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];

        FileMetadata fi = (FileMetadata) rq.getArgs()[1];

        try {

            if (fi == null)
                fi = layout.getFileMetadataNoCaching(null, fileId);

            long fileSize = fi.getFilesize();

            // determine the last object
            long lastObject = fi.getLastObjectNumber();
            assert (lastObject < Integer.MAX_VALUE);

            // append a new file version to the file version log
            try {
                fi.getVersionManager().createFileVersion(TimeSync.getGlobalTime(), fileSize, lastObject + 1);
            } catch (IOException e) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, OutputUtils.stackTraceToString(e));
            }

            cback.createFileVersionComplete(fileSize, null);

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            cback.createFileVersionComplete(0,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
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
            cback.createGetFileIDListComplete(null,
                    ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));

        }
    }

    private long truncateShrink(String fileId, long fileSize, long epoch, StripingPolicyImpl sp, FileMetadata fi,
            int relOsdId, CowPolicy cow, Long newObjVer, long newTimestamp) throws IOException {

        // first find out which is the new "last object"
        final long newLastObject = sp.getObjectNoForOffset(fileSize - 1);
        final long oldLastObject = fi.getLastObjectNumber();
        assert (oldLastObject == -1 || newLastObject <= oldLastObject) : "new= " + newLastObject + " old="
                + oldLastObject;

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

                // if the currently examined object is new last object and local:
                // shrink it
                if (rowObj == newLastObject) {
                    final int newObjSize = (int) (fileSize - sp.getObjectStartOffset(newLastObject));
                    truncateObject(fileId, newLastObject, sp, newObjSize, relOsdId, cow, newObjVer, newTimestamp);
                }
            }

        }

        // if copy-on-write is disabled ...
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
                    truncateObject(fileId, newLastObject, sp, newObjSize, relOsdId, cow, newObjVer, newTimestamp);

                } else if (rowObj > newLastObject) {

                    // currently examined object is larger than new last object:
                    // delete it
                    final ObjectVersionInfo v = fi.getVersionManager().getLargestObjectVersion(rowObj);
                    layout.deleteObject(fileId, fi, rowObj, v.version, v.timestamp);

                    fi.getVersionManager().removeObjectVersionInfo(rowObj, v.version, v.timestamp);
                }
            }

        }

        // make sure that new last object exists, as it may have been a gap
        for (long obj = newLastObject - 1; obj > newLastObject - sp.getWidth(); obj--) {

            if (obj > 0 && sp.isLocalObject(obj, relOsdId)) {

                final ObjectVersionInfo v = fi.getVersionManager().getLargestObjectVersion(obj);

                // create padding object if version does not exist
                if (v == ObjectVersionInfo.MISSING)
                    createPaddingObject(fileId, obj, sp, newObjVer != null ? newObjVer : 1, newTimestamp,
                            sp.getStripeSizeForObject(obj), fi);
            }
        }

        return newLastObject;
    }

    private long truncateExtend(String fileId, long fileSize, long epoch, StripingPolicyImpl sp, FileMetadata fi,
            int relOsdId, CowPolicy cow, Long newObjVer, long newTimestamp) throws IOException {
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
            truncateObject(fileId, newLastObject, sp, (int) (fileSize - sp.getObjectStartOffset(newLastObject)),
                    relOsdId, cow, newObjVer, newTimestamp);
        }

        // otherwise ...
        else {

            // extend the old last object to full object size
            if ((oldLastObject > -1) && (sp.isLocalObject(oldLastObject, relOsdId))) {
                truncateObject(fileId, oldLastObject, sp, sp.getStripeSizeForObject(oldLastObject), relOsdId, cow,
                        newObjVer, newTimestamp);
            }

            // if the new last object is a local object, create a padding
            // object to ensure that it exists
            if (sp.isLocalObject(newLastObject, relOsdId)) {

                long version = newObjVer != null ? newObjVer : Math.max(1, fi.getVersionManager()
                        .getLargestObjectVersion(newLastObject).version);
                int objSize = (int) (fileSize - sp.getObjectStartOffset(newLastObject));

                createPaddingObject(fileId, newLastObject, sp, version, newTimestamp, objSize, fi);
            }

            // make sure that new last objects also exist on all other OSDs
            for (long obj = newLastObject - 1; obj > newLastObject - sp.getWidth(); obj--) {
                if (obj > 0 && sp.isLocalObject(obj, relOsdId)) {
                    ObjectVersionInfo v = fi.getVersionManager().getLargestObjectVersion(obj);
                    if (v == ObjectVersionInfo.MISSING) {
                        // does not exist
                        final long newVersion = newObjVer != null ? newObjVer : Math.max(1, fi.getVersionManager()
                                .getLargestObjectVersion(obj).version);
                        createPaddingObject(fileId, obj, sp, newVersion, newTimestamp, sp.getStripeSizeForObject(obj),
                                fi);
                    }
                }
            }

        }

        return newLastObject;
    }

    private void truncateObject(String fileId, long objNo, StripingPolicyImpl sp, int newSize, long relOsdId,
            CowPolicy cow, Long newObjVer, long newTimestamp) throws IOException {

        assert (newSize >= 0) : "new size is " + newSize + " but should be >= 0";
        assert (newSize <= sp.getStripeSizeForObject(objNo));
        assert (objNo >= 0) : "objNo is " + objNo;
        assert (sp.getOSDforObject(objNo) == relOsdId);

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "truncate object to %d", newSize);

        final FileMetadata fi = layout.getFileMetadata(sp, fileId);
        final long newVersion = newObjVer != null ? newObjVer : Math.max(1, fi.getVersionManager()
                .getLargestObjectVersion(objNo).version);

        layout.truncateObject(fileId, fi, objNo, newSize, newVersion, newTimestamp, cow);

    }

    private void createPaddingObject(String fileId, long objNo, StripingPolicyImpl sp, long version, long timestamp,
            int size, FileMetadata fi) throws IOException {
        layout.createPaddingObject(fileId, fi, objNo, version, timestamp, size);
    }

    private long ensureSnapshotConsistency(Long receivedServerTimestamp) {

        long currentTime = TimeSync.getPreciseGlobalTime();
        if (receivedServerTimestamp != null && receivedServerTimestamp != -1) {

            // make sure that the current local time is later than the received server timestamp; if
            // necessary, repeatedly wait for 10ms
            while (currentTime < receivedServerTimestamp)
                try {
                    Thread.sleep(10);
                    currentTime = TimeSync.getPreciseGlobalTime();
                } catch (InterruptedException e) {
                    Logging.logError(Logging.LEVEL_WARN, this, e);
                }
            if (currentTime == receivedServerTimestamp)
                currentTime++;
        }

        return currentTime;
    }
}
