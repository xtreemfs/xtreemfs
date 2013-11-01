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
 * Abstract baseclass for random IO benchmarks.
 *
 * @author jensvfischer
 */
abstract class RandomBenchmark extends AbstractBenchmark {

    RandomBenchmark(long size, Config config, AdminClient client, VolumeManager volumeManager) throws Exception {
        super(size, config, client, volumeManager);
    }
}
