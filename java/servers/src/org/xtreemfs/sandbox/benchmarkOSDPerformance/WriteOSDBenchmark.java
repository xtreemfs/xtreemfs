/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a sequential write benchmark.
 * 
 * @author jensvfischer
 */
public class WriteOSDBenchmark extends OSDBenchmark {


    WriteOSDBenchmark(Volume volume, ConnectionData connection) throws Exception {
        super(volume, connection);
    }

    /**
     * Starts a benchmark run with the specified amount of write benchmarks in parallel. Every benchmark is
     * started within its own thread. The method waits for all threads to finish.
     *
     * @param numberOfWriters
     *            number of write benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
    static ConcurrentLinkedQueue<BenchmarkResult> startWriteBenchmarks(int numberOfWriters, long sizeInBytes,
                                                                       ConcurrentLinkedQueue<Thread> threads) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfWriters; i++) {
            OSDBenchmark benchmark = new WriteOSDBenchmark(VolumeManager.getInstance().getNextVolume(), Controller.connection);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }

        return results;
    }

    /* Called within the benchmark method. Performs the actual writing of data to the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {
        Random random = new Random();
        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        FileHandle fileHandle = volume.openFile(connection.userCredentials, BENCHMARK_FILENAME, flags, 511);
        long byteCounter = 0;
        for (long j = 0; j < numberOfBlocks; j++) {
            long nextOffset = j * XTREEMFS_BLOCK_SIZE_IN_BYTES;
            assert nextOffset >= 0 : "Offset < 0 not allowed";
            random.nextBytes(data);
            byteCounter += fileHandle.write(connection.userCredentials, data, XTREEMFS_BLOCK_SIZE_IN_BYTES, nextOffset);
        }
        fileHandle.close();
        return byteCounter;
    }

}
