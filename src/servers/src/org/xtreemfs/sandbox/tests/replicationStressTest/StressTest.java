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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.monitoring.Monitoring;
import org.xtreemfs.common.util.ONCRPCServiceURL;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.replication.transferStrategies.RandomStrategy;
import org.xtreemfs.utils.CLIParser;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 * A not ending long-run stress test which creates replicas and read the data with x clients. <br>
 * 24.02.2009
 * 
 * @author clorenz
 */
public class StressTest {
    public static final String        HELP                         = "h";

    public static final String        DEBUG                        = "-debug";

    public static final int           DIR_ADDRESS_ARG_POSITION     = 0;

    public static final int           TMP_DIR_ARG_POSITION         = 1;

    public static final int           READER_TYPE_ARG_POSITION     = 2;

    public static final String        THREAD_NUMBER                = "n";

    public static final String        MAX_FILESIZE                 = "s";

    public static final String        STRIPE_WIDTH                 = "w";

    public static final String        RANDOM_SEED                  = "-seed";

    public static final String        START_READERS_ONLY           = "-readers";

    public static final String        FULL_REPLICA                 = "-full";                        // otherwise ondemand

    public static final String        TIME_TILL_NEW_FILE           = "-time";                        // otherwise ondemand

    public static final String        TRANSFER_STRATEGY            = "-strategy";

    public static final String        TRANSFER_STRATEGY_RANDOM     = "random";

    public static final String        TRANSFER_STRATEGY_SEQUENTIAL = "sequential";

    public static final String        READER_TYPE_ONDEMAND         = "ondemand";

    public static final String        READER_TYPE_FULL             = "full";

    public static final int           DEFAULT_THREAD_NUMBER        = 2;

    public static final int           DEFAULT_MAX_FILESIZE         = 100 * 1024 * 1024;              // 100 MB

    public static final int           DEFAULT_STRIPE_WIDTH         = 1;

    public static final int           DEFAULT_RANDOM_SEED          = 123;

    public static final String        DEFAULT_REPLICA_TYPE         = "ondemand";

    public static final int           DEFAULT_TRANSFER_STRATEGY    = RandomStrategy.REPLICATION_FLAG;

    /**
     * biggest generated data in RAM (piecewise write of file)
     */
    static final int                  PART_SIZE                    = 1024 * 1024;                    // 1MB

    static final int                  STRIPE_SIZE                  = 128;                            // KB

    private static TimeSync           timeSync;
    private static RPCNIOSocketClient client;
    private static Random             random;

    /**
     * if a test fails this value is set to true
     */
    static boolean                    containedErrors              = false;

    public static InetSocketAddress getMRC(DIRClient dirClient, String volumeName) throws ONCRPCException,
            IOException, InterruptedException {
        ServiceSet sSet;
        // get MRC address
        if (volumeName == null) { // get a MRC for a new volume
            RPCResponse<ServiceSet> r = dirClient.xtreemfs_service_get_by_type(null,
                    ServiceType.SERVICE_TYPE_MRC);
            sSet = r.get();
            r.freeBuffers();
        } else { // get the MRC responsible for the given volume
            RPCResponse<ServiceSet> r = dirClient.xtreemfs_service_get_by_uuid(null, volumeName);
            sSet = r.get();
            r.freeBuffers();
        }

        InetSocketAddress mrcAddress;
        if (sSet.size() != 0)
            mrcAddress = new ServiceUUID(sSet.get(0).getUuid()).getAddress();
        else
            throw new IOException("Cannot find a MRC.");

        return mrcAddress;
    }

