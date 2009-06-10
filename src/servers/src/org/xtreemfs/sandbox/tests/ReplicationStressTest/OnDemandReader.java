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
package org.xtreemfs.sandbox.tests.ReplicationStressTest;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.interfaces.UserCredentials;

/**
 * a reader for reading replicas which are marked as ondemand replicas (preferred)
 * <br>
 * 08.06.2009
 */
public class OnDemandReader extends ReplicationStressTestReader {
    /**
     * @throws Exception
     * 
     */
    public OnDemandReader(int threadNo, InetSocketAddress mrcAddress,
            CopyOnWriteArrayList<FileInfo> fileList, Random random, UserCredentials userCredentials)
            throws Exception {
        super(threadNo, mrcAddress, fileList, random, userCredentials);
    }

    /**
     * read/replicate files
     * 
     * @return throughput
     */
    public int readFile(FileInfo file) throws Exception {
        int partSize = (int) (ReplicationStressTest.PART_SIZE * 5 * random.nextDouble()); // random partsize
        long timeRequiredForReading = 0;

        java.io.RandomAccessFile originalFile = null;
        try {
            originalFile = new java.io.RandomAccessFile(ReplicationStressTest.tmpDir
                    + ReplicationStressTest.tmpFilename, "r");
            RandomAccessFile raf = new RandomAccessFile("r", mrcAddress, ReplicationStressTest.VOLUME_NAME
                    + ReplicationStressTest.DIR_PATH + file.filename, client, userCredentials);

            long filesize = raf.length();

            // prepare ranges for reading file
            List<Long> startOffsets = new LinkedList<Long>();
            for (long startOffset = 0; startOffset < filesize; startOffset = startOffset + partSize + 1) {
                startOffsets.add(startOffset);
            }

            // shuffle list for non straight forward reading
            Collections.shuffle(startOffsets, random);

            // read file
            for (Long startOffset : startOffsets) {
                byte[] result = new byte[partSize];
                byte[] expectedResult = new byte[partSize];

                // read
                try {
                    // monitoring: time (latency/throughput)
                    long timeBefore = System.currentTimeMillis();
                    file.readFromXtreemFS(result, raf, startOffset);
                    timeRequiredForReading += System.currentTimeMillis() - timeBefore;
                } catch (Exception e) {
                    // TODO: catch exception, if request is rejected because of change of XLocations version
                    ReplicationStressTest.containedErrors = true;
                    file.readFromDisk(expectedResult, originalFile, startOffset, filesize);

                    System.out.println(e.getMessage());
                    log(e.getCause().toString(), file, startOffset, startOffset + partSize, filesize, result,
                            expectedResult);
                    StringBuffer s = new StringBuffer();
                    for (Replica replica : raf.getXLoc().getReplicas()) {
                        s.append(replica.getOSDs().toString());
                    }
                    System.out.println(s.toString());
                    System.out.println("number of replicas:\t" + raf.getXLoc().getNumReplicas());

                    continue;
                }

                // ASSERT the byte-data
                file.readFromDisk(expectedResult, originalFile, startOffset, filesize);
                if (!Arrays.equals(result, expectedResult)) {
                    ReplicationStressTest.containedErrors = true;
                    log("Read wrong data.", file, startOffset, startOffset + partSize, filesize, result,
                            expectedResult);
                }
            }
            return (int) ((filesize / 1024) / (timeRequiredForReading / 1000)); // KB/s
        } finally {
            if (originalFile != null)
                originalFile.close();
        }
    }
}
