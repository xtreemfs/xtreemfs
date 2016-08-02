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
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

import java.io.IOException;
import java.util.Random;

/**
 * Class implementing a filebased read benchmark.
 * 
 * @author jensvfischer
 */
class FilebasedReadBenchmark extends FilebasedBenchmark {

    private String[] filenames;

    FilebasedReadBenchmark(long size, BenchmarkConfig config, AdminClient client, VolumeManager volumeManager) throws Exception {
        super(size, config, client, volumeManager);
    }

    @Override
    void prepareBenchmark() throws Exception {
        this.filenames = volumeManager.getRandomFilelistForVolume(volume, benchmarkSize);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

        long numberOfFilesToRead = benchmarkSize / filesize;

        int filenamesSize = filenames.length;
        long byteCounter = 0;
        Random random = new Random();

        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber();

        for (long i = 0; !cancelled && i < numberOfFilesToRead; i++) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Reading file %d of %d ...", i + 1, numberOfFilesToRead);
            }
            
            String filename = filenames[random.nextInt(filenamesSize)];
            FileHandle fileHandle = volume.openFile(config.getUserCredentials(), filename, flags);

            if (filesize <= requestSize) {
                random.nextBytes(data);
                byteCounter += fileHandle.read(config.getUserCredentials(), data, filesize, 0);
            } else
                for (long j = 0; j < filesize / requestSize; j++) {
                    long nextOffset = j * requestSize;
                    assert nextOffset >= 0 : "Offset < 0 not allowed";
                    byteCounter += fileHandle.read(config.getUserCredentials(), data, requestSize, nextOffset);
                }
            fileHandle.close();
        }
        return byteCounter;
    }

    @Override
    void finalizeBenchmark() throws Exception {
    }

}
