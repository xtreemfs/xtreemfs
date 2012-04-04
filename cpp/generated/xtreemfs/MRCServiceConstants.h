//automatically generated from MRC.proto at Wed Apr 04 18:21:52 CEST 2012
//(c) 2012. See LICENSE file for details.

#ifndef MRCSERVICECONSTANTS_H_
#define MRCSERVICECONSTANTS_H_
#include <boost/cstdint.hpp>

namespace xtreemfs {
namespace pbrpc {

const boost::uint32_t INTERFACE_ID_MRC = 20001;
const boost::uint32_t PROC_ID_FSETATTR = 2;
const boost::uint32_t PROC_ID_FTRUNCATE = 3;
const boost::uint32_t PROC_ID_GETATTR = 4;
const boost::uint32_t PROC_ID_GETXATTR = 5;
const boost::uint32_t PROC_ID_LINK = 6;
const boost::uint32_t PROC_ID_LISTXATTR = 7;
const boost::uint32_t PROC_ID_MKDIR = 8;
const boost::uint32_t PROC_ID_OPEN = 9;
const boost::uint32_t PROC_ID_READDIR = 10;
const boost::uint32_t PROC_ID_READLINK = 11;
const boost::uint32_t PROC_ID_REMOVEXATTR = 12;
const boost::uint32_t PROC_ID_RENAME = 13;
const boost::uint32_t PROC_ID_RMDIR = 14;
const boost::uint32_t PROC_ID_SETATTR = 15;
const boost::uint32_t PROC_ID_SETXATTR = 16;
const boost::uint32_t PROC_ID_STATVFS = 17;
const boost::uint32_t PROC_ID_SYMLINK = 18;
const boost::uint32_t PROC_ID_UNLINK = 19;
const boost::uint32_t PROC_ID_ACCESS = 20;
const boost::uint32_t PROC_ID_XTREEMFS_CHECKPOINT = 30;
const boost::uint32_t PROC_ID_XTREEMFS_CHECK_FILE_EXISTS = 31;
const boost::uint32_t PROC_ID_XTREEMFS_DUMP_DATABASE = 32;
const boost::uint32_t PROC_ID_XTREEMFS_GET_SUITABLE_OSDS = 33;
const boost::uint32_t PROC_ID_XTREEMFS_INTERNAL_DEBUG = 34;
const boost::uint32_t PROC_ID_XTREEMFS_LISTDIR = 35;
const boost::uint32_t PROC_ID_XTREEMFS_LSVOL = 36;
const boost::uint32_t PROC_ID_XTREEMFS_MKVOL = 47;
const boost::uint32_t PROC_ID_XTREEMFS_RENEW_CAPABILITY = 37;
const boost::uint32_t PROC_ID_XTREEMFS_REPLICATION_TO_MASTER = 38;
const boost::uint32_t PROC_ID_XTREEMFS_REPLICA_ADD = 39;
const boost::uint32_t PROC_ID_XTREEMFS_REPLICA_LIST = 40;
const boost::uint32_t PROC_ID_XTREEMFS_REPLICA_REMOVE = 41;
const boost::uint32_t PROC_ID_XTREEMFS_RESTORE_DATABASE = 42;
const boost::uint32_t PROC_ID_XTREEMFS_RESTORE_FILE = 43;
const boost::uint32_t PROC_ID_XTREEMFS_RMVOL = 44;
const boost::uint32_t PROC_ID_XTREEMFS_SHUTDOWN = 45;
const boost::uint32_t PROC_ID_XTREEMFS_UPDATE_FILE_SIZE = 46;
const boost::uint32_t PROC_ID_XTREEMFS_SET_REPLICA_UPDATE_POLICY = 48;
const boost::uint32_t PROC_ID_XTREEMFS_SET_READ_ONLY_XATTR = 49;
const boost::uint32_t PROC_ID_XTREEMFS_GET_FILE_CREDENTIALS = 50;

}  // namespace pbrpc
}  // namespace xtreemfs

#endif // MRCSERVICECLIENT_H_
