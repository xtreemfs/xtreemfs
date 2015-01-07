//automatically generated from MRC.proto at Thu Dec 11 16:09:40 CET 2014
//(c) 2014. See LICENSE file for details.

#ifndef MRCSERVICECONSTANTS_H_
#define MRCSERVICECONSTANTS_H_
#include <stdint.h>

namespace xtreemfs {
namespace pbrpc {

const uint32_t INTERFACE_ID_MRC = 20001;
const uint32_t PROC_ID_FSETATTR = 2;
const uint32_t PROC_ID_FTRUNCATE = 3;
const uint32_t PROC_ID_GETATTR = 4;
const uint32_t PROC_ID_GETXATTR = 5;
const uint32_t PROC_ID_LINK = 6;
const uint32_t PROC_ID_LISTXATTR = 7;
const uint32_t PROC_ID_MKDIR = 8;
const uint32_t PROC_ID_OPEN = 9;
const uint32_t PROC_ID_READDIR = 10;
const uint32_t PROC_ID_READLINK = 11;
const uint32_t PROC_ID_REMOVEXATTR = 12;
const uint32_t PROC_ID_RENAME = 13;
const uint32_t PROC_ID_RMDIR = 14;
const uint32_t PROC_ID_SETATTR = 15;
const uint32_t PROC_ID_SETXATTR = 16;
const uint32_t PROC_ID_STATVFS = 17;
const uint32_t PROC_ID_SYMLINK = 18;
const uint32_t PROC_ID_UNLINK = 19;
const uint32_t PROC_ID_ACCESS = 20;
const uint32_t PROC_ID_XTREEMFS_CHECKPOINT = 30;
const uint32_t PROC_ID_XTREEMFS_CHECK_FILE_EXISTS = 31;
const uint32_t PROC_ID_XTREEMFS_DUMP_DATABASE = 32;
const uint32_t PROC_ID_XTREEMFS_GET_SUITABLE_OSDS = 33;
const uint32_t PROC_ID_XTREEMFS_INTERNAL_DEBUG = 34;
const uint32_t PROC_ID_XTREEMFS_LISTDIR = 35;
const uint32_t PROC_ID_XTREEMFS_LSVOL = 36;
const uint32_t PROC_ID_XTREEMFS_MKVOL = 47;
const uint32_t PROC_ID_XTREEMFS_RENEW_CAPABILITY = 37;
const uint32_t PROC_ID_XTREEMFS_REPLICATION_TO_MASTER = 38;
const uint32_t PROC_ID_XTREEMFS_REPLICA_ADD = 39;
const uint32_t PROC_ID_XTREEMFS_REPLICA_LIST = 40;
const uint32_t PROC_ID_XTREEMFS_REPLICA_REMOVE = 41;
const uint32_t PROC_ID_XTREEMFS_RESTORE_DATABASE = 42;
const uint32_t PROC_ID_XTREEMFS_RESTORE_FILE = 43;
const uint32_t PROC_ID_XTREEMFS_RMVOL = 44;
const uint32_t PROC_ID_XTREEMFS_SHUTDOWN = 45;
const uint32_t PROC_ID_XTREEMFS_UPDATE_FILE_SIZE = 46;
const uint32_t PROC_ID_XTREEMFS_SET_REPLICA_UPDATE_POLICY = 48;
const uint32_t PROC_ID_XTREEMFS_SET_READ_ONLY_XATTR = 49;
const uint32_t PROC_ID_XTREEMFS_GET_FILE_CREDENTIALS = 50;
const uint32_t PROC_ID_XTREEMFS_GET_XLOCSET = 51;

}  // namespace pbrpc
}  // namespace xtreemfs

#endif // MRCSERVICECLIENT_H_
