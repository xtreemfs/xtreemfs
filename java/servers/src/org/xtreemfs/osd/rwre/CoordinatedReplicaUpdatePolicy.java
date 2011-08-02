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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;
import org.xtreemfs.osd.rwre.ReplicatedFileState.ReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.AuthoritativeReplicaState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersion;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectVersionMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateLog;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.TruncateRecord;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 *
 * @author bjko
 */
public abstract class CoordinatedReplicaUpdatePolicy extends ReplicaUpdatePolicy {

    public static final String FILE_CELLID_PREFIX = "/file/";

    private final OSDServiceClient client;


    public CoordinatedReplicaUpdatePolicy(List<ServiceUUID> remoteOSDUUIDs, String localUUID, String fileId, OSDServiceClient client) throws IOException {
        super(remoteOSDUUIDs,new ASCIIString(FILE_CELLID_PREFIX+fileId), localUUID);
        this.client = client;
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) created %s for %s",localUUID,this.getClass().getSimpleName(),cellId);
    }

    /**
     *
     * @param operation
     * @return number of external acks required for an operation (majority minus local replica).
     */
    protected abstract int getNumRequiredAcks(Operation operation);

    protected abstract boolean backupCanRead();
   

    @Override
    public void closeFile() {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) closed %s for %s",localUUID,this.getClass().getSimpleName(),cellId);
    }

    @Override
    public boolean requiresLease() {
        return true;
    }

    @Override
    public void executeReset(FileCredentials credentials, final ReplicaStatus localReplicaState, final ExecuteResetCallback callback) {
        final String fileId = credentials.getXcap().getFileId();
        final int numAcksRequired = getNumRequiredAcks(Operation.INTERNAL_UPDATE);
        final int numRequests = remoteOSDUUIDs.size();
        final int maxErrors = numRequests - numAcksRequired;

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) fetching replica state for %s from %d replicas (majority: %d)",
                    localUUID, fileId, numRequests, numAcksRequired);
        }

        final RPCResponse[] responses = new RPCResponse[remoteOSDUUIDs.size()];
        try {
            for (int i = 0; i < responses.length; i++) {
                responses[i] = client.xtreemfs_rwr_status(remoteOSDUUIDs.get(i).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                        credentials, credentials.getXcap().getFileId(),
                        this.localObjVersion);
            }
        } catch (IOException ex) {
            callback.failed(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString(),ex));
            return;
        }

        RPCResponseAvailableListener listener = new RPCResponseAvailableListener() {
            int numResponses = 0;
            int numErrors = 0;
            boolean exceptionSent = false;
            ReplicaStatus[] states = new ReplicaStatus[numAcksRequired + 1];

            @Override
            public void responseAvailable(RPCResponse r) {
                if (numResponses < numAcksRequired) {
                    int osdNum = -1;
                    for (int i = 0; i < numRequests; i++) {
                        if (responses[i] == r) {
                            osdNum = i;
                            break;
                        }
                    }
                    assert(osdNum > -1);
                    try {
                        states[osdNum] = (ReplicaStatus)r.get();
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) received status response for %s from %s", localUUID, fileId, remoteOSDUUIDs.get(osdNum));
                        }
                        numResponses++;
                    } catch (Exception ex) {
                        numErrors++;
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"no status response from %s fro %s due to exception: %s (acks: %d, errs: %d, maxErrs: %d)",
                                               remoteOSDUUIDs.get(osdNum), fileId, ex.toString(), numResponses, numErrors, maxErrors);
                        if (numErrors > maxErrors) {
                            if (!exceptionSent) {
                                exceptionSent = true;
                                if (Logging.isDebug()) {
                                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) read status FAILED for %s on %s",localUUID, fileId, remoteOSDUUIDs.get(osdNum));
                                }
                                callback.failed(ErrorUtils.getInternalServerError(ex));
                            }
                        }
                        return;
                    } finally {
                        r.freeBuffers();
                    }
                } else {
                    try {
                        r.get();
                    } catch (Exception e) {
                        // ignore.
                    }
                    r.freeBuffers();
                    return;
                }

                if (numResponses == numAcksRequired) {
                    states[numAcksRequired] = localReplicaState;
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) received enough status responses for %s",localUUID, fileId);
                    }
                    AuthoritativeReplicaState auth = CalculateAuthoritativeState(states, fileId);
                    // TODO(bjko): Send auth state to all backups.

                    callback.finished(auth);
                }

            }
        };
        for (int i = 0; i < responses.length; i++) {
            responses[i].registerListener(listener);
        }

        //callback.writeUpdateCompleted(null, null, null);
    }
    
    private static final class ObjectMapRecord {
        public long version;
        public List<InetSocketAddress> osds;
    }

    public AuthoritativeReplicaState CalculateAuthoritativeState(ReplicaStatus[] states, String fileId) {
        StringBuilder stateStr = new StringBuilder();
        Map<Long,TruncateRecord> truncateLog = new HashMap();
        Map<Long,ObjectVersionMapping.Builder> all_objects = new HashMap();
        long maxTruncateEpoch = 0;
        long maxObjectVersion = 0;

        for (int i = 0; i < states.length; i++) {
            final ReplicaStatus state = states[i];
            if (state == null) {
                // Skip null entries, server did not respond.
                continue;
            }
            if (state.getTruncateEpoch() > maxTruncateEpoch) {
                maxTruncateEpoch = state.getTruncateEpoch();
            }
            for (TruncateRecord trec : state.getTruncateLog().getRecordsList()) {
                truncateLog.put(trec.getVersion(), trec);
            }
            for (ObjectVersion over : state.getObjectVersionsList()) {
                final long onum = over.getObjectNumber();
                if (over.getObjectVersion() > maxObjectVersion) {
                    maxObjectVersion = over.getObjectVersion();
                }
                ObjectVersionMapping.Builder omr = all_objects.get(onum);
                if ((omr == null) || (omr.getObjectVersion() < over.getObjectVersion())) {
                    omr = ObjectVersionMapping.newBuilder();
                    omr.setObjectVersion(over.getObjectVersion());
                    omr.setObjectNumber(onum);
                    all_objects.put(onum, omr);
                }
                if (omr.getObjectVersion() == over.getObjectVersion()) {
                    if (i < states.length - 1) {
                        omr.addOsdUuids(remoteOSDUUIDs.get(i).toString());
                    } else {
                        omr.addOsdUuids(localUUID);
                        // Last state is the local state, i.e. local OSD.
                    }
                }
            }
        }

        for (TruncateRecord trec : truncateLog.values()) {
            Iterator<Entry<Long, ObjectVersionMapping.Builder>> iter = all_objects.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<Long, ObjectVersionMapping.Builder> e = iter.next();
                if (e.getKey() > trec.getLastObjectNumber() &&
                    e.getValue().getObjectVersion() < trec.getVersion()) {
                    iter.remove();
                }
            }
        }

        if (Logging.isDebug()) {
            stateStr.append("tlog={");
            for (TruncateRecord trec : truncateLog.values()) {
                stateStr.append("(");
                stateStr.append(trec.getVersion());
                stateStr.append(",");
                stateStr.append(trec.getLastObjectNumber());
                stateStr.append("),");
            }
            stateStr.append("} ");

            stateStr.append("objs={");
            for (Entry<Long, ObjectVersionMapping.Builder> obj : all_objects.entrySet()) {
                stateStr.append("(");
                stateStr.append(obj.getKey());
                stateStr.append(",");
                stateStr.append(obj.getValue().getObjectVersion());
                stateStr.append("),");
            }
            stateStr.append("} maxV=");
            stateStr.append(maxObjectVersion);
            stateStr.append(" maxTE=");
            stateStr.append(maxTruncateEpoch);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) AUTH state for %s: %s",localUUID, fileId, stateStr.toString());
            }
        }

        AuthoritativeReplicaState.Builder auth = AuthoritativeReplicaState.newBuilder();
        auth.setTruncateEpoch(0);

        TruncateLog.Builder tlogBuilder = TruncateLog.newBuilder();
        tlogBuilder.addAllRecords(truncateLog.values());
        auth.setTruncateLog(tlogBuilder);
        auth.setTruncateEpoch(maxTruncateEpoch);
        auth.setMaxObjVersion(maxObjectVersion);

        for (ObjectVersionMapping.Builder obj : all_objects.values()) {
            auth.addObjectVersions(obj);
        }

        return auth.build();
    }

    @Override
    public void executeWrite(FileCredentials credentials, long objNo, long objVersion, InternalObjectData data, final ClientOperationCallback callback) {
        final String fileId = credentials.getXcap().getFileId();
        final int numAcksRequired = getNumRequiredAcks(Operation.WRITE);
        final int numRequests = remoteOSDUUIDs.size();
        final int maxErrors = numRequests - numAcksRequired;
        
        final RPCResponse[] responses = new RPCResponse[remoteOSDUUIDs.size()];
        final RPCResponseAvailableListener l = getResponseListener(callback, maxErrors, numAcksRequired, fileId, Operation.WRITE);
        try {
            for (int i = 0; i < responses.length; i++) {
                responses[i] = client.xtreemfs_rwr_update(remoteOSDUUIDs.get(i).getAddress(),
                        RPCAuthentication.authNone, RPCAuthentication.userService,
                        credentials, credentials.getXcap().getFileId(), 0,
                        objNo, objVersion, 0, data.getMetadata(), data.getData().createViewBuffer());
                responses[i].registerListener(l);
            }
        } catch (IOException ex) {
            callback.failed(ErrorUtils.getInternalServerError(ex));
        } finally {
            BufferPool.free(data.getData());
        }

        //callback.writeUpdateCompleted(null, null, null);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) sent update for %s", localUUID, fileId);
    }

    @Override
    public void executeTruncate(FileCredentials credentials, long newFileSize, long newObjectVersion, final ClientOperationCallback callback) {
        final String fileId = credentials.getXcap().getFileId();
        final int numAcksRequired = getNumRequiredAcks(Operation.TRUNCATE);
        final int numRequests = remoteOSDUUIDs.size();
        final int maxErrors = numRequests - numAcksRequired;

        final RPCResponseAvailableListener l = getResponseListener(callback, maxErrors, numAcksRequired, fileId, Operation.TRUNCATE);
        final RPCResponse[] responses = new RPCResponse[remoteOSDUUIDs.size()];
        try {
            for (int i = 0; i < responses.length; i++) {
                responses[i] = client.xtreemfs_rwr_truncate(remoteOSDUUIDs.get(i).getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                        credentials, credentials.getXcap().getFileId(), newFileSize, newObjectVersion);
                responses[i].registerListener(l);
            }
        } catch (IOException ex) {
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }

        //callback.writeUpdateCompleted(null, null, null);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) sent truncate update for %s", localUUID, fileId);
    }

    protected RPCResponseAvailableListener getResponseListener(final ClientOperationCallback callback,
            final int maxErrors, final int numAcksRequired, final String fileId, final Operation operation) {

        assert(numAcksRequired <= this.remoteOSDUUIDs.size());
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) new response listener for %s (acks %d, errs %d)",localUUID,fileId,numAcksRequired,maxErrors);

        assert(maxErrors >= 0);
        RPCResponseAvailableListener listener = new RPCResponseAvailableListener() {
            int numAcks = 0;
            int numErrors = 0;
            boolean exceptionSent = false;

            @Override
            public void responseAvailable(RPCResponse r) {
                try {
                    r.get();
                    numAcks++;
                } catch (Exception ex) {
                    numErrors++;
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"exception for %s/%s (acks: %d, errs: %d, maxErrs: %d)",
                                           operation, fileId, numAcks, numErrors, maxErrors);
                    if (numErrors > maxErrors) {
                        if (!exceptionSent) {
                            exceptionSent = true;
                            Logging.logMessage(Logging.LEVEL_INFO, Category.replication, this,"replicated %s FAILED for %s (acks: %d, errs: %d, maxErrs: %d)",
                                               operation, fileId, numAcks, numErrors, maxErrors);
                            callback.failed(ErrorUtils.getInternalServerError(ex));
                        }
                    }
                    return;
                } finally {
                    r.freeBuffers();
                }
                if (numAcks == numAcksRequired) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"replicated %s successfull for %s",operation,fileId);
                    }
                    callback.finsihed();
                }

            }
        };
        return listener;
    }

    @Override
    public long onClientOperation(Operation operation, long objVersion, ReplicaState currentState, Flease lease) throws RedirectToMasterException, IOException {
        if (currentState == ReplicaState.PRIMARY) {
            long tmpObjVer;
            if (operation != Operation.READ) {
                if (this.localObjVersion == -1) {
                    this.localObjVersion = 0;
                }
                assert(this.localObjVersion > -1);
                tmpObjVer = ++this.localObjVersion;
            } else {
                tmpObjVer = localObjVersion;
            }
            final long nextObjVer = tmpObjVer;

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,"(R:%s) prepared op for %s with objVer %d",localUUID, cellId,nextObjVer);

            return nextObjVer;
        } else if (currentState == ReplicaState.BACKUP) {
            if (backupCanRead() && (operation == Operation.READ)) {
                return this.localObjVersion;
            } else {
                if ((lease == null) || (lease.isEmptyLease())) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,"unknown lease state for %s: %s",this.cellId,lease);
                    throw new RetryException("unknown lease state for cell "+this.cellId+", can't redirect to master. Please retry.");
                } else
                    Logging.logMessage(Logging.LEVEL_INFO, Category.replication, this, "(R:%s) local is backup, redirecting for fileid %s to %s",
                            localUUID, this.cellId,lease.getLeaseHolder().toString());
                    throw new RedirectToMasterException(lease.getLeaseHolder().toString());
            }
        } else {
            throw new IOException("invalid state: "+currentState);
        }
    }

    @Override
    public boolean onRemoteUpdate(long objVersion, ReplicaState state) throws IOException {
        //apply everything...
        if (state == ReplicaState.PRIMARY) {
            throw new IOException("no accepting updates in PRIMARY mode");
        }

        if ((objVersion == 1) && (localObjVersion == -1)) {
            localObjVersion = 1;
            return false;
        }

        if (objVersion > localObjVersion)
            localObjVersion = objVersion;
        return false;
    }

    @Override
    public boolean onPrimary(int masterEpoch) throws IOException {
        //no need to catch up on primary
        if (masterEpoch != FleaseMessage.IGNORE_MASTER_EPOCH) {
            this.localObjVersion = (long)masterEpoch << 32;
        }
        return true;
    }

    @Override
    public boolean onBackup() throws IOException {
        return false;
    }

    @Override
    public void onFailed() throws IOException {
        //don't care
    }

}