    public static void initializeVolume(MRCClient mrcClient, String volumeName, String dirPath,
            int stripeWidth) throws ONCRPCException, IOException, InterruptedException {

        // create a volume (no access control)
        RPCResponse r2 = mrcClient.mkvol(null, TestFile.userCredentials, volumeName,
                OSDSelectionPolicyType.OSD_SELECTION_POLICY_SIMPLE.intValue(), new StripingPolicy(
                        StripingPolicyType.STRIPING_POLICY_RAID0, STRIPE_SIZE, stripeWidth),
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL.intValue(), 0);
        r2.get();
        r2.freeBuffers();

        // create a directory
        r2 = mrcClient.mkdir(null, TestFile.userCredentials, volumeName + dirPath, 0);
        r2.get();
        r2.freeBuffers();

        Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, mrcClient, "test volume initialized");
    }

    /**
     * fill tmp-file with data
     */
    public static void writeTmpFileToDisk(String filepath, long maxFilesize) throws Exception {
        java.io.RandomAccessFile out = null;
        try {
            out = new java.io.RandomAccessFile(filepath, "rw");

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

            assert (out.length() == maxFilesize);
        } finally {
            if (out != null)
                out.close();
        }
    }

    /*
     * copied from test...SetupUtils
     */
    /**
     * @param size
     *            in byte
     */
    private static byte[] generateData(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) throws Exception {
        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(3);
        options.put(HELP, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(DEBUG, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(THREAD_NUMBER, new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put(MAX_FILESIZE, new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put(STRIPE_WIDTH, new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put(RANDOM_SEED, new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put(START_READERS_ONLY, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(FULL_REPLICA, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(TRANSFER_STRATEGY, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(TIME_TILL_NEW_FILE, new CliOption(CliOption.OPTIONTYPE.NUMBER));

        try {
            CLIParser.parseCLI(args, options, arguments);
        } catch (Exception exc) {
            System.out.println(exc);
            usage();
            return;
        }

        CliOption h = options.get(HELP);
        if (h.switchValue) {
            usage();
            return;
        }

        // mandatory arguments
        if (arguments.size() < 3 || arguments.size() > 3) {
            usage();
            return;
        }

        // parse dir-address
        final ONCRPCServiceURL dirURL = new ONCRPCServiceURL(arguments.get(0));
        final InetSocketAddress dirAddress = new InetSocketAddress(dirURL.getHost(), dirURL.getPort());

        // parse tmp-dir
        String tmpDir = arguments.get(TMP_DIR_ARG_POSITION);

        // parse reader-type
        String readersType = arguments.get(READER_TYPE_ARG_POSITION);

        // parse options
        boolean debug = false;
        int threadNo = DEFAULT_THREAD_NUMBER;
        int maxFilesize = DEFAULT_MAX_FILESIZE; // in bytes
        int stripeWidth = DEFAULT_STRIPE_WIDTH;
        random = new Random(DEFAULT_RANDOM_SEED);
        boolean readersOnly = false;
        int replicationFlags = ReplicationFlags.setPartialReplica(0);
        int timeTillNewFile = -1;
        for (Entry<String, CliOption> e : options.entrySet()) {
            if (e.getKey().equals(DEBUG) && e.getValue().switchValue) {
                debug = e.getValue().switchValue;
            }
            if (e.getKey().equals(THREAD_NUMBER) && e.getValue().numValue != null) {
                threadNo = e.getValue().numValue.intValue();
            }
            if (e.getKey().equals(MAX_FILESIZE) && e.getValue().numValue != null) {
                maxFilesize = e.getValue().numValue.intValue() * 1024 * 1024;
            }
            if (e.getKey().equals(STRIPE_WIDTH) && e.getValue().numValue != null) {
                stripeWidth = e.getValue().numValue.intValue();
            }
            if (e.getKey().equals(RANDOM_SEED) && e.getValue().numValue != null) {
                random = new Random(e.getValue().numValue.intValue());
            }
            if (e.getKey().equals(START_READERS_ONLY) && e.getValue().switchValue) {
                readersOnly = e.getValue().switchValue;
            }
            if (e.getKey().equals(FULL_REPLICA) && e.getValue().switchValue) {
                replicationFlags = 0; // default
            }
            if (e.getKey().equals(TIME_TILL_NEW_FILE) && e.getValue().numValue != null) {
                timeTillNewFile = e.getValue().numValue.intValue();
            }
            if (e.getKey().equals(TRANSFER_STRATEGY) && e.getValue().stringValue != null) {
                if (e.getValue().stringValue.equals(TRANSFER_STRATEGY_RANDOM))
                    replicationFlags = ReplicationFlags.setRandomStrategy(replicationFlags);
                else if (e.getValue().stringValue.equals(TRANSFER_STRATEGY_SEQUENTIAL))
                    replicationFlags = ReplicationFlags.setSequentialStrategy(replicationFlags);
            } else {
                replicationFlags = replicationFlags | DEFAULT_TRANSFER_STRATEGY;
            }
        }

        /*
         * parsing done => start logic
         */
        if (debug)
            Logging.start(Logging.LEVEL_DEBUG, Category.test, Category.replication);
        else
            Logging.start(Logging.LEVEL_INFO, Category.test, Category.replication);
        
        Monitoring.enable();

        // start important services
        // client
        client = new RPCNIOSocketClient(null, 10000, 5 * 60 * 1000);
        client.start();
        client.waitForStartup();
        DIRClient dirClient = new DIRClient(client, dirAddress);

        // start services
        timeSync = TimeSync.initialize(dirClient, 60 * 1000, 50);
        timeSync.waitForStartup();
        
        UUIDResolver.start(dirClient, 1000, 10 * 10 * 1000);

        // write filedata to disk
        new File(tmpDir).mkdirs();
        writeTmpFileToDisk(tmpDir + TestFile.DISK_FILENAME, maxFilesize);

        // get MRC
        InetSocketAddress mrcAddress;
        if (!readersOnly)
            // ... and initialize volume
            mrcAddress = getMRC(dirClient, null);
        else
            mrcAddress = getMRC(dirClient, TestFile.VOLUME_NAME);
        MRCClient mrcClient = new MRCClient(client, mrcAddress);
        initializeVolume(mrcClient, TestFile.VOLUME_NAME, TestFile.DIR_PATH, stripeWidth);

        // set File attributes
        TestFile.diskFileFilesize = maxFilesize;
        TestFile.mrcAddress = mrcAddress;
        TestFile.diskDir = tmpDir;
        if (timeTillNewFile != -1)
            Writer.SLEEP_TIME_UNTIL_NEW_FILE_WILL_BE_WRITTEN = timeTillNewFile;

        // create file list
        CopyOnWriteArrayList<TestFile> fileList = new CopyOnWriteArrayList<TestFile>();

        Thread writerThread = null;
        if (!readersOnly) {
            if (readersType.equals(READER_TYPE_FULL))
                Writer.HOLE_PROPABILITY = 0;
            // create writing thread
            writerThread = new Thread(new Writer(client, fileList, new Random(random.nextInt()),
                    replicationFlags));
            // start writer thread
            writerThread.start();
        }

        Thread[] readerThreads = new Thread[threadNo];
        // create reading threads
        for (int i = 0; i < threadNo; i++) {
            if (readersType.equals(READER_TYPE_FULL)) {
                readerThreads[i] = new Thread(new FullReplicaReader(fileList, new Random(random.nextLong()),
                        i));
            } else if (readersType.equals(READER_TYPE_ONDEMAND)) {
                readerThreads[i] = new Thread(new OnDemandReader(fileList, new Random(random.nextLong()), i));
            }
        }

        // start reader threads
        for (Thread thread : readerThreads) {
            thread.start();
        }

        // wait until all threads have ended
        for (Thread thread : readerThreads){
            thread.join();
        }

        // shutdown ALL
        // stop writer thread
        if (writerThread != null)
            writerThread.interrupt();
        // stop reader threads
        for (Thread thread : readerThreads) {
            thread.interrupt();
        }
        if (client != null) {
            client.shutdown();
            client.waitForShutdown();
        }
        UUIDResolver.shutdown();
        if (timeSync != null) {
            timeSync.shutdown();
            timeSync.waitForShutdown();
        }

        System.out.println("##############################################################################");
        if (StressTest.containedErrors)
            System.out.println("Test contained Errors. See log for details.");
        else
            System.out.println("Test quits without Errors. Great!");
    }

    public static void usage() {
        StringBuffer out = new StringBuffer();
        out.append("Usage: java -cp <xtreemfs-jar> " + StressTest.class.getCanonicalName());
        out.append(" [options] <DIR-address> <tmp-directory> <reader-type>\n");
        out.append("options:\n");
        out.append("\t-" + THREAD_NUMBER + ":\t\t\t\t number of threads/clients (default: " + DEFAULT_THREAD_NUMBER
                + ")\n");
        out.append("\t-" + MAX_FILESIZE + ":\t\t\t\t max filesize (in MB) (default: " + DEFAULT_MAX_FILESIZE
                + "MB)\n");
        out.append("\t-" + STRIPE_WIDTH + ":\t\t\t\t stripe width / number of OSDs per file (default: "
                + DEFAULT_STRIPE_WIDTH + ")\n");
        out.append("\t-" + DEBUG + ":\t\t\t debug output\n");
        out.append("\t-" + RANDOM_SEED + ":\t\t\t\t seed of used random\n");
        out.append("\t-" + START_READERS_ONLY + ":\t\t\t starts only the readers, no writer\n");
        out.append("\t-" + FULL_REPLICA
                + ":\t\t\t\t if set the replica will be a full replica (otherwise an ondemand replica)\n");
        out.append("\t-" + TIME_TILL_NEW_FILE + ":\t\t\t\t sets the time to wait till a new file will be written\n");
        out.append("\t-" + TRANSFER_STRATEGY
                + " <transfer strategy>:\t chooses the used transfer strategy (default: random)\n");
        out.append("reader-types:\n");
        out.append("\t" + READER_TYPE_ONDEMAND + ":\t for ondemand replicas\n");
        out.append("\t" + READER_TYPE_FULL + ":\t\t for full replicas (you must also set the option -"
                + FULL_REPLICA + ")\n");
        out.append("transfer-strategies:\n");
        out.append("\t" + TRANSFER_STRATEGY_RANDOM
                        + ":\t\t random selection of objects and OSDs (default)\n");
        out.append("\t" + TRANSFER_STRATEGY_SEQUENTIAL + ":\t sequential selection of objects and OSDs\n");
        out.append("NOTE: MAYBE THIS TEST NEVER ENDS!\n");
        System.out.println(out.toString());
        System.exit(1);
    }
}
