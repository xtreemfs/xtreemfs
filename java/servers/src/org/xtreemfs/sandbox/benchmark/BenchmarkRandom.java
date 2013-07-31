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
public abstract class BenchmarkRandom extends AbstractBenchmark {

    public BenchmarkRandom(Volume volume, Params params) throws Exception {
        super(params.randomSizeInBytes, volume, params);
    }
}
