/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
Grid Operating System, see <http://www.xtreemos.eu> for more details.
The XtreemOS project has been developed with the financial support of the
European Commission's IST program under contract #FP6-033576.

XtreemFS is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation, either version 2 of the License, or (at your option)
any later version.

XtreemFS is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.FleaseMessageSenderInterface;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.FleaseStatusListener;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.operations.EventGetFileSize;
import org.xtreemfs.osd.operations.EventTruncate;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.rwre.ReplicatedFileState.ReplicaState;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 *
 * @author bjko
 */
public class RWReplicationStage extends Stage implements FleaseMessageSenderInterface {

    public static final int STAGEOP_REPLICATED_WRITE = 1;
    public static final int STAGEOP_CLOSE = 2;
    public static final int STAGEOP_PROCESS_FLEASE_MSG = 3;
    public static final int STAGEOP_OPEN = 4;
    public static final int STAGEOP_PREPAREOP = 5;
    public static final int STAGEOP_TRUNCATE = 6;
    public static final int STAGEOP_GETSTATUS = 7;

    public static final int STAGEOP_INTERNAL_GETOBJECTS = 10;
    public static final int STAGEOP_INTERNAL_OBJFETCHED = 11;

    public static final int STAGEOP_LEASE_STATE_CHANGED = 13;
    public static final int STAGEOP_INTERNAL_FSAVAIL = 14;
    public static final int STAGEOP_INTERNAL_TRUNCED = 15;

    
    public  static enum Operation {
        READ,
        WRITE,
        TRUNCATE,
        INTERNAL_UPDATE,
        INTERNAL_TRUNCATE
    };


    private final RPCNIOSocketClient client;

    private final OSDClient          osdClient;

    private final Map<String,ReplicatedFileState> files;

    private final Map<ASCIIString,String> cellToFileId;

    private final OSDRequestDispatcher master;

    private final FleaseStage          fstage;

    private final ASCIIString          localID;

    private int                        numObjsInFlight;

    private static final int           MAX_OBJS_IN_FLIGHT = 10;

    private final Queue<ReplicatedFileState> filesInReset;



