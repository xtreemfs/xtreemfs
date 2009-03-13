#ifndef _61425627471_H
#define _61425627471_H





namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
        class OSDInterfaceFuzzer
      {
      public:
          OSDInterfaceFuzzer( OSDInterface& test_interface )
              : test_interface( test_interface )
          { }
  
          void fuzz()
          {
           fuzz_read();
           fuzz_truncate();
           fuzz_unlink();
           fuzz_write();
           fuzz_keep_file_open();
           fuzz_internal_get_gmax();
           fuzz_internal_truncate();
           fuzz_internal_read_local();
           fuzz_check_object();
          }
  
  
           void fuzz_read()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint32_t length = 0;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint32_t length = UINT32_MAX;
  
             try
             {
              test_interface.read( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_truncate()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t new_file_size = 0;
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.truncate( file_id, credentials, new_file_size, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t new_file_size = UINT64_MAX;
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.truncate( file_id, credentials, new_file_size, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t new_file_size = 0;
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.truncate( file_id, credentials, new_file_size, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t new_file_size = UINT64_MAX;
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.truncate( file_id, credentials, new_file_size, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_unlink()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
  
             try
             {
              test_interface.unlink( file_id, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
  
             try
             {
              test_interface.unlink( file_id, credentials );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_write()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = 0;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = 0;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = 0;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint32_t offset = UINT32_MAX;
           uint64_t lease_timeout = UINT64_MAX;
           org::xtreemfs::interfaces::ObjectData object_data;
           {
           std::string checksum;
           uint32_t zero_padding = 0;
           bool invalid_checksum_on_osd = true;
           YIELD::STLString* data = new YIELD::STLString;
            object_data.set_checksum( checksum );
            object_data.set_zero_padding( zero_padding );
            object_data.set_invalid_checksum_on_osd( invalid_checksum_on_osd );
            object_data.set_data( data );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  
             try
             {
              test_interface.write( file_id, credentials, object_number, object_version, offset, lease_timeout, object_data, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_keep_file_open()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
  
             try
             {
              test_interface.keep_file_open( file_id, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
  
             try
             {
              test_interface.keep_file_open( file_id, credentials );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_internal_get_gmax()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
  
             try
             {
              test_interface.internal_get_gmax( file_id, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
  
             try
             {
              test_interface.internal_get_gmax( file_id, credentials );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_internal_truncate()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t new_file_size = 0;
  
             try
             {
              test_interface.internal_truncate( file_id, credentials, new_file_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t new_file_size = UINT64_MAX;
  
             try
             {
              test_interface.internal_truncate( file_id, credentials, new_file_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t new_file_size = 0;
  
             try
             {
              test_interface.internal_truncate( file_id, credentials, new_file_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t new_file_size = UINT64_MAX;
  
             try
             {
              test_interface.internal_truncate( file_id, credentials, new_file_size );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_internal_read_local()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint64_t offset = 0;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint64_t offset = 0;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint64_t offset = UINT64_MAX;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint64_t offset = UINT64_MAX;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = 0;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = 0;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = UINT64_MAX;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = UINT64_MAX;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint64_t offset = 0;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint64_t offset = 0;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint64_t offset = UINT64_MAX;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint64_t offset = UINT64_MAX;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = 0;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = 0;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = UINT64_MAX;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = UINT64_MAX;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint64_t offset = 0;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint64_t offset = 0;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint64_t offset = UINT64_MAX;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
           uint64_t offset = UINT64_MAX;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = 0;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = 0;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = UINT64_MAX;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = UINT64_MAX;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint64_t offset = 0;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint64_t offset = 0;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint64_t offset = UINT64_MAX;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
           uint64_t offset = UINT64_MAX;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = 0;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = 0;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = UINT64_MAX;
           uint64_t length = 0;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
           uint64_t offset = UINT64_MAX;
           uint64_t length = UINT64_MAX;
  
             try
             {
              test_interface.internal_read_local( file_id, credentials, object_number, object_version, offset, length );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_check_object()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
  
             try
             {
              test_interface.check_object( file_id, credentials, object_number, object_version );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
  
             try
             {
              test_interface.check_object( file_id, credentials, object_number, object_version );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
  
             try
             {
              test_interface.check_object( file_id, credentials, object_number, object_version );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id;
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
  
             try
             {
              test_interface.check_object( file_id, credentials, object_number, object_version );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = 0;
  
             try
             {
              test_interface.check_object( file_id, credentials, object_number, object_version );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = 0;
           uint64_t object_version = UINT64_MAX;
  
             try
             {
              test_interface.check_object( file_id, credentials, object_number, object_version );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = 0;
  
             try
             {
              test_interface.check_object( file_id, credentials, object_number, object_version );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::FileCredentials credentials;
           {
           org::xtreemfs::interfaces::XLocSet xlocs;
           {
           org::xtreemfs::interfaces::ReplicaSet replicas;
           uint32_t version = 0;
           std::string repUpdatePolicy;
           uint64_t read_only_file_size = 0;
            xlocs.set_replicas( replicas );
            xlocs.set_version( version );
            xlocs.set_repUpdatePolicy( repUpdatePolicy );
            xlocs.set_read_only_file_size( read_only_file_size );
           }
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
            credentials.set_xlocs( xlocs );
            credentials.set_xcap( xcap );
           }
           uint64_t object_number = UINT64_MAX;
           uint64_t object_version = UINT64_MAX;
  
             try
             {
              test_interface.check_object( file_id, credentials, object_number, object_version );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
      private:
          OSDInterface& test_interface;
      };
  
    };
  
  
  
  };
  
  
  
};

#endif
