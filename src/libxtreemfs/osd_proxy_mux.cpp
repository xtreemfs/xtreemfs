// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/osd_proxy_mux.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;


class OSDProxyMux::ReadResponseTarget : public YIELD::concurrency::EventTarget
{
public:
  ReadResponseTarget
  ( 
    auto_OSDProxyMux osd_proxy_mux, 
    yidl::runtime::auto_Object <readRequest> read_request 
  )
    : original_response_target( read_request->get_response_target() ), 
      osd_proxy_mux( osd_proxy_mux ), 
      read_request( read_request )
  { }

  // yidl::runtime::Object
  YIDL_RUNTIME_OBJECT_PROTOTYPES( OSDProxyMux::ReadResponseTarget, 1 );

  // YIELD::concurrency::EventTarget
  void send( YIELD::concurrency::Event& ev )
  {
    switch ( ev.get_type_id() )
    {
      case YIDL_RUNTIME_OBJECT_TYPE_ID( readResponse ):
      {
        static_cast<readResponse&>( ev ).set_selected_file_replica
        ( 
          read_request->get_selected_file_replica() 
        );

        original_response_target->send( ev );
      }
      break;

      case YIDL_RUNTIME_OBJECT_TYPE_ID
        ( YIELD::concurrency::ExceptionResponse ):
      {
        if 
        ( 
          read_request->get_file_credentials().get_xlocs().
            get_replicas().size() > 1 
        )
        {
          // Set selected_replica to -1 * the index of 
          // the failed replica and retry
          if ( read_request->get_selected_file_replica() <= 0 ) 
            DebugBreak();

          read_request->set_selected_file_replica
          ( 
            -1 * read_request->get_selected_file_replica() 
          );

          osd_proxy_mux->send( read_request->incRef() );

          YIELD::concurrency::Event::decRef( ev );
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
  YIELD::concurrency::auto_EventTarget original_response_target;
  yidl::runtime::auto_Object<OSDProxyMux> osd_proxy_mux;
  yidl::runtime::auto_Object<readRequest> read_request;
};


class OSDProxyMux::TruncateResponseTarget 
  : public YIELD::concurrency::EventTarget
{
public:
  TruncateResponseTarget
  ( 
    size_t expected_response_count, 
    YIELD::concurrency::auto_EventTarget final_response_target 
  )
    : expected_response_count( expected_response_count ), 
      final_response_target( final_response_target )
  { }

  virtual ~TruncateResponseTarget()
  {
    for 
    ( 
      std::vector<YIELD::concurrency::Event*>::iterator 
        response_i = responses.begin(); 
      response_i != responses.end(); 
      response_i++ 
    )
      YIELD::concurrency::Event::decRef( **response_i );
  }

  // yidl::runtime::Object
  YIDL_RUNTIME_OBJECT_PROTOTYPES( OSDProxyMux::TruncateResponseTarget, 0 );

  // YIELD::concurrency::EventTarget
  void send( YIELD::concurrency::Event& ev )
  {
    responses_lock.acquire();

    responses.push_back( &ev );

    if ( responses.size() == expected_response_count )
    {
      for 
      ( 
        std::vector<YIELD::concurrency::Event*>::iterator 
          response_i = responses.begin(); 
        response_i != responses.end(); 
        response_i++ 
      )
      {
        if 
        ( 
          ( *response_i )->get_type_id() == 
          YIDL_RUNTIME_OBJECT_TYPE_ID( YIELD::concurrency::ExceptionResponse ) 
        )
        {
          respond
          ( 
            static_cast<YIELD::concurrency::ExceptionResponse*>
              ( *response_i )->incRef(), 
            final_response_target 
          );
          responses_lock.release();
          return;
        }
      }

      respond( responses, final_response_target );
    }

    responses_lock.release();
  }

protected:
  virtual void respond
  ( 
    yidl::runtime::auto_Object<YIELD::concurrency::ExceptionResponse> 
      exception_response, 
    YIELD::concurrency::auto_EventTarget final_response_target 
  )
  {
    final_response_target->send( *exception_response.release() );
  }

  virtual void respond
  ( 
    std::vector<YIELD::concurrency::Event*>& responses, 
    YIELD::concurrency::auto_EventTarget final_response_target 
  )
  {
    final_response_target->send( responses[responses.size() - 1]->incRef() );
  }

private:
  size_t expected_response_count;
  YIELD::concurrency::auto_EventTarget final_response_target;

  std::vector<YIELD::concurrency::Event*> responses;
  YIELD::platform::Mutex responses_lock;
};


auto_OSDProxyMux
OSDProxyMux::create
( 
  auto_DIRProxy dir_proxy,
  uint16_t concurrency_level,
  uint32_t flags,
  YIELD::platform::auto_Log log,
  const YIELD::platform::Time& operation_timeout,
  uint8_t reconnect_tries_max,
  YIELD::ipc::auto_SSLContext ssl_context,
  auto_UserCredentialsCache user_credentials_cache 
)
{
  if ( user_credentials_cache == NULL )
    user_credentials_cache = new UserCredentialsCache;

  return new OSDProxyMux
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
  yidl::runtime::auto_Object<DIRProxy> dir_proxy, 
  uint32_t flags, 
  YIELD::platform::auto_Log log, 
  const YIELD::platform::Time& operation_timeout, 
  uint8_t reconnect_tries_max,
  YIELD::ipc::auto_SSLContext ssl_context,
  auto_UserCredentialsCache user_credentials_cache
)
  : concurrency_level( concurrency_level ), 
    dir_proxy( dir_proxy ),     
    flags( flags ), 
    log( log ), 
    operation_timeout( operation_timeout ), 
    reconnect_tries_max( reconnect_tries_max ),
    ssl_context( ssl_context ),
    user_credentials_cache( user_credentials_cache )
{
  osd_proxy_stage_group = new YIELD::concurrency::SEDAStageGroup;
}

OSDProxyMux::~OSDProxyMux()
{
  for 
  ( 
    OSDProxyMap::iterator osd_proxies_i = osd_proxies.begin(); 
    osd_proxies_i != osd_proxies.end(); 
    osd_proxies_i++ 
  )
    OSDProxy::decRef( *osd_proxies_i->second );
}

auto_OSDProxy OSDProxyMux::getOSDProxy
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
      const std::string& osd_uuid = selected_file_replica->get_osd_uuids()[osd_i];
      return getOSDProxy( osd_uuid );
    }

    default: DebugBreak(); throw YIELD::platform::Exception(); break;
  }
}

auto_OSDProxy OSDProxyMux::getOSDProxy( const std::string& osd_uuid )
{
  auto_OSDProxy osd_proxy;

  OSDProxyMap::iterator osd_proxies_i = osd_proxies.find( osd_uuid );

  if ( osd_proxies_i != osd_proxies.end() )
    osd_proxy = osd_proxies_i->second->incRef();
  else
  {
    yidl::runtime::auto_Object<AddressMappingSet> 
      address_mappings = dir_proxy->getAddressMappingsFromUUID( osd_uuid );

    for 
    ( 
      AddressMappingSet::iterator 
        address_mapping_i = address_mappings->begin(); 
      address_mapping_i != address_mappings->end(); 
      address_mapping_i++ 
    )
    {
#ifdef YIELD_HAVE_OPENSSL
      if ( ssl_context != NULL && 
           ( 
             ( *address_mapping_i ).get_protocol() == ONCRPCS_SCHEME ||
             ( *address_mapping_i ).get_protocol() == ONCRPCG_SCHEME
           ) 
         )
      {
        osd_proxy = OSDProxy::create
        ( 
          ( *address_mapping_i ).get_uri(), 
          concurrency_level,
          flags, 
          log, 
          operation_timeout, 
          reconnect_tries_max, 
          ssl_context,
          user_credentials_cache
        ).release();

        osd_proxy_stage_group->createStage( osd_proxy->incRef() );
      }
      else
#endif
      if ( ( *address_mapping_i ).get_protocol() == ONCRPC_SCHEME )
      {
        osd_proxy = OSDProxy::create
        ( 
          ( *address_mapping_i ).get_uri(),
          concurrency_level,
          flags, 
          log, 
          operation_timeout, 
          reconnect_tries_max, 
          ssl_context,
          user_credentials_cache
        ).release();

        osd_proxy_stage_group->createStage( osd_proxy->incRef() );
      }
    }

    if ( osd_proxy != NULL )
      osd_proxies[osd_uuid] = &osd_proxy->incRef();
    else
      throw YIELD::platform::Exception( "no acceptable ONC-RPC URI for UUID" );
  }

  return osd_proxy;
}

void OSDProxyMux::handlereadRequest( readRequest& req )
{
  auto_OSDProxy osd_proxy
  ( 
    getOSDProxy
    ( 
      req, 
      req.get_file_credentials(), 
      req.get_object_number() 
    ) 
  );

  if 
  ( 
    req.get_response_target()->get_type_id() != 
    YIDL_RUNTIME_OBJECT_TYPE_ID( ReadResponseTarget ) 
  )
    req.set_response_target( new ReadResponseTarget( incRef(), req ) );

  static_cast<YIELD::concurrency::EventTarget*>
    ( osd_proxy.get() )->send( req );
}

void OSDProxyMux::handletruncateRequest( truncateRequest& req )
{
  const ReplicaSet& 
    replicas = req.get_file_credentials().get_xlocs().get_replicas();

  if ( req.get_response_target() != NULL )
    req.set_response_target
    ( 
      new TruncateResponseTarget( replicas.size(), req.get_response_target() ) 
    );

  for 
  ( 
    ReplicaSet::const_iterator 
      replica_i = replicas.begin(); 
    replica_i != replicas.end(); 
    replica_i++ 
  )
    static_cast<YIELD::concurrency::EventTarget*>
    ( 
      getOSDProxy( ( *replica_i ).get_osd_uuids()[0] ).get() 
    )->send( req.incRef() );

  truncateRequest::decRef( req );
}

void OSDProxyMux::handleunlinkRequest( unlinkRequest& req )
{
  const ReplicaSet& 
    replicas = req.get_file_credentials().get_xlocs().get_replicas();

  if ( req.get_response_target() != NULL )
    req.set_response_target
    ( 
      new TruncateResponseTarget( replicas.size(), req.get_response_target() ) 
    );

  for 
  ( 
    ReplicaSet::const_iterator 
      replica_i = replicas.begin(); 
    replica_i != replicas.end(); 
    replica_i++ 
  )
    static_cast<YIELD::concurrency::EventTarget*>
    ( 
      getOSDProxy( ( *replica_i ).get_osd_uuids()[0] ).get() 
    )->send( req.incRef() );

  unlinkRequest::decRef( req );
}

void OSDProxyMux::handlewriteRequest( writeRequest& req )
{
  getOSDProxy
  ( 
    req, 
    req.get_file_credentials(), 
    req.get_object_number() 
  )->send( req );
}

void OSDProxyMux::handlextreemfs_lock_acquireRequest
( 
  xtreemfs_lock_acquireRequest& req 
)
{
  getOSDProxy
  ( 
    req, 
    req.get_file_credentials(), 
    0 
  )->send( req );
}

void OSDProxyMux::handlextreemfs_lock_checkRequest
( 
  xtreemfs_lock_checkRequest& req 
)
{
  getOSDProxy
  ( 
    req, 
    req.get_file_credentials(), 
    0 
  )->send( req );
}

void OSDProxyMux::handlextreemfs_lock_releaseRequest
( 
  xtreemfs_lock_releaseRequest& req 
)
{
  getOSDProxy
  ( 
    req, 
    req.get_file_credentials(), 
    0 
  )->send( req );
}

