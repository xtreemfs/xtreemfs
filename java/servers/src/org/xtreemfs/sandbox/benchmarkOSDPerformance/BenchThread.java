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
 * Thread for a {@link BenchmarkOSDPerformance}. Starts
 * {@link BenchmarkOSDPerformance#benchmark(long, java.util.concurrent.ConcurrentLinkedQueue)} as run method.
 * 
 * @author jensvfischer
 */
public class BenchThread implements Runnable {

    private final long                             sizeInBytes;
    private ConcurrentLinkedQueue<BenchmarkResult> results;
    private BenchmarkOSDPerformance                benchmarkOSDPerformance;

    public BenchThread(BenchmarkOSDPerformance benchmarkOSDPerformance, long sizeInBytes,
            ConcurrentLinkedQueue<BenchmarkResult> results) {
        this.benchmarkOSDPerformance = benchmarkOSDPerformance;
        this.sizeInBytes = sizeInBytes;
        this.results = results;
    }

    @Override
    public void run() {
        try {
            benchmarkOSDPerformance.benchmark(sizeInBytes, results);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
