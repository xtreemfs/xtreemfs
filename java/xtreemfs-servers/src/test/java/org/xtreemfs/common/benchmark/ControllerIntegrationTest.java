package org.xtreemfs.test.common.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.xtreemfs.common.benchmark.BenchmarkUtils.KiB_IN_BYTES;
import static org.xtreemfs.common.benchmark.BenchmarkUtils.MiB_IN_BYTES;
import static org.xtreemfs.foundation.pbrpc.client.RPCAuthentication.authNone;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.benchmark.BenchmarkConfig;
import org.xtreemfs.common.benchmark.BenchmarkConfig.ConfigBuilder;
import org.xtreemfs.common.benchmark.BenchmarkResult;
import org.xtreemfs.common.benchmark.BenchmarkUtils;
import org.xtreemfs.common.benchmark.BenchmarkUtils.BenchmarkType;
import org.xtreemfs.common.benchmark.Controller;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class ControllerIntegrationTest {
    @Rule
    public final TestRule               testLog        = TestHelper.testLog;

    private static DIRRequestDispatcher   dir;
    private static TestEnvironment        testEnv;
    private static DIRConfig              dirConfig;
    private static RPC.UserCredentials    userCredentials;
    private static RPC.Auth               auth           = RPCAuthentication.authNone;
    private static DIRClient              dirClient;
    private static final int              NUMBER_OF_OSDS = 3;
    private static OSDConfig              osdConfigs[];
    private static OSD                    osds[];
    private static MRCRequestDispatcher   mrc2;
    private static String                 dirAddress;

    private ConfigBuilder configBuilder;
    private Controller                    controller;
    private Client                        client;

    @BeforeClass
    public static void initializeTest() throws Exception {
        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));
        Logging.start(Logging.LEVEL_WARN, Logging.Category.tool);

        dirConfig = SetupUtils.createDIRConfig();
        osdConfigs = SetupUtils.createMultipleOSDConfigs(NUMBER_OF_OSDS);

        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.RPC_CLIENT, TestEnvironment.Services.MRC);
        testEnv.start();

        userCredentials = RPC.UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirClient = new DIRClient(new DIRServiceClient(testEnv.getRpcClient(), null),
                new InetSocketAddress[] { testEnv.getDIRAddress() }, 3, 1000);

        osds = new OSD[NUMBER_OF_OSDS];
        for (int i = 0; i < osds.length; i++) {
            osds[i] = new OSD(osdConfigs[i]);
        }
        dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
    }

    @Before
    public void setUp() throws Exception {
        configBuilder = BenchmarkConfig.newBuilder();
        configBuilder.setDirAddress(dirAddress);
        configBuilder.setUserName("test").setGroup("test");
        Options options = new Options();
        client = ClientFactory.createClient(dirAddress, userCredentials, null, options);
        client.start();

        /* check that all volumes have been deleted properly by previous tests (prevent error masking) */
        assertNoVolumes("BenchVolA", "BenchVolB", "BenchVolC");
    }

    @After
    public void tearDown() throws Exception {
        client.shutdown();
    }

    @AfterClass
    public static void shutdownTest() throws Exception {

        for (int i = 0; i < osds.length; i++) {
            if (osds[i] != null) {
                osds[i].shutdown();
            }
        }

        if (mrc2 != null) {
            mrc2.shutdown();
            mrc2 = null;
        }

        testEnv.shutdown();
        dir.shutdown();
        dir.waitForShutdown();
    }

    @Test
    public void testSetupVolumes() throws Exception {
        controller = new Controller(configBuilder.build());

        controller.setupVolumes("BenchVolA", "BenchVolB", "BenchVolC");

        List volumes = Arrays.asList(client.listVolumeNames());
        assertTrue(volumes.contains("BenchVolA"));
        assertTrue(volumes.contains("BenchVolB"));
        assertTrue(volumes.contains("BenchVolC"));

        controller.teardown();
        volumes = Arrays.asList(client.listVolumeNames());
        assertEquals(0, volumes.size());
    }

    @Test
    public void testSequentialBenchmark() throws Exception {
        controller = new Controller(configBuilder.build());
        controller.setupVolumes("BenchVolA", "BenchVolB");
        List<BenchmarkResult> results = controller.startSequentialWriteBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("SEQ_WRITE", 2, 10L * MiB_IN_BYTES, 2, results);
        results = controller.startSequentialReadBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("SEQ_READ", 2, 10L * MiB_IN_BYTES, 2, results);
        controller.teardown();
    }

    @Test
    public void testSequentialBenchmarkSeparatedRuns() throws Exception {
        configBuilder.setNoCleanup();
        BenchmarkConfig config = configBuilder.build();
        controller = new Controller(config);
        controller.setupVolumes("BenchVolA", "BenchVolB");
        List<BenchmarkResult> results = controller.startSequentialWriteBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("SEQ_WRITE", 2, 10L * MiB_IN_BYTES, 2, results);
        controller.teardown();

        controller = new Controller(config);
        controller.setupVolumes("BenchVolA", "BenchVolB");
        results = controller.startSequentialReadBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("SEQ_READ", 2, 10L * MiB_IN_BYTES, 2, results);
        controller.teardown();
        deleteVolumes("BenchVolA", "BenchVolB");
    }

    @Test
    public void testRandomBenchmark() throws Exception {
        configBuilder.setBasefileSizeInBytes(20L * MiB_IN_BYTES);
        controller = new Controller(configBuilder.build());
        controller.setupVolumes("BenchVolA", "BenchVolB");
        List<BenchmarkResult> results = controller.startRandomWriteBenchmark(1L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("RAND_WRITE", 2, 1L * MiB_IN_BYTES, 2, results);
        results = controller.startRandomReadBenchmark(1L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("RAND_READ", 2, 1L * MiB_IN_BYTES, 2, results);
        controller.teardown();
    }

    @Test
    public void testRandomBenchmarkSeparateRuns() throws Exception {
        configBuilder.setBasefileSizeInBytes(20L * MiB_IN_BYTES).setNoCleanup();
        BenchmarkConfig config = configBuilder.build();

        controller = new Controller(config);
        controller.setupVolumes("BenchVolA", "BenchVolB");
        List<BenchmarkResult> results = controller.startRandomWriteBenchmark(1L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("RAND_WRITE", 2, 1L * MiB_IN_BYTES, 2, results);
        controller.teardown();

        controller = new Controller(config);
        controller.setupVolumes("BenchVolA", "BenchVolB");
        results = controller.startRandomReadBenchmark(1L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("RAND_READ", 2, 1L * MiB_IN_BYTES, 2, results);
        controller.teardown();
        deleteVolumes("BenchVolA", "BenchVolB");
    }

    @Test
    public void testFilebasedBenchmark() throws Exception {
        controller = new Controller(configBuilder.build());
        controller.setupVolumes("BenchVolA", "BenchVolB");
        List<BenchmarkResult> results = controller.startFilebasedWriteBenchmark(1L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("FILES_WRITE", 2, 1L * MiB_IN_BYTES, 2, results);
        results = controller.startFilebasedReadBenchmark(1L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("FILES_READ", 2, 1L * MiB_IN_BYTES, 2, results);
        controller.teardown();
    }

    @Test
    public void testFilebasedBenchmarkSeparateRuns() throws Exception {
        configBuilder.setNoCleanup();
        BenchmarkConfig config = configBuilder.build();

        controller = new Controller(config);
        controller.setupVolumes("BenchVolA", "BenchVolB");
        List<BenchmarkResult> results = controller.startFilebasedWriteBenchmark(1L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("FILES_WRITE", 2, 1L * MiB_IN_BYTES, 2, results);
        controller.teardown();

        controller = new Controller(config);
        controller.setupVolumes("BenchVolA", "BenchVolB");
        results = controller.startFilebasedReadBenchmark(1L*BenchmarkUtils.MiB_IN_BYTES, 2);
        compareResults("FILES_READ", 2, 1L * MiB_IN_BYTES, 2, results);
        controller.teardown();
        deleteVolumes("BenchVolA", "BenchVolB");
    }

    @Test
    public void testConfigUser() throws Exception {
        configBuilder.setUserName("test");
        Volume volume = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);
        assertEquals("test", volume.getAttr(userCredentials, "benchmarks/sequentialBenchmark/benchFile0").getUserId());
        deleteVolumes("BenchVolA");
    }

    @Test
    public void testConfigGroup() throws Exception {
        configBuilder.setGroup("test");
        Volume volume = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);
        assertEquals("test", volume.getAttr(userCredentials, "benchmarks/sequentialBenchmark/benchFile0").getGroupId());
        deleteVolumes("BenchVolA");
    }

    @Test
    public void testConfigSeqSize() throws Exception {
        long seqSize = 2L * MiB_IN_BYTES;
        Volume volume = performBenchmark(seqSize, configBuilder, BenchmarkType.SEQ_WRITE);
        assertEquals(seqSize, volume.getAttr(userCredentials, "benchmarks/sequentialBenchmark/benchFile0").getSize());
        deleteVolumes("BenchVolA");
    }

    @Test
    public void testConfigBasefileSize() throws Exception {
        long randSize = 1L * MiB_IN_BYTES;
        long basefileSize = 20L * MiB_IN_BYTES;
        configBuilder.setBasefileSizeInBytes(basefileSize);
        Volume volume = performBenchmark(randSize, configBuilder, BenchmarkType.RAND_WRITE);
        assertEquals(basefileSize, volume.getAttr(userCredentials, "benchmarks/basefile").getSize());
        deleteVolumes("BenchVolA");
    }

    @Test
    public void testConfigFilesSize() throws Exception {
        int fileSize = 8 * KiB_IN_BYTES;
        configBuilder.setFilesize(fileSize);
        Volume volume = performBenchmark(1L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.FILES_WRITE);
        int numberOfFiles = (MiB_IN_BYTES) / (8 * KiB_IN_BYTES);
        for (int i = 0; i < numberOfFiles; i++) {
            long fileSizeActual = volume.getAttr(userCredentials, "benchmarks/randomBenchmark/benchFile" + i).getSize();
            assertEquals(fileSize, fileSizeActual);
        }
        deleteVolumes("BenchVolA");
    }

    @Test
    public void testConfigStripeSize() throws Exception {
        int stripeSize = 64 * KiB_IN_BYTES;
        configBuilder.setStripeSizeInBytes(stripeSize);
        Volume volume = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);

        String sp_values = volume.getXAttr(userCredentials, "", "xtreemfs.default_sp");
        String stripped_sp = sp_values.replace("{", "").replace("}", "").replace("\"", "");
        assertTrue(stripped_sp.contains("size:64"));

        deleteVolumes("BenchVolA");
    }

    @Test
    public void testConfigStripeWidth() throws Exception {
        configBuilder.setStripeWidth(2);
        Volume volume = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);
        String sp_values = volume.getXAttr(userCredentials, "", "xtreemfs.default_sp");

        String stripped_sp = sp_values.replace("{", "").replace("}", "").replace("\"", "");
        assertTrue(stripped_sp.contains("width:2"));

        deleteVolumes("BenchVolA");
    }

    /*
     * Test, that using an existing volume doesn't change the stripe size and width previously set on said volume.
     */
    @Test
    public void testConfigStripeSizeWidthNotSet() throws Exception {

        List<GlobalTypes.KeyValuePair> volumeAttributes = new ArrayList<GlobalTypes.KeyValuePair>();
        client.createVolume(authNone, userCredentials, "BenchVolA", 511, "test", "test", GlobalTypes.AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                GlobalTypes.StripingPolicyType.STRIPING_POLICY_RAID0, 1024, 2, volumeAttributes);

        Volume volume = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);

        String sp_values = volume.getXAttr(userCredentials, "", "xtreemfs.default_sp");
        String stripped_sp = sp_values.replace("{", "").replace("}", "").replace("\"", "");
        assertTrue(stripped_sp.contains("width:2"));
        assertTrue(stripped_sp.contains("size:1024"));

        deleteVolumes("BenchVolA");
    }

    /*
     * Test, that setting stripe size and width overrides values of an existing volume.
     */
    @Test
    public void testConfigStripeSizeWidthSet() throws Exception {
        List<GlobalTypes.KeyValuePair> volumeAttributes = new ArrayList<GlobalTypes.KeyValuePair>();
        client.createVolume(authNone, userCredentials, "BenchVolA", 511, "test", "test", GlobalTypes.AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                GlobalTypes.StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1, volumeAttributes);

        configBuilder.setStripeSizeInBytes(256*1024).setStripeWidth(2);
        Volume volume = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);

        String sp_values = volume.getXAttr(userCredentials, "", "xtreemfs.default_sp");
        String stripped_sp = sp_values.replace("{", "").replace("}", "").replace("\"", "");
        assertTrue(stripped_sp.contains("width:2"));
        assertTrue(stripped_sp.contains("size:256"));

        deleteVolumes("BenchVolA");
    }

    @Test
    public void testConfigOSDSelectionPolicy() throws Exception {
        configBuilder.setOsdSelectionPolicies("1001,3003");
        Volume volumeA = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);
        assertEquals("1001,3003", volumeA.getOSDSelectionPolicy(userCredentials));
        deleteVolumes("BenchVolA");
    }


    /*
     * Test, that using an existing volume doesn't change the osd selection policies previously set on said volume.
     */
    @Test
    public void testConfigOSDSelectionPolicyNotSet() throws Exception {

        List<GlobalTypes.KeyValuePair> volumeAttributes = new ArrayList<GlobalTypes.KeyValuePair>();
        client.createVolume(authNone, userCredentials, "BenchVolA", 511, "test", "test", GlobalTypes.AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                GlobalTypes.StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1, volumeAttributes);
        Volume volume = client.openVolume("BenchVolA", null, new Options());
        volume.setOSDSelectionPolicy(userCredentials, "1001,3003");
        volume.close();

        configBuilder.setUserName("test").setGroup("test");
        Volume volumeA = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);
        assertEquals("1001,3003", volumeA.getOSDSelectionPolicy(userCredentials));
        deleteVolumes("BenchVolA");
    }

    @Test
    public void testConfigOSDSelectionUUID() throws Exception {
        /* perform benchmark on osd "UUID:localhost:42640" */
        configBuilder.setSelectOsdsByUuid("UUID:localhost:42640");
        Volume volumeA = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);

        /* perform benchmark on osd "UUID:localhost:42641" */
        configBuilder = BenchmarkConfig.newBuilder();
        configBuilder.setUserName("test").setGroup("test");
        configBuilder.setDirAddress(dirAddress);
        configBuilder.setSelectOsdsByUuid("UUID:localhost:42641").setNoCleanup();
        controller = new Controller(configBuilder.build());
        controller.setupVolumes("BenchVolB");
        controller.startSequentialWriteBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, 1);
        controller.teardown();
        Volume volumeB = client.openVolume("BenchVolB", null, new Options());

        /* assert, that the benchmark files were created on the correct osd */
        assertEquals("1002", volumeA.getOSDSelectionPolicy(userCredentials));
        assertEquals("UUID:localhost:42640",
                volumeA.getSuitableOSDs(userCredentials, "benchmarks/sequentialBenchmark/benchFile0", 1).get(0));
        assertEquals("1002", volumeB.getOSDSelectionPolicy(userCredentials));
        assertEquals("UUID:localhost:42641",
                volumeB.getSuitableOSDs(userCredentials, "benchmarks/sequentialBenchmark/benchFile0", 1).get(0));
        deleteVolumes("BenchVolA", "BenchVolB");
    }

    @Test
    public void testConfigReplicationPolicy() throws Exception {
        configBuilder.setReplicationPolicy("WqRq");
        configBuilder.setReplicationFactor(3);
        Volume volumeA = performBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);

        String default_rp = volumeA.getXAttr(userCredentials,"", "xtreemfs.default_rp");
        String stripped_rp = default_rp.replace("{", "").replace("}", "").replace("\"", "");
        assertTrue(stripped_rp.contains("replication-factor:3"));
        assertTrue(stripped_rp.contains("update-policy:WqRq"));

        deleteVolumes("BenchVolA");
    }

    /*
     * Tests, that performing a benchmark on a volume for which a default replication policy was set does not override the policy
     */
    @Test
    public void testConfigReplicationPolicyNotSet() throws Exception {

        List<GlobalTypes.KeyValuePair> volumeAttributes = new ArrayList<GlobalTypes.KeyValuePair>();
        client.createVolume(authNone, userCredentials, "BenchVolA", 511, "test", "test",
                GlobalTypes.AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
                GlobalTypes.StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1, volumeAttributes);
        Volume volume = client.openVolume("BenchVolA", null, new Options());
        volume.setDefaultReplicationPolicy(userCredentials, "/", "WqRq", 3, 0);
        volume.close();

        configBuilder.setUserName("test").setGroup("test");
        Volume volumeA = performBenchmark(10L * BenchmarkUtils.MiB_IN_BYTES, configBuilder, BenchmarkType.SEQ_WRITE);

        String default_rp = volumeA.getXAttr(userCredentials, "", "xtreemfs.default_rp");
        String stripped_rp = default_rp.replace("{", "").replace("}", "").replace("\"", "");
        assertTrue(stripped_rp.contains("replication-factor:3"));
        assertTrue(stripped_rp.contains("update-policy:WqRq"));
        deleteVolumes("BenchVolA");
    }

    /* The NoCleanup option is testet implicitly in all the above Config tests */

