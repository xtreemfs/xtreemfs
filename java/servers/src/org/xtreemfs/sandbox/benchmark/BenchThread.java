/*
 * Copyright (c) 2009-2011 by Jens V. Fischer,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread for a {@link Benchmark}. Starts
 * {@link Benchmark#benchmark(java.util.concurrent.ConcurrentLinkedQueue)} as run method.
 * 
 * @author jensvfischer
 */
public class BenchThread implements Runnable {

    private ConcurrentLinkedQueue<BenchmarkResult> results;
    private Benchmark benchmark;

    public BenchThread(Benchmark benchmark, ConcurrentLinkedQueue<BenchmarkResult> results) {
        this.benchmark = benchmark;
        this.results = results;
    }

    @Override
    public void run() {
        try {
            benchmark.benchmark(results);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
