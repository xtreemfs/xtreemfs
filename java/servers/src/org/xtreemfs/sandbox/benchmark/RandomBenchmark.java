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
 * Abstract baseclass for random IO benchmarks.
 * 
 * Todo (jvf): If filebased benchmark is refactored to not be random IO benchmark, this class is useless
 * 
 * @author jensvfischer
 */
abstract class RandomBenchmark extends AbstractBenchmark {

    RandomBenchmark(Volume volume, Config config) throws Exception {
        super(config.getRandomSizeInBytes(), volume, config);
    }
}
