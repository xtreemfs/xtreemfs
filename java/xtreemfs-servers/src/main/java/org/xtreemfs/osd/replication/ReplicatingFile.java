/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.operations.EventInsertPaddingObject;
import org.xtreemfs.osd.operations.EventWriteObject;
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
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalReadLocalResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectList;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

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
        InternalObjectData                 data            = null;
        
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
                } catch (IOException e) {
                    // try other OSD
                    replicateObject();
                }
            } else { // should not happen
                sendError(ErrorUtils.getErrorResponse(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, "transfer strategy returns neither a value nor an exception"));
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
        public boolean objectFetched(InternalObjectData data, final ServiceUUID usedOSD)
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
        public boolean objectNotFetched(final InternalObjectData data, final ServiceUUID usedOSD) throws TransferStrategyException {
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
        
        public boolean objectNotFetchedBecauseError(final ErrorResponse error, final ServiceUUID usedOSD)
            throws TransferStrategyException {
            if (Logging.isDebug() && error != null)
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "%s:%d - an error occurred while fetching the object from OSD %s: %s", fileID, objectNo,
                    usedOSD.toString(), error.getErrorMessage());
            
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
        public void sendError(ErrorResponse error) {
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
    
    /** If a VIEW_ERROR was encountered during replication. */
    private boolean                          viewOutdated;

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
    private final HashMap<Long, ReplicatingObject> objectsInProgress;
    
    /**
     * contains all requests which are waiting for an object, where the
     * replication of the object has been not started so far <br>
     * key: objectNo
     */
    private final HashMap<Long, ReplicatingObject> waitingRequests;
    
    public ReplicatingFile(String fileID, XLocations xLoc, Capability cap, CowPolicy cow,
        OSDRequestDispatcher master) {
        this.master = master;
        this.osdAvailability = master.getServiceAvailability();
        
        this.fileID = fileID;
        this.xLoc = xLoc;
        this.cap = cap;
        this.cow = cow;
        this.cancelled = false;
        this.viewOutdated = false;
        this.objectsInProgress = new HashMap<Long, ReplicatingObject>();
        this.waitingRequests = new HashMap<Long, ReplicatingObject>();
        
        // IMPORTANT: stripe size must be the same in all striping policies
        StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
        assert (checkEqualStripeSizeOfReplicas(xLoc.getReplicas()));
        this.lastObject = sp.getObjectNoForOffset(xLoc.getXLocSet().getReadOnlyFileSize() - 1);
        
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
            this.viewOutdated = false;
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
            // But it may be already queued in waitingRequests.
            info = waitingRequests.get(objectNo);

            if (info == null) {
                // Neither queued in objectsInProgress nor waitingRequests.
                info = new ReplicatingObject(objectNo);
                waitingRequests.put(objectNo, info);
                // add to strategy
                strategy.addObject(objectNo, true);
            }
        }

        info.getWaitingRequests().add(rq);

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
    
    public boolean isViewOutdated() {
        return viewOutdated;
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
                            "%s:%d - fetch object from OSD %s with object set of total size %s; " +
                                    "number of objectsInProgress: %s",
                                           fileID, next.objectNo, next.osd,
                                           strategy.getObjectsCount(), this.getNumberOfObjectsInProgress());
                    else
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "%s:%d - fetch object from OSD %s (%s objects remain; " +
                                    "number of objectsInProgress: %s; number of waitingObjects: %s)",
                                           fileID, next.objectNo, next.osd,
                                           strategy.getObjectsCount(),
                                           getNumberOfObjectsInProgress(),
                                           getNumberOfWaitingObjects());
                
                try {
                    sendFetchObjectRequest(next.objectNo, next.osd, next.attachObjectSet);
                } catch (IOException e) {
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
    public void objectFetched(long objectNo, final ServiceUUID usedOSD, InternalObjectData data) {
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
            object.sendError(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, e.getMessage()));
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
    public void objectNotFetched(long objectNo, final ServiceUUID usedOSD, InternalObjectData data) {
        ReplicatingObject object = objectsInProgress.get(objectNo);
        assert (object != null);

        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                           "%s:%s - object not fetched",
                           fileID, objectNo);


        try {
            boolean objectCompleted = object.objectNotFetched(data, usedOSD);
            if (objectCompleted) {
                objectReplicationCompleted(objectNo);
                
                if (!strategy.isObjectListEmpty()) { // there are still objects to fetch
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                            "background replication: replicate next object for file %s", fileID);
                    replicate(); // background replication
                }
            }
        } catch (TransferStrategyException e) {
            // TODO: differ between ErrorCodes
            object.sendError(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, e.getMessage()));
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
    public void objectNotFetchedBecauseError(long objectNo, final ServiceUUID usedOSD, final ErrorResponse error) {
        ReplicatingObject object = objectsInProgress.get(objectNo);
        assert (object != null);

        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                           "%s:%s - object not fetched because of non-view error",
                           fileID, objectNo);

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
            object.sendError(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, e.getMessage()));
            objectReplicationCompleted(objectNo);
            // end replicating this file
        }
    }
    
    public void objectNotFetchedBecauseViewError(long objectNo, final ServiceUUID usedOSD, final ErrorResponse error) {
        ReplicatingObject object = objectsInProgress.get(objectNo);
        assert (object != null);

        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                           "%s:%s - object not fetched because of view error",
                           fileID, objectNo);

        // Remember the view error (used to deny future requests until a new view/xlocset is provided)
        viewOutdated = true;

        // Send an error to every pending request.
        reportError(error);

        // Stop this objects replication.
        objectReplicationCompleted(objectNo);

        // Remove waiting elements from the queue.
        waitingRequests.clear();
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
        Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                           "%s:%s - object replication completed; " +
                                   "removing object %s from objectsInProgress. " +
                                   "New number of objectsInProgress: %s",
                           fileID, objectNo, objectNo, this.getNumberOfObjectsInProgress());
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
        throws UnknownUUIDException, IOException {
        // check capability validity and update capability if necessary
        try {
            checkCap();    
        } catch (IOException e1) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this,
                "cannot update capability for file %s due to " + e1.getLocalizedMessage(), fileID);
        }
        
        // check that the load-restriction works
        assert (objectsInProgress.size() <= MAX_MAX_OBJECTS_IN_PROGRESS);
        
        OSDServiceClient client = master.getOSDClientForReplication();
        // IMPORTANT: stripe size must be the same in all striping policies
        FileCredentials fcred = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLoc.getXLocSet()).build();
        RPCResponse<InternalReadLocalResponse> response = client.xtreemfs_internal_read_local(osd.getAddress(), RPCAuthentication.authNone,RPCAuthentication.userService,
            fcred, fileID, objectNo, 0, 0, xLoc
                    .getLocalReplica().getStripingPolicy().getStripeSizeForObject(objectNo), attachObjectSet,
            new ArrayList(0));
        
        response.registerListener(new RPCResponseAvailableListener<InternalReadLocalResponse>() {
            @Override
            public void responseAvailable(RPCResponse<InternalReadLocalResponse> r) {
                InternalReadLocalResponse internalReadLocalResponse = null;
                try {
                    internalReadLocalResponse = r.get();
                    ObjectData metadata = internalReadLocalResponse.getData();
                    InternalObjectData data = new InternalObjectData(metadata,r.getData());
                    ObjectList objectList = null;
                    if (internalReadLocalResponse.getObjectSetCount() == 1)
                        objectList = internalReadLocalResponse.getObjectSet(0);
                    master.getReplicationStage().internalObjectFetched(fileID, objectNo, osd, data,
                        objectList, null);
                } catch (PBRPCException e) {
                    if (e.getErrorType() != ErrorType.INVALID_VIEW) {
                        osdAvailability.setServiceWasNotAvailable(osd);
                    }
                    master.getReplicationStage().internalObjectFetched(fileID, objectNo, osd, null, null,
                            e.getErrorResponse());
                } catch (IOException e) {
                    osdAvailability.setServiceWasNotAvailable(osd);
                    master.getReplicationStage().internalObjectFetched(fileID, objectNo, osd, null, null,
                            ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, e.toString()));
                    // e.printStackTrace();
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
    public void reportError(ErrorResponse error) {
        Logging.logMessage(Logging.LEVEL_ERROR, this, ErrorUtils.formatError(error));
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
     */
    public void checkCap() throws IOException {
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
                        ServiceSet sSet = master.getDIRClient().xtreemfs_service_get_by_uuid(null, RPCAuthentication.authNone, RPCAuthentication.userService,
                            volume);
                        
                        if (sSet.getServicesCount() != 0) {
                            for (KeyValuePair kvp : sSet.getServices(0).getData().getDataList()) {
                                if (kvp.getKey().equals("mrc")) {
                                    mrcAddress = new ServiceUUID(kvp.getValue()).getAddress();
                                }
                            }

                        }
                        else
                            throw new IOException("Cannot find a MRC.");
                    } catch (UserException e) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, e.getLocalizedMessage()
                            + "; for file %s", fileID);
                    }
                    
                }
                
                // update Xcap
                RPCResponse<XCap> r = master.getMRCClient().xtreemfs_renew_capability(mrcAddress, RPCAuthentication.authNone, RPCAuthentication.userService, 
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
