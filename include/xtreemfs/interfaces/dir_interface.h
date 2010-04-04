#ifndef _2114912582_H_
#define _2114912582_H_


#include "constants.h"
#include "types.h"
#include "yield/concurrency.h"
#include "yidl.h"


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
        AddressMapping()
          : version( 0 ), port( 0 ), ttl_s( 0 )
        { }

        AddressMapping
        (
          const string& uuid,
          uint64_t version,
          const string& protocol,
          const string& address,
          uint16_t port,
          const string& match_network,
          uint32_t ttl_s,
          const string& uri
        )
          : uuid( uuid ),
            version( version ),
            protocol( protocol ),
            address( address ),
            port( port ),
            match_network( match_network ),
            ttl_s( ttl_s ),
            uri( uri )
        { }

        AddressMapping( const AddressMapping& other )
          : uuid( other.get_uuid() ),
            version( other.get_version() ),
            protocol( other.get_protocol() ),
            address( other.get_address() ),
            port( other.get_port() ),
            match_network( other.get_match_network() ),
            ttl_s( other.get_ttl_s() ),
            uri( other.get_uri() )
        { }

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
          return get_uuid() == other.get_uuid()
                 &&
                 get_version() == other.get_version()
                 &&
                 get_protocol() == other.get_protocol()
                 &&
                 get_address() == other.get_address()
                 &&
                 get_port() == other.get_port()
                 &&
                 get_match_network() == other.get_match_network()
                 &&
                 get_ttl_s() == other.get_ttl_s()
                 &&
                 get_uri() == other.get_uri();
        }

        // yidl::runtime::Object
        AddressMapping& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( AddressMapping, 2010030946 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "uuid", 0 ), get_uuid() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "version", 0 ), get_version() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "protocol", 0 ), get_protocol() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "address", 0 ), get_address() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "port", 0 ), get_port() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "match_network", 0 ), get_match_network() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "ttl_s", 0 ), get_ttl_s() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "uri", 0 ), get_uri() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "uuid", 0 ), uuid );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "version", 0 ), version );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "protocol", 0 ), protocol );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "address", 0 ), address );
          port = unmarshaller.read_uint16( ::yidl::runtime::Unmarshaller::StringLiteralKey( "port", 0 ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "match_network", 0 ), match_network );
          ttl_s = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "ttl_s", 0 ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "uri", 0 ), uri );
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

        // yidl::runtime::Object
        AddressMappingSet& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( AddressMappingSet, 2010030947 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          size_type value_i_max = size();
          for ( size_type value_i = 0; value_i < value_i_max; value_i++ )
          {
            marshaller.write( ::yidl::runtime::Marshaller::Key(), ( *this )[value_i] );
          }
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          org::xtreemfs::interfaces::AddressMapping value;
          unmarshaller.read( ::yidl::runtime::Marshaller::Key(), value );
          push_back( value );
        }

        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };

      class ServiceDataMap
          : public ::yidl::runtime::Map,
            public map< ::yidl::runtime::Marshaller::StringKey,string >
      {
      public:
        virtual ~ServiceDataMap() { }

          // yidl::runtime::Object
        ServiceDataMap& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ServiceDataMap, 2010030948 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          for ( const_iterator i = begin(); i != end(); i++ )
          {
            marshaller.write( i->first, i->second );
          }
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          ::yidl::runtime::Marshaller::StringKey* key
            = static_cast< ::yidl::runtime::Marshaller::StringKey* >
              (
                unmarshaller.read( ::yidl::runtime::Marshaller::Key::TYPE_STRING )
              );

          if ( key != NULL )
          {
            string value;
            unmarshaller.read( *key, value );
            ( *this )[*key] = value;
            ::yidl::runtime::Unmarshaller::Key::dec_ref( *key );
          }
        }

        // yidl::runtime::Map
        size_t get_size() const { return size(); }
      };

      class Service : public ::yidl::runtime::Struct
      {
      public:
        Service()
          : type( SERVICE_TYPE_MIXED ), version( 0 ), last_updated_s( 0 )
        { }

        Service
        (
          org::xtreemfs::interfaces::ServiceType type,
          const string& uuid,
          uint64_t version,
          const string& name,
          uint64_t last_updated_s,
          const org::xtreemfs::interfaces::ServiceDataMap& data
        )
          : type( type ),
            uuid( uuid ),
            version( version ),
            name( name ),
            last_updated_s( last_updated_s ),
            data( data )
        { }

        Service( const Service& other )
          : type( other.get_type() ),
            uuid( other.get_uuid() ),
            version( other.get_version() ),
            name( other.get_name() ),
            last_updated_s( other.get_last_updated_s() ),
            data( other.get_data() )
        { }

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
          return get_type() == other.get_type()
                 &&
                 get_uuid() == other.get_uuid()
                 &&
                 get_version() == other.get_version()
                 &&
                 get_name() == other.get_name()
                 &&
                 get_last_updated_s() == other.get_last_updated_s()
                 &&
                 get_data() == other.get_data();
        }

        // yidl::runtime::Object
        Service& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Service, 2010030950 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "type", 0 ), static_cast<int32_t>( get_type() ) );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "uuid", 0 ), get_uuid() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "version", 0 ), get_version() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "name", 0 ), get_name() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "last_updated_s", 0 ), get_last_updated_s() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), get_data() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          type = static_cast<org::xtreemfs::interfaces::ServiceType>( unmarshaller.read_int32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "type", 0 ) ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "uuid", 0 ), uuid );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "version", 0 ), version );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "name", 0 ), name );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "last_updated_s", 0 ), last_updated_s );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), data );
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

        // yidl::runtime::Object
        ServiceSet& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ServiceSet, 2010030951 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          size_type value_i_max = size();
          for ( size_type value_i = 0; value_i < value_i_max; value_i++ )
          {
            marshaller.write( ::yidl::runtime::Marshaller::Key(), ( *this )[value_i] );
          }
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          org::xtreemfs::interfaces::Service value;
          unmarshaller.read( ::yidl::runtime::Marshaller::Key(), value );
          push_back( value );
        }

        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };

      class DirService : public ::yidl::runtime::Struct
      {
      public:
        DirService()
          : port( 0 ), interface_version( 0 )
        { }

        DirService
        (
          const string& address,
          uint16_t port,
          const string& protocol,
          uint32_t interface_version
        )
          : address( address ),
            port( port ),
            protocol( protocol ),
            interface_version( interface_version )
        { }

        DirService( const DirService& other )
          : address( other.get_address() ),
            port( other.get_port() ),
            protocol( other.get_protocol() ),
            interface_version( other.get_interface_version() )
        { }

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
          return get_address() == other.get_address()
                 &&
                 get_port() == other.get_port()
                 &&
                 get_protocol() == other.get_protocol()
                 &&
                 get_interface_version() == other.get_interface_version();
        }

        // yidl::runtime::Object
        DirService& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( DirService, 2010030951 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "address", 0 ), get_address() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "port", 0 ), get_port() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "protocol", 0 ), get_protocol() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "interface_version", 0 ), get_interface_version() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "address", 0 ), address );
          port = unmarshaller.read_uint16( ::yidl::runtime::Unmarshaller::StringLiteralKey( "port", 0 ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "protocol", 0 ), protocol );
          interface_version = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "interface_version", 0 ) );
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
        const static uint32_t HTTP_PORT_DEFAULT = 30638;
        const static uint32_t ONC_RPC_PORT_DEFAULT = 32638;const static uint32_t TAG = 2010031016;

        virtual ~DIRInterface() { }

        uint32_t get_tag() const { return 2010031016; }



        virtual void
        xtreemfs_address_mappings_get
        (
          const string& uuid,
          org::xtreemfs::interfaces::AddressMappingSet& address_mappings
        )
        { }

        virtual void xtreemfs_address_mappings_remove( const string& uuid ) { }

        virtual uint64_t
        xtreemfs_address_mappings_set
        (
          const org::xtreemfs::interfaces::AddressMappingSet& address_mappings
        ){
          return 0;
        }

        virtual void xtreemfs_checkpoint() { }

        virtual void
        xtreemfs_discover_dir
        (
          org::xtreemfs::interfaces::DirService& dir_service
        )
        { }

        virtual uint64_t xtreemfs_global_time_s_get(){
          return 0;
        }

        virtual void xtreemfs_service_deregister( const string& uuid ) { }

        virtual void
        xtreemfs_service_get_by_name
        (
          const string& name,
          org::xtreemfs::interfaces::ServiceSet& services
        )
        { }

        virtual void
        xtreemfs_service_get_by_type
        (
          org::xtreemfs::interfaces::ServiceType type,
          org::xtreemfs::interfaces::ServiceSet& services
        )
        { }

        virtual void
        xtreemfs_service_get_by_uuid
        (
          const string& uuid,
          org::xtreemfs::interfaces::ServiceSet& services
        )
        { }

        virtual void xtreemfs_service_offline( const string& uuid ) { }

        virtual uint64_t
        xtreemfs_service_register
        (
          const org::xtreemfs::interfaces::Service& service
        ){
          return 0;
        }

        virtual void xtreemfs_shutdown() { }
      };


      // Use this macro in an implementation class to get all of the prototypes for the operations in DIRInterface
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_PROTOTYPES\
      virtual void\
      xtreemfs_address_mappings_get\
      (\
        const string& uuid,\
        org::xtreemfs::interfaces::AddressMappingSet& address_mappings\
      );\
      virtual void xtreemfs_address_mappings_remove( const string& uuid );\
      virtual uint64_t\
      xtreemfs_address_mappings_set\
      (\
        const org::xtreemfs::interfaces::AddressMappingSet& address_mappings\
      );\
      virtual void xtreemfs_checkpoint();\
      virtual void\
      xtreemfs_discover_dir\
      (\
        org::xtreemfs::interfaces::DirService& dir_service\
      );\
      virtual uint64_t xtreemfs_global_time_s_get();\
      virtual void xtreemfs_service_deregister( const string& uuid );\
      virtual void\
      xtreemfs_service_get_by_name\
      (\
        const string& name,\
        org::xtreemfs::interfaces::ServiceSet& services\
      );\
      virtual void\
      xtreemfs_service_get_by_type\
      (\
        org::xtreemfs::interfaces::ServiceType type,\
        org::xtreemfs::interfaces::ServiceSet& services\
      );\
      virtual void\
      xtreemfs_service_get_by_uuid\
      (\
        const string& uuid,\
        org::xtreemfs::interfaces::ServiceSet& services\
      );\
      virtual void xtreemfs_service_offline( const string& uuid );\
      virtual uint64_t\
      xtreemfs_service_register\
      (\
        const org::xtreemfs::interfaces::Service& service\
      );\
      virtual void xtreemfs_shutdown();\


      #ifndef ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS ORG_EXCEPTION_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS ::yield::concurrency::Exception
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


      class DIRInterfaceMessages
      {
      public:
      // Request/response pair definitions for the operations in DIRInterface
        class xtreemfs_address_mappings_getRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_getRequest() { }

          xtreemfs_address_mappings_getRequest( const string& uuid )
            : uuid( uuid )
          { }

          xtreemfs_address_mappings_getRequest( const xtreemfs_address_mappings_getRequest& other )
            : uuid( other.get_uuid() )
          { }

          virtual ~xtreemfs_address_mappings_getRequest() {  }

          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }

          bool operator==( const xtreemfs_address_mappings_getRequest& other ) const
          {
            return get_uuid() == other.get_uuid();
          }

          // yidl::runtime::Object
          xtreemfs_address_mappings_getRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_getRequest, 2010031017 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "uuid", 0 ), get_uuid() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "uuid", 0 ), uuid );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_address_mappings_getResponse;
          }

          virtual void
          respond
          (
            const org::xtreemfs::interfaces::AddressMappingSet& address_mappings
          )
          {
            respond
            (
              *new xtreemfs_address_mappings_getResponse
                   (
                     address_mappings
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          string uuid;
        };


        class xtreemfs_address_mappings_getResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_getResponse() { }

          xtreemfs_address_mappings_getResponse
          (
            const org::xtreemfs::interfaces::AddressMappingSet& address_mappings
          )
            : address_mappings( address_mappings )
          { }

          xtreemfs_address_mappings_getResponse( const xtreemfs_address_mappings_getResponse& other )
            : address_mappings( other.get_address_mappings() )
          { }

          virtual ~xtreemfs_address_mappings_getResponse() {  }

          const org::xtreemfs::interfaces::AddressMappingSet& get_address_mappings() const { return address_mappings; }
          void set_address_mappings( const org::xtreemfs::interfaces::AddressMappingSet&  address_mappings ) { this->address_mappings = address_mappings; }

          bool operator==( const xtreemfs_address_mappings_getResponse& other ) const
          {
            return get_address_mappings() == other.get_address_mappings();
          }

          // yidl::runtime::Object
          xtreemfs_address_mappings_getResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_getResponse, 2010031017 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "address_mappings", 0 ), get_address_mappings() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "address_mappings", 0 ), address_mappings );
          }

        protected:
          org::xtreemfs::interfaces::AddressMappingSet address_mappings;
        };


        class xtreemfs_address_mappings_removeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_removeRequest() { }

          xtreemfs_address_mappings_removeRequest( const string& uuid )
            : uuid( uuid )
          { }

          xtreemfs_address_mappings_removeRequest( const xtreemfs_address_mappings_removeRequest& other )
            : uuid( other.get_uuid() )
          { }

          virtual ~xtreemfs_address_mappings_removeRequest() {  }

          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }

          bool operator==( const xtreemfs_address_mappings_removeRequest& other ) const
          {
            return get_uuid() == other.get_uuid();
          }

          // yidl::runtime::Object
          xtreemfs_address_mappings_removeRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_removeRequest, 2010031018 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "uuid", 0 ), get_uuid() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "uuid", 0 ), uuid );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_address_mappings_removeResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_address_mappings_removeResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          string uuid;
        };


        class xtreemfs_address_mappings_removeResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_address_mappings_removeResponse() {  }

          bool operator==( const xtreemfs_address_mappings_removeResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_address_mappings_removeResponse& inc_ref() { return Object::inc_ref( *this ); }

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

          xtreemfs_address_mappings_setRequest
          (
            const org::xtreemfs::interfaces::AddressMappingSet& address_mappings
          )
            : address_mappings( address_mappings )
          { }

          xtreemfs_address_mappings_setRequest( const xtreemfs_address_mappings_setRequest& other )
            : address_mappings( other.get_address_mappings() )
          { }

          virtual ~xtreemfs_address_mappings_setRequest() {  }

          const org::xtreemfs::interfaces::AddressMappingSet& get_address_mappings() const { return address_mappings; }
          void set_address_mappings( const org::xtreemfs::interfaces::AddressMappingSet&  address_mappings ) { this->address_mappings = address_mappings; }

          bool operator==( const xtreemfs_address_mappings_setRequest& other ) const
          {
            return get_address_mappings() == other.get_address_mappings();
          }

          // yidl::runtime::Object
          xtreemfs_address_mappings_setRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_setRequest, 2010031019 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "address_mappings", 0 ), get_address_mappings() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "address_mappings", 0 ), address_mappings );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_address_mappings_setResponse;
          }

          virtual void respond( uint64_t _return_value )
          {
            respond( *new xtreemfs_address_mappings_setResponse( _return_value ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          org::xtreemfs::interfaces::AddressMappingSet address_mappings;
        };


        class xtreemfs_address_mappings_setResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_setResponse()
            : _return_value( 0 )
          { }

          xtreemfs_address_mappings_setResponse( uint64_t _return_value )
            : _return_value( _return_value )
          { }

          xtreemfs_address_mappings_setResponse( const xtreemfs_address_mappings_setResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_address_mappings_setResponse() {  }

          uint64_t get__return_value() const { return _return_value; }
          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_address_mappings_setResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_address_mappings_setResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_address_mappings_setResponse, 2010031019 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), get__return_value() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          uint64_t _return_value;
        };


        class xtreemfs_checkpointRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_checkpointRequest() {  }

          bool operator==( const xtreemfs_checkpointRequest& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_checkpointRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_checkpointRequest, 2010031020 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_checkpointResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_checkpointResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }
        };


        class xtreemfs_checkpointResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_checkpointResponse() {  }

          bool operator==( const xtreemfs_checkpointResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_checkpointResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_checkpointResponse, 2010031020 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_discover_dirRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_discover_dirRequest() {  }

          bool operator==( const xtreemfs_discover_dirRequest& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_discover_dirRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_discover_dirRequest, 2010031021 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_discover_dirResponse;
          }

          virtual void
          respond
          (
            const org::xtreemfs::interfaces::DirService& dir_service
          )
          {
            respond
            (
              *new xtreemfs_discover_dirResponse
                   (
                     dir_service
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }
        };


        class xtreemfs_discover_dirResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_discover_dirResponse() { }

          xtreemfs_discover_dirResponse
          (
            const org::xtreemfs::interfaces::DirService& dir_service
          )
            : dir_service( dir_service )
          { }

          xtreemfs_discover_dirResponse( const xtreemfs_discover_dirResponse& other )
            : dir_service( other.get_dir_service() )
          { }

          virtual ~xtreemfs_discover_dirResponse() {  }

          const org::xtreemfs::interfaces::DirService& get_dir_service() const { return dir_service; }
          void set_dir_service( const org::xtreemfs::interfaces::DirService&  dir_service ) { this->dir_service = dir_service; }

          bool operator==( const xtreemfs_discover_dirResponse& other ) const
          {
            return get_dir_service() == other.get_dir_service();
          }

          // yidl::runtime::Object
          xtreemfs_discover_dirResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_discover_dirResponse, 2010031021 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "dir_service", 0 ), get_dir_service() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "dir_service", 0 ), dir_service );
          }

        protected:
          org::xtreemfs::interfaces::DirService dir_service;
        };


        class xtreemfs_global_time_s_getRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_global_time_s_getRequest() {  }

          bool operator==( const xtreemfs_global_time_s_getRequest& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_global_time_s_getRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_global_time_s_getRequest, 2010031022 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_global_time_s_getResponse;
          }

          virtual void respond( uint64_t _return_value )
          {
            respond( *new xtreemfs_global_time_s_getResponse( _return_value ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }
        };


        class xtreemfs_global_time_s_getResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_global_time_s_getResponse()
            : _return_value( 0 )
          { }

          xtreemfs_global_time_s_getResponse( uint64_t _return_value )
            : _return_value( _return_value )
          { }

          xtreemfs_global_time_s_getResponse( const xtreemfs_global_time_s_getResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_global_time_s_getResponse() {  }

          uint64_t get__return_value() const { return _return_value; }
          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_global_time_s_getResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_global_time_s_getResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_global_time_s_getResponse, 2010031022 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), get__return_value() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          uint64_t _return_value;
        };


        class xtreemfs_service_deregisterRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_deregisterRequest() { }

          xtreemfs_service_deregisterRequest( const string& uuid )
            : uuid( uuid )
          { }

          xtreemfs_service_deregisterRequest( const xtreemfs_service_deregisterRequest& other )
            : uuid( other.get_uuid() )
          { }

          virtual ~xtreemfs_service_deregisterRequest() {  }

          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }

          bool operator==( const xtreemfs_service_deregisterRequest& other ) const
          {
            return get_uuid() == other.get_uuid();
          }

          // yidl::runtime::Object
          xtreemfs_service_deregisterRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_deregisterRequest, 2010031028 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "uuid", 0 ), get_uuid() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "uuid", 0 ), uuid );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_service_deregisterResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_service_deregisterResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          string uuid;
        };


        class xtreemfs_service_deregisterResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_service_deregisterResponse() {  }

          bool operator==( const xtreemfs_service_deregisterResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_service_deregisterResponse& inc_ref() { return Object::inc_ref( *this ); }

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

          xtreemfs_service_get_by_nameRequest( const string& name )
            : name( name )
          { }

          xtreemfs_service_get_by_nameRequest( const xtreemfs_service_get_by_nameRequest& other )
            : name( other.get_name() )
          { }

          virtual ~xtreemfs_service_get_by_nameRequest() {  }

          const string& get_name() const { return name; }
          void set_name( const string& name ) { this->name = name; }

          bool operator==( const xtreemfs_service_get_by_nameRequest& other ) const
          {
            return get_name() == other.get_name();
          }

          // yidl::runtime::Object
          xtreemfs_service_get_by_nameRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_nameRequest, 2010031026 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "name", 0 ), get_name() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "name", 0 ), name );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_service_get_by_nameResponse;
          }

          virtual void respond( const org::xtreemfs::interfaces::ServiceSet& services )
          {
            respond( *new xtreemfs_service_get_by_nameResponse( services ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          string name;
        };


        class xtreemfs_service_get_by_nameResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_nameResponse() { }

          xtreemfs_service_get_by_nameResponse
          (
            const org::xtreemfs::interfaces::ServiceSet& services
          )
            : services( services )
          { }

          xtreemfs_service_get_by_nameResponse( const xtreemfs_service_get_by_nameResponse& other )
            : services( other.get_services() )
          { }

          virtual ~xtreemfs_service_get_by_nameResponse() {  }

          const org::xtreemfs::interfaces::ServiceSet& get_services() const { return services; }
          void set_services( const org::xtreemfs::interfaces::ServiceSet&  services ) { this->services = services; }

          bool operator==( const xtreemfs_service_get_by_nameResponse& other ) const
          {
            return get_services() == other.get_services();
          }

          // yidl::runtime::Object
          xtreemfs_service_get_by_nameResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_nameResponse, 2010031026 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "services", 0 ), get_services() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "services", 0 ), services );
          }

        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };


        class xtreemfs_service_get_by_typeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_typeRequest()
            : type( SERVICE_TYPE_MIXED )
          { }

          xtreemfs_service_get_by_typeRequest
          (
            org::xtreemfs::interfaces::ServiceType type
          )
            : type( type )
          { }

          xtreemfs_service_get_by_typeRequest( const xtreemfs_service_get_by_typeRequest& other )
            : type( other.get_type() )
          { }

          virtual ~xtreemfs_service_get_by_typeRequest() {  }

          org::xtreemfs::interfaces::ServiceType get_type() const { return type; }
          void set_type( org::xtreemfs::interfaces::ServiceType type ) { this->type = type; }

          bool operator==( const xtreemfs_service_get_by_typeRequest& other ) const
          {
            return get_type() == other.get_type();
          }

          // yidl::runtime::Object
          xtreemfs_service_get_by_typeRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_typeRequest, 2010031024 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "type", 0 ), static_cast<int32_t>( get_type() ) );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            type = static_cast<org::xtreemfs::interfaces::ServiceType>( unmarshaller.read_int32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "type", 0 ) ) );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_service_get_by_typeResponse;
          }

          virtual void respond( const org::xtreemfs::interfaces::ServiceSet& services )
          {
            respond( *new xtreemfs_service_get_by_typeResponse( services ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          org::xtreemfs::interfaces::ServiceType type;
        };


        class xtreemfs_service_get_by_typeResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_typeResponse() { }

          xtreemfs_service_get_by_typeResponse
          (
            const org::xtreemfs::interfaces::ServiceSet& services
          )
            : services( services )
          { }

          xtreemfs_service_get_by_typeResponse( const xtreemfs_service_get_by_typeResponse& other )
            : services( other.get_services() )
          { }

          virtual ~xtreemfs_service_get_by_typeResponse() {  }

          const org::xtreemfs::interfaces::ServiceSet& get_services() const { return services; }
          void set_services( const org::xtreemfs::interfaces::ServiceSet&  services ) { this->services = services; }

          bool operator==( const xtreemfs_service_get_by_typeResponse& other ) const
          {
            return get_services() == other.get_services();
          }

          // yidl::runtime::Object
          xtreemfs_service_get_by_typeResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_typeResponse, 2010031024 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "services", 0 ), get_services() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "services", 0 ), services );
          }

        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };


        class xtreemfs_service_get_by_uuidRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_uuidRequest() { }

          xtreemfs_service_get_by_uuidRequest( const string& uuid )
            : uuid( uuid )
          { }

          xtreemfs_service_get_by_uuidRequest( const xtreemfs_service_get_by_uuidRequest& other )
            : uuid( other.get_uuid() )
          { }

          virtual ~xtreemfs_service_get_by_uuidRequest() {  }

          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }

          bool operator==( const xtreemfs_service_get_by_uuidRequest& other ) const
          {
            return get_uuid() == other.get_uuid();
          }

          // yidl::runtime::Object
          xtreemfs_service_get_by_uuidRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_uuidRequest, 2010031025 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "uuid", 0 ), get_uuid() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "uuid", 0 ), uuid );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_service_get_by_uuidResponse;
          }

          virtual void respond( const org::xtreemfs::interfaces::ServiceSet& services )
          {
            respond( *new xtreemfs_service_get_by_uuidResponse( services ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          string uuid;
        };


        class xtreemfs_service_get_by_uuidResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_uuidResponse() { }

          xtreemfs_service_get_by_uuidResponse
          (
            const org::xtreemfs::interfaces::ServiceSet& services
          )
            : services( services )
          { }

          xtreemfs_service_get_by_uuidResponse( const xtreemfs_service_get_by_uuidResponse& other )
            : services( other.get_services() )
          { }

          virtual ~xtreemfs_service_get_by_uuidResponse() {  }

          const org::xtreemfs::interfaces::ServiceSet& get_services() const { return services; }
          void set_services( const org::xtreemfs::interfaces::ServiceSet&  services ) { this->services = services; }

          bool operator==( const xtreemfs_service_get_by_uuidResponse& other ) const
          {
            return get_services() == other.get_services();
          }

          // yidl::runtime::Object
          xtreemfs_service_get_by_uuidResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_get_by_uuidResponse, 2010031025 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "services", 0 ), get_services() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "services", 0 ), services );
          }

        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };


        class xtreemfs_service_offlineRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_offlineRequest() { }

          xtreemfs_service_offlineRequest( const string& uuid )
            : uuid( uuid )
          { }

          xtreemfs_service_offlineRequest( const xtreemfs_service_offlineRequest& other )
            : uuid( other.get_uuid() )
          { }

          virtual ~xtreemfs_service_offlineRequest() {  }

          const string& get_uuid() const { return uuid; }
          void set_uuid( const string& uuid ) { this->uuid = uuid; }

          bool operator==( const xtreemfs_service_offlineRequest& other ) const
          {
            return get_uuid() == other.get_uuid();
          }

          // yidl::runtime::Object
          xtreemfs_service_offlineRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_offlineRequest, 2010031029 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "uuid", 0 ), get_uuid() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "uuid", 0 ), uuid );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_service_offlineResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_service_offlineResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          string uuid;
        };


        class xtreemfs_service_offlineResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_service_offlineResponse() {  }

          bool operator==( const xtreemfs_service_offlineResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_service_offlineResponse& inc_ref() { return Object::inc_ref( *this ); }

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

          xtreemfs_service_registerRequest
          (
            const org::xtreemfs::interfaces::Service& service
          )
            : service( service )
          { }

          xtreemfs_service_registerRequest( const xtreemfs_service_registerRequest& other )
            : service( other.get_service() )
          { }

          virtual ~xtreemfs_service_registerRequest() {  }

          const org::xtreemfs::interfaces::Service& get_service() const { return service; }
          void set_service( const org::xtreemfs::interfaces::Service&  service ) { this->service = service; }

          bool operator==( const xtreemfs_service_registerRequest& other ) const
          {
            return get_service() == other.get_service();
          }

          // yidl::runtime::Object
          xtreemfs_service_registerRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_registerRequest, 2010031027 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "service", 0 ), get_service() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "service", 0 ), service );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_service_registerResponse;
          }

          virtual void respond( uint64_t _return_value )
          {
            respond( *new xtreemfs_service_registerResponse( _return_value ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          org::xtreemfs::interfaces::Service service;
        };


        class xtreemfs_service_registerResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_registerResponse()
            : _return_value( 0 )
          { }

          xtreemfs_service_registerResponse( uint64_t _return_value )
            : _return_value( _return_value )
          { }

          xtreemfs_service_registerResponse( const xtreemfs_service_registerResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_service_registerResponse() {  }

          uint64_t get__return_value() const { return _return_value; }
          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_service_registerResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_service_registerResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_service_registerResponse, 2010031027 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), get__return_value() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          uint64_t _return_value;
        };


        class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_shutdownRequest() {  }

          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_shutdownRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 2010031030 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_shutdownResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_shutdownResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }
        };


        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_shutdownResponse() {  }

          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_shutdownResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 2010031030 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class ConcurrentModificationException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          ConcurrentModificationException() { }
          ConcurrentModificationException( const string& stack_trace ) : stack_trace( stack_trace ) { }
          virtual ~ConcurrentModificationException() throw() { ; }

          const string& get_stack_trace() const { return stack_trace; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::Object
          ConcurrentModificationException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ConcurrentModificationException, 2010031036 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stack_trace", 0 ), get_stack_trace() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
          }

          // yield::concurrency::Exception
          virtual ::yield::concurrency::Exception& clone() const
          {
            return *new ConcurrentModificationException( get_stack_trace() );
          }

          virtual void throwStackClone() const
          {
            throw ConcurrentModificationException( get_stack_trace() );
          }

        protected:
          string stack_trace;
        };


        class DIRException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          DIRException() { }
          DIRException( uint32_t error_code ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_code ) { }
          DIRException( const char* error_message ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          DIRException( const string& error_message ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          DIRException( uint32_t error_code, const char* error_message ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_code, error_message ) { }
          DIRException( uint32_t error_code, const string& error_message ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_code, error_message ) { }
          DIRException( uint32_t error_code, const string& error_message, const string& stack_trace ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_code, error_message ), stack_trace( stack_trace ) { }
          virtual ~DIRException() throw() { ; }

          const string& get_stack_trace() const { return stack_trace; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::Object
          DIRException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( DIRException, 2010031039 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_code", 0 ), get_error_code() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_message", 0 ), get_error_message() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stack_trace", 0 ), get_stack_trace() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            uint32_t error_code; error_code = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_code", 0 ) ); set_error_code( error_code );
            string error_message; unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_message", 0 ), error_message ); set_error_message( error_message );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
          }

          // yield::concurrency::Exception
          virtual ::yield::concurrency::Exception& clone() const
          {
            return *new DIRException( get_error_code(), get_error_message(), get_stack_trace() );
          }

          virtual void throwStackClone() const
          {
            throw DIRException( get_error_code(), get_error_message(), get_stack_trace() );
          }

        protected:
          string stack_trace;
        };


        class InvalidArgumentException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          InvalidArgumentException() { }
          InvalidArgumentException( const char* error_message ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          InvalidArgumentException( const string& error_message ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          virtual ~InvalidArgumentException() throw() { ; }

          // yidl::runtime::Object
          InvalidArgumentException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( InvalidArgumentException, 2010031037 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_message", 0 ), get_error_message() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            string error_message; unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_message", 0 ), error_message ); set_error_message( error_message );
          }

          // yield::concurrency::Exception
          virtual ::yield::concurrency::Exception& clone() const
          {
            return *new InvalidArgumentException( get_error_message() );
          }

          virtual void throwStackClone() const
          {
            throw InvalidArgumentException( get_error_message() );
          }
        };


        class ProtocolException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          ProtocolException() : accept_stat( 0 ) { }
          ProtocolException( uint32_t error_code ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_code ), accept_stat( 0 ) { }
          ProtocolException( uint32_t accept_stat, uint32_t error_code, const string& stack_trace ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS( error_code ), accept_stat( accept_stat ), stack_trace( stack_trace ) { }
          virtual ~ProtocolException() throw() { ; }

          uint32_t get_accept_stat() const { return accept_stat; }
          void set_accept_stat( uint32_t accept_stat ) { this->accept_stat = accept_stat; }
          const string& get_stack_trace() const { return stack_trace; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::Object
          ProtocolException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ProtocolException, 2010031038 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "accept_stat", 0 ), get_accept_stat() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_code", 0 ), get_error_code() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stack_trace", 0 ), get_stack_trace() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            accept_stat = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "accept_stat", 0 ) );
            uint32_t error_code; error_code = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_code", 0 ) ); set_error_code( error_code );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
          }

          // yield::concurrency::Exception
          virtual ::yield::concurrency::Exception& clone() const
          {
            return *new ProtocolException( get_accept_stat(), get_error_code(), get_stack_trace() );
          }

          virtual void throwStackClone() const
          {
            throw ProtocolException( get_accept_stat(), get_error_code(), get_stack_trace() );
          }

        protected:
          uint32_t accept_stat;
          string stack_trace;
        };


        class RedirectException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          RedirectException() : port( 0 ) { }
          RedirectException( const string& address, uint16_t port ) : address( address ), port( port ) { }
          virtual ~RedirectException() throw() { ; }

          const string& get_address() const { return address; }
          void set_address( const string& address ) { this->address = address; }
          uint16_t get_port() const { return port; }
          void set_port( uint16_t port ) { this->port = port; }

          // yidl::runtime::Object
          RedirectException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( RedirectException, 2010031040 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "address", 0 ), get_address() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "port", 0 ), get_port() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "address", 0 ), address );
            port = unmarshaller.read_uint16( ::yidl::runtime::Unmarshaller::StringLiteralKey( "port", 0 ) );
          }

          // yield::concurrency::Exception
          virtual ::yield::concurrency::Exception& clone() const
          {
            return *new RedirectException( get_address(), get_port() );
          }

          virtual void throwStackClone() const
          {
            throw RedirectException( get_address(), get_port() );
          }

        protected:
          string address;
          uint16_t port;
        };
      };


      class DIRInterfaceMessageFactory
        : public ::yield::concurrency::MessageFactory,
          private DIRInterfaceMessages
      {
      public:
        // yield::concurrency::MessageFactory
        virtual ::yield::concurrency::Exception* createException( uint32_t type_id )
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

        virtual ::yield::concurrency::Exception*
        createException
        (
          const char* type_name,
          size_t type_name_len
        )
        {
          if ( type_name_len == 31 && strncmp( type_name, "ConcurrentModificationException", 31 ) == 0 ) return new ConcurrentModificationException;
          else if ( type_name_len == 12 && strncmp( type_name, "DIRException", 12 ) == 0 ) return new DIRException;
          else if ( type_name_len == 24 && strncmp( type_name, "InvalidArgumentException", 24 ) == 0 ) return new InvalidArgumentException;
          else if ( type_name_len == 17 && strncmp( type_name, "ProtocolException", 17 ) == 0 ) return new ProtocolException;
          else if ( type_name_len == 17 && strncmp( type_name, "RedirectException", 17 ) == 0 ) return new RedirectException;
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

        virtual ::yield::concurrency::Request*
        createRequest
        (
          const char* type_name,
          size_t type_name_len
        )
        {
          if ( type_name_len == 36 && strncmp( type_name, "xtreemfs_address_mappings_getRequest", 36 ) == 0 ) return new xtreemfs_address_mappings_getRequest;
          else if ( type_name_len == 39 && strncmp( type_name, "xtreemfs_address_mappings_removeRequest", 39 ) == 0 ) return new xtreemfs_address_mappings_removeRequest;
          else if ( type_name_len == 36 && strncmp( type_name, "xtreemfs_address_mappings_setRequest", 36 ) == 0 ) return new xtreemfs_address_mappings_setRequest;
          else if ( type_name_len == 26 && strncmp( type_name, "xtreemfs_checkpointRequest", 26 ) == 0 ) return new xtreemfs_checkpointRequest;
          else if ( type_name_len == 28 && strncmp( type_name, "xtreemfs_discover_dirRequest", 28 ) == 0 ) return new xtreemfs_discover_dirRequest;
          else if ( type_name_len == 33 && strncmp( type_name, "xtreemfs_global_time_s_getRequest", 33 ) == 0 ) return new xtreemfs_global_time_s_getRequest;
          else if ( type_name_len == 34 && strncmp( type_name, "xtreemfs_service_deregisterRequest", 34 ) == 0 ) return new xtreemfs_service_deregisterRequest;
          else if ( type_name_len == 35 && strncmp( type_name, "xtreemfs_service_get_by_nameRequest", 35 ) == 0 ) return new xtreemfs_service_get_by_nameRequest;
          else if ( type_name_len == 35 && strncmp( type_name, "xtreemfs_service_get_by_typeRequest", 35 ) == 0 ) return new xtreemfs_service_get_by_typeRequest;
          else if ( type_name_len == 35 && strncmp( type_name, "xtreemfs_service_get_by_uuidRequest", 35 ) == 0 ) return new xtreemfs_service_get_by_uuidRequest;
          else if ( type_name_len == 31 && strncmp( type_name, "xtreemfs_service_offlineRequest", 31 ) == 0 ) return new xtreemfs_service_offlineRequest;
          else if ( type_name_len == 32 && strncmp( type_name, "xtreemfs_service_registerRequest", 32 ) == 0 ) return new xtreemfs_service_registerRequest;
          else if ( type_name_len == 24 && strncmp( type_name, "xtreemfs_shutdownRequest", 24 ) == 0 ) return new xtreemfs_shutdownRequest;
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

        virtual ::yield::concurrency::Response*
        createResponse
        (
          const char* type_name,
          size_t type_name_len
        )
        {
          if ( type_name_len == 37 && strncmp( type_name, "xtreemfs_address_mappings_getResponse", 37 ) == 0 ) return new xtreemfs_address_mappings_getResponse;
          else if ( type_name_len == 40 && strncmp( type_name, "xtreemfs_address_mappings_removeResponse", 40 ) == 0 ) return new xtreemfs_address_mappings_removeResponse;
          else if ( type_name_len == 37 && strncmp( type_name, "xtreemfs_address_mappings_setResponse", 37 ) == 0 ) return new xtreemfs_address_mappings_setResponse;
          else if ( type_name_len == 27 && strncmp( type_name, "xtreemfs_checkpointResponse", 27 ) == 0 ) return new xtreemfs_checkpointResponse;
          else if ( type_name_len == 29 && strncmp( type_name, "xtreemfs_discover_dirResponse", 29 ) == 0 ) return new xtreemfs_discover_dirResponse;
          else if ( type_name_len == 34 && strncmp( type_name, "xtreemfs_global_time_s_getResponse", 34 ) == 0 ) return new xtreemfs_global_time_s_getResponse;
          else if ( type_name_len == 35 && strncmp( type_name, "xtreemfs_service_deregisterResponse", 35 ) == 0 ) return new xtreemfs_service_deregisterResponse;
          else if ( type_name_len == 36 && strncmp( type_name, "xtreemfs_service_get_by_nameResponse", 36 ) == 0 ) return new xtreemfs_service_get_by_nameResponse;
          else if ( type_name_len == 36 && strncmp( type_name, "xtreemfs_service_get_by_typeResponse", 36 ) == 0 ) return new xtreemfs_service_get_by_typeResponse;
          else if ( type_name_len == 36 && strncmp( type_name, "xtreemfs_service_get_by_uuidResponse", 36 ) == 0 ) return new xtreemfs_service_get_by_uuidResponse;
          else if ( type_name_len == 32 && strncmp( type_name, "xtreemfs_service_offlineResponse", 32 ) == 0 ) return new xtreemfs_service_offlineResponse;
          else if ( type_name_len == 33 && strncmp( type_name, "xtreemfs_service_registerResponse", 33 ) == 0 ) return new xtreemfs_service_registerResponse;
          else if ( type_name_len == 25 && strncmp( type_name, "xtreemfs_shutdownResponse", 25 ) == 0 ) return new xtreemfs_shutdownResponse;
          else return NULL;
        }


        // yidl::runtime::MarshallableObjectFactory
        virtual ::yidl::runtime::MarshallableObject*
        createMarshallableObject
        (
          uint32_t type_id
        )
        {
          switch ( type_id )
          {
            case 2010030951: return new ServiceSet;
            case 2010030950: return new Service;
            case 2010030918: return new UserCredentials;
            case 2010030948: return new ServiceDataMap;
            case 2010030947: return new AddressMappingSet;
            case 2010030946: return new AddressMapping;
            case 2010030917: return new StringSet;
            default: return NULL;
          }
        }

        virtual ::yidl::runtime::MarshallableObject*
        createMarshallableObject
        (
          const char* type_name,
          size_t type_name_len
        )
        {
          if ( type_name_len == 10 && strncmp( type_name, "ServiceSet", 10 ) == 0 ) return new ServiceSet;
          else if ( type_name_len == 7 && strncmp( type_name, "Service", 7 ) == 0 ) return new Service;
          else if ( type_name_len == 15 && strncmp( type_name, "UserCredentials", 15 ) == 0 ) return new UserCredentials;
          else if ( type_name_len == 14 && strncmp( type_name, "ServiceDataMap", 14 ) == 0 ) return new ServiceDataMap;
          else if ( type_name_len == 17 && strncmp( type_name, "AddressMappingSet", 17 ) == 0 ) return new AddressMappingSet;
          else if ( type_name_len == 14 && strncmp( type_name, "AddressMapping", 14 ) == 0 ) return new AddressMapping;
          else if ( type_name_len == 10 && strncmp( type_name, "DirService", 10 ) == 0 ) return new DirService;
          else if ( type_name_len == 9 && strncmp( type_name, "StringSet", 9 ) == 0 ) return new StringSet;
          else return NULL;
        }
      };


      class DIRInterfaceRequestHandler
        : public ::yield::concurrency::RequestHandler,
          protected DIRInterfaceMessages
      {
      public:
        DIRInterfaceRequestHandler()  // Subclasses must implement
          : _interface( NULL ) // all relevant handle*Request methods
        { }

        // Steals interface_ to allow for *new
        DIRInterfaceRequestHandler( DIRInterface& _interface )
          : _interface( &_interface )
        { }

        virtual ~DIRInterfaceRequestHandler()
        {
          delete _interface;
        }

        // yidl::runtime::RTTIObject
        virtual const char* get_type_name() const
        {
          return "DIRInterface";
        }

        // yield::concurrency::RequestHandler
        virtual void handle( ::yield::concurrency::Request& request )
        {
          // Switch on the request types that this interface handles, unwrap the corresponding requests and delegate to _interface
          switch ( request.get_type_id() )
          {
            case 2010031017UL: handle( static_cast<xtreemfs_address_mappings_getRequest&>( request ) ); return;
            case 2010031018UL: handle( static_cast<xtreemfs_address_mappings_removeRequest&>( request ) ); return;
            case 2010031019UL: handle( static_cast<xtreemfs_address_mappings_setRequest&>( request ) ); return;
            case 2010031020UL: handle( static_cast<xtreemfs_checkpointRequest&>( request ) ); return;
            case 2010031021UL: handle( static_cast<xtreemfs_discover_dirRequest&>( request ) ); return;
            case 2010031022UL: handle( static_cast<xtreemfs_global_time_s_getRequest&>( request ) ); return;
            case 2010031028UL: handle( static_cast<xtreemfs_service_deregisterRequest&>( request ) ); return;
            case 2010031026UL: handle( static_cast<xtreemfs_service_get_by_nameRequest&>( request ) ); return;
            case 2010031024UL: handle( static_cast<xtreemfs_service_get_by_typeRequest&>( request ) ); return;
            case 2010031025UL: handle( static_cast<xtreemfs_service_get_by_uuidRequest&>( request ) ); return;
            case 2010031029UL: handle( static_cast<xtreemfs_service_offlineRequest&>( request ) ); return;
            case 2010031027UL: handle( static_cast<xtreemfs_service_registerRequest&>( request ) ); return;
            case 2010031030UL: handle( static_cast<xtreemfs_shutdownRequest&>( request ) ); return;
          }
        }

      protected:

        virtual void handle( xtreemfs_address_mappings_getRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::AddressMappingSet address_mappings;

              _interface->xtreemfs_address_mappings_get
              (
                __request.get_uuid(),
                address_mappings
              );

              __request.respond( address_mappings );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_address_mappings_removeRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_address_mappings_remove( __request.get_uuid() );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_address_mappings_setRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              uint64_t _return_value =

              _interface->xtreemfs_address_mappings_set
              (
                __request.get_address_mappings()
              );

              __request.respond( _return_value );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_checkpointRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_checkpoint();
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_discover_dirRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::DirService dir_service;

              _interface->xtreemfs_discover_dir( dir_service );

              __request.respond( dir_service );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_global_time_s_getRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              uint64_t _return_value =

              _interface->xtreemfs_global_time_s_get();

              __request.respond( _return_value );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_service_deregisterRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_service_deregister( __request.get_uuid() );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_service_get_by_nameRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::ServiceSet services;

              _interface->xtreemfs_service_get_by_name
              (
                __request.get_name(),
                services
              );

              __request.respond( services );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_service_get_by_typeRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::ServiceSet services;

              _interface->xtreemfs_service_get_by_type
              (
                __request.get_type(),
                services
              );

              __request.respond( services );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_service_get_by_uuidRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::ServiceSet services;

              _interface->xtreemfs_service_get_by_uuid
              (
                __request.get_uuid(),
                services
              );

              __request.respond( services );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_service_offlineRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_service_offline( __request.get_uuid() );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_service_registerRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              uint64_t _return_value =

              _interface->xtreemfs_service_register( __request.get_service() );

              __request.respond( _return_value );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( xtreemfs_shutdownRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_shutdown();
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

      private:
        DIRInterface* _interface;
      };

      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_HANDLER_PROTOTYPES \
      virtual void handle( xtreemfs_address_mappings_getRequest& __request );\
      virtual void handle( xtreemfs_address_mappings_removeRequest& __request );\
      virtual void handle( xtreemfs_address_mappings_setRequest& __request );\
      virtual void handle( xtreemfs_checkpointRequest& __request );\
      virtual void handle( xtreemfs_discover_dirRequest& __request );\
      virtual void handle( xtreemfs_global_time_s_getRequest& __request );\
      virtual void handle( xtreemfs_service_deregisterRequest& __request );\
      virtual void handle( xtreemfs_service_get_by_nameRequest& __request );\
      virtual void handle( xtreemfs_service_get_by_typeRequest& __request );\
      virtual void handle( xtreemfs_service_get_by_uuidRequest& __request );\
      virtual void handle( xtreemfs_service_offlineRequest& __request );\
      virtual void handle( xtreemfs_service_registerRequest& __request );\
      virtual void handle( xtreemfs_shutdownRequest& __request );


      class DIRInterfaceProxy
        : public DIRInterface,
          public ::yield::concurrency::RequestHandler,
          private DIRInterfaceMessages
      {
      public:
        DIRInterfaceProxy( ::yield::concurrency::EventHandler& request_handler )
          : __request_handler( request_handler )
        { }

        ~DIRInterfaceProxy()
        {
          ::yield::concurrency::EventHandler::dec_ref( __request_handler );
        }

        // yidl::runtime::RTTIObject
        virtual const char* get_type_name() const
        {
          return "DIRInterfaceProxy";
        }

        // yield::concurrency::RequestHandler
        virtual void handle( ::yield::concurrency::Request& request )
        {
          __request_handler.handle( request );
        }

        // DIRInterface
        virtual void
        xtreemfs_address_mappings_get
        (
          const string& uuid,
          org::xtreemfs::interfaces::AddressMappingSet& address_mappings
        )
        {
          xtreemfs_address_mappings_getRequest* __request = new xtreemfs_address_mappings_getRequest( uuid );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_getResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_getResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_address_mappings_getResponse> __response = __response_queue->dequeue();
          address_mappings = __response->get_address_mappings();
        }

        virtual void xtreemfs_address_mappings_remove( const string& uuid )
        {
          xtreemfs_address_mappings_removeRequest* __request = new xtreemfs_address_mappings_removeRequest( uuid );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_removeResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_removeResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_address_mappings_removeResponse> __response = __response_queue->dequeue();
        }

        virtual uint64_t
        xtreemfs_address_mappings_set
        (
          const org::xtreemfs::interfaces::AddressMappingSet& address_mappings
        )
        {
          xtreemfs_address_mappings_setRequest* __request = new xtreemfs_address_mappings_setRequest( address_mappings );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_setResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_address_mappings_setResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_address_mappings_setResponse> __response = __response_queue->dequeue();
          uint64_t _return_value = __response->get__return_value();return _return_value;
        }

        virtual void xtreemfs_checkpoint()
        {
          xtreemfs_checkpointRequest* __request = new xtreemfs_checkpointRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> __response = __response_queue->dequeue();
        }

        virtual void
        xtreemfs_discover_dir
        (
          org::xtreemfs::interfaces::DirService& dir_service
        )
        {
          xtreemfs_discover_dirRequest* __request = new xtreemfs_discover_dirRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_discover_dirResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_discover_dirResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_discover_dirResponse> __response = __response_queue->dequeue();
          dir_service = __response->get_dir_service();
        }

        virtual uint64_t xtreemfs_global_time_s_get()
        {
          xtreemfs_global_time_s_getRequest* __request = new xtreemfs_global_time_s_getRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_global_time_s_getResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_global_time_s_getResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_global_time_s_getResponse> __response = __response_queue->dequeue();
          uint64_t _return_value = __response->get__return_value();return _return_value;
        }

        virtual void xtreemfs_service_deregister( const string& uuid )
        {
          xtreemfs_service_deregisterRequest* __request = new xtreemfs_service_deregisterRequest( uuid );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_deregisterResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_deregisterResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_service_deregisterResponse> __response = __response_queue->dequeue();
        }

        virtual void
        xtreemfs_service_get_by_name
        (
          const string& name,
          org::xtreemfs::interfaces::ServiceSet& services
        )
        {
          xtreemfs_service_get_by_nameRequest* __request = new xtreemfs_service_get_by_nameRequest( name );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_nameResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_nameResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_service_get_by_nameResponse> __response = __response_queue->dequeue();
          services = __response->get_services();
        }

        virtual void
        xtreemfs_service_get_by_type
        (
          org::xtreemfs::interfaces::ServiceType type,
          org::xtreemfs::interfaces::ServiceSet& services
        )
        {
          xtreemfs_service_get_by_typeRequest* __request = new xtreemfs_service_get_by_typeRequest( type );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_typeResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_typeResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_service_get_by_typeResponse> __response = __response_queue->dequeue();
          services = __response->get_services();
        }

        virtual void
        xtreemfs_service_get_by_uuid
        (
          const string& uuid,
          org::xtreemfs::interfaces::ServiceSet& services
        )
        {
          xtreemfs_service_get_by_uuidRequest* __request = new xtreemfs_service_get_by_uuidRequest( uuid );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_uuidResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_get_by_uuidResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_service_get_by_uuidResponse> __response = __response_queue->dequeue();
          services = __response->get_services();
        }

        virtual void xtreemfs_service_offline( const string& uuid )
        {
          xtreemfs_service_offlineRequest* __request = new xtreemfs_service_offlineRequest( uuid );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_offlineResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_offlineResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_service_offlineResponse> __response = __response_queue->dequeue();
        }

        virtual uint64_t
        xtreemfs_service_register
        (
          const org::xtreemfs::interfaces::Service& service
        )
        {
          xtreemfs_service_registerRequest* __request = new xtreemfs_service_registerRequest( service );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_service_registerResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_service_registerResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_service_registerResponse> __response = __response_queue->dequeue();
          uint64_t _return_value = __response->get__return_value();return _return_value;
        }

        virtual void xtreemfs_shutdown()
        {
          xtreemfs_shutdownRequest* __request = new xtreemfs_shutdownRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> __response = __response_queue->dequeue();
        }

      private:
        // __request_handler is not a counted reference, since that would create
        // a reference cycle when __request_handler is a subclass of DIRInterfaceProxy
        ::yield::concurrency::EventHandler& __request_handler;
      };};
  };
};
#endif
