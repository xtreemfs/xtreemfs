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
public abstract class FilebasedBenchmark extends RandomBenchmark {

    static final String BENCHMARK_FILENAME           = "benchmarks/randomBenchmark/benchFile";
    final int randomIOFilesize;


    public FilebasedBenchmark(Volume volume, Params params) throws Exception {
        super(volume, params);
        this.randomIOFilesize = params.randomIOFilesize;
    }

    /* convert to 4 KiB Blocks */
    protected long convertTo4KiBBlocks(long numberOfBlocks) {
        return (numberOfBlocks * (long) stripeWidth) / (long) randomIOFilesize;
    }
}
