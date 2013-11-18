/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.simulator;

import org.xtreemfs.scheduler.data.Reservation;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class SchedulerEvent {
    private long timeStamp;
    private SchedulerSimulatorOperation operation;
    private Reservation reservation;
    private String volumeName;

    public SchedulerEvent(long timeStamp,
                          SchedulerSimulatorOperation operation,
                          Reservation reservation,
                          String volumeName) {
        this.setTimeStamp(timeStamp);
        this.setOperation(operation);
        this.setReservation(reservation);
        this.setVolumeName(volumeName);
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public SchedulerSimulatorOperation getOperation() {
        return operation;
    }

    public void setOperation(SchedulerSimulatorOperation operation) {
        this.operation = operation;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    enum SchedulerSimulatorOperation {
        CREATE_VOLUME,
        DELETE_VOLUME
    }
}
