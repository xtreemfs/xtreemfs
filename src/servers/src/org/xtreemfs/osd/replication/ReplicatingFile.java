/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.osd.replication;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.InternalReadLocalResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.operations.EventWriteObject;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.replication.TransferStrategy.NextRequest;
import org.xtreemfs.osd.replication.TransferStrategy.TransferStrategyException;
import org.xtreemfs.osd.stages.ReplicationStage.FetchObjectCallback;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;

/**
 * Encapsulates important infos about a file <br>
 * <br>
 * 01.04.2009
 */
public class ReplicatingFile {
    /*
     * inner class
     */
    private class ReplicatingObject {
        public final long          objectNo;

        // create a list only if object is really requested
        private List<StageRequest> waitingRequests = null;

        /**
         * used to save data with an invalid checksum, because it could be the best result we get <br>
         * null in all cases except an object with an invalid checksum is fetched
         */
        ObjectData                 data            = null;

        public ReplicatingObject(long objectNo) {
            this.objectNo = objectNo;
        }

        public List<StageRequest> getWaitingRequests() {
            if (waitingRequests == null)
                this.waitingRequests = new LinkedList<StageRequest>();
            return waitingRequests;
        }

        public boolean hasWaitingRequests() {
            return (waitingRequests == null) ? false : !getWaitingRequests().isEmpty();
        }
        
        public boolean hasDataFromEarlierResponses() {
            return (data != null);
        }

        /**
         * is used for (complete) replicating an object which was previously chosen to be replicated
         * 
         * @param objectNo
         * @throws TransferStrategyException
         */
        public void replicateObject() throws TransferStrategyException {
            strategy.selectNextOSD(objectNo);
            NextRequest next = strategy.getNext();

            if (next != null) { // OSD found for fetching object
                // FIXME: only for debugging
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "fetch object %s:%d from OSD %s", fileID, next.objectNo, next.osd);

                try {
                    sendFetchObjectRequest(next.objectNo, next.osd);
                } catch (UnknownUUIDException e) {
                    // try other OSD
                    replicateObject();
                }
            } else { // should not happen
                sendError(new Exception("internal server error"));
                Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this,
                        "transfer strategy returns neither a value nor an exception");

