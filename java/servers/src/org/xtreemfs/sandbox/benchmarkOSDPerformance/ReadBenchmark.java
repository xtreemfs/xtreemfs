/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a sequential read benchmark.
 * 
 * @author jensvfischer
 */
public class ReadBenchmark extends Benchmark {


    ReadBenchmark(Volume volume, ConnectionData connection) throws Exception {
        super(volume, connection);
    }

    /**
     * Starts a benchmark run with the specified amount of read benchmarks in parallel. Every benchmark is
     * started within its own thread. The method waits for all threads to finish. Requires a
     * {@link WriteBenchmark} first (because the ReadBench reads the files written by the WriteBench).
     *
     * @param numberOfReaders
     *            number of read benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
    static ConcurrentLinkedQueue<BenchmarkResult> startReadBenchmarks(int numberOfReaders, long sizeInBytes,
                                                                      ConcurrentLinkedQueue<Thread> threads) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfReaders; i++) {
            Benchmark benchmark = new ReadBenchmark(VolumeManager.getInstance().getNextVolume(), Controller.connection);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }
        return results;
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {
        FileHandle fileHandle = volume.openFile(connection.userCredentials, BENCHMARK_FILENAME,
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        long byteCounter = 0;
        for (long j = 0; j < numberOfBlocks; j++) {
            long nextOffset = j * XTREEMFS_BLOCK_SIZE_IN_BYTES;
            assert nextOffset >= 0 : "Offset < 0 not allowed";
            byteCounter += fileHandle.read(connection.userCredentials, data, XTREEMFS_BLOCK_SIZE_IN_BYTES, nextOffset);
        }
        fileHandle.close();
        return byteCounter;
    }

}
