//automatically generated from DIR.proto at Thu Aug 20 09:17:21 CEST 2015
//(c) 2015. See LICENSE file for details.

package org.xtreemfs.pbrpc.generatedinterfaces;

import com.google.protobuf.Message;

public class DIRServiceConstants {

    public static final int INTERFACE_ID = 10001;
    public static final int PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_GET = 1;
    public static final int PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_REMOVE = 2;
    public static final int PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_SET = 3;
    public static final int PROC_ID_XTREEMFS_DISCOVER_DIR = 4;
    public static final int PROC_ID_XTREEMFS_GLOBAL_TIME_S_GET = 5;
    public static final int PROC_ID_XTREEMFS_SERVICE_DEREGISTER = 6;
    public static final int PROC_ID_XTREEMFS_SERVICE_GET_BY_NAME = 7;
    public static final int PROC_ID_XTREEMFS_SERVICE_GET_BY_TYPE = 8;
    public static final int PROC_ID_XTREEMFS_SERVICE_GET_BY_UUID = 9;
    public static final int PROC_ID_XTREEMFS_SERVICE_OFFLINE = 10;
    public static final int PROC_ID_XTREEMFS_SERVICE_REGISTER = 11;
    public static final int PROC_ID_XTREEMFS_CHECKPOINT = 20;
    public static final int PROC_ID_XTREEMFS_SHUTDOWN = 21;
    public static final int PROC_ID_XTREEMFS_CONFIGURATION_GET = 22;
    public static final int PROC_ID_XTREEMFS_CONFIGURATION_SET = 23;
    public static final int PROC_ID_XTREEMFS_VIVALDI_CLIENT_UPDATE = 24;

    public static Message getRequestMessage(int procId) {
        switch (procId) {
           case 1: return DIR.addressMappingGetRequest.getDefaultInstance();
           case 2: return DIR.addressMappingGetRequest.getDefaultInstance();
           case 3: return DIR.AddressMappingSet.getDefaultInstance();
           case 4: return null;
           case 5: return null;
           case 6: return DIR.serviceDeregisterRequest.getDefaultInstance();
           case 7: return DIR.serviceGetByNameRequest.getDefaultInstance();
           case 8: return DIR.serviceGetByTypeRequest.getDefaultInstance();
           case 9: return DIR.serviceGetByUUIDRequest.getDefaultInstance();
           case 10: return DIR.serviceGetByUUIDRequest.getDefaultInstance();
           case 11: return DIR.serviceRegisterRequest.getDefaultInstance();
           case 20: return null;
           case 21: return null;
           case 22: return DIR.configurationGetRequest.getDefaultInstance();
           case 23: return DIR.Configuration.getDefaultInstance();
           case 24: return GlobalTypes.VivaldiCoordinates.getDefaultInstance();
           default: throw new RuntimeException("unknown procedure id");
        }
    }


    public static Message getResponseMessage(int procId) {
        switch (procId) {
           case 1: return DIR.AddressMappingSet.getDefaultInstance();
           case 2: return null;
           case 3: return DIR.addressMappingSetResponse.getDefaultInstance();
           case 4: return DIR.DirService.getDefaultInstance();
           case 5: return DIR.globalTimeSGetResponse.getDefaultInstance();
           case 6: return null;
           case 7: return DIR.ServiceSet.getDefaultInstance();
           case 8: return DIR.ServiceSet.getDefaultInstance();
           case 9: return DIR.ServiceSet.getDefaultInstance();
           case 10: return null;
           case 11: return DIR.serviceRegisterResponse.getDefaultInstance();
           case 20: return null;
           case 21: return null;
           case 22: return DIR.Configuration.getDefaultInstance();
           case 23: return DIR.configurationSetResponse.getDefaultInstance();
           case 24: return null;
           default: throw new RuntimeException("unknown procedure id");
        }
    }


}