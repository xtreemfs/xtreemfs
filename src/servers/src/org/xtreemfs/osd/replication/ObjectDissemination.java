/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xtreemfs.common.Capability;
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
import org.xtreemfs.osd.stages.ReplicationStage.FetchObjectCallback;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;

/**
 * Handles the fetching of replicas.
 * <br>15.09.2008
 */
public class ObjectDissemination {
    private OSDRequestDispatcher master;

    /**
     * Encapsulates important infos about a file
     * <br>01.04.2009
     */
    private static class FileInfo {
        private static class ObjectInfo {
            List<StageRequest> waitingRequests;
            ServiceUUID lastOSD;

            public ObjectInfo() {
                this.waitingRequests = new ArrayList<StageRequest>();
            }
        }

        String fileID;
        final TransferStrategy strategy;
        long lastObject;
        XLocations xLoc;
        Capability cap;
        CowPolicy cow;
        /**
         * if the file is removed while it will be replicated
         */
        boolean cancelled;
        
        /**
         * key: objectNo
         */
        private HashMap<Long, ObjectInfo> objectsInProgress;

        public FileInfo(String fileID, TransferStrategy strategy, XLocations xLoc, Capability cap,
                CowPolicy cow) {
            this.fileID = fileID;
            this.strategy = strategy;
            this.xLoc = xLoc;
            this.cap = cap;
            this.cow = cow;
            this.cancelled = false;
            this.objectsInProgress = new HashMap<Long, ObjectInfo>();

            StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
            this.lastObject = sp.getObjectNoForOffset(xLoc.getXLocSet().getRead_only_file_size() - 1);
            }

        /**
         * @see java.util.HashMap#containsKey(java.lang.Object)
         */
        public boolean containsKey(Long arg0) {
            return objectsInProgress.containsKey(arg0);
        }

        /**
         * @see java.util.HashMap#get(java.lang.Object)
         */
        public ObjectInfo get(Long arg0) {
            return objectsInProgress.get(arg0);
        }

        /**
         * @see java.util.ArrayList#add(java.lang.Object)
         */
        public ObjectInfo add(Long arg0, StageRequest rq) {
            ObjectInfo info = objectsInProgress.get(arg0);
            if (info == null)
                info = new ObjectInfo();
            info.waitingRequests.add(rq);
            objectsInProgress.put(arg0, info);
            return info;
        }

        /**
         * @see java.util.HashMap#isEmpty()
         */
        public boolean isEmpty() {
            return objectsInProgress.isEmpty();
        }

        /**
         * @see java.util.HashMap#remove(java.lang.Object)
         */
        public ObjectInfo remove(Long arg0) {
            return objectsInProgress.remove(arg0);
        }

        /**
         * @see java.util.HashMap#size()
         */
        public int size() {
            return objectsInProgress.size();
        }
        
        public void update(Capability cap, XLocations xLoc, CowPolicy cow) {
            this.cap = cap;
            this.cow = cow;
            if (xLoc.getXLocSet().getVersion() > this.xLoc.getXLocSet().getVersion()) {
                this.xLoc = xLoc;
                this.strategy.updateXLoc(xLoc);
            }
        }
    }

    /**
     * objects of these files are downloading currently or in future
     * key: fileID
     */
    private HashMap<String, FileInfo> filesInProgress;

    /**
     * manages the OSD availability
     */
    private final ServiceAvailability osdAvailability;
    
    public ObjectDissemination(OSDRequestDispatcher master, final ServiceAvailability osdAvailability) {
        this.master = master;
        this.osdAvailability = osdAvailability;

        this.filesInProgress = new HashMap<String, FileInfo>();
    }

    /**
     * saves the request and fetches the object
     */
    public void fetchObject(String fileID, long objectNo, XLocations xLoc, Capability capability,
            CowPolicy cow, final StageRequest rq) {
        FileInfo fileInfo = this.filesInProgress.get(fileID);
        if (fileInfo == null) { // file not in progress
            // create a new strategy
            TransferStrategy strategy = new RandomStrategy(fileID, xLoc, xLoc.getXLocSet()
                    .getRead_only_file_size(), osdAvailability);
            fileInfo = new FileInfo(fileID, strategy, xLoc, capability, cow);
            this.filesInProgress.put(fileID, fileInfo);

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "start replication for file %s",
                    fileID);
        }

