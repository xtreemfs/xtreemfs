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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.sandbox.tests.ReplicationStressTest.ReplicationStressTestReader.FileInfo;

/**
 * A not ending long-run stress test which creates replicas and read the data with x clients. <br>
 * 24.02.2009
 * 
 * @author clorenz
 */
public class ReplicationStressTest {
    static String       tmpDir;
    static final String tmpFilename = "replicatedFile";
    static final String VOLUME_NAME = "replicationTestVolume";
    static final String DIR_PATH    = "/replicationTest/";

    private static int  fileNumber  = 0;

    static String getFileName() {
        return "test" + fileNumber++;
    }

    /**
     * biggest generated data in RAM (piecewise write of file)
     */
    static final int                 PART_SIZE                                 = 1024 * 1024;   // 1MB
    static final int                 STRIPE_SIZE                               = 128;           // KB

    static final int                 SLEEP_TIME_UNTIL_NEW_FILE_WILL_BE_WRITTEN = 1000 * 60 * 10; // 10 minutes
    /**
     * max this number of replicas will be added/remove all at once
     */
    static final int                 MAX_REPLICA_CHURN                         = 4;
    static final int                 HOLE_PROPABILITY                          = 10;            // 10% chance

    final UserCredentials            userCredentials;

    private final TimeSync           timeSync;
    private final RPCNIOSocketClient client;
    private final DIRClient          dirClient;
    MRCClient                        mrcClient;

    private Random                   random;

    static boolean                   containedErrors                           = false;

    /**
     * controller(-thread)
     * 
     * @throws Exception
     */
    public ReplicationStressTest(InetSocketAddress dirAddress, Random random) throws Exception {
        Thread.currentThread().setName("WriterThread");
        Logging.start(Logging.LEVEL_DEBUG, Category.test, Category.replication);

        this.random = random;

        // user credentials
        StringSet groupIDs = new StringSet();
        groupIDs.add("root");
        userCredentials = new UserCredentials("root", groupIDs, "");

        // client
        client = new RPCNIOSocketClient(null, 10000, 5 * 60 * 1000);
        client.start();
        client.waitForStartup();
        dirClient = new DIRClient(client, dirAddress);

        // start services
        timeSync = TimeSync.initialize(dirClient, 60 * 1000, 50);
        timeSync.waitForStartup();

        UUIDResolver.start(dirClient, 1000, 10 * 10 * 1000);
    }

