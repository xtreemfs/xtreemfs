/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.util.Map.Entry;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * Updates periodically the fileSize at the MRC. 
 */
public class PeriodicFileSizeUpdateThread extends Thread {

    private VolumeImplementation volume = null;

    public PeriodicFileSizeUpdateThread(VolumeImplementation volume, boolean startAsDaemon) {
        this.volume = volume;
        setDaemon(startAsDaemon);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        while (!isInterrupted()) {
            // send thread to sleep (default 1minute)
            try {
                Thread.sleep(volume.getOptions().getPeriodicFileSizeUpdatesIntervalS() * 1000);
            } catch (InterruptedException e) {
                break;
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "START openFileTable: Periodic filesize update for %s open files.", volume
                                .getOpenFileTable().size());
            }

            // Iterate over the openFileTable
            for (Entry<Long, FileInfo> entry : volume.getOpenFileTable().entrySet()) {
                try {
                    entry.getValue().writeBackFileSizeAsync();
                } catch (IOException e) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "PeriodicFileSizeUpdateThread: failed to update filesize. Reason: ",
                                e.getMessage());
                    }
                } 
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "END openFileTable: Periodic filesize update for %s open files.", volume
                                .getOpenFileTable().size());
            }
        }
    }
}
