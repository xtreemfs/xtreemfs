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
import java.util.LinkedList;
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
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.operations.MRCOperation;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.osd.rwre.CoordinatedReplicaUpdatePolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.LeaseState;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.AuthoritativeReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_auth_stateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_reset_statusRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_reset_statusResponse;
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
        XLOCSET_CHANGE
    };

    public final static String           XLOCSET_CHANGE_ATTR_KEY = "xlocsetchange";
    private final static long            STATUS_RETRY_DELAY_MS   = 5000;

    protected volatile boolean           quit;
    private final MRCRequestDispatcher   master;
    private final BlockingQueue<RequestMethod> q;

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
     * @param m
     */
    private void processRequest(RequestMethod m) throws InterruptedException {
        try {
            switch (m.getRequestType()) {
            case XLOCSET_CHANGE:
                processXLocSetChange(m);
                break;
            default:
                throw new Exception("unknown stage operation");
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            // Try to handle the error
            Logging.logError(Logging.LEVEL_WARN, this, e);
            try {
                m.getCallback().handleInstallXLocSetError(e, m.getFileId(), m.getNewXLocList(), m.getCurXLocList());
            } catch (Throwable e2) {
                Logging.logError(Logging.LEVEL_ERROR, this, e2);
            }
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
    

    /**
     * @see {@link XLocSetCoordinator#requestXLocSetChange(String, FileMetadata, XLocList, XLocList, MRCRequest, MRCOperation, XLocSetCoordinatorCallback)}
     * @param fileId
     * @param file
     * @param curXLocList
     * @param newXLocList
     * @param rq
     * @param op
     *            An MRCOperation that is also implementing the XLocSetCoordinatorCallback interface.
     * @return
     * @throws DatabaseException
     */
    public <O extends MRCOperation & XLocSetCoordinatorCallback> RequestMethod requestXLocSetChange(String fileId,
            FileMetadata file, XLocList curXLocList, XLocList newXLocList, MRCRequest rq, O op)
                    throws DatabaseException {
        return requestXLocSetChange(fileId, file, curXLocList, newXLocList, rq, op, op);
    }

    /**
     * Returns a {@link RequestMethod} which can be used a context for {@link StorageManager#createAtomicDBUpdate}, if
     * the DBAccessResultListener is a {@link XLocSetCoordinator}. The XLocSet modification will be coordinated when the
     * DBUpdate is done. <br>
     * 
     * It is advised to lock the xLocSet on the same update with {@link #lockXLocSet}.
     * 
     * @param fileId
     * @param file
     *            FileMetadata Object
     * @param curXLocList
     *            The current xLocList
     * @param newXLocList
     *            The to be installed xLocList
     * @param rq
     *            MRCRrequest
     * @param op
     *            MRCOperation
     * @param callback
     *            Called when the new XLocList can be safely installed
     * @return RequestMethod
     * @throws DatabaseException
     */
    public RequestMethod requestXLocSetChange(String fileId, FileMetadata file, XLocList curXLocList,
            XLocList newXLocList,
            MRCRequest rq, MRCOperation op, XLocSetCoordinatorCallback callback) throws DatabaseException {

        Capability cap = buildCapability(fileId, file);
        RequestMethod m = new RequestMethod(RequestType.XLOCSET_CHANGE, fileId, rq, op, callback, cap, curXLocList,
                newXLocList);

        return m;
    }

    private void processXLocSetChange(final RequestMethod m) throws Throwable {
        final String fileId = m.getFileId();
        final Capability cap = m.getCapability();

        final XLocList curXLocList = m.getCurXLocList();
        final XLocList newXLocList = m.getNewXLocList();

        final XLocSet curXLocSet = Converter.xLocListToXLocSet(curXLocList).build();
        // Ensure the next view won't be propagated until it is installed at the MRC.
        final XLocSet newXLocSet = Converter.xLocListToXLocSet(newXLocList).setVersion(curXLocSet.getVersion()).build();

        // TODO(jdillmann): Use centralized method to check if a lease is required.
        if (curXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ)
                || curXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE)
                || curXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA)) {

            // Invalidate the majority of the replicas, get their ReplicaStatus and calculate the AuthState.
            int numRequiredAcks = calculateNumRequiredAcks(fileId, curXLocSet);
            ReplicaStatus[] states = invalidateReplicas(fileId, cap, curXLocSet, numRequiredAcks);
            AuthoritativeReplicaState authState = calculateAuthoritativeState(fileId, curXLocSet, states);

            // Update the required number of replicas while they are invalidated.
            updateReplicas(fileId, cap, newXLocSet, curXLocSet, authState);

        } else if (curXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)) {
            // In case of the read-only replication the replicas will be invalidated. But the coordination takes place
            // at the client by libxtreemfs.
            // The INVALIDATION request will be send to every replica, but the coordination continues on the first
            // response.
            invalidateReplicas(fileId, cap, curXLocSet, 1);

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
        master.getProcStage().enqueueInternalCallbackOperation(new InternalCallbackInterface() {
            @Override
            public void execute() throws Throwable {
                final XLocSetCoordinatorCallback callback = m.getCallback();
                try {
                    callback.installXLocSet(fileId, newXLocList, curXLocList);
                } catch (Throwable e) {
                    callback.handleInstallXLocSetError(e, fileId, newXLocList, curXLocList);
                }
            }
        });
    }

    /**
     * Lock the xLocSet for a file. The lock should be released, when the coordination succeeded or on crash recovery. <br>
     * 
     * Currently the lock is saving the {@link MRCRequestDispatcher#hashCode()} to be able to identify if a previous MRC
     * instance crashed while a xLocSet change was in progress.
     * 
     * @param fileId
     * @param sMan
     * @param update
     * @throws DatabaseException
     */
    public void lockXLocSet(long fileId, StorageManager sMan, AtomicDBUpdate update) throws DatabaseException {
        byte[] hashValue = new byte[4];
        ByteBuffer hashByte = ByteBuffer.wrap(hashValue).putInt(master.hashCode());
        sMan.setXAttr(fileId, StorageManager.SYSTEM_UID, StorageManager.SYS_ATTR_KEY_PREFIX
                + XLocSetCoordinator.XLOCSET_CHANGE_ATTR_KEY, hashValue, update);
        hashByte.clear();
    }

    /**
     * @see #lockXLocSet(long, StorageManager, AtomicDBUpdate)
     */
    public void lockXLocSet(FileMetadata file, StorageManager sMan, AtomicDBUpdate update) throws DatabaseException {
        lockXLocSet(file.getId(), sMan, update);
    }

    /**
     * @see #lockXLocSet(long, StorageManager, AtomicDBUpdate)
     */
    public void lockXLocSet(String fileId, StorageManager sMan, AtomicDBUpdate update) throws DatabaseException,
            UserException {
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(fileId);
        lockXLocSet(idRes.getLocalFileId(), sMan, update);
    }

    /**
     * Unlock the xLocSet for a file. This will clear the lock in the database.
     * 
     * @param fileId
     * @param sMan
     * @param update
     * @throws DatabaseException
     */
    public void unlockXLocSet(long fileId, StorageManager sMan, AtomicDBUpdate update)
            throws DatabaseException {
        sMan.setXAttr(fileId, StorageManager.SYSTEM_UID, StorageManager.SYS_ATTR_KEY_PREFIX
                + XLocSetCoordinator.XLOCSET_CHANGE_ATTR_KEY, null, update);
    }

    /**
     * @see #unlockXLocSet(long, StorageManager, AtomicDBUpdate)
     */
    public void unlockXLocSet(FileMetadata file, StorageManager sMan, AtomicDBUpdate update) throws DatabaseException {
        unlockXLocSet(file.getId(), sMan, update);
    }

    /**
     * @see #unlockXLocSet(long, StorageManager, AtomicDBUpdate)
     */
    public void unlockXLocSet(String fileId, StorageManager sMan, AtomicDBUpdate update)
            throws DatabaseException, UserException {
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(fileId);
        unlockXLocSet(idRes.getLocalFileId(), sMan, update);
    }

    /**
     * Check if the XLocSet of the file is locked and if the lock was acquired by a previous MRC instance. If this is
     * the case, it can be assumed the previous MRC crashed and the {@link XLocSetLock} should be released and replicas
     * revalidated.
     * 
     * @param fileId
     * @param sMan
     * @return XLocSetLock
     * @throws DatabaseException
     */
    public XLocSetLock getXLocSetLock(long fileId, StorageManager sMan) throws DatabaseException {
        XLocSetLock lock;

        // Check if a hashCode has been saved to to the files XAttr to lock the xLocSet.
        byte[] prevHashBytes = sMan.getXAttr(fileId, StorageManager.SYSTEM_UID,
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
     * @see #getXLocSetLock(long, StorageManager)
     */
    public XLocSetLock getXLocSetLock(FileMetadata file, StorageManager sMan) throws DatabaseException {
        return getXLocSetLock(file.getId(), sMan);
    }

    /**
     * @see #getXLocSetLock(long, StorageManager)
     */
    public XLocSetLock getXLocSetLock(String fileId, StorageManager sMan) throws DatabaseException, UserException {
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(fileId);
        return getXLocSetLock(idRes.getLocalFileId(), sMan);
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
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "invalidateReplicas called with %d numAcksRequired",
                numAcksRequired);

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
                        RPCAuthentication.authNone, RPCAuthentication.userService, creds, fileId);
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
            private boolean         primaryExists    = false;
            private final RPCResponse<xtreemfs_xloc_set_invalidateResponse>[] responses;
            private final ReplicaStatus[] states;

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

                    if (response.hasReplicaStatus()) {
                        states[osdNum] = response.getReplicaStatus();
                    }

                    if (response.getLeaseState() == LeaseState.PRIMARY) {
                        primaryResponded = true;
                        primaryExists = true;
                    } else if (response.getLeaseState() == LeaseState.BACKUP) {
                        primaryExists = true;
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

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "invalidateReplicas invalidated %d replicas",
                    listener.numResponses);
        }

        // If a primary exists, wait until the lease has timed out.
        // This is required, since the lease can't be actively returned.
        // TODO (jdillmann): If the primary did response we could continue to update phase (but still would have to wait
        // until leaseTO before finally installing the new xLoc)
        synchronized (listener) {
            if (listener.primaryExists) {
                long now = System.currentTimeMillis();
                long leaseEndTimeMS = now + leaseToMS;
                
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "invalidateReplicas is waiting on the primary (now: %d, until: %d, timeout: %d ).", now,
                        leaseEndTimeMS, leaseToMS);

                while (now < leaseEndTimeMS) {
                    listener.wait(leaseEndTimeMS - now);
                    now = System.currentTimeMillis();
                }
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
     * @param newXLocSet
     * @param curXLocSet
     * @param authState
     * @throws InterruptedException
     * @throws MRCException
     */
    private void updateReplicas(String fileId, Capability cap, XLocSet newXLocSet, XLocSet curXLocSet,
            AuthoritativeReplicaState authState) throws InterruptedException, MRCException {
        final OSDServiceClient client = master.getOSDClient();

        // Create a list of OSDs which should be updated. Replicas that are up to date will be filtered.
        Set<String> replicasUpToDate = calculateReplicasUpToDate(authState);

        List<ServiceUUID> OSDServiceUUIDs = new ArrayList<ServiceUUID>();
        int numUpToDate = 0;
        for (int i = 0; i < newXLocSet.getReplicasCount(); i++) {
            String OSDUUID = Helper.getOSDUUIDFromXlocSet(newXLocSet, i, 0);

            if (replicasUpToDate.contains(OSDUUID.toString())) {
                numUpToDate++;
            } else  {
                OSDServiceUUIDs.add(new ServiceUUID(OSDUUID));
            }
        }

        // Calculate the number of required updates. OSDs already up to date will be ignored.
        int numAcksRequired = calculateNumRequiredAcks(fileId, newXLocSet);
        int numRequiredUpdates = numAcksRequired - numUpToDate;
        int numMaxErrors = OSDServiceUUIDs.size() - numRequiredUpdates;

        // Create the Union of the current and the new XLocSet to ensure setting the AuthState is successful.
        XLocSet unionXLocSet = xLocSetUnion(curXLocSet, newXLocSet);

        // Build the authState request.
        FileCredentials fileCredentials = FileCredentials.newBuilder().setXlocs(unionXLocSet).setXcap(cap.getXCap())
                .build();
        xtreemfs_rwr_auth_stateRequest authStateRequest = xtreemfs_rwr_auth_stateRequest.newBuilder()
                .setFileId(fileId).setFileCredentials(fileCredentials).setState(authState).build();

        // Send the request to every replica from new new XLocSet not up to date yet.
        @SuppressWarnings("unchecked")
        final RPCResponse<xtreemfs_rwr_reset_statusResponse>[] responses = new RPCResponse[OSDServiceUUIDs.size()];
        for (int i = 0; i < OSDServiceUUIDs.size(); i++) {
            try {
                final ServiceUUID OSDUUID = OSDServiceUUIDs.get(i);
                @SuppressWarnings("unchecked")
                final RPCResponse<xtreemfs_rwr_reset_statusResponse> rpcResponse = client
                        .xtreemfs_rwr_auth_state_invalidated(OSDUUID.getAddress(), RPCAuthentication.authNone,
                                RPCAuthentication.userService, authStateRequest);
                responses[i] = rpcResponse;
            } catch (IOException ex) {
                if (Logging.isDebug()) {
                    Logging.logError(Logging.LEVEL_DEBUG, this, ex);
                }
                throw new MRCException(ex);
            }
        }

        // Analyze the results and abort if too many errors occurred.
        RWRResetStatusResponseListener listener = new RWRResetStatusResponseListener(OSDServiceUUIDs, responses);
        int numRemainingUpdates = 0;
        List<ServiceUUID> OSDsInReset;
        synchronized (listener) {
            while (listener.numErrors <= numMaxErrors && !listener.allResponsesAvailable()) {
                listener.wait();
            }

            if (listener.numErrors > numMaxErrors) {
                throw new MRCException(
                        "XLocSetCoordinator failed because too many replicas didn't respond to the update request.");
            }

            numRemainingUpdates = numRequiredUpdates - listener.numComplete;
            // listener.OSDsInReset won't change anymore, since all responses are available.
            OSDsInReset = listener.OSDsInReset;
        }
        
        
        // If there are still required updates remaining, wait for the updates to complete and poll the reset status.
        if (numRemainingUpdates > 0) {
            xtreemfs_rwr_reset_statusRequest resetStatusRequest = xtreemfs_rwr_reset_statusRequest.newBuilder()
                    .setFileId(fileId).setFileCredentials(fileCredentials).setState(authState).build();
            boolean complete = waitForReset(OSDsInReset, numRemainingUpdates, resetStatusRequest);

            if (!complete) {
                throw new MRCException(
                        "XLocSetCoordinator failed because too many replicas didn't complete the update request.");
            }
        }
    }

    private boolean waitForReset(List<ServiceUUID> OSDServiceUUIDs, int numRequiredUpdates,
            final xtreemfs_rwr_reset_statusRequest resetStatusRequest) throws MRCException, InterruptedException {
        final OSDServiceClient client = master.getOSDClient();

        RWRResetStatusResponseListener listener;
        int numMaxErrors;
        long nextPollTime = System.currentTimeMillis() + STATUS_RETRY_DELAY_MS;
        // long nextPollTime = -1;

        while (numRequiredUpdates <= OSDServiceUUIDs.size()) {
            // delay the next poll.
            long now = System.currentTimeMillis();
            if (now < nextPollTime) {
                sleep(nextPollTime - now);
            }
            nextPollTime = System.currentTimeMillis() + STATUS_RETRY_DELAY_MS;

            listener = new RWRResetStatusResponseListener(OSDServiceUUIDs, resetStatusRequest, client);
            numMaxErrors = OSDServiceUUIDs.size() - numRequiredUpdates;

            synchronized (listener) {
                // Wait until every request is finished.
                while (!listener.allResponsesAvailable()) {
                    listener.wait();

                    // Return on success.
                    if (listener.numComplete >= numRequiredUpdates) {
                        return true;
                    }

                    // Abort if too many errors occured.
                    if (listener.numErrors > numMaxErrors) {
                        return false;
                    }
                }

                // Adapt to the responses and poll the status after a delay.
                OSDServiceUUIDs = listener.OSDsInReset;
                numRequiredUpdates = numRequiredUpdates - listener.numComplete;
            }
        }

        return false;
    }

    /**
     * This local class is used to collect responses to the asynchronous status requests.<br>
     * It counts the number of errors and successful requests.
     */
    private static class RWRResetStatusResponseListener
            implements RPCResponseAvailableListener<xtreemfs_rwr_reset_statusResponse> {
        private int                                              numAcks     = 0;
        private int                                              numErrors   = 0;
        private RPCResponse<xtreemfs_rwr_reset_statusResponse>[] responses;

        private List<ServiceUUID>                                OSDServiceUUIDs;
        private List<ServiceUUID>                                OSDsInReset;

        private int                                              numComplete  = 0;

        public RWRResetStatusResponseListener(List<ServiceUUID> OSDServiceUUIDs,
                RPCResponse<xtreemfs_rwr_reset_statusResponse>[] responses) throws MRCException {
            this.OSDServiceUUIDs = OSDServiceUUIDs;
            this.responses = responses;

            // Attach this as the listener.
            OSDsInReset = new LinkedList<ServiceUUID>();
            for (int i = 0; i < responses.length; i++) {
                responses[i].registerListener(this);
            }
        }

        @SuppressWarnings("unchecked")
        public RWRResetStatusResponseListener(List<ServiceUUID> OSDServiceUUIDs,
                xtreemfs_rwr_reset_statusRequest resetStatusRequest, final OSDServiceClient client)
                        throws MRCException {
            this.OSDServiceUUIDs = OSDServiceUUIDs;

            // Send the request to every replica from new new XLocSet not up to date yet.
            responses = new RPCResponse[OSDServiceUUIDs.size()];
            for (int i = 0; i < OSDServiceUUIDs.size(); i++) {
                try {
                    final ServiceUUID OSDUUID = OSDServiceUUIDs.get(i);
                    final RPCResponse<xtreemfs_rwr_reset_statusResponse> rpcResponse = client.xtreemfs_rwr_reset_status(
                            OSDUUID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                            resetStatusRequest);
                    responses[i] = rpcResponse;
                } catch (IOException ex) {
                    if (Logging.isDebug()) {
                        Logging.logError(Logging.LEVEL_DEBUG, this, ex);
                    }
                    throw new MRCException(ex);
                }
            }

            // Attach this as the listener.
            OSDsInReset = new LinkedList<ServiceUUID>();
            for (int i = 0; i < responses.length; i++) {
                responses[i].registerListener(this);
            }
        }

        @Override
        synchronized public void responseAvailable(RPCResponse<xtreemfs_rwr_reset_statusResponse> r) {
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
                xtreemfs_rwr_reset_statusResponse response = r.get();
                numAcks++;

                if (response.getRunning()) {
                    ServiceUUID osd = OSDServiceUUIDs.get(osdNum);
                    OSDsInReset.add(osd);
                } else if (response.getComplete()) {
                    numComplete++;
                }
            } catch (Exception ex) {
                numErrors++;
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "no response from replica due to exception: %s", ex.toString());
            } finally {
                r.freeBuffers();
                this.notifyAll();
            }
        }

        synchronized boolean allResponsesAvailable() {
            return ((numAcks + numErrors) == responses.length);
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

        int numRequiredAcks = xLocSet.getReplicasCount() / 2 + 1;
        return numRequiredAcks;
    }

    /**
     * Build the union of both XLocSets if they have the ReplicaUpdatePolicy and none of the replicas is striped.
     * Replicas of the first XLocSet are kept in order, then non duplicate Replicas of the second XLocSet are
     * appended in order. The version is inherited from xLocSet1.
     * 
     * @throws RuntimeException
     *             if assertions are enabled and a replica is striped.
     * 
     * @param xLocSet1
     * @param xLocSet2
     * @return Union of xLocSet1 and xLocSet2
     */
    private XLocSet xLocSetUnion(XLocSet xLocSet1, XLocSet xLocSet2) {
        assert(xLocSet1.getReadOnlyFileSize() == xLocSet2.getReadOnlyFileSize());
        assert(xLocSet1.getReplicaUpdatePolicy().equals(xLocSet2.getReplicaUpdatePolicy()));
        
        XLocSet.Builder xLocSetBuilder = XLocSet.newBuilder()
                .setReadOnlyFileSize(xLocSet1.getReadOnlyFileSize())
                .setReplicaUpdatePolicy(xLocSet1.getReplicaUpdatePolicy())
                .setVersion(xLocSet1.getVersion());
        
        for (int i = 0; i < xLocSet1.getReplicasCount(); i++) {
            Replica replica = xLocSet1.getReplicas(i);
            assert (replica.getStripingPolicy().getWidth() == 1);
            xLocSetBuilder.addReplicas(replica);
        }

        
        HashSet<String> osdUUIDSet = new HashSet<String>();
        osdUUIDSet.addAll(Helper.getOSDUUIDsFromXlocSet(xLocSet1));
        for (int i = 0; i < xLocSet2.getReplicasCount(); i++) {
            Replica replica = xLocSet2.getReplicas(i);
            assert (replica.getStripingPolicy().getWidth() == 1);
            if (!osdUUIDSet.contains(replica.getOsdUuids(0))) {
                xLocSetBuilder.addReplicas(replica);
            }
        }
        
        return xLocSetBuilder.build();
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

        // The request can be finished as soon as the xlocset is locked.
        master.finished(result, m.getRequest());
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
}
