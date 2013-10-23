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
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Abstract baseclass for the benchmark classes.
 * 
 * @author jensvfischer
 */
 abstract class AbstractBenchmark {

    final int           stripeSize;
    final long          benchmarkSizeInBytes;
    final Volume        volume;
    final AdminClient   client;
    final Config        config;
    final VolumeManager volumeManager;

    AbstractBenchmark(long benchmarkSizeInBytes, Config config, AdminClient client, VolumeManager volumeManager) throws Exception {
        this.client = client;
        this.benchmarkSizeInBytes = benchmarkSizeInBytes;
        this.volume = volumeManager.getNextVolume();
        this.config = config;
        this.stripeSize = config.getStripeSizeInBytes();
        this.volumeManager = volumeManager;        
    }

    /*
     * Performs a single sequential read- or write-benchmark. Whether a read- or write-benchmark is performed depends on
     * which subclass is instantiated. This method is supposed to be called within its own thread to run a benchmark.
     */
    void benchmark(ConcurrentLinkedQueue<BenchmarkResult> results) throws Exception {

        String shortClassname = this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Starting %s", shortClassname);

        // Setting up
        byte[] data = new byte[stripeSize];

        long numberOfBlocks = benchmarkSizeInBytes / stripeSize;
        long byteCounter = 0;

        /* Run the AbstractBenchmark */
        long before = System.currentTimeMillis();
        byteCounter = performIO(data, numberOfBlocks);
        long after = System.currentTimeMillis();

        /* Calculate and return results */
        double timeInSec = (after - before) / 1000.;

        BenchmarkResult result = new BenchmarkResult(timeInSec, benchmarkSizeInBytes, byteCounter);
        results.add(result);

        finalizeBenchmark();

        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Finished %s", shortClassname);
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

    /* Starts a benchmark in its own thread. */
    void startBenchmarkThread(ConcurrentLinkedQueue<BenchmarkResult> results, ConcurrentLinkedQueue<Thread> threads) throws Exception {
        prepareBenchmark();
        Thread benchThread = new Thread(new BenchmarkThread(this, results));
        threads.add(benchThread);
        benchThread.start();
    }

}
