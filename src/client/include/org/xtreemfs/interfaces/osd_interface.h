// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_INTERFACES_OSD_INTERFACE_H_
#define _ORG_XTREEMFS_INTERFACES_OSD_INTERFACE_H_

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

      class InternalGmax : public YIELD::Object
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

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( InternalGmax, 1050 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { epoch = input_stream.readUint64( YIELD::StructuredStream::Declaration( "epoch" ) ); last_object_id = input_stream.readUint64( YIELD::StructuredStream::Declaration( "last_object_id" ) ); file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "file_size" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "epoch" ), epoch ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "last_object_id" ), last_object_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "file_size" ), file_size ); }

      protected:
        uint64_t epoch;
        uint64_t last_object_id;
        uint64_t file_size;
      };

      class ObjectData : public YIELD::Object
      {
      public:
        ObjectData() : checksum( 0 ), zero_padding( 0 ), invalid_checksum_on_osd( false ) { }
        ObjectData( YIELD::auto_Object<YIELD::String> data, uint32_t checksum, uint32_t zero_padding, bool invalid_checksum_on_osd ) : data( data ), checksum( checksum ), zero_padding( zero_padding ), invalid_checksum_on_osd( invalid_checksum_on_osd ) { }
        virtual ~ObjectData() { }

        void set_data( YIELD::String* data ) { this->data.reset( data ); }
        void set_data( YIELD::auto_Object<YIELD::String> data ) { this->data = data; }
        YIELD::auto_Object<YIELD::String> get_data() const { return data; }
        void set_checksum( uint32_t checksum ) { this->checksum = checksum; }
        uint32_t get_checksum() const { return checksum; }
        void set_zero_padding( uint32_t zero_padding ) { this->zero_padding = zero_padding; }
        uint32_t get_zero_padding() const { return zero_padding; }
        void set_invalid_checksum_on_osd( bool invalid_checksum_on_osd ) { this->invalid_checksum_on_osd = invalid_checksum_on_osd; }
        bool get_invalid_checksum_on_osd() const { return invalid_checksum_on_osd; }

        bool operator==( const ObjectData& other ) const { return data.get() == other.data.get() && checksum == other.checksum && zero_padding == other.zero_padding && invalid_checksum_on_osd == other.invalid_checksum_on_osd; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( ObjectData, 1051 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { if ( data == NULL ) data = new YIELD::String; input_stream.readString( YIELD::StructuredStream::Declaration( "data" ), *data ); checksum = input_stream.readUint32( YIELD::StructuredStream::Declaration( "checksum" ) ); zero_padding = input_stream.readUint32( YIELD::StructuredStream::Declaration( "zero_padding" ) ); invalid_checksum_on_osd = input_stream.readBool( YIELD::StructuredStream::Declaration( "invalid_checksum_on_osd" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { if ( data.get() ) output_stream.writeString( YIELD::StructuredStream::Declaration( "data" ), *data ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "checksum" ), checksum ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "zero_padding" ), zero_padding ); output_stream.writeBool( YIELD::StructuredStream::Declaration( "invalid_checksum_on_osd" ), invalid_checksum_on_osd ); }

      protected:
        YIELD::auto_Object<YIELD::String> data;
        uint32_t checksum;
        uint32_t zero_padding;
        bool invalid_checksum_on_osd;
      };

      class InternalReadLocalResponse : public YIELD::Object
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

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( InternalReadLocalResponse, 1052 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "new_file_size" ), &new_file_size ); zero_padding = input_stream.readUint32( YIELD::StructuredStream::Declaration( "zero_padding" ) ); input_stream.readStruct( YIELD::StructuredStream::Declaration( "data" ), &data ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "new_file_size" ), new_file_size ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "zero_padding" ), zero_padding ); output_stream.writeStruct( YIELD::StructuredStream::Declaration( "data" ), data ); }

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
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS YIELD::Interface
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

      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_EXCEPTION_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS YIELD::ExceptionResponse
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
        virtual void read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data, uint64_t response_timeout_ns ) { YIELD::auto_Object<readRequest> __request( new readRequest( file_credentials, file_id, object_number, object_version, offset, length ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<readResponse> __response = __response_queue->dequeue_typed<readResponse>( response_timeout_ns ); object_data = __response->get_object_data(); }

        virtual void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { truncate( file_credentials, file_id, new_file_size, osd_write_response, static_cast<uint64_t>( -1 ) ); }
        virtual void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, uint64_t response_timeout_ns ) { YIELD::auto_Object<truncateRequest> __request( new truncateRequest( file_credentials, file_id, new_file_size ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<truncateResponse> __response = __response_queue->dequeue_typed<truncateResponse>( response_timeout_ns ); osd_write_response = __response->get_osd_write_response(); }

        virtual void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { unlink( file_credentials, file_id, static_cast<uint64_t>( -1 ) ); }
        virtual void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t response_timeout_ns ) { YIELD::auto_Object<unlinkRequest> __request( new unlinkRequest( file_credentials, file_id ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<unlinkResponse> __response = __response_queue->dequeue_typed<unlinkResponse>( response_timeout_ns ); }

        virtual void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { write( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data, osd_write_response, static_cast<uint64_t>( -1 ) ); }
        virtual void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, uint64_t response_timeout_ns ) { YIELD::auto_Object<writeRequest> __request( new writeRequest( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<writeResponse> __response = __response_queue->dequeue_typed<writeResponse>( response_timeout_ns ); osd_write_response = __response->get_osd_write_response(); }

        virtual void xtreemfs_broadcast_gmax( const std::string& fileId, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize ) { xtreemfs_broadcast_gmax( fileId, truncateEpoch, lastObject, fileSize, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_broadcast_gmax( const std::string& fileId, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_broadcast_gmaxRequest> __request( new xtreemfs_broadcast_gmaxRequest( fileId, truncateEpoch, lastObject, fileSize ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_broadcast_gmaxResponse> __response = __response_queue->dequeue_typed<xtreemfs_broadcast_gmaxResponse>( response_timeout_ns ); }

        virtual org::xtreemfs::interfaces::ObjectData xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version ) { return xtreemfs_check_object( file_credentials, file_id, object_number, object_version, static_cast<uint64_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::ObjectData xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_check_objectRequest> __request( new xtreemfs_check_objectRequest( file_credentials, file_id, object_number, object_version ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_check_objectResponse> __response = __response_queue->dequeue_typed<xtreemfs_check_objectResponse>( response_timeout_ns ); org::xtreemfs::interfaces::ObjectData _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_cleanup_get_results( org::xtreemfs::interfaces::StringSet& results ) { xtreemfs_cleanup_get_results( results, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_get_results( org::xtreemfs::interfaces::StringSet& results, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_cleanup_get_resultsRequest> __request( new xtreemfs_cleanup_get_resultsRequest() ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_cleanup_get_resultsResponse> __response = __response_queue->dequeue_typed<xtreemfs_cleanup_get_resultsResponse>( response_timeout_ns ); results = __response->get_results(); }

        virtual void xtreemfs_cleanup_is_running( bool& is_running ) { xtreemfs_cleanup_is_running( is_running, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_is_running( bool& is_running, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_cleanup_is_runningRequest> __request( new xtreemfs_cleanup_is_runningRequest() ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_cleanup_is_runningResponse> __response = __response_queue->dequeue_typed<xtreemfs_cleanup_is_runningResponse>( response_timeout_ns ); is_running = __response->get_is_running(); }

        virtual void xtreemfs_cleanup_start( bool remove_zombies, bool remove_unavail_volume, bool lost_and_found ) { xtreemfs_cleanup_start( remove_zombies, remove_unavail_volume, lost_and_found, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_start( bool remove_zombies, bool remove_unavail_volume, bool lost_and_found, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_cleanup_startRequest> __request( new xtreemfs_cleanup_startRequest( remove_zombies, remove_unavail_volume, lost_and_found ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_cleanup_startResponse> __response = __response_queue->dequeue_typed<xtreemfs_cleanup_startResponse>( response_timeout_ns ); }

        virtual void xtreemfs_cleanup_status( std::string& status ) { xtreemfs_cleanup_status( status, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_status( std::string& status, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_cleanup_statusRequest> __request( new xtreemfs_cleanup_statusRequest() ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_cleanup_statusResponse> __response = __response_queue->dequeue_typed<xtreemfs_cleanup_statusResponse>( response_timeout_ns ); status = __response->get_status(); }

        virtual void xtreemfs_cleanup_stop() { xtreemfs_cleanup_stop( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_cleanup_stop( uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_cleanup_stopRequest> __request( new xtreemfs_cleanup_stopRequest() ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_cleanup_stopResponse> __response = __response_queue->dequeue_typed<xtreemfs_cleanup_stopResponse>( response_timeout_ns ); }

        virtual org::xtreemfs::interfaces::InternalGmax xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return xtreemfs_internal_get_gmax( file_credentials, file_id, static_cast<uint64_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalGmax xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_internal_get_gmaxRequest> __request( new xtreemfs_internal_get_gmaxRequest( file_credentials, file_id ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_internal_get_gmaxResponse> __response = __response_queue->dequeue_typed<xtreemfs_internal_get_gmaxResponse>( response_timeout_ns ); org::xtreemfs::interfaces::InternalGmax _return_value = __response->get__return_value(); return _return_value; }

        virtual uint64_t xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return xtreemfs_internal_get_file_size( file_credentials, file_id, static_cast<uint64_t>( -1 ) ); }
        virtual uint64_t xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_internal_get_file_sizeRequest> __request( new xtreemfs_internal_get_file_sizeRequest( file_credentials, file_id ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_internal_get_file_sizeResponse> __response = __response_queue->dequeue_typed<xtreemfs_internal_get_file_sizeResponse>( response_timeout_ns ); uint64_t _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { xtreemfs_internal_truncate( file_credentials, file_id, new_file_size, osd_write_response, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_internal_truncateRequest> __request( new xtreemfs_internal_truncateRequest( file_credentials, file_id, new_file_size ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_internal_truncateResponse> __response = __response_queue->dequeue_typed<xtreemfs_internal_truncateResponse>( response_timeout_ns ); osd_write_response = __response->get_osd_write_response(); }

        virtual org::xtreemfs::interfaces::InternalReadLocalResponse xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) { return xtreemfs_internal_read_local( file_credentials, file_id, object_number, object_version, offset, length, static_cast<uint64_t>( -1 ) ); }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_internal_read_localRequest> __request( new xtreemfs_internal_read_localRequest( file_credentials, file_id, object_number, object_version, offset, length ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_internal_read_localResponse> __response = __response_queue->dequeue_typed<xtreemfs_internal_read_localResponse>( response_timeout_ns ); org::xtreemfs::interfaces::InternalReadLocalResponse _return_value = __response->get__return_value(); return _return_value; }

        virtual void xtreemfs_shutdown() { xtreemfs_shutdown( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_shutdownRequest> __request( new xtreemfs_shutdownRequest() ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_shutdownResponse> __response = __response_queue->dequeue_typed<xtreemfs_shutdownResponse>( response_timeout_ns ); }

        virtual void xtreemfs_ping( const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates, org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates ) { xtreemfs_ping( coordinates, remote_coordinates, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_ping( const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates, org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_pingRequest> __request( new xtreemfs_pingRequest( coordinates ) ); YIELD::auto_Object< YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > > __response_queue = new YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > >; __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); YIELD::auto_Object<xtreemfs_pingResponse> __response = __response_queue->dequeue_typed<xtreemfs_pingResponse>( response_timeout_ns ); remote_coordinates = __response->get_remote_coordinates(); }


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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( readResponse, 1301 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "object_data" ), &object_data ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "object_data" ), object_data ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( readRequest, 1301 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); offset = input_stream.readUint32( YIELD::StructuredStream::Declaration( "offset" ) ); length = input_stream.readUint32( YIELD::StructuredStream::Declaration( "length" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "offset" ), offset ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "length" ), length ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( truncateResponse, 1302 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "osd_write_response" ), &osd_write_response ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "osd_write_response" ), osd_write_response ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( truncateRequest, 1302 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); new_file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "new_file_size" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "new_file_size" ), new_file_size ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( unlinkResponse, 1303 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( unlinkRequest, 1303 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( writeResponse, 1304 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "osd_write_response" ), &osd_write_response ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "osd_write_response" ), osd_write_response ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( writeRequest, 1304 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); offset = input_stream.readUint32( YIELD::StructuredStream::Declaration( "offset" ) ); lease_timeout = input_stream.readUint64( YIELD::StructuredStream::Declaration( "lease_timeout" ) ); input_stream.readStruct( YIELD::StructuredStream::Declaration( "object_data" ), &object_data ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "offset" ), offset ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "lease_timeout" ), lease_timeout ); output_stream.writeStruct( YIELD::StructuredStream::Declaration( "object_data" ), object_data ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_broadcast_gmaxResponse, 2300 );

        };

        class xtreemfs_broadcast_gmaxRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_broadcast_gmaxRequest() : truncateEpoch( 0 ), lastObject( 0 ), fileSize( 0 ) { }
          xtreemfs_broadcast_gmaxRequest( const std::string& fileId, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize ) : fileId( fileId ), truncateEpoch( truncateEpoch ), lastObject( lastObject ), fileSize( fileSize ) { }
          xtreemfs_broadcast_gmaxRequest( const char* fileId, size_t fileId_len, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize ) : fileId( fileId, fileId_len ), truncateEpoch( truncateEpoch ), lastObject( lastObject ), fileSize( fileSize ) { }
          virtual ~xtreemfs_broadcast_gmaxRequest() { }

          void set_fileId( const std::string& fileId ) { set_fileId( fileId.c_str(), fileId.size() ); }
          void set_fileId( const char* fileId, size_t fileId_len ) { this->fileId.assign( fileId, fileId_len ); }
          const std::string& get_fileId() const { return fileId; }
          void set_truncateEpoch( uint64_t truncateEpoch ) { this->truncateEpoch = truncateEpoch; }
          uint64_t get_truncateEpoch() const { return truncateEpoch; }
          void set_lastObject( uint64_t lastObject ) { this->lastObject = lastObject; }
          uint64_t get_lastObject() const { return lastObject; }
          void set_fileSize( uint64_t fileSize ) { this->fileSize = fileSize; }
          uint64_t get_fileSize() const { return fileSize; }

          bool operator==( const xtreemfs_broadcast_gmaxRequest& other ) const { return fileId == other.fileId && truncateEpoch == other.truncateEpoch && lastObject == other.lastObject && fileSize == other.fileSize; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_broadcast_gmaxRequest, 2300 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "fileId" ), fileId ); truncateEpoch = input_stream.readUint64( YIELD::StructuredStream::Declaration( "truncateEpoch" ) ); lastObject = input_stream.readUint64( YIELD::StructuredStream::Declaration( "lastObject" ) ); fileSize = input_stream.readUint64( YIELD::StructuredStream::Declaration( "fileSize" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "fileId" ), fileId ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "truncateEpoch" ), truncateEpoch ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "lastObject" ), lastObject ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "fileSize" ), fileSize ); }

        protected:
          std::string fileId;
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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_check_objectResponse, 1403 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "_return_value" ), &_return_value ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_check_objectRequest, 1403 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_get_resultsResponse, 1409 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "results" ), &results ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "results" ), results ); }

        protected:
          org::xtreemfs::interfaces::StringSet results;
        };

        class xtreemfs_cleanup_get_resultsRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_get_resultsRequest() { }
          virtual ~xtreemfs_cleanup_get_resultsRequest() { }

          bool operator==( const xtreemfs_cleanup_get_resultsRequest& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_get_resultsRequest, 1409 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_is_runningResponse, 1408 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { is_running = input_stream.readBool( YIELD::StructuredStream::Declaration( "is_running" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeBool( YIELD::StructuredStream::Declaration( "is_running" ), is_running ); }

        protected:
          bool is_running;
        };

        class xtreemfs_cleanup_is_runningRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_is_runningRequest() { }
          virtual ~xtreemfs_cleanup_is_runningRequest() { }

          bool operator==( const xtreemfs_cleanup_is_runningRequest& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_is_runningRequest, 1408 );

        };

        class xtreemfs_cleanup_startResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_startResponse() { }
          virtual ~xtreemfs_cleanup_startResponse() { }

          bool operator==( const xtreemfs_cleanup_startResponse& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_startResponse, 1405 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_startRequest, 1405 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { remove_zombies = input_stream.readBool( YIELD::StructuredStream::Declaration( "remove_zombies" ) ); remove_unavail_volume = input_stream.readBool( YIELD::StructuredStream::Declaration( "remove_unavail_volume" ) ); lost_and_found = input_stream.readBool( YIELD::StructuredStream::Declaration( "lost_and_found" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeBool( YIELD::StructuredStream::Declaration( "remove_zombies" ), remove_zombies ); output_stream.writeBool( YIELD::StructuredStream::Declaration( "remove_unavail_volume" ), remove_unavail_volume ); output_stream.writeBool( YIELD::StructuredStream::Declaration( "lost_and_found" ), lost_and_found ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_statusResponse, 1407 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "status" ), status ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "status" ), status ); }

        protected:
          std::string status;
        };

        class xtreemfs_cleanup_statusRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_statusRequest() { }
          virtual ~xtreemfs_cleanup_statusRequest() { }

          bool operator==( const xtreemfs_cleanup_statusRequest& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_statusRequest, 1407 );

        };

        class xtreemfs_cleanup_stopResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_stopResponse() { }
          virtual ~xtreemfs_cleanup_stopResponse() { }

          bool operator==( const xtreemfs_cleanup_stopResponse& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_stopResponse, 1406 );

        };

        class xtreemfs_cleanup_stopRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_stopRequest() { }
          virtual ~xtreemfs_cleanup_stopRequest() { }

          bool operator==( const xtreemfs_cleanup_stopRequest& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_cleanup_stopRequest, 1406 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_get_gmaxResponse, 1400 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "_return_value" ), &_return_value ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_get_gmaxRequest, 1400 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_get_file_sizeResponse, 1404 );

          // YIELD::Object
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
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }

          bool operator==( const xtreemfs_internal_get_file_sizeRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_get_file_sizeRequest, 1404 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_truncateResponse, 1401 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "osd_write_response" ), &osd_write_response ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "osd_write_response" ), osd_write_response ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_truncateRequest, 1401 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); new_file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "new_file_size" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "new_file_size" ), new_file_size ); }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
          uint64_t new_file_size;
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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_read_localResponse, 1402 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "_return_value" ), &_return_value ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }

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

          bool operator==( const xtreemfs_internal_read_localRequest& other ) const { return file_credentials == other.file_credentials && file_id == other.file_id && object_number == other.object_number && object_version == other.object_version && offset == other.offset && length == other.length; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_read_localRequest, 1402 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); object_number = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_number" ) ); object_version = input_stream.readUint64( YIELD::StructuredStream::Declaration( "object_version" ) ); offset = input_stream.readUint64( YIELD::StructuredStream::Declaration( "offset" ) ); length = input_stream.readUint64( YIELD::StructuredStream::Declaration( "length" ) ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_number" ), object_number ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "object_version" ), object_version ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "offset" ), offset ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "length" ), length ); }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          std::string file_id;
          uint64_t object_number;
          uint64_t object_version;
          uint64_t offset;
          uint64_t length;
        };

        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownResponse() { }
          virtual ~xtreemfs_shutdownResponse() { }

          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 1350 );

        };

        class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownRequest() { }
          virtual ~xtreemfs_shutdownRequest() { }

          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 1350 );

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_pingResponse, 2301 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "remote_coordinates" ), &remote_coordinates ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "remote_coordinates" ), remote_coordinates ); }

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

          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( xtreemfs_pingRequest, 2301 );

          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "coordinates" ), &coordinates ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "coordinates" ), coordinates ); }

        protected:
          org::xtreemfs::interfaces::VivaldiCoordinates coordinates;
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
          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

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
          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

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
          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); }

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
          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

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
          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { accept_stat = input_stream.readUint32( YIELD::StructuredStream::Declaration( "accept_stat" ) ); error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "accept_stat" ), accept_stat ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

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
          // YIELD::Object
          void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "to_uuid" ), to_uuid ); }
          void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "to_uuid" ), to_uuid ); }

        protected:
          std::string to_uuid;
          };



        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( OSDInterface, 1300 );

        // YIELD::EventHandler
        virtual void handleEvent( YIELD::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.get_tag() )
            {
              case 1301UL: handlereadRequest( static_cast<readRequest&>( ev ) ); return;
              case 1302UL: handletruncateRequest( static_cast<truncateRequest&>( ev ) ); return;
              case 1303UL: handleunlinkRequest( static_cast<unlinkRequest&>( ev ) ); return;
              case 1304UL: handlewriteRequest( static_cast<writeRequest&>( ev ) ); return;
              case 2300UL: handlextreemfs_broadcast_gmaxRequest( static_cast<xtreemfs_broadcast_gmaxRequest&>( ev ) ); return;
              case 1403UL: handlextreemfs_check_objectRequest( static_cast<xtreemfs_check_objectRequest&>( ev ) ); return;
              case 1409UL: handlextreemfs_cleanup_get_resultsRequest( static_cast<xtreemfs_cleanup_get_resultsRequest&>( ev ) ); return;
              case 1408UL: handlextreemfs_cleanup_is_runningRequest( static_cast<xtreemfs_cleanup_is_runningRequest&>( ev ) ); return;
              case 1405UL: handlextreemfs_cleanup_startRequest( static_cast<xtreemfs_cleanup_startRequest&>( ev ) ); return;
              case 1407UL: handlextreemfs_cleanup_statusRequest( static_cast<xtreemfs_cleanup_statusRequest&>( ev ) ); return;
              case 1406UL: handlextreemfs_cleanup_stopRequest( static_cast<xtreemfs_cleanup_stopRequest&>( ev ) ); return;
              case 1400UL: handlextreemfs_internal_get_gmaxRequest( static_cast<xtreemfs_internal_get_gmaxRequest&>( ev ) ); return;
              case 1404UL: handlextreemfs_internal_get_file_sizeRequest( static_cast<xtreemfs_internal_get_file_sizeRequest&>( ev ) ); return;
              case 1401UL: handlextreemfs_internal_truncateRequest( static_cast<xtreemfs_internal_truncateRequest&>( ev ) ); return;
              case 1402UL: handlextreemfs_internal_read_localRequest( static_cast<xtreemfs_internal_read_localRequest&>( ev ) ); return;
              case 1350UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
              case 2301UL: handlextreemfs_pingRequest( static_cast<xtreemfs_pingRequest&>( ev ) ); return;
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
              case 1301: return new readRequest;
              case 1302: return new truncateRequest;
              case 1303: return new unlinkRequest;
              case 1304: return new writeRequest;
              case 2300: return new xtreemfs_broadcast_gmaxRequest;
              case 1403: return new xtreemfs_check_objectRequest;
              case 1409: return new xtreemfs_cleanup_get_resultsRequest;
              case 1408: return new xtreemfs_cleanup_is_runningRequest;
              case 1405: return new xtreemfs_cleanup_startRequest;
              case 1407: return new xtreemfs_cleanup_statusRequest;
              case 1406: return new xtreemfs_cleanup_stopRequest;
              case 1400: return new xtreemfs_internal_get_gmaxRequest;
              case 1404: return new xtreemfs_internal_get_file_sizeRequest;
              case 1401: return new xtreemfs_internal_truncateRequest;
              case 1402: return new xtreemfs_internal_read_localRequest;
              case 1350: return new xtreemfs_shutdownRequest;
              case 2301: return new xtreemfs_pingRequest;
              default: return NULL;
            }
          }

          virtual YIELD::auto_Object<YIELD::Response> createResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 1301: return new readResponse;
              case 1302: return new truncateResponse;
              case 1303: return new unlinkResponse;
              case 1304: return new writeResponse;
              case 2300: return new xtreemfs_broadcast_gmaxResponse;
              case 1403: return new xtreemfs_check_objectResponse;
              case 1409: return new xtreemfs_cleanup_get_resultsResponse;
              case 1408: return new xtreemfs_cleanup_is_runningResponse;
              case 1405: return new xtreemfs_cleanup_startResponse;
              case 1407: return new xtreemfs_cleanup_statusResponse;
              case 1406: return new xtreemfs_cleanup_stopResponse;
              case 1400: return new xtreemfs_internal_get_gmaxResponse;
              case 1404: return new xtreemfs_internal_get_file_sizeResponse;
              case 1401: return new xtreemfs_internal_truncateResponse;
              case 1402: return new xtreemfs_internal_read_localResponse;
              case 1350: return new xtreemfs_shutdownResponse;
              case 2301: return new xtreemfs_pingResponse;
              default: return NULL;
            }
          }

          virtual YIELD::auto_Object<YIELD::ExceptionResponse> createExceptionResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 1306: return new ConcurrentModificationException;
              case 1307: return new errnoException;
              case 1308: return new InvalidArgumentException;
              case 1311: return new OSDException;
              case 1309: return new ProtocolException;
              case 1310: return new RedirectException;
              default: return NULL;
            }
          }



      protected:
        virtual void handlereadRequest( readRequest& req ) { YIELD::auto_Object<readResponse> resp( new readResponse ); org::xtreemfs::interfaces::ObjectData object_data; _read( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_length(), object_data ); resp->set_object_data( object_data ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handletruncateRequest( truncateRequest& req ) { YIELD::auto_Object<truncateResponse> resp( new truncateResponse ); org::xtreemfs::interfaces::OSDWriteResponse osd_write_response; _truncate( req.get_file_credentials(), req.get_file_id(), req.get_new_file_size(), osd_write_response ); resp->set_osd_write_response( osd_write_response ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handleunlinkRequest( unlinkRequest& req ) { YIELD::auto_Object<unlinkResponse> resp( new unlinkResponse ); _unlink( req.get_file_credentials(), req.get_file_id() ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlewriteRequest( writeRequest& req ) { YIELD::auto_Object<writeResponse> resp( new writeResponse ); org::xtreemfs::interfaces::OSDWriteResponse osd_write_response; _write( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_lease_timeout(), req.get_object_data(), osd_write_response ); resp->set_osd_write_response( osd_write_response ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_broadcast_gmaxRequest( xtreemfs_broadcast_gmaxRequest& req ) { YIELD::auto_Object<xtreemfs_broadcast_gmaxResponse> resp( new xtreemfs_broadcast_gmaxResponse ); _xtreemfs_broadcast_gmax( req.get_fileId(), req.get_truncateEpoch(), req.get_lastObject(), req.get_fileSize() ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_check_objectRequest( xtreemfs_check_objectRequest& req ) { YIELD::auto_Object<xtreemfs_check_objectResponse> resp( new xtreemfs_check_objectResponse ); org::xtreemfs::interfaces::ObjectData _return_value = _xtreemfs_check_object( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_get_resultsRequest( xtreemfs_cleanup_get_resultsRequest& req ) { YIELD::auto_Object<xtreemfs_cleanup_get_resultsResponse> resp( new xtreemfs_cleanup_get_resultsResponse ); org::xtreemfs::interfaces::StringSet results; _xtreemfs_cleanup_get_results( results ); resp->set_results( results ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_is_runningRequest( xtreemfs_cleanup_is_runningRequest& req ) { YIELD::auto_Object<xtreemfs_cleanup_is_runningResponse> resp( new xtreemfs_cleanup_is_runningResponse ); bool is_running; _xtreemfs_cleanup_is_running( is_running ); resp->set_is_running( is_running ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_startRequest( xtreemfs_cleanup_startRequest& req ) { YIELD::auto_Object<xtreemfs_cleanup_startResponse> resp( new xtreemfs_cleanup_startResponse ); _xtreemfs_cleanup_start( req.get_remove_zombies(), req.get_remove_unavail_volume(), req.get_lost_and_found() ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_statusRequest( xtreemfs_cleanup_statusRequest& req ) { YIELD::auto_Object<xtreemfs_cleanup_statusResponse> resp( new xtreemfs_cleanup_statusResponse ); std::string status; _xtreemfs_cleanup_status( status ); resp->set_status( status ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_cleanup_stopRequest( xtreemfs_cleanup_stopRequest& req ) { YIELD::auto_Object<xtreemfs_cleanup_stopResponse> resp( new xtreemfs_cleanup_stopResponse ); _xtreemfs_cleanup_stop(); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_internal_get_gmaxRequest( xtreemfs_internal_get_gmaxRequest& req ) { YIELD::auto_Object<xtreemfs_internal_get_gmaxResponse> resp( new xtreemfs_internal_get_gmaxResponse ); org::xtreemfs::interfaces::InternalGmax _return_value = _xtreemfs_internal_get_gmax( req.get_file_credentials(), req.get_file_id() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_internal_get_file_sizeRequest( xtreemfs_internal_get_file_sizeRequest& req ) { YIELD::auto_Object<xtreemfs_internal_get_file_sizeResponse> resp( new xtreemfs_internal_get_file_sizeResponse ); uint64_t _return_value = _xtreemfs_internal_get_file_size( req.get_file_credentials(), req.get_file_id() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_internal_truncateRequest( xtreemfs_internal_truncateRequest& req ) { YIELD::auto_Object<xtreemfs_internal_truncateResponse> resp( new xtreemfs_internal_truncateResponse ); org::xtreemfs::interfaces::OSDWriteResponse osd_write_response; _xtreemfs_internal_truncate( req.get_file_credentials(), req.get_file_id(), req.get_new_file_size(), osd_write_response ); resp->set_osd_write_response( osd_write_response ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_internal_read_localRequest( xtreemfs_internal_read_localRequest& req ) { YIELD::auto_Object<xtreemfs_internal_read_localResponse> resp( new xtreemfs_internal_read_localResponse ); org::xtreemfs::interfaces::InternalReadLocalResponse _return_value = _xtreemfs_internal_read_local( req.get_file_credentials(), req.get_file_id(), req.get_object_number(), req.get_object_version(), req.get_offset(), req.get_length() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req ) { YIELD::auto_Object<xtreemfs_shutdownResponse> resp( new xtreemfs_shutdownResponse ); _xtreemfs_shutdown(); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }
        virtual void handlextreemfs_pingRequest( xtreemfs_pingRequest& req ) { YIELD::auto_Object<xtreemfs_pingResponse> resp( new xtreemfs_pingResponse ); org::xtreemfs::interfaces::VivaldiCoordinates remote_coordinates; _xtreemfs_ping( req.get_coordinates(), remote_coordinates ); resp->set_remote_coordinates( remote_coordinates ); req.respond( *resp.release() ); YIELD::Object::decRef( req ); }

      virtual void _read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data ) { }
        virtual void _truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { }
        virtual void _unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { }
        virtual void _write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { }
        virtual void _xtreemfs_broadcast_gmax( const std::string& fileId, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize ) { }
        virtual org::xtreemfs::interfaces::ObjectData _xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version ) { return org::xtreemfs::interfaces::ObjectData(); }
        virtual void _xtreemfs_cleanup_get_results( org::xtreemfs::interfaces::StringSet& results ) { }
        virtual void _xtreemfs_cleanup_is_running( bool& is_running ) { }
        virtual void _xtreemfs_cleanup_start( bool remove_zombies, bool remove_unavail_volume, bool lost_and_found ) { }
        virtual void _xtreemfs_cleanup_status( std::string& status ) { }
        virtual void _xtreemfs_cleanup_stop() { }
        virtual org::xtreemfs::interfaces::InternalGmax _xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return org::xtreemfs::interfaces::InternalGmax(); }
        virtual uint64_t _xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id ) { return 0; }
        virtual void _xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { }
        virtual org::xtreemfs::interfaces::InternalReadLocalResponse _xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length ) { return org::xtreemfs::interfaces::InternalReadLocalResponse(); }
        virtual void _xtreemfs_shutdown() { }
        virtual void _xtreemfs_ping( const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates, org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates ) { }
      };

      // Use this macro in an implementation class to get all of the prototypes for the operations in OSDInterface
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PROTOTYPES \
      virtual void _read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data );\
      virtual void _truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );\
      virtual void _unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual void _write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );\
      virtual void _xtreemfs_broadcast_gmax( const std::string& fileId, uint64_t truncateEpoch, uint64_t lastObject, uint64_t fileSize );\
      virtual org::xtreemfs::interfaces::ObjectData _xtreemfs_check_object( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version );\
      virtual void _xtreemfs_cleanup_get_results( org::xtreemfs::interfaces::StringSet& results );\
      virtual void _xtreemfs_cleanup_is_running( bool& is_running );\
      virtual void _xtreemfs_cleanup_start( bool remove_zombies, bool remove_unavail_volume, bool lost_and_found );\
      virtual void _xtreemfs_cleanup_status( std::string& status );\
      virtual void _xtreemfs_cleanup_stop();\
      virtual org::xtreemfs::interfaces::InternalGmax _xtreemfs_internal_get_gmax( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual uint64_t _xtreemfs_internal_get_file_size( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );\
      virtual void _xtreemfs_internal_truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );\
      virtual org::xtreemfs::interfaces::InternalReadLocalResponse _xtreemfs_internal_read_local( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint64_t offset, uint64_t length );\
      virtual void _xtreemfs_shutdown();\
      virtual void _xtreemfs_ping( const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates, org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates );

      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handlereadRequestRequest( readRequest& req );\
      virtual void handletruncateRequestRequest( truncateRequest& req );\
      virtual void handleunlinkRequestRequest( unlinkRequest& req );\
      virtual void handlewriteRequestRequest( writeRequest& req );\
      virtual void handlextreemfs_broadcast_gmaxRequestRequest( xtreemfs_broadcast_gmaxRequest& req );\
      virtual void handlextreemfs_check_objectRequestRequest( xtreemfs_check_objectRequest& req );\
      virtual void handlextreemfs_cleanup_get_resultsRequestRequest( xtreemfs_cleanup_get_resultsRequest& req );\
      virtual void handlextreemfs_cleanup_is_runningRequestRequest( xtreemfs_cleanup_is_runningRequest& req );\
      virtual void handlextreemfs_cleanup_startRequestRequest( xtreemfs_cleanup_startRequest& req );\
      virtual void handlextreemfs_cleanup_statusRequestRequest( xtreemfs_cleanup_statusRequest& req );\
      virtual void handlextreemfs_cleanup_stopRequestRequest( xtreemfs_cleanup_stopRequest& req );\
      virtual void handlextreemfs_internal_get_gmaxRequestRequest( xtreemfs_internal_get_gmaxRequest& req );\
      virtual void handlextreemfs_internal_get_file_sizeRequestRequest( xtreemfs_internal_get_file_sizeRequest& req );\
      virtual void handlextreemfs_internal_truncateRequestRequest( xtreemfs_internal_truncateRequest& req );\
      virtual void handlextreemfs_internal_read_localRequestRequest( xtreemfs_internal_read_localRequest& req );\
      virtual void handlextreemfs_shutdownRequestRequest( xtreemfs_shutdownRequest& req );\
      virtual void handlextreemfs_pingRequestRequest( xtreemfs_pingRequest& req );

    };



  };



};
#endif
