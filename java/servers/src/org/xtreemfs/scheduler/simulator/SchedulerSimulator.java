/*
 * Copyright (c) 2009-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.scheduler.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.scheduler.algorithm.ReservationScheduler;
import org.xtreemfs.scheduler.algorithm.ReservationSchedulerFactory;
import org.xtreemfs.scheduler.data.OSDDescription;
import org.xtreemfs.scheduler.data.Reservation;

/**
 *
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class SchedulerSimulator {

    private final int QUEUE_CAPACITY = 10000;

    private List<OSDDescription> osds;
    private SchedulerConfig config;
    private ReservationScheduler scheduler;
    private EventQueue queue;

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

        this.queue = new EventQueue(QUEUE_CAPACITY);
    }

    public void runSimulation() {
        for(long step = 0; step < this.config.getSimulationSteps(); step++) {
            generateEvents();

            if(this.queue.size() > 0) {
                SchedulerEvent e = this.queue.getNextEvent();
                handleEvent(e);
            }

            updateStatistics();
        }
    }

    private void handleEvent(SchedulerEvent e) {
        switch(e.getOperation()) {
            case CREATE_VOLUME:
                Map<String, Reservation> reservations = new HashMap<String, Reservation>();
                reservations.put(e.getVolumeName(), e.getReservation());
                scheduler.addReservations(reservations);
                break;
            case DELETE_VOLUME:
                scheduler.removeReservation(e.getVolumeName());
                break;
            default:
        }
    }

    private void generateEvents() {

    }

    private void updateStatistics() {

    }
}
