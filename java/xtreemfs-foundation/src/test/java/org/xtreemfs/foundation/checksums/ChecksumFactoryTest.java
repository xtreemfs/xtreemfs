/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.foundation.checksums.ChecksumAlgorithm;
import org.xtreemfs.foundation.checksums.ChecksumFactory;
import org.xtreemfs.foundation.checksums.ChecksumProvider;
import org.xtreemfs.foundation.checksums.provider.JavaChecksumProvider;
import org.xtreemfs.foundation.logging.Logging;

/**
 * tests the checksum factory and some checksums
 *
 * 19.08.2008
 *
 * @author clorenz
 */
public class ChecksumFactoryTest {
    private ChecksumFactory factory;
    private ByteBuffer      data;

    @Before
    public void setUp() throws Exception {
        Logging.start(Logging.LEVEL_ERROR);

        this.factory = ChecksumFactory.getInstance();

        ChecksumProvider provider = new JavaChecksumProvider();
        this.factory.addProvider(provider);

        this.data = ByteBuffer.wrap(generateRandomBytes(1024 * 128));
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * generates randomly filled byte-array
     *
     * @param length
     *            of the byte-array
     */
    public static byte[] generateRandomBytes(int length) {
        Random r = new Random();
        byte[] bytes = new byte[length];

        r.nextBytes(bytes);
        return bytes;
    }

    /**
     * tests the internal java checksum algorithms
     * 
     * @throws Exception
     */
    @Test
    public void testJavaChecksumAlgorithm() throws Exception {
        // compute checksum with xtreemfs ChecksumFactory
        long xtreemfsValue = computeXtreemfsChecksum("Adler32", true);

        // compute checksum with java API
        Checksum javaAlgorithm = new Adler32();
        javaAlgorithm.update(data.array(), 0, data.array().length);
        long javaValue = javaAlgorithm.getValue();

        // System.out.println(javaValue);
        // System.out.println(xtreemfsValue);

        assertEquals(javaValue, xtreemfsValue);
    }

    // /**
    // * tests the internal java message digest algorithms
    // * @throws Exception
    // */
    // public void testJavaMessageDigestAlgorithm() throws Exception {
    // // compute checksum with xtreemfs ChecksumFactory
    // String xtreemfsValue = computeXtreemfsChecksum("MD5", true);
    //
    // // compute checksum with java API
    // String javaValue = computeJavaMessageDigest("MD5");
    //
    // // System.out.println("java:     "+xtreemfsValue);
    // // System.out.println("xtreemfs: "+javaValue.toString());
    //
    // assertEquals(javaValue.toString(), xtreemfsValue);
    // }

    /**
     * @param algorithm
     * @param returnAlgorithm
     * @return
     * @throws NoSuchAlgorithmException
     */
    private long computeXtreemfsChecksum(String algorithm, boolean returnAlgorithm) throws NoSuchAlgorithmException {
        // compute checksum with xtreemfs ChecksumFactory
        ChecksumAlgorithm xtreemfsAlgorithm = factory.getAlgorithm(algorithm);
        xtreemfsAlgorithm.update(data);
        long xtreemfsValue = xtreemfsAlgorithm.getValue();
        if (returnAlgorithm)
            this.factory.returnAlgorithm(xtreemfsAlgorithm);
        return xtreemfsValue;
    }

    private long computeJavaCheckSum(String algorithm) throws NoSuchAlgorithmException {
        // compute checksum with java API
        Adler32 adler = new Adler32();
        adler.update(data.array());
        return adler.getValue();
    }

    /**
     * tests, if the internal buffer of the checksums is working correctly, if the checksum is used more than once
     * 
     * @throws Exception
     */
    @Test
    public void testIfChecksumIsAlwaysTheSame() throws Exception {
        ChecksumAlgorithm algorithm = factory.getAlgorithm("Adler32");
        algorithm.update(data);
        long oldValue = algorithm.getValue();

        for (int i = 0; i < 32; i++) {
            algorithm.update(data);
            long newValue = algorithm.getValue();

            assertEquals(oldValue, newValue);
            oldValue = newValue;
        }
    }

    /**
     * tests, if the ChecksumFactory delivers only "thread-safe" instances (cache-pool)
     * 
     * @throws Exception
     */
    @Test
    public void testThreadSafety() throws Exception {
        final int THREADS = 8;
        this.data = ByteBuffer.wrap(generateRandomBytes(1024 * 1024 * 32));

        // compute correct checksum with java API
        Long javaValue = computeJavaCheckSum("Adler32");

        Callable<Long> computation = new Callable<Long>() {
            @Override
            public Long call() {
                try {
                    // compute checksum with xtreemfs ChecksumFactory
                    long xtreemfsValue = computeXtreemfsChecksum("Adler32", true);
                    return xtreemfsValue;
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return 0l;
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0l;
                }
            }
        };
        LinkedList<Future<Long>> results = useMultipleThreads(THREADS, computation);

        // compare correct java checksum with xtreemfs checksums
        for (Future<Long> result : results) {
            assertEquals(javaValue, result.get());
        }
    }

    /**
     * tests, if the ChecksumFactory cache-pool works correctly
     * 
     * @throws Exception
     */
    @Test
    public void testChecksumFactoryCache() throws Exception {
        // FIXME: use bigger values for more comprehensive testing, but this will slow down the test
        final int THREADS = 8;
        final int ROUNDS = 50;
        this.data = ByteBuffer.wrap(generateRandomBytes(1024 * 1024));

        // compute correct checksum with java API
        Long javaValue = computeJavaCheckSum("Adler32");

        Callable<LinkedList<Long>> computation = new Callable<LinkedList<Long>>() {
            @Override
            public LinkedList<Long> call() {
                try {
                    LinkedList<Long> values = new LinkedList<Long>();
                    boolean returning = false;
                    for (int i = 0; i < ROUNDS; i++) {
                        // compute checksum with xtreemfs ChecksumFactory
                        long xtreemfsValue = computeXtreemfsChecksum("Adler32", returning);
                        values.add(xtreemfsValue);
                        returning = !returning;
                    }
                    return values;
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        LinkedList<Future<LinkedList<Long>>> results = useMultipleThreads(THREADS, computation);

        // compare correct java checksum with xtreemfs checksums
        for (Future<LinkedList<Long>> result : results) {
            for (Long value : result.get()) {
                assertEquals(javaValue, value);
            }
        }
    }

    /**
     * executes a given computation in a couple of threads and returns the results of the computations
     *
     * @param THREADS
     * @param computation
     * @return a list of futures, which contain the results of the computations
     * @throws InterruptedException
     */
    private <E> LinkedList<Future<E>> useMultipleThreads(final int THREADS, Callable<E> computation)
            throws InterruptedException {
        LinkedList<Future<E>> results = new LinkedList<Future<E>>();
        // compute xtreemfs checksums with multiple threads
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < THREADS; i++) {
            Future<E> tmp = executor.submit(computation);
            results.add(tmp);
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        return results;
    }
}
