package org.xtreemfs.scheduler.stages;

import org.junit.*;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.benchmark.BenchmarkUtils;
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
import org.xtreemfs.scheduler.SchedulerClient;
import org.xtreemfs.scheduler.SchedulerConfig;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class BenchmarkStageTest {

    private static DIRRequestDispatcher       dir;
    private static TestEnvironment            testEnv;
    private static DIRConfig                  dirConfig;
    private static DIRClient                  dirClient;
    private static final int                  NUMBER_OF_OSDS = 2;
    private static OSDConfig                  osdConfigs[];
    private static OSD                        osds[];
    static SchedulerConfig                    config;
    static BabuDBConfig                       dbsConfig;
    private static SchedulerRequestDispatcher scheduler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Logging.start(Logging.LEVEL_WARN, Logging.Category.all);

        config = SetupUtils.createSchedulerConfig();
        dbsConfig = SetupUtils.createSchedulerdbsConfig();

        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

        dirConfig = SetupUtils.createDIRConfig();
        osdConfigs = SetupUtils.createMultipleOSDConfigs(NUMBER_OF_OSDS);

        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.RPC_CLIENT, TestEnvironment.Services.MRC, TestEnvironment.Services.SCHEDULER_CLIENT);
        testEnv.start();

        scheduler = new SchedulerRequestDispatcher(config, dbsConfig);
        scheduler.startup();
        scheduler.waitForStartup();


        dirClient = new DIRClient(new DIRServiceClient(testEnv.getRpcClient(), null),
                new InetSocketAddress[] { testEnv.getDIRAddress() }, 3, 1000);

        osds = new OSD[NUMBER_OF_OSDS];
        for (int i = 0; i < osds.length; i++) {
            osds[i] = new OSD(osdConfigs[i]);
        }
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        scheduler.shutdown();
        scheduler.waitForShutdown();
        testEnv.shutdown();
    }
    
    @Test
    @Ignore
    public void testBenchmarkStage() throws Exception {
        final String osd1 = "UUID:localhost:42640";
        final String osd2 = "UUID:localhost:42641";
        final BenchmarkStage benchStage = new BenchmarkStage("BenchmarkStage", 5, scheduler);
        benchStage.start();
        benchStage.waitForStartup();

        BenchmarkCompleteCallback cb = new BenchmarkCompleteCallback() {
            private int resultCounter = 0;

            @Override
            public void benchmarkComplete(OSDPerformanceDescription perfDescription) {
                resultCounter++;
                if (resultCounter == 2) {
                    assertTrue(true);
                    benchStage.shutdown();
                }
            }

            @Override
            public void benchmarkFailed(Throwable error) {
                System.out.println(error.getMessage());
                fail();
            }
        };



        benchStage.enqueueOperation(0, new BenchmarkArgsImpl(10L * BenchmarkUtils.MiB_IN_BYTES,
                1L * BenchmarkUtils.MiB_IN_BYTES, 1, 1, osd1), null, cb);
        benchStage.enqueueOperation(0, new BenchmarkArgsImpl(10L * BenchmarkUtils.MiB_IN_BYTES,
                1L * BenchmarkUtils.MiB_IN_BYTES, 1, 1, osd2), null, cb);

        benchStage.join();
    }


    static class BenchmarkArgsImpl implements BenchmarkArgs{
        private long sequentialSize;
        private long randomSize;
        private int numberOfThreads;
        private int numberOfRepetitions;
        private int retries;
        private String osdUuid;

        BenchmarkArgsImpl(long sequentialSize, long randomSize, int numberOfThreads, int numberOfRepetitions, String osdUuid) {
            this.sequentialSize = sequentialSize;
            this.randomSize = randomSize;
            this.numberOfThreads = numberOfThreads;
            this.numberOfRepetitions = numberOfRepetitions;
            this.osdUuid = osdUuid;
        }

        @Override
        public long getSequentialSize() {
            return sequentialSize;
        }
        
        @Override
        public long getRandomSize() {
            return randomSize;
        }

        @Override
        public int getNumberOfThreads() {
            return numberOfThreads;
        }

        @Override
        public int getNumberOfRepetitions() {
            return numberOfRepetitions;
        }

        @Override
        public String getOsdUuid() {
            return osdUuid;
        }

        @Override
        public void incRetries() {
            retries++;
        }

        @Override
        public int getRetries() {
            return retries;
        }
    }    
    
}
