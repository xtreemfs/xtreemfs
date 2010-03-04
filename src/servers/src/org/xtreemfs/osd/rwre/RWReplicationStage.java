/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.FleaseMessageSenderInterface;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.rwre.ReplicaUpdatePolicy.PrepareOperationCallback;
import org.xtreemfs.osd.stages.Stage;

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

    public  static enum Operation {
        READ,
        WRITE,
        TRUNCATE
    };


    private final RPCNIOSocketClient client;

    private final OSDClient          osdClient;

    private final Map<String,ReplicatedFileState> files;

    private final OSDRequestDispatcher master;

    private final FleaseStage          fstage;



    public RWReplicationStage(OSDRequestDispatcher master, SSLOptions sslOpts) throws IOException {
        super("RWReplSt");
        this.master = master;
        client = new RPCNIOSocketClient(sslOpts, 15000, 60000*5);
        osdClient = new OSDClient(client);
        files = new HashMap<String, ReplicatedFileState>();

        master.getConfig().getPort();

        FleaseConfig fcfg = new FleaseConfig(master.getConfig().getFleaseLeaseToMS(),
                master.getConfig().getFleaseDmaxMS(), master.getConfig().getFleaseDmaxMS(),
                null, master.getConfig().getUUID().toString(), master.getConfig().getFleaseRetries());

        fstage = new FleaseStage(fcfg, master.getConfig().getObjDir()+"/.flease_"+master.getConfig().getUUID().toString()+".lock",
                this, false, new FleaseViewChangeListenerInterface() {

            @Override
            public void viewIdChangeEvent(ASCIIString cellId, int viewId) {
                throw new UnsupportedOperationException("Not supported yet.");
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

    public void openFile(FileCredentials credentials, XLocations locations, long maxObjVersion,
            OpenFileCallback callback, OSDRequest request) {
        this.enqueueOperation(STAGEOP_OPEN, new Object[]{credentials,locations,maxObjVersion}, request, callback);
    }

    public static interface OpenFileCallback {
        public void fileOpenComplete(Exception error);
    }

    public void prepareOperation(FileCredentials credentials, long objNo, long objVersion, Operation op, PrepareOperationCallback callback,
            OSDRequest request) {
        this.enqueueOperation(STAGEOP_PREPAREOP, new Object[]{credentials,objNo,objVersion,op}, request, callback);
    }
    
    public void replicatedWrite(FileCredentials credentials, long objNo, long objVersion, ObjectData data,
            XLocations locations,
            ReplicatedOperationCallback callback, OSDRequest request) {
        this.enqueueOperation(STAGEOP_REPLICATED_WRITE, new Object[]{credentials,objNo,objVersion,data,locations}, request, callback);
    }

    public void replicateTruncate(FileCredentials credentials, long newFileSize, long newObjectVersion,
            XLocations locations,
            ReplicatedOperationCallback callback, OSDRequest request) {
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
    public void sendMessage(FleaseMessage message, InetSocketAddress recipient) throws IOException {
        ReusableBuffer data = BufferPool.allocate(message.getSize());
        message.serialize(data);
        data.flip();
        osdClient.rwr_flease_msg(recipient, data, master.getHostName(),master.getConfig().getPort());
    }


    public static interface ReplicatedOperationCallback {
        public void writeCompleted(Exception error);
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
                state.getPolicy().closeFile();
            }
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this,ex);
        }
    }

    private void processFileOpen(StageRequest method) {
        final OpenFileCallback callback = (OpenFileCallback) method.getCallback();

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
                state = new ReplicatedFileState(fileId,loc, master.getConfig().getUUID(), fstage, osdClient);
                files.put(fileId,state);
                state.getPolicy().openFile(maxObjVersion,new ReplicaUpdatePolicy.OpenFileCallback() {

                    @Override
                    public void fileOpened(boolean locallyReadable, boolean locallyWritable, Exception error) {
                        callback.fileOpenComplete(error);
                    }
                });
            } else {
                callback.fileOpenComplete(new IOException("file was already opened!"));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.fileOpenComplete(ex);
        }
    }

    private void processReplicatedWrite(StageRequest method) {
        final ReplicatedOperationCallback callback = (ReplicatedOperationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final Long objNo = (Long) method.getArgs()[1];
            final Long objVersion = (Long) method.getArgs()[2];
            final ObjectData objData  = (ObjectData) method.getArgs()[3];
            final XLocations loc = (XLocations) method.getArgs()[4];


            final String fileId = credentials.getXcap().getFile_id();


            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                //"open" file
                state = new ReplicatedFileState(fileId,loc, master.getConfig().getUUID(), fstage, osdClient);
                files.put(fileId,state);
            }

            ReplicatedOperation rw = new ReplicatedOperation(state.getPolicy(), callback);
            rw.write(credentials, objNo, objVersion, objData);

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.writeCompleted(ex);
        }
    }

    private void processReplicatedTruncate(StageRequest method) {
        final ReplicatedOperationCallback callback = (ReplicatedOperationCallback) method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final Long newFileSize = (Long) method.getArgs()[1];
            final Long newObjVersion = (Long) method.getArgs()[2];
            final XLocations loc = (XLocations) method.getArgs()[3];


            final String fileId = credentials.getXcap().getFile_id();


            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                //"open" file
                state = new ReplicatedFileState(fileId,loc, master.getConfig().getUUID(), fstage, osdClient);
                files.put(fileId,state);
            }

            ReplicatedOperation rw = new ReplicatedOperation(state.getPolicy(), callback);
            rw.truncate(credentials, newFileSize, newObjVersion);

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.writeCompleted(ex);
        }
    }

    private void processPrepareOp(StageRequest method) {
        final PrepareOperationCallback callback = (PrepareOperationCallback)method.getCallback();
        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final Long objNo = (Long) method.getArgs()[1];
            final Long objVersion = (Long) method.getArgs()[2];
            final Operation op = (Operation) method.getArgs()[3];


            final String fileId = credentials.getXcap().getFile_id();


            ReplicatedFileState state = files.get(fileId);
            if (state == null) {
                callback.prepareOperationComplete(false, null, -1l, new IllegalArgumentException("file is not open!"));
            }
            state.getPolicy().prepareOperation(objNo, objVersion, op, callback);

        } catch (Exception ex) {
            ex.printStackTrace();
            callback.prepareOperationComplete(false, null, -1l,ex);
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
                fStatus.put("peers (OSDs)",fState.getPolicy().getRemoteOSDs().toString());
                fStatus.put("pending updates", "n/a");
                fStatus.put("cellId", cellId.toString());
                String primary = "unknown";
                if (fleaseState.get(cellId) != null) {
                    primary = fleaseState.get(cellId).getLeaseHolder().toString();
                    if (primary.equals(master.getConfig().getUUID().toString())) {
                        primary = "primary";
                    } else {
                        primary = "backup ( primary is "+primary+")";
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
