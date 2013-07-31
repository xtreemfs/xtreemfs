/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import org.xtreemfs.common.libxtreemfs.Volume;

/**
 * @author jensvfischer
 */
public abstract class SequentialBenchmark extends AbstractBenchmark {


    static final String BENCHMARK_FILENAME           = "benchmarks/sequentialBenchmark/benchFile";

    public SequentialBenchmark(Volume volume, Params params) throws Exception {
        super(params.sequentialSizeInBytes, volume, params);
    }

    @Override
    void prepareBenchmark() throws Exception {
    }

    @Override
    void finalizeBenchmark() throws Exception {}
}
