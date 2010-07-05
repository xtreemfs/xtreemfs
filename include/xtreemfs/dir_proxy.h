// Copyright (c) 2010 NEC HPC Europe
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
// DISCLAIMED. IN NO EVENT SHALL NEC HPC Europe BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#ifndef _XTREEMFS_DIR_PROXY_H_
#define _XTREEMFS_DIR_PROXY_H_

#include "xtreemfs/proxy.h"
#include "xtreemfs/interfaces/dir_interface.h"


namespace xtreemfs
{
  class Options;
  using org::xtreemfs::interfaces::AddressMappingSet;


  class DIRProxy 
    : public org::xtreemfs::interfaces::DIRInterfaceProxy,
      public Proxy
  {
  public:
    DIRProxy( EventHandler& request_handler ); // Steals this reference
    virtual ~DIRProxy();

    static DIRProxy& create( const Options& options );

    static DIRProxy&
    create
    (
      const URI& absolute_uri,
      Log* error_log = NULL,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
      SSLContext* ssl_context = NULL,
#endif
      Log* trace_log = NULL
    );

    AddressMappingSet& getAddressMappingsFromUUID( const string& uuid );
    URI getVolumeURIFromVolumeName( const string& volume_name_utf8 );

    // yidl::runtime::Object
    DIRProxy& inc_ref() { return Object::inc_ref( *this ); }

  private:
    class CachedAddressMappings;
    map<string, CachedAddressMappings*> uuid_to_address_mappings_cache;
    yield::platform::Mutex uuid_to_address_mappings_cache_lock;
  };
};

#endif
