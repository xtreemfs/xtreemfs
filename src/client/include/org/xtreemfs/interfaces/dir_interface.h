// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_INTERFACES_DIR_INTERFACE_H_
#define _ORG_XTREEMFS_INTERFACES_DIR_INTERFACE_H_

#include "constants.h"
#include "types.h"
#include "yield/arch.h"
#include "yield/platform.h"
#include <map>
#include <vector>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      enum ServiceType { SERVICE_TYPE_MRC = 1, SERVICE_TYPE_OSD = 2, SERVICE_TYPE_VOLUME = 3 };


      class AddressMapping : public YIELD::Object
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

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( AddressMapping, 1030 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "version" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "protocol" ), protocol ); input_stream.readString( YIELD::StructuredStream::Declaration( "address" ), address ); port = input_stream.readUint16( YIELD::StructuredStream::Declaration( "port" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "match_network" ), match_network ); ttl_s = input_stream.readUint32( YIELD::StructuredStream::Declaration( "ttl_s" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "uri" ), uri ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "version" ), version ); output_stream.writeString( YIELD::StructuredStream::Declaration( "protocol" ), protocol ); output_stream.writeString( YIELD::StructuredStream::Declaration( "address" ), address ); output_stream.writeUint16( YIELD::StructuredStream::Declaration( "port" ), port ); output_stream.writeString( YIELD::StructuredStream::Declaration( "match_network" ), match_network ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "ttl_s" ), ttl_s ); output_stream.writeString( YIELD::StructuredStream::Declaration( "uri" ), uri ); }

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

      class AddressMappingSet : public std::vector<org::xtreemfs::interfaces::AddressMapping>, public YIELD::Object
      {
      public:
        AddressMappingSet() { }
        AddressMappingSet( const org::xtreemfs::interfaces::AddressMapping& first_value ) { std::vector<org::xtreemfs::interfaces::AddressMapping>::push_back( first_value ); }
        AddressMappingSet( size_type size ) : std::vector<org::xtreemfs::interfaces::AddressMapping>( size ) { }
        virtual ~AddressMappingSet() { }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( AddressMappingSet, 1031 );
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::AddressMapping value; input_stream.readStruct( YIELD::StructuredStream::Declaration( "value" ), &value ); push_back( value ); }
        uint64_t get_size() const { return std::vector<org::xtreemfs::interfaces::AddressMapping>::size(); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "value" ), ( *this )[value_i] ); } }
      };

      class ServiceDataMap : public std::map<std::string,std::string>, public YIELD::Object
      {
      public:
        virtual ~ServiceDataMap() { }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( ServiceDataMap, 1032 );
        uint64_t get_size() const { return std::map<std::string, std::string>::size(); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { std::string key; input_stream.readString( YIELD::StructuredStream::Declaration( 0, static_cast<uint32_t>( 0 ) ), key ); if ( !key.empty() ) { std::string value; input_stream.readString( YIELD::StructuredStream::Declaration( key.c_str(), static_cast<uint32_t>( 0 ) ), value ); ( *this )[key] = value; } }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { for ( iterator i = begin(); i != end(); i++ ) { output_stream.writeString( YIELD::StructuredStream::Declaration( i->first.c_str(), static_cast<uint32_t>( 0 ) ), i->second ); } }
      };

      class Service : public YIELD::Object
      {
      public:
        Service() : type( SERVICE_TYPE_MRC ), version( 0 ), last_updated_s( 0 ) { }
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

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( Service, 1034 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { type = ( org::xtreemfs::interfaces::ServiceType )input_stream.readInt32( YIELD::StructuredStream::Declaration( "type" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "version" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); last_updated_s = input_stream.readUint64( YIELD::StructuredStream::Declaration( "last_updated_s" ) ); input_stream.readMap( YIELD::StructuredStream::Declaration( "data" ), &data ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeInt32( YIELD::StructuredStream::Declaration( "type" ), type ); output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "version" ), version ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "last_updated_s" ), last_updated_s ); output_stream.writeMap( YIELD::StructuredStream::Declaration( "data" ), data ); }

      protected:
        org::xtreemfs::interfaces::ServiceType type;
        std::string uuid;
        uint64_t version;
        std::string name;
        uint64_t last_updated_s;
        org::xtreemfs::interfaces::ServiceDataMap data;
      };

      class ServiceSet : public std::vector<org::xtreemfs::interfaces::Service>, public YIELD::Object
      {
      public:
        ServiceSet() { }
        ServiceSet( const org::xtreemfs::interfaces::Service& first_value ) { std::vector<org::xtreemfs::interfaces::Service>::push_back( first_value ); }
        ServiceSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Service>( size ) { }
        virtual ~ServiceSet() { }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( ServiceSet, 1035 );
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::Service value; input_stream.readStruct( YIELD::StructuredStream::Declaration( "value" ), &value ); push_back( value ); }
        uint64_t get_size() const { return std::vector<org::xtreemfs::interfaces::Service>::size(); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "value" ), ( *this )[value_i] ); } }
      };



      #ifndef ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS YIELD::Interface
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
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS YIELD::Request
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
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS YIELD::Response
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
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS YIELD::ExceptionResponse
      #endif
      #endif



      class DIRInterface : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS
      {
      public:
        DIRInterface() { }
        virtual ~DIRInterface() { }


      const static uint32_t DEFAULT_ONCRPC_PORT = 32638;
      const static uint32_t DEFAULT_ONCRPCS_PORT = 32638;
      const static uint32_t DEFAULT_HTTP_PORT = 30638;


        virtual void xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { xtreemfs_address_mappings_get( uuid, address_mappings, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_address_mappings_getRequest> __request( new xtreemfs_address_mappings_getRequest( uuid ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_address_mappings_getResponse> __response = __response_queue->dequeue_typed<xtreemfs_address_mappings_getResponse>( response_timeout_ns ); address_mappings = __response->get_address_mappings(); }

        virtual void xtreemfs_address_mappings_remove( const std::string& uuid ) { xtreemfs_address_mappings_remove( uuid, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_address_mappings_remove( const std::string& uuid, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_address_mappings_removeRequest> __request( new xtreemfs_address_mappings_removeRequest( uuid ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_address_mappings_removeResponse> __response = __response_queue->dequeue_typed<xtreemfs_address_mappings_removeResponse>( response_timeout_ns ); }

        virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { return xtreemfs_address_mappings_set( address_mappings, static_cast<uint64_t>( -1 ) ); }
        virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_address_mappings_setRequest> __request( new xtreemfs_address_mappings_setRequest( address_mappings ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_address_mappings_setResponse> __response = __response_queue->dequeue_typed<xtreemfs_address_mappings_setResponse>( response_timeout_ns ); uint64_t _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_checkpoint() { xtreemfs_checkpoint( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_checkpoint( uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_checkpointRequest> __request( new xtreemfs_checkpointRequest() ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_checkpointResponse> __response = __response_queue->dequeue_typed<xtreemfs_checkpointResponse>( response_timeout_ns ); }

        virtual uint64_t xtreemfs_global_time_s_get() { return xtreemfs_global_time_s_get( static_cast<uint64_t>( -1 ) ); }
        virtual uint64_t xtreemfs_global_time_s_get( uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_global_time_s_getRequest> __request( new xtreemfs_global_time_s_getRequest() ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_global_time_s_getResponse> __response = __response_queue->dequeue_typed<xtreemfs_global_time_s_getResponse>( response_timeout_ns ); uint64_t _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services ) { xtreemfs_service_get_by_type( type, services, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_service_get_by_typeRequest> __request( new xtreemfs_service_get_by_typeRequest( type ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_service_get_by_typeResponse> __response = __response_queue->dequeue_typed<xtreemfs_service_get_by_typeResponse>( response_timeout_ns ); services = __response->get_services(); }

        virtual void xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services ) { xtreemfs_service_get_by_uuid( uuid, services, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_service_get_by_uuidRequest> __request( new xtreemfs_service_get_by_uuidRequest( uuid ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_service_get_by_uuidResponse> __response = __response_queue->dequeue_typed<xtreemfs_service_get_by_uuidResponse>( response_timeout_ns ); services = __response->get_services(); }

        virtual void xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services ) { xtreemfs_service_get_by_name( name, services, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_service_get_by_nameRequest> __request( new xtreemfs_service_get_by_nameRequest( name ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_service_get_by_nameResponse> __response = __response_queue->dequeue_typed<xtreemfs_service_get_by_nameResponse>( response_timeout_ns ); services = __response->get_services(); }

        virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service ) { return xtreemfs_service_register( service, static_cast<uint64_t>( -1 ) ); }
        virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_service_registerRequest> __request( new xtreemfs_service_registerRequest( service ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_service_registerResponse> __response = __response_queue->dequeue_typed<xtreemfs_service_registerResponse>( response_timeout_ns ); uint64_t _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_service_deregister( const std::string& uuid ) { xtreemfs_service_deregister( uuid, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_deregister( const std::string& uuid, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_service_deregisterRequest> __request( new xtreemfs_service_deregisterRequest( uuid ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_service_deregisterResponse> __response = __response_queue->dequeue_typed<xtreemfs_service_deregisterResponse>( response_timeout_ns ); }

        virtual void xtreemfs_service_offline( const std::string& uuid ) { xtreemfs_service_offline( uuid, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_service_offline( const std::string& uuid, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_service_offlineRequest> __request( new xtreemfs_service_offlineRequest( uuid ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_service_offlineResponse> __response = __response_queue->dequeue_typed<xtreemfs_service_offlineResponse>( response_timeout_ns ); }

        virtual void xtreemfs_shutdown() { xtreemfs_shutdown( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_shutdownRequest> __request( new xtreemfs_shutdownRequest() ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_shutdownResponse> __response = __response_queue->dequeue_typed<xtreemfs_shutdownResponse>( response_timeout_ns ); }


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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_address_mappings_getResponse, 1101 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "address_mappings" ), &address_mappings ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "address_mappings" ), address_mappings ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_address_mappings_getRequest, 1101 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }

        protected:
          std::string uuid;
        };

        class xtreemfs_address_mappings_removeResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_address_mappings_removeResponse() { }
          virtual ~xtreemfs_address_mappings_removeResponse() { }

          bool operator==( const xtreemfs_address_mappings_removeResponse& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_address_mappings_removeResponse, 1103 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_address_mappings_removeRequest, 1103 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_address_mappings_setResponse, 1102 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { _return_value = input_stream.readUint64( YIELD::StructuredStream::Declaration( "_return_value" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_address_mappings_setRequest, 1102 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "address_mappings" ), &address_mappings ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "address_mappings" ), address_mappings ); }

        protected:
          org::xtreemfs::interfaces::AddressMappingSet address_mappings;
        };

        class xtreemfs_checkpointResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointResponse() { }
          virtual ~xtreemfs_checkpointResponse() { }

          bool operator==( const xtreemfs_checkpointResponse& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_checkpointResponse, 1150 );

        };

        class xtreemfs_checkpointRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointRequest() { }
          virtual ~xtreemfs_checkpointRequest() { }

          bool operator==( const xtreemfs_checkpointRequest& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_checkpointRequest, 1150 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_global_time_s_getResponse, 1108 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { _return_value = input_stream.readUint64( YIELD::StructuredStream::Declaration( "_return_value" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }

        protected:
          uint64_t _return_value;
        };

        class xtreemfs_global_time_s_getRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_global_time_s_getRequest() { }
          virtual ~xtreemfs_global_time_s_getRequest() { }

          bool operator==( const xtreemfs_global_time_s_getRequest& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_global_time_s_getRequest, 1108 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_get_by_typeResponse, 1106 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "services" ), &services ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "services" ), services ); }

        protected:
          org::xtreemfs::interfaces::ServiceSet services;
        };

        class xtreemfs_service_get_by_typeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_service_get_by_typeRequest() : type( SERVICE_TYPE_MRC ) { }
          xtreemfs_service_get_by_typeRequest( org::xtreemfs::interfaces::ServiceType type ) : type( type ) { }
          virtual ~xtreemfs_service_get_by_typeRequest() { }

          void set_type( org::xtreemfs::interfaces::ServiceType type ) { this->type = type; }
          org::xtreemfs::interfaces::ServiceType get_type() const { return type; }

          bool operator==( const xtreemfs_service_get_by_typeRequest& other ) const { return type == other.type; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_get_by_typeRequest, 1106 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { type = ( org::xtreemfs::interfaces::ServiceType )input_stream.readInt32( YIELD::StructuredStream::Declaration( "type" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeInt32( YIELD::StructuredStream::Declaration( "type" ), type ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_get_by_uuidResponse, 1107 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "services" ), &services ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "services" ), services ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_get_by_uuidRequest, 1107 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_get_by_nameResponse, 1109 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "services" ), &services ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "services" ), services ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_get_by_nameRequest, 1109 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); }

        protected:
          std::string name;
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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_registerResponse, 1104 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { _return_value = input_stream.readUint64( YIELD::StructuredStream::Declaration( "_return_value" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_registerRequest, 1104 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "service" ), &service ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "service" ), service ); }

        protected:
          org::xtreemfs::interfaces::Service service;
        };

        class xtreemfs_service_deregisterResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_deregisterResponse() { }
          virtual ~xtreemfs_service_deregisterResponse() { }

          bool operator==( const xtreemfs_service_deregisterResponse& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_deregisterResponse, 1105 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_deregisterRequest, 1105 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }

        protected:
          std::string uuid;
        };

        class xtreemfs_service_offlineResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_service_offlineResponse() { }
          virtual ~xtreemfs_service_offlineResponse() { }

          bool operator==( const xtreemfs_service_offlineResponse& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_offlineResponse, 1110 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_service_offlineRequest, 1110 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }

        protected:
          std::string uuid;
        };

        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownResponse() { }
          virtual ~xtreemfs_shutdownResponse() { }

          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 1151 );

        };

        class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownRequest() { }
          virtual ~xtreemfs_shutdownRequest() { }

          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 1151 );

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

            // YIELD::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new ConcurrentModificationException( stack_trace); }
            virtual void throwStackClone() const { throw ConcurrentModificationException( stack_trace); }
          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

        protected:
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

            // YIELD::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new InvalidArgumentException( error_message); }
            virtual void throwStackClone() const { throw InvalidArgumentException( error_message); }
          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); }

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

            // YIELD::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new ProtocolException( accept_stat, error_code, stack_trace); }
            virtual void throwStackClone() const { throw ProtocolException( accept_stat, error_code, stack_trace); }
          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { accept_stat = input_stream.readUint32( YIELD::StructuredStream::Declaration( "accept_stat" ) ); error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "accept_stat" ), accept_stat ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

        protected:
          uint32_t accept_stat;
          uint32_t error_code;
          std::string stack_trace;
          };



        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( DIRInterface, 1100 );

        // YIELD::EventHandler
        virtual void handleEvent( YIELD::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.get_tag() )
            {
              case 1101UL: handlextreemfs_address_mappings_getRequest( static_cast<xtreemfs_address_mappings_getRequest&>( ev ) ); return;
              case 1103UL: handlextreemfs_address_mappings_removeRequest( static_cast<xtreemfs_address_mappings_removeRequest&>( ev ) ); return;
              case 1102UL: handlextreemfs_address_mappings_setRequest( static_cast<xtreemfs_address_mappings_setRequest&>( ev ) ); return;
              case 1150UL: handlextreemfs_checkpointRequest( static_cast<xtreemfs_checkpointRequest&>( ev ) ); return;
              case 1108UL: handlextreemfs_global_time_s_getRequest( static_cast<xtreemfs_global_time_s_getRequest&>( ev ) ); return;
              case 1106UL: handlextreemfs_service_get_by_typeRequest( static_cast<xtreemfs_service_get_by_typeRequest&>( ev ) ); return;
              case 1107UL: handlextreemfs_service_get_by_uuidRequest( static_cast<xtreemfs_service_get_by_uuidRequest&>( ev ) ); return;
              case 1109UL: handlextreemfs_service_get_by_nameRequest( static_cast<xtreemfs_service_get_by_nameRequest&>( ev ) ); return;
              case 1104UL: handlextreemfs_service_registerRequest( static_cast<xtreemfs_service_registerRequest&>( ev ) ); return;
              case 1105UL: handlextreemfs_service_deregisterRequest( static_cast<xtreemfs_service_deregisterRequest&>( ev ) ); return;
              case 1110UL: handlextreemfs_service_offlineRequest( static_cast<xtreemfs_service_offlineRequest&>( ev ) ); return;
              case 1151UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
              default: handleUnknownEvent( ev ); return;
            }
          }
          catch( YIELD::ExceptionResponse* exception_response )
          {
            static_cast<YIELD::Request&>( ev ).respond( *exception_response );
          }
          catch ( YIELD::ExceptionResponse& exception_response )
          {
            static_cast<YIELD::Request&>( ev ).respond( *exception_response.clone() );
          }
          catch ( YIELD::Exception& exc )
          {
            static_cast<YIELD::Request&>( ev ).respond( *( new YIELD::ExceptionResponse( exc ) ) );
          }

          YIELD::Object::decRef( ev );
        }


        // YIELD::Interface
          virtual YIELD::auto_Object<YIELD::Request> createRequest( uint32_t tag )
          {
            switch ( tag )
            {
              case 1101: return new xtreemfs_address_mappings_getRequest;
              case 1103: return new xtreemfs_address_mappings_removeRequest;
              case 1102: return new xtreemfs_address_mappings_setRequest;
              case 1150: return new xtreemfs_checkpointRequest;
              case 1108: return new xtreemfs_global_time_s_getRequest;
              case 1106: return new xtreemfs_service_get_by_typeRequest;
              case 1107: return new xtreemfs_service_get_by_uuidRequest;
              case 1109: return new xtreemfs_service_get_by_nameRequest;
              case 1104: return new xtreemfs_service_registerRequest;
              case 1105: return new xtreemfs_service_deregisterRequest;
              case 1110: return new xtreemfs_service_offlineRequest;
              case 1151: return new xtreemfs_shutdownRequest;
              default: return NULL;
            }
          }

          virtual YIELD::auto_Object<YIELD::Response> createResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 1101: return new xtreemfs_address_mappings_getResponse;
              case 1103: return new xtreemfs_address_mappings_removeResponse;
              case 1102: return new xtreemfs_address_mappings_setResponse;
              case 1150: return new xtreemfs_checkpointResponse;
              case 1108: return new xtreemfs_global_time_s_getResponse;
              case 1106: return new xtreemfs_service_get_by_typeResponse;
              case 1107: return new xtreemfs_service_get_by_uuidResponse;
              case 1109: return new xtreemfs_service_get_by_nameResponse;
              case 1104: return new xtreemfs_service_registerResponse;
              case 1105: return new xtreemfs_service_deregisterResponse;
              case 1110: return new xtreemfs_service_offlineResponse;
              case 1151: return new xtreemfs_shutdownResponse;
              default: return NULL;
            }
          }

          virtual YIELD::auto_Object<YIELD::ExceptionResponse> createExceptionResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 1106: return new ConcurrentModificationException;
              case 1108: return new InvalidArgumentException;
              case 1109: return new ProtocolException;
              default: return NULL;
            }
          }



      protected:
        virtual void handlextreemfs_address_mappings_getRequest( xtreemfs_address_mappings_getRequest& req ) { YIELD::auto_Object<xtreemfs_address_mappings_getResponse> resp( new xtreemfs_address_mappings_getResponse ); org::xtreemfs::interfaces::AddressMappingSet address_mappings; _xtreemfs_address_mappings_get( req.get_uuid(), address_mappings ); resp->set_address_mappings( address_mappings ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_address_mappings_removeRequest( xtreemfs_address_mappings_removeRequest& req ) { YIELD::auto_Object<xtreemfs_address_mappings_removeResponse> resp( new xtreemfs_address_mappings_removeResponse ); _xtreemfs_address_mappings_remove( req.get_uuid() ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_address_mappings_setRequest( xtreemfs_address_mappings_setRequest& req ) { YIELD::auto_Object<xtreemfs_address_mappings_setResponse> resp( new xtreemfs_address_mappings_setResponse ); uint64_t _return_value = _xtreemfs_address_mappings_set( req.get_address_mappings() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& req ) { YIELD::auto_Object<xtreemfs_checkpointResponse> resp( new xtreemfs_checkpointResponse ); _xtreemfs_checkpoint(); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_global_time_s_getRequest( xtreemfs_global_time_s_getRequest& req ) { YIELD::auto_Object<xtreemfs_global_time_s_getResponse> resp( new xtreemfs_global_time_s_getResponse ); uint64_t _return_value = _xtreemfs_global_time_s_get(); resp->set__return_value( _return_value ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_service_get_by_typeRequest( xtreemfs_service_get_by_typeRequest& req ) { YIELD::auto_Object<xtreemfs_service_get_by_typeResponse> resp( new xtreemfs_service_get_by_typeResponse ); org::xtreemfs::interfaces::ServiceSet services; _xtreemfs_service_get_by_type( req.get_type(), services ); resp->set_services( services ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_service_get_by_uuidRequest( xtreemfs_service_get_by_uuidRequest& req ) { YIELD::auto_Object<xtreemfs_service_get_by_uuidResponse> resp( new xtreemfs_service_get_by_uuidResponse ); org::xtreemfs::interfaces::ServiceSet services; _xtreemfs_service_get_by_uuid( req.get_uuid(), services ); resp->set_services( services ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_service_get_by_nameRequest( xtreemfs_service_get_by_nameRequest& req ) { YIELD::auto_Object<xtreemfs_service_get_by_nameResponse> resp( new xtreemfs_service_get_by_nameResponse ); org::xtreemfs::interfaces::ServiceSet services; _xtreemfs_service_get_by_name( req.get_name(), services ); resp->set_services( services ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_service_registerRequest( xtreemfs_service_registerRequest& req ) { YIELD::auto_Object<xtreemfs_service_registerResponse> resp( new xtreemfs_service_registerResponse ); uint64_t _return_value = _xtreemfs_service_register( req.get_service() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_service_deregisterRequest( xtreemfs_service_deregisterRequest& req ) { YIELD::auto_Object<xtreemfs_service_deregisterResponse> resp( new xtreemfs_service_deregisterResponse ); _xtreemfs_service_deregister( req.get_uuid() ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_service_offlineRequest( xtreemfs_service_offlineRequest& req ) { YIELD::auto_Object<xtreemfs_service_offlineResponse> resp( new xtreemfs_service_offlineResponse ); _xtreemfs_service_offline( req.get_uuid() ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req ) { YIELD::auto_Object<xtreemfs_shutdownResponse> resp( new xtreemfs_shutdownResponse ); _xtreemfs_shutdown(); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }

      virtual void _xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { }
        virtual void _xtreemfs_address_mappings_remove( const std::string& uuid ) { }
        virtual uint64_t _xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { return 0; }
        virtual void _xtreemfs_checkpoint() { }
        virtual uint64_t _xtreemfs_global_time_s_get() { return 0; }
        virtual void _xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services ) { }
        virtual void _xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services ) { }
        virtual void _xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services ) { }
        virtual uint64_t _xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service ) { return 0; }
        virtual void _xtreemfs_service_deregister( const std::string& uuid ) { }
        virtual void _xtreemfs_service_offline( const std::string& uuid ) { }
        virtual void _xtreemfs_shutdown() { }
      };

      // Use this macro in an implementation class to get all of the prototypes for the operations in DIRInterface
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_PROTOTYPES \
      virtual void _xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings );\
      virtual void _xtreemfs_address_mappings_remove( const std::string& uuid );\
      virtual uint64_t _xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings );\
      virtual void _xtreemfs_checkpoint();\
      virtual uint64_t _xtreemfs_global_time_s_get();\
      virtual void _xtreemfs_service_get_by_type( org::xtreemfs::interfaces::ServiceType type, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void _xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void _xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual uint64_t _xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service );\
      virtual void _xtreemfs_service_deregister( const std::string& uuid );\
      virtual void _xtreemfs_service_offline( const std::string& uuid );\
      virtual void _xtreemfs_shutdown();

      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handlextreemfs_address_mappings_getRequest( xtreemfs_address_mappings_getRequest& req );\
      virtual void handlextreemfs_address_mappings_removeRequest( xtreemfs_address_mappings_removeRequest& req );\
      virtual void handlextreemfs_address_mappings_setRequest( xtreemfs_address_mappings_setRequest& req );\
      virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& req );\
      virtual void handlextreemfs_global_time_s_getRequest( xtreemfs_global_time_s_getRequest& req );\
      virtual void handlextreemfs_service_get_by_typeRequest( xtreemfs_service_get_by_typeRequest& req );\
      virtual void handlextreemfs_service_get_by_uuidRequest( xtreemfs_service_get_by_uuidRequest& req );\
      virtual void handlextreemfs_service_get_by_nameRequest( xtreemfs_service_get_by_nameRequest& req );\
      virtual void handlextreemfs_service_registerRequest( xtreemfs_service_registerRequest& req );\
      virtual void handlextreemfs_service_deregisterRequest( xtreemfs_service_deregisterRequest& req );\
      virtual void handlextreemfs_service_offlineRequest( xtreemfs_service_offlineRequest& req );\
      virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req );

    };



  };



};
#endif
