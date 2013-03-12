/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

import java.util.Random;

/**
 * @author jensvfischer
 */
public abstract class RandomBenchmark extends Benchmark {
    final static int  RANDOM_IO_BLOCKSIZE = 1024 * 4;                 // 4 KiB
    final static long sizeOfBasefile      = 3L * (long) GiB_IN_BYTES;

    public RandomBenchmark(Volume volume, ConnectionData connection) throws Exception {
        super(volume, connection);
    }

    @Override
    void prepareBenchmark() throws Exception {
        /* create file to read from if not existing */
        if (basefileDoesNotExists()) {
            createBasefile();
        }
    }

    @Override
    void finalizeBenchmark(){}

    /* convert to 4 KiB Blocks */
    protected static long convertTo4KiBBlocks(long numberOfBlocks) {
        return (numberOfBlocks * (long) XTREEMFS_BLOCK_SIZE_IN_BYTES) / (long) RANDOM_IO_BLOCKSIZE;
    }

    protected long generateNextRandomOffset() {
        long nextOffset = Math.round(Math.random() * sizeOfBasefile - (long) RANDOM_IO_BLOCKSIZE);
        assert nextOffset >= 0 : "Offset < 0";
        assert nextOffset <= (sizeOfBasefile - RANDOM_IO_BLOCKSIZE) : "Offset > Filesize";
        return nextOffset;
    }

    /* check if a basefile to read from exists and if it has the right size */
    boolean basefileDoesNotExists() throws Exception {
        try {
            FileHandle fileHandle = volume.openFile(connection.userCredentials, BENCHMARK_FILENAME,
                    GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
            long fileSizeInBytes = fileHandle.getAttr(connection.userCredentials).getSize();
            fileHandle.close();
            return sizeOfBasefile != fileSizeInBytes;
        } catch (PosixErrorException e) {
            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Could not find a basefile. Errormessage: %s", e.getMessage());
            return true;
        }
    }

    /* Create a large file to read from */
    private void createBasefile() throws Exception {
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this,"Start creating a basefile of size %s bytes.", sizeOfBasefile);
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
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this,"Basefile written. Size %s Bytes.", byteCounter);

    }


}
