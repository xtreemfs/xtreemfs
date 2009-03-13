#include "org/xtreemfs/client/osd_proxy_factory.h"
#include "org/xtreemfs/client/dir_proxy.h"
using namespace org::xtreemfs::client;


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class VersionedURI : public YIELD::URI
      {
      public:
        VersionedURI( const std::string& uri, uint64_t version )
          : YIELD::URI( uri ), version( version )
        { }
        
        uint64_t get_version() const { return version; }

      private:
        uint64_t version;
      };
    };
  };
};


OSDProxyFactory::OSDProxyFactory( DIRProxy& dir_proxy, YIELD::StageGroup& osd_proxy_stage_group, uint32_t osd_proxy_flags )
  : dir_proxy( dir_proxy ), osd_proxy_stage_group( osd_proxy_stage_group ), osd_proxy_flags( osd_proxy_flags )
{ }

OSDProxyFactory::~OSDProxyFactory()
{
  for ( std::map<std::string, VersionedURI*>::iterator uuid_to_uri_i = uuid_to_uri_map.begin(); uuid_to_uri_i != uuid_to_uri_map.end(); uuid_to_uri_i++ )
    delete uuid_to_uri_i->second;
}

OSDProxy& OSDProxyFactory::createOSDProxy( const YIELD::URI& uri )
{
  OSDProxy* osd_proxy = new OSDProxy( uri );
  osd_proxy_stage_group.createStage( *osd_proxy, osd_proxy_flags );
  return *osd_proxy;
}

OSDProxy& OSDProxyFactory::createOSDProxy( const std::string& uuid, uint64_t version )
{
  uuid_to_uri_map_lock.acquire();  
  std::map<std::string, VersionedURI*>::iterator uuid_to_uri_i = uuid_to_uri_map.find( uuid );
  if ( uuid_to_uri_i != uuid_to_uri_map.end() )
  {
    VersionedURI* versioned_uri = uuid_to_uri_i->second;   
    if ( versioned_uri->get_version() == version )      
    {
      OSDProxy& osd_proxy = createOSDProxy( *versioned_uri ); // Have to create the proxy here in case the versioned_uri is deleted by another thread
      uuid_to_uri_map_lock.release();
      return osd_proxy;
    }
    else if ( versioned_uri->get_version() < version )
    {
      uuid_to_uri_map.erase( uuid_to_uri_i );
      delete versioned_uri;
      uuid_to_uri_map_lock.release();
    }
    else
      YIELD::DebugBreak();
  }
  else
    uuid_to_uri_map_lock.release();

  org::xtreemfs::interfaces::AddressMappingSet address_mappings;
  dir_proxy.address_mappings_get( uuid, address_mappings, static_cast<uint64_t>( -1 ) );
  if ( !address_mappings.empty() )
  {
    const org::xtreemfs::interfaces::AddressMapping& address_mapping = address_mappings[0];
    std::ostringstream uri_str;
    uri_str << address_mapping.get_protocol() << "://" << address_mapping.get_address() << ":" << address_mapping.get_port();
    VersionedURI* versioned_uri = new VersionedURI( uri_str.str(), version );
    OSDProxy& osd_proxy = createOSDProxy( *versioned_uri );
    uuid_to_uri_map_lock.acquire();
    uuid_to_uri_map[uuid] = versioned_uri;
    uuid_to_uri_map_lock.release();
    return osd_proxy;
  }
  else 
      throw YIELD::Exception( "could not find address mapping for UUID" );
}
