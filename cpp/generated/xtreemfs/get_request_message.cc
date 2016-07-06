//automatically generated at Wed Jul 06 11:03:11 CEST 2016
//(c) 2016. See LICENSE file for details.

#include "xtreemfs/get_request_message.h"

#include "xtreemfs/OSD.pb.h"
#include "xtreemfs/MRC.pb.h"
#include "include/Common.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/DIR.pb.h"

namespace xtreemfs {
namespace pbrpc {

google::protobuf::Message* GetMessageForProcID(uint32_t interface_id,
                                               uint32_t proc_id) {
  switch (interface_id) {
// Generated from DIR.proto
    case 10001: {
      switch (proc_id) {
        case 1: {
          return new xtreemfs::pbrpc::addressMappingGetRequest();
          break;
        }
        case 2: {
          return new xtreemfs::pbrpc::addressMappingGetRequest();
          break;
        }
        case 3: {
          return new xtreemfs::pbrpc::AddressMappingSet();
          break;
        }
        case 4: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 5: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 6: {
          return new xtreemfs::pbrpc::serviceDeregisterRequest();
          break;
        }
        case 7: {
          return new xtreemfs::pbrpc::serviceGetByNameRequest();
          break;
        }
        case 8: {
          return new xtreemfs::pbrpc::serviceGetByTypeRequest();
          break;
        }
        case 9: {
          return new xtreemfs::pbrpc::serviceGetByUUIDRequest();
          break;
        }
        case 10: {
          return new xtreemfs::pbrpc::serviceGetByUUIDRequest();
          break;
        }
        case 11: {
          return new xtreemfs::pbrpc::serviceRegisterRequest();
          break;
        }
        case 20: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 21: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 22: {
          return new xtreemfs::pbrpc::configurationGetRequest();
          break;
        }
        case 23: {
          return new xtreemfs::pbrpc::Configuration();
          break;
        }
        case 24: {
          return new xtreemfs::pbrpc::VivaldiCoordinates();
          break;
        }
        default: {
          return NULL;
        }
      }
    break;
    }
// Generated from MRC.proto
    case 20001: {
      switch (proc_id) {
        case 2: {
          return new xtreemfs::pbrpc::fsetattrRequest();
          break;
        }
        case 3: {
          return new xtreemfs::pbrpc::XCap();
          break;
        }
        case 4: {
          return new xtreemfs::pbrpc::getattrRequest();
          break;
        }
        case 5: {
          return new xtreemfs::pbrpc::getxattrRequest();
          break;
        }
        case 6: {
          return new xtreemfs::pbrpc::linkRequest();
          break;
        }
        case 7: {
          return new xtreemfs::pbrpc::listxattrRequest();
          break;
        }
        case 8: {
          return new xtreemfs::pbrpc::mkdirRequest();
          break;
        }
        case 9: {
          return new xtreemfs::pbrpc::openRequest();
          break;
        }
        case 10: {
          return new xtreemfs::pbrpc::readdirRequest();
          break;
        }
        case 11: {
          return new xtreemfs::pbrpc::readlinkRequest();
          break;
        }
        case 12: {
          return new xtreemfs::pbrpc::removexattrRequest();
          break;
        }
        case 13: {
          return new xtreemfs::pbrpc::renameRequest();
          break;
        }
        case 14: {
          return new xtreemfs::pbrpc::rmdirRequest();
          break;
        }
        case 15: {
          return new xtreemfs::pbrpc::setattrRequest();
          break;
        }
        case 16: {
          return new xtreemfs::pbrpc::setxattrRequest();
          break;
        }
        case 17: {
          return new xtreemfs::pbrpc::statvfsRequest();
          break;
        }
        case 18: {
          return new xtreemfs::pbrpc::symlinkRequest();
          break;
        }
        case 19: {
          return new xtreemfs::pbrpc::unlinkRequest();
          break;
        }
        case 20: {
          return new xtreemfs::pbrpc::accessRequest();
          break;
        }
        case 30: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 31: {
          return new xtreemfs::pbrpc::xtreemfs_check_file_existsRequest();
          break;
        }
        case 52: {
          return new xtreemfs::pbrpc::xtreemfs_clear_vouchersRequest();
          break;
        }
        case 32: {
          return new xtreemfs::pbrpc::xtreemfs_dump_restore_databaseRequest();
          break;
        }
        case 33: {
          return new xtreemfs::pbrpc::xtreemfs_get_suitable_osdsRequest();
          break;
        }
        case 34: {
          return new xtreemfs::pbrpc::stringMessage();
          break;
        }
        case 35: {
          return new xtreemfs::pbrpc::xtreemfs_listdirRequest();
          break;
        }
        case 36: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 47: {
          return new xtreemfs::pbrpc::Volume();
          break;
        }
        case 37: {
          return new xtreemfs::pbrpc::XCap();
          break;
        }
        case 53: {
          return new xtreemfs::pbrpc::xtreemfs_renew_capabilityRequest();
          break;
        }
        case 38: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 39: {
          return new xtreemfs::pbrpc::xtreemfs_replica_addRequest();
          break;
        }
        case 40: {
          return new xtreemfs::pbrpc::xtreemfs_replica_listRequest();
          break;
        }
        case 41: {
          return new xtreemfs::pbrpc::xtreemfs_replica_removeRequest();
          break;
        }
        case 42: {
          return new xtreemfs::pbrpc::xtreemfs_dump_restore_databaseRequest();
          break;
        }
        case 43: {
          return new xtreemfs::pbrpc::xtreemfs_restore_fileRequest();
          break;
        }
        case 44: {
          return new xtreemfs::pbrpc::xtreemfs_rmvolRequest();
          break;
        }
        case 45: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 46: {
          return new xtreemfs::pbrpc::xtreemfs_update_file_sizeRequest();
          break;
        }
        case 48: {
          return new xtreemfs::pbrpc::xtreemfs_set_replica_update_policyRequest();
          break;
        }
        case 50: {
          return new xtreemfs::pbrpc::xtreemfs_get_file_credentialsRequest();
          break;
        }
        case 51: {
          return new xtreemfs::pbrpc::xtreemfs_get_xlocsetRequest();
          break;
        }
        case 54: {
          return new xtreemfs::pbrpc::xtreemfs_reselect_osdsRequest();
          break;
        }
        default: {
          return NULL;
        }
      }
    break;
    }
// Generated from OSD.proto
    case 30001: {
      switch (proc_id) {
        case 10: {
          return new xtreemfs::pbrpc::readRequest();
          break;
        }
        case 11: {
          return new xtreemfs::pbrpc::truncateRequest();
          break;
        }
        case 12: {
          return new xtreemfs::pbrpc::unlink_osd_Request();
          break;
        }
        case 13: {
          return new xtreemfs::pbrpc::writeRequest();
          break;
        }
        case 20: {
          return new xtreemfs::pbrpc::xtreemfs_broadcast_gmaxRequest();
          break;
        }
        case 21: {
          return new xtreemfs::pbrpc::xtreemfs_check_objectRequest();
          break;
        }
        case 30: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 31: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 32: {
          return new xtreemfs::pbrpc::xtreemfs_cleanup_startRequest();
          break;
        }
        case 33: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 34: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 35: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 22: {
          return new xtreemfs::pbrpc::xtreemfs_finalize_vouchersRequest();
          break;
        }
        case 36: {
          return new xtreemfs::pbrpc::xtreemfs_repair_objectRequest();
          break;
        }
        case 73: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_fetchRequest();
          break;
        }
        case 71: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_flease_msgRequest();
          break;
        }
        case 75: {
          return new xtreemfs::pbrpc::FileCredentials();
          break;
        }
        case 78: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_set_primary_epochRequest();
          break;
        }
        case 76: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_statusRequest();
          break;
        }
        case 74: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_truncateRequest();
          break;
        }
        case 72: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_updateRequest();
          break;
        }
        case 79: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_auth_stateRequest();
          break;
        }
        case 80: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_reset_completeRequest();
          break;
        }
        case 40: {
          return new xtreemfs::pbrpc::xtreemfs_internal_get_gmaxRequest();
          break;
        }
        case 41: {
          return new xtreemfs::pbrpc::truncateRequest();
          break;
        }
        case 42: {
          return new xtreemfs::pbrpc::xtreemfs_internal_get_file_sizeRequest();
          break;
        }
        case 43: {
          return new xtreemfs::pbrpc::xtreemfs_internal_read_localRequest();
          break;
        }
        case 44: {
          return new xtreemfs::pbrpc::xtreemfs_internal_get_object_setRequest();
          break;
        }
        case 45: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 50: {
          return new xtreemfs::pbrpc::lockRequest();
          break;
        }
        case 51: {
          return new xtreemfs::pbrpc::lockRequest();
          break;
        }
        case 52: {
          return new xtreemfs::pbrpc::lockRequest();
          break;
        }
        case 60: {
          return new xtreemfs::pbrpc::xtreemfs_pingMesssage();
          break;
        }
        case 70: {
          return new xtreemfs::pbrpc::emptyRequest();
          break;
        }
        case 81: {
          return new xtreemfs::pbrpc::xtreemfs_xloc_set_invalidateRequest();
          break;
        }
        case 82: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_auth_stateRequest();
          break;
        }
        case 83: {
          return new xtreemfs::pbrpc::xtreemfs_rwr_reset_statusRequest();
          break;
        }
        case 84: {
          return new xtreemfs::pbrpc::xtreemfs_ec_get_interval_vectorsRequest();
          break;
        }
        case 85: {
          return new xtreemfs::pbrpc::xtreemfs_ec_commit_vectorRequest();
          break;
        }
        case 86: {
          return new xtreemfs::pbrpc::xtreemfs_ec_write_dataRequest();
          break;
        }
        case 87: {
          return new xtreemfs::pbrpc::xtreemfs_ec_readRequest();
          break;
        }
        default: {
          return NULL;
        }
      }
    break;
    }
    default: {
      return NULL;
    }
  }
}

}  // namespace pbrpc
}  // namespace xtreemfs
