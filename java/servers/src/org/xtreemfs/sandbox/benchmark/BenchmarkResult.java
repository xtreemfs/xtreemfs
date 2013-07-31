/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

/**
 * Result objects for {@link AbstractBenchmark}.
 * 
 * @author jensvfischer
 */
public class BenchmarkResult {

    final static int MiB_IN_BYTES = 1024 * 1024;
    final static int GiB_IN_BYTES = 1024 * 1024 * 1024;

    AbstractBenchmark.BenchmarkType benchmarkType;
    int              numberOfReadersOrWriters;
    double           timeInSec;
    double           speedInMiBProSec;
    long             dataWrittenInBytes;
    long             threadID;
    long             byteCount;

    public BenchmarkResult(double timeInSec, double speedInMiBProSec, long dataWrittenInBytes, long threadID,
            long byteCount) {
        this.timeInSec = timeInSec;
        this.speedInMiBProSec = speedInMiBProSec;
        this.dataWrittenInBytes = dataWrittenInBytes;
        this.threadID = threadID;
        this.byteCount = byteCount;
    }

    @Override
    public String toString() {

        String dataWritten = dataWrittenInBytes >= GiB_IN_BYTES ? dataWrittenInBytes / GiB_IN_BYTES + " GiB ["
                : dataWrittenInBytes / MiB_IN_BYTES + " MiB [";
        String readersOrWriters;

        if (benchmarkType == AbstractBenchmark.BenchmarkType.SEQ_WRITE
                || benchmarkType == AbstractBenchmark.BenchmarkType.RAND_WRITE
                || benchmarkType == AbstractBenchmark.BenchmarkType.FILES_WRITE){
            readersOrWriters = "\tNumber of Writers: " + numberOfReadersOrWriters + "\n";
        } else if (benchmarkType == AbstractBenchmark.BenchmarkType.SEQ_READ
                || benchmarkType == AbstractBenchmark.BenchmarkType.RAND_READ
                || benchmarkType == AbstractBenchmark.BenchmarkType.FILES_READ){
            readersOrWriters = "\tNumber of Readers: " + numberOfReadersOrWriters + "\n";
        } else {
            readersOrWriters = "\tNumber of Readers/Writers: " + numberOfReadersOrWriters + "\n";
        }

        return "{\n\tBenchmarkType: " + benchmarkType + "\n" + readersOrWriters + "\tThreadID: " + threadID + "\n"
                + "\tTime: " + timeInSec + " Sec\n" + "\tSpeed: " + speedInMiBProSec + " MiB/s\n" + "\tData written: "
                + dataWritten + dataWrittenInBytes + " Bytes]\n" + "\tByteCount: " + byteCount + " Bytes\n" + "}";
    }

    public String toCSV(){
        return benchmarkType+";"+numberOfReadersOrWriters+";"+timeInSec+";"+speedInMiBProSec+";"+dataWrittenInBytes+";"+byteCount;
    }


}
