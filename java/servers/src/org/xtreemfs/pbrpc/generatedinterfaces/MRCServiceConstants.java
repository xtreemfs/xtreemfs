//automatically generated from MRC.proto at Tue Nov 17 15:29:23 CET 2015
//(c) 2015. See LICENSE file for details.

package org.xtreemfs.pbrpc.generatedinterfaces;

import com.google.protobuf.Message;

public class MRCServiceConstants {

    public static final int INTERFACE_ID = 20001;
    public static final int PROC_ID_FSETATTR = 2;
    public static final int PROC_ID_FTRUNCATE = 3;
    public static final int PROC_ID_GETATTR = 4;
    public static final int PROC_ID_GETXATTR = 5;
    public static final int PROC_ID_LINK = 6;
    public static final int PROC_ID_LISTXATTR = 7;
    public static final int PROC_ID_MKDIR = 8;
    public static final int PROC_ID_OPEN = 9;
    public static final int PROC_ID_READDIR = 10;
    public static final int PROC_ID_READLINK = 11;
    public static final int PROC_ID_REMOVEXATTR = 12;
    public static final int PROC_ID_RENAME = 13;
    public static final int PROC_ID_RMDIR = 14;
    public static final int PROC_ID_SETATTR = 15;
    public static final int PROC_ID_SETXATTR = 16;
    public static final int PROC_ID_STATVFS = 17;
    public static final int PROC_ID_SYMLINK = 18;
    public static final int PROC_ID_UNLINK = 19;
    public static final int PROC_ID_ACCESS = 20;
    public static final int PROC_ID_XTREEMFS_CHECKPOINT = 30;
    public static final int PROC_ID_XTREEMFS_CHECK_FILE_EXISTS = 31;
    public static final int PROC_ID_XTREEMFS_CLEAR_VOUCHERS = 52;
    public static final int PROC_ID_XTREEMFS_DUMP_DATABASE = 32;
    public static final int PROC_ID_XTREEMFS_GET_SUITABLE_OSDS = 33;
    public static final int PROC_ID_XTREEMFS_INTERNAL_DEBUG = 34;
    public static final int PROC_ID_XTREEMFS_LISTDIR = 35;
    public static final int PROC_ID_XTREEMFS_LSVOL = 36;
    public static final int PROC_ID_XTREEMFS_MKVOL = 47;
    public static final int PROC_ID_XTREEMFS_RENEW_CAPABILITY = 37;
    public static final int PROC_ID_XTREEMFS_RENEW_CAPABILITY_AND_VOUCHER = 53;
    public static final int PROC_ID_XTREEMFS_REPLICATION_TO_MASTER = 38;
    public static final int PROC_ID_XTREEMFS_REPLICA_ADD = 39;
    public static final int PROC_ID_XTREEMFS_REPLICA_LIST = 40;
    public static final int PROC_ID_XTREEMFS_REPLICA_REMOVE = 41;
    public static final int PROC_ID_XTREEMFS_RESTORE_DATABASE = 42;
    public static final int PROC_ID_XTREEMFS_RESTORE_FILE = 43;
    public static final int PROC_ID_XTREEMFS_RMVOL = 44;
    public static final int PROC_ID_XTREEMFS_SHUTDOWN = 45;
    public static final int PROC_ID_XTREEMFS_UPDATE_FILE_SIZE = 46;
    public static final int PROC_ID_XTREEMFS_SET_REPLICA_UPDATE_POLICY = 48;
    public static final int PROC_ID_XTREEMFS_SET_READ_ONLY_XATTR = 49;
    public static final int PROC_ID_XTREEMFS_GET_FILE_CREDENTIALS = 50;
    public static final int PROC_ID_XTREEMFS_GET_XLOCSET = 51;

