// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_INTERFACES_OSD_INTERFACE_H
#define ORG_XTREEMFS_INTERFACES_OSD_INTERFACE_H

#include "constants.h"
#include "mrc_osd_types.h"
#include "yield/arch.h"
#include "yield/platform.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {

      class InternalGmax : public YIELD::Serializable
      {
      public:
        InternalGmax() : epoch( 0 ), last_object_id( 0 ), file_size( 0 ) { }
        InternalGmax( uint64_t epoch, uint64_t last_object_id, uint64_t file_size ) : epoch( epoch ), last_object_id( last_object_id ), file_size( file_size ) { }
        virtual ~InternalGmax() { }

        void set_epoch( uint64_t epoch ) { this->epoch = epoch; }
        uint64_t get_epoch() const { return epoch; }
        void set_last_object_id( uint64_t last_object_id ) { this->last_object_id = last_object_id; }
        uint64_t get_last_object_id() const { return last_object_id; }
        void set_file_size( uint64_t file_size ) { this->file_size = file_size; }
        uint64_t get_file_size() const { return file_size; }

        bool operator==( const InternalGmax& other ) const { return epoch == other.epoch && last_object_id == other.last_object_id && file_size == other.file_size; }

        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::InternalGmax", 3838673994UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { epoch = input_stream.readUint64( YIELD::StructuredStream::Declaration( "epoch" ) ); last_object_id = input_stream.readUint64( YIELD::StructuredStream::Declaration( "last_object_id" ) ); file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "file_size" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "epoch" ), epoch ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "last_object_id" ), last_object_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "file_size" ), file_size ); }

      protected:
        uint64_t epoch;
        uint64_t last_object_id;
        uint64_t file_size;
      };

      class ObjectData : public YIELD::Serializable
      {
      public:
        ObjectData() : data( NULL ), checksum( 0 ), zero_padding( 0 ), invalid_checksum_on_osd( false ) { }
        ObjectData( const YIELD::auto_SharedObject<YIELD::Serializable>& data, uint32_t checksum, uint32_t zero_padding, bool invalid_checksum_on_osd ) : data( data ), checksum( checksum ), zero_padding( zero_padding ), invalid_checksum_on_osd( invalid_checksum_on_osd ) { }
        virtual ~ObjectData() { }

        void set_data( YIELD::Serializable* data ) { this->data.reset( data ); }
        void set_data( YIELD::auto_SharedObject<YIELD::Serializable>& data ) { this->data = data; }
        YIELD::auto_SharedObject<Serializable> get_data() const { return YIELD::SharedObject::incRef( data.get() ); }
        void set_checksum( uint32_t checksum ) { this->checksum = checksum; }
        uint32_t get_checksum() const { return checksum; }
        void set_zero_padding( uint32_t zero_padding ) { this->zero_padding = zero_padding; }
        uint32_t get_zero_padding() const { return zero_padding; }
        void set_invalid_checksum_on_osd( bool invalid_checksum_on_osd ) { this->invalid_checksum_on_osd = invalid_checksum_on_osd; }
        bool get_invalid_checksum_on_osd() const { return invalid_checksum_on_osd; }

        bool operator==( const ObjectData& other ) const { return data.get() == other.data.get() && checksum == other.checksum && zero_padding == other.zero_padding && invalid_checksum_on_osd == other.invalid_checksum_on_osd; }

        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::ObjectData", 1049653481UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { data = input_stream.readSerializable( YIELD::StructuredStream::Declaration( "data" ) ); checksum = input_stream.readUint32( YIELD::StructuredStream::Declaration( "checksum" ) ); zero_padding = input_stream.readUint32( YIELD::StructuredStream::Declaration( "zero_padding" ) ); invalid_checksum_on_osd = input_stream.readBool( YIELD::StructuredStream::Declaration( "invalid_checksum_on_osd" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { if ( data.get() ) output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "data" ), *data.get() ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "checksum" ), checksum ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "zero_padding" ), zero_padding ); output_stream.writeBool( YIELD::StructuredStream::Declaration( "invalid_checksum_on_osd" ), invalid_checksum_on_osd ); }

      protected:
        YIELD::auto_SharedObject<YIELD::Serializable> data;
        uint32_t checksum;
        uint32_t zero_padding;
        bool invalid_checksum_on_osd;
      };

      class InternalReadLocalResponse : public YIELD::Serializable
      {
      public:
        InternalReadLocalResponse() : zero_padding( 0 ) { }
        InternalReadLocalResponse( const org::xtreemfs::interfaces::NewFileSize& new_file_size, uint32_t zero_padding, const org::xtreemfs::interfaces::ObjectData& data ) : new_file_size( new_file_size ), zero_padding( zero_padding ), data( data ) { }
        virtual ~InternalReadLocalResponse() { }

        void set_new_file_size( const org::xtreemfs::interfaces::NewFileSize&  new_file_size ) { this->new_file_size = new_file_size; }
        const org::xtreemfs::interfaces::NewFileSize& get_new_file_size() const { return new_file_size; }
        void set_zero_padding( uint32_t zero_padding ) { this->zero_padding = zero_padding; }
        uint32_t get_zero_padding() const { return zero_padding; }
        void set_data( const org::xtreemfs::interfaces::ObjectData&  data ) { this->data = data; }
        const org::xtreemfs::interfaces::ObjectData& get_data() const { return data; }

        bool operator==( const InternalReadLocalResponse& other ) const { return new_file_size == other.new_file_size && zero_padding == other.zero_padding && data == other.data; }

        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::InternalReadLocalResponse", 375306877UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::NewFileSize", "new_file_size" ), &new_file_size ); zero_padding = input_stream.readUint32( YIELD::StructuredStream::Declaration( "zero_padding" ) ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "data" ), &data ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::NewFileSize", "new_file_size" ), new_file_size ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "zero_padding" ), zero_padding ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "data" ), data ); }

      protected:
        org::xtreemfs::interfaces::NewFileSize new_file_size;
        uint32_t zero_padding;
        org::xtreemfs::interfaces::ObjectData data;
      };



      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS YIELD::EventHandler
      #endif
      #endif

      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS ORG_XTREEMFS_REQUEST_PARENT_CLASS
      #elif defined( ORG_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS ORG_REQUEST_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS YIELD::Request
      #endif
      #endif

      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS ORG_XTREEMFS_RESPONSE_PARENT_CLASS
      #elif defined( ORG_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS ORG_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS YIELD::Response
      #endif
      #endif

      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_EVENT_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_EVENT_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_EVENT_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_EVENT_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_EVENT_PARENT_CLASS ORG_EXCEPTION_EVENT_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_EVENT_PARENT_CLASS YIELD::ExceptionEvent
      #endif
      #endif



      class OSDInterface : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS
      {
      public:
        OSDInterface() { }
        virtual ~OSDInterface() { }


      const static uint32_t DEFAULT_ONCRPC_PORT = 32640;
      const static uint32_t DEFAULT_ONCRPCS_PORT = 32640;
      const static uint32_t DEFAULT_HTTP_PORT = 30640;

      virtual void read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data ) { read( file_credentials, file_id, object_number, object_version, offset, length, object_data, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data, YIELD::EventTarget* send_target ) { read( file_credentials, file_id, object_number, object_version, offset, length, object_data, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data, YIELD::timeout_ns_t response_timeout_ns ) { read( file_credentials, file_id, object_number, object_version, offset, length, object_data, NULL, response_timeout_ns ); }
        virtual void read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { readSyncRequest* __req = new readSyncRequest( file_credentials, file_id, object_number, object_version, offset, length ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { readResponse& __resp = ( readResponse& )__req->waitForDefaultResponse( response_timeout_ns ); object_data = __resp.get_object_data(); YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }
        virtual void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { truncate( file_credentials, file_id, new_file_size, osd_write_response, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target ) { truncate( file_credentials, file_id, new_file_size, osd_write_response, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::timeout_ns_t response_timeout_ns ) { truncate( file_credentials, file_id, new_file_size, osd_write_response, NULL, response_timeout_ns ); }
        virtual void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { truncateSyncRequest* __req = new truncateSyncRequest( file_credentials, file_id, new_file_size ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { truncateResponse& __resp = ( truncateResponse& )__req->waitForDefaultResponse( response_timeout_ns ); osd_write_response = __resp.get_osd_write_response(); YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }
        virtual void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { unlink( file_credentials, file_id, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, YIELD::EventTarget* send_target ) { unlink( file_credentials, file_id, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, YIELD::timeout_ns_t response_timeout_ns ) { unlink( file_credentials, file_id, NULL, response_timeout_ns ); }
        virtual void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { unlinkSyncRequest* __req = new unlinkSyncRequest( file_credentials, file_id ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { unlinkResponse& __resp = ( unlinkResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }
        virtual void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { write( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data, osd_write_response, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target ) { write( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data, osd_write_response, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::timeout_ns_t response_timeout_ns ) { write( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data, osd_write_response, NULL, response_timeout_ns ); }
        virtual void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { writeSyncRequest* __req = new writeSyncRequest( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { writeResponse& __resp = ( writeResponse& )__req->waitForDefaultResponse( response_timeout_ns ); osd_write_response = __resp.get_osd_write_response(); YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }
        virtual org::xtreemfs::interfaces::ObjectData xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version ) { return xtreemfs_check_object( file_credentials, file_id, object_number, object_version, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::ObjectData xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, YIELD::EventTarget* send_target ) { return xtreemfs_check_object( file_credentials, file_id, object_number, object_version, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::ObjectData xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, YIELD::timeout_ns_t response_timeout_ns ) { return xtreemfs_check_object( file_credentials, file_id, object_number, object_version, NULL, response_timeout_ns ); }
        virtual org::xtreemfs::interfaces::ObjectData xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_check_objectSyncRequest* __req = new xtreemfs_check_objectSyncRequest( file_credentials, file_id, object_number, object_version ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { xtreemfs_check_objectResponse& __resp = ( xtreemfs_check_objectResponse& )__req->waitForDefaultResponse( response_timeout_ns ); org::xtreemfs::interfaces::ObjectData _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); return _return_value; } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }
        virtual org::xtreemfs::interfaces::InternalGmax xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return xtreemfs_internal_get_gmax( file_credentials, file_id, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalGmax xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, YIELD::EventTarget* send_target ) { return xtreemfs_internal_get_gmax( file_credentials, file_id, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalGmax xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, YIELD::timeout_ns_t response_timeout_ns ) { return xtreemfs_internal_get_gmax( file_credentials, file_id, NULL, response_timeout_ns ); }
        virtual org::xtreemfs::interfaces::InternalGmax xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_internal_get_gmaxSyncRequest* __req = new xtreemfs_internal_get_gmaxSyncRequest( file_credentials, file_id ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { xtreemfs_internal_get_gmaxResponse& __resp = ( xtreemfs_internal_get_gmaxResponse& )__req->waitForDefaultResponse( response_timeout_ns ); org::xtreemfs::interfaces::InternalGmax _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); return _return_value; } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }
        virtual uint64_t xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return xtreemfs_internal_get_file_size( file_credentials, file_id, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, YIELD::EventTarget* send_target ) { return xtreemfs_internal_get_file_size( file_credentials, file_id, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual uint64_t xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, YIELD::timeout_ns_t response_timeout_ns ) { return xtreemfs_internal_get_file_size( file_credentials, file_id, NULL, response_timeout_ns ); }
        virtual uint64_t xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_internal_get_file_sizeSyncRequest* __req = new xtreemfs_internal_get_file_sizeSyncRequest( file_credentials, file_id ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { xtreemfs_internal_get_file_sizeResponse& __resp = ( xtreemfs_internal_get_file_sizeResponse& )__req->waitForDefaultResponse( response_timeout_ns ); uint64_t _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); return _return_value; } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }
        virtual void xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { xtreemfs_internal_truncate( file_credentials, file_id, new_file_size, osd_write_response, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target ) { xtreemfs_internal_truncate( file_credentials, file_id, new_file_size, osd_write_response, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_internal_truncate( file_credentials, file_id, new_file_size, osd_write_response, NULL, response_timeout_ns ); }
        virtual void xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_internal_truncateSyncRequest* __req = new xtreemfs_internal_truncateSyncRequest( file_credentials, file_id, new_file_size ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { xtreemfs_internal_truncateResponse& __resp = ( xtreemfs_internal_truncateResponse& )__req->waitForDefaultResponse( response_timeout_ns ); osd_write_response = __resp.get_osd_write_response(); YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) { return xtreemfs_internal_read_local( file_credentials, file_id, object_number, object_version, offset, length, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, YIELD::EventTarget* send_target ) { return xtreemfs_internal_read_local( file_credentials, file_id, object_number, object_version, offset, length, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, YIELD::timeout_ns_t response_timeout_ns ) { return xtreemfs_internal_read_local( file_credentials, file_id, object_number, object_version, offset, length, NULL, response_timeout_ns ); }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_internal_read_localSyncRequest* __req = new xtreemfs_internal_read_localSyncRequest( file_credentials, file_id, object_number, object_version, offset, length ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { xtreemfs_internal_read_localResponse& __resp = ( xtreemfs_internal_read_localResponse& )__req->waitForDefaultResponse( response_timeout_ns ); org::xtreemfs::interfaces::InternalReadLocalResponse _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); return _return_value; } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }
        virtual void xtreemfs_shutdown() { xtreemfs_shutdown( NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( YIELD::EventTarget* send_target ) { xtreemfs_shutdown( send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_shutdown( NULL, response_timeout_ns ); }
        virtual void xtreemfs_shutdown( YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_shutdownSyncRequest* __req = new xtreemfs_shutdownSyncRequest(); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::SharedObject::incRef( *__req ) ); try { xtreemfs_shutdownResponse& __resp = ( xtreemfs_shutdownResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::SharedObject::decRef( __resp ); YIELD::SharedObject::decRef( *__req ); } catch ( ... ) { YIELD::SharedObject::decRef( *__req ); throw; } }  // Request/response pair Event type definitions for the operations in OSDInterface

      class readResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        readResponse() { }
        readResponse( const org::xtreemfs::interfaces::ObjectData& object_data ) : object_data( object_data ) { }
        virtual ~readResponse() { }

        void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }
        const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }

        bool operator==( const readResponse& other ) const { return object_data == other.object_data; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::readResponse", 4289876024UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "object_data" ), &object_data ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "object_data" ), object_data ); }

      protected:
        org::xtreemfs::interfaces::ObjectData object_data;
      };

      class readRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        readRequest() : object_number( 0 ), object_version( 0 ), offset( 0 ), length( 0 ) { }
        readRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length ) : file_credentials( file_credentials ), file_id( file_id ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ) { }
        readRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ) { }
        virtual ~readRequest() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        uint64_t get_object_number() const { return object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
        uint64_t get_object_version() const { return object_version; }
        void set_offset( uint32_t offset ) { this->offset = offset; }
        uint32_t get_offset() const { return offset; }
        void set_length( uint32_t length ) { this->length = length; }
        uint32_t get_length() const { return length; }

        bool operator==( const readRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && object_number == other.object_number && object_version == other.object_version && offset == other.offset && length == other.length; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::readRequest", 4214126648UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); offset = input_stream.readUint32( YIELD::StructuredStream::Declaration( "offset" ) ); length = input_stream.readUint32( YIELD::StructuredStream::Declaration( "length" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "offset" ), offset ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "length" ), length ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 1; }

        virtual uint32_t getDefaultResponseTypeId() const { return 4289876024UL; }
        virtual Event* createDefaultResponse() { return new readResponse; }


      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        std::string file_id;
        uint64_t object_number;
        uint64_t object_version;
        uint32_t offset;
        uint32_t length;
      };

      class readSyncRequest : public readRequest
      {
      public:
        readSyncRequest() : readRequest( org::xtreemfs::interfaces::FileCredentials(), std::string(), 0, 0, 0, 0 ) { }
        readSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length ) : readRequest( file_credentials, file_id, object_number, object_version, offset, length ) { }
        readSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length ) : readRequest( file_credentials, file_id, file_id_len, object_number, object_version, offset, length ) { }
        virtual ~readSyncRequest() { }

        bool operator==( const readSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::readResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class truncateResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        truncateResponse() { }
        truncateResponse( const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) : osd_write_response( osd_write_response ) { }
        virtual ~truncateResponse() { }

        void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }
        const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }

        bool operator==( const truncateResponse& other ) const { return osd_write_response == other.osd_write_response; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::truncateResponse", 233138659UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), &osd_write_response ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), osd_write_response ); }

      protected:
        org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
      };

      class truncateRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        truncateRequest() : new_file_size( 0 ) { }
        truncateRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size ) : file_credentials( file_credentials ), file_id( file_id ), new_file_size( new_file_size ) { }
        truncateRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t new_file_size ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ), new_file_size( new_file_size ) { }
        virtual ~truncateRequest() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }
        uint64_t get_new_file_size() const { return new_file_size; }

        bool operator==( const truncateRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && new_file_size == other.new_file_size; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::truncateRequest", 3445878689UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); new_file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "new_file_size" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "new_file_size" ), new_file_size ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 2; }

        virtual uint32_t getDefaultResponseTypeId() const { return 233138659UL; }
        virtual Event* createDefaultResponse() { return new truncateResponse; }


      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        std::string file_id;
        uint64_t new_file_size;
      };

      class truncateSyncRequest : public truncateRequest
      {
      public:
        truncateSyncRequest() : truncateRequest( org::xtreemfs::interfaces::FileCredentials(), std::string(), 0 ) { }
        truncateSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size ) : truncateRequest( file_credentials, file_id, new_file_size ) { }
        truncateSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t new_file_size ) : truncateRequest( file_credentials, file_id, file_id_len, new_file_size ) { }
        virtual ~truncateSyncRequest() { }

        bool operator==( const truncateSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::truncateResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class unlinkResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        unlinkResponse() { }
        virtual ~unlinkResponse() { }

        bool operator==( const unlinkResponse& ) const { return true; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::unlinkResponse", 1159409605UL );

      };

      class unlinkRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        unlinkRequest() { }
        unlinkRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) : file_credentials( file_credentials ), file_id( file_id ) { }
        unlinkRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ) { }
        virtual ~unlinkRequest() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }

        bool operator==( const unlinkRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::unlinkRequest", 2625011690UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 3; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1159409605UL; }
        virtual Event* createDefaultResponse() { return new unlinkResponse; }


      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        std::string file_id;
      };

      class unlinkSyncRequest : public unlinkRequest
      {
      public:
        unlinkSyncRequest() : unlinkRequest( org::xtreemfs::interfaces::FileCredentials(), std::string() ) { }
        unlinkSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) : unlinkRequest( file_credentials, file_id ) { }
        unlinkSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len ) : unlinkRequest( file_credentials, file_id, file_id_len ) { }
        virtual ~unlinkSyncRequest() { }

        bool operator==( const unlinkSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::unlinkResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class writeResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        writeResponse() { }
        writeResponse( const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) : osd_write_response( osd_write_response ) { }
        virtual ~writeResponse() { }

        void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }
        const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }

        bool operator==( const writeResponse& other ) const { return osd_write_response == other.osd_write_response; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::writeResponse", 3887614948UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), &osd_write_response ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), osd_write_response ); }

      protected:
        org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
      };

      class writeRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        writeRequest() : object_number( 0 ), object_version( 0 ), offset( 0 ), lease_timeout( 0 ) { }
        writeRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data ) : file_credentials( file_credentials ), file_id( file_id ), object_number( object_number ), object_version( object_version ), offset( offset ), lease_timeout( lease_timeout ), object_data( object_data ) { }
        writeRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ), object_number( object_number ), object_version( object_version ), offset( offset ), lease_timeout( lease_timeout ), object_data( object_data ) { }
        virtual ~writeRequest() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        uint64_t get_object_number() const { return object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
        uint64_t get_object_version() const { return object_version; }
        void set_offset( uint32_t offset ) { this->offset = offset; }
        uint32_t get_offset() const { return offset; }
        void set_lease_timeout( uint64_t lease_timeout ) { this->lease_timeout = lease_timeout; }
        uint64_t get_lease_timeout() const { return lease_timeout; }
        void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }
        const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }

        bool operator==( const writeRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && object_number == other.object_number && object_version == other.object_version && offset == other.offset && lease_timeout == other.lease_timeout && object_data == other.object_data; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::writeRequest", 3616508705UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); offset = input_stream.readUint32( YIELD::StructuredStream::Declaration( "offset" ) ); lease_timeout = input_stream.readUint64( YIELD::StructuredStream::Declaration( "lease_timeout" ) ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "object_data" ), &object_data ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "offset" ), offset ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "lease_timeout" ), lease_timeout ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "object_data" ), object_data ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 4; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3887614948UL; }
        virtual Event* createDefaultResponse() { return new writeResponse; }


      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        std::string file_id;
        uint64_t object_number;
        uint64_t object_version;
        uint32_t offset;
        uint64_t lease_timeout;
        org::xtreemfs::interfaces::ObjectData object_data;
      };

      class writeSyncRequest : public writeRequest
      {
      public:
        writeSyncRequest() : writeRequest( org::xtreemfs::interfaces::FileCredentials(), std::string(), 0, 0, 0, 0, org::xtreemfs::interfaces::ObjectData() ) { }
        writeSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data ) : writeRequest( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data ) { }
        writeSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data ) : writeRequest( file_credentials, file_id, file_id_len, object_number, object_version, offset, lease_timeout, object_data ) { }
        virtual ~writeSyncRequest() { }

        bool operator==( const writeSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::writeResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_check_objectResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_check_objectResponse() { }
        xtreemfs_check_objectResponse( const org::xtreemfs::interfaces::ObjectData& _return_value ) : _return_value( _return_value ) { }
        virtual ~xtreemfs_check_objectResponse() { }

        void set__return_value( const org::xtreemfs::interfaces::ObjectData&  _return_value ) { this->_return_value = _return_value; }
        const org::xtreemfs::interfaces::ObjectData& get__return_value() const { return _return_value; }

        bool operator==( const xtreemfs_check_objectResponse& other ) const { return _return_value == other._return_value; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_check_objectResponse", 1067317409UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "_return_value" ), &_return_value ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "_return_value" ), _return_value ); }

      protected:
        org::xtreemfs::interfaces::ObjectData _return_value;
      };

      class xtreemfs_check_objectRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_check_objectRequest() : object_number( 0 ), object_version( 0 ) { }
        xtreemfs_check_objectRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version ) : file_credentials( file_credentials ), file_id( file_id ), object_number( object_number ), object_version( object_version ) { }
        xtreemfs_check_objectRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t object_number, uint64_t object_version ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ), object_number( object_number ), object_version( object_version ) { }
        virtual ~xtreemfs_check_objectRequest() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        uint64_t get_object_number() const { return object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
        uint64_t get_object_version() const { return object_version; }

        bool operator==( const xtreemfs_check_objectRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && object_number == other.object_number && object_version == other.object_version; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_check_objectRequest", 3353072678UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 103; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1067317409UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_check_objectResponse; }


      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        std::string file_id;
        uint64_t object_number;
        uint64_t object_version;
      };

      class xtreemfs_check_objectSyncRequest : public xtreemfs_check_objectRequest
      {
      public:
        xtreemfs_check_objectSyncRequest() : xtreemfs_check_objectRequest( org::xtreemfs::interfaces::FileCredentials(), std::string(), 0, 0 ) { }
        xtreemfs_check_objectSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version ) : xtreemfs_check_objectRequest( file_credentials, file_id, object_number, object_version ) { }
        xtreemfs_check_objectSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t object_number, uint64_t object_version ) : xtreemfs_check_objectRequest( file_credentials, file_id, file_id_len, object_number, object_version ) { }
        virtual ~xtreemfs_check_objectSyncRequest() { }

        bool operator==( const xtreemfs_check_objectSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::xtreemfs_check_objectResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_internal_get_gmaxResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_internal_get_gmaxResponse() { }
        xtreemfs_internal_get_gmaxResponse( const org::xtreemfs::interfaces::InternalGmax& _return_value ) : _return_value( _return_value ) { }
        virtual ~xtreemfs_internal_get_gmaxResponse() { }

        void set__return_value( const org::xtreemfs::interfaces::InternalGmax&  _return_value ) { this->_return_value = _return_value; }
        const org::xtreemfs::interfaces::InternalGmax& get__return_value() const { return _return_value; }

        bool operator==( const xtreemfs_internal_get_gmaxResponse& other ) const { return _return_value == other._return_value; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_gmaxResponse", 827639084UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalGmax", "_return_value" ), &_return_value ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalGmax", "_return_value" ), _return_value ); }

      protected:
        org::xtreemfs::interfaces::InternalGmax _return_value;
      };

      class xtreemfs_internal_get_gmaxRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_internal_get_gmaxRequest() { }
        xtreemfs_internal_get_gmaxRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) : file_credentials( file_credentials ), file_id( file_id ) { }
        xtreemfs_internal_get_gmaxRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ) { }
        virtual ~xtreemfs_internal_get_gmaxRequest() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }

        bool operator==( const xtreemfs_internal_get_gmaxRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_gmaxRequest", 1588008696UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 100; }

        virtual uint32_t getDefaultResponseTypeId() const { return 827639084UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_internal_get_gmaxResponse; }


      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        std::string file_id;
      };

      class xtreemfs_internal_get_gmaxSyncRequest : public xtreemfs_internal_get_gmaxRequest
      {
      public:
        xtreemfs_internal_get_gmaxSyncRequest() : xtreemfs_internal_get_gmaxRequest( org::xtreemfs::interfaces::FileCredentials(), std::string() ) { }
        xtreemfs_internal_get_gmaxSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) : xtreemfs_internal_get_gmaxRequest( file_credentials, file_id ) { }
        xtreemfs_internal_get_gmaxSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len ) : xtreemfs_internal_get_gmaxRequest( file_credentials, file_id, file_id_len ) { }
        virtual ~xtreemfs_internal_get_gmaxSyncRequest() { }

        bool operator==( const xtreemfs_internal_get_gmaxSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_gmaxResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_internal_get_file_sizeResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_internal_get_file_sizeResponse() : _return_value( 0 ) { }
        xtreemfs_internal_get_file_sizeResponse( uint64_t _return_value ) : _return_value( _return_value ) { }
        virtual ~xtreemfs_internal_get_file_sizeResponse() { }

        void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }
        uint64_t get__return_value() const { return _return_value; }

        bool operator==( const xtreemfs_internal_get_file_sizeResponse& other ) const { return _return_value == other._return_value; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_file_sizeResponse", 3846913658UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { _return_value = input_stream.readUint64( YIELD::StructuredStream::Declaration( "_return_value" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }

      protected:
        uint64_t _return_value;
      };

      class xtreemfs_internal_get_file_sizeRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_internal_get_file_sizeRequest() { }
        xtreemfs_internal_get_file_sizeRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) : file_credentials( file_credentials ), file_id( file_id ) { }
        xtreemfs_internal_get_file_sizeRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ) { }
        virtual ~xtreemfs_internal_get_file_sizeRequest() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }

        bool operator==( const xtreemfs_internal_get_file_sizeRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_file_sizeRequest", 3111153693UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 104; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3846913658UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_internal_get_file_sizeResponse; }


      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        std::string file_id;
      };

      class xtreemfs_internal_get_file_sizeSyncRequest : public xtreemfs_internal_get_file_sizeRequest
      {
      public:
        xtreemfs_internal_get_file_sizeSyncRequest() : xtreemfs_internal_get_file_sizeRequest( org::xtreemfs::interfaces::FileCredentials(), std::string() ) { }
        xtreemfs_internal_get_file_sizeSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) : xtreemfs_internal_get_file_sizeRequest( file_credentials, file_id ) { }
        xtreemfs_internal_get_file_sizeSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len ) : xtreemfs_internal_get_file_sizeRequest( file_credentials, file_id, file_id_len ) { }
        virtual ~xtreemfs_internal_get_file_sizeSyncRequest() { }

        bool operator==( const xtreemfs_internal_get_file_sizeSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_file_sizeResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_internal_truncateResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_internal_truncateResponse() { }
        xtreemfs_internal_truncateResponse( const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) : osd_write_response( osd_write_response ) { }
        virtual ~xtreemfs_internal_truncateResponse() { }

        void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }
        const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }

        bool operator==( const xtreemfs_internal_truncateResponse& other ) const { return osd_write_response == other.osd_write_response; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_truncateResponse", 666509058UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), &osd_write_response ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), osd_write_response ); }

      protected:
        org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
      };

      class xtreemfs_internal_truncateRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_internal_truncateRequest() : new_file_size( 0 ) { }
        xtreemfs_internal_truncateRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size ) : file_credentials( file_credentials ), file_id( file_id ), new_file_size( new_file_size ) { }
        xtreemfs_internal_truncateRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t new_file_size ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ), new_file_size( new_file_size ) { }
        virtual ~xtreemfs_internal_truncateRequest() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }
        uint64_t get_new_file_size() const { return new_file_size; }

        bool operator==( const xtreemfs_internal_truncateRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && new_file_size == other.new_file_size; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_truncateRequest", 2996883011UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); new_file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "new_file_size" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "new_file_size" ), new_file_size ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 101; }

        virtual uint32_t getDefaultResponseTypeId() const { return 666509058UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_internal_truncateResponse; }


      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        std::string file_id;
        uint64_t new_file_size;
      };

      class xtreemfs_internal_truncateSyncRequest : public xtreemfs_internal_truncateRequest
      {
      public:
        xtreemfs_internal_truncateSyncRequest() : xtreemfs_internal_truncateRequest( org::xtreemfs::interfaces::FileCredentials(), std::string(), 0 ) { }
        xtreemfs_internal_truncateSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size ) : xtreemfs_internal_truncateRequest( file_credentials, file_id, new_file_size ) { }
        xtreemfs_internal_truncateSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t new_file_size ) : xtreemfs_internal_truncateRequest( file_credentials, file_id, file_id_len, new_file_size ) { }
        virtual ~xtreemfs_internal_truncateSyncRequest() { }

        bool operator==( const xtreemfs_internal_truncateSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_truncateResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_internal_read_localResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_internal_read_localResponse() { }
        xtreemfs_internal_read_localResponse( const org::xtreemfs::interfaces::InternalReadLocalResponse& _return_value ) : _return_value( _return_value ) { }
        virtual ~xtreemfs_internal_read_localResponse() { }

        void set__return_value( const org::xtreemfs::interfaces::InternalReadLocalResponse&  _return_value ) { this->_return_value = _return_value; }
        const org::xtreemfs::interfaces::InternalReadLocalResponse& get__return_value() const { return _return_value; }

        bool operator==( const xtreemfs_internal_read_localResponse& other ) const { return _return_value == other._return_value; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_read_localResponse", 2259419931UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalReadLocalResponse", "_return_value" ), &_return_value ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalReadLocalResponse", "_return_value" ), _return_value ); }

      protected:
        org::xtreemfs::interfaces::InternalReadLocalResponse _return_value;
      };

      class xtreemfs_internal_read_localRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_internal_read_localRequest() : object_number( 0 ), object_version( 0 ), offset( 0 ), length( 0 ) { }
        xtreemfs_internal_read_localRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) : file_credentials( file_credentials ), file_id( file_id ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ) { }
        xtreemfs_internal_read_localRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ) { }
        virtual ~xtreemfs_internal_read_localRequest() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        uint64_t get_object_number() const { return object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
        uint64_t get_object_version() const { return object_version; }
        void set_offset( uint64_t offset ) { this->offset = offset; }
        uint64_t get_offset() const { return offset; }
        void set_length( uint64_t length ) { this->length = length; }
        uint64_t get_length() const { return length; }

        bool operator==( const xtreemfs_internal_read_localRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && object_number == other.object_number && object_version == other.object_version && offset == other.offset && length == other.length; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_read_localRequest", 3939848817UL );

        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); offset = input_stream.readUint64( YIELD::StructuredStream::Declaration( "offset" ) ); length = input_stream.readUint64( YIELD::StructuredStream::Declaration( "length" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "offset" ), offset ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "length" ), length ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 102; }

        virtual uint32_t getDefaultResponseTypeId() const { return 2259419931UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_internal_read_localResponse; }


      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        std::string file_id;
        uint64_t object_number;
        uint64_t object_version;
        uint64_t offset;
        uint64_t length;
      };

      class xtreemfs_internal_read_localSyncRequest : public xtreemfs_internal_read_localRequest
      {
      public:
        xtreemfs_internal_read_localSyncRequest() : xtreemfs_internal_read_localRequest( org::xtreemfs::interfaces::FileCredentials(), std::string(), 0, 0, 0, 0 ) { }
        xtreemfs_internal_read_localSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) : xtreemfs_internal_read_localRequest( file_credentials, file_id, object_number, object_version, offset, length ) { }
        xtreemfs_internal_read_localSyncRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) : xtreemfs_internal_read_localRequest( file_credentials, file_id, file_id_len, object_number, object_version, offset, length ) { }
        virtual ~xtreemfs_internal_read_localSyncRequest() { }

        bool operator==( const xtreemfs_internal_read_localSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_read_localResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_shutdownResponse() { }
        virtual ~xtreemfs_shutdownResponse() { }

        bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_shutdownResponse", 3362631755UL );

      };

      class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_shutdownRequest() { }
        virtual ~xtreemfs_shutdownRequest() { }

        bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::xtreemfs_shutdownRequest", 1977271802UL );


        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 3; }
        virtual uint32_t getOperationNumber() const { return 50; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3362631755UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_shutdownResponse; }

      };

      class xtreemfs_shutdownSyncRequest : public xtreemfs_shutdownRequest
      {
      public:
        xtreemfs_shutdownSyncRequest() { }
        virtual ~xtreemfs_shutdownSyncRequest() { }

        bool operator==( const xtreemfs_shutdownSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::xtreemfs_shutdownResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

        class OSDException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_EVENT_PARENT_CLASS
        {
        public:
          OSDException() : error_code( 0 ) { }
        OSDException( uint32_t error_code, const std::string& error_message, const std::string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
        OSDException( uint32_t error_code, const char* error_message, size_t error_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), error_message( error_message, error_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
          OSDException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_EVENT_PARENT_CLASS( what ) { }
          virtual ~OSDException() throw() { }

        void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
        uint32_t get_error_code() const { return error_code; }
        void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
        void set_error_message( const char* error_message, size_t error_message_len = 0 ) { this->error_message.assign( error_message, ( error_message_len != 0 ) ? error_message_len : std::strlen( error_message ) ); }
        const std::string& get_error_message() const { return error_message; }
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len = 0 ) { this->stack_trace.assign( stack_trace, ( stack_trace_len != 0 ) ? stack_trace_len : std::strlen( stack_trace ) ); }
        const std::string& get_stack_trace() const { return stack_trace; }

          // YIELD::RTTI
          TYPE_INFO( EXCEPTION_EVENT, "org::xtreemfs::interfaces::OSDInterface::OSDException", 863197607UL );

          // YIELD::ExceptionEvent
          virtual ExceptionEvent* clone() const { return new OSDException( error_code, error_message, stack_trace); }
          virtual void throwStackClone() const { throw OSDException( error_code, error_message, stack_trace); }
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

      protected:
        uint32_t error_code;
        std::string error_message;
        std::string stack_trace;
        };



        void registerSerializableFactories( YIELD::SerializableFactories& serializable_factories )
        {
          serializable_factories.registerSerializableFactory( 4214126648UL, new YIELD::SerializableFactoryImpl<readRequest> ); serializable_factories.registerSerializableFactory( 3698651647UL, new YIELD::SerializableFactoryImpl<readSyncRequest> ); serializable_factories.registerSerializableFactory( 4289876024UL, new YIELD::SerializableFactoryImpl<readResponse> );
          serializable_factories.registerSerializableFactory( 3445878689UL, new YIELD::SerializableFactoryImpl<truncateRequest> ); serializable_factories.registerSerializableFactory( 2997414208UL, new YIELD::SerializableFactoryImpl<truncateSyncRequest> ); serializable_factories.registerSerializableFactory( 233138659UL, new YIELD::SerializableFactoryImpl<truncateResponse> );
          serializable_factories.registerSerializableFactory( 2625011690UL, new YIELD::SerializableFactoryImpl<unlinkRequest> ); serializable_factories.registerSerializableFactory( 2654062122UL, new YIELD::SerializableFactoryImpl<unlinkSyncRequest> ); serializable_factories.registerSerializableFactory( 1159409605UL, new YIELD::SerializableFactoryImpl<unlinkResponse> );
          serializable_factories.registerSerializableFactory( 3616508705UL, new YIELD::SerializableFactoryImpl<writeRequest> ); serializable_factories.registerSerializableFactory( 2126281761UL, new YIELD::SerializableFactoryImpl<writeSyncRequest> ); serializable_factories.registerSerializableFactory( 3887614948UL, new YIELD::SerializableFactoryImpl<writeResponse> );
          serializable_factories.registerSerializableFactory( 3353072678UL, new YIELD::SerializableFactoryImpl<xtreemfs_check_objectRequest> ); serializable_factories.registerSerializableFactory( 1140397443UL, new YIELD::SerializableFactoryImpl<xtreemfs_check_objectSyncRequest> ); serializable_factories.registerSerializableFactory( 1067317409UL, new YIELD::SerializableFactoryImpl<xtreemfs_check_objectResponse> );
          serializable_factories.registerSerializableFactory( 1588008696UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_get_gmaxRequest> ); serializable_factories.registerSerializableFactory( 2242047621UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_get_gmaxSyncRequest> ); serializable_factories.registerSerializableFactory( 827639084UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_get_gmaxResponse> );
          serializable_factories.registerSerializableFactory( 3111153693UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_get_file_sizeRequest> ); serializable_factories.registerSerializableFactory( 40033817UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_get_file_sizeSyncRequest> ); serializable_factories.registerSerializableFactory( 3846913658UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_get_file_sizeResponse> );
          serializable_factories.registerSerializableFactory( 2996883011UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_truncateRequest> ); serializable_factories.registerSerializableFactory( 3574935183UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_truncateSyncRequest> ); serializable_factories.registerSerializableFactory( 666509058UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_truncateResponse> );
          serializable_factories.registerSerializableFactory( 3939848817UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_read_localRequest> ); serializable_factories.registerSerializableFactory( 3473632705UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_read_localSyncRequest> ); serializable_factories.registerSerializableFactory( 2259419931UL, new YIELD::SerializableFactoryImpl<xtreemfs_internal_read_localResponse> );
          serializable_factories.registerSerializableFactory( 1977271802UL, new YIELD::SerializableFactoryImpl<xtreemfs_shutdownRequest> ); serializable_factories.registerSerializableFactory( 1832510619UL, new YIELD::SerializableFactoryImpl<xtreemfs_shutdownSyncRequest> ); serializable_factories.registerSerializableFactory( 3362631755UL, new YIELD::SerializableFactoryImpl<xtreemfs_shutdownResponse> );
          serializable_factories.registerSerializableFactory( 863197607UL, new YIELD::SerializableFactoryImpl<OSDException> );;
        }


        // EventHandler
        virtual const char* getEventHandlerName() const { return "OSDInterface"; }

        virtual void handleEvent( YIELD::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.getTypeId() )
            {
              case 4214126648UL: handlereadRequest( static_cast<readRequest&>( ev ) ); return;
              case 3445878689UL: handletruncateRequest( static_cast<truncateRequest&>( ev ) ); return;
              case 2625011690UL: handleunlinkRequest( static_cast<unlinkRequest&>( ev ) ); return;
              case 3616508705UL: handlewriteRequest( static_cast<writeRequest&>( ev ) ); return;
              case 3353072678UL: handlextreemfs_check_objectRequest( static_cast<xtreemfs_check_objectRequest&>( ev ) ); return;
              case 1588008696UL: handlextreemfs_internal_get_gmaxRequest( static_cast<xtreemfs_internal_get_gmaxRequest&>( ev ) ); return;
              case 3111153693UL: handlextreemfs_internal_get_file_sizeRequest( static_cast<xtreemfs_internal_get_file_sizeRequest&>( ev ) ); return;
              case 2996883011UL: handlextreemfs_internal_truncateRequest( static_cast<xtreemfs_internal_truncateRequest&>( ev ) ); return;
              case 3939848817UL: handlextreemfs_internal_read_localRequest( static_cast<xtreemfs_internal_read_localRequest&>( ev ) ); return;
              case 1977271802UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
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
          virtual void handlereadRequest( readRequest& req ) { readResponse* resp = NULL; try { resp = new readResponse; org::xtreemfs::interfaces::ObjectData object_data; _read( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_length(), object_data ); resp->set_object_data( object_data ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handletruncateRequest( truncateRequest& req ) { truncateResponse* resp = NULL; try { resp = new truncateResponse; org::xtreemfs::interfaces::OSDWriteResponse osd_write_response; _truncate( req.get_file_credentials(), req.get_file_id(), req.get_new_file_size(), osd_write_response ); resp->set_osd_write_response( osd_write_response ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleunlinkRequest( unlinkRequest& req ) { unlinkResponse* resp = NULL; try { resp = new unlinkResponse; _unlink( req.get_file_credentials(), req.get_file_id() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlewriteRequest( writeRequest& req ) { writeResponse* resp = NULL; try { resp = new writeResponse; org::xtreemfs::interfaces::OSDWriteResponse osd_write_response; _write( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_lease_timeout(), req.get_object_data(), osd_write_response ); resp->set_osd_write_response( osd_write_response ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_check_objectRequest( xtreemfs_check_objectRequest& req ) { xtreemfs_check_objectResponse* resp = NULL; try { resp = new xtreemfs_check_objectResponse; org::xtreemfs::interfaces::ObjectData _return_value = _xtreemfs_check_object( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_internal_get_gmaxRequest( xtreemfs_internal_get_gmaxRequest& req ) { xtreemfs_internal_get_gmaxResponse* resp = NULL; try { resp = new xtreemfs_internal_get_gmaxResponse; org::xtreemfs::interfaces::InternalGmax _return_value = _xtreemfs_internal_get_gmax( req.get_file_credentials(), req.get_file_id() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_internal_get_file_sizeRequest( xtreemfs_internal_get_file_sizeRequest& req ) { xtreemfs_internal_get_file_sizeResponse* resp = NULL; try { resp = new xtreemfs_internal_get_file_sizeResponse; uint64_t _return_value = _xtreemfs_internal_get_file_size( req.get_file_credentials(), req.get_file_id() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_internal_truncateRequest( xtreemfs_internal_truncateRequest& req ) { xtreemfs_internal_truncateResponse* resp = NULL; try { resp = new xtreemfs_internal_truncateResponse; org::xtreemfs::interfaces::OSDWriteResponse osd_write_response; _xtreemfs_internal_truncate( req.get_file_credentials(), req.get_file_id(), req.get_new_file_size(), osd_write_response ); resp->set_osd_write_response( osd_write_response ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_internal_read_localRequest( xtreemfs_internal_read_localRequest& req ) { xtreemfs_internal_read_localResponse* resp = NULL; try { resp = new xtreemfs_internal_read_localResponse; org::xtreemfs::interfaces::InternalReadLocalResponse _return_value = _xtreemfs_internal_read_local( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_length() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req ) { xtreemfs_shutdownResponse* resp = NULL; try { resp = new xtreemfs_shutdownResponse; _xtreemfs_shutdown(); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }

      virtual void _read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data ) { }
        virtual void _truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { }
        virtual void _unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { }
        virtual void _write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { }
        virtual org::xtreemfs::interfaces::ObjectData _xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version ) { return org::xtreemfs::interfaces::ObjectData(); }
        virtual org::xtreemfs::interfaces::InternalGmax _xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return org::xtreemfs::interfaces::InternalGmax(); }
        virtual uint64_t _xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return 0; }
        virtual void _xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse _xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) { return org::xtreemfs::interfaces::InternalReadLocalResponse(); }
        virtual void _xtreemfs_shutdown() { }
      };

      // Use this macro in an implementation class to get all of the prototypes for the operations in OSDInterface
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PROTOTYPES \
      virtual void _read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data );\
      virtual void _truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );\
      virtual void _unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual void _write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );\
      virtual org::xtreemfs::interfaces::ObjectData _xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version );\
      virtual org::xtreemfs::interfaces::InternalGmax _xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual uint64_t _xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual void _xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );\
      virtual org::xtreemfs::interfaces::InternalReadLocalResponse _xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length );\
      virtual void _xtreemfs_shutdown();

      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handlereadRequestRequest( readRequest& req );\
      virtual void handletruncateRequestRequest( truncateRequest& req );\
      virtual void handleunlinkRequestRequest( unlinkRequest& req );\
      virtual void handlewriteRequestRequest( writeRequest& req );\
      virtual void handlextreemfs_check_objectRequestRequest( xtreemfs_check_objectRequest& req );\
      virtual void handlextreemfs_internal_get_gmaxRequestRequest( xtreemfs_internal_get_gmaxRequest& req );\
      virtual void handlextreemfs_internal_get_file_sizeRequestRequest( xtreemfs_internal_get_file_sizeRequest& req );\
      virtual void handlextreemfs_internal_truncateRequestRequest( xtreemfs_internal_truncateRequest& req );\
      virtual void handlextreemfs_internal_read_localRequestRequest( xtreemfs_internal_read_localRequest& req );\
      virtual void handlextreemfs_shutdownRequestRequest( xtreemfs_shutdownRequest& req );

    };



  };



};

#endif
