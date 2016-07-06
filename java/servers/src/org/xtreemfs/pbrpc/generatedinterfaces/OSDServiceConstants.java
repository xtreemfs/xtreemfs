//automatically generated from OSD.proto at Wed Jul 06 11:03:11 CEST 2016
//(c) 2016. See LICENSE file for details.

package org.xtreemfs.pbrpc.generatedinterfaces;

import com.google.protobuf.Message;

public class OSDServiceConstants {

    public static final int INTERFACE_ID = 30001;
    public static final int PROC_ID_READ = 10;
    public static final int PROC_ID_TRUNCATE = 11;
    public static final int PROC_ID_UNLINK = 12;
    public static final int PROC_ID_WRITE = 13;
    public static final int PROC_ID_XTREEMFS_BROADCAST_GMAX = 20;
    public static final int PROC_ID_XTREEMFS_CHECK_OBJECT = 21;
    public static final int PROC_ID_XTREEMFS_CLEANUP_GET_RESULTS = 30;
    public static final int PROC_ID_XTREEMFS_CLEANUP_IS_RUNNING = 31;
    public static final int PROC_ID_XTREEMFS_CLEANUP_START = 32;
    public static final int PROC_ID_XTREEMFS_CLEANUP_STATUS = 33;
    public static final int PROC_ID_XTREEMFS_CLEANUP_STOP = 34;
    public static final int PROC_ID_XTREEMFS_CLEANUP_VERSIONS_START = 35;
    public static final int PROC_ID_XTREEMFS_FINALIZE_VOUCHERS = 22;
    public static final int PROC_ID_XTREEMFS_REPAIR_OBJECT = 36;
    public static final int PROC_ID_XTREEMFS_RWR_FETCH = 73;
    public static final int PROC_ID_XTREEMFS_RWR_FLEASE_MSG = 71;
    public static final int PROC_ID_XTREEMFS_RWR_NOTIFY = 75;
    public static final int PROC_ID_XTREEMFS_RWR_SET_PRIMARY_EPOCH = 78;
    public static final int PROC_ID_XTREEMFS_RWR_STATUS = 76;
    public static final int PROC_ID_XTREEMFS_RWR_TRUNCATE = 74;
    public static final int PROC_ID_XTREEMFS_RWR_UPDATE = 72;
    public static final int PROC_ID_XTREEMFS_RWR_AUTH_STATE = 79;
    public static final int PROC_ID_XTREEMFS_RWR_RESET_COMPLETE = 80;
    public static final int PROC_ID_XTREEMFS_INTERNAL_GET_GMAX = 40;
    public static final int PROC_ID_XTREEMFS_INTERNAL_TRUNCATE = 41;
    public static final int PROC_ID_XTREEMFS_INTERNAL_GET_FILE_SIZE = 42;
    public static final int PROC_ID_XTREEMFS_INTERNAL_READ_LOCAL = 43;
    public static final int PROC_ID_XTREEMFS_INTERNAL_GET_OBJECT_SET = 44;
    public static final int PROC_ID_XTREEMFS_INTERNAL_GET_FILEID_LIST = 45;
    public static final int PROC_ID_XTREEMFS_LOCK_ACQUIRE = 50;
    public static final int PROC_ID_XTREEMFS_LOCK_CHECK = 51;
    public static final int PROC_ID_XTREEMFS_LOCK_RELEASE = 52;
    public static final int PROC_ID_XTREEMFS_PING = 60;
    public static final int PROC_ID_XTREEMFS_SHUTDOWN = 70;
    public static final int PROC_ID_XTREEMFS_XLOC_SET_INVALIDATE = 81;
    public static final int PROC_ID_XTREEMFS_RWR_AUTH_STATE_INVALIDATED = 82;
    public static final int PROC_ID_XTREEMFS_RWR_RESET_STATUS = 83;
    public static final int PROC_ID_XTREEMFS_EC_GET_INTERVAL_VECTORS = 84;
    public static final int PROC_ID_XTREEMFS_EC_COMMIT_VECTOR = 85;
    public static final int PROC_ID_XTREEMFS_EC_WRITE_DATA = 86;
    public static final int PROC_ID_XTREEMFS_EC_READ_DATA = 87;

