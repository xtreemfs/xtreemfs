package org.xtreemfs.scheduler.stages;

import java.net.InetSocketAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.scheduler.SchedulerConfig;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class StageTest {

    static DIRRequestDispatcher       dir;
    static TestEnvironment            testEnv;
    static DIRConfig                  dirConfig;
    static DIRClient                  dirClient;
    static final int                  NUMBER_OF_OSDS = 2;
    static OSDConfig                  osdConfigs[];
    static OSD                        osds[];
    static SchedulerConfig            config;
    static BabuDBConfig               dbsConfig;
    static SchedulerRequestDispatcher scheduler;

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
                TestEnvironment.Services.RPC_CLIENT, TestEnvironment.Services.MRC,
                TestEnvironment.Services.SCHEDULER_CLIENT);
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
}
