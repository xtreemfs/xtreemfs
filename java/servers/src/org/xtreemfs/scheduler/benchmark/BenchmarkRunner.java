/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.benchmark;

import org.xtreemfs.common.benchmark.*;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Use the benchmark library to measure the performance of an OSD.
 *
 * @author jensvfischer
 */
public class BenchmarkRunner {

    private final int maxNumberOfRepetitions;
    private final long sequentialSize;
    private final long randomSize;
    private final int maxNumberOfThreads;
    private final Config config;

    private final Controller controller;
    private ConcurrentLinkedQueue<BenchmarkResult> rawResults;

    public BenchmarkRunner(Config config, long sequentialSize, long randomSize, int maxNumberOfThreads, int maxNumberOfRepetitions) throws Exception {
        this.config = config;
        this.randomSize = randomSize;
        this.sequentialSize = sequentialSize;
        this.maxNumberOfThreads = maxNumberOfThreads;
        this.maxNumberOfRepetitions = maxNumberOfRepetitions;
        this.controller = new Controller(config);
        this.rawResults = new ConcurrentLinkedQueue<BenchmarkResult>();
    }

    /**
     * Run a suite of benchmarks to measure the performance of an OSD.
     *
     * @return a rerformance description where streaming performance and iops are set
     * @throws Exception
     */
    public OSDPerformanceDescription runBenchmark() throws Exception {
        controller.setupDefaultVolumes(maxNumberOfThreads);
        OSDPerformanceDescription perfDescription = new OSDPerformanceDescription();

        /* measure stream perfomance */
        Map<Integer, Double> streamPerf = measureStreamPerformance(sequentialSize, maxNumberOfThreads);
        perfDescription.setStreamingPerformance(streamPerf);

        /* measure iops */
        double iops = measureIOPS(randomSize);
        perfDescription.setIops(iops);

        /* teardown */
        controller.teardown();

        return perfDescription;
    }

    private Map<Integer, Double> measureStreamPerformance(long size, int maxNumberOfThreads) throws Exception {

        /* prepare read benchmarks */
        controller.startSequentialWriteBenchmark(size, maxNumberOfThreads);

        Map<Integer, Double> streamPerf = new HashMap<Integer, Double>();

        /* conduct read benchmarks */
        for (int numberOfThreads = 1; numberOfThreads <= maxNumberOfThreads; numberOfThreads++) {

            ConcurrentLinkedQueue<BenchmarkResult> resultsFromRepetitions = new ConcurrentLinkedQueue<BenchmarkResult>();

            for (int repetitions = 1; repetitions <= maxNumberOfRepetitions; repetitions++) {
                ConcurrentLinkedQueue<BenchmarkResult> resultCurrentRun =  controller.startSequentialReadBenchmark(size, numberOfThreads);
                rawResults.addAll(resultCurrentRun);
                resultsFromRepetitions.add(averageResults(resultCurrentRun, true));
            }

            BenchmarkResult resultFinal = averageResults(resultsFromRepetitions, false);
            streamPerf.put(resultFinal.getNumberOfReadersOrWriters(), resultFinal.getSpeedInMiBPerSec());
        }
        return streamPerf;
    }

    private double measureIOPS(long size) throws Exception {

        /* prepare read benchmarks (i.e. create basefiles) */
        controller.startRandomReadBenchmark(128L*BenchmarkUtils.KiB_IN_BYTES, 1);

        /* perform benchmarks */
        ConcurrentLinkedQueue<BenchmarkResult> resultsFromRepetitions = new ConcurrentLinkedQueue<BenchmarkResult>();
        for (int repetitions = 1; repetitions <= maxNumberOfRepetitions; repetitions++) {
            BenchmarkResult resultCurrentRun =  controller.startRandomReadBenchmark(size, 1).poll();
            rawResults.add(resultCurrentRun);
            resultsFromRepetitions.add(resultCurrentRun);
        }

        /* calculate iops */
        BenchmarkResult resultFinal = averageResults(resultsFromRepetitions, false);
        double iops = (resultFinal.getSpeedInMiBPerSec()/4)*1024;
        return iops;
    }

    private BenchmarkResult averageResults(ConcurrentLinkedQueue<BenchmarkResult> results, boolean addUpSize) {

        int numberOfResults = results.size();
        BenchmarkResult result1 = results.poll();
        int numberOfThreads = result1.getNumberOfReadersOrWriters();
        long sizeInBytes = result1.getByteCount();
        double timeInSec = result1.getTimeInSec();
        BenchmarkUtils.BenchmarkType type = result1.getBenchmarkType();
        
        if (addUpSize)
            if (numberOfResults != numberOfThreads)
                throw new IllegalArgumentException(
                        "If the method is used to add up sizes, the number of threads should equal the number of results");

        for (BenchmarkResult result : results) {

            if (numberOfThreads != result.getNumberOfReadersOrWriters())
                throw new IllegalArgumentException("Something went wrong, the number of threads should be equal in all results");

            if (type != result.getBenchmarkType())
                throw new IllegalArgumentException("Something went wrong, the type should be the same in all results");

            if (addUpSize)
                sizeInBytes += result.getByteCount();
            else {
                if (sizeInBytes != result.getByteCount())
                    throw new IllegalArgumentException("Something went wrong, the byte count should be equal in all results");
            }

            timeInSec += result.getTimeInSec();
        }

        BenchmarkResult averagedResult = new BenchmarkResult(timeInSec/numberOfResults, sizeInBytes, numberOfThreads, type);
        return averagedResult;
    }

    /**
     * Get the raw results of all the benchmarks performed to measure the OSD performance.
     *
     * @return the raw results
     */
    public ConcurrentLinkedQueue<BenchmarkResult> getRawResults() {
        return rawResults;
    }
}