        // update to newer cap, ...
        fileInfo.update(capability, xLoc, cow);
        
        // keep in mind current request
        if(fileInfo.containsKey(objectNo))
            // propably another request is already fetching this object
            fileInfo.add(objectNo, rq);
        else {  
            fileInfo.add(objectNo, rq);
            fileInfo.strategy.addObject(objectNo, true);
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "FETCHING OBJECT: %s:%d", fileID,
                    objectNo);
    
            prepareRequest(fileInfo, objectNo);
        }
    }

    /**
     * Prepares the request by using the associated TransferStrategy.
     */
    private void prepareRequest(FileInfo fileInfo, long objectNo) {
        TransferStrategy strategy = fileInfo.strategy;
        strategy.selectNextOSD(objectNo);
        NextRequest next = strategy.getNext();

        if (next != null) { // there is something to fetch
            // FIXME: only for debugging
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "fetch object %s:%d from OSD %s",
                    fileInfo.fileID, next.objectNo, next.osd);

            fileInfo.get(objectNo).lastOSD = next.osd;
            sendFetchObjectRequest(next.objectNo, next.osd, fileInfo.fileID, fileInfo.cap, fileInfo.xLoc);
        } else {
            sendError(fileInfo, objectNo, new OSDException(ErrorCodes.IO_ERROR,
                    "Object does not exist locally and none replica could be fetched.", ""));

            // FIXME: only for debugging
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "object %s:%d could not be fetched",
                    fileInfo.fileID, next.objectNo);
            System.out.println("For object " + objectNo + " of file " + fileInfo.fileID + " none replica could be fetched.");
            System.out.println("last object: " + fileInfo.lastObject);
            System.out.println("capability: " + fileInfo.cap.toString());
            System.out.println("XLocation: " + fileInfo.xLoc.getReplicas().toString());
            System.out.println("last OSD: " + fileInfo.get(objectNo).lastOSD);
            System.out.println("number of waiting requests: " + fileInfo.get(objectNo).waitingRequests.size());
            System.out.println("objects count: " + fileInfo.strategy.getObjectsCount());
            System.out.println("is hole?: " + fileInfo.strategy.isHole(objectNo));

            objectCompleted(fileInfo, objectNo);
        }
    }

    /**
     * Sends a RPC for reading the object on another OSD.
     */
    private void sendFetchObjectRequest(final long objectNo, final ServiceUUID osd, final String fileID,
            Capability capability, XLocations xLoc) {
        try {
            OSDClient client = master.getOSDClient();
            // TODO: change this, if using different striping policies
            RPCResponse<InternalReadLocalResponse> response = client.internal_read_local(osd.getAddress(),
                    fileID, new FileCredentials(xLoc.getXLocSet(), capability.getXCap()), objectNo, 0, 0,
                    xLoc.getLocalReplica().getStripingPolicy().getStripeSizeForObject(objectNo));

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
        } catch (UnknownUUIDException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * creates the responses and updates the maps
     */
    public void objectFetched(String fileID, long objectNo, ObjectData data) {
        FileInfo fileInfo = filesInProgress.get(fileID);
        assert(fileInfo != null);
        // FIXME: only for debugging
        if(fileInfo == null)
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "fileInfo for file %s is null, but must not.", fileID);

        if (!data.getInvalid_checksum_on_osd()) {
            // correct checksum
            sendResponses(fileInfo, objectNo, data.getData(), ObjectStatus.EXISTS);

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                        "OBJECT FETCHED %s:%d", fileID, objectNo);

            if (!fileInfo.cancelled) {
                // write data to disk
                OSDOperation writeObjectEvent = master.getInternalEvent(EventWriteObject.class);
                // NOTE: "original" buffer is used
                writeObjectEvent.startInternalEvent(new Object[] { fileID, objectNo, data.getData(),
                        fileInfo.xLoc, fileInfo.cow });
            }

            objectCompleted(fileInfo, objectNo);
        } else {
            // FIXME: only for debugging
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "fetched object %s:%d has invalid checksum",
                    fileID, objectNo);
           // TODO: save data, in case other replicas are less useful
//            if (!fileInfo.cancelled) {
                // try next replica
                fileInfo.strategy.addObject(objectNo, true); // TODO: preferred or only requested?
                prepareRequest(fileInfo, objectNo);
//            } else {
//                sendError(fileInfo, objectNo, new OSDException(ErrorCodes.IO_ERROR,
//                        "Object does not exist locally and replication was cancelled.", ""));
//                
//                objectCompleted(fileInfo, objectNo);
//            }
            // free buffer
            BufferPool.free(data.getData());
        }
    }

    /**
     * Checks if it is a hole. Otherwise it tries to use another OSD for fetching.
     */
    public void objectNotFetched(String fileID, long objectNo) {
        FileInfo fileInfo = filesInProgress.get(fileID);
        assert(fileInfo != null);
        // FIXME: only for debugging
        if(fileInfo == null)
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "fileInfo for file %s is null, but must not.", fileID);

        // check if it is a hole
        // TODO: change this, if using different striping policies
        if (fileInfo.strategy.isHole(objectNo)) {
            // => hole or error; we assume it is a hole
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                    "OBJECT %s:%d COULD NOT BE FETCHED; MUST BE A HOLE.", fileID, objectNo);

            sendResponses(fileInfo, objectNo, null, ObjectStatus.PADDING_OBJECT);

            objectCompleted(fileInfo, objectNo);

            // TODO: remember on this OSD that this object is a hole
        } else {
//            if (!fileInfo.cancelled) {
                // FIXME: only for debugging
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "object %s:%d could not be fetched from OSD %s => try next OSD",
                        fileInfo.fileID, objectNo, fileInfo.get(objectNo).lastOSD);
                // try next replica
                fileInfo.strategy.addObject(objectNo, true); // TODO: preferred or only requested?
                prepareRequest(fileInfo, objectNo);
