#ifndef _89143847337_H
#define _89143847337_H

#include "yield/arch.h"
#include "mrc_osd_types.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      class ObjectData : public YIELD::Serializable
      {
      public:
        ObjectData() : zero_padding( 0 ), invalid_checksum_on_osd( false ), data( NULL ) { }
        ObjectData( const std::string& checksum, uint32_t zero_padding, bool invalid_checksum_on_osd, const YIELD::auto_SharedObject<YIELD::Serializable>& data ) : checksum( checksum ), zero_padding( zero_padding ), invalid_checksum_on_osd( invalid_checksum_on_osd ), data( data ) { }
        ObjectData( const char* checksum, size_t checksum_len, uint32_t zero_padding, bool invalid_checksum_on_osd, const YIELD::auto_SharedObject<YIELD::Serializable>& data ) : checksum( checksum, checksum_len ), zero_padding( zero_padding ), invalid_checksum_on_osd( invalid_checksum_on_osd ), data( data ) { }
        virtual ~ObjectData() { }
  
        void set_checksum( const std::string& checksum ) { set_checksum( checksum.c_str(), checksum.size() ); }
        void set_checksum( const char* checksum, size_t checksum_len = 0 ) { this->checksum.assign( checksum, ( checksum_len != 0 ) ? checksum_len : std::strlen( checksum ) ); }
        const std::string& get_checksum() const { return checksum; }
        void set_zero_padding( uint32_t zero_padding ) { this->zero_padding = zero_padding; }
        uint32_t get_zero_padding() const { return zero_padding; }
        void set_invalid_checksum_on_osd( bool invalid_checksum_on_osd ) { this->invalid_checksum_on_osd = invalid_checksum_on_osd; }
        bool get_invalid_checksum_on_osd() const { return invalid_checksum_on_osd; }
        void set_data( YIELD::Serializable* data ) { this->data.reset( data ); }
        void set_data( YIELD::auto_SharedObject<YIELD::Serializable>& data ) { this->data = data; }
        YIELD::auto_SharedObject<Serializable> get_data() const { return YIELD::SharedObject::incRef( data.get() ); }
  
        bool operator==( const ObjectData& other ) const { return checksum == other.checksum && zero_padding == other.zero_padding && invalid_checksum_on_osd == other.invalid_checksum_on_osd && data.get() == other.data.get(); }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::ObjectData", 1049653481UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "checksum" ), checksum ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "zero_padding" ), zero_padding ); output_stream.writeBool( YIELD::StructuredStream::Declaration( "invalid_checksum_on_osd" ), invalid_checksum_on_osd ); if ( data.get() ) output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "data" ), *data.get() ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "checksum" ), checksum ); zero_padding = input_stream.readUint32( YIELD::StructuredStream::Declaration( "zero_padding" ) ); invalid_checksum_on_osd = input_stream.readBool( YIELD::StructuredStream::Declaration( "invalid_checksum_on_osd" ) ); data = input_stream.readSerializable( YIELD::StructuredStream::Declaration( "data" ) ); }
  
      protected:
        std::string checksum;
        uint32_t zero_padding;
        bool invalid_checksum_on_osd;
        YIELD::auto_SharedObject<YIELD::Serializable> data;
      };
  
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
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::InternalGmax", 3838673994UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "epoch" ), epoch ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "last_object_id" ), last_object_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "file_size" ), file_size ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { epoch = input_stream.readUint64( YIELD::StructuredStream::Declaration( "epoch" ) ); last_object_id = input_stream.readUint64( YIELD::StructuredStream::Declaration( "last_object_id" ) ); file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "file_size" ) ); }
  
      protected:
        uint64_t epoch;
        uint64_t last_object_id;
        uint64_t file_size;
      };
  
      class InternalReadLocalResponse : public YIELD::Serializable
      {
      public:
        InternalReadLocalResponse() : zero_padding( 0 ) { }
        InternalReadLocalResponse( const org::xtreemfs::interfaces::InternalReadLocalResponse::NewFileSize& file_size, uint32_t zero_padding, const org::xtreemfs::interfaces::InternalReadLocalResponse::ObjectData& data ) : file_size( file_size ), zero_padding( zero_padding ), data( data ) { }
        virtual ~InternalReadLocalResponse() { }
  
        void set_file_size( const org::xtreemfs::interfaces::InternalReadLocalResponse::NewFileSize&  file_size ) { this->file_size = file_size; }
        const org::xtreemfs::interfaces::InternalReadLocalResponse::NewFileSize& get_file_size() const { return file_size; }
        void set_zero_padding( uint32_t zero_padding ) { this->zero_padding = zero_padding; }
        uint32_t get_zero_padding() const { return zero_padding; }
        void set_data( const org::xtreemfs::interfaces::InternalReadLocalResponse::ObjectData&  data ) { this->data = data; }
        const org::xtreemfs::interfaces::InternalReadLocalResponse::ObjectData& get_data() const { return data; }
  
        bool operator==( const InternalReadLocalResponse& other ) const { return file_size == other.file_size && zero_padding == other.zero_padding && data == other.data; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::InternalReadLocalResponse", 375306877UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalReadLocalResponse::NewFileSize", "file_size" ), file_size ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "zero_padding" ), zero_padding ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalReadLocalResponse::ObjectData", "data" ), data ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalReadLocalResponse::NewFileSize", "file_size" ), &file_size ); zero_padding = input_stream.readUint32( YIELD::StructuredStream::Declaration( "zero_padding" ) ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalReadLocalResponse::ObjectData", "data" ), &data ); }
  
      protected:
        org::xtreemfs::interfaces::InternalReadLocalResponse::NewFileSize file_size;
        uint32_t zero_padding;
        org::xtreemfs::interfaces::InternalReadLocalResponse::ObjectData data;
      };
  
  
  
      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PARENT_CLASS ORG_XTREEMFS_PARENT_CLASS
      #elif defined( ORG_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PARENT_CLASS ORG_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PARENT_CLASS YIELD::EventHandler
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
  
  
  
      class OSDInterface : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PARENT_CLASS
      {
      public:
        OSDInterface() { }
        virtual ~OSDInterface() { }
  
      virtual org::xtreemfs::interfaces::ObjectData read( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length ) { return read( file_id, credentials, object_number, object_version, offset, length, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::ObjectData read( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, const char* send_target ) { return read( file_id, credentials, object_number, object_version, offset, length, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::ObjectData read( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, YIELD::timeout_ns_t send_timeout_ns ) { return read( file_id, credentials, object_number, object_version, offset, length, NULL, send_timeout_ns ); }
        virtual org::xtreemfs::interfaces::ObjectData read( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { readSyncRequest* __req = new readSyncRequest( file_id, credentials, object_number, object_version, offset, length, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); readResponse& __resp = ( readResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); org::xtreemfs::interfaces::ObjectData _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_response ) { truncate( file_id, credentials, new_file_size, osd_response, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_response, const char* send_target ) { truncate( file_id, credentials, new_file_size, osd_response, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_response, YIELD::timeout_ns_t send_timeout_ns ) { truncate( file_id, credentials, new_file_size, osd_response, NULL, send_timeout_ns ); }
        virtual void truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_response, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { truncateSyncRequest* __req = new truncateSyncRequest( file_id, credentials, new_file_size, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); truncateResponse& __resp = ( truncateResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); osd_response = __resp.get_osd_response(); YIELD::SharedObject::decRef( __resp ); }
        virtual void unlink( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials ) { unlink( file_id, credentials, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void unlink( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, const char* send_target ) { unlink( file_id, credentials, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void unlink( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t send_timeout_ns ) { unlink( file_id, credentials, NULL, send_timeout_ns ); }
        virtual void unlink( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { unlinkSyncRequest* __req = new unlinkSyncRequest( file_id, credentials, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); unlinkResponse& __resp = ( unlinkResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void write( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, org::xtreemfs::interfaces::OSDWriteResponse& osd_response ) { write( file_id, credentials, object_number, object_version, offset, lease_timeout, data, osd_response, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void write( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, org::xtreemfs::interfaces::OSDWriteResponse& osd_response, const char* send_target ) { write( file_id, credentials, object_number, object_version, offset, lease_timeout, data, osd_response, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void write( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, org::xtreemfs::interfaces::OSDWriteResponse& osd_response, YIELD::timeout_ns_t send_timeout_ns ) { write( file_id, credentials, object_number, object_version, offset, lease_timeout, data, osd_response, NULL, send_timeout_ns ); }
        virtual void write( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, org::xtreemfs::interfaces::OSDWriteResponse& osd_response, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { writeSyncRequest* __req = new writeSyncRequest( file_id, credentials, object_number, object_version, offset, lease_timeout, data, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); writeResponse& __resp = ( writeResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); osd_response = __resp.get_osd_response(); YIELD::SharedObject::decRef( __resp ); }
        virtual void keep_file_open( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials ) { keep_file_open( file_id, credentials, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void keep_file_open( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, const char* send_target ) { keep_file_open( file_id, credentials, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void keep_file_open( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t send_timeout_ns ) { keep_file_open( file_id, credentials, NULL, send_timeout_ns ); }
        virtual void keep_file_open( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { keep_file_openSyncRequest* __req = new keep_file_openSyncRequest( file_id, credentials, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); keep_file_openResponse& __resp = ( keep_file_openResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual org::xtreemfs::interfaces::InternalGmax internal_get_gmax( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials ) { return internal_get_gmax( file_id, credentials, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalGmax internal_get_gmax( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, const char* send_target ) { return internal_get_gmax( file_id, credentials, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalGmax internal_get_gmax( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t send_timeout_ns ) { return internal_get_gmax( file_id, credentials, NULL, send_timeout_ns ); }
        virtual org::xtreemfs::interfaces::InternalGmax internal_get_gmax( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { internal_get_gmaxSyncRequest* __req = new internal_get_gmaxSyncRequest( file_id, credentials, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); internal_get_gmaxResponse& __resp = ( internal_get_gmaxResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); org::xtreemfs::interfaces::InternalGmax _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void internal_truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size ) { internal_truncate( file_id, credentials, new_file_size, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void internal_truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, const char* send_target ) { internal_truncate( file_id, credentials, new_file_size, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void internal_truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, YIELD::timeout_ns_t send_timeout_ns ) { internal_truncate( file_id, credentials, new_file_size, NULL, send_timeout_ns ); }
        virtual void internal_truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { internal_truncateSyncRequest* __req = new internal_truncateSyncRequest( file_id, credentials, new_file_size, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); internal_truncateResponse& __resp = ( internal_truncateResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse internal_read_local( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) { return internal_read_local( file_id, credentials, object_number, object_version, offset, length, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse internal_read_local( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, const char* send_target ) { return internal_read_local( file_id, credentials, object_number, object_version, offset, length, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse internal_read_local( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, YIELD::timeout_ns_t send_timeout_ns ) { return internal_read_local( file_id, credentials, object_number, object_version, offset, length, NULL, send_timeout_ns ); }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse internal_read_local( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { internal_read_localSyncRequest* __req = new internal_read_localSyncRequest( file_id, credentials, object_number, object_version, offset, length, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); internal_read_localResponse& __resp = ( internal_read_localResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); org::xtreemfs::interfaces::InternalReadLocalResponse _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual org::xtreemfs::interfaces::ObjectData check_object( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version ) { return check_object( file_id, credentials, object_number, object_version, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::ObjectData check_object( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, const char* send_target ) { return check_object( file_id, credentials, object_number, object_version, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::ObjectData check_object( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, YIELD::timeout_ns_t send_timeout_ns ) { return check_object( file_id, credentials, object_number, object_version, NULL, send_timeout_ns ); }
        virtual org::xtreemfs::interfaces::ObjectData check_object( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { check_objectSyncRequest* __req = new check_objectSyncRequest( file_id, credentials, object_number, object_version, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); check_objectResponse& __resp = ( check_objectResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); org::xtreemfs::interfaces::ObjectData _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void admin_shutdown( const std::string& password ) { admin_shutdown( password, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_shutdown( const std::string& password, const char* send_target ) { admin_shutdown( password, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_shutdown( const std::string& password, YIELD::timeout_ns_t send_timeout_ns ) { admin_shutdown( password, NULL, send_timeout_ns ); }
        virtual void admin_shutdown( const std::string& password, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { admin_shutdownSyncRequest* __req = new admin_shutdownSyncRequest( password, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); admin_shutdownResponse& __resp = ( admin_shutdownResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }  // Request/response pair Event type definitions for the operations in OSDInterface
  
      class readResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        readResponse() { }
        readResponse( const org::xtreemfs::interfaces::ObjectData& _return_value ) : _return_value( _return_value ) { }
        virtual ~readResponse() { }
  
        void set__return_value( const org::xtreemfs::interfaces::ObjectData&  _return_value ) { this->_return_value = _return_value; }
        const org::xtreemfs::interfaces::ObjectData& get__return_value() const { return _return_value; }
  
        bool operator==( const readResponse& other ) const { return _return_value == other._return_value; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::readResponse", 4289876024UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "_return_value" ), _return_value ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "_return_value" ), &_return_value ); }
  
      protected:
        org::xtreemfs::interfaces::ObjectData _return_value;
      };
  
      class readRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        readRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), object_number( 0 ), object_version( 0 ), offset( 0 ), length( 0 ) { }
        readRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ), credentials( credentials ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ) { }
        readRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ), credentials( credentials ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ) { }
        virtual ~readRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        uint64_t get_object_number() const { return object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
        uint64_t get_object_version() const { return object_version; }
        void set_offset( uint32_t offset ) { this->offset = offset; }
        uint32_t get_offset() const { return offset; }
        void set_length( uint32_t length ) { this->length = length; }
        uint32_t get_length() const { return length; }
  
        bool operator==( const readRequest& other ) const { return file_id == other.file_id && credentials == other.credentials && object_number == other.object_number && object_version == other.object_version && offset == other.offset && length == other.length; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::readRequest", 4214126648UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "offset" ), offset ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "length" ), length ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); offset = input_stream.readUint32( YIELD::StructuredStream::Declaration( "offset" ) ); length = input_stream.readUint32( YIELD::StructuredStream::Declaration( "length" ) ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 1; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 4289876024UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::readResponse; }
  
  
      protected:
        std::string file_id;
        org::xtreemfs::interfaces::FileCredentials credentials;
        uint64_t object_number;
        uint64_t object_version;
        uint32_t offset;
        uint32_t length;
      };
  
      class readSyncRequest : public readRequest
      {
      public:
        readSyncRequest() : readRequest( std::string(), org::xtreemfs::interfaces::FileCredentials(), 0, 0, 0, 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        readSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : readRequest( file_id, credentials, object_number, object_version, offset, length, response_timeout_ns ) { }
        readSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : readRequest( file_id, file_id_len, credentials, object_number, object_version, offset, length, response_timeout_ns ) { }
        virtual ~readSyncRequest() { }
  
        bool operator==( const readSyncRequest& other ) const { return true; }
  
  
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
        truncateResponse( const org::xtreemfs::interfaces::OSDWriteResponse& osd_response ) : osd_response( osd_response ) { }
        virtual ~truncateResponse() { }
  
        void set_osd_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_response ) { this->osd_response = osd_response; }
        const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_response() const { return osd_response; }
  
        bool operator==( const truncateResponse& other ) const { return osd_response == other.osd_response; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::truncateResponse", 233138659UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_response" ), osd_response ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_response" ), &osd_response ); }
  
      protected:
        org::xtreemfs::interfaces::OSDWriteResponse osd_response;
      };
  
      class truncateRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        truncateRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), new_file_size( 0 ) { }
        truncateRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ), credentials( credentials ), new_file_size( new_file_size ) { }
        truncateRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ), credentials( credentials ), new_file_size( new_file_size ) { }
        virtual ~truncateRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
        void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }
        uint64_t get_new_file_size() const { return new_file_size; }
  
        bool operator==( const truncateRequest& other ) const { return file_id == other.file_id && credentials == other.credentials && new_file_size == other.new_file_size; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::truncateRequest", 3445878689UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "new_file_size" ), new_file_size ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); new_file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "new_file_size" ) ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 2; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 233138659UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::truncateResponse; }
  
  
      protected:
        std::string file_id;
        org::xtreemfs::interfaces::FileCredentials credentials;
        uint64_t new_file_size;
      };
  
      class truncateSyncRequest : public truncateRequest
      {
      public:
        truncateSyncRequest() : truncateRequest( std::string(), org::xtreemfs::interfaces::FileCredentials(), 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        truncateSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : truncateRequest( file_id, credentials, new_file_size, response_timeout_ns ) { }
        truncateSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : truncateRequest( file_id, file_id_len, credentials, new_file_size, response_timeout_ns ) { }
        virtual ~truncateSyncRequest() { }
  
        bool operator==( const truncateSyncRequest& other ) const { return true; }
  
  
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
  
        bool operator==( const unlinkResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::unlinkResponse", 1159409605UL )
  
      };
  
      class unlinkRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        unlinkRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        unlinkRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ), credentials( credentials ) { }
        unlinkRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ), credentials( credentials ) { }
        virtual ~unlinkRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
  
        bool operator==( const unlinkRequest& other ) const { return file_id == other.file_id && credentials == other.credentials; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::unlinkRequest", 2625011690UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 3; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1159409605UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::unlinkResponse; }
  
  
      protected:
        std::string file_id;
        org::xtreemfs::interfaces::FileCredentials credentials;
      };
  
      class unlinkSyncRequest : public unlinkRequest
      {
      public:
        unlinkSyncRequest() : unlinkRequest( std::string(), org::xtreemfs::interfaces::FileCredentials(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        unlinkSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : unlinkRequest( file_id, credentials, response_timeout_ns ) { }
        unlinkSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : unlinkRequest( file_id, file_id_len, credentials, response_timeout_ns ) { }
        virtual ~unlinkSyncRequest() { }
  
        bool operator==( const unlinkSyncRequest& other ) const { return true; }
  
  
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
        writeResponse( const org::xtreemfs::interfaces::OSDWriteResponse& osd_response ) : osd_response( osd_response ) { }
        virtual ~writeResponse() { }
  
        void set_osd_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_response ) { this->osd_response = osd_response; }
        const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_response() const { return osd_response; }
  
        bool operator==( const writeResponse& other ) const { return osd_response == other.osd_response; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::writeResponse", 3887614948UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_response" ), osd_response ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_response" ), &osd_response ); }
  
      protected:
        org::xtreemfs::interfaces::OSDWriteResponse osd_response;
      };
  
      class writeRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        writeRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), object_number( 0 ), object_version( 0 ), offset( 0 ), lease_timeout( 0 ) { }
        writeRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ), credentials( credentials ), object_number( object_number ), object_version( object_version ), offset( offset ), lease_timeout( lease_timeout ), data( data ) { }
        writeRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ), credentials( credentials ), object_number( object_number ), object_version( object_version ), offset( offset ), lease_timeout( lease_timeout ), data( data ) { }
        virtual ~writeRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        uint64_t get_object_number() const { return object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
        uint64_t get_object_version() const { return object_version; }
        void set_offset( uint32_t offset ) { this->offset = offset; }
        uint32_t get_offset() const { return offset; }
        void set_lease_timeout( uint64_t lease_timeout ) { this->lease_timeout = lease_timeout; }
        uint64_t get_lease_timeout() const { return lease_timeout; }
        void set_data( const org::xtreemfs::interfaces::ObjectData&  data ) { this->data = data; }
        const org::xtreemfs::interfaces::ObjectData& get_data() const { return data; }
  
        bool operator==( const writeRequest& other ) const { return file_id == other.file_id && credentials == other.credentials && object_number == other.object_number && object_version == other.object_version && offset == other.offset && lease_timeout == other.lease_timeout && data == other.data; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::writeRequest", 3616508705UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "offset" ), offset ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "lease_timeout" ), lease_timeout ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "data" ), data ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); offset = input_stream.readUint32( YIELD::StructuredStream::Declaration( "offset" ) ); lease_timeout = input_stream.readUint64( YIELD::StructuredStream::Declaration( "lease_timeout" ) ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "data" ), &data ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 4; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3887614948UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::writeResponse; }
  
  
      protected:
        std::string file_id;
        org::xtreemfs::interfaces::FileCredentials credentials;
        uint64_t object_number;
        uint64_t object_version;
        uint32_t offset;
        uint64_t lease_timeout;
        org::xtreemfs::interfaces::ObjectData data;
      };
  
      class writeSyncRequest : public writeRequest
      {
      public:
        writeSyncRequest() : writeRequest( std::string(), org::xtreemfs::interfaces::FileCredentials(), 0, 0, 0, 0, org::xtreemfs::interfaces::ObjectData(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        writeSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : writeRequest( file_id, credentials, object_number, object_version, offset, lease_timeout, data, response_timeout_ns ) { }
        writeSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : writeRequest( file_id, file_id_len, credentials, object_number, object_version, offset, lease_timeout, data, response_timeout_ns ) { }
        virtual ~writeSyncRequest() { }
  
        bool operator==( const writeSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::writeResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class keep_file_openResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        keep_file_openResponse() { }
        virtual ~keep_file_openResponse() { }
  
        bool operator==( const keep_file_openResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::keep_file_openResponse", 261263724UL )
  
      };
  
      class keep_file_openRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        keep_file_openRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        keep_file_openRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ), credentials( credentials ) { }
        keep_file_openRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ), credentials( credentials ) { }
        virtual ~keep_file_openRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
  
        bool operator==( const keep_file_openRequest& other ) const { return file_id == other.file_id && credentials == other.credentials; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::keep_file_openRequest", 3571631653UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 5; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 261263724UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::keep_file_openResponse; }
  
  
      protected:
        std::string file_id;
        org::xtreemfs::interfaces::FileCredentials credentials;
      };
  
      class keep_file_openSyncRequest : public keep_file_openRequest
      {
      public:
        keep_file_openSyncRequest() : keep_file_openRequest( std::string(), org::xtreemfs::interfaces::FileCredentials(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        keep_file_openSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : keep_file_openRequest( file_id, credentials, response_timeout_ns ) { }
        keep_file_openSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : keep_file_openRequest( file_id, file_id_len, credentials, response_timeout_ns ) { }
        virtual ~keep_file_openSyncRequest() { }
  
        bool operator==( const keep_file_openSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::keep_file_openResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class internal_get_gmaxResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        internal_get_gmaxResponse() { }
        internal_get_gmaxResponse( const org::xtreemfs::interfaces::InternalGmax& _return_value ) : _return_value( _return_value ) { }
        virtual ~internal_get_gmaxResponse() { }
  
        void set__return_value( const org::xtreemfs::interfaces::InternalGmax&  _return_value ) { this->_return_value = _return_value; }
        const org::xtreemfs::interfaces::InternalGmax& get__return_value() const { return _return_value; }
  
        bool operator==( const internal_get_gmaxResponse& other ) const { return _return_value == other._return_value; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::internal_get_gmaxResponse", 1454663115UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalGmax", "_return_value" ), _return_value ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalGmax", "_return_value" ), &_return_value ); }
  
      protected:
        org::xtreemfs::interfaces::InternalGmax _return_value;
      };
  
      class internal_get_gmaxRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        internal_get_gmaxRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        internal_get_gmaxRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ), credentials( credentials ) { }
        internal_get_gmaxRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ), credentials( credentials ) { }
        virtual ~internal_get_gmaxRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
  
        bool operator==( const internal_get_gmaxRequest& other ) const { return file_id == other.file_id && credentials == other.credentials; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::internal_get_gmaxRequest", 593348104UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 100; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1454663115UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::internal_get_gmaxResponse; }
  
  
      protected:
        std::string file_id;
        org::xtreemfs::interfaces::FileCredentials credentials;
      };
  
      class internal_get_gmaxSyncRequest : public internal_get_gmaxRequest
      {
      public:
        internal_get_gmaxSyncRequest() : internal_get_gmaxRequest( std::string(), org::xtreemfs::interfaces::FileCredentials(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        internal_get_gmaxSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : internal_get_gmaxRequest( file_id, credentials, response_timeout_ns ) { }
        internal_get_gmaxSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : internal_get_gmaxRequest( file_id, file_id_len, credentials, response_timeout_ns ) { }
        virtual ~internal_get_gmaxSyncRequest() { }
  
        bool operator==( const internal_get_gmaxSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::internal_get_gmaxResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class internal_truncateResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        internal_truncateResponse() { }
        virtual ~internal_truncateResponse() { }
  
        bool operator==( const internal_truncateResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::internal_truncateResponse", 1835140915UL )
  
      };
  
      class internal_truncateRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        internal_truncateRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), new_file_size( 0 ) { }
        internal_truncateRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ), credentials( credentials ), new_file_size( new_file_size ) { }
        internal_truncateRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ), credentials( credentials ), new_file_size( new_file_size ) { }
        virtual ~internal_truncateRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
        void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }
        uint64_t get_new_file_size() const { return new_file_size; }
  
        bool operator==( const internal_truncateRequest& other ) const { return file_id == other.file_id && credentials == other.credentials && new_file_size == other.new_file_size; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::internal_truncateRequest", 2365800131UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "new_file_size" ), new_file_size ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); new_file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "new_file_size" ) ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 101; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1835140915UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::internal_truncateResponse; }
  
  
      protected:
        std::string file_id;
        org::xtreemfs::interfaces::FileCredentials credentials;
        uint64_t new_file_size;
      };
  
      class internal_truncateSyncRequest : public internal_truncateRequest
      {
      public:
        internal_truncateSyncRequest() : internal_truncateRequest( std::string(), org::xtreemfs::interfaces::FileCredentials(), 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        internal_truncateSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : internal_truncateRequest( file_id, credentials, new_file_size, response_timeout_ns ) { }
        internal_truncateSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : internal_truncateRequest( file_id, file_id_len, credentials, new_file_size, response_timeout_ns ) { }
        virtual ~internal_truncateSyncRequest() { }
  
        bool operator==( const internal_truncateSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::internal_truncateResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class internal_read_localResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        internal_read_localResponse() { }
        internal_read_localResponse( const org::xtreemfs::interfaces::InternalReadLocalResponse& _return_value ) : _return_value( _return_value ) { }
        virtual ~internal_read_localResponse() { }
  
        void set__return_value( const org::xtreemfs::interfaces::InternalReadLocalResponse&  _return_value ) { this->_return_value = _return_value; }
        const org::xtreemfs::interfaces::InternalReadLocalResponse& get__return_value() const { return _return_value; }
  
        bool operator==( const internal_read_localResponse& other ) const { return _return_value == other._return_value; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::internal_read_localResponse", 3564028294UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalReadLocalResponse", "_return_value" ), _return_value ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::InternalReadLocalResponse", "_return_value" ), &_return_value ); }
  
      protected:
        org::xtreemfs::interfaces::InternalReadLocalResponse _return_value;
      };
  
      class internal_read_localRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        internal_read_localRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), object_number( 0 ), object_version( 0 ), offset( 0 ), length( 0 ) { }
        internal_read_localRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ), credentials( credentials ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ) { }
        internal_read_localRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ), credentials( credentials ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ) { }
        virtual ~internal_read_localRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        uint64_t get_object_number() const { return object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
        uint64_t get_object_version() const { return object_version; }
        void set_offset( uint64_t offset ) { this->offset = offset; }
        uint64_t get_offset() const { return offset; }
        void set_length( uint64_t length ) { this->length = length; }
        uint64_t get_length() const { return length; }
  
        bool operator==( const internal_read_localRequest& other ) const { return file_id == other.file_id && credentials == other.credentials && object_number == other.object_number && object_version == other.object_version && offset == other.offset && length == other.length; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::internal_read_localRequest", 908569103UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "offset" ), offset ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "length" ), length ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); offset = input_stream.readUint64( YIELD::StructuredStream::Declaration( "offset" ) ); length = input_stream.readUint64( YIELD::StructuredStream::Declaration( "length" ) ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 102; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3564028294UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::internal_read_localResponse; }
  
  
      protected:
        std::string file_id;
        org::xtreemfs::interfaces::FileCredentials credentials;
        uint64_t object_number;
        uint64_t object_version;
        uint64_t offset;
        uint64_t length;
      };
  
      class internal_read_localSyncRequest : public internal_read_localRequest
      {
      public:
        internal_read_localSyncRequest() : internal_read_localRequest( std::string(), org::xtreemfs::interfaces::FileCredentials(), 0, 0, 0, 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        internal_read_localSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : internal_read_localRequest( file_id, credentials, object_number, object_version, offset, length, response_timeout_ns ) { }
        internal_read_localSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : internal_read_localRequest( file_id, file_id_len, credentials, object_number, object_version, offset, length, response_timeout_ns ) { }
        virtual ~internal_read_localSyncRequest() { }
  
        bool operator==( const internal_read_localSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::internal_read_localResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class check_objectResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        check_objectResponse() { }
        check_objectResponse( const org::xtreemfs::interfaces::ObjectData& _return_value ) : _return_value( _return_value ) { }
        virtual ~check_objectResponse() { }
  
        void set__return_value( const org::xtreemfs::interfaces::ObjectData&  _return_value ) { this->_return_value = _return_value; }
        const org::xtreemfs::interfaces::ObjectData& get__return_value() const { return _return_value; }
  
        bool operator==( const check_objectResponse& other ) const { return _return_value == other._return_value; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::check_objectResponse", 353874046UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "_return_value" ), _return_value ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ObjectData", "_return_value" ), &_return_value ); }
  
      protected:
        org::xtreemfs::interfaces::ObjectData _return_value;
      };
  
      class check_objectRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        check_objectRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), object_number( 0 ), object_version( 0 ) { }
        check_objectRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ), credentials( credentials ), object_number( object_number ), object_version( object_version ) { }
        check_objectRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ), credentials( credentials ), object_number( object_number ), object_version( object_version ) { }
        virtual ~check_objectRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        uint64_t get_object_number() const { return object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
        uint64_t get_object_version() const { return object_version; }
  
        bool operator==( const check_objectRequest& other ) const { return file_id == other.file_id && credentials == other.credentials && object_number == other.object_number && object_version == other.object_version; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::check_objectRequest", 2344559360UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 103; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 353874046UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::check_objectResponse; }
  
  
      protected:
        std::string file_id;
        org::xtreemfs::interfaces::FileCredentials credentials;
        uint64_t object_number;
        uint64_t object_version;
      };
  
      class check_objectSyncRequest : public check_objectRequest
      {
      public:
        check_objectSyncRequest() : check_objectRequest( std::string(), org::xtreemfs::interfaces::FileCredentials(), 0, 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        check_objectSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : check_objectRequest( file_id, credentials, object_number, object_version, response_timeout_ns ) { }
        check_objectSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : check_objectRequest( file_id, file_id_len, credentials, object_number, object_version, response_timeout_ns ) { }
        virtual ~check_objectSyncRequest() { }
  
        bool operator==( const check_objectSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::check_objectResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class admin_shutdownResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        admin_shutdownResponse() { }
        virtual ~admin_shutdownResponse() { }
  
        bool operator==( const admin_shutdownResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::OSDInterface::admin_shutdownResponse", 6822557UL )
  
      };
  
      class admin_shutdownRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        admin_shutdownRequest() : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_shutdownRequest( const std::string& password, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password ) { }
        admin_shutdownRequest( const char* password, size_t password_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password, password_len ) { }
        virtual ~admin_shutdownRequest() { }
  
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }
  
        bool operator==( const admin_shutdownRequest& other ) const { return password == other.password; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::OSDInterface::admin_shutdownRequest", 2344589109UL )
  
  
        // Serializable
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); }
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceUID() const { return 0; }
        virtual uint32_t getOperationUID() const { return 50; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 6822557UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::OSDInterface::admin_shutdownResponse; }
  
  
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
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::OSDInterface::admin_shutdownResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
  
  
        void registerSerializableFactories( YIELD::SerializableFactories& serializable_factories )
        {
          serializable_factories.registerSerializableFactory( 4289876024UL, new YIELD::SerializableFactoryImpl<readResponse> ); serializable_factories.registerSerializableFactory( 4214126648UL, new YIELD::SerializableFactoryImpl<readRequest> ); serializable_factories.registerSerializableFactory( 3698651647UL, new YIELD::SerializableFactoryImpl<readSyncRequest> );
          serializable_factories.registerSerializableFactory( 233138659UL, new YIELD::SerializableFactoryImpl<truncateResponse> ); serializable_factories.registerSerializableFactory( 3445878689UL, new YIELD::SerializableFactoryImpl<truncateRequest> ); serializable_factories.registerSerializableFactory( 2997414208UL, new YIELD::SerializableFactoryImpl<truncateSyncRequest> );
          serializable_factories.registerSerializableFactory( 1159409605UL, new YIELD::SerializableFactoryImpl<unlinkResponse> ); serializable_factories.registerSerializableFactory( 2625011690UL, new YIELD::SerializableFactoryImpl<unlinkRequest> ); serializable_factories.registerSerializableFactory( 2654062122UL, new YIELD::SerializableFactoryImpl<unlinkSyncRequest> );
          serializable_factories.registerSerializableFactory( 3887614948UL, new YIELD::SerializableFactoryImpl<writeResponse> ); serializable_factories.registerSerializableFactory( 3616508705UL, new YIELD::SerializableFactoryImpl<writeRequest> ); serializable_factories.registerSerializableFactory( 2126281761UL, new YIELD::SerializableFactoryImpl<writeSyncRequest> );
          serializable_factories.registerSerializableFactory( 261263724UL, new YIELD::SerializableFactoryImpl<keep_file_openResponse> ); serializable_factories.registerSerializableFactory( 3571631653UL, new YIELD::SerializableFactoryImpl<keep_file_openRequest> ); serializable_factories.registerSerializableFactory( 1491481718UL, new YIELD::SerializableFactoryImpl<keep_file_openSyncRequest> );
          serializable_factories.registerSerializableFactory( 1454663115UL, new YIELD::SerializableFactoryImpl<internal_get_gmaxResponse> ); serializable_factories.registerSerializableFactory( 593348104UL, new YIELD::SerializableFactoryImpl<internal_get_gmaxRequest> ); serializable_factories.registerSerializableFactory( 3911126955UL, new YIELD::SerializableFactoryImpl<internal_get_gmaxSyncRequest> );
          serializable_factories.registerSerializableFactory( 1835140915UL, new YIELD::SerializableFactoryImpl<internal_truncateResponse> ); serializable_factories.registerSerializableFactory( 2365800131UL, new YIELD::SerializableFactoryImpl<internal_truncateRequest> ); serializable_factories.registerSerializableFactory( 32798225UL, new YIELD::SerializableFactoryImpl<internal_truncateSyncRequest> );
          serializable_factories.registerSerializableFactory( 3564028294UL, new YIELD::SerializableFactoryImpl<internal_read_localResponse> ); serializable_factories.registerSerializableFactory( 908569103UL, new YIELD::SerializableFactoryImpl<internal_read_localRequest> ); serializable_factories.registerSerializableFactory( 564821627UL, new YIELD::SerializableFactoryImpl<internal_read_localSyncRequest> );
          serializable_factories.registerSerializableFactory( 353874046UL, new YIELD::SerializableFactoryImpl<check_objectResponse> ); serializable_factories.registerSerializableFactory( 2344559360UL, new YIELD::SerializableFactoryImpl<check_objectRequest> ); serializable_factories.registerSerializableFactory( 2248268279UL, new YIELD::SerializableFactoryImpl<check_objectSyncRequest> );
          serializable_factories.registerSerializableFactory( 6822557UL, new YIELD::SerializableFactoryImpl<admin_shutdownResponse> ); serializable_factories.registerSerializableFactory( 2344589109UL, new YIELD::SerializableFactoryImpl<admin_shutdownRequest> ); serializable_factories.registerSerializableFactory( 1570224646UL, new YIELD::SerializableFactoryImpl<admin_shutdownSyncRequest> );
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
              case 3571631653UL: handlekeep_file_openRequest( static_cast<keep_file_openRequest&>( ev ) ); return;
              case 593348104UL: handleinternal_get_gmaxRequest( static_cast<internal_get_gmaxRequest&>( ev ) ); return;
              case 2365800131UL: handleinternal_truncateRequest( static_cast<internal_truncateRequest&>( ev ) ); return;
              case 908569103UL: handleinternal_read_localRequest( static_cast<internal_read_localRequest&>( ev ) ); return;
              case 2344559360UL: handlecheck_objectRequest( static_cast<check_objectRequest&>( ev ) ); return;
              case 2344589109UL: handleadmin_shutdownRequest( static_cast<admin_shutdownRequest&>( ev ) ); return;
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
          virtual void handlereadRequest( readRequest& req ) { readResponse* resp = NULL; try { resp = new readResponse; org::xtreemfs::interfaces::ObjectData _return_value = _read( req.get_file_id(), req.get_credentials(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_length() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handletruncateRequest( truncateRequest& req ) { truncateResponse* resp = NULL; try { resp = new truncateResponse; org::xtreemfs::interfaces::OSDWriteResponse osd_response; _truncate( req.get_file_id(), req.get_credentials(), req.get_new_file_size(), osd_response ); resp->set_osd_response( osd_response ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleunlinkRequest( unlinkRequest& req ) { unlinkResponse* resp = NULL; try { resp = new unlinkResponse; _unlink( req.get_file_id(), req.get_credentials() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlewriteRequest( writeRequest& req ) { writeResponse* resp = NULL; try { resp = new writeResponse; org::xtreemfs::interfaces::OSDWriteResponse osd_response; _write( req.get_file_id(), req.get_credentials(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_lease_timeout(), req.get_data(), osd_response ); resp->set_osd_response( osd_response ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlekeep_file_openRequest( keep_file_openRequest& req ) { keep_file_openResponse* resp = NULL; try { resp = new keep_file_openResponse; _keep_file_open( req.get_file_id(), req.get_credentials() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleinternal_get_gmaxRequest( internal_get_gmaxRequest& req ) { internal_get_gmaxResponse* resp = NULL; try { resp = new internal_get_gmaxResponse; org::xtreemfs::interfaces::InternalGmax _return_value = _internal_get_gmax( req.get_file_id(), req.get_credentials() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleinternal_truncateRequest( internal_truncateRequest& req ) { internal_truncateResponse* resp = NULL; try { resp = new internal_truncateResponse; _internal_truncate( req.get_file_id(), req.get_credentials(), req.get_new_file_size() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleinternal_read_localRequest( internal_read_localRequest& req ) { internal_read_localResponse* resp = NULL; try { resp = new internal_read_localResponse; org::xtreemfs::interfaces::InternalReadLocalResponse _return_value = _internal_read_local( req.get_file_id(), req.get_credentials(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_length() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlecheck_objectRequest( check_objectRequest& req ) { check_objectResponse* resp = NULL; try { resp = new check_objectResponse; org::xtreemfs::interfaces::ObjectData _return_value = _check_object( req.get_file_id(), req.get_credentials(), req.get_object_number(), req.get_object_version() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleadmin_shutdownRequest( admin_shutdownRequest& req ) { admin_shutdownResponse* resp = NULL; try { resp = new admin_shutdownResponse; _admin_shutdown( req.get_password() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
  
      virtual org::xtreemfs::interfaces::ObjectData _read( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length ) { return org::xtreemfs::interfaces::ObjectData(); }
        virtual void _truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_response ) { }
        virtual void _unlink( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials ) { }
        virtual void _write( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, org::xtreemfs::interfaces::OSDWriteResponse& osd_response ) { }
        virtual void _keep_file_open( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials ) { }
        virtual org::xtreemfs::interfaces::InternalGmax _internal_get_gmax( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials ) { return org::xtreemfs::interfaces::InternalGmax(); }
        virtual void _internal_truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size ) { }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse _internal_read_local( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) { return org::xtreemfs::interfaces::InternalReadLocalResponse(); }
        virtual org::xtreemfs::interfaces::ObjectData _check_object( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version ) { return org::xtreemfs::interfaces::ObjectData(); }
        virtual void _admin_shutdown( const std::string& password ) { }
      };
  
      // Use this macro in an implementation class to get all of the prototypes for the operations in OSDInterface
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PROTOTYPES \
      virtual org::xtreemfs::interfaces::ObjectData _read( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length );\
      virtual void _truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_response );\
      virtual void _unlink( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials );\
      virtual void _write( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, org::xtreemfs::interfaces::OSDWriteResponse& osd_response );\
      virtual void _keep_file_open( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials );\
      virtual org::xtreemfs::interfaces::InternalGmax _internal_get_gmax( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials );\
      virtual void _internal_truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size );\
      virtual org::xtreemfs::interfaces::InternalReadLocalResponse _internal_read_local( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length );\
      virtual org::xtreemfs::interfaces::ObjectData _check_object( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version );\
      virtual void _admin_shutdown( const std::string& password );
  
      // Use this macro in an implementation class to get dummy implementations for the operations in this interface
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_DUMMY_DEFINITIONS \
      virtual org::xtreemfs::interfaces::ObjectData _read( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length ) { return org::xtreemfs::interfaces::ObjectData(); }\
      virtual void _truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_response ) { }\
      virtual void _unlink( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials ) { }\
      virtual void _write( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& data, org::xtreemfs::interfaces::OSDWriteResponse& osd_response ) { }\
      virtual void _keep_file_open( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials ) { }\
      virtual org::xtreemfs::interfaces::InternalGmax _internal_get_gmax( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials ) { return org::xtreemfs::interfaces::InternalGmax(); }\
      virtual void _internal_truncate( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t new_file_size ) { }\
      virtual org::xtreemfs::interfaces::InternalReadLocalResponse _internal_read_local( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) { return org::xtreemfs::interfaces::InternalReadLocalResponse(); }\
      virtual org::xtreemfs::interfaces::ObjectData _check_object( const std::string& file_id, const org::xtreemfs::interfaces::FileCredentials& credentials, uint64_t object_number, uint64_t object_version ) { return org::xtreemfs::interfaces::ObjectData(); }\
      virtual void _admin_shutdown( const std::string& password ) { }
  
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handlereadRequest( readRequest& req );\
      virtual void handletruncateRequest( truncateRequest& req );\
      virtual void handleunlinkRequest( unlinkRequest& req );\
      virtual void handlewriteRequest( writeRequest& req );\
      virtual void handlekeep_file_openRequest( keep_file_openRequest& req );\
      virtual void handleinternal_get_gmaxRequest( internal_get_gmaxRequest& req );\
      virtual void handleinternal_truncateRequest( internal_truncateRequest& req );\
      virtual void handleinternal_read_localRequest( internal_read_localRequest& req );\
      virtual void handlecheck_objectRequest( check_objectRequest& req );\
      virtual void handleadmin_shutdownRequest( admin_shutdownRequest& req );
  
  
  
    };
  
  
  
  };
  
  

};

#endif
