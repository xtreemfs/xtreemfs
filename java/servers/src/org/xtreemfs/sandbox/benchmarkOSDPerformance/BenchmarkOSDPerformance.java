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
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 * Benchmarklibrary for measuring read- and writeperformance of a OSD.
 *     TODO ErrorHandling
 *     TODO Move Cleanup to the scheduleBenchmark-methods (needs clarification whether Benchmarks should be
 *         performed via libxtreemfs of directly on the OSD)
 *
 *  @author jensvfischer
 */
public abstract class BenchmarkOSDPerformance {

    static final int      MiB_IN_BYTES                 = 1024 * 1024;
    static final int      GiB_IN_BYTES                 = 1024 * 1024 * 1024;
    static final int      XTREEMFS_BLOCK_SIZE_IN_BYTES = 128 * 1024;        // 128 KiB
    static final String   BENCHMARK_FILENAME           = "benchmarkFile";
    static final String   VOLUME_BASE_NAME             = "performanceTest";

    final String          volumeName;
    final AdminClient     client;
    final ConnectionData connection;

    BenchmarkOSDPerformance(String volumeName, ConnectionData connection) throws Exception {
        client = Controller.getClient(connection);
        this.volumeName = volumeName;
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

    /**
     * Starts first a write and then a read benchmark run with the specified amount of benchmarks in parallel.
     * Every benchmark is started within its own thread. The method waits for all threads to finish.
     * 
     * @param dirAddress
     *            DIR Server
     * @param mrcAddress
     *            MRC Server
     * @param userCredentials
     *            User Credentials
     * @param auth
     *            Auth
     * @param sslOptions
     *            SSL Options
     * @param numberOfWriters
     *            number of write benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
//    public static ConcurrentLinkedQueue<BenchmarkResult> scheduleReadWriteBenchmarks(String dirAddress,
//            String mrcAddress, UserCredentials userCredentials, Auth auth, SSLOptions sslOptions, int numberOfWriters,
//            long sizeInBytes) throws Exception {
//
//        ConnectionData connection=new ConnectionData();
//
//        ConcurrentLinkedQueue<BenchmarkResult> resultsWriteBench = Controller.startWriteBenchmarks( numberOfWriters, sizeInBytes);
//        ConcurrentLinkedQueue<BenchmarkResult> resultsReadBench = Controller.startReadBenchmarks(numberOfWriters, sizeInBytes);
//
//        ConcurrentLinkedQueue<BenchmarkResult> resultsRWBench = new ConcurrentLinkedQueue<BenchmarkResult>(
//                resultsWriteBench);
//
//        for (BenchmarkResult result : resultsReadBench)
//            resultsRWBench.add(result);
//
//        return resultsRWBench;
//    }

//    /* Only runs with previous write benchmark, because it reads the data written by the write benchmark */
//    public static void main(String[] args) throws Exception {
//
//        Logging.start(Logging.LEVEL_INFO, Logging.Category.tool);
//
//        ConnectionData connection = new ConnectionData(); // using the default values for
//                                                                             // the local setup
//
//        int numberOfWriters = 3;
//        long sizeInBytes = (long) 3 * GiB_IN_BYTES;
//
//        ConcurrentLinkedQueue<BenchmarkResult> results = scheduleReadWriteBenchmarks(connection.dirAddress,
//                connection.mrcAddress, connection.userCredentials, connection.auth, connection.sslOptions, numberOfWriters, sizeInBytes);
//
//        /* cleaning up */
////        for (int i = 0; i < numberOfWriters; i++)
////            connection.deleteVolumeIfExisting(VOLUME_BASE_NAME + i);
//
//        Controller.scrub("47c551e1-2f30-42da-be3f-8c91c51dd15b", "");
//
//        /* print the results */
//        for (BenchmarkResult res : results) {
//            System.out.println(res);
//        }
//
//    }

}
