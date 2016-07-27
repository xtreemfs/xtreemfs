/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

import java.io.IOException;

/**
 * Class implementing a sequential read benchmark.
 * 
 * @author jensvfischer
 */
class SequentialReadBenchmark extends SequentialBenchmark {
    private String[] filenames;

    SequentialReadBenchmark(long size, BenchmarkConfig config, AdminClient client, VolumeManager volumeManager) throws Exception {
        super(size, config, client, volumeManager);
    }

    @Override
    void prepareBenchmark() throws Exception {
        this.filenames = volumeManager.getSequentialFilelistForVolume(volume, benchmarkSize);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {
        FileHandle fileHandle = volume.openFile(config.getUserCredentials(), filenames[0],
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        try {
            return tryPerformIO(data, numberOfBlocks, fileHandle);
        } catch (IOException e) {
            /* closing the filehandle manually seems to be the only way to avoid an AssertionError in
             * VolumeImplementation.internalShutdown() when shutting down client */
            fileHandle.close();
            throw e;
        }
    }


    private long tryPerformIO(byte[] data, long numberOfBlocks, FileHandle fileHandle) throws IOException {
        long byteCounter = 0;
        for (long j = 0; !cancelled && j < numberOfBlocks; j++) {
            long nextOffset = j * requestSize;
            assert nextOffset >= 0 : "Offset < 0 not allowed";
            byteCounter += fileHandle.read(config.getUserCredentials(), data, requestSize, nextOffset);
        }
        fileHandle.close();
        return byteCounter;
    }

}
