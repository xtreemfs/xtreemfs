/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.replication;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.monitoring.Monitoring;
import org.xtreemfs.foundation.monitoring.MonitoringLog;
import org.xtreemfs.foundation.monitoring.NumberMonitoring;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.replication.transferStrategies.TransferStrategy.TransferStrategyException;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * Handles the fetching of replicas. <br>
 * 15.09.2008
 */
public class ObjectDissemination {
    private final OSDRequestDispatcher                 master;

    /**
     * controls how many fetch-object-requests will be allowed to sent overall by all files (used for
     * load-balancing)
     */
    private static final int                           MAX_OBJECTS_IN_PROGRESS_OVERALL                          = 20;

    /**
     * objects of these files are downloading currently or in future <br>
     * key: fileID
     */
    private ConcurrentHashMap<String, ReplicatingFile> filesInProgress;

    /**
     * Simple LRU-cache for last completed files.<br>
     * NOTE: contains only NOT canceled files
     */
    LRUCache<String, ReplicatingFile>                  lastCompletedFilesCache;

    /*
     * monitoring stuff
     */
    private Thread                                     monitoringThread                                         = null;

    private NumberMonitoring                           monitoring;

    private AtomicLong                                 monitoringReadDataSizeInLastXs;

    /**
     * Measures the throughput of the last 1 second.
     */
    public static final String                         MONITORING_KEY_THROUGHPUT_OF_LAST_X_SECONDS              = "REPLICATION: average throughput over all files of last X seconds (KB/s)";

//    public static final String                         MONITORING_KEY_REQUIRED_TIME_FOR_COMPLETING_REPLICA      = "REPLICATION: time (ms) required for completing file ";
//
//    /*
//     * if the data contains holes this metric will be falsified
//     */
//    public static final String                         MONITORING_KEY_UNNECESSARY_REQUESTS                 = "REPLICATION: number of unnecessary requests";
//
//    public static final String                         MONITORING_KEY_OBJECT_LIST_OVERHEAD                 = "REPLICATION: overhead of object lists transfer (bytes)";

    public static final int                            MONITORING_THROUGHPUT_INTERVAL                      = 1000;                                                                     // 10s

//    // FIXME: change output file
//    public static final String                         MONITORING_OUTPUT_FILE                              = "/tmp/monitoringLog.txt";

