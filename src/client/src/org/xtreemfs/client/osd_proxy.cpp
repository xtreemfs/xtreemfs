// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/osd_proxy.h"
using namespace org::xtreemfs::client;


OSDProxy::OSDProxy( const YIELD::URI& absolute_uri, YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue, YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SSLContext> ssl_context )
  : Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>( absolute_uri, fd_event_queue, log, operation_timeout, peer_sockaddr, reconnect_tries_max, ssl_context )
{ }

YIELD::auto_Object<YIELD::ONCRPCRequest> OSDProxy::createProtocolRequest( YIELD::auto_Object<YIELD::Request> body )
{
  return YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::createProtocolRequest( body ); // No credentials
}
