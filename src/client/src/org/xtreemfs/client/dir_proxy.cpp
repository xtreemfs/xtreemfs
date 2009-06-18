// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/dir_proxy.h"
using namespace org::xtreemfs::client;


DIRProxy::~DIRProxy()
{
  for ( std::map<std::string, CachedAddressMappings*>::iterator uuid_to_address_mappings_i = uuid_to_address_mappings_cache.begin(); uuid_to_address_mappings_i != uuid_to_address_mappings_cache.end(); uuid_to_address_mappings_i++ )
    YIELD::Object::decRef( *uuid_to_address_mappings_i->second );
}

YIELD::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> DIRProxy::getAddressMappingsFromUUID( const std::string& uuid )
{
  if ( uuid_to_address_mappings_cache_lock.try_acquire() )
  {
    std::map<std::string, CachedAddressMappings*>::iterator uuid_to_address_mappings_i = uuid_to_address_mappings_cache.find( uuid );
    if ( uuid_to_address_mappings_i != uuid_to_address_mappings_cache.end() )
    {
      CachedAddressMappings* cached_address_mappings = uuid_to_address_mappings_i->second;
      uint32_t cached_address_mappings_age_s = ( YIELD::Time()- cached_address_mappings->get_creation_time() ).as_unix_time_s();
      if ( cached_address_mappings_age_s < cached_address_mappings->get_ttl_s() )
      {
        cached_address_mappings->incRef();
        uuid_to_address_mappings_cache_lock.release();
        return cached_address_mappings;
      }
      else
      {
        YIELD::Object::decRef( cached_address_mappings );
        uuid_to_address_mappings_cache.erase( uuid_to_address_mappings_i );
        uuid_to_address_mappings_cache_lock.release();
      }
    }
    else
      uuid_to_address_mappings_cache_lock.release();
  }
  
  org::xtreemfs::interfaces::AddressMappingSet address_mappings;
  xtreemfs_address_mappings_get( uuid, address_mappings );
  if ( !address_mappings.empty() )
  {
    CachedAddressMappings* cached_address_mappings = new CachedAddressMappings( address_mappings, address_mappings[0].get_ttl_s() );
    uuid_to_address_mappings_cache_lock.acquire();
    uuid_to_address_mappings_cache[uuid] = &cached_address_mappings->incRef();
    uuid_to_address_mappings_cache_lock.release();    
    return cached_address_mappings;
  }
  else
    throw YIELD::Exception( "could not find address mappings for UUID" );
}


YIELD::auto_Object<YIELD::URI> DIRProxy::getVolumeURIFromVolumeName( const std::string& volume_name )
{
  org::xtreemfs::interfaces::ServiceSet services;
  xtreemfs_service_get_by_name( volume_name, services );
  if ( !services.empty() )
  {
    for ( org::xtreemfs::interfaces::ServiceSet::const_iterator service_i = services.begin(); service_i != services.end(); service_i++ )
    {
      const org::xtreemfs::interfaces::ServiceDataMap& data = ( *service_i ).get_data();
      for ( org::xtreemfs::interfaces::ServiceDataMap::const_iterator service_data_i = data.begin(); service_data_i != data.end(); service_data_i++ )
      {
        if ( service_data_i->first == "mrc" )
        {
          YIELD::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> address_mappings = getAddressMappingsFromUUID( service_data_i->second );

          for ( org::xtreemfs::interfaces::AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin(); address_mapping_i != address_mappings->end(); address_mapping_i++ )
          {
            if ( ( *address_mapping_i ).get_protocol() == org::xtreemfs::interfaces::ONCRPC_SCHEME )
              return new YIELD::URI( ( *address_mapping_i ).get_uri() );
          }

          for ( org::xtreemfs::interfaces::AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin(); address_mapping_i != address_mappings->end(); address_mapping_i++ )
          {
            if ( ( *address_mapping_i ).get_protocol() == org::xtreemfs::interfaces::ONCRPCS_SCHEME )
              return new YIELD::URI( ( *address_mapping_i ).get_uri() );
          }

          for ( org::xtreemfs::interfaces::AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin(); address_mapping_i != address_mappings->end(); address_mapping_i++ )
          {
            if ( ( *address_mapping_i ).get_protocol() == org::xtreemfs::interfaces::ONCRPCU_SCHEME )
              return new YIELD::URI( ( *address_mapping_i ).get_uri() );
          }
        }
      }
    }

    throw YIELD::Exception( "unknown volume" );
  }
  else
    throw YIELD::Exception( "unknown volume" );
}
