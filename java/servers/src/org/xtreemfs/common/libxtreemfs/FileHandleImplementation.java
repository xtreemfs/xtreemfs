/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.RPCCaller.CallGenerator;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.InternalServerErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.InvalidChecksumException;
import org.xtreemfs.common.libxtreemfs.exceptions.InvalidViewException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.UUIDIteratorListIsEmpyException;
import org.xtreemfs.common.libxtreemfs.exceptions.UUIDNotInXlocSetException;
import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SERVICES;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.timestampResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_xlocsetRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_update_file_sizeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectList;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.lockRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.readRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.truncateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.writeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_check_objectRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_file_sizeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_file_sizeResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_object_setRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_repair_objectRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

//JCIP import net.jcip.annotations.GuardedBy;

/**
 * Implementation of FileHandle and AdminFileHandle. Used only internally.
 */
public class FileHandleImplementation implements FileHandle, AdminFileHandle {

    /**
     * UUID of the Client (needed to distinguish Locks of different clients).
     */
    final private String                            clientUuid;

    /**
     * UUIDIterator of the MRC.
     */
    final private UUIDIterator                      mrcUuidIterator;

    /**
     * UUIDIterator which contains the UUIDs of all replicas.
     */
    final private UUIDIterator                      osdUuidIterator;

    /**
     * Needed to resolve UUIDs.
     */
    final private UUIDResolver                      uuidResolver;

    /**
     * Multiple FileHandle may refer to the same File and therefore unique file properties (e.g. Path, FileId, XlocSet)
     * are stored in a FileInfo object.
     */
    final private FileInfo                          fileInfo;

    /**
     * Volume which did open this file.
     */
    final private VolumeImplementation              volume;

    // TODO(mberlin): Add flags member.

    /**
     * Capabilitiy for the file, used to authorize against services.
     */
    // JCIP @GuardedBy("this")
    private XCap                                    xcap;

    /**
     * True if there is an outstanding xcapRenew callback.
     */
    // JCIP @GuardedBy("xcapRenewalPendingLock")
    private boolean                                 xcapRenewalPending;

    /**
     * Used to wait for pending XCap renewal callbacks.
     */
    final private Object                            xcapRenewalPendingLock;

    /**
     * Contains a file size update which has to be written back (or NULL).
     */
    private OSDWriteResponse                        osdWriteResponseForAsyncWriteBack;

    /**
     * MRCServiceClient from the VolumeImplemention.
     */
    final private MRCServiceClient                  mrcServiceClient;

    /**
     * Pointer to object owned by VolumeImplemention.
     */
    final private OSDServiceClient                  osdServiceClient;

    /**
     * Stores which {@link StripingPolicyType} corresponds to which {@link StripeTranslator}.
     */
    final Map<StripingPolicyType, StripeTranslator> stripeTranslators;

    /**
     * Set to true if async writes (max requests > 0, no O_SYNC) are enabled.
     */
    private final boolean                                 asyncWritesEnabled;

    /**
     * Set to true if an async write of this file_handle failed. If true, this file_handle is broken and no further
     * writes/reads/truncates are possible.
     */
    private boolean                                 asyncWritesFailed;


    final private Options                           volumeOptions;

    /**
     * Auth needed for ServiceClients. Always set to AUTH_NONE by Volume.
     */
    final private Auth                              authBogus;

    /**
     * For same reason needed as authBogus. Always set to user "xtreemfs".
     */
    final private UserCredentials                   userCredentialsBogus;

    public FileHandleImplementation(VolumeImplementation volume, String clientUuid, FileInfo fileInfo, XCap xcap,
            UUIDIterator mrcUuidIterator, UUIDIterator osdUuidIterator, UUIDResolver uuidResolver,
            MRCServiceClient mrcServiceClient, OSDServiceClient osdServiceClient,
            Map<StripingPolicyType, StripeTranslator> stripeTranslators, boolean asyncWritesEnabled, Options options,
            Auth authBogus, UserCredentials userCredentialsBogus) {
        this.volume = volume;
        this.clientUuid = clientUuid;
        this.fileInfo = fileInfo;
        this.xcap = xcap;
        this.mrcUuidIterator = mrcUuidIterator;
        this.osdUuidIterator = osdUuidIterator;
        this.uuidResolver = uuidResolver;
        this.mrcServiceClient = mrcServiceClient;
        this.osdServiceClient = osdServiceClient;
        this.stripeTranslators = stripeTranslators;
        this.asyncWritesEnabled = asyncWritesEnabled;
        this.volumeOptions = options;
        this.authBogus = authBogus;
        this.userCredentialsBogus = userCredentialsBogus;

        xcapRenewalPending = false;
        xcapRenewalPendingLock = new Object();
    }

    /**
     * Used to execute an operation and check on invalid view exceptions. <br>
     * If the view the operation was executed with is outdated, it will be renewed and the operation retried.
     */
    private abstract class ViewCheckedOperation<T> {
        
        T execute() throws IOException {
            int maxTries = volumeOptions.getMaxViewRenewals();
            int attempt = 0;

            while (++attempt <= maxTries || maxTries == 0) {
                try {
                    return doOperation();
                } catch (InvalidViewException e) {
                    if (attempt == maxTries && maxTries > 0) {
                        // The request did finally fail.
                        // Append the number of attempts to the message and rethrow.
                        String errorMsg = e.getMessage() + " Request finally failed after " + attempt + " attempts";
                        throw new InvalidViewException(errorMsg);

                    } else {
                        // Delay the xLocSet renewal and the next run of the operation.
                        RPCCaller.waitDelay(volumeOptions.getRetryDelay_s());

                        // Try to renew the XLocSet.
                        renewXLocSet();
                    }
                }
            }

            throw new InvalidViewException("The operation did fail due to an outdated view after " 
                    + attempt + " attempts.");
        }
        
