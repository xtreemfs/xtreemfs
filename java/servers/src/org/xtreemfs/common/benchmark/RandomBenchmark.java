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
 * Abstract baseclass for random IO benchmarks.
 * 
 * Todo (jvf): If filebased benchmark is refactored to not be random IO benchmark, this class is useless
 * 
 * @author jensvfischer
 */
abstract class RandomBenchmark extends AbstractBenchmark {

    RandomBenchmark(Config config, AdminClient client, VolumeManager volumeManager) throws Exception {
        super(config.getRandomSizeInBytes(), config, client, volumeManager);
    }
}
