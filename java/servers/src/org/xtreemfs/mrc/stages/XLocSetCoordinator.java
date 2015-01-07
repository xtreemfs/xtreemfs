/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.stages;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.Helper;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.operations.MRCOperation;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.osd.rwre.CoordinatedReplicaUpdatePolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.AuthoritativeReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_auth_stateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_xloc_set_invalidateResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * Changes to xLocSet can harm the data consistency. The XLocSetCoordinator assures that a xLocSet change is atomic from
 * a global point of view and that enough replicas are up to date to maintain consistency.<br>
 * Since database calls have to be exclusively from one process the coordinator calls the
 * {@link XLocSetCoordinatorCallback} in the context of the {@link ProcessingStage} when consistency in the new XLocSet
 * is assured.
 */
public class XLocSetCoordinator extends LifeCycleThread implements DBAccessResultListener<Object> {
    private enum RequestType {
        ADD_REPLICAS, REMOVE_REPLICAS
    };

    public final static String           XLOCSET_CHANGE_ATTR_KEY = "xlocsetchange";

    protected volatile boolean           quit;
    private final MRCRequestDispatcher   master;
    private BlockingQueue<RequestMethod> q;

    /** The lease timeout is needed to ensure no primary can exist after invalidating. */
    private final int                    leaseToMS;

    public XLocSetCoordinator(MRCRequestDispatcher master) {
        super("XLocSetCoordinator");
        quit = false;
        q = new LinkedBlockingQueue<RequestMethod>();
        this.master = master;

        leaseToMS = master.getConfig().getFleaseLeaseToMS();
    }

    @Override
    public void run() {
        notifyStarted();
        while (!quit) {
            try {
                RequestMethod m = q.take();
                processRequest(m);
            } catch (InterruptedException ex) {
                continue;
            } catch (Throwable ex) {
                this.notifyCrashed(ex);
                break;
            }
        }
        notifyStopped();
    }

