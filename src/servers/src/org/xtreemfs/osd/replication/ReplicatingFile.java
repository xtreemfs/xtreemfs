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
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.InternalReadLocalResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.ObjectList;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.operations.EventWriteObject;
import org.xtreemfs.osd.operations.EventInsertPaddingObject;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.replication.transferStrategies.RandomStrategy;
import org.xtreemfs.osd.replication.transferStrategies.RarestFirstStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialPrefetchingStrategy;
import org.xtreemfs.osd.replication.transferStrategies.SequentialStrategy;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.NextRequest;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;
import org.xtreemfs.osd.stages.ReplicationStage.FetchObjectCallback;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;

/**
 * attends to the replication of all objects of this file <br>
 * 01.04.2009
 */
class ReplicatingFile {
    /*
     * inner class
     */
    private class ReplicatingObject {
        public final long          objectNo;
        
        // create a list only if object is really requested
        private List<StageRequest> waitingRequests = null;
        
        /**
         * used to save data with an invalid checksum, because it could be the
         * best result we get <br>
         * null in all cases except an object with an invalid checksum is
         * fetched
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
         * is used for (complete) replicating an object which was previously
         * chosen to be replicated
         * 
         * @param objectNo
         * @throws TransferStrategyException
         */
        public void replicateObject() throws TransferStrategyException {
            strategy.selectNextOSD(objectNo);
            NextRequest next = strategy.getNext();
            
            if (next != null) { // OSD found for fetching object
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "%s:%d - fetch object from OSD %s", fileID, next.objectNo, next.osd);
                
                try {
                    sendFetchObjectRequest(next.objectNo, next.osd, next.attachObjectSet);
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
        
        /**
         * 
         * @param usedOSD
         * @return true, if object is completed; false otherwise
         * @throws TransferStrategyException
         */
        public boolean objectFetched(ObjectData data, final ServiceUUID usedOSD)
            throws TransferStrategyException {
            if (!data.getInvalid_checksum_on_osd()) {
                // correct checksum
                if (hasWaitingRequests())
                    sendResponses(data.getData(), ObjectStatus.EXISTS);
                
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                        "%s:%d - OBJECT FETCHED", fileID, objectNo);
                
                
                if (!isStopped()) {
                    
                    // write data to disk
                    OSDOperation writeObjectEvent = master.getInternalEvent(EventWriteObject.class);
                    // NOTE: "original" buffer is used
                    
                    writeObjectEvent.startInternalEvent(new Object[] { fileID, objectNo, data.getData(),
                        xLoc, cow });
                    
                }
                
                return true;
            } else {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "%s:%d - fetched object has an invalid checksum", fileID, objectNo);
                
                // save data, in case other replicas are less useful
                this.data = data;
                
                // if (!ReplicatingFile.cancelled) {
                // try next replica
                strategy.addObject(objectNo, hasWaitingRequests());
                replicateObject();
                // } else {
                // sendError(ReplicatingFile, objectNo, new
                // OSDException(ErrorCodes.IO_ERROR,
                // "Object does not exist locally and replication was cancelled.",
                // ""));
                //                
                // }
                return false;
            }
        }
        
