/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.util.Map.Entry;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * Renews periodically the XCap 
 */
public class PeriodicXcapRenewalThread extends Thread {

    private VolumeImplementation volume = null;

    public PeriodicXcapRenewalThread(VolumeImplementation volume, boolean startAsDaemon) {
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
                Thread.sleep(volume.getOptions().getPeriodicXcapRenewalIntervalS()*1000);
            } catch (Exception e) {
                break;
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "START openFileTable: Periodic Xcap renewal for %s open files.", volume
                                .getOpenFileTable().size());
            }

            // iterate over the openFileTable.
            for (Entry<Long, FileInfo> entry : volume.getOpenFileTable().entrySet()) {
            	try {
            		entry.getValue().renewXCapsAsync();	
            	} catch (AddressToUUIDNotFoundException e) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                                "PeriodicXCapThread: failed to renew XCap. Reason: ",
                                e.getMessage());
                    }
				}
                
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                        "END openFileTable: Periodic Xcap renewal for %s open files.", volume
                                .getOpenFileTable().size());
            }
        }
    }
}
