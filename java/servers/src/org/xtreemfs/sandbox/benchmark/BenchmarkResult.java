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
class BenchmarkResult {

    BenchmarkUtils.BenchmarkType benchmarkType;
    int              numberOfReadersOrWriters;
    double           timeInSec;
    double           speedInMiBProSec;
    long             dataWrittenInBytes;
    long             threadID;
    long             byteCount;

    BenchmarkResult(double timeInSec, double speedInMiBProSec, long dataWrittenInBytes, long threadID,
            long byteCount) {
        this.timeInSec = timeInSec;
        this.speedInMiBProSec = speedInMiBProSec;
        this.dataWrittenInBytes = dataWrittenInBytes;
        this.threadID = threadID;
        this.byteCount = byteCount;
    }

    @Override
    public String toString() {

        String dataWritten = dataWrittenInBytes >= BenchmarkUtils.getGiB_IN_BYTES() ? dataWrittenInBytes / BenchmarkUtils.getGiB_IN_BYTES() + " GiB ["
                : dataWrittenInBytes / BenchmarkUtils.getMiB_IN_BYTES() + " MiB [";
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
                + dataWritten + dataWrittenInBytes + " Bytes]\n" + "\tByteCount: " + byteCount + " Bytes\n" + "}";
    }

    String toCSV() {
        return benchmarkType + ";" + numberOfReadersOrWriters + ";" + timeInSec + ";" + speedInMiBProSec + ";"
                + dataWrittenInBytes + ";" + byteCount;
    }

}
