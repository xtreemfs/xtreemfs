/*
 * Copyright (c) 2009-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.monitoring;

import java.io.IOException;

/**
 * 
 * <br>
 * 17.08.2009
 */
public class MonitoringLog implements MonitoringListener<Double> {
    private static MonitoringLog instance;

    private long                 monitoringStartTime;

    public static synchronized void initialize(String filepath) throws IOException {
        if (instance == null) {
            instance = new MonitoringLog();

            instance.monitoringStartTime = System.currentTimeMillis();

            // file to write to
            // (new File(filepath)).getParentFile().mkdirs();
            // instance.out = new FileWriter(filepath);
        }
    }

    @Override
    public void valueAddedOrChanged(MonitoringEvent<Double> event) {
        monitor(event.getKey(), event.getNewValue().toString());
    }

    public static synchronized void monitor(String key, String value) {
        long time = (System.currentTimeMillis() - instance.monitoringStartTime) / 1000;
        System.out.println("[" + time + "s]\t" + key + "\t:\t" + value);
    }

    @SuppressWarnings("unchecked")
    public static void registerFor(Monitoring monitoring, String... keys) {
        for (String key : keys)
            monitoring.registerListener(key, instance);
    }
}
