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
import org.xtreemfs.scheduler.data.store.ReservationStore;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        List<OSDDescription> osds = master.getOsds();

        double freeStreamingCapacity = 0.0;
        double freeRandomCapacity = 0.0;
        double freeIOPS = 0.0;
        double freeSeq = 0.0;

        // Get sequential throughput and capacity
        Collections.sort(osds, new Comparator<OSDDescription>() {
            @Override
            public int compare(OSDDescription o1, OSDDescription o2) {
                if(getFreeSeqTp(o1) > getFreeSeqTp(o2))
                    return -1;
                else if(getFreeSeqTp(o1) < getFreeSeqTp(o2))
                    return 1;
                else
                    return 0;
            }
        });

        for(int i = 0; i < osds.size(); i++) {
            OSDDescription o = osds.get(i);
            double minCapacity = getFreeSeqCapacity(o);
            int num = 0;

            for(int j = 0; j < osds.size(); j++) {
                if(getFreeSeqTp(osds.get(j)) >= getFreeSeqTp(osds.get(i))) {
                    num++;
                    minCapacity = Math.min(getFreeSeqCapacity(osds.get(i)), getFreeSeqCapacity(osds.get(j)));
                }
            }

            if(getFreeSeqTp(osds.get(i)) > freeSeq) {
                freeSeq = getFreeSeqTp(osds.get(i)) * num;
                freeStreamingCapacity = minCapacity * num;
            } else if(getFreeSeqTp(osds.get(i)) == freeSeq && freeStreamingCapacity < minCapacity * num) {
                freeStreamingCapacity = minCapacity * num;
            }
        }

        // Get random throughput and capacity
        Collections.sort(osds, new Comparator<OSDDescription>() {
            @Override
            public int compare(OSDDescription o1, OSDDescription o2) {
                if(getFreeIOPS(o1) > getFreeIOPS(o2))
                    return -1;
                else if(getFreeIOPS(o1) < getFreeIOPS(o2))
                    return 1;
                else
                    return 0;
            }
        });

        for(int i = 0; i < osds.size(); i++) {
            OSDDescription o = osds.get(i);
            double minCapacity = getFreeRandomCapacity(o);
            int num = 0;

            for(int j = 0; j < osds.size(); j++) {
                if(getFreeIOPS(osds.get(j)) >= getFreeIOPS(osds.get(i))) {
                    num++;
                    minCapacity = Math.min(getFreeRandomCapacity(osds.get(i)), getFreeRandomCapacity(osds.get(j)));
                }
            }

            if(getFreeIOPS(osds.get(i)) > freeIOPS) {
                freeIOPS = getFreeIOPS(osds.get(i)) * num;
                freeRandomCapacity = minCapacity * num;
            } else if(getFreeIOPS(osds.get(i)) == freeIOPS && freeRandomCapacity < minCapacity * num) {
                freeRandomCapacity = minCapacity * num;
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

    private double getFreeSeqTp(OSDDescription osd) {
        if(osd.getUsage() == OSDDescription.OSDUsage.STREAMING ||
                osd.getUsage() == OSDDescription.OSDUsage.UNUSED ||
                osd.getUsage() == OSDDescription.OSDUsage.ALL) {
            return osd.getFreeResources().getSeqTP();
        } else {
            return 0.0;
        }
    }

    private double getFreeSeqCapacity(OSDDescription osd) {
        if(osd.getUsage() == OSDDescription.OSDUsage.STREAMING ||
                osd.getUsage() == OSDDescription.OSDUsage.UNUSED ||
                osd.getUsage() == OSDDescription.OSDUsage.ALL) {
            return osd.getFreeResources().getCapacity();
        } else {
            return 0.0;
        }
    }

    private double getFreeIOPS(OSDDescription osd) {
        if(osd.getUsage() == OSDDescription.OSDUsage.RANDOM_IO ||
                osd.getUsage() == OSDDescription.OSDUsage.UNUSED ||
                osd.getUsage() == OSDDescription.OSDUsage.ALL) {
            return osd.getFreeResources().getIops();
        } else {
            return 0.0;
        }
    }

    private double getFreeRandomCapacity(OSDDescription osd) {
        if(osd.getUsage() == OSDDescription.OSDUsage.RANDOM_IO ||
                osd.getUsage() == OSDDescription.OSDUsage.UNUSED ||
                osd.getUsage() == OSDDescription.OSDUsage.ALL) {
            return osd.getFreeResources().getCapacity();
        } else {
            return 0.0;
        }
    }
}