        /**
         * Checks if it is a hole. Otherwise it tries to use another OSD for
         * fetching.
         * 
         * @param usedOSD
         *            last used OSD for fetching this object
         * 
         * @return true, if object is completed; false otherwise
         * @throws TransferStrategyException
         */
        public boolean objectNotFetched(final ObjectData data, final ServiceUUID usedOSD) throws TransferStrategyException {
            // check if it is a hole
            if (xLoc.getReplica(usedOSD).isComplete()) {
                // => hole or error; we assume it is a hole
                if (hasDataFromEarlierResponses() && hasWaitingRequests()) {
                    // no hole, but an object for which only a replica with a
                    // wrong checksum could be found
                    sendResponses(this.data.getData(), ObjectStatus.EXISTS);
                    
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                            "%s:%d - OBJECT FETCHED, but with wrong checksum", fileID, objectNo);
                } else {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "%s:%d - OBJECT COULD NOT BE FETCHED FROM A COMPLETE REPLICA; MUST BE A HOLE.",
                            fileID, objectNo);
                    
                    if (hasWaitingRequests())
                        sendResponses(null, ObjectStatus.PADDING_OBJECT);
                    
                    // mark the object as a hole
                    OSDOperation writeObjectEvent = master.getInternalEvent(EventInsertPaddingObject.class);
                    writeObjectEvent.startInternalEvent(new Object[] { fileID, objectNo, xLoc,
                        data.getZero_padding() });
                    
                }
                return true;
            } else {
                return objectNotFetchedBecauseError(null, usedOSD);
            }
        }
        
        public boolean objectNotFetchedBecauseError(final Exception error, final ServiceUUID usedOSD)
            throws TransferStrategyException {
            if (Logging.isDebug() && error != null)
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "%s:%d - an error occurred while fetching the object from OSD %s: %s", fileID, objectNo,
                    usedOSD.toString(), error.getMessage());
            
            // if (!ReplicatingFile.cancelled) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "%s:%d - object could not be fetched from OSD => try next OSD", fileID, objectNo);
            
            // try next replica
            strategy.addObject(objectNo, hasWaitingRequests());
            replicateObject();
            // } else {
            // sendError(ReplicatingFile, objectNo, new
            // OSDException(ErrorCodes.IO_ERROR,
            // "Object does not exist locally and replication was cancelled.",
            // ""));
            //                
            // objectCompleted(ReplicatingFile, objectNo);
            // }
            return false;
        }
        
        /**
         * sends the responses to all belonging clients <br>
         * NOTE: data-buffer will not be modified
         */
        private void sendResponses(ReusableBuffer data, ObjectStatus status) {
            List<StageRequest> reqs = getWaitingRequests();
            // IMPORTANT: stripe size must be the same in all striping policies
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
    /**
     * the absolute maximum that can be set for maxRequestsPerFile
     */
    private static final int                 MAX_MAX_OBJECTS_IN_PROGRESS = 5;
    
    private final OSDRequestDispatcher       master;
    
    /**
     * controls how many fetch-object-requests will be sent per file (used for
     * load-balancing)
     */
    private static int                       maxObjectsInProgress;
    
    public final String                      fileID;
    
    private final TransferStrategy           strategy;
    
    private final long                       lastObject;
    
    private XLocations                       xLoc;
    
    private Capability                       cap;
    
    private CowPolicy                        cow;
    
    private InetSocketAddress                mrcAddress                  = null;
    
    /**
     * if the file is removed while it will be replicated
     */
    private boolean                          cancelled;
    
    /**
     * marks THIS replica as to be a full replica (enables background
     * replication)
     */
    // FIXME: private
    public boolean                           isFullReplica;
    
    /**
     * manages the OSD availability
     */
    private final ServiceAvailability        osdAvailability;
    
    /**
     * key: objectNo
     */
    private HashMap<Long, ReplicatingObject> objectsInProgress;
    
    /**
     * contains all requests which are waiting for an object, where the
     * replication of the object has been not started so far <br>
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
        
        // IMPORTANT: stripe size must be the same in all striping policies
        StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
        assert (checkEqualStripeSizeOfReplicas(xLoc.getReplicas()));
        this.lastObject = sp.getObjectNoForOffset(xLoc.getXLocSet().getRead_only_file_size() - 1);
        
        // create a new strategy
        if (ReplicationFlags.isRandomStrategy(xLoc.getLocalReplica().getTransferStrategyFlags()))
            strategy = new RandomStrategy(fileID, xLoc, osdAvailability);
        // FIXME: test stuff
        // strategy = new RandomStrategyWithoutObjectSets(fileID, xLoc,
        // osdAvailability);
        else if (ReplicationFlags.isSequentialStrategy(xLoc.getLocalReplica().getTransferStrategyFlags()))
            strategy = new SequentialStrategy(fileID, xLoc, osdAvailability);
        else if (ReplicationFlags.isSequentialPrefetchingStrategy(xLoc.getLocalReplica()
                .getTransferStrategyFlags()))
            strategy = new SequentialPrefetchingStrategy(fileID, xLoc, osdAvailability);
        else if (ReplicationFlags.isRarestFirstStrategy(xLoc.getLocalReplica().getTransferStrategyFlags()))
            strategy = new RarestFirstStrategy(fileID, xLoc, osdAvailability);
        else
            throw new IllegalArgumentException("Set Replication Strategy not known ("
                + xLoc.getLocalReplica().getTransferStrategyFlags() + ").");
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "%s - using strategy: %s",
                fileID, strategy.getClass().getName());
        
        // check if background replication is required
        isFullReplica = !xLoc.getLocalReplica().isPartialReplica();
        if (isFullReplica) {
            // get striping column of local OSD
            int coloumn = xLoc.getLocalReplica().getOSDs().indexOf(master.getConfig().getUUID());
            // add all objects (for this OSD) to strategy
            Iterator<Long> objectsIt = sp.getObjectsOfOSD(coloumn, 0, lastObject);
            while (objectsIt.hasNext()) {
                strategy.addObject(objectsIt.next(), false);
            }
        }
        
        // monitoring = new NumberMonitoring();
        // startMonitoringStuff();
    }
    
    /**
     * updates the capability and XLocations-list, if they are newer
     * 
     * @return true, if something has changed
     */
    public boolean update(Capability cap, XLocations xLoc, CowPolicy cow) {
        boolean changed = false;
        this.cow = cow;
        // if newer
        if (cap.getExpires() > this.cap.getExpires() || cap.getEpochNo() > this.cap.getEpochNo()) {
            this.cap = cap;
            changed = true;
        }
        if (hasXLocChanged(xLoc)) {
            this.xLoc = xLoc;
            this.strategy.updateXLoc(xLoc);
            changed = true;
        }
        return changed;
    }
    
    /**
     * checks if the xLoc has changed since the last update (or creation-time)
     * 
     * @param xLoc
     * @return
     */
    public boolean hasXLocChanged(XLocations xLoc) {
        return xLoc.getXLocSet().getVersion() > this.xLoc.getXLocSet().getVersion();
    }
    
    /**
     * enqueues the request and corresponding object for replication
     * 
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public boolean addObjectForReplication(Long objectNo, StageRequest rq) {
        assert (rq != null);
        
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
     * adds the object to the list of objects which are currently in progress
     * 
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    private boolean processObject(Long objectNo) {
        if (!isObjectInProgress(objectNo)) {
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
     * checks if replication of objects is in progress
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
     * @see java.util.HashMap#size()
     */
    public int getNumberOfWaitingObjects() {
        return waitingRequests.size();
    }
    
    /**
     * chooses an object and matching OSD for replicating it
     * 
     * @throws TransferStrategyException
     */
    public void replicate() throws TransferStrategyException {
        while (objectsInProgress.size() < maxObjectsInProgress) {
            strategy.selectNext();
            NextRequest next = strategy.getNext();
            
            if (next != null) { // there is something to fetch
                // object replication is in progress
                processObject(next.objectNo);
                
                if (Logging.isDebug())
                    if (next.attachObjectSet)
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "%s:%d - fetch object from OSD %s with object set", fileID, next.objectNo,
                            next.osd);
                    else
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "%s:%d - fetch object from OSD %s", fileID, next.objectNo, next.osd);
                
                try {
                    sendFetchObjectRequest(next.objectNo, next.osd, next.attachObjectSet);
                } catch (UnknownUUIDException e) {
                    // try other OSD
                    objectsInProgress.get(next.objectNo).replicateObject();
                }
            } else
                break;
        }
    }
    
    /**
     * 
     * @param objectNo
     * @param usedOSD
     * @param data
     */
    public void objectFetched(long objectNo, final ServiceUUID usedOSD, ObjectData data) {
        ReplicatingObject object = objectsInProgress.get(objectNo);
        assert (object != null) : objectNo + ", " + usedOSD.toString();
        
        try {
            boolean objectCompleted = object.objectFetched(data, usedOSD);
            if (objectCompleted) {
                objectReplicationCompleted(objectNo);
                
                // if (!strategy.isObjectListEmpty()) { // there are still
                // objects to fetch
                // if (Logging.isDebug())
                // Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication,
                // this,
                // "background replication: replicate next object for file %s",
                // fileID);
                // replicate(); // background replication
                // }
            }
        } catch (TransferStrategyException e) {
            // TODO: differ between ErrorCodes
            object.sendError(new OSDException(ErrorCodes.IO_ERROR, e.getMessage(), e.getStackTrace()
                    .toString()));
            objectReplicationCompleted(objectNo);
            // end replicating this file
        }
    }
    
    /**
     * Checks if it is a hole. Otherwise it tries to use another OSD for
     * fetching.
     * 
     * @param objectNo
     * @param usedOSD
     */
    public void objectNotFetched(long objectNo, final ServiceUUID usedOSD, ObjectData data) {
        ReplicatingObject object = objectsInProgress.get(objectNo);
        assert (object != null);
        
        try {
            boolean objectCompleted = object.objectNotFetched(data, usedOSD);
            if (objectCompleted) {
                objectReplicationCompleted(objectNo);
                
                if (!strategy.isObjectListEmpty()) { // there are still objects
                    // to fetch
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "background replication: replicate next object for file %s", fileID);
                    replicate(); // background replication
                }
            }
        } catch (TransferStrategyException e) {
            // TODO: differ between ErrorCodes
            object.sendError(new OSDException(ErrorCodes.IO_ERROR, e.getMessage(), e.getStackTrace()
                    .toString()));
            objectReplicationCompleted(objectNo);
            // end replicating this file
        }
    }
    
    /**
     * Tries to use another OSD for fetching.
     * 
     * @param objectNo
     * @param usedOSD
     */
    /*
     * code copied from objectNotFetched(...)
     */
    public void objectNotFetchedBecauseError(long objectNo, final ServiceUUID usedOSD, final Exception error) {
        ReplicatingObject object = objectsInProgress.get(objectNo);
        assert (object != null);
        
        try {
            boolean objectCompleted = object.objectNotFetchedBecauseError(error, usedOSD);
            if (objectCompleted) {
                objectReplicationCompleted(objectNo);
                
                if (!strategy.isObjectListEmpty()) { // there are still objects
                    // to fetch
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "background replication: replicate next object for file %s", fileID);
                    replicate(); // background replication
                }
            }
        } catch (TransferStrategyException e) {
            // TODO: differ between ErrorCodes
            object.sendError(new OSDException(ErrorCodes.IO_ERROR, e.getMessage(), e.getStackTrace()
                    .toString()));
            objectReplicationCompleted(objectNo);
            // end replicating this file
        }
    }
    
    /**
     * cleans up maps, lists, ...
     * 
     * @param objectNo
     */
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
     * 
     */
    public void objectSetFetched(ServiceUUID osd, ObjectSet objectSet) {
        strategy.setOSDsObjectSet(objectSet, osd);
    }
    
    /**
     * Sends a RPC for reading the object on another OSD.
     * 
     * @param attachObjectSet
     * @throws UnknownUUIDException
     */
    private void sendFetchObjectRequest(final long objectNo, final ServiceUUID osd, boolean attachObjectSet)
        throws UnknownUUIDException {
        // check capability validity and update capability if necessary
        try {
            checkCap();
        } catch (IOException e1) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this,
                "cannot update capability for file %s due to " + e1.getLocalizedMessage(), fileID);
        } catch (ONCRPCException e1) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this,
                "cannot update capability for file %s due to " + e1.getLocalizedMessage(), fileID);
        }
        
        // check that the load-restriction works
        assert (objectsInProgress.size() <= MAX_MAX_OBJECTS_IN_PROGRESS);
        
        OSDClient client = master.getOSDClientForReplication();
        // IMPORTANT: stripe size must be the same in all striping policies
        RPCResponse<InternalReadLocalResponse> response = client.internal_read_local(osd.getAddress(),
            fileID, new FileCredentials(cap.getXCap(), xLoc.getXLocSet()), objectNo, 0, 0, xLoc
                    .getLocalReplica().getStripingPolicy().getStripeSizeForObject(objectNo), attachObjectSet,
            null);
        
        response.registerListener(new RPCResponseAvailableListener<InternalReadLocalResponse>() {
            @Override
            public void responseAvailable(RPCResponse<InternalReadLocalResponse> r) {
                InternalReadLocalResponse internalReadLocalResponse = null;
                try {
                    internalReadLocalResponse = r.get();
                    ObjectData data = internalReadLocalResponse.getData();
                    ObjectList objectList = null;
                    if (internalReadLocalResponse.getObject_set().size() == 1)
                        objectList = internalReadLocalResponse.getObject_set().get(0);
                    master.getReplicationStage().internalObjectFetched(fileID, objectNo, osd, data,
                        objectList, null);
                } catch (ONCRPCException e) {
                    // osdAvailability.setServiceWasNotAvailable(osd);
                    master.getReplicationStage().internalObjectFetched(fileID, objectNo, osd, null, null, e);
                    // e.printStackTrace();
                    
                    if (internalReadLocalResponse != null)
                        BufferPool.free(internalReadLocalResponse.getData().getData());
                } catch (IOException e) {
                    osdAvailability.setServiceWasNotAvailable(osd);
                    master.getReplicationStage().internalObjectFetched(fileID, objectNo, osd, null, null, e);
                    // e.printStackTrace();
                    
                    if (internalReadLocalResponse != null)
                        BufferPool.free(internalReadLocalResponse.getData().getData());
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
    public void reportError(Exception error) {
        Logging.logError(Logging.LEVEL_ERROR, this, error);
        for (ReplicatingObject object : waitingRequests.values())
            object.sendError(error);
        for (ReplicatingObject object : objectsInProgress.values())
            object.sendError(error);
    }
    
    /**
     * checks if the capability is still valid; renews the capability if
     * necessary
     * 
     * @throws IOException
     * @throws ONCRPCException
     */
    public void checkCap() throws IOException, ONCRPCException {
        try {
            long curTime = TimeSync.getGlobalTime() / 1000; // s
            
            // get the correct MRC only once and only if the capability must be
            // updated
            if (cap.getExpires() - curTime < 60 * 1000) { // capability expires
                // in less than 60s
                if (mrcAddress == null) {
                    String volume = null;
                    try {
                        // get volume of file
                        volume = new MRCHelper.GlobalFileIdResolver(fileID).getVolumeId();
                        
                        // get MRC appropriate for this file
                        RPCResponse<ServiceSet> r = master.getDIRClient().xtreemfs_service_get_by_uuid(null,
                            volume);
                        ServiceSet sSet;
                        sSet = r.get();
                        r.freeBuffers();
                        
                        if (sSet.size() != 0)
                            mrcAddress = new ServiceUUID(sSet.get(0).getData().get("mrc")).getAddress();
                        else
                            throw new IOException("Cannot find a MRC.");
                    } catch (UserException e) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, e.getLocalizedMessage()
                            + "; for file %s", fileID);
                    }
                    
                }
                
                // update Xcap
                RPCResponse<XCap> r = master.getMRCClient().xtreemfs_renew_capability(mrcAddress,
                    cap.getXCap());
                XCap xCap = r.get();
                r.freeBuffers();
                
                cap = new Capability(xCap, master.getConfig().getCapabilitySecret());
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
    /**
     * adjust this value for load-balancing
     * 
     * @param maxObjects
     */
    public static void setMaxObjectsInProgressPerFile(int maxObjects) {
        // at least one request/object MUST be sent per file
        if (maxObjects >= 1)
            if (maxObjects <= MAX_MAX_OBJECTS_IN_PROGRESS)
                ReplicatingFile.maxObjectsInProgress = maxObjects;
            else
                ReplicatingFile.maxObjectsInProgress = MAX_MAX_OBJECTS_IN_PROGRESS;
        else
            ReplicatingFile.maxObjectsInProgress = 1;
    }
    
    /*
     * additional test if asserts are enabled
     */
    private boolean checkEqualStripeSizeOfReplicas(List<Replica> replicas) {
        boolean allEqual = true;
        int stripeSize = replicas.get(0).getStripingPolicy().getStripeSizeForObject(0);
        for (Replica replica : replicas)
            if (stripeSize != replica.getStripingPolicy().getStripeSizeForObject(0))
                allEqual = false;
        return allEqual;
    }
    
    public void startNewReplication() throws TransferStrategyException {
        if (!strategy.isObjectListEmpty()) { // there are still objects to fetch
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "background replication: replicate next object for file %s", fileID);
            replicate(); // background replication
        }
    }
    
    /*
     * monitoring for HighestThroughputOSDSelection
     */
    // ConcurrentHashMap<ServiceUUID, Integer> requestsSentToOSDs = new
    // ConcurrentHashMap<ServiceUUID, Integer>();
    // ConcurrentHashMap<ServiceUUID, Integer> requestsReceivedFromOSDs = new
    // ConcurrentHashMap<ServiceUUID, Integer>();
    //
    // private Thread monitoringThread = null;
    // private NumberMonitoring monitoring;
    // public static final String MONITORING_KEY_THROUGHPUT =
    // "requests (sent/received) for OSD ";
    //
    // public void startMonitoringStuff() {
    // if (Monitoring.isEnabled()) {
    // monitoringThread = new Thread(new Runnable() {
    // public static final int MONITORING_INTERVAL = 10000; // 10s
    //
    // @Override
    // public void run() {
    // try {
    // while (true) {
    // if (Thread.interrupted())
    // break;
    // Thread.sleep(MONITORING_INTERVAL); // sleep
    //
    // for (Entry<ServiceUUID, Integer> e : requestsSentToOSDs.entrySet()) {
    // Integer requestsReceived = requestsReceivedFromOSDs.remove(e.getKey());
    // Integer requestsSent = e.getValue();
    // monitoring.put(MONITORING_KEY_THROUGHPUT + e.getKey(),
    // (requestsSent / requestsReceived) / (MONITORING_INTERVAL / 1000d));
    // }
    // for (Entry<ServiceUUID, Integer> e : requestsReceivedFromOSDs.entrySet())
    // {
    // Integer requestsSent = requestsSentToOSDs.remove(e.getKey());
    // Integer requestsReceived = e.getValue();
    // monitoring.put(MONITORING_KEY_THROUGHPUT + e.getKey(),
    // (requestsSent / requestsReceived) / (MONITORING_INTERVAL / 1000d));
    // }
    // // remove all
    // requestsReceivedFromOSDs.clear();
    // requestsSentToOSDs.clear();
    // }
    // } catch (InterruptedException e) {
    // // shutdown
    // }
    // }
    // });
    // monitoringThread.setDaemon(true);
    // monitoringThread.start();
    // }
    // }
}