        abstract T doOperation() throws IOException;
    }
    

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#read(org.xtreemfs.foundation .pbrpc.generatedinterfaces.RPC
     * .UserCredentials, org.xtreemfs.foundation.buffer.ReusableBuffer, int, long)
     */
    @Override
    public int read(UserCredentials userCredentials, byte[] data, int count, long offset) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        return read(userCredentials, data, 0, count, offset);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#read(org.xtreemfs.foundation .pbrpc.generatedinterfaces.RPC
     * .UserCredentials, org.xtreemfs.foundation.buffer.ReusableBuffer, int, long)
     */
    @Override
    public int read(final UserCredentials userCredentials, final byte[] data, final int dataOffset, final int count,
            final long offset) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {

        ViewCheckedOperation<Integer> operation = new ViewCheckedOperation<Integer>() {
            @Override
            Integer doOperation() throws IOException {
                return doRead(userCredentials, data, dataOffset, count, offset);
            }
        };
        return operation.execute();
    }

    private int doRead(UserCredentials userCredentials, byte[] data, int dataOffset, int count, long offset)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        fileInfo.waitForPendingAsyncWrites();
        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        synchronized (this) {
            if (asyncWritesFailed) {
                throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EIO, "A previous asynchronous"
                        + " write did fail. No more actions on this file handle are allowed.");
            }
            // TODO(mberlin): XCap might expire while retrying a request.
            // Provide a mechanism to renew the xcap in the request.
            fcBuilder.setXcap(xcap.toBuilder());
        }
        FileCredentials fc = fcBuilder.setXlocs(fileInfo.getXLocSet()).build();

        ReusableBuffer buf = ReusableBuffer.wrap(data, dataOffset, count);
        int receivedData = 0;

        if (fc.getXlocs().getReplicasCount() == 0) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, "No replica found for file %s",
                    fileInfo.getPath());
            throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EIO, "no replica found for file: "
                    + fileInfo.getPath());
        }

        // Pick the first replica to determine striping policy.
        // (We assume that all replicas use the same striping policy.)
        StripingPolicy policy = fc.getXlocs().getReplicas(0).getStripingPolicy();
        StripeTranslator translator = getStripeTranslator(policy.getType());

        // Map offset to corresponding OSDs.
        Vector<ReadOperation> operations = new Vector<ReadOperation>();
        translator.translateReadRequest(count, offset, policy, operations);

        UUIDIterator tempUuidIteratorForStriping = new UUIDIterator();
        // Read all objects
        for (int j = 0; j < operations.size(); j++) {
            readRequest.Builder readRqBuilder = readRequest.newBuilder();

            readRqBuilder.setFileCredentials(fc);
            readRqBuilder.setFileId(fc.getXcap().getFileId());
            readRqBuilder.setObjectNumber(operations.get(j).getObjNumber());
            readRqBuilder.setObjectVersion(0);
            readRqBuilder.setOffset(operations.get(j).getReqOffset());
            readRqBuilder.setLength(operations.get(j).getReqSize());

            // Differ between striping and the rest (replication, no replication).
            UUIDIterator uuidIterator;
            if (readRqBuilder.getFileCredentials().getXlocs().getReplicas(0).getOsdUuidsCount() > 1) {
                // Replica is striped. Pick UUID from xlocset.
                tempUuidIteratorForStriping.clear();
                
                // Replicas may have different stripe widths. However, the current Java client
                // StripeTranslator code only supports the same stripe width as the first replica has.
                int stripeWidthFirstReplica = fc.getXlocs().getReplicas(0).getStripingPolicy().getWidth();

                for (int replicaIdx = 0; replicaIdx < fc.getXlocs().getReplicasCount(); replicaIdx++) {
                    if (fc.getXlocs().getReplicas(replicaIdx).getStripingPolicy().getWidth() == stripeWidthFirstReplica) {
                        tempUuidIteratorForStriping.addUUID(Helper.getOSDUUIDFromXlocSet(fc.getXlocs(),
                                                            replicaIdx,
                                                            operations.get(j).getOsdOffset()));
                    }
                }
                
                uuidIterator = tempUuidIteratorForStriping;
            } else {
                // TODO(mberlin): Enhance UUIDIterator to read from different replicas.
                uuidIterator = osdUuidIterator;
            }

            buf.position(operations.get(j).getBufferStart());
            // If synccall gets a buffer it fill it with data from the response.
            ObjectData objectData = RPCCaller.<readRequest, ObjectData> syncCall(SERVICES.OSD, userCredentialsBogus,
                    authBogus, volumeOptions, uuidResolver, uuidIterator, false, readRqBuilder.build(), buf,
                    new CallGenerator<readRequest, ObjectData>() {

                        @Override
                        public RPCResponse<ObjectData> executeCall(InetSocketAddress server, Auth auth,
                                UserCredentials userCreds, readRequest callRequest) throws IOException {
                            return osdServiceClient.read(server, auth, userCreds, callRequest);

                        }
                    });
            // if zeropadding > 0, put zeros at the end of the buffer.
            for (int i = 0; i < objectData.getZeroPadding(); i++) {
                buf.put((byte) 0);
            }
            receivedData += buf.position() - operations.get(j).getBufferStart();
        }
        return receivedData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#write(org.xtreemfs.foundation .pbrpc.generatedinterfaces.
     * RPC.UserCredentials, org.xtreemfs.foundation.buffer.ReusableBuffer, int, long)
     */
    @Override
    public synchronized int write(UserCredentials userCredentials, byte[] data, int count, long offset)
            throws IOException, PosixErrorException, InternalServerErrorException, AddressToUUIDNotFoundException {
        ReusableBuffer buffer = ReusableBuffer.wrap(data);
        return write(userCredentials, buffer, count, offset);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#write(org.xtreemfs.foundation.pbrpc.generatedinterfaces.
     * RPC.UserCredentials, byte[], int, int, long)
     */
    @Override
    public synchronized int write(UserCredentials userCredentials, byte[] data, int dataOffset, int count, long offset)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        ReusableBuffer buffer = ReusableBuffer.wrap(data, dataOffset, count);
        return write(userCredentials, buffer, count, offset);
    }

    private int write(UserCredentials userCredentials, ReusableBuffer buffer, int count, long offset)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        synchronized (this) {
            if (asyncWritesFailed) {
                throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EIO, "A previous asynchronous "
                        + "write did fail. No further writes on this file handle are allowed.");
            }
            fcBuilder.setXcap(xcap.toBuilder());
        }

        fcBuilder.setXlocs(fileInfo.getXLocSet());

        String globalFileId = fcBuilder.getXcap().getFileId();
        XLocSet xlocs = fcBuilder.getXlocs();

        if (xlocs.getReplicasCount() == 0) {
            String error = "No replica found for file: " + fileInfo.getPath();
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, error);
            throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EIO, error);
        }

        // Map operation to stripes.
        Vector<WriteOperation> operations = new Vector<WriteOperation>();
        StripingPolicy stripingPolicy = xlocs.getReplicas(0).getStripingPolicy();
        StripeTranslator translator = getStripeTranslator(stripingPolicy.getType());

        translator.translateWriteRequest(count, offset, stripingPolicy, buffer, operations);

        FileCredentials fileCredentials = fcBuilder.build();

        String osdUuid = "";
        writeRequest.Builder request;

        if (asyncWritesEnabled) {
            // Write all objects.
            for (int j = 0; j < operations.size(); j++) {
                request = writeRequest.newBuilder();
                request.setFileCredentials(fileCredentials);
                request.setFileId(globalFileId);

                request.setObjectNumber(operations.get(j).getObjNumber());
                request.setObjectVersion(0);
                request.setOffset(operations.get(j).getReqOffset());
                request.setLeaseTimeout(0);

                ObjectData objectData = ObjectData.newBuilder().setChecksum(0).setInvalidChecksumOnOsd(false)
                        .setZeroPadding(0).build();
                request.setObjectData(objectData);

                // Create new WriteBuffer and differ between striping and the
                // rest (
                // (replication = use UUIDIterator, no replication = set
                // specific UUID).
                AsyncWriteBuffer writeBuffer;

                if (xlocs.getReplicas(0).getOsdUuidsCount() > 1) {
                    // Replica is striped. Pick UUID from xlocset
                    writeBuffer = new AsyncWriteBuffer(request.build(), operations.get(j).getReqData(), operations.get(
                            j).getReqSize(), this, Helper.getOSDUUIDFromXlocSet(xlocs, 0, operations.get(j)
                            .getOsdOffset()));
                } else {
                    writeBuffer = new AsyncWriteBuffer(request.build(), operations.get(j).getReqData(), operations.get(
                            j).getReqSize(), this);
                }

                // TODO(mberlin): Currently the UserCredentials are ignored by the OSD and
                // therefore we avoid copying them into writeBuffer.
                fileInfo.asyncWrite(writeBuffer);

                // Processing of file size updates is handled by the FileInfo's AsyncWriteHandler.
            }
        } else {
            // synchroneous write
            for (int j = 0; j < operations.size(); j++) {
                request = writeRequest.newBuilder();
                request.setFileCredentials(fileCredentials);
                request.setFileId(globalFileId);
                request.setObjectNumber(operations.get(j).getObjNumber());
                request.setObjectVersion(0);
                request.setOffset(operations.get(j).getReqOffset());
                request.setLeaseTimeout(0);

                ObjectData objectData = ObjectData.newBuilder().setChecksum(0).setInvalidChecksumOnOsd(false)
                        .setZeroPadding(0).build();

                request.setObjectData(objectData);

                // Differ between striping and the rest (replication, no replication).
                UUIDIterator uuidIterator;
                if (xlocs.getReplicas(0).getOsdUuidsCount() > 1) {
                    // Replica is striped. Pick UUID from Xlocset. Use first and only replica.
                    osdUuid = Helper.getOSDUUIDFromXlocSet(xlocs, 0, operations.get(j).getOsdOffset());
                    uuidIterator = new UUIDIterator();
                    uuidIterator.clearAndAddUUID(osdUuid);
                } else {
                    // TODO: enhance UUIDIterator to read from different replicas.
                    uuidIterator = osdUuidIterator;
                }

                final ReusableBuffer writeDataBuffer = operations.get(j).getReqData();
                OSDWriteResponse response = RPCCaller.<writeRequest, OSDWriteResponse> syncCall(SERVICES.OSD,
                        userCredentials, authBogus, volumeOptions, uuidResolver, uuidIterator, false, request.build(),
                        new CallGenerator<writeRequest, OSDWriteResponse>() {

                            @Override
                            public RPCResponse<OSDWriteResponse> executeCall(InetSocketAddress server, Auth authHeader,
                                    UserCredentials userCreds, writeRequest input) throws IOException {
                                return osdServiceClient.write(server, authHeader, userCreds, input,
                                        writeDataBuffer.createViewBuffer());
                            }
                        });

                assert (response != null);

                // If the filesize has changed, remember OSDWriteResponse for later file size
                // update towards the MRC (executed by PeriodicFileSizeUpdateThread).
                if (response.hasSizeInBytes()) {
                    fileInfo.tryToUpdateOSDWriteResponse(response, xcap);
                }
            }
        }
        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#flush()
     */
    @Override
    public void flush() throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        flush(false);
    }

    protected void flush(boolean closeFile) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        fileInfo.flush(this, closeFile);
        throwIfAsyncWritesFailed();
    }

    private boolean didAsyncWriteFail() {
        synchronized (this) {
            return this.asyncWritesFailed;
        }
    }

    private void throwIfAsyncWritesFailed() throws PosixErrorException {
        if (didAsyncWriteFail()) {
            String error = "Flush for file " + fileInfo.getPath() + " did not succeed flushing "
                    + "all pending writes as at least one asynchronous write did fail.";

            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, error);
            throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EIO, error);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#truncate(org.xtreemfs.foundation .pbrpc.generatedinterfaces
     * .RPC.UserCredentials, int)
     */
    @Override
    public void truncate(UserCredentials userCredentials, long newFileSize) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        truncate(userCredentials, newFileSize, false);
    }

    @Override
    public void truncate(UserCredentials userCredentials, long newFileSize, boolean updateOnlyMRC) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {
        fileInfo.waitForPendingAsyncWrites();
        XCap xcapCopy;
        boolean awf = false;
        synchronized (this) {
            awf = asyncWritesFailed;
        }
        if (awf) {
            throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EIO, "A previous asynchronous "
                    + "write did fail.No further action on this file handle are allowed.");
        }
        xcapCopy = getXcap();

        // 1. Call truncate at the MRC (in order to increase the trunc epoch).
        XCap truncateXCap = RPCCaller.<XCap, XCap> syncCall(SERVICES.MRC, userCredentials, authBogus, volumeOptions,
                uuidResolver, mrcUuidIterator, false, xcapCopy, new CallGenerator<XCap, XCap>() {
                    @Override
                    public RPCResponse<XCap> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, XCap input) throws IOException {
                        return mrcServiceClient.ftruncate(server, authHeader, userCreds, input);
                    }
                });
        // set new XCap received from MRC. Necessary to invoke truncate at OSD.
        synchronized (this) {
            xcap = truncateXCap;
        }
        truncatePhaseTwoAndThree(userCredentials, newFileSize, updateOnlyMRC);
    }

    /**
     * Used by truncate() and Volume.openFile() to truncate the file to "newFileSize" on the OSD and update the file
     * size at the MRC.
     * 
     * @throws AddressToUUIDNotFoundException
     * @throws IOException
     * @throws PosixErrorException
     **/
    protected void truncatePhaseTwoAndThree(UserCredentials userCredentials, long newFileSize, boolean updateOnlyMRC)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {

        OSDWriteResponse response = null;

        XCap xCapCopy = getXcap();

        if (!updateOnlyMRC) {

            // 2. Call truncate at the head OSD.
            truncateRequest.Builder requestBuilder = truncateRequest.newBuilder();
            FileCredentials.Builder fileCredentialsBuilder = FileCredentials.newBuilder();
            fileCredentialsBuilder.setXlocs(fileInfo.getXLocSet());
            fileCredentialsBuilder.setXcap(xCapCopy);
            requestBuilder.setFileId(fileCredentialsBuilder.getXcap().getFileId());
            requestBuilder.setFileCredentials(fileCredentialsBuilder.build());
            requestBuilder.setNewFileSize(newFileSize);

            response = RPCCaller.<truncateRequest, OSDWriteResponse> syncCall(SERVICES.OSD, userCredentials, authBogus,
                    volumeOptions, uuidResolver, osdUuidIterator, false, requestBuilder.build(),
                    new CallGenerator<truncateRequest, OSDWriteResponse>() {
                        @Override
                        public RPCResponse<OSDWriteResponse> executeCall(InetSocketAddress server, Auth authHeader,
                                UserCredentials userCreds, truncateRequest input) throws IOException {
                            return osdServiceClient.truncate(server, authHeader, userCreds, input);
                        }
                    });

            assert (response != null);
            assert (response.hasSizeInBytes());
        } else {

            // create OSDWriteResponse
            response = OSDWriteResponse.newBuilder().setTruncateEpoch(xCapCopy.getTruncateEpoch())
                    .setSizeInBytes(newFileSize).build();

        }

        // register the new OSDWriteResponse to this file's FileInfo.
        fileInfo.tryToUpdateOSDWriteResponse(response, xCapCopy);

        // 3. Update the file size at the MRC.
        fileInfo.flushPendingFileSizeUpdate(this);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#getAttr(org.xtreemfs.foundation .pbrpc.generatedinterfaces
     * .RPC.UserCredentials)
     */
    @Override
    public Stat getAttr(UserCredentials userCredentials) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        return volume.getAttr(userCredentials, fileInfo.getPath());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#acquireLock(org.xtreemfs. foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, int, long, long, boolean, boolean)
     */
    @Override
    public Lock acquireLock(UserCredentials userCredentials, int processId, long offset, long length,
            boolean exclusive, boolean waitForLock) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        // Create Lock object for the acquire lock request.
        Lock.Builder lockBuilder = Lock.newBuilder();
        lockBuilder.setClientUuid(clientUuid);
        lockBuilder.setClientPid(processId);
        lockBuilder.setOffset(offset);
        lockBuilder.setLength(length);
        lockBuilder.setExclusive(exclusive);

        Lock lock = lockBuilder.build();

        // Check active locks first.
        Tupel<Lock, boolean[]> checkLockReturn = fileInfo.checkLock(lock);
        boolean conflictFound = checkLockReturn.getSecond()[0];
        boolean cachedLockForPidEqual = checkLockReturn.getSecond()[2];

        if (conflictFound) {
            throw new PosixErrorException(POSIXErrno.POSIX_ERROR_EAGAIN, "conflicting lock");
        }

        // We allow only one lock per PID, i.e. an existing lock can be always
        // overwritten. In consequence, acquireLock() always has to be executed
        // except the new lock is equal to the current lock.
        if (cachedLockForPidEqual) {
            return lock;
        }

        // Cache could not be used. Create FileCredentials, complete lockRequest
        // and send to OSD.
        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        fcBuilder.setXlocs(fileInfo.getXLocSet());
        fcBuilder.setXcap(getXcap());
        lockRequest request = lockRequest.newBuilder().setLockRequest(lock).setFileCredentials(fcBuilder.build())
                .build();

        Lock response = null;
        if (!waitForLock) {
            response = RPCCaller.<lockRequest, Lock> syncCall(SERVICES.OSD, userCredentials, authBogus, volumeOptions,
                    uuidResolver, osdUuidIterator, false, request, new CallGenerator<lockRequest, Lock>() {
                        @Override
                        public RPCResponse<Lock> executeCall(InetSocketAddress server, Auth authHeader,
                                UserCredentials userCreds, lockRequest input) throws IOException {
                            return osdServiceClient.xtreemfs_lock_acquire(server, authHeader, userCreds, input);
                        }
                    });
        } else {
            // Retry to obtain the lock in case of EAGAIN responses.\
            int retriesLeft = volumeOptions.getMaxTries();
            while (retriesLeft >= 0) {
                retriesLeft--;
                try {
                    response = RPCCaller.<lockRequest, Lock> syncCall(SERVICES.OSD, userCredentials, authBogus,
                            volumeOptions, uuidResolver, osdUuidIterator, false, true, 1, request,
                            new CallGenerator<lockRequest, Lock>() {
                                @Override
                                public RPCResponse<Lock> executeCall(InetSocketAddress server, Auth authHeader,
                                        UserCredentials userCreds, lockRequest input) throws IOException {
                                    return osdServiceClient.xtreemfs_lock_acquire(server, authHeader, userCreds, input);
                                }
                            });
                    // break if there is no error.
                    break;
                } catch (PosixErrorException pe) {
                    if (pe.getPosixError().equals(POSIXErrno.POSIX_ERROR_EAGAIN) == false) {
                        // Only retry if there exists a conflicting lock and the server did
                        // return an EAGAIN - otherwise rethrow the exception.
                        throw pe;
                    }
                }
            }
        }
        // "Cache" new lock.
        fileInfo.putLock(response);
        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#checkLock(org.xtreemfs.foundation .pbrpc.generatedinterfaces
     * .RPC.UserCredentials, int, long, long, boolean)
     */
    @Override
    public Lock checkLock(UserCredentials userCredentials, int processId, long offset, long length, boolean exclusive)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        // Create lock object for the check lock request.
        Lock.Builder lockBuilder = Lock.newBuilder();
        lockBuilder.setClientUuid(clientUuid);
        lockBuilder.setClientPid(processId);
        lockBuilder.setOffset(offset);
        lockBuilder.setLength(length);
        lockBuilder.setExclusive(exclusive);

        Lock lock = lockBuilder.build();

        // Check active locks first.
        Tupel<Lock, boolean[]> checkLockReturn = fileInfo.checkLock(lock);
        Lock conflictingLock = checkLockReturn.getFirst();
        boolean conflictFound = checkLockReturn.getSecond()[0];
        boolean lockForPidCached = checkLockReturn.getSecond()[1];

        if (conflictFound) {
            return conflictingLock;
        }

        // We allow only one lock per PID, i.e. an existing lock can be always
        // overwritten.
        if (lockForPidCached) {
            return lock;
        }

        // Cache could not be used. Create lockRequest and send to OSD.
        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        fcBuilder.setXlocs(fileInfo.getXLocSet());
        fcBuilder.setXcap(getXcap());
        lockRequest request = lockRequest.newBuilder().setLockRequest(lock).setFileCredentials(fcBuilder.build())
                .build();

        Lock response = RPCCaller.<lockRequest, Lock> syncCall(SERVICES.OSD, userCredentials, authBogus, volumeOptions,
                uuidResolver, osdUuidIterator, false, request, new CallGenerator<lockRequest, Lock>() {
                    @Override
                    public RPCResponse<Lock> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, lockRequest input) throws IOException {
                        return osdServiceClient.xtreemfs_lock_check(server, authHeader, userCreds, input);
                    }
                });
        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#releaseLock(org.xtreemfs. foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, int, long, long, boolean)
     */
    @Override
    public void releaseLock(UserCredentials userCredentials, int processId, long offset, long length, boolean exclusive)
            throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        Lock.Builder lockBuilder = Lock.newBuilder();
        lockBuilder.setClientUuid(clientUuid);
        lockBuilder.setClientPid(processId);
        lockBuilder.setOffset(offset);
        lockBuilder.setLength(length);
        lockBuilder.setExclusive(exclusive);
        releaseLock(userCredentials, lockBuilder.build());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#releaseLock(org.xtreemfs. foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock)
     */
    @Override
    public void releaseLock(UserCredentials userCredentials, Lock lock) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        // Only release locks which are known to this client.
        if (!fileInfo.checkIfProcessHasLocks(lock.getClientPid())) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "FileHandleImplementation.releaseLock(): Skipping unlock request as there"
                                + " is no lock known for PID: %s (Lock description: %s, %s ,%s)", lock.getClientPid(),
                        lock.getOffset(), lock.getLength(), lock.getExclusive());
            }
            return;
        }

        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        fcBuilder.setXlocs(fileInfo.getXLocSet());
        fcBuilder.setXcap(getXcap());

        lockRequest unlockRequest = lockRequest.newBuilder().setFileCredentials(fcBuilder.build()).setLockRequest(lock)
                .build();

        RPCCaller.<lockRequest, emptyResponse> syncCall(SERVICES.OSD, userCredentials, authBogus, volumeOptions,
                uuidResolver, osdUuidIterator, false, unlockRequest, new CallGenerator<lockRequest, emptyResponse>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public RPCResponse<emptyResponse> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, lockRequest input) throws IOException {
                        return osdServiceClient.xtreemfs_lock_release(server, authHeader, userCreds, input);
                    }
                });
        fileInfo.delLock(lock);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#releaseLockOfProcess(int)
     */
    @Override
    public void releaseLockOfProcess(int processId) throws IOException, PosixErrorException,
            AddressToUUIDNotFoundException {
        fileInfo.releaseLockOfProcess(this, processId);
    }

    /**
     * Releases "lock" with userCredentials from this fileHandle object.
     * 
     * @param lock
     */
    protected void releaseLock(Lock lock) throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        releaseLock(userCredentialsBogus, lock);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#pingReplica(org.xtreemfs. foundation.pbrpc.generatedinterfaces
     * .RPC.UserCredentials, java.lang.String)
     */
    @Override
    public void pingReplica(UserCredentials userCredentials, String osdUuid) throws IOException,
            AddressToUUIDNotFoundException {
        readRequest.Builder readRequestBuilder = readRequest.newBuilder();
        FileCredentials.Builder fileCredentialsBuilder = FileCredentials.newBuilder();
        synchronized (this) {
            fileCredentialsBuilder.setXcap(xcap.toBuilder());
        }
        XLocSet xlocs = fileInfo.getXLocSet();
        fileCredentialsBuilder.setXlocs(xlocs);
        readRequestBuilder.setFileId(fileCredentialsBuilder.getXcap().getFileId());
        readRequestBuilder.setFileCredentials(fileCredentialsBuilder);

        // Check if osdUuid is part of the xlocset
        if (xlocs.getReplicasCount() == 0) {
            throw new UUIDIteratorListIsEmpyException("pingReplica: The Xlocset contains no replicas");
        }
        boolean uuidFound = false;
        for (Replica replica : xlocs.getReplicasList()) {
            // TODO(mberlin): Every OSD in a striped replica has to be pinged.
            // Always check only the head OSD.
            if (replica.getOsdUuids(0).equals(osdUuid)) {
                uuidFound = true;
                // Check replication flags, if it's a full replica.
                if (xlocs.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)
                        && ((replica.getReplicationFlags() & REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber()) == 0)) {
                    // Nothing to do here because the replication does not need to be
                    // triggered for partial replicas.
                    return;
                }
                break;
            }
        }
        if (uuidFound == false) {
            throw new UUIDNotInXlocSetException("UUID: " + osdUuid + " not found  in the xlocset: " + xlocs.toString());
        }

        // Read one byte from the replica to trigger the replication.
        readRequestBuilder.setObjectNumber(0);
        readRequestBuilder.setObjectVersion(0);
        readRequestBuilder.setOffset(0);
        readRequestBuilder.setLength(1);

        UUIDIterator tempUuidIterator = new UUIDIterator();
        tempUuidIterator.addUUID(osdUuid);

        RPCCaller.<readRequest, ObjectData> syncCall(SERVICES.OSD, userCredentialsBogus, authBogus, volumeOptions,
                uuidResolver, tempUuidIterator, false, readRequestBuilder.build(),
                new CallGenerator<readRequest, ObjectData>() {
                    @Override
                    public RPCResponse<ObjectData> executeCall(InetSocketAddress server, Auth auth,
                            UserCredentials userCreds, readRequest callRequest) throws IOException {
                        return osdServiceClient.read(server, auth, userCreds, callRequest);

                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xtreemfs.common.libxtreemfs.FileHandle#close()
     */
    @Override
    public void close() throws IOException {
        try {
            flush(true);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            fileInfo.closeFileHandle(this);
        }
    }

    protected void writeBackFileSizeAsync() throws IOException, PosixErrorException, AddressToUUIDNotFoundException {
        xtreemfs_update_file_sizeRequest.Builder rqBuilder = xtreemfs_update_file_sizeRequest.newBuilder();

        synchronized (this) {
            if (osdWriteResponseForAsyncWriteBack == null) {
                return;
            }
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "updateFileSize: %s " + "#bytes: %s",
                        fileInfo.getPath(), osdWriteResponseForAsyncWriteBack.getSizeInBytes());
            }
            rqBuilder.setXcap(xcap).setOsdWriteResponse(osdWriteResponseForAsyncWriteBack.toBuilder());
        }

        // set close file to false because true implies synchronous call.
        rqBuilder.setCloseFile(false);

        String address = uuidResolver.uuidToAddress(mrcUuidIterator.getUUID());
        InetSocketAddress server = RPCCaller.getInetSocketAddressFromAddress(address, SERVICES.MRC);

        RPCResponse<timestampResponse> r = mrcServiceClient.xtreemfs_update_file_size(server, authBogus,
                userCredentialsBogus, rqBuilder.build());

        final FileHandleImplementation fileHandle = this;

        r.registerListener(new RPCResponseAvailableListener<timestampResponse>() {

            @Override
            public void responseAvailable(RPCResponse<timestampResponse> r) {
                try {
                    r.get();
                    fileInfo.asyncFileSizeUpdateResponseHandler(osdWriteResponseForAsyncWriteBack, fileHandle, true);
                } catch (Exception e) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "renewXcapAsync: The following "
                                + "error occurred during the async all: ", e.getMessage());
                    }
                    fileInfo.asyncFileSizeUpdateResponseHandler(osdWriteResponseForAsyncWriteBack, fileHandle, false);
                } finally {
                    r.freeBuffers();
                }
            }
        });
    }

    protected void setOsdWriteResponseForAsyncWriteBack(OSDWriteResponse osdwr) {
        synchronized (this) {
            assert (osdWriteResponseForAsyncWriteBack == null);
            this.osdWriteResponseForAsyncWriteBack = osdwr.toBuilder().build();
        }
    }

    protected void renewXCapAsync() throws IOException, AddressToUUIDNotFoundException {
        XCap xcapCopy;

        synchronized (this) {
            // TODO: Only renew after some time has elapsed.
            // TODO: Cope with local clocks which have high clock skew.
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "Renew SCap for fileId: %s  Expiration in: %s", Helper.extractFileIdFromXcap(xcap),
                        xcap.getExpireTimeoutS() - System.currentTimeMillis() / 1000);
            }
            xcapCopy = this.xcap.toBuilder().build();

            synchronized (xcapRenewalPendingLock) {
                xcapRenewalPending = true;
            }
        }

        String address = uuidResolver.uuidToAddress(mrcUuidIterator.getUUID());
        InetSocketAddress server = RPCCaller.getInetSocketAddressFromAddress(address, SERVICES.MRC);

        RPCResponse<XCap> r = mrcServiceClient.xtreemfs_renew_capability(server, authBogus, userCredentialsBogus,
                xcapCopy);

        r.registerListener(new RPCResponseAvailableListener<XCap>() {
            @Override
            public void responseAvailable(RPCResponse<XCap> r) {
                try {
                    XCap newXCap = r.get();
                    setRenewedXcap(newXCap);
                } catch (Exception e) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "renewXcapAsync: Renewing XCap"
                                + " of file %s failed. Error: %s", fileInfo.getPath(), e.getMessage());
                    }
                } finally {
                    r.freeBuffers();
                    synchronized (xcapRenewalPendingLock) {
                        xcapRenewalPending = false;
                        xcapRenewalPendingLock.notifyAll();
                    }
                }
            }
        });
    }

    private void setRenewedXcap(XCap newXCap) {
        synchronized (this) {
            // Overwrite current XCap only by a newer one (i.e. later expire time)
            if (newXCap.getExpireTimeS() > xcap.getExpireTimeS()) {
                xcap = newXCap;
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "XCap renewed for fileId %s",
                            Helper.extractFileIdFromXcap(xcap));
                }
            }
        }
    }

    /**
     * Sets asyncWritesFailed to true.
     */
    protected void markAsyncWritesAsFailed() {
        synchronized (this) {
            asyncWritesFailed = true;
        }
    }

    protected XCap getXcap() {
        synchronized (this) {
            return xcap.toBuilder().build();
        }
    }

    /**
     * Sends pending file size updates synchronous (needed for flush/close).
     * 
     * @throws IOException
     */
    protected void writeBackFileSize(OSDWriteResponse response, boolean closeFile) throws IOException,
            PosixErrorException, AddressToUUIDNotFoundException {

        long fileId = 0;
        fileId = Helper.extractFileIdFromXcap(getXcap());
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                    "WriteBackFileSize: fileId: %s  #bytes: %s  close file?: %s", fileId, response.getSizeInBytes(),
                    closeFile);
        }

        xtreemfs_update_file_sizeRequest.Builder requestBuilder = xtreemfs_update_file_sizeRequest.newBuilder();

        requestBuilder.setXcap(getXcap());
        requestBuilder.setOsdWriteResponse(response.toBuilder().build());
        requestBuilder.setCloseFile(closeFile);

        RPCCaller.<xtreemfs_update_file_sizeRequest, timestampResponse> syncCall(SERVICES.MRC, userCredentialsBogus,
                authBogus, volumeOptions, uuidResolver, mrcUuidIterator, false, requestBuilder.build(),
                new CallGenerator<xtreemfs_update_file_sizeRequest, timestampResponse>() {
                    @Override
                    public RPCResponse<timestampResponse> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, xtreemfs_update_file_sizeRequest input) throws IOException {

                        return mrcServiceClient.xtreemfs_update_file_size(server, authHeader, userCreds, input);
                    }
                });
    }

    @Override
    public String getGlobalFileId() {
        return getXcap().getFileId();
    }

    private StripeTranslator getStripeTranslator(StripingPolicyType type) throws IOException {
        // Find the corresponding StripingPolicy
        StripeTranslator st = stripeTranslators.get(type);

        if (st == null) {
            throw new XtreemFSException("No StripingPolicy found for type:" + type);
        }
        return st;
    }

    /**
     * Blocks as long as the async xcap renewal is finished.
     */
    protected void waitForAsyncXcapRenewalFinished() {
        synchronized (xcapRenewalPendingLock) {
            if (xcapRenewalPending == true) {
                try {
                    xcapRenewalPendingLock.wait();
                } catch (InterruptedException ie) {
                    // TODO: find out what to do in this case;
                    return;
                }
            }
        }
    }

    protected XLocSet getXlocSet() {
        return fileInfo.getXLocSet();
    }

    @Override
    public List<Replica> getReplicasList() {
        return getXlocSet().getReplicasList();
    }

    @Override
    public Replica getReplica(int replicaIndex) {
        return getReplicasList().get(replicaIndex);
    }

    @Override
    public StripingPolicy getStripingPolicy() {
        return getStripingPolicy(0);
    }

    @Override
    public StripingPolicy getStripingPolicy(int replicaIndex) {
        return getReplica(replicaIndex).getStripingPolicy();
    }

    @Override
    public String getReplicaUpdatePolicy() {
        return getXlocSet().getReplicaUpdatePolicy();
    }

    @Override
    public long getNumObjects(UserCredentials userCredentials) throws IOException, AddressToUUIDNotFoundException,
            PosixErrorException {

        return Helper.getNumObjects(userCredentials, getAttr(userCredentials), getStripingPolicy());
    }

    @Override
    public boolean checkAndMarkIfReadOnlyReplicaComplete(int replicaIndex, UserCredentials userCredentials)
            throws IOException, AddressToUUIDNotFoundException {

        Replica replica = getReplica(replicaIndex);
        if (((replica.getReplicationFlags() & REPL_FLAG.REPL_FLAG_IS_COMPLETE.getNumber()) != 0)) {
            return true;
        }

        long lastObjectNo = getNumObjects(userCredentials) - 1;

        // Number of the first Object the OSD (with index "osdRelPos") should hold
        int osdRelPos = 0;

        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        fcBuilder.setXlocs(fileInfo.getXLocSet());
        fcBuilder.setXcap(getXcap());

        String fileId = Helper.extractGlobalFileIdFromXcap(fcBuilder.getXcap());

        for (String osdUUID : replica.getOsdUuidsList()) {

            UUIDIterator it = new UUIDIterator();
            it.addUUID(osdUUID);

            xtreemfs_internal_get_object_setRequest request = xtreemfs_internal_get_object_setRequest.newBuilder()
                    .setFileId(fileId).setFileCredentials(fcBuilder.build()).build();

            // Remark: GetObjectSetOperation does not validate views, thus no XLocSetHandler is required.
            ObjectList ol = RPCCaller.<xtreemfs_internal_get_object_setRequest, ObjectList> syncCall(SERVICES.OSD,
                    RPCAuthentication.userService, RPCAuthentication.authNone, volumeOptions, uuidResolver, it, false,
                    request, new CallGenerator<xtreemfs_internal_get_object_setRequest, ObjectList>() {
                        @Override
                        public RPCResponse<ObjectList> executeCall(InetSocketAddress server, Auth authHeader,
                                UserCredentials userCreds, xtreemfs_internal_get_object_setRequest input)
                                throws IOException, PosixErrorException {
                            return osdServiceClient.xtreemfs_internal_get_object_set(server, authHeader, userCreds,
                                    input);
                        }
                    });

            byte[] serializedBitSet = ol.getSet().toByteArray();
            ObjectSet oset = null;

            try {
                oset = new ObjectSet(ol.getStripeWidth(), ol.getFirst(), serializedBitSet);
            } catch (Exception e) {
                throw new IOException("cannot deserialize object set: " + e, e);
            }

            // check if the stripecolumn holds all Objects it should
            for (long objNo = osdRelPos; objNo <= lastObjectNo; objNo += replica.getStripingPolicy().getWidth()) {
                if (oset.contains(objNo) == false) {
                    return false;
                }
            }

            // increase osdRelPos for the next stripecolumn (stored on the next OSD)
            osdRelPos++;
        }
        try {
            volume.setXAttr(userCredentials, fileInfo.getPath(), "xtreemfs.mark_replica_complete",
                    replica.getOsdUuids(0), XATTR_FLAGS.XATTR_FLAGS_CREATE);
        } catch (IOException e) {
            // Couldn't mark replica as complete but it is complete, so just log it and ignore exception
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "unable to mark replica as complete: %s", e.toString());
        }
        return true;

    }

    @Override
    public int checkObjectAndGetSize(int replicaIndex, long objectNo) throws IOException, InvalidChecksumException {
        ObjectData od = null;
        RPCResponse<ObjectData> osdRsp = null;
        try {

            FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
            fcBuilder.setXlocs(fileInfo.getXLocSet());
            fcBuilder.setXcap(getXcap());

            String fileId = Helper.extractGlobalFileIdFromXcap(fcBuilder.getXcap());

            Replica r = getReplica(replicaIndex);
            String objOsd = r.getOsdUuids((int) objectNo % r.getStripingPolicy().getWidth());

            UUIDIterator it = new UUIDIterator();
            it.addUUID(objOsd);

            xtreemfs_check_objectRequest request = xtreemfs_check_objectRequest.newBuilder()
                    .setFileCredentials(fcBuilder.build()).setFileId(fileId).setObjectNumber(objectNo)
                    .setObjectVersion(0l).build();

            // Remark: CheckObjectOperation does not validate views, thus no XLocSetHandler is required.
            od = RPCCaller.<xtreemfs_check_objectRequest, ObjectData> syncCall(SERVICES.OSD,
                    RPCAuthentication.userService, RPCAuthentication.authNone, volumeOptions, uuidResolver, it, false,
                    request, new CallGenerator<xtreemfs_check_objectRequest, ObjectData>() {
                        @Override
                        public RPCResponse<ObjectData> executeCall(InetSocketAddress server, Auth authHeader,
                                UserCredentials userCreds, xtreemfs_check_objectRequest input) throws IOException,
                                PosixErrorException {
                            return osdServiceClient.xtreemfs_check_object(server, authHeader, userCreds, input);
                        }
                    });
        } catch (IOException e) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "IO error: %s", e.toString());
            throw new IOException("operation aborted", e);
        }

        if (od.getInvalidChecksumOnOsd()) {
            throw new InvalidChecksumException("object " + objectNo + " has an invalid checksum");
        }

        return od.getZeroPadding();
    }

    @Override
    public long getSizeOnOSD() throws IOException {

        // build FC
        FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
        fcBuilder.setXlocs(fileInfo.getXLocSet());
        fcBuilder.setXcap(getXcap());

        // get fileId
        String fileId = Helper.extractGlobalFileIdFromXcap(fcBuilder.getXcap());

        xtreemfs_internal_get_file_sizeRequest request = xtreemfs_internal_get_file_sizeRequest.newBuilder()
                .setFileCredentials(fcBuilder.build()).setFileId(fileId).build();

        xtreemfs_internal_get_file_sizeResponse response = RPCCaller
                .<xtreemfs_internal_get_file_sizeRequest, xtreemfs_internal_get_file_sizeResponse> syncCall(
                        SERVICES.OSD,
                        RPCAuthentication.userService,
                        RPCAuthentication.authNone,
                        volumeOptions,
                        uuidResolver,
                        osdUuidIterator,
                        false,
                        request,
                        new CallGenerator<xtreemfs_internal_get_file_sizeRequest, xtreemfs_internal_get_file_sizeResponse>() {

            @Override
            public RPCResponse<xtreemfs_internal_get_file_sizeResponse> executeCall(InetSocketAddress server,
                    Auth authHeader, UserCredentials userCreds, xtreemfs_internal_get_file_sizeRequest input)
                    throws IOException, PosixErrorException {
                
                                return osdServiceClient.xtreemfs_internal_get_file_size(server, authHeader, userCreds,
                                        input);
            }
        });
                
        assert (response != null);
        return response.getFileSize();
    }

    @Override
    public void repairObject(int replicaIndex, long objectNo) throws IOException {

        // check if file is replicated
        if (this.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE)) {
            // unable to repair object if file is not replicated
            throw new IOException("file must be replicated");
        } else {
            FileCredentials.Builder fcBuilder = FileCredentials.newBuilder();
            fcBuilder.setXlocs(fileInfo.getXLocSet());
            fcBuilder.setXcap(getXcap());

            String fileId = Helper.extractGlobalFileIdFromXcap(fcBuilder.getXcap());

            String objOsd = Helper.getOSDUUIDFromObjectNo(getReplica(replicaIndex), objectNo);

            UUIDIterator it = new UUIDIterator();
            it.addUUID(objOsd);

            xtreemfs_repair_objectRequest request = xtreemfs_repair_objectRequest.newBuilder()
                    .setFileCredentials(fcBuilder.build()).setFileId(fileId).setObjectNumber(objectNo)
                    .setObjectVersion(0l).build();

            emptyResponse response = RPCCaller.<xtreemfs_repair_objectRequest, emptyResponse> syncCall(SERVICES.OSD,
                    RPCAuthentication.userService, RPCAuthentication.authNone, volumeOptions, uuidResolver, it, false,
                    request, new CallGenerator<xtreemfs_repair_objectRequest, emptyResponse>() {
                        @Override
                        public RPCResponse<emptyResponse> executeCall(InetSocketAddress server, Auth authHeader,
                                UserCredentials userCreds, xtreemfs_repair_objectRequest input) throws IOException,
                                PosixErrorException {
                            return osdServiceClient.xtreemfs_repair_object(server, authHeader, userCreds, input);
                        }
                    });
        }
    }

    /**
     * Renew the xLocSet synchronously.<br>
     * If another renewal is in pending, no additional renewal will be started and the caller will wait until the
     * pending one finished.
     */
    void renewXLocSet() throws PosixErrorException, InternalServerErrorException, IOException {
        // Store the current xLocSet before entering the renewal mutex section.
        XLocSet xLocSetToRenew = fileInfo.getXLocSet();

        synchronized (fileInfo.xLocSetRenewalLock) {
            XLocSet xLocSetCurrent = fileInfo.getXLocSet();

            // Renew the xLocSet only if the xLocSet has not been renewed yet by another process.
            if (xLocSetCurrent.getVersion() <= xLocSetToRenew.getVersion()) {
                // The xCap is required to prevent unauthorized access to the XLocSet.
                XCap xCap = getXcap();
                xtreemfs_get_xlocsetRequest request = xtreemfs_get_xlocsetRequest.newBuilder().setXcap(xCap).build();

                XLocSet newXLocSet = RPCCaller.<xtreemfs_get_xlocsetRequest, XLocSet> syncCall(SERVICES.MRC,
                        userCredentialsBogus, authBogus, volumeOptions, uuidResolver, mrcUuidIterator, false, false, 1,
                        request, new CallGenerator<xtreemfs_get_xlocsetRequest, XLocSet>() {

                            @Override
                            public RPCResponse<XLocSet> executeCall(InetSocketAddress server, Auth authHeader,
                                    UserCredentials userCreds, xtreemfs_get_xlocsetRequest input) throws IOException,
                                    PosixErrorException {
                                return mrcServiceClient.xtreemfs_get_xlocset(server, authHeader, userCreds, input);
                            }
                        });

                fileInfo.updateXLocSetAndRest(newXLocSet);
            }
        }
    }
}
