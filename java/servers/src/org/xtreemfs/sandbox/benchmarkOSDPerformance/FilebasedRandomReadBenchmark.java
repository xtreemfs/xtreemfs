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
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a random read benchmark.
 * 
 * @author jensvfischer
 */
public class FilebasedRandomReadBenchmark extends Benchmark {

    final static int RANDOM_IO_BLOCKSIZE = 1024 * 4; // 4 KiB

    FilebasedRandomReadBenchmark(ConnectionData connection, KeyValuePair<Volume, LinkedList<String>> volumeWithFiles)
            throws Exception {
        super(volumeWithFiles.key, connection);
        this.filenames = volumeWithFiles.value;
    }


    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long performIO(byte[] data, long numberOfBlocks) throws IOException {

        long numberOfFiles = convertTo4KiBBlocks(numberOfBlocks);
        long byteCounter = 0;
        Random random = new Random();

        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber();

        for (long i = 0; i < numberOfFiles; i++) {
            // for (String filename : filenames) {
            String filename = filenames.get(random.nextInt(filenames.size()));
            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.tool, this, "File to be read: %s", filename);
            FileHandle fileHandle = volume.openFile(connection.userCredentials, filename, flags);
            byteCounter += fileHandle.read(connection.userCredentials, data, RANDOM_IO_BLOCKSIZE, 0);
            fileHandle.close();
        }
        return byteCounter;
    }

    /* convert to 4 KiB Blocks */
    private static long convertTo4KiBBlocks(long numberOfBlocks) {
        return (numberOfBlocks * (long) XTREEMFS_BLOCK_SIZE_IN_BYTES) / (long) RANDOM_IO_BLOCKSIZE;
    }

}
