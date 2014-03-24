/*
* Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
*
*
* Licensed under the BSD License, see LICENSE file for details.
*
*/

package org.xtreemfs.scheduler.stages;

import org.xtreemfs.common.benchmark.BenchmarkConfig;
import org.xtreemfs.common.benchmark.BenchmarkConfig.ConfigBuilder;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.scheduler.benchmark.BenchmarkRunner;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;

/**
* The benchmark stage starts a performance measurement of the given OSD. <br/>
* If the benchmark exits with an exception, the
* benchmark is retried until the MAX_NUMBER_OF_RETRIES is reached. The benchmark will not be retried immediately, but
* the request will be re-queued. In case of success, the
* {@link BenchmarkCompleteCallback#benchmarkComplete(org.xtreemfs.scheduler.data.OSDPerformanceDescription)} will be
* called, otherwise (after the max number of retries), the {@link BenchmarkCompleteCallback#benchmarkFailed(Throwable)}
* is called.
*
* @author jensvfischer
*/
public class BenchmarkStage extends Stage<BenchmarkArgs, BenchmarkCompleteCallback> {


    public BenchmarkStage(String stageName, int queueCapacity) {
        super(stageName, queueCapacity);
    }

    /**
     * Handles the actual execution of a stage method. Must be implemented by
     * all stages.
     *
     * @param stageRequest the StageRequest object representing the stage request
     */
    @Override
    protected void processMethod(StageRequest<BenchmarkArgs, BenchmarkCompleteCallback> stageRequest) {
        BenchmarkCompleteCallback cb = stageRequest.getCallback();
        BenchmarkArgs args = stageRequest.getArgs();
        ConfigBuilder builder = BenchmarkConfig.newBuilder();
        builder.setParent(stageRequest.getArgs().getConfig());
        builder.setSelectOsdsByUuid(args.getOsdUuid());
        BenchmarkRunner br;
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.stage, this,
                "Starting Benchmark of OSD %s", args.getOsdUuid());
        try {
            br = new BenchmarkRunner(builder.build(), args.getSequentialSize(), args.getRandomSize(),
                    args.getNumberOfThreads(), args.getNumberOfRepetitions());
            OSDPerformanceDescription perfDesc = br.runBenchmark();
            cb.benchmarkComplete(perfDesc);
            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.stage, this,
                    "Finished Benchmark of OSD %s", args.getOsdUuid());
        } catch (Exception e) {
            if (args.getRetries() > 0) {
                args.decRetries();
                Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.stage, this,
                        "The benchmark of OSD %s failed, the benchmark will be retried later", args.getOsdUuid());
                this.enqueueOperation(0, args, null, cb);
            } else {
                Logging.logMessage(
                        Logging.LEVEL_WARN,
                        Logging.Category.stage,
                        this,
                        "The benchmark onf OSD %s failed more often than the max number of retries, the benchmark will not be retried later",
                        args.getOsdUuid());
                cb.benchmarkFailed(e);
            }
        }
    }

}