// TODO Understand logic behind this test and fix it, as it depends on the execution order of all tests.
// It is implicitly assumed that the tests execute in the order they appear in this source file. This cannot
// be guaranteed, and if the below test runs before some other test, this other test and all following tests
// will fail.
//    @Test
//    public void testConfigNoCleanupVolumes() throws Exception {
//        configBuilder.setNoCleanupVolumes();
//        controller = new Controller(configBuilder.build());
//        controller.setupVolumes("BenchVolA", "BenchVolB", "BenchVolC");
//        controller.startSequentialWriteBenchmark(10L*BenchmarkUtils.MiB_IN_BYTES, 3);
//
//        Volume volumeA = client.openVolume("BenchVolA", null, new Options());
//        Volume volumeB = client.openVolume("BenchVolB", null, new Options());
//        Volume volumeC = client.openVolume("BenchVolC", null, new Options());
//
//        /* the benchFiles are still there after the benchmark */
//        long seqSize = 10L * BenchmarkUtils.MiB_IN_BYTES;
//        assertEquals(seqSize, volumeA.getAttr(userCredentials, "benchmarks/sequentialBenchmark/benchFile0").getSize());
//        assertEquals(seqSize, volumeB.getAttr(userCredentials, "benchmarks/sequentialBenchmark/benchFile0").getSize());
//        assertEquals(seqSize, volumeC.getAttr(userCredentials, "benchmarks/sequentialBenchmark/benchFile0").getSize());
//
//        controller.teardown();
//
//        /*
//         * after the teardown (which includes the deletion of the benchmark volumes and files), only the volumes are
//         * present
//         */
//        assertEquals(0, (int) Integer.valueOf(volumeA.getXAttr(userCredentials, "", "xtreemfs.num_files")));
//        assertEquals(0, (int) Integer.valueOf(volumeB.getXAttr(userCredentials, "", "xtreemfs.num_files")));
//        assertEquals(0, (int) Integer.valueOf(volumeC.getXAttr(userCredentials, "", "xtreemfs.num_files")));
//        assertEquals(0, (int) Integer.valueOf(volumeA.getXAttr(userCredentials, "", "xtreemfs.used_space")));
//        assertEquals(0, (int) Integer.valueOf(volumeB.getXAttr(userCredentials, "", "xtreemfs.used_space")));
//        assertEquals(0, (int) Integer.valueOf(volumeC.getXAttr(userCredentials, "", "xtreemfs.used_space")));
//        deleteVolumes("BenchVolA", "BenchVolB", "BenchVolC");
//    }

