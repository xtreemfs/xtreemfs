// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_CLIENT_OSD_PROXY_RESPONSE_H_
#define _ORG_XTREEMFS_CLIENT_OSD_PROXY_RESPONSE_H_

#include "yield.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OSDProxyResponse : public YIELD::Response
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
  };
};

#define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS org::xtreemfs::client::OSDProxyResponse

#endif
