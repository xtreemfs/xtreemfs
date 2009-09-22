// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_OSD_PROXY_RESPONSE_H_
#define _XTREEMFS_OSD_PROXY_RESPONSE_H_

#include "yield.h"


namespace xtreemfs
{
  class OSDProxyResponse : public YIELD::concurrency::Response
  {
  public:
    ssize_t get_selected_file_replica() const { return selected_file_replica; }
    void set_selected_file_replica( ssize_t selected_file_replica ) { this->selected_file_replica = selected_file_replica; }

  protected:
    OSDProxyResponse()
      : selected_file_replica( 0 )
    { }

  private:
    ssize_t selected_file_replica;
  };
};

#define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS ::xtreemfs::OSDProxyResponse

#endif
