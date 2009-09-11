// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/osd_proxy.h"
using namespace xtreemfs;


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
