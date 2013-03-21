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

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;

/**
 * TODO (jvf) Zusammenspiel von sw und rr verbessern (kleiner sw sollte das basefile nicht jedes mal killen)
 * 
 * @author jensvfischer
 * 
 */
public class Controller {

    static final int KiB_IN_BYTES = 1024;
    static final int MiB_IN_BYTES = 1024 * 1024;
    static final int GiB_IN_BYTES = 1024 * 1024 * 1024;
    private Params   params;

    public Controller(Params params) {
        this.params = params;
    }

    private void setup() {
    }

    /**
     * Starts the specified amount of benchmarks in parallel. Every benchmark is started within its own
     * thread. The method waits for all threads to finish.
     * 
     * @param benchmarkType
     * @param numberOfThreads
     *            number of benchmarks run in parallel
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return
     * @throws Exception
     */
    ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(BenchmarkType benchmarkType, int numberOfThreads) throws Exception {

//        TODO (jvf) Check für benchmarksize % blocksize an anderer Stelle einbauen
//        if (sizeInBytes % Benchmark.XTREEMFS_BLOCK_SIZE_IN_BYTES != 0)
//            throw new IllegalArgumentException("Size must be in alignment with (i.e. divisible through) the block size");

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
        VolumeManager.getInstance().deleteCreatedVolumes();
        VolumeManager.getInstance().scrub();
        BenchmarkClientFactory.shutdownClients();
    }

    public static void printResults(ConcurrentLinkedQueue<BenchmarkResult> results) {
        /* print the results */
        for (BenchmarkResult res : results) {
            System.out.println(res);
        }
    }

    private void tryConnection() {
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
