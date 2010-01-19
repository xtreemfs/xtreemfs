// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_INTERFACES_DIR_INTERFACE_H_
#define _XTREEMFS_INTERFACES_DIR_INTERFACE_H_

#include "constants.h"
#include "types.h"
#include "yidl.h"
#include "yield/concurrency.h"
#include <map>
#include <vector>


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
        AddressMapping( const std::string& uuid, uint64_t version, const std::string& protocol, const std::string& address, uint16_t port, const std::string& match_network, uint32_t ttl_s, const std::string& uri ) : uuid( uuid ), version( version ), protocol( protocol ), address( address ), port( port ), match_network( match_network ), ttl_s( ttl_s ), uri( uri ) { }
        AddressMapping( const char* uuid, size_t uuid_len, uint64_t version, const char* protocol, size_t protocol_len, const char* address, size_t address_len, uint16_t port, const char* match_network, size_t match_network_len, uint32_t ttl_s, const char* uri, size_t uri_len ) : uuid( uuid, uuid_len ), version( version ), protocol( protocol, protocol_len ), address( address, address_len ), port( port ), match_network( match_network, match_network_len ), ttl_s( ttl_s ), uri( uri, uri_len ) { }
        virtual ~AddressMapping() { }

        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len ) { this->uuid.assign( uuid, uuid_len ); }
        const std::string& get_uuid() const { return uuid; }
        void set_version( uint64_t version ) { this->version = version; }
        uint64_t get_version() const { return version; }
        void set_protocol( const std::string& protocol ) { set_protocol( protocol.c_str(), protocol.size() ); }
        void set_protocol( const char* protocol, size_t protocol_len ) { this->protocol.assign( protocol, protocol_len ); }
        const std::string& get_protocol() const { return protocol; }
        void set_address( const std::string& address ) { set_address( address.c_str(), address.size() ); }
        void set_address( const char* address, size_t address_len ) { this->address.assign( address, address_len ); }
        const std::string& get_address() const { return address; }
        void set_port( uint16_t port ) { this->port = port; }
        uint16_t get_port() const { return port; }
        void set_match_network( const std::string& match_network ) { set_match_network( match_network.c_str(), match_network.size() ); }
        void set_match_network( const char* match_network, size_t match_network_len ) { this->match_network.assign( match_network, match_network_len ); }
        const std::string& get_match_network() const { return match_network; }
        void set_ttl_s( uint32_t ttl_s ) { this->ttl_s = ttl_s; }
        uint32_t get_ttl_s() const { return ttl_s; }
        void set_uri( const std::string& uri ) { set_uri( uri.c_str(), uri.size() ); }
        void set_uri( const char* uri, size_t uri_len ) { this->uri.assign( uri, uri_len ); }
        const std::string& get_uri() const { return uri; }

        bool operator==( const AddressMapping& other ) const { return uuid == other.uuid && version == other.version && protocol == other.protocol && address == other.address && port == other.port && match_network == other.match_network && ttl_s == other.ttl_s && uri == other.uri; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( AddressMapping, 2010011941 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "uuid", 0, uuid ); marshaller.writeUint64( "version", 0, version ); marshaller.writeString( "protocol", 0, protocol ); marshaller.writeString( "address", 0, address ); marshaller.writeUint16( "port", 0, port ); marshaller.writeString( "match_network", 0, match_network ); marshaller.writeUint32( "ttl_s", 0, ttl_s ); marshaller.writeString( "uri", 0, uri ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "uuid", 0, uuid ); version = unmarshaller.readUint64( "version", 0 ); unmarshaller.readString( "protocol", 0, protocol ); unmarshaller.readString( "address", 0, address ); port = unmarshaller.readUint16( "port", 0 ); unmarshaller.readString( "match_network", 0, match_network ); ttl_s = unmarshaller.readUint32( "ttl_s", 0 ); unmarshaller.readString( "uri", 0, uri ); }

      protected:
        std::string uuid;
        uint64_t version;
        std::string protocol;
        std::string address;
        uint16_t port;
        std::string match_network;
        uint32_t ttl_s;
        std::string uri;
      };

      class AddressMappingSet : public ::yidl::runtime::Sequence, public std::vector<org::xtreemfs::interfaces::AddressMapping>
      {
      public:
        AddressMappingSet() { }
        AddressMappingSet( const org::xtreemfs::interfaces::AddressMapping& first_value ) { std::vector<org::xtreemfs::interfaces::AddressMapping>::push_back( first_value ); }
        AddressMappingSet( size_type size ) : std::vector<org::xtreemfs::interfaces::AddressMapping>( size ) { }
        virtual ~AddressMappingSet() { }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( AddressMappingSet, 2010011942 );

        // yidl::Sequence
        size_t get_size() const { return size(); }
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( "value", 0, ( *this )[value_i] ); } }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::AddressMapping value; unmarshaller.readStruct( "value", 0, value ); push_back( value ); }
      };

      class ServiceDataMap : public ::yidl::runtime::Map, public std::map<std::string,std::string>
      {
      public:
        virtual ~ServiceDataMap() { }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( ServiceDataMap, 2010011943 );

        // yidl::Map
        size_t get_size() const { return size(); }
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { for ( const_iterator i = begin(); i != end(); i++ ) { marshaller.writeString( i->first.c_str(), static_cast<uint32_t>( 0 ), i->second ); } }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { std::string key; unmarshaller.readString( 0, static_cast<uint32_t>( 0 ), key ); if ( !key.empty() ) { std::string value; unmarshaller.readString( key.c_str(), static_cast<uint32_t>( 0 ), value ); ( *this )[key] = value; } }
      };

      class Service : public ::yidl::runtime::Struct
      {
      public:
        Service() : type( SERVICE_TYPE_MIXED ), version( 0 ), last_updated_s( 0 ) { }
        Service( org::xtreemfs::interfaces::ServiceType type, const std::string& uuid, uint64_t version, const std::string& name, uint64_t last_updated_s, const org::xtreemfs::interfaces::ServiceDataMap& data ) : type( type ), uuid( uuid ), version( version ), name( name ), last_updated_s( last_updated_s ), data( data ) { }
        Service( org::xtreemfs::interfaces::ServiceType type, const char* uuid, size_t uuid_len, uint64_t version, const char* name, size_t name_len, uint64_t last_updated_s, const org::xtreemfs::interfaces::ServiceDataMap& data ) : type( type ), uuid( uuid, uuid_len ), version( version ), name( name, name_len ), last_updated_s( last_updated_s ), data( data ) { }
        virtual ~Service() { }

        void set_type( org::xtreemfs::interfaces::ServiceType type ) { this->type = type; }
        org::xtreemfs::interfaces::ServiceType get_type() const { return type; }
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len ) { this->uuid.assign( uuid, uuid_len ); }
        const std::string& get_uuid() const { return uuid; }
        void set_version( uint64_t version ) { this->version = version; }
        uint64_t get_version() const { return version; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len ) { this->name.assign( name, name_len ); }
        const std::string& get_name() const { return name; }
        void set_last_updated_s( uint64_t last_updated_s ) { this->last_updated_s = last_updated_s; }
        uint64_t get_last_updated_s() const { return last_updated_s; }
        void set_data( const org::xtreemfs::interfaces::ServiceDataMap&  data ) { this->data = data; }
        const org::xtreemfs::interfaces::ServiceDataMap& get_data() const { return data; }

        bool operator==( const Service& other ) const { return type == other.type && uuid == other.uuid && version == other.version && name == other.name && last_updated_s == other.last_updated_s && data == other.data; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( Service, 2010011945 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeInt32( "type", 0, static_cast<int32_t>( type ) ); marshaller.writeString( "uuid", 0, uuid ); marshaller.writeUint64( "version", 0, version ); marshaller.writeString( "name", 0, name ); marshaller.writeUint64( "last_updated_s", 0, last_updated_s ); marshaller.writeMap( "data", 0, data ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { type = static_cast<org::xtreemfs::interfaces::ServiceType>( unmarshaller.readInt32( "type", 0 ) ); unmarshaller.readString( "uuid", 0, uuid ); version = unmarshaller.readUint64( "version", 0 ); unmarshaller.readString( "name", 0, name ); last_updated_s = unmarshaller.readUint64( "last_updated_s", 0 ); unmarshaller.readMap( "data", 0, data ); }

      protected:
        org::xtreemfs::interfaces::ServiceType type;
        std::string uuid;
        uint64_t version;
        std::string name;
        uint64_t last_updated_s;
        org::xtreemfs::interfaces::ServiceDataMap data;
      };

      class ServiceSet : public ::yidl::runtime::Sequence, public std::vector<org::xtreemfs::interfaces::Service>
      {
      public:
        ServiceSet() { }
        ServiceSet( const org::xtreemfs::interfaces::Service& first_value ) { std::vector<org::xtreemfs::interfaces::Service>::push_back( first_value ); }
        ServiceSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Service>( size ) { }
        virtual ~ServiceSet() { }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( ServiceSet, 2010011946 );

        // yidl::Sequence
        size_t get_size() const { return size(); }
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( "value", 0, ( *this )[value_i] ); } }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::Service value; unmarshaller.readStruct( "value", 0, value ); push_back( value ); }
      };

      class DirService : public ::yidl::runtime::Struct
      {
      public:
        DirService() : port( 0 ), interface_version( 0 ) { }
        DirService( const std::string& address, uint16_t port, const std::string& protocol, uint32_t interface_version ) : address( address ), port( port ), protocol( protocol ), interface_version( interface_version ) { }
        DirService( const char* address, size_t address_len, uint16_t port, const char* protocol, size_t protocol_len, uint32_t interface_version ) : address( address, address_len ), port( port ), protocol( protocol, protocol_len ), interface_version( interface_version ) { }
        virtual ~DirService() { }

        void set_address( const std::string& address ) { set_address( address.c_str(), address.size() ); }
        void set_address( const char* address, size_t address_len ) { this->address.assign( address, address_len ); }
        const std::string& get_address() const { return address; }
        void set_port( uint16_t port ) { this->port = port; }
        uint16_t get_port() const { return port; }
        void set_protocol( const std::string& protocol ) { set_protocol( protocol.c_str(), protocol.size() ); }
        void set_protocol( const char* protocol, size_t protocol_len ) { this->protocol.assign( protocol, protocol_len ); }
        const std::string& get_protocol() const { return protocol; }
        void set_interface_version( uint32_t interface_version ) { this->interface_version = interface_version; }
        uint32_t get_interface_version() const { return interface_version; }

        bool operator==( const DirService& other ) const { return address == other.address && port == other.port && protocol == other.protocol && interface_version == other.interface_version; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( DirService, 2010011946 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "address", 0, address ); marshaller.writeUint16( "port", 0, port ); marshaller.writeString( "protocol", 0, protocol ); marshaller.writeUint32( "interface_version", 0, interface_version ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "address", 0, address ); port = unmarshaller.readUint16( "port", 0 ); unmarshaller.readString( "protocol", 0, protocol ); interface_version = unmarshaller.readUint32( "interface_version", 0 ); }

      protected:
        std::string address;
        uint16_t port;
        std::string protocol;
        uint32_t interface_version;
      };



      #ifndef ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ::YIELD::concurrency::Interface
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
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS ::YIELD::concurrency::Request
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
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS ::YIELD::concurrency::Response
      #endif
      #endif

      #ifndef ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_EXCEPTION_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ::YIELD::concurrency::ExceptionResponse
      #endif
      #endif



      class DIRInterface : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS
      {
      public:
        DIRInterface() { }
        virtual ~DIRInterface() { }


      const static uint32_t HTTP_PORT_DEFAULT = 30638;
      const static uint32_t ONCRPC_PORT_DEFAULT = 32638;
      const static uint32_t ONCRPCG_PORT_DEFAULT = 32638;
      const static uint32_t ONCRPCS_PORT_DEFAULT = 32638;
      const static uint32_t ONCRPCU_PORT_DEFAULT = 32638;


        virtual void xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { xtreemfs_address_mappings_get( uuid, address_mappings, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_address_mappings_getRequest> __request( new xtreemfs_address_mappings_getRequest( uuid ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_address_mappings_getResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_address_mappings_getResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_address_mappings_getResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); address_mappings = __response->get_address_mappings(); }

        virtual void xtreemfs_address_mappings_remove( const std::string& uuid ) { xtreemfs_address_mappings_remove( uuid, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_address_mappings_remove( const std::string& uuid, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_address_mappings_removeRequest> __request( new xtreemfs_address_mappings_removeRequest( uuid ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_address_mappings_removeResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_address_mappings_removeResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_address_mappings_removeResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { return xtreemfs_address_mappings_set( address_mappings, static_cast<uint64_t>( -1 ) ); }
        virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_address_mappings_setRequest> __request( new xtreemfs_address_mappings_setRequest( address_mappings ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_address_mappings_setResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_address_mappings_setResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_address_mappings_setResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); uint64_t _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_checkpoint() { xtreemfs_checkpoint( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_checkpoint( uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_checkpointRequest> __request( new xtreemfs_checkpointRequest() ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_checkpointResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_checkpointResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_discover_dir( org::xtreemfs::interfaces::DirService& dir_service ) { xtreemfs_discover_dir( dir_service, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_discover_dir( org::xtreemfs::interfaces::DirService& dir_service, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_discover_dirRequest> __request( new xtreemfs_discover_dirRequest() ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_discover_dirResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_discover_dirResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_discover_dirResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); dir_service = __response->get_dir_service(); }

        virtual uint64_t xtreemfs_global_time_s_get() { return xtreemfs_global_time_s_get( static_cast<uint64_t>( -1 ) ); }
        virtual uint64_t xtreemfs_global_time_s_get( uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_global_time_s_getRequest> __request( new xtreemfs_global_time_s_getRequest() ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_global_time_s_getResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_global_time_s_getResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_global_time_s_getResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); uint64_t _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_replication_to_master() { xtreemfs_replication_to_master( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replication_to_master( uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterRequest> __request( new xtreemfs_replication_to_masterRequest() ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_replication_to_masterResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_service_deregister( const std::string& uuid ) { xtreemfs_service_deregister( uuid, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_deregister( const std::string& uuid, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_service_deregisterRequest> __request( new xtreemfs_service_deregisterRequest( uuid ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_service_deregisterResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_service_deregisterResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_service_deregisterResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services ) { xtreemfs_service_get_by_name( name, services, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_service_get_by_nameRequest> __request( new xtreemfs_service_get_by_nameRequest( name ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_service_get_by_nameResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_service_get_by_nameResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_service_get_by_nameResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); services = __response->get_services(); }

        virtual void xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services ) { xtreemfs_service_get_by_type( type, services, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_service_get_by_typeRequest> __request( new xtreemfs_service_get_by_typeRequest( type ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_service_get_by_typeResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_service_get_by_typeResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_service_get_by_typeResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); services = __response->get_services(); }

        virtual void xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services ) { xtreemfs_service_get_by_uuid( uuid, services, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_service_get_by_uuidRequest> __request( new xtreemfs_service_get_by_uuidRequest( uuid ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_service_get_by_uuidResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_service_get_by_uuidResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_service_get_by_uuidResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); services = __response->get_services(); }

        virtual void xtreemfs_service_offline( const std::string& uuid ) { xtreemfs_service_offline( uuid, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_offline( const std::string& uuid, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_service_offlineRequest> __request( new xtreemfs_service_offlineRequest( uuid ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_service_offlineResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_service_offlineResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_service_offlineResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service ) { return xtreemfs_service_register( service, static_cast<uint64_t>( -1 ) ); }
        virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_service_registerRequest> __request( new xtreemfs_service_registerRequest( service ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_service_registerResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_service_registerResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_service_registerResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); uint64_t _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_shutdown() { xtreemfs_shutdown( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_shutdownRequest> __request( new xtreemfs_shutdownRequest() ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_shutdownResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_shutdownResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }


        // Request/response pair definitions for the operations in DIRInterface

        class xtreemfs_address_mappings_getResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_getResponse() { }
          xtreemfs_address_mappings_getResponse( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) : address_mappings( address_mappings ) { }
          virtual ~xtreemfs_address_mappings_getResponse() { }

          void set_address_mappings( const org::xtreemfs::interfaces::AddressMappingSet&  address_mappings ) { this->address_mappings = address_mappings; }
          const org::xtreemfs::interfaces::AddressMappingSet& get_address_mappings() const { return address_mappings; }

          bool operator==( const xtreemfs_address_mappings_getResponse& other ) const { return address_mappings == other.address_mappings; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_address_mappings_getResponse, 2010012012 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "address_mappings", 0, address_mappings ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "address_mappings", 0, address_mappings ); }

        protected:
          org::xtreemfs::interfaces::AddressMappingSet address_mappings;
        };

        class xtreemfs_address_mappings_getRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_getRequest() { }
          xtreemfs_address_mappings_getRequest( const std::string& uuid ) : uuid( uuid ) { }
          xtreemfs_address_mappings_getRequest( const char* uuid, size_t uuid_len ) : uuid( uuid, uuid_len ) { }
          virtual ~xtreemfs_address_mappings_getRequest() { }

          void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
          void set_uuid( const char* uuid, size_t uuid_len ) { this->uuid.assign( uuid, uuid_len ); }
          const std::string& get_uuid() const { return uuid; }

          bool operator==( const xtreemfs_address_mappings_getRequest& other ) const { return uuid == other.uuid; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_address_mappings_getRequest, 2010012012 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "uuid", 0, uuid ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "uuid", 0, uuid ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_address_mappings_getResponse; }


        protected:
          std::string uuid;
        };

        class xtreemfs_address_mappings_removeResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_removeResponse() { }
          virtual ~xtreemfs_address_mappings_removeResponse() { }

          bool operator==( const xtreemfs_address_mappings_removeResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_address_mappings_removeResponse, 2010012013 );

        };

        class xtreemfs_address_mappings_removeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_removeRequest() { }
          xtreemfs_address_mappings_removeRequest( const std::string& uuid ) : uuid( uuid ) { }
          xtreemfs_address_mappings_removeRequest( const char* uuid, size_t uuid_len ) : uuid( uuid, uuid_len ) { }
          virtual ~xtreemfs_address_mappings_removeRequest() { }

          void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
          void set_uuid( const char* uuid, size_t uuid_len ) { this->uuid.assign( uuid, uuid_len ); }
          const std::string& get_uuid() const { return uuid; }

          bool operator==( const xtreemfs_address_mappings_removeRequest& other ) const { return uuid == other.uuid; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_address_mappings_removeRequest, 2010012013 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "uuid", 0, uuid ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "uuid", 0, uuid ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_address_mappings_removeResponse; }


        protected:
          std::string uuid;
        };

        class xtreemfs_address_mappings_setResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_setResponse() : _return_value( 0 ) { }
          xtreemfs_address_mappings_setResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
          virtual ~xtreemfs_address_mappings_setResponse() { }

          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
          uint64_t get__return_value() const { return _return_value; }

          bool operator==( const xtreemfs_address_mappings_setResponse& other ) const { return _return_value == other._return_value; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_address_mappings_setResponse, 2010012014 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint64( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { _return_value = unmarshaller.readUint64( "_return_value", 0 ); }

        protected:
          uint64_t _return_value;
        };

        class xtreemfs_address_mappings_setRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_setRequest() { }
          xtreemfs_address_mappings_setRequest( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) : address_mappings( address_mappings ) { }
          virtual ~xtreemfs_address_mappings_setRequest() { }

          void set_address_mappings( const org::xtreemfs::interfaces::AddressMappingSet&  address_mappings ) { this->address_mappings = address_mappings; }
          const org::xtreemfs::interfaces::AddressMappingSet& get_address_mappings() const { return address_mappings; }

          bool operator==( const xtreemfs_address_mappings_setRequest& other ) const { return address_mappings == other.address_mappings; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_address_mappings_setRequest, 2010012014 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "address_mappings", 0, address_mappings ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "address_mappings", 0, address_mappings ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_address_mappings_setResponse; }


        protected:
          org::xtreemfs::interfaces::AddressMappingSet address_mappings;
        };

        class xtreemfs_checkpointResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointResponse() { }
          virtual ~xtreemfs_checkpointResponse() { }

          bool operator==( const xtreemfs_checkpointResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_checkpointResponse, 2010012015 );

        };

        class xtreemfs_checkpointRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointRequest() { }
          virtual ~xtreemfs_checkpointRequest() { }

          bool operator==( const xtreemfs_checkpointRequest& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_checkpointRequest, 2010012015 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_checkpointResponse; }

        };

        class xtreemfs_discover_dirResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_discover_dirResponse() { }
          xtreemfs_discover_dirResponse( const org::xtreemfs::interfaces::DirService& dir_service ) : dir_service( dir_service ) { }
          virtual ~xtreemfs_discover_dirResponse() { }

          void set_dir_service( const org::xtreemfs::interfaces::DirService&  dir_service ) { this->dir_service = dir_service; }
          const org::xtreemfs::interfaces::DirService& get_dir_service() const { return dir_service; }

          bool operator==( const xtreemfs_discover_dirResponse& other ) const { return dir_service == other.dir_service; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_discover_dirResponse, 2010012016 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "dir_service", 0, dir_service ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "dir_service", 0, dir_service ); }

        protected:
          org::xtreemfs::interfaces::DirService dir_service;
        };

        class xtreemfs_discover_dirRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_discover_dirRequest() { }
          virtual ~xtreemfs_discover_dirRequest() { }

          bool operator==( const xtreemfs_discover_dirRequest& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_discover_dirRequest, 2010012016 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_discover_dirResponse; }

        };

        class xtreemfs_global_time_s_getResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_global_time_s_getResponse() : _return_value( 0 ) { }
          xtreemfs_global_time_s_getResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
          virtual ~xtreemfs_global_time_s_getResponse() { }

          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
          uint64_t get__return_value() const { return _return_value; }

          bool operator==( const xtreemfs_global_time_s_getResponse& other ) const { return _return_value == other._return_value; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_global_time_s_getResponse, 2010012017 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint64( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { _return_value = unmarshaller.readUint64( "_return_value", 0 ); }

        protected:
          uint64_t _return_value;
        };

        class xtreemfs_global_time_s_getRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_global_time_s_getRequest() { }
          virtual ~xtreemfs_global_time_s_getRequest() { }

          bool operator==( const xtreemfs_global_time_s_getRequest& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_global_time_s_getRequest, 2010012017 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_global_time_s_getResponse; }

        };

        class xtreemfs_replication_to_masterResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_replication_to_masterResponse() { }
          virtual ~xtreemfs_replication_to_masterResponse() { }

          bool operator==( const xtreemfs_replication_to_masterResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replication_to_masterResponse, 2010012018 );

        };

        class xtreemfs_replication_to_masterRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_replication_to_masterRequest() { }
          virtual ~xtreemfs_replication_to_masterRequest() { }

          bool operator==( const xtreemfs_replication_to_masterRequest& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replication_to_masterRequest, 2010012018 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_replication_to_masterResponse; }

        };

        class xtreemfs_service_deregisterResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_deregisterResponse() { }
          virtual ~xtreemfs_service_deregisterResponse() { }

          bool operator==( const xtreemfs_service_deregisterResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_deregisterResponse, 2010012023 );

        };

        class xtreemfs_service_deregisterRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_deregisterRequest() { }
          xtreemfs_service_deregisterRequest( const std::string& uuid ) : uuid( uuid ) { }
          xtreemfs_service_deregisterRequest( const char* uuid, size_t uuid_len ) : uuid( uuid, uuid_len ) { }
          virtual ~xtreemfs_service_deregisterRequest() { }

          void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
          void set_uuid( const char* uuid, size_t uuid_len ) { this->uuid.assign( uuid, uuid_len ); }
          const std::string& get_uuid() const { return uuid; }

          bool operator==( const xtreemfs_service_deregisterRequest& other ) const { return uuid == other.uuid; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_deregisterRequest, 2010012023 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "uuid", 0, uuid ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "uuid", 0, uuid ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_service_deregisterResponse; }


        protected:
          std::string uuid;
        };

        class xtreemfs_service_get_by_nameResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_nameResponse() { }
          xtreemfs_service_get_by_nameResponse( const org::xtreemfs::interfaces::ServiceSet& services ) : services( services ) { }
          virtual ~xtreemfs_service_get_by_nameResponse() { }

          void set_services( const org::xtreemfs::interfaces::ServiceSet&  services ) { this->services = services; }
          const org::xtreemfs::interfaces::ServiceSet& get_services() const { return services; }

          bool operator==( const xtreemfs_service_get_by_nameResponse& other ) const { return services == other.services; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_get_by_nameResponse, 2010012021 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "services", 0, services ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "services", 0, services ); }

        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };

        class xtreemfs_service_get_by_nameRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_nameRequest() { }
          xtreemfs_service_get_by_nameRequest( const std::string& name ) : name( name ) { }
          xtreemfs_service_get_by_nameRequest( const char* name, size_t name_len ) : name( name, name_len ) { }
          virtual ~xtreemfs_service_get_by_nameRequest() { }

          void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
          void set_name( const char* name, size_t name_len ) { this->name.assign( name, name_len ); }
          const std::string& get_name() const { return name; }

          bool operator==( const xtreemfs_service_get_by_nameRequest& other ) const { return name == other.name; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_get_by_nameRequest, 2010012021 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "name", 0, name ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "name", 0, name ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_service_get_by_nameResponse; }


        protected:
          std::string name;
        };

        class xtreemfs_service_get_by_typeResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_typeResponse() { }
          xtreemfs_service_get_by_typeResponse( const org::xtreemfs::interfaces::ServiceSet& services ) : services( services ) { }
          virtual ~xtreemfs_service_get_by_typeResponse() { }

          void set_services( const org::xtreemfs::interfaces::ServiceSet&  services ) { this->services = services; }
          const org::xtreemfs::interfaces::ServiceSet& get_services() const { return services; }

          bool operator==( const xtreemfs_service_get_by_typeResponse& other ) const { return services == other.services; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_get_by_typeResponse, 2010012019 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "services", 0, services ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "services", 0, services ); }

        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };

        class xtreemfs_service_get_by_typeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_typeRequest() : type( SERVICE_TYPE_MIXED ) { }
          xtreemfs_service_get_by_typeRequest( org::xtreemfs::interfaces::ServiceType type ) : type( type ) { }
          virtual ~xtreemfs_service_get_by_typeRequest() { }

          void set_type( org::xtreemfs::interfaces::ServiceType type ) { this->type = type; }
          org::xtreemfs::interfaces::ServiceType get_type() const { return type; }

          bool operator==( const xtreemfs_service_get_by_typeRequest& other ) const { return type == other.type; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_get_by_typeRequest, 2010012019 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeInt32( "type", 0, static_cast<int32_t>( type ) ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { type = static_cast<org::xtreemfs::interfaces::ServiceType>( unmarshaller.readInt32( "type", 0 ) ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_service_get_by_typeResponse; }


        protected:
          org::xtreemfs::interfaces::ServiceType type;
        };

        class xtreemfs_service_get_by_uuidResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_uuidResponse() { }
          xtreemfs_service_get_by_uuidResponse( const org::xtreemfs::interfaces::ServiceSet& services ) : services( services ) { }
          virtual ~xtreemfs_service_get_by_uuidResponse() { }

          void set_services( const org::xtreemfs::interfaces::ServiceSet&  services ) { this->services = services; }
          const org::xtreemfs::interfaces::ServiceSet& get_services() const { return services; }

          bool operator==( const xtreemfs_service_get_by_uuidResponse& other ) const { return services == other.services; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_get_by_uuidResponse, 2010012020 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "services", 0, services ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "services", 0, services ); }

        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };

        class xtreemfs_service_get_by_uuidRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_uuidRequest() { }
          xtreemfs_service_get_by_uuidRequest( const std::string& uuid ) : uuid( uuid ) { }
          xtreemfs_service_get_by_uuidRequest( const char* uuid, size_t uuid_len ) : uuid( uuid, uuid_len ) { }
          virtual ~xtreemfs_service_get_by_uuidRequest() { }

          void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
          void set_uuid( const char* uuid, size_t uuid_len ) { this->uuid.assign( uuid, uuid_len ); }
          const std::string& get_uuid() const { return uuid; }

          bool operator==( const xtreemfs_service_get_by_uuidRequest& other ) const { return uuid == other.uuid; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_get_by_uuidRequest, 2010012020 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "uuid", 0, uuid ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "uuid", 0, uuid ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_service_get_by_uuidResponse; }


        protected:
          std::string uuid;
        };

        class xtreemfs_service_offlineResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_offlineResponse() { }
          virtual ~xtreemfs_service_offlineResponse() { }

          bool operator==( const xtreemfs_service_offlineResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_offlineResponse, 2010012024 );

        };

        class xtreemfs_service_offlineRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_offlineRequest() { }
          xtreemfs_service_offlineRequest( const std::string& uuid ) : uuid( uuid ) { }
          xtreemfs_service_offlineRequest( const char* uuid, size_t uuid_len ) : uuid( uuid, uuid_len ) { }
          virtual ~xtreemfs_service_offlineRequest() { }

          void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
          void set_uuid( const char* uuid, size_t uuid_len ) { this->uuid.assign( uuid, uuid_len ); }
          const std::string& get_uuid() const { return uuid; }

          bool operator==( const xtreemfs_service_offlineRequest& other ) const { return uuid == other.uuid; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_offlineRequest, 2010012024 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "uuid", 0, uuid ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "uuid", 0, uuid ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_service_offlineResponse; }


        protected:
          std::string uuid;
        };

        class xtreemfs_service_registerResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_registerResponse() : _return_value( 0 ) { }
          xtreemfs_service_registerResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
          virtual ~xtreemfs_service_registerResponse() { }

          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
          uint64_t get__return_value() const { return _return_value; }

          bool operator==( const xtreemfs_service_registerResponse& other ) const { return _return_value == other._return_value; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_registerResponse, 2010012022 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint64( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { _return_value = unmarshaller.readUint64( "_return_value", 0 ); }

        protected:
          uint64_t _return_value;
        };

        class xtreemfs_service_registerRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_registerRequest() { }
          xtreemfs_service_registerRequest( const org::xtreemfs::interfaces::Service& service ) : service( service ) { }
          virtual ~xtreemfs_service_registerRequest() { }

          void set_service( const org::xtreemfs::interfaces::Service&  service ) { this->service = service; }
          const org::xtreemfs::interfaces::Service& get_service() const { return service; }

          bool operator==( const xtreemfs_service_registerRequest& other ) const { return service == other.service; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_service_registerRequest, 2010012022 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "service", 0, service ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "service", 0, service ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_service_registerResponse; }


        protected:
          org::xtreemfs::interfaces::Service service;
        };

        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownResponse() { }
          virtual ~xtreemfs_shutdownResponse() { }

          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 2010012025 );

        };

        class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownRequest() { }
          virtual ~xtreemfs_shutdownRequest() { }

          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 2010012025 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_shutdownResponse; }

        };

          class ConcurrentModificationException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            ConcurrentModificationException() { }
          ConcurrentModificationException( const std::string& stack_trace ) : stack_trace( stack_trace ) { }
          ConcurrentModificationException( const char* stack_trace, size_t stack_trace_len ) : stack_trace( stack_trace, stack_trace_len ) { }
            ConcurrentModificationException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~ConcurrentModificationException() throw() { }

          void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
          void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
          const std::string& get_stack_trace() const { return stack_trace; }

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new ConcurrentModificationException( stack_trace); }
            virtual void throwStackClone() const { throw ConcurrentModificationException( stack_trace); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "stack_trace", 0, stack_trace ); }

        protected:
          std::string stack_trace;
          };

          class DIRException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            DIRException() : error_code( 0 ) { }
          DIRException( uint32_t error_code, const std::string& error_message, const std::string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
          DIRException( uint32_t error_code, const char* error_message, size_t error_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), error_message( error_message, error_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
            DIRException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~DIRException() throw() { }

          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          uint32_t get_error_code() const { return error_code; }
          void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
          void set_error_message( const char* error_message, size_t error_message_len ) { this->error_message.assign( error_message, error_message_len ); }
          const std::string& get_error_message() const { return error_message; }
          void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
          void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
          const std::string& get_stack_trace() const { return stack_trace; }

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new DIRException( error_code, error_message, stack_trace); }
            virtual void throwStackClone() const { throw DIRException( error_code, error_message, stack_trace); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { error_code = unmarshaller.readUint32( "error_code", 0 ); unmarshaller.readString( "error_message", 0, error_message ); unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint32( "error_code", 0, error_code ); marshaller.writeString( "error_message", 0, error_message ); marshaller.writeString( "stack_trace", 0, stack_trace ); }

        protected:
          uint32_t error_code;
          std::string error_message;
          std::string stack_trace;
          };

          class InvalidArgumentException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            InvalidArgumentException() { }
          InvalidArgumentException( const std::string& error_message ) : error_message( error_message ) { }
          InvalidArgumentException( const char* error_message, size_t error_message_len ) : error_message( error_message, error_message_len ) { }
            InvalidArgumentException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~InvalidArgumentException() throw() { }

          void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
          void set_error_message( const char* error_message, size_t error_message_len ) { this->error_message.assign( error_message, error_message_len ); }
          const std::string& get_error_message() const { return error_message; }

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new InvalidArgumentException( error_message); }
            virtual void throwStackClone() const { throw InvalidArgumentException( error_message); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "error_message", 0, error_message ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "error_message", 0, error_message ); }

        protected:
          std::string error_message;
          };

          class ProtocolException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            ProtocolException() : accept_stat( 0 ), error_code( 0 ) { }
          ProtocolException( uint32_t accept_stat, uint32_t error_code, const std::string& stack_trace ) : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace ) { }
          ProtocolException( uint32_t accept_stat, uint32_t error_code, const char* stack_trace, size_t stack_trace_len ) : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace, stack_trace_len ) { }
            ProtocolException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~ProtocolException() throw() { }

          void set_accept_stat( uint32_t accept_stat ) { this->accept_stat = accept_stat; }
          uint32_t get_accept_stat() const { return accept_stat; }
          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          uint32_t get_error_code() const { return error_code; }
          void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
          void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
          const std::string& get_stack_trace() const { return stack_trace; }

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new ProtocolException( accept_stat, error_code, stack_trace); }
            virtual void throwStackClone() const { throw ProtocolException( accept_stat, error_code, stack_trace); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { accept_stat = unmarshaller.readUint32( "accept_stat", 0 ); error_code = unmarshaller.readUint32( "error_code", 0 ); unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint32( "accept_stat", 0, accept_stat ); marshaller.writeUint32( "error_code", 0, error_code ); marshaller.writeString( "stack_trace", 0, stack_trace ); }

        protected:
          uint32_t accept_stat;
          uint32_t error_code;
          std::string stack_trace;
          };

          class RedirectException : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            RedirectException() : port( 0 ) { }
          RedirectException( const std::string& address, uint16_t port ) : address( address ), port( port ) { }
          RedirectException( const char* address, size_t address_len, uint16_t port ) : address( address, address_len ), port( port ) { }
            RedirectException( const char* what ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~RedirectException() throw() { }

          void set_address( const std::string& address ) { set_address( address.c_str(), address.size() ); }
          void set_address( const char* address, size_t address_len ) { this->address.assign( address, address_len ); }
          const std::string& get_address() const { return address; }
          void set_port( uint16_t port ) { this->port = port; }
          uint16_t get_port() const { return port; }

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new RedirectException( address, port); }
            virtual void throwStackClone() const { throw RedirectException( address, port); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "address", 0, address ); port = unmarshaller.readUint16( "port", 0 ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "address", 0, address ); marshaller.writeUint16( "port", 0, port ); }

        protected:
          std::string address;
          uint16_t port;
          };



        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( DIRInterface, 2010012011 );

        // YIELD::concurrency::EventHandler
        virtual void handleEvent( ::YIELD::concurrency::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.get_type_id() )
            {
              case 2010012012UL: handlextreemfs_address_mappings_getRequest( static_cast<xtreemfs_address_mappings_getRequest&>( ev ) ); return;
              case 2010012013UL: handlextreemfs_address_mappings_removeRequest( static_cast<xtreemfs_address_mappings_removeRequest&>( ev ) ); return;
              case 2010012014UL: handlextreemfs_address_mappings_setRequest( static_cast<xtreemfs_address_mappings_setRequest&>( ev ) ); return;
              case 2010012015UL: handlextreemfs_checkpointRequest( static_cast<xtreemfs_checkpointRequest&>( ev ) ); return;
              case 2010012016UL: handlextreemfs_discover_dirRequest( static_cast<xtreemfs_discover_dirRequest&>( ev ) ); return;
              case 2010012017UL: handlextreemfs_global_time_s_getRequest( static_cast<xtreemfs_global_time_s_getRequest&>( ev ) ); return;
              case 2010012018UL: handlextreemfs_replication_to_masterRequest( static_cast<xtreemfs_replication_to_masterRequest&>( ev ) ); return;
              case 2010012023UL: handlextreemfs_service_deregisterRequest( static_cast<xtreemfs_service_deregisterRequest&>( ev ) ); return;
              case 2010012021UL: handlextreemfs_service_get_by_nameRequest( static_cast<xtreemfs_service_get_by_nameRequest&>( ev ) ); return;
              case 2010012019UL: handlextreemfs_service_get_by_typeRequest( static_cast<xtreemfs_service_get_by_typeRequest&>( ev ) ); return;
              case 2010012020UL: handlextreemfs_service_get_by_uuidRequest( static_cast<xtreemfs_service_get_by_uuidRequest&>( ev ) ); return;
              case 2010012024UL: handlextreemfs_service_offlineRequest( static_cast<xtreemfs_service_offlineRequest&>( ev ) ); return;
              case 2010012022UL: handlextreemfs_service_registerRequest( static_cast<xtreemfs_service_registerRequest&>( ev ) ); return;
              case 2010012025UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
              default: handleUnknownEvent( ev ); return;
            }
          }
          catch( ::YIELD::concurrency::ExceptionResponse* exception_response )
          {
            static_cast< ::YIELD::concurrency::Request& >( ev ).respond( *exception_response );
          }
          catch ( ::YIELD::concurrency::ExceptionResponse& exception_response )
          {
            static_cast< ::YIELD::concurrency::Request& >( ev ).respond( *exception_response.clone() );
          }
          catch ( ::YIELD::platform::Exception& exception )
          {
            static_cast< ::YIELD::concurrency::Request& >( ev ).respond( *( new ::YIELD::concurrency::ExceptionResponse( exception ) ) );
          }

          ::yidl::runtime::Object::decRef( ev );
        }


        // YIELD::concurrency::Interface
          virtual ::YIELD::concurrency::Request* checkRequest( Object& request )
          {
            switch ( request.get_type_id() )
            {
              case 2010012012: return static_cast<xtreemfs_address_mappings_getRequest*>( &request );
              case 2010012013: return static_cast<xtreemfs_address_mappings_removeRequest*>( &request );
              case 2010012014: return static_cast<xtreemfs_address_mappings_setRequest*>( &request );
              case 2010012015: return static_cast<xtreemfs_checkpointRequest*>( &request );
              case 2010012016: return static_cast<xtreemfs_discover_dirRequest*>( &request );
              case 2010012017: return static_cast<xtreemfs_global_time_s_getRequest*>( &request );
              case 2010012018: return static_cast<xtreemfs_replication_to_masterRequest*>( &request );
              case 2010012023: return static_cast<xtreemfs_service_deregisterRequest*>( &request );
              case 2010012021: return static_cast<xtreemfs_service_get_by_nameRequest*>( &request );
              case 2010012019: return static_cast<xtreemfs_service_get_by_typeRequest*>( &request );
              case 2010012020: return static_cast<xtreemfs_service_get_by_uuidRequest*>( &request );
              case 2010012024: return static_cast<xtreemfs_service_offlineRequest*>( &request );
              case 2010012022: return static_cast<xtreemfs_service_registerRequest*>( &request );
              case 2010012025: return static_cast<xtreemfs_shutdownRequest*>( &request );
              default: return NULL;
            }
          }

          virtual ::YIELD::concurrency::Response* checkResponse( Object& response )
          {
            switch ( response.get_type_id() )
            {
              case 2010012012: return static_cast<xtreemfs_address_mappings_getResponse*>( &response );
              case 2010012013: return static_cast<xtreemfs_address_mappings_removeResponse*>( &response );
              case 2010012014: return static_cast<xtreemfs_address_mappings_setResponse*>( &response );
              case 2010012015: return static_cast<xtreemfs_checkpointResponse*>( &response );
              case 2010012016: return static_cast<xtreemfs_discover_dirResponse*>( &response );
              case 2010012017: return static_cast<xtreemfs_global_time_s_getResponse*>( &response );
              case 2010012018: return static_cast<xtreemfs_replication_to_masterResponse*>( &response );
              case 2010012023: return static_cast<xtreemfs_service_deregisterResponse*>( &response );
              case 2010012021: return static_cast<xtreemfs_service_get_by_nameResponse*>( &response );
              case 2010012019: return static_cast<xtreemfs_service_get_by_typeResponse*>( &response );
              case 2010012020: return static_cast<xtreemfs_service_get_by_uuidResponse*>( &response );
              case 2010012024: return static_cast<xtreemfs_service_offlineResponse*>( &response );
              case 2010012022: return static_cast<xtreemfs_service_registerResponse*>( &response );
              case 2010012025: return static_cast<xtreemfs_shutdownResponse*>( &response );
              case 2010012031: return static_cast<ConcurrentModificationException*>( &response );
              case 2010012034: return static_cast<DIRException*>( &response );
              case 2010012032: return static_cast<InvalidArgumentException*>( &response );
              case 2010012033: return static_cast<ProtocolException*>( &response );
              case 2010012035: return static_cast<RedirectException*>( &response );
              default: return NULL;
            }
          }

          virtual ::YIELD::concurrency::auto_Request createRequest( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012012: return new xtreemfs_address_mappings_getRequest;
              case 2010012013: return new xtreemfs_address_mappings_removeRequest;
              case 2010012014: return new xtreemfs_address_mappings_setRequest;
              case 2010012015: return new xtreemfs_checkpointRequest;
              case 2010012016: return new xtreemfs_discover_dirRequest;
              case 2010012017: return new xtreemfs_global_time_s_getRequest;
              case 2010012018: return new xtreemfs_replication_to_masterRequest;
              case 2010012023: return new xtreemfs_service_deregisterRequest;
              case 2010012021: return new xtreemfs_service_get_by_nameRequest;
              case 2010012019: return new xtreemfs_service_get_by_typeRequest;
              case 2010012020: return new xtreemfs_service_get_by_uuidRequest;
              case 2010012024: return new xtreemfs_service_offlineRequest;
              case 2010012022: return new xtreemfs_service_registerRequest;
              case 2010012025: return new xtreemfs_shutdownRequest;
              default: return NULL;
            }
          }

          virtual ::YIELD::concurrency::auto_Response createResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012012: return new xtreemfs_address_mappings_getResponse;
              case 2010012013: return new xtreemfs_address_mappings_removeResponse;
              case 2010012014: return new xtreemfs_address_mappings_setResponse;
              case 2010012015: return new xtreemfs_checkpointResponse;
              case 2010012016: return new xtreemfs_discover_dirResponse;
              case 2010012017: return new xtreemfs_global_time_s_getResponse;
              case 2010012018: return new xtreemfs_replication_to_masterResponse;
              case 2010012023: return new xtreemfs_service_deregisterResponse;
              case 2010012021: return new xtreemfs_service_get_by_nameResponse;
              case 2010012019: return new xtreemfs_service_get_by_typeResponse;
              case 2010012020: return new xtreemfs_service_get_by_uuidResponse;
              case 2010012024: return new xtreemfs_service_offlineResponse;
              case 2010012022: return new xtreemfs_service_registerResponse;
              case 2010012025: return new xtreemfs_shutdownResponse;
              default: return NULL;
            }
          }

          virtual ::YIELD::concurrency::auto_ExceptionResponse createExceptionResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012031: return new ConcurrentModificationException;
              case 2010012034: return new DIRException;
              case 2010012032: return new InvalidArgumentException;
              case 2010012033: return new ProtocolException;
              case 2010012035: return new RedirectException;
              default: return NULL;
            }
          }



      protected:
        virtual void handlextreemfs_address_mappings_getRequest( xtreemfs_address_mappings_getRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_address_mappings_getResponse> resp( new xtreemfs_address_mappings_getResponse ); org::xtreemfs::interfaces::AddressMappingSet address_mappings; _xtreemfs_address_mappings_get( req.get_uuid(), address_mappings ); resp->set_address_mappings( address_mappings ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_address_mappings_removeRequest( xtreemfs_address_mappings_removeRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_address_mappings_removeResponse> resp( new xtreemfs_address_mappings_removeResponse ); _xtreemfs_address_mappings_remove( req.get_uuid() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_address_mappings_setRequest( xtreemfs_address_mappings_setRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_address_mappings_setResponse> resp( new xtreemfs_address_mappings_setResponse ); uint64_t _return_value = _xtreemfs_address_mappings_set( req.get_address_mappings() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> resp( new xtreemfs_checkpointResponse ); _xtreemfs_checkpoint(); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_discover_dirRequest( xtreemfs_discover_dirRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_discover_dirResponse> resp( new xtreemfs_discover_dirResponse ); org::xtreemfs::interfaces::DirService dir_service; _xtreemfs_discover_dir( dir_service ); resp->set_dir_service( dir_service ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_global_time_s_getRequest( xtreemfs_global_time_s_getRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_global_time_s_getResponse> resp( new xtreemfs_global_time_s_getResponse ); uint64_t _return_value = _xtreemfs_global_time_s_get(); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_replication_to_masterRequest( xtreemfs_replication_to_masterRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> resp( new xtreemfs_replication_to_masterResponse ); _xtreemfs_replication_to_master(); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_service_deregisterRequest( xtreemfs_service_deregisterRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_service_deregisterResponse> resp( new xtreemfs_service_deregisterResponse ); _xtreemfs_service_deregister( req.get_uuid() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_service_get_by_nameRequest( xtreemfs_service_get_by_nameRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_service_get_by_nameResponse> resp( new xtreemfs_service_get_by_nameResponse ); org::xtreemfs::interfaces::ServiceSet services; _xtreemfs_service_get_by_name( req.get_name(), services ); resp->set_services( services ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_service_get_by_typeRequest( xtreemfs_service_get_by_typeRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_service_get_by_typeResponse> resp( new xtreemfs_service_get_by_typeResponse ); org::xtreemfs::interfaces::ServiceSet services; _xtreemfs_service_get_by_type( req.get_type(), services ); resp->set_services( services ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_service_get_by_uuidRequest( xtreemfs_service_get_by_uuidRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_service_get_by_uuidResponse> resp( new xtreemfs_service_get_by_uuidResponse ); org::xtreemfs::interfaces::ServiceSet services; _xtreemfs_service_get_by_uuid( req.get_uuid(), services ); resp->set_services( services ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_service_offlineRequest( xtreemfs_service_offlineRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_service_offlineResponse> resp( new xtreemfs_service_offlineResponse ); _xtreemfs_service_offline( req.get_uuid() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_service_registerRequest( xtreemfs_service_registerRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_service_registerResponse> resp( new xtreemfs_service_registerResponse ); uint64_t _return_value = _xtreemfs_service_register( req.get_service() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> resp( new xtreemfs_shutdownResponse ); _xtreemfs_shutdown(); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }

      virtual void _xtreemfs_address_mappings_get( const std::string& , org::xtreemfs::interfaces::AddressMappingSet&  ) { }
        virtual void _xtreemfs_address_mappings_remove( const std::string&  ) { }
        virtual uint64_t _xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet&  ) { return 0; }
        virtual void _xtreemfs_checkpoint() { }
        virtual void _xtreemfs_discover_dir( org::xtreemfs::interfaces::DirService&  ) { }
        virtual uint64_t _xtreemfs_global_time_s_get() { return 0; }
        virtual void _xtreemfs_replication_to_master() { }
        virtual void _xtreemfs_service_deregister( const std::string&  ) { }
        virtual void _xtreemfs_service_get_by_name( const std::string& , org::xtreemfs::interfaces::ServiceSet&  ) { }
        virtual void _xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType, org::xtreemfs::interfaces::ServiceSet&  ) { }
        virtual void _xtreemfs_service_get_by_uuid( const std::string& , org::xtreemfs::interfaces::ServiceSet&  ) { }
        virtual void _xtreemfs_service_offline( const std::string&  ) { }
        virtual uint64_t _xtreemfs_service_register( const org::xtreemfs::interfaces::Service&  ) { return 0; }
        virtual void _xtreemfs_shutdown() { }
      };

      // Use this macro in an implementation class to get all of the prototypes for the operations in DIRInterface
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_PROTOTYPES \
      virtual void _xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings );\
      virtual void _xtreemfs_address_mappings_remove( const std::string& uuid );\
      virtual uint64_t _xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings );\
      virtual void _xtreemfs_checkpoint();\
      virtual void _xtreemfs_discover_dir( org::xtreemfs::interfaces::DirService& dir_service );\
      virtual uint64_t _xtreemfs_global_time_s_get();\
      virtual void _xtreemfs_replication_to_master();\
      virtual void _xtreemfs_service_deregister( const std::string& uuid );\
      virtual void _xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void _xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void _xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void _xtreemfs_service_offline( const std::string& uuid );\
      virtual uint64_t _xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service );\
      virtual void _xtreemfs_shutdown();

      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handlextreemfs_address_mappings_getRequest( xtreemfs_address_mappings_getRequest& req );\
      virtual void handlextreemfs_address_mappings_removeRequest( xtreemfs_address_mappings_removeRequest& req );\
      virtual void handlextreemfs_address_mappings_setRequest( xtreemfs_address_mappings_setRequest& req );\
      virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& req );\
      virtual void handlextreemfs_discover_dirRequest( xtreemfs_discover_dirRequest& req );\
      virtual void handlextreemfs_global_time_s_getRequest( xtreemfs_global_time_s_getRequest& req );\
      virtual void handlextreemfs_replication_to_masterRequest( xtreemfs_replication_to_masterRequest& req );\
      virtual void handlextreemfs_service_deregisterRequest( xtreemfs_service_deregisterRequest& req );\
      virtual void handlextreemfs_service_get_by_nameRequest( xtreemfs_service_get_by_nameRequest& req );\
      virtual void handlextreemfs_service_get_by_typeRequest( xtreemfs_service_get_by_typeRequest& req );\
      virtual void handlextreemfs_service_get_by_uuidRequest( xtreemfs_service_get_by_uuidRequest& req );\
      virtual void handlextreemfs_service_offlineRequest( xtreemfs_service_offlineRequest& req );\
      virtual void handlextreemfs_service_registerRequest( xtreemfs_service_registerRequest& req );\
      virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req );

    };



  };



};
#endif
