/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.FleaseMessageSenderInterface;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.FleaseStatusListener;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.operations.EventRWRStatus;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.rwre.ReplicatedFileState.ReplicaState;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StorageStage.DeleteObjectsCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetMaxObjectNoCallback;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.AuthoritativeReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersion;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 */
public class RWReplicationStage extends Stage implements FleaseMessageSenderInterface {

    public static final int STAGEOP_REPLICATED_WRITE = 1;
    public static final int STAGEOP_CLOSE = 2;
    public static final int STAGEOP_PROCESS_FLEASE_MSG = 3;
    public static final int STAGEOP_PREPAREOP = 5;
    public static final int STAGEOP_TRUNCATE = 6;
    public static final int STAGEOP_GETSTATUS = 7;

    public static final int STAGEOP_INTERNAL_AUTHSTATE = 10;
    public static final int STAGEOP_INTERNAL_OBJFETCHED = 11;

    public static final int STAGEOP_LEASE_STATE_CHANGED = 13;
    public static final int STAGEOP_INTERNAL_STATEAVAIL = 14;
    public static final int STAGEOP_INTERNAL_DELETE_COMPLETE = 15;
    public static final int STAGEOP_FORCE_RESET = 16;
    public static final int STAGEOP_INTERNAL_MAXOBJ_AVAIL = 17;

    public  static enum Operation {
        READ,
        WRITE,
        TRUNCATE,
        INTERNAL_UPDATE,
        INTERNAL_TRUNCATE
    };


    private final RPCNIOSocketClient client;

    private final OSDServiceClient   osdClient;

    private final Map<String,ReplicatedFileState> files;

    private final Map<ASCIIString,String> cellToFileId;

    private final OSDRequestDispatcher master;

    private final FleaseStage          fstage;

    private final RPCNIOSocketClient   fleaseClient;

    private final OSDServiceClient     fleaseOsdClient;


    private final ASCIIString          localID;

    private int                        numObjsInFlight;

    private static final int           MAX_OBJS_IN_FLIGHT = 10;

    private static final int MAX_PENDING_PER_FILE = 10;

    private static final int MAX_EXTERNAL_REQUESTS_IN_Q = 250;

    private final Queue<ReplicatedFileState> filesInReset;

    private final FleaseMasterEpochThread masterEpochThread;

    private final AtomicInteger externalRequestsInQueue;



