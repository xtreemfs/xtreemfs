/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.util.Map.Entry;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * 
 * <br>
 * Oct 20, 2011
 */
public class PeriodicXcapRenewalThread extends Thread {

    private VolumeImplementation volume = null;

    /**
     * 
     */
    public PeriodicXcapRenewalThread(VolumeImplementation volume) {
        this.volume = volume;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        while (true) {
            // send thread to sleep (default 1minute)
            try {
                Thread.sleep(volume.getOptions().getPeriodicXcapRenewalIntervalS()*1000);
            } catch (Exception e) {
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "START openFileTable: Periodic Xcap renewal for %s open files.", volume
                                .getOpenFileTable().size());
            }

            // iterate over the openFileTable.
            for (Entry<Long, FileInfo> entry : volume.getOpenFileTable().entrySet()) {
                entry.getValue().renewXCapsAsync();
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "END openFileTable: Periodic Xcap renewal for %s open files.", volume
                                .getOpenFileTable().size());
            }
        }
    }
}
