/*
 * Copyright (c) 2009-2011 by Jens V. Fischer,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread for a {@link Benchmark}. Starts
 * {@link Benchmark#benchmark(long, java.util.concurrent.ConcurrentLinkedQueue)} as run method.
 * 
 * @author jensvfischer
 */
public class BenchThread implements Runnable {

    private final long                             sizeInBytes;
    private ConcurrentLinkedQueue<BenchmarkResult> results;
    private Benchmark benchmark;

    public BenchThread(Benchmark benchmark, long sizeInBytes, ConcurrentLinkedQueue<BenchmarkResult> results) {
        this.benchmark = benchmark;
        this.sizeInBytes = sizeInBytes;
        this.results = results;
    }

    @Override
    public void run() {
        try {
            benchmark.benchmark(sizeInBytes, results);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
