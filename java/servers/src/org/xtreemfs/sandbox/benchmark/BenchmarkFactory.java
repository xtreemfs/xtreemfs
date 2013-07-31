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

    public static AbstractBenchmark createBenchmark(AbstractBenchmark.BenchmarkType benchmarkType, Volume volume, Params params) throws Exception {

        AbstractBenchmark benchmark = null;

        switch (benchmarkType) {
        case SEQ_WRITE:
            benchmark = new WriteBenchmark(volume, params);
            break;
        case SEQ_READ:
            benchmark = new ReadBenchmark(volume, params);
            break;
        case RAND_WRITE:
			benchmark = new RandomWriteBenchmark(volume, params);
            break;
        case RAND_READ:
            benchmark = new RandomReadBenchmark(volume, params);
            break;
        case FILES_WRITE:
            benchmark = new FilebasedWriteBenchmark(volume, params);
            break;
        case FILES_READ:
            benchmark = new FilebasedReadBenchmark(volume, params);
            break;
        }
        return benchmark;
    }
}
