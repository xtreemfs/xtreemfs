#ifndef _83980537392_H
#define _83980537392_H





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
           fuzz_xtreemfs_address_mappings_get();
           fuzz_xtreemfs_address_mappings_remove();
           fuzz_xtreemfs_address_mappings_set();
           fuzz_xtreemfs_checkpoint();
           fuzz_xtreemfs_global_time_get();
           fuzz_xtreemfs_service_get_by_type();
           fuzz_xtreemfs_service_get_by_uuid();
           fuzz_xtreemfs_service_get_by_name();
           fuzz_xtreemfs_service_register();
           fuzz_xtreemfs_service_deregister();
           fuzz_xtreemfs_shutdown();
          }
  
  
           void fuzz_xtreemfs_address_mappings_get()
           {
            {
           std::string uuid;
           org::xtreemfs::interfaces::AddressMappingSet address_mappings;
  
             try
             {
              test_interface.xtreemfs_address_mappings_get( uuid, address_mappings );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string uuid( "bogus string" );
           org::xtreemfs::interfaces::AddressMappingSet address_mappings;
  
             try
             {
              test_interface.xtreemfs_address_mappings_get( uuid, address_mappings );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_address_mappings_remove()
           {
            {
           std::string uuid;
  
             try
             {
              test_interface.xtreemfs_address_mappings_remove( uuid );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string uuid( "bogus string" );
  
             try
             {
              test_interface.xtreemfs_address_mappings_remove( uuid );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_address_mappings_set()
           {
            {
           org::xtreemfs::interfaces::AddressMappingSet address_mappings;
  
             try
             {
              test_interface.xtreemfs_address_mappings_set( address_mappings );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_checkpoint()
           {
            {
           org::xtreemfs::interfaces::UserCredentials user_credentials( "test", StringSet( "test" ) );
  
             try
             {
              test_interface.xtreemfs_checkpoint( user_credentials );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_global_time_get()
           {
            {
  
  
             try
             {
              test_interface.xtreemfs_global_time_get();
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_service_get_by_type()
           {
            {
           uint16_t type = 0;
           org::xtreemfs::interfaces::ServiceSet services;
  
             try
             {
              test_interface.xtreemfs_service_get_by_type( type, services );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           uint16_t type = UINT16_MAX;
           org::xtreemfs::interfaces::ServiceSet services;
  
             try
             {
              test_interface.xtreemfs_service_get_by_type( type, services );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_service_get_by_uuid()
           {
            {
           std::string uuid;
           org::xtreemfs::interfaces::ServiceSet services;
  
             try
             {
              test_interface.xtreemfs_service_get_by_uuid( uuid, services );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string uuid( "bogus string" );
           org::xtreemfs::interfaces::ServiceSet services;
  
             try
             {
              test_interface.xtreemfs_service_get_by_uuid( uuid, services );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_service_get_by_name()
           {
            {
           std::string name;
           org::xtreemfs::interfaces::ServiceSet services;
  
             try
             {
              test_interface.xtreemfs_service_get_by_name( name, services );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string name( "bogus string" );
           org::xtreemfs::interfaces::ServiceSet services;
  
             try
             {
              test_interface.xtreemfs_service_get_by_name( name, services );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_service_register()
           {
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = 0;
           uint16_t type = 0;
           std::string name;
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = 0;
           uint16_t type = 0;
           std::string name;
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = 0;
           uint16_t type = 0;
           std::string name( "bogus string" );
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = 0;
           uint16_t type = 0;
           std::string name( "bogus string" );
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = 0;
           uint16_t type = UINT16_MAX;
           std::string name;
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = 0;
           uint16_t type = UINT16_MAX;
           std::string name;
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = 0;
           uint16_t type = UINT16_MAX;
           std::string name( "bogus string" );
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = 0;
           uint16_t type = UINT16_MAX;
           std::string name( "bogus string" );
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = UINT64_MAX;
           uint16_t type = 0;
           std::string name;
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = UINT64_MAX;
           uint16_t type = 0;
           std::string name;
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = UINT64_MAX;
           uint16_t type = 0;
           std::string name( "bogus string" );
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = UINT64_MAX;
           uint16_t type = 0;
           std::string name( "bogus string" );
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = UINT64_MAX;
           uint16_t type = UINT16_MAX;
           std::string name;
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = UINT64_MAX;
           uint16_t type = UINT16_MAX;
           std::string name;
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = UINT64_MAX;
           uint16_t type = UINT16_MAX;
           std::string name( "bogus string" );
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid;
           uint64_t version = UINT64_MAX;
           uint16_t type = UINT16_MAX;
           std::string name( "bogus string" );
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = 0;
           uint16_t type = 0;
           std::string name;
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = 0;
           uint16_t type = 0;
           std::string name;
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = 0;
           uint16_t type = 0;
           std::string name( "bogus string" );
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = 0;
           uint16_t type = 0;
           std::string name( "bogus string" );
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = 0;
           uint16_t type = UINT16_MAX;
           std::string name;
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = 0;
           uint16_t type = UINT16_MAX;
           std::string name;
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = 0;
           uint16_t type = UINT16_MAX;
           std::string name( "bogus string" );
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = 0;
           uint16_t type = UINT16_MAX;
           std::string name( "bogus string" );
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = UINT64_MAX;
           uint16_t type = 0;
           std::string name;
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = UINT64_MAX;
           uint16_t type = 0;
           std::string name;
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = UINT64_MAX;
           uint16_t type = 0;
           std::string name( "bogus string" );
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = UINT64_MAX;
           uint16_t type = 0;
           std::string name( "bogus string" );
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = UINT64_MAX;
           uint16_t type = UINT16_MAX;
           std::string name;
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = UINT64_MAX;
           uint16_t type = UINT16_MAX;
           std::string name;
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = UINT64_MAX;
           uint16_t type = UINT16_MAX;
           std::string name( "bogus string" );
           uint64_t last_updated = 0;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           org::xtreemfs::interfaces::Service service;
           {
           std::string uuid( "bogus string" );
           uint64_t version = UINT64_MAX;
           uint16_t type = UINT16_MAX;
           std::string name( "bogus string" );
           uint64_t last_updated = UINT64_MAX;
           org::xtreemfs::interfaces::ServiceDataMap data;
            service.set_uuid( uuid );
            service.set_version( version );
            service.set_type( type );
            service.set_name( name );
            service.set_last_updated( last_updated );
            service.set_data( data );
           }
  
             try
             {
              test_interface.xtreemfs_service_register( service );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_service_deregister()
           {
            {
           std::string uuid;
  
             try
             {
              test_interface.xtreemfs_service_deregister( uuid );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string uuid( "bogus string" );
  
             try
             {
              test_interface.xtreemfs_service_deregister( uuid );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_shutdown()
           {
            {
           org::xtreemfs::interfaces::UserCredentials user_credentials( "test", StringSet( "test" ) );
  
             try
             {
              test_interface.xtreemfs_shutdown( user_credentials );
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
