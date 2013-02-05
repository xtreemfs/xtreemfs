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
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * Class implementing a sequential read benchmark.
 * 
 * @author jensvfischer
 */
public class ReadBenchmarkOSDPerformance extends BenchmarkOSDPerformance {

    ReadBenchmarkOSDPerformance() throws Exception {
    }

    ReadBenchmarkOSDPerformance(String volumeName) throws Exception {
        super(volumeName);
    }

    ReadBenchmarkOSDPerformance(String dirAddress, String mrcAddress, UserCredentials userCredentials1, Auth auth,
            SSLOptions sslOptions, String volumeName) {
        super(dirAddress, mrcAddress, userCredentials1, auth, sslOptions, volumeName);
    }

    /* Called within the benchmark method. Performs the actual reading of data from the volume. */
    @Override
    long writeOrReadData(byte[] data, long numberOfBlocks) throws IOException {
        Volume volume = client.openVolume(volumeName, sslOptions, options);
        FileHandle fileHandle = volume.openFile(userCredentials, "testfile",
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
        long byteCounter = 0;
        for (long j = 0; j < numberOfBlocks; j++) {
            byteCounter += fileHandle.read(userCredentials, data, XTREEMFS_BLOCK_SIZE_IN_BYTES, j
                    * XTREEMFS_BLOCK_SIZE_IN_BYTES);
        }
        fileHandle.close();
        return byteCounter;
    }

    /**
     * Starts a benchmark run with the specified amount of read benchmarks in parallel. Every benchmark is
     * started within its own thread. The method waits for all threads to finish. Requires a
     * {@link WriteBenchmarkOSDPerformance} first (because the ReadBench reads the files written by the
     * WriteBench).
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
     * @param numberOfReaders
     *            number of read benchmarks run in parallel
     * @param sizeInBytes
     *            Size of the benchmark in bytes. Must be in alignment with (i.e. divisible through) the block
     *            size (128 KiB).
     * @return results of the benchmarks
     * @throws Exception
     */
    public static ConcurrentLinkedQueue<BenchmarkResult> scheduleBenchmarks(String dirAddress, String mrcAddress,
            UserCredentials userCredentials, Auth auth, SSLOptions sslOptions, int numberOfReaders, long sizeInBytes)
            throws Exception {

        if (sizeInBytes % XTREEMFS_BLOCK_SIZE_IN_BYTES != 0)
            throw new IllegalArgumentException("Size must be in alignment with (i.e. divisible through) the block size");

        ConcurrentLinkedQueue<BenchmarkResult> results = new ConcurrentLinkedQueue<BenchmarkResult>();
        ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();

        /* start the benchmark threads */
        for (int i = 0; i < numberOfReaders; i++) {
            BenchmarkOSDPerformance benchmark = new ReadBenchmarkOSDPerformance(dirAddress, mrcAddress,
                    userCredentials, auth, sslOptions, VOLUME_BASE_NAME + i);
            benchmark.startBenchThread(sizeInBytes, results, threads);
        }

        /* wait for all threads to finish */
        for (Thread thread : threads) {
            thread.join();
        }

        /* Set BenchmarkResult Type */
        for (BenchmarkResult res : results) {
            if (numberOfReaders > 1)
                res.benchmarkType = BenchmarkType.SEQUENTIAL_MULTI_READ;
            else
                res.benchmarkType = BenchmarkType.SEQUENTIAL_SINGLE_READ;
            res.numberOfReadersOrWriters = numberOfReaders;
        }
        return results;
    }

    public static void main(String[] args) throws Exception {

        Logging.start(Logging.LEVEL_ALERT, Logging.Category.tool);

        BenchmarkOSDPerformance readBench = new ReadBenchmarkOSDPerformance();
        int numberOfReaders = 1;
        long sizeInBytes = (long) 3 * GiB_IN_BYTES;

        ConcurrentLinkedQueue<BenchmarkResult> results = scheduleBenchmarks(readBench.dirAddress, readBench.mrcAddress,
                readBench.userCredentials, readBench.auth, readBench.sslOptions, numberOfReaders, sizeInBytes);

        /* cleaning up */
        for (int i = 0; i < numberOfReaders; i++)
            readBench.deleteVolumeIfExisting(VOLUME_BASE_NAME + i);

        scrub("47c551e1-2f30-42da-be3f-8c91c51dd15b", "");

        /* print the results */
        for (BenchmarkResult res : results) {
            System.out.println(res);
        }

    }

}
