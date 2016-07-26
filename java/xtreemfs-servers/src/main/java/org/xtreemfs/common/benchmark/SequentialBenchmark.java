/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import org.xtreemfs.common.libxtreemfs.AdminClient;

/**
 * Abstract baseclass for sequential benchmarks.
 * 
 * @author jensvfischer
 */
abstract class SequentialBenchmark extends AbstractBenchmark {

    static final String BENCHMARK_FILENAME = "benchmarks/sequentialBenchmark/benchFile";

    SequentialBenchmark(long size, BenchmarkConfig config, AdminClient client, VolumeManager volumeManager) throws Exception {
        super(size, config, client, volumeManager);
    }

    @Override
    void prepareBenchmark() throws Exception {
    }

    @Override
    void finalizeBenchmark() throws Exception {
    }

    static String getBenchmarkFilename() {
        return BENCHMARK_FILENAME;
    }
}
