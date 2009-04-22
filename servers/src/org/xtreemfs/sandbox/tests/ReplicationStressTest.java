/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
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
package org.xtreemfs.sandbox.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.client.MRCClient;

/**
 * A not ending long-run stress test which creates replicas and read the data with x clients.
 * 24.02.2009
 * 
 * @author clorenz
 */
public class ReplicationStressTest {
    public static String tmpDir;
    public static final String tmpFilename = "replicatedFile";
    public static final String VOLUME_NAME = "replicationTestVolume";
    public static final String DIR_PATH = "/replicationTest/";

    private static int fileNumber = 0;

    public static String getFileName() {
        return "test" + fileNumber++;
    }

    /**
     * biggest generated data in RAM (piecewise write of file)
     */
    public final int PART_SIZE = 1024 * 1024; // 1MB
    public static final int STRIPE_SIZE = 128; // KB

    public final int SLEEP_TIME = 1000 * 30; // FIXME: 10 minutes

    /*
     * reader-threads
     */
    public class ReaderThreads implements Runnable {
        /**
         * 
         */
        public final int NUMBER_OF_RANGES = 20;
        public final int SLEEP_TIME = 1000 * 10; // FIXME: sleep 1 minute

        private final UserCredentials userCredentials;
        private final RPCNIOSocketClient client;
        private InetSocketAddress mrcAddress;
        int threadNo;

        private CopyOnWriteArrayList<String> fileList;
        private Random random;

        public ReaderThreads(int threadNo, InetSocketAddress mrcAddress,
                CopyOnWriteArrayList<String> fileList, Random random, UserCredentials userCredentials)
                throws Exception {
            this.mrcAddress = mrcAddress;
            this.fileList = fileList;
            this.random = random;
            this.userCredentials = userCredentials;
            this.threadNo = threadNo;

            client = new RPCNIOSocketClient(null, 10000, 5 * 60 * 1000);
            client.start();
            client.waitForStartup();            
        }

        public void shutdown() throws Exception {
            client.shutdown();
            client.waitForShutdown();
        }

        @Override
        public void run() {
            Thread.currentThread().setName("ReaderThread " + threadNo);
            Logging.logMessage(Logging.LEVEL_ERROR, this, Thread.currentThread().getName() + " started");

            int fileCounter = 0;
            while (!Thread.interrupted()) {
                try {
                    while (fileList.isEmpty())
                        Thread.sleep(SLEEP_TIME);

                    // get any file from list
                    String fileName = fileList.get(random.nextInt(fileList.size()));
                    readFile(fileName);
                    
                    if (++fileCounter % 100 == 0)
                        Logging.logMessage(Logging.LEVEL_ERROR, this, Thread.currentThread().getName()
                                + " has read " + fileCounter + " files");
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
            // shutdown
            try {
                shutdown();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        /**
         * read/replicate files
         */
        public void readFile(String fileName) throws Exception {
            int partSize = PART_SIZE * 2;
            java.io.RandomAccessFile originalFile = null;
            try {
                originalFile = new java.io.RandomAccessFile(tmpDir + tmpFilename, "r");

                RandomAccessFile raf = new RandomAccessFile("r", mrcAddress, VOLUME_NAME + DIR_PATH
                        + fileName, client, userCredentials);
                long filesize = raf.length();

                // prepare ranges for reading file
                List<Integer> startOffsets = new LinkedList<Integer>();
                for (int startOffset = 0; startOffset < filesize; startOffset = startOffset + partSize + 1) {
                    startOffsets.add(startOffset);
                }

                // shuffle list for non straight forward reading
                Collections.shuffle(startOffsets, random);

                // read file
                for (Integer startOffset : startOffsets) {
                    byte[] result = new byte[partSize];

                    // read
                    try {
                        raf.seek(startOffset);
                        raf.read(result, 0, result.length);
                    } catch (Exception e) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this, "File " + fileName + " cannot be read.");
                        throw e;
                    }

                    // TODO: monitoring: time (latency)

                    // ASSERT the byte-data
                    byte[] expectedResult = new byte[partSize];
                    originalFile.seek(startOffset);
                    originalFile.read(expectedResult);
                    if (startOffset + partSize > filesize) {
                        // swap data to zeros
                        expectedResult = Arrays.copyOf(expectedResult, (int) (filesize - startOffset));
                        expectedResult = Arrays.copyOf(expectedResult, partSize);
                    }
                    if (!Arrays.equals(result, expectedResult)) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this, "ERROR: Read wrong data in file "
                                + fileName + " from " + startOffset + " to " + (startOffset + partSize)
                                + ". Filesize is " + filesize + ".");
                        System.out.println("first 128 bytes read:\t"
                                + Arrays.toString(Arrays.copyOfRange(result, 0, 128)));
                        System.out.println("first 128 bytes expected:\t"
                                + Arrays.toString(Arrays.copyOfRange(expectedResult, 0, 128)));
                        System.out.println("last 128 bytes read:\t"
                                + Arrays.toString(Arrays.copyOfRange(result, result.length - 128,
                                        result.length)));
                        System.out.println("last 128 bytes expected:\t"
                                + Arrays.toString(Arrays.copyOfRange(expectedResult, result.length - 128,
                                        result.length)));
                    }
                }
            } finally {
                if (originalFile != null)
                    originalFile.close();
            }
        }
    }

