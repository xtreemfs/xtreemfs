/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.stages;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCCallbackRequest;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.operations.MRCOperation;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.osd.rwre.CoordinatedReplicaUpdatePolicy;
import org.xtreemfs.osd.rwre.WaR1UpdatePolicy;
import org.xtreemfs.osd.rwre.WaRaUpdatePolicy;
import org.xtreemfs.osd.rwre.WqRqUpdatePolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.AuthoritativeReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersion;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_fetch_invalidatedRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_xloc_set_invalidateResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

public class XLocSetCoordinator extends LifeCycleThread implements DBAccessResultListener<Object> {
    private enum RequestType {
        ADD_REPLICAS, REMOVE_REPLICAS, REPLACE_REPLICA
    };

    public final static String           XLOCSET_CHANGE_ATTR_KEY = "XLocSetChange";

    protected volatile boolean           quit;
    private final MRCRequestDispatcher   master;
    private BlockingQueue<RequestMethod> q;

    public XLocSetCoordinator(MRCRequestDispatcher master) {
        super("XLocSetCoordinator");
        quit = false;
        q = new LinkedBlockingQueue<RequestMethod>();
        this.master = master;
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
            // case REPLACE_REPLICA:
            // processReplaceReplica(m);
            default:
                throw new Exception("unknown stage operation");
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            handleError(m, e);
        }
    }

    public final static class RequestMethod {

        RequestType                type;
        MRCOperation               op;
        MRCRequest                 rq;
        String                     fileId;
        Capability                 capability;
        XLocSetCoordinatorCallback callback;
        Object[]                   arguments;

        public RequestMethod(RequestType type, String fileId, MRCRequest rq, MRCOperation op,
                XLocSetCoordinatorCallback callback, Capability cap, Object[] args) {
            this.type = type;
            this.op = op;
            this.rq = rq;
            this.fileId = fileId;
            this.callback = callback;
            this.capability = cap;
            this.arguments = args;
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

        public Object[] getArguments() {
            return arguments;
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
        RequestMethod m = new RequestMethod(RequestType.ADD_REPLICAS, fileId, rq, op, callback, cap, new Object[] {
                curXLocList, extXLocList });

        return m;
    }

    private void processAddReplicas(final RequestMethod m) throws Throwable {
        final MRCRequest rq = m.getRequest();

        final String fileId = m.getFileId();
        final Capability cap = m.getCapability();

        final XLocList curXLocList = (XLocList) m.getArguments()[0];
        final XLocList extXLocList = (XLocList) m.getArguments()[1];

        final XLocSet curXLocSet = Converter.xLocListToXLocSet(curXLocList).build();
        final XLocSet extXLocSet = Converter.xLocListToXLocSet(extXLocList).build();

        if (extXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ)) {
            addReplicasWqRq(fileId, cap, curXLocSet, extXLocSet);
        } else {

            // Invalidate the read-only replicas to make the behavior more consistent with read-write replication.
            // This is unnecessary because the state of read-only data can't change and a replica that had the correct
            // data once will never be superseded.
            // TODO(jdillmann): Discuss if this should be kept or not.
            invalidateReplicas(fileId, cap, curXLocSet);

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "replication policy (%s) will be handled by VolumeImplementation",
                        extXLocSet.getReplicaUpdatePolicy());
            }
        }

        // Call the installXLocSet method in the context of the ProcessingStage
        MRCCallbackRequest callbackRequest = new MRCCallbackRequest(rq.getRPCRequest(),
                new InternalCallbackInterface() {
                    @Override
                    public void startCallback(MRCRequest rq) throws Throwable {
                        m.getCallback().installXLocSet(rq, fileId, extXLocList, curXLocList);
                    }
                });

        master.getProcStage().enqueueOperation(callbackRequest, ProcessingStage.STAGEOP_INTERNAL_CALLBACK, null);
    }

    private void addReplicasWqRq(String fileId, Capability cap, XLocSet curXLocSet, XLocSet extXLocSet) throws Throwable {

        ArrayList<ServiceUUID> OSDUUIDs = new ArrayList<ServiceUUID>(extXLocSet.getReplicasCount());
        for (int i = 0; i < extXLocSet.getReplicasCount(); i++) {
            // save each head OSDUUID to the list
            OSDUUIDs.add(i, new ServiceUUID(Helper.getOSDUUIDFromXlocSet(extXLocSet, i, 0)));
        }

        // Invalidate the majority of the replicas and get their ReplicaStatus
        ReplicaStatus[] states = invalidateReplicas(fileId, cap, curXLocSet);

        // Calculate the AuthState and the minimal majority perceived
        CoordinatedReplicaUpdatePolicy policy = getMockupPolicy(extXLocSet.getReplicaUpdatePolicy(), fileId, OSDUUIDs);
        AuthoritativeReplicaState authState = policy.CalculateAuthoritativeState(states, fileId);
        int minMajority = calculateMinimalMajority(states, authState);

        // Determine how many Replicas have to be read
        int requiredRead;
        if (policy.backupCanRead()) {
            requiredRead = 1;
        } else {
            // since the RequiredAcks excludes the local Replica we have to add it explicitly
            requiredRead = policy.getNumRequiredAcks(null) + 1;
        }

        // Calculate the requiredUpdates
        // TODO(jdillmann): explain the formula (R + W > N, W'' > N - R - W', W'' = N - R - W' + 1)
        // W = N - R + 1 <=> R + W - 1 = N => R + W > N
        int requiredUpdates = extXLocSet.getReplicasCount() - minMajority - requiredRead + 1;

        if (requiredUpdates > 0) {
            assert (requiredUpdates <= (extXLocSet.getReplicasCount() - curXLocSet.getReplicasCount()));

            // build the FileCredentials
            FileCredentials fileCredentials = FileCredentials.newBuilder().setXlocs(extXLocSet).setXcap(cap.getXCap())
                    .build();

            // build the fetch request
            xtreemfs_rwr_fetch_invalidatedRequest fiRequest = xtreemfs_rwr_fetch_invalidatedRequest.newBuilder()
                    .setFileId(fileId).setFileCredentials(fileCredentials).setState(authState).build();

            // TODO(jdillmann): make this robust and use a diff of the ext/cur set
            // take the new replicas from the end of the list
            for (ServiceUUID OSDUUID : OSDUUIDs.subList(OSDUUIDs.size() - requiredUpdates, OSDUUIDs.size())) {

                // TODO(jdillmann): check if there is any sane way to use libxtreemfs at the mrc
                @SuppressWarnings("unchecked")
                RPCResponse<emptyResponse> rpcResponse = master.getOSDClient().xtreemfs_rwr_fetch_invalidated(
                        OSDUUID.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fiRequest);
                rpcResponse.get();
                rpcResponse.freeBuffers();
            }
        }
    }

    // If the read operation is triggered from the client side via libxtreemfs it behaves different from the call made
    // here. To retain compatibility the ping will remain at the VolumeImplementation.
