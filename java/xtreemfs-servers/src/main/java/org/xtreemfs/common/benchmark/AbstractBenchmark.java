/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.logging.Logging;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Abstract baseclass for the benchmark classes.
 * 
 * @author jensvfischer
 */
 abstract class AbstractBenchmark implements Callable<BenchmarkResult>{

    static volatile boolean cancelled = false;

    final int             requestSize;
    final long            benchmarkSize;
    final Volume          volume;
    final AdminClient     client;
    final BenchmarkConfig config;
    final VolumeManager   volumeManager;

    AbstractBenchmark(long benchmarkSize, BenchmarkConfig config, AdminClient client, VolumeManager volumeManager) throws Exception {
        this.client = client;
        this.benchmarkSize = benchmarkSize;
        this.volume = volumeManager.getNextVolume();
        this.config = config;
        this.requestSize = config.getChunkSizeInBytes();
        this.volumeManager = volumeManager;
        this.cancelled = false; // reset cancellation status
    }

    /*
     * Performs a single sequential read- or write-benchmark. Whether a read- or write-benchmark is performed depends on
     * which subclass is instantiated. This method is supposed to be called within its own thread to run a benchmark.
     */
    BenchmarkResult runBenchmark() throws Exception {

        String shortClassname = this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Starting %s on volume %s", shortClassname, volume.getVolumeName());

        // Setting up
        byte[] data = new byte[requestSize];

        long numberOfRequests = benchmarkSize / requestSize;

        /* Run the AbstractBenchmark */
        long before = System.currentTimeMillis();
        long requestCounter = performIO(data, numberOfRequests);
        long after = System.currentTimeMillis();

        if (benchmarkSize != requestCounter)
            throw new BenchmarkFailedException("Data written does not equal the requested size");

        /* Calculate results */
        double timeInSec = (after - before) / 1000.;
        BenchmarkResult result = new BenchmarkResult(timeInSec, benchmarkSize, requestCounter);

        finalizeBenchmark();

        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Finished %s", shortClassname);
        return result;
    }

    /* called before a benchmark thread is started */
    abstract void prepareBenchmark() throws Exception;

    /*
     * Writes or reads the specified amount of data to/from the volume specified in the object initialization. Called
     * within the benchmark method.
     */
    abstract long performIO(byte[] data, long numberOfBlocks) throws IOException;

    /* called at the end of every benchmark */
    abstract void finalizeBenchmark() throws Exception;

    public static void cancel(){
        cancelled = true;
    }

    @Override
    public BenchmarkResult call() throws Exception {
        return this.runBenchmark();
    }

}
