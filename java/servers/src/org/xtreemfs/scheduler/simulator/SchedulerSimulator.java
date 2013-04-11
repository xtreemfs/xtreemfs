/*
 * Copyright (c) 2009-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.scheduler.simulator;

import java.util.ArrayList;
import java.util.List;
import org.xtreemfs.scheduler.algorithm.ReservationScheduler;
import org.xtreemfs.scheduler.algorithm.ReservationSchedulerFactory;
import org.xtreemfs.scheduler.data.OSDDescription;

/**
 *
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class SchedulerSimulator {

    private List<OSDDescription> osds;
    private SchedulerConfig config;
    private ReservationScheduler scheduler;

    public SchedulerSimulator(SchedulerConfig config) {
        this.osds = new ArrayList<OSDDescription>();
        this.config = config;

        // Create OSDs
        for (Integer i : this.config.getOsdNumbers()) {
            for (int j = 0; j < i; j++) {
                String osdName = "osd-" + i + "-" + j;
                OSDDescription osd = new OSDDescription(osdName, 
                        config.getOsdDescriptions().get(i), 
                        config.getOsdTypes().get(i));
                osds.add(osd);
            }
        }

        // Create Scheduler
        this.scheduler = ReservationSchedulerFactory.getScheduler(osds, 
                config.getMinAngle(), 
                config.getCapacityGain(), 
                config.getRandomIOGain(), 
                config.getStreamingGain(), 
                config.isPreferUsedOSDs());
    }

    public void runSimulation() {
    }
}
