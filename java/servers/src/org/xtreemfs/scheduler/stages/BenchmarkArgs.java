package org.xtreemfs.scheduler.stages;

import org.xtreemfs.common.config.ServiceConfig;

/**
 * The interface for the benchmark arguments class for a {@link Stage.StageRequest}.
 *
 * @author jensvfischer
 */
public interface BenchmarkArgs {

    /**
     * Get the size for the sequential read benchmarks.
     *
     * @return size for the sequential read benchmarks
     */
    public long getSequentialSize();

    /**
     * Get the size for the random read benchmarks
     *
     * @return the size for the random read benchmarks
     */
    public long getRandomSize();

    /**
     * Get the maximum number of threads for the benchmarks.
     *
     * @return The maximum number of threads for the benchmarks
     */
    public int getNumberOfThreads();

    /**
     * Get how often a benchmark is repeated for the measurement.
     *
     * @return the number of repetitions
     */
    public int getNumberOfRepetitions();

    /**
     * Get the UUID of the OSD to be measured.
     *
     * @return The UUID of the OSD to be measured
     */
    public String getOsdUuid();

    /**
     * Decrement the number of retries. <br/>
     *
     * The number of retries detemines how often the {@link org.xtreemfs.scheduler.stages.BenchmarkStage} retries to do
     * the benchmark in case of failure. At each trial, {@link #decRetries()} is called. If {@link #decRetries()}
     * returns a value greater then zero, the request is re-enqueued
     */
    public void decRetries();

    /**
     * Get the number of retries. <br/>
     *
     * The number of retries detemines how often the {@link org.xtreemfs.scheduler.stages.BenchmarkStage} retries to do
     * the benchmark in case of failure. At each trial, {@link #decRetries()} is called. If {@link #decRetries()}
     * returns a value greater then zero, the request is re-enqueued.
     * 
     * @return the number of retries
     */
    public int getRetries();


    /**
     * Get the {@link org.xtreemfs.common.benchmark.BenchmarkConfig} to use with the request.
     *
     * @return
     */
    public ServiceConfig getConfig();

}
