/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import java.io.IOException;
import java.util.Random;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a filebased read benchmark.
 * 
 * @author jensvfischer
 */
class FilebasedReadBenchmark extends FilebasedBenchmark {

    private String[] filenames;

    FilebasedReadBenchmark(Config config, AdminClient client, VolumeManager volumeManager) throws Exception {
        super(config, client, volumeManager);
    }

    @Override
    void prepareBenchmark() throws Exception {
        this.filenames = volumeManager.getRandomFilelistForVolume(volume);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

        long numberOfFilesToRead = config.getRandomSizeInBytes() / filesize;

        int filenamesSize = filenames.length;
        long byteCounter = 0;
        Random random = new Random();

        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber();

        for (long i = 0; i < numberOfFilesToRead; i++) {
            String filename = filenames[random.nextInt(filenamesSize)];
            FileHandle fileHandle = volume.openFile(config.getUserCredentials(), filename, flags);

            if (filesize <= stripeSize) {
                random.nextBytes(data);
                byteCounter += fileHandle.read(config.getUserCredentials(), data, filesize, 0);
            } else
                for (long j = 0; j < filesize / stripeSize; j++) {
                    long nextOffset = j * stripeSize;
                    assert nextOffset >= 0 : "Offset < 0 not allowed";
                    byteCounter += fileHandle.read(config.getUserCredentials(), data, stripeSize, nextOffset);
                }
            fileHandle.close();
        }
        return byteCounter;
    }

    @Override
    void finalizeBenchmark() throws Exception {
    }

}
