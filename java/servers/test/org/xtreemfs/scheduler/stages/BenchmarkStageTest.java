package org.xtreemfs.scheduler.stages;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.xtreemfs.common.benchmark.BenchmarkConfig;
import org.xtreemfs.common.benchmark.BenchmarkUtils;
import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;

public class BenchmarkStageTest extends StageTest {

    @Test
    public void testBenchmarkStage() throws Exception {
        final String osd1 = "UUID:localhost:42640";
        final String osd2 = "UUID:localhost:42641";
        final BenchmarkStage benchStage = new BenchmarkStage("BenchmarkStage", 5);
        benchStage.start();
        benchStage.waitForStartup();

        BenchmarkCompleteCallback cb = new BenchmarkCompleteCallback() {
            private int resultCounter = 0;

            @Override
            public void benchmarkComplete(OSDPerformanceDescription perfDescription) {
                resultCounter++;
                if (resultCounter == 2) {
                    assertNotNull(perfDescription);
                    benchStage.shutdown();
                }
            }

            @Override
            public void benchmarkFailed(Throwable error) {
                benchStage.shutdown();
            }
        };

        BenchmarkConfig.ConfigBuilder builder = BenchmarkConfig.newBuilder();
        builder.setParent(osdConfigs[0]);
        builder.setBasefileSizeInBytes(10*1024*1024L);
        BenchmarkConfig benchmarkConfig = builder.build();


        benchStage.enqueueOperation(0, new BenchmarkArgsImpl(10L * BenchmarkUtils.MiB_IN_BYTES,
                1L * BenchmarkUtils.MiB_IN_BYTES, 1, 1, osd1, 3, builder.build()), null, cb);
        benchStage.enqueueOperation(0, new BenchmarkArgsImpl(10L * BenchmarkUtils.MiB_IN_BYTES,
                1L * BenchmarkUtils.MiB_IN_BYTES, 1, 1, osd2, 3, builder.build()), null, cb);

        benchStage.join();
    }

    static class BenchmarkArgsImpl implements BenchmarkArgs {
        private long sequentialSize;
        private long randomSize;
        private int numberOfThreads;
        private int numberOfRepetitions;
        private int retries;
        private String osdUuid;
        private ServiceConfig config;

        BenchmarkArgsImpl(long sequentialSize, long randomSize, int numberOfThreads, int numberOfRepetitions,
                String osdUuid, int numberOfRetries, ServiceConfig config) {
            this.sequentialSize = sequentialSize;
            this.randomSize = randomSize;
            this.numberOfThreads = numberOfThreads;
            this.numberOfRepetitions = numberOfRepetitions;
            this.osdUuid = osdUuid;
            this.retries = numberOfRetries;
            this.config = config;
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
        public void decRetries() {
            retries--;
        }

        @Override
        public int getRetries() {
            return retries;
        }

        @Override
        public ServiceConfig getConfig() { return config; }
    }

}
