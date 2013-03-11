/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a random read benchmark.
 * 
 * @author jensvfischer
 */
public class FilebasedRandomWriteOSDBenchmark extends OSDBenchmark {

    final static int RANDOM_IO_BLOCKSIZE = 1024 * 4; // 4 KiB

    FilebasedRandomWriteOSDBenchmark(Volume volume, ConnectionData connection) throws Exception {
        super(volume, connection, new LinkedList<String>());
    }

    static ConcurrentLinkedQueue<BenchmarkResult> startBenchmarks(int numberOfWriters, long sizeInBytes,
            ConcurrentLinkedQueue<Thread> threads) throws Exception {

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfWriters; i++) {
            OSDBenchmark benchmark = new FilebasedRandomWriteOSDBenchmark(VolumeManager.getInstance().getNextVolume(),
                    Controller.connection);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }
        return results;
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

        long numberOfFiles = convertTo4KiBBlocks(numberOfBlocks);
        long byteCounter = 0;
        Random random = new Random();

        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();

        for (long j = 0; j < numberOfFiles; j++) {
            FileHandle fileHandle = volume.openFile(connection.userCredentials, BENCHMARK_FILENAME + j, flags, 511);
            this.filenames.add(BENCHMARK_FILENAME + j);
            random.nextBytes(data);
            byteCounter += fileHandle.write(connection.userCredentials, data, RANDOM_IO_BLOCKSIZE, 0);
            fileHandle.close();
        }
        return byteCounter;
    }

    /* convert to 4 KiB Blocks */
    private static long convertTo4KiBBlocks(long numberOfBlocks) {
        return (numberOfBlocks * (long) XTREEMFS_BLOCK_SIZE_IN_BYTES) / (long) RANDOM_IO_BLOCKSIZE;
    }

}
