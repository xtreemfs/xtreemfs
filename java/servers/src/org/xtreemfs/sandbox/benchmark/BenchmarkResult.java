/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import static org.xtreemfs.sandbox.benchmark.BenchmarkUtils.BenchmarkType;

/**
 * Result object for benchmarks.
 * 
 * @author jensvfischer
 */
public class BenchmarkResult {

    private BenchmarkType benchmarkType;
    private int           numberOfReadersOrWriters;
    private double        timeInSec;
    private long          dataRequestedInBytes;
    private long          byteCount;

    BenchmarkResult(double timeInSec, long dataRequestedInBytes, long byteCount) {
        this.timeInSec = timeInSec;
        this.dataRequestedInBytes = dataRequestedInBytes;
        this.byteCount = byteCount;
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
     * Get the speed of the benchmark in MiB/Sec
     *
     * @return the speed of the benchmark in MiB/Sec
     */
    public double getSpeedInMiBPerSec() {
        return round((byteCount / BenchmarkUtils.getMiB_IN_BYTES()) / timeInSec, 2);
    }

    /**
     * Get the size in the bytes the benchmark was requested to write or read.
     *
     * @return the benchmark size in bytes
     */
    public long getDataRequestedInBytes() {
        return dataRequestedInBytes;
    }

    /**
     * Get the count of bytes actually written or red by the benchmark (should be equal to {@link #getDataRequestedInBytes()}).
     *
     * @return the number of bytes written or red by the benchmark
     */
    public long getByteCount() {
        return byteCount;
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

    /* Round doubles to specified number of decimals */
    private double round(double value, int places) {
        if (places < 0)
            throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

}
