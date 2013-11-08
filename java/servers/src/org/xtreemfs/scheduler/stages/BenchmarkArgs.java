package org.xtreemfs.scheduler.stages;

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
     * Increment the number of retries
     */
    public void incRetries();

    /**
     * Get the number of retries. The number of retries need to be null initially.
     *
     * @return the number of retries
     */
    public int getRetries();

}
