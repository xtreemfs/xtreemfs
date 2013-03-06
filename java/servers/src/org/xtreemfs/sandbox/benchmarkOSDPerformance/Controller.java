/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.foundation.logging.Logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author jensvfischer
 */
public class Controller {
    static LinkedList<AdminClient> clients = new LinkedList<AdminClient>();
    static final String VOLUME_BASE_NAME = "performanceTest";

    static final int      MiB_IN_BYTES                 = 1024 * 1024;
    static final int      GiB_IN_BYTES                 = 1024 * 1024 * 1024;
    static ConnectionData connection;


    /* create and start a client and add the client to the client collection */
    static AdminClient getClient(ConnectionData connection) throws Exception {
        AdminClient client = ClientFactory.createAdminClient(connection.dirAddress, connection.userCredentials, connection.sslOptions, connection.options);
        clients.add(client);
        client.start();
        return client;
    }

    /* shutdown all clients */
    static void shutdownClients() {
        for (AdminClient client : clients)
            client.shutdown();
        clients = new LinkedList<AdminClient>();
    }

    private static void printResults(ConcurrentLinkedQueue<BenchmarkResult> results) {
    /* print the results */
        for (BenchmarkResult res : results) {
            System.out.println(res);
        }
    }

    static void deleteVolumeIfExisting(String volumeName) throws Exception {
        AdminClient client = getClient(connection);
        if (new ArrayList<String>(Arrays.asList(client.listVolumeNames())).contains(volumeName)) {
            client.deleteVolume(connection.auth, connection.userCredentials, volumeName);
            Logging.logMessage(Logging.LEVEL_INFO, Controller.class, "Deleting volume %s", volumeName, Logging.Category.tool);
        }
    }

    /* Performs cleanup on a OSD (because deleting the volume does not delete the files in the volume) */
    static void scrub(String osd, String pwd) throws Exception {

        ConnectionData connection = new ConnectionData();


        Logging.logMessage(Logging.LEVEL_INFO, BenchmarkOSDPerformance.class, "Starting cleanup of osd %s", osd, Logging.Category.tool);

        AdminClient client = getClient(connection);
        client.startCleanUp(osd, pwd, true, true, true);
        while (client.isRunningCleanUp(osd, pwd)) {
            Thread.sleep(1000);
        }

        Logging.logMessage(Logging.LEVEL_DEBUG, BenchmarkOSDPerformance.class, "Cleanup Result: %s",
                client.getCleanUpResult(osd, pwd), Logging.Category.tool);
        Logging.logMessage(Logging.LEVEL_INFO, BenchmarkOSDPerformance.class, "Finished cleanup of %s", osd, Logging.Category.tool);

    }


    /**
     * Starts a benchmark run with the specified amount of write benchmarks in parallel. Every benchmark is
     * started within its own thread. The method waits for all threads to finish.
     * @param numberOfWriters number of write benchmarks run in parallel
     * @param sizeInBytes     Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *                        size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
    private static ConcurrentLinkedQueue<BenchmarkResult> startWriteBenchmarks(int numberOfWriters, long sizeInBytes, ConcurrentLinkedQueue<Thread> threads)
            throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfWriters; i++) {
            BenchmarkOSDPerformance benchmark = new WriteBenchmarkOSDPerformance(BenchmarkOSDPerformance.VOLUME_BASE_NAME + i, connection);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }

        return results;
    }

    /**
     * Starts a benchmark run with the specified amount of read benchmarks in parallel. Every benchmark is
     * started within its own thread. The method waits for all threads to finish. Requires a
     * {@link org.xtreemfs.sandbox.benchmarkOSDPerformance.WriteBenchmarkOSDPerformance} first (because the ReadBench reads the files written by the
     * WriteBench).
     * @param numberOfReaders
     *            number of read benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
    private static ConcurrentLinkedQueue<BenchmarkResult> startReadBenchmarks(int numberOfReaders, long sizeInBytes, ConcurrentLinkedQueue<Thread> threads)
            throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfReaders; i++) {
            BenchmarkOSDPerformance benchmark = new ReadBenchmarkOSDPerformance(BenchmarkOSDPerformance.VOLUME_BASE_NAME + i, connection);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }
        return results;
    }

    static ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(BenchmarkType benchmarkType, int numberOfThreads, long sizeInBytes)
            throws Exception {

        if (sizeInBytes % BenchmarkOSDPerformance.XTREEMFS_BLOCK_SIZE_IN_BYTES != 0)
            throw new IllegalArgumentException("Size must be in alignment with (i.e. divisible through) the block size");

        ConcurrentLinkedQueue<BenchmarkResult> results = null;
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();


        switch (benchmarkType){
            case READ:
                results = startReadBenchmarks(numberOfThreads, sizeInBytes, threads);
                break;
            case WRITE:
                results = startWriteBenchmarks(numberOfThreads, sizeInBytes, threads);
                break;
            case RANDOM_IO:
                results = startRandomBenchmark(numberOfThreads, sizeInBytes);
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

    private static ConcurrentLinkedQueue<BenchmarkResult> startRandomBenchmark(int numberOfThreads, long sizeInBytes) throws Exception {
        return RandomReadBenchmarkOSDPerformance.startRandomBenchmark(numberOfThreads, sizeInBytes);
    }


    public static void main(String[] args) throws Exception {
        connection = new ConnectionData();
        Logging.start(Logging.LEVEL_INFO, Logging.Category.tool);

        int numberOfThreads = 1;
        long sizeInBytes = 3L * (long)  GiB_IN_BYTES;

        for (int i =0; i<=numberOfThreads;i++)
            deleteVolumeIfExisting(VOLUME_BASE_NAME+i);



        ConcurrentLinkedQueue<BenchmarkResult> wResults = startBenchmarks(BenchmarkType.WRITE, numberOfThreads, sizeInBytes);
        printResults(wResults);
        ConcurrentLinkedQueue<BenchmarkResult> rResults = startBenchmarks(BenchmarkType.READ, numberOfThreads, sizeInBytes);
        printResults(rResults);


        ConcurrentLinkedQueue<BenchmarkResult> randResults = startBenchmarks(BenchmarkType.RANDOM_IO, numberOfThreads, 3L*(long)MiB_IN_BYTES);
        printResults(randResults);



        for (int i =0; i<=numberOfThreads;i++)
            deleteVolumeIfExisting(VOLUME_BASE_NAME+i);

        scrub("47c551e1-2f30-42da-be3f-8c91c51dd15b", "");

        shutdownClients();

    }



}
