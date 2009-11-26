// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_MRC_PROXY_H_
#define _XTREEMFS_MRC_PROXY_H_

#include "xtreemfs/proxy.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "xtreemfs/interfaces/mrc_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


namespace xtreemfs
{
  class Path;


  class MRCProxy : public Proxy<MRCProxy, org::xtreemfs::interfaces::MRCInterface>
  {
  public:
    static yidl::runtime::auto_Object<MRCProxy> 
      create
      ( 
        const YIELD::ipc::URI& absolute_uri,
        uint32_t flags = 0,
        YIELD::platform::auto_Log log = NULL,
        const YIELD::platform::Time& operation_timeout = YIELD::ipc::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>::OPERATION_TIMEOUT_DEFAULT,
        const char* password = "",
        uint8_t reconnect_tries_max = YIELD::ipc::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>::RECONNECT_TRIES_MAX_DEFAULT,
        YIELD::ipc::auto_SSLContext ssl_context = NULL 
      );

    // org::xtreemfs::interfaces::MRCInterface
    void chown( const Path& path, int uid, int gid );
    void getattr( const Path& path, org::xtreemfs::interfaces::Stat& stbuf );

    void readdir
    ( 
      const Path& path, 
      org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries 
    );

    // Proxy
    virtual void getCurrentUserCredentials
      ( org::xtreemfs::interfaces::UserCredentials& out_user_credentials );

  private:
    friend class YIELD::ipc::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>;

    MRCProxy
    ( 
      uint32_t flags, 
      YIELD::platform::auto_Log log, 
      const YIELD::platform::Time& operation_timeout, 
      const std::string& password, 
      YIELD::ipc::auto_SocketAddress peername, 
      uint8_t reconnect_tries_max,
      YIELD::ipc::auto_SocketFactory socket_factory 
    )
      : Proxy<MRCProxy, org::xtreemfs::interfaces::MRCInterface>
        ( 
          flags, log, operation_timeout, peername, 
          reconnect_tries_max, socket_factory 
        ), 
        password( password )
    { }

    ~MRCProxy() { }

    std::string password;
  };

  typedef yidl::runtime::auto_Object<MRCProxy> auto_MRCProxy;
};

#endif
