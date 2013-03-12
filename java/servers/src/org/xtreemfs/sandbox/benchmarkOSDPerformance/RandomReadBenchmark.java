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
 * Class implementing a random read benchmark.
 * 
 * @author jensvfischer
 */
public class RandomReadBenchmark extends RandomBenchmark {

    RandomReadBenchmark(Volume volume, ConnectionData connection) throws Exception {
        super(volume, connection);
    }

    static ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(int numberOfReaders, long sizeInBytes,
                                                                  ConcurrentLinkedQueue<Thread> threads) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfReaders; i++) {
            RandomReadBenchmark benchmark = new RandomReadBenchmark(VolumeManager.getInstance().getNextVolume(),
                    Controller.connection);
            benchmark.prepareBenchmark();
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }
        return results;
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

        numberOfBlocks = convertTo4KiBBlocks(numberOfBlocks);
        long byteCounter = 0;

        for (long j = 0; j < numberOfBlocks; j++) {
            FileHandle fileHandle = volume.openFile(connection.userCredentials, BENCHMARK_FILENAME,
                    GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
            long nextOffset = generateNextRandomOffset();
            byteCounter += fileHandle.read(connection.userCredentials, data, RANDOM_IO_BLOCKSIZE, nextOffset);
            fileHandle.close();
        }
        return byteCounter;
    }

}
