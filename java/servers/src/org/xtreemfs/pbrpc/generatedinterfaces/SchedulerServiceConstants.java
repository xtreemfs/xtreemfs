//automatically generated from Scheduler.proto at Fri Apr 05 14:03:44 CEST 2013
//(c) 2013. See LICENSE file for details.

package org.xtreemfs.pbrpc.generatedinterfaces;

import com.google.protobuf.Message;

public class SchedulerServiceConstants {

    public static final int INTERFACE_ID = 40001;
    public static final int PROC_ID_SCHEDULERESERVATION = 101;
    public static final int PROC_ID_REMOVERESERVATION = 102;
    public static final int PROC_ID_GETSCHEDULE = 103;
    public static final int PROC_ID_GETVOLUMES = 104;
    public static final int PROC_ID_GETALLVOLUMES = 105;

    public static Message getRequestMessage(int procId) {
        switch (procId) {
           case 101: return Scheduler.reservation.getDefaultInstance();
           case 102: return Scheduler.volumeIdentifier.getDefaultInstance();
           case 103: return Scheduler.volumeIdentifier.getDefaultInstance();
           case 104: return Scheduler.osdIdentifier.getDefaultInstance();
           case 105: return null;
           default: throw new RuntimeException("unknown procedure id");
        }
    }


    public static Message getResponseMessage(int procId) {
        switch (procId) {
           case 101: return Scheduler.osdSet.getDefaultInstance();
           case 102: return null;
           case 103: return Scheduler.osdSet.getDefaultInstance();
           case 104: return Scheduler.volumeSet.getDefaultInstance();
           case 105: return Scheduler.reservationSet.getDefaultInstance();
           default: throw new RuntimeException("unknown procedure id");
        }
    }


}