/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.stages;

import org.xtreemfs.common.benchmark.ConfigBuilder;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.scheduler.SchedulerConfig;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;
import org.xtreemfs.scheduler.benchmark.BenchmarkRunner;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;

import java.net.InetSocketAddress;

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

    private final static int MAX_NUMBER_OF_RETRIES = 3;  
    
    private final SchedulerRequestDispatcher master;

    public BenchmarkStage(String stageName, int queueCapacity, SchedulerRequestDispatcher master) {
        super(stageName, queueCapacity);
        this.master = master;
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
        ConfigBuilder builder = parseConfig();
        builder.setSelectOsdsByUuid(args.getOsdUuid());
        BenchmarkRunner br;
        try {
            br = new BenchmarkRunner(builder.build(), args.getSequentialSize(), args.getRandomSize(),
                    args.getNumberOfThreads(), args.getNumberOfRepetitions());
            OSDPerformanceDescription perfDesc = br.runBenchmark();            
            cb.benchmarkComplete(perfDesc);
        } catch (Exception e) {
            if (args.getRetries() < MAX_NUMBER_OF_RETRIES) {
                args.incRetries();
                Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.stage, this,
                        "The benchmark onf OSD %s failed, the benchmark will be retried later", args.getOsdUuid());
                Logging.logError(Logging.LEVEL_WARN, this, e);
                this.enqueueOperation(0, args, null, cb);
            } else
                Logging.logMessage(
                        Logging.LEVEL_WARN,
                        Logging.Category.stage,
                        this,
                        "The benchmark onf OSD %s failed more often than the max number of retries, the benchmark will not be retried later",
                        args.getOsdUuid());
            Logging.logError(Logging.LEVEL_WARN, this, e);
            cb.benchmarkFailed(e);
        }
    }

    private ConfigBuilder parseConfig(){
        SchedulerConfig schedConfig = master.getConfig();
        ConfigBuilder builder = new ConfigBuilder();
        InetSocketAddress dir = schedConfig.getDirectoryService();
        builder.setDirAddress(dir.getHostName()+":"+dir.getPort());
        builder.setAdminPassword(schedConfig.getAdminPassword());
//      Todo Auth? SSL?
        return builder;
    }

}
