/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import org.xtreemfs.common.libxtreemfs.Volume;

/**
 * @author jensvfischer
 */
public abstract class FilebasedRandomBenchmark extends RandomBenchmark {

    static final String BENCHMARK_FILENAME           = "benchmarks/randomBenchmark/benchFile";
    final int randomIOFilesize;


    public FilebasedRandomBenchmark(Volume volume, Params params) throws Exception {
        super(volume, params);
        this.randomIOFilesize = params.randomIOFilesize;
    }

    /* convert to 4 KiB Blocks */
    protected long convertTo4KiBBlocks(long numberOfBlocks) {
        return (numberOfBlocks * (long) XTREEMFS_BLOCK_SIZE_IN_BYTES) / (long) randomIOFilesize;
    }
}
