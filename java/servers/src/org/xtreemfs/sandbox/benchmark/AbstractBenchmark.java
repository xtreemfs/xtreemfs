/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.logging.Logging;

/**
 * Benchmark library for measuring read- and writeperformance of a OSD. ToDo (jvf) ErrorHandling
 * 
 * @author jensvfischer
 */
public abstract class AbstractBenchmark {

    static final int  MiB_IN_BYTES = 1024 * 1024;
    static final int  GiB_IN_BYTES = 1024 * 1024 * 1024;

    final int         stripeWidth;
    final long        benchmarkSizeInBytes;
    final Volume      volume;
    final AdminClient client;
    final Params      params;

    AbstractBenchmark(long benchmarkSizeInBytes, Volume volume, Params params) throws Exception {
        client = BenchmarkClientFactory.getNewClient(params);
        this.benchmarkSizeInBytes = benchmarkSizeInBytes;
        this.volume = volume;
        this.params = params;
        stripeWidth = params.stripeSizeInBytes;
    }

    /*
     * Performs a single sequential read- or write-benchmark. Whether a read- or write-benchmark is performed
     * depends on which subclass is instantiated. This method is supposed to be called within its own thread
     * to run a benchmark.
     */
    void benchmark(ConcurrentLinkedQueue<BenchmarkResult> results) throws Exception {

        String shortClassname = this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
        Logging.logMessage(Logging.LEVEL_INFO, this, "Starting %s", shortClassname, Logging.Category.tool);

        // Setting up
        byte[] data = new byte[stripeWidth];

        long numberOfBlocks = benchmarkSizeInBytes / stripeWidth;
        long byteCounter = 0;

        /* Run the AbstractBenchmark */
        long before = System.currentTimeMillis();
        byteCounter = tryPerformIO(data, numberOfBlocks);
        long after = System.currentTimeMillis();

        /* Calculate and return results */
        double timeInSec = (after - before) / 1000.;
        double speedMiBPerSec = round((byteCounter / MiB_IN_BYTES) / timeInSec, 2);

        BenchmarkResult result = new BenchmarkResult(timeInSec, speedMiBPerSec, benchmarkSizeInBytes, Thread
                .currentThread().getId(), byteCounter);
        results.add(result);

        finalizeBenchmark();

        Logging.logMessage(Logging.LEVEL_INFO, this, "Finished %s", shortClassname, Logging.Category.tool);
    }

    /* called before a benchmark thread is started */
    abstract void prepareBenchmark() throws Exception;

    /* Error handling for 'performIO()' */
    long tryPerformIO(byte[] data, long numberOfBlocks) {
        long byteCounter;
        try {
            byteCounter = performIO(data, numberOfBlocks);
        } catch (IOException e) {
            byteCounter = 0;
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this,
                    "Error while trying to perform IO: %s", e.getMessage());
        }
        return byteCounter;
    }

    /*
     * Writes or reads the specified amount of data to/from the volume specified in the object initialization.
     * Called within the benchmark method.
     */
    abstract long performIO(byte[] data, long numberOfBlocks) throws IOException;

    /* called at the end of every benchmark */
    abstract void finalizeBenchmark() throws Exception;

    /* Starts a benchmark in its own thread. */
    public void startBenchmark(ConcurrentLinkedQueue<BenchmarkResult> results, ConcurrentLinkedQueue<Thread> threads)
            throws Exception {
        prepareBenchmark();
        Thread benchThread = new Thread(new BenchThread(this, results));
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

    /**
     * Enum for the different benchmark Types.
     *
     * @author jensvfischer
     */
    public static enum BenchmarkType {
        SEQ_WRITE, SEQ_READ, RAND_WRITE, RAND_READ, FILES_WRITE, FILES_READ
    }
}
