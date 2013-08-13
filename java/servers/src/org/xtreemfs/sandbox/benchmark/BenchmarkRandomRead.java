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
 * Class implementing a random IO read benchmarks.
 * <p/>
 * A random IO read benchmarks reads small blocks with random offsets within a large basefile.
 * 
 * @author jensvfischer
 */
public class BenchmarkRandomRead extends BenchmarkRandomOffsetbased {

    BenchmarkRandomRead(Volume volume, Params params) throws Exception {
        super(volume, params);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

        numberOfBlocks = convertTo4KiBBlocks(numberOfBlocks);
        long byteCounter = 0;

        for (long j = 0; j < numberOfBlocks; j++) {
            FileHandle fileHandle = volume.openFile(params.userCredentials, BASFILE_FILENAME,
                    GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
            long nextOffset = generateNextRandomOffset();
            byteCounter += fileHandle.read(params.userCredentials, data, RANDOM_IO_BLOCKSIZE, nextOffset);
            fileHandle.close();
        }
        return byteCounter;
    }

}
