#ifndef _13303512770_H
#define _13303512770_H

namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
        class DIRInterfaceFuzzer
      {
      public:
          DIRInterfaceFuzzer( DIRInterface& test_interface )
              : test_interface( test_interface )
          { }
  
          void fuzz()
          {
           fuzz_address_mappings_get();
           fuzz_address_mappings_set();
           fuzz_address_mappings_delete();
           fuzz_service_register();
           fuzz_service_deregister();
           fuzz_service_get_by_type();
           fuzz_service_get_by_uuid();
           fuzz_service_get_by_name();
           fuzz_global_time_get();
          }
  
  
           void fuzz_address_mappings_get()
           {
            {
           std::string uuid;
           org::xtreemfs::interfaces::AddressMappingSet address_mappings;
  
             try
             {
              test_interface.address_mappings_get( uuid, address_mappings );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string uuid( "bogus string" );
           org::xtreemfs::interfaces::AddressMappingSet address_mappings;
  
             try
             {
              test_interface.address_mappings_get( uuid, address_mappings );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_address_mappings_set()
           {
            {
           org::xtreemfs::interfaces::AddressMappingSet address_mappings;
  
             try
             {
              test_interface.address_mappings_set( address_mappings );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_address_mappings_delete()
           {
            {
           std::string uuid;
  
             try
             {
              test_interface.address_mappings_delete( uuid );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string uuid( "bogus string" );
  
             try
             {
              test_interface.address_mappings_delete( uuid );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_service_register()
           {
            {
           org::xtreemfs::interfaces::ServiceRegistry service;
           {
           std::string uuid;
           uint64_t version = 0;
           uint16_t service_type = 0;
           std::string service_name;
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceRegistryDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_service_type( service_type );
            service.set_service_name( service_name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.service_register( service );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_service_deregister()
           {
            {
           std::string uuid;
  
             try
             {
              test_interface.service_deregister( uuid );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string uuid( "bogus string" );
  
             try
             {
              test_interface.service_deregister( uuid );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_service_get_by_type()
           {
            {
           uint16_t type = 0;
           org::xtreemfs::interfaces::ServiceRegistrySet services;
  
             try
             {
              test_interface.service_get_by_type( type, services );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           uint16_t type = UINT16_MAX;
           org::xtreemfs::interfaces::ServiceRegistrySet services;
  
             try
             {
              test_interface.service_get_by_type( type, services );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_service_get_by_uuid()
           {
            {
           std::string uuid;
           org::xtreemfs::interfaces::ServiceRegistrySet services;
  
             try
             {
              test_interface.service_get_by_uuid( uuid, services );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string uuid( "bogus string" );
           org::xtreemfs::interfaces::ServiceRegistrySet services;
  
             try
             {
              test_interface.service_get_by_uuid( uuid, services );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_service_get_by_name()
           {
            {
           std::string service_name;
           org::xtreemfs::interfaces::ServiceRegistrySet services;
  
             try
             {
              test_interface.service_get_by_name( service_name, services );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string service_name( "bogus string" );
           org::xtreemfs::interfaces::ServiceRegistrySet services;
  
             try
             {
              test_interface.service_get_by_name( service_name, services );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_global_time_get()
           {
            {
  
  
             try
             {
              test_interface.global_time_get();
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
      private:
          DIRInterface& test_interface;
      };
  
    };
  
  
  
  };
  
  
  
};

#endif