                objectReplicationCompleted(objectNo);
            }
        }

        public boolean objectFetched(ObjectData data) throws TransferStrategyException {
            if (!data.getInvalid_checksum_on_osd()) {
                // correct checksum
                if (hasWaitingRequests())
                    sendResponses(data.getData(), ObjectStatus.EXISTS);

                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                            "OBJECT FETCHED %s:%d", fileID, objectNo);

                if (!isStopped()) {
                    // write data to disk
                    OSDOperation writeObjectEvent = master.getInternalEvent(EventWriteObject.class);
                    // NOTE: "original" buffer is used
                    writeObjectEvent.startInternalEvent(new Object[] { fileID, objectNo, data.getData(),
                            xLoc, cow });
                }

//                objectCompleted(objectNo);
                return true;
            } else {
                // FIXME: only for debugging
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "fetched object %s:%d has invalid checksum", fileID, objectNo);

                // save data, in case other replicas are less useful
                this.data = data;

                // if (!ReplicatingFile.cancelled) {
                // try next replica
                strategy.addObject(objectNo, hasWaitingRequests());
                replicateObject();
                // } else {
                // sendError(ReplicatingFile, objectNo, new OSDException(ErrorCodes.IO_ERROR,
                // "Object does not exist locally and replication was cancelled.", ""));
                //                
                // objectCompleted(ReplicatingFile, objectNo);
                // }
                return false;
            }
        }

        public boolean objectNotFetched() throws TransferStrategyException {
            // check if it is a hole
            if (strategy.isHole(objectNo)) { // TODO: change this, if using different striping policies
                if(hasDataFromEarlierResponses() && hasWaitingRequests()) {
                    // no hole, but an object where only a replica with a wrong checksum could be found
                    sendResponses(data.getData(), ObjectStatus.EXISTS);

                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                                "OBJECT FETCHED %s:%d, but with wrong checksum", fileID, objectNo);
                    
//                  objectCompleted(objectNo);
                    return true;
                } else {
                    // => hole or error; we assume it is a hole
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                                "OBJECT %s:%d COULD NOT BE FETCHED; MUST BE A HOLE.", fileID, objectNo);
    
                    if (hasWaitingRequests())
                        sendResponses(null, ObjectStatus.PADDING_OBJECT);
    
                    // TODO: remember on this OSD that this object is a hole
    
//                  objectCompleted(objectNo);
                    return true;
                }
            } else {
                // if (!ReplicatingFile.cancelled) {
                // FIXME: only for debugging
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "object %s:%d could not be fetched from OSD => try next OSD", fileID, objectNo);

                // try next replica
                strategy.addObject(objectNo, hasWaitingRequests());
                replicateObject();
                // } else {
                // sendError(ReplicatingFile, objectNo, new OSDException(ErrorCodes.IO_ERROR,
                // "Object does not exist locally and replication was cancelled.", ""));
                //                
                // objectCompleted(ReplicatingFile, objectNo);
                // }
                return false;
            }
        }

        /**
         * sends the responses to all belonging clients <br>
         * NOTE: data-buffer will not be modified
         */
        private void sendResponses(ReusableBuffer data, ObjectStatus status) {
            List<StageRequest> reqs = getWaitingRequests();
            StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
            // responses
            for (StageRequest rq : reqs) {
                ObjectInformation objectInfo;
                // create returning ObjectInformation
                if (status == ObjectStatus.EXISTS) {
                    objectInfo = new ObjectInformation(status, data.createViewBuffer(), sp
                            .getStripeSizeForObject(objectNo));
                } else {
                    objectInfo = new ObjectInformation(status, null, sp.getStripeSizeForObject(objectNo));
                }
                objectInfo.setGlobalLastObjectNo(lastObject);

                final FetchObjectCallback callback = (FetchObjectCallback) rq.getCallback();
                callback.fetchComplete(objectInfo, null);
            }
        }

        /**
         * sends an error to all belonging clients
         */
        public void sendError(Exception error) {
            List<StageRequest> reqs = getWaitingRequests();
            // responses
            for (StageRequest rq : reqs) {
                final FetchObjectCallback callback = (FetchObjectCallback) rq.getCallback();
                callback.fetchComplete(null, error);
            }
        }
    }
    
    /*
     * outer class
     */
    private final OSDRequestDispatcher       master;

    public final String                      fileID;
    final TransferStrategy                   strategy;
    long                                     lastObject;
    private XLocations                       xLoc;
    private Capability                       cap;
    private CowPolicy                        cow;

    /**
     * if the file is removed while it will be replicated
     */
    private boolean                          cancelled;

    /**
     * marking THIS replica as a full replica (enables background replication)
     */
    private boolean                          fullReplica;

    /**
     * manages the OSD availability
     */
    private final ServiceAvailability        osdAvailability;

    /**
     * key: objectNo
     */
    private HashMap<Long, ReplicatingObject> objectsInProgress;

    /**
     * contains all requests which are waiting for an object, where the replication of the object has been not
     * started so far <br>
     * key: objectNo
     */
    private HashMap<Long, ReplicatingObject> waitingRequests;

    public ReplicatingFile(String fileID, XLocations xLoc, Capability cap, CowPolicy cow,
            OSDRequestDispatcher master) {
        this.master = master;
        this.osdAvailability = master.getServiceAvailability();

        this.fileID = fileID;
        this.xLoc = xLoc;
        this.cap = cap;
        this.cow = cow;
        this.cancelled = false;
        this.objectsInProgress = new HashMap<Long, ReplicatingObject>();
        this.waitingRequests = new HashMap<Long, ReplicatingObject>();

        StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
        this.lastObject = sp.getObjectNoForOffset(xLoc.getXLocSet().getRead_only_file_size() - 1);

        // create a new strategy
//        if (xLoc.getLocalReplica().isStrategy(Constants.REPL_FLAG_STRATEGY_SIMPLE))
//            strategy = new SimpleStrategy(fileID, xLoc, xLoc.getXLocSet().getRead_only_file_size(),
//                    osdAvailability);
//        else if (xLoc.getLocalReplica().isStrategy(Constants.REPL_FLAG_STRATEGY_RANDOM))
            strategy = new RandomStrategy(fileID, xLoc, osdAvailability);
//        else
//            throw new IllegalArgumentException("Set Replication Strategy not known.");
    }

    /**
     * enqueues the request and corresponding object for replication
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public boolean addObjectForReplicating(Long objectNo, StageRequest rq) {
        assert(rq != null);
        
        ReplicatingObject info = objectsInProgress.get(objectNo);
        if (info == null) { // object is currently not replicating
            info = new ReplicatingObject(objectNo);
            info.getWaitingRequests().add(rq);
            waitingRequests.put(objectNo, info);
            // add to strategy
            strategy.addObject(objectNo, true);
        } else {
            info.getWaitingRequests().add(rq);
        }
        return true;
    }

    /**
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    private boolean processObject(Long objectNo) {
        if(!isObjectInProgress(objectNo)) {
            ReplicatingObject object = waitingRequests.remove(objectNo);
            if (object != null) // at least one request is waiting
                objectsInProgress.put(objectNo, object);
            else
                objectsInProgress.put(objectNo, new ReplicatingObject(objectNo));
            return true;
        }
        return false;
    }

    /**
     * @see java.util.HashMap#containsKey(java.lang.Object)
     */
    public boolean isObjectInProgress(Long objectNo) {
        return objectsInProgress.containsKey(objectNo);
    }

    /**
     * @see java.util.HashMap#get(java.lang.Object)
     */
    public ReplicatingObject getObjectInProgress(Long objectNo) {
        return objectsInProgress.get(objectNo);
    }

    /**
     * replication of objects is in progress
     * 
     * @see java.util.HashMap#isEmpty()
     */
    public boolean isReplicating() {
        return !objectsInProgress.isEmpty();
    }

    public boolean isStopped() {
        return cancelled;
    }

    /**
     * @see java.util.HashMap#size()
     */
    public int getNumberOfObjectsInProgress() {
        return objectsInProgress.size();
    }

    /**
     * updates the capability and XLocations-list, if they are newer
     */
    public void update(Capability cap, XLocations xLoc, CowPolicy cow) {
        this.cow = cow;
        // if newer
        if (cap.getExpires() > this.cap.getExpires() || cap.getEpochNo() > this.cap.getEpochNo()) {
            this.cap = cap;
        }
        if (xLoc.getXLocSet().getVersion() > this.xLoc.getXLocSet().getVersion()) {
            this.xLoc = xLoc;
            this.strategy.updateXLoc(xLoc);
        }
    }

    /**
     * chooses an object and matching OSD for replicating it
     * @throws TransferStrategyException
     */
    public void replicate() throws TransferStrategyException {
        strategy.selectNext();
        NextRequest next = strategy.getNext();

        if (next != null) { // there is something to fetch
            // object replication is in progress
            processObject(next.objectNo);
            
            // FIXME: only for debugging
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "fetch object %s:%d from OSD %s", fileID, next.objectNo, next.osd);

            try {
                sendFetchObjectRequest(next.objectNo, next.osd);
            } catch (UnknownUUIDException e) {
                // try other OSD
                objectsInProgress.get(next.objectNo).replicateObject();
            }
        } else { // file must be fully replicated (background replication)
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "stop replicating file %s", fileID);
        }
    }

    /**
     * 
     * @param objectNo
     * @param data
     * @return true, if object is completed; false otherwise
     */
    public void objectFetched(long objectNo, ObjectData data) {
        ReplicatingObject object = objectsInProgress.get(objectNo);
        assert (object != null);

        try {
            boolean objectCompleted = object.objectFetched(data);
            if(objectCompleted)
                objectReplicationCompleted(objectNo);
        } catch (TransferStrategyException e) {
            // TODO: differ between ErrorCodes
            object.sendError(new OSDException(ErrorCodes.IO_ERROR, e.getMessage(), e.getStackTrace().toString()));
            objectReplicationCompleted(objectNo);
        }
    }

    /**
     * Checks if it is a hole. Otherwise it tries to use another OSD for fetching.
     * 
     * @param objectNo
     * @return true, if object is completed; false otherwise
     */
    public void objectNotFetched(long objectNo) {
        ReplicatingObject object = objectsInProgress.get(objectNo);
        assert (object != null);

        try {
            boolean objectCompleted = object.objectNotFetched();
            if (objectCompleted)
                objectReplicationCompleted(objectNo);
        } catch (TransferStrategyException e) {
            // TODO: differ between ErrorCodes
            object.sendError(new OSDException(ErrorCodes.IO_ERROR, e.getMessage(), e.getStackTrace().toString()));
            objectReplicationCompleted(objectNo);
        }
    }

    public void objectReplicationCompleted(long objectNo) {
        // delete object in maps/lists
        strategy.removeObject(objectNo);
        ReplicatingObject replicatingObject = objectsInProgress.remove(objectNo);
        // free old data
        if (replicatingObject.hasDataFromEarlierResponses())
            BufferPool.free(replicatingObject.data.getData());
    }

    public void stopReplicatingFile() {
        cancelled = true;
    }

    /**
     * Sends a RPC for reading the object on another OSD.
     * 
     * @throws UnknownUUIDException
     */
    private void sendFetchObjectRequest(final long objectNo, final ServiceUUID osd)
            throws UnknownUUIDException {
        OSDClient client = master.getOSDClient();
        // TODO: change this, if using different striping policies
        RPCResponse<InternalReadLocalResponse> response = client.internal_read_local(osd.getAddress(),
                fileID, new FileCredentials(xLoc.getXLocSet(), cap.getXCap()), objectNo, 0, 0, xLoc
                        .getLocalReplica().getStripingPolicy().getStripeSizeForObject(objectNo));

        response.registerListener(new RPCResponseAvailableListener<InternalReadLocalResponse>() {
            @Override
            public void responseAvailable(RPCResponse<InternalReadLocalResponse> r) {
                try {
                    ObjectData data = r.get().getData();
                    master.getReplicationStage().internalObjectFetched(fileID, objectNo, data);
                } catch (ONCRPCException e) {
                    // TODO
                    osdAvailability.setServiceWasNotAvailable(osd);
                    master.getReplicationStage().internalObjectFetched(fileID, objectNo, null);
                    e.printStackTrace();
                } catch (IOException e) {
                    osdAvailability.setServiceWasNotAvailable(osd);
                    master.getReplicationStage().internalObjectFetched(fileID, objectNo, null);
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // ignore
                } finally {
                    r.freeBuffers();
                }
            }
        });
    }
    
    /**
     * sends an error to all belonging clients (for all objects of the file)
     */
    public void sendError(Exception error) {
        for (ReplicatingObject object : waitingRequests.values()) {
            List<StageRequest> reqs = object.getWaitingRequests();
            // responses
            for (StageRequest rq : reqs) {
                final FetchObjectCallback callback = (FetchObjectCallback) rq.getCallback();
                callback.fetchComplete(null, error);
            }
        }
    }

}
