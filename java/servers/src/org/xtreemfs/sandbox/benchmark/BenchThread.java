/*
 * Copyright (c) 2009-2011 by Jens V. Fischer,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.xtreemfs.foundation.logging.Logging;

/**
 * Thread for a {@link AbstractBenchmark}.
 * <p/>
 * Starts {@link AbstractBenchmark#benchmark(java.util.concurrent.ConcurrentLinkedQueue)} as run method.
 * 
 * @author jensvfischer
 */
class BenchThread implements Runnable {

    private ConcurrentLinkedQueue<BenchmarkResult> results;
    private AbstractBenchmark                      benchmark;

    BenchThread(AbstractBenchmark benchmark, ConcurrentLinkedQueue<BenchmarkResult> results) {
        this.benchmark = benchmark;
        this.results = results;
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName(benchmark.getClass().getSimpleName() + "-Thread");
            benchmark.benchmark(results);
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "Error while trying to run %s", Thread.currentThread()
                    .getName());
            Logging.logError(Logging.LEVEL_ERROR, this, e);
        }
    }

}
