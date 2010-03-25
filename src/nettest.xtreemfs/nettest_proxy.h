// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#ifndef _NETTEST_XTREEMFS_NETTEST_PROXY_H_
#define _NETTEST_XTREEMFS_NETTEST_PROXY_H_

#include "xtreemfs/options.h"
#include "xtreemfs/proxy.h"
#include "nettest_interface.h"


namespace xtreemfs
{
  class NettestProxy
    : public Proxy
             <
               org::xtreemfs::interfaces::NettestInterface,
               org::xtreemfs::interfaces::NettestInterfaceMessageFactory,
               org::xtreemfs::interfaces::NettestInterfaceRequestSender
             >
  {
  public:
    virtual ~NettestProxy() { }

    static NettestProxy& create( const Options& options );

  private:
    NettestProxy
    (
      Configuration& configuration,
      Log* error_log,
      IOQueue& io_queue,      
      SocketAddress& peername,
      TCPSocketFactory& tcp_socket_factory,
      Log* trace_log
    );
  };

  typedef yidl::runtime::auto_Object<NettestProxy> auto_NettestProxy;
};

#endif
