/*
 * Copyright (c) 2015 by Jan Fajerski,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.ec;

import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.*;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.RedundancyStage;
import org.xtreemfs.osd.stages.FleaseMasterEpochStage;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author Jan Fajerski
 */
public class ECStage extends RedundancyStage implements FleaseMessageSenderInterface {

    public static final int STAGEOP_EC_WRITE                  = 1;
    public static final int STAGEOP_CLOSE                     = 2;
    public static final int STAGEOP_PROCESS_FLEASE_MSG        = 3;
    public static final int STAGEOP_PREPAREOP                 = 5;
    public static final int STAGEOP_TRUNCATE                  = 6;
    public static final int STAGEOP_GETSTATUS                 = 7;

    public static final int STAGEOP_INTERNAL_AUTHSTATE        = 10;
    public static final int STAGEOP_INTERNAL_OBJFETCHED       = 11;

    public static final int STAGEOP_LEASE_STATE_CHANGED       = 13;
    public static final int STAGEOP_INTERNAL_STATEAVAIL       = 14;
    public static final int STAGEOP_INTERNAL_DELETE_COMPLETE  = 15;
    public static final int STAGEOP_FORCE_RESET               = 16;
    public static final int STAGEOP_INTERNAL_MAXOBJ_AVAIL     = 17;
    public static final int STAGEOP_INTERNAL_BACKUP_AUTHSTATE = 18;

    public static final int STAGEOP_SETVIEW                   = 21;
    public static final int STAGEOP_INVALIDATEVIEW            = 22;
    public static final int STAGEOP_FETCHINVALIDATED          = 23;

    public static enum Operation {
        READ,
        WRITE,
        TRUNCATE,
        INTERNAL_UPDATE,
        INTERNAL_TRUNCATE
    };

    private final RPCNIOSocketClient               client;

    private final OSDServiceClient                 osdClient;

    private final Map<String, StripedFileState>    files;

    private final Map<ASCIIString, String>         cellToFileId;

    private final OSDRequestDispatcher             master;

    private final FleaseStage                      fstage;

    private final RPCNIOSocketClient               fleaseClient;

    private final OSDServiceClient                 fleaseOsdClient;

    private final ASCIIString                      localID;

    private int                                    numObjsInFlight;

    private static final int                       MAX_OBJS_IN_FLIGHT         = 10;

    private static final int                       MAX_PENDING_PER_FILE       = 10;

    private static final int                       MAX_EXTERNAL_REQUESTS_IN_Q = 250;

    private final FleaseMasterEpochStage masterEpochStage;

    private final AtomicInteger                    externalRequestsInQueue;

    public ECStage(OSDRequestDispatcher master, SSLOptions sslOpts, int maxRequestsQueueLength)
            throws IOException {
        super("ECSt", maxRequestsQueueLength);
        this.master = master;
        client = new RPCNIOSocketClient(sslOpts, 15000, 60000 * 5, "ECStage");
        fleaseClient = new RPCNIOSocketClient(sslOpts, 15000, 60000 * 5, "ECStage (flease)");
        osdClient = new OSDServiceClient(client, null);
        fleaseOsdClient = new OSDServiceClient(fleaseClient, null);
        files = new HashMap<String, StripedFileState>();
        cellToFileId = new HashMap<ASCIIString, String>();
        numObjsInFlight = 0;
        externalRequestsInQueue = new AtomicInteger(0);

        localID = new ASCIIString(master.getConfig().getUUID().toString());

        masterEpochStage = new FleaseMasterEpochStage(master.getStorageStage().getStorageLayout(),
                maxRequestsQueueLength);

        FleaseConfig fcfg = new FleaseConfig(master.getConfig().getFleaseLeaseToMS(), master.getConfig()
                .getFleaseDmaxMS(), master.getConfig().getFleaseMsgToMS(), null, localID.toString(), master.getConfig()
                .getFleaseRetries());

        fstage = new FleaseStage(fcfg, master.getConfig().getObjDir() + "/", this, false,
                new FleaseViewChangeListenerInterface() {

                    @Override
                    public void viewIdChangeEvent(ASCIIString cellId, int viewId) {
                    }
                }, new FleaseStatusListener() {

                    @Override
                    public void statusChanged(ASCIIString cellId, Flease lease) {
                    }

                    @Override
                    public void leaseFailed(ASCIIString cellID, FleaseException error) {
                    }
                }, masterEpochStage);
        fstage.setLifeCycleListener(master);
    }

    @Override
    public void start() {
        masterEpochStage.start();
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
        masterEpochStage.shutdown();
        super.shutdown();
    }

    @Override
    public void waitForStartup() throws Exception {
        masterEpochStage.waitForStartup();
        client.waitForStartup();
        fleaseClient.waitForStartup();
        fstage.waitForStartup();
        super.waitForStartup();
    }

    @Override
    public void waitForShutdown() throws Exception {
        client.waitForShutdown();
        fleaseClient.waitForShutdown();
        fstage.waitForShutdown();
        masterEpochStage.waitForShutdown();
        super.waitForShutdown();
    }