    public static Message getRequestMessage(int procId) {
        switch (procId) {
           case 2: return MRC.fsetattrRequest.getDefaultInstance();
           case 3: return GlobalTypes.XCap.getDefaultInstance();
           case 4: return MRC.getattrRequest.getDefaultInstance();
           case 5: return MRC.getxattrRequest.getDefaultInstance();
           case 6: return MRC.linkRequest.getDefaultInstance();
           case 7: return MRC.listxattrRequest.getDefaultInstance();
           case 8: return MRC.mkdirRequest.getDefaultInstance();
           case 9: return MRC.openRequest.getDefaultInstance();
           case 10: return MRC.readdirRequest.getDefaultInstance();
           case 11: return MRC.readlinkRequest.getDefaultInstance();
           case 12: return MRC.removexattrRequest.getDefaultInstance();
           case 13: return MRC.renameRequest.getDefaultInstance();
           case 14: return MRC.rmdirRequest.getDefaultInstance();
           case 15: return MRC.setattrRequest.getDefaultInstance();
           case 16: return MRC.setxattrRequest.getDefaultInstance();
           case 17: return MRC.statvfsRequest.getDefaultInstance();
           case 18: return MRC.symlinkRequest.getDefaultInstance();
           case 19: return MRC.unlinkRequest.getDefaultInstance();
           case 20: return MRC.accessRequest.getDefaultInstance();
           case 30: return null;
           case 31: return MRC.xtreemfs_check_file_existsRequest.getDefaultInstance();
           case 52: return MRC.xtreemfs_clear_vouchersRequest.getDefaultInstance();
           case 32: return MRC.xtreemfs_dump_restore_databaseRequest.getDefaultInstance();
           case 33: return MRC.xtreemfs_get_suitable_osdsRequest.getDefaultInstance();
           case 34: return MRC.stringMessage.getDefaultInstance();
           case 35: return MRC.xtreemfs_listdirRequest.getDefaultInstance();
           case 36: return null;
           case 47: return MRC.Volume.getDefaultInstance();
           case 37: return GlobalTypes.XCap.getDefaultInstance();
           case 53: return MRC.xtreemfs_renew_capabilityRequest.getDefaultInstance();
           case 38: return null;
           case 39: return MRC.xtreemfs_replica_addRequest.getDefaultInstance();
           case 40: return MRC.xtreemfs_replica_listRequest.getDefaultInstance();
           case 41: return MRC.xtreemfs_replica_removeRequest.getDefaultInstance();
           case 42: return MRC.xtreemfs_dump_restore_databaseRequest.getDefaultInstance();
           case 43: return MRC.xtreemfs_restore_fileRequest.getDefaultInstance();
           case 44: return MRC.xtreemfs_rmvolRequest.getDefaultInstance();
           case 45: return null;
           case 46: return MRC.xtreemfs_update_file_sizeRequest.getDefaultInstance();
           case 48: return MRC.xtreemfs_set_replica_update_policyRequest.getDefaultInstance();
           case 49: return MRC.xtreemfs_set_read_only_xattrRequest.getDefaultInstance();
           case 50: return MRC.xtreemfs_get_file_credentialsRequest.getDefaultInstance();
           case 51: return MRC.xtreemfs_get_xlocsetRequest.getDefaultInstance();
           default: throw new RuntimeException("unknown procedure id");
        }
    }


    public static Message getResponseMessage(int procId) {
        switch (procId) {
           case 2: return null;
           case 3: return GlobalTypes.XCap.getDefaultInstance();
           case 4: return MRC.getattrResponse.getDefaultInstance();
           case 5: return MRC.getxattrResponse.getDefaultInstance();
           case 6: return MRC.timestampResponse.getDefaultInstance();
           case 7: return MRC.listxattrResponse.getDefaultInstance();
           case 8: return MRC.timestampResponse.getDefaultInstance();
           case 9: return MRC.openResponse.getDefaultInstance();
           case 10: return MRC.DirectoryEntries.getDefaultInstance();
           case 11: return MRC.readlinkResponse.getDefaultInstance();
           case 12: return MRC.timestampResponse.getDefaultInstance();
           case 13: return MRC.renameResponse.getDefaultInstance();
           case 14: return MRC.timestampResponse.getDefaultInstance();
           case 15: return MRC.timestampResponse.getDefaultInstance();
           case 16: return MRC.timestampResponse.getDefaultInstance();
           case 17: return MRC.StatVFS.getDefaultInstance();
           case 18: return MRC.timestampResponse.getDefaultInstance();
           case 19: return MRC.unlinkResponse.getDefaultInstance();
           case 20: return null;
           case 30: return null;
           case 31: return MRC.xtreemfs_check_file_existsResponse.getDefaultInstance();
           case 52: return null;
           case 32: return null;
           case 33: return MRC.xtreemfs_get_suitable_osdsResponse.getDefaultInstance();
           case 34: return MRC.stringMessage.getDefaultInstance();
           case 35: return MRC.xtreemfs_listdirResponse.getDefaultInstance();
           case 36: return MRC.Volumes.getDefaultInstance();
           case 47: return null;
           case 37: return GlobalTypes.XCap.getDefaultInstance();
           case 53: return GlobalTypes.XCap.getDefaultInstance();
           case 38: return null;
           case 39: return null;
           case 40: return GlobalTypes.Replicas.getDefaultInstance();
           case 41: return GlobalTypes.FileCredentials.getDefaultInstance();
           case 42: return null;
           case 43: return null;
           case 44: return null;
           case 45: return null;
           case 46: return MRC.timestampResponse.getDefaultInstance();
           case 48: return MRC.xtreemfs_set_replica_update_policyResponse.getDefaultInstance();
           case 49: return MRC.xtreemfs_set_read_only_xattrResponse.getDefaultInstance();
           case 50: return GlobalTypes.FileCredentials.getDefaultInstance();
           case 51: return GlobalTypes.XLocSet.getDefaultInstance();
           default: throw new RuntimeException("unknown procedure id");
        }
    }


}