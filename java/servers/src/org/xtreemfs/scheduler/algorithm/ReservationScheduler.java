/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.scheduler.algorithm;

import java.util.List;
import java.util.Map;

import org.xtreemfs.scheduler.data.OSDDescription;
import org.xtreemfs.scheduler.data.Reservation;
import org.xtreemfs.scheduler.data.ResourceSet;
import org.xtreemfs.scheduler.exceptions.SchedulerException;

/**
 *
 * @author kleineweber
 */
public interface ReservationScheduler {
    public List<OSDDescription> scheduleReservation(Reservation r) throws SchedulerException;
    public void removeReservation(String volumeIdentifier);
    public void addReservations(Map<String, Reservation> reservations);
    public void reset();
    public ResourceSet getFreeResources();
}
