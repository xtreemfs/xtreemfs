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

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;

/**
*
* @author jensvfischer
*
*/
public class Controller {

    private Params params;

    public Controller(Params params) {
        this.params = params;
    }

    private void setup() {
    }

    ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(BenchmarkType benchmarkType, int numberOfThreads)
            throws Exception {
        ConcurrentLinkedQueue<BenchmarkResult> result;
        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        for (int i = 0; i < params.numberOfRepetitions; i++) {
            result = startBenchmark(benchmarkType, numberOfThreads);
            results.addAll(result);
        }
        return results;
    }

    /**
     * Starts the specified amount of benchmarks in parallel. Every benchmark is started within its own
     * thread. The method waits for all threads to finish.
     *
     * @param benchmarkType
     * @param numberOfThreads
     *            number of benchmarks run in parallel Size of the benchmark in bytes. Must be in alignment
     *            with (i.e. divisible through) the block size (128 KiB).
     * @return
     * @throws Exception
     */
    ConcurrentLinkedQueue<BenchmarkResult> startBenchmark(BenchmarkType benchmarkType, int numberOfThreads)
            throws Exception {

        // TODO (jvf) Check für benchmarksize % blocksize an anderer Stelle einbauen
        // if (sizeInBytes % Benchmark.stripeWidth != 0)
        // throw new
        // IllegalArgumentException("Size must be in alignment with (i.e. divisible through) the block size");

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        for (int i = 0; i < numberOfThreads; i++) {
            Benchmark benchmark = BenchmarkFactory.createBenchmark(benchmarkType, VolumeManager.getInstance()
                    .getNextVolume(), params);
            benchmark.startBenchmark(results, threads);
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

    public void teardown() throws Exception {
        deleteVolumesAndFiles();
        BenchmarkClientFactory.shutdownClients();
    }

    private void deleteVolumesAndFiles() throws Exception {
        VolumeManager volumeManager = VolumeManager.getInstance();
        if (!params.noCleanup && !params.noCleanupOfVolumes){
            volumeManager.deleteCreatedFiles();   // is needed in case no volume was created!
            volumeManager.deleteCreatedVolumes();
        }
        else if (!params.noCleanup)
           volumeManager.deleteCreatedFiles();

        volumeManager.scrub();
    }

    public static void printResults(ConcurrentLinkedQueue<BenchmarkResult> results) {
        System.out.println("Type;NumberOfParallelThreads;TimeInSec;MiB/Sec;DataWrittenInBytes;ByteCount");
        /* print the results */
        for (BenchmarkResult res : results) {
            System.out.println(res);
        }
    }

    public void tryConnection() {
        try {
            BenchmarkClientFactory.getNewClient(params).getServiceByType(DIR.ServiceType.SERVICE_TYPE_OSD);
        } catch (IOException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, Controller.class,
                    "Failed to establish connection to servers. Errormessage: %s", e.getMessage());
            Thread.yield(); // allows the logger to catch up
            e.printStackTrace();
            System.exit(42);
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, Controller.class, e.getMessage());
            e.printStackTrace();
        }
    }

    void setupVolumes(String... volumeNames) throws Exception {
        VolumeManager.init(this.params);
        VolumeManager volumeManager = VolumeManager.getInstance();

        if (volumeNames.length == 0)
            volumeManager.createDefaultVolumes(params.numberOfThreads);
        else if (volumeNames.length != params.numberOfThreads) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this,
                    "Number of volume names must be in accordance with number of benchmarks run in parallel");
            throw new IllegalArgumentException(
                    "Number of volume names must be in accordance with number of benchmarks run in parallel");
        } else
            volumeManager.openVolumes(volumeNames);
    }

    public static void main(String[] args) throws Exception {
        Logging.start(Logging.LEVEL_INFO, Logging.Category.tool);

        Params params = new ParamsBuilder().build();

        Controller controller = new Controller(params);
        controller.tryConnection();
        controller.setupVolumes();
        ConcurrentLinkedQueue results = controller.startBenchmarks(BenchmarkType.WRITE, 1);
        printResults(results);
        controller.teardown();
    }

}
