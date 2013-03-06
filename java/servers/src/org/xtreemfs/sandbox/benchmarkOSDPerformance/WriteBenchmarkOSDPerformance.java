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
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a sequential write benchmark.
 * 
 * @author jensvfischer
 */
public class WriteBenchmarkOSDPerformance extends BenchmarkOSDPerformance {


    WriteBenchmarkOSDPerformance(String volumeName, ConnectionData connection) throws Exception {
        super(volumeName, connection);
    }

    /* Called within the benchmark method. Performs the actual writing of data to the volume. */
    @Override
    long writeOrReadData(byte[] data, long numberOfBlocks) throws IOException {
        Volume volume = createAndOpenVolume(volumeName);
        Random random = new Random();
        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        FileHandle fileHandle = volume.openFile(connection.userCredentials, BENCHMARK_FILENAME, flags, 777);
        long byteCounter = 0;
        for (long j = 0; j < numberOfBlocks; j++) {
            long nextOffset = j * XTREEMFS_BLOCK_SIZE_IN_BYTES;
            assert nextOffset >= 0 : "Offset < 0 not allowed";
            random.nextBytes(data);
            byteCounter += fileHandle.write(connection.userCredentials, data, XTREEMFS_BLOCK_SIZE_IN_BYTES, nextOffset);
        }
        fileHandle.close();
        return byteCounter;
    }

    Volume createAndOpenVolume(String volumeName) throws PosixErrorException, AddressToUUIDNotFoundException,
            IOException {
        client.createVolume(connection.mrcAddress, connection.auth, connection.userCredentials, volumeName);
        return client.openVolume(volumeName, connection.sslOptions, connection.options);
    }

//    public static void main(String[] args) throws Exception {
//
//        Logging.start(Logging.LEVEL_INFO, Logging.Category.tool);
//
//        ConnectionData connection = new ConnectionData();
//        int numberOfWriters = 1;
//        long sizeInBytes = 3L * GiB_IN_BYTES;
//
//        ConcurrentLinkedQueue<BenchmarkResult> results = Controller.startWriteBenchmarks(connection, numberOfWriters, sizeInBytes);
//
//        // /* Cleaning up (Does prevent subsequent read benchmark) */
//        // for (int i = 0; i < numberOfWriters; i++)
//        // wBench.deleteVolumeIfExisting(VOLUME_BASE_NAME + i);
//        //
//        // scrub("47c551e1-2f30-42da-be3f-8c91c51dd15b", "");
//
//        /* print the results */
//        for (BenchmarkResult res : results) {
//            System.out.println(res);
//        }
//
//    }

}