    public ObjectDissemination(final OSDRequestDispatcher master) {
        this.master = master;

        this.filesInProgress = new ConcurrentHashMap<String, ReplicatingFile>();
        this.lastCompletedFilesCache = new LRUCache<String, ReplicatingFile>(20);
        
        // monitoring
        this.monitoring = new NumberMonitoring();
        this.monitoringReadDataSizeInLastXs = new AtomicLong(0);
        if (Monitoring.isEnabled()) {
            // enable stats on client (maybe stats is already enabled)
            //RPCNIOSocketClient.ENABLE_STATISTICS = true;
            
            try {
                MonitoringLog.initialize("");
            } catch (IOException e1) {
                // Auto-generated catch block
                e1.printStackTrace();
            }
            MonitoringLog.registerFor(monitoring, MONITORING_KEY_THROUGHPUT_OF_LAST_X_SECONDS);
            // FIXME: debug stuff
//            MonitoringLog.registerFor(monitoring, "StorageStage Queue");
            
            // create new thread which monitors the average throughput of all active files in a given interval
            monitoringThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            if (Thread.interrupted())
                                break;
                            Thread.sleep(MONITORING_THROUGHPUT_INTERVAL); // sleep

                            long sizeInLastXs = monitoringReadDataSizeInLastXs.getAndSet(0);
                            if (sizeInLastXs > 0) // log only interesting values
                                monitoring.put(MONITORING_KEY_THROUGHPUT_OF_LAST_X_SECONDS,
                                        (sizeInLastXs / 1024d) / (MONITORING_THROUGHPUT_INTERVAL / 1000d));
                        }
                    } catch (InterruptedException e) {
                        // shutdown
                    }
                }
            });
            monitoringThread.setDaemon(true);
            monitoringThread.start();
        }
    }

    /**
     * saves the request and fetches the object
     */
    public void fetchObject(String fileID, long objectNo, XLocations xLoc, Capability capability,
            CowPolicy cow, final StageRequest rq) {
        ReplicatingFile file = this.filesInProgress.get(fileID);
        if (file == null) { // file not in progress
            /*
             * Optimization: Use a cache of last completed files, so no new instance must be created every
             * time. But use file from cache only if the xLoc has not changed. Otherwise it could be not
             * guaranteed, that the information in the ReplicatingFile instance are correct (up-to-date).
             */
            file = this.lastCompletedFilesCache.get(fileID);
            if (file == null || (file != null && file.hasXLocChanged(xLoc))) { // create new one
                file = new ReplicatingFile(fileID, xLoc, capability, cow, master);
            }
//            // FIXME: test stuff
//            if (Monitoring.isEnabled() && file.isFullReplica)
//                monitoring.putLong(MONITORING_KEY_REQUIRED_TIME_FOR_COMPLETING_REPLICA + fileID, System
//                        .currentTimeMillis());

            // add file to filesInProgress
            this.filesInProgress.put(fileID, file);

            // update requestsPerFile for all files (load-balancing)
            ReplicatingFile.setMaxObjectsInProgressPerFile(MAX_OBJECTS_IN_PROGRESS_OVERALL / filesInProgress.size());

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this,
                        "%s - start replicating file", fileID);
        }

        // update to newer cap, ...
        file.update(capability, xLoc, cow);

        // if the view is still outdated, return an error.
        if (file.isViewOutdated()) {
            file.reportError(ErrorUtils.getErrorResponse(ErrorType.INVALID_VIEW, POSIXErrno.POSIX_ERROR_NONE,
                    "this replicas view is outdated. the xlocset has to be updated."));
        }

        // keep in mind current request
        if (file.isObjectInProgress(objectNo)) {
            // probably another request is already fetching this object
            file.addObjectForReplication(objectNo, rq);
        } else {
            file.addObjectForReplication(objectNo, rq);

            // start replication
            try {
                file.replicate();
            } catch (TransferStrategyException e) {
                if (e.getErrorCode() == TransferStrategyException.ErrorCode.NO_OSD_FOUND)
                    file.reportError(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                            "no OSD could be found for fetching an object", e));
                else if (e.getErrorCode() == TransferStrategyException.ErrorCode.NO_OSD_REACHABLE)
                    file.reportError(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO,
                            "no OSD is reachable for fetching an object", e));
            }

            if (!file.isReplicating())
                fileCompleted(file.fileID);
        }
    }

    /**
     * process all necessary actions if object was fetched correctly, otherwise triggers new fetch-attempt
     * 
     * @param usedOSD
     */
    public void objectFetched(String fileID, long objectNo, final ServiceUUID usedOSD, InternalObjectData data) {
        ReplicatingFile file = filesInProgress.get(fileID);
        assert (file != null);
        
        // monitoring
        monitoringReadDataSizeInLastXs.addAndGet(data.getData().limit());

        file.objectFetched(objectNo, usedOSD, data);

//        if (!file.isReplicating())
//            fileCompleted(file.fileID);
    }

    /**
     * process all necessary actions, because object could not be fetched
     * 
     * @param usedOSD
     */
    public void objectNotFetched(String fileID, final ServiceUUID usedOSD, long objectNo, InternalObjectData data) {
        ReplicatingFile file = filesInProgress.get(fileID);
        assert (file != null);
        
//        // monitoring
//        if (Monitoring.isEnabled())
//            monitoring.putIncreaseForLong(MONITORING_KEY_UNNECESSARY_REQUESTS, 1l);

        file.objectNotFetched(objectNo, usedOSD, data);

        if (!file.isReplicating())
            fileCompleted(file.fileID);
    }

    public void objectNotFetchedBecauseError(String fileID, final ServiceUUID usedOSD, long objectNo, final ErrorResponse error) {
        ReplicatingFile file = filesInProgress.get(fileID);
        assert (file != null);
        
//        // monitoring
//        if (Monitoring.isEnabled())
//            monitoring.putIncreaseForLong(MONITORING_KEY_UNNECESSARY_REQUESTS, 1l);

        file.objectNotFetchedBecauseError(objectNo, usedOSD, error);

        if (!file.isReplicating())
            fileCompleted(file.fileID);
    }

    public void objectNotFetchedBecauseViewError(String fileID, final ServiceUUID usedOSD, long objectNo,
            final ErrorResponse error) {
        ReplicatingFile file = filesInProgress.get(fileID);
        assert (file != null);

        // // monitoring
        // if (Monitoring.isEnabled())
        // monitoring.putIncreaseForLong(MONITORING_KEY_UNNECESSARY_REQUESTS, 1l);

        file.objectNotFetchedBecauseViewError(objectNo, usedOSD, error);

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

//        // monitoring
//        if (Monitoring.isEnabled() && completedFile.isFullReplica) {
//            long requiredTime = System.currentTimeMillis()
//                    - monitoring.getLong(MONITORING_KEY_REQUIRED_TIME_FOR_COMPLETING_REPLICA + fileID);
//            monitoring.putLong(MONITORING_KEY_REQUIRED_TIME_FOR_COMPLETING_REPLICA + fileID,
//                    requiredTime);
//            // FIXME: test stuff
//            MonitoringLog.monitor(MONITORING_KEY_REQUIRED_TIME_FOR_COMPLETING_REPLICA + fileID, Long
//                    .toString(requiredTime));
//            // all monitoring keys who are not overwritten (+fileID) must be removed from map, otherwise it
//            // will overflow
//            monitoring.remove(MONITORING_KEY_REQUIRED_TIME_FOR_COMPLETING_REPLICA + fileID);
//        }
//        // FIXME: test stuff
//        Long overhead = monitoring.getLong(MONITORING_KEY_OBJECT_LIST_OVERHEAD);
//        if (overhead != null)
//            MonitoringLog.monitor(MONITORING_KEY_OBJECT_LIST_OVERHEAD, overhead.toString());
//        Long unnecessaryRequests = monitoring.getLong(MONITORING_KEY_UNNECESSARY_REQUESTS);
//        if (unnecessaryRequests != null)
//            MonitoringLog.monitor(MONITORING_KEY_UNNECESSARY_REQUESTS, unnecessaryRequests.toString());

        /*
         * Optimization: Canceled files will be never reused. Because in most cases they are canceled due to
         * the removal of the local replica.
         */
        if(completedFile.isStopped()) {
            // do not cache canceled files
            // remove canceled file from cache
            lastCompletedFilesCache.remove(fileID);
        } else {
            // cache completed file
            lastCompletedFilesCache.put(fileID, completedFile);
        }

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "%s - stop replicating file",
                    fileID);

        // update allowed max objects in progress for all files (load-balancing)
        if (filesInProgress.size() == 0)
            ReplicatingFile.setMaxObjectsInProgressPerFile(MAX_OBJECTS_IN_PROGRESS_OVERALL / 1);
        else
            ReplicatingFile.setMaxObjectsInProgressPerFile(MAX_OBJECTS_IN_PROGRESS_OVERALL / filesInProgress.size());

        // TODO: save persistent marker that all objects of file are completely replicated, if it is a full replica
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
     * @param objectSetBytes
     * 
     */
    public void objectSetFetched(String fileID, ServiceUUID osd, ObjectSet objectSet, long objectSetBytes) {
        // TODO: find more handsome way for notifying about list-size
        ReplicatingFile file = filesInProgress.get(fileID);
        if (file != null) {
            file.objectSetFetched(osd, objectSet);
            
//            // monitoring
//            if(Monitoring.isEnabled())
//                monitoring.putIncreaseForLong(MONITORING_KEY_OBJECT_LIST_OVERHEAD, objectSetBytes);
        }
    }

    /**
     * sends an error to all belonging clients of this file (for all objects)
     */
    public void sendError(String fileID, ErrorResponse e) {
        ReplicatingFile file = filesInProgress.get(fileID);
        assert (file != null);

        file.reportError(e);
    }

    public void shutdown() {
        if (Monitoring.isEnabled()) {
            if (monitoringThread != null)
                monitoringThread.interrupt();
        }
    }

    /**
     * @param fileId
     */
    public void startNewReplication(String fileID) {
        ReplicatingFile file = filesInProgress.get(fileID);
        if (file != null)
            try {
                file.startNewReplication();
                if (!file.isReplicating())
                    fileCompleted(file.fileID);
            } catch (TransferStrategyException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }
}
