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
 * Class implementing a random IO read benchmarks.
 * <p/>
 * A random IO read benchmarks reads small blocks with random offsets within a large basefile.
 * 
 * @author jensvfischer
 */
class RandomWriteBenchmark extends RandomOffsetbasedBenchmark {

    RandomWriteBenchmark(long size, BenchmarkConfig config, AdminClient client, VolumeManager volumeManager) throws Exception {
        super(size, config, client, volumeManager);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

        Random random = new Random();

        numberOfBlocks = convertTo4KiBBlocks(numberOfBlocks);
        long byteCounter = 0;

        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber();

        for (long j = 0; !cancelled && j < numberOfBlocks; j++) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Writing block %d of %d ...", j + 1, numberOfBlocks);
            }
            
            FileHandle fileHandle = volume.openFile(config.getUserCredentials(), BASFILE_FILENAME, flags, 511);
            long nextOffset = generateNextRandomOffset();
            random.nextBytes(data);
            byteCounter += fileHandle.write(config.getUserCredentials(), data, RANDOM_IO_BLOCKSIZE, nextOffset);
            fileHandle.close();
        }

        return byteCounter;
    }

}
