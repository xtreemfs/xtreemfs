/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

/**
 * @author jensvfischer
 */
public class Runner {

    public static void main(String[] args) throws Exception {
        xtfs_benchmark.main(new String[] { "-sw", "-ssize", "10m", "-p", "5", "--no-cleanup-volumes", "bench1", "bench2",
                "bench3", "bench4", "bench5", "bench6", "bench7", "bench8", "bench9", "bench10" });

    }
}
