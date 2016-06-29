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
import java.util.LinkedList;
import java.util.Random;

/**
 * Class implementing a filebased write benchmark.
 * 
 * @author jensvfischer
 */
class FilebasedWriteBenchmark extends FilebasedBenchmark {

    private LinkedList<String> filenames;

    FilebasedWriteBenchmark(long size, BenchmarkConfig config, AdminClient client, VolumeManager volumeManager) throws Exception {
        super(size, config, client, volumeManager);
        filenames = new LinkedList<String>();
    }

    @Override
    void prepareBenchmark() throws Exception {
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

        long numberOfFiles = benchmarkSize / filesize;
        long byteCounter = 0;
        Random random = new Random();

        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();

        for (long i = 0; !cancelled && i < numberOfFiles; i++) {
            FileHandle fileHandle = volume.openFile(config.getUserCredentials(), BENCHMARK_FILENAME + i, flags, 511);
            this.filenames.add(BENCHMARK_FILENAME + i);

            if (filesize <= requestSize) {
                random.nextBytes(data);
                byteCounter += fileHandle.write(config.getUserCredentials(), data, filesize, 0);
            } else
                for (long j = 0; j < filesize / requestSize; j++) {
                    long nextOffset = j * requestSize;
                    assert nextOffset >= 0 : "Offset < 0 not allowed";
                    random.nextBytes(data);
                    byteCounter += fileHandle.write(config.getUserCredentials(), data, requestSize, nextOffset);
                }
            fileHandle.close();
        }
        return byteCounter;
    }

    @Override
    void finalizeBenchmark() throws Exception {
        volumeManager.setRandomFilelistForVolume(volume, filenames);
        volumeManager.addCreatedFiles(volume, filenames);
    }

}
