#ifndef _962331829_H_
#define _962331829_H_


#include "constants.h"
#include "types.h"
#include "yield/concurrency.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      enum ServiceType { SERVICE_TYPE_MIXED = 0, SERVICE_TYPE_MRC = 1, SERVICE_TYPE_OSD = 2, SERVICE_TYPE_VOLUME = 3 };
  
  
      class AddressMapping : public ::yidl::runtime::Struct
      {
      public:
        AddressMapping() : version( 0 ), port( 0 ), ttl_s( 0 ) { }
        AddressMapping( const string& uuid, uint64_t version, const string& protocol, const string& address, uint16_t port, const string& match_network, uint32_t ttl_s, const string& uri ) : uuid( uuid ), version( version ), protocol( protocol ), address( address ), port( port ), match_network( match_network ), ttl_s( ttl_s ), uri( uri ) { }
        virtual ~AddressMapping() {  }
  
        const string& get_uuid() const { return uuid; }
        uint64_t get_version() const { return version; }
        const string& get_protocol() const { return protocol; }
        const string& get_address() const { return address; }
        uint16_t get_port() const { return port; }
        const string& get_match_network() const { return match_network; }
        uint32_t get_ttl_s() const { return ttl_s; }
        const string& get_uri() const { return uri; }
        void set_uuid( const string& uuid ) { this->uuid = uuid; }
        void set_version( uint64_t version ) { this->version = version; }
        void set_protocol( const string& protocol ) { this->protocol = protocol; }
        void set_address( const string& address ) { this->address = address; }
        void set_port( uint16_t port ) { this->port = port; }
        void set_match_network( const string& match_network ) { this->match_network = match_network; }
        void set_ttl_s( uint32_t ttl_s ) { this->ttl_s = ttl_s; }
        void set_uri( const string& uri ) { this->uri = uri; }
  
        bool operator==( const AddressMapping& other ) const
        {
          return uuid == other.uuid
                 &&
                 version == other.version
                 &&
                 protocol == other.protocol
                 &&
                 address == other.address
                 &&
                 port == other.port
                 &&
                 match_network == other.match_network
                 &&
                 ttl_s == other.ttl_s
                 &&
                 uri == other.uri;
        }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( AddressMapping, 2010030946 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( "uuid", 0, uuid );
          marshaller.write( "version", 0, version );
          marshaller.write( "protocol", 0, protocol );
          marshaller.write( "address", 0, address );
          marshaller.write( "port", 0, port );
          marshaller.write( "match_network", 0, match_network );
          marshaller.write( "ttl_s", 0, ttl_s );
          marshaller.write( "uri", 0, uri );
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( "uuid", 0, uuid );
          version = unmarshaller.read_uint64( "version", 0 );
          unmarshaller.read( "protocol", 0, protocol );
          unmarshaller.read( "address", 0, address );
          port = unmarshaller.read_uint16( "port", 0 );
          unmarshaller.read( "match_network", 0, match_network );
          ttl_s = unmarshaller.read_uint32( "ttl_s", 0 );
          unmarshaller.read( "uri", 0, uri );
        }
  
      protected:
        string uuid;
        uint64_t version;
        string protocol;
        string address;
        uint16_t port;
        string match_network;
        uint32_t ttl_s;
        string uri;
      };
  
      class AddressMappingSet
        : public ::yidl::runtime::Sequence,
          public vector<org::xtreemfs::interfaces::AddressMapping>
      {
      public:
        AddressMappingSet() { }
        AddressMappingSet( const org::xtreemfs::interfaces::AddressMapping& first_value ) { vector<org::xtreemfs::interfaces::AddressMapping>::push_back( first_value ); }
        AddressMappingSet( size_type size ) : vector<org::xtreemfs::interfaces::AddressMapping>( size ) { }
        virtual ~AddressMappingSet() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( AddressMappingSet, 2010030947 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          size_type value_i_max = size();
          for ( size_type value_i = 0; value_i < value_i_max; value_i++ )
          {
            marshaller.write( "value", 0, ( *this )[value_i] );
          }
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          org::xtreemfs::interfaces::AddressMapping value;
          unmarshaller.read( "value", 0, value );
          push_back( value );
        }
  
        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };
  
      class ServiceDataMap
          : public ::yidl::runtime::Map,
            public map<string,string>
      {
      public:
        virtual ~ServiceDataMap() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ServiceDataMap, 2010030948 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          for ( const_iterator i = begin(); i != end(); i++ )
          {
            marshaller.write( i->first.c_str(), static_cast<uint32_t>( 0 ), i->second );
          }
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          string key;
          unmarshaller.read( 0, static_cast<uint32_t>( 0 ), key );
          if ( !key.empty() )
          {
            string value;
            unmarshaller.read( key.c_str(), static_cast<uint32_t>( 0 ), value );
            ( *this )[key] = value;
          }
        }
  
        // yidl::runtime::Map
        size_t get_size() const { return size(); }
      };
  
      class Service : public ::yidl::runtime::Struct
      {
      public:
        Service() : type( SERVICE_TYPE_MIXED ), version( 0 ), last_updated_s( 0 ) { }
        Service( org::xtreemfs::interfaces::ServiceType type, const string& uuid, uint64_t version, const string& name, uint64_t last_updated_s, const org::xtreemfs::interfaces::ServiceDataMap& data ) : type( type ), uuid( uuid ), version( version ), name( name ), last_updated_s( last_updated_s ), data( data ) { }
        virtual ~Service() {  }
  
        org::xtreemfs::interfaces::ServiceType get_type() const { return type; }
        const string& get_uuid() const { return uuid; }
        uint64_t get_version() const { return version; }
        const string& get_name() const { return name; }
        uint64_t get_last_updated_s() const { return last_updated_s; }
        const org::xtreemfs::interfaces::ServiceDataMap& get_data() const { return data; }
        void set_type( org::xtreemfs::interfaces::ServiceType type ) { this->type = type; }
        void set_uuid( const string& uuid ) { this->uuid = uuid; }
        void set_version( uint64_t version ) { this->version = version; }
        void set_name( const string& name ) { this->name = name; }
        void set_last_updated_s( uint64_t last_updated_s ) { this->last_updated_s = last_updated_s; }
        void set_data( const org::xtreemfs::interfaces::ServiceDataMap&  data ) { this->data = data; }
  
        bool operator==( const Service& other ) const
        {
          return type == other.type
                 &&
                 uuid == other.uuid
                 &&
                 version == other.version
                 &&
                 name == other.name
                 &&
                 last_updated_s == other.last_updated_s
                 &&
                 data == other.data;
        }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Service, 2010030950 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( "type", 0, static_cast<int32_t>( type ) );
          marshaller.write( "uuid", 0, uuid );
          marshaller.write( "version", 0, version );
          marshaller.write( "name", 0, name );
          marshaller.write( "last_updated_s", 0, last_updated_s );
          marshaller.write( "data", 0, data );
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          type = static_cast<org::xtreemfs::interfaces::ServiceType>( unmarshaller.read_int32( "type", 0 ) );
          unmarshaller.read( "uuid", 0, uuid );
          version = unmarshaller.read_uint64( "version", 0 );
          unmarshaller.read( "name", 0, name );
          last_updated_s = unmarshaller.read_uint64( "last_updated_s", 0 );
          unmarshaller.read( "data", 0, data );
        }
  
      protected:
        org::xtreemfs::interfaces::ServiceType type;
        string uuid;
        uint64_t version;
        string name;
        uint64_t last_updated_s;
        org::xtreemfs::interfaces::ServiceDataMap data;
      };
  
      class ServiceSet
        : public ::yidl::runtime::Sequence,
          public vector<org::xtreemfs::interfaces::Service>
      {
      public:
        ServiceSet() { }
        ServiceSet( const org::xtreemfs::interfaces::Service& first_value ) { vector<org::xtreemfs::interfaces::Service>::push_back( first_value ); }
        ServiceSet( size_type size ) : vector<org::xtreemfs::interfaces::Service>( size ) { }
        virtual ~ServiceSet() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ServiceSet, 2010030951 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          size_type value_i_max = size();
          for ( size_type value_i = 0; value_i < value_i_max; value_i++ )
          {
            marshaller.write( "value", 0, ( *this )[value_i] );
          }
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          org::xtreemfs::interfaces::Service value;
          unmarshaller.read( "value", 0, value );
          push_back( value );
        }
  
        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };
  
      class DirService : public ::yidl::runtime::Struct
      {
      public:
        DirService() : port( 0 ), interface_version( 0 ) { }
        DirService( const string& address, uint16_t port, const string& protocol, uint32_t interface_version ) : address( address ), port( port ), protocol( protocol ), interface_version( interface_version ) { }
        virtual ~DirService() {  }
  
        const string& get_address() const { return address; }
        uint16_t get_port() const { return port; }
        const string& get_protocol() const { return protocol; }
        uint32_t get_interface_version() const { return interface_version; }
        void set_address( const string& address ) { this->address = address; }
        void set_port( uint16_t port ) { this->port = port; }
        void set_protocol( const string& protocol ) { this->protocol = protocol; }
        void set_interface_version( uint32_t interface_version ) { this->interface_version = interface_version; }
  
        bool operator==( const DirService& other ) const
        {
          return address == other.address
                 &&
                 port == other.port
                 &&
                 protocol == other.protocol
                 &&
                 interface_version == other.interface_version;
        }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( DirService, 2010030951 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( "address", 0, address );
          marshaller.write( "port", 0, port );
          marshaller.write( "protocol", 0, protocol );
          marshaller.write( "interface_version", 0, interface_version );
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( "address", 0, address );
          port = unmarshaller.read_uint16( "port", 0 );
          unmarshaller.read( "protocol", 0, protocol );
          interface_version = unmarshaller.read_uint32( "interface_version", 0 );
        }
  
      protected:
        string address;
        uint16_t port;
        string protocol;
        uint32_t interface_version;
      };
  
  
      class DIRInterface
      {
      public:
        const static uint32_t TAG = 2010031016;
  
        virtual ~DIRInterface() { }
  
        uint32_t get_tag() const { return 2010031016; }
  
        const static uint32_t HTTP_PORT_DEFAULT = 30638;
        const static uint32_t ONCRPC_PORT_DEFAULT = 32638;
        const static uint32_t ONCRPCG_PORT_DEFAULT = 32638;
        const static uint32_t ONCRPCS_PORT_DEFAULT = 32638;
        const static uint32_t ONCRPCU_PORT_DEFAULT = 32638;
  
        virtual void xtreemfs_address_mappings_get( const string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { }
        virtual void xtreemfs_address_mappings_remove( const string& uuid ) { }
        virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { return 0; }
        virtual void xtreemfs_checkpoint() { }
        virtual void xtreemfs_discover_dir( org::xtreemfs::interfaces::DirService& dir_service ) { }
        virtual uint64_t xtreemfs_global_time_s_get() { return 0; }
        virtual void xtreemfs_replication_to_master() { }
        virtual void xtreemfs_service_deregister( const string& uuid ) { }
        virtual void xtreemfs_service_get_by_name( const string& name, org::xtreemfs::interfaces::ServiceSet& services ) { }
        virtual void xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services ) { }
        virtual void xtreemfs_service_get_by_uuid( const string& uuid, org::xtreemfs::interfaces::ServiceSet& services ) { }
        virtual void xtreemfs_service_offline( const string& uuid ) { }
        virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service ) { return 0; }
        virtual void xtreemfs_shutdown() { }
      };
  
      // Use this macro in an implementation class to get all of the prototypes for the operations in DIRInterface
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_PROTOTYPES \
      virtual void xtreemfs_address_mappings_get( const string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings );\
      virtual void xtreemfs_address_mappings_remove( const string& uuid );\
      virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings );\
      virtual void xtreemfs_checkpoint();\
      virtual void xtreemfs_discover_dir( org::xtreemfs::interfaces::DirService& dir_service );\
      virtual uint64_t xtreemfs_global_time_s_get();\
      virtual void xtreemfs_replication_to_master();\
      virtual void xtreemfs_service_deregister( const string& uuid );\
      virtual void xtreemfs_service_get_by_name( const string& name, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void xtreemfs_service_get_by_uuid( const string& uuid, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void xtreemfs_service_offline( const string& uuid );\
      virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service );\
      virtual void xtreemfs_shutdown();
  
  
      #ifndef ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_EXCEPTION_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ::yield::concurrency::ExceptionResponse
      #endif
      #endif
      #ifndef ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS ORG_XTREEMFS_REQUEST_PARENT_CLASS
      #elif defined( ORG_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS ORG_REQUEST_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS ::yield::concurrency::Request
      #endif
      #endif
      #ifndef ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS ORG_XTREEMFS_RESPONSE_PARENT_CLASS
      #elif defined( ORG_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS ORG_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS ::yield::concurrency::Response
      #endif
      #endif
  
  
      class DIRInterfaceEvents
      {
      public:
        // Request/response pair definitions for the operations in DIRInterface
  
        class xtreemfs_address_mappings_getRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_getRequest() { }
          xtreemfs_address_mappings_getRequest( const string& uuid ) : uuid( uuid ) { }
          virtual ~xtreemfs_address_mappings_getRequest() {  }
  
          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }
  
          bool operator==( const xtreemfs_address_mappings_getRequest& other ) const
          {
            return uuid == other.uuid;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_getRequest, 2010031017 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "uuid", 0, uuid );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "uuid", 0, uuid );
          }
  
        protected:
          string uuid;
        };
  
        class xtreemfs_address_mappings_getResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_getResponse() { }
          xtreemfs_address_mappings_getResponse( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) : address_mappings( address_mappings ) { }
          virtual ~xtreemfs_address_mappings_getResponse() {  }
  
          const org::xtreemfs::interfaces::AddressMappingSet& get_address_mappings() const { return address_mappings; }
          void set_address_mappings( const org::xtreemfs::interfaces::AddressMappingSet&  address_mappings ) { this->address_mappings = address_mappings; }
  
          bool operator==( const xtreemfs_address_mappings_getResponse& other ) const
          {
            return address_mappings == other.address_mappings;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_getResponse, 2010031017 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "address_mappings", 0, address_mappings );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "address_mappings", 0, address_mappings );
          }
  
        protected:
          org::xtreemfs::interfaces::AddressMappingSet address_mappings;
        };
  
        class xtreemfs_address_mappings_removeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_removeRequest() { }
          xtreemfs_address_mappings_removeRequest( const string& uuid ) : uuid( uuid ) { }
          virtual ~xtreemfs_address_mappings_removeRequest() {  }
  
          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }
  
          bool operator==( const xtreemfs_address_mappings_removeRequest& other ) const
          {
            return uuid == other.uuid;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_removeRequest, 2010031018 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "uuid", 0, uuid );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "uuid", 0, uuid );
          }
  
        protected:
          string uuid;
        };
  
        class xtreemfs_address_mappings_removeResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_removeResponse() { }
          virtual ~xtreemfs_address_mappings_removeResponse() {  }
  
          bool operator==( const xtreemfs_address_mappings_removeResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_removeResponse, 2010031018 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_address_mappings_setRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_setRequest() { }
          xtreemfs_address_mappings_setRequest( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) : address_mappings( address_mappings ) { }
          virtual ~xtreemfs_address_mappings_setRequest() {  }
  
          const org::xtreemfs::interfaces::AddressMappingSet& get_address_mappings() const { return address_mappings; }
          void set_address_mappings( const org::xtreemfs::interfaces::AddressMappingSet&  address_mappings ) { this->address_mappings = address_mappings; }
  
          bool operator==( const xtreemfs_address_mappings_setRequest& other ) const
          {
            return address_mappings == other.address_mappings;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_setRequest, 2010031019 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "address_mappings", 0, address_mappings );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "address_mappings", 0, address_mappings );
          }
  
        protected:
          org::xtreemfs::interfaces::AddressMappingSet address_mappings;
        };
  
        class xtreemfs_address_mappings_setResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_setResponse() : _return_value( 0 ) { }
          xtreemfs_address_mappings_setResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
          virtual ~xtreemfs_address_mappings_setResponse() {  }
  
          uint64_t get__return_value() const { return _return_value; }
          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
  
          bool operator==( const xtreemfs_address_mappings_setResponse& other ) const
          {
            return _return_value == other._return_value;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_setResponse, 2010031019 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "_return_value", 0, _return_value );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            _return_value = unmarshaller.read_uint64( "_return_value", 0 );
          }
  
        protected:
          uint64_t _return_value;
        };
  
        class xtreemfs_checkpointRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointRequest() { }
          virtual ~xtreemfs_checkpointRequest() {  }
  
          bool operator==( const xtreemfs_checkpointRequest& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_checkpointRequest, 2010031020 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_checkpointResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointResponse() { }
          virtual ~xtreemfs_checkpointResponse() {  }
  
          bool operator==( const xtreemfs_checkpointResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_checkpointResponse, 2010031020 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_discover_dirRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_discover_dirRequest() { }
          virtual ~xtreemfs_discover_dirRequest() {  }
  
          bool operator==( const xtreemfs_discover_dirRequest& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_discover_dirRequest, 2010031021 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_discover_dirResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_discover_dirResponse() { }
          xtreemfs_discover_dirResponse( const org::xtreemfs::interfaces::DirService& dir_service ) : dir_service( dir_service ) { }
          virtual ~xtreemfs_discover_dirResponse() {  }
  
          const org::xtreemfs::interfaces::DirService& get_dir_service() const { return dir_service; }
          void set_dir_service( const org::xtreemfs::interfaces::DirService&  dir_service ) { this->dir_service = dir_service; }
  
          bool operator==( const xtreemfs_discover_dirResponse& other ) const
          {
            return dir_service == other.dir_service;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_discover_dirResponse, 2010031021 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "dir_service", 0, dir_service );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "dir_service", 0, dir_service );
          }
  
        protected:
          org::xtreemfs::interfaces::DirService dir_service;
        };
  
        class xtreemfs_global_time_s_getRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_global_time_s_getRequest() { }
          virtual ~xtreemfs_global_time_s_getRequest() {  }
  
          bool operator==( const xtreemfs_global_time_s_getRequest& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_global_time_s_getRequest, 2010031022 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_global_time_s_getResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_global_time_s_getResponse() : _return_value( 0 ) { }
          xtreemfs_global_time_s_getResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
          virtual ~xtreemfs_global_time_s_getResponse() {  }
  
          uint64_t get__return_value() const { return _return_value; }
          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
  
          bool operator==( const xtreemfs_global_time_s_getResponse& other ) const
          {
            return _return_value == other._return_value;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_global_time_s_getResponse, 2010031022 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "_return_value", 0, _return_value );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            _return_value = unmarshaller.read_uint64( "_return_value", 0 );
          }
  
        protected:
          uint64_t _return_value;
        };
  
        class xtreemfs_replication_to_masterRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_replication_to_masterRequest() { }
          virtual ~xtreemfs_replication_to_masterRequest() {  }
  
          bool operator==( const xtreemfs_replication_to_masterRequest& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replication_to_masterRequest, 2010031023 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_replication_to_masterResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_replication_to_masterResponse() { }
          virtual ~xtreemfs_replication_to_masterResponse() {  }
  
          bool operator==( const xtreemfs_replication_to_masterResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replication_to_masterResponse, 2010031023 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_service_deregisterRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_deregisterRequest() { }
          xtreemfs_service_deregisterRequest( const string& uuid ) : uuid( uuid ) { }
          virtual ~xtreemfs_service_deregisterRequest() {  }
  
          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }
  
          bool operator==( const xtreemfs_service_deregisterRequest& other ) const
          {
            return uuid == other.uuid;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_deregisterRequest, 2010031028 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "uuid", 0, uuid );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "uuid", 0, uuid );
          }
  
        protected:
          string uuid;
        };
  
        class xtreemfs_service_deregisterResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_deregisterResponse() { }
          virtual ~xtreemfs_service_deregisterResponse() {  }
  
          bool operator==( const xtreemfs_service_deregisterResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_deregisterResponse, 2010031028 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_service_get_by_nameRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_nameRequest() { }
          xtreemfs_service_get_by_nameRequest( const string& name ) : name( name ) { }
          virtual ~xtreemfs_service_get_by_nameRequest() {  }
  
          const string& get_name() const { return name; }
          void set_name( const string& name ) { this->name = name; }
  
          bool operator==( const xtreemfs_service_get_by_nameRequest& other ) const
          {
            return name == other.name;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_nameRequest, 2010031026 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "name", 0, name );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "name", 0, name );
          }
  
        protected:
          string name;
        };
  
        class xtreemfs_service_get_by_nameResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_nameResponse() { }
          xtreemfs_service_get_by_nameResponse( const org::xtreemfs::interfaces::ServiceSet& services ) : services( services ) { }
          virtual ~xtreemfs_service_get_by_nameResponse() {  }
  
          const org::xtreemfs::interfaces::ServiceSet& get_services() const { return services; }
          void set_services( const org::xtreemfs::interfaces::ServiceSet&  services ) { this->services = services; }
  
          bool operator==( const xtreemfs_service_get_by_nameResponse& other ) const
          {
            return services == other.services;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_nameResponse, 2010031026 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "services", 0, services );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "services", 0, services );
          }
  
        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };
  
        class xtreemfs_service_get_by_typeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_typeRequest() : type( SERVICE_TYPE_MIXED ) { }
          xtreemfs_service_get_by_typeRequest( org::xtreemfs::interfaces::ServiceType type ) : type( type ) { }
          virtual ~xtreemfs_service_get_by_typeRequest() {  }
  
          org::xtreemfs::interfaces::ServiceType get_type() const { return type; }
          void set_type( org::xtreemfs::interfaces::ServiceType type ) { this->type = type; }
  
          bool operator==( const xtreemfs_service_get_by_typeRequest& other ) const
          {
            return type == other.type;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_typeRequest, 2010031024 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "type", 0, static_cast<int32_t>( type ) );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            type = static_cast<org::xtreemfs::interfaces::ServiceType>( unmarshaller.read_int32( "type", 0 ) );
          }
  
        protected:
          org::xtreemfs::interfaces::ServiceType type;
        };
  
        class xtreemfs_service_get_by_typeResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_typeResponse() { }
          xtreemfs_service_get_by_typeResponse( const org::xtreemfs::interfaces::ServiceSet& services ) : services( services ) { }
          virtual ~xtreemfs_service_get_by_typeResponse() {  }
  
          const org::xtreemfs::interfaces::ServiceSet& get_services() const { return services; }
          void set_services( const org::xtreemfs::interfaces::ServiceSet&  services ) { this->services = services; }
  
          bool operator==( const xtreemfs_service_get_by_typeResponse& other ) const
          {
            return services == other.services;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_typeResponse, 2010031024 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "services", 0, services );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "services", 0, services );
          }
  
        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };
  
        class xtreemfs_service_get_by_uuidRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_uuidRequest() { }
          xtreemfs_service_get_by_uuidRequest( const string& uuid ) : uuid( uuid ) { }
          virtual ~xtreemfs_service_get_by_uuidRequest() {  }
  
          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }
  
          bool operator==( const xtreemfs_service_get_by_uuidRequest& other ) const
          {
            return uuid == other.uuid;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_uuidRequest, 2010031025 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "uuid", 0, uuid );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "uuid", 0, uuid );
          }
  
        protected:
          string uuid;
        };
  
        class xtreemfs_service_get_by_uuidResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_uuidResponse() { }
          xtreemfs_service_get_by_uuidResponse( const org::xtreemfs::interfaces::ServiceSet& services ) : services( services ) { }
          virtual ~xtreemfs_service_get_by_uuidResponse() {  }
  
          const org::xtreemfs::interfaces::ServiceSet& get_services() const { return services; }
          void set_services( const org::xtreemfs::interfaces::ServiceSet&  services ) { this->services = services; }
  
          bool operator==( const xtreemfs_service_get_by_uuidResponse& other ) const
          {
            return services == other.services;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_uuidResponse, 2010031025 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "services", 0, services );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "services", 0, services );
          }
  
        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };
  
        class xtreemfs_service_offlineRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_offlineRequest() { }
          xtreemfs_service_offlineRequest( const string& uuid ) : uuid( uuid ) { }
          virtual ~xtreemfs_service_offlineRequest() {  }
  
          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }
  
          bool operator==( const xtreemfs_service_offlineRequest& other ) const
          {
            return uuid == other.uuid;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_offlineRequest, 2010031029 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "uuid", 0, uuid );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "uuid", 0, uuid );
          }
  
        protected:
          string uuid;
        };
  
        class xtreemfs_service_offlineResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_offlineResponse() { }
          virtual ~xtreemfs_service_offlineResponse() {  }
  
          bool operator==( const xtreemfs_service_offlineResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_offlineResponse, 2010031029 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_service_registerRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_registerRequest() { }
          xtreemfs_service_registerRequest( const org::xtreemfs::interfaces::Service& service ) : service( service ) { }
          virtual ~xtreemfs_service_registerRequest() {  }
  
          const org::xtreemfs::interfaces::Service& get_service() const { return service; }
          void set_service( const org::xtreemfs::interfaces::Service&  service ) { this->service = service; }
  
          bool operator==( const xtreemfs_service_registerRequest& other ) const
          {
            return service == other.service;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_registerRequest, 2010031027 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "service", 0, service );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "service", 0, service );
          }
  
        protected:
          org::xtreemfs::interfaces::Service service;
        };
  
        class xtreemfs_service_registerResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_registerResponse() : _return_value( 0 ) { }
          xtreemfs_service_registerResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
          virtual ~xtreemfs_service_registerResponse() {  }
  
          uint64_t get__return_value() const { return _return_value; }
          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
  
          bool operator==( const xtreemfs_service_registerResponse& other ) const
          {
            return _return_value == other._return_value;
          }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_registerResponse, 2010031027 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "_return_value", 0, _return_value );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            _return_value = unmarshaller.read_uint64( "_return_value", 0 );
          }
  
        protected:
          uint64_t _return_value;
        };
  
        class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownRequest() { }
          virtual ~xtreemfs_shutdownRequest() {  }
  
          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 2010031030 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownResponse() { }
          virtual ~xtreemfs_shutdownResponse() {  }
  
          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 2010031030 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };
  
        class ConcurrentModificationException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          ConcurrentModificationException() { }
          ConcurrentModificationException( const string& stack_trace ) : stack_trace( stack_trace ) { }
          ConcurrentModificationException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~ConcurrentModificationException() throw() { }
  
          const string& get_stack_trace() const { return stack_trace; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "stack_trace", 0, stack_trace );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "stack_trace", 0, stack_trace );
          }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const
          {
            return new ConcurrentModificationException( stack_trace );
          }
  
          virtual void throwStackClone() const
          {
            throw ConcurrentModificationException( stack_trace );
          }
  
        protected:
          string stack_trace;
        };
  
        class DIRException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          DIRException() : error_code( 0 ) { }
          DIRException( uint32_t error_code, const string& error_message, const string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
          DIRException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~DIRException() throw() { }
  
          uint32_t get_error_code() const { return error_code; }
          const string& get_error_message() const { return error_message; }
          const string& get_stack_trace() const { return stack_trace; }
          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          void set_error_message( const string& error_message ) { this->error_message = error_message; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "error_code", 0, error_code );
            marshaller.write( "error_message", 0, error_message );
            marshaller.write( "stack_trace", 0, stack_trace );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            error_code = unmarshaller.read_uint32( "error_code", 0 );
            unmarshaller.read( "error_message", 0, error_message );
            unmarshaller.read( "stack_trace", 0, stack_trace );
          }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const
          {
            return new DIRException( error_code, error_message, stack_trace );
          }
  
          virtual void throwStackClone() const
          {
            throw DIRException( error_code, error_message, stack_trace );
          }
  
        protected:
          uint32_t error_code;
          string error_message;
          string stack_trace;
        };
  
        class InvalidArgumentException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          InvalidArgumentException() { }
          InvalidArgumentException( const string& error_message ) : error_message( error_message ) { }
          InvalidArgumentException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~InvalidArgumentException() throw() { }
  
          const string& get_error_message() const { return error_message; }
          void set_error_message( const string& error_message ) { this->error_message = error_message; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "error_message", 0, error_message );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "error_message", 0, error_message );
          }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const
          {
            return new InvalidArgumentException( error_message );
          }
  
          virtual void throwStackClone() const
          {
            throw InvalidArgumentException( error_message );
          }
  
        protected:
          string error_message;
        };
  
        class ProtocolException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          ProtocolException() : accept_stat( 0 ), error_code( 0 ) { }
          ProtocolException( uint32_t accept_stat, uint32_t error_code, const string& stack_trace ) : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace ) { }
          ProtocolException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~ProtocolException() throw() { }
  
          uint32_t get_accept_stat() const { return accept_stat; }
          uint32_t get_error_code() const { return error_code; }
          const string& get_stack_trace() const { return stack_trace; }
          void set_accept_stat( uint32_t accept_stat ) { this->accept_stat = accept_stat; }
          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "accept_stat", 0, accept_stat );
            marshaller.write( "error_code", 0, error_code );
            marshaller.write( "stack_trace", 0, stack_trace );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            accept_stat = unmarshaller.read_uint32( "accept_stat", 0 );
            error_code = unmarshaller.read_uint32( "error_code", 0 );
            unmarshaller.read( "stack_trace", 0, stack_trace );
          }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const
          {
            return new ProtocolException( accept_stat, error_code, stack_trace );
          }
  
          virtual void throwStackClone() const
          {
            throw ProtocolException( accept_stat, error_code, stack_trace );
          }
  
        protected:
          uint32_t accept_stat;
          uint32_t error_code;
          string stack_trace;
        };
  
        class RedirectException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          RedirectException() : port( 0 ) { }
          RedirectException( const string& address, uint16_t port ) : address( address ), port( port ) { }
          RedirectException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~RedirectException() throw() { }
  
          const string& get_address() const { return address; }
          uint16_t get_port() const { return port; }
          void set_address( const string& address ) { this->address = address; }
          void set_port( uint16_t port ) { this->port = port; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( "address", 0, address );
            marshaller.write( "port", 0, port );
          }
  
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( "address", 0, address );
            port = unmarshaller.read_uint16( "port", 0 );
          }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const
          {
            return new RedirectException( address, port );
          }
  
          virtual void throwStackClone() const
          {
            throw RedirectException( address, port );
          }
  
        protected:
          string address;
          uint16_t port;
        };
      };
  
  
      class DIRInterfaceEventFactory
        : public ::yield::concurrency::EventFactory,
          private DIRInterfaceEvents
      {
      public:
        // yield::concurrency::EventFactory
        virtual ::yield::concurrency::ExceptionResponse*
        createExceptionResponse
        (
          uint32_t type_id
        )
        {
          switch ( type_id )
          {
            case 2010031036: return new ConcurrentModificationException;
            case 2010031039: return new DIRException;
            case 2010031037: return new InvalidArgumentException;
            case 2010031038: return new ProtocolException;
            case 2010031040: return new RedirectException;
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::ExceptionResponse*
        createExceptionResponse
        (
          const char* type_name
        )
        {
          if ( strcmp( type_name, "ConcurrentModificationException" ) == 0 ) return new ConcurrentModificationException;
          else if ( strcmp( type_name, "DIRException" ) == 0 ) return new DIRException;
          else if ( strcmp( type_name, "InvalidArgumentException" ) == 0 ) return new InvalidArgumentException;
          else if ( strcmp( type_name, "ProtocolException" ) == 0 ) return new ProtocolException;
          else if ( strcmp( type_name, "RedirectException" ) == 0 ) return new RedirectException;
          else return NULL;
        }
  
        virtual ::yield::concurrency::Request* createRequest( uint32_t type_id )
        {
          switch ( type_id )
          {
            case 2010031017: return new xtreemfs_address_mappings_getRequest;
            case 2010031018: return new xtreemfs_address_mappings_removeRequest;
            case 2010031019: return new xtreemfs_address_mappings_setRequest;
            case 2010031020: return new xtreemfs_checkpointRequest;
            case 2010031021: return new xtreemfs_discover_dirRequest;
            case 2010031022: return new xtreemfs_global_time_s_getRequest;
            case 2010031023: return new xtreemfs_replication_to_masterRequest;
            case 2010031028: return new xtreemfs_service_deregisterRequest;
            case 2010031026: return new xtreemfs_service_get_by_nameRequest;
            case 2010031024: return new xtreemfs_service_get_by_typeRequest;
            case 2010031025: return new xtreemfs_service_get_by_uuidRequest;
            case 2010031029: return new xtreemfs_service_offlineRequest;
            case 2010031027: return new xtreemfs_service_registerRequest;
            case 2010031030: return new xtreemfs_shutdownRequest;
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::Request* createRequest( const char* type_name )
        {
          if ( strcmp( type_name, "xtreemfs_address_mappings_getRequest" ) == 0 ) return new xtreemfs_address_mappings_getRequest;
          else if ( strcmp( type_name, "xtreemfs_address_mappings_removeRequest" ) == 0 ) return new xtreemfs_address_mappings_removeRequest;
          else if ( strcmp( type_name, "xtreemfs_address_mappings_setRequest" ) == 0 ) return new xtreemfs_address_mappings_setRequest;
          else if ( strcmp( type_name, "xtreemfs_checkpointRequest" ) == 0 ) return new xtreemfs_checkpointRequest;
          else if ( strcmp( type_name, "xtreemfs_discover_dirRequest" ) == 0 ) return new xtreemfs_discover_dirRequest;
          else if ( strcmp( type_name, "xtreemfs_global_time_s_getRequest" ) == 0 ) return new xtreemfs_global_time_s_getRequest;
          else if ( strcmp( type_name, "xtreemfs_replication_to_masterRequest" ) == 0 ) return new xtreemfs_replication_to_masterRequest;
          else if ( strcmp( type_name, "xtreemfs_service_deregisterRequest" ) == 0 ) return new xtreemfs_service_deregisterRequest;
          else if ( strcmp( type_name, "xtreemfs_service_get_by_nameRequest" ) == 0 ) return new xtreemfs_service_get_by_nameRequest;
          else if ( strcmp( type_name, "xtreemfs_service_get_by_typeRequest" ) == 0 ) return new xtreemfs_service_get_by_typeRequest;
          else if ( strcmp( type_name, "xtreemfs_service_get_by_uuidRequest" ) == 0 ) return new xtreemfs_service_get_by_uuidRequest;
          else if ( strcmp( type_name, "xtreemfs_service_offlineRequest" ) == 0 ) return new xtreemfs_service_offlineRequest;
          else if ( strcmp( type_name, "xtreemfs_service_registerRequest" ) == 0 ) return new xtreemfs_service_registerRequest;
          else if ( strcmp( type_name, "xtreemfs_shutdownRequest" ) == 0 ) return new xtreemfs_shutdownRequest;
          else return NULL;
        }
  
        virtual ::yield::concurrency::Response* createResponse( uint32_t type_id )
        {
          switch ( type_id )
          {
            case 2010031017: return new xtreemfs_address_mappings_getResponse;
            case 2010031018: return new xtreemfs_address_mappings_removeResponse;
            case 2010031019: return new xtreemfs_address_mappings_setResponse;
            case 2010031020: return new xtreemfs_checkpointResponse;
            case 2010031021: return new xtreemfs_discover_dirResponse;
            case 2010031022: return new xtreemfs_global_time_s_getResponse;
            case 2010031023: return new xtreemfs_replication_to_masterResponse;
            case 2010031028: return new xtreemfs_service_deregisterResponse;
            case 2010031026: return new xtreemfs_service_get_by_nameResponse;
            case 2010031024: return new xtreemfs_service_get_by_typeResponse;
            case 2010031025: return new xtreemfs_service_get_by_uuidResponse;
            case 2010031029: return new xtreemfs_service_offlineResponse;
            case 2010031027: return new xtreemfs_service_registerResponse;
            case 2010031030: return new xtreemfs_shutdownResponse;
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::Response* createResponse( const char* type_name )
        {
          if ( strcmp( type_name, "xtreemfs_address_mappings_getResponse" ) == 0 ) return new xtreemfs_address_mappings_getResponse;
          else if ( strcmp( type_name, "xtreemfs_address_mappings_removeResponse" ) == 0 ) return new xtreemfs_address_mappings_removeResponse;
          else if ( strcmp( type_name, "xtreemfs_address_mappings_setResponse" ) == 0 ) return new xtreemfs_address_mappings_setResponse;
          else if ( strcmp( type_name, "xtreemfs_checkpointResponse" ) == 0 ) return new xtreemfs_checkpointResponse;
          else if ( strcmp( type_name, "xtreemfs_discover_dirResponse" ) == 0 ) return new xtreemfs_discover_dirResponse;
          else if ( strcmp( type_name, "xtreemfs_global_time_s_getResponse" ) == 0 ) return new xtreemfs_global_time_s_getResponse;
          else if ( strcmp( type_name, "xtreemfs_replication_to_masterResponse" ) == 0 ) return new xtreemfs_replication_to_masterResponse;
          else if ( strcmp( type_name, "xtreemfs_service_deregisterResponse" ) == 0 ) return new xtreemfs_service_deregisterResponse;
          else if ( strcmp( type_name, "xtreemfs_service_get_by_nameResponse" ) == 0 ) return new xtreemfs_service_get_by_nameResponse;
          else if ( strcmp( type_name, "xtreemfs_service_get_by_typeResponse" ) == 0 ) return new xtreemfs_service_get_by_typeResponse;
          else if ( strcmp( type_name, "xtreemfs_service_get_by_uuidResponse" ) == 0 ) return new xtreemfs_service_get_by_uuidResponse;
          else if ( strcmp( type_name, "xtreemfs_service_offlineResponse" ) == 0 ) return new xtreemfs_service_offlineResponse;
          else if ( strcmp( type_name, "xtreemfs_service_registerResponse" ) == 0 ) return new xtreemfs_service_registerResponse;
          else if ( strcmp( type_name, "xtreemfs_shutdownResponse" ) == 0 ) return new xtreemfs_shutdownResponse;
          else return NULL;
        }
  
        virtual ::yield::concurrency::ExceptionResponse*
        isExceptionResponse
        (
          ::yidl::runtime::MarshallableObject& marshallable_object
        ) const
        {
          switch ( marshallable_object.get_type_id() )
          {
            case 2010031036: return static_cast<ConcurrentModificationException*>( &marshallable_object );
            case 2010031039: return static_cast<DIRException*>( &marshallable_object );
            case 2010031037: return static_cast<InvalidArgumentException*>( &marshallable_object );
            case 2010031038: return static_cast<ProtocolException*>( &marshallable_object );
            case 2010031040: return static_cast<RedirectException*>( &marshallable_object );
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::Request*
        isRequest
        (
          ::yidl::runtime::MarshallableObject& marshallable_object
        ) const
        {
          switch ( marshallable_object.get_type_id() )
          {
            case 2010031017: return static_cast<xtreemfs_address_mappings_getRequest*>( &marshallable_object );
            case 2010031018: return static_cast<xtreemfs_address_mappings_removeRequest*>( &marshallable_object );
            case 2010031019: return static_cast<xtreemfs_address_mappings_setRequest*>( &marshallable_object );
            case 2010031020: return static_cast<xtreemfs_checkpointRequest*>( &marshallable_object );
            case 2010031021: return static_cast<xtreemfs_discover_dirRequest*>( &marshallable_object );
            case 2010031022: return static_cast<xtreemfs_global_time_s_getRequest*>( &marshallable_object );
            case 2010031023: return static_cast<xtreemfs_replication_to_masterRequest*>( &marshallable_object );
            case 2010031028: return static_cast<xtreemfs_service_deregisterRequest*>( &marshallable_object );
            case 2010031026: return static_cast<xtreemfs_service_get_by_nameRequest*>( &marshallable_object );
            case 2010031024: return static_cast<xtreemfs_service_get_by_typeRequest*>( &marshallable_object );
            case 2010031025: return static_cast<xtreemfs_service_get_by_uuidRequest*>( &marshallable_object );
            case 2010031029: return static_cast<xtreemfs_service_offlineRequest*>( &marshallable_object );
            case 2010031027: return static_cast<xtreemfs_service_registerRequest*>( &marshallable_object );
            case 2010031030: return static_cast<xtreemfs_shutdownRequest*>( &marshallable_object );
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::Response*
        isResponse
        (
          ::yidl::runtime::MarshallableObject& marshallable_object
        ) const
        {
          switch ( marshallable_object.get_type_id() )
          {
            case 2010031017: return static_cast<xtreemfs_address_mappings_getResponse*>( &marshallable_object );
            case 2010031018: return static_cast<xtreemfs_address_mappings_removeResponse*>( &marshallable_object );
            case 2010031019: return static_cast<xtreemfs_address_mappings_setResponse*>( &marshallable_object );
            case 2010031020: return static_cast<xtreemfs_checkpointResponse*>( &marshallable_object );
            case 2010031021: return static_cast<xtreemfs_discover_dirResponse*>( &marshallable_object );
            case 2010031022: return static_cast<xtreemfs_global_time_s_getResponse*>( &marshallable_object );
            case 2010031023: return static_cast<xtreemfs_replication_to_masterResponse*>( &marshallable_object );
            case 2010031028: return static_cast<xtreemfs_service_deregisterResponse*>( &marshallable_object );
            case 2010031026: return static_cast<xtreemfs_service_get_by_nameResponse*>( &marshallable_object );
            case 2010031024: return static_cast<xtreemfs_service_get_by_typeResponse*>( &marshallable_object );
            case 2010031025: return static_cast<xtreemfs_service_get_by_uuidResponse*>( &marshallable_object );
            case 2010031029: return static_cast<xtreemfs_service_offlineResponse*>( &marshallable_object );
            case 2010031027: return static_cast<xtreemfs_service_registerResponse*>( &marshallable_object );
            case 2010031030: return static_cast<xtreemfs_shutdownResponse*>( &marshallable_object );
            default: return NULL;
          }
        }
  
      };
  
  
      class DIRInterfaceEventHandler
        : public ::yield::concurrency::EventHandler,
          protected DIRInterfaceEvents
      {
      public:
        DIRInterfaceEventHandler()  // Subclasses must implement
          : _interface( NULL ) // all relevant handle*Request methods
        { }
  
        // Steals interface_ to allow for *new
        DIRInterfaceEventHandler( DIRInterface& _interface )
          : _interface( &_interface )
        { }
  
        virtual ~DIRInterfaceEventHandler()
        {
          delete _interface;
        }
  
        // yield::concurrency::EventHandler
        virtual const char* get_event_handler_name() const
        {
          return "DIRInterface";
        }
  
        virtual void handleEvent( ::yield::concurrency::Event& event )
        {
          // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to _interface
          switch ( event.get_type_id() )
          {
            case 2010031017UL: handlextreemfs_address_mappings_getRequest( static_cast<xtreemfs_address_mappings_getRequest&>( event ) ); return;
            case 2010031018UL: handlextreemfs_address_mappings_removeRequest( static_cast<xtreemfs_address_mappings_removeRequest&>( event ) ); return;
            case 2010031019UL: handlextreemfs_address_mappings_setRequest( static_cast<xtreemfs_address_mappings_setRequest&>( event ) ); return;
            case 2010031020UL: handlextreemfs_checkpointRequest( static_cast<xtreemfs_checkpointRequest&>( event ) ); return;
            case 2010031021UL: handlextreemfs_discover_dirRequest( static_cast<xtreemfs_discover_dirRequest&>( event ) ); return;
            case 2010031022UL: handlextreemfs_global_time_s_getRequest( static_cast<xtreemfs_global_time_s_getRequest&>( event ) ); return;
            case 2010031023UL: handlextreemfs_replication_to_masterRequest( static_cast<xtreemfs_replication_to_masterRequest&>( event ) ); return;
            case 2010031028UL: handlextreemfs_service_deregisterRequest( static_cast<xtreemfs_service_deregisterRequest&>( event ) ); return;
            case 2010031026UL: handlextreemfs_service_get_by_nameRequest( static_cast<xtreemfs_service_get_by_nameRequest&>( event ) ); return;
            case 2010031024UL: handlextreemfs_service_get_by_typeRequest( static_cast<xtreemfs_service_get_by_typeRequest&>( event ) ); return;
            case 2010031025UL: handlextreemfs_service_get_by_uuidRequest( static_cast<xtreemfs_service_get_by_uuidRequest&>( event ) ); return;
            case 2010031029UL: handlextreemfs_service_offlineRequest( static_cast<xtreemfs_service_offlineRequest&>( event ) ); return;
            case 2010031027UL: handlextreemfs_service_registerRequest( static_cast<xtreemfs_service_registerRequest&>( event ) ); return;
            case 2010031030UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( event ) ); return;
            default: ::yield::concurrency::Event::dec_ref( event ); return;
          }
        }
  
      protected:
        virtual void handlextreemfs_address_mappings_getRequest( xtreemfs_address_mappings_getRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_address_mappings_getResponse> response( new xtreemfs_address_mappings_getResponse );
              org::xtreemfs::interfaces::AddressMappingSet address_mappings;
              _interface->xtreemfs_address_mappings_get( __request.get_uuid(), address_mappings );
              response->set_address_mappings( address_mappings );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_address_mappings_getRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_address_mappings_removeRequest( xtreemfs_address_mappings_removeRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_address_mappings_removeResponse> response( new xtreemfs_address_mappings_removeResponse );
              _interface->xtreemfs_address_mappings_remove( __request.get_uuid() );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_address_mappings_removeRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_address_mappings_setRequest( xtreemfs_address_mappings_setRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_address_mappings_setResponse> response( new xtreemfs_address_mappings_setResponse );
              uint64_t _return_value =
              _interface->xtreemfs_address_mappings_set( __request.get_address_mappings() );
              response->set__return_value( _return_value );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_address_mappings_setRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> response( new xtreemfs_checkpointResponse );
              _interface->xtreemfs_checkpoint();
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_checkpointRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_discover_dirRequest( xtreemfs_discover_dirRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_discover_dirResponse> response( new xtreemfs_discover_dirResponse );
              org::xtreemfs::interfaces::DirService dir_service;
              _interface->xtreemfs_discover_dir( dir_service );
              response->set_dir_service( dir_service );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_discover_dirRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_global_time_s_getRequest( xtreemfs_global_time_s_getRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_global_time_s_getResponse> response( new xtreemfs_global_time_s_getResponse );
              uint64_t _return_value =
              _interface->xtreemfs_global_time_s_get();
              response->set__return_value( _return_value );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_global_time_s_getRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_replication_to_masterRequest( xtreemfs_replication_to_masterRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> response( new xtreemfs_replication_to_masterResponse );
              _interface->xtreemfs_replication_to_master();
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_replication_to_masterRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_service_deregisterRequest( xtreemfs_service_deregisterRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_service_deregisterResponse> response( new xtreemfs_service_deregisterResponse );
              _interface->xtreemfs_service_deregister( __request.get_uuid() );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_service_deregisterRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_service_get_by_nameRequest( xtreemfs_service_get_by_nameRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_service_get_by_nameResponse> response( new xtreemfs_service_get_by_nameResponse );
              org::xtreemfs::interfaces::ServiceSet services;
              _interface->xtreemfs_service_get_by_name( __request.get_name(), services );
              response->set_services( services );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_service_get_by_nameRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_service_get_by_typeRequest( xtreemfs_service_get_by_typeRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_service_get_by_typeResponse> response( new xtreemfs_service_get_by_typeResponse );
              org::xtreemfs::interfaces::ServiceSet services;
              _interface->xtreemfs_service_get_by_type( __request.get_type(), services );
              response->set_services( services );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_service_get_by_typeRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_service_get_by_uuidRequest( xtreemfs_service_get_by_uuidRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_service_get_by_uuidResponse> response( new xtreemfs_service_get_by_uuidResponse );
              org::xtreemfs::interfaces::ServiceSet services;
              _interface->xtreemfs_service_get_by_uuid( __request.get_uuid(), services );
              response->set_services( services );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_service_get_by_uuidRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_service_offlineRequest( xtreemfs_service_offlineRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_service_offlineResponse> response( new xtreemfs_service_offlineResponse );
              _interface->xtreemfs_service_offline( __request.get_uuid() );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_service_offlineRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_service_registerRequest( xtreemfs_service_registerRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_service_registerResponse> response( new xtreemfs_service_registerResponse );
              uint64_t _return_value =
              _interface->xtreemfs_service_register( __request.get_service() );
              response->set__return_value( _return_value );
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_service_registerRequest::dec_ref( __request );
        }
  
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> response( new xtreemfs_shutdownResponse );
              _interface->xtreemfs_shutdown();
              __request.respond( response->inc_ref() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }
  
          xtreemfs_shutdownRequest::dec_ref( __request );
        }
  
      private:
        DIRInterface* _interface;
      };
  
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EVENT_HANDLER_PROTOTYPES \
      virtual void handlextreemfs_address_mappings_getRequest( xtreemfs_address_mappings_getRequest& __request );\
      virtual void handlextreemfs_address_mappings_removeRequest( xtreemfs_address_mappings_removeRequest& __request );\
      virtual void handlextreemfs_address_mappings_setRequest( xtreemfs_address_mappings_setRequest& __request );\
      virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& __request );\
      virtual void handlextreemfs_discover_dirRequest( xtreemfs_discover_dirRequest& __request );\
      virtual void handlextreemfs_global_time_s_getRequest( xtreemfs_global_time_s_getRequest& __request );\
      virtual void handlextreemfs_replication_to_masterRequest( xtreemfs_replication_to_masterRequest& __request );\
      virtual void handlextreemfs_service_deregisterRequest( xtreemfs_service_deregisterRequest& __request );\
      virtual void handlextreemfs_service_get_by_nameRequest( xtreemfs_service_get_by_nameRequest& __request );\
      virtual void handlextreemfs_service_get_by_typeRequest( xtreemfs_service_get_by_typeRequest& __request );\
      virtual void handlextreemfs_service_get_by_uuidRequest( xtreemfs_service_get_by_uuidRequest& __request );\
      virtual void handlextreemfs_service_offlineRequest( xtreemfs_service_offlineRequest& __request );\
      virtual void handlextreemfs_service_registerRequest( xtreemfs_service_registerRequest& __request );\
      virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& __request );
  
  
      class DIRInterfaceEventSender
        : public DIRInterface,
          private DIRInterfaceEvents
      {
      public:
        DIRInterfaceEventSender( ::yield::concurrency::EventTarget& event_target )
          : event_target( event_target.inc_ref() )
        { }
  
        virtual ~DIRInterfaceEventSender()
        {
          ::yield::concurrency::EventTarget::dec_ref( event_target );
        }
          virtual void xtreemfs_address_mappings_get( const string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings )
          {
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_getRequest> __request( new xtreemfs_address_mappings_getRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_getResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_getResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_getResponse> __response = __response_queue->dequeue();address_mappings = __response->get_address_mappings();
          }
  
          virtual void xtreemfs_address_mappings_get( const string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_getRequest> __request( new xtreemfs_address_mappings_getRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_getResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_getResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_getResponse> __response = __response_queue->dequeue( response_timeout_ns );address_mappings = __response->get_address_mappings();
          }
  
          virtual void xtreemfs_address_mappings_remove( const string& uuid )
          {
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_removeRequest> __request( new xtreemfs_address_mappings_removeRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_removeResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_removeResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_removeResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_address_mappings_remove( const string& uuid, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_removeRequest> __request( new xtreemfs_address_mappings_removeRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_removeResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_removeResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_removeResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings )
          {
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_setRequest> __request( new xtreemfs_address_mappings_setRequest( address_mappings ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_setResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_setResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_setResponse> __response = __response_queue->dequeue();uint64_t _return_value = __response->get__return_value(); return _return_value;
          }
  
          virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_setRequest> __request( new xtreemfs_address_mappings_setRequest( address_mappings ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_setResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_setResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_address_mappings_setResponse> __response = __response_queue->dequeue( response_timeout_ns );uint64_t _return_value = __response->get__return_value(); return _return_value;
          }
  
          virtual void xtreemfs_checkpoint()
          {
            ::yidl::runtime::auto_Object<xtreemfs_checkpointRequest> __request( new xtreemfs_checkpointRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_checkpoint( uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_checkpointRequest> __request( new xtreemfs_checkpointRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_discover_dir( org::xtreemfs::interfaces::DirService& dir_service )
          {
            ::yidl::runtime::auto_Object<xtreemfs_discover_dirRequest> __request( new xtreemfs_discover_dirRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_discover_dirResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_discover_dirResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_discover_dirResponse> __response = __response_queue->dequeue();dir_service = __response->get_dir_service();
          }
  
          virtual void xtreemfs_discover_dir( org::xtreemfs::interfaces::DirService& dir_service, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_discover_dirRequest> __request( new xtreemfs_discover_dirRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_discover_dirResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_discover_dirResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_discover_dirResponse> __response = __response_queue->dequeue( response_timeout_ns );dir_service = __response->get_dir_service();
          }
  
          virtual uint64_t xtreemfs_global_time_s_get()
          {
            ::yidl::runtime::auto_Object<xtreemfs_global_time_s_getRequest> __request( new xtreemfs_global_time_s_getRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_global_time_s_getResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_global_time_s_getResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_global_time_s_getResponse> __response = __response_queue->dequeue();uint64_t _return_value = __response->get__return_value(); return _return_value;
          }
  
          virtual uint64_t xtreemfs_global_time_s_get( uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_global_time_s_getRequest> __request( new xtreemfs_global_time_s_getRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_global_time_s_getResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_global_time_s_getResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_global_time_s_getResponse> __response = __response_queue->dequeue( response_timeout_ns );uint64_t _return_value = __response->get__return_value(); return _return_value;
          }
  
          virtual void xtreemfs_replication_to_master()
          {
            ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterRequest> __request( new xtreemfs_replication_to_masterRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_replication_to_master( uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterRequest> __request( new xtreemfs_replication_to_masterRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_service_deregister( const string& uuid )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_deregisterRequest> __request( new xtreemfs_service_deregisterRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_deregisterResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_deregisterResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_deregisterResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_service_deregister( const string& uuid, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_deregisterRequest> __request( new xtreemfs_service_deregisterRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_deregisterResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_deregisterResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_deregisterResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_service_get_by_name( const string& name, org::xtreemfs::interfaces::ServiceSet& services )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_nameRequest> __request( new xtreemfs_service_get_by_nameRequest( name ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_nameResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_nameResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_nameResponse> __response = __response_queue->dequeue();services = __response->get_services();
          }
  
          virtual void xtreemfs_service_get_by_name( const string& name, org::xtreemfs::interfaces::ServiceSet& services, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_nameRequest> __request( new xtreemfs_service_get_by_nameRequest( name ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_nameResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_nameResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_nameResponse> __response = __response_queue->dequeue( response_timeout_ns );services = __response->get_services();
          }
  
          virtual void xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_typeRequest> __request( new xtreemfs_service_get_by_typeRequest( type ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_typeResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_typeResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_typeResponse> __response = __response_queue->dequeue();services = __response->get_services();
          }
  
          virtual void xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_typeRequest> __request( new xtreemfs_service_get_by_typeRequest( type ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_typeResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_typeResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_typeResponse> __response = __response_queue->dequeue( response_timeout_ns );services = __response->get_services();
          }
  
          virtual void xtreemfs_service_get_by_uuid( const string& uuid, org::xtreemfs::interfaces::ServiceSet& services )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_uuidRequest> __request( new xtreemfs_service_get_by_uuidRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_uuidResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_uuidResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_uuidResponse> __response = __response_queue->dequeue();services = __response->get_services();
          }
  
          virtual void xtreemfs_service_get_by_uuid( const string& uuid, org::xtreemfs::interfaces::ServiceSet& services, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_uuidRequest> __request( new xtreemfs_service_get_by_uuidRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_uuidResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_uuidResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_get_by_uuidResponse> __response = __response_queue->dequeue( response_timeout_ns );services = __response->get_services();
          }
  
          virtual void xtreemfs_service_offline( const string& uuid )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_offlineRequest> __request( new xtreemfs_service_offlineRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_offlineResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_offlineResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_offlineResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_service_offline( const string& uuid, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_offlineRequest> __request( new xtreemfs_service_offlineRequest( uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_offlineResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_offlineResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_offlineResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_registerRequest> __request( new xtreemfs_service_registerRequest( service ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_registerResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_registerResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_registerResponse> __response = __response_queue->dequeue();uint64_t _return_value = __response->get__return_value(); return _return_value;
          }
  
          virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_service_registerRequest> __request( new xtreemfs_service_registerRequest( service ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_registerResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_registerResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_service_registerResponse> __response = __response_queue->dequeue( response_timeout_ns );uint64_t _return_value = __response->get__return_value(); return _return_value;
          }
  
          virtual void xtreemfs_shutdown()
          {
            ::yidl::runtime::auto_Object<xtreemfs_shutdownRequest> __request( new xtreemfs_shutdownRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_shutdown( uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_shutdownRequest> __request( new xtreemfs_shutdownRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
      private:
        ::yield::concurrency::EventTarget& event_target;
      };
      };
    };
  };
#endif
