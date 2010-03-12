/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.flease.FleaseFuture;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.proposer.FleaseListener;
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
public class WaR1UpdatePolicy extends ReplicaUpdatePolicy {

    public static final String FILE_CELLID_PREFIX = "/file/";

    private final OSDClient client;


    public WaR1UpdatePolicy(List<InetSocketAddress> remoteOSDs, String fileId, long maxObjVerOnDisk, OSDClient client) throws IOException, InterruptedException {
        super(remoteOSDs,new ASCIIString(FILE_CELLID_PREFIX+fileId),maxObjVerOnDisk);
        this.client = client;
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"created WaR1 for "+cellId);
    }

    @Override
    public void closeFile() {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"closed WaR1 for "+cellId);
    }

    @Override
    public boolean requiresLease() {
        return true;
    }

    @Override
    public void executeReset(FileCredentials credentials, final long updateObjVer, final ExecuteResetCallback callback) {
        final String fileId = credentials.getXcap().getFile_id();
        final int numResponsesRequired = this.getRemoteOSDs().size();

        final RPCResponse[] responses = new RPCResponse[remoteOSDs.size()];
        for (int i = 0; i < responses.length; i++) {
            responses[i] = client.rwr_status(remoteOSDs.get(i),
                    credentials, credentials.getXcap().getFile_id(),
                    this.localObjVersion);
        }

        RPCResponseAvailableListener listener = new RPCResponseAvailableListener() {
            int numResponses = 0;
            boolean exceptionSent = false;
            ReplicaStatus[] states = new ReplicaStatus[numResponsesRequired];

            @Override
            public void responseAvailable(RPCResponse r) {
                int osdNum = -1;
                for (int i = 0; i < numResponsesRequired; i++) {
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
                    if (!exceptionSent) {
                        exceptionSent = true;
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,"read status FAILED for %s",fileId);
                        }
                        callback.failed(ex);
                    }
                    return;
                } finally {
                    r.freeBuffers();
                }
                if (numResponses == numResponsesRequired) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"read status successfull for %s",fileId);
                    }
                    //analyze the state
                    boolean needsReset = false;
                    long maxObjVer = 0;
                    for (ReplicaStatus state : states) {
                        if (state.getMax_obj_version() > localObjVersion) {
                            needsReset = true;
                            maxObjVer = state.getMax_obj_version();
                            break;
                        }
                    }
                    if (needsReset) {

                        Map<Long,ObjectFetchRecord> recs = new HashMap();
                        LinkedList<ObjectFetchRecord> order = new LinkedList<ObjectFetchRecord>();
                        for (int i = 0; i < numResponsesRequired; i++) {
                            final ReplicaStatus state = states[i];

                            for (ObjectVersion over : state.getObjectVersions()) {
                                //skip entries which are newer than the update we received
                                if ((updateObjVer > -1) && (over.getObject_version() >= updateObjVer))
                                    continue;

                                ObjectFetchRecord ofr = recs.get(over.getObject_number());
                                if (ofr == null) {
                                    List<InetSocketAddress> osds = new ArrayList(numResponsesRequired);
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
        final int numResponsesRequired = this.getRemoteOSDs().size();
        RPCResponseAvailableListener listener = new RPCResponseAvailableListener() {
            int numResponses = 0;
            boolean exceptionSent = false;

            @Override
            public void responseAvailable(RPCResponse r) {
                try {
                    r.get();
                    numResponses++;
                } catch (Exception ex) {
                    if (!exceptionSent) {
                        exceptionSent = true;
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,"replicated write FAILED for %s",fileId);
                        }
                        callback.failed(ex);
                    }
                    return;
                } finally {
                    r.freeBuffers();
                }
                if (numResponses == numResponsesRequired) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"replicated write successfull for %s",fileId);
                    }
                    callback.finsihed();
                }

            }
        };
        RPCResponse[] responses = new RPCResponse[remoteOSDs.size()];
        for (int i = 0; i < responses.length; i++) {
            ObjectData dataCpy = new ObjectData(data.getChecksum(), data.getInvalid_checksum_on_osd(), data.getZero_padding(), data.getData().createViewBuffer());
            responses[i] = client.rwr_update(remoteOSDs.get(i),
                    credentials, credentials.getXcap().getFile_id(),
                    objNo, objVersion, 0, dataCpy);
            responses[i].registerListener(listener);
        }
        BufferPool.free(data.getData());

        //callback.writeUpdateCompleted(null, null, null);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"sent update for "+cellId);
    }

    @Override
    public void executeTruncate(FileCredentials credentials, long newFileSize, long newObjectVersion, final ClientOperationCallback callback) {
        final String fileId = credentials.getXcap().getFile_id();
        final int numResponsesRequired = this.getRemoteOSDs().size();
        RPCResponseAvailableListener listener = new RPCResponseAvailableListener() {
            int numResponses = 0;
            boolean exceptionSent = false;

            @Override
            public void responseAvailable(RPCResponse r) {
                try {
                    r.get();
                    numResponses++;
                } catch (Exception ex) {
                    if (!exceptionSent) {
                        exceptionSent = true;
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,"replicated write FAILED for %s",fileId);
                        }
                        callback.failed(ex);
                    }
                    return;
                } finally {
                    r.freeBuffers();
                }
                if (numResponses == numResponsesRequired) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"replicated write successfull for %s",fileId);
                    }
                    callback.finsihed();
                }

            }
        };
        RPCResponse[] responses = new RPCResponse[remoteOSDs.size()];
        for (int i = 0; i < responses.length; i++) {
            responses[i] = client.rwr_truncate(remoteOSDs.get(i),
                    credentials, credentials.getXcap().getFile_id(), newFileSize, newObjectVersion);
            responses[i].registerListener(listener);
        }

        //callback.writeUpdateCompleted(null, null, null);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"sent truncate updates for "+cellId);
    }

    @Override
    public long onClientOperation(Operation operation, long objVersion, ReplicaState currentState, ASCIIString leaseOwner) throws RedirectException, OSDException, IOException {
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
            if (leaseOwner == null)
                throw new OSDException(ErrorCodes.LEASE_TIMED_OUT, "unknown lease state, can't redirect to master", "");
            else
                throw new RedirectException(leaseOwner.toString());
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

        if ((objVersion == 1) && (localObjVersion == -1))
            return false;

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