    @Override
    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }

    /**
     * Choose the matching method and process the request.
     * 
     * ErrorHandling is done separately in the handleError method.
     * 
     * @param m
     */
    private void processRequest(RequestMethod m) throws InterruptedException {
        try {
            switch (m.getRequestType()) {
            case ADD_REPLICAS:
                processAddReplicas(m);
                break;
            case REMOVE_REPLICAS:
                processRemoveReplicas(m);
                break;
            default:
                throw new Exception("unknown stage operation");
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            handleError(m, e);
        }
    }

    /**
     * The RequestMethod keeps all the properties needed to coordinate a xLocSet change.
     */
    public final static class RequestMethod {

        /** The type of the coordination request **/
        RequestType                type;
        /** The MRCOPeration the coordination request is originating from. **/
        MRCOperation               op;
        /** The original client request asking to change the XLocSet. **/
        MRCRequest                 rq;
        /** The fileId whose XLocSet has to be changed. **/
        String                     fileId;
        /** A capability which will grant sufficient rights to invalidate and update replicas. **/
        Capability                 capability;
        /**
         * The callback which will be executed in the context of the {@link ProcessingStage} when the coordination is
         * finished.
         **/
        XLocSetCoordinatorCallback callback;
        /** The current XLocList. **/
        XLocList                   curXLocList;
        /** The new xLocList requested to be installed. **/
        XLocList                   newXLocList;

        public RequestMethod(RequestType type, String fileId, MRCRequest rq, MRCOperation op,
                XLocSetCoordinatorCallback callback, Capability cap, XLocList curXLocList, XLocList newXLocList) {
            this.type = type;
            this.op = op;
            this.rq = rq;
            this.fileId = fileId;
            this.callback = callback;
            this.capability = cap;
            this.curXLocList = curXLocList;
            this.newXLocList = newXLocList;
        }

        public RequestType getRequestType() {
            return type;
        }

        public String getFileId() {
            return fileId;
        }

        public MRCRequest getRequest() {
            return rq;
        }

        public MRCOperation getOperation() {
            return op;
        }

        public XLocSetCoordinatorCallback getCallback() {
            return callback;
        }

        public Capability getCapability() {
            return capability;
        }

        public XLocList getCurXLocList() {
            return curXLocList;
        }

        public XLocList getNewXLocList() {
            return newXLocList;
        }
    }

    public <O extends MRCOperation & XLocSetCoordinatorCallback> RequestMethod addReplicas(String fileId,
            FileMetadata file, XLocList curXLocList, XLocList extXLocList, MRCRequest rq, O op)
            throws DatabaseException {
        return addReplicas(fileId, file, curXLocList, extXLocList, rq, op, op);
    }

    public RequestMethod addReplicas(String fileId, FileMetadata file, XLocList curXLocList, XLocList extXLocList,
            MRCRequest rq, MRCOperation op, XLocSetCoordinatorCallback callback) throws DatabaseException {

        Capability cap = buildCapability(fileId, file);
        RequestMethod m = new RequestMethod(RequestType.ADD_REPLICAS, fileId, rq, op, callback, cap, curXLocList,
                extXLocList);

        return m;
    }

    private void processAddReplicas(final RequestMethod m) throws Throwable {
        final MRCRequest rq = m.getRequest();

        final String fileId = m.getFileId();
        final Capability cap = m.getCapability();

        final XLocList curXLocList = m.getCurXLocList();
        final XLocList extXLocList = m.getNewXLocList();

        final XLocSet curXLocSet = Converter.xLocListToXLocSet(curXLocList).build();
        // Ensure the next view won't be propagated until it is installed at the MRC.
        final XLocSet extXLocSet = Converter.xLocListToXLocSet(extXLocList).setVersion(curXLocSet.getVersion()).build();

        // TODO(jdillmann): Use centralized method to check if a lease is required.
        if (extXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ)
                || extXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE)
                || extXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA)) {

            // Invalidate the majority of the replicas and get their ReplicaStatus.
            int curNumRequiredAcks = calculateNumRequiredAcks(fileId, curXLocSet);
            ReplicaStatus[] states = invalidateReplicas(fileId, cap, curXLocSet, curNumRequiredAcks);

            // Calculate the AuthState and determine how many replicas have to be updated.
            AuthoritativeReplicaState authState = calculateAuthoritativeState(fileId, curXLocSet, states);
            Set<String> replicasUpToDate = calculateReplicasUpToDate(authState);

            // Calculate the number of required updates and send them an update request.
            int extNumRequiredAcks = calculateNumRequiredAcks(fileId, extXLocSet);
            updateReplicas(fileId, cap, extXLocSet, authState, extNumRequiredAcks, replicasUpToDate);

        } else if (extXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)) {
            // In case of the read-only replication the replicas will be invalidated. But the coordination takes place
            // at the client by libxtreemfs.
            // The INVALIDATION request will be send to every replica, but the coordination continues on the first
            // response.
            invalidateReplicas(fileId, cap, curXLocSet, 1);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "replication policy (%s) will be handled by VolumeImplementation",
                        extXLocSet.getReplicaUpdatePolicy());
            }
        } else {
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "Unknown replica update policy: "
                    + extXLocSet.getReplicaUpdatePolicy());
        }

        // Call the installXLocSet method in the context of the ProcessingStage.
        master.getProcStage().enqueueInternalCallbackOperation(rq, new InternalCallbackInterface() {
            @Override
            public void execute(MRCRequest rq) throws Throwable {
                m.getCallback().installXLocSet(rq, fileId, extXLocList, curXLocList);
            }
        });
    }

    public <O extends MRCOperation & XLocSetCoordinatorCallback> RequestMethod removeReplicas(String fileId,
            FileMetadata file, XLocList curXLocList, XLocList newXLocList, MRCRequest rq, O op)
            throws DatabaseException {
        return removeReplicas(fileId, file, curXLocList, newXLocList, rq, op, op);
    }

    public RequestMethod removeReplicas(String fileId, FileMetadata file, XLocList curXLocList, XLocList newXLocList,
            MRCRequest rq, MRCOperation op, XLocSetCoordinatorCallback callback) throws DatabaseException {

        Capability cap = buildCapability(fileId, file);
        RequestMethod m = new RequestMethod(RequestType.REMOVE_REPLICAS, fileId, rq, op, callback, cap, curXLocList,
                newXLocList);

        return m;
    }

    private void processRemoveReplicas(final RequestMethod m) throws Throwable {
        final MRCRequest rq = m.getRequest();

        final String fileId = m.getFileId();
        final Capability cap = m.getCapability();

        final XLocList curXLocList = m.getCurXLocList();
        final XLocList newXLocList = m.getNewXLocList();

        final XLocSet curXLocSet = Converter.xLocListToXLocSet(curXLocList).build();
        // Ensure the next view won't be propagated until it is installed at the MRC.
        final XLocSet newXLocSet = Converter.xLocListToXLocSet(newXLocList).setVersion(curXLocSet.getVersion()).build();

        
        if (curXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ)
                || curXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE)
                || curXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA)) {

            // Invalidate the majority of the replicas and get their ReplicaStatus
            int numRequiredAcks = calculateNumRequiredAcks(fileId, curXLocSet);
            ReplicaStatus[] states = invalidateReplicas(fileId, cap, curXLocSet, numRequiredAcks);

            // Calculate the AuthState and determine how many replicas have to be updated.
            AuthoritativeReplicaState authState = calculateAuthoritativeState(fileId, curXLocSet, states);
            Set<String> replicasUpToDate = calculateReplicasUpToDate(authState);

            // Remove the replicas, that will be removed from the xLocSet, from the set containing up to date replicas.
            HashSet<String> newReplicas = new HashSet<String>();
            for (int i = 0; i < newXLocSet.getReplicasCount(); i++) {
                String OSDUUID = Helper.getOSDUUIDFromXlocSet(curXLocSet, i, 0);
                newReplicas.add(OSDUUID);
            }

            Iterator<String> iterator = replicasUpToDate.iterator();
            while (iterator.hasNext()) {
                String OSDUUID = iterator.next();
                if (!newReplicas.contains(OSDUUID)) {
                    iterator.remove();
                }
            }

            // Calculate the number of required updates and send them an update request.
            int newNumRequiredAcks = calculateNumRequiredAcks(fileId, newXLocSet);
            updateReplicas(fileId, cap, newXLocSet, authState, newNumRequiredAcks, replicasUpToDate);

        } else if (curXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)) {
            // In case of the read-only replication the replicas will be invalidated. But the coordination takes place
            // at the client by libxtreemfs.
            // The INVALIDATION request will be send to every replica, but the coordination continues on the first
            // response.
            ReplicaStatus[] states = invalidateReplicas(fileId, cap, curXLocSet, 1);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "replication policy (%s) will be handled by VolumeImplementation",
                        curXLocSet.getReplicaUpdatePolicy());
            }
        } else {
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "Unknown replica update policy: "
                    + curXLocSet.getReplicaUpdatePolicy());
        }

        // Call the installXLocSet method in the context of the ProcessingStage.
        master.getProcStage().enqueueInternalCallbackOperation(rq, new InternalCallbackInterface() {
            @Override
            public void execute(MRCRequest rq) throws Throwable {
                m.getCallback().installXLocSet(rq, fileId, newXLocList, curXLocList);
            }
        });
    }

    /**
     * Lock the xLocSet for a file. The lock should be released, when the coordination succeeded or on crash recovery. <br>
     * 
     * Currently the lock is saving the {@link MRCRequestDispatcher#hashCode()} to be able to identify if a previous MRC
     * instance crashed while a xLocSet change was in progress.
     * 
     * @param file
     * @param sMan
     * @param update
     * @throws DatabaseException
     */
    public void lockXLocSet(FileMetadata file, StorageManager sMan, AtomicDBUpdate update) throws DatabaseException {
        byte[] hashValue = new byte[4];
        ByteBuffer hashByte = ByteBuffer.wrap(hashValue).putInt(master.hashCode());
        sMan.setXAttr(file.getId(), StorageManager.SYSTEM_UID, StorageManager.SYS_ATTR_KEY_PREFIX
                + XLocSetCoordinator.XLOCSET_CHANGE_ATTR_KEY, hashValue, update);
        hashByte.clear();
    }

    /**
     * Unlock the xLocSet for a file. This will clear the lock in the database.
     * 
     * @param file
     * @param sMan
     * @param update
     * @throws DatabaseException
     */
    public void unlockXLocSet(FileMetadata file, StorageManager sMan, AtomicDBUpdate update)
            throws DatabaseException {
        sMan.setXAttr(file.getId(), StorageManager.SYSTEM_UID, StorageManager.SYS_ATTR_KEY_PREFIX
                + XLocSetCoordinator.XLOCSET_CHANGE_ATTR_KEY, null, update);
    }

    /**
     * Check if the XLocSet of the file is locked and if the lock was acquired by a previous MRC instance. If this is
     * the case, it can be assumed the previous MRC crashed and the {@link XLocSetLock} should be released and replicas
     * revalidated.
     * 
     * @param file
     * @param sMan
     * @return XLocSetLock
     * @throws DatabaseException
     */
    public XLocSetLock getXLocSetLock(FileMetadata file, StorageManager sMan) throws DatabaseException {
        XLocSetLock lock;

        // Check if a hashCode has been saved to to the files XAttr to lock the xLocSet.
        byte[] prevHashBytes = sMan.getXAttr(file.getId(), StorageManager.SYSTEM_UID,
                StorageManager.SYS_ATTR_KEY_PREFIX + XLocSetCoordinator.XLOCSET_CHANGE_ATTR_KEY);
        if (prevHashBytes != null) {
            // Check if the saved hashCode differs from the current one. If this is the case, the MRC has crashed.
            ByteBuffer prevHashBuffer = ByteBuffer.wrap(prevHashBytes);
            int prevHash = prevHashBuffer.getInt();
            prevHashBuffer.clear();

            if (prevHash != master.hashCode()) {
                lock = new XLocSetLock(true, true);
            } else {
                lock = new XLocSetLock(true, false);
            }
        } else {
            lock = new XLocSetLock(false, false);
        }

        return lock;
    }


    /**
     * Build a valid Capability for the given file
     * 
     * @param fileId
     * @param file
     */
    private Capability buildCapability(String fileId, FileMetadata file) {
        // Build the Capability
        int accessMode = FileAccessManager.O_RDWR;
        int validity = master.getConfig().getCapabilityTimeout();
        long expires = TimeSync.getGlobalTime() / 1000 + master.getConfig().getCapabilityTimeout();

        String clientIdentity;
        try {
            clientIdentity = master.getConfig().getAddress() != null ? master.getConfig().getAddress().toString()
                    : InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            clientIdentity = "";
        }

        int epochNo = file.getEpoch();
        boolean replicateOnClose = false;
        SnapConfig snapConfig = SnapConfig.SNAP_CONFIG_SNAPS_DISABLED;
        long snapTimestamp = 0;
        String sharedSecret = master.getConfig().getCapabilitySecret();

        Capability cap = new Capability(fileId, accessMode, validity, expires, clientIdentity, epochNo,
                replicateOnClose, snapConfig, snapTimestamp, sharedSecret);
        return cap;
    }

    /**
     * Invalidate the majority of the replicas listed in xLocList. Will sleep until the lease has timed out if the
     * primary didn't respond.
     * 
     * @param fileId
     * @param capability
     * @param xLocSet
     * @param numAcksRequired
     * @throws InterruptedException
     * @throws MRCException
     */
    private ReplicaStatus[] invalidateReplicas(String fileId, Capability cap, XLocSet xLocSet, int numAcksRequired)
            throws InterruptedException, MRCException {

        @SuppressWarnings("unchecked")
        final RPCResponse<xtreemfs_xloc_set_invalidateResponse>[] responses = new RPCResponse[xLocSet
                .getReplicasCount()];

        // Send INVALIDATE requests to every replica in the set.
        OSDServiceClient client = master.getOSDClient();
        FileCredentials creds = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLocSet).build();
        for (int i = 0; i < responses.length; i++) {
            ServiceUUID OSDServiceUUID = new ServiceUUID(Helper.getOSDUUIDFromXlocSet(xLocSet, i, 0));
            try {
                responses[i] = client.xtreemfs_xloc_set_invalidate(OSDServiceUUID.getAddress(),
                        RPCAuthentication.authNone,
                        RPCAuthentication.userService, creds, fileId);
            } catch (IOException ex) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_DEBUG, this, ex);
                }
                throw new MRCException(ex);
            }
        }

        /**
         * This local class is used to collect responses to the asynchronous invalidate requests. <br>
         * It counts the number of errors and successful requests and stores an array of returned ReplicaStatus and a
         * flag indicating if the primary did respond.
         */
        class InvalidatedResponseListener implements RPCResponseAvailableListener<xtreemfs_xloc_set_invalidateResponse> {
            private int             numResponses     = 0;
            private int             numErrors        = 0;
            private boolean         primaryResponded = false;
            private RPCResponse<xtreemfs_xloc_set_invalidateResponse>[] responses;
            private ReplicaStatus[] states;

            public InvalidatedResponseListener(RPCResponse<xtreemfs_xloc_set_invalidateResponse>[] responses) {
                this.responses = responses;
                states = new ReplicaStatus[responses.length];

                for (int i = 0; i < responses.length; i++) {
                    responses[i].registerListener(this);
                }
            }

            @Override
            synchronized public void responseAvailable(RPCResponse<xtreemfs_xloc_set_invalidateResponse> r) {

                // Get the index of the responding OSD.
                int osdNum = -1;
                for (int i = 0; i < responses.length; i++) {
                    if (responses[i] == r) {
                        osdNum = i;
                        break;
                    }
                }
                assert (osdNum > -1);

                try {
                    xtreemfs_xloc_set_invalidateResponse response = r.get();

                    if (response.hasStatus()) {
                        states[osdNum] = response.getStatus();
                    }

                    if (response.getIsPrimary()) {
                        primaryResponded = true;
                    }

                    numResponses++;
                } catch (Exception ex) {
                    numErrors++;
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "no invalidated response from replica due to exception: %s", ex.toString());
                } finally {
                    r.freeBuffers();
                    this.notifyAll();
                }
            }

            synchronized ReplicaStatus[] getReplicaStates() {
                ReplicaStatus[] result = new ReplicaStatus[states.length];
                System.arraycopy(states, 0, result, 0, states.length);
                return result;
            }
        }
        InvalidatedResponseListener listener = new InvalidatedResponseListener(responses);

        // Wait until the majority responded.
        int numMaxErrors = responses.length - numAcksRequired;
        synchronized (listener) {
            while (listener.numResponses < numAcksRequired) {
                listener.wait();

                if (listener.numErrors > numMaxErrors) {
                    throw new MRCException(
                            "XLocSetCoordinator failed because too many replicas didn't respond to the invalidate request.");
                }
            }
        }

        // Wait until either the primary responded, every replica responded or the lease has timed out.
        // TODO(jdillmann): Return lease information while invalidating and wait only until the actual lease timed out
        // iff the primary didn't respond. This will also ensure no unnecessary time is spend waiting if no primary
        // exists at all.
        long now = System.currentTimeMillis();
        long leaseEndTimeMs = now + leaseToMS;
        synchronized (listener) {
            while (!listener.primaryResponded
                    && now < leaseEndTimeMs
                    && (listener.numResponses + listener.numErrors) < responses.length) {
                listener.wait(leaseEndTimeMs - now);
                now = System.currentTimeMillis();
            }
        }
        
        // Clone the states and return them.
        ReplicaStatus[] states = listener.getReplicaStates();
        return states;
    }

    /**
     * Send update requests to every replica in the xLocSet that is not already up to date.
     * 
     * @param fileId
     * @param cap
     * @param xLocSet
     * @param authState
     * @param numAcksRequired
     * @param replicasUpToDate
     * @throws InterruptedException
     * @throws MRCException
     */
    private void updateReplicas(String fileId, Capability cap, XLocSet xLocSet, AuthoritativeReplicaState authState,
            int numAcksRequired, Set<String> replicasUpToDate) throws InterruptedException, MRCException {
        final OSDServiceClient client = master.getOSDClient();

        // Create a list of OSDs which should be updated. Replicas that are up to date will be filtered.
        ArrayList<ServiceUUID> OSDServiceUUIDs = new ArrayList<ServiceUUID>();
        for (int i = 0; i < xLocSet.getReplicasCount(); i++) {
            String OSDUUID = Helper.getOSDUUIDFromXlocSet(xLocSet, i, 0);
            if (!replicasUpToDate.contains(OSDUUID.toString())) {
                OSDServiceUUIDs.add(new ServiceUUID(OSDUUID));
            }
        }

        // Build the authState request.
        FileCredentials fileCredentials = FileCredentials.newBuilder().setXlocs(xLocSet).setXcap(cap.getXCap()).build();
        xtreemfs_rwr_auth_stateRequest authStateRequest = xtreemfs_rwr_auth_stateRequest.newBuilder()
                .setFileId(fileId).setFileCredentials(fileCredentials).setState(authState).build();

        // Send the request to every replica not up to date yet.
        @SuppressWarnings("unchecked")
        final RPCResponse<emptyResponse>[] responses = new RPCResponse[OSDServiceUUIDs.size()];
        for (int i = 0; i < OSDServiceUUIDs.size(); i++) {
            try {
                final ServiceUUID OSDUUID = OSDServiceUUIDs.get(i);
                @SuppressWarnings("unchecked")
                final RPCResponse<emptyResponse> rpcResponse = client.xtreemfs_rwr_auth_state_invalidated(
                        OSDUUID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, authStateRequest);
                responses[i] = rpcResponse;
            } catch (IOException ex) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_DEBUG, this, ex);
                }
                throw new MRCException(ex);
            }
        }

        /**
         * This local class is used to collect responses to the asynchronous update requests.<br>
         * It counts the number of errors and successful requests.
         */
        class FetchInvalidatedResponseListener implements RPCResponseAvailableListener<emptyResponse> {
            private int numResponses = 0;
            private int numErrors    = 0;

            FetchInvalidatedResponseListener(RPCResponse<emptyResponse>[] responses) {
                for (int i = 0; i < responses.length; i++) {
                    responses[i].registerListener(this);
                }
            }

            @Override
            synchronized public void responseAvailable(RPCResponse<emptyResponse> r) {
                try {
                    r.get();
                    numResponses++;
                } catch (Exception ex) {
                    numErrors++;
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "no fetchInvalidated response from replica due to exception: %s", ex.toString());
                } finally {
                    r.freeBuffers();
                    this.notifyAll();
                }
            }
        }
        FetchInvalidatedResponseListener listener = new FetchInvalidatedResponseListener(responses);


        // Wait until the required number of updates has been successfully executed.
        int numRequiredUpdates = numAcksRequired - replicasUpToDate.size();
        int numMaxErrors = OSDServiceUUIDs.size() - numRequiredUpdates;
        synchronized (listener) {
            while (listener.numResponses < numRequiredUpdates) {
                listener.wait();

                if (listener.numErrors > numMaxErrors) {
                    throw new MRCException(
                            "XLocSetCoordinator failed because too many replicas didn't respond to the update request.");
                }
            }
        }
    }
    
    

    /**
     * Calculate the set of replicas that are up-to-date, which means they have the latest version of every
     * object associated to a file.
     * 
     * @param authState
     * @return set of replicas that are up-to-date
     */
    private Set<String> calculateReplicasUpToDate(AuthoritativeReplicaState authState) {

        HashMap<String, Integer> replicaObjCount = new HashMap<String, Integer>();

        int totalObjCount = 0;
        for (ObjectVersionMapping ovm : authState.getObjectVersionsList()) {
            if (ovm.getOsdUuidsCount() == 0) {
                continue;
            }
            
            totalObjCount++;

            for (String OSDUUID : ovm.getOsdUuidsList()) {
                Integer c = replicaObjCount.get(OSDUUID);
                c = (c == null) ? 1 : c + 1;
                replicaObjCount.put(OSDUUID, c);
            }
        }
        
        Iterator<Entry<String, Integer>> iter = replicaObjCount.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<String, Integer> e = iter.next();
            if (e.getValue() < totalObjCount) {
                iter.remove();
            }
        }

        return replicaObjCount.keySet();
    }

    /**
     * Calculate the AuthoritativeReplicaState from the ReplicaStatus returned by the INVALIDATE operation.
     * 
     * @param fileId
     * @param xLocSet
     * @param states
     * @return
     */
    private AuthoritativeReplicaState calculateAuthoritativeState(String fileId, XLocSet xLocSet, ReplicaStatus[] states) {
        // Generate a list of ServiceUUIDs from the XLocSet.
        int i;
        List<ServiceUUID> OSDUUIDs = new ArrayList<ServiceUUID>(xLocSet.getReplicasCount() - 1);
        for (i = 0; i < xLocSet.getReplicasCount() - 1; i++) {
            // Add each head OSDUUID to the list.
            OSDUUIDs.add(i, new ServiceUUID(Helper.getOSDUUIDFromXlocSet(xLocSet, i, 0)));
        }
        // Save the last OSD as the localUUID.
        String localUUID = Helper.getOSDUUIDFromXlocSet(xLocSet, i, 0);

        // Calculate an return the AuthoritativeReplicaState.
        return CoordinatedReplicaUpdatePolicy.CalculateAuthoritativeState(states, fileId, localUUID, OSDUUIDs);
    }

    /**
     * Calculate the number of required ACKS that are constituting a majority.
     * 
     * @param fileId
     * @param xLocSet
     * @return
     */
    private int calculateNumRequiredAcks(String fileId, XLocSet xLocSet) {
        /*
         * Essentially it is required to get ACKs from a majority for WqRq replicated files, 
         * but for WaR1 it is required to 
         *  - wait until every OSD is up to date in case of the addition of a replica
         *  - and to wait for the invalidation/installation of the view on the removed OSD and one other.
         *  
         * But at the moment every read/write policy is depending on a majority due to the lease mechanism, 
         * which results in an issue as seen at https://github.com/xtreemfs/xtreemfs/issues/235 but allows 
         * for simpler handling of ACKs for view coordination.
         */

        int numRequiredAcks = (int) Math.ceil(((double) xLocSet.getReplicasCount() + 1.0) / 2.0);
        return numRequiredAcks;

        // // Generate a list of ServiceUUIDs from the XLocSet.
        // int i;
        // List<ServiceUUID> OSDUUIDs = new ArrayList<ServiceUUID>(xLocSet.getReplicasCount() - 1);
        // for (i = 0; i < xLocSet.getReplicasCount() - 1; i++) {
        // // Add each head OSDUUID to the list.
        // OSDUUIDs.add(i, new ServiceUUID(Helper.getOSDUUIDFromXlocSet(xLocSet, i, 0)));
        // }
        // // Save the last OSD as the localUUID.
        // String localUUID = Helper.getOSDUUIDFromXlocSet(xLocSet, i, 0);
        //
        // // Create the policy without specifing a local OSD.
        // String replicaUpdatePolicy = xLocSet.getReplicaUpdatePolicy();
        // ReplicaUpdatePolicy policy = ReplicaUpdatePolicy.newReplicaUpdatePolicy(replicaUpdatePolicy, OSDUUIDs,
        // localUUID, fileId, null);
        //
        // if (!(policy instanceof CoordinatedReplicaUpdatePolicy)) {
        // throw new IllegalArgumentException("CoordinatedReplicaUpdatePolicy instance expected.");
        // }
        //
        // // Since the policy assumes that the localUUID's state is known, we have to wait for one more ACK on
        // operations
        // // that aren't executed from an OSD.
        // int numRequiredAcks = ((CoordinatedReplicaUpdatePolicy) policy).getNumRequiredAcks(null) + 1;
        // return numRequiredAcks;
    }

    /**
     * Add the RequestMethod saved as context in the local queue when the update operation completes. <br />
     * Attention the context of the DB operation calling this, has to be a {@link RequestMethod}.
     */
    @Override
    public void finished(Object result, Object context) {
        if (!(context instanceof RequestMethod)) {
            throw new RuntimeException(
                    "The XLocSetCoordinator DBAccessResultListener has to be called with a RequestMethod as the context.");
        }

        RequestMethod m = (RequestMethod) context;
        q.add(m);
    }

    @Override
    public void failed(Throwable error, Object context) {
        if (!(context instanceof RequestMethod)) {
            throw new RuntimeException(
                    "The XLocSetCoordinator DBAccessResultListener has to be called with a RequestMethod as the context.");
        }

        RequestMethod m = (RequestMethod) context;
        master.failed(error, m.getRequest());
    }

    // TODO(jdillmann): Share error reporting between ProcessingStage.parseAndExecure and this
    private void handleError(RequestMethod m, Throwable err) {
        MRCOperation op = m.getOperation();
        MRCRequest rq = m.getRequest();
        try {
            // simply rethrow the exception
            throw err;

        } catch (UserException exc) {
            reportUserError(op, rq, exc, exc.getErrno());

        } catch (MRCException exc) {
            Throwable cause = exc.getCause();
            if (cause instanceof DatabaseException
                    && ((DatabaseException) cause).getType() == ExceptionType.NOT_ALLOWED)
                reportUserError(op, rq, exc, POSIXErrno.POSIX_ERROR_EPERM);
            else
                reportServerError(op, rq, exc);

        } catch (DatabaseException exc) {
            if (exc.getType() == ExceptionType.NOT_ALLOWED) {
                reportUserError(op, rq, exc, POSIXErrno.POSIX_ERROR_EPERM);
            } else if (exc.getType() == ExceptionType.REDIRECT) {
                try {
                    redirect(rq,
                            exc.getAttachment() != null ? (String) exc.getAttachment() : master.getReplMasterUUID());
                } catch (MRCException e) {
                    reportServerError(op, rq, e);
                }
            } else
                reportServerError(op, rq, exc);

        } catch (Throwable exc) {
            reportServerError(op, rq, exc);
        }
    }

    private void reportUserError(MRCOperation op, MRCRequest rq, Throwable exc, POSIXErrno errno) {
        if (Logging.isDebug())
            Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
        op.finishRequest(rq, new ErrorRecord(ErrorType.ERRNO, errno, exc.getMessage(), exc));
    }

    private void reportServerError(MRCOperation op, MRCRequest rq, Throwable exc) {
        if (Logging.isDebug())
            Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
        op.finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                "An error has occurred at the MRC. Details: " + exc.getMessage(), exc));
    }

    private void redirect(MRCRequest rq, String uuid) {
        rq.getRPCRequest().sendRedirect(uuid);
    }

}
