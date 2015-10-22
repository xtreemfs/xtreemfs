//automatically generated from Ping.proto at Thu Aug 20 09:17:22 CEST 2015
//(c) 2015. See LICENSE file for details.

package org.xtreemfs.foundation.pbrpc.generatedinterfaces;

import com.google.protobuf.Message;

public class PingServiceConstants {

    public static final int INTERFACE_ID = 1;
    public static final int PROC_ID_DOPING = 1;
    public static final int PROC_ID_EMPTYPING = 2;

    public static Message getRequestMessage(int procId) {
        switch (procId) {
           case 1: return Ping.PingRequest.getDefaultInstance();
           case 2: return null;
           default: throw new RuntimeException("unknown procedure id");
        }
    }


    public static Message getResponseMessage(int procId) {
        switch (procId) {
           case 1: return Ping.PingResponse.getDefaultInstance();
           case 2: return null;
           default: throw new RuntimeException("unknown procedure id");
        }
    }


}