//    private void addReplicasRo(String fileId, Capability cap, XLocSet curXLocSet, XLocSet extXLocSet) throws Throwable {
//
//        // extending the replica set won't break any functionality. Access based on an older XLocSet will still be
//        // correct, as long as the stripes are staying on the same OSDs
//
//        List<Replica> newReplicas = new ArrayList<Replica>(extXLocSet.getReplicasList());
//        newReplicas.removeAll(curXLocSet.getReplicasList());
//        assert (extXLocSet.getReplicasCount() - curXLocSet.getReplicasCount() == newReplicas.size());
//
//        for (Replica replica : newReplicas) {
//            
//            // Check replication flags, if it's a full replica.
//            if ((replica.getReplicationFlags() & REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber()) == 0) {
//                // Nothing to do here because the replication does not need to be triggered for partial replicas.
//                continue;
//            }
//
//            FileCredentials.Builder fileCredentialsBuilder = FileCredentials.newBuilder()
//                    .setXcap(cap.getXCap())
//                    .setXlocs(extXLocSet);
//
//            readRequest.Builder readRequestBuilder = readRequest.newBuilder()
//                    .setFileId(fileId)
//                    .setFileCredentials(fileCredentialsBuilder);
//
//            // Read one byte from the replica to trigger the replication.
//            readRequestBuilder
//                    .setObjectNumber(0)
//                    .setObjectVersion(0)
//                    .setOffset(0)
//                    .setLength(1);
//
//            String headOSD = replica.getOsdUuids(0);
//            InetSocketAddress server = new ServiceUUID(headOSD).getAddress();
//
//            // TODO(jdillmann): check if there is any sane way to use libxtreemfs at the mrc
//            RPCResponse<OSD.ObjectData> rpcResponse = master.getOSDClient().read(server, RPCAuthentication.authNone,
//                    RPCAuthentication.userService, readRequestBuilder.build());
//            rpcResponse.get();
//            rpcResponse.freeBuffers();
//
//            // TODO(jdillmann): Every OSD in a striped replica has to be pinged.
//        }
//    }

    public <O extends MRCOperation & XLocSetCoordinatorCallback> RequestMethod removeReplicas(String fileId,
            FileMetadata file, XLocList curXLocList, XLocList newXLocList, MRCRequest rq, O op)
            throws DatabaseException {
        return removeReplicas(fileId, file, curXLocList, newXLocList, rq, op, op);
    }

    public RequestMethod removeReplicas(String fileId, FileMetadata file, XLocList curXLocList, XLocList newXLocList,
            MRCRequest rq, MRCOperation op, XLocSetCoordinatorCallback callback) throws DatabaseException {

        Capability cap = buildCapability(fileId, file);
        RequestMethod m = new RequestMethod(RequestType.REMOVE_REPLICAS, fileId, rq, op, callback, cap, new Object[] {
                curXLocList, newXLocList });

        return m;
    }

    private void processRemoveReplicas(final RequestMethod m) throws Throwable {
        final MRCRequest rq = m.getRequest();

        final String fileId = m.getFileId();
        final Capability cap = m.getCapability();

        final XLocList curXLocList = (XLocList) m.getArguments()[0];
        final XLocList newXLocList = (XLocList) m.getArguments()[1];

        final XLocSet curXLocSet = Converter.xLocListToXLocSet(curXLocList).build();
        final XLocSet newXLocSet = Converter.xLocListToXLocSet(newXLocList).build();

        
        if (curXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ)) {
            removeReplicasWqRq(fileId, cap, curXLocSet, newXLocSet);
        } else {
            // In case of the read-only replication the replicas will be invalidated. But the coordination takes place
            // at the client by libxtreemfs.
            ReplicaStatus[] states = invalidateReplicas(fileId, cap, curXLocSet);
        }

        // Call the installXLocSet method in the context of the ProcessingStage
        MRCCallbackRequest callbackRequest = new MRCCallbackRequest(rq.getRPCRequest(),
                new InternalCallbackInterface() {
                    @Override
                    public void startCallback(MRCRequest rq) throws Throwable {
                        m.getCallback().installXLocSet(rq, fileId, newXLocList, curXLocList);
                    }
                });

        master.getProcStage().enqueueOperation(callbackRequest, ProcessingStage.STAGEOP_INTERNAL_CALLBACK, null);

    }

    private void removeReplicasWqRq(String fileId, Capability cap, XLocSet curXLocSet, XLocSet newXLocSet)
            throws Throwable {
        // Invalidate the majority of the replicas and get their ReplicaStatus
        ReplicaStatus[] states = invalidateReplicas(fileId, cap, curXLocSet);

        // Build a mockupPolicy for the current XLocSet
        ArrayList<ServiceUUID> curOSDUUIDs = new ArrayList<ServiceUUID>(curXLocSet.getReplicasCount());
        for (int i = 0; i < curXLocSet.getReplicasCount(); i++) {
            // save each head OSDUUID to the list
            curOSDUUIDs.add(i, new ServiceUUID(Helper.getOSDUUIDFromXlocSet(curXLocSet, i, 0)));
        }
        CoordinatedReplicaUpdatePolicy curPolicy = getMockupPolicy(curXLocSet.getReplicaUpdatePolicy(), fileId,
                curOSDUUIDs);

        // If the backup can read we can assume every replica will be written on updates.
        if (!curPolicy.backupCanRead()) {

            // calculate the current AuthState.
            AuthoritativeReplicaState authState = curPolicy.CalculateAuthoritativeState(states, fileId);

            // Create a policy for the new XLocSet and calculate the minimal reads,
            ArrayList<ServiceUUID> newOSDUUIDs = new ArrayList<ServiceUUID>(newXLocSet.getReplicasCount());
            for (int i = 0; i < newXLocSet.getReplicasCount(); i++) {
                // save each head OSDUUID to the list
                newOSDUUIDs.add(i, new ServiceUUID(Helper.getOSDUUIDFromXlocSet(newXLocSet, i, 0)));
            }

            CoordinatedReplicaUpdatePolicy newPolicy = getMockupPolicy(newXLocSet.getReplicaUpdatePolicy(), fileId,
                    newOSDUUIDs);
            int requiredRead = newPolicy.getNumRequiredAcks(null) + 1;

            // Create a Set containing the remaining UUIDs.
            final HashSet<String> newXLocSetUUIDs = new HashSet<String>(newXLocSet.getReplicasCount());
            for (int i = 0; i < newXLocSet.getReplicasCount(); i++) {
                // TODO(jdillmann): Currently using the head OSD. Not sure what to do with striping.
                String osdUUID = Helper.getOSDUUIDFromXlocSet(newXLocSet, i, 0);
                newXLocSetUUIDs.add(osdUUID);
            }

            // Create a Set containing the UUIDs of replicas that have been removed.
            final HashSet<String> removedOSDUUIDs = new HashSet<String>(curXLocSet.getReplicasCount()
                    - newXLocSet.getReplicasCount());
            for (int i = 0; i < curXLocSet.getReplicasCount(); i++) {
                String osdUUID = Helper.getOSDUUIDFromXlocSet(curXLocSet, i, 0);
                if (!newXLocSetUUIDs.contains(osdUUID)) {
                    removedOSDUUIDs.add(osdUUID);
                }
            }

            // Create a UUID set containing UUIDs of OSDs missing some objects, that could harm the invariant.
            // Since all the missing objects will be fetched, even OSDs missing more than one object have to be added
            // only once.
            HashSet<String> fetchUUIDs = new HashSet<String>();

            // Check the AuthState for missing objects and find the OSDs missing them.
            // TODO(jdillmann): TruncateLog ?
            for (ObjectVersionMapping ovm : authState.getObjectVersionsList()) {
                HashSet<String> osdUUIDs = new HashSet<String>(ovm.getOsdUuidsCount());
                for (String UUID : ovm.getOsdUuidsList()) {
                    if (!removedOSDUUIDs.contains(UUID)) {
                        osdUUIDs.add(UUID);
                    }
                }

                // W + requiredRead > N <= W = N - requiredRead + 1
                // == (requiredWrite + minMajority) = N - requiredRead + 1
                // == requiredWrite = N - requiredRead - minMajority +1
                int N = newXLocSet.getReplicasCount();
                int minMajority = osdUUIDs.size();
                int requiredWrite = N - requiredRead - minMajority + 1;

                // if the removal would harm the invariant update an OSD, which hasn't the latest data yet.
                if (requiredWrite > 0) {

                    int i = 0;
                    for (String UUID : newXLocSetUUIDs) {
                        if (!osdUUIDs.contains(UUID)) {
                            i++;
                            fetchUUIDs.add(UUID);

                            if (i == requiredWrite) {
                                break;
                            }
                        }
                    }

                    assert (i == requiredWrite);
                }
            }

            // Instruct the Replicas, that are not up to date to fetch all the data.
            if (fetchUUIDs.size() > 0) {
                // build the FileCredentials
                FileCredentials fileCredentials = FileCredentials.newBuilder().setXlocs(curXLocSet)
                        .setXcap(cap.getXCap()).build();

                // build the fetch request
                xtreemfs_rwr_fetch_invalidatedRequest fiRequest = xtreemfs_rwr_fetch_invalidatedRequest.newBuilder()
                        .setFileId(fileId).setFileCredentials(fileCredentials).setState(authState).build();

                // and send the request to the OSDs missing some data
                for (String OSDUUID : fetchUUIDs) {
                    ServiceUUID service = new ServiceUUID(OSDUUID);
                    // TODO(jdillmann): check if there is any sane way to use libxtreemfs at the mrc
                    @SuppressWarnings("unchecked")
                    RPCResponse<emptyResponse> rpcResponse = master.getOSDClient().xtreemfs_rwr_fetch_invalidated(
                            service.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, fiRequest);
                    rpcResponse.get();
                    rpcResponse.freeBuffers();
                }
            }
        }

        // It is now guaranteed, that the invariant won't be harmed by removing the Replicas.
    }

    // public void replaceReplica(String fileId, MRCOperation op, MRCRequest rq) {
    // q.add(new RequestMethod(RequestType.REPLACE_REPLICA, fileId, op, rq));
    // }
    //
    // private void processReplaceReplica(RequestMethod m) throws Throwable {
    // throw new NotImplementedException();
    // }

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

        // TODO(jdillmann): check correct MRC address
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
     * Invalidate the Replicas listed in xLocList. Will sleep until the lease has timed out if the primary
     * didn't respond
     * 
     * @param fileId
     * @param capability
     * @param xLocSet
     * @throws Throwable
     */
    private ReplicaStatus[] invalidateReplicas(String fileId, Capability cap, XLocSet xLocSet) throws Throwable {

        FileCredentials creds = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLocSet).build();

        // Invalidate the replicas (the majority should be enough)
        RPCResponse<xtreemfs_xloc_set_invalidateResponse> rpcResponse;
        xtreemfs_xloc_set_invalidateResponse response = null;
        OSDServiceClient client = master.getOSDClient();

        boolean primaryResponded = false;
        int responseCount = 0;

        ReplicaStatus[] states = new ReplicaStatus[xLocSet.getReplicasCount()];

        // TODO(jdillmann): Use RPCResponseAvailableListener @see CoordinatedReplicaUpdatePolicy.executeReset
        for (int i = 0; i < xLocSet.getReplicasCount(); i++) {

            Replica replica = xLocSet.getReplicas(i);
            InetSocketAddress server = new ServiceUUID(replica.getOsdUuids(0)).getAddress();

            try {
                rpcResponse = client.xtreemfs_xloc_set_invalidate(server, RPCAuthentication.authNone,
                        RPCAuthentication.userService, creds, fileId);
                response = rpcResponse.get();
                responseCount = responseCount + 1;

                if (response.hasStatus()) {
                    states[i] = response.getStatus();
                }

                if (response.getIsPrimary()) {
                    primaryResponded = true;
                }

            } catch (InterruptedException e) {
                if (quit) {
                    throw e;
                }
            } catch (IOException e) {
                // TODO(jdillmann): Do something with the error
                if (Logging.isDebug())
                    Logging.logError(Logging.LEVEL_DEBUG, this, e);
            }
        }

        // if the primary didn't respond we have to wait until the lease timed out
        // if every replica replied and none has been primary we don't have to wait
        if (!primaryResponded && !(responseCount == xLocSet.getReplicasCount())) {
            // FIXME (jdillmann): Howto get the lease timeout value from the OSD config?
            long leaseToMS = 15 * 1000;

            // TODO(jdillmann): Care about the InterruptedException to ensure we will sleep for the required
            // time
            sleep(leaseToMS);
        }

        return states;
    }

    private int calculateMinimalMajority(ReplicaStatus[] states, AuthoritativeReplicaState authState) {

        // save object version mappings in a hashmap to speedup lookups
        HashMap<Long, ObjectVersionMapping> objectVersionMappings = new HashMap<Long, ObjectVersionMapping>(
                authState.getObjectVersionsCount());
        HashMap<Long, Integer> objectCount = new HashMap<Long, Integer>(authState.getObjectVersionsCount());

        for (ObjectVersionMapping ovm : authState.getObjectVersionsList()) {
            objectVersionMappings.put(ovm.getObjectNumber(), ovm);
        }

        for (ReplicaStatus state : states) {
            for (ObjectVersion objectVersion : state.getObjectVersionsList()) {
                Long objectNumber = objectVersion.getObjectNumber();
                ObjectVersionMapping maxObjectVersion = objectVersionMappings.get(objectNumber);

                if (maxObjectVersion != null && maxObjectVersion.getObjectVersion() == objectVersion.getObjectVersion()) {
                    Integer count = objectCount.get(objectNumber);
                    objectCount.put(objectNumber, (count == null ? 1 : count + 1));
                }
            }
        }

        // TODO(jdillmann): What should i do if the file is a sparse file and no objects exists?
        Integer minimalMajority = states.length;
        if (objectCount.size() > 0) {
            minimalMajority = Collections.min(objectCount.values());
        }

        return minimalMajority;
    }

    private CoordinatedReplicaUpdatePolicy getMockupPolicy(String replicaUpdatePolicy, String fileId,
            List<ServiceUUID> OSDUUIDs) {

        // CoordinatedReplicaUpdatePolicy assumes an separation between the remote OSDs and the local one.
        // This will take the last OSD out of the list and use it as the local.
        OSDUUIDs = new ArrayList<ServiceUUID>(OSDUUIDs);
        ServiceUUID mockupLocal = OSDUUIDs.remove(OSDUUIDs.size() - 1);

        CoordinatedReplicaUpdatePolicy policy = null;
        if (replicaUpdatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE)) {
            policy = new WaR1UpdatePolicy(OSDUUIDs, mockupLocal.toString(), fileId, null);
        } else if (replicaUpdatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA)) {
            policy = new WaRaUpdatePolicy(OSDUUIDs, mockupLocal.toString(), fileId, null);
        } else if (replicaUpdatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ)) {
            policy = new WqRqUpdatePolicy(OSDUUIDs, mockupLocal.toString(), fileId, null);
        } else {
            throw new IllegalArgumentException("unsupported replica update mode: " + replicaUpdatePolicy);
        }

        return policy;
    }

    /**
     * Add the RequestMethod saved as context in the local queue when the update operation completes
     */
    @Override
    public void finished(Object result, Object context) {
        // TODO(jdillmann): Check if the context is really a RequestMethod
        RequestMethod m = (RequestMethod) context;
        q.add(m);
    }

    @Override
    public void failed(Throwable error, Object context) {
        // TODO(jdillmann): Check if the context is really a RequestMethod
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
