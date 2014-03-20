/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.operations;

import com.google.protobuf.Message;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceConstants;
import org.xtreemfs.scheduler.SchedulerRequest;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;
import org.xtreemfs.scheduler.data.OSDDescription;
import org.xtreemfs.scheduler.data.Reservation;
import org.xtreemfs.scheduler.data.store.ReservationStore;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class GetFreeResourcesOperation extends SchedulerOperation {
    public GetFreeResourcesOperation(SchedulerRequestDispatcher master) throws BabuDBException {
        super(master);
    }

    @Override
    public int getProcedureId() {
        return SchedulerServiceConstants.PROC_ID_GETFREERESOURCES;
    }

    @Override
    public void startRequest(SchedulerRequest rq) {
        Scheduler.freeResourcesResponse.Builder resultBuilder = Scheduler.freeResourcesResponse.newBuilder();
        ReservationStore reservations = master.getStore();

        double freeStreamingCapacity = 0.0;
        double freeRandomCapacity = 0.0;

        double freeIOPS = 0.0;
        double freeSeq = 0.0;

        for(OSDDescription osd: master.getOsds()) {
            if(osd.getUsage() == OSDDescription.OSDUsage.STREAMING ||
                    osd.getUsage() == OSDDescription.OSDUsage.UNUSED ||
                    osd.getUsage() == OSDDescription.OSDUsage.ALL) {
                freeSeq += osd.getCapabilities().getStreamingPerformance().get(osd.getReservations().size() + 1);
                freeStreamingCapacity += osd.getCapabilities().getCapacity();
            }

            if(osd.getUsage() == OSDDescription.OSDUsage.RANDOM_IO ||
                    osd.getUsage() == OSDDescription.OSDUsage.UNUSED ||
                    osd.getUsage() == OSDDescription.OSDUsage.ALL) {
                freeIOPS += osd.getCapabilities().getIops();
                freeRandomCapacity += osd.getCapabilities().getCapacity();
            }
        }

        for(Reservation r: reservations.getReservations()) {
            if(r.getType() == Reservation.ReservationType.STREAMING_RESERVATION) {
                freeSeq -= r.getStreamingThroughput();
                freeStreamingCapacity -= r.getCapacity();
            }
            if(r.getType() == Reservation.ReservationType.RANDOM_IO_RESERVATION) {
                freeIOPS -= r.getRamdomThroughput();
                freeRandomCapacity -= r.getCapacity();
            }
        }

        resultBuilder.setStreamingCapacity(freeStreamingCapacity);
        resultBuilder.setRandomCapacity(freeRandomCapacity);
        resultBuilder.setRandomThroughput(freeIOPS);
        resultBuilder.setStreamingThroughput(freeSeq);
        rq.sendSuccess(resultBuilder.build());
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    protected Message getRequestMessagePrototype() {
        return Scheduler.freeResourcesResponse.getDefaultInstance();
    }

    @Override
    void requestFinished(Object result, SchedulerRequest rq) {
        rq.sendSuccess((Scheduler.freeResourcesResponse) result);
    }
}
