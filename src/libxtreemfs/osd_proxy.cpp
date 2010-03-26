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
// OSDECT, INOSDECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "xtreemfs/osd_proxy.h"
using namespace xtreemfs;


OSDProxy::OSDProxy( EventHandler& request_handler )
  : OSDInterfaceProxy( request_handler )
{ }

OSDProxy&
OSDProxy::create
(
  const URI& absolute_uri,
  Configuration* configuration,
  Log* error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext* ssl_context,
#endif
  Log* trace_log
)
{
  return *new OSDProxy
              (
                createONCRPCClient
                (
                  absolute_uri,
                  *new org::xtreemfs::interfaces::OSDInterfaceMessageFactory,
                  ONC_RPC_PORT_DEFAULT,
                  0x20000000 + TAG,
                  TAG,
                  configuration,
                  error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
                  ssl_context,
#endif
                  trace_log
                )
              );
}

bool
operator>
(
  const org::xtreemfs::interfaces::OSDWriteResponse& left,
  const org::xtreemfs::interfaces::OSDWriteResponse& right
)
{
  if ( left.get_new_file_size().empty() )
    return false;

  else if ( right.get_new_file_size().empty() )
    return true;

  else if
  (
    left.get_new_file_size()[0].get_truncate_epoch() >
    right.get_new_file_size()[0].get_truncate_epoch()
  )
    return true;

  else if
  (
    left.get_new_file_size()[0].get_truncate_epoch() ==
      right.get_new_file_size()[0].get_truncate_epoch()
    &&
    left.get_new_file_size()[0].get_size_in_bytes() >
      right.get_new_file_size()[0].get_size_in_bytes()
  )
    return true;

  else
    return false;
}