    public RWReplicationStage(OSDRequestDispatcher master, SSLOptions sslOpts) throws IOException {
        super("RWReplSt");
        this.master = master;
        client = new RPCNIOSocketClient(sslOpts, 15000, 60000*5);
        fleaseClient = new RPCNIOSocketClient(sslOpts, 15000, 60000*5);
        osdClient = new OSDServiceClient(client,null);
        fleaseOsdClient = new OSDServiceClient(fleaseClient,null);
        files = new HashMap<String, ReplicatedFileState>();
        cellToFileId = new HashMap<ASCIIString,String>();
        numObjsInFlight = 0;
        filesInReset = new LinkedList();
        externalRequestsInQueue = new AtomicInteger(0);

        localID = new ASCIIString(master.getConfig().getUUID().toString());

        masterEpochThread = new FleaseMasterEpochThread(master.getStorageStage().getStorageLayout());

        FleaseConfig fcfg = new FleaseConfig(master.getConfig().getFleaseLeaseToMS(),
                master.getConfig().getFleaseDmaxMS(), master.getConfig().getFleaseDmaxMS(),
                null, localID.toString(), master.getConfig().getFleaseRetries());

        fstage = new FleaseStage(fcfg, master.getConfig().getObjDir()+"/",
                this, false, new FleaseViewChangeListenerInterface() {

            @Override
            public void viewIdChangeEvent(ASCIIString cellId, int viewId) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, new FleaseStatusListener() {

            @Override
            public void statusChanged(ASCIIString cellId, Flease lease) {
                //FIXME: change state
                eventLeaseStateChanged(cellId, lease, null);
            }

            @Override
            public void leaseFailed(ASCIIString cellID, FleaseException error) {
                //change state
                //flush pending requests
                eventLeaseStateChanged(cellID, null, error);
            }
        }, masterEpochThread);
    }

    @Override
    public void start() {
        masterEpochThread.start();
        client.start();
        fleaseClient.start();
        fstage.start();
        super.start();
    }

    @Override
    public void shutdown() {
        client.shutdown();
        fleaseClient.shutdown();
        fstage.shutdown();
        masterEpochThread.shutdown();
        super.shutdown();
    }

    @Override
    public void waitForStartup() throws Exception {
        masterEpochThread.waitForStartup();
        client.waitForStartup();
        fleaseClient.waitForStartup();
        fstage.waitForStartup();
        super.waitForStartup();
    }

    public void waitForShutdown() throws Exception {
        client.waitForShutdown();
        fleaseClient.waitForShutdown();
        fstage.waitForShutdown();
        masterEpochThread.waitForShutdown();
        super.waitForShutdown();
    }

    public void eventReplicaStateAvailable(String fileId, ReplicaStatus localState, ErrorResponse error) {
         this.enqueueOperation(STAGEOP_INTERNAL_STATEAVAIL, new Object[]{fileId,localState,error}, null, null);
    }

    public void eventForceReset(FileCredentials credentials, XLocations xloc) {
         this.enqueueOperation(STAGEOP_FORCE_RESET, new Object[]{credentials, xloc}, null, null);
    }

    public void eventDeleteObjectsComplete(String fileId, ErrorResponse error) {
         this.enqueueOperation(STAGEOP_INTERNAL_DELETE_COMPLETE, new Object[]{fileId,error}, null, null);
    }

    void eventObjectFetched(String fileId, ObjectVersionMapping object, InternalObjectData data, ErrorResponse error) {
         this.enqueueOperation(STAGEOP_INTERNAL_OBJFETCHED, new Object[]{fileId,object,data,error}, null, null);
    }


    void eventSetAuthState(String fileId, AuthoritativeReplicaState authState, ReplicaStatus localState, ErrorResponse error) {
        this.enqueueOperation(STAGEOP_INTERNAL_AUTHSTATE, new Object[]{fileId,authState, localState, error}, null, null);
    }

    void eventLeaseStateChanged(ASCIIString cellId, Flease lease, FleaseException error) {
        this.enqueueOperation(STAGEOP_LEASE_STATE_CHANGED, new Object[]{cellId,lease,error}, null, null);
    }

    void eventMaxObjAvail(String fileId, long maxObjVer, long fileSize, long truncateEpoch, ErrorResponse error) {
        this.enqueueOperation(STAGEOP_INTERNAL_MAXOBJ_AVAIL, new Object[]{fileId,maxObjVer,error}, null, null);
    }

    private void processLeaseStateChanged(StageRequest method) {
        try {
            final ASCIIString cellId = (ASCIIString) method.getArgs()[0];
            final Flease lease = (Flease) method.getArgs()[1];
            final FleaseException error = (FleaseException) method.getArgs()[2];

            if (error == null) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) lease change event: %s, %s",localID, cellId, lease);
                }
            } else {
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,"(R:%s) lease error in cell %s: %s",localID, cellId, error);
            }

            final String fileId = cellToFileId.get(cellId);
            if (fileId != null) {
                ReplicatedFileState state = files.get(fileId);
                assert(state != null);

                boolean leaseOk = false;
                if (error == null) {
                    boolean localIsPrimary = (lease.getLeaseHolder() != null) && (lease.getLeaseHolder().equals(localID));
                    ReplicaState oldState = state.getState();
                    state.setLocalIsPrimary(localIsPrimary);
                    state.setLease(lease);

                    // Error handling for timeouts on the primary.
                    if (oldState == ReplicaState.PRIMARY
                        &&lease.getLeaseHolder() == null
                        && lease.getLeaseTimeout_ms() == 0) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,"(R:%s) was primary, lease error in cell %s, restarting replication: %s",localID, cellId,lease,error);
                        failed(state, ErrorUtils.getInternalServerError(new IOException(fileId +": lease timed out, renew failed")));
                    } else {
                        if ( (state.getState() == ReplicaState.BACKUP)
                            || (state.getState() == ReplicaState.PRIMARY)
                            || (state.getState() == ReplicaState.WAITING_FOR_LEASE) ) {
                                if (localIsPrimary) {
                                    //notify onPrimary
                                    if (oldState != ReplicaState.PRIMARY) {
                                        state.setMasterEpoch(lease.getMasterEpochNumber());
                                        doPrimary(state);
                                    }
                                } else {
                                    if (oldState != ReplicaState.BACKUP) {
                                        state.setMasterEpoch(FleaseMessage.IGNORE_MASTER_EPOCH);
                                        doBackup(state);
                                    }
                                }
                        }
                    }
                } else {
                    failed(state, ErrorUtils.getInternalServerError(error));
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void processSetAuthoritativeState(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final AuthoritativeReplicaState authState = (AuthoritativeReplicaState) method.getArgs()[1];
            final ReplicaStatus localState = (ReplicaStatus) method.getArgs()[2];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[3];

            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,"(R:%s) set AUTH for unknown file: %s",localID, fileId);
                return;
            }

            if (error != null) {
                failed(state, error);
            } else {
                // Calculate what we need to do locally based on the local state.
                boolean resetRequired = localState.getTruncateEpoch() < authState.getTruncateEpoch();

                // Create a list of missing objects.
                Map<Long,Long> objectsToBeDeleted = new HashMap();

                for (ObjectVersion localObject : localState.getObjectVersionsList()) {
                    // Never delete any object which is newer than auth state!
                    if (localObject.getObjectVersion() <= authState.getMaxObjVersion()) {
                        objectsToBeDeleted.put(localObject.getObjectNumber(), localObject.getObjectVersion());
                    }
                }
                // Delete everything that is older or not part of the authoritative state.
                for (ObjectVersionMapping authObject : authState.getObjectVersionsList()) {
                    Long version = objectsToBeDeleted.get(authObject.getObjectNumber());
                    if ((version != null) && (version == authObject.getObjectVersion())) {
                        objectsToBeDeleted.remove(authObject.getObjectNumber());
                    }
                }

                Map<Long,ObjectVersionMapping> missingObjects = new HashMap();
                for (ObjectVersionMapping authObject : authState.getObjectVersionsList()) {
                    missingObjects.put(authObject.getObjectNumber(), authObject);
                }
                for (ObjectVersion localObject : localState.getObjectVersionsList()) {
                    ObjectVersionMapping object = missingObjects.get(localObject.getObjectNumber());
                    if ((object != null) && (localObject.getObjectVersion() >= object.getObjectVersion())) {
                        missingObjects.remove(localObject.getObjectNumber());
                    }
                }

                if (!missingObjects.isEmpty()
                    || !objectsToBeDeleted.isEmpty()
                    || (localState.getTruncateEpoch() < authState.getTruncateEpoch())) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) replica RESET required updates for: %s", localID, state.getFileId());
                    }

                    state.setObjectsToFetch(new LinkedList(missingObjects.values()));
                    filesInReset.add(state);

                    // Start by deleting the old objects.
                    master.getStorageStage().deleteObjects(fileId, state.getsPolicy(), authState.getTruncateEpoch(), objectsToBeDeleted, new DeleteObjectsCallback() {

                        @Override
                        public void deleteObjectsComplete(ErrorResponse error) {
                            eventDeleteObjectsComplete(fileId, error);
                        }
                    });

                } else {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) replica RESET finished (replica is up-to-date): %s",localID, state.getFileId());
                    }
                    doOpen(state);
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void processDeleteObjectsComplete(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[1];

            ReplicatedFileState state = files.get(fileId);
            if (state != null) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) deleted all objects requested by RESET for %s with %s",localID,state.getFileId(),
                            ErrorUtils.formatError(error));
                }
                if (error != null) {
                    failed(state, error);
                } else {
                   fetchObjects();
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void fetchObjects() {

        while (numObjsInFlight < MAX_OBJS_IN_FLIGHT) {

            ReplicatedFileState file = filesInReset.poll();
            if (file == null)
                break;

            
            if (!file.getObjectsToFetch().isEmpty()) {
                ObjectVersionMapping o = file.getObjectsToFetch().remove(0);
                file.setNumObjectsPending(file.getNumObjectsPending()+1);
                numObjsInFlight++;
                fetchObject(file.getFileId(), o);
            }

            if (!file.getObjectsToFetch().isEmpty()) {
                filesInReset.add(file);
            }
        }
    }

    private void fetchObject(final String fileId, final ObjectVersionMapping record) {
        ReplicatedFileState state = files.get(fileId);
        if (state == null) {
            return;
        }

        try {
            final ServiceUUID osd = new ServiceUUID(record.getOsdUuidsList().get(0));
            //fetch that object
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) file %s, fetch object %d (version %d) from %s",
                        localID, fileId,record.getObjectNumber(),record.getObjectVersion(),osd);

            RPCResponse r = osdClient.xtreemfs_rwr_fetch(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                    state.getCredentials(), fileId, record.getObjectNumber(), record.getObjectVersion());
            r.registerListener(new RPCResponseAvailableListener() {

                @Override
                public void responseAvailable(RPCResponse r) {
                    try {
                        ObjectData metadata = (ObjectData) r.get();
                        InternalObjectData data = new InternalObjectData(metadata, r.getData());
                        eventObjectFetched(fileId, record, data, null);
                    } catch (Exception ex) {
                        eventObjectFetched(fileId, record, null, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
                    } finally {
                        r.freeBuffers();
                    }
                }
            });
        } catch (IOException ex) {
            eventObjectFetched(fileId, record, null, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
        }
        
    }
    
    private void processObjectFetched(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final ObjectVersionMapping record = (ObjectVersionMapping) method.getArgs()[1];
            final InternalObjectData data = (InternalObjectData) method.getArgs()[2];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[3];

            ReplicatedFileState state = files.get(fileId);
            if (state != null) {

                if (error != null) {
                    numObjsInFlight--;
                    fetchObjects();
                    failed(state, error);
                } else {
                    master.getStorageStage().writeObjectWithoutGMax(fileId, record.getObjectNumber(),
                            state.getsPolicy(), 0, data.getData(), CowPolicy.PolicyNoCow, null, false,
                            record.getObjectVersion(), null, new WriteObjectCallback() {

                        @Override
                        public void writeComplete(OSDWriteResponse result, ErrorResponse error) {
                            if (error != null) {
                                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,"cannot write object locally: %s",ErrorUtils.formatError(error));
                            }
                        }
                    });
                    master.getPreprocStage().pingFile(fileId);

                    numObjsInFlight--;
                    final int numPendingFile = state.getNumObjectsPending()-1;
                    state.setNumObjectsPending(numPendingFile);
                    state.getPolicy().objectFetched(record.getObjectVersion());
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) fetched object for replica, file %s, remaining %d",localID, fileId,numPendingFile);
                    fetchObjects();
                    if (numPendingFile == 0) {
                        //reset complete!
                        Logging.logMessage(Logging.LEVEL_INFO, Category.replication, this,"(R:%s) RESET complete for file %s",localID, fileId);
                        doOpen(state);
                    }
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void doReset(final ReplicatedFileState file, long updateObjVer) {

        if (file.getState() == ReplicaState.RESET) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"file %s is already in RESET",file.getFileId());
            return;
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) replica state changed for %s from %s to %s",localID, file.getFileId(),file.getState(),ReplicaState.RESET);
        }
        file.setState(ReplicaState.RESET);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) replica RESET started: %s (update objVer=%d)",localID, file.getFileId(),updateObjVer);
        }

        OSDOperation op = master.getInternalEvent(EventRWRStatus.class);
        op.startInternalEvent(new Object[]{file.getFileId(),file.getsPolicy()});
        
    }

    private void processReplicaStateAvailExecReset(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final ReplicaStatus localReplicaState = (ReplicaStatus) method.getArgs()[1];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[2];

            final ReplicatedFileState state = files.get(fileId);
            if (state != null) {
                if (error != null) {
                    Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,"local state for %s failed: %s",
                                state.getFileId(), error);
                    failed(state, error);
                } else {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) local state for %s available.",
                                localID, state.getFileId());
                    }
                    state.getPolicy().executeReset(state.getCredentials(), localReplicaState, new ReplicaUpdatePolicy.ExecuteResetCallback() {

                        @Override
                        public void finished(AuthoritativeReplicaState authState) {
                            eventSetAuthState(state.getFileId(), authState, localReplicaState, null);
                        }

                        @Override
                        public void failed(ErrorResponse error) {
                            eventSetAuthState(state.getFileId(), null, null, error);
                        }
                    });
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void processForceReset(StageRequest method) {
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];

            ReplicatedFileState state = getState(credentials, loc, true);
            if (!state.isForceReset()) {
                state.setForceReset(true);
            }
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void doWaitingForLease(final ReplicatedFileState file) {
        if (file.getPolicy().requiresLease()) {
            if (file.isCellOpen()) {
                if (file.isLocalIsPrimary()) {
                    doPrimary(file);
                } else {
                    doBackup(file);
                }
            } else {
                file.setCellOpen(true);
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) replica state changed for %s from %s to %s",
                            localID,file.getFileId(),file.getState(),ReplicaState.WAITING_FOR_LEASE);
                }
                try {
                    file.setState(ReplicaState.WAITING_FOR_LEASE);
                    List<InetSocketAddress> osdAddresses = new ArrayList();
                    for (ServiceUUID osd : file.getPolicy().getRemoteOSDUUIDs()) {
                        osdAddresses.add(osd.getAddress());
                    }
                    fstage.openCell(file.getPolicy().getCellId(), osdAddresses, true);
                    //wait for lease...
                } catch (UnknownUUIDException ex) {
                    failed(file, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
                }
            }

        } else {
            //become primary immediately
            doPrimary(file);
        }
    }

    private void doOpen(final ReplicatedFileState file) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"(R:%s) replica state changed for %s from %s to %s",localID, file.getFileId(),file.getState(),ReplicaState.OPEN);
        }
        file.setState(ReplicaState.OPEN);
        if (file.getPendingRequests().size() > 0) {
            doWaitingForLease(file);
        }
    }

    private void doPrimary(final ReplicatedFileState file) {
        assert(file.isLocalIsPrimary());
        try {
            if (file.getPolicy().onPrimary((int)file.getMasterEpoch()) && !file.isPrimaryReset()) {
                file.setPrimaryReset(true);
                doReset(file,ReplicaUpdatePolicy.UNLIMITED_RESET);
            } else {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) replica state changed for %s from %s to %s",
                            localID, file.getFileId(),file.getState(),ReplicaState.PRIMARY);
                }
                file.setPrimaryReset(false);
                file.setState(ReplicaState.PRIMARY);
                while (!file.getPendingRequests().isEmpty()) {
                    StageRequest m = file.getPendingRequests().remove(0);
                    enqueuePrioritized(m);
                }
            }
        } catch (IOException ex) {
            failed(file, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
        }
    }

    private void doBackup(final ReplicatedFileState file) {
        assert(!file.isLocalIsPrimary());
        //try {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) replica state changed for %s from %s to %s",
                    localID, file.getFileId(),file.getState(),ReplicaState.BACKUP);
        }
        file.setPrimaryReset(false);
        file.setState(ReplicaState.BACKUP);
        while (!file.getPendingRequests().isEmpty()) {
            StageRequest m = file.getPendingRequests().remove(0);
            enqueuePrioritized(m);
        }
        /*} catch (IOException ex) {
            failed(file, ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(), ex));
        }*/
    }

    private void failed(ReplicatedFileState file, ErrorResponse ex) {
        Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,"(R:%s) replica for file %s failed: %s",localID, file.getFileId(),ErrorUtils.formatError(ex));
        file.setPrimaryReset(false);
        file.setState(ReplicaState.OPEN);
        file.setCellOpen(false);
        fstage.closeCell(file.getPolicy().getCellId(), false);
        for (StageRequest rq : file.getPendingRequests()) {
            RWReplicationCallback callback = (RWReplicationCallback) rq.getCallback();
            callback.failed(ex);
        }
        file.getPendingRequests().clear();
    }

    private void enqueuePrioritized(StageRequest rq) {
        while (!q.offer(rq)) {
            StageRequest otherRq = q.poll();
            otherRq.sendInternalServerError(new IllegalStateException("internal queue overflow, cannot enqueue operation for processing."));
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Dropping request from rwre queue due to overload");
        }
    }


    public static interface RWReplicationCallback {
        public void success(long newObjectVersion);
        public void redirect(String redirectTo);
        public void failed(ErrorResponse ex);
    }

    /*public void openFile(FileCredentials credentials, XLocations locations, boolean forceReset,
            RWReplicationCallback callback, OSDRequest request) {
        this.enqueueOperation(STAGEOP_OPEN, new Object[]{credentials,locations,forceReset}, request, callback);
    }*/

    protected void enqueueExternalOperation(int stageOp, Object[] arguments, OSDRequest request, Object callback) {
        if (externalRequestsInQueue.get() >= MAX_EXTERNAL_REQUESTS_IN_Q) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "RW replication stage is overloaded, request %d for %s dropped", request.getRequestId(), request.getFileId());
            request.sendInternalServerError(new IllegalStateException("RW replication stage is overloaded, request dropped"));
        } else {
            externalRequestsInQueue.incrementAndGet();
            this.enqueueOperation(stageOp, arguments, request, callback);
        }
    }

    public void prepareOperation(FileCredentials credentials, XLocations xloc, long objNo, long objVersion, Operation op, RWReplicationCallback callback,
            OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_PREPAREOP, new Object[]{credentials,xloc,objNo,objVersion,op}, request, callback);
    }
    
    public void replicatedWrite(FileCredentials credentials, XLocations xloc, long objNo, long objVersion, InternalObjectData data,
            RWReplicationCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_REPLICATED_WRITE, new Object[]{credentials,xloc,objNo,objVersion,data}, request, callback);
    }

    public void replicateTruncate(FileCredentials credentials, XLocations xloc, long newFileSize, long newObjectVersion,
            RWReplicationCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_TRUNCATE, new Object[]{credentials,xloc,newFileSize,newObjectVersion}, request, callback);
    }

    public void fileClosed(String fileId) {
        this.enqueueOperation(STAGEOP_CLOSE, new Object[]{fileId}, null, null);
    }

    public void receiveFleaseMessage(ReusableBuffer message, InetSocketAddress sender) {
        //this.enqueueOperation(STAGEOP_PROCESS_FLEASE_MSG, new Object[]{message,sender}, null, null);
        try {
            FleaseMessage msg = new FleaseMessage(message);
            BufferPool.free(message);
            msg.setSender(sender);
            fstage.receiveMessage(msg);
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    public void getStatus(StatusCallback callback) {
        this.enqueueOperation(STAGEOP_GETSTATUS, new Object[]{}, null, callback);
    }

    public static interface StatusCallback {
        public void statusComplete(Map<String,Map<String,String>> status);
    }

    @Override
    public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
        ReusableBuffer data = BufferPool.allocate(message.getSize());
        message.serialize(data);
        data.flip();
        try {
            RPCResponse r = fleaseOsdClient.xtreemfs_rwr_flease_msg(recipient, RPCAuthentication.authNone, RPCAuthentication.userService, master.getHostName(),master.getConfig().getPort(),data);
            r.registerListener(new RPCResponseAvailableListener() {

                @Override
                public void responseAvailable(RPCResponse r) {
                    r.freeBuffers();
                }
            });
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }


    @Override
    protected void processMethod(StageRequest method) {
        switch (method.getStageMethod()) {
            case STAGEOP_REPLICATED_WRITE : {
                externalRequestsInQueue.decrementAndGet();
                processReplicatedWrite(method);
                break;
            }
            case STAGEOP_TRUNCATE : {
                externalRequestsInQueue.decrementAndGet();
                processReplicatedTruncate(method);
                break;
            }
            case STAGEOP_CLOSE : processFileClosed(method); break;
            case STAGEOP_PROCESS_FLEASE_MSG : processFleaseMessage(method); break;
            case STAGEOP_PREPAREOP : {
                externalRequestsInQueue.decrementAndGet();
                processPrepareOp(method);
                break;
            }
            case STAGEOP_INTERNAL_AUTHSTATE : processSetAuthoritativeState(method); break;
            case STAGEOP_LEASE_STATE_CHANGED : processLeaseStateChanged(method); break;
            case STAGEOP_INTERNAL_OBJFETCHED : processObjectFetched(method); break;
            case STAGEOP_INTERNAL_STATEAVAIL : processReplicaStateAvailExecReset(method); break;
            case STAGEOP_INTERNAL_DELETE_COMPLETE : processDeleteObjectsComplete(method); break;
            case STAGEOP_INTERNAL_MAXOBJ_AVAIL : processMaxObjAvail(method); break;
            case STAGEOP_FORCE_RESET : processForceReset(method); break;
            case STAGEOP_GETSTATUS : processGetStatus(method); break;
            default : throw new IllegalArgumentException("no such stageop");
        }
    }


    private void processFleaseMessage(StageRequest method) {
        try {
            final ReusableBuffer data = (ReusableBuffer) method.getArgs()[0];
            final InetSocketAddress sender = (InetSocketAddress) method.getArgs()[1];

            FleaseMessage msg = new FleaseMessage(data);
            BufferPool.free(data);
            msg.setSender(sender);
            fstage.receiveMessage(msg);

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void processFileClosed(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            ReplicatedFileState state = files.remove(fileId);
            if (state != null) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"closing file %s",fileId);
                }
                state.getPolicy().closeFile();
                if (state.getPolicy().requiresLease())
                    fstage.closeCell(state.getPolicy().getCellId(), false);
                cellToFileId.remove(state.getPolicy().getCellId());
            }
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private ReplicatedFileState getState(FileCredentials credentials, XLocations loc, boolean forceReset) throws IOException {

        final String fileId = credentials.getXcap().getFileId();

        ReplicatedFileState state = files.get(fileId);
        if (state == null) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"open file: "+fileId);
            //"open" file
            state = new ReplicatedFileState(fileId,loc, master.getConfig().getUUID(), fstage, osdClient);
            files.put(fileId,state);
            state.setCredentials(credentials);
            state.setForceReset(forceReset);
            cellToFileId.put(state.getPolicy().getCellId(),fileId);
            assert(state.getState() == ReplicaState.INITIALIZING);

            master.getStorageStage().internalGetMaxObjectNo(fileId, loc.getLocalReplica().getStripingPolicy(), new InternalGetMaxObjectNoCallback() {

                @Override
                public void maxObjectNoCompleted(long maxObjNo, long fileSize, long truncateEpoch, ErrorResponse error) {
                    eventMaxObjAvail(fileId, maxObjNo, fileSize, truncateEpoch, error);
                }
            });
        }
        return state;
    }

    private void processMaxObjAvail(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final Long maxObjVersion = (Long) method.getArgs()[1];
            final ErrorResponse error = (ErrorResponse) method.getArgs()[2];

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) max obj avail for file: "+fileId+" max="+maxObjVersion, localID);


            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,"received maxObjAvail event for unknow file: %s",fileId);
                return;
            }

            assert(state.getState() == ReplicaState.INITIALIZING);
            state.getPolicy().setLocalObjectVersion(maxObjVersion);
            doOpen(state);
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private void processReplicatedWrite(StageRequest method) {
        final RWReplicationCallback callback = (RWReplicationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];
            final Long objNo = (Long) method.getArgs()[2];
            final Long objVersion = (Long) method.getArgs()[3];
            final InternalObjectData objData  = (InternalObjectData) method.getArgs()[4];
            

            final String fileId = credentials.getXcap().getFileId();

            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                BufferPool.free(objData.getData());
                callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, "file is not open!"));
            }
            state.setCredentials(credentials);

            state.getPolicy().executeWrite(credentials, objNo, objVersion, objData, new ReplicaUpdatePolicy.ClientOperationCallback() {

                @Override
                public void finsihed() {
                    callback.success(objVersion);
                }

                @Override
                public void failed(ErrorResponse error) {
                    callback.failed(error);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    private void processReplicatedTruncate(StageRequest method) {
        final RWReplicationCallback callback = (RWReplicationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];
            final Long newFileSize = (Long) method.getArgs()[2];
            final Long newObjVersion = (Long) method.getArgs()[3];
            
            final String fileId = credentials.getXcap().getFileId();

            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, "file is not open!"));
            }
            state.setCredentials(credentials);

            state.getPolicy().executeTruncate(credentials, newFileSize, newObjVersion, new ReplicaUpdatePolicy.ClientOperationCallback() {

                @Override
                public void finsihed() {
                    callback.success(newObjVersion);
                }

                @Override
                public void failed(ErrorResponse error) {
                    callback.failed(error);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    private void processPrepareOp(StageRequest method) {
        final RWReplicationCallback callback = (RWReplicationCallback)method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];
            final Long objVersion = (Long) method.getArgs()[3];
            final Operation op = (Operation) method.getArgs()[4];

            final String fileId = credentials.getXcap().getFileId();

            ReplicatedFileState state = getState(credentials, loc, false);

            if ((op == Operation.INTERNAL_UPDATE) || (op == Operation.INTERNAL_TRUNCATE)) {
                switch (state.getState()) {
                    case WAITING_FOR_LEASE:
                    case INITIALIZING:
                    case RESET:
                    case OPEN: {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"enqeue update for %s (state is %s)",fileId,state.getState());
                        }
                        if (state.getPendingRequests().size() > MAX_PENDING_PER_FILE) {
                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                        "rejecting request: too many requests (is: %d, max %d) in queue for file %s",
                                        state.getPendingRequests().size(), MAX_PENDING_PER_FILE, fileId);
                            }
                            callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "too many requests in queue for file"));
                        } else {
                            state.getPendingRequests().add(method);
                        }
                        if (state.getState() == ReplicaState.OPEN) {
                            //immediately change to backup mode...no need to check the lease
                            doWaitingForLease(state);
                        }
                        return;
                    }
                }
                boolean needsReset = state.getPolicy().onRemoteUpdate(objVersion, state.getState());
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"%s needs reset: %s",fileId,needsReset);
                }
                if (needsReset) {
                    state.getPendingRequests().add(method);
                    doReset(state,objVersion);
                } else {
                    callback.success(0);
                }
            } else {
                state.setCredentials(credentials);
                
                switch (state.getState()) {
                    case WAITING_FOR_LEASE:
                    case INITIALIZING:
                    case RESET : {
                        if (state.getPendingRequests().size() > MAX_PENDING_PER_FILE) {
                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                        "rejecting request: too many requests (is: %d, max %d) in queue for file %s",
                                        state.getPendingRequests().size(), MAX_PENDING_PER_FILE, fileId);
                            }
                            callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "too many requests in queue for file"));
                        } else {
                            state.getPendingRequests().add(method);
                        }
                        return;
                    }
                    case OPEN : {
                        if (state.getPendingRequests().size() > MAX_PENDING_PER_FILE) {
                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                        "rejecting request: too many requests (is: %d, max %d) in queue for file %s",
                                        state.getPendingRequests().size(), MAX_PENDING_PER_FILE, fileId);
                            }
                            callback.failed(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "too many requests in queue for file"));
                        } else {
                            state.getPendingRequests().add(method);
                        }
                        doWaitingForLease(state);
                        return;
                    }
                }

                try {
                    long newVersion = state.getPolicy().onClientOperation(op,objVersion,state.getState(),state.getLease());
                    callback.success(newVersion);
                } catch (RedirectToMasterException ex) {
                    callback.redirect(ex.getMasterUUID());
                } catch (RetryException ex) {
                    final ErrorResponse err = ErrorUtils.getInternalServerError(ex);
                    failed(state, err);
                    if (state.getState() == ReplicaState.BACKUP
                        || state.getState() == ReplicaState.PRIMARY) {
                        // Request is not in queue, we must notify
                        // callback.
                        callback.failed(err);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    private void processGetStatus(StageRequest method) {
        final StatusCallback callback = (StatusCallback)method.getCallback();
        try {
            Map<String,Map<String,String>> status = new HashMap();

            Map<ASCIIString,FleaseMessage> fleaseState = fstage.getLocalState();

            for (String fileId : this.files.keySet()) {
                Map<String,String> fStatus = new HashMap();
                final ReplicatedFileState fState = files.get(fileId);
                final ASCIIString cellId = fState.getPolicy().getCellId();
                fStatus.put("policy",fState.getPolicy().getClass().getSimpleName());
                fStatus.put("peers (OSDs)",fState.getPolicy().getRemoteOSDUUIDs().toString());
                fStatus.put("pending requests", fState.getPendingRequests() == null ? "0" : ""+fState.getPendingRequests().size());
                fStatus.put("cellId", cellId.toString());
                String primary = "unknown";
                if ((fState.getLease() != null) && (!fState.getLease().isEmptyLease())) {
                    if (fState.getLease().isValid()) {
                        if (fState.isLocalIsPrimary()) {
                            primary = "primary";
                        } else {
                            primary = "backup ( primary is "+fState.getLease().getLeaseHolder()+")";
                        }
                    } else {
                        primary = "outdated lease: "+fState.getLease().getLeaseHolder();
                    }
                }
                fStatus.put("role", primary);
                status.put(fileId,fStatus);
            }
            callback.statusComplete(status);
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.statusComplete(null);
        }
    }

    

}
