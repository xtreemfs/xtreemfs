/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.utils.utils;

/**
 * Todo Zusammenspiel von sw und rr verbessern (kleiner sw sollte den das basefile nicht jedes mal killen)
 * 
 * @author jensvfischer
 * 
 */
public class Controller {

    static final int                                KiB_IN_BYTES                 = 1024;
    static final int                                MiB_IN_BYTES                 = 1024 * 1024;
    static final int                                GiB_IN_BYTES                 = 1024 * 1024 * 1024;
    static ConnectionData                           connection;
    private static Map<String, CLIParser.CliOption> options;

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

    /**
     * Starts the specified amount of benchmarks in parallel. Every benchmark is started within its own
     * thread. The method waits for all threads to finish.
     * 
     * @param benchmarkType
     * @param numberOfThreads
     *            number of benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return
     * @throws Exception
     */
    static ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(BenchmarkType benchmarkType, int numberOfThreads,
            long sizeInBytes) throws Exception {

        if (sizeInBytes % Benchmark.XTREEMFS_BLOCK_SIZE_IN_BYTES != 0)
            throw new IllegalArgumentException("Size must be in alignment with (i.e. divisible through) the block size");

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        for (int i = 0; i < numberOfThreads; i++) {
            Benchmark benchmark = BenchmarkFactory.createBenchmark(benchmarkType, VolumeManager.getInstance()
                    .getNextVolume(), connection);
            benchmark.startBenchmark(sizeInBytes, results, threads);
        }

        /* wait for all threads to finish */
        for (Thread thread : threads) {
            thread.join();
        }

        /* reset VolumeManager to prepare for possible consecutive benchmarks */
        VolumeManager.getInstance().reset();

        /* Set BenchmarkResult Type */
        for (BenchmarkResult res : results) {
            res.benchmarkType = benchmarkType;
            res.numberOfReadersOrWriters = numberOfThreads;
        }
        return results;
    }

    public static void main(String[] args) throws Exception {
        Logging.start(Logging.LEVEL_INFO, Logging.Category.tool);
        Params params = new Params();
        params.parseCLIOptions(args);

        displayUsageIfSet(params);

        connection = params.getConnectionData();
        tryConnection();

        setupVolumes(params);

        runBenchmarks(params);

        // volumeManager.deleteCreatedVolumes();
        // volumeManager.scrub();

        System.exit(0);

    }

    private static void setupVolumes(Params params) throws Exception {
        VolumeManager.init(connection);
        VolumeManager volumeManager = VolumeManager.getInstance();
        List<String> arguments = params.getArguments();

        if (arguments.size() == 0)
            volumeManager.createDefaultVolumes(params.getNumberOfThreads());
        else {
            // ToDo add check to verify that the number of volumes is in accordence with the number of threads
            String[] volumes = arguments.toArray(new String[arguments.size()]);
            volumeManager.openVolumes(volumes);
        }
    }

    private static void displayUsageIfSet(Params params){
        if (params.usageIsSet())
            params.usage();
    }

    private static void runBenchmarks(Params params) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> result;
        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        VolumeManager volumeManager = VolumeManager.getInstance();

        if (params.sequentialReadBenchmarkIsSet()) {
            result = startBenchmarks(BenchmarkType.WRITE, params.getNumberOfThreads(), params.getSequentialSizeInBytes());
            results.addAll(result);
        }

        if (params.sequentialWriteBenchmarkIsSet()) {
            result = startBenchmarks(BenchmarkType.READ, params.getNumberOfThreads(), params.getSequentialSizeInBytes());
            results.addAll(result);
        }

        if (params.randomReadBenchmarkIsSet()) {
            result = startBenchmarks(BenchmarkType.RANDOM_IO_READ, 1, params.getRandomSizeInBytes());
            results.addAll(result);
        }

        if (params.randomFilebasedWriteBenchmarkIsSet()) {
            result = startBenchmarks(BenchmarkType.RANDOM_IO_WRITE_FILEBASED, 1, params.getRandomSizeInBytes());
            results.addAll(result);
        }

        if (params.randomFilebasedReadBenchmarkIsSet()) {
            result = startBenchmarks(BenchmarkType.RANDOM_IO_READ_FILEBASED, 1, params.getRandomSizeInBytes());
            results.addAll(result);
        }

        printResults(results);
    }

}
