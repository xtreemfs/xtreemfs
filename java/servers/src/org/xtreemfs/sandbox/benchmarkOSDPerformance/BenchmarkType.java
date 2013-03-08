/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

/**
 * Enum for the different benchmark Types.
 * 
 * @author jensvfischer
 */
public enum BenchmarkType {
    READ, WRITE, SEQUENTIAL_SINGLE_WRITE, SEQUENTIAL_MULTI_WRITE, SEQUENTIAL_SINGLE_READ, SEQUENTIAL_MULTI_READ, RANDOM_IO, RANDOM_IO_WRITE, RANDOM_IO_READ
}
