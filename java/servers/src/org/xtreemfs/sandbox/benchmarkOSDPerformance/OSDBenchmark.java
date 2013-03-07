/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.logging.Logging;

/**
 * Benchmarklibrary for measuring read- and writeperformance of a OSD. TODO ErrorHandling TODO Move Cleanup to
 * the scheduleBenchmark-methods (needs clarification whether Benchmarks should be performed via libxtreemfs
 * of directly on the OSD)
 * 
 * @author jensvfischer
 */
public abstract class OSDBenchmark {


    static final int     MiB_IN_BYTES                 = 1024 * 1024;
    static final int     GiB_IN_BYTES                 = 1024 * 1024 * 1024;
    static final int     XTREEMFS_BLOCK_SIZE_IN_BYTES = 128 * 1024;        // 128 KiB
    static final String  BENCHMARK_FILENAME           = "benchmarkFile";

    final Volume volume;
    final AdminClient    client;
    final ConnectionData connection;

    OSDBenchmark(Volume volume, ConnectionData connection) throws Exception {
        client = Controller.getClient();
        this.volume = volume;
        this.connection = connection;
    }

    /*
     * Writes or reads the specified amount of data to/from the volume specified in the object initialization.
     * Called within the benchmark method.
     */
    abstract long writeOrReadData(byte[] data, long numberOfBlocks) throws IOException;

    /*
     * Performs a single sequential read- or write-benchmark. Whether a read- or write-benchmark is performed
     * depends on which subclass is instantiated. This method is supposed to be called within its own thread
     * to run a benchmark.
     */
    void benchmark(long sizeInBytes, ConcurrentLinkedQueue<BenchmarkResult> results) throws Exception {

        String shortClassname = this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
        Logging.logMessage(Logging.LEVEL_INFO, this, "Starting %s", shortClassname, Logging.Category.tool);

        // Setting up
        byte[] data = new byte[XTREEMFS_BLOCK_SIZE_IN_BYTES];

        long numberOfBlocks = sizeInBytes / XTREEMFS_BLOCK_SIZE_IN_BYTES;
        long byteCounter = 0;

        /* Run the Benchmark */
        long before = System.currentTimeMillis();
        byteCounter = writeOrReadData(data, numberOfBlocks);
        long after = System.currentTimeMillis();

        /* Calculate and return results */
        double timeInSec = (after - before) / 1000.;
        double speedMiBPerSec = round((byteCounter / MiB_IN_BYTES) / timeInSec, 2);

        BenchmarkResult result = new BenchmarkResult(timeInSec, speedMiBPerSec, sizeInBytes, Thread.currentThread()
                .getId(), byteCounter);
        results.add(result);

        /* shutdown */

        Logging.logMessage(Logging.LEVEL_INFO, this, "Finished %s", shortClassname, Logging.Category.tool);
    }

    /* Starts a benchmark in its own thread. */
    public void startBenchThread(long sizeInBytes, ConcurrentLinkedQueue<BenchmarkResult> results,
            ConcurrentLinkedQueue<Thread> threads) throws Exception {
        Thread benchThread = new Thread(new BenchThread(this, sizeInBytes, results));
        threads.add(benchThread);
        benchThread.start();
    }

    /* Round doubles to specified number of decimals */
    static double round(double value, int places) {
        if (places < 0)
            throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

}