    public RWReplicationStage(OSDRequestDispatcher master, SSLOptions sslOpts) throws IOException {
        super("RWReplSt");
        this.master = master;
        client = new RPCNIOSocketClient(sslOpts, 15000, 60000*5);
        osdClient = new OSDClient(client);
        files = new HashMap<String, ReplicatedFileState>();
        cellToFileId = new HashMap<ASCIIString,String>();
        numObjsInFlight = 0;
        filesInReset = new LinkedList();

        localID = new ASCIIString(master.getConfig().getUUID().toString());

        FleaseConfig fcfg = new FleaseConfig(master.getConfig().getFleaseLeaseToMS(),
                master.getConfig().getFleaseDmaxMS(), master.getConfig().getFleaseDmaxMS(),
                null, localID.toString(), master.getConfig().getFleaseRetries());

        fstage = new FleaseStage(fcfg, master.getConfig().getObjDir()+"/.flease_"+master.getConfig().getUUID().toString()+".lock",
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
        });
    }

    @Override
    public void start() {
        client.start();
        fstage.start();
        super.start();
    }

    @Override
    public void shutdown() {
        client.shutdown();
        fstage.shutdown();
        super.shutdown();
    }

    @Override
    public void waitForStartup() throws Exception {
        client.waitForStartup();
        fstage.waitForStartup();
        super.waitForStartup();
    }

    public void waitForShutdown() throws Exception {
        client.waitForShutdown();
        fstage.waitForShutdown();
        super.waitForShutdown();
    }

    public void eventFileSizeAvailable(String fileId, long filesize, long truncateEpoch, long updateObjVer, Exception error) {
         this.enqueueOperation(STAGEOP_INTERNAL_FSAVAIL, new Object[]{fileId,filesize,truncateEpoch,updateObjVer,error}, null, null);
    }

    public void eventTruncateComplete(String fileId, Exception error) {
         this.enqueueOperation(STAGEOP_INTERNAL_TRUNCED, new Object[]{fileId,error}, null, null);
    }

    void eventObjectFetched(String fileId, ObjectFetchRecord object, ObjectData data,Exception error) {
         this.enqueueOperation(STAGEOP_INTERNAL_OBJFETCHED, new Object[]{fileId,object,data,error}, null, null);
    }

    void eventFetchObjects(String fileId, Queue<ObjectFetchRecord> objectsToFetch, Exception error) {
        this.enqueueOperation(STAGEOP_INTERNAL_GETOBJECTS, new Object[]{fileId,objectsToFetch,error}, null, null);
    }

    void eventLeaseStateChanged(ASCIIString cellId, Flease lease, Exception error) {
        this.enqueueOperation(STAGEOP_LEASE_STATE_CHANGED, new Object[]{cellId,lease,error}, null, null);
    }

    private void processLeaseStateChanged(StageRequest method) {
        try {
            final ASCIIString cellId = (ASCIIString) method.getArgs()[0];
            final Flease lease = (Flease) method.getArgs()[1];
            final FleaseException error = (FleaseException) method.getArgs()[2];

            Logging.logMessage(Logging.LEVEL_DEBUG, this,"lease change event: %s, %s, %s",cellId,lease.getLeaseHolder(),error);

            final String fileId = cellToFileId.get(cellId);
            if (fileId != null) {
                ReplicatedFileState state = files.get(fileId);
                assert(state != null);

                if (error == null) {
                    boolean localIsPrimary = (lease.getLeaseHolder() != null) && (lease.getLeaseHolder().equals(localID));
                    ReplicaState oldState = state.getState();
                    state.setLocalIsPrimary(localIsPrimary);
                    state.setLease(lease);
                    if ( (state.getState() == ReplicaState.BACKUP)
                        || (state.getState() == ReplicaState.PRIMARY)
                        || (state.getState() == ReplicaState.WAITING_FOR_LEASE) ) {
                            if (localIsPrimary) {
                                //notify onPrimary
                                if (oldState != ReplicaState.PRIMARY)
                                    doPrimary(state);
                            } else {
                                if (oldState != ReplicaState.BACKUP)
                                    doBackup(state);
                            }
                    }
                } else {
                    if (state.getState() == ReplicaState.WAITING_FOR_LEASE) {
                        failed(state, error);
                    }
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void processFetchObjects(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final Queue<ObjectFetchRecord> objects = (Queue<ObjectFetchRecord>) method.getArgs()[1];
            final Exception error = (Exception) method.getArgs()[2];


            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                Logging.logMessage(Logging.LEVEL_WARN, this,"fetch objects for unknown file: %s",fileId);
                return;
            }

            if (error != null) {
                failed(state, error);
            } else {
                if ((objects == null) || (objects.size() == 0)) {
                    Logging.logMessage(Logging.LEVEL_INFO, this,"replica RESET finished: %s",state.getFileId());
                    doOpen(state);
                } else {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"file %s, objects to fetch: %s",fileId,objects);
                    }

                    state.setObjectsToFetch(objects);
                    filesInReset.add(state);

                    ObjectFetchRecord r1 = objects.peek();
                    if (r1.isTruncate()) {
                        executeTruncate(state);
                    } else {
                        fetchObjects();
                    }
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void executeTruncate(ReplicatedFileState state) {
        ObjectFetchRecord r1 = state.getObjectsToFetch().poll();
        OSDOperation op = master.getInternalEvent(EventTruncate.class);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"truncate requested by RESET for %s to %d/%d",state.getFileId(),
                    r1.getNewFileSize(),r1.getNewTruncateEpoch());
        }
        op.startInternalEvent(new Object[]{state.getFileId(),r1.getNewFileSize(),r1.getNewTruncateEpoch(),state.getsPolicy(),state.getLocalReplica()});
    }

    private void processTruncateComplete(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final Exception error = (Exception) method.getArgs()[1];

            ReplicatedFileState state = files.get(fileId);
            if (state != null) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"truncate requested by RESET for %s completed with %s",state.getFileId(),
                            error);
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

            ObjectFetchRecord o = file.getObjectsToFetch().poll();
            if (o != null) {
                file.setNumObjectsPending(file.getNumObjectsPending()+1);
                fetchObject(file.getFileId(), o);
            }

            if (file.getObjectsToFetch().peek() != null) {
                filesInReset.add(file);
            }
        }
    }

    private void fetchObject(final String fileId, final ObjectFetchRecord record) {
        final InetSocketAddress osd = record.getNextOSD();
        //fetch that object
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"file %s, fetch object %d from %s",fileId,record.getObjNumber(),osd);
        ReplicatedFileState state = files.get(fileId);
        if (state == null) {
            return;
        }
        RPCResponse r = osdClient.rwr_fetch(osd, state.getCredentials(), fileId, record.getObjNumber(), record.getObjVersion());
        r.registerListener(new RPCResponseAvailableListener() {

            @Override
            public void responseAvailable(RPCResponse r) {
                try {
                    ObjectData data = (ObjectData) r.get();
                    eventObjectFetched(fileId, record, data, null);
                } catch (Exception ex) {
                    eventObjectFetched(fileId, record, null, ex);
                }
            }
        });
        
    }
    
    private void processObjectFetched(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final ObjectFetchRecord record = (ObjectFetchRecord) method.getArgs()[1];
            final ObjectData data = (ObjectData) method.getArgs()[2];
            final Exception error = (Exception) method.getArgs()[3];

            ReplicatedFileState state = files.get(fileId);
            if (state != null) {

                if (error != null) {
                    failed(state, error);
                } else {
                    master.getStorageStage().writeObjectWithoutGMax(fileId, record.getObjNumber(),
                            state.getsPolicy(), 0, data.getData(), CowPolicy.PolicyNoCow, null, false,
                            record.getObjVersion(), null, new WriteObjectCallback() {

                        @Override
                        public void writeComplete(OSDWriteResponse result, Exception error) {
                            if (error != null) {
                                Logging.logMessage(Logging.LEVEL_ERROR, this,"cannot write object locally: %s",error);
                            }
                        }
                    });

                    numObjsInFlight--;
                    final int numPendingFile = state.getNumObjectsPending()-1;
                    state.setNumObjectsPending(numPendingFile);
                    state.getPolicy().objectFetched(record.getObjVersion());
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"fetched object for replica, file %s, remaining %d",fileId,numPendingFile);
                    if (numPendingFile == 0) {
                        //reset complete!
                        doOpen(state);
                    }
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void doReset(final ReplicatedFileState file, long updateObjVer) {

        file.setState(ReplicaState.RESET);
        Logging.logMessage(Logging.LEVEL_INFO, this,"replica RESET started: %s (update objVer=%d)",file.getFileId(),updateObjVer);

        OSDOperation op = master.getInternalEvent(EventGetFileSize.class);
        op.startInternalEvent(new Object[]{file.getFileId(),updateObjVer,file.getsPolicy()});
        
    }

    private void processFSAvailExecReset(StageRequest method) {
        try {
            final String fileId = (String) method.getArgs()[0];
            final Long filesize = (Long) method.getArgs()[1];
            final Long truncateEpoch = (Long) method.getArgs()[2];
            final Long updateObjVer = (Long) method.getArgs()[3];
            final Exception error = (Exception) method.getArgs()[4];

            final ReplicatedFileState state = files.get(fileId);
            if (state != null) {

                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"local FS read for %s: %d/%s",state.getFileId(),
                            filesize,error);
                }

                if (error != null) {
                    failed(state, error);
                } else {
                    state.getPolicy().executeReset(state.getCredentials(), updateObjVer, filesize, truncateEpoch, new ReplicaUpdatePolicy.ExecuteResetCallback() {

                        @Override
                        public void finished(Queue<ObjectFetchRecord> objectsToFetch) {
                            eventFetchObjects(state.getFileId(), objectsToFetch, null);
                        }

                        @Override
                        public void failed(Exception error) {
                            eventFetchObjects(state.getFileId(), null, error);
                        }
                    });
                }
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
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"replica state changed for %s from %s to %s",
                            file.getFileId(),file.getState(),ReplicaState.WAITING_FOR_LEASE);
                }
                file.setState(ReplicaState.WAITING_FOR_LEASE);
                fstage.openCell(file.getPolicy().getCellId(), file.getPolicy().getRemoteOSDs());
                //wait for lease...
            }

        } else {
            //become primary immediately
            doPrimary(file);
        }
    }

    private void doOpen(final ReplicatedFileState file) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"replica state changed for %s from %s to %s",file.getFileId(),file.getState(),ReplicaState.OPEN);
        }
        file.setState(ReplicaState.OPEN);
        if (file.getPendingRequests().size() > 0) {
            doWaitingForLease(file);
        }
    }

    private void doPrimary(final ReplicatedFileState file) {
        assert(file.isLocalIsPrimary());
        try {
            if (file.getPolicy().onPrimary() && !file.isPrimaryReset()) {
                file.setPrimaryReset(true);
                doReset(file,ReplicaUpdatePolicy.UNBOUND_RESET);
            } else {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"replica state changed for %s from %s to %s",
                            file.getFileId(),file.getState(),ReplicaState.PRIMARY);
                }
                file.setPrimaryReset(false);
                file.setState(ReplicaState.PRIMARY);
                this.q.addAll(file.getPendingRequests());
                file.getPendingRequests().clear();
            }
        } catch (IOException ex) {
            failed(file, ex);
        }
    }

    private void doBackup(final ReplicatedFileState file) {
        assert(!file.isLocalIsPrimary());
        try {
            if (file.getPolicy().onBackup()) {
                doReset(file,ReplicaUpdatePolicy.UNBOUND_RESET);
            } else {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"replica state changed for %s from %s to %s",
                            file.getFileId(),file.getState(),ReplicaState.BACKUP);
                }
                file.setPrimaryReset(false);
                file.setState(ReplicaState.BACKUP);
                this.q.addAll(file.getPendingRequests());
                file.getPendingRequests().clear();
            }
        } catch (IOException ex) {
            failed(file, ex);
        }
    }

    private void failed(ReplicatedFileState file, Exception ex) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"replica for file %s failed: %s",file.getFileId(),ex.toString());
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"replica state changed for %s from %s to %s",
                    file.getFileId(),file.getState(),ReplicaState.BACKUP);
        }
        file.setPrimaryReset(false);
        file.setState(ReplicaState.OPEN);
        for (StageRequest rq : file.getPendingRequests()) {
            RWReplicationCallback callback = (RWReplicationCallback) rq.getCallback();
            callback.failed(ex);
        }
    }


    public static interface RWReplicationCallback {
        public void success(long newObjectVersion);
        public void redirect(RedirectException redirectTo);
        public void failed(Exception ex);
    }

    public void openFile(FileCredentials credentials, XLocations locations, long maxObjVersion,
            RWReplicationCallback callback, OSDRequest request) {
        this.enqueueOperation(STAGEOP_OPEN, new Object[]{credentials,locations,maxObjVersion}, request, callback);
    }

    public void prepareOperation(FileCredentials credentials, long objNo, long objVersion, Operation op, RWReplicationCallback callback,
            OSDRequest request) {
        this.enqueueOperation(STAGEOP_PREPAREOP, new Object[]{credentials,objNo,objVersion,op}, request, callback);
    }
    
    public void replicatedWrite(FileCredentials credentials, long objNo, long objVersion, ObjectData data,
            XLocations locations,
            RWReplicationCallback callback, OSDRequest request) {
        this.enqueueOperation(STAGEOP_REPLICATED_WRITE, new Object[]{credentials,objNo,objVersion,data,locations}, request, callback);
    }

    public void replicateTruncate(FileCredentials credentials, long newFileSize, long newObjectVersion,
            XLocations locations,
            RWReplicationCallback callback, OSDRequest request) {
        this.enqueueOperation(STAGEOP_TRUNCATE, new Object[]{credentials,newFileSize,newObjectVersion,locations}, request, callback);
    }

    public void fileClosed(String fileId) {
        this.enqueueOperation(STAGEOP_CLOSE, new Object[]{fileId}, null, null);
    }

    public void receiveFleaseMessage(ReusableBuffer message, InetSocketAddress sender) {
        this.enqueueOperation(STAGEOP_PROCESS_FLEASE_MSG, new Object[]{message,sender}, null, null);
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
        osdClient.rwr_flease_msg(recipient, data, master.getHostName(),master.getConfig().getPort());
    }


    @Override
    protected void processMethod(StageRequest method) {
        switch (method.getStageMethod()) {
            case STAGEOP_REPLICATED_WRITE : processReplicatedWrite(method); break;
            case STAGEOP_TRUNCATE : processReplicatedTruncate(method); break;
            case STAGEOP_CLOSE : processFileClosed(method); break;
            case STAGEOP_PROCESS_FLEASE_MSG : processFleaseMessage(method); break;
            case STAGEOP_PREPAREOP : processPrepareOp(method); break;
            case STAGEOP_OPEN : processFileOpen(method); break;
            case STAGEOP_INTERNAL_GETOBJECTS : processFetchObjects(method); break;
            case STAGEOP_LEASE_STATE_CHANGED : processLeaseStateChanged(method); break;
            case STAGEOP_INTERNAL_OBJFETCHED : processObjectFetched(method); break;
            case STAGEOP_INTERNAL_FSAVAIL : processFSAvailExecReset(method); break;
            case STAGEOP_INTERNAL_TRUNCED : processTruncateComplete(method); break;
            //case STAGEOP_GETSTATUS : processGetStatus(method); break;
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
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"closing file %s",fileId);
                }
                state.getPolicy().closeFile();
                cellToFileId.remove(state.getPolicy().getCellId());
            }
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void processFileOpen(StageRequest method) {
        final RWReplicationCallback callback = (RWReplicationCallback) method.getCallback();

        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final XLocations loc = (XLocations) method.getArgs()[1];
            final Long maxObjVersion = (Long) method.getArgs()[2];

            final String fileId = credentials.getXcap().getFile_id();

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"open file: "+fileId);


            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                //"open" file
                state = new ReplicatedFileState(fileId,loc, master.getConfig().getUUID(), fstage, osdClient, maxObjVersion);
                files.put(fileId,state);
                state.setCredentials(credentials);
                cellToFileId.put(state.getPolicy().getCellId(),fileId);
                assert(state.getState() == ReplicaState.OPEN);
                callback.success(0);
                
            } else {
                callback.failed(new IOException("file was already opened!"));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ex);
        }
    }

    private void processReplicatedWrite(StageRequest method) {
        final RWReplicationCallback callback = (RWReplicationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final Long objNo = (Long) method.getArgs()[1];
            final Long objVersion = (Long) method.getArgs()[2];
            final ObjectData objData  = (ObjectData) method.getArgs()[3];
            final XLocations loc = (XLocations) method.getArgs()[4];


            final String fileId = credentials.getXcap().getFile_id();


            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                callback.failed(new IllegalArgumentException("file is not open!"));
            }
            state.setCredentials(credentials);

            state.getPolicy().executeWrite(credentials, objNo, objVersion, objData, new ReplicaUpdatePolicy.ClientOperationCallback() {

                @Override
                public void finsihed() {
                    callback.success(objVersion);
                }

                @Override
                public void failed(Exception error) {
                    callback.failed(error);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ex);
        }
    }

    private void processReplicatedTruncate(StageRequest method) {
        final RWReplicationCallback callback = (RWReplicationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final Long newFileSize = (Long) method.getArgs()[1];
            final Long newObjVersion = (Long) method.getArgs()[2];
            final XLocations loc = (XLocations) method.getArgs()[3];


            final String fileId = credentials.getXcap().getFile_id();


            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                callback.failed(new IllegalArgumentException("file is not open!"));
            }
            state.setCredentials(credentials);

            state.getPolicy().executeTruncate(credentials, newFileSize, newObjVersion, new ReplicaUpdatePolicy.ClientOperationCallback() {

                @Override
                public void finsihed() {
                    callback.success(newObjVersion);
                }

                @Override
                public void failed(Exception error) {
                    callback.failed(error);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ex);
        }
    }

    private void processPrepareOp(StageRequest method) {
        final RWReplicationCallback callback = (RWReplicationCallback)method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final Long objVersion = (Long) method.getArgs()[2];
            final Operation op = (Operation) method.getArgs()[3];

            final String fileId = credentials.getXcap().getFile_id();

            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                callback.failed(new IllegalArgumentException("file "+fileId+" is not open!"));
                return;
            }

            if ((op == Operation.INTERNAL_UPDATE) || (op == Operation.INTERNAL_TRUNCATE)) {
                switch (state.getState()) {
                    case WAITING_FOR_LEASE:
                    case RESET : {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,"enqeue update for %s (state is %s)",fileId,state.getState());
                        }
                        state.getPendingRequests().add(method);
                        return;
                    }
                }
                boolean needsReset = state.getPolicy().onRemoteUpdate(objVersion, state.getState());
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"%s needs reset: %s",fileId,needsReset);
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
                    case RESET : {
                        state.getPendingRequests().add(method);
                        return;
                    }
                    case OPEN : {
                        state.getPendingRequests().add(method);
                        doWaitingForLease(state);
                        return;
                    }
                }
                long newVersion = state.getPolicy().onClientOperation(op,objVersion,state.getState(),state.getLease());
                callback.success(newVersion);
            }
        } catch (RedirectException ex) {
            callback.redirect(ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ex);
        }
    }