    public final UserCredentials userCredentials;

    private final TimeSync timeSync;
    private final RPCNIOSocketClient client;
    private final DIRClient dirClient;
    public MRCClient mrcClient;

    private CopyOnWriteArrayList<String> fileList;
    private Random random;

    /**
     * controller(-thread)
     * 
     * @throws Exception
     */
    public ReplicationStressTest(InetSocketAddress dirAddress, CopyOnWriteArrayList<String> fileList,
            Random random) throws Exception {
        Thread.currentThread().setName("WriterThread");
        Logging.start(Logging.LEVEL_ERROR);

        this.fileList = fileList;
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
        RPCResponse<ServiceSet> r = dirClient.xtreemfs_service_get_by_type(null, ServiceType.SERVICE_TYPE_MRC);
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
                OSDSelectionPolicyType.OSD_SELECTION_POLICY_SIMPLE.intValue(), new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0,
                        STRIPE_SIZE, stripeWidth), AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL.intValue(), 0);
        r2.get();
        r2.freeBuffers();

        // create a directory
        r2 = mrcClient.mkdir(null, userCredentials, VOLUME_NAME + DIR_PATH, 0);
        r2.get();
        r2.freeBuffers();
        
        Logging.logMessage(Logging.LEVEL_ERROR, this, "test volume initialized");
    }

    /**
     * fill tmp-file with data
     */
    public void writeTmpFileToDisk(long maxFilesize) throws Exception {
        java.io.RandomAccessFile out = null;
        try {
            out = new java.io.RandomAccessFile(tmpDir + tmpFilename, "rw");

            long part = 0;
            long filesize = maxFilesize;
            byte[] data;
            if (filesize <= PART_SIZE) {
                // write the WHOLE data
                data = generateData((int) filesize);
                out.write(data);
            } else {
                while (part + PART_SIZE < filesize) {
                    if (random.nextInt(100) > 10) { // 90% chance
                        // write A piece of data
                        data = generateData(PART_SIZE);
                        out.write(data);
                    } else { // skip writing => hole
                        out.seek(out.getFilePointer() + PART_SIZE);
                    }
                    part = part + PART_SIZE;
                }
                if (part < filesize) {
                    // write LAST piece of data
                    data = generateData((int) (filesize - part));
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
    public String writeFile(long maxFilesize) throws Exception {
        String fileName = getFileName();
        Logging.logMessage(Logging.LEVEL_ERROR, this, "write file " + fileName);

        double factor = random.nextDouble();
        factor = (factor + 0.2 < 1) ? (factor + 0.2) : factor; // rather bigger filesizes
        long filesize = Math.round(maxFilesize * factor);

        java.io.RandomAccessFile in = null;
        try {
            in = new java.io.RandomAccessFile(tmpDir + tmpFilename, "rw");
            
            assert(filesize <= in.length());

            int part = 0;
            byte[] data;

            // create file in xtreemfs
            RandomAccessFile raf = new RandomAccessFile("rw", mrcClient.getDefaultServerAddress(),
                    VOLUME_NAME + DIR_PATH + fileName, client, userCredentials);

            if (filesize < PART_SIZE) {
                // read and write the WHOLE data
                data = new byte[(int) filesize];
                in.read(data);
                raf.write(data, 0, data.length);
            } else {
                while (part + PART_SIZE < filesize) {
                    // read and write A piece of data
                    data = new byte[PART_SIZE];
                    in.read(data);
                    // write data to file
                    raf.write(data, 0, data.length);

                    part = part + PART_SIZE;
                }
                if (part < filesize) {
                    // read and write LAST piece of data
                    data = new byte[(int) (filesize - part)];
                    in.read(data);
                    // write data to file
                    raf.write(data, 0, data.length);
                }
            }

            // ASSERT correct filesize
            int mrcFilesize = (int) raf.length();
            if (filesize != mrcFilesize)
                Logging.logMessage(Logging.LEVEL_ERROR, this, "ERROR: Filesize of file " + fileName
                        + " is not correctly written. It should be " + filesize + " instead of "
                        + mrcFilesize + ".");
        } finally {
            if (in != null)
                in.close();
        }
        return fileName;
    }

    /**
     * set file read only and add replicas
     */
    public void prepareReplication(String fileName) throws Exception {
        RandomAccessFile raf = new RandomAccessFile("r", mrcClient.getDefaultServerAddress(), VOLUME_NAME
                + DIR_PATH + fileName, client, userCredentials);

        raf.setReadOnly(true);

        addReplica(fileName, random.nextInt(4));
    }

    /**
     * @throws IOException 
     * @throws InterruptedException 
     * @throws ONCRPCException 
     * 
     */
    private void removeReplicas(String fileName, int number) throws Exception {
        for (int i = 0; i < number; i++) {
            RandomAccessFile raf = new RandomAccessFile("r", mrcClient.getDefaultServerAddress(), VOLUME_NAME
                    + DIR_PATH + fileName, client, userCredentials);

            // only the original replica is remaining
            if(raf.getXLoc().getReplicas().size() <= 1)
                break;

            // select any replica, except the first (original)
            int replicaNumber = random.nextInt(raf.getXLoc().getReplicas().size());
            replicaNumber = (replicaNumber == 0) ? 1 : replicaNumber;
            Replica replica = raf.getXLoc().getReplicas().get(replicaNumber);
            
            raf.removeReplica(replica);
        }
    }

    /**
     * @throws IOException 
     * @throws InterruptedException 
     * @throws ONCRPCException 
     * 
     */
    private void addReplica(String fileName, int number) throws Exception {
        for (int i = 0; i < number; i++) {
            RandomAccessFile raf = new RandomAccessFile("r", mrcClient.getDefaultServerAddress(), VOLUME_NAME
                    + DIR_PATH + fileName, client, userCredentials);
            // get OSDs for a replica
            List<ServiceUUID> replica = raf.getSuitableOSDsForAReplica();
            
            // enough OSDs available?
            if(replica.size() >= raf.getStripingPolicy().getWidth()) {
                Collections.shuffle(replica, random);
                replica = replica.subList(0, raf.getStripingPolicy().getWidth());
                raf.addReplica(replica, raf.getStripingPolicy());
            } else
                break;
        }
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
        CopyOnWriteArrayList<String> fileList = new CopyOnWriteArrayList<String>();

        ReplicationStressTest controller = new ReplicationStressTest(dirAddress, fileList, random);
        controller.initializeVolume(stripeWidth);

        Thread[] readerThreads = new Thread[threadNumber]; 
        // create reading threads
        for (int i = 0; i < threadNumber; i++) {
            readerThreads[i] = new Thread((controller.new ReaderThreads(i, controller.mrcClient
                    .getDefaultServerAddress(), fileList, new Random(random.nextLong()),
                    controller.userCredentials)));
        }

        // write filedata to disk
        controller.writeTmpFileToDisk(maxFilesize);

        // start reading threads
        for (Thread thread : readerThreads) {
            thread.start();
        }

        // change some details (replicas, new files) from time to time
        while (!Thread.interrupted()) {
            try {
                // create new file
                String filename = controller.writeFile(maxFilesize);
                controller.prepareReplication(filename);

                fileList.add(filename);

                // add/remove replicas for existing files
                for(int i = 0; i < 2; i++) { // FIXME
                    Thread.sleep(controller.SLEEP_TIME);

                    for (int j = 0; j < fileList.size(); j++) {
                        String fileName = fileList.get(random.nextInt(fileList.size()));
                        controller.removeReplicas(fileName, random.nextInt(2));
                    }
                    for (int j = 0; j < fileList.size(); j++) {
                        String fileName = fileList.get(random.nextInt(fileList.size()));
                        controller.addReplica(fileName, random.nextInt(2));
                    }
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        // shutdown ALL
        // stop reading threads
        for (Thread thread : readerThreads) {
            thread.interrupt();
        }
        controller.shutdown();
    }

    public static void usage() {
        StringBuffer out = new StringBuffer();
        out.append("Usage: java -cp <xtreemfs-jar> org.xtreemfs.sandbox.tests.ReplicationStressTest ");
        out.append("<DIR-address> <tmp-directory> <number of readers> <max. filesize in MB> <random seed> <stripe width>\n");
        out.append("THIS TEST WILL NEVER END!\n");
        System.out.println(out.toString());
        System.exit(1);
    }
}
