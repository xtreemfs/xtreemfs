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

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a random read benchmark.
 * 
 * @author jensvfischer
 */
public class FilebasedRandomWriteBenchmark extends Benchmark {

    final static int RANDOM_IO_BLOCKSIZE = 1024 * 4; // 4 KiB
    private LinkedList<String> filenames;

    FilebasedRandomWriteBenchmark(Volume volume, Params params) throws Exception {
        super(volume, params);
        filenames = new LinkedList<String>();
    }

    @Override
    void prepareBenchmark() throws Exception {}

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
            FileHandle fileHandle = volume.openFile(params.userCredentials, BENCHMARK_FILENAME + j, flags, 511);
            this.filenames.add(BENCHMARK_FILENAME + j);
            random.nextBytes(data);
            byteCounter += fileHandle.write(params.userCredentials, data, RANDOM_IO_BLOCKSIZE, 0);
            fileHandle.close();
        }
        return byteCounter;
    }

    @Override
    void finalizeBenchmark() throws Exception {
        VolumeManager.getInstance().setFilelistForVolume(volume, filenames);
        // ToDo if no FilebasedRandomReadBenchmark follows delete written files
    }

    /* convert to 4 KiB Blocks */
    private static long convertTo4KiBBlocks(long numberOfBlocks) {
        return (numberOfBlocks * (long) XTREEMFS_BLOCK_SIZE_IN_BYTES) / (long) RANDOM_IO_BLOCKSIZE;
    }

}
