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
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a sequential write benchmark.
 * 
 * @author jensvfischer
 */
public class WriteBenchmarkOSDPerformance extends BenchmarkOSDPerformance {

    WriteBenchmarkOSDPerformance() throws Exception {
    }

    WriteBenchmarkOSDPerformance(String volumeName) throws Exception {
        super(volumeName);
    }

    WriteBenchmarkOSDPerformance(String dirAddress, String mrcAddress, UserCredentials userCredentials1, Auth auth,
            SSLOptions sslOptions, String volumeName) {
        super(dirAddress, mrcAddress, userCredentials1, auth, sslOptions, volumeName);
    }

    /* Called within the benchmark method. Performs the actual writing of data to the volume. */
    @Override
    long writeOrReadData(byte[] data, long numberOfBlocks) throws IOException {
        Volume volume = createAndOpenVolume(volumeName);
        Random random = new Random();
        int flags = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber();
        FileHandle fileHandle = volume.openFile(userCredentials, BENCHMARK_FILENAME, flags, 777);
        long byteCounter = 0;
        for (long j = 0; j < numberOfBlocks; j++) {
            long nextOffset = j * XTREEMFS_BLOCK_SIZE_IN_BYTES;
            assert nextOffset >= 0 : "Offset < 0 not allowed";
            random.nextBytes(data);
            byteCounter += fileHandle.write(userCredentials, data, XTREEMFS_BLOCK_SIZE_IN_BYTES, nextOffset);
        }
        fileHandle.close();
        return byteCounter;
    }

    Volume createAndOpenVolume(String volumeName) throws PosixErrorException, AddressToUUIDNotFoundException,
            IOException {
        client.createVolume(mrcAddress, auth, userCredentials, volumeName);
        return client.openVolume(volumeName, sslOptions, options);
    }

    /**
     * Starts a benchmark run with the specified amount of write benchmarks in parallel. Every benchmark is
     * started within its own thread. The method waits for all threads to finish.
     * 
     * @param dirAddress
     *            DIR Server
     * @param mrcAddress
     *            MRC Server
     * @param userCredentials
     *            User Credentials
     * @param auth
     *            Auth
     * @param sslOptions
     *            SSL Options
     * @param numberOfWriters
     *            number of write benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
    public static ConcurrentLinkedQueue<BenchmarkResult> scheduleBenchmarks(String dirAddress, String mrcAddress,
            UserCredentials userCredentials, Auth auth, SSLOptions sslOptions, int numberOfWriters, long sizeInBytes)
            throws Exception {

        if (sizeInBytes % XTREEMFS_BLOCK_SIZE_IN_BYTES != 0)
            throw new IllegalArgumentException("Size must be in alignment with (i.e. divisible through) the block size");

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfWriters; i++) {
            BenchmarkOSDPerformance benchmark = new WriteBenchmarkOSDPerformance(dirAddress, mrcAddress,
                    userCredentials, auth, sslOptions, VOLUME_BASE_NAME + i);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }

        /* wait for all threads to finish */
        for (Thread thread : threads) {
            thread.join();
        }

        /* Set BenchmarkResult Type */
        for (BenchmarkResult res : results) {
            if (numberOfWriters > 1)
                res.benchmarkType = BenchmarkType.SEQUENTIAL_MULTI_WRITE;
            else
                res.benchmarkType = BenchmarkType.SEQUENTIAL_SINGLE_WRITE;
            res.numberOfReadersOrWriters = numberOfWriters;
        }
        return results;
    }

    public static void main(String[] args) throws Exception {

        Logging.start(Logging.LEVEL_INFO, Logging.Category.tool);

        BenchmarkOSDPerformance wBench = new WriteBenchmarkOSDPerformance();
        int numberOfWriters = 1;
        long sizeInBytes = 3L * GiB_IN_BYTES;

        ConcurrentLinkedQueue<BenchmarkResult> results = scheduleBenchmarks(wBench.dirAddress, wBench.mrcAddress,
                wBench.userCredentials, wBench.auth, wBench.sslOptions, numberOfWriters, sizeInBytes);

        // /* Cleaning up (Does prevent subsequent read benchmark) */
        // for (int i = 0; i < numberOfWriters; i++)
        // wBench.deleteVolumeIfExisting(VOLUME_BASE_NAME + i);
        //
        // scrub("47c551e1-2f30-42da-be3f-8c91c51dd15b", "");

        /* print the results */
        for (BenchmarkResult res : results) {
            System.out.println(res);
        }

    }

}
