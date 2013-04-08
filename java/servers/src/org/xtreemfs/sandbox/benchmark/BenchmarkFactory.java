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
public class BenchmarkFactory {

    public static Benchmark createBenchmark(BenchmarkType benchmarkType, Volume volume, Params params) throws Exception {

        Benchmark benchmark = null;

        switch (benchmarkType) {
        case WRITE:
            benchmark = new WriteBenchmark(volume, params);
            break;
        case READ:
            benchmark = new ReadBenchmark(volume, params);
            break;
        case RANDOM_IO_WRITE:
            break;
        case RANDOM_IO_READ:
            benchmark = new RandomReadBenchmark(volume, params);
            break;
        case RANDOM_IO_WRITE_FILEBASED:
            benchmark = new FilebasedRandomWriteBenchmark(volume, params);
            break;
        case RANDOM_IO_READ_FILEBASED:
            benchmark = new FilebasedRandomReadBenchmark(volume, params);
            break;
        }
        return benchmark;
    }
}
