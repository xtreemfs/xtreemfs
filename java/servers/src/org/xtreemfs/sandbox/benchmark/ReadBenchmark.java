/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import java.io.IOException;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a sequential read benchmark.
 * 
 * @author jensvfischer
 */
public class ReadBenchmark extends SequentialBenchmark {
    private String[] filenames;


    ReadBenchmark(Volume volume, Params params) throws Exception {
        super(volume, params);
    }

    @Override
    void prepareBenchmark() throws Exception {
        this.filenames = VolumeManager.getInstance().getSequentialFilelistForVolume(volume);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {
        FileHandle fileHandle = volume.openFile(params.userCredentials, filenames[0],
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        long byteCounter = 0;
        for (long j = 0; j < numberOfBlocks; j++) {
            long nextOffset = j * stripeWidth;
            assert nextOffset >= 0 : "Offset < 0 not allowed";
            byteCounter += fileHandle.read(params.userCredentials, data, stripeWidth, nextOffset);
        }
        fileHandle.close();
        return byteCounter;
    }

}
