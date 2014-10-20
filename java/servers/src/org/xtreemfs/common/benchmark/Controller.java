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
import java.util.ArrayList;
import java.util.concurrent.*;

import static org.xtreemfs.common.benchmark.BenchmarkUtils.BenchmarkType;
import static org.xtreemfs.foundation.logging.Logging.LEVEL_INFO;
import static org.xtreemfs.foundation.logging.Logging.logError;
import static org.xtreemfs.foundation.logging.Logging.logMessage;

/**
 * Controller for the benchmark library.
 * <p/>
 *
 * The {@link Controller} and {@link BenchmarkConfig} represent the API to the benchmark library.
 *
 * @author jensvfischer
 *
 */
public class Controller {

    private BenchmarkConfig config;
    private ClientManager   clientManager;
    private VolumeManager   volumeManager;
    private ThreadPoolExecutor threadPool;

    /**
     * Create a new controller object.
     * 
     * @param config
     *            The parameters to be used for the benchmark.
     */
    public Controller(BenchmarkConfig config) throws Exception {
        this.config = config;
        this.clientManager = new ClientManager(config);
        this.volumeManager = new VolumeManager(config, clientManager.getNewClient());
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandlerBenchmark(this));
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
     * The volumes will be created with the options given by {@link BenchmarkConfig}.
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
     * Starts sequential write benchmarks with the parameters specified in the {@link BenchmarkConfig}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ArrayList<BenchmarkResult> startSequentialWriteBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.SEQ_WRITE);
        return startBenchmark(size, numberOfThreads, BenchmarkType.SEQ_WRITE);
    }

    /**
     * Starts unaligned sequential write benchmarks with the parameters specified in the {@link BenchmarkConfig}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ArrayList<BenchmarkResult> startUnalignedSequentialWriteBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.SEQ_UNALIGNED_WRITE);
        return startBenchmark(size, numberOfThreads, BenchmarkType.SEQ_UNALIGNED_WRITE);
    }

    /**
     * Starts sequential read benchmarks with the parameters specified in the {@link BenchmarkConfig}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ArrayList<BenchmarkResult> startSequentialReadBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.SEQ_READ);
        return startBenchmark(size, numberOfThreads, BenchmarkType.SEQ_READ);
    }

    /**
     * Starts random write benchmarks with the parameters specified in the {@link BenchmarkConfig}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ArrayList<BenchmarkResult> startRandomWriteBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.RAND_WRITE);
        return startBenchmark(size, numberOfThreads, BenchmarkType.RAND_WRITE);
    }

    /**
     * Starts random read benchmarks with the parameters specified in the {@link BenchmarkConfig}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ArrayList<BenchmarkResult> startRandomReadBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.RAND_READ);
        return startBenchmark(size, numberOfThreads, BenchmarkType.RAND_READ);
    }

    /**
     * Starts filebased write benchmarks with the parameters specified in the {@link BenchmarkConfig}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ArrayList<BenchmarkResult> startFilebasedWriteBenchmark(long size, int numberOfThreads) throws Exception {
        verifySizesAndThreads(size, numberOfThreads, BenchmarkType.FILES_WRITE);
        return startBenchmark(size, numberOfThreads, BenchmarkType.FILES_WRITE);
    }

    /**
     * Starts filebased read benchmarks with the parameters specified in the {@link BenchmarkConfig}. <br/>
     *
     * @return the results of the benchmark (see {@link BenchmarkResult})
     * @throws Exception
     */
    public ArrayList<BenchmarkResult> startFilebasedReadBenchmark(long size, int numberOfThreads) throws Exception {
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
    public static String[] getDefaultDir() {
        String[] dirAddresses;
        DefaultDirConfig cfg = null;
        try {
            cfg = new DefaultDirConfig();
            dirAddresses = cfg.getDirectoryServices();
            return dirAddresses;
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
     * {@link BenchmarkConfig}.
     *
     * @throws Exception
     */
    public void teardown() throws Exception {
        deleteVolumesAndFiles();
        if (config.isOsdCleanup())
            volumeManager.cleanupOSD();
        shutdownClients();
        shutdownThreadPool();
    }

    private void verifySizesAndThreads(long size, int threads, BenchmarkType type) {
        if ((type == BenchmarkType.SEQ_READ) || (type == BenchmarkType.SEQ_WRITE)) {
            if (size % (config.getStripeSizeInBytes() * config.getStripeWidth()) != 0)
                throw new IllegalArgumentException("size of " + type
                        + " must satisfy: size mod (stripeSize * stripeWidth) == 0");
            if (size < config.getChunkSizeInBytes())
                throw new IllegalArgumentException("Chunksize < size of " + type);
        }
        if ((type == BenchmarkType.RAND_READ) || (type == BenchmarkType.RAND_WRITE)) {
            if (config.getBasefileSizeInBytes() < size)
                throw new IllegalArgumentException("Basefile < size of " + type);
            if (size < config.getChunkSizeInBytes())
                throw new IllegalArgumentException("Chunksize < size of " + type);
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
    private ArrayList<BenchmarkResult> startBenchmark(long size, int numberOfThreads, BenchmarkType benchmarkType)
            throws Exception {

        checkThreadPool(numberOfThreads);

        CompletionService<BenchmarkResult> completionService = new ExecutorCompletionService<BenchmarkResult>(threadPool);
        ArrayList<Future<BenchmarkResult>> futures = new ArrayList<Future<BenchmarkResult>>();

        /* create and start all benchmark tasks (i.e. submit to CompletionService) */
        for (int i = 0; i < numberOfThreads ; i++) {
            AbstractBenchmark benchmark = BenchmarkFactory.createBenchmark(size, benchmarkType, config, clientManager.getNewClient(), volumeManager);
            benchmark.prepareBenchmark();
            Future<BenchmarkResult> future = completionService.submit(benchmark);
            futures.add(future);
        }

        ArrayList<BenchmarkResult> results = awaitCompletion(numberOfThreads, completionService, futures);

        /* Set BenchmarkResult type and number of threads */
        for (BenchmarkResult res : results) {
            res.setBenchmarkType(benchmarkType);
            res.setNumberOfReadersOrWriters(numberOfThreads);
        }

        /* reset VolumeManager to prepare for possible consecutive benchmarks */
        volumeManager.reset();

        return results;
    }

    private void checkThreadPool(int numberOfThreads) throws InterruptedException {
        /* check if a thread pool is already instantiated */
        if (null == threadPool)
            threadPool = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>());
        /* check if thread pool has the needed number of threads, adjust if necessary */
        else if (threadPool.getPoolSize() != numberOfThreads){
            threadPool.setCorePoolSize(numberOfThreads);
            threadPool.setMaximumPoolSize(numberOfThreads);
        }
    }


    private ArrayList<BenchmarkResult> awaitCompletion(int numberOfThreads, CompletionService<BenchmarkResult> completionService, ArrayList<Future<BenchmarkResult>> futures) throws Exception {
        ArrayList<BenchmarkResult> results = new ArrayList<BenchmarkResult>(numberOfThreads);
        Exception exception = null;
        /* wait for all threads to finish */
        for (int i = 0; i < numberOfThreads; i++) {
            try {
                Future<BenchmarkResult> benchmarkResultFuture = completionService.take();
                futures.remove(benchmarkResultFuture);
                BenchmarkResult result = benchmarkResultFuture.get();
                results.add(result);
            } catch (ExecutionException e) {
                logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this, "An exception occurred within an benchmark task.");
                logError(Logging.LEVEL_ERROR, this, e.getCause());
                /* cancel all other running benchmark tasks in case of failure in one task */
                for (Future future : futures) {future.cancel(true);}
                /*
                 * Future.cancel(true) works by setting the interrupted flag. In some cases the cancellation doesn't
                 * work (the query of the interrupted status returns false). Most likely the interrupted status is
                 * consumed somewhere without causing the task to stop or to reach the toplevel task code (the code in
                 * the XYBenchmark classes). This could be due to catching an InterruptedException exception or calling
                 * Thread.interrupted() (both consumes the flag status) without reestablishing the status with
                 * Thread.currentThread().interrupt(). Therefore an additional cancellation needs to be deployed...
                 */
                AbstractBenchmark.cancel();
                exception = e;
            } catch (CancellationException e) {
                // consume (planned cancellation from calling future.cancel())
                logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Benchmark task has been canceled due to an exception in another benchmark task.");
            }
        }
        if (null != exception)
            throw new BenchmarkFailedException(exception.getCause());
        return results;
    }

    /* delete all created volumes and files depending on the noCleanup options */
    void deleteVolumesAndFiles() {
        if (!config.isNoCleanup() && !config.isNoCleanupVolumes()) {
            volumeManager.deleteCreatedFiles(); // is needed in case no volume was created
            volumeManager.deleteCreatedVolumes();
        } else if (!config.isNoCleanup())
            volumeManager.deleteCreatedFiles();
    }

    void shutdownClients() {
        clientManager.shutdownClients();
    }

    void shutdownThreadPool(){
        if (threadPool != null)
            threadPool.shutdownNow();
    }
}
