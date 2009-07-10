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

import java.util.HashMap;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.LRUCache;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * Handles the fetching of replicas. <br>
 * 15.09.2008
 */
public class ObjectDissemination {
    private OSDRequestDispatcher             master;

    /**
     * controls how many fetch-object-requests will be allowed to sent overall by all files (used for
     * load-balancing)
     */
    private static final int                 MAX_REQUESTS_OVERALL = 20;

    /**
     * objects of these files are downloading currently or in future <br>
     * key: fileID
     */
    private HashMap<String, ReplicatingFile> filesInProgress;

    /**
     * simple FIFO-cache for last completed files
     */
    LRUCache<String, ReplicatingFile>        lastCompletedFilesCache;

    public ObjectDissemination(OSDRequestDispatcher master) {
        this.master = master;

        this.filesInProgress = new HashMap<String, ReplicatingFile>();
        this.lastCompletedFilesCache = new LRUCache<String, ReplicatingFile>(20);
    }

    /**
     * saves the request and fetches the object
     */
    public void fetchObject(String fileID, long objectNo, XLocations xLoc, Capability capability,
            CowPolicy cow, final StageRequest rq) {
        ReplicatingFile file = this.filesInProgress.get(fileID);
        if (file == null) { // file not in progress
            // try to get it from cache
            file = this.lastCompletedFilesCache.get(fileID);
            if (file == null) // create new one
                file = new ReplicatingFile(fileID, xLoc, capability, cow, master);

            // add file to filesInProgress
            this.filesInProgress.put(fileID, file);

            // update requestsPerFile for all files (load-balancing)
            ReplicatingFile.setMaxRequestsPerFile(MAX_REQUESTS_OVERALL / filesInProgress.size());

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "start replicating file %s", fileID);
        }

        // update to newer cap, ...
        file.update(capability, xLoc, cow);

        // keep in mind current request
        if (file.isObjectInProgress(objectNo)) {
            // propably another request is already fetching this object
            file.addObjectForReplicating(objectNo, rq);
        } else {
            file.addObjectForReplicating(objectNo, rq);

            // start replication
            try {
                file.replicate();
            } catch (TransferStrategyException e) {
                if (e.getErrorCode() == TransferStrategyException.ErrorCode.NO_OSD_FOUND)
                    file.reportError(new OSDException(ErrorCodes.IO_ERROR,
                            "no OSD could be found for fetching an object", e.getStackTrace().toString()));
                else if (e.getErrorCode() == TransferStrategyException.ErrorCode.NO_OSD_REACHABLE)
                    file.reportError(new OSDException(ErrorCodes.IO_ERROR,
                            "no OSD is reachable for fetching an object", e.getStackTrace().toString()));
            }

            if (!file.isReplicating())
                fileCompleted(file.fileID);
        }
    }

    /**
     * process all necessary actions if object was fetched correctly, otherwise triggers new fetch-attempt
     * 
     * @param usedOSD
     *            TODO
     */
    public void objectFetched(String fileID, long objectNo, final ServiceUUID usedOSD, ObjectData data) {
        ReplicatingFile file = filesInProgress.get(fileID);
        assert (file != null);

        file.objectFetched(objectNo, usedOSD, data);

        if (!file.isReplicating())
            fileCompleted(file.fileID);
    }

    /**
     * process all necessary actions, because object could not be fetched
     * 
     * @param usedOSD
     *            TODO
     */
    public void objectNotFetched(String fileID, final ServiceUUID usedOSD, long objectNo) {
        ReplicatingFile file = filesInProgress.get(fileID);
        assert (file != null);

        file.objectNotFetched(objectNo, usedOSD);

        if (!file.isReplicating())
            fileCompleted(file.fileID);
    }

    /**
     * cleans up maps, lists, ...
     * 
     * @param fileID
     */
    private void fileCompleted(String fileID) {
        // if the last requested object was fetched for this file => remove from map
        ReplicatingFile completedFile = filesInProgress.remove(fileID);
        assert (completedFile != null);
        // cache completed file
        lastCompletedFilesCache.put(fileID, completedFile);

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "stop replicating file %s",
                    fileID);

        // update requestsPerFile for all files (load-balancing)
        if (filesInProgress.size() == 0)
            ReplicatingFile.setMaxRequestsPerFile(MAX_REQUESTS_OVERALL / 1);
        else
            ReplicatingFile.setMaxRequestsPerFile(MAX_REQUESTS_OVERALL / filesInProgress.size());

        // TODO: save persistent marker that all objects of file are completely replicated, if replica is full
        // replica
    }

    /**
     * Stops replication for this file.
     */
    public void cancelFile(String fileID) {
        ReplicatingFile file = filesInProgress.get(fileID);
        if (file != null)
            if (file.isReplicating()) // => probably requests were sent
                // mark cancelled for deleting later
                file.stopReplicatingFile();
            else
                // delete directly
                filesInProgress.remove(fileID);
    }

    /**
     * sends an error to all belonging clients of this file (for all objects)
     */
    public void sendError(String fileID, Exception e) {
        ReplicatingFile file = filesInProgress.get(fileID);
        assert (file != null);

        file.reportError(e);
    }
}
