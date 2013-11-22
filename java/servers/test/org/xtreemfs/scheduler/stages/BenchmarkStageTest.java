package org.xtreemfs.scheduler.stages;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.xtreemfs.common.benchmark.BenchmarkUtils;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;

public class BenchmarkStageTest extends StageTest {

    @Test
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

    static class BenchmarkArgsImpl implements BenchmarkArgs {
        private long   sequentialSize;
        private long   randomSize;
        private int    numberOfThreads;
        private int    numberOfRepetitions;
        private int    retries;
        private String osdUuid;

        BenchmarkArgsImpl(long sequentialSize, long randomSize, int numberOfThreads, int numberOfRepetitions,
                String osdUuid) {
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
