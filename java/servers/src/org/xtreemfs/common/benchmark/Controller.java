/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

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

    private Config        config;
    private ClientManager clientManager;
    private VolumeManager volumeManager;

    /**
     * Create a new controller object.
     * 
     * @param config
     *            The parameters to be used for the benchmark.
     */
    public Controller(Config config) throws Exception {
        this.config = config;
        this.clientManager = new ClientManager(config);
        this.volumeManager = new VolumeManager(config, clientManager.getNewClient());
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
        if (volumeNames.length == 0)
            volumeManager.createDefaultVolumes(config.getNumberOfThreads());
        else
            volumeManager.openVolumes(volumeNames);
    }

    /**
     * Starts sequential write benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startSequentialWriteBenchmark() throws Exception {
        return repeatBenchmark(BenchmarkUtils.BenchmarkType.SEQ_WRITE);
    }

    /**
     * Starts sequential read benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startSequentialReadBenchmark() throws Exception {
        return repeatBenchmark(BenchmarkUtils.BenchmarkType.SEQ_READ);
    }

    /**
     * Starts random write benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startRandomWriteBenchmark() throws Exception {
        return repeatBenchmark(BenchmarkUtils.BenchmarkType.RAND_WRITE);
    }

    /**
     * Starts random read benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startRandomReadBenchmark() throws Exception {
        return repeatBenchmark(BenchmarkUtils.BenchmarkType.RAND_READ);
    }

    /**
     * Starts filebased write benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startFilebasedWriteBenchmark() throws Exception {
        return repeatBenchmark(BenchmarkUtils.BenchmarkType.FILES_WRITE);
    }

    /**
     * Starts filebased read benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startFilebasedReadBenchmark() throws Exception {
        return repeatBenchmark(BenchmarkUtils.BenchmarkType.FILES_READ);
    }

    public void tryConnection() throws Exception {
        try {
            clientManager.getNewClient().getServiceByType(DIR.ServiceType.SERVICE_TYPE_OSD);
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
        clientManager.shutdownClients();
        if (config.isOsdCleanup())
            volumeManager.cleanupOSD();
    }

    /* Repeat a benchmark multiple times and pack the results. */
    private ConcurrentLinkedQueue<BenchmarkResult> repeatBenchmark(BenchmarkUtils.BenchmarkType benchmarkType)
            throws Exception {
        ConcurrentLinkedQueue<BenchmarkResult> result;
        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        for (int i = 0; i < config.getNumberOfRepetitions(); i++) {
            result = startBenchmark(benchmarkType);
            results.addAll(result);
        }
        return results;
    }

    /*
     * Starts benchmarks in parallel. Every benchmark is started within its own thread. The method waits for all threads
     * to finish.
     */
    private ConcurrentLinkedQueue<BenchmarkResult> startBenchmark(BenchmarkUtils.BenchmarkType benchmarkType)
            throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        for (int i = 0; i < config.getNumberOfThreads(); i++) {
            AbstractBenchmark benchmark = BenchmarkFactory.createBenchmark(benchmarkType, config, clientManager.getNewClient(), volumeManager);
            benchmark.startBenchmarkThread(results, threads);
        }

        /* wait for all threads to finish */
        for (Thread thread : threads) {
            thread.join();
        }

        /* reset VolumeManager to prepare for possible consecutive benchmarks */
        volumeManager.reset();

        /* Set BenchmarkResult Type */
        for (BenchmarkResult res : results) {
            res.setBenchmarkType(benchmarkType);
            res.setNumberOfReadersOrWriters(config.getNumberOfThreads());
        }
        return results;
    }

    /* delete all created volumes and files depending on the noCleanup options */
    private void deleteVolumesAndFiles() throws Exception {
        if (!config.isNoCleanup() && !config.isNoCleanupOfVolumes()) {
            volumeManager.deleteCreatedFiles(); // is needed in case no volume was created
            volumeManager.deleteCreatedVolumes();
        } else if (!config.isNoCleanup())
            volumeManager.deleteCreatedFiles();
    }
}
