/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import org.xtreemfs.foundation.logging.Logging;

/**
 * UncaughtExceptionHandler for the benchmark tool.
 * <p/>
 * 
 * Shuts down all benchmarks and cleans up in case of uncaught exceptions.
 * 
 * @author jensvfischer
 */
class UncaughtExceptionHandlerBenchmark implements Thread.UncaughtExceptionHandler {

    private Controller controller;

    UncaughtExceptionHandlerBenchmark(Controller controller) {
        this.controller = controller;
    }

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
                "An uncaught exception was thrown in %s (Thread-Id: %s). The benchmark tool will be shut down.", t.getName(), t.getId());
        Logging.logError(Logging.LEVEL_ERROR, this, e);

        controller.deleteVolumesAndFiles();
        controller.shutdownClients();
        controller.shutdownThreadPool();
    }
}
