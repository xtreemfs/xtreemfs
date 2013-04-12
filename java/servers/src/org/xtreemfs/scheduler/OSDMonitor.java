/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.logging.Logging;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class OSDMonitor extends LifeCycleThread {
    private final int SLEEP_TIME = 10 * 1000;

    private SchedulerRequestDispatcher master;
    private volatile boolean quit;

    public OSDMonitor(SchedulerRequestDispatcher master) {
        super("OSDMonitor");
        this.master = master;
        this.quit = false;
        this.notifyStarted();
    }

    @Override
    public void shutdown() {
        this.quit = true;
    }

    @Override
    public void run() {
        while(!quit) {
            try{
                master.reloadOSDs();
                LifeCycleThread.sleep(SLEEP_TIME);
            } catch(Exception ex) {
                Logging.logError(Logging.LEVEL_ERROR, this, ex);
            }
        }
    }
}
