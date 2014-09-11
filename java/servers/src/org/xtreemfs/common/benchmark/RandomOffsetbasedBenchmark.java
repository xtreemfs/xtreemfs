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
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

import java.util.LinkedList;
import java.util.Random;

/**
 * Abstract baseclass for random IO benchmarks.
 * <p/>
 * Random IO benchmarks write or read small blocks with random offsets within a large basefile.
 * 
 * @author jensvfischer
 */
abstract class RandomOffsetbasedBenchmark extends AbstractBenchmark {
    final static int    RANDOM_IO_BLOCKSIZE = 1024 * 4;             // 4 KiB
    final long          sizeOfBasefile;
    final static String BASFILE_FILENAME    = "benchmarks/basefile";

    RandomOffsetbasedBenchmark(long size, BenchmarkConfig config, AdminClient client, VolumeManager volumeManager) throws Exception {
        super(size, config, client, volumeManager);
        sizeOfBasefile = config.getBasefileSizeInBytes();
    }

    @Override
    void prepareBenchmark() throws Exception {
        /* create file to read from if not existing */
        if (basefileDoesNotExists()) {
            createBasefile();
        }
    }

    @Override
    void finalizeBenchmark() {
    }

    /* convert to 4 KiB Blocks */
    long convertTo4KiBBlocks(long numberOfBlocks) {
        return (numberOfBlocks * (long) requestSize) / (long) RANDOM_IO_BLOCKSIZE;
    }

    long generateNextRandomOffset() {
        long nextOffset = Math.round(Math.random() * (sizeOfBasefile - (long) RANDOM_IO_BLOCKSIZE));
        assert nextOffset >= 0 : "Offset < 0. Offset: " + nextOffset + " Basefilesize: " + sizeOfBasefile;
        assert nextOffset <= (sizeOfBasefile - RANDOM_IO_BLOCKSIZE) : " Offset > Filesize. Offset: " + nextOffset
                + "Basefilesize: " + sizeOfBasefile;
        return nextOffset;
    }

    /* check if a basefile to read from exists and if it has the right size */
    boolean basefileDoesNotExists() throws Exception {
        try {
            FileHandle fileHandle = volume.openFile(config.getUserCredentials(), BASFILE_FILENAME,
                    GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
            long fileSizeInBytes = fileHandle.getAttr(config.getUserCredentials()).getSize();
            fileHandle.close();
            return sizeOfBasefile != fileSizeInBytes;
        } catch (PosixErrorException e) {
            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "No basefile found.");
            return true;
        }
    }

    /* Create a large file to read from */
    private void createBasefile() throws Exception {
        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this,
                "Start creating a basefile of size %s bytes.", sizeOfBasefile);
        long numberOfBlocks = sizeOfBasefile / (long) requestSize;
        Random random = new Random();
        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        FileHandle fileHandle = volume.openFile(config.getUserCredentials(), BASFILE_FILENAME, flags, 511);
        long byteCounter = 0;
        byte[] data = new byte[requestSize];
        for (long j = 0; j < numberOfBlocks; j++) {
            long nextOffset = j * requestSize;
            assert nextOffset >= 0 : "Offset < 0 not allowed";
            random.nextBytes(data);
            byteCounter += fileHandle.write(config.getUserCredentials(), data, requestSize, nextOffset);
        }
        fileHandle.close();
        assert byteCounter == sizeOfBasefile : " Error while writing the basefile for the random io benchmark";

        addBasefileToCreatedFiles();

        Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.tool, this, "Basefile written. Size %s Bytes.",
                byteCounter);

    }

    private void addBasefileToCreatedFiles() throws Exception {
        if (!config.isNoCleanupBasefile()) {
            LinkedList<String> createdFiles = new LinkedList<String>();
            createdFiles.add(BASFILE_FILENAME);
            volumeManager.addCreatedFiles(volume, createdFiles);
        }
    }

}
