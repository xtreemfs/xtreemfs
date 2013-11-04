/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.utils.DefaultDirConfig;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.xtreemfs.common.benchmark.BenchmarkUtils.BenchmarkType;
import static org.xtreemfs.foundation.logging.Logging.LEVEL_INFO;
import static org.xtreemfs.foundation.logging.Logging.logMessage;

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
     * If the volumeNames given are already existing volumes, the volumes are only opened. <br/>
     *
     * @param volumeNames
     *            the volumes to be created/opened
     * @throws Exception
     */
    public void setupVolumes(String... volumeNames) throws Exception {
        if (volumeNames.length < 1)
                throw new IllegalArgumentException("Number of volumes < 1");
        else
            volumeManager.openVolumes(volumeNames);
    }

    /**
     * Create and open default volumes for the benchmarks. <br/>
     * The volumes will be created with the options given by {@link Config}.
     *
     * @param numberOfVolumes the number of volumes to be created
     * @throws Exception
     */
    public void setupDefaultVolumes(int numberOfVolumes) throws Exception {
        if (numberOfVolumes < 1)
            throw new IllegalArgumentException("Number of volumes < 1");
        else
            volumeManager.createDefaultVolumes(numberOfVolumes);
    }


    /**
     * Starts sequential write benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startSequentialWriteBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.SEQ_WRITE);
        return startBenchmark(size, numberOfThreads, BenchmarkType.SEQ_WRITE);
    }

    /**
     * Starts sequential read benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startSequentialReadBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.SEQ_READ);
        return startBenchmark(size, numberOfThreads, BenchmarkType.SEQ_READ);
    }

    /**
     * Starts random write benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startRandomWriteBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.RAND_WRITE);
        return startBenchmark(size, numberOfThreads, BenchmarkType.RAND_WRITE);
    }

    /**
     * Starts random read benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startRandomReadBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.RAND_READ);
        return startBenchmark(size, numberOfThreads, BenchmarkType.RAND_READ);
    }

    /**
     * Starts filebased write benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startFilebasedWriteBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.FILES_WRITE);
        return startBenchmark(size, numberOfThreads, BenchmarkType.FILES_WRITE);
    }

    /**
     * Starts filebased read benchmarks with the parameters specified in the {@link Config}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ConcurrentLinkedQueue<BenchmarkResult> startFilebasedReadBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.FILES_READ);
        return startBenchmark(size, numberOfThreads, BenchmarkType.FILES_READ);
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
        if (config.isOsdCleanup())
            volumeManager.cleanupOSD();
        clientManager.shutdownClients();
    }

    private void verifySizesAndThreads(long size, int threads, BenchmarkType type) {
        if ((type == BenchmarkType.SEQ_READ) || (type == BenchmarkType.SEQ_WRITE)) {
            if (size % (config.getStripeSizeInBytes() * config.getStripeWidth()) != 0)
                throw new IllegalArgumentException("size of " + type
                        + " must satisfy: size mod (stripeSize * stripeWidth) == 0");
        }
        if ((type == BenchmarkType.RAND_READ) || (type == BenchmarkType.RAND_WRITE)) {
            if (config.getBasefileSizeInBytes() < size)
                throw new IllegalArgumentException("Basefile < size of " + type);
        }
        if ((type == BenchmarkType.FILES_WRITE) || (type == BenchmarkType.FILES_READ)) {
            if (size % config.getFilesize() != 0)
                throw new IllegalArgumentException("Size of " + type + " must satisfy: size mod filesize == 0");
        }
        if (volumeManager.getVolumes().size() < threads )
            throw new IllegalArgumentException("Less volumes than parallel threads");
    }

    /*
     * Starts benchmarks in parallel. Every benchmark is started within its own thread. The method waits for all threads
     * to finish.
     */
    private ConcurrentLinkedQueue<BenchmarkResult> startBenchmark(long size, int numberOfThreads, BenchmarkType benchmarkType)
            throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        for (int i = 0; i < numberOfThreads ; i++) {
            AbstractBenchmark benchmark = BenchmarkFactory.createBenchmark(size, benchmarkType, config, clientManager.getNewClient(), volumeManager);
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
            res.setNumberOfReadersOrWriters(numberOfThreads);
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
