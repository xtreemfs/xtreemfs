/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import static org.xtreemfs.foundation.logging.Logging.LEVEL_INFO;
import static org.xtreemfs.foundation.logging.Logging.logMessage;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.utils.DefaultDirConfig;

/**
 * Controller for the benchmark library.
 * <p/>
 * 
 * The {@link Controller}, the {@link ConfigBuilder} and {@link Config} represent the API to the benchmark library.
 * 
 * @author jensvfischer
 * 
 */
public class Controller {

    private Config config;

    /**
     * Create a new controller object.
     * 
     * @param config
     *            The parameters to be used for the benchmark.
     */
    public Controller(Config config) {
        this.config = config;
    }

    /**
     * Create and open the volumes needed for the benchmarks. <br/>
     * If no volume names are given, default volumes are created. The number of default volumes to be created is
     * determined by {@link Config#numberOfThreads}. <br/>
     * If the volumeNames given are already existing volumes, the volumes are only opened.
     * 
     * @param volumeNames
     *            the volumes to be created/opened
     * @throws Exception
     */
    public void setupVolumes(String... volumeNames) throws Exception {
        VolumeManager.init(this.config);
        VolumeManager volumeManager = VolumeManager.getInstance();

        if (volumeNames.length == 0)
            volumeManager.createDefaultVolumes(config.numberOfThreads);
        else
            volumeManager.openVolumes(volumeNames);
    }

    /**
     * Repeat a benchmark multiple times.
     * 
     * @param benchmarkType
     *            the type of the benchmark to be repeated
     * @param numberOfThreads
     *            the number of benchmarks run in parallel
     * @return the results of all repetitions of all the run benchmarks
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(BenchmarkType benchmarkType, int numberOfThreads)
            throws Exception {
        ConcurrentLinkedQueue<BenchmarkResult> result;
        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        for (int i = 0; i < config.numberOfRepetitions; i++) {
            result = startBenchmark(benchmarkType, numberOfThreads);
            results.addAll(result);
        }
        return results;
    }

    /**
     * Starts the specified amount of benchmarks in parallel. Every benchmark is started within its own thread. The
     * method waits for all threads to finish.
     * 
     * @param benchmarkType
     * @param numberOfThreads
     *            number of benchmarks run in parallel Size of the benchmark in bytes. Must be in alignment with (i.e.
     *            divisible through) the block size (128 KiB).
     * @return the results of the benchmark
     * @throws Exception
     */
    ConcurrentLinkedQueue<BenchmarkResult> startBenchmark(BenchmarkType benchmarkType, int numberOfThreads)
            throws Exception {

        if (config.sequentialSizeInBytes % (config.stripeSizeInBytes * config.stripeWidth) != 0
                || config.randomSizeInBytes % (config.stripeSizeInBytes * config.stripeWidth) != 0)
            throw new IllegalArgumentException("Size must satisfy: size mod (stripeSize * stripeWidth) == 0");

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        for (int i = 0; i < numberOfThreads; i++) {
            AbstractBenchmark benchmark = BenchmarkFactory.createBenchmark(benchmarkType, VolumeManager.getInstance()
                    .getNextVolume(), config);
            benchmark.startBenchmarkThread(results, threads);
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

	public void tryConnection() throws Exception {
        try {
            ClientManager.getInstance().getNewClient(config).getServiceByType(DIR.ServiceType.SERVICE_TYPE_OSD);
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, Controller.class,
                    "Failed to establish connection to DIR server.");
            throw e;
        }
    }

    /**
     * Get the DIR address from default_dir
     * 
     * @return the DIR address, or null if default_dir wasn't found or couldn't be accessed.
     */
    public static String getDefaultDir() {
        String[] dirAddresses;
        DefaultDirConfig cfg = null;
        try {
            cfg = new DefaultDirConfig();
            dirAddresses = cfg.getDirectoryServices();
            return dirAddresses[0];
        } catch (IOException e) {
            logMessage(LEVEL_INFO, Logging.Category.tool, Controller.class,
                    "Could not access or find Default DIR Config in %s. Using default (localhost).",
                    DefaultDirConfig.DEFAULT_DIR_CONFIG);
            return null;
        }
    }

    /**
     * Deletes all created volumes and files and shuts down all clients. This method should be called when all
     * benchmarks are finished. The deletion of the volumes and files is regulated by the noCleanup options in
     * {@link Config}.
     * 
     * @throws Exception
     */
    public void teardown() throws Exception {
        deleteVolumesAndFiles();
        ClientManager.getInstance().shutdownClients();
        if (config.osdCleanup)
            VolumeManager.getInstance().cleanupOSD();
    }

    /* delete all created volumes and files depending on the noCleanup options */
    private void deleteVolumesAndFiles() throws Exception {
        VolumeManager volumeManager = VolumeManager.getInstance();
        if (!config.noCleanup && !config.noCleanupOfVolumes) {
            volumeManager.deleteCreatedFiles(); // is needed in case no volume was created
            volumeManager.deleteCreatedVolumes();
        } else if (!config.noCleanup)
            volumeManager.deleteCreatedFiles();
    }

    /**
     * Example of how to use the controller.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Logging.start(Logging.LEVEL_INFO, Logging.Category.tool);

        /* use the default params */
        Config config = new ConfigBuilder().build();

        /* instantiate a new controller */
        Controller controller = new Controller(config);

        /* test the connection */
        controller.tryConnection();

        /* setup the volumes */
        controller.setupVolumes();

        /* start a sequential write benchmark with one thread */
        ConcurrentLinkedQueue results = controller.startBenchmarks(BenchmarkType.SEQ_WRITE, 1);

        /* print out the results */
        xtfs_benchmark.printResults(results);

        /* shutdown controller */
        controller.teardown();
    }

}
