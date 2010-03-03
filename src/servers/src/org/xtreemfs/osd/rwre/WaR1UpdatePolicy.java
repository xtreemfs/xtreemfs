/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.flease.FleaseFuture;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.proposer.FleaseListener;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;

/**
 *
 * @author bjko
 */
public class WaR1UpdatePolicy extends ReplicaUpdatePolicy {

    public static final String FILE_CELLID_PREFIX = "/file/";

    private final FleaseStage fstage;

    private final OSDClient client;

    private long            objVersion;

    public WaR1UpdatePolicy(List<InetSocketAddress> remoteOSDs, String fileId, FleaseStage fstage, OSDClient client) throws IOException, InterruptedException {
        super(remoteOSDs,new ASCIIString(FILE_CELLID_PREFIX+fileId));
        this.fstage = fstage;
        this.client = client;
        objVersion = -1;
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"created WaR1 for "+cellId);
    }

    @Override
    public boolean hasFinished(int numResponses) throws IOException {
        return numResponses == super.remoteOSDs.size();
        //return true;
    }

    @Override
    public void closeFile() {
        fstage.closeCell(cellId);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"closed WaR1 for "+cellId);
    }

    @Override
    public void writeUpdate(final FileCredentials credentials, final long objNo, final long objVersion, final ObjectData data, final ReplicatedOperationCallback callback) {
        //get lease
        assert(objVersion >= 0);

        RPCResponse[] responses = new RPCResponse[remoteOSDs.size()];
        for (int i = 0; i < responses.length; i++) {
            ObjectData dataCpy = new ObjectData(data.getChecksum(), data.getInvalid_checksum_on_osd(), data.getZero_padding(), data.getData().createViewBuffer());
            responses[i] = client.rwr_update(remoteOSDs.get(i),
                    credentials, credentials.getXcap().getFile_id(),
                    objNo, objVersion, 0, dataCpy);
        }
        BufferPool.free(data.getData());
        callback.operationCompleted(responses, null, null);
         
        //callback.writeUpdateCompleted(null, null, null);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"sent update for "+cellId);

    }

    @Override
    public void openFile(final long maxObjNo, final OpenFileCallback callback) {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"opening "+cellId+"...");

        if (maxObjNo > 0)
            this.objVersion = maxObjNo;
        else
            this.objVersion = 1;
        
        try {
            FleaseFuture f = fstage.openCell(cellId, remoteOSDs, true);
            f.get();
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.fileOpened(false, false, ex);
            return;
        }

        fstage.getLease(cellId, new FleaseListener() {

            @Override
            public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms) {
                if (leaseHolder.equals(fstage.getIdentity())) {
                    callback.fileOpened(true, true, null);
                } else {
                    callback.fileOpened(false, false, null);
                }
            }

            @Override
            public void proposalFailed(ASCIIString cellId, Throwable cause) {
                callback.fileOpened(false, false, new IOException("cannot get primary lease for file",cause));
            }
        });
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"opened "+cellId);
    }


    @Override
    public void prepareOperation(long objNo, long objVersion, Operation operation,final PrepareOperationCallback callback) {
        //get lease
        long tmpObjVer;
        if (operation != Operation.READ) {
            assert(objVersion >= 0);
             tmpObjVer = ++this.objVersion;
        } else {
            tmpObjVer = objVersion;
        }
        final long nextObjVer = tmpObjVer;

        fstage.getLease(cellId, new FleaseListener() {

            @Override
            public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms) {
                System.out.println("prep finished: "+cellId+" to="+leaseTimeout_ms);
                if (leaseHolder.equals(fstage.getIdentity())) {
                    callback.prepareOperationComplete(true, null, nextObjVer, null);
                } else {
                    callback.prepareOperationComplete(false, leaseHolder.toString(), -1l, null);
                }
            }

            @Override
            public void proposalFailed(ASCIIString cellId, Throwable cause) {
                callback.prepareOperationComplete(false, null, -1l,
                        new IOException("cannot acquire primary lease: "+cause.getMessage(),cause));
            }
        });
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"prepared op for "+cellId);
    }

    @Override
    public void truncateFile(FileCredentials credentials, long newFileSize, long newObjVer, ReplicatedOperationCallback callback) {
        assert(objVersion >= 0);

        RPCResponse[] responses = new RPCResponse[remoteOSDs.size()];
        for (int i = 0; i < responses.length; i++) {
            responses[i] = client.rwr_truncate(remoteOSDs.get(i),
                    credentials, credentials.getXcap().getFile_id(), newFileSize, newObjVer);
        }
        callback.operationCompleted(responses, null, null);

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"sent update for "+cellId);

    }

}
