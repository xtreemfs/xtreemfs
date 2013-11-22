package org.xtreemfs.scheduler.stages;

import org.junit.Test;

public class NullStageTest extends StageTest {

    @Test
    public void testNullStage() throws Exception {
        final NullStage nullStage = new NullStage("NullStage", 5);
        nullStage.start();
        nullStage.waitForStartup();

        nullStage.enqueueOperation(0, null, null, null);
        nullStage.enqueueOperation(0, null, null, null);

        Thread.sleep(3000);
        nullStage.shutdown();
        nullStage.join();
    }
}
