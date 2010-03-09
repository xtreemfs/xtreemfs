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


#include "xtreemfs/osd_proxy_mux.h"
#include "xtreemfs/dir_proxy.h"
using org::xtreemfs::interfaces::AddressMappingSet;
using org::xtreemfs::interfaces::Replica;
using org::xtreemfs::interfaces::ReplicaSet;
using org::xtreemfs::interfaces::StripingPolicy;
using org::xtreemfs::interfaces::STRIPING_POLICY_RAID0;
using yield::concurrency::Event;
using yield::concurrency::EventTarget;
using yield::concurrency::ExceptionResponse;
using namespace xtreemfs;


class OSDProxyMux::ReadResponseTarget : public EventTarget
{
public:
  ReadResponseTarget( OSDProxyMux& osd_proxy_mux, readRequest& read_request )
    : original_response_target( read_request.get_response_target() ),
      osd_proxy_mux( osd_proxy_mux.inc_ref() ),
      read_request( read_request ) // Steals this reference
  { }

  // EventTarget
  void send( Event& ev )
  {
    switch ( ev.get_type_id() )
    {
      case readResponse::TYPE_ID:
      {
        static_cast<readResponse&>( ev ).set_selected_file_replica
        (
          read_request.get_selected_file_replica()
        );

        original_response_target->send( ev );
      }
      break;

      case ExceptionResponse::TYPE_ID:
      {
        if
        (
          read_request.get_file_credentials().get_xlocs().
            get_replicas().size() > 1
        )
        {
          // Set selected_replica to -1 * the index of
          // the failed replica and retry
          if ( read_request.get_selected_file_replica() <= 0 )
            DebugBreak();

          read_request.set_selected_file_replica
          (
            -1 * read_request.get_selected_file_replica()
          );

          osd_proxy_mux.send( read_request.inc_ref() );

          Event::dec_ref( ev );
        }
        else // There is only one replica, send the exception back
          original_response_target->send( ev );
      }
      break;

      default:
      {
        original_response_target->send( ev );
      }
      break;
    }
  }

private:
  EventTarget* original_response_target;
  OSDProxyMux& osd_proxy_mux;
  readRequest& read_request;
};


class OSDProxyMux::TruncateResponseTarget : public EventTarget
{
public:
  TruncateResponseTarget
  (
    size_t expected_response_count,
    EventTarget& final_response_target
  )
    : expected_response_count( expected_response_count ),
      final_response_target( final_response_target.inc_ref() )
  { }

  virtual ~TruncateResponseTarget()
  {
    for
    (
      vector<Event*>::iterator
        response_i = responses.begin();
      response_i != responses.end();
      response_i++
    )
      Event::dec_ref( **response_i );

    EventTarget::dec_ref( final_response_target );
  }

  // EventTarget
  void send( Event& ev )
  {
    responses_lock.acquire();

    responses.push_back( &ev );

    if ( responses.size() == expected_response_count )
    {
      for
      (
        vector<Event*>::iterator response_i = responses.begin();
        response_i != responses.end();
        response_i++
      )
      {
        if ( ( *response_i )->get_type_id() == ExceptionResponse::TYPE_ID )
        {
          final_response_target.send
          (             
            static_cast<ExceptionResponse&>( **response_i ).inc_ref()
          );

          responses_lock.release();

          return;
        }
      }

      final_response_target.send( responses[responses.size() - 1]->inc_ref() );
    }

    responses_lock.release();
  }

private:
  size_t expected_response_count;
  EventTarget& final_response_target;

  vector<Event*> responses;
  yield::platform::Mutex responses_lock;
};


OSDProxyMux&
OSDProxyMux::create
(
  DIRProxy& dir_proxy,
  uint16_t concurrency_level,
  uint32_t flags,
  Log* log,
  const Time& operation_timeout,
  uint8_t reconnect_tries_max,
  SSLContext* ssl_context,
  UserCredentialsCache* user_credentials_cache
)
{
  return *new OSDProxyMux
              (
                concurrency_level,
                dir_proxy,
                flags,
                log,
                operation_timeout,
                reconnect_tries_max,
                ssl_context,
                user_credentials_cache
              );
}

