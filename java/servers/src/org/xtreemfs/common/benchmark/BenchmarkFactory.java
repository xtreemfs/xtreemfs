/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.Volume;

/**
 * Instantiates a benchmark dependig on the BenchmarkType.
 * 
 * @author jensvfischer
 */
class BenchmarkFactory {

    static AbstractBenchmark createBenchmark(BenchmarkUtils.BenchmarkType benchmarkType, Volume volume, Config config, AdminClient client)
            throws Exception {

        AbstractBenchmark benchmark = null;

        switch (benchmarkType) {
        case SEQ_WRITE:
            benchmark = new SequentialWriteBenchmark(volume, config, client);
            break;
        case SEQ_READ:
            benchmark = new SequentialReadBenchmark(volume, config, client);
            break;
        case RAND_WRITE:
            benchmark = new RandomWriteBenchmark(volume, config, client);
            break;
        case RAND_READ:
            benchmark = new RandomReadBenchmark(volume, config, client);
            break;
        case FILES_WRITE:
            benchmark = new FilebasedWriteBenchmark(volume, config, client);
            break;
        case FILES_READ:
            benchmark = new FilebasedReadBenchmark(volume, config, client);
            break;
        }
        return benchmark;
    }
}
