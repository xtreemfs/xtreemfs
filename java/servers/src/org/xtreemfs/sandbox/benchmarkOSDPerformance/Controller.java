/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;

/**
 * @author jensvfischer
 */
public class Controller {

    static final int               MiB_IN_BYTES = 1024 * 1024;
    static final int               GiB_IN_BYTES = 1024 * 1024 * 1024;
    static ConnectionData          connection;

    static void tryConnection() throws Exception {
        try {
            BenchmarkClientFactory.getNewClient(connection).getServiceByType(DIR.ServiceType.SERVICE_TYPE_OSD);
        } catch (IOException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, Controller.class, "Failed to establish connection to servers. Errormessage: %s", e.getMessage());
            Thread.yield(); // allows the logger to catch up
            e.printStackTrace();
            System.exit(42);
    } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, Controller.class, e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private static void printResults(ConcurrentLinkedQueue<BenchmarkResult> results) {
        /* print the results */
        for (BenchmarkResult res : results) {
            System.out.println(res);
        }
    }

    static void deleteVolumeIfExisting(String volumeName) throws Exception {
        AdminClient client = BenchmarkClientFactory.getNewClient(connection);
        if (new ArrayList<String>(Arrays.asList(client.listVolumeNames())).contains(volumeName)) {
            client.deleteVolume(connection.auth, connection.userCredentials, volumeName);
            Logging.logMessage(Logging.LEVEL_INFO, Controller.class, "Deleting volume %s", volumeName,
                    Logging.Category.tool);
        }
    }

    /**
     * Starts a benchmark run with the specified amount of write benchmarks in parallel. Every benchmark is
     * started within its own thread. The method waits for all threads to finish.
     * 
     * @param numberOfWriters
     *            number of write benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
    private static ConcurrentLinkedQueue<BenchmarkResult> startWriteBenchmarks(int numberOfWriters, long sizeInBytes,
            ConcurrentLinkedQueue<Thread> threads) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfWriters; i++) {
            OSDBenchmark benchmark = new WriteOSDBenchmark(VolumeManager.getInstance().getNextVolume(), connection);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }

        return results;
    }

    /**
     * Starts a benchmark run with the specified amount of read benchmarks in parallel. Every benchmark is
     * started within its own thread. The method waits for all threads to finish. Requires a
     * {@link WriteOSDBenchmark} first (because the ReadBench reads the files written by the WriteBench).
     * 
     * @param numberOfReaders
     *            number of read benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
    private static ConcurrentLinkedQueue<BenchmarkResult> startReadBenchmarks(int numberOfReaders, long sizeInBytes,
            ConcurrentLinkedQueue<Thread> threads) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfReaders; i++) {
            OSDBenchmark benchmark = new ReadOSDBenchmark(VolumeManager.getInstance().getNextVolume(), connection);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }
        return results;
    }

    private static ConcurrentLinkedQueue<BenchmarkResult> startRandomBenchmarks(int numberOfReaders, long sizeInBytes,
            ConcurrentLinkedQueue<Thread> threads) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfReaders; i++) {
            RandomReadOSDBenchmark benchmark = new RandomReadOSDBenchmark(VolumeManager.getInstance().getNextVolume(),
                    connection);
            benchmark.prepareBenchmark();
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }
        return results;
    }

    static ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(BenchmarkType benchmarkType, int numberOfThreads,
            long sizeInBytes) throws Exception {

        if (sizeInBytes % OSDBenchmark.XTREEMFS_BLOCK_SIZE_IN_BYTES != 0)
            throw new IllegalArgumentException("Size must be in alignment with (i.e. divisible through) the block size");

        ConcurrentLinkedQueue<BenchmarkResult> results = null;
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        switch (benchmarkType) {
        case READ:
            results = startReadBenchmarks(numberOfThreads, sizeInBytes, threads);
            break;
        case WRITE:
            results = startWriteBenchmarks(numberOfThreads, sizeInBytes, threads);
            break;
        case RANDOM_IO:
            results = startRandomBenchmarks(numberOfThreads, sizeInBytes, threads);
            break;
        }

        /* wait for all threads to finish */
        for (Thread thread : threads) {
            thread.join();
        }

        /* Set BenchmarkResult Type */
        for (BenchmarkResult res : results) {
            res.benchmarkType = benchmarkType;
            res.numberOfReadersOrWriters = numberOfThreads;
        }
        return results;
    }

    static ConnectionData getConnectionData() {
        return connection;
    }

    public static void main(String[] args) throws Exception {
        Logging.start(Logging.LEVEL_INFO, Logging.Category.tool);
        connection = new ConnectionData();
        tryConnection();

        VolumeManager volumeManager = VolumeManager.getInstance();

        int numberOfThreads = 1;
        long sizeInBytes = 3L * (long) MiB_IN_BYTES;

        volumeManager.createDefaultVolumes(numberOfThreads);

        ConcurrentLinkedQueue<BenchmarkResult> wResults = startBenchmarks(BenchmarkType.WRITE, numberOfThreads,
                sizeInBytes);
        printResults(wResults);

        volumeManager.createDefaultVolumes(numberOfThreads);

        ConcurrentLinkedQueue<BenchmarkResult> rResults = startBenchmarks(BenchmarkType.READ, numberOfThreads,
                sizeInBytes);
        printResults(rResults);

        // volumeManager.createDefaultVolumes(numberOfThreads);
        //
        // ConcurrentLinkedQueue<BenchmarkResult> randResults = startBenchmarks(BenchmarkType.RANDOM_IO,
        // numberOfThreads,
        // 10L * (long) MiB_IN_BYTES);
        // printResults(randResults);

        volumeManager.deleteCreatedVolumes();
        volumeManager.scrub();

//        BenchmarkClientFactory.shutdownClients();
        System.exit(0);

    }

}