    public void initializeVolume(int stripeWidth) throws ONCRPCException, IOException, InterruptedException {
        ServiceSet sSet;
        // get MRC address
        RPCResponse<ServiceSet> r = dirClient
                .xtreemfs_service_get_by_type(null, ServiceType.SERVICE_TYPE_MRC);
        sSet = r.get();
        r.freeBuffers();

        InetSocketAddress mrcAddress;
        if (sSet.size() != 0)
            mrcAddress = new ServiceUUID(sSet.get(0).getUuid()).getAddress();
        else
            throw new IOException("Cannot find a MRC.");

        this.mrcClient = new MRCClient(client, mrcAddress);

        // create a volume (no access control)
        RPCResponse r2 = mrcClient.mkvol(null, userCredentials, VOLUME_NAME,
                OSDSelectionPolicyType.OSD_SELECTION_POLICY_SIMPLE.intValue(), new StripingPolicy(
                        StripingPolicyType.STRIPING_POLICY_RAID0, STRIPE_SIZE, stripeWidth),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL.intValue(), 0);
        r2.get();
        r2.freeBuffers();

        // create a directory
        r2 = mrcClient.mkdir(null, userCredentials, VOLUME_NAME + DIR_PATH, 0);
        r2.get();
        r2.freeBuffers();

        Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this, "test volume initialized");
    }

    /**
     * fill tmp-file with data
     */
    public void writeTmpFileToDisk(long maxFilesize) throws Exception {
        java.io.RandomAccessFile out = null;
        try {
            out = new java.io.RandomAccessFile(tmpDir + tmpFilename, "rw");

            long filesize = maxFilesize;
            byte[] data;
            if (filesize <= PART_SIZE) {
                // write the WHOLE data
                data = generateData((int) filesize);
                out.write(data);
            } else {
                while (out.getFilePointer() + PART_SIZE < filesize) {
                    // write A piece of data
                    data = generateData(PART_SIZE);
                    out.write(data);
                }
                if (out.getFilePointer() < filesize) {
                    // write LAST piece of data
                    data = generateData((int) (filesize - out.getFilePointer()));
                    out.write(data);
                }
            }
        } finally {
            if (out != null)
                out.close();
        }
    }

    /**
     * fill files with data (different sizes)
     */
    public FileInfo writeFile(long maxFilesize) throws Exception {
        FileInfo file = new FileInfo(getFileName());
        file.holes = new TreeSet<Long>();
        double factor = random.nextDouble();
        factor = (factor + 0.2 < 1) ? (factor + 0.2) : factor; // rather bigger filesizes
        long filesize = Math.round(maxFilesize * factor);

        java.io.RandomAccessFile in = null;
        try {
            in = new java.io.RandomAccessFile(tmpDir + tmpFilename, "rw");

            assert (filesize <= in.length());

            byte[] data;

            // create file in xtreemfs
            RandomAccessFile raf = new RandomAccessFile("rw", mrcClient.getDefaultServerAddress(),
                    VOLUME_NAME + DIR_PATH + file.filename, client, userCredentials);

            if (filesize < PART_SIZE) {
                // read and write the WHOLE data
                data = new byte[(int) filesize];
                in.read(data);
                raf.write(data, 0, data.length);
            } else {
                while (raf.getFilePointer() + PART_SIZE < filesize) {
                    if (random.nextInt(100) > HOLE_PROPABILITY) {
                        // read and write A piece of data
                        data = new byte[PART_SIZE];
                        in.read(data);
                        // write data to file
                        raf.write(data, 0, data.length);
                    } else { // skip writing => hole
                        file.holes.add(raf.getFilePointer());
                        in.seek(raf.getFilePointer() + PART_SIZE);
                        raf.seek(raf.getFilePointer() + PART_SIZE);
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
                        + file.filename + " is not correctly written. It should be " + filesize
                        + " instead of " + mrcFilesize + ".");

            Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this, "file " + file.filename
                    + " successfully written");
        } finally {
            if (in != null)
                in.close();
        }
        return file;
    }

    /**
     * set file read only and add replicas
     */
    public void prepareReplication(String fileName) throws Exception {
        RandomAccessFile raf = new RandomAccessFile("r", mrcClient.getDefaultServerAddress(), VOLUME_NAME
                + DIR_PATH + fileName, client, userCredentials);

        raf.setReadOnly(true);

        addReplicas(fileName, random.nextInt(MAX_REPLICA_CHURN) + 1);
    }

    /**
     * @throws IOException
     * @throws InterruptedException
     * @throws ONCRPCException
     * 
     */
    private void addReplicas(String fileName, int number) throws Exception {
        int added = 0;
        RandomAccessFile raf = new RandomAccessFile("r", mrcClient.getDefaultServerAddress(), VOLUME_NAME
                + DIR_PATH + fileName, client, userCredentials);
        for (int i = 0; i < number; i++) {
            // get OSDs for a replica
            List<ServiceUUID> replica = raf.getSuitableOSDsForAReplica();

            // enough OSDs available?
            if (replica.size() >= raf.getStripingPolicy().getWidth()) {
                Collections.shuffle(replica, random);
                replica = replica.subList(0, raf.getStripingPolicy().getWidth());
                raf.addReplica(replica, raf.getStripingPolicy(), Constants.REPL_FLAG_STRATEGY_RANDOM
                        | Constants.REPL_FLAG_FILL_ON_DEMAND);
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
    private void removeReplicas(String fileName, int number) throws Exception {
        int removed = 0;
        RandomAccessFile raf = new RandomAccessFile("r", mrcClient.getDefaultServerAddress(), VOLUME_NAME
                + DIR_PATH + fileName, client, userCredentials);
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
            } while (replica.isFull());

            raf.removeReplica(replica);
            removed++;
        }
        boolean containsAtLeastOneFullReplica = false;
        for (Replica replica : raf.getXLoc().getReplicas()) {
            containsAtLeastOneFullReplica = replica.isFull() || containsAtLeastOneFullReplica;
        }
        assert (containsAtLeastOneFullReplica);
        if (!containsAtLeastOneFullReplica)
            throw new Exception("The full replica has been deleted. This will cause errors.");

        Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this, removed + " replicas removed for file "
                + fileName + " (number of replicas : " + raf.getXLoc().getNumReplicas() + ")");
    }

    public void shutdown() throws Exception {
        // shutdown
        if (client != null) {
            client.shutdown();
            client.waitForShutdown();
        }

        UUIDResolver.shutdown();

        if (timeSync != null) {
            timeSync.shutdown();
            timeSync.waitForShutdown();
        }
    }

    /*
     * copied from test...SetupUtils
     */
    /**
     * @param size
     *            in byte
     */
    private byte[] generateData(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) throws Exception {
        final int MANDATORY_ARGS = 6;

        if (args.length < MANDATORY_ARGS || args.length > MANDATORY_ARGS)
            usage();

        int argNumber = 0;
        // parse arguments
        InetSocketAddress dirAddress = new InetSocketAddress(args[argNumber].split(":")[0], Integer
                .parseInt(args[argNumber++].split(":")[1]));
        tmpDir = args[argNumber++];
        // "client" reading threads
        int threadNumber = Integer.parseInt(args[argNumber++]);
        // 
        int maxFilesize = Integer.parseInt(args[argNumber++]) * 1024 * 1024; // MB => byte
        Random random = new Random(Integer.parseInt(args[argNumber++]));
        int stripeWidth = Integer.parseInt(args[argNumber++]);

        // create file list
        CopyOnWriteArrayList<FileInfo> fileList = new CopyOnWriteArrayList<FileInfo>();

        ReplicationStressTest controller = new ReplicationStressTest(dirAddress, random);
        controller.initializeVolume(stripeWidth);

        Thread[] readerThreads = new Thread[threadNumber];
        // create reading threads
        for (int i = 0; i < threadNumber; i++) {
            readerThreads[i] = new Thread(new OnDemandReader(i, controller.mrcClient
                    .getDefaultServerAddress(), fileList, new Random(random.nextLong()),
                    controller.userCredentials));
        }

        // write filedata to disk
        controller.writeTmpFileToDisk(maxFilesize);

        // start reading threads
        for (Thread thread : readerThreads) {
            thread.start();
        }

        // write 4 (5) files at start
        for (int i = 0; i < 4; i++) {
            // create new file
            FileInfo file = controller.writeFile(maxFilesize);
            controller.prepareReplication(file.filename);

            fileList.add(file);
        }

        // change some details (replicas, new files) from time to time
        while (!Thread.interrupted()) {
            try {
                // create new file
                FileInfo file = controller.writeFile(maxFilesize);
                controller.prepareReplication(file.filename);

                fileList.add(file);

                // add/remove replicas for existing files
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(controller.SLEEP_TIME_UNTIL_NEW_FILE_WILL_BE_WRITTEN / 10);

                    for (int j = 0; j < fileList.size() * 0.5; j++) { // change 1/2 of files
                        file = fileList.get(random.nextInt(fileList.size()));
                        controller.removeReplicas(file.filename, random.nextInt(MAX_REPLICA_CHURN) + 1);
                    }
                    for (int j = 0; j < fileList.size() * 0.5; j++) { // change 1/2 of files
                        file = fileList.get(random.nextInt(fileList.size()));
                        controller.addReplicas(file.filename, random.nextInt(MAX_REPLICA_CHURN) + 1);
                    }
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // shutdown ALL
        // stop reading threads
        for (Thread thread : readerThreads) {
            thread.interrupt();
        }
        controller.shutdown();

        System.out
                .println("#####################################################################################################");
        if (ReplicationStressTest.containedErrors)
            System.out.println("Test contained Errors. See log for details.");
        else
            System.out.println("Test quits without Errors. Great!");
    }

    public static void usage() {
        StringBuffer out = new StringBuffer();
        out.append("Usage: java -cp <xtreemfs-jar> org.xtreemfs.sandbox.tests.ReplicationStressTest ");
        out
                .append("<DIR-address> <tmp-directory> <number of readers> <max. filesize in MB> <random seed> <stripe width>\n");
        out.append("THIS TEST WILL NEVER END!\n");
        System.out.println(out.toString());
        System.exit(1);
    }
}
