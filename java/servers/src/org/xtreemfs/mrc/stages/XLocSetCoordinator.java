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
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.operations.MRCOperation;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_replica_addRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.readRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_xloc_set_invalidateResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class XLocSetCoordinator extends LifeCycleThread {
    private enum RequestType {
        ADD_REPLICAS, REMOVE_REPLICAS, REPLACE_REPLICA
    };
    
    private static String                XLOCSET_CHANGE_ATTR_KEY = "XLocSetChange";

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
            case REPLACE_REPLICA:
                processReplaceReplica(m);
            default:
                throw new Exception("unknown stage operation");
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            handleError(m, e);
        }
    }

    private final static class RequestMethod {

        RequestType  type;
        MRCOperation op;
        MRCRequest   rq;
        String       fileId;

        // XLocSetCoordinatorCallback callback;

        // public RequestMethod(RequestType type, MRCOperation op, MRCRequest rq, XLocSetCoordinatorCallback
        // callback) {
        // this.type = type;
        // this.op = op;
        // this.rq = rq;
        // this.callback = callback;
        // }

        public RequestMethod(RequestType type, String fileId, MRCOperation op, MRCRequest rq) {
            this.type = type;
            this.op = op;
            this.rq = rq;
            this.fileId = fileId;
        }

        public RequestType getRequestType() {
            return type;
        }

        public MRCRequest getRequest() {
            return rq;
        }

        // public XLocSetCoordinatorCallback getCallback() {
        // return callback;
        // }

        public MRCOperation getOperation() {
            return op;
        }

        public String getFileId() {
            return fileId;
        }
    }

    public interface XLocSetCoordinatorCallback {
        public void finished();
        public void error();
    }

    public void addReplicas(String fileId, MRCOperation op, MRCRequest rq) {
        q.add(new RequestMethod(RequestType.ADD_REPLICAS, fileId, op, rq));
    }

    // TODO (jdillmann): Allow multiple replicas to be added / rename
    private void processAddReplicas(RequestMethod m) throws Throwable {
        final MRCRequest rq = m.getRequest();
        
        final xtreemfs_replica_addRequest rqArgs = (xtreemfs_replica_addRequest) rq.getRequestArgs();

        final VolumeManager vMan = master.getVolumeManager();
        
        // parse volume and file ID from global file ID
        final String fileId = m.getFileId();
        final GlobalFileIdResolver idRes = new GlobalFileIdResolver(fileId);
        final StorageManager sMan = vMan.getStorageManager(idRes.getVolumeId());

        // retrieve the file metadata
        final FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
        if (file == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + fileId + "' does not exist");
        


        Replica newRepl = rqArgs.getNewReplica();
        org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy sp = newRepl.getStripingPolicy();

        StripingPolicy sPol = sMan.createStripingPolicy(sp.getType().toString(), sp.getStripeSize(), sp.getWidth());

        // check whether the new replica relies on a set of OSDs which
        // hasn't been used yet
        XLocList xLocList = file.getXLocList();
        assert (xLocList != null);

        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(xLocList.getReplUpdatePolicy()))
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                    "missing replica update policy - needs to be specified before adding replicas");

        if (!MRCHelper.isResolvable(newRepl.getOsdUuidsList()))
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "replica contains unresolvable OSD UUIDs in '"
                    + newRepl.getOsdUuidsList() + "'");

        if (xLocList.getReplica(0).getStripingPolicy().getStripeSize() != newRepl.getStripingPolicy().getStripeSize())
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid stripe size; must be "
                    + xLocList.getReplica(0).getStripingPolicy().getStripeSize());

        if (!MRCHelper.isAddable(xLocList, newRepl.getOsdUuidsList()))
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "at least one OSD already used in current X-Locations list '"
                            + Converter.xLocListToString(xLocList) + "'");


        // create a new XLocList
        XLoc replica = sMan.createXLoc(sPol, newRepl.getOsdUuidsList().toArray(new String[newRepl.getOsdUuidsCount()]),
                newRepl.getReplicationFlags());

        XLoc[] extendedRepl = new XLoc[xLocList.getReplicaCount() + 1];
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            XLoc repl = xLocList.getReplica(i);
            extendedRepl[i] = repl;
        }
        extendedRepl[extendedRepl.length - 1] = replica;

        XLocList extXLocList = sMan.createXLocList(extendedRepl, xLocList.getReplUpdatePolicy(),
                xLocList.getVersion() + 1);
        XLocSet extXLocSet = Converter.xLocListToXLocSet(extXLocList).build();

        // Don't use callbacks - this will ensure no other ReplicaSetChange could happen inbetween
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);

        // TODO (jdillmann): Catch UnknownUUIDException, JSONException from Converter.xLocListToJSON
        sMan.setXAttr(file.getId(), StorageManager.SYSTEM_UID, 
                StorageManager.SYS_ATTR_KEY_PREFIX + XLOCSET_CHANGE_ATTR_KEY, 
                Converter.xLocListToJSON(extXLocList, master.getOSDStatusManager()).getBytes(),
                update);
        update.execute();

        

        invalidateReplicas(fileId, file, xLocList);



        // Check replication flags, if it's a full replica. The replication does not need to be triggered for partial replicas
        if (!(extXLocSet.getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY))
                || ((replica.getReplicationFlags() & REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber()) == 0)) {

            // Build the FileCredentials
            Capability cap = buildCapability(fileId, file);
            FileCredentials.Builder fileCredentialsBuilder = FileCredentials.newBuilder();
            fileCredentialsBuilder.setXlocs(extXLocSet);
            fileCredentialsBuilder.setXcap(cap.getXCap());

            // Build the readRequest
            readRequest.Builder readRequestBuilder = readRequest.newBuilder();
            readRequestBuilder.setFileCredentials(fileCredentialsBuilder);
            readRequestBuilder.setFileId(fileId);

            // Read one byte from the replica to trigger the replication.
            readRequestBuilder.setObjectNumber(0);
            readRequestBuilder.setObjectVersion(0);
            readRequestBuilder.setOffset(0);
            readRequestBuilder.setLength(1);

            // Get the UUID and the address of the new replica
            String headOsd = newRepl.getOsdUuids(0);
            InetSocketAddress server = new ServiceUUID(headOsd).getAddress();

            // Ping the new replica
            OSDServiceClient client = master.getOSDClient();

            // TODO (jdillmann): retry on timeouts?
            RPCResponse<ObjectData> rpcResponse;
            rpcResponse = client.read(server, RPCAuthentication.authNone, RPCAuthentication.userService,
                    readRequestBuilder.build());
            rpcResponse.freeBuffers();

            // TODO (jdillmann): tell the primary to give up its role to be able to continue with other new
            // replicas
        }





        update = sMan.createAtomicDBUpdate(master, rq);

        // update the X-Locations list
        file.setXLocList(extXLocList);
        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
        
        // remove the XLocSetChange attribute
        sMan.setXAttr(file.getId(), StorageManager.SYSTEM_UID, 
                StorageManager.SYS_ATTR_KEY_PREFIX + XLOCSET_CHANGE_ATTR_KEY, 
                null,update);
        
        // set the response
        rq.setResponse(emptyResponse.getDefaultInstance());

        update.execute();
    }

    public void removeReplicas(String fileId, MRCOperation op, MRCRequest rq) {
        q.add(new RequestMethod(RequestType.REMOVE_REPLICAS, fileId, op, rq));
    }

    private void processRemoveReplicas(RequestMethod m) throws Throwable {
        final MRCRequest rq = m.getRequest();

    }

    public void replaceReplica(String fileId, MRCOperation op, MRCRequest rq) {
        q.add(new RequestMethod(RequestType.REPLACE_REPLICA, fileId, op, rq));
    }

    private void processReplaceReplica(RequestMethod m) throws Throwable {
        throw new NotImplementedException();
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

        // TODO (jdillmann): check correct MRC adress
        String clientIdentity;
        try {
            clientIdentity = master.getConfig().getAddress() != null ? master.getConfig().getAddress().toString()                : InetAddress.getLocalHost().getHostAddress();
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
     * @param file
     * @param xLocList
     * @throws Throwable
     */
    private void invalidateReplicas(String fileId, FileMetadata file, XLocList xLocList)
            throws Throwable {
        // convert the XLocList to an XLocSet 
        XLocSet xLocSet = Converter.xLocListToXLocSet(xLocList).build();
        
        Capability cap = buildCapability(fileId, file);
        FileCredentials creds = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLocSet).build();
        
        // Invalidate the replicas (the majority should be enough)
        RPCResponse<xtreemfs_xloc_set_invalidateResponse> rpcResponse;
        xtreemfs_xloc_set_invalidateResponse response = null;
        OSDServiceClient client = master.getOSDClient();
        
        boolean primaryResponded = false;
        int responseCount = 0;
        // TODO (jdillmann): Use RPCResponseAvailableListener @see CoordinatedReplicaUpdatePolicy.executeReset
        for (int i = 0; i < xLocSet.getReplicasCount(); i++) {
            String headOsd = Helper.getOSDUUIDFromXlocSet(xLocSet, i, 0);
            // TODO (jdillmann): Cache the ServiceUUID because we will need it again for INSTALL
            InetSocketAddress server = new ServiceUUID(headOsd).getAddress();

            try {
                rpcResponse = client.xtreemfs_xloc_set_invalidate(server, RPCAuthentication.authNone,
                        RPCAuthentication.userService, creds, fileId);
                response = rpcResponse.get();
                responseCount = responseCount + 1;

            } catch (InterruptedException e) {
                if (quit) {
                    throw e;
                }
            } catch (IOException e) {
                // TODO (jdillmann): Do something with the error
                if (Logging.isDebug())
                    Logging.logError(Logging.LEVEL_DEBUG, this, e);
                continue;
            }
            

            if (response.getIsPrimary()) {
                primaryResponded = true;
            }
        }
        

        // if the primary didn't respond we have to wait until the lease timed out
        // if every replica replied and none has been primary we don't have to wait
        if (!primaryResponded && !(responseCount == xLocSet.getReplicasCount())) {
            // FIXME (jdillmann): Howto get the lease timeout value from the OSD config?
            long leaseToMS = 15 * 1000;
            
            // TODO (jdillmann): Care about the InterruptedException to ensure we will sleep for the required time
            sleep(leaseToMS);
        }

    }

    // TODO (jdillmann): Share error reporting between ProcessingStage.parseAndExecure and this
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
