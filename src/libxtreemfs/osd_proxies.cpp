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


#include "xtreemfs/osd_proxies.h"
#include "xtreemfs/dir_proxy.h"
using org::xtreemfs::interfaces::AddressMappingSet;
using org::xtreemfs::interfaces::ONCRPC_SCHEME;
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
using org::xtreemfs::interfaces::ONCRPCG_SCHEME;
using org::xtreemfs::interfaces::ONCRPCS_SCHEME;
#endif
using org::xtreemfs::interfaces::Replica;
using org::xtreemfs::interfaces::ReplicaSet;
using org::xtreemfs::interfaces::StripingPolicy;
using org::xtreemfs::interfaces::STRIPING_POLICY_RAID0;
using namespace xtreemfs;


OSDProxies::OSDProxies
(
  DIRProxy& dir_proxy,
  Log* error_log,  
  OSDProxy::Configuration* osd_proxy_configuration,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext* osd_proxy_ssl_context,
#endif
  StageGroup* osd_proxy_stage_group,
  Log* trace_log
)
  : dir_proxy( dir_proxy.inc_ref() ),
    error_log( Object::inc_ref( error_log ) ),
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    osd_proxy_ssl_context( Object::inc_ref( osd_proxy_ssl_context ) ),
#endif
    osd_proxy_stage_group( Object::inc_ref( osd_proxy_stage_group ) ),
    trace_log( Object::inc_ref( trace_log ) )
{
  if ( osd_proxy_configuration != NULL )
    this->osd_proxy_configuration = osd_proxy_configuration;
  else
    this->osd_proxy_configuration = new OSDProxy::Configuration;
}

OSDProxies::~OSDProxies()
{
  for ( iterator osd_proxy_i = begin(); osd_proxy_i != end(); ++osd_proxy_i )
    OSDProxy::dec_ref( *osd_proxy_i->second );

  DIRProxy::dec_ref( dir_proxy );
  Log::dec_ref( error_log );
  OSDProxy::Configuration::dec_ref( *osd_proxy_configuration );
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext::dec_ref( osd_proxy_ssl_context );
#endif
  StageGroup::dec_ref( osd_proxy_stage_group );
  Log::dec_ref( trace_log );
}

OSDProxy&
OSDProxies::get_osd_proxy
(
  uint64_t object_number,
  size_t& selected_file_replica_i,
  const XLocSet& xlocs
)
{
  const ReplicaSet& file_replicas = xlocs.get_replicas();
  const Replica* selected_file_replica;

  if ( selected_file_replica_i > 0 )
  {
    // Already selected a replica for the file
    selected_file_replica = &file_replicas[selected_file_replica_i - 1];
  }
  else if ( file_replicas.size() == 1 )
  {
    // Only one replica available
    selected_file_replica = &file_replicas[0];
    selected_file_replica_i = 1;
  }
  else
  {
    selected_file_replica = NULL;

    for
    (
      ReplicaSet::size_type file_replica_i = 0;
      file_replica_i < file_replicas.size();
      file_replica_i++
    )
    {
      if
      (
        selected_file_replica_i == 0 ||
        static_cast<ssize_t>( file_replica_i + 1 ) !=
          selected_file_replica_i * -1
      )
      {
        // No replica has been selected yet ||
        // A replica was selected and it failed, but this is not it
        selected_file_replica = &file_replicas[file_replica_i];
        selected_file_replica_i = file_replica_i + 1;
        break;
      }
    }

    if ( selected_file_replica == NULL )
    {
      selected_file_replica = &file_replicas[0];
      selected_file_replica_i = 1;
    }
  }

  const StripingPolicy& striping_policy
    = selected_file_replica->get_striping_policy();

  switch ( striping_policy.get_type() )
  {
    case STRIPING_POLICY_RAID0:
    {
      size_t osd_i = object_number % striping_policy.get_width();
      const string& osd_uuid = selected_file_replica->get_osd_uuids()[osd_i];
      return get_osd_proxy( osd_uuid );
    }
    break;

    default:
    {
      DebugBreak(); 
      throw Exception();
    }
    break;
  }
}

OSDProxy& OSDProxies::get_osd_proxy( const string& osd_uuid )
{
  OSDProxy* osd_proxy = NULL;

  iterator osd_proxy_i = find( osd_uuid );
  if ( osd_proxy_i != end() )
    osd_proxy = &osd_proxy_i->second->inc_ref();
  else
  {
    yidl::runtime::auto_Object<AddressMappingSet>
      address_mappings = dir_proxy.getAddressMappingsFromUUID( osd_uuid );

    for
    (
      AddressMappingSet::iterator
        address_mapping_i = address_mappings->begin();
      address_mapping_i != address_mappings->end();
      address_mapping_i++
    )
    {
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
      if
      ( 
        osd_proxy_ssl_context != NULL 
        &&
        (
          ( *address_mapping_i ).get_protocol() == ONCRPCS_SCHEME 
          ||
          ( *address_mapping_i ).get_protocol() == ONCRPCG_SCHEME
        )
      )
      {
        osd_proxy
          = &OSDProxy::create
            (
              ( *address_mapping_i ).get_uri(),
              &osd_proxy_configuration->inc_ref(),
              error_log,
              osd_proxy_ssl_context,
              trace_log
            );

        //if ( osd_proxy_stage_group != NULL )
        //  osd_proxy_stage_group->createStage( osd_proxy->inc_ref() );
      }
      else
#endif
      if ( ( *address_mapping_i ).get_protocol() == ONCRPC_SCHEME )
      {
        osd_proxy
          = &OSDProxy::create
            (
              ( *address_mapping_i ).get_uri(),
              &osd_proxy_configuration->inc_ref(),
              error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
              osd_proxy_ssl_context,
#endif
              trace_log
            );

        //if ( osd_proxy_stage_group != NULL )
        //  osd_proxy_stage_group->createStage( osd_proxy->inc_ref() );
      }
    }

    if ( osd_proxy != NULL )
      insert( make_pair( osd_uuid, &osd_proxy->inc_ref() ) );
    else
      throw Exception( "no acceptable ONC-RPC URI for UUID" );
  }

  return *osd_proxy;
}
