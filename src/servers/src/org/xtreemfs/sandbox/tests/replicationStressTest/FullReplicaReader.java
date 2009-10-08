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
package org.xtreemfs.sandbox.tests.replicationStressTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.monitoring.NumberMonitoring;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.ObjectList;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.replication.ObjectSet;

/**
 * a reader for reading replicas which are marked as full replicas (preferred) <br>
 * 09.06.2009
 */
class FullReplicaReader extends Reader {

    protected OSDClient               osdClient;
    private static HashSet<Replica> completedReplicas = new HashSet<Replica>();

    /**
     * @throws Exception
     * 
     */
    public FullReplicaReader(CopyOnWriteArrayList<TestFile> fileList, Random random, int threadNo)
            throws Exception {
        super(fileList, random, threadNo);
        this.osdClient = new OSDClient(client);
    }

    /**
     * read/replicate files
     */
    public void readFile(TestFile file) throws Exception {
        long timeRequiredForReading = 0;

        java.io.RandomAccessFile originalFile = null;
        RandomAccessFile raf =null;
        try {
            originalFile = new java.io.RandomAccessFile(TestFile.diskDir + TestFile.DISK_FILENAME, "r");
            raf = new RandomAccessFile("r", mrcAddress, TestFile.VOLUME_NAME
                    + TestFile.DIR_PATH + file.filename, client, TestFile.userCredentials);

            long filesize = raf.length();
            StripingPolicyImpl sp = raf.getCurrentlyUsedReplica().getStripingPolicy();
            long lastObject = sp.getObjectNoForOffset(filesize - 1);

            // sleep a short time, so the background replication could begin
            Thread.sleep(SLEEP_TIME);

            /*
             * read only the first 1024byte
             */
            byte[] result = new byte[1024];
            byte[] expectedResult = new byte[1024];

            // read
            try {
                file.readFromXtreemFS(result, raf, 0);
            } catch (Exception e) {
                // TODO: catch exception, if request is rejected because of change of XLocations version
                StressTest.containedErrors = true;
                file.readFromDisk(expectedResult, originalFile, 0, filesize);

                log(e.getCause().toString(), file, 0, 0 + 1024, filesize, result, expectedResult);
            }

            // ASSERT the byte-data
            file.readFromDisk(expectedResult, originalFile, 0, filesize);
            if (!Arrays.equals(result, expectedResult)) {
                StressTest.containedErrors = true;
                log("Read wrong data.", file, 0, 0 + 1024, filesize, result,
                        expectedResult);
            }

            // wait some time so the background replication could work
            Thread.sleep(SLEEP_TIME);

            // check if replica is complete => background replication should work
            // get object lists from all OSDs (by RAF)
            long objects = 0; // sum of all saved objects from all OSDs of this replica
            for (ServiceUUID osd : raf.getCurrentlyUsedReplica().getOSDs()) {
                RPCResponse<ObjectList> response = osdClient.internal_getObjectList(osd.getAddress(), raf
                        .getFileId(), raf.getCredentials());
                ObjectList list = response.get();
                response.freeBuffers();

                ObjectSet objectList = new ObjectSet(list.getStripeWidth(), list.getFirstObjectNo(), list.getSet()
                                .array());
                objects += objectList.size();
            }
            
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this,
                    "Replica %s (Head-OSD) of file %s has replicated %d objects (= %d KB of %d KB).", raf
                            .getCurrentlyUsedReplica().getHeadOsd().toString(), file.filename, objects,
                    (objects * sp.getStripeSizeForObject(1)) / 1024, filesize / 1024);

            if (objects == lastObject + 1 && !raf.getCurrentlyUsedReplica().isComplete()) {
                // replication is completed; all objects are replicated
                if (!completedReplicas.contains(raf.getCurrentlyUsedReplica())) {
                    completedReplicas.add(raf.getCurrentlyUsedReplica());
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this,
                            "Replication of replica %s (Head-OSD) for file %s (all objects) is complete.",
                            raf.getCurrentlyUsedReplica().getHeadOsd().toString(), file.filename);
                }
            } else
                completedReplicas.remove(raf.getCurrentlyUsedReplica());

            // monitoring: time (latency/throughput)
            NumberMonitoring monitoringInfo = raf.getMonitoringInfo();
            // monitor throughput
            super.monitoring.putAverageLong(MONITORING_KEY_THROUGHPUT, 0l); // KB/s
        } finally {
            if (originalFile != null)
                originalFile.close();
            if (raf != null)
                raf.close();
        }
    }
}
