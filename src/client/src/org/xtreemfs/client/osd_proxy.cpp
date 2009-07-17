#include "org/xtreemfs/client/osd_proxy.h"
using namespace org::xtreemfs::client;


YIELD::auto_Object<OSDProxy> OSDProxy::create( const YIELD::URI& absolute_uri,
                                               YIELD::auto_Object<YIELD::StageGroup> stage_group,
                                               const std::string& uuid,
                                               uint32_t flags,
                                               YIELD::auto_Log log,
                                               const YIELD::Time& operation_timeout,
                                               const YIELD::Time& ping_interval,
                                               YIELD::auto_SSLContext ssl_context )
{
  YIELD::auto_Object<OSDProxy> osd_proxy = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::create<OSDProxy>( absolute_uri, stage_group, flags, log, operation_timeout, ssl_context );
  osd_proxy->set_ping_interval( ping_interval );
  osd_proxy->uuid = uuid;
  return osd_proxy;
}

bool operator>( const org::xtreemfs::interfaces::OSDWriteResponse& left, const org::xtreemfs::interfaces::OSDWriteResponse& right )
{
  if ( left.get_new_file_size().empty() )
    return false;
  else if ( right.get_new_file_size().empty() )
    return true;
  else if ( left.get_new_file_size()[0].get_truncate_epoch() > right.get_new_file_size()[0].get_truncate_epoch() )
    return true;
  else if ( left.get_new_file_size()[0].get_truncate_epoch() == right.get_new_file_size()[0].get_truncate_epoch() &&
            left.get_new_file_size()[0].get_size_in_bytes() > right.get_new_file_size()[0].get_size_in_bytes() )
    return true;
  else
    return false;
}