//    private void processGetStatus(StageRequest method) {
//        final StatusCallback callback = (StatusCallback)method.getCallback();
//        try {
//            Map<String,Map<String,String>> status = new HashMap();
//
//            Map<ASCIIString,FleaseMessage> fleaseState = fstage.getLocalState();
//
//            for (String fileId : this.files.keySet()) {
//                Map<String,String> fStatus = new HashMap();
//                final ReplicatedFileState fState = files.get(fileId);
//                final ASCIIString cellId = fState.getPolicy().getCellId();
//                fStatus.put("policy",fState.getPolicy().getClass().getSimpleName());
//                fStatus.put("peers (OSDs)",fState.getPolicy().getRemoteOSDs().toString());
//                fStatus.put("pending updates", "n/a");
//                fStatus.put("cellId", cellId.toString());
//                String primary = "unknown";
//                if (fleaseState.get(cellId) != null) {
//                    primary = fleaseState.get(cellId).getLeaseHolder().toString();
//                    if (primary.equals(master.getConfig().getUUID().toString())) {
//                        primary = "primary";
//                    } else {
//                        primary = "backup ( primary is "+primary+")";
//                    }
//                }
//                fStatus.put("role", primary);
//                status.put(fileId,fStatus);
//            }
//            callback.statusComplete(status);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            callback.statusComplete(null);
//        }
//    }

    

}