// TODO See above.
//    @Test
//    public void testConfigNoCleanupBasefile() throws Exception {
//        long basefileSize = 30L * BenchmarkUtils.MiB_IN_BYTES;
//        long randSize = BenchmarkUtils.MiB_IN_BYTES;
//        configBuilder.setNoCleanupBasefile().setBasefileSizeInBytes(basefileSize)
//                .setNoCleanupVolumes();
//        controller = new Controller(configBuilder.build());
//        controller.setupVolumes("BenchVolA");
//        controller.startRandomWriteBenchmark(randSize, 1);
//
//        /* the filebased benchmark is used to show, that really files are (created and) deleted, except the basefile */
//        controller.startFilebasedWriteBenchmark(randSize, 1);
//
//        Volume volume = client.openVolume("BenchVolA", null, new Options());
//
//        /* number of files from filebased benchmark + basefile */
//        int numberOfFiles = (int) (randSize / (4 * BenchmarkUtils.KiB_IN_BYTES)) + 1;
//        assertEquals(numberOfFiles, (int) Integer.valueOf(volume.getXAttr(userCredentials, "", "xtreemfs.num_files")));
//        assertEquals(basefileSize + randSize,
//                (int) Integer.valueOf(volume.getXAttr(userCredentials, "", "xtreemfs.used_space")));
//
//        controller.teardown();
//
//        /*
//         * after the teardown (which includes the deletion of the benchmark volumes and files), only the basefile is
//         * still present
//         */
//        assertEquals(basefileSize, volume.getAttr(userCredentials, "benchmarks/basefile").getSize());
//        assertEquals(1, (int) Integer.valueOf(volume.getXAttr(userCredentials, "", "xtreemfs.num_files")));
//        assertEquals(basefileSize, (int) Integer.valueOf(volume.getXAttr(userCredentials, "", "xtreemfs.used_space")));
//    }

    private void assertNoVolumes(String... volumes) throws Exception {
        for (String volumeName : volumes) {
            try {
                Volume volume = client.openVolume(volumeName, null, new Options());
                fail("VolumeNotFoundException expected");
            } catch (VolumeNotFoundException e) {
                // ok (Exception expected)
            }
        }
    }

    private void compareResults(String type, int threads, long size, int numberOfResults, List<BenchmarkResult> results) {
        int resultCounter = 0;
        for (BenchmarkResult result : results) {
            resultCounter++;
            String benchmarkType = result.getBenchmarkType().toString();
            assertEquals(type, benchmarkType);
            assertEquals(threads, result.getNumberOfReadersOrWriters());
            assertEquals(size, result.getRequestedSize());
            assertEquals(size, result.getActualSize());
        }
        assertEquals(numberOfResults, resultCounter);
    }

    private Volume performBenchmark(long size, ConfigBuilder configBuilder, BenchmarkType type) throws Exception {
        configBuilder.setNoCleanup();
        controller = new Controller(configBuilder.build());
        controller.setupVolumes("BenchVolA");
        switch (type) {
        case SEQ_WRITE:
            controller.startSequentialWriteBenchmark(size, 1);
            break;
        case RAND_WRITE:
            controller.startRandomWriteBenchmark(size, 1);
            break;
        case FILES_WRITE:
            controller.startFilebasedWriteBenchmark(size, 1);
            break;
        }
        controller.teardown();

        Volume volume = client.openVolume("BenchVolA", null, new Options());
        return volume;
    }

    private void deleteVolumes(String... volumeNames) throws IOException {
        for (String volumeName : volumeNames) {
            client.deleteVolume(auth, userCredentials, volumeName);
        }
    }
}
