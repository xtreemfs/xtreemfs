/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils.xtfs_benchmark;

import org.xtreemfs.foundation.logging.Logging;

/**
 * UncaughtExceptionHandler for the benchmark tool.
 * <p/>
 * 
 * Forces exit of benchmark tool in case of uncaught exceptions.
 * 
 * @author jensvfischer
 */
class UncaughtExceptionHandlerBenchmark implements Thread.UncaughtExceptionHandler {

    /**
     * Method invoked when the given thread terminates due to the given uncaught exception.
     * <p>
     * Any exception thrown by this method will be ignored by the Java Virtual Machine.
     * 
     * @param t
     *            the thread
     * @param e
     *            the exception
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Logging.logMessage(Logging.LEVEL_ERROR, this,
                "An uncaught exception was thrown in %s. The benchmark tool will be terminated.", t.getName());
        Logging.logError(Logging.LEVEL_ERROR, this, e);
        System.exit(1);
    }
}
