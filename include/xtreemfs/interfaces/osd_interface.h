// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

    #ifndef _XTREEMFS_INTERFACES_OSD_INTERFACE_H_
    #define _XTREEMFS_INTERFACES_OSD_INTERFACE_H_

    #include "constants.h"
#include "mrc_osd_types.h"
#include "yidl.h"
#include "yield/concurrency.h"
#include <vector>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {

      class InternalGmax : public ::yidl::Struct
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

        // yidl::Object
        YIDL_OBJECT_PROTOTYPES( InternalGmax, 2009082668 );

        // YIELD::Struct
        void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeUint64( "epoch", 0, epoch ); marshaller.writeUint64( "last_object_id", 0, last_object_id ); marshaller.writeUint64( "file_size", 0, file_size ); }
        void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { epoch = unmarshaller.readUint64( "epoch", 0 ); last_object_id = unmarshaller.readUint64( "last_object_id", 0 ); file_size = unmarshaller.readUint64( "file_size", 0 ); }

      protected:
        uint64_t epoch;
        uint64_t last_object_id;
        uint64_t file_size;
      };

      class Lock : public ::yidl::Struct
      {
      public:
        Lock() : client_pid( 0 ), offset( 0 ), length( 0 ) { }
        Lock( const std::string& client_uuid, uint32_t client_pid, uint64_t offset, uint64_t length ) : client_uuid( client_uuid ), client_pid( client_pid ), offset( offset ), length( length ) { }
        Lock( const char* client_uuid, size_t client_uuid_len, uint32_t client_pid, uint64_t offset, uint64_t length ) : client_uuid( client_uuid, client_uuid_len ), client_pid( client_pid ), offset( offset ), length( length ) { }
        virtual ~Lock() { }

        void set_client_uuid( const std::string& client_uuid ) { set_client_uuid( client_uuid.c_str(), client_uuid.size() ); }
        void set_client_uuid( const char* client_uuid, size_t client_uuid_len ) { this->client_uuid.assign( client_uuid, client_uuid_len ); }
        const std::string& get_client_uuid() const { return client_uuid; }
        void set_client_pid( uint32_t client_pid ) { this->client_pid = client_pid; }
        uint32_t get_client_pid() const { return client_pid; }
        void set_offset( uint64_t offset ) { this->offset = offset; }
        uint64_t get_offset() const { return offset; }
        void set_length( uint64_t length ) { this->length = length; }
        uint64_t get_length() const { return length; }

        bool operator==( const Lock& other ) const { return client_uuid == other.client_uuid && client_pid == other.client_pid && offset == other.offset && length == other.length; }

        // yidl::Object
        YIDL_OBJECT_PROTOTYPES( Lock, 2009082672 );

        // YIELD::Struct
        void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeString( "client_uuid", 0, client_uuid ); marshaller.writeUint32( "client_pid", 0, client_pid ); marshaller.writeUint64( "offset", 0, offset ); marshaller.writeUint64( "length", 0, length ); }
        void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readString( "client_uuid", 0, client_uuid ); client_pid = unmarshaller.readUint32( "client_pid", 0 ); offset = unmarshaller.readUint64( "offset", 0 ); length = unmarshaller.readUint64( "length", 0 ); }

      protected:
        std::string client_uuid;
        uint32_t client_pid;
        uint64_t offset;
        uint64_t length;
      };

      class ObjectData : public ::yidl::Struct
      {
      public:
        ObjectData() : checksum( 0 ), invalid_checksum_on_osd( false ), zero_padding( 0 ) { }
        ObjectData( uint32_t checksum, bool invalid_checksum_on_osd, uint32_t zero_padding, ::yidl::auto_Buffer data ) : checksum( checksum ), invalid_checksum_on_osd( invalid_checksum_on_osd ), zero_padding( zero_padding ), data( data ) { }
        virtual ~ObjectData() { }

        void set_checksum( uint32_t checksum ) { this->checksum = checksum; }
        uint32_t get_checksum() const { return checksum; }
        void set_invalid_checksum_on_osd( bool invalid_checksum_on_osd ) { this->invalid_checksum_on_osd = invalid_checksum_on_osd; }
        bool get_invalid_checksum_on_osd() const { return invalid_checksum_on_osd; }
        void set_zero_padding( uint32_t zero_padding ) { this->zero_padding = zero_padding; }
        uint32_t get_zero_padding() const { return zero_padding; }
        void set_data( ::yidl::auto_Buffer data ) { this->data = data; }
        ::yidl::auto_Buffer get_data() const { return data; }

        bool operator==( const ObjectData& other ) const { return checksum == other.checksum && invalid_checksum_on_osd == other.invalid_checksum_on_osd && zero_padding == other.zero_padding && *data == *other.data; }

        // yidl::Object
        YIDL_OBJECT_PROTOTYPES( ObjectData, 2009082669 );

        // YIELD::Struct
        void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeUint32( "checksum", 0, checksum ); marshaller.writeBoolean( "invalid_checksum_on_osd", 0, invalid_checksum_on_osd ); marshaller.writeUint32( "zero_padding", 0, zero_padding ); marshaller.writeBuffer( "data", 0, data ); }
        void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { checksum = unmarshaller.readUint32( "checksum", 0 ); invalid_checksum_on_osd = unmarshaller.readBoolean( "invalid_checksum_on_osd", 0 ); zero_padding = unmarshaller.readUint32( "zero_padding", 0 ); unmarshaller.readBuffer( "data", 0, data ); }

      protected:
        uint32_t checksum;
        bool invalid_checksum_on_osd;
        uint32_t zero_padding;
        ::yidl::auto_Buffer data;
      };

      class ObjectList : public ::yidl::Struct
      {
      public:
        ObjectList() : stripeWidth( 0 ), firstObjectNo( 0 ) { }
        ObjectList( ::yidl::auto_Buffer set, uint32_t stripeWidth, uint32_t firstObjectNo ) : set( set ), stripeWidth( stripeWidth ), firstObjectNo( firstObjectNo ) { }
        virtual ~ObjectList() { }

        void set_set( ::yidl::auto_Buffer set ) { this->set = set; }
        ::yidl::auto_Buffer get_set() const { return set; }
        void set_stripeWidth( uint32_t stripeWidth ) { this->stripeWidth = stripeWidth; }
        uint32_t get_stripeWidth() const { return stripeWidth; }
        void set_firstObjectNo( uint32_t firstObjectNo ) { this->firstObjectNo = firstObjectNo; }
        uint32_t get_firstObjectNo() const { return firstObjectNo; }

        bool operator==( const ObjectList& other ) const { return *set == *other.set && stripeWidth == other.stripeWidth && firstObjectNo == other.firstObjectNo; }

        // yidl::Object
        YIDL_OBJECT_PROTOTYPES( ObjectList, 2009082673 );

        // YIELD::Struct
        void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeBuffer( "set", 0, set ); marshaller.writeUint32( "stripeWidth", 0, stripeWidth ); marshaller.writeUint32( "firstObjectNo", 0, firstObjectNo ); }
        void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readBuffer( "set", 0, set ); stripeWidth = unmarshaller.readUint32( "stripeWidth", 0 ); firstObjectNo = unmarshaller.readUint32( "firstObjectNo", 0 ); }

      protected:
        ::yidl::auto_Buffer set;
        uint32_t stripeWidth;
        uint32_t firstObjectNo;
      };

      class ObjectListSet : public ::yidl::Sequence, public std::vector<org::xtreemfs::interfaces::ObjectList>
      {
      public:
        ObjectListSet() { }
        ObjectListSet( const org::xtreemfs::interfaces::ObjectList& first_value ) { std::vector<org::xtreemfs::interfaces::ObjectList>::push_back( first_value ); }
        ObjectListSet( size_type size ) : std::vector<org::xtreemfs::interfaces::ObjectList>( size ) { }
        virtual ~ObjectListSet() { }

        // yidl::Object
        YIDL_OBJECT_PROTOTYPES( ObjectListSet, 2009082674 );

        // YIELD::Sequence
        size_t get_size() const { return size(); }
        void marshal( ::yidl::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( "value", 0, ( *this )[value_i] ); } }
        void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::ObjectList value; unmarshaller.readStruct( "value", 0, value ); push_back( value ); }
      };

      class InternalReadLocalResponse : public ::yidl::Struct
      {
      public:
        InternalReadLocalResponse() { }
        InternalReadLocalResponse( const org::xtreemfs::interfaces::ObjectData& data, const org::xtreemfs::interfaces::ObjectListSet& object_set ) : data( data ), object_set( object_set ) { }
        virtual ~InternalReadLocalResponse() { }

        void set_data( const org::xtreemfs::interfaces::ObjectData&  data ) { this->data = data; }
        const org::xtreemfs::interfaces::ObjectData& get_data() const { return data; }
        void set_object_set( const org::xtreemfs::interfaces::ObjectListSet&  object_set ) { this->object_set = object_set; }
        const org::xtreemfs::interfaces::ObjectListSet& get_object_set() const { return object_set; }

        bool operator==( const InternalReadLocalResponse& other ) const { return data == other.data && object_set == other.object_set; }

        // yidl::Object
        YIDL_OBJECT_PROTOTYPES( InternalReadLocalResponse, 2009082670 );

        // YIELD::Struct
        void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "data", 0, data ); marshaller.writeSequence( "object_set", 0, object_set ); }
        void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "data", 0, data ); unmarshaller.readSequence( "object_set", 0, object_set ); }

      protected:
        org::xtreemfs::interfaces::ObjectData data;
        org::xtreemfs::interfaces::ObjectListSet object_set;
      };



      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS ::YIELD::Interface
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
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS ::YIELD::Request
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
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS ::YIELD::Response
      #endif
      #endif

      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_EXCEPTION_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ::YIELD::ExceptionResponse
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


        virtual void read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data ) { read( file_credentials, file_id, object_number, object_version, offset, length, object_data, static_cast<uint64_t>( -1 ) ); }
        virtual void read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data, uint64_t response_timeout_ns ) { ::yidl::auto_Object<readRequest> __request( new readRequest( file_credentials, file_id, object_number, object_version, offset, length ) ); ::YIELD::auto_ResponseQueue<readResponse> __response_queue( new ::YIELD::ResponseQueue<readResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<readResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); object_data = __response->get_object_data(); }

        virtual void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { truncate( file_credentials, file_id, new_file_size, osd_write_response, static_cast<uint64_t>( -1 ) ); }
        virtual void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, uint64_t response_timeout_ns ) { ::yidl::auto_Object<truncateRequest> __request( new truncateRequest( file_credentials, file_id, new_file_size ) ); ::YIELD::auto_ResponseQueue<truncateResponse> __response_queue( new ::YIELD::ResponseQueue<truncateResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<truncateResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); osd_write_response = __response->get_osd_write_response(); }

        virtual void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { unlink( file_credentials, file_id, static_cast<uint64_t>( -1 ) ); }
        virtual void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t response_timeout_ns ) { ::yidl::auto_Object<unlinkRequest> __request( new unlinkRequest( file_credentials, file_id ) ); ::YIELD::auto_ResponseQueue<unlinkResponse> __response_queue( new ::YIELD::ResponseQueue<unlinkResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<unlinkResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { write( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data, osd_write_response, static_cast<uint64_t>( -1 ) ); }
        virtual void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, uint64_t response_timeout_ns ) { ::yidl::auto_Object<writeRequest> __request( new writeRequest( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data ) ); ::YIELD::auto_ResponseQueue<writeResponse> __response_queue( new ::YIELD::ResponseQueue<writeResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<writeResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); osd_write_response = __response->get_osd_write_response(); }

        virtual void xtreemfs_broadcast_gmax( const std::string& file_id, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize ) { xtreemfs_broadcast_gmax( file_id, truncateEpoch, lastObject, fileSize, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_broadcast_gmax( const std::string& file_id, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_broadcast_gmaxRequest> __request( new xtreemfs_broadcast_gmaxRequest( file_id, truncateEpoch, lastObject, fileSize ) ); ::YIELD::auto_ResponseQueue<xtreemfs_broadcast_gmaxResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_broadcast_gmaxResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_broadcast_gmaxResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual org::xtreemfs::interfaces::ObjectData xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version ) { return xtreemfs_check_object( file_credentials, file_id, object_number, object_version, static_cast<uint64_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::ObjectData xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_check_objectRequest> __request( new xtreemfs_check_objectRequest( file_credentials, file_id, object_number, object_version ) ); ::YIELD::auto_ResponseQueue<xtreemfs_check_objectResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_check_objectResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_check_objectResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); org::xtreemfs::interfaces::ObjectData _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_cleanup_get_results( org::xtreemfs::interfaces::StringSet& results ) { xtreemfs_cleanup_get_results( results, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_get_results( org::xtreemfs::interfaces::StringSet& results, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_cleanup_get_resultsRequest> __request( new xtreemfs_cleanup_get_resultsRequest() ); ::YIELD::auto_ResponseQueue<xtreemfs_cleanup_get_resultsResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_cleanup_get_resultsResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_cleanup_get_resultsResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); results = __response->get_results(); }

        virtual void xtreemfs_cleanup_is_running( bool& is_running ) { xtreemfs_cleanup_is_running( is_running, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_is_running( bool& is_running, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_cleanup_is_runningRequest> __request( new xtreemfs_cleanup_is_runningRequest() ); ::YIELD::auto_ResponseQueue<xtreemfs_cleanup_is_runningResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_cleanup_is_runningResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_cleanup_is_runningResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); is_running = __response->get_is_running(); }

        virtual void xtreemfs_cleanup_start( bool remove_zombies, bool remove_unavail_volume, bool lost_and_found ) { xtreemfs_cleanup_start( remove_zombies, remove_unavail_volume, lost_and_found, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_start( bool remove_zombies, bool remove_unavail_volume, bool lost_and_found, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_cleanup_startRequest> __request( new xtreemfs_cleanup_startRequest( remove_zombies, remove_unavail_volume, lost_and_found ) ); ::YIELD::auto_ResponseQueue<xtreemfs_cleanup_startResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_cleanup_startResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_cleanup_startResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_cleanup_status( std::string& status ) { xtreemfs_cleanup_status( status, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_status( std::string& status, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_cleanup_statusRequest> __request( new xtreemfs_cleanup_statusRequest() ); ::YIELD::auto_ResponseQueue<xtreemfs_cleanup_statusResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_cleanup_statusResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_cleanup_statusResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); status = __response->get_status(); }

        virtual void xtreemfs_cleanup_stop() { xtreemfs_cleanup_stop( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_stop( uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_cleanup_stopRequest> __request( new xtreemfs_cleanup_stopRequest() ); ::YIELD::auto_ResponseQueue<xtreemfs_cleanup_stopResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_cleanup_stopResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_cleanup_stopResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual org::xtreemfs::interfaces::InternalGmax xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return xtreemfs_internal_get_gmax( file_credentials, file_id, static_cast<uint64_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalGmax xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_internal_get_gmaxRequest> __request( new xtreemfs_internal_get_gmaxRequest( file_credentials, file_id ) ); ::YIELD::auto_ResponseQueue<xtreemfs_internal_get_gmaxResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_internal_get_gmaxResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_internal_get_gmaxResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); org::xtreemfs::interfaces::InternalGmax _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { xtreemfs_internal_truncate( file_credentials, file_id, new_file_size, osd_write_response, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_internal_truncateRequest> __request( new xtreemfs_internal_truncateRequest( file_credentials, file_id, new_file_size ) ); ::YIELD::auto_ResponseQueue<xtreemfs_internal_truncateResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_internal_truncateResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_internal_truncateResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); osd_write_response = __response->get_osd_write_response(); }

        virtual uint64_t xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return xtreemfs_internal_get_file_size( file_credentials, file_id, static_cast<uint64_t>( -1 ) ); }
        virtual uint64_t xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_internal_get_file_sizeRequest> __request( new xtreemfs_internal_get_file_sizeRequest( file_credentials, file_id ) ); ::YIELD::auto_ResponseQueue<xtreemfs_internal_get_file_sizeResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_internal_get_file_sizeResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_internal_get_file_sizeResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); uint64_t _return_value = __response->get__return_value(); return _return_value; }

        virtual org::xtreemfs::interfaces::InternalReadLocalResponse xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, bool attachObjectList, const org::xtreemfs::interfaces::ObjectListSet& requiredObjects ) { return xtreemfs_internal_read_local( file_credentials, file_id, object_number, object_version, offset, length, attachObjectList, requiredObjects, static_cast<uint64_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, bool attachObjectList, const org::xtreemfs::interfaces::ObjectListSet& requiredObjects, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_internal_read_localRequest> __request( new xtreemfs_internal_read_localRequest( file_credentials, file_id, object_number, object_version, offset, length, attachObjectList, requiredObjects ) ); ::YIELD::auto_ResponseQueue<xtreemfs_internal_read_localResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_internal_read_localResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_internal_read_localResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); org::xtreemfs::interfaces::InternalReadLocalResponse _return_value = __response->get__return_value(); return _return_value; }

        virtual org::xtreemfs::interfaces::ObjectList xtreemfs_internal_get_object_set( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return xtreemfs_internal_get_object_set( file_credentials, file_id, static_cast<uint64_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::ObjectList xtreemfs_internal_get_object_set( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_internal_get_object_setRequest> __request( new xtreemfs_internal_get_object_setRequest( file_credentials, file_id ) ); ::YIELD::auto_ResponseQueue<xtreemfs_internal_get_object_setResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_internal_get_object_setResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_internal_get_object_setResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); org::xtreemfs::interfaces::ObjectList _return_value = __response->get__return_value(); return _return_value; }

        virtual org::xtreemfs::interfaces::Lock xtreemfs_lock_acquire( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& client_uuid, int32_t client_pid, const std::string& file_id, uint64_t offset, uint64_t length, bool exclusive ) { return xtreemfs_lock_acquire( file_credentials, client_uuid, client_pid, file_id, offset, length, exclusive, static_cast<uint64_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::Lock xtreemfs_lock_acquire( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& client_uuid, int32_t client_pid, const std::string& file_id, uint64_t offset, uint64_t length, bool exclusive, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_lock_acquireRequest> __request( new xtreemfs_lock_acquireRequest( file_credentials, client_uuid, client_pid, file_id, offset, length, exclusive ) ); ::YIELD::auto_ResponseQueue<xtreemfs_lock_acquireResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_lock_acquireResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_lock_acquireResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); org::xtreemfs::interfaces::Lock _return_value = __response->get__return_value(); return _return_value; }

        virtual org::xtreemfs::interfaces::Lock xtreemfs_lock_check( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& client_uuid, int32_t client_pid, const std::string& file_id, uint64_t offset, uint64_t length, bool exclusive ) { return xtreemfs_lock_check( file_credentials, client_uuid, client_pid, file_id, offset, length, exclusive, static_cast<uint64_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::Lock xtreemfs_lock_check( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& client_uuid, int32_t client_pid, const std::string& file_id, uint64_t offset, uint64_t length, bool exclusive, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_lock_checkRequest> __request( new xtreemfs_lock_checkRequest( file_credentials, client_uuid, client_pid, file_id, offset, length, exclusive ) ); ::YIELD::auto_ResponseQueue<xtreemfs_lock_checkResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_lock_checkResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_lock_checkResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); org::xtreemfs::interfaces::Lock _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_lock_release( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, const org::xtreemfs::interfaces::Lock& lock ) { xtreemfs_lock_release( file_credentials, file_id, lock, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_lock_release( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, const org::xtreemfs::interfaces::Lock& lock, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_lock_releaseRequest> __request( new xtreemfs_lock_releaseRequest( file_credentials, file_id, lock ) ); ::YIELD::auto_ResponseQueue<xtreemfs_lock_releaseResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_lock_releaseResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_lock_releaseResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_ping( const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates, org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates ) { xtreemfs_ping( coordinates, remote_coordinates, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_ping( const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates, org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates, uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_pingRequest> __request( new xtreemfs_pingRequest( coordinates ) ); ::YIELD::auto_ResponseQueue<xtreemfs_pingResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_pingResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_pingResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); remote_coordinates = __response->get_remote_coordinates(); }

        virtual void xtreemfs_shutdown() { xtreemfs_shutdown( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( uint64_t response_timeout_ns ) { ::yidl::auto_Object<xtreemfs_shutdownRequest> __request( new xtreemfs_shutdownRequest() ); ::YIELD::auto_ResponseQueue<xtreemfs_shutdownResponse> __response_queue( new ::YIELD::ResponseQueue<xtreemfs_shutdownResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::auto_Object<xtreemfs_shutdownResponse> __response = __response_queue->timed_dequeue( response_timeout_ns ); }


        // Request/response pair definitions for the operations in OSDInterface

        class readResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          readResponse() { }
          readResponse( const org::xtreemfs::interfaces::ObjectData& object_data ) : object_data( object_data ) { }
          virtual ~readResponse() { }

          void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }
          const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }

          bool operator==( const readResponse& other ) const { return object_data == other.object_data; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( readResponse, 2009082928 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "object_data", 0, object_data ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "object_data", 0, object_data ); }

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
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
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

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( readRequest, 2009082928 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "object_number", 0, object_number ); marshaller.writeUint64( "object_version", 0, object_version ); marshaller.writeUint32( "offset", 0, offset ); marshaller.writeUint32( "length", 0, length ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); object_number = unmarshaller.readUint64( "object_number", 0 ); object_version = unmarshaller.readUint64( "object_version", 0 ); offset = unmarshaller.readUint32( "offset", 0 ); length = unmarshaller.readUint32( "length", 0 ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new readResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
          uint64_t object_number;
          uint64_t object_version;
          uint32_t offset;
          uint32_t length;
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

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( truncateResponse, 2009082929 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "osd_write_response", 0, osd_write_response ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "osd_write_response", 0, osd_write_response ); }

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
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }
          void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }
          uint64_t get_new_file_size() const { return new_file_size; }

          bool operator==( const truncateRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && new_file_size == other.new_file_size; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( truncateRequest, 2009082929 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "new_file_size", 0, new_file_size ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); new_file_size = unmarshaller.readUint64( "new_file_size", 0 ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new truncateResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
          uint64_t new_file_size;
        };

        class unlinkResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          unlinkResponse() { }
          virtual ~unlinkResponse() { }

          bool operator==( const unlinkResponse& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( unlinkResponse, 2009082930 );

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
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }

          bool operator==( const unlinkRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( unlinkRequest, 2009082930 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new unlinkResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
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

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( writeResponse, 2009082931 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "osd_write_response", 0, osd_write_response ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "osd_write_response", 0, osd_write_response ); }

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
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
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

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( writeRequest, 2009082931 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "object_number", 0, object_number ); marshaller.writeUint64( "object_version", 0, object_version ); marshaller.writeUint32( "offset", 0, offset ); marshaller.writeUint64( "lease_timeout", 0, lease_timeout ); marshaller.writeStruct( "object_data", 0, object_data ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); object_number = unmarshaller.readUint64( "object_number", 0 ); object_version = unmarshaller.readUint64( "object_version", 0 ); offset = unmarshaller.readUint32( "offset", 0 ); lease_timeout = unmarshaller.readUint64( "lease_timeout", 0 ); unmarshaller.readStruct( "object_data", 0, object_data ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new writeResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
          uint64_t object_number;
          uint64_t object_version;
          uint32_t offset;
          uint64_t lease_timeout;
          org::xtreemfs::interfaces::ObjectData object_data;
        };

        class xtreemfs_broadcast_gmaxResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_broadcast_gmaxResponse() { }
          virtual ~xtreemfs_broadcast_gmaxResponse() { }

          bool operator==( const xtreemfs_broadcast_gmaxResponse& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_broadcast_gmaxResponse, 2009082938 );

        };

        class xtreemfs_broadcast_gmaxRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_broadcast_gmaxRequest() : truncateEpoch( 0 ), lastObject( 0 ), fileSize( 0 ) { }
          xtreemfs_broadcast_gmaxRequest( const std::string& file_id, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize ) : file_id( file_id ), truncateEpoch( truncateEpoch ), lastObject( lastObject ), fileSize( fileSize ) { }
          xtreemfs_broadcast_gmaxRequest( const char* file_id, size_t file_id_len, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize ) : file_id( file_id, file_id_len ), truncateEpoch( truncateEpoch ), lastObject( lastObject ), fileSize( fileSize ) { }
          virtual ~xtreemfs_broadcast_gmaxRequest() { }

          void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }
          void set_truncateEpoch( uint64_t truncateEpoch ) { this->truncateEpoch = truncateEpoch; }
          uint64_t get_truncateEpoch() const { return truncateEpoch; }
          void set_lastObject( uint64_t lastObject ) { this->lastObject = lastObject; }
          uint64_t get_lastObject() const { return lastObject; }
          void set_fileSize( uint64_t fileSize ) { this->fileSize = fileSize; }
          uint64_t get_fileSize() const { return fileSize; }

          bool operator==( const xtreemfs_broadcast_gmaxRequest& other ) const { return file_id == other.file_id && truncateEpoch == other.truncateEpoch && lastObject == other.lastObject && fileSize == other.fileSize; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_broadcast_gmaxRequest, 2009082938 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "truncateEpoch", 0, truncateEpoch ); marshaller.writeUint64( "lastObject", 0, lastObject ); marshaller.writeUint64( "fileSize", 0, fileSize ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readString( "file_id", 0, file_id ); truncateEpoch = unmarshaller.readUint64( "truncateEpoch", 0 ); lastObject = unmarshaller.readUint64( "lastObject", 0 ); fileSize = unmarshaller.readUint64( "fileSize", 0 ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_broadcast_gmaxResponse; }


        protected:
          std::string file_id;
          uint64_t truncateEpoch;
          uint64_t lastObject;
          uint64_t fileSize;
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

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_check_objectResponse, 2009082939 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "_return_value", 0, _return_value ); }

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
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }
          void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
          uint64_t get_object_number() const { return object_number; }
          void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
          uint64_t get_object_version() const { return object_version; }

          bool operator==( const xtreemfs_check_objectRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && object_number == other.object_number && object_version == other.object_version; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_check_objectRequest, 2009082939 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "object_number", 0, object_number ); marshaller.writeUint64( "object_version", 0, object_version ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); object_number = unmarshaller.readUint64( "object_number", 0 ); object_version = unmarshaller.readUint64( "object_version", 0 ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_check_objectResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
          uint64_t object_number;
          uint64_t object_version;
        };

        class xtreemfs_cleanup_get_resultsResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_get_resultsResponse() { }
          xtreemfs_cleanup_get_resultsResponse( const org::xtreemfs::interfaces::StringSet& results ) : results( results ) { }
          virtual ~xtreemfs_cleanup_get_resultsResponse() { }

          void set_results( const org::xtreemfs::interfaces::StringSet&  results ) { this->results = results; }
          const org::xtreemfs::interfaces::StringSet& get_results() const { return results; }

          bool operator==( const xtreemfs_cleanup_get_resultsResponse& other ) const { return results == other.results; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_get_resultsResponse, 2009082948 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeSequence( "results", 0, results ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "results", 0, results ); }

        protected:
          org::xtreemfs::interfaces::StringSet results;
        };

        class xtreemfs_cleanup_get_resultsRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_get_resultsRequest() { }
          virtual ~xtreemfs_cleanup_get_resultsRequest() { }

          bool operator==( const xtreemfs_cleanup_get_resultsRequest& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_get_resultsRequest, 2009082948 );
          // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_cleanup_get_resultsResponse; }

        };

        class xtreemfs_cleanup_is_runningResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_is_runningResponse() : is_running( false ) { }
          xtreemfs_cleanup_is_runningResponse( bool is_running ) : is_running( is_running ) { }
          virtual ~xtreemfs_cleanup_is_runningResponse() { }

          void set_is_running( bool is_running ) { this->is_running = is_running; }
          bool get_is_running() const { return is_running; }

          bool operator==( const xtreemfs_cleanup_is_runningResponse& other ) const { return is_running == other.is_running; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_is_runningResponse, 2009082949 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeBoolean( "is_running", 0, is_running ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { is_running = unmarshaller.readBoolean( "is_running", 0 ); }

        protected:
          bool is_running;
        };

        class xtreemfs_cleanup_is_runningRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_is_runningRequest() { }
          virtual ~xtreemfs_cleanup_is_runningRequest() { }

          bool operator==( const xtreemfs_cleanup_is_runningRequest& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_is_runningRequest, 2009082949 );
          // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_cleanup_is_runningResponse; }

        };

        class xtreemfs_cleanup_startResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_startResponse() { }
          virtual ~xtreemfs_cleanup_startResponse() { }

          bool operator==( const xtreemfs_cleanup_startResponse& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_startResponse, 2009082950 );

        };

        class xtreemfs_cleanup_startRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_startRequest() : remove_zombies( false ), remove_unavail_volume( false ), lost_and_found( false ) { }
          xtreemfs_cleanup_startRequest( bool remove_zombies, bool remove_unavail_volume, bool lost_and_found ) : remove_zombies( remove_zombies ), remove_unavail_volume( remove_unavail_volume ), lost_and_found( lost_and_found ) { }
          virtual ~xtreemfs_cleanup_startRequest() { }

          void set_remove_zombies( bool remove_zombies ) { this->remove_zombies = remove_zombies; }
          bool get_remove_zombies() const { return remove_zombies; }
          void set_remove_unavail_volume( bool remove_unavail_volume ) { this->remove_unavail_volume = remove_unavail_volume; }
          bool get_remove_unavail_volume() const { return remove_unavail_volume; }
          void set_lost_and_found( bool lost_and_found ) { this->lost_and_found = lost_and_found; }
          bool get_lost_and_found() const { return lost_and_found; }

          bool operator==( const xtreemfs_cleanup_startRequest& other ) const { return remove_zombies == other.remove_zombies && remove_unavail_volume == other.remove_unavail_volume && lost_and_found == other.lost_and_found; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_startRequest, 2009082950 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeBoolean( "remove_zombies", 0, remove_zombies ); marshaller.writeBoolean( "remove_unavail_volume", 0, remove_unavail_volume ); marshaller.writeBoolean( "lost_and_found", 0, lost_and_found ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { remove_zombies = unmarshaller.readBoolean( "remove_zombies", 0 ); remove_unavail_volume = unmarshaller.readBoolean( "remove_unavail_volume", 0 ); lost_and_found = unmarshaller.readBoolean( "lost_and_found", 0 ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_cleanup_startResponse; }


        protected:
          bool remove_zombies;
          bool remove_unavail_volume;
          bool lost_and_found;
        };

        class xtreemfs_cleanup_statusResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_statusResponse() { }
          xtreemfs_cleanup_statusResponse( const std::string& status ) : status( status ) { }
          xtreemfs_cleanup_statusResponse( const char* status, size_t status_len ) : status( status, status_len ) { }
          virtual ~xtreemfs_cleanup_statusResponse() { }

          void set_status( const std::string& status ) { set_status( status.c_str(), status.size() ); }
          void set_status( const char* status, size_t status_len ) { this->status.assign( status, status_len ); }
          const std::string& get_status() const { return status; }

          bool operator==( const xtreemfs_cleanup_statusResponse& other ) const { return status == other.status; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_statusResponse, 2009082951 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeString( "status", 0, status ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readString( "status", 0, status ); }

        protected:
          std::string status;
        };

        class xtreemfs_cleanup_statusRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_statusRequest() { }
          virtual ~xtreemfs_cleanup_statusRequest() { }

          bool operator==( const xtreemfs_cleanup_statusRequest& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_statusRequest, 2009082951 );
          // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_cleanup_statusResponse; }

        };

        class xtreemfs_cleanup_stopResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_stopResponse() { }
          virtual ~xtreemfs_cleanup_stopResponse() { }

          bool operator==( const xtreemfs_cleanup_stopResponse& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_stopResponse, 2009082952 );

        };

        class xtreemfs_cleanup_stopRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_stopRequest() { }
          virtual ~xtreemfs_cleanup_stopRequest() { }

          bool operator==( const xtreemfs_cleanup_stopRequest& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_cleanup_stopRequest, 2009082952 );
          // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_cleanup_stopResponse; }

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

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_get_gmaxResponse, 2009082958 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "_return_value", 0, _return_value ); }

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
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }

          bool operator==( const xtreemfs_internal_get_gmaxRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_get_gmaxRequest, 2009082958 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_internal_get_gmaxResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
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

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_truncateResponse, 2009082959 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "osd_write_response", 0, osd_write_response ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "osd_write_response", 0, osd_write_response ); }

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
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }
          void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }
          uint64_t get_new_file_size() const { return new_file_size; }

          bool operator==( const xtreemfs_internal_truncateRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && new_file_size == other.new_file_size; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_truncateRequest, 2009082959 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "new_file_size", 0, new_file_size ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); new_file_size = unmarshaller.readUint64( "new_file_size", 0 ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_internal_truncateResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
          uint64_t new_file_size;
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

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_get_file_sizeResponse, 2009082960 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeUint64( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { _return_value = unmarshaller.readUint64( "_return_value", 0 ); }

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
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }

          bool operator==( const xtreemfs_internal_get_file_sizeRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_get_file_sizeRequest, 2009082960 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_internal_get_file_sizeResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
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

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_read_localResponse, 2009082961 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "_return_value", 0, _return_value ); }

        protected:
          org::xtreemfs::interfaces::InternalReadLocalResponse _return_value;
        };

        class xtreemfs_internal_read_localRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_internal_read_localRequest() : object_number( 0 ), object_version( 0 ), offset( 0 ), length( 0 ), attachObjectList( false ) { }
          xtreemfs_internal_read_localRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, bool attachObjectList, const org::xtreemfs::interfaces::ObjectListSet& requiredObjects ) : file_credentials( file_credentials ), file_id( file_id ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ), attachObjectList( attachObjectList ), requiredObjects( requiredObjects ) { }
          xtreemfs_internal_read_localRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, bool attachObjectList, const org::xtreemfs::interfaces::ObjectListSet& requiredObjects ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ), object_number( object_number ), object_version( object_version ), offset( offset ), length( length ), attachObjectList( attachObjectList ), requiredObjects( requiredObjects ) { }
          virtual ~xtreemfs_internal_read_localRequest() { }

          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }
          void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
          uint64_t get_object_number() const { return object_number; }
          void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
          uint64_t get_object_version() const { return object_version; }
          void set_offset( uint64_t offset ) { this->offset = offset; }
          uint64_t get_offset() const { return offset; }
          void set_length( uint64_t length ) { this->length = length; }
          uint64_t get_length() const { return length; }
          void set_attachObjectList( bool attachObjectList ) { this->attachObjectList = attachObjectList; }
          bool get_attachObjectList() const { return attachObjectList; }
          void set_requiredObjects( const org::xtreemfs::interfaces::ObjectListSet&  requiredObjects ) { this->requiredObjects = requiredObjects; }
          const org::xtreemfs::interfaces::ObjectListSet& get_requiredObjects() const { return requiredObjects; }

          bool operator==( const xtreemfs_internal_read_localRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && object_number == other.object_number && object_version == other.object_version && offset == other.offset && length == other.length && attachObjectList == other.attachObjectList && requiredObjects == other.requiredObjects; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_read_localRequest, 2009082961 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "object_number", 0, object_number ); marshaller.writeUint64( "object_version", 0, object_version ); marshaller.writeUint64( "offset", 0, offset ); marshaller.writeUint64( "length", 0, length ); marshaller.writeBoolean( "attachObjectList", 0, attachObjectList ); marshaller.writeSequence( "requiredObjects", 0, requiredObjects ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); object_number = unmarshaller.readUint64( "object_number", 0 ); object_version = unmarshaller.readUint64( "object_version", 0 ); offset = unmarshaller.readUint64( "offset", 0 ); length = unmarshaller.readUint64( "length", 0 ); attachObjectList = unmarshaller.readBoolean( "attachObjectList", 0 ); unmarshaller.readSequence( "requiredObjects", 0, requiredObjects ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_internal_read_localResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
          uint64_t object_number;
          uint64_t object_version;
          uint64_t offset;
          uint64_t length;
          bool attachObjectList;
          org::xtreemfs::interfaces::ObjectListSet requiredObjects;
        };

        class xtreemfs_internal_get_object_setResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_internal_get_object_setResponse() { }
          xtreemfs_internal_get_object_setResponse( const org::xtreemfs::interfaces::ObjectList& _return_value ) : _return_value( _return_value ) { }
          virtual ~xtreemfs_internal_get_object_setResponse() { }

          void set__return_value( const org::xtreemfs::interfaces::ObjectList&  _return_value ) { this->_return_value = _return_value; }
          const org::xtreemfs::interfaces::ObjectList& get__return_value() const { return _return_value; }

          bool operator==( const xtreemfs_internal_get_object_setResponse& other ) const { return _return_value == other._return_value; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_get_object_setResponse, 2009082962 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "_return_value", 0, _return_value ); }

        protected:
          org::xtreemfs::interfaces::ObjectList _return_value;
        };

        class xtreemfs_internal_get_object_setRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_internal_get_object_setRequest() { }
          xtreemfs_internal_get_object_setRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) : file_credentials( file_credentials ), file_id( file_id ) { }
          xtreemfs_internal_get_object_setRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ) { }
          virtual ~xtreemfs_internal_get_object_setRequest() { }

          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }

          bool operator==( const xtreemfs_internal_get_object_setRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_internal_get_object_setRequest, 2009082962 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_internal_get_object_setResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
        };

        class xtreemfs_lock_acquireResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_lock_acquireResponse() { }
          xtreemfs_lock_acquireResponse( const org::xtreemfs::interfaces::Lock& _return_value ) : _return_value( _return_value ) { }
          virtual ~xtreemfs_lock_acquireResponse() { }

          void set__return_value( const org::xtreemfs::interfaces::Lock&  _return_value ) { this->_return_value = _return_value; }
          const org::xtreemfs::interfaces::Lock& get__return_value() const { return _return_value; }

          bool operator==( const xtreemfs_lock_acquireResponse& other ) const { return _return_value == other._return_value; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_lock_acquireResponse, 2009082968 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "_return_value", 0, _return_value ); }

        protected:
          org::xtreemfs::interfaces::Lock _return_value;
        };

        class xtreemfs_lock_acquireRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lock_acquireRequest() : client_pid( 0 ), offset( 0 ), length( 0 ), exclusive( false ) { }
          xtreemfs_lock_acquireRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& client_uuid, int32_t client_pid, const std::string& file_id, uint64_t offset, uint64_t length, bool exclusive ) : file_credentials( file_credentials ), client_uuid( client_uuid ), client_pid( client_pid ), file_id( file_id ), offset( offset ), length( length ), exclusive( exclusive ) { }
          xtreemfs_lock_acquireRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* client_uuid, size_t client_uuid_len, int32_t client_pid, const char* file_id, size_t file_id_len, uint64_t offset, uint64_t length, bool exclusive ) : file_credentials( file_credentials ), client_uuid( client_uuid, client_uuid_len ), client_pid( client_pid ), file_id( file_id, file_id_len ), offset( offset ), length( length ), exclusive( exclusive ) { }
          virtual ~xtreemfs_lock_acquireRequest() { }

          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          void set_client_uuid( const std::string& client_uuid ) { set_client_uuid( client_uuid.c_str(), client_uuid.size() ); }
          void set_client_uuid( const char* client_uuid, size_t client_uuid_len ) { this->client_uuid.assign( client_uuid, client_uuid_len ); }
          const std::string& get_client_uuid() const { return client_uuid; }
          void set_client_pid( int32_t client_pid ) { this->client_pid = client_pid; }
          int32_t get_client_pid() const { return client_pid; }
          void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }
          void set_offset( uint64_t offset ) { this->offset = offset; }
          uint64_t get_offset() const { return offset; }
          void set_length( uint64_t length ) { this->length = length; }
          uint64_t get_length() const { return length; }
          void set_exclusive( bool exclusive ) { this->exclusive = exclusive; }
          bool get_exclusive() const { return exclusive; }

          bool operator==( const xtreemfs_lock_acquireRequest& other ) const { return file_credentials == other.file_credentials && client_uuid == other.client_uuid && client_pid == other.client_pid && file_id == other.file_id && offset == other.offset && length == other.length && exclusive == other.exclusive; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_lock_acquireRequest, 2009082968 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "client_uuid", 0, client_uuid ); marshaller.writeInt32( "client_pid", 0, client_pid ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "offset", 0, offset ); marshaller.writeUint64( "length", 0, length ); marshaller.writeBoolean( "exclusive", 0, exclusive ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "client_uuid", 0, client_uuid ); client_pid = unmarshaller.readInt32( "client_pid", 0 ); unmarshaller.readString( "file_id", 0, file_id ); offset = unmarshaller.readUint64( "offset", 0 ); length = unmarshaller.readUint64( "length", 0 ); exclusive = unmarshaller.readBoolean( "exclusive", 0 ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_lock_acquireResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string client_uuid;
          int32_t client_pid;
          std::string file_id;
          uint64_t offset;
          uint64_t length;
          bool exclusive;
        };

        class xtreemfs_lock_checkResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_lock_checkResponse() { }
          xtreemfs_lock_checkResponse( const org::xtreemfs::interfaces::Lock& _return_value ) : _return_value( _return_value ) { }
          virtual ~xtreemfs_lock_checkResponse() { }

          void set__return_value( const org::xtreemfs::interfaces::Lock&  _return_value ) { this->_return_value = _return_value; }
          const org::xtreemfs::interfaces::Lock& get__return_value() const { return _return_value; }

          bool operator==( const xtreemfs_lock_checkResponse& other ) const { return _return_value == other._return_value; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_lock_checkResponse, 2009082969 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "_return_value", 0, _return_value ); }

        protected:
          org::xtreemfs::interfaces::Lock _return_value;
        };

        class xtreemfs_lock_checkRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lock_checkRequest() : client_pid( 0 ), offset( 0 ), length( 0 ), exclusive( false ) { }
          xtreemfs_lock_checkRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& client_uuid, int32_t client_pid, const std::string& file_id, uint64_t offset, uint64_t length, bool exclusive ) : file_credentials( file_credentials ), client_uuid( client_uuid ), client_pid( client_pid ), file_id( file_id ), offset( offset ), length( length ), exclusive( exclusive ) { }
          xtreemfs_lock_checkRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* client_uuid, size_t client_uuid_len, int32_t client_pid, const char* file_id, size_t file_id_len, uint64_t offset, uint64_t length, bool exclusive ) : file_credentials( file_credentials ), client_uuid( client_uuid, client_uuid_len ), client_pid( client_pid ), file_id( file_id, file_id_len ), offset( offset ), length( length ), exclusive( exclusive ) { }
          virtual ~xtreemfs_lock_checkRequest() { }

          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          void set_client_uuid( const std::string& client_uuid ) { set_client_uuid( client_uuid.c_str(), client_uuid.size() ); }
          void set_client_uuid( const char* client_uuid, size_t client_uuid_len ) { this->client_uuid.assign( client_uuid, client_uuid_len ); }
          const std::string& get_client_uuid() const { return client_uuid; }
          void set_client_pid( int32_t client_pid ) { this->client_pid = client_pid; }
          int32_t get_client_pid() const { return client_pid; }
          void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }
          void set_offset( uint64_t offset ) { this->offset = offset; }
          uint64_t get_offset() const { return offset; }
          void set_length( uint64_t length ) { this->length = length; }
          uint64_t get_length() const { return length; }
          void set_exclusive( bool exclusive ) { this->exclusive = exclusive; }
          bool get_exclusive() const { return exclusive; }

          bool operator==( const xtreemfs_lock_checkRequest& other ) const { return file_credentials == other.file_credentials && client_uuid == other.client_uuid && client_pid == other.client_pid && file_id == other.file_id && offset == other.offset && length == other.length && exclusive == other.exclusive; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_lock_checkRequest, 2009082969 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "client_uuid", 0, client_uuid ); marshaller.writeInt32( "client_pid", 0, client_pid ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "offset", 0, offset ); marshaller.writeUint64( "length", 0, length ); marshaller.writeBoolean( "exclusive", 0, exclusive ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "client_uuid", 0, client_uuid ); client_pid = unmarshaller.readInt32( "client_pid", 0 ); unmarshaller.readString( "file_id", 0, file_id ); offset = unmarshaller.readUint64( "offset", 0 ); length = unmarshaller.readUint64( "length", 0 ); exclusive = unmarshaller.readBoolean( "exclusive", 0 ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_lock_checkResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string client_uuid;
          int32_t client_pid;
          std::string file_id;
          uint64_t offset;
          uint64_t length;
          bool exclusive;
        };

        class xtreemfs_lock_releaseResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_lock_releaseResponse() { }
          virtual ~xtreemfs_lock_releaseResponse() { }

          bool operator==( const xtreemfs_lock_releaseResponse& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_lock_releaseResponse, 2009082970 );

        };

        class xtreemfs_lock_releaseRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lock_releaseRequest() { }
          xtreemfs_lock_releaseRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, const org::xtreemfs::interfaces::Lock& lock ) : file_credentials( file_credentials ), file_id( file_id ), lock( lock ) { }
          xtreemfs_lock_releaseRequest( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::Lock& lock ) : file_credentials( file_credentials ), file_id( file_id, file_id_len ), lock( lock ) { }
          virtual ~xtreemfs_lock_releaseRequest() { }

          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }
          void set_lock( const org::xtreemfs::interfaces::Lock&  lock ) { this->lock = lock; }
          const org::xtreemfs::interfaces::Lock& get_lock() const { return lock; }

          bool operator==( const xtreemfs_lock_releaseRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && lock == other.lock; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_lock_releaseRequest, 2009082970 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeStruct( "lock", 0, lock ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); unmarshaller.readString( "file_id", 0, file_id ); unmarshaller.readStruct( "lock", 0, lock ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_lock_releaseResponse; }


        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
          org::xtreemfs::interfaces::Lock lock;
        };

        class xtreemfs_pingResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_pingResponse() { }
          xtreemfs_pingResponse( const org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates ) : remote_coordinates( remote_coordinates ) { }
          virtual ~xtreemfs_pingResponse() { }

          void set_remote_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  remote_coordinates ) { this->remote_coordinates = remote_coordinates; }
          const org::xtreemfs::interfaces::VivaldiCoordinates& get_remote_coordinates() const { return remote_coordinates; }

          bool operator==( const xtreemfs_pingResponse& other ) const { return remote_coordinates == other.remote_coordinates; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_pingResponse, 2009082978 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "remote_coordinates", 0, remote_coordinates ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "remote_coordinates", 0, remote_coordinates ); }

        protected:
          org::xtreemfs::interfaces::VivaldiCoordinates remote_coordinates;
        };

        class xtreemfs_pingRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_pingRequest() { }
          xtreemfs_pingRequest( const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates ) : coordinates( coordinates ) { }
          virtual ~xtreemfs_pingRequest() { }

          void set_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  coordinates ) { this->coordinates = coordinates; }
          const org::xtreemfs::interfaces::VivaldiCoordinates& get_coordinates() const { return coordinates; }

          bool operator==( const xtreemfs_pingRequest& other ) const { return coordinates == other.coordinates; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_pingRequest, 2009082978 );

          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeStruct( "coordinates", 0, coordinates ); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "coordinates", 0, coordinates ); }  // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_pingResponse; }


        protected:
          org::xtreemfs::interfaces::VivaldiCoordinates coordinates;
        };

        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownResponse() { }
          virtual ~xtreemfs_shutdownResponse() { }

          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 2009082988 );

        };

        class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownRequest() { }
          virtual ~xtreemfs_shutdownRequest() { }

          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

          // yidl::Object
          YIDL_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 2009082988 );
          // YIELD::Request
          virtual ::YIELD::auto_Response createResponse() { return new xtreemfs_shutdownResponse; }

        };

          class ConcurrentModificationException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            ConcurrentModificationException() { }
          ConcurrentModificationException( const std::string& stack_trace ) : stack_trace( stack_trace ) { }
          ConcurrentModificationException( const char* stack_trace, size_t stack_trace_len ) : stack_trace( stack_trace, stack_trace_len ) { }
            ConcurrentModificationException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~ConcurrentModificationException() throw() { }

          void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
          void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
          const std::string& get_stack_trace() const { return stack_trace; }

            // YIELD::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new ConcurrentModificationException( stack_trace); }
            virtual void throwStackClone() const { throw ConcurrentModificationException( stack_trace); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeString( "stack_trace", 0, stack_trace ); }

        protected:
          std::string stack_trace;
          };

          class errnoException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            errnoException() : error_code( 0 ) { }
          errnoException( uint32_t error_code, const std::string& error_message, const std::string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
          errnoException( uint32_t error_code, const char* error_message, size_t error_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), error_message( error_message, error_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
            errnoException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~errnoException() throw() { }

          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          uint32_t get_error_code() const { return error_code; }
          void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
          void set_error_message( const char* error_message, size_t error_message_len ) { this->error_message.assign( error_message, error_message_len ); }
          const std::string& get_error_message() const { return error_message; }
          void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
          void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
          const std::string& get_stack_trace() const { return stack_trace; }

            // YIELD::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new errnoException( error_code, error_message, stack_trace); }
            virtual void throwStackClone() const { throw errnoException( error_code, error_message, stack_trace); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { error_code = unmarshaller.readUint32( "error_code", 0 ); unmarshaller.readString( "error_message", 0, error_message ); unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeUint32( "error_code", 0, error_code ); marshaller.writeString( "error_message", 0, error_message ); marshaller.writeString( "stack_trace", 0, stack_trace ); }

        protected:
          uint32_t error_code;
          std::string error_message;
          std::string stack_trace;
          };

          class InvalidArgumentException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            InvalidArgumentException() { }
          InvalidArgumentException( const std::string& error_message ) : error_message( error_message ) { }
          InvalidArgumentException( const char* error_message, size_t error_message_len ) : error_message( error_message, error_message_len ) { }
            InvalidArgumentException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~InvalidArgumentException() throw() { }

          void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
          void set_error_message( const char* error_message, size_t error_message_len ) { this->error_message.assign( error_message, error_message_len ); }
          const std::string& get_error_message() const { return error_message; }

            // YIELD::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new InvalidArgumentException( error_message); }
            virtual void throwStackClone() const { throw InvalidArgumentException( error_message); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readString( "error_message", 0, error_message ); }
          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeString( "error_message", 0, error_message ); }

        protected:
          std::string error_message;
          };

          class OSDException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            OSDException() : error_code( 0 ) { }
          OSDException( uint32_t error_code, const std::string& error_message, const std::string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
          OSDException( uint32_t error_code, const char* error_message, size_t error_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), error_message( error_message, error_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
            OSDException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~OSDException() throw() { }

          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          uint32_t get_error_code() const { return error_code; }
          void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
          void set_error_message( const char* error_message, size_t error_message_len ) { this->error_message.assign( error_message, error_message_len ); }
          const std::string& get_error_message() const { return error_message; }
          void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
          void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
          const std::string& get_stack_trace() const { return stack_trace; }

            // YIELD::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new OSDException( error_code, error_message, stack_trace); }
            virtual void throwStackClone() const { throw OSDException( error_code, error_message, stack_trace); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { error_code = unmarshaller.readUint32( "error_code", 0 ); unmarshaller.readString( "error_message", 0, error_message ); unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeUint32( "error_code", 0, error_code ); marshaller.writeString( "error_message", 0, error_message ); marshaller.writeString( "stack_trace", 0, stack_trace ); }

        protected:
          uint32_t error_code;
          std::string error_message;
          std::string stack_trace;
          };

          class ProtocolException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            ProtocolException() : accept_stat( 0 ), error_code( 0 ) { }
          ProtocolException( uint32_t accept_stat, uint32_t error_code, const std::string& stack_trace ) : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace ) { }
          ProtocolException( uint32_t accept_stat, uint32_t error_code, const char* stack_trace, size_t stack_trace_len ) : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace, stack_trace_len ) { }
            ProtocolException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
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
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { accept_stat = unmarshaller.readUint32( "accept_stat", 0 ); error_code = unmarshaller.readUint32( "error_code", 0 ); unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeUint32( "accept_stat", 0, accept_stat ); marshaller.writeUint32( "error_code", 0, error_code ); marshaller.writeString( "stack_trace", 0, stack_trace ); }

        protected:
          uint32_t accept_stat;
          uint32_t error_code;
          std::string stack_trace;
          };

          class RedirectException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
          {
          public:
            RedirectException() { }
          RedirectException( const std::string& to_uuid ) : to_uuid( to_uuid ) { }
          RedirectException( const char* to_uuid, size_t to_uuid_len ) : to_uuid( to_uuid, to_uuid_len ) { }
            RedirectException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~RedirectException() throw() { }

          void set_to_uuid( const std::string& to_uuid ) { set_to_uuid( to_uuid.c_str(), to_uuid.size() ); }
          void set_to_uuid( const char* to_uuid, size_t to_uuid_len ) { this->to_uuid.assign( to_uuid, to_uuid_len ); }
          const std::string& get_to_uuid() const { return to_uuid; }

            // YIELD::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new RedirectException( to_uuid); }
            virtual void throwStackClone() const { throw RedirectException( to_uuid); }
          void unmarshal( ::yidl::Unmarshaller& unmarshaller ) { unmarshaller.readString( "to_uuid", 0, to_uuid ); }
          // YIELD::Struct
          void marshal( ::yidl::Marshaller& marshaller ) const { marshaller.writeString( "to_uuid", 0, to_uuid ); }

        protected:
          std::string to_uuid;
          };



        // yidl::Object
        YIDL_OBJECT_PROTOTYPES( OSDInterface, 2009082918 );

        // YIELD::EventHandler
        virtual void handleEvent( ::YIELD::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.get_type_id() )
            {
              case 2009082928UL: handlereadRequest( static_cast<readRequest&>( ev ) ); return;
              case 2009082929UL: handletruncateRequest( static_cast<truncateRequest&>( ev ) ); return;
              case 2009082930UL: handleunlinkRequest( static_cast<unlinkRequest&>( ev ) ); return;
              case 2009082931UL: handlewriteRequest( static_cast<writeRequest&>( ev ) ); return;
              case 2009082938UL: handlextreemfs_broadcast_gmaxRequest( static_cast<xtreemfs_broadcast_gmaxRequest&>( ev ) ); return;
              case 2009082939UL: handlextreemfs_check_objectRequest( static_cast<xtreemfs_check_objectRequest&>( ev ) ); return;
              case 2009082948UL: handlextreemfs_cleanup_get_resultsRequest( static_cast<xtreemfs_cleanup_get_resultsRequest&>( ev ) ); return;
              case 2009082949UL: handlextreemfs_cleanup_is_runningRequest( static_cast<xtreemfs_cleanup_is_runningRequest&>( ev ) ); return;
              case 2009082950UL: handlextreemfs_cleanup_startRequest( static_cast<xtreemfs_cleanup_startRequest&>( ev ) ); return;
              case 2009082951UL: handlextreemfs_cleanup_statusRequest( static_cast<xtreemfs_cleanup_statusRequest&>( ev ) ); return;
              case 2009082952UL: handlextreemfs_cleanup_stopRequest( static_cast<xtreemfs_cleanup_stopRequest&>( ev ) ); return;
              case 2009082958UL: handlextreemfs_internal_get_gmaxRequest( static_cast<xtreemfs_internal_get_gmaxRequest&>( ev ) ); return;
              case 2009082959UL: handlextreemfs_internal_truncateRequest( static_cast<xtreemfs_internal_truncateRequest&>( ev ) ); return;
              case 2009082960UL: handlextreemfs_internal_get_file_sizeRequest( static_cast<xtreemfs_internal_get_file_sizeRequest&>( ev ) ); return;
              case 2009082961UL: handlextreemfs_internal_read_localRequest( static_cast<xtreemfs_internal_read_localRequest&>( ev ) ); return;
              case 2009082962UL: handlextreemfs_internal_get_object_setRequest( static_cast<xtreemfs_internal_get_object_setRequest&>( ev ) ); return;
              case 2009082968UL: handlextreemfs_lock_acquireRequest( static_cast<xtreemfs_lock_acquireRequest&>( ev ) ); return;
              case 2009082969UL: handlextreemfs_lock_checkRequest( static_cast<xtreemfs_lock_checkRequest&>( ev ) ); return;
              case 2009082970UL: handlextreemfs_lock_releaseRequest( static_cast<xtreemfs_lock_releaseRequest&>( ev ) ); return;
              case 2009082978UL: handlextreemfs_pingRequest( static_cast<xtreemfs_pingRequest&>( ev ) ); return;
              case 2009082988UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
              default: handleUnknownEvent( ev ); return;
            }
          }
          catch( ::YIELD::ExceptionResponse* exception_response )
          {
            static_cast< ::YIELD::Request& >( ev ).respond( *exception_response );
          }
          catch ( ::YIELD::ExceptionResponse& exception_response )
          {
            static_cast< ::YIELD::Request& >( ev ).respond( *exception_response.clone() );
          }
          catch ( ::YIELD::Exception& exc )
          {
            static_cast< ::YIELD::Request& >( ev ).respond( *( new ::YIELD::ExceptionResponse( exc ) ) );
          }

          ::yidl::Object::decRef( ev );
        }


        // YIELD::Interface
          virtual ::YIELD::Request* checkRequest( Object& request )
          {
            switch ( request.get_type_id() )
            {
              case 2009082928: return static_cast<readRequest*>( &request );
              case 2009082929: return static_cast<truncateRequest*>( &request );
              case 2009082930: return static_cast<unlinkRequest*>( &request );
              case 2009082931: return static_cast<writeRequest*>( &request );
              case 2009082938: return static_cast<xtreemfs_broadcast_gmaxRequest*>( &request );
              case 2009082939: return static_cast<xtreemfs_check_objectRequest*>( &request );
              case 2009082948: return static_cast<xtreemfs_cleanup_get_resultsRequest*>( &request );
              case 2009082949: return static_cast<xtreemfs_cleanup_is_runningRequest*>( &request );
              case 2009082950: return static_cast<xtreemfs_cleanup_startRequest*>( &request );
              case 2009082951: return static_cast<xtreemfs_cleanup_statusRequest*>( &request );
              case 2009082952: return static_cast<xtreemfs_cleanup_stopRequest*>( &request );
              case 2009082958: return static_cast<xtreemfs_internal_get_gmaxRequest*>( &request );
              case 2009082959: return static_cast<xtreemfs_internal_truncateRequest*>( &request );
              case 2009082960: return static_cast<xtreemfs_internal_get_file_sizeRequest*>( &request );
              case 2009082961: return static_cast<xtreemfs_internal_read_localRequest*>( &request );
              case 2009082962: return static_cast<xtreemfs_internal_get_object_setRequest*>( &request );
              case 2009082968: return static_cast<xtreemfs_lock_acquireRequest*>( &request );
              case 2009082969: return static_cast<xtreemfs_lock_checkRequest*>( &request );
              case 2009082970: return static_cast<xtreemfs_lock_releaseRequest*>( &request );
              case 2009082978: return static_cast<xtreemfs_pingRequest*>( &request );
              case 2009082988: return static_cast<xtreemfs_shutdownRequest*>( &request );
              default: return NULL;
            }
          }

          virtual ::YIELD::Response* checkResponse( Object& response )
          {
            switch ( response.get_type_id() )
            {
              case 2009082928: return static_cast<readResponse*>( &response );
              case 2009082929: return static_cast<truncateResponse*>( &response );
              case 2009082930: return static_cast<unlinkResponse*>( &response );
              case 2009082931: return static_cast<writeResponse*>( &response );
              case 2009082938: return static_cast<xtreemfs_broadcast_gmaxResponse*>( &response );
              case 2009082939: return static_cast<xtreemfs_check_objectResponse*>( &response );
              case 2009082948: return static_cast<xtreemfs_cleanup_get_resultsResponse*>( &response );
              case 2009082949: return static_cast<xtreemfs_cleanup_is_runningResponse*>( &response );
              case 2009082950: return static_cast<xtreemfs_cleanup_startResponse*>( &response );
              case 2009082951: return static_cast<xtreemfs_cleanup_statusResponse*>( &response );
              case 2009082952: return static_cast<xtreemfs_cleanup_stopResponse*>( &response );
              case 2009082958: return static_cast<xtreemfs_internal_get_gmaxResponse*>( &response );
              case 2009082959: return static_cast<xtreemfs_internal_truncateResponse*>( &response );
              case 2009082960: return static_cast<xtreemfs_internal_get_file_sizeResponse*>( &response );
              case 2009082961: return static_cast<xtreemfs_internal_read_localResponse*>( &response );
              case 2009082962: return static_cast<xtreemfs_internal_get_object_setResponse*>( &response );
              case 2009082968: return static_cast<xtreemfs_lock_acquireResponse*>( &response );
              case 2009082969: return static_cast<xtreemfs_lock_checkResponse*>( &response );
              case 2009082970: return static_cast<xtreemfs_lock_releaseResponse*>( &response );
              case 2009082978: return static_cast<xtreemfs_pingResponse*>( &response );
              case 2009082988: return static_cast<xtreemfs_shutdownResponse*>( &response );
              case 2009082919: return static_cast<ConcurrentModificationException*>( &response );
              case 2009082920: return static_cast<errnoException*>( &response );
              case 2009082921: return static_cast<InvalidArgumentException*>( &response );
              case 2009082922: return static_cast<OSDException*>( &response );
              case 2009082923: return static_cast<ProtocolException*>( &response );
              case 2009082924: return static_cast<RedirectException*>( &response );
              default: return NULL;
            }
          }

          virtual ::yidl::auto_Object< ::YIELD::Request > createRequest( uint32_t tag )
          {
            switch ( tag )
            {
              case 2009082928: return new readRequest;
              case 2009082929: return new truncateRequest;
              case 2009082930: return new unlinkRequest;
              case 2009082931: return new writeRequest;
              case 2009082938: return new xtreemfs_broadcast_gmaxRequest;
              case 2009082939: return new xtreemfs_check_objectRequest;
              case 2009082948: return new xtreemfs_cleanup_get_resultsRequest;
              case 2009082949: return new xtreemfs_cleanup_is_runningRequest;
              case 2009082950: return new xtreemfs_cleanup_startRequest;
              case 2009082951: return new xtreemfs_cleanup_statusRequest;
              case 2009082952: return new xtreemfs_cleanup_stopRequest;
              case 2009082958: return new xtreemfs_internal_get_gmaxRequest;
              case 2009082959: return new xtreemfs_internal_truncateRequest;
              case 2009082960: return new xtreemfs_internal_get_file_sizeRequest;
              case 2009082961: return new xtreemfs_internal_read_localRequest;
              case 2009082962: return new xtreemfs_internal_get_object_setRequest;
              case 2009082968: return new xtreemfs_lock_acquireRequest;
              case 2009082969: return new xtreemfs_lock_checkRequest;
              case 2009082970: return new xtreemfs_lock_releaseRequest;
              case 2009082978: return new xtreemfs_pingRequest;
              case 2009082988: return new xtreemfs_shutdownRequest;
              default: return NULL;
            }
          }

          virtual ::yidl::auto_Object< ::YIELD::Response > createResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 2009082928: return new readResponse;
              case 2009082929: return new truncateResponse;
              case 2009082930: return new unlinkResponse;
              case 2009082931: return new writeResponse;
              case 2009082938: return new xtreemfs_broadcast_gmaxResponse;
              case 2009082939: return new xtreemfs_check_objectResponse;
              case 2009082948: return new xtreemfs_cleanup_get_resultsResponse;
              case 2009082949: return new xtreemfs_cleanup_is_runningResponse;
              case 2009082950: return new xtreemfs_cleanup_startResponse;
              case 2009082951: return new xtreemfs_cleanup_statusResponse;
              case 2009082952: return new xtreemfs_cleanup_stopResponse;
              case 2009082958: return new xtreemfs_internal_get_gmaxResponse;
              case 2009082959: return new xtreemfs_internal_truncateResponse;
              case 2009082960: return new xtreemfs_internal_get_file_sizeResponse;
              case 2009082961: return new xtreemfs_internal_read_localResponse;
              case 2009082962: return new xtreemfs_internal_get_object_setResponse;
              case 2009082968: return new xtreemfs_lock_acquireResponse;
              case 2009082969: return new xtreemfs_lock_checkResponse;
              case 2009082970: return new xtreemfs_lock_releaseResponse;
              case 2009082978: return new xtreemfs_pingResponse;
              case 2009082988: return new xtreemfs_shutdownResponse;
              default: return NULL;
            }
          }

          virtual ::yidl::auto_Object< ::YIELD::ExceptionResponse > createExceptionResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 2009082919: return new ConcurrentModificationException;
              case 2009082920: return new errnoException;
              case 2009082921: return new InvalidArgumentException;
              case 2009082922: return new OSDException;
              case 2009082923: return new ProtocolException;
              case 2009082924: return new RedirectException;
              default: return NULL;
            }
          }



      protected:
        virtual void handlereadRequest( readRequest& req ) { ::yidl::auto_Object<readResponse> resp( new readResponse ); org::xtreemfs::interfaces::ObjectData object_data; _read( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_length(), object_data ); resp->set_object_data( object_data ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handletruncateRequest( truncateRequest& req ) { ::yidl::auto_Object<truncateResponse> resp( new truncateResponse ); org::xtreemfs::interfaces::OSDWriteResponse osd_write_response; _truncate( req.get_file_credentials(), req.get_file_id(), req.get_new_file_size(), osd_write_response ); resp->set_osd_write_response( osd_write_response ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handleunlinkRequest( unlinkRequest& req ) { ::yidl::auto_Object<unlinkResponse> resp( new unlinkResponse ); _unlink( req.get_file_credentials(), req.get_file_id() ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlewriteRequest( writeRequest& req ) { ::yidl::auto_Object<writeResponse> resp( new writeResponse ); org::xtreemfs::interfaces::OSDWriteResponse osd_write_response; _write( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_lease_timeout(), req.get_object_data(), osd_write_response ); resp->set_osd_write_response( osd_write_response ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_broadcast_gmaxRequest( xtreemfs_broadcast_gmaxRequest& req ) { ::yidl::auto_Object<xtreemfs_broadcast_gmaxResponse> resp( new xtreemfs_broadcast_gmaxResponse ); _xtreemfs_broadcast_gmax( req.get_file_id(), req.get_truncateEpoch(), req.get_lastObject(), req.get_fileSize() ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_check_objectRequest( xtreemfs_check_objectRequest& req ) { ::yidl::auto_Object<xtreemfs_check_objectResponse> resp( new xtreemfs_check_objectResponse ); org::xtreemfs::interfaces::ObjectData _return_value = _xtreemfs_check_object( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_get_resultsRequest( xtreemfs_cleanup_get_resultsRequest& req ) { ::yidl::auto_Object<xtreemfs_cleanup_get_resultsResponse> resp( new xtreemfs_cleanup_get_resultsResponse ); org::xtreemfs::interfaces::StringSet results; _xtreemfs_cleanup_get_results( results ); resp->set_results( results ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_is_runningRequest( xtreemfs_cleanup_is_runningRequest& req ) { ::yidl::auto_Object<xtreemfs_cleanup_is_runningResponse> resp( new xtreemfs_cleanup_is_runningResponse ); bool is_running; _xtreemfs_cleanup_is_running( is_running ); resp->set_is_running( is_running ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_startRequest( xtreemfs_cleanup_startRequest& req ) { ::yidl::auto_Object<xtreemfs_cleanup_startResponse> resp( new xtreemfs_cleanup_startResponse ); _xtreemfs_cleanup_start( req.get_remove_zombies(), req.get_remove_unavail_volume(), req.get_lost_and_found() ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_statusRequest( xtreemfs_cleanup_statusRequest& req ) { ::yidl::auto_Object<xtreemfs_cleanup_statusResponse> resp( new xtreemfs_cleanup_statusResponse ); std::string status; _xtreemfs_cleanup_status( status ); resp->set_status( status ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_stopRequest( xtreemfs_cleanup_stopRequest& req ) { ::yidl::auto_Object<xtreemfs_cleanup_stopResponse> resp( new xtreemfs_cleanup_stopResponse ); _xtreemfs_cleanup_stop(); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_internal_get_gmaxRequest( xtreemfs_internal_get_gmaxRequest& req ) { ::yidl::auto_Object<xtreemfs_internal_get_gmaxResponse> resp( new xtreemfs_internal_get_gmaxResponse ); org::xtreemfs::interfaces::InternalGmax _return_value = _xtreemfs_internal_get_gmax( req.get_file_credentials(), req.get_file_id() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_internal_truncateRequest( xtreemfs_internal_truncateRequest& req ) { ::yidl::auto_Object<xtreemfs_internal_truncateResponse> resp( new xtreemfs_internal_truncateResponse ); org::xtreemfs::interfaces::OSDWriteResponse osd_write_response; _xtreemfs_internal_truncate( req.get_file_credentials(), req.get_file_id(), req.get_new_file_size(), osd_write_response ); resp->set_osd_write_response( osd_write_response ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_internal_get_file_sizeRequest( xtreemfs_internal_get_file_sizeRequest& req ) { ::yidl::auto_Object<xtreemfs_internal_get_file_sizeResponse> resp( new xtreemfs_internal_get_file_sizeResponse ); uint64_t _return_value = _xtreemfs_internal_get_file_size( req.get_file_credentials(), req.get_file_id() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_internal_read_localRequest( xtreemfs_internal_read_localRequest& req ) { ::yidl::auto_Object<xtreemfs_internal_read_localResponse> resp( new xtreemfs_internal_read_localResponse ); org::xtreemfs::interfaces::InternalReadLocalResponse _return_value = _xtreemfs_internal_read_local( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_length(), req.get_attachObjectList(), req.get_requiredObjects() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_internal_get_object_setRequest( xtreemfs_internal_get_object_setRequest& req ) { ::yidl::auto_Object<xtreemfs_internal_get_object_setResponse> resp( new xtreemfs_internal_get_object_setResponse ); org::xtreemfs::interfaces::ObjectList _return_value = _xtreemfs_internal_get_object_set( req.get_file_credentials(), req.get_file_id() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_lock_acquireRequest( xtreemfs_lock_acquireRequest& req ) { ::yidl::auto_Object<xtreemfs_lock_acquireResponse> resp( new xtreemfs_lock_acquireResponse ); org::xtreemfs::interfaces::Lock _return_value = _xtreemfs_lock_acquire( req.get_file_credentials(), req.get_client_uuid(), req.get_client_pid(), req.get_file_id(), req.get_offset(), req.get_length(), req.get_exclusive() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_lock_checkRequest( xtreemfs_lock_checkRequest& req ) { ::yidl::auto_Object<xtreemfs_lock_checkResponse> resp( new xtreemfs_lock_checkResponse ); org::xtreemfs::interfaces::Lock _return_value = _xtreemfs_lock_check( req.get_file_credentials(), req.get_client_uuid(), req.get_client_pid(), req.get_file_id(), req.get_offset(), req.get_length(), req.get_exclusive() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_lock_releaseRequest( xtreemfs_lock_releaseRequest& req ) { ::yidl::auto_Object<xtreemfs_lock_releaseResponse> resp( new xtreemfs_lock_releaseResponse ); _xtreemfs_lock_release( req.get_file_credentials(), req.get_file_id(), req.get_lock() ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_pingRequest( xtreemfs_pingRequest& req ) { ::yidl::auto_Object<xtreemfs_pingResponse> resp( new xtreemfs_pingResponse ); org::xtreemfs::interfaces::VivaldiCoordinates remote_coordinates; _xtreemfs_ping( req.get_coordinates(), remote_coordinates ); resp->set_remote_coordinates( remote_coordinates ); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req ) { ::yidl::auto_Object<xtreemfs_shutdownResponse> resp( new xtreemfs_shutdownResponse ); _xtreemfs_shutdown(); req.respond( *resp.release() ); ::yidl::Object::decRef( req ); }

      virtual void _read( const org::xtreemfs::interfaces::FileCredentials& , const std::string& , uint64_t, uint64_t, uint32_t, uint32_t, org::xtreemfs::interfaces::ObjectData&  ) { }
        virtual void _truncate( const org::xtreemfs::interfaces::FileCredentials& , const std::string& , uint64_t, org::xtreemfs::interfaces::OSDWriteResponse&  ) { }
        virtual void _unlink( const org::xtreemfs::interfaces::FileCredentials& , const std::string&  ) { }
        virtual void _write( const org::xtreemfs::interfaces::FileCredentials& , const std::string& , uint64_t, uint64_t, uint32_t, uint64_t, const org::xtreemfs::interfaces::ObjectData& , org::xtreemfs::interfaces::OSDWriteResponse&  ) { }
        virtual void _xtreemfs_broadcast_gmax( const std::string& , uint64_t, uint64_t, uint64_t ) { }
        virtual org::xtreemfs::interfaces::ObjectData _xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& , const std::string& , uint64_t, uint64_t ) { return org::xtreemfs::interfaces::ObjectData(); }
        virtual void _xtreemfs_cleanup_get_results( org::xtreemfs::interfaces::StringSet&  ) { }
        virtual void _xtreemfs_cleanup_is_running( bool&  ) { }
        virtual void _xtreemfs_cleanup_start( bool, bool, bool ) { }
        virtual void _xtreemfs_cleanup_status( std::string&  ) { }
        virtual void _xtreemfs_cleanup_stop() { }
        virtual org::xtreemfs::interfaces::InternalGmax _xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& , const std::string&  ) { return org::xtreemfs::interfaces::InternalGmax(); }
        virtual void _xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& , const std::string& , uint64_t, org::xtreemfs::interfaces::OSDWriteResponse&  ) { }
        virtual uint64_t _xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& , const std::string&  ) { return 0; }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse _xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& , const std::string& , uint64_t, uint64_t, uint64_t, uint64_t, bool, const org::xtreemfs::interfaces::ObjectListSet&  ) { return org::xtreemfs::interfaces::InternalReadLocalResponse(); }
        virtual org::xtreemfs::interfaces::ObjectList _xtreemfs_internal_get_object_set( const org::xtreemfs::interfaces::FileCredentials& , const std::string&  ) { return org::xtreemfs::interfaces::ObjectList(); }
        virtual org::xtreemfs::interfaces::Lock _xtreemfs_lock_acquire( const org::xtreemfs::interfaces::FileCredentials& , const std::string& , int32_t, const std::string& , uint64_t, uint64_t, bool ) { return org::xtreemfs::interfaces::Lock(); }
        virtual org::xtreemfs::interfaces::Lock _xtreemfs_lock_check( const org::xtreemfs::interfaces::FileCredentials& , const std::string& , int32_t, const std::string& , uint64_t, uint64_t, bool ) { return org::xtreemfs::interfaces::Lock(); }
        virtual void _xtreemfs_lock_release( const org::xtreemfs::interfaces::FileCredentials& , const std::string& , const org::xtreemfs::interfaces::Lock&  ) { }
        virtual void _xtreemfs_ping( const org::xtreemfs::interfaces::VivaldiCoordinates& , org::xtreemfs::interfaces::VivaldiCoordinates&  ) { }
        virtual void _xtreemfs_shutdown() { }
      };

      // Use this macro in an implementation class to get all of the prototypes for the operations in OSDInterface
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PROTOTYPES \
      virtual void _read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data );\
      virtual void _truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );\
      virtual void _unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual void _write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );\
      virtual void _xtreemfs_broadcast_gmax( const std::string& file_id, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize );\
      virtual org::xtreemfs::interfaces::ObjectData _xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version );\
      virtual void _xtreemfs_cleanup_get_results( org::xtreemfs::interfaces::StringSet& results );\
      virtual void _xtreemfs_cleanup_is_running( bool& is_running );\
      virtual void _xtreemfs_cleanup_start( bool remove_zombies, bool remove_unavail_volume, bool lost_and_found );\
      virtual void _xtreemfs_cleanup_status( std::string& status );\
      virtual void _xtreemfs_cleanup_stop();\
      virtual org::xtreemfs::interfaces::InternalGmax _xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual void _xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );\
      virtual uint64_t _xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual org::xtreemfs::interfaces::InternalReadLocalResponse _xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, bool attachObjectList, const org::xtreemfs::interfaces::ObjectListSet& requiredObjects );\
      virtual org::xtreemfs::interfaces::ObjectList _xtreemfs_internal_get_object_set( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual org::xtreemfs::interfaces::Lock _xtreemfs_lock_acquire( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& client_uuid, int32_t client_pid, const std::string& file_id, uint64_t offset, uint64_t length, bool exclusive );\
      virtual org::xtreemfs::interfaces::Lock _xtreemfs_lock_check( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& client_uuid, int32_t client_pid, const std::string& file_id, uint64_t offset, uint64_t length, bool exclusive );\
      virtual void _xtreemfs_lock_release( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, const org::xtreemfs::interfaces::Lock& lock );\
      virtual void _xtreemfs_ping( const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates, org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates );\
      virtual void _xtreemfs_shutdown();

      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handlereadRequest( readRequest& req );\
      virtual void handletruncateRequest( truncateRequest& req );\
      virtual void handleunlinkRequest( unlinkRequest& req );\
      virtual void handlewriteRequest( writeRequest& req );\
      virtual void handlextreemfs_broadcast_gmaxRequest( xtreemfs_broadcast_gmaxRequest& req );\
      virtual void handlextreemfs_check_objectRequest( xtreemfs_check_objectRequest& req );\
      virtual void handlextreemfs_cleanup_get_resultsRequest( xtreemfs_cleanup_get_resultsRequest& req );\
      virtual void handlextreemfs_cleanup_is_runningRequest( xtreemfs_cleanup_is_runningRequest& req );\
      virtual void handlextreemfs_cleanup_startRequest( xtreemfs_cleanup_startRequest& req );\
      virtual void handlextreemfs_cleanup_statusRequest( xtreemfs_cleanup_statusRequest& req );\
      virtual void handlextreemfs_cleanup_stopRequest( xtreemfs_cleanup_stopRequest& req );\
      virtual void handlextreemfs_internal_get_gmaxRequest( xtreemfs_internal_get_gmaxRequest& req );\
      virtual void handlextreemfs_internal_truncateRequest( xtreemfs_internal_truncateRequest& req );\
      virtual void handlextreemfs_internal_get_file_sizeRequest( xtreemfs_internal_get_file_sizeRequest& req );\
      virtual void handlextreemfs_internal_read_localRequest( xtreemfs_internal_read_localRequest& req );\
      virtual void handlextreemfs_internal_get_object_setRequest( xtreemfs_internal_get_object_setRequest& req );\
      virtual void handlextreemfs_lock_acquireRequest( xtreemfs_lock_acquireRequest& req );\
      virtual void handlextreemfs_lock_checkRequest( xtreemfs_lock_checkRequest& req );\
      virtual void handlextreemfs_lock_releaseRequest( xtreemfs_lock_releaseRequest& req );\
      virtual void handlextreemfs_pingRequest( xtreemfs_pingRequest& req );\
      virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req );

    };



  };



};
#endif
