#ifndef _71611007958_H
#define _71611007958_H

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
  
      class AddressMapping : public YIELD::Serializable
      {
      public:
        AddressMapping() : version( 0 ), port( 0 ), ttl_s( 0 ) { }
        AddressMapping( const std::string& uuid, uint64_t version, const std::string& protocol, const std::string& address, uint16_t port, const std::string& match_network, uint32_t ttl_s ) : uuid( uuid ), version( version ), protocol( protocol ), address( address ), port( port ), match_network( match_network ), ttl_s( ttl_s ) { }
        AddressMapping( const char* uuid, size_t uuid_len, uint64_t version, const char* protocol, size_t protocol_len, const char* address, size_t address_len, uint16_t port, const char* match_network, size_t match_network_len, uint32_t ttl_s ) : uuid( uuid, uuid_len ), version( version ), protocol( protocol, protocol_len ), address( address, address_len ), port( port ), match_network( match_network, match_network_len ), ttl_s( ttl_s ) { }
        virtual ~AddressMapping() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
        void set_version( uint64_t version ) { this->version = version; }
        uint64_t get_version() const { return version; }
        void set_protocol( const std::string& protocol ) { set_protocol( protocol.c_str(), protocol.size() ); }
        void set_protocol( const char* protocol, size_t protocol_len = 0 ) { this->protocol.assign( protocol, ( protocol_len != 0 ) ? protocol_len : std::strlen( protocol ) ); }
        const std::string& get_protocol() const { return protocol; }
        void set_address( const std::string& address ) { set_address( address.c_str(), address.size() ); }
        void set_address( const char* address, size_t address_len = 0 ) { this->address.assign( address, ( address_len != 0 ) ? address_len : std::strlen( address ) ); }
        const std::string& get_address() const { return address; }
        void set_port( uint16_t port ) { this->port = port; }
        uint16_t get_port() const { return port; }
        void set_match_network( const std::string& match_network ) { set_match_network( match_network.c_str(), match_network.size() ); }
        void set_match_network( const char* match_network, size_t match_network_len = 0 ) { this->match_network.assign( match_network, ( match_network_len != 0 ) ? match_network_len : std::strlen( match_network ) ); }
        const std::string& get_match_network() const { return match_network; }
        void set_ttl_s( uint32_t ttl_s ) { this->ttl_s = ttl_s; }
        uint32_t get_ttl_s() const { return ttl_s; }
  
        bool operator==( const AddressMapping& other ) const { return uuid == other.uuid && version == other.version && protocol == other.protocol && address == other.address && port == other.port && match_network == other.match_network && ttl_s == other.ttl_s; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::AddressMapping", 3678065204UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "version" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "protocol" ), protocol ); input_stream.readString( YIELD::StructuredStream::Declaration( "address" ), address ); port = input_stream.readUint16( YIELD::StructuredStream::Declaration( "port" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "match_network" ), match_network ); ttl_s = input_stream.readUint32( YIELD::StructuredStream::Declaration( "ttl_s" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "version" ), version ); output_stream.writeString( YIELD::StructuredStream::Declaration( "protocol" ), protocol ); output_stream.writeString( YIELD::StructuredStream::Declaration( "address" ), address ); output_stream.writeUint16( YIELD::StructuredStream::Declaration( "port" ), port ); output_stream.writeString( YIELD::StructuredStream::Declaration( "match_network" ), match_network ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "ttl_s" ), ttl_s ); }
  
      protected:
        std::string uuid;
        uint64_t version;
        std::string protocol;
        std::string address;
        uint16_t port;
        std::string match_network;
        uint32_t ttl_s;
      };
  
      class AddressMappingSet : public std::vector<org::xtreemfs::interfaces::AddressMapping>, public YIELD::Serializable
      {
      public:
        AddressMappingSet() { }
        AddressMappingSet( const org::xtreemfs::interfaces::AddressMapping& first_value ) { std::vector<org::xtreemfs::interfaces::AddressMapping>::push_back( first_value ); }
        AddressMappingSet( size_type size ) : std::vector<org::xtreemfs::interfaces::AddressMapping>( size ) { }
        virtual ~AddressMappingSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::AddressMappingSet", 3884050721UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::AddressMapping item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMapping", "item" ), &item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMapping", "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::AddressMapping>::size(); }
      };
  
      class ServiceDataMap : public std::map<std::string,std::string>, public YIELD::Serializable
      {
      public:
        virtual ~ServiceDataMap() { }
  
        // YIELD::RTTI
        TYPE_INFO( MAP, "org::xtreemfs::interfaces::ServiceDataMap", 3914327351UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { std::string key; input_stream.readString( YIELD::StructuredStream::Declaration( 0, static_cast<uint32_t>( 0 ) ), key ); if ( !key.empty() ) { std::string value; input_stream.readString( YIELD::StructuredStream::Declaration( key.c_str(), static_cast<uint32_t>( 0 ) ), value ); ( *this )[key] = value; } }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { for ( iterator i = begin(); i != end(); i++ ) { output_stream.writeString( YIELD::StructuredStream::Declaration( i->first.c_str(), static_cast<uint32_t>( 0 ) ), i->second ); } }
        size_t getSize() const { return std::map<std::string, std::string>::size(); }
      };
  
      class Service : public YIELD::Serializable
      {
      public:
        Service() : version( 0 ), type( 0 ), last_updated_s( 0 ) { }
        Service( const std::string& uuid, uint64_t version, uint16_t type, const std::string& name, uint64_t last_updated_s, const org::xtreemfs::interfaces::ServiceDataMap& data ) : uuid( uuid ), version( version ), type( type ), name( name ), last_updated_s( last_updated_s ), data( data ) { }
        Service( const char* uuid, size_t uuid_len, uint64_t version, uint16_t type, const char* name, size_t name_len, uint64_t last_updated_s, const org::xtreemfs::interfaces::ServiceDataMap& data ) : uuid( uuid, uuid_len ), version( version ), type( type ), name( name, name_len ), last_updated_s( last_updated_s ), data( data ) { }
        virtual ~Service() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
        void set_version( uint64_t version ) { this->version = version; }
        uint64_t get_version() const { return version; }
        void set_type( uint16_t type ) { this->type = type; }
        uint16_t get_type() const { return type; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len = 0 ) { this->name.assign( name, ( name_len != 0 ) ? name_len : std::strlen( name ) ); }
        const std::string& get_name() const { return name; }
        void set_last_updated_s( uint64_t last_updated_s ) { this->last_updated_s = last_updated_s; }
        uint64_t get_last_updated_s() const { return last_updated_s; }
        void set_data( const org::xtreemfs::interfaces::ServiceDataMap&  data ) { this->data = data; }
        const org::xtreemfs::interfaces::ServiceDataMap& get_data() const { return data; }
  
        bool operator==( const Service& other ) const { return uuid == other.uuid && version == other.version && type == other.type && name == other.name && last_updated_s == other.last_updated_s && data == other.data; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::Service", 2906886611UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "version" ) ); type = input_stream.readUint16( YIELD::StructuredStream::Declaration( "type" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); last_updated_s = input_stream.readUint64( YIELD::StructuredStream::Declaration( "last_updated_s" ) ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceDataMap", "data" ), &data ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "version" ), version ); output_stream.writeUint16( YIELD::StructuredStream::Declaration( "type" ), type ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "last_updated_s" ), last_updated_s ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceDataMap", "data" ), data ); }
  
      protected:
        std::string uuid;
        uint64_t version;
        uint16_t type;
        std::string name;
        uint64_t last_updated_s;
        org::xtreemfs::interfaces::ServiceDataMap data;
      };
  
      class ServiceSet : public std::vector<org::xtreemfs::interfaces::Service>, public YIELD::Serializable
      {
      public:
        ServiceSet() { }
        ServiceSet( const org::xtreemfs::interfaces::Service& first_value ) { std::vector<org::xtreemfs::interfaces::Service>::push_back( first_value ); }
        ServiceSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Service>( size ) { }
        virtual ~ServiceSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::ServiceSet", 3685523999UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::Service item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Service", "item" ), &item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Service", "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::Service>::size(); }
      };
  
  
  
      #ifndef ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS YIELD::EventHandler
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
  
      #ifndef ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_EVENT_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_EVENT_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_EVENT_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_EVENT_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_EVENT_PARENT_CLASS ORG_EXCEPTION_EVENT_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_EXCEPTION_EVENT_PARENT_CLASS YIELD::ExceptionEvent
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
  
      virtual void xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { xtreemfs_address_mappings_get( uuid, address_mappings, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::EventTarget* send_target ) { xtreemfs_address_mappings_get( uuid, address_mappings, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_address_mappings_get( uuid, address_mappings, NULL, response_timeout_ns ); }
        virtual void xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_address_mappings_getSyncRequest* __req = new xtreemfs_address_mappings_getSyncRequest( uuid ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_address_mappings_getResponse& __resp = ( xtreemfs_address_mappings_getResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req ); address_mappings = __resp.get_address_mappings(); YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_address_mappings_remove( const std::string& uuid ) { xtreemfs_address_mappings_remove( uuid, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_address_mappings_remove( const std::string& uuid, YIELD::EventTarget* send_target ) { xtreemfs_address_mappings_remove( uuid, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_address_mappings_remove( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_address_mappings_remove( uuid, NULL, response_timeout_ns ); }
        virtual void xtreemfs_address_mappings_remove( const std::string& uuid, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_address_mappings_removeSyncRequest* __req = new xtreemfs_address_mappings_removeSyncRequest( uuid ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_address_mappings_removeResponse& __resp = ( xtreemfs_address_mappings_removeResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { return xtreemfs_address_mappings_set( address_mappings, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::EventTarget* send_target ) { return xtreemfs_address_mappings_set( address_mappings, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::timeout_ns_t response_timeout_ns ) { return xtreemfs_address_mappings_set( address_mappings, NULL, response_timeout_ns ); }
        virtual uint64_t xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_address_mappings_setSyncRequest* __req = new xtreemfs_address_mappings_setSyncRequest( address_mappings ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_address_mappings_setResponse& __resp = ( xtreemfs_address_mappings_setResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req ); uint64_t _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void xtreemfs_checkpoint() { xtreemfs_checkpoint( NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_checkpoint( YIELD::EventTarget* send_target ) { xtreemfs_checkpoint( send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_checkpoint( YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_checkpoint( NULL, response_timeout_ns ); }
        virtual void xtreemfs_checkpoint( YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_checkpointSyncRequest* __req = new xtreemfs_checkpointSyncRequest(); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_checkpointResponse& __resp = ( xtreemfs_checkpointResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual uint64_t xtreemfs_global_time_s_get() { return xtreemfs_global_time_s_get( NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t xtreemfs_global_time_s_get( YIELD::EventTarget* send_target ) { return xtreemfs_global_time_s_get( send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t xtreemfs_global_time_s_get( YIELD::timeout_ns_t response_timeout_ns ) { return xtreemfs_global_time_s_get( NULL, response_timeout_ns ); }
        virtual uint64_t xtreemfs_global_time_s_get( YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_global_time_s_getSyncRequest* __req = new xtreemfs_global_time_s_getSyncRequest(); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_global_time_s_getResponse& __resp = ( xtreemfs_global_time_s_getResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req ); uint64_t _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void xtreemfs_service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceSet& services ) { xtreemfs_service_get_by_type( type, services, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceSet& services, YIELD::EventTarget* send_target ) { xtreemfs_service_get_by_type( type, services, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceSet& services, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_get_by_type( type, services, NULL, response_timeout_ns ); }
        virtual void xtreemfs_service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceSet& services, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_get_by_typeSyncRequest* __req = new xtreemfs_service_get_by_typeSyncRequest( type ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_service_get_by_typeResponse& __resp = ( xtreemfs_service_get_by_typeResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req ); services = __resp.get_services(); YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services ) { xtreemfs_service_get_by_uuid( uuid, services, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services, YIELD::EventTarget* send_target ) { xtreemfs_service_get_by_uuid( uuid, services, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_get_by_uuid( uuid, services, NULL, response_timeout_ns ); }
        virtual void xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_get_by_uuidSyncRequest* __req = new xtreemfs_service_get_by_uuidSyncRequest( uuid ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_service_get_by_uuidResponse& __resp = ( xtreemfs_service_get_by_uuidResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req ); services = __resp.get_services(); YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services ) { xtreemfs_service_get_by_name( name, services, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services, YIELD::EventTarget* send_target ) { xtreemfs_service_get_by_name( name, services, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_get_by_name( name, services, NULL, response_timeout_ns ); }
        virtual void xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_get_by_nameSyncRequest* __req = new xtreemfs_service_get_by_nameSyncRequest( name ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_service_get_by_nameResponse& __resp = ( xtreemfs_service_get_by_nameResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req ); services = __resp.get_services(); YIELD::SharedObject::decRef( __resp ); }
        virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service ) { return xtreemfs_service_register( service, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service, YIELD::EventTarget* send_target ) { return xtreemfs_service_register( service, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service, YIELD::timeout_ns_t response_timeout_ns ) { return xtreemfs_service_register( service, NULL, response_timeout_ns ); }
        virtual uint64_t xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_registerSyncRequest* __req = new xtreemfs_service_registerSyncRequest( service ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_service_registerResponse& __resp = ( xtreemfs_service_registerResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req ); uint64_t _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void xtreemfs_service_deregister( const std::string& uuid ) { xtreemfs_service_deregister( uuid, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_deregister( const std::string& uuid, YIELD::EventTarget* send_target ) { xtreemfs_service_deregister( uuid, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_deregister( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_deregister( uuid, NULL, response_timeout_ns ); }
        virtual void xtreemfs_service_deregister( const std::string& uuid, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_deregisterSyncRequest* __req = new xtreemfs_service_deregisterSyncRequest( uuid ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_service_deregisterResponse& __resp = ( xtreemfs_service_deregisterResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_service_offline( const std::string& uuid ) { xtreemfs_service_offline( uuid, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_offline( const std::string& uuid, YIELD::EventTarget* send_target ) { xtreemfs_service_offline( uuid, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_service_offline( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_offline( uuid, NULL, response_timeout_ns ); }
        virtual void xtreemfs_service_offline( const std::string& uuid, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_service_offlineSyncRequest* __req = new xtreemfs_service_offlineSyncRequest( uuid ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_service_offlineResponse& __resp = ( xtreemfs_service_offlineResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_shutdown() { xtreemfs_shutdown( NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( YIELD::EventTarget* send_target ) { xtreemfs_shutdown( send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_shutdown( NULL, response_timeout_ns ); }
        virtual void xtreemfs_shutdown( YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_shutdownSyncRequest* __req = new xtreemfs_shutdownSyncRequest(); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_shutdownResponse& __resp = ( xtreemfs_shutdownResponse& )__req->waitForDefaultResponse( response_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }  // Request/response pair Event type definitions for the operations in DIRInterface
  
      class xtreemfs_address_mappings_getResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_address_mappings_getResponse() { }
        xtreemfs_address_mappings_getResponse( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) : address_mappings( address_mappings ) { }
        virtual ~xtreemfs_address_mappings_getResponse() { }
  
        void set_address_mappings( const org::xtreemfs::interfaces::AddressMappingSet&  address_mappings ) { this->address_mappings = address_mappings; }
        const org::xtreemfs::interfaces::AddressMappingSet& get_address_mappings() const { return address_mappings; }
  
        bool operator==( const xtreemfs_address_mappings_getResponse& other ) const { return address_mappings == other.address_mappings; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_getResponse", 3746718570UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMappingSet", "address_mappings" ), &address_mappings ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMappingSet", "address_mappings" ), address_mappings ); }
  
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
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
  
        bool operator==( const xtreemfs_address_mappings_getRequest& other ) const { return uuid == other.uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_getRequest", 873266041UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 1; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3746718570UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_address_mappings_getResponse; }
  
  
      protected:
        std::string uuid;
      };
  
      class xtreemfs_address_mappings_getSyncRequest : public xtreemfs_address_mappings_getRequest
      {
      public:
        xtreemfs_address_mappings_getSyncRequest() : xtreemfs_address_mappings_getRequest( std::string() ) { }
        xtreemfs_address_mappings_getSyncRequest( const std::string& uuid ) : xtreemfs_address_mappings_getRequest( uuid ) { }
        xtreemfs_address_mappings_getSyncRequest( const char* uuid, size_t uuid_len ) : xtreemfs_address_mappings_getRequest( uuid, uuid_len ) { }
        virtual ~xtreemfs_address_mappings_getSyncRequest() { }
  
        bool operator==( const xtreemfs_address_mappings_getSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_getResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_address_mappings_removeResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_address_mappings_removeResponse() { }
        virtual ~xtreemfs_address_mappings_removeResponse() { }
  
        bool operator==( const xtreemfs_address_mappings_removeResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_removeResponse", 279051446UL );
  
      };
  
      class xtreemfs_address_mappings_removeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_address_mappings_removeRequest() { }
        xtreemfs_address_mappings_removeRequest( const std::string& uuid ) : uuid( uuid ) { }
        xtreemfs_address_mappings_removeRequest( const char* uuid, size_t uuid_len ) : uuid( uuid, uuid_len ) { }
        virtual ~xtreemfs_address_mappings_removeRequest() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
  
        bool operator==( const xtreemfs_address_mappings_removeRequest& other ) const { return uuid == other.uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_removeRequest", 1972930277UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 3; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 279051446UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_address_mappings_removeResponse; }
  
  
      protected:
        std::string uuid;
      };
  
      class xtreemfs_address_mappings_removeSyncRequest : public xtreemfs_address_mappings_removeRequest
      {
      public:
        xtreemfs_address_mappings_removeSyncRequest() : xtreemfs_address_mappings_removeRequest( std::string() ) { }
        xtreemfs_address_mappings_removeSyncRequest( const std::string& uuid ) : xtreemfs_address_mappings_removeRequest( uuid ) { }
        xtreemfs_address_mappings_removeSyncRequest( const char* uuid, size_t uuid_len ) : xtreemfs_address_mappings_removeRequest( uuid, uuid_len ) { }
        virtual ~xtreemfs_address_mappings_removeSyncRequest() { }
  
        bool operator==( const xtreemfs_address_mappings_removeSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_removeResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
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
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_setResponse", 261715923UL );
  
        // YIELD::Serializable
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
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_setRequest", 1158057195UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMappingSet", "address_mappings" ), &address_mappings ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMappingSet", "address_mappings" ), address_mappings ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 2; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 261715923UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_address_mappings_setResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::AddressMappingSet address_mappings;
      };
  
      class xtreemfs_address_mappings_setSyncRequest : public xtreemfs_address_mappings_setRequest
      {
      public:
        xtreemfs_address_mappings_setSyncRequest() : xtreemfs_address_mappings_setRequest( org::xtreemfs::interfaces::AddressMappingSet() ) { }
        xtreemfs_address_mappings_setSyncRequest( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) : xtreemfs_address_mappings_setRequest( address_mappings ) { }
        virtual ~xtreemfs_address_mappings_setSyncRequest() { }
  
        bool operator==( const xtreemfs_address_mappings_setSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_address_mappings_setResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_checkpointResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_checkpointResponse() { }
        virtual ~xtreemfs_checkpointResponse() { }
  
        bool operator==( const xtreemfs_checkpointResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_checkpointResponse", 2107062528UL );
  
      };
  
      class xtreemfs_checkpointRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_checkpointRequest() { }
        virtual ~xtreemfs_checkpointRequest() { }
  
        bool operator==( const xtreemfs_checkpointRequest& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_checkpointRequest", 584547126UL );
  
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 50; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 2107062528UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_checkpointResponse; }
  
      };
  
      class xtreemfs_checkpointSyncRequest : public xtreemfs_checkpointRequest
      {
      public:
        xtreemfs_checkpointSyncRequest() { }
        virtual ~xtreemfs_checkpointSyncRequest() { }
  
        bool operator==( const xtreemfs_checkpointSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_checkpointResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
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
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_global_time_s_getResponse", 3126302679UL );
  
        // YIELD::Serializable
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
  
        bool operator==( const xtreemfs_global_time_s_getRequest& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_global_time_s_getRequest", 4097630521UL );
  
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 8; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3126302679UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_global_time_s_getResponse; }
  
      };
  
      class xtreemfs_global_time_s_getSyncRequest : public xtreemfs_global_time_s_getRequest
      {
      public:
        xtreemfs_global_time_s_getSyncRequest() { }
        virtual ~xtreemfs_global_time_s_getSyncRequest() { }
  
        bool operator==( const xtreemfs_global_time_s_getSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_global_time_s_getResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
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
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_typeResponse", 3237041635UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceSet", "services" ), &services ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceSet", "services" ), services ); }
  
      protected:
        org::xtreemfs::interfaces::ServiceSet services;
      };
  
      class xtreemfs_service_get_by_typeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_service_get_by_typeRequest() : type( 0 ) { }
        xtreemfs_service_get_by_typeRequest( uint16_t type ) : type( type ) { }
        virtual ~xtreemfs_service_get_by_typeRequest() { }
  
        void set_type( uint16_t type ) { this->type = type; }
        uint16_t get_type() const { return type; }
  
        bool operator==( const xtreemfs_service_get_by_typeRequest& other ) const { return type == other.type; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_typeRequest", 1132616721UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { type = input_stream.readUint16( YIELD::StructuredStream::Declaration( "type" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint16( YIELD::StructuredStream::Declaration( "type" ), type ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 6; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3237041635UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_service_get_by_typeResponse; }
  
  
      protected:
        uint16_t type;
      };
  
      class xtreemfs_service_get_by_typeSyncRequest : public xtreemfs_service_get_by_typeRequest
      {
      public:
        xtreemfs_service_get_by_typeSyncRequest() : xtreemfs_service_get_by_typeRequest( 0 ) { }
        xtreemfs_service_get_by_typeSyncRequest( uint16_t type ) : xtreemfs_service_get_by_typeRequest( type ) { }
        virtual ~xtreemfs_service_get_by_typeSyncRequest() { }
  
        bool operator==( const xtreemfs_service_get_by_typeSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_typeResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
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
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_uuidResponse", 4062260027UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceSet", "services" ), &services ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceSet", "services" ), services ); }
  
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
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
  
        bool operator==( const xtreemfs_service_get_by_uuidRequest& other ) const { return uuid == other.uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_uuidRequest", 535177358UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 7; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 4062260027UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_service_get_by_uuidResponse; }
  
  
      protected:
        std::string uuid;
      };
  
      class xtreemfs_service_get_by_uuidSyncRequest : public xtreemfs_service_get_by_uuidRequest
      {
      public:
        xtreemfs_service_get_by_uuidSyncRequest() : xtreemfs_service_get_by_uuidRequest( std::string() ) { }
        xtreemfs_service_get_by_uuidSyncRequest( const std::string& uuid ) : xtreemfs_service_get_by_uuidRequest( uuid ) { }
        xtreemfs_service_get_by_uuidSyncRequest( const char* uuid, size_t uuid_len ) : xtreemfs_service_get_by_uuidRequest( uuid, uuid_len ) { }
        virtual ~xtreemfs_service_get_by_uuidSyncRequest() { }
  
        bool operator==( const xtreemfs_service_get_by_uuidSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_uuidResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
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
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_nameResponse", 2629941400UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceSet", "services" ), &services ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceSet", "services" ), services ); }
  
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
        void set_name( const char* name, size_t name_len = 0 ) { this->name.assign( name, ( name_len != 0 ) ? name_len : std::strlen( name ) ); }
        const std::string& get_name() const { return name; }
  
        bool operator==( const xtreemfs_service_get_by_nameRequest& other ) const { return name == other.name; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_nameRequest", 2675528333UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 9; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 2629941400UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_service_get_by_nameResponse; }
  
  
      protected:
        std::string name;
      };
  
      class xtreemfs_service_get_by_nameSyncRequest : public xtreemfs_service_get_by_nameRequest
      {
      public:
        xtreemfs_service_get_by_nameSyncRequest() : xtreemfs_service_get_by_nameRequest( std::string() ) { }
        xtreemfs_service_get_by_nameSyncRequest( const std::string& name ) : xtreemfs_service_get_by_nameRequest( name ) { }
        xtreemfs_service_get_by_nameSyncRequest( const char* name, size_t name_len ) : xtreemfs_service_get_by_nameRequest( name, name_len ) { }
        virtual ~xtreemfs_service_get_by_nameSyncRequest() { }
  
        bool operator==( const xtreemfs_service_get_by_nameSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_nameResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
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
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_registerResponse", 2428095872UL );
  
        // YIELD::Serializable
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
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_registerRequest", 388320218UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Service", "service" ), &service ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Service", "service" ), service ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 4; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 2428095872UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_service_registerResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Service service;
      };
  
      class xtreemfs_service_registerSyncRequest : public xtreemfs_service_registerRequest
      {
      public:
        xtreemfs_service_registerSyncRequest() : xtreemfs_service_registerRequest( org::xtreemfs::interfaces::Service() ) { }
        xtreemfs_service_registerSyncRequest( const org::xtreemfs::interfaces::Service& service ) : xtreemfs_service_registerRequest( service ) { }
        virtual ~xtreemfs_service_registerSyncRequest() { }
  
        bool operator==( const xtreemfs_service_registerSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_registerResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_service_deregisterResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_service_deregisterResponse() { }
        virtual ~xtreemfs_service_deregisterResponse() { }
  
        bool operator==( const xtreemfs_service_deregisterResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_deregisterResponse", 3229214492UL );
  
      };
  
      class xtreemfs_service_deregisterRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_service_deregisterRequest() { }
        xtreemfs_service_deregisterRequest( const std::string& uuid ) : uuid( uuid ) { }
        xtreemfs_service_deregisterRequest( const char* uuid, size_t uuid_len ) : uuid( uuid, uuid_len ) { }
        virtual ~xtreemfs_service_deregisterRequest() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
  
        bool operator==( const xtreemfs_service_deregisterRequest& other ) const { return uuid == other.uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_deregisterRequest", 902638874UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 5; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3229214492UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_service_deregisterResponse; }
  
  
      protected:
        std::string uuid;
      };
  
      class xtreemfs_service_deregisterSyncRequest : public xtreemfs_service_deregisterRequest
      {
      public:
        xtreemfs_service_deregisterSyncRequest() : xtreemfs_service_deregisterRequest( std::string() ) { }
        xtreemfs_service_deregisterSyncRequest( const std::string& uuid ) : xtreemfs_service_deregisterRequest( uuid ) { }
        xtreemfs_service_deregisterSyncRequest( const char* uuid, size_t uuid_len ) : xtreemfs_service_deregisterRequest( uuid, uuid_len ) { }
        virtual ~xtreemfs_service_deregisterSyncRequest() { }
  
        bool operator==( const xtreemfs_service_deregisterSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_deregisterResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_service_offlineResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_service_offlineResponse() { }
        virtual ~xtreemfs_service_offlineResponse() { }
  
        bool operator==( const xtreemfs_service_offlineResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_offlineResponse", 3340695553UL );
  
      };
  
      class xtreemfs_service_offlineRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_service_offlineRequest() { }
        xtreemfs_service_offlineRequest( const std::string& uuid ) : uuid( uuid ) { }
        xtreemfs_service_offlineRequest( const char* uuid, size_t uuid_len ) : uuid( uuid, uuid_len ) { }
        virtual ~xtreemfs_service_offlineRequest() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
  
        bool operator==( const xtreemfs_service_offlineRequest& other ) const { return uuid == other.uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_offlineRequest", 2700206036UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 10; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3340695553UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_service_offlineResponse; }
  
  
      protected:
        std::string uuid;
      };
  
      class xtreemfs_service_offlineSyncRequest : public xtreemfs_service_offlineRequest
      {
      public:
        xtreemfs_service_offlineSyncRequest() : xtreemfs_service_offlineRequest( std::string() ) { }
        xtreemfs_service_offlineSyncRequest( const std::string& uuid ) : xtreemfs_service_offlineRequest( uuid ) { }
        xtreemfs_service_offlineSyncRequest( const char* uuid, size_t uuid_len ) : xtreemfs_service_offlineRequest( uuid, uuid_len ) { }
        virtual ~xtreemfs_service_offlineSyncRequest() { }
  
        bool operator==( const xtreemfs_service_offlineSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_offlineResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_shutdownResponse() { }
        virtual ~xtreemfs_shutdownResponse() { }
  
        bool operator==( const xtreemfs_shutdownResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_shutdownResponse", 2619067177UL );
  
      };
  
      class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_shutdownRequest() { }
        virtual ~xtreemfs_shutdownRequest() { }
  
        bool operator==( const xtreemfs_shutdownRequest& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::xtreemfs_shutdownRequest", 2357328795UL );
  
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 51; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 2619067177UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_shutdownResponse; }
  
      };
  
      class xtreemfs_shutdownSyncRequest : public xtreemfs_shutdownRequest
      {
      public:
        xtreemfs_shutdownSyncRequest() { }
        virtual ~xtreemfs_shutdownSyncRequest() { }
  
        bool operator==( const xtreemfs_shutdownSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::xtreemfs_shutdownResponse>( timeout_ns ); }
  
      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
  
  
        void registerSerializableFactories( YIELD::SerializableFactories& serializable_factories )
        {
          serializable_factories.registerSerializableFactory( 873266041UL, new YIELD::SerializableFactoryImpl<xtreemfs_address_mappings_getRequest> ); serializable_factories.registerSerializableFactory( 296463183UL, new YIELD::SerializableFactoryImpl<xtreemfs_address_mappings_getSyncRequest> ); serializable_factories.registerSerializableFactory( 3746718570UL, new YIELD::SerializableFactoryImpl<xtreemfs_address_mappings_getResponse> );
          serializable_factories.registerSerializableFactory( 1972930277UL, new YIELD::SerializableFactoryImpl<xtreemfs_address_mappings_removeRequest> ); serializable_factories.registerSerializableFactory( 2768342264UL, new YIELD::SerializableFactoryImpl<xtreemfs_address_mappings_removeSyncRequest> ); serializable_factories.registerSerializableFactory( 279051446UL, new YIELD::SerializableFactoryImpl<xtreemfs_address_mappings_removeResponse> );
          serializable_factories.registerSerializableFactory( 1158057195UL, new YIELD::SerializableFactoryImpl<xtreemfs_address_mappings_setRequest> ); serializable_factories.registerSerializableFactory( 284218117UL, new YIELD::SerializableFactoryImpl<xtreemfs_address_mappings_setSyncRequest> ); serializable_factories.registerSerializableFactory( 261715923UL, new YIELD::SerializableFactoryImpl<xtreemfs_address_mappings_setResponse> );
          serializable_factories.registerSerializableFactory( 584547126UL, new YIELD::SerializableFactoryImpl<xtreemfs_checkpointRequest> ); serializable_factories.registerSerializableFactory( 4267539024UL, new YIELD::SerializableFactoryImpl<xtreemfs_checkpointSyncRequest> ); serializable_factories.registerSerializableFactory( 2107062528UL, new YIELD::SerializableFactoryImpl<xtreemfs_checkpointResponse> );
          serializable_factories.registerSerializableFactory( 4097630521UL, new YIELD::SerializableFactoryImpl<xtreemfs_global_time_s_getRequest> ); serializable_factories.registerSerializableFactory( 1198969347UL, new YIELD::SerializableFactoryImpl<xtreemfs_global_time_s_getSyncRequest> ); serializable_factories.registerSerializableFactory( 3126302679UL, new YIELD::SerializableFactoryImpl<xtreemfs_global_time_s_getResponse> );
          serializable_factories.registerSerializableFactory( 1132616721UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_get_by_typeRequest> ); serializable_factories.registerSerializableFactory( 2184923526UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_get_by_typeSyncRequest> ); serializable_factories.registerSerializableFactory( 3237041635UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_get_by_typeResponse> );
          serializable_factories.registerSerializableFactory( 535177358UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_get_by_uuidRequest> ); serializable_factories.registerSerializableFactory( 2311445990UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_get_by_uuidSyncRequest> ); serializable_factories.registerSerializableFactory( 4062260027UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_get_by_uuidResponse> );
          serializable_factories.registerSerializableFactory( 2675528333UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_get_by_nameRequest> ); serializable_factories.registerSerializableFactory( 80253442UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_get_by_nameSyncRequest> ); serializable_factories.registerSerializableFactory( 2629941400UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_get_by_nameResponse> );
          serializable_factories.registerSerializableFactory( 388320218UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_registerRequest> ); serializable_factories.registerSerializableFactory( 172549561UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_registerSyncRequest> ); serializable_factories.registerSerializableFactory( 2428095872UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_registerResponse> );
          serializable_factories.registerSerializableFactory( 902638874UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_deregisterRequest> ); serializable_factories.registerSerializableFactory( 2619018585UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_deregisterSyncRequest> ); serializable_factories.registerSerializableFactory( 3229214492UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_deregisterResponse> );
          serializable_factories.registerSerializableFactory( 2700206036UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_offlineRequest> ); serializable_factories.registerSerializableFactory( 2692693592UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_offlineSyncRequest> ); serializable_factories.registerSerializableFactory( 3340695553UL, new YIELD::SerializableFactoryImpl<xtreemfs_service_offlineResponse> );
          serializable_factories.registerSerializableFactory( 2357328795UL, new YIELD::SerializableFactoryImpl<xtreemfs_shutdownRequest> ); serializable_factories.registerSerializableFactory( 919339664UL, new YIELD::SerializableFactoryImpl<xtreemfs_shutdownSyncRequest> ); serializable_factories.registerSerializableFactory( 2619067177UL, new YIELD::SerializableFactoryImpl<xtreemfs_shutdownResponse> );
        }
  
  
        // EventHandler
        virtual const char* getEventHandlerName() const { return "DIRInterface"; }
  
        virtual void handleEvent( YIELD::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.getTypeId() )
            {
              case 873266041UL: handlextreemfs_address_mappings_getRequest( static_cast<xtreemfs_address_mappings_getRequest&>( ev ) ); return;
              case 1972930277UL: handlextreemfs_address_mappings_removeRequest( static_cast<xtreemfs_address_mappings_removeRequest&>( ev ) ); return;
              case 1158057195UL: handlextreemfs_address_mappings_setRequest( static_cast<xtreemfs_address_mappings_setRequest&>( ev ) ); return;
              case 584547126UL: handlextreemfs_checkpointRequest( static_cast<xtreemfs_checkpointRequest&>( ev ) ); return;
              case 4097630521UL: handlextreemfs_global_time_s_getRequest( static_cast<xtreemfs_global_time_s_getRequest&>( ev ) ); return;
              case 1132616721UL: handlextreemfs_service_get_by_typeRequest( static_cast<xtreemfs_service_get_by_typeRequest&>( ev ) ); return;
              case 535177358UL: handlextreemfs_service_get_by_uuidRequest( static_cast<xtreemfs_service_get_by_uuidRequest&>( ev ) ); return;
              case 2675528333UL: handlextreemfs_service_get_by_nameRequest( static_cast<xtreemfs_service_get_by_nameRequest&>( ev ) ); return;
              case 388320218UL: handlextreemfs_service_registerRequest( static_cast<xtreemfs_service_registerRequest&>( ev ) ); return;
              case 902638874UL: handlextreemfs_service_deregisterRequest( static_cast<xtreemfs_service_deregisterRequest&>( ev ) ); return;
              case 2700206036UL: handlextreemfs_service_offlineRequest( static_cast<xtreemfs_service_offlineRequest&>( ev ) ); return;
              case 2357328795UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
              default: handleUnknownEvent( ev ); return;
            }
          }
          catch( YIELD::ExceptionEvent* exc_ev )
          {
            static_cast<YIELD::Request&>( ev ).respond( *exc_ev );
          }
          catch ( YIELD::ExceptionEvent& exc_ev )
          {
            static_cast<YIELD::Request&>( ev ).respond( *exc_ev.clone() );
          }
          catch ( YIELD::Exception& exc )
          {
              static_cast<YIELD::Request&>( ev ).respond( *( new YIELD::ExceptionEvent( exc.get_error_code(), exc.what() ) ) );
          }
  
          YIELD::SharedObject::decRef( ev );
        }
  
  
      protected:
          virtual void handlextreemfs_address_mappings_getRequest( xtreemfs_address_mappings_getRequest& req ) { xtreemfs_address_mappings_getResponse* resp = NULL; try { resp = new xtreemfs_address_mappings_getResponse; org::xtreemfs::interfaces::AddressMappingSet address_mappings; _xtreemfs_address_mappings_get( req.get_uuid(), address_mappings ); resp->set_address_mappings( address_mappings ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_address_mappings_removeRequest( xtreemfs_address_mappings_removeRequest& req ) { xtreemfs_address_mappings_removeResponse* resp = NULL; try { resp = new xtreemfs_address_mappings_removeResponse; _xtreemfs_address_mappings_remove( req.get_uuid() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_address_mappings_setRequest( xtreemfs_address_mappings_setRequest& req ) { xtreemfs_address_mappings_setResponse* resp = NULL; try { resp = new xtreemfs_address_mappings_setResponse; uint64_t _return_value = _xtreemfs_address_mappings_set( req.get_address_mappings() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& req ) { xtreemfs_checkpointResponse* resp = NULL; try { resp = new xtreemfs_checkpointResponse; _xtreemfs_checkpoint(); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_global_time_s_getRequest( xtreemfs_global_time_s_getRequest& req ) { xtreemfs_global_time_s_getResponse* resp = NULL; try { resp = new xtreemfs_global_time_s_getResponse; uint64_t _return_value = _xtreemfs_global_time_s_get(); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_service_get_by_typeRequest( xtreemfs_service_get_by_typeRequest& req ) { xtreemfs_service_get_by_typeResponse* resp = NULL; try { resp = new xtreemfs_service_get_by_typeResponse; org::xtreemfs::interfaces::ServiceSet services; _xtreemfs_service_get_by_type( req.get_type(), services ); resp->set_services( services ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_service_get_by_uuidRequest( xtreemfs_service_get_by_uuidRequest& req ) { xtreemfs_service_get_by_uuidResponse* resp = NULL; try { resp = new xtreemfs_service_get_by_uuidResponse; org::xtreemfs::interfaces::ServiceSet services; _xtreemfs_service_get_by_uuid( req.get_uuid(), services ); resp->set_services( services ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_service_get_by_nameRequest( xtreemfs_service_get_by_nameRequest& req ) { xtreemfs_service_get_by_nameResponse* resp = NULL; try { resp = new xtreemfs_service_get_by_nameResponse; org::xtreemfs::interfaces::ServiceSet services; _xtreemfs_service_get_by_name( req.get_name(), services ); resp->set_services( services ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_service_registerRequest( xtreemfs_service_registerRequest& req ) { xtreemfs_service_registerResponse* resp = NULL; try { resp = new xtreemfs_service_registerResponse; uint64_t _return_value = _xtreemfs_service_register( req.get_service() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_service_deregisterRequest( xtreemfs_service_deregisterRequest& req ) { xtreemfs_service_deregisterResponse* resp = NULL; try { resp = new xtreemfs_service_deregisterResponse; _xtreemfs_service_deregister( req.get_uuid() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_service_offlineRequest( xtreemfs_service_offlineRequest& req ) { xtreemfs_service_offlineResponse* resp = NULL; try { resp = new xtreemfs_service_offlineResponse; _xtreemfs_service_offline( req.get_uuid() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req ) { xtreemfs_shutdownResponse* resp = NULL; try { resp = new xtreemfs_shutdownResponse; _xtreemfs_shutdown(); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
  
      virtual void _xtreemfs_address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { }
        virtual void _xtreemfs_address_mappings_remove( const std::string& uuid ) { }
        virtual uint64_t _xtreemfs_address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { return 0; }
        virtual void _xtreemfs_checkpoint() { }
        virtual uint64_t _xtreemfs_global_time_s_get() { return 0; }
        virtual void _xtreemfs_service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceSet& services ) { }
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
      virtual void _xtreemfs_service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void _xtreemfs_service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual void _xtreemfs_service_get_by_name( const std::string& name, org::xtreemfs::interfaces::ServiceSet& services );\
      virtual uint64_t _xtreemfs_service_register( const org::xtreemfs::interfaces::Service& service );\
      virtual void _xtreemfs_service_deregister( const std::string& uuid );\
      virtual void _xtreemfs_service_offline( const std::string& uuid );\
      virtual void _xtreemfs_shutdown();
  
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handlextreemfs_address_mappings_getRequestRequest( xtreemfs_address_mappings_getRequest& req );\
      virtual void handlextreemfs_address_mappings_removeRequestRequest( xtreemfs_address_mappings_removeRequest& req );\
      virtual void handlextreemfs_address_mappings_setRequestRequest( xtreemfs_address_mappings_setRequest& req );\
      virtual void handlextreemfs_checkpointRequestRequest( xtreemfs_checkpointRequest& req );\
      virtual void handlextreemfs_global_time_s_getRequestRequest( xtreemfs_global_time_s_getRequest& req );\
      virtual void handlextreemfs_service_get_by_typeRequestRequest( xtreemfs_service_get_by_typeRequest& req );\
      virtual void handlextreemfs_service_get_by_uuidRequestRequest( xtreemfs_service_get_by_uuidRequest& req );\
      virtual void handlextreemfs_service_get_by_nameRequestRequest( xtreemfs_service_get_by_nameRequest& req );\
      virtual void handlextreemfs_service_registerRequestRequest( xtreemfs_service_registerRequest& req );\
      virtual void handlextreemfs_service_deregisterRequestRequest( xtreemfs_service_deregisterRequest& req );\
      virtual void handlextreemfs_service_offlineRequestRequest( xtreemfs_service_offlineRequest& req );\
      virtual void handlextreemfs_shutdownRequestRequest( xtreemfs_shutdownRequest& req );
  
    };
  
  
  
  };
  
  

};

#endif
