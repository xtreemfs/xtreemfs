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
 * Instantiates a benchmark dependig on the BenchmarkType.
 * 
 * @author jensvfischer
 */
class BenchmarkFactory {

    static AbstractBenchmark createBenchmark(long size, BenchmarkUtils.BenchmarkType benchmarkType, BenchmarkConfig config, AdminClient client, VolumeManager volumeManager)
            throws Exception {

        AbstractBenchmark benchmark = null;

        switch (benchmarkType) {
        case SEQ_WRITE:
            benchmark = new SequentialWriteBenchmark(size, config, client, volumeManager);
            break;
        case SEQ_UNALIGNED_WRITE:
            benchmark = new UnalignedSequentialWriteBenchmark(size, config, client, volumeManager);
            break;
        case SEQ_READ:
            benchmark = new SequentialReadBenchmark(size, config, client, volumeManager);
            break;
        case RAND_WRITE:
            benchmark = new RandomWriteBenchmark(size, config, client, volumeManager);
            break;
        case RAND_READ:
            benchmark = new RandomReadBenchmark(size, config, client, volumeManager);
            break;
        case FILES_WRITE:
            benchmark = new FilebasedWriteBenchmark(size, config, client, volumeManager);
            break;
        case FILES_READ:
            benchmark = new FilebasedReadBenchmark(size, config, client, volumeManager);
            break;
        }
        return benchmark;
    }
}
