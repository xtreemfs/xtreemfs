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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.interfaces.utils.ONCRPCException;

/**
 * 
 * <br>
 * 22.06.2009
 */
class Writer implements Runnable {
    static int                                     SLEEP_TIME_UNTIL_NEW_FILE_WILL_BE_WRITTEN = 1000 * 60 * 10; // 10
    // minutes
    /**
     * max this number of replicas will be added/remove all at once
     */
    static final int                               MAX_REPLICA_CHURN                         = 4;
    static int                                     HOLE_PROPABILITY                          = 10;            // 10%
    // chance

    protected final RPCNIOSocketClient             client;
    protected final InetSocketAddress              mrcAddress;

    protected final CopyOnWriteArrayList<TestFile> fileList;
    protected Random                               random;
    protected int                                  replicationFlags;

    /**
     * @param client
     * @param fileList
     * @param random
     * @param replicationFlags
     */
    public Writer(RPCNIOSocketClient client, CopyOnWriteArrayList<TestFile> fileList, Random random,
            int replicationFlags) {
        this.client = client;
        this.fileList = fileList;
        this.random = random;
        this.replicationFlags = replicationFlags;
        this.mrcAddress = TestFile.mrcAddress;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("WriterThread");
        try {
            // write 5 files at startup
            for (int i = 1; i <= 6; i++) {
                // create new file
                TestFile file = writeFile(HOLE_PROPABILITY);
                prepareReplication(file.filename);

                fileList.add(file);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        // change some details (replicas, new files) from time to time
        while (!Thread.interrupted()) {
            try {
                // create new file
                TestFile file = writeFile(HOLE_PROPABILITY);
                prepareReplication(file.filename);

                fileList.add(file);

                // add/remove replicas for existing files
                boolean switch1 = false;
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(SLEEP_TIME_UNTIL_NEW_FILE_WILL_BE_WRITTEN / 10);

                    if (switch1) {
                        // add replicas
                        for (int j = 0; j < fileList.size() * 0.5; j++) { // change 1/2 of files
                            file = fileList.get(random.nextInt(fileList.size()));
                            addReplicas(file.filename, random.nextInt(MAX_REPLICA_CHURN) + 1);
                        }
                    } else {
                        // remove replicas
                        for (int j = 0; j < fileList.size() * 0.5; j++) { // change 1/2 of files
                            file = fileList.get(random.nextInt(fileList.size()));
                            removeReplicas(file.filename, random.nextInt(MAX_REPLICA_CHURN) + 1);
                        }
                    }
                    switch1 = !switch1;
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * fill files with data (different sizes)
     * 
     * @param holePropability
     *            TODO
     */
    protected TestFile writeFile(int holePropability) throws Exception {
        String filename = TestFile.getNewFileName();
        TreeSet<Long> holes = new TreeSet<Long>();

        double factor = random.nextDouble();
        factor = (factor + 0.2 < 1) ? (factor + 0.2) : factor; // rather bigger filesizes
        long filesize = Math.round(TestFile.diskFileFilesize * factor);

        java.io.RandomAccessFile in = null;
        RandomAccessFile raf = null;
        try {
            in = new java.io.RandomAccessFile(TestFile.diskDir + TestFile.DISK_FILENAME, "rw");

            assert (filesize <= in.length());

            byte[] data;

            // create file in xtreemfs
            raf = new RandomAccessFile("rw", mrcAddress, TestFile.VOLUME_NAME
                    + TestFile.DIR_PATH + filename, client, TestFile.userCredentials);

            if (filesize < StressTest.PART_SIZE) {
                // read and write the WHOLE data
                data = new byte[(int) filesize];
                in.read(data);
                raf.write(data, 0, data.length);
            } else {
                while (raf.getFilePointer() + StressTest.PART_SIZE < filesize) {
                    if (random.nextInt(100) > holePropability) {
                        // read and write A piece of data
                        data = new byte[StressTest.PART_SIZE];
                        in.read(data);
                        // write data to file
                        raf.write(data, 0, data.length);
                    } else { // skip writing => hole
                        holes.add(raf.getFilePointer());
                        in.seek(raf.getFilePointer() + StressTest.PART_SIZE);
                        raf.seek(raf.getFilePointer() + StressTest.PART_SIZE);
                    }
                    assert (in.getFilePointer() == raf.getFilePointer());
                }
                if (raf.getFilePointer() < filesize) {
                    // read and write LAST piece of data
                    data = new byte[(int) (filesize - raf.getFilePointer())];
                    in.read(data);
                    // write data to file
                    raf.write(data, 0, data.length);
                }
                assert (in.getFilePointer() == raf.getFilePointer());
            }

            // ASSERT correct filesize
            int mrcFilesize = (int) raf.length();
            if (filesize != mrcFilesize)
                Logging.logMessage(Logging.LEVEL_ERROR, Category.test, this, "ERROR: Filesize of file "
                        + filename + " is not correctly written. It should be " + filesize + " instead of "
                        + mrcFilesize + ".");

            Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this,
                    "file %s with a filesize of %d MB successfully written.", filename,
                    mrcFilesize / 1024 / 1024);
        } finally {
            if (in != null)
                in.close();
            if (raf != null)
                raf.close();
        }
        return new TestFile(filename, filesize, holes);
    }

    /**
     * set file read only and add replicas
     */
    protected void prepareReplication(String fileName) throws Exception {
        RandomAccessFile raf = new RandomAccessFile("r", mrcAddress, TestFile.VOLUME_NAME + TestFile.DIR_PATH
                + fileName, client, TestFile.userCredentials);

        raf.setReadOnly(true);

        addReplicas(fileName, random.nextInt(MAX_REPLICA_CHURN) + 1);
    }

    /**
     * @throws IOException
     * @throws InterruptedException
     * @throws ONCRPCException
     * 
     */
    protected void addReplicas(String fileName, int number) throws Exception {
        int added = 0;
        RandomAccessFile raf = new RandomAccessFile("r", mrcAddress, TestFile.VOLUME_NAME + TestFile.DIR_PATH
                + fileName, client, TestFile.userCredentials);
        for (int i = 0; i < number; i++) {
            // get OSDs for a replica
            List<ServiceUUID> replica = raf.getSuitableOSDsForAReplica();

            // enough OSDs available?
            if (replica.size() >= raf.getStripingPolicy().getWidth()) {
                Collections.shuffle(replica, random);
                replica = replica.subList(0, raf.getStripingPolicy().getWidth());
                raf.addReplica(replica, raf.getStripingPolicy(), replicationFlags);
                added++;
            } else
                break;
        }
        Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this, added + " replicas added for file "
                + fileName + " (number of replicas : " + raf.getXLoc().getNumReplicas() + ")");
    }

    /**
     * @throws IOException
     * @throws InterruptedException
     * @throws ONCRPCException
     * 
     */
    protected void removeReplicas(String fileName, int number) throws Exception {
        int removed = 0;
        RandomAccessFile raf = new RandomAccessFile("r", mrcAddress, TestFile.VOLUME_NAME + TestFile.DIR_PATH
                + fileName, client, TestFile.userCredentials);
        for (int i = 0; i < number; i++) {
            // only the original replica is remaining
            if (raf.getXLoc().getReplicas().size() <= 1)
                break;

            // select any replica, except the replica marked as full
            Replica replica;
            do {
                int replicaNumber = random.nextInt(raf.getXLoc().getReplicas().size());
                // replicaNumber = (replicaNumber == 0) ? 1 : replicaNumber;
                replica = raf.getXLoc().getReplicas().get(replicaNumber);
            } while (replica.isComplete());

            raf.removeReplica(replica);
            removed++;
        }
        boolean containsAtLeastOneFullReplica = false;
        for (Replica replica : raf.getXLoc().getReplicas()) {
            containsAtLeastOneFullReplica = replica.isComplete() || containsAtLeastOneFullReplica;
        }
        assert (containsAtLeastOneFullReplica);
        if (!containsAtLeastOneFullReplica)
            throw new Exception("The full replica has been deleted. This will cause errors.");

        Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this, removed + " replicas removed for file "
                + fileName + " (number of replicas : " + raf.getXLoc().getNumReplicas() + ")");
    }
}
