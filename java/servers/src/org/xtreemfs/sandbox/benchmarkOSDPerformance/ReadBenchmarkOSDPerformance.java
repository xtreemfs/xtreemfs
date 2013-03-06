/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a sequential read benchmark.
 * 
 * @author jensvfischer
 */
public class ReadBenchmarkOSDPerformance extends BenchmarkOSDPerformance {


    ReadBenchmarkOSDPerformance(String volumeName, ConnectionData connection) throws Exception {
        super(volumeName, connection);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long writeOrReadData(byte[] data, long numberOfBlocks) throws IOException {
        Volume volume = client.openVolume(volumeName, connection.sslOptions, connection.options);
        FileHandle fileHandle = volume.openFile(connection.userCredentials, BENCHMARK_FILENAME,
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        long byteCounter = 0;
        for (long j = 0; j < numberOfBlocks; j++) {
            long nextOffset = j * XTREEMFS_BLOCK_SIZE_IN_BYTES;
            assert nextOffset >= 0 : "Offset < 0 not allowed";
            byteCounter += fileHandle.read(connection.userCredentials, data, XTREEMFS_BLOCK_SIZE_IN_BYTES, nextOffset);
        }
        fileHandle.close();
        return byteCounter;
    }

//    public static void main(String[] args) throws Exception {
//
//        Logging.start(Logging.LEVEL_ALERT, Logging.Category.tool);
//
//        ConnectionData connection = new ConnectionData();
//        int numberOfReaders = 1;
//        long sizeInBytes = (long) 3 * GiB_IN_BYTES;
//
//        ConcurrentLinkedQueue<BenchmarkResult> results = Controller.startReadBenchmarks(connection, numberOfReaders, sizeInBytes);
//
//        BenchmarkOSDPerformance readBench = new ReadBenchmarkOSDPerformance(VOLUME_BASE_NAME, connection);
//        /* cleaning up */
//        for (int i = 0; i < numberOfReaders; i++)
//            readBench.deleteVolumeIfExisting(VOLUME_BASE_NAME + i);
//
//        Controller.scrub("47c551e1-2f30-42da-be3f-8c91c51dd15b", "");
//
//        /* print the results */
//        for (BenchmarkResult res : results) {
//            System.out.println(res);
//        }
//
//    }

}
