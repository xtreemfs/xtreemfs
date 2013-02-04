/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

/**
 * Result objects for {@link BenchmarkOSDPerformance}.
 * 
 * @author jensvfischer
 */
public class BenchmarkResult {

    final static int MB_IN_BYTES = 1024 * 1024;
    final static int GB_IN_BYTES = 1024 * 1024 * 1024;

    BenchmarkType    benchmarkType;
    int              numberOfReadersOrWriters;
    double           timeInSec;
    double           speedInMbProSec;
    long             dataWrittenInBytes;
    long             threadID;
    long             byteCount;

    public BenchmarkResult(double timeInSec, double speedInMbProSec, long dataWrittenInBytes, long threadID,
            long byteCount) {
        this.timeInSec = timeInSec;
        this.speedInMbProSec = speedInMbProSec;
        this.dataWrittenInBytes = dataWrittenInBytes;
        this.threadID = threadID;
        this.byteCount = byteCount;
    }

    @Override
    public String toString() {

        String dataWritten = dataWrittenInBytes >= GB_IN_BYTES ? dataWrittenInBytes / GB_IN_BYTES + " GB ["
                : dataWrittenInBytes / MB_IN_BYTES + " MB [";
        String readersOrWriters;

        if (benchmarkType == BenchmarkType.SEQUENTIAL_SINGLE_WRITE
                || benchmarkType == BenchmarkType.SEQUENTIAL_MULTI_WRITE) {
            readersOrWriters = "\tNumber of Writers: " + numberOfReadersOrWriters + "\n";
        } else if (benchmarkType == BenchmarkType.SEQUENTIAL_SINGLE_READ
                || benchmarkType == BenchmarkType.SEQUENTIAL_MULTI_READ) {
            readersOrWriters = "\tNumber of Readers: " + numberOfReadersOrWriters + "\n";
        } else {
            readersOrWriters = "\tNumber of Readers/Writers: " + numberOfReadersOrWriters + "\n";
        }

        return "{\n\tBenchmarkType: " + benchmarkType + "\n" + readersOrWriters + "\tThreadID: " + threadID + "\n"
                + "\tTime: " + timeInSec + " Sec\n" + "\tSpeed: " + speedInMbProSec + " MB/s\n" + "\tData written: "
                + dataWritten + dataWrittenInBytes + " Bytes]\n" + "\tByteCount: " + byteCount + " Bytes\n" + "}";
    }

}
