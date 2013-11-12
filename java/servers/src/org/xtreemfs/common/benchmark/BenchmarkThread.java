/*
 * Copyright (c) 2009-2011 by Jens V. Fischer,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import org.xtreemfs.foundation.logging.Logging;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread for a {@link AbstractBenchmark}.
 * <p/>
 * Starts {@link AbstractBenchmark#benchmark(java.util.concurrent.ConcurrentLinkedQueue)} as run method.
 * 
 * @author jensvfischer
 */
class BenchmarkThread implements Runnable {

    private ConcurrentLinkedQueue<BenchmarkResult> results;
    private AbstractBenchmark                      benchmark;

    BenchmarkThread(AbstractBenchmark benchmark, ConcurrentLinkedQueue<BenchmarkResult> results) {
        this.benchmark = benchmark;
        this.results = results;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(benchmark.getClass().getSimpleName() + "-Thread");
        benchmark.benchmark(results);

    }
}
