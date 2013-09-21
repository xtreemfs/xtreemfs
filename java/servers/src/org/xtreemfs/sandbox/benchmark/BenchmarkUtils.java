/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

class BenchmarkUtils {

    private static final int KiB_IN_BYTES = 1024;
    private static final int MiB_IN_BYTES = 1024 * 1024;
    private static final int GiB_IN_BYTES = 1024 * 1024 * 1024;

	static int getKiB_IN_BYTES() {
		return KiB_IN_BYTES;
	}

	static int getMiB_IN_BYTES() {
        return MiB_IN_BYTES;
    }

    static int getGiB_IN_BYTES() {
        return GiB_IN_BYTES;
    }
}
