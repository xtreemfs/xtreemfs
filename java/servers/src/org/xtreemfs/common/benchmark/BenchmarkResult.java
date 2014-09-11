/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import static org.xtreemfs.common.benchmark.BenchmarkUtils.BenchmarkType;

/**
 * Result object for benchmarks.
 * 
 * @author jensvfischer
 */
public class BenchmarkResult implements Comparable<BenchmarkResult> {

    private BenchmarkType benchmarkType;
    private int           numberOfReadersOrWriters;
    private double        timeInSec;
    private long          requestedSize;
    private long          actualSize;
    private boolean       failed;
    private Throwable     error;

    public BenchmarkResult(double timeInSec, long requestedSize, long actualSize) {
        this.timeInSec = timeInSec;
        this.requestedSize = requestedSize;
        this.actualSize = actualSize;
        this.failed = false;
    }

    public BenchmarkResult(double timeInSec, long actualSize, int numberOfReadersOrWriters, BenchmarkType benchmarkType) {
        this.benchmarkType = benchmarkType;
        this.numberOfReadersOrWriters = numberOfReadersOrWriters;
        this.timeInSec = timeInSec;
        this.actualSize = actualSize;
    }

    public BenchmarkResult(Throwable error) {
        this.failed = true;
        this.error = error;
    }

    void setBenchmarkType(BenchmarkType benchmarkType) {
        this.benchmarkType = benchmarkType;
    }

    void setNumberOfReadersOrWriters(int numberOfReadersOrWriters) {
        this.numberOfReadersOrWriters = numberOfReadersOrWriters;
    }

    /**
     * Get the type of the benchmark.
     * <p/>
     * 
     * The benchmark type is one of the following:
     * 
     * <ul>
     * <li>SEQ_WRITE: Sequential write benchmark</li>
     * <li>SEQ_READ: Sequential read benchmark</li>
     * <li>RAND_WRITE: Random write benchmark</li>
     * <li>RAND_READ: Random read benchmark</li>
     * <li>FILES_WRITE: Filebased write benchmark</li>
     * <li>FILES_READ: Filebased read benchmark</li>
     * </ul>
     * 
     * 
     * @return the type of the benchmark
     */
    public BenchmarkType getBenchmarkType() {
        return benchmarkType;
    }

    /**
     * Get the number of parallel benchmark threads.
     *
     * @return the number of parallel benchmark threads
     */
    public int getNumberOfReadersOrWriters() {
        return numberOfReadersOrWriters;
    }

    /**
     * Get the number of seconds the benchmark run took.
     *
     * @return the number of seconds the benchmark run took
     */
    public double getTimeInSec() {
        return timeInSec;
    }

    /**
     * Get the size, the benchmark was requested to write or read.
     *
     * @return the benchmark size in bytes
     */
    public long getRequestedSize() {
        return requestedSize;
    }

    /**
     * Get the count of requests actually written or red by the benchmark (should be equal to {@link #getRequestedSize()}).
     *
     * @return the number of requests written or red by the benchmark
     */
    public long getActualSize() {
        return actualSize;
    }

    public Throwable getError() {
        return error;
    }

    /**
     * Returns true, if the benchmark is either a sequential write benchmark, a random write benchmark or a filebased write benchmark.
     *
     * @return true, if the benchmark is a write benchmark
     */
    public boolean isWriteBenchmark(){
        return benchmarkType == BenchmarkType.SEQ_WRITE || benchmarkType == BenchmarkType.RAND_WRITE
                || benchmarkType == BenchmarkType.FILES_WRITE;
    }

    /**
     * Returns true, if the benchmark is either a sequential read benchmark, a random read benchmark or a filebased read benchmark.
     *
     * @return true, if the benchmark is a read benchmark
     */
    public boolean isReadBenchmark(){
        return benchmarkType == BenchmarkType.SEQ_READ || benchmarkType == BenchmarkType.RAND_READ
                || benchmarkType == BenchmarkType.FILES_READ;
    }

    public boolean isFailed() {
        return failed;
    }

    @Override
    public int compareTo(BenchmarkResult otherResult) {
        return benchmarkType.toString().compareTo(otherResult.benchmarkType.toString());
    }
}