//            } else {
//                sendError(fileInfo, objectNo, new OSDException(ErrorCodes.IO_ERROR,
//                        "Object does not exist locally and replication was cancelled.", ""));
//                
//                objectCompleted(fileInfo, objectNo);
//            }
        }
    }

    /**
     * Stops replication for this file.
     */ 
    public void cancelFile(String fileID) {
        FileInfo file = filesInProgress.get(fileID);
        if (file != null)
            if (!file.isEmpty()) // => probably requests were sent
                // mark cancelled for deleting later
                file.cancelled = true;
            else
                // delete directly
                filesInProgress.remove(fileID);
    }

    /**
     * removes the object from maps/lists and checks if the replication of all objects of the file is
     * completed
     * @param fileInfo
     * @param objectNo
     */
    private void objectCompleted(FileInfo fileInfo, long objectNo) {
        // delete object in maps/lists
        fileInfo.strategy.removeObject(objectNo);
        fileInfo.remove(objectNo);
        if (fileInfo.isEmpty())
            // nothing is replicating of this file
            fileCompleted(fileInfo.fileID);
    }

    /**
     * @param fileID
     */
    private void fileCompleted(String fileID) {
        // if the last requested object was fetched for this file => remove from map
        filesInProgress.remove(fileID);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "stop replicating file %s", fileID);
    }

    /**
     * sends the responses to all belonging clients
     * <br>NOTE: data-buffer will not be modified
     */
    private void sendResponses(FileInfo fileInfo, long objectNo, ReusableBuffer data, ObjectStatus status) {
        List<StageRequest> reqs = fileInfo.get(objectNo).waitingRequests;
        StripingPolicyImpl sp = fileInfo.xLoc.getLocalReplica().getStripingPolicy();
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
            // FIXME: filesize not from strategy
            objectInfo.setGlobalLastObjectNo(fileInfo.lastObject);

            final FetchObjectCallback callback = (FetchObjectCallback) rq.getCallback();
            callback.fetchComplete(objectInfo, null);
        }
    }
    
    /**
     * sends an error to all belonging clients 
     */
    private void sendError(FileInfo fileInfo, long objectNo, Exception error) {
        List<StageRequest> reqs = fileInfo.get(objectNo).waitingRequests;
        // responses
        for (StageRequest rq : reqs) {
            final FetchObjectCallback callback = (FetchObjectCallback) rq.getCallback();
            callback.fetchComplete(null, error);
        }
    }
}
