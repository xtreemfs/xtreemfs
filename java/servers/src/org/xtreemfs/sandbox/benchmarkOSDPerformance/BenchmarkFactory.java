/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import java.util.LinkedList;

import org.xtreemfs.common.libxtreemfs.Volume;

/**
 * @author jensvfischer
 */
public class BenchmarkFactory {

    public static Benchmark createBenchmark(BenchmarkType benchmarkType, Volume volume, ConnectionData connection) throws Exception {

        Benchmark benchmark = null;

        switch (benchmarkType) {
        case WRITE:
            benchmark = new WriteBenchmark(volume, connection);
            break;
        case READ:
            benchmark = new ReadBenchmark(volume, connection);
            break;
        case RANDOM_IO_WRITE:
            break;
        case RANDOM_IO_READ:
            benchmark = new RandomReadBenchmark(volume, connection);
            break;
        case RANDOM_IO_WRITE_FILEBASED:
            benchmark = new FilebasedRandomWriteBenchmark(volume, connection);
            break;
        case RANDOM_IO_READ_FILEBASED:
            benchmark = new FilebasedRandomReadBenchmark(volume, connection);
            break;
        }
        return benchmark;
    }
}
