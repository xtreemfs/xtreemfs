// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/osd_proxy.h"
using namespace xtreemfs;


yidl::auto_Object<OSDProxy> OSDProxy::create( const YIELD::URI& absolute_uri,
                                               const std::string& uuid,
                                               uint32_t flags,
                                               YIELD::auto_Log log,
                                               const YIELD::Time& operation_timeout,
                                               const YIELD::Time& ping_interval,
                                               YIELD::auto_SSLContext ssl_context )
{
  yidl::auto_Object<OSDProxy> osd_proxy = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::create<OSDProxy>( absolute_uri, flags, log, operation_timeout, ssl_context );
  osd_proxy->set_ping_interval( ping_interval );
  osd_proxy->uuid = uuid;
  return osd_proxy;
}

namespace xtreemfs
{
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
};