    public static Message getRequestMessage(int procId) {
        switch (procId) {
           case 10: return OSD.readRequest.getDefaultInstance();
           case 11: return OSD.truncateRequest.getDefaultInstance();
           case 12: return OSD.unlink_osd_Request.getDefaultInstance();
           case 13: return OSD.writeRequest.getDefaultInstance();
           case 20: return OSD.xtreemfs_broadcast_gmaxRequest.getDefaultInstance();
           case 21: return OSD.xtreemfs_check_objectRequest.getDefaultInstance();
           case 30: return null;
           case 31: return null;
           case 32: return OSD.xtreemfs_cleanup_startRequest.getDefaultInstance();
           case 33: return null;
           case 34: return null;
           case 35: return null;
           case 22: return OSD.xtreemfs_finalize_vouchersRequest.getDefaultInstance();
           case 36: return OSD.xtreemfs_repair_objectRequest.getDefaultInstance();
           case 73: return OSD.xtreemfs_rwr_fetchRequest.getDefaultInstance();
           case 71: return OSD.xtreemfs_rwr_flease_msgRequest.getDefaultInstance();
           case 75: return GlobalTypes.FileCredentials.getDefaultInstance();
           case 78: return OSD.xtreemfs_rwr_set_primary_epochRequest.getDefaultInstance();
           case 76: return OSD.xtreemfs_rwr_statusRequest.getDefaultInstance();
           case 74: return OSD.xtreemfs_rwr_truncateRequest.getDefaultInstance();
           case 72: return OSD.xtreemfs_rwr_updateRequest.getDefaultInstance();
           case 79: return OSD.xtreemfs_rwr_auth_stateRequest.getDefaultInstance();
           case 80: return OSD.xtreemfs_rwr_reset_completeRequest.getDefaultInstance();
           case 40: return OSD.xtreemfs_internal_get_gmaxRequest.getDefaultInstance();
           case 41: return OSD.truncateRequest.getDefaultInstance();
           case 42: return OSD.xtreemfs_internal_get_file_sizeRequest.getDefaultInstance();
           case 43: return OSD.xtreemfs_internal_read_localRequest.getDefaultInstance();
           case 44: return OSD.xtreemfs_internal_get_object_setRequest.getDefaultInstance();
           case 45: return null;
           case 50: return OSD.lockRequest.getDefaultInstance();
           case 51: return OSD.lockRequest.getDefaultInstance();
           case 52: return OSD.lockRequest.getDefaultInstance();
           case 60: return OSD.xtreemfs_pingMesssage.getDefaultInstance();
           case 70: return null;
           case 81: return OSD.xtreemfs_xloc_set_invalidateRequest.getDefaultInstance();
           case 82: return OSD.xtreemfs_rwr_auth_stateRequest.getDefaultInstance();
           case 83: return OSD.xtreemfs_rwr_reset_statusRequest.getDefaultInstance();
           case 84: return OSD.xtreemfs_ec_get_interval_vectorsRequest.getDefaultInstance();
           case 85: return OSD.xtreemfs_ec_commit_vectorRequest.getDefaultInstance();
           case 86: return OSD.xtreemfs_ec_write_dataRequest.getDefaultInstance();
           case 87: return OSD.xtreemfs_ec_readRequest.getDefaultInstance();
           default: throw new RuntimeException("unknown procedure id");
        }
    }


    public static Message getResponseMessage(int procId) {
        switch (procId) {
           case 10: return OSD.ObjectData.getDefaultInstance();
           case 11: return GlobalTypes.OSDWriteResponse.getDefaultInstance();
           case 12: return null;
           case 13: return GlobalTypes.OSDWriteResponse.getDefaultInstance();
           case 20: return null;
           case 21: return OSD.ObjectData.getDefaultInstance();
           case 30: return OSD.xtreemfs_cleanup_get_resultsResponse.getDefaultInstance();
           case 31: return OSD.xtreemfs_cleanup_is_runningResponse.getDefaultInstance();
           case 32: return null;
           case 33: return OSD.xtreemfs_cleanup_statusResponse.getDefaultInstance();
           case 34: return null;
           case 35: return null;
           case 22: return GlobalTypes.OSDFinalizeVouchersResponse.getDefaultInstance();
           case 36: return null;
           case 73: return OSD.ObjectData.getDefaultInstance();
           case 71: return null;
           case 75: return null;
           case 78: return OSD.ObjectData.getDefaultInstance();
           case 76: return OSD.ReplicaStatus.getDefaultInstance();
           case 74: return null;
           case 72: return null;
           case 79: return null;
           case 80: return null;
           case 40: return OSD.InternalGmax.getDefaultInstance();
           case 41: return GlobalTypes.OSDWriteResponse.getDefaultInstance();
           case 42: return OSD.xtreemfs_internal_get_file_sizeResponse.getDefaultInstance();
           case 43: return OSD.InternalReadLocalResponse.getDefaultInstance();
           case 44: return OSD.ObjectList.getDefaultInstance();
           case 45: return OSD.xtreemfs_internal_get_fileid_listResponse.getDefaultInstance();
           case 50: return OSD.Lock.getDefaultInstance();
           case 51: return OSD.Lock.getDefaultInstance();
           case 52: return null;
           case 60: return OSD.xtreemfs_pingMesssage.getDefaultInstance();
           case 70: return null;
           case 81: return OSD.xtreemfs_xloc_set_invalidateResponse.getDefaultInstance();
           case 82: return OSD.xtreemfs_rwr_reset_statusResponse.getDefaultInstance();
           case 83: return OSD.xtreemfs_rwr_reset_statusResponse.getDefaultInstance();
           case 84: return OSD.xtreemfs_ec_get_interval_vectorsResponse.getDefaultInstance();
           case 85: return OSD.xtreemfs_ec_commit_vectorResponse.getDefaultInstance();
           case 86: return OSD.xtreemfs_ec_write_dataResponse.getDefaultInstance();
           case 87: return OSD.xtreemfs_ec_readResponse.getDefaultInstance();
           default: throw new RuntimeException("unknown procedure id");
        }
    }


}