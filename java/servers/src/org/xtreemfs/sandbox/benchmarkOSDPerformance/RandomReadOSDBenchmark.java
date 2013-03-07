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
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a random read benchmark.
 * 
 * @author jensvfischer
 */
public class RandomReadOSDBenchmark extends OSDBenchmark {

    final static int RANDOM_IO_BLOCKSIZE = 1024 * 4; // 4 KiB
    final static long      sizeOfBasefile = 3L * (long) GiB_IN_BYTES;

    RandomReadOSDBenchmark(Volume volume, ConnectionData connection) throws Exception {
        super(volume, connection);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long writeOrReadData(byte[] data, long numberOfBlocks) throws IOException {

        numberOfBlocks = convertTo4KiBBlocks(numberOfBlocks);
        long byteCounter = 0;

        for (long j = 0; j < numberOfBlocks; j++) {
            FileHandle fileHandle = volume.openFile(connection.userCredentials, BENCHMARK_FILENAME,
                    GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
            long nextOffset = generateNextRandomOffset();
            byteCounter += fileHandle.read(connection.userCredentials, data, RANDOM_IO_BLOCKSIZE, nextOffset);
            fileHandle.close();
        }
        return byteCounter;
    }

    private long generateNextRandomOffset() {
        long nextOffset = Math.round(Math.random() * sizeOfBasefile - (long) RANDOM_IO_BLOCKSIZE);
        assert nextOffset >= 0 : "Offset < 0";
        assert nextOffset <= (sizeOfBasefile - RANDOM_IO_BLOCKSIZE) : "Offset > Filesize";
        return nextOffset;
    }

    /* check if the specified file exists */
    boolean fileNotExists(String filename) throws Exception {
        FileHandle fileHandle = volume.openFile(connection.userCredentials, filename,
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        boolean ret = fileHandle == null;
        fileHandle.close();
        return ret;
    }

    /* check if size of the specified file matches the specified Size */
    boolean sizeOfFileNotMatches(String filename) throws Exception {
        FileHandle fileHandle = volume.openFile(connection.userCredentials, filename,
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        long fileSizeInBytes = fileHandle.getAttr(connection.userCredentials).getSize();
        fileHandle.close();
        return sizeOfBasefile != fileSizeInBytes;
    }

    /* Create a large file to read from */
    private void createFileToReadFrom() throws Exception {
        long numberOfBlocks = sizeOfBasefile / (long) XTREEMFS_BLOCK_SIZE_IN_BYTES;
        Random random = new Random();
        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        FileHandle fileHandle = volume.openFile(connection.userCredentials, BENCHMARK_FILENAME, flags, 511);
        long byteCounter = 0;
        byte[] data = new byte[XTREEMFS_BLOCK_SIZE_IN_BYTES];
        for (long j = 0; j < numberOfBlocks; j++) {
            long nextOffset = j * XTREEMFS_BLOCK_SIZE_IN_BYTES;
            assert nextOffset >= 0 : "Offset < 0 not allowed";
            random.nextBytes(data);
            byteCounter += fileHandle.write(connection.userCredentials, data, XTREEMFS_BLOCK_SIZE_IN_BYTES, nextOffset);
        }
        fileHandle.close();
        assert byteCounter == sizeOfBasefile : " Error while writing the basefile for the random io benchmark";
        System.out.println("Basefile written. Size " + byteCounter + " Bytes");
    }


    /* convert to 4 KiB Blocks */
    private static long convertTo4KiBBlocks(long numberOfBlocks) {
        return (numberOfBlocks * (long) XTREEMFS_BLOCK_SIZE_IN_BYTES) / (long) RANDOM_IO_BLOCKSIZE;
    }

    void prepareBenchmark() throws Exception {
        /* create file to read from if not existing */
        if (fileNotExists(BENCHMARK_FILENAME) || sizeOfFileNotMatches(BENCHMARK_FILENAME)) {
            createFileToReadFrom();
        }
    }

}