    protected void enqueueExternalOperation(int stageOp, Object[] arguments, OSDRequest request,
            ReusableBuffer createdViewBuffer, Object callback) {
        if (externalRequestsInQueue.get() >= MAX_EXTERNAL_REQUESTS_IN_Q) {
            Logging.logMessage(Logging.LEVEL_WARN, this,
                    "EC stage is overloaded, request %d for %s dropped", request.getRequestId(),
                    request.getFileId());
            request.sendInternalServerError(new IllegalStateException(
                    "EC stage is overloaded, request dropped"));

            // Make sure that the data buffer is returned to the pool if
            // necessary, as some operations create view buffers on the
            // data. Otherwise, a 'finalized but not freed before' warning
            // may occur.
            if (createdViewBuffer != null) {
                assert (createdViewBuffer.getRefCount() >= 2);
                BufferPool.free(createdViewBuffer);
            }

        } else {
            externalRequestsInQueue.incrementAndGet();
            this.enqueueOperation(stageOp, arguments, request, createdViewBuffer, callback);
        }
    }

    public void prepareOperation(FileCredentials credentials, XLocations xloc, long objNo, long objVersion,
            Operation op, ECCallback callback, OSDRequest request) {
        this.enqueueExternalOperation(STAGEOP_PREPAREOP, new Object[] { credentials, xloc, objNo, objVersion, op },
                request, null, callback);
    }

    private void processPrepareOp(StageRequest method) {
        final ECCallback callback = (ECCallback) method.getCallback();

        try {
            final FileCredentials credentials = (FileCredentials) method.getArgs()[0];
            final String fileId = credentials.getXcap().getFileId();
            final XLocations loc = (XLocations) method.getArgs()[1];
            final Long objVersion = (Long) method.getArgs()[3];
            final Operation op = (Operation) method.getArgs()[4];

            StripedFileState state = getState(credentials, loc, false, false);
            /*
             * check if request must fail
             * check what kind of operation should be prepared
             * collect ops in state if we are master or intend to become master but limit amount of pending ops
             * determine new version for update or determine version to read
             *
             */

            callback.success(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failed(ErrorUtils.getInternalServerError(ex));
        }
    }

    public void receiveFleaseMessage(ReusableBuffer message, InetSocketAddress sender) {
        try {
            FleaseMessage msg = new FleaseMessage(message);
            BufferPool.free(message);
            msg.setSender(sender);
            fstage.receiveMessage(msg);
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void getStatus(StatusCallback callback) {
        this.enqueueOperation(STAGEOP_GETSTATUS, new Object[] {}, null, callback);
    }

    public static interface StatusCallback {
        public void statusComplete(Map<String, Map<String, String>> status);
    }

    public static interface ECCallback {
        public void success(long newObjectVersion);
        public void redirect(String redirectTo);
        public void failed(ErrorResponse ex);
    }

    @Override
    public void sendMessage(FleaseMessage message, InetSocketAddress recipient) {
        ReusableBuffer data = BufferPool.allocate(message.getSize());
        message.serialize(data);
        data.flip();
        try {
            RPCResponse r = fleaseOsdClient.xtreemfs_rwr_flease_msg(recipient, RPCAuthentication.authNone,
                    RPCAuthentication.userService, master.getHostName(), master.getConfig().getPort(), data);
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
            case STAGEOP_EC_WRITE: {
                externalRequestsInQueue.decrementAndGet();
                processECWrite(method);
                break;
            }
        case STAGEOP_TRUNCATE: {
            externalRequestsInQueue.decrementAndGet();
            break;
        }
        case STAGEOP_CLOSE:  break;
        case STAGEOP_PROCESS_FLEASE_MSG: processFleaseMessage(method); break;
        case STAGEOP_PREPAREOP: {
            externalRequestsInQueue.decrementAndGet();
            processPrepareOp(method);
            break;
        }
        case STAGEOP_INTERNAL_AUTHSTATE:  break;
        case STAGEOP_LEASE_STATE_CHANGED:  break;
        case STAGEOP_INTERNAL_OBJFETCHED:  break;
        case STAGEOP_INTERNAL_STATEAVAIL:  break;
        case STAGEOP_INTERNAL_DELETE_COMPLETE:  break;
        case STAGEOP_INTERNAL_MAXOBJ_AVAIL:  break;
        case STAGEOP_INTERNAL_BACKUP_AUTHSTATE:  break;
        case STAGEOP_FORCE_RESET:  break;
        case STAGEOP_GETSTATUS:  break;
        case STAGEOP_SETVIEW:  break;
        case STAGEOP_INVALIDATEVIEW:  break;
        case STAGEOP_FETCHINVALIDATED:  break;
        default : throw new IllegalArgumentException("no such stageop");
        }
    }

    private void processECWrite(StageRequest method) {
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
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private StripedFileState getState(FileCredentials credentials, XLocations loc, boolean forceReset,
                                         boolean invalidated) throws IOException {

        final String fileId = credentials.getXcap().getFileId();

        StripedFileState state = files.get(fileId);
        if (state == null) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.ec, this, "open file: " + fileId);
            // "open" file
            state = new StripedFileState(fileId);
            files.put(fileId, state);
            state.setCredentials(credentials);
            //state.setInvalidated(invalidated);
            //cellToFileId.put(state.getPolicy().getCellId(), fileId);

            //master.getStorageStage().internalGetMaxObjectNo(fileId, loc.getLocalReplica().getStripingPolicy(),
                    //new InternalGetMaxObjectNoCallback() {

                        //@Override
                        //public void maxObjectNoCompleted(long maxObjNo, long fileSize, long truncateEpoch,
                                                         //ErrorResponse error) {
                            //eventMaxObjAvail(fileId, maxObjNo, fileSize, truncateEpoch, error);
                        //}
                    //});
        }
        return state;
    }
}