OSDProxyMux::OSDProxyMux
(
  uint16_t concurrency_level,
  DIRProxy& dir_proxy,
  uint32_t flags,
  Log* log,
  const Time& operation_timeout,
  uint8_t reconnect_tries_max,
  SSLContext* ssl_context,
  UserCredentialsCache* user_credentials_cache
)
  : concurrency_level( concurrency_level ),
    dir_proxy( dir_proxy.inc_ref() ),
    flags( flags ),
    log( Object::inc_ref( log ) ),
    operation_timeout( operation_timeout ),
    reconnect_tries_max( reconnect_tries_max ),
    ssl_context( Object::inc_ref( ssl_context ) ),
    user_credentials_cache( Object::inc_ref( user_credentials_cache ) ),
    osd_proxy_stage_group( *new yield::concurrency::SEDAStageGroup )
{ }

OSDProxyMux::~OSDProxyMux()
{
  for
  (
    OSDProxyMap::iterator osd_proxies_i = osd_proxies.begin();
    osd_proxies_i != osd_proxies.end();
    osd_proxies_i++
  )
    OSDProxy::dec_ref( *osd_proxies_i->second );

  DIRProxy::dec_ref( dir_proxy );
  Log::dec_ref( log );
  yield::concurrency::StageGroup::dec_ref( osd_proxy_stage_group );
  SSLContext::dec_ref( ssl_context );
  UserCredentialsCache::dec_ref( user_credentials_cache );
}

OSDProxy&
OSDProxyMux::get_osd_proxy
(
  OSDProxyRequest& osd_proxy_request,
  const FileCredentials& file_credentials,
  uint64_t object_number
)
{
  const ReplicaSet& file_replicas = file_credentials.get_xlocs().get_replicas();
  const Replica* selected_file_replica;

  if ( osd_proxy_request.get_selected_file_replica() > 0 )
  {
    // Already selected a replica for the file
    selected_file_replica =
      &file_replicas[osd_proxy_request.get_selected_file_replica() - 1];
  }
  else if ( file_replicas.size() == 1 )
  {
    // Only one replica available
    selected_file_replica = &file_replicas[0];
    osd_proxy_request.set_selected_file_replica( 1 );
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
        osd_proxy_request.get_selected_file_replica() == 0 ||
        static_cast<ssize_t>( file_replica_i + 1 ) !=
          osd_proxy_request.get_selected_file_replica() * -1
      )
      {
        // No replica has been selected yet ||
        // A replica was selected and it failed, but this is not it

        selected_file_replica = &file_replicas[file_replica_i];
        osd_proxy_request.set_selected_file_replica( file_replica_i + 1 );
        break;
      }
    }

    if ( selected_file_replica == NULL )
    {
      selected_file_replica = &file_replicas[0];
      osd_proxy_request.set_selected_file_replica( 1 );
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

    default: DebugBreak(); throw yield::platform::Exception(); break;
  }
}

OSDProxy& OSDProxyMux::get_osd_proxy( const string& osd_uuid )
{
  OSDProxy* osd_proxy = NULL;

  OSDProxyMap::iterator osd_proxies_i = osd_proxies.find( osd_uuid );
  if ( osd_proxies_i != osd_proxies.end() )
    osd_proxy = &osd_proxies_i->second->inc_ref();
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
#ifdef YIELD_IPC_HAVE_OPENSSL
      if ( ssl_context != NULL &&
           (
             ( *address_mapping_i ).get_protocol() == ONCRPCS_SCHEME ||
             ( *address_mapping_i ).get_protocol() == ONCRPCG_SCHEME
           )
         )
      {
        osd_proxy 
          = &OSDProxy::create
            (
              ( *address_mapping_i ).get_uri(),
              concurrency_level,
              flags,
              log,
              operation_timeout,
              reconnect_tries_max,
              ssl_context,
              user_credentials_cache
            );

        osd_proxy_stage_group.createStage( osd_proxy->inc_ref() );
      }
      else
#endif
      if ( ( *address_mapping_i ).get_protocol() == ONCRPC_SCHEME )
      {
        osd_proxy 
          = &OSDProxy::create
            (
              ( *address_mapping_i ).get_uri(),
              concurrency_level,
              flags,
              log,
              operation_timeout,
              reconnect_tries_max,
              ssl_context,
              user_credentials_cache
            );

        osd_proxy_stage_group.createStage( osd_proxy->inc_ref() );
      }
    }

    if ( osd_proxy != NULL )
      osd_proxies[osd_uuid] = &osd_proxy->inc_ref();
    else
      throw yield::platform::Exception( "no acceptable ONC-RPC URI for UUID" );
  }

  return *osd_proxy;
}

