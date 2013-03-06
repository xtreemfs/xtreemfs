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

//import org.xtreemfs.common.auth.UserCredentials;

/**
* Class implementing a random read benchmark.
*
* @author jensvfischer
*/
public class RandomReadBenchmarkOSDPerformance extends BenchmarkOSDPerformance {

    final static int RANDOM_IO_BLOCKSIZE = 1024 * 4; // 4 KiB
    static long      sizeOfFileToRead;

    RandomReadBenchmarkOSDPerformance(String volumeName, ConnectionData connection) throws Exception {
        super(volumeName, connection);
    }


    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long writeOrReadData(byte[] data, long numberOfBlocks) throws IOException {

        numberOfBlocks = convertTo4KiBBlocks(numberOfBlocks);
        Volume volume = client.openVolume(volumeName, connection.sslOptions, connection.options);
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
        long nextOffset = Math.round(Math.random() * sizeOfFileToRead - (long) RANDOM_IO_BLOCKSIZE);
        assert nextOffset >= 0 : "Offset < 0";
        assert nextOffset <= (sizeOfFileToRead - RANDOM_IO_BLOCKSIZE) : "Offset > Filesize";
        return nextOffset;
    }

    /**
     * Starts a benchmark run with the specified amount of read benchmarks in parallel. Every benchmark is
     * started within its own thread. The method waits for all threads to finish. Requires a
     * {@link org.xtreemfs.sandbox.benchmarkOSDPerformance.WriteBenchmarkOSDPerformance} first (because the
     * ReadBench reads the files written by the WriteBench).
     *
//     * @param dirAddress
//     *            DIR Server
//     * @param mrcAddress
//     *            MRC Server
//     * @param userCredentials
//     *            User Credentials
//     * @param auth
//     *            Auth
//     * @param sslOptions
//     *            SSL Options
     * @param numberOfReaders
     *            number of read benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
    public static ConcurrentLinkedQueue<BenchmarkResult> scheduleBenchmarks(ConnectionData connection, int numberOfReaders, long sizeInBytes)
            throws Exception {

        if (sizeInBytes % XTREEMFS_BLOCK_SIZE_IN_BYTES != 0)
            throw new IllegalArgumentException("Size must be in alignment with (i.e. divisible through) the block size");

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfReaders; i++) {
            BenchmarkOSDPerformance benchmark = new RandomReadBenchmarkOSDPerformance(VOLUME_BASE_NAME + i, connection);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }

        /* wait for all threads to finish */
        for (Thread thread : threads) {
            thread.join();
        }

        /* Set BenchmarkResult Type */
        for (BenchmarkResult res : results)
            res.benchmarkType = BenchmarkType.RANDOM_IO;

        return results;
    }

    /* check if the specified file exists */
    boolean fileNotExists(String filename) throws Exception {
        Volume volume = client.openVolume(volumeName, connection.sslOptions, connection.options);
        FileHandle fileHandle = volume.openFile(connection.userCredentials, filename,
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        boolean ret = fileHandle == null;
        fileHandle.close();
        return ret;
    }

    /* check if size of the specified file matches the specified Size */
    boolean sizeOfFileNotMatches(String filename, long specifiedSizeInBytes) throws Exception {
        Volume volume = client.openVolume(volumeName, connection.sslOptions, connection.options);
        FileHandle fileHandle = volume.openFile(connection.userCredentials, filename,
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        long fileSizeInBytes = fileHandle.getAttr(connection.userCredentials).getSize();
        fileHandle.close();
        return specifiedSizeInBytes != fileSizeInBytes;
    }

    /* Create a large file to read from */
    private static void createFileToReadFrom(long sizeInBytes) throws Exception {
        ConnectionData connection = new ConnectionData();
        BenchmarkOSDPerformance writeBench = new WriteBenchmarkOSDPerformance(VOLUME_BASE_NAME, connection);

        cleaningUp(1, writeBench);

        ConcurrentLinkedQueue<BenchmarkResult> results = Controller.startBenchmarks(BenchmarkType.WRITE, 1, sizeInBytes);

        // print the results
        for (BenchmarkResult res : results) {
            System.out.println(res);
        }
    }

    /* delete volumes and scrub osd */
    private static void cleaningUp(int numberOfReaders, BenchmarkOSDPerformance writeBench) throws Exception {
        for (int i = 0; i < numberOfReaders; i++)
            Controller.deleteVolumeIfExisting(VOLUME_BASE_NAME + i);
        Controller.scrub("47c551e1-2f30-42da-be3f-8c91c51dd15b", "");
    }

    /* convert to 4 KiB Blocks */
    private static long convertTo4KiBBlocks(long numberOfBlocks) {
        return (numberOfBlocks * (long) XTREEMFS_BLOCK_SIZE_IN_BYTES) / (long) RANDOM_IO_BLOCKSIZE;
    }

    public static ConcurrentLinkedQueue<BenchmarkResult> startRandomBenchmark(int numberOfReaders, long sizeInBytes) throws Exception {

        long sizeOfFileToReadInBytes = 3L * GiB_IN_BYTES;
        assert sizeOfFileToReadInBytes >= 0 : "Filesize < 0 not allowed";
        long sizeOfBenchmarkInBytes = sizeInBytes;
        assert sizeOfBenchmarkInBytes >= 0 : "Size < 0 not allowed";

        /* set the static to the specified filesize */
        sizeOfFileToRead = sizeOfFileToReadInBytes;

        ConnectionData connection = new ConnectionData();
        RandomReadBenchmarkOSDPerformance randomBench = new RandomReadBenchmarkOSDPerformance(VOLUME_BASE_NAME + "0",connection);

        /* create file to read from if not existing */
        if (randomBench.fileNotExists(BENCHMARK_FILENAME)
                || randomBench.sizeOfFileNotMatches(BENCHMARK_FILENAME, sizeOfFileToReadInBytes)) {
            createFileToReadFrom(sizeOfFileToReadInBytes);
        }

         return scheduleBenchmarks(connection, numberOfReaders, sizeOfBenchmarkInBytes);

    }

}
