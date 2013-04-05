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

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a random read benchmark.
 * The Files used for the benchmark are deleted after finishing the benchmark.
 * 
 * @author jensvfischer
 */
public class FilebasedRandomReadBenchmark extends FilebasedRandomBenchmark {

    private String[] filenames;

    FilebasedRandomReadBenchmark(Volume volume, Params params) throws Exception {
        super(volume, params);
    }

    @Override
    void prepareBenchmark() throws Exception {
        this.filenames = VolumeManager.getInstance().getRandomFilelistForVolume(volume);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

//        long numberOfFilesToRead = convertTo4KiBBlocks(numberOfBlocks);
        long numberOfFilesToRead = params.randomSizeInBytes/4096;

        int filenamesSize = filenames.length;
        long byteCounter = 0;
        Random random = new Random();

        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber();

        for (long i = 0; i < numberOfFilesToRead; i++) {
            String filename = filenames[random.nextInt(filenamesSize)];
            FileHandle fileHandle = volume.openFile(params.userCredentials, filename, flags);
            byteCounter += fileHandle.read(params.userCredentials, data, randomIOFilesize, 0);
            fileHandle.close();
        }
        return byteCounter;
    }

    @Override
    void finalizeBenchmark() throws Exception {}

    long nextLong(long limit) {
        long next = Math.round(Math.random() * limit);
        assert next >= 0 : "Next long < 0";
        return next;
    }

}
