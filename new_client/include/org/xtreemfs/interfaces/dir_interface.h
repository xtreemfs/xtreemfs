#ifndef _6879542829_H
#define _6879542829_H

#include "constants.h"

#include "yield/arch.h"

#include <map>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
  
      class AddressMapping : public YIELD::Serializable
      {
      public:
        AddressMapping() : version( 0 ), port( 0 ), ttl( 0 ) { }
        AddressMapping( const std::string& uuid, uint64_t version, const std::string& protocol, const std::string& address, uint16_t port, const std::string& match_network, uint32_t ttl ) : uuid( uuid ), version( version ), protocol( protocol ), address( address ), port( port ), match_network( match_network ), ttl( ttl ) { }
        AddressMapping( const char* uuid, size_t uuid_len, uint64_t version, const char* protocol, size_t protocol_len, const char* address, size_t address_len, uint16_t port, const char* match_network, size_t match_network_len, uint32_t ttl ) : uuid( uuid, uuid_len ), version( version ), protocol( protocol, protocol_len ), address( address, address_len ), port( port ), match_network( match_network, match_network_len ), ttl( ttl ) { }
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
        void set_ttl( uint32_t ttl ) { this->ttl = ttl; }
        uint32_t get_ttl() const { return ttl; }
  
        bool operator==( const AddressMapping& other ) const { return uuid == other.uuid && version == other.version && protocol == other.protocol && address == other.address && port == other.port && match_network == other.match_network && ttl == other.ttl; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::AddressMapping", 3678065204UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "version" ), version ); output_stream.writeString( YIELD::StructuredStream::Declaration( "protocol" ), protocol ); output_stream.writeString( YIELD::StructuredStream::Declaration( "address" ), address ); output_stream.writeUint16( YIELD::StructuredStream::Declaration( "port" ), port ); output_stream.writeString( YIELD::StructuredStream::Declaration( "match_network" ), match_network ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "ttl" ), ttl ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "version" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "protocol" ), protocol ); input_stream.readString( YIELD::StructuredStream::Declaration( "address" ), address ); port = input_stream.readUint16( YIELD::StructuredStream::Declaration( "port" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "match_network" ), match_network ); ttl = input_stream.readUint32( YIELD::StructuredStream::Declaration( "ttl" ) ); }
  
      protected:
        std::string uuid;
        uint64_t version;
        std::string protocol;
        std::string address;
        uint16_t port;
        std::string match_network;
        uint32_t ttl;
      };
  
      class AddressMappingSet : public std::vector<org::xtreemfs::interfaces::AddressMapping>, public YIELD::Serializable
      {
      public:
        AddressMappingSet() { }
        AddressMappingSet( size_type size ) : std::vector<org::xtreemfs::interfaces::AddressMapping>( size ) { }
        virtual ~AddressMappingSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::AddressMappingSet", 3884050721UL );
  
        // YIELD::Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMapping", "item" ), ( *this )[size() - 1] ); } }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::AddressMapping item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMapping", "item" ), &item ); push_back( item ); }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::AddressMapping>::size(); }
      };
  
      class KeyValuePair : public YIELD::Serializable
      {
      public:
        KeyValuePair() { }
        KeyValuePair( const std::string& key, const std::string& value ) : key( key ), value( value ) { }
        KeyValuePair( const char* key, size_t key_len, const char* value, size_t value_len ) : key( key, key_len ), value( value, value_len ) { }
        virtual ~KeyValuePair() { }
  
        void set_key( const std::string& key ) { set_key( key.c_str(), key.size() ); }
        void set_key( const char* key, size_t key_len = 0 ) { this->key.assign( key, ( key_len != 0 ) ? key_len : std::strlen( key ) ); }
        const std::string& get_key() const { return key; }
        void set_value( const std::string& value ) { set_value( value.c_str(), value.size() ); }
        void set_value( const char* value, size_t value_len = 0 ) { this->value.assign( value, ( value_len != 0 ) ? value_len : std::strlen( value ) ); }
        const std::string& get_value() const { return value; }
  
        bool operator==( const KeyValuePair& other ) const { return key == other.key && value == other.value; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::KeyValuePair", 1206317684UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "key" ), key ); output_stream.writeString( YIELD::StructuredStream::Declaration( "value" ), value ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "key" ), key ); input_stream.readString( YIELD::StructuredStream::Declaration( "value" ), value ); }
  
      protected:
        std::string key;
        std::string value;
      };
  
      class KeyValuePairSet : public std::vector<org::xtreemfs::interfaces::KeyValuePair>, public YIELD::Serializable
      {
      public:
        KeyValuePairSet() { }
        KeyValuePairSet( size_type size ) : std::vector<org::xtreemfs::interfaces::KeyValuePair>( size ) { }
        virtual ~KeyValuePairSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::KeyValuePairSet", 232319006UL );
  
        // YIELD::Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::KeyValuePair", "item" ), ( *this )[size() - 1] ); } }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::KeyValuePair item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::KeyValuePair", "item" ), &item ); push_back( item ); }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::KeyValuePair>::size(); }
      };
  
      class ServiceRegistry : public YIELD::Serializable
      {
      public:
        ServiceRegistry() : version( 0 ), service_type( 0 ), last_updated( 0 ) { }
        ServiceRegistry( const std::string& uuid, uint64_t version, uint16_t service_type, const std::string& service_name, uint64_t last_updated, const org::xtreemfs::interfaces::KeyValuePairSet& data ) : uuid( uuid ), version( version ), service_type( service_type ), service_name( service_name ), last_updated( last_updated ), data( data ) { }
        ServiceRegistry( const char* uuid, size_t uuid_len, uint64_t version, uint16_t service_type, const char* service_name, size_t service_name_len, uint64_t last_updated, const org::xtreemfs::interfaces::KeyValuePairSet& data ) : uuid( uuid, uuid_len ), version( version ), service_type( service_type ), service_name( service_name, service_name_len ), last_updated( last_updated ), data( data ) { }
        virtual ~ServiceRegistry() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
        void set_version( uint64_t version ) { this->version = version; }
        uint64_t get_version() const { return version; }
        void set_service_type( uint16_t service_type ) { this->service_type = service_type; }
        uint16_t get_service_type() const { return service_type; }
        void set_service_name( const std::string& service_name ) { set_service_name( service_name.c_str(), service_name.size() ); }
        void set_service_name( const char* service_name, size_t service_name_len = 0 ) { this->service_name.assign( service_name, ( service_name_len != 0 ) ? service_name_len : std::strlen( service_name ) ); }
        const std::string& get_service_name() const { return service_name; }
        void set_last_updated( uint64_t last_updated ) { this->last_updated = last_updated; }
        uint64_t get_last_updated() const { return last_updated; }
        void set_data( const org::xtreemfs::interfaces::KeyValuePairSet&  data ) { this->data = data; }
        const org::xtreemfs::interfaces::KeyValuePairSet& get_data() const { return data; }
  
        bool operator==( const ServiceRegistry& other ) const { return uuid == other.uuid && version == other.version && service_type == other.service_type && service_name == other.service_name && last_updated == other.last_updated && data == other.data; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::ServiceRegistry", 3874052735UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "version" ), version ); output_stream.writeUint16( YIELD::StructuredStream::Declaration( "service_type" ), service_type ); output_stream.writeString( YIELD::StructuredStream::Declaration( "service_name" ), service_name ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "last_updated" ), last_updated ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::KeyValuePairSet", "data" ), data ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "version" ) ); service_type = input_stream.readUint16( YIELD::StructuredStream::Declaration( "service_type" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "service_name" ), service_name ); last_updated = input_stream.readUint64( YIELD::StructuredStream::Declaration( "last_updated" ) ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::KeyValuePairSet", "data" ), &data ); }
  
      protected:
        std::string uuid;
        uint64_t version;
        uint16_t service_type;
        std::string service_name;
        uint64_t last_updated;
        org::xtreemfs::interfaces::KeyValuePairSet data;
      };
  
      class ServiceRegistrySet : public std::vector<org::xtreemfs::interfaces::ServiceRegistry>, public YIELD::Serializable
      {
      public:
        ServiceRegistrySet() { }
        ServiceRegistrySet( size_type size ) : std::vector<org::xtreemfs::interfaces::ServiceRegistry>( size ) { }
        virtual ~ServiceRegistrySet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::ServiceRegistrySet", 614373350UL );
  
        // YIELD::Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceRegistry", "item" ), ( *this )[size() - 1] ); } }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::ServiceRegistry item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceRegistry", "item" ), &item ); push_back( item ); }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::ServiceRegistry>::size(); }
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
  
      virtual void address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { address_mappings_get( uuid, address_mappings, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings, const char* send_target ) { address_mappings_get( uuid, address_mappings, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::timeout_ns_t send_timeout_ns ) { address_mappings_get( uuid, address_mappings, NULL, send_timeout_ns ); }
        virtual void address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { address_mappings_getSyncRequest* __req = new address_mappings_getSyncRequest( uuid, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); address_mappings_getResponse& __resp = ( address_mappings_getResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); address_mappings = __resp.get_address_mappings(); YIELD::SharedObject::decRef( __resp ); }
        virtual uint64_t address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { return address_mappings_set( address_mappings, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, const char* send_target ) { return address_mappings_set( address_mappings, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::timeout_ns_t send_timeout_ns ) { return address_mappings_set( address_mappings, NULL, send_timeout_ns ); }
        virtual uint64_t address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { address_mappings_setSyncRequest* __req = new address_mappings_setSyncRequest( address_mappings, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); address_mappings_setResponse& __resp = ( address_mappings_setResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); uint64_t _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void address_mappings_delete( const std::string& uuid ) { address_mappings_delete( uuid, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void address_mappings_delete( const std::string& uuid, const char* send_target ) { address_mappings_delete( uuid, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void address_mappings_delete( const std::string& uuid, YIELD::timeout_ns_t send_timeout_ns ) { address_mappings_delete( uuid, NULL, send_timeout_ns ); }
        virtual void address_mappings_delete( const std::string& uuid, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { address_mappings_deleteSyncRequest* __req = new address_mappings_deleteSyncRequest( uuid, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); address_mappings_deleteResponse& __resp = ( address_mappings_deleteResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual uint64_t service_register( const org::xtreemfs::interfaces::ServiceRegistry& service ) { return service_register( service, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t service_register( const org::xtreemfs::interfaces::ServiceRegistry& service, const char* send_target ) { return service_register( service, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t service_register( const org::xtreemfs::interfaces::ServiceRegistry& service, YIELD::timeout_ns_t send_timeout_ns ) { return service_register( service, NULL, send_timeout_ns ); }
        virtual uint64_t service_register( const org::xtreemfs::interfaces::ServiceRegistry& service, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { service_registerSyncRequest* __req = new service_registerSyncRequest( service, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); service_registerResponse& __resp = ( service_registerResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); uint64_t _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void service_deregister( const std::string& uuid ) { service_deregister( uuid, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void service_deregister( const std::string& uuid, const char* send_target ) { service_deregister( uuid, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void service_deregister( const std::string& uuid, YIELD::timeout_ns_t send_timeout_ns ) { service_deregister( uuid, NULL, send_timeout_ns ); }
        virtual void service_deregister( const std::string& uuid, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { service_deregisterSyncRequest* __req = new service_deregisterSyncRequest( uuid, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); service_deregisterResponse& __resp = ( service_deregisterResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceRegistrySet& services ) { service_get_by_type( type, services, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceRegistrySet& services, const char* send_target ) { service_get_by_type( type, services, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceRegistrySet& services, YIELD::timeout_ns_t send_timeout_ns ) { service_get_by_type( type, services, NULL, send_timeout_ns ); }
        virtual void service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceRegistrySet& services, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { service_get_by_typeSyncRequest* __req = new service_get_by_typeSyncRequest( type, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); service_get_by_typeResponse& __resp = ( service_get_by_typeResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); services = __resp.get_services(); YIELD::SharedObject::decRef( __resp ); }
        virtual void service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceRegistrySet& services ) { service_get_by_uuid( uuid, services, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceRegistrySet& services, const char* send_target ) { service_get_by_uuid( uuid, services, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceRegistrySet& services, YIELD::timeout_ns_t send_timeout_ns ) { service_get_by_uuid( uuid, services, NULL, send_timeout_ns ); }
        virtual void service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceRegistrySet& services, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { service_get_by_uuidSyncRequest* __req = new service_get_by_uuidSyncRequest( uuid, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); service_get_by_uuidResponse& __resp = ( service_get_by_uuidResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); services = __resp.get_services(); YIELD::SharedObject::decRef( __resp ); }
        virtual uint64_t global_time_get() { return global_time_get( NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t global_time_get( const char* send_target ) { return global_time_get( send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t global_time_get( YIELD::timeout_ns_t send_timeout_ns ) { return global_time_get( NULL, send_timeout_ns ); }
        virtual uint64_t global_time_get( const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { global_time_getSyncRequest* __req = new global_time_getSyncRequest( send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); global_time_getResponse& __resp = ( global_time_getResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); uint64_t _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void admin_checkpoint( const std::string& password ) { admin_checkpoint( password, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_checkpoint( const std::string& password, const char* send_target ) { admin_checkpoint( password, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_checkpoint( const std::string& password, YIELD::timeout_ns_t send_timeout_ns ) { admin_checkpoint( password, NULL, send_timeout_ns ); }
        virtual void admin_checkpoint( const std::string& password, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { admin_checkpointSyncRequest* __req = new admin_checkpointSyncRequest( password, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); admin_checkpointResponse& __resp = ( admin_checkpointResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void admin_shutdown( const std::string& password ) { admin_shutdown( password, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_shutdown( const std::string& password, const char* send_target ) { admin_shutdown( password, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_shutdown( const std::string& password, YIELD::timeout_ns_t send_timeout_ns ) { admin_shutdown( password, NULL, send_timeout_ns ); }
        virtual void admin_shutdown( const std::string& password, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { admin_shutdownSyncRequest* __req = new admin_shutdownSyncRequest( password, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); admin_shutdownResponse& __resp = ( admin_shutdownResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }  // Request/response pair Event type definitions for the operations in DIRInterface
  
      class address_mappings_getResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        address_mappings_getResponse() { }
        address_mappings_getResponse( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) : address_mappings( address_mappings ) { }
        virtual ~address_mappings_getResponse() { }
  
        void set_address_mappings( const org::xtreemfs::interfaces::AddressMappingSet&  address_mappings ) { this->address_mappings = address_mappings; }
        const org::xtreemfs::interfaces::AddressMappingSet& get_address_mappings() const { return address_mappings; }
  
        bool operator==( const address_mappings_getResponse& other ) const { return address_mappings == other.address_mappings; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::address_mappings_getResponse", 3467605668UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMappingSet", "address_mappings" ), address_mappings ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMappingSet", "address_mappings" ), &address_mappings ); }
  
      protected:
        org::xtreemfs::interfaces::AddressMappingSet address_mappings;
      };
  
      class address_mappings_getRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        address_mappings_getRequest() : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        address_mappings_getRequest( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), uuid( uuid ) { }
        address_mappings_getRequest( const char* uuid, size_t uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), uuid( uuid, uuid_len ) { }
        virtual ~address_mappings_getRequest() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
  
        bool operator==( const address_mappings_getRequest& other ) const { return uuid == other.uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::address_mappings_getRequest", 1983642220UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 1; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3467605668UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::address_mappings_getResponse; }
  
  
      protected:
        std::string uuid;
      };
  
      class address_mappings_getSyncRequest : public address_mappings_getRequest
      {
      public:
        address_mappings_getSyncRequest() : address_mappings_getRequest( std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        address_mappings_getSyncRequest( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : address_mappings_getRequest( uuid, response_timeout_ns ) { }
        address_mappings_getSyncRequest( const char* uuid, size_t uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : address_mappings_getRequest( uuid, uuid_len, response_timeout_ns ) { }
        virtual ~address_mappings_getSyncRequest() { }
  
        bool operator==( const address_mappings_getSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::address_mappings_getResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class address_mappings_setResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        address_mappings_setResponse() : _return_value( 0 ) { }
        address_mappings_setResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
        virtual ~address_mappings_setResponse() { }
  
        void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
        uint64_t get__return_value() const { return _return_value; }
  
        bool operator==( const address_mappings_setResponse& other ) const { return _return_value == other._return_value; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::address_mappings_setResponse", 1879726015UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { _return_value = input_stream.readUint64( YIELD::StructuredStream::Declaration( "_return_value" ) ); }
  
      protected:
        uint64_t _return_value;
      };
  
      class address_mappings_setRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        address_mappings_setRequest() : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        address_mappings_setRequest( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), address_mappings( address_mappings ) { }
        virtual ~address_mappings_setRequest() { }
  
        void set_address_mappings( const org::xtreemfs::interfaces::AddressMappingSet&  address_mappings ) { this->address_mappings = address_mappings; }
        const org::xtreemfs::interfaces::AddressMappingSet& get_address_mappings() const { return address_mappings; }
  
        bool operator==( const address_mappings_setRequest& other ) const { return address_mappings == other.address_mappings; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::address_mappings_setRequest", 1955507110UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMappingSet", "address_mappings" ), address_mappings ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::AddressMappingSet", "address_mappings" ), &address_mappings ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 2; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1879726015UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::address_mappings_setResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::AddressMappingSet address_mappings;
      };
  
      class address_mappings_setSyncRequest : public address_mappings_setRequest
      {
      public:
        address_mappings_setSyncRequest() : address_mappings_setRequest( org::xtreemfs::interfaces::AddressMappingSet(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        address_mappings_setSyncRequest( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : address_mappings_setRequest( address_mappings, response_timeout_ns ) { }
        virtual ~address_mappings_setSyncRequest() { }
  
        bool operator==( const address_mappings_setSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::address_mappings_setResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class address_mappings_deleteResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        address_mappings_deleteResponse() { }
        virtual ~address_mappings_deleteResponse() { }
  
        bool operator==( const address_mappings_deleteResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::address_mappings_deleteResponse", 3734389618UL );
  
      };
  
      class address_mappings_deleteRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        address_mappings_deleteRequest() : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        address_mappings_deleteRequest( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), uuid( uuid ) { }
        address_mappings_deleteRequest( const char* uuid, size_t uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), uuid( uuid, uuid_len ) { }
        virtual ~address_mappings_deleteRequest() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
  
        bool operator==( const address_mappings_deleteRequest& other ) const { return uuid == other.uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::address_mappings_deleteRequest", 2769570357UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 3; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3734389618UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::address_mappings_deleteResponse; }
  
  
      protected:
        std::string uuid;
      };
  
      class address_mappings_deleteSyncRequest : public address_mappings_deleteRequest
      {
      public:
        address_mappings_deleteSyncRequest() : address_mappings_deleteRequest( std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        address_mappings_deleteSyncRequest( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : address_mappings_deleteRequest( uuid, response_timeout_ns ) { }
        address_mappings_deleteSyncRequest( const char* uuid, size_t uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : address_mappings_deleteRequest( uuid, uuid_len, response_timeout_ns ) { }
        virtual ~address_mappings_deleteSyncRequest() { }
  
        bool operator==( const address_mappings_deleteSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::address_mappings_deleteResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class service_registerResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        service_registerResponse() : _return_value( 0 ) { }
        service_registerResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
        virtual ~service_registerResponse() { }
  
        void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
        uint64_t get__return_value() const { return _return_value; }
  
        bool operator==( const service_registerResponse& other ) const { return _return_value == other._return_value; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::service_registerResponse", 3458002187UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { _return_value = input_stream.readUint64( YIELD::StructuredStream::Declaration( "_return_value" ) ); }
  
      protected:
        uint64_t _return_value;
      };
  
      class service_registerRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        service_registerRequest() : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        service_registerRequest( const org::xtreemfs::interfaces::ServiceRegistry& service, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), service( service ) { }
        virtual ~service_registerRequest() { }
  
        void set_service( const org::xtreemfs::interfaces::ServiceRegistry&  service ) { this->service = service; }
        const org::xtreemfs::interfaces::ServiceRegistry& get_service() const { return service; }
  
        bool operator==( const service_registerRequest& other ) const { return service == other.service; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::service_registerRequest", 2211822638UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceRegistry", "service" ), service ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceRegistry", "service" ), &service ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 4; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3458002187UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::service_registerResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::ServiceRegistry service;
      };
  
      class service_registerSyncRequest : public service_registerRequest
      {
      public:
        service_registerSyncRequest() : service_registerRequest( org::xtreemfs::interfaces::ServiceRegistry(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        service_registerSyncRequest( const org::xtreemfs::interfaces::ServiceRegistry& service, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : service_registerRequest( service, response_timeout_ns ) { }
        virtual ~service_registerSyncRequest() { }
  
        bool operator==( const service_registerSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::service_registerResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class service_deregisterResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        service_deregisterResponse() { }
        virtual ~service_deregisterResponse() { }
  
        bool operator==( const service_deregisterResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::service_deregisterResponse", 3236629002UL );
  
      };
  
      class service_deregisterRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        service_deregisterRequest() : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        service_deregisterRequest( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), uuid( uuid ) { }
        service_deregisterRequest( const char* uuid, size_t uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), uuid( uuid, uuid_len ) { }
        virtual ~service_deregisterRequest() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
  
        bool operator==( const service_deregisterRequest& other ) const { return uuid == other.uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::service_deregisterRequest", 3067256812UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 5; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3236629002UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::service_deregisterResponse; }
  
  
      protected:
        std::string uuid;
      };
  
      class service_deregisterSyncRequest : public service_deregisterRequest
      {
      public:
        service_deregisterSyncRequest() : service_deregisterRequest( std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        service_deregisterSyncRequest( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : service_deregisterRequest( uuid, response_timeout_ns ) { }
        service_deregisterSyncRequest( const char* uuid, size_t uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : service_deregisterRequest( uuid, uuid_len, response_timeout_ns ) { }
        virtual ~service_deregisterSyncRequest() { }
  
        bool operator==( const service_deregisterSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::service_deregisterResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class service_get_by_typeResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        service_get_by_typeResponse() { }
        service_get_by_typeResponse( const org::xtreemfs::interfaces::ServiceRegistrySet& services ) : services( services ) { }
        virtual ~service_get_by_typeResponse() { }
  
        void set_services( const org::xtreemfs::interfaces::ServiceRegistrySet&  services ) { this->services = services; }
        const org::xtreemfs::interfaces::ServiceRegistrySet& get_services() const { return services; }
  
        bool operator==( const service_get_by_typeResponse& other ) const { return services == other.services; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::service_get_by_typeResponse", 662022626UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceRegistrySet", "services" ), services ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceRegistrySet", "services" ), &services ); }
  
      protected:
        org::xtreemfs::interfaces::ServiceRegistrySet services;
      };
  
      class service_get_by_typeRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        service_get_by_typeRequest() : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), type( 0 ) { }
        service_get_by_typeRequest( uint16_t type, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), type( type ) { }
        virtual ~service_get_by_typeRequest() { }
  
        void set_type( uint16_t type ) { this->type = type; }
        uint16_t get_type() const { return type; }
  
        bool operator==( const service_get_by_typeRequest& other ) const { return type == other.type; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::service_get_by_typeRequest", 4251087078UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint16( YIELD::StructuredStream::Declaration( "type" ), type ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { type = input_stream.readUint16( YIELD::StructuredStream::Declaration( "type" ) ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 6; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 662022626UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::service_get_by_typeResponse; }
  
  
      protected:
        uint16_t type;
      };
  
      class service_get_by_typeSyncRequest : public service_get_by_typeRequest
      {
      public:
        service_get_by_typeSyncRequest() : service_get_by_typeRequest( 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        service_get_by_typeSyncRequest( uint16_t type, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : service_get_by_typeRequest( type, response_timeout_ns ) { }
        virtual ~service_get_by_typeSyncRequest() { }
  
        bool operator==( const service_get_by_typeSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::service_get_by_typeResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class service_get_by_uuidResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        service_get_by_uuidResponse() { }
        service_get_by_uuidResponse( const org::xtreemfs::interfaces::ServiceRegistrySet& services ) : services( services ) { }
        virtual ~service_get_by_uuidResponse() { }
  
        void set_services( const org::xtreemfs::interfaces::ServiceRegistrySet&  services ) { this->services = services; }
        const org::xtreemfs::interfaces::ServiceRegistrySet& get_services() const { return services; }
  
        bool operator==( const service_get_by_uuidResponse& other ) const { return services == other.services; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::service_get_by_uuidResponse", 2586276636UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceRegistrySet", "services" ), services ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ServiceRegistrySet", "services" ), &services ); }
  
      protected:
        org::xtreemfs::interfaces::ServiceRegistrySet services;
      };
  
      class service_get_by_uuidRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        service_get_by_uuidRequest() : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        service_get_by_uuidRequest( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), uuid( uuid ) { }
        service_get_by_uuidRequest( const char* uuid, size_t uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), uuid( uuid, uuid_len ) { }
        virtual ~service_get_by_uuidRequest() { }
  
        void set_uuid( const std::string& uuid ) { set_uuid( uuid.c_str(), uuid.size() ); }
        void set_uuid( const char* uuid, size_t uuid_len = 0 ) { this->uuid.assign( uuid, ( uuid_len != 0 ) ? uuid_len : std::strlen( uuid ) ); }
        const std::string& get_uuid() const { return uuid; }
  
        bool operator==( const service_get_by_uuidRequest& other ) const { return uuid == other.uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::service_get_by_uuidRequest", 2653848206UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "uuid" ), uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 7; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 2586276636UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::service_get_by_uuidResponse; }
  
  
      protected:
        std::string uuid;
      };
  
      class service_get_by_uuidSyncRequest : public service_get_by_uuidRequest
      {
      public:
        service_get_by_uuidSyncRequest() : service_get_by_uuidRequest( std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        service_get_by_uuidSyncRequest( const std::string& uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : service_get_by_uuidRequest( uuid, response_timeout_ns ) { }
        service_get_by_uuidSyncRequest( const char* uuid, size_t uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : service_get_by_uuidRequest( uuid, uuid_len, response_timeout_ns ) { }
        virtual ~service_get_by_uuidSyncRequest() { }
  
        bool operator==( const service_get_by_uuidSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::service_get_by_uuidResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class global_time_getResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        global_time_getResponse() : _return_value( 0 ) { }
        global_time_getResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
        virtual ~global_time_getResponse() { }
  
        void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
        uint64_t get__return_value() const { return _return_value; }
  
        bool operator==( const global_time_getResponse& other ) const { return _return_value == other._return_value; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::global_time_getResponse", 3513836833UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { _return_value = input_stream.readUint64( YIELD::StructuredStream::Declaration( "_return_value" ) ); }
  
      protected:
        uint64_t _return_value;
      };
  
      class global_time_getRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        global_time_getRequest( YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ) { }
        virtual ~global_time_getRequest() { }
  
        bool operator==( const global_time_getRequest& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::global_time_getRequest", 1678219641UL );
  
        void deserialize( YIELD::StructuredInputStream& input_stream ) { ; }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 8; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3513836833UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::global_time_getResponse; }
  
  
      protected:
  
      };
  
      class global_time_getSyncRequest : public global_time_getRequest
      {
      public:
        global_time_getSyncRequest( YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : global_time_getRequest( response_timeout_ns ) { }
        virtual ~global_time_getSyncRequest() { }
  
        bool operator==( const global_time_getSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::global_time_getResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class admin_checkpointResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        admin_checkpointResponse() { }
        virtual ~admin_checkpointResponse() { }
  
        bool operator==( const admin_checkpointResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::admin_checkpointResponse", 1488245919UL );
  
      };
  
      class admin_checkpointRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        admin_checkpointRequest() : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_checkpointRequest( const std::string& password, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password ) { }
        admin_checkpointRequest( const char* password, size_t password_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password, password_len ) { }
        virtual ~admin_checkpointRequest() { }
  
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }
  
        bool operator==( const admin_checkpointRequest& other ) const { return password == other.password; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::admin_checkpointRequest", 839471856UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 50; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1488245919UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::admin_checkpointResponse; }
  
  
      protected:
        std::string password;
      };
  
      class admin_checkpointSyncRequest : public admin_checkpointRequest
      {
      public:
        admin_checkpointSyncRequest() : admin_checkpointRequest( std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_checkpointSyncRequest( const std::string& password, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_checkpointRequest( password, response_timeout_ns ) { }
        admin_checkpointSyncRequest( const char* password, size_t password_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_checkpointRequest( password, password_len, response_timeout_ns ) { }
        virtual ~admin_checkpointSyncRequest() { }
  
        bool operator==( const admin_checkpointSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::admin_checkpointResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class admin_shutdownResponse : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        admin_shutdownResponse() { }
        virtual ~admin_shutdownResponse() { }
  
        bool operator==( const admin_shutdownResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::DIRInterface::admin_shutdownResponse", 1280024429UL );
  
      };
  
      class admin_shutdownRequest : public ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        admin_shutdownRequest() : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_shutdownRequest( const std::string& password, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password ) { }
        admin_shutdownRequest( const char* password, size_t password_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_DIRINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password, password_len ) { }
        virtual ~admin_shutdownRequest() { }
  
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }
  
        bool operator==( const admin_shutdownRequest& other ) const { return password == other.password; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::DIRInterface::admin_shutdownRequest", 1760149581UL );
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 1; }
        virtual uint32_t getOperationNumber() const { return 51; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1280024429UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::DIRInterface::admin_shutdownResponse; }
  
  
      protected:
        std::string password;
      };
  
      class admin_shutdownSyncRequest : public admin_shutdownRequest
      {
      public:
        admin_shutdownSyncRequest() : admin_shutdownRequest( std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_shutdownSyncRequest( const std::string& password, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_shutdownRequest( password, response_timeout_ns ) { }
        admin_shutdownSyncRequest( const char* password, size_t password_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_shutdownRequest( password, password_len, response_timeout_ns ) { }
        virtual ~admin_shutdownSyncRequest() { }
  
        bool operator==( const admin_shutdownSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::DIRInterface::admin_shutdownResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
  
  
        void registerSerializableFactories( YIELD::SerializableFactories& serializable_factories )
        {
          serializable_factories.registerSerializableFactory( 3467605668UL, new YIELD::SerializableFactoryImpl<address_mappings_getResponse> ); serializable_factories.registerSerializableFactory( 1983642220UL, new YIELD::SerializableFactoryImpl<address_mappings_getRequest> ); serializable_factories.registerSerializableFactory( 2214763735UL, new YIELD::SerializableFactoryImpl<address_mappings_getSyncRequest> );
          serializable_factories.registerSerializableFactory( 1879726015UL, new YIELD::SerializableFactoryImpl<address_mappings_setResponse> ); serializable_factories.registerSerializableFactory( 1955507110UL, new YIELD::SerializableFactoryImpl<address_mappings_setRequest> ); serializable_factories.registerSerializableFactory( 3037499637UL, new YIELD::SerializableFactoryImpl<address_mappings_setSyncRequest> );
          serializable_factories.registerSerializableFactory( 3734389618UL, new YIELD::SerializableFactoryImpl<address_mappings_deleteResponse> ); serializable_factories.registerSerializableFactory( 2769570357UL, new YIELD::SerializableFactoryImpl<address_mappings_deleteRequest> ); serializable_factories.registerSerializableFactory( 1134069179UL, new YIELD::SerializableFactoryImpl<address_mappings_deleteSyncRequest> );
          serializable_factories.registerSerializableFactory( 3458002187UL, new YIELD::SerializableFactoryImpl<service_registerResponse> ); serializable_factories.registerSerializableFactory( 2211822638UL, new YIELD::SerializableFactoryImpl<service_registerRequest> ); serializable_factories.registerSerializableFactory( 443212180UL, new YIELD::SerializableFactoryImpl<service_registerSyncRequest> );
          serializable_factories.registerSerializableFactory( 3236629002UL, new YIELD::SerializableFactoryImpl<service_deregisterResponse> ); serializable_factories.registerSerializableFactory( 3067256812UL, new YIELD::SerializableFactoryImpl<service_deregisterRequest> ); serializable_factories.registerSerializableFactory( 1514635645UL, new YIELD::SerializableFactoryImpl<service_deregisterSyncRequest> );
          serializable_factories.registerSerializableFactory( 662022626UL, new YIELD::SerializableFactoryImpl<service_get_by_typeResponse> ); serializable_factories.registerSerializableFactory( 4251087078UL, new YIELD::SerializableFactoryImpl<service_get_by_typeRequest> ); serializable_factories.registerSerializableFactory( 2382751063UL, new YIELD::SerializableFactoryImpl<service_get_by_typeSyncRequest> );
          serializable_factories.registerSerializableFactory( 2586276636UL, new YIELD::SerializableFactoryImpl<service_get_by_uuidResponse> ); serializable_factories.registerSerializableFactory( 2653848206UL, new YIELD::SerializableFactoryImpl<service_get_by_uuidRequest> ); serializable_factories.registerSerializableFactory( 1049065552UL, new YIELD::SerializableFactoryImpl<service_get_by_uuidSyncRequest> );
          serializable_factories.registerSerializableFactory( 3513836833UL, new YIELD::SerializableFactoryImpl<global_time_getResponse> ); serializable_factories.registerSerializableFactory( 1678219641UL, new YIELD::SerializableFactoryImpl<global_time_getRequest> ); serializable_factories.registerSerializableFactory( 3150531345UL, new YIELD::SerializableFactoryImpl<global_time_getSyncRequest> );
          serializable_factories.registerSerializableFactory( 1488245919UL, new YIELD::SerializableFactoryImpl<admin_checkpointResponse> ); serializable_factories.registerSerializableFactory( 839471856UL, new YIELD::SerializableFactoryImpl<admin_checkpointRequest> ); serializable_factories.registerSerializableFactory( 1925339384UL, new YIELD::SerializableFactoryImpl<admin_checkpointSyncRequest> );
          serializable_factories.registerSerializableFactory( 1280024429UL, new YIELD::SerializableFactoryImpl<admin_shutdownResponse> ); serializable_factories.registerSerializableFactory( 1760149581UL, new YIELD::SerializableFactoryImpl<admin_shutdownRequest> ); serializable_factories.registerSerializableFactory( 2862779601UL, new YIELD::SerializableFactoryImpl<admin_shutdownSyncRequest> );
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
              case 1983642220UL: handleaddress_mappings_getRequest( static_cast<address_mappings_getRequest&>( ev ) ); return;
              case 1955507110UL: handleaddress_mappings_setRequest( static_cast<address_mappings_setRequest&>( ev ) ); return;
              case 2769570357UL: handleaddress_mappings_deleteRequest( static_cast<address_mappings_deleteRequest&>( ev ) ); return;
              case 2211822638UL: handleservice_registerRequest( static_cast<service_registerRequest&>( ev ) ); return;
              case 3067256812UL: handleservice_deregisterRequest( static_cast<service_deregisterRequest&>( ev ) ); return;
              case 4251087078UL: handleservice_get_by_typeRequest( static_cast<service_get_by_typeRequest&>( ev ) ); return;
              case 2653848206UL: handleservice_get_by_uuidRequest( static_cast<service_get_by_uuidRequest&>( ev ) ); return;
              case 1678219641UL: handleglobal_time_getRequest( static_cast<global_time_getRequest&>( ev ) ); return;
              case 839471856UL: handleadmin_checkpointRequest( static_cast<admin_checkpointRequest&>( ev ) ); return;
              case 1760149581UL: handleadmin_shutdownRequest( static_cast<admin_shutdownRequest&>( ev ) ); return;
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
          virtual void handleaddress_mappings_getRequest( address_mappings_getRequest& req ) { address_mappings_getResponse* resp = NULL; try { resp = new address_mappings_getResponse; org::xtreemfs::interfaces::AddressMappingSet address_mappings; _address_mappings_get( req.get_uuid(), address_mappings ); resp->set_address_mappings( address_mappings ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleaddress_mappings_setRequest( address_mappings_setRequest& req ) { address_mappings_setResponse* resp = NULL; try { resp = new address_mappings_setResponse; uint64_t _return_value = _address_mappings_set( req.get_address_mappings() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleaddress_mappings_deleteRequest( address_mappings_deleteRequest& req ) { address_mappings_deleteResponse* resp = NULL; try { resp = new address_mappings_deleteResponse; _address_mappings_delete( req.get_uuid() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleservice_registerRequest( service_registerRequest& req ) { service_registerResponse* resp = NULL; try { resp = new service_registerResponse; uint64_t _return_value = _service_register( req.get_service() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleservice_deregisterRequest( service_deregisterRequest& req ) { service_deregisterResponse* resp = NULL; try { resp = new service_deregisterResponse; _service_deregister( req.get_uuid() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleservice_get_by_typeRequest( service_get_by_typeRequest& req ) { service_get_by_typeResponse* resp = NULL; try { resp = new service_get_by_typeResponse; org::xtreemfs::interfaces::ServiceRegistrySet services; _service_get_by_type( req.get_type(), services ); resp->set_services( services ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleservice_get_by_uuidRequest( service_get_by_uuidRequest& req ) { service_get_by_uuidResponse* resp = NULL; try { resp = new service_get_by_uuidResponse; org::xtreemfs::interfaces::ServiceRegistrySet services; _service_get_by_uuid( req.get_uuid(), services ); resp->set_services( services ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleglobal_time_getRequest( global_time_getRequest& req ) { global_time_getResponse* resp = NULL; try { resp = new global_time_getResponse; uint64_t _return_value = _global_time_get(); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleadmin_checkpointRequest( admin_checkpointRequest& req ) { admin_checkpointResponse* resp = NULL; try { resp = new admin_checkpointResponse; _admin_checkpoint( req.get_password() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleadmin_shutdownRequest( admin_shutdownRequest& req ) { admin_shutdownResponse* resp = NULL; try { resp = new admin_shutdownResponse; _admin_shutdown( req.get_password() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
  
      virtual void _address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { }
        virtual uint64_t _address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { return 0; }
        virtual void _address_mappings_delete( const std::string& uuid ) { }
        virtual uint64_t _service_register( const org::xtreemfs::interfaces::ServiceRegistry& service ) { return 0; }
        virtual void _service_deregister( const std::string& uuid ) { }
        virtual void _service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceRegistrySet& services ) { }
        virtual void _service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceRegistrySet& services ) { }
        virtual uint64_t _global_time_get() { return 0; }
        virtual void _admin_checkpoint( const std::string& password ) { }
        virtual void _admin_shutdown( const std::string& password ) { }
      };
  
      // Use this macro in an implementation class to get all of the prototypes for the operations in DIRInterface
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_PROTOTYPES \
      virtual void _address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings );\
      virtual uint64_t _address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings );\
      virtual void _address_mappings_delete( const std::string& uuid );\
      virtual uint64_t _service_register( const org::xtreemfs::interfaces::ServiceRegistry& service );\
      virtual void _service_deregister( const std::string& uuid );\
      virtual void _service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceRegistrySet& services );\
      virtual void _service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceRegistrySet& services );\
      virtual uint64_t _global_time_get();\
      virtual void _admin_checkpoint( const std::string& password );\
      virtual void _admin_shutdown( const std::string& password );
  
      // Use this macro in an implementation class to get dummy implementations for the operations in this interface
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_DUMMY_DEFINITIONS \
      virtual void _address_mappings_get( const std::string& uuid, org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { }\
      virtual uint64_t _address_mappings_set( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings ) { return 0; }\
      virtual void _address_mappings_delete( const std::string& uuid ) { }\
      virtual uint64_t _service_register( const org::xtreemfs::interfaces::ServiceRegistry& service ) { return 0; }\
      virtual void _service_deregister( const std::string& uuid ) { }\
      virtual void _service_get_by_type( uint16_t type, org::xtreemfs::interfaces::ServiceRegistrySet& services ) { }\
      virtual void _service_get_by_uuid( const std::string& uuid, org::xtreemfs::interfaces::ServiceRegistrySet& services ) { }\
      virtual uint64_t _global_time_get() { return 0; }\
      virtual void _admin_checkpoint( const std::string& password ) { }\
      virtual void _admin_shutdown( const std::string& password ) { }
  
      #define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handleaddress_mappings_getRequest( address_mappings_getRequest& req );\
      virtual void handleaddress_mappings_setRequest( address_mappings_setRequest& req );\
      virtual void handleaddress_mappings_deleteRequest( address_mappings_deleteRequest& req );\
      virtual void handleservice_registerRequest( service_registerRequest& req );\
      virtual void handleservice_deregisterRequest( service_deregisterRequest& req );\
      virtual void handleservice_get_by_typeRequest( service_get_by_typeRequest& req );\
      virtual void handleservice_get_by_uuidRequest( service_get_by_uuidRequest& req );\
      virtual void handleglobal_time_getRequest( global_time_getRequest& req );\
      virtual void handleadmin_checkpointRequest( admin_checkpointRequest& req );\
      virtual void handleadmin_shutdownRequest( admin_shutdownRequest& req );
  
    };
  
  
  
  };
  
  

};

#endif