void OSDProxyMux::handlereadRequest( readRequest& req )
{
  OSDProxy& osd_proxy 
    = get_osd_proxy
      ( 
        req,
        req.get_file_credentials(),
        req.get_object_number()
      );

  req.set_response_target( new ReadResponseTarget( *this, req ) );

  static_cast<EventTarget&>( osd_proxy ).send( req );

  OSDProxy::dec_ref( osd_proxy );
}

void OSDProxyMux::handletruncateRequest( truncateRequest& req )
{
  const ReplicaSet& replicas
    = req.get_file_credentials().get_xlocs().get_replicas();

  if ( req.get_response_target() != NULL )
  {
    req.set_response_target
    (
      new TruncateResponseTarget( replicas.size(), *req.get_response_target() )
    );
  }

  for
  (
    ReplicaSet::const_iterator replica_i = replicas.begin();
    replica_i != replicas.end();
    replica_i++
  )
  {
    OSDProxy& osd_proxy = get_osd_proxy( ( *replica_i ).get_osd_uuids()[0] );
    osd_proxy.send( req.inc_ref() );
    OSDProxy::dec_ref( osd_proxy );
  }

  truncateRequest::dec_ref( req );
}

void OSDProxyMux::handleunlinkRequest( unlinkRequest& req )
{
  const ReplicaSet&
    replicas = req.get_file_credentials().get_xlocs().get_replicas();

  if ( req.get_response_target() != NULL )
  {
    req.set_response_target
    (
      new TruncateResponseTarget( replicas.size(), *req.get_response_target() )
    );
  }

  for
  (
    ReplicaSet::const_iterator replica_i = replicas.begin();
    replica_i != replicas.end();
    replica_i++
  )
  {
    OSDProxy& osd_proxy = get_osd_proxy( ( *replica_i ).get_osd_uuids()[0] );
    osd_proxy.send( req.inc_ref() );
    OSDProxy::dec_ref( osd_proxy );
  }

  unlinkRequest::dec_ref( req );
}

void OSDProxyMux::handlewriteRequest( writeRequest& req )
{
  OSDProxy& osd_proxy 
    = get_osd_proxy
      (
        req,
        req.get_file_credentials(),
        req.get_object_number()
      );

  osd_proxy.send( req );

  OSDProxy::dec_ref( osd_proxy );
}

void OSDProxyMux::handlextreemfs_lock_acquireRequest
(
  xtreemfs_lock_acquireRequest& req
)
{
  OSDProxy& osd_proxy 
    = get_osd_proxy
      (
        req,
        req.get_file_credentials(),
        0
      );

  osd_proxy.send( req );

  OSDProxy::dec_ref( osd_proxy );
}

void OSDProxyMux::handlextreemfs_lock_checkRequest
(
  xtreemfs_lock_checkRequest& req
)
{
  OSDProxy& osd_proxy 
    = get_osd_proxy
      (
        req,
        req.get_file_credentials(),
        0
      );

  osd_proxy.send( req );

  OSDProxy::dec_ref( osd_proxy );
}

void OSDProxyMux::handlextreemfs_lock_releaseRequest
(
  xtreemfs_lock_releaseRequest& req
)
{
  OSDProxy& osd_proxy 
    = get_osd_proxy
      (
        req,
        req.get_file_credentials(),
        0
      );

  osd_proxy.send( req );

  OSDProxy::dec_ref( osd_proxy );
}
