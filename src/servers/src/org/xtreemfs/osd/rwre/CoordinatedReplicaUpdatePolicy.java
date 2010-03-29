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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.ObjectVersion;
import org.xtreemfs.interfaces.ReplicaStatus;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;
import org.xtreemfs.osd.rwre.ReplicatedFileState.ReplicaState;

/**
 *
 * @author bjko
 */
public abstract class CoordinatedReplicaUpdatePolicy extends ReplicaUpdatePolicy {

    public static final String FILE_CELLID_PREFIX = "/file/";

    private final OSDClient client;


    public CoordinatedReplicaUpdatePolicy(List<InetSocketAddress> remoteOSDs, String fileId, long maxObjVerOnDisk, OSDClient client) throws IOException, InterruptedException {
        super(remoteOSDs,new ASCIIString(FILE_CELLID_PREFIX+fileId),maxObjVerOnDisk);
        this.client = client;
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"created %s for %s",this.getClass().getSimpleName(),cellId);
    }

    protected abstract int getNumRequiredAcks(Operation operation);

    protected abstract boolean backupCanRead();
   

    @Override
    public void closeFile() {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"closed %s for %s",this.getClass().getSimpleName(),cellId);
    }

    @Override
    public boolean requiresLease() {
        return true;
    }

    @Override
    public void executeReset(FileCredentials credentials, final long updateObjVer, final long localFileSize,
            final long localTruncateEpoch, final ExecuteResetCallback callback) {
        final String fileId = credentials.getXcap().getFile_id();
        final int numAcksRequired = getNumRequiredAcks(Operation.INTERNAL_UPDATE);
        final int numRequests = remoteOSDs.size();
        final int maxErrors = numRequests-numAcksRequired;
        System.out.println("max errors: "+maxErrors+", numAcks: "+numAcksRequired+" numRequests: "+numRequests);

        final RPCResponse[] responses = new RPCResponse[remoteOSDs.size()];
        for (int i = 0; i < responses.length; i++) {
            responses[i] = client.rwr_status(remoteOSDs.get(i),
                    credentials, credentials.getXcap().getFile_id(),
                    this.localObjVersion);
        }

        RPCResponseAvailableListener listener = new RPCResponseAvailableListener() {
            int numResponses = 0;
            int numErrors = 0;
            boolean exceptionSent = false;
            ReplicaStatus[] states = new ReplicaStatus[numAcksRequired];

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
                        numResponses++;
                    } catch (Exception ex) {
                        numErrors++;
                        if (numErrors > maxErrors) {
                            if (!exceptionSent) {
                                exceptionSent = true;
                                if (Logging.isDebug()) {
                                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"read status FAILED for %s",fileId);
                                }
                                callback.failed(ex);
                            }
                        }
                        return;
                    } finally {
                        r.freeBuffers();
                    }
                }

                if (numResponses == numAcksRequired) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"read status successfull for %s",fileId);
                    }
                    //analyze the state
                    boolean needsReset = false;
                    long maxObjVer = 0;
                    long maxFileSize = 0;
                    long maxTrEpoch = 0;
                    for (ReplicaStatus state : states) {
                        if (state.getMax_obj_version() > localObjVersion) {
                            needsReset = true;
                            maxObjVer = state.getMax_obj_version();
                            maxFileSize = state.getFile_size();
                            maxTrEpoch = state.getTruncate_epoch();
                            break;
                        }
                    }
                    if (needsReset) {
                        Map<Long,ObjectFetchRecord> recs = new HashMap();
                        LinkedList<ObjectFetchRecord> order = new LinkedList<ObjectFetchRecord>();

                        //make sure to truncate if necessary
                        if ((localTruncateEpoch < maxTrEpoch) && (localFileSize > maxFileSize)) {
                            //we need to execute a local truncate first!
                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this,"file needs truncate: %s (te local=%d, remote=%d)",fileId,localTruncateEpoch,maxTrEpoch);
                            }
                            order.add(new ObjectFetchRecord(maxFileSize, maxTrEpoch));
                        }

                        for (int i = 0; i < numAcksRequired; i++) {
                            final ReplicaStatus state = states[i];

                            for (ObjectVersion over : state.getObjectVersions()) {
                                //skip entries which are newer than the update we received
                                if ((updateObjVer > -1) && (over.getObject_version() >= updateObjVer))
                                    continue;

                                ObjectFetchRecord ofr = recs.get(over.getObject_number());
                                if (ofr == null) {
                                    List<InetSocketAddress> osds = new ArrayList(numAcksRequired);
                                    osds.add(getRemoteOSDs().get(i));
                                    ofr = new ObjectFetchRecord(over.getObject_number(), over.getObject_version(), osds);
                                    recs.put(over.getObject_number(), ofr);
                                    order.add(ofr);
                                } else {
                                    if (ofr.getObjVersion() < over.getObject_version()) {
                                        ofr.setObjVersion(over.getObject_version());
                                        ofr.getOsds().clear();
                                        ofr.getOsds().add(getRemoteOSDs().get(i));
                                    } else if (ofr.getObjVersion() == over.getObject_version()) {
                                        ofr.getOsds().add(getRemoteOSDs().get(i));
                                    }
                                }
                            }
                        }
                        Collections.sort(order, new Comparator<ObjectFetchRecord>() {

                            @Override
                            public int compare(ObjectFetchRecord o1, ObjectFetchRecord o2) {
                                if (o1.isTruncate())
                                    return -1;
                                int verCmp = (int)(o1.getObjVersion()-o2.getObjVersion());
                                if (verCmp == 0) {
                                    return (int)(o2.getObjNumber()-o2.getObjNumber());
                                } else
                                    return verCmp;
                            }
                        });
                        callback.finished(order);
                    } else {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,"reset not required for %s, max is %d",fileId,maxObjVer);
                        }
                        callback.finished(null);
                    }

                }

            }
        };
        for (int i = 0; i < responses.length; i++) {
            responses[i].registerListener(listener);
        }

        //callback.writeUpdateCompleted(null, null, null);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"sent update for "+cellId);
    }

    @Override
    public void executeWrite(FileCredentials credentials, long objNo, long objVersion, ObjectData data, final ClientOperationCallback callback) {
        final String fileId = credentials.getXcap().getFile_id();
        final int numAcksRequired = getNumRequiredAcks(Operation.WRITE);
        final int numRequests = remoteOSDs.size();
        final int maxErrors = numRequests-numAcksRequired;
        
        final RPCResponse[] responses = new RPCResponse[remoteOSDs.size()];
        final RPCResponseAvailableListener l = getResponseListener(callback, maxErrors, numAcksRequired, fileId, Operation.WRITE);
        for (int i = 0; i < responses.length; i++) {
            ObjectData dataCpy = new ObjectData(data.getChecksum(), data.getInvalid_checksum_on_osd(), data.getZero_padding(), data.getData().createViewBuffer());
            responses[i] = client.rwr_update(remoteOSDs.get(i),
                    credentials, credentials.getXcap().getFile_id(),
                    objNo, objVersion, 0, dataCpy);
            responses[i].registerListener(l);
        }
        BufferPool.free(data.getData());

        //callback.writeUpdateCompleted(null, null, null);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"sent update for "+cellId);
    }

    @Override
    public void executeTruncate(FileCredentials credentials, long newFileSize, long newObjectVersion, final ClientOperationCallback callback) {
        final String fileId = credentials.getXcap().getFile_id();
        final int numAcksRequired = getNumRequiredAcks(Operation.TRUNCATE);
        final int numRequests = remoteOSDs.size();
        final int maxErrors = numRequests-numAcksRequired;

        final RPCResponseAvailableListener l = getResponseListener(callback, maxErrors, numAcksRequired, fileId, Operation.TRUNCATE);
        final RPCResponse[] responses = new RPCResponse[remoteOSDs.size()];
        for (int i = 0; i < responses.length; i++) {
            responses[i] = client.rwr_truncate(remoteOSDs.get(i),
                    credentials, credentials.getXcap().getFile_id(), newFileSize, newObjectVersion);
            responses[i].registerListener(l);
        }

        //callback.writeUpdateCompleted(null, null, null);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"sent truncate updates for "+cellId);
    }

    protected RPCResponseAvailableListener getResponseListener(final ClientOperationCallback callback,
            final int maxErrors, final int numAcksRequired, final String fileId, final Operation operation) {

        assert(numAcksRequired <= this.remoteOSDs.size());
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
                    if (numErrors > maxErrors) {
                        if (!exceptionSent) {
                            exceptionSent = true;
                            Logging.logMessage(Logging.LEVEL_INFO, this,"replicated %s FAILED for %s",operation,fileId);
                            callback.failed(ex);
                        }
                        return;
                    }
                } finally {
                    r.freeBuffers();
                }
                if (numAcks == numAcksRequired) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"replicated %s successfull for %s",operation,fileId);
                    }
                    callback.finsihed();
                }

            }
        };
        return listener;
    }

    @Override
    public long onClientOperation(Operation operation, long objVersion, ReplicaState currentState, Flease lease) throws RedirectException, OSDException, IOException {
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
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"prepared op for %s with objVer %d",cellId,nextObjVer);

            return nextObjVer;
        } else if (currentState == ReplicaState.BACKUP) {
            if (backupCanRead() && (operation == Operation.READ)) {
                return this.localObjVersion;
            } else {
                if ((lease == null) || (lease.isEmptyLease()) || (!lease.isValid()))
                    throw new OSDException(ErrorCodes.LEASE_TIMED_OUT, "unknown lease state, can't redirect to master", "");
                else
                    throw new RedirectException(lease.getLeaseHolder().toString());
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

        if (objVersion > localObjVersion+1) {
            return true;
        }

        if (objVersion > localObjVersion)
            localObjVersion = objVersion;
        return false;
    }

    @Override
    public boolean onPrimary() throws IOException {
        //no need to catch up on primary
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
