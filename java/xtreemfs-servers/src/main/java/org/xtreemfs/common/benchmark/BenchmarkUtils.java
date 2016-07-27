/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

/**
 * Util class for the Benchmarks
 *
 * @author jensvfischer
 *
 */
public class BenchmarkUtils {

    public static final int KiB_IN_BYTES = 1024;
    public static final int MiB_IN_BYTES = 1024 * 1024;
    public static final int GiB_IN_BYTES = 1024 * 1024 * 1024;

    /**
     * Enum for the different benchmark Types.
     */
    public static enum BenchmarkType {
        SEQ_WRITE, SEQ_UNALIGNED_WRITE, SEQ_READ, RAND_WRITE, RAND_READ, FILES_WRITE, FILES_READ
    }
}
