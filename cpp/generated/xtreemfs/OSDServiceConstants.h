//automatically generated from OSD.proto at Wed Apr 04 18:21:52 CEST 2012
//(c) 2012. See LICENSE file for details.

#ifndef OSDSERVICECONSTANTS_H_
#define OSDSERVICECONSTANTS_H_
#include <boost/cstdint.hpp>

namespace xtreemfs {
namespace pbrpc {

const boost::uint32_t INTERFACE_ID_OSD = 30001;
const boost::uint32_t PROC_ID_READ = 10;
const boost::uint32_t PROC_ID_TRUNCATE = 11;
const boost::uint32_t PROC_ID_UNLINK = 12;
const boost::uint32_t PROC_ID_WRITE = 13;
const boost::uint32_t PROC_ID_XTREEMFS_BROADCAST_GMAX = 20;
const boost::uint32_t PROC_ID_XTREEMFS_CHECK_OBJECT = 21;
const boost::uint32_t PROC_ID_XTREEMFS_CLEANUP_GET_RESULTS = 30;
const boost::uint32_t PROC_ID_XTREEMFS_CLEANUP_IS_RUNNING = 31;
const boost::uint32_t PROC_ID_XTREEMFS_CLEANUP_START = 32;
const boost::uint32_t PROC_ID_XTREEMFS_CLEANUP_STATUS = 33;
const boost::uint32_t PROC_ID_XTREEMFS_CLEANUP_STOP = 34;
const boost::uint32_t PROC_ID_XTREEMFS_CLEANUP_VERSIONS_START = 35;
const boost::uint32_t PROC_ID_XTREEMFS_RWR_FETCH = 73;
const boost::uint32_t PROC_ID_XTREEMFS_RWR_FLEASE_MSG = 71;
const boost::uint32_t PROC_ID_XTREEMFS_RWR_NOTIFY = 75;
const boost::uint32_t PROC_ID_XTREEMFS_RWR_SET_PRIMARY_EPOCH = 78;
const boost::uint32_t PROC_ID_XTREEMFS_RWR_STATUS = 76;
const boost::uint32_t PROC_ID_XTREEMFS_RWR_TRUNCATE = 74;
const boost::uint32_t PROC_ID_XTREEMFS_RWR_UPDATE = 72;
const boost::uint32_t PROC_ID_XTREEMFS_RWR_AUTH_STATE = 79;
const boost::uint32_t PROC_ID_XTREEMFS_RWR_RESET_COMPLETE = 80;
const boost::uint32_t PROC_ID_XTREEMFS_INTERNAL_GET_GMAX = 40;
const boost::uint32_t PROC_ID_XTREEMFS_INTERNAL_TRUNCATE = 41;
const boost::uint32_t PROC_ID_XTREEMFS_INTERNAL_GET_FILE_SIZE = 42;
const boost::uint32_t PROC_ID_XTREEMFS_INTERNAL_READ_LOCAL = 43;
const boost::uint32_t PROC_ID_XTREEMFS_INTERNAL_GET_OBJECT_SET = 44;
const boost::uint32_t PROC_ID_XTREEMFS_INTERNAL_GET_FILEID_LIST = 45;
const boost::uint32_t PROC_ID_XTREEMFS_LOCK_ACQUIRE = 50;
const boost::uint32_t PROC_ID_XTREEMFS_LOCK_CHECK = 51;
const boost::uint32_t PROC_ID_XTREEMFS_LOCK_RELEASE = 52;
const boost::uint32_t PROC_ID_XTREEMFS_PING = 60;
const boost::uint32_t PROC_ID_XTREEMFS_SHUTDOWN = 70;

}  // namespace pbrpc
}  // namespace xtreemfs

#endif // OSDSERVICECLIENT_H_
