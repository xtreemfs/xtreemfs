//automatically generated from Ping.proto at Thu Oct 16 09:46:15 CEST 2014
//(c) 2014. See LICENSE file for details.

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