/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

/**
 * Result object for benchmarks.
 * 
 * @author jensvfischer
 */
public class BenchmarkResult {

    private BenchmarkUtils.BenchmarkType benchmarkType;
    private int                          numberOfReadersOrWriters;
    private double                       timeInSec;
    private double                       speedInMiBProSec;
    private long                         dataRequestedInBytes;
    private long                         threadID;
    private long                         byteCount;

    BenchmarkResult(double timeInSec, double speedInMiBProSec, long dataRequestedInBytes, long threadID,
            long byteCount) {
        this.timeInSec = timeInSec;
        this.speedInMiBProSec = speedInMiBProSec;
        this.dataRequestedInBytes = dataRequestedInBytes;
        this.threadID = threadID;
        this.byteCount = byteCount;
    }

    @Override
    public String toString() {

        String dataWritten = dataRequestedInBytes >= BenchmarkUtils.getGiB_IN_BYTES() ? dataRequestedInBytes / BenchmarkUtils.getGiB_IN_BYTES() + " GiB ["
                : dataRequestedInBytes / BenchmarkUtils.getMiB_IN_BYTES() + " MiB [";
        String readersOrWriters;

        if (benchmarkType == BenchmarkUtils.BenchmarkType.SEQ_WRITE || benchmarkType == BenchmarkUtils.BenchmarkType.RAND_WRITE
                || benchmarkType == BenchmarkUtils.BenchmarkType.FILES_WRITE) {
            readersOrWriters = "\tNumber of Writers: " + numberOfReadersOrWriters + "\n";
        } else if (benchmarkType == BenchmarkUtils.BenchmarkType.SEQ_READ || benchmarkType == BenchmarkUtils.BenchmarkType.RAND_READ
                || benchmarkType == BenchmarkUtils.BenchmarkType.FILES_READ) {
            readersOrWriters = "\tNumber of Readers: " + numberOfReadersOrWriters + "\n";
        } else {
            readersOrWriters = "\tNumber of Readers/Writers: " + numberOfReadersOrWriters + "\n";
        }

        return "{\n\tBenchmarkType: " + benchmarkType + "\n" + readersOrWriters + "\tThreadID: " + threadID + "\n"
                + "\tTime: " + timeInSec + " Sec\n" + "\tSpeed: " + speedInMiBProSec + " MiB/s\n" + "\tData written: "
                + dataWritten + dataRequestedInBytes + " Bytes]\n" + "\tByteCount: " + byteCount + " Bytes\n" + "}";
    }

    String toCSV() {
        return benchmarkType + ";" + numberOfReadersOrWriters + ";" + timeInSec + ";" + speedInMiBProSec + ";"
                + dataRequestedInBytes + ";" + byteCount;
    }

    void setBenchmarkType(BenchmarkUtils.BenchmarkType benchmarkType) {
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
    public BenchmarkUtils.BenchmarkType getBenchmarkType() {
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
    public double getSpeedInMiBProSec() {
        return speedInMiBProSec;
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
}
