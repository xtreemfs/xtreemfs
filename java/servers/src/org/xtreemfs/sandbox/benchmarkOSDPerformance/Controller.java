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
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;

/**
 * @author jensvfischer
 */
public class Controller {

    static final int      MiB_IN_BYTES = 1024 * 1024;
    static final int      GiB_IN_BYTES = 1024 * 1024 * 1024;
    static ConnectionData connection;

    static void tryConnection() throws Exception {
        try {
            BenchmarkClientFactory.getNewClient(connection).getServiceByType(DIR.ServiceType.SERVICE_TYPE_OSD);
        } catch (IOException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, Controller.class,
                    "Failed to establish connection to servers. Errormessage: %s", e.getMessage());
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

    static ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(BenchmarkType benchmarkType, int numberOfThreads,
            long sizeInBytes) throws Exception {
        return startBenchmarks(benchmarkType, numberOfThreads, sizeInBytes,
                new KeyValuePair<Volume, LinkedList<String>>());
    }

    static ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(BenchmarkType benchmarkType, int numberOfThreads,
            long sizeInBytes, KeyValuePair<Volume, LinkedList<String>> volumeWithFiles) throws Exception {

        if (sizeInBytes % Benchmark.XTREEMFS_BLOCK_SIZE_IN_BYTES != 0)
            throw new IllegalArgumentException("Size must be in alignment with (i.e. divisible through) the block size");

        ConcurrentLinkedQueue<BenchmarkResult> results = null;
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        switch (benchmarkType) {
        case WRITE:
            results = WriteBenchmark.startWriteBenchmarks(numberOfThreads, sizeInBytes, threads);
            break;
        case READ:
            results = ReadBenchmark.startReadBenchmarks(numberOfThreads, sizeInBytes, threads);
            break;
        case RANDOM_IO_WRITE:
            break;
        case RANDOM_IO_READ:
            results = RandomReadBenchmark.startBenchmarks(numberOfThreads, sizeInBytes, threads);
            break;
        case RANDOM_IO_WRITE_FILEBASED:
            results = FilebasedRandomWriteBenchmark.startBenchmarks(numberOfThreads,
                    sizeInBytes, threads);
            break;
        case RANDOM_IO_READ_FILEBASED:
            results = FilebasedRandomReadBenchmark.startBenchmarks(numberOfThreads, sizeInBytes,
                    threads, volumeWithFiles);
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
        Logging.start(Logging.LEVEL_DEBUG, Logging.Category.tool);
        connection = new ConnectionData();
        tryConnection();

        VolumeManager volumeManager = VolumeManager.getInstance();

        int numberOfThreads = 1;
        long sizeInBytes = 10L * (long) MiB_IN_BYTES;

        volumeManager.createDefaultVolumes(numberOfThreads);

        // ConcurrentLinkedQueue<BenchmarkResult> wResults = startBenchmarks(BenchmarkType.WRITE,
        // numberOfThreads,
        // sizeInBytes);
        // printResults(wResults);
        //
        // volumeManager.createDefaultVolumes(numberOfThreads);
        //
        // ConcurrentLinkedQueue<BenchmarkResult> rResults = startBenchmarks(BenchmarkType.READ,
        // numberOfThreads,
        // sizeInBytes);
        // printResults(rResults);

        // volumeManager.createDefaultVolumes(numberOfThreads);
        //
        // ConcurrentLinkedQueue<BenchmarkResult> randResults = startBenchmarks(BenchmarkType.RANDOM_IO_READ,
        // numberOfThreads, sizeInBytes);
        // printResults(randResults);

        for (int i = 0; i < 1; i++) {
            ConcurrentLinkedQueue<BenchmarkResult> randResults = startBenchmarks(
                    BenchmarkType.RANDOM_IO_WRITE_FILEBASED, numberOfThreads, sizeInBytes);
            printResults(randResults);

            ConcurrentLinkedQueue<BenchmarkResult> randReadResults = startBenchmarks(
                    BenchmarkType.RANDOM_IO_READ_FILEBASED, numberOfThreads, sizeInBytes,
                    randResults.poll().volumeWithFiles);
            printResults(randReadResults);

        }
//
//        sizeInBytes = 10L * (long) MiB_IN_BYTES;
//
//        for (int i = 0; i < 5; i++) {
//            ConcurrentLinkedQueue<BenchmarkResult> randResults = startBenchmarks(
//                    BenchmarkType.RANDOM_IO_WRITE_FILEBASED, numberOfThreads, sizeInBytes);
//            printResults(randResults);
//
//            ConcurrentLinkedQueue<BenchmarkResult> randReadResults = startBenchmarks(
//                    BenchmarkType.RANDOM_IO_READ_FILEBASED, numberOfThreads, sizeInBytes,
//                    randResults.poll().volumeWithFiles);
//            printResults(randReadResults);
//
//        }

        volumeManager.deleteCreatedVolumes();
        volumeManager.scrub();

        // BenchmarkClientFactory.shutdownClients();
        System.exit(0);

    }

}
