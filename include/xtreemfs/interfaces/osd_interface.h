#ifndef _186512954_H_
#define _186512954_H_


#include "constants.h"
#include "mrc_osd_types.h"
#include "yield/concurrency.h"
#include "yidl.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {

      class InternalGmax : public ::yidl::runtime::Struct
      {
      public:
        InternalGmax()
          : epoch( 0 ), file_size( 0 ), last_object_id( 0 )
        { }

        InternalGmax( uint64_t epoch, uint64_t file_size, uint64_t last_object_id )
          : epoch( epoch ), file_size( file_size ), last_object_id( last_object_id )
        { }

        virtual ~InternalGmax() {  }

        uint64_t get_epoch() const { return epoch; }
        uint64_t get_file_size() const { return file_size; }
        uint64_t get_last_object_id() const { return last_object_id; }
        void set_epoch( uint64_t epoch ) { this->epoch = epoch; }
        void set_file_size( uint64_t file_size ) { this->file_size = file_size; }
        void set_last_object_id( uint64_t last_object_id ) { this->last_object_id = last_object_id; }

        bool operator==( const InternalGmax& other ) const
        {
          return epoch == other.epoch
                 &&
                 file_size == other.file_size
                 &&
                 last_object_id == other.last_object_id;
        }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( InternalGmax, 2010030966 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "epoch", 0 ), epoch );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_size", 0 ), file_size );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "last_object_id", 0 ), last_object_id );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "epoch", 0 ), epoch );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_size", 0 ), file_size );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "last_object_id", 0 ), last_object_id );
        }

      protected:
        uint64_t epoch;
        uint64_t file_size;
        uint64_t last_object_id;
      };

      class Lock : public ::yidl::runtime::Struct
      {
      public:
        Lock()
          : client_pid( 0 ), length( 0 ), offset( 0 )
        { }

        Lock
        (
          uint32_t client_pid,
          const string& client_uuid,
          uint64_t length,
          uint64_t offset
        )
          : client_pid( client_pid ),
            client_uuid( client_uuid ),
            length( length ),
            offset( offset )
        { }

        virtual ~Lock() {  }

        uint32_t get_client_pid() const { return client_pid; }
        const string& get_client_uuid() const { return client_uuid; }
        uint64_t get_length() const { return length; }
        uint64_t get_offset() const { return offset; }
        void set_client_pid( uint32_t client_pid ) { this->client_pid = client_pid; }
        void set_client_uuid( const string& client_uuid ) { this->client_uuid = client_uuid; }
        void set_length( uint64_t length ) { this->length = length; }
        void set_offset( uint64_t offset ) { this->offset = offset; }

        bool operator==( const Lock& other ) const
        {
          return client_pid == other.client_pid
                 &&
                 client_uuid == other.client_uuid
                 &&
                 length == other.length
                 &&
                 offset == other.offset;
        }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Lock, 2010030970 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_pid", 0 ), client_pid );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_uuid", 0 ), client_uuid );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), length );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), offset );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          client_pid = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "client_pid", 0 ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "client_uuid", 0 ), client_uuid );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "length", 0 ), length );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "offset", 0 ), offset );
        }

      protected:
        uint32_t client_pid;
        string client_uuid;
        uint64_t length;
        uint64_t offset;
      };

      class ObjectData : public ::yidl::runtime::Struct
      {
      public:
        ObjectData()
          : checksum( 0 ), invalid_checksum_on_osd( false ), zero_padding( 0 ), data( NULL )
        { }

        ObjectData
        (
          uint32_t checksum,
          bool invalid_checksum_on_osd,
          uint32_t zero_padding,
          ::yidl::runtime::Buffer* data
        )
          : checksum( checksum ),
            invalid_checksum_on_osd( invalid_checksum_on_osd ),
            zero_padding( zero_padding ),
            data( ::yidl::runtime::Object::inc_ref( data ) )
        { }

        virtual ~ObjectData() { ::yidl::runtime::Buffer::dec_ref( data ); }

        uint32_t get_checksum() const { return checksum; }
        bool get_invalid_checksum_on_osd() const { return invalid_checksum_on_osd; }
        uint32_t get_zero_padding() const { return zero_padding; }
        ::yidl::runtime::Buffer* get_data() const { return data; }
        void set_checksum( uint32_t checksum ) { this->checksum = checksum; }
        void set_invalid_checksum_on_osd( bool invalid_checksum_on_osd ) { this->invalid_checksum_on_osd = invalid_checksum_on_osd; }
        void set_zero_padding( uint32_t zero_padding ) { this->zero_padding = zero_padding; }
        void set_data( ::yidl::runtime::Buffer* data ) { ::yidl::runtime::Buffer::dec_ref( this->data ); this->data = ::yidl::runtime::Object::inc_ref( data ); }

        bool operator==( const ObjectData& other ) const
        {
          return checksum == other.checksum
                 &&
                 invalid_checksum_on_osd == other.invalid_checksum_on_osd
                 &&
                 zero_padding == other.zero_padding
                 &&
                 data == other.data;
        }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ObjectData, 2010030967 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "checksum", 0 ), checksum );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "invalid_checksum_on_osd", 0 ), invalid_checksum_on_osd );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "zero_padding", 0 ), zero_padding );
          if ( data != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), *data );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          checksum = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "checksum", 0 ) );
          invalid_checksum_on_osd = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "invalid_checksum_on_osd", 0 ) );
          zero_padding = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "zero_padding", 0 ) );
          if ( data != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), *data );
        }

      protected:
        uint32_t checksum;
        bool invalid_checksum_on_osd;
        uint32_t zero_padding;
        ::yidl::runtime::Buffer* data;
      };

      class ObjectList : public ::yidl::runtime::Struct
      {
      public:
        ObjectList()
          : set( NULL ), stripe_width( 0 ), first_( 0 )
        { }

        ObjectList
        (
          ::yidl::runtime::Buffer* set,
          uint32_t stripe_width,
          uint32_t first_
        )
          : set( ::yidl::runtime::Object::inc_ref( set ) ),
            stripe_width( stripe_width ),
            first_( first_ )
        { }

        virtual ~ObjectList() { ::yidl::runtime::Buffer::dec_ref( set ); }

        ::yidl::runtime::Buffer* get_set() const { return set; }
        uint32_t get_stripe_width() const { return stripe_width; }
        uint32_t get_first_() const { return first_; }
        void set_set( ::yidl::runtime::Buffer* set ) { ::yidl::runtime::Buffer::dec_ref( this->set ); this->set = ::yidl::runtime::Object::inc_ref( set ); }
        void set_stripe_width( uint32_t stripe_width ) { this->stripe_width = stripe_width; }
        void set_first_( uint32_t first_ ) { this->first_ = first_; }

        bool operator==( const ObjectList& other ) const
        {
          return set == other.set
                 &&
                 stripe_width == other.stripe_width
                 &&
                 first_ == other.first_;
        }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ObjectList, 2010030971 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          if ( set != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "set", 0 ), *set );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stripe_width", 0 ), stripe_width );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "first_", 0 ), first_ );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          if ( set != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "set", 0 ), *set );
          stripe_width = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "stripe_width", 0 ) );
          first_ = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "first_", 0 ) );
        }

      protected:
        ::yidl::runtime::Buffer* set;
        uint32_t stripe_width;
        uint32_t first_;
      };

      class ObjectListSet
        : public ::yidl::runtime::Sequence,
          public vector<org::xtreemfs::interfaces::ObjectList>
      {
      public:
        ObjectListSet() { }
        ObjectListSet( const org::xtreemfs::interfaces::ObjectList& first_value ) { vector<org::xtreemfs::interfaces::ObjectList>::push_back( first_value ); }
        ObjectListSet( size_type size ) : vector<org::xtreemfs::interfaces::ObjectList>( size ) { }
        virtual ~ObjectListSet() { }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ObjectListSet, 2010030972 );

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
          org::xtreemfs::interfaces::ObjectList value;
          unmarshaller.read( ::yidl::runtime::Marshaller::Key(), value );
          push_back( value );
        }

        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };

      class ObjectVersion : public ::yidl::runtime::Struct
      {
      public:
        ObjectVersion()
          : object_number( 0 ), object_version( 0 )
        { }

        ObjectVersion( uint64_t object_number, uint64_t object_version )
          : object_number( object_number ), object_version( object_version )
        { }

        virtual ~ObjectVersion() {  }

        uint64_t get_object_number() const { return object_number; }
        uint64_t get_object_version() const { return object_version; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }

        bool operator==( const ObjectVersion& other ) const
        {
          return object_number == other.object_number
                 &&
                 object_version == other.object_version;
        }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ObjectVersion, 2010030973 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), object_number );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), object_version );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_number", 0 ), object_number );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
        }

      protected:
        uint64_t object_number;
        uint64_t object_version;
      };

      class ObjectVersionList
        : public ::yidl::runtime::Sequence,
          public vector<org::xtreemfs::interfaces::ObjectVersion>
      {
      public:
        ObjectVersionList() { }
        ObjectVersionList( const org::xtreemfs::interfaces::ObjectVersion& first_value ) { vector<org::xtreemfs::interfaces::ObjectVersion>::push_back( first_value ); }
        ObjectVersionList( size_type size ) : vector<org::xtreemfs::interfaces::ObjectVersion>( size ) { }
        virtual ~ObjectVersionList() { }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ObjectVersionList, 2010030974 );

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
          org::xtreemfs::interfaces::ObjectVersion value;
          unmarshaller.read( ::yidl::runtime::Marshaller::Key(), value );
          push_back( value );
        }

        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };

      class ReplicaStatus : public ::yidl::runtime::Struct
      {
      public:
        ReplicaStatus()
          : truncate_epoch( 0 ), file_size( 0 ), max_obj_version( 0 )
        { }

        ReplicaStatus
        (
          uint64_t truncate_epoch,
          uint64_t file_size,
          uint64_t max_obj_version,
          const org::xtreemfs::interfaces::ObjectVersionList& objectVersions
        )
          : truncate_epoch( truncate_epoch ),
            file_size( file_size ),
            max_obj_version( max_obj_version ),
            objectVersions( objectVersions )
        { }

        virtual ~ReplicaStatus() {  }

        uint64_t get_truncate_epoch() const { return truncate_epoch; }
        uint64_t get_file_size() const { return file_size; }
        uint64_t get_max_obj_version() const { return max_obj_version; }
        const org::xtreemfs::interfaces::ObjectVersionList& get_objectVersions() const { return objectVersions; }
        void set_truncate_epoch( uint64_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        void set_file_size( uint64_t file_size ) { this->file_size = file_size; }
        void set_max_obj_version( uint64_t max_obj_version ) { this->max_obj_version = max_obj_version; }
        void set_objectVersions( const org::xtreemfs::interfaces::ObjectVersionList&  objectVersions ) { this->objectVersions = objectVersions; }

        bool operator==( const ReplicaStatus& other ) const
        {
          return truncate_epoch == other.truncate_epoch
                 &&
                 file_size == other.file_size
                 &&
                 max_obj_version == other.max_obj_version
                 &&
                 objectVersions == other.objectVersions;
        }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ReplicaStatus, 2010030975 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "truncate_epoch", 0 ), truncate_epoch );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_size", 0 ), file_size );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "max_obj_version", 0 ), max_obj_version );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "objectVersions", 0 ), objectVersions );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "truncate_epoch", 0 ), truncate_epoch );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_size", 0 ), file_size );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "max_obj_version", 0 ), max_obj_version );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "objectVersions", 0 ), objectVersions );
        }

      protected:
        uint64_t truncate_epoch;
        uint64_t file_size;
        uint64_t max_obj_version;
        org::xtreemfs::interfaces::ObjectVersionList objectVersions;
      };

      class InternalReadLocalResponse : public ::yidl::runtime::Struct
      {
      public:
        InternalReadLocalResponse() { }

        InternalReadLocalResponse
        (
          const org::xtreemfs::interfaces::ObjectData& data,
          const org::xtreemfs::interfaces::ObjectListSet& object_set
        )
          : data( data ), object_set( object_set )
        { }

        virtual ~InternalReadLocalResponse() {  }

        const org::xtreemfs::interfaces::ObjectData& get_data() const { return data; }
        const org::xtreemfs::interfaces::ObjectListSet& get_object_set() const { return object_set; }
        void set_data( const org::xtreemfs::interfaces::ObjectData&  data ) { this->data = data; }
        void set_object_set( const org::xtreemfs::interfaces::ObjectListSet&  object_set ) { this->object_set = object_set; }

        bool operator==( const InternalReadLocalResponse& other ) const
        {
          return data == other.data
                 &&
                 object_set == other.object_set;
        }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( InternalReadLocalResponse, 2010030968 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), data );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_set", 0 ), object_set );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), data );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_set", 0 ), object_set );
        }

      protected:
        org::xtreemfs::interfaces::ObjectData data;
        org::xtreemfs::interfaces::ObjectListSet object_set;
      };


      class OSDInterface
      {
      public:
          const static uint32_t HTTP_PORT_DEFAULT = 30640;
        const static uint32_t ONCRPC_PORT_DEFAULT = 32640;
        const static uint32_t ONCRPCG_PORT_DEFAULT = 32640;
        const static uint32_t ONCRPCS_PORT_DEFAULT = 32640;
        const static uint32_t ONCRPCU_PORT_DEFAULT = 32640;const static uint32_t TAG = 2010031216;

        virtual ~OSDInterface() { }

        uint32_t get_tag() const { return 2010031216; }



        virtual void
        read
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          uint32_t offset,
          uint32_t length,
          org::xtreemfs::interfaces::ObjectData& object_data
        )
        { }

        virtual void
        truncate
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t new_file_size,
          org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
        )
        { }

        virtual void
        unlink
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        )
        { }

        virtual void
        write
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          uint32_t offset,
          uint64_t lease_timeout,
          const org::xtreemfs::interfaces::ObjectData& object_data,
          org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
        )
        { }

        virtual void
        xtreemfs_broadcast_gmax
        (
          const string& file_id,
          uint64_t truncate_epoch,
          uint64_t last_object,
          uint64_t file_size
        )
        { }

        virtual org::xtreemfs::interfaces::ObjectData
        xtreemfs_check_object
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version
        ){
          return org::xtreemfs::interfaces::ObjectData();
        }

        virtual void
        xtreemfs_cleanup_get_results
        (
          org::xtreemfs::interfaces::StringSet& results
        )
        { }

        virtual void xtreemfs_cleanup_is_running( bool& is_running ) { }

        virtual void
        xtreemfs_cleanup_start
        (
          bool remove_zombies,
          bool remove_unavail_volume,
          bool lost_and_found
        )
        { }

        virtual void xtreemfs_cleanup_status( string& status ) { }

        virtual void xtreemfs_cleanup_stop() { }

        virtual void
        xtreemfs_rwr_fetch
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          org::xtreemfs::interfaces::ObjectData& object_data
        )
        { }

        virtual void
        xtreemfs_rwr_flease_msg
        (
          ::yidl::runtime::Buffer* fleaseMessage,
          const string& senderHostname,
          uint32_t senderPort
        )
        { }

        virtual void
        xtreemfs_rwr_notify
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        )
        { }

        virtual void
        xtreemfs_rwr_status
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          int64_t max_local_obj_version,
          org::xtreemfs::interfaces::ReplicaStatus& local_state
        )
        { }

        virtual void
        xtreemfs_rwr_truncate
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t new_file_size,
          uint64_t object_version
        )
        { }

        virtual void
        xtreemfs_rwr_update
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          uint32_t offset,
          const org::xtreemfs::interfaces::ObjectData& object_data
        )
        { }

        virtual org::xtreemfs::interfaces::InternalGmax
        xtreemfs_internal_get_gmax
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        ){
          return org::xtreemfs::interfaces::InternalGmax();
        }

        virtual void
        xtreemfs_internal_truncate
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t new_file_size,
          org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
        )
        { }

        virtual uint64_t
        xtreemfs_internal_get_file_size
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        ){
          return 0;
        }

        virtual org::xtreemfs::interfaces::InternalReadLocalResponse
        xtreemfs_internal_read_local
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          uint64_t offset,
          uint64_t length,
          bool attach_object_list,
          const org::xtreemfs::interfaces::ObjectListSet& required_objects
        ){
          return org::xtreemfs::interfaces::InternalReadLocalResponse();
        }

        virtual org::xtreemfs::interfaces::ObjectList
        xtreemfs_internal_get_object_set
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        ){
          return org::xtreemfs::interfaces::ObjectList();
        }

        virtual org::xtreemfs::interfaces::Lock
        xtreemfs_lock_acquire
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& client_uuid,
          int32_t client_pid,
          const string& file_id,
          uint64_t offset,
          uint64_t length,
          bool exclusive
        ){
          return org::xtreemfs::interfaces::Lock();
        }

        virtual org::xtreemfs::interfaces::Lock
        xtreemfs_lock_check
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& client_uuid,
          int32_t client_pid,
          const string& file_id,
          uint64_t offset,
          uint64_t length,
          bool exclusive
        ){
          return org::xtreemfs::interfaces::Lock();
        }

        virtual void
        xtreemfs_lock_release
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          const org::xtreemfs::interfaces::Lock& lock
        )
        { }

        virtual void
        xtreemfs_ping
        (
          const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates,
          org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates
        )
        { }

        virtual void xtreemfs_shutdown() { }
      };


      // Use this macro in an implementation class to get all of the prototypes for the operations in OSDInterface
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PROTOTYPES\
      virtual void\
      read\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        uint64_t object_number,\
        uint64_t object_version,\
        uint32_t offset,\
        uint32_t length,\
        org::xtreemfs::interfaces::ObjectData& object_data\
      );\
      virtual void\
      truncate\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        uint64_t new_file_size,\
        org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response\
      );\
      virtual void\
      unlink\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id\
      );\
      virtual void\
      write\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        uint64_t object_number,\
        uint64_t object_version,\
        uint32_t offset,\
        uint64_t lease_timeout,\
        const org::xtreemfs::interfaces::ObjectData& object_data,\
        org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response\
      );\
      virtual void\
      xtreemfs_broadcast_gmax\
      (\
        const string& file_id,\
        uint64_t truncate_epoch,\
        uint64_t last_object,\
        uint64_t file_size\
      );\
      virtual org::xtreemfs::interfaces::ObjectData\
      xtreemfs_check_object\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        uint64_t object_number,\
        uint64_t object_version\
      );\
      virtual void\
      xtreemfs_cleanup_get_results\
      (\
        org::xtreemfs::interfaces::StringSet& results\
      );\
      virtual void xtreemfs_cleanup_is_running( bool& is_running );\
      virtual void\
      xtreemfs_cleanup_start\
      (\
        bool remove_zombies,\
        bool remove_unavail_volume,\
        bool lost_and_found\
      );\
      virtual void xtreemfs_cleanup_status( string& status );\
      virtual void xtreemfs_cleanup_stop();\
      virtual void\
      xtreemfs_rwr_fetch\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        uint64_t object_number,\
        uint64_t object_version,\
        org::xtreemfs::interfaces::ObjectData& object_data\
      );\
      virtual void\
      xtreemfs_rwr_flease_msg\
      (\
        ::yidl::runtime::Buffer* fleaseMessage,\
        const string& senderHostname,\
        uint32_t senderPort\
      );\
      virtual void\
      xtreemfs_rwr_notify\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id\
      );\
      virtual void\
      xtreemfs_rwr_status\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        int64_t max_local_obj_version,\
        org::xtreemfs::interfaces::ReplicaStatus& local_state\
      );\
      virtual void\
      xtreemfs_rwr_truncate\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        uint64_t new_file_size,\
        uint64_t object_version\
      );\
      virtual void\
      xtreemfs_rwr_update\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        uint64_t object_number,\
        uint64_t object_version,\
        uint32_t offset,\
        const org::xtreemfs::interfaces::ObjectData& object_data\
      );\
      virtual org::xtreemfs::interfaces::InternalGmax\
      xtreemfs_internal_get_gmax\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id\
      );\
      virtual void\
      xtreemfs_internal_truncate\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        uint64_t new_file_size,\
        org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response\
      );\
      virtual uint64_t\
      xtreemfs_internal_get_file_size\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id\
      );\
      virtual org::xtreemfs::interfaces::InternalReadLocalResponse\
      xtreemfs_internal_read_local\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        uint64_t object_number,\
        uint64_t object_version,\
        uint64_t offset,\
        uint64_t length,\
        bool attach_object_list,\
        const org::xtreemfs::interfaces::ObjectListSet& required_objects\
      );\
      virtual org::xtreemfs::interfaces::ObjectList\
      xtreemfs_internal_get_object_set\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id\
      );\
      virtual org::xtreemfs::interfaces::Lock\
      xtreemfs_lock_acquire\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& client_uuid,\
        int32_t client_pid,\
        const string& file_id,\
        uint64_t offset,\
        uint64_t length,\
        bool exclusive\
      );\
      virtual org::xtreemfs::interfaces::Lock\
      xtreemfs_lock_check\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& client_uuid,\
        int32_t client_pid,\
        const string& file_id,\
        uint64_t offset,\
        uint64_t length,\
        bool exclusive\
      );\
      virtual void\
      xtreemfs_lock_release\
      (\
        const org::xtreemfs::interfaces::FileCredentials& file_credentials,\
        const string& file_id,\
        const org::xtreemfs::interfaces::Lock& lock\
      );\
      virtual void\
      xtreemfs_ping\
      (\
        const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates,\
        org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates\
      );\
      virtual void xtreemfs_shutdown();\


      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_EXCEPTION_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ::yield::concurrency::ExceptionResponse
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
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS ::yield::concurrency::Request
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
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS ::yield::concurrency::Response
      #endif
      #endif


      class OSDInterfaceMessages
      {
      public:
      // Request/response pair definitions for the operations in OSDInterface
        class readRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          readRequest()
            : object_number( 0 ), object_version( 0 ), offset( 0 ), length( 0 )
          { }

          readRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            uint64_t object_number,
            uint64_t object_version,
            uint32_t offset,
            uint32_t length,
            const org::xtreemfs::interfaces::ObjectData& object_data
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              object_number( object_number ),
              object_version( object_version ),
              offset( offset ),
              length( length ),
              object_data( object_data )
          { }

          virtual ~readRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_object_number() const { return object_number; }
          uint64_t get_object_version() const { return object_version; }
          uint32_t get_offset() const { return offset; }
          uint32_t get_length() const { return length; }
          const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
          void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
          void set_offset( uint32_t offset ) { this->offset = offset; }
          void set_length( uint32_t length ) { this->length = length; }
          void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::ObjectData& object_data
          )
          {
            respond
            (
              *new readResponse
                   (
                     object_data
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const readRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   object_number == other.object_number
                   &&
                   object_version == other.object_version
                   &&
                   offset == other.offset
                   &&
                   length == other.length
                   &&
                   object_data == other.object_data;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( readRequest, 2010031226 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), object_number );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), object_version );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), offset );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), length );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_number", 0 ), object_number );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
            offset = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "offset", 0 ) );
            length = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "length", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          uint64_t object_number;
          uint64_t object_version;
          uint32_t offset;
          uint32_t length;
          org::xtreemfs::interfaces::ObjectData object_data;
        };


        class readResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          readResponse() { }

          readResponse( const org::xtreemfs::interfaces::ObjectData& object_data )
            : object_data( object_data )
          { }

          virtual ~readResponse() {  }

          const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }
          void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }

          bool operator==( const readResponse& other ) const
          {
            return object_data == other.object_data;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( readResponse, 2010031226 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

        protected:
          org::xtreemfs::interfaces::ObjectData object_data;
        };


        class truncateRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          truncateRequest()
            : new_file_size( 0 )
          { }

          truncateRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            uint64_t new_file_size
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              new_file_size( new_file_size )
          { }

          virtual ~truncateRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_new_file_size() const { return new_file_size; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
          )
          {
            respond
            (
              *new truncateResponse
                   (
                     osd_write_response
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const truncateRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   new_file_size == other.new_file_size;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( truncateRequest, 2010031227 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          uint64_t new_file_size;
        };


        class truncateResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          truncateResponse() { }

          truncateResponse
          (
            const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
          )
            : osd_write_response( osd_write_response )
          { }

          virtual ~truncateResponse() {  }

          const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }
          void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }

          bool operator==( const truncateResponse& other ) const
          {
            return osd_write_response == other.osd_write_response;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( truncateResponse, 2010031227 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "osd_write_response", 0 ), osd_write_response );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "osd_write_response", 0 ), osd_write_response );
          }

        protected:
          org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
        };


        class unlinkRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          unlinkRequest() { }

          unlinkRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id
          )
            : file_credentials( file_credentials ), file_id( file_id )
          { }

          virtual ~unlinkRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }


          virtual void respond()
          {
            respond( *new unlinkResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const unlinkRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( unlinkRequest, 2010031228 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
        };


        class unlinkResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          unlinkResponse() { }
          virtual ~unlinkResponse() {  }

          bool operator==( const unlinkResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( unlinkResponse, 2010031228 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class writeRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          writeRequest()
            : object_number( 0 ), object_version( 0 ), offset( 0 ), lease_timeout( 0 )
          { }

          writeRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            uint64_t object_number,
            uint64_t object_version,
            uint32_t offset,
            uint64_t lease_timeout,
            const org::xtreemfs::interfaces::ObjectData& object_data
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              object_number( object_number ),
              object_version( object_version ),
              offset( offset ),
              lease_timeout( lease_timeout ),
              object_data( object_data )
          { }

          virtual ~writeRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_object_number() const { return object_number; }
          uint64_t get_object_version() const { return object_version; }
          uint32_t get_offset() const { return offset; }
          uint64_t get_lease_timeout() const { return lease_timeout; }
          const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
          void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
          void set_offset( uint32_t offset ) { this->offset = offset; }
          void set_lease_timeout( uint64_t lease_timeout ) { this->lease_timeout = lease_timeout; }
          void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
          )
          {
            respond
            (
              *new writeResponse
                   (
                     osd_write_response
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const writeRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   object_number == other.object_number
                   &&
                   object_version == other.object_version
                   &&
                   offset == other.offset
                   &&
                   lease_timeout == other.lease_timeout
                   &&
                   object_data == other.object_data;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( writeRequest, 2010031229 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), object_number );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), object_version );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), offset );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "lease_timeout", 0 ), lease_timeout );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_number", 0 ), object_number );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
            offset = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "offset", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "lease_timeout", 0 ), lease_timeout );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          uint64_t object_number;
          uint64_t object_version;
          uint32_t offset;
          uint64_t lease_timeout;
          org::xtreemfs::interfaces::ObjectData object_data;
        };


        class writeResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          writeResponse() { }

          writeResponse
          (
            const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
          )
            : osd_write_response( osd_write_response )
          { }

          virtual ~writeResponse() {  }

          const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }
          void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }

          bool operator==( const writeResponse& other ) const
          {
            return osd_write_response == other.osd_write_response;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( writeResponse, 2010031229 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "osd_write_response", 0 ), osd_write_response );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "osd_write_response", 0 ), osd_write_response );
          }

        protected:
          org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
        };


        class xtreemfs_broadcast_gmaxRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_broadcast_gmaxRequest()
            : truncate_epoch( 0 ), last_object( 0 ), file_size( 0 )
          { }

          xtreemfs_broadcast_gmaxRequest
          (
            const string& file_id,
            uint64_t truncate_epoch,
            uint64_t last_object,
            uint64_t file_size
          )
            : file_id( file_id ),
              truncate_epoch( truncate_epoch ),
              last_object( last_object ),
              file_size( file_size )
          { }

          virtual ~xtreemfs_broadcast_gmaxRequest() {  }

          const string& get_file_id() const { return file_id; }
          uint64_t get_truncate_epoch() const { return truncate_epoch; }
          uint64_t get_last_object() const { return last_object; }
          uint64_t get_file_size() const { return file_size; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_truncate_epoch( uint64_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
          void set_last_object( uint64_t last_object ) { this->last_object = last_object; }
          void set_file_size( uint64_t file_size ) { this->file_size = file_size; }


          virtual void respond()
          {
            respond( *new xtreemfs_broadcast_gmaxResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_broadcast_gmaxRequest& other ) const
          {
            return file_id == other.file_id
                   &&
                   truncate_epoch == other.truncate_epoch
                   &&
                   last_object == other.last_object
                   &&
                   file_size == other.file_size;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_broadcast_gmaxRequest, 2010031236 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "truncate_epoch", 0 ), truncate_epoch );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "last_object", 0 ), last_object );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_size", 0 ), file_size );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "truncate_epoch", 0 ), truncate_epoch );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "last_object", 0 ), last_object );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_size", 0 ), file_size );
          }

        protected:
          string file_id;
          uint64_t truncate_epoch;
          uint64_t last_object;
          uint64_t file_size;
        };


        class xtreemfs_broadcast_gmaxResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_broadcast_gmaxResponse() { }
          virtual ~xtreemfs_broadcast_gmaxResponse() {  }

          bool operator==( const xtreemfs_broadcast_gmaxResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_broadcast_gmaxResponse, 2010031236 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_check_objectRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_check_objectRequest()
            : object_number( 0 ), object_version( 0 )
          { }

          xtreemfs_check_objectRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            uint64_t object_number,
            uint64_t object_version
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              object_number( object_number ),
              object_version( object_version )
          { }

          virtual ~xtreemfs_check_objectRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_object_number() const { return object_number; }
          uint64_t get_object_version() const { return object_version; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
          void set_object_version( uint64_t object_version ) { this->object_version = object_version; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::ObjectData& _return_value
          )
          {
            respond
            (
              *new xtreemfs_check_objectResponse
                   (
                     _return_value
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_check_objectRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   object_number == other.object_number
                   &&
                   object_version == other.object_version;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_check_objectRequest, 2010031237 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), object_number );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), object_version );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_number", 0 ), object_number );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          uint64_t object_number;
          uint64_t object_version;
        };


        class xtreemfs_check_objectResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_check_objectResponse() { }

          xtreemfs_check_objectResponse
          (
            const org::xtreemfs::interfaces::ObjectData& _return_value
          )
            : _return_value( _return_value )
          { }

          virtual ~xtreemfs_check_objectResponse() {  }

          const org::xtreemfs::interfaces::ObjectData& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::ObjectData&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_check_objectResponse& other ) const
          {
            return _return_value == other._return_value;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_check_objectResponse, 2010031237 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          org::xtreemfs::interfaces::ObjectData _return_value;
        };


        class xtreemfs_cleanup_get_resultsRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_get_resultsRequest() { }
          virtual ~xtreemfs_cleanup_get_resultsRequest() {  }


          virtual void respond( const org::xtreemfs::interfaces::StringSet& results )
          {
            respond( *new xtreemfs_cleanup_get_resultsResponse( results ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_cleanup_get_resultsRequest& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_get_resultsRequest, 2010031246 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_cleanup_get_resultsResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_get_resultsResponse() { }

          xtreemfs_cleanup_get_resultsResponse
          (
            const org::xtreemfs::interfaces::StringSet& results
          )
            : results( results )
          { }

          virtual ~xtreemfs_cleanup_get_resultsResponse() {  }

          const org::xtreemfs::interfaces::StringSet& get_results() const { return results; }
          void set_results( const org::xtreemfs::interfaces::StringSet&  results ) { this->results = results; }

          bool operator==( const xtreemfs_cleanup_get_resultsResponse& other ) const
          {
            return results == other.results;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_get_resultsResponse, 2010031246 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "results", 0 ), results );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "results", 0 ), results );
          }

        protected:
          org::xtreemfs::interfaces::StringSet results;
        };


        class xtreemfs_cleanup_is_runningRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_is_runningRequest() { }
          virtual ~xtreemfs_cleanup_is_runningRequest() {  }


          virtual void respond( bool is_running )
          {
            respond( *new xtreemfs_cleanup_is_runningResponse( is_running ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_cleanup_is_runningRequest& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_is_runningRequest, 2010031247 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_cleanup_is_runningResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_is_runningResponse()
            : is_running( false )
          { }

          xtreemfs_cleanup_is_runningResponse( bool is_running )
            : is_running( is_running )
          { }

          virtual ~xtreemfs_cleanup_is_runningResponse() {  }

          bool get_is_running() const { return is_running; }
          void set_is_running( bool is_running ) { this->is_running = is_running; }

          bool operator==( const xtreemfs_cleanup_is_runningResponse& other ) const
          {
            return is_running == other.is_running;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_is_runningResponse, 2010031247 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "is_running", 0 ), is_running );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            is_running = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "is_running", 0 ) );
          }

        protected:
          bool is_running;
        };


        class xtreemfs_cleanup_startRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_startRequest()
            : remove_zombies( false ), remove_unavail_volume( false ), lost_and_found( false )
          { }

          xtreemfs_cleanup_startRequest
          (
            bool remove_zombies,
            bool remove_unavail_volume,
            bool lost_and_found
          )
            : remove_zombies( remove_zombies ),
              remove_unavail_volume( remove_unavail_volume ),
              lost_and_found( lost_and_found )
          { }

          virtual ~xtreemfs_cleanup_startRequest() {  }

          bool get_remove_zombies() const { return remove_zombies; }
          bool get_remove_unavail_volume() const { return remove_unavail_volume; }
          bool get_lost_and_found() const { return lost_and_found; }
          void set_remove_zombies( bool remove_zombies ) { this->remove_zombies = remove_zombies; }
          void set_remove_unavail_volume( bool remove_unavail_volume ) { this->remove_unavail_volume = remove_unavail_volume; }
          void set_lost_and_found( bool lost_and_found ) { this->lost_and_found = lost_and_found; }


          virtual void respond()
          {
            respond( *new xtreemfs_cleanup_startResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_cleanup_startRequest& other ) const
          {
            return remove_zombies == other.remove_zombies
                   &&
                   remove_unavail_volume == other.remove_unavail_volume
                   &&
                   lost_and_found == other.lost_and_found;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_startRequest, 2010031248 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "remove_zombies", 0 ), remove_zombies );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "remove_unavail_volume", 0 ), remove_unavail_volume );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "lost_and_found", 0 ), lost_and_found );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            remove_zombies = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "remove_zombies", 0 ) );
            remove_unavail_volume = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "remove_unavail_volume", 0 ) );
            lost_and_found = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "lost_and_found", 0 ) );
          }

        protected:
          bool remove_zombies;
          bool remove_unavail_volume;
          bool lost_and_found;
        };


        class xtreemfs_cleanup_startResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_startResponse() { }
          virtual ~xtreemfs_cleanup_startResponse() {  }

          bool operator==( const xtreemfs_cleanup_startResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_startResponse, 2010031248 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_cleanup_statusRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_statusRequest() { }
          virtual ~xtreemfs_cleanup_statusRequest() {  }


          virtual void respond( const string& status )
          {
            respond( *new xtreemfs_cleanup_statusResponse( status ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_cleanup_statusRequest& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_statusRequest, 2010031249 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_cleanup_statusResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_statusResponse() { }

          xtreemfs_cleanup_statusResponse( const string& status )
            : status( status )
          { }

          virtual ~xtreemfs_cleanup_statusResponse() {  }

          const string& get_status() const { return status; }
          void set_status( const string& status ) { this->status = status; }

          bool operator==( const xtreemfs_cleanup_statusResponse& other ) const
          {
            return status == other.status;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_statusResponse, 2010031249 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "status", 0 ), status );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "status", 0 ), status );
          }

        protected:
          string status;
        };


        class xtreemfs_cleanup_stopRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_stopRequest() { }
          virtual ~xtreemfs_cleanup_stopRequest() {  }


          virtual void respond()
          {
            respond( *new xtreemfs_cleanup_stopResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_cleanup_stopRequest& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_stopRequest, 2010031250 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_cleanup_stopResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_stopResponse() { }
          virtual ~xtreemfs_cleanup_stopResponse() {  }

          bool operator==( const xtreemfs_cleanup_stopResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_stopResponse, 2010031250 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_rwr_fetchRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_fetchRequest()
            : object_number( 0 ), object_version( 0 )
          { }

          xtreemfs_rwr_fetchRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            uint64_t object_number,
            uint64_t object_version
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              object_number( object_number ),
              object_version( object_version )
          { }

          virtual ~xtreemfs_rwr_fetchRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_object_number() const { return object_number; }
          uint64_t get_object_version() const { return object_version; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
          void set_object_version( uint64_t object_version ) { this->object_version = object_version; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::ObjectData& object_data
          )
          {
            respond
            (
              *new xtreemfs_rwr_fetchResponse
                   (
                     object_data
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_rwr_fetchRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   object_number == other.object_number
                   &&
                   object_version == other.object_version;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_fetchRequest, 2010031289 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), object_number );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), object_version );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_number", 0 ), object_number );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          uint64_t object_number;
          uint64_t object_version;
        };


        class xtreemfs_rwr_fetchResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_fetchResponse() { }

          xtreemfs_rwr_fetchResponse
          (
            const org::xtreemfs::interfaces::ObjectData& object_data
          )
            : object_data( object_data )
          { }

          virtual ~xtreemfs_rwr_fetchResponse() {  }

          const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }
          void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }

          bool operator==( const xtreemfs_rwr_fetchResponse& other ) const
          {
            return object_data == other.object_data;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_fetchResponse, 2010031289 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

        protected:
          org::xtreemfs::interfaces::ObjectData object_data;
        };


        class xtreemfs_rwr_flease_msgRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_flease_msgRequest()
            : fleaseMessage( NULL ), senderPort( 0 )
          { }

          xtreemfs_rwr_flease_msgRequest
          (
            ::yidl::runtime::Buffer* fleaseMessage,
            const string& senderHostname,
            uint32_t senderPort
          )
            : fleaseMessage( ::yidl::runtime::Object::inc_ref( fleaseMessage ) ),
              senderHostname( senderHostname ),
              senderPort( senderPort )
          { }

          virtual ~xtreemfs_rwr_flease_msgRequest() { ::yidl::runtime::Buffer::dec_ref( fleaseMessage ); }

          ::yidl::runtime::Buffer* get_fleaseMessage() const { return fleaseMessage; }
          const string& get_senderHostname() const { return senderHostname; }
          uint32_t get_senderPort() const { return senderPort; }
          void set_fleaseMessage( ::yidl::runtime::Buffer* fleaseMessage ) { ::yidl::runtime::Buffer::dec_ref( this->fleaseMessage ); this->fleaseMessage = ::yidl::runtime::Object::inc_ref( fleaseMessage ); }
          void set_senderHostname( const string& senderHostname ) { this->senderHostname = senderHostname; }
          void set_senderPort( uint32_t senderPort ) { this->senderPort = senderPort; }


          virtual void respond()
          {
            respond( *new xtreemfs_rwr_flease_msgResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_rwr_flease_msgRequest& other ) const
          {
            return fleaseMessage == other.fleaseMessage
                   &&
                   senderHostname == other.senderHostname
                   &&
                   senderPort == other.senderPort;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_flease_msgRequest, 2010031287 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            if ( fleaseMessage != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "fleaseMessage", 0 ), *fleaseMessage );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "senderHostname", 0 ), senderHostname );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "senderPort", 0 ), senderPort );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            if ( fleaseMessage != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "fleaseMessage", 0 ), *fleaseMessage );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "senderHostname", 0 ), senderHostname );
            senderPort = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "senderPort", 0 ) );
          }

        protected:
          ::yidl::runtime::Buffer* fleaseMessage;
          string senderHostname;
          uint32_t senderPort;
        };


        class xtreemfs_rwr_flease_msgResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_flease_msgResponse() { }
          virtual ~xtreemfs_rwr_flease_msgResponse() {  }

          bool operator==( const xtreemfs_rwr_flease_msgResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_flease_msgResponse, 2010031287 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_rwr_notifyRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_notifyRequest() { }

          xtreemfs_rwr_notifyRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id
          )
            : file_credentials( file_credentials ), file_id( file_id )
          { }

          virtual ~xtreemfs_rwr_notifyRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }


          virtual void respond()
          {
            respond( *new xtreemfs_rwr_notifyResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_rwr_notifyRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_notifyRequest, 2010031291 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
        };


        class xtreemfs_rwr_notifyResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_notifyResponse() { }
          virtual ~xtreemfs_rwr_notifyResponse() {  }

          bool operator==( const xtreemfs_rwr_notifyResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_notifyResponse, 2010031291 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_rwr_statusRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_statusRequest()
            : max_local_obj_version( 0 )
          { }

          xtreemfs_rwr_statusRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            int64_t max_local_obj_version
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              max_local_obj_version( max_local_obj_version )
          { }

          virtual ~xtreemfs_rwr_statusRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          int64_t get_max_local_obj_version() const { return max_local_obj_version; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_max_local_obj_version( int64_t max_local_obj_version ) { this->max_local_obj_version = max_local_obj_version; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::ReplicaStatus& local_state
          )
          {
            respond
            (
              *new xtreemfs_rwr_statusResponse
                   (
                     local_state
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_rwr_statusRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   max_local_obj_version == other.max_local_obj_version;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_statusRequest, 2010031292 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "max_local_obj_version", 0 ), max_local_obj_version );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "max_local_obj_version", 0 ), max_local_obj_version );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          int64_t max_local_obj_version;
        };


        class xtreemfs_rwr_statusResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_statusResponse() { }

          xtreemfs_rwr_statusResponse
          (
            const org::xtreemfs::interfaces::ReplicaStatus& local_state
          )
            : local_state( local_state )
          { }

          virtual ~xtreemfs_rwr_statusResponse() {  }

          const org::xtreemfs::interfaces::ReplicaStatus& get_local_state() const { return local_state; }
          void set_local_state( const org::xtreemfs::interfaces::ReplicaStatus&  local_state ) { this->local_state = local_state; }

          bool operator==( const xtreemfs_rwr_statusResponse& other ) const
          {
            return local_state == other.local_state;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_statusResponse, 2010031292 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "local_state", 0 ), local_state );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "local_state", 0 ), local_state );
          }

        protected:
          org::xtreemfs::interfaces::ReplicaStatus local_state;
        };


        class xtreemfs_rwr_truncateRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_truncateRequest()
            : new_file_size( 0 ), object_version( 0 )
          { }

          xtreemfs_rwr_truncateRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            uint64_t new_file_size,
            uint64_t object_version
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              new_file_size( new_file_size ),
              object_version( object_version )
          { }

          virtual ~xtreemfs_rwr_truncateRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_new_file_size() const { return new_file_size; }
          uint64_t get_object_version() const { return object_version; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }
          void set_object_version( uint64_t object_version ) { this->object_version = object_version; }


          virtual void respond()
          {
            respond( *new xtreemfs_rwr_truncateResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_rwr_truncateRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   new_file_size == other.new_file_size
                   &&
                   object_version == other.object_version;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_truncateRequest, 2010031290 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), object_version );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          uint64_t new_file_size;
          uint64_t object_version;
        };


        class xtreemfs_rwr_truncateResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_truncateResponse() { }
          virtual ~xtreemfs_rwr_truncateResponse() {  }

          bool operator==( const xtreemfs_rwr_truncateResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_truncateResponse, 2010031290 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_rwr_updateRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_updateRequest()
            : object_number( 0 ), object_version( 0 ), offset( 0 )
          { }

          xtreemfs_rwr_updateRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            uint64_t object_number,
            uint64_t object_version,
            uint32_t offset,
            const org::xtreemfs::interfaces::ObjectData& object_data
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              object_number( object_number ),
              object_version( object_version ),
              offset( offset ),
              object_data( object_data )
          { }

          virtual ~xtreemfs_rwr_updateRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_object_number() const { return object_number; }
          uint64_t get_object_version() const { return object_version; }
          uint32_t get_offset() const { return offset; }
          const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
          void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
          void set_offset( uint32_t offset ) { this->offset = offset; }
          void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }


          virtual void respond()
          {
            respond( *new xtreemfs_rwr_updateResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_rwr_updateRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   object_number == other.object_number
                   &&
                   object_version == other.object_version
                   &&
                   offset == other.offset
                   &&
                   object_data == other.object_data;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_updateRequest, 2010031288 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), object_number );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), object_version );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), offset );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_number", 0 ), object_number );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
            offset = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "offset", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_data", 0 ), object_data );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          uint64_t object_number;
          uint64_t object_version;
          uint32_t offset;
          org::xtreemfs::interfaces::ObjectData object_data;
        };


        class xtreemfs_rwr_updateResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_rwr_updateResponse() { }
          virtual ~xtreemfs_rwr_updateResponse() {  }

          bool operator==( const xtreemfs_rwr_updateResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_updateResponse, 2010031288 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_internal_get_gmaxRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_internal_get_gmaxRequest() { }

          xtreemfs_internal_get_gmaxRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id
          )
            : file_credentials( file_credentials ), file_id( file_id )
          { }

          virtual ~xtreemfs_internal_get_gmaxRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::InternalGmax& _return_value
          )
          {
            respond
            (
              *new xtreemfs_internal_get_gmaxResponse
                   (
                     _return_value
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_internal_get_gmaxRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_gmaxRequest, 2010031256 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
        };


        class xtreemfs_internal_get_gmaxResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_internal_get_gmaxResponse() { }

          xtreemfs_internal_get_gmaxResponse
          (
            const org::xtreemfs::interfaces::InternalGmax& _return_value
          )
            : _return_value( _return_value )
          { }

          virtual ~xtreemfs_internal_get_gmaxResponse() {  }

          const org::xtreemfs::interfaces::InternalGmax& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::InternalGmax&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_internal_get_gmaxResponse& other ) const
          {
            return _return_value == other._return_value;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_gmaxResponse, 2010031256 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          org::xtreemfs::interfaces::InternalGmax _return_value;
        };


        class xtreemfs_internal_truncateRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_internal_truncateRequest()
            : new_file_size( 0 )
          { }

          xtreemfs_internal_truncateRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            uint64_t new_file_size
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              new_file_size( new_file_size )
          { }

          virtual ~xtreemfs_internal_truncateRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_new_file_size() const { return new_file_size; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
          )
          {
            respond
            (
              *new xtreemfs_internal_truncateResponse
                   (
                     osd_write_response
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_internal_truncateRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   new_file_size == other.new_file_size;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_truncateRequest, 2010031257 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          uint64_t new_file_size;
        };


        class xtreemfs_internal_truncateResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_internal_truncateResponse() { }

          xtreemfs_internal_truncateResponse
          (
            const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
          )
            : osd_write_response( osd_write_response )
          { }

          virtual ~xtreemfs_internal_truncateResponse() {  }

          const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }
          void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }

          bool operator==( const xtreemfs_internal_truncateResponse& other ) const
          {
            return osd_write_response == other.osd_write_response;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_truncateResponse, 2010031257 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "osd_write_response", 0 ), osd_write_response );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "osd_write_response", 0 ), osd_write_response );
          }

        protected:
          org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
        };


        class xtreemfs_internal_get_file_sizeRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_internal_get_file_sizeRequest() { }

          xtreemfs_internal_get_file_sizeRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id
          )
            : file_credentials( file_credentials ), file_id( file_id )
          { }

          virtual ~xtreemfs_internal_get_file_sizeRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }


          virtual void respond( uint64_t _return_value )
          {
            respond( *new xtreemfs_internal_get_file_sizeResponse( _return_value ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_internal_get_file_sizeRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_file_sizeRequest, 2010031258 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
        };


        class xtreemfs_internal_get_file_sizeResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_internal_get_file_sizeResponse()
            : _return_value( 0 )
          { }

          xtreemfs_internal_get_file_sizeResponse( uint64_t _return_value )
            : _return_value( _return_value )
          { }

          virtual ~xtreemfs_internal_get_file_sizeResponse() {  }

          uint64_t get__return_value() const { return _return_value; }
          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_internal_get_file_sizeResponse& other ) const
          {
            return _return_value == other._return_value;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_file_sizeResponse, 2010031258 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          uint64_t _return_value;
        };


        class xtreemfs_internal_read_localRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_internal_read_localRequest()
            : object_number( 0 ),
              object_version( 0 ),
              offset( 0 ),
              length( 0 ),
              attach_object_list( false )
          { }

          xtreemfs_internal_read_localRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            uint64_t object_number,
            uint64_t object_version,
            uint64_t offset,
            uint64_t length,
            bool attach_object_list,
            const org::xtreemfs::interfaces::ObjectListSet& required_objects
          )
            : file_credentials( file_credentials ),
              file_id( file_id ),
              object_number( object_number ),
              object_version( object_version ),
              offset( offset ),
              length( length ),
              attach_object_list( attach_object_list ),
              required_objects( required_objects )
          { }

          virtual ~xtreemfs_internal_read_localRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_object_number() const { return object_number; }
          uint64_t get_object_version() const { return object_version; }
          uint64_t get_offset() const { return offset; }
          uint64_t get_length() const { return length; }
          bool get_attach_object_list() const { return attach_object_list; }
          const org::xtreemfs::interfaces::ObjectListSet& get_required_objects() const { return required_objects; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
          void set_object_version( uint64_t object_version ) { this->object_version = object_version; }
          void set_offset( uint64_t offset ) { this->offset = offset; }
          void set_length( uint64_t length ) { this->length = length; }
          void set_attach_object_list( bool attach_object_list ) { this->attach_object_list = attach_object_list; }
          void set_required_objects( const org::xtreemfs::interfaces::ObjectListSet&  required_objects ) { this->required_objects = required_objects; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::InternalReadLocalResponse& _return_value
          )
          {
            respond
            (
              *new xtreemfs_internal_read_localResponse
                   (
                     _return_value
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_internal_read_localRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   object_number == other.object_number
                   &&
                   object_version == other.object_version
                   &&
                   offset == other.offset
                   &&
                   length == other.length
                   &&
                   attach_object_list == other.attach_object_list
                   &&
                   required_objects == other.required_objects;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_read_localRequest, 2010031259 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), object_number );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), object_version );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), offset );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), length );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "attach_object_list", 0 ), attach_object_list );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "required_objects", 0 ), required_objects );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_number", 0 ), object_number );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "offset", 0 ), offset );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "length", 0 ), length );
            attach_object_list = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "attach_object_list", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "required_objects", 0 ), required_objects );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          uint64_t object_number;
          uint64_t object_version;
          uint64_t offset;
          uint64_t length;
          bool attach_object_list;
          org::xtreemfs::interfaces::ObjectListSet required_objects;
        };


        class xtreemfs_internal_read_localResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_internal_read_localResponse() { }

          xtreemfs_internal_read_localResponse
          (
            const org::xtreemfs::interfaces::InternalReadLocalResponse& _return_value
          )
            : _return_value( _return_value )
          { }

          virtual ~xtreemfs_internal_read_localResponse() {  }

          const org::xtreemfs::interfaces::InternalReadLocalResponse& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::InternalReadLocalResponse&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_internal_read_localResponse& other ) const
          {
            return _return_value == other._return_value;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_read_localResponse, 2010031259 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          org::xtreemfs::interfaces::InternalReadLocalResponse _return_value;
        };


        class xtreemfs_internal_get_object_setRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_internal_get_object_setRequest() { }

          xtreemfs_internal_get_object_setRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id
          )
            : file_credentials( file_credentials ), file_id( file_id )
          { }

          virtual ~xtreemfs_internal_get_object_setRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::ObjectList& _return_value
          )
          {
            respond
            (
              *new xtreemfs_internal_get_object_setResponse
                   (
                     _return_value
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_internal_get_object_setRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_object_setRequest, 2010031260 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
        };


        class xtreemfs_internal_get_object_setResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_internal_get_object_setResponse() { }

          xtreemfs_internal_get_object_setResponse
          (
            const org::xtreemfs::interfaces::ObjectList& _return_value
          )
            : _return_value( _return_value )
          { }

          virtual ~xtreemfs_internal_get_object_setResponse() {  }

          const org::xtreemfs::interfaces::ObjectList& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::ObjectList&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_internal_get_object_setResponse& other ) const
          {
            return _return_value == other._return_value;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_object_setResponse, 2010031260 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          org::xtreemfs::interfaces::ObjectList _return_value;
        };


        class xtreemfs_lock_acquireRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lock_acquireRequest()
            : client_pid( 0 ), offset( 0 ), length( 0 ), exclusive( false )
          { }

          xtreemfs_lock_acquireRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& client_uuid,
            int32_t client_pid,
            const string& file_id,
            uint64_t offset,
            uint64_t length,
            bool exclusive
          )
            : file_credentials( file_credentials ),
              client_uuid( client_uuid ),
              client_pid( client_pid ),
              file_id( file_id ),
              offset( offset ),
              length( length ),
              exclusive( exclusive )
          { }

          virtual ~xtreemfs_lock_acquireRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_client_uuid() const { return client_uuid; }
          int32_t get_client_pid() const { return client_pid; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_offset() const { return offset; }
          uint64_t get_length() const { return length; }
          bool get_exclusive() const { return exclusive; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_client_uuid( const string& client_uuid ) { this->client_uuid = client_uuid; }
          void set_client_pid( int32_t client_pid ) { this->client_pid = client_pid; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_offset( uint64_t offset ) { this->offset = offset; }
          void set_length( uint64_t length ) { this->length = length; }
          void set_exclusive( bool exclusive ) { this->exclusive = exclusive; }


          virtual void respond( const org::xtreemfs::interfaces::Lock& _return_value )
          {
            respond( *new xtreemfs_lock_acquireResponse( _return_value ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_lock_acquireRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   client_uuid == other.client_uuid
                   &&
                   client_pid == other.client_pid
                   &&
                   file_id == other.file_id
                   &&
                   offset == other.offset
                   &&
                   length == other.length
                   &&
                   exclusive == other.exclusive;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_acquireRequest, 2010031266 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_uuid", 0 ), client_uuid );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_pid", 0 ), client_pid );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), offset );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), length );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "exclusive", 0 ), exclusive );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "client_uuid", 0 ), client_uuid );
            client_pid = unmarshaller.read_int32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "client_pid", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "offset", 0 ), offset );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "length", 0 ), length );
            exclusive = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "exclusive", 0 ) );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string client_uuid;
          int32_t client_pid;
          string file_id;
          uint64_t offset;
          uint64_t length;
          bool exclusive;
        };


        class xtreemfs_lock_acquireResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_lock_acquireResponse() { }

          xtreemfs_lock_acquireResponse
          (
            const org::xtreemfs::interfaces::Lock& _return_value
          )
            : _return_value( _return_value )
          { }

          virtual ~xtreemfs_lock_acquireResponse() {  }

          const org::xtreemfs::interfaces::Lock& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::Lock&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_lock_acquireResponse& other ) const
          {
            return _return_value == other._return_value;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_acquireResponse, 2010031266 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          org::xtreemfs::interfaces::Lock _return_value;
        };


        class xtreemfs_lock_checkRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lock_checkRequest()
            : client_pid( 0 ), offset( 0 ), length( 0 ), exclusive( false )
          { }

          xtreemfs_lock_checkRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& client_uuid,
            int32_t client_pid,
            const string& file_id,
            uint64_t offset,
            uint64_t length,
            bool exclusive
          )
            : file_credentials( file_credentials ),
              client_uuid( client_uuid ),
              client_pid( client_pid ),
              file_id( file_id ),
              offset( offset ),
              length( length ),
              exclusive( exclusive )
          { }

          virtual ~xtreemfs_lock_checkRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_client_uuid() const { return client_uuid; }
          int32_t get_client_pid() const { return client_pid; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_offset() const { return offset; }
          uint64_t get_length() const { return length; }
          bool get_exclusive() const { return exclusive; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_client_uuid( const string& client_uuid ) { this->client_uuid = client_uuid; }
          void set_client_pid( int32_t client_pid ) { this->client_pid = client_pid; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_offset( uint64_t offset ) { this->offset = offset; }
          void set_length( uint64_t length ) { this->length = length; }
          void set_exclusive( bool exclusive ) { this->exclusive = exclusive; }


          virtual void respond( const org::xtreemfs::interfaces::Lock& _return_value )
          {
            respond( *new xtreemfs_lock_checkResponse( _return_value ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_lock_checkRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   client_uuid == other.client_uuid
                   &&
                   client_pid == other.client_pid
                   &&
                   file_id == other.file_id
                   &&
                   offset == other.offset
                   &&
                   length == other.length
                   &&
                   exclusive == other.exclusive;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_checkRequest, 2010031267 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_uuid", 0 ), client_uuid );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_pid", 0 ), client_pid );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), offset );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), length );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "exclusive", 0 ), exclusive );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "client_uuid", 0 ), client_uuid );
            client_pid = unmarshaller.read_int32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "client_pid", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "offset", 0 ), offset );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "length", 0 ), length );
            exclusive = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "exclusive", 0 ) );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string client_uuid;
          int32_t client_pid;
          string file_id;
          uint64_t offset;
          uint64_t length;
          bool exclusive;
        };


        class xtreemfs_lock_checkResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_lock_checkResponse() { }

          xtreemfs_lock_checkResponse
          (
            const org::xtreemfs::interfaces::Lock& _return_value
          )
            : _return_value( _return_value )
          { }

          virtual ~xtreemfs_lock_checkResponse() {  }

          const org::xtreemfs::interfaces::Lock& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::Lock&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_lock_checkResponse& other ) const
          {
            return _return_value == other._return_value;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_checkResponse, 2010031267 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "_return_value", 0 ), _return_value );
          }

        protected:
          org::xtreemfs::interfaces::Lock _return_value;
        };


        class xtreemfs_lock_releaseRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lock_releaseRequest() { }

          xtreemfs_lock_releaseRequest
          (
            const org::xtreemfs::interfaces::FileCredentials& file_credentials,
            const string& file_id,
            const org::xtreemfs::interfaces::Lock& lock
          )
            : file_credentials( file_credentials ), file_id( file_id ), lock( lock )
          { }

          virtual ~xtreemfs_lock_releaseRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          const org::xtreemfs::interfaces::Lock& get_lock() const { return lock; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_lock( const org::xtreemfs::interfaces::Lock&  lock ) { this->lock = lock; }


          virtual void respond()
          {
            respond( *new xtreemfs_lock_releaseResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_lock_releaseRequest& other ) const
          {
            return file_credentials == other.file_credentials
                   &&
                   file_id == other.file_id
                   &&
                   lock == other.lock;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_releaseRequest, 2010031268 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), file_id );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "lock", 0 ), lock );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "lock", 0 ), lock );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          org::xtreemfs::interfaces::Lock lock;
        };


        class xtreemfs_lock_releaseResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_lock_releaseResponse() { }
          virtual ~xtreemfs_lock_releaseResponse() {  }

          bool operator==( const xtreemfs_lock_releaseResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_releaseResponse, 2010031268 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_pingRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_pingRequest() { }

          xtreemfs_pingRequest
          (
            const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates
          )
            : coordinates( coordinates )
          { }

          virtual ~xtreemfs_pingRequest() {  }

          const org::xtreemfs::interfaces::VivaldiCoordinates& get_coordinates() const { return coordinates; }
          void set_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  coordinates ) { this->coordinates = coordinates; }


          virtual void
          respond
          (
            const org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates
          )
          {
            respond
            (
              *new xtreemfs_pingResponse
                   (
                     remote_coordinates
                   )
            );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_pingRequest& other ) const
          {
            return coordinates == other.coordinates;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_pingRequest, 2010031276 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "coordinates", 0 ), coordinates );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "coordinates", 0 ), coordinates );
          }

        protected:
          org::xtreemfs::interfaces::VivaldiCoordinates coordinates;
        };


        class xtreemfs_pingResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_pingResponse() { }

          xtreemfs_pingResponse
          (
            const org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates
          )
            : remote_coordinates( remote_coordinates )
          { }

          virtual ~xtreemfs_pingResponse() {  }

          const org::xtreemfs::interfaces::VivaldiCoordinates& get_remote_coordinates() const { return remote_coordinates; }
          void set_remote_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  remote_coordinates ) { this->remote_coordinates = remote_coordinates; }

          bool operator==( const xtreemfs_pingResponse& other ) const
          {
            return remote_coordinates == other.remote_coordinates;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_pingResponse, 2010031276 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "remote_coordinates", 0 ), remote_coordinates );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "remote_coordinates", 0 ), remote_coordinates );
          }

        protected:
          org::xtreemfs::interfaces::VivaldiCoordinates remote_coordinates;
        };


        class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownRequest() { }
          virtual ~xtreemfs_shutdownRequest() {  }


          virtual void respond()
          {
            respond( *new xtreemfs_shutdownResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 2010031286 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownResponse() { }
          virtual ~xtreemfs_shutdownResponse() {  }

          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 2010031286 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class ConcurrentModificationException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          ConcurrentModificationException() { }

          ConcurrentModificationException( const string& stack_trace )
            : stack_trace( stack_trace )
          { }

          ConcurrentModificationException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~ConcurrentModificationException() throw() { ; }

          const string& get_stack_trace() const { return stack_trace; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ConcurrentModificationException, 2010031217 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
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


        class errnoException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          errnoException()
            : error_code( 0 )
          { }

          errnoException
          (
            uint32_t error_code,
            const string& error_message,
            const string& stack_trace
          )
            : error_code( error_code ),
              error_message( error_message ),
              stack_trace( stack_trace )
          { }

          errnoException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~errnoException() throw() { ; }

          uint32_t get_error_code() const { return error_code; }
          const string& get_error_message() const { return error_message; }
          const string& get_stack_trace() const { return stack_trace; }
          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          void set_error_message( const string& error_message ) { this->error_message = error_message; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( errnoException, 2010031218 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_code", 0 ), error_code );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_message", 0 ), error_message );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            error_code = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_code", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_message", 0 ), error_message );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
          }

          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const
          {
            return new errnoException( error_code, error_message, stack_trace );
          }

          virtual void throwStackClone() const
          {
            throw errnoException( error_code, error_message, stack_trace );
          }

        protected:
          uint32_t error_code;
          string error_message;
          string stack_trace;
        };


        class InvalidArgumentException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          InvalidArgumentException() { }

          InvalidArgumentException( const string& error_message )
            : error_message( error_message )
          { }

          InvalidArgumentException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~InvalidArgumentException() throw() { ; }

          const string& get_error_message() const { return error_message; }
          void set_error_message( const string& error_message ) { this->error_message = error_message; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( InvalidArgumentException, 2010031219 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_message", 0 ), error_message );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_message", 0 ), error_message );
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


        class OSDException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          OSDException()
            : error_code( 0 )
          { }

          OSDException
          (
            uint32_t error_code,
            const string& error_message,
            const string& stack_trace
          )
            : error_code( error_code ),
              error_message( error_message ),
              stack_trace( stack_trace )
          { }

          OSDException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~OSDException() throw() { ; }

          uint32_t get_error_code() const { return error_code; }
          const string& get_error_message() const { return error_message; }
          const string& get_stack_trace() const { return stack_trace; }
          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          void set_error_message( const string& error_message ) { this->error_message = error_message; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( OSDException, 2010031220 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_code", 0 ), error_code );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_message", 0 ), error_message );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            error_code = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_code", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_message", 0 ), error_message );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
          }

          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const
          {
            return new OSDException( error_code, error_message, stack_trace );
          }

          virtual void throwStackClone() const
          {
            throw OSDException( error_code, error_message, stack_trace );
          }

        protected:
          uint32_t error_code;
          string error_message;
          string stack_trace;
        };


        class ProtocolException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          ProtocolException()
            : accept_stat( 0 ), error_code( 0 )
          { }

          ProtocolException
          (
            uint32_t accept_stat,
            uint32_t error_code,
            const string& stack_trace
          )
            : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace )
          { }

          ProtocolException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~ProtocolException() throw() { ; }

          uint32_t get_accept_stat() const { return accept_stat; }
          uint32_t get_error_code() const { return error_code; }
          const string& get_stack_trace() const { return stack_trace; }
          void set_accept_stat( uint32_t accept_stat ) { this->accept_stat = accept_stat; }
          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ProtocolException, 2010031221 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "accept_stat", 0 ), accept_stat );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "error_code", 0 ), error_code );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            accept_stat = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "accept_stat", 0 ) );
            error_code = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "error_code", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "stack_trace", 0 ), stack_trace );
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


        class RedirectException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          RedirectException() { }

          RedirectException( const string& to_uuid )
            : to_uuid( to_uuid )
          { }

          RedirectException( const char* what ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~RedirectException() throw() { ; }

          const string& get_to_uuid() const { return to_uuid; }
          void set_to_uuid( const string& to_uuid ) { this->to_uuid = to_uuid; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( RedirectException, 2010031222 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "to_uuid", 0 ), to_uuid );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "to_uuid", 0 ), to_uuid );
          }

          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const
          {
            return new RedirectException( to_uuid );
          }

          virtual void throwStackClone() const
          {
            throw RedirectException( to_uuid );
          }

        protected:
          string to_uuid;
        };
      };


      class OSDInterfaceMessageFactory
        : public ::yield::concurrency::MessageFactory,
          private OSDInterfaceMessages
      {
      public:
        // yield::concurrency::MessageFactory
        virtual ::yield::concurrency::ExceptionResponse*
        createExceptionResponse
        (
          uint32_t type_id
        )
        {
          switch ( type_id )
          {
            case 2010031217: return new ConcurrentModificationException;
            case 2010031218: return new errnoException;
            case 2010031219: return new InvalidArgumentException;
            case 2010031220: return new OSDException;
            case 2010031221: return new ProtocolException;
            case 2010031222: return new RedirectException;
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
          else if ( strcmp( type_name, "errnoException" ) == 0 ) return new errnoException;
          else if ( strcmp( type_name, "InvalidArgumentException" ) == 0 ) return new InvalidArgumentException;
          else if ( strcmp( type_name, "OSDException" ) == 0 ) return new OSDException;
          else if ( strcmp( type_name, "ProtocolException" ) == 0 ) return new ProtocolException;
          else if ( strcmp( type_name, "RedirectException" ) == 0 ) return new RedirectException;
          else return NULL;
        }

        virtual ::yield::concurrency::Request* createRequest( uint32_t type_id )
        {
          switch ( type_id )
          {
            case 2010031226: return new readRequest;
            case 2010031227: return new truncateRequest;
            case 2010031228: return new unlinkRequest;
            case 2010031229: return new writeRequest;
            case 2010031236: return new xtreemfs_broadcast_gmaxRequest;
            case 2010031237: return new xtreemfs_check_objectRequest;
            case 2010031246: return new xtreemfs_cleanup_get_resultsRequest;
            case 2010031247: return new xtreemfs_cleanup_is_runningRequest;
            case 2010031248: return new xtreemfs_cleanup_startRequest;
            case 2010031249: return new xtreemfs_cleanup_statusRequest;
            case 2010031250: return new xtreemfs_cleanup_stopRequest;
            case 2010031289: return new xtreemfs_rwr_fetchRequest;
            case 2010031287: return new xtreemfs_rwr_flease_msgRequest;
            case 2010031291: return new xtreemfs_rwr_notifyRequest;
            case 2010031292: return new xtreemfs_rwr_statusRequest;
            case 2010031290: return new xtreemfs_rwr_truncateRequest;
            case 2010031288: return new xtreemfs_rwr_updateRequest;
            case 2010031256: return new xtreemfs_internal_get_gmaxRequest;
            case 2010031257: return new xtreemfs_internal_truncateRequest;
            case 2010031258: return new xtreemfs_internal_get_file_sizeRequest;
            case 2010031259: return new xtreemfs_internal_read_localRequest;
            case 2010031260: return new xtreemfs_internal_get_object_setRequest;
            case 2010031266: return new xtreemfs_lock_acquireRequest;
            case 2010031267: return new xtreemfs_lock_checkRequest;
            case 2010031268: return new xtreemfs_lock_releaseRequest;
            case 2010031276: return new xtreemfs_pingRequest;
            case 2010031286: return new xtreemfs_shutdownRequest;
            default: return NULL;
          }
        }

        virtual ::yield::concurrency::Request* createRequest( const char* type_name )
        {
          if ( strcmp( type_name, "readRequest" ) == 0 ) return new readRequest;
          else if ( strcmp( type_name, "truncateRequest" ) == 0 ) return new truncateRequest;
          else if ( strcmp( type_name, "unlinkRequest" ) == 0 ) return new unlinkRequest;
          else if ( strcmp( type_name, "writeRequest" ) == 0 ) return new writeRequest;
          else if ( strcmp( type_name, "xtreemfs_broadcast_gmaxRequest" ) == 0 ) return new xtreemfs_broadcast_gmaxRequest;
          else if ( strcmp( type_name, "xtreemfs_check_objectRequest" ) == 0 ) return new xtreemfs_check_objectRequest;
          else if ( strcmp( type_name, "xtreemfs_cleanup_get_resultsRequest" ) == 0 ) return new xtreemfs_cleanup_get_resultsRequest;
          else if ( strcmp( type_name, "xtreemfs_cleanup_is_runningRequest" ) == 0 ) return new xtreemfs_cleanup_is_runningRequest;
          else if ( strcmp( type_name, "xtreemfs_cleanup_startRequest" ) == 0 ) return new xtreemfs_cleanup_startRequest;
          else if ( strcmp( type_name, "xtreemfs_cleanup_statusRequest" ) == 0 ) return new xtreemfs_cleanup_statusRequest;
          else if ( strcmp( type_name, "xtreemfs_cleanup_stopRequest" ) == 0 ) return new xtreemfs_cleanup_stopRequest;
          else if ( strcmp( type_name, "xtreemfs_rwr_fetchRequest" ) == 0 ) return new xtreemfs_rwr_fetchRequest;
          else if ( strcmp( type_name, "xtreemfs_rwr_flease_msgRequest" ) == 0 ) return new xtreemfs_rwr_flease_msgRequest;
          else if ( strcmp( type_name, "xtreemfs_rwr_notifyRequest" ) == 0 ) return new xtreemfs_rwr_notifyRequest;
          else if ( strcmp( type_name, "xtreemfs_rwr_statusRequest" ) == 0 ) return new xtreemfs_rwr_statusRequest;
          else if ( strcmp( type_name, "xtreemfs_rwr_truncateRequest" ) == 0 ) return new xtreemfs_rwr_truncateRequest;
          else if ( strcmp( type_name, "xtreemfs_rwr_updateRequest" ) == 0 ) return new xtreemfs_rwr_updateRequest;
          else if ( strcmp( type_name, "xtreemfs_internal_get_gmaxRequest" ) == 0 ) return new xtreemfs_internal_get_gmaxRequest;
          else if ( strcmp( type_name, "xtreemfs_internal_truncateRequest" ) == 0 ) return new xtreemfs_internal_truncateRequest;
          else if ( strcmp( type_name, "xtreemfs_internal_get_file_sizeRequest" ) == 0 ) return new xtreemfs_internal_get_file_sizeRequest;
          else if ( strcmp( type_name, "xtreemfs_internal_read_localRequest" ) == 0 ) return new xtreemfs_internal_read_localRequest;
          else if ( strcmp( type_name, "xtreemfs_internal_get_object_setRequest" ) == 0 ) return new xtreemfs_internal_get_object_setRequest;
          else if ( strcmp( type_name, "xtreemfs_lock_acquireRequest" ) == 0 ) return new xtreemfs_lock_acquireRequest;
          else if ( strcmp( type_name, "xtreemfs_lock_checkRequest" ) == 0 ) return new xtreemfs_lock_checkRequest;
          else if ( strcmp( type_name, "xtreemfs_lock_releaseRequest" ) == 0 ) return new xtreemfs_lock_releaseRequest;
          else if ( strcmp( type_name, "xtreemfs_pingRequest" ) == 0 ) return new xtreemfs_pingRequest;
          else if ( strcmp( type_name, "xtreemfs_shutdownRequest" ) == 0 ) return new xtreemfs_shutdownRequest;
          else return NULL;
        }

        virtual ::yield::concurrency::Response* createResponse( uint32_t type_id )
        {
          switch ( type_id )
          {
            case 2010031226: return new readResponse;
            case 2010031227: return new truncateResponse;
            case 2010031228: return new unlinkResponse;
            case 2010031229: return new writeResponse;
            case 2010031236: return new xtreemfs_broadcast_gmaxResponse;
            case 2010031237: return new xtreemfs_check_objectResponse;
            case 2010031246: return new xtreemfs_cleanup_get_resultsResponse;
            case 2010031247: return new xtreemfs_cleanup_is_runningResponse;
            case 2010031248: return new xtreemfs_cleanup_startResponse;
            case 2010031249: return new xtreemfs_cleanup_statusResponse;
            case 2010031250: return new xtreemfs_cleanup_stopResponse;
            case 2010031289: return new xtreemfs_rwr_fetchResponse;
            case 2010031287: return new xtreemfs_rwr_flease_msgResponse;
            case 2010031291: return new xtreemfs_rwr_notifyResponse;
            case 2010031292: return new xtreemfs_rwr_statusResponse;
            case 2010031290: return new xtreemfs_rwr_truncateResponse;
            case 2010031288: return new xtreemfs_rwr_updateResponse;
            case 2010031256: return new xtreemfs_internal_get_gmaxResponse;
            case 2010031257: return new xtreemfs_internal_truncateResponse;
            case 2010031258: return new xtreemfs_internal_get_file_sizeResponse;
            case 2010031259: return new xtreemfs_internal_read_localResponse;
            case 2010031260: return new xtreemfs_internal_get_object_setResponse;
            case 2010031266: return new xtreemfs_lock_acquireResponse;
            case 2010031267: return new xtreemfs_lock_checkResponse;
            case 2010031268: return new xtreemfs_lock_releaseResponse;
            case 2010031276: return new xtreemfs_pingResponse;
            case 2010031286: return new xtreemfs_shutdownResponse;
            default: return NULL;
          }
        }

        virtual ::yield::concurrency::Response* createResponse( const char* type_name )
        {
          if ( strcmp( type_name, "readResponse" ) == 0 ) return new readResponse;
          else if ( strcmp( type_name, "truncateResponse" ) == 0 ) return new truncateResponse;
          else if ( strcmp( type_name, "unlinkResponse" ) == 0 ) return new unlinkResponse;
          else if ( strcmp( type_name, "writeResponse" ) == 0 ) return new writeResponse;
          else if ( strcmp( type_name, "xtreemfs_broadcast_gmaxResponse" ) == 0 ) return new xtreemfs_broadcast_gmaxResponse;
          else if ( strcmp( type_name, "xtreemfs_check_objectResponse" ) == 0 ) return new xtreemfs_check_objectResponse;
          else if ( strcmp( type_name, "xtreemfs_cleanup_get_resultsResponse" ) == 0 ) return new xtreemfs_cleanup_get_resultsResponse;
          else if ( strcmp( type_name, "xtreemfs_cleanup_is_runningResponse" ) == 0 ) return new xtreemfs_cleanup_is_runningResponse;
          else if ( strcmp( type_name, "xtreemfs_cleanup_startResponse" ) == 0 ) return new xtreemfs_cleanup_startResponse;
          else if ( strcmp( type_name, "xtreemfs_cleanup_statusResponse" ) == 0 ) return new xtreemfs_cleanup_statusResponse;
          else if ( strcmp( type_name, "xtreemfs_cleanup_stopResponse" ) == 0 ) return new xtreemfs_cleanup_stopResponse;
          else if ( strcmp( type_name, "xtreemfs_rwr_fetchResponse" ) == 0 ) return new xtreemfs_rwr_fetchResponse;
          else if ( strcmp( type_name, "xtreemfs_rwr_flease_msgResponse" ) == 0 ) return new xtreemfs_rwr_flease_msgResponse;
          else if ( strcmp( type_name, "xtreemfs_rwr_notifyResponse" ) == 0 ) return new xtreemfs_rwr_notifyResponse;
          else if ( strcmp( type_name, "xtreemfs_rwr_statusResponse" ) == 0 ) return new xtreemfs_rwr_statusResponse;
          else if ( strcmp( type_name, "xtreemfs_rwr_truncateResponse" ) == 0 ) return new xtreemfs_rwr_truncateResponse;
          else if ( strcmp( type_name, "xtreemfs_rwr_updateResponse" ) == 0 ) return new xtreemfs_rwr_updateResponse;
          else if ( strcmp( type_name, "xtreemfs_internal_get_gmaxResponse" ) == 0 ) return new xtreemfs_internal_get_gmaxResponse;
          else if ( strcmp( type_name, "xtreemfs_internal_truncateResponse" ) == 0 ) return new xtreemfs_internal_truncateResponse;
          else if ( strcmp( type_name, "xtreemfs_internal_get_file_sizeResponse" ) == 0 ) return new xtreemfs_internal_get_file_sizeResponse;
          else if ( strcmp( type_name, "xtreemfs_internal_read_localResponse" ) == 0 ) return new xtreemfs_internal_read_localResponse;
          else if ( strcmp( type_name, "xtreemfs_internal_get_object_setResponse" ) == 0 ) return new xtreemfs_internal_get_object_setResponse;
          else if ( strcmp( type_name, "xtreemfs_lock_acquireResponse" ) == 0 ) return new xtreemfs_lock_acquireResponse;
          else if ( strcmp( type_name, "xtreemfs_lock_checkResponse" ) == 0 ) return new xtreemfs_lock_checkResponse;
          else if ( strcmp( type_name, "xtreemfs_lock_releaseResponse" ) == 0 ) return new xtreemfs_lock_releaseResponse;
          else if ( strcmp( type_name, "xtreemfs_pingResponse" ) == 0 ) return new xtreemfs_pingResponse;
          else if ( strcmp( type_name, "xtreemfs_shutdownResponse" ) == 0 ) return new xtreemfs_shutdownResponse;
          else return NULL;
        }

      };


      class OSDInterfaceRequestHandler
        : public ::yield::concurrency::RequestHandler,
          protected OSDInterfaceMessages
      {
      public:
        OSDInterfaceRequestHandler()  // Subclasses must implement
          : _interface( NULL ) // all relevant handle*Request methods
        { }

        // Steals interface_ to allow for *new
        OSDInterfaceRequestHandler( OSDInterface& _interface )
          : _interface( &_interface )
        { }

        virtual ~OSDInterfaceRequestHandler()
        {
          delete _interface;
        }

        // yield::concurrency::EventHandler
        virtual const char* get_name() const
        {
          return "OSDInterface";
        }

        // yield::concurrency::RequestHandler

        virtual void handleRequest( ::yield::concurrency::Request& request )
        {
          // Switch on the request types that this interface handles, unwrap the corresponding requests and delegate to _interface
          switch ( request.get_type_id() )
          {
            case 2010031226UL: handlereadRequest( static_cast<readRequest&>( request ) ); return;
            case 2010031227UL: handletruncateRequest( static_cast<truncateRequest&>( request ) ); return;
            case 2010031228UL: handleunlinkRequest( static_cast<unlinkRequest&>( request ) ); return;
            case 2010031229UL: handlewriteRequest( static_cast<writeRequest&>( request ) ); return;
            case 2010031236UL: handlextreemfs_broadcast_gmaxRequest( static_cast<xtreemfs_broadcast_gmaxRequest&>( request ) ); return;
            case 2010031237UL: handlextreemfs_check_objectRequest( static_cast<xtreemfs_check_objectRequest&>( request ) ); return;
            case 2010031246UL: handlextreemfs_cleanup_get_resultsRequest( static_cast<xtreemfs_cleanup_get_resultsRequest&>( request ) ); return;
            case 2010031247UL: handlextreemfs_cleanup_is_runningRequest( static_cast<xtreemfs_cleanup_is_runningRequest&>( request ) ); return;
            case 2010031248UL: handlextreemfs_cleanup_startRequest( static_cast<xtreemfs_cleanup_startRequest&>( request ) ); return;
            case 2010031249UL: handlextreemfs_cleanup_statusRequest( static_cast<xtreemfs_cleanup_statusRequest&>( request ) ); return;
            case 2010031250UL: handlextreemfs_cleanup_stopRequest( static_cast<xtreemfs_cleanup_stopRequest&>( request ) ); return;
            case 2010031289UL: handlextreemfs_rwr_fetchRequest( static_cast<xtreemfs_rwr_fetchRequest&>( request ) ); return;
            case 2010031287UL: handlextreemfs_rwr_flease_msgRequest( static_cast<xtreemfs_rwr_flease_msgRequest&>( request ) ); return;
            case 2010031291UL: handlextreemfs_rwr_notifyRequest( static_cast<xtreemfs_rwr_notifyRequest&>( request ) ); return;
            case 2010031292UL: handlextreemfs_rwr_statusRequest( static_cast<xtreemfs_rwr_statusRequest&>( request ) ); return;
            case 2010031290UL: handlextreemfs_rwr_truncateRequest( static_cast<xtreemfs_rwr_truncateRequest&>( request ) ); return;
            case 2010031288UL: handlextreemfs_rwr_updateRequest( static_cast<xtreemfs_rwr_updateRequest&>( request ) ); return;
            case 2010031256UL: handlextreemfs_internal_get_gmaxRequest( static_cast<xtreemfs_internal_get_gmaxRequest&>( request ) ); return;
            case 2010031257UL: handlextreemfs_internal_truncateRequest( static_cast<xtreemfs_internal_truncateRequest&>( request ) ); return;
            case 2010031258UL: handlextreemfs_internal_get_file_sizeRequest( static_cast<xtreemfs_internal_get_file_sizeRequest&>( request ) ); return;
            case 2010031259UL: handlextreemfs_internal_read_localRequest( static_cast<xtreemfs_internal_read_localRequest&>( request ) ); return;
            case 2010031260UL: handlextreemfs_internal_get_object_setRequest( static_cast<xtreemfs_internal_get_object_setRequest&>( request ) ); return;
            case 2010031266UL: handlextreemfs_lock_acquireRequest( static_cast<xtreemfs_lock_acquireRequest&>( request ) ); return;
            case 2010031267UL: handlextreemfs_lock_checkRequest( static_cast<xtreemfs_lock_checkRequest&>( request ) ); return;
            case 2010031268UL: handlextreemfs_lock_releaseRequest( static_cast<xtreemfs_lock_releaseRequest&>( request ) ); return;
            case 2010031276UL: handlextreemfs_pingRequest( static_cast<xtreemfs_pingRequest&>( request ) ); return;
            case 2010031286UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( request ) ); return;
            default: ::yield::concurrency::Request::dec_ref( request ); return;
          }
        }

      protected:
        virtual void handlereadRequest( readRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::ObjectData object_data( __request.get_object_data() );

              _interface->read
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_object_number(),
                __request.get_object_version(),
                __request.get_offset(),
                __request.get_length(),
                object_data
              );

              __request.respond( object_data );
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

          readRequest::dec_ref( __request );
        }

        virtual void handletruncateRequest( truncateRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;

              _interface->truncate
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_new_file_size(),
                osd_write_response
              );

              __request.respond( osd_write_response );
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

          truncateRequest::dec_ref( __request );
        }

        virtual void handleunlinkRequest( unlinkRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->unlink
              (
                __request.get_file_credentials(),
                __request.get_file_id()
              );
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

          unlinkRequest::dec_ref( __request );
        }

        virtual void handlewriteRequest( writeRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;

              _interface->write
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_object_number(),
                __request.get_object_version(),
                __request.get_offset(),
                __request.get_lease_timeout(),
                __request.get_object_data(),
                osd_write_response
              );

              __request.respond( osd_write_response );
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

          writeRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_broadcast_gmaxRequest( xtreemfs_broadcast_gmaxRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_broadcast_gmax
              (
                __request.get_file_id(),
                __request.get_truncate_epoch(),
                __request.get_last_object(),
                __request.get_file_size()
              );
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

          xtreemfs_broadcast_gmaxRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_check_objectRequest( xtreemfs_check_objectRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::ObjectData _return_value =

              _interface->xtreemfs_check_object
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_object_number(),
                __request.get_object_version()
              );

              __request.respond( _return_value );
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

          xtreemfs_check_objectRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_cleanup_get_resultsRequest( xtreemfs_cleanup_get_resultsRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::StringSet results;

              _interface->xtreemfs_cleanup_get_results( results );

              __request.respond( results );
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

          xtreemfs_cleanup_get_resultsRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_cleanup_is_runningRequest( xtreemfs_cleanup_is_runningRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              bool is_running;

              _interface->xtreemfs_cleanup_is_running( is_running );

              __request.respond( is_running );
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

          xtreemfs_cleanup_is_runningRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_cleanup_startRequest( xtreemfs_cleanup_startRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_cleanup_start
              (
                __request.get_remove_zombies(),
                __request.get_remove_unavail_volume(),
                __request.get_lost_and_found()
              );
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

          xtreemfs_cleanup_startRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_cleanup_statusRequest( xtreemfs_cleanup_statusRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              string status;

              _interface->xtreemfs_cleanup_status( status );

              __request.respond( status );
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

          xtreemfs_cleanup_statusRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_cleanup_stopRequest( xtreemfs_cleanup_stopRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_cleanup_stop();
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

          xtreemfs_cleanup_stopRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_rwr_fetchRequest( xtreemfs_rwr_fetchRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::ObjectData object_data;

              _interface->xtreemfs_rwr_fetch
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_object_number(),
                __request.get_object_version(),
                object_data
              );

              __request.respond( object_data );
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

          xtreemfs_rwr_fetchRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_rwr_flease_msgRequest( xtreemfs_rwr_flease_msgRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_rwr_flease_msg
              (
                __request.get_fleaseMessage(),
                __request.get_senderHostname(),
                __request.get_senderPort()
              );
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

          xtreemfs_rwr_flease_msgRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_rwr_notifyRequest( xtreemfs_rwr_notifyRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_rwr_notify
              (
                __request.get_file_credentials(),
                __request.get_file_id()
              );
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

          xtreemfs_rwr_notifyRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_rwr_statusRequest( xtreemfs_rwr_statusRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::ReplicaStatus local_state;

              _interface->xtreemfs_rwr_status
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_max_local_obj_version(),
                local_state
              );

              __request.respond( local_state );
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

          xtreemfs_rwr_statusRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_rwr_truncateRequest( xtreemfs_rwr_truncateRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_rwr_truncate
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_new_file_size(),
                __request.get_object_version()
              );
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

          xtreemfs_rwr_truncateRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_rwr_updateRequest( xtreemfs_rwr_updateRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_rwr_update
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_object_number(),
                __request.get_object_version(),
                __request.get_offset(),
                __request.get_object_data()
              );
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

          xtreemfs_rwr_updateRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_internal_get_gmaxRequest( xtreemfs_internal_get_gmaxRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::InternalGmax _return_value =

              _interface->xtreemfs_internal_get_gmax
              (
                __request.get_file_credentials(),
                __request.get_file_id()
              );

              __request.respond( _return_value );
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

          xtreemfs_internal_get_gmaxRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_internal_truncateRequest( xtreemfs_internal_truncateRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;

              _interface->xtreemfs_internal_truncate
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_new_file_size(),
                osd_write_response
              );

              __request.respond( osd_write_response );
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

          xtreemfs_internal_truncateRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_internal_get_file_sizeRequest( xtreemfs_internal_get_file_sizeRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              uint64_t _return_value =

              _interface->xtreemfs_internal_get_file_size
              (
                __request.get_file_credentials(),
                __request.get_file_id()
              );

              __request.respond( _return_value );
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

          xtreemfs_internal_get_file_sizeRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_internal_read_localRequest( xtreemfs_internal_read_localRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::InternalReadLocalResponse _return_value =

              _interface->xtreemfs_internal_read_local
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_object_number(),
                __request.get_object_version(),
                __request.get_offset(),
                __request.get_length(),
                __request.get_attach_object_list(),
                __request.get_required_objects()
              );

              __request.respond( _return_value );
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

          xtreemfs_internal_read_localRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_internal_get_object_setRequest( xtreemfs_internal_get_object_setRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::ObjectList _return_value =

              _interface->xtreemfs_internal_get_object_set
              (
                __request.get_file_credentials(),
                __request.get_file_id()
              );

              __request.respond( _return_value );
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

          xtreemfs_internal_get_object_setRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_lock_acquireRequest( xtreemfs_lock_acquireRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::Lock _return_value =

              _interface->xtreemfs_lock_acquire
              (
                __request.get_file_credentials(),
                __request.get_client_uuid(),
                __request.get_client_pid(),
                __request.get_file_id(),
                __request.get_offset(),
                __request.get_length(),
                __request.get_exclusive()
              );

              __request.respond( _return_value );
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

          xtreemfs_lock_acquireRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_lock_checkRequest( xtreemfs_lock_checkRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::Lock _return_value =

              _interface->xtreemfs_lock_check
              (
                __request.get_file_credentials(),
                __request.get_client_uuid(),
                __request.get_client_pid(),
                __request.get_file_id(),
                __request.get_offset(),
                __request.get_length(),
                __request.get_exclusive()
              );

              __request.respond( _return_value );
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

          xtreemfs_lock_checkRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_lock_releaseRequest( xtreemfs_lock_releaseRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_lock_release
              (
                __request.get_file_credentials(),
                __request.get_file_id(),
                __request.get_lock()
              );
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

          xtreemfs_lock_releaseRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_pingRequest( xtreemfs_pingRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::VivaldiCoordinates remote_coordinates;

              _interface->xtreemfs_ping
              (
                __request.get_coordinates(),
                remote_coordinates
              );

              __request.respond( remote_coordinates );
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

          xtreemfs_pingRequest::dec_ref( __request );
        }

        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_shutdown();
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
        OSDInterface* _interface;
      };

      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EVENT_HANDLER_PROTOTYPES \
      virtual void handlereadRequest( readRequest& __request );\
      virtual void handletruncateRequest( truncateRequest& __request );\
      virtual void handleunlinkRequest( unlinkRequest& __request );\
      virtual void handlewriteRequest( writeRequest& __request );\
      virtual void handlextreemfs_broadcast_gmaxRequest( xtreemfs_broadcast_gmaxRequest& __request );\
      virtual void handlextreemfs_check_objectRequest( xtreemfs_check_objectRequest& __request );\
      virtual void handlextreemfs_cleanup_get_resultsRequest( xtreemfs_cleanup_get_resultsRequest& __request );\
      virtual void handlextreemfs_cleanup_is_runningRequest( xtreemfs_cleanup_is_runningRequest& __request );\
      virtual void handlextreemfs_cleanup_startRequest( xtreemfs_cleanup_startRequest& __request );\
      virtual void handlextreemfs_cleanup_statusRequest( xtreemfs_cleanup_statusRequest& __request );\
      virtual void handlextreemfs_cleanup_stopRequest( xtreemfs_cleanup_stopRequest& __request );\
      virtual void handlextreemfs_rwr_fetchRequest( xtreemfs_rwr_fetchRequest& __request );\
      virtual void handlextreemfs_rwr_flease_msgRequest( xtreemfs_rwr_flease_msgRequest& __request );\
      virtual void handlextreemfs_rwr_notifyRequest( xtreemfs_rwr_notifyRequest& __request );\
      virtual void handlextreemfs_rwr_statusRequest( xtreemfs_rwr_statusRequest& __request );\
      virtual void handlextreemfs_rwr_truncateRequest( xtreemfs_rwr_truncateRequest& __request );\
      virtual void handlextreemfs_rwr_updateRequest( xtreemfs_rwr_updateRequest& __request );\
      virtual void handlextreemfs_internal_get_gmaxRequest( xtreemfs_internal_get_gmaxRequest& __request );\
      virtual void handlextreemfs_internal_truncateRequest( xtreemfs_internal_truncateRequest& __request );\
      virtual void handlextreemfs_internal_get_file_sizeRequest( xtreemfs_internal_get_file_sizeRequest& __request );\
      virtual void handlextreemfs_internal_read_localRequest( xtreemfs_internal_read_localRequest& __request );\
      virtual void handlextreemfs_internal_get_object_setRequest( xtreemfs_internal_get_object_setRequest& __request );\
      virtual void handlextreemfs_lock_acquireRequest( xtreemfs_lock_acquireRequest& __request );\
      virtual void handlextreemfs_lock_checkRequest( xtreemfs_lock_checkRequest& __request );\
      virtual void handlextreemfs_lock_releaseRequest( xtreemfs_lock_releaseRequest& __request );\
      virtual void handlextreemfs_pingRequest( xtreemfs_pingRequest& __request );\
      virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& __request );


      class OSDInterfaceMessageSender : public OSDInterface, private OSDInterfaceMessages
      {
      public:
        OSDInterfaceMessageSender() // Used when the event_target is a subclass
          : __event_target( NULL )
        { }

        OSDInterfaceMessageSender( ::yield::concurrency::EventTarget& event_target )
          : __event_target( &event_target )
        { }


        virtual void
        read
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          uint32_t offset,
          uint32_t length,
          org::xtreemfs::interfaces::ObjectData& object_data
        )
        {
          readRequest* __request = new readRequest( file_credentials, file_id, object_number, object_version, offset, length, object_data );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<readResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<readResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<readResponse> __response = __response_queue->dequeue();
          object_data = __response->get_object_data();
        }

        virtual void
        truncate
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t new_file_size,
          org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
        )
        {
          truncateRequest* __request = new truncateRequest( file_credentials, file_id, new_file_size );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<truncateResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<truncateResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<truncateResponse> __response = __response_queue->dequeue();
          osd_write_response = __response->get_osd_write_response();
        }

        virtual void
        unlink
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        )
        {
          unlinkRequest* __request = new unlinkRequest( file_credentials, file_id );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<unlinkResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<unlinkResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<unlinkResponse> __response = __response_queue->dequeue();
        }

        virtual void
        write
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          uint32_t offset,
          uint64_t lease_timeout,
          const org::xtreemfs::interfaces::ObjectData& object_data,
          org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
        )
        {
          writeRequest* __request = new writeRequest( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<writeResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<writeResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<writeResponse> __response = __response_queue->dequeue();
          osd_write_response = __response->get_osd_write_response();
        }

        virtual void
        xtreemfs_broadcast_gmax
        (
          const string& file_id,
          uint64_t truncate_epoch,
          uint64_t last_object,
          uint64_t file_size
        )
        {
          xtreemfs_broadcast_gmaxRequest* __request = new xtreemfs_broadcast_gmaxRequest( file_id, truncate_epoch, last_object, file_size );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_broadcast_gmaxResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_broadcast_gmaxResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_broadcast_gmaxResponse> __response = __response_queue->dequeue();
        }

        virtual org::xtreemfs::interfaces::ObjectData
        xtreemfs_check_object
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version
        )
        {
          xtreemfs_check_objectRequest* __request = new xtreemfs_check_objectRequest( file_credentials, file_id, object_number, object_version );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_check_objectResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_check_objectResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_check_objectResponse> __response = __response_queue->dequeue();
          org::xtreemfs::interfaces::ObjectData _return_value = __response->get__return_value();return _return_value;
        }

        virtual void
        xtreemfs_cleanup_get_results
        (
          org::xtreemfs::interfaces::StringSet& results
        )
        {
          xtreemfs_cleanup_get_resultsRequest* __request = new xtreemfs_cleanup_get_resultsRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_get_resultsResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_get_resultsResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_cleanup_get_resultsResponse> __response = __response_queue->dequeue();
          results = __response->get_results();
        }

        virtual void xtreemfs_cleanup_is_running( bool& is_running )
        {
          xtreemfs_cleanup_is_runningRequest* __request = new xtreemfs_cleanup_is_runningRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_is_runningResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_is_runningResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_cleanup_is_runningResponse> __response = __response_queue->dequeue();
          is_running = __response->get_is_running();
        }

        virtual void
        xtreemfs_cleanup_start
        (
          bool remove_zombies,
          bool remove_unavail_volume,
          bool lost_and_found
        )
        {
          xtreemfs_cleanup_startRequest* __request = new xtreemfs_cleanup_startRequest( remove_zombies, remove_unavail_volume, lost_and_found );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_startResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_startResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_cleanup_startResponse> __response = __response_queue->dequeue();
        }

        virtual void xtreemfs_cleanup_status( string& status )
        {
          xtreemfs_cleanup_statusRequest* __request = new xtreemfs_cleanup_statusRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_statusResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_statusResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_cleanup_statusResponse> __response = __response_queue->dequeue();
          status = __response->get_status();
        }

        virtual void xtreemfs_cleanup_stop()
        {
          xtreemfs_cleanup_stopRequest* __request = new xtreemfs_cleanup_stopRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_stopResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_stopResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_cleanup_stopResponse> __response = __response_queue->dequeue();
        }

        virtual void
        xtreemfs_rwr_fetch
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          org::xtreemfs::interfaces::ObjectData& object_data
        )
        {
          xtreemfs_rwr_fetchRequest* __request = new xtreemfs_rwr_fetchRequest( file_credentials, file_id, object_number, object_version );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_rwr_fetchResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_rwr_fetchResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_rwr_fetchResponse> __response = __response_queue->dequeue();
          object_data = __response->get_object_data();
        }

        virtual void
        xtreemfs_rwr_flease_msg
        (
          ::yidl::runtime::Buffer* fleaseMessage,
          const string& senderHostname,
          uint32_t senderPort
        )
        {
          xtreemfs_rwr_flease_msgRequest* __request = new xtreemfs_rwr_flease_msgRequest( fleaseMessage, senderHostname, senderPort );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_rwr_flease_msgResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_rwr_flease_msgResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_rwr_flease_msgResponse> __response = __response_queue->dequeue();
        }

        virtual void
        xtreemfs_rwr_notify
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        )
        {
          xtreemfs_rwr_notifyRequest* __request = new xtreemfs_rwr_notifyRequest( file_credentials, file_id );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_rwr_notifyResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_rwr_notifyResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_rwr_notifyResponse> __response = __response_queue->dequeue();
        }

        virtual void
        xtreemfs_rwr_status
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          int64_t max_local_obj_version,
          org::xtreemfs::interfaces::ReplicaStatus& local_state
        )
        {
          xtreemfs_rwr_statusRequest* __request = new xtreemfs_rwr_statusRequest( file_credentials, file_id, max_local_obj_version );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_rwr_statusResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_rwr_statusResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_rwr_statusResponse> __response = __response_queue->dequeue();
          local_state = __response->get_local_state();
        }

        virtual void
        xtreemfs_rwr_truncate
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t new_file_size,
          uint64_t object_version
        )
        {
          xtreemfs_rwr_truncateRequest* __request = new xtreemfs_rwr_truncateRequest( file_credentials, file_id, new_file_size, object_version );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_rwr_truncateResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_rwr_truncateResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_rwr_truncateResponse> __response = __response_queue->dequeue();
        }

        virtual void
        xtreemfs_rwr_update
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          uint32_t offset,
          const org::xtreemfs::interfaces::ObjectData& object_data
        )
        {
          xtreemfs_rwr_updateRequest* __request = new xtreemfs_rwr_updateRequest( file_credentials, file_id, object_number, object_version, offset, object_data );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_rwr_updateResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_rwr_updateResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_rwr_updateResponse> __response = __response_queue->dequeue();
        }

        virtual org::xtreemfs::interfaces::InternalGmax
        xtreemfs_internal_get_gmax
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        )
        {
          xtreemfs_internal_get_gmaxRequest* __request = new xtreemfs_internal_get_gmaxRequest( file_credentials, file_id );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_internal_get_gmaxResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_internal_get_gmaxResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_internal_get_gmaxResponse> __response = __response_queue->dequeue();
          org::xtreemfs::interfaces::InternalGmax _return_value = __response->get__return_value();return _return_value;
        }

        virtual void
        xtreemfs_internal_truncate
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t new_file_size,
          org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
        )
        {
          xtreemfs_internal_truncateRequest* __request = new xtreemfs_internal_truncateRequest( file_credentials, file_id, new_file_size );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_internal_truncateResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_internal_truncateResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_internal_truncateResponse> __response = __response_queue->dequeue();
          osd_write_response = __response->get_osd_write_response();
        }

        virtual uint64_t
        xtreemfs_internal_get_file_size
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        )
        {
          xtreemfs_internal_get_file_sizeRequest* __request = new xtreemfs_internal_get_file_sizeRequest( file_credentials, file_id );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_internal_get_file_sizeResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_internal_get_file_sizeResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_internal_get_file_sizeResponse> __response = __response_queue->dequeue();
          uint64_t _return_value = __response->get__return_value();return _return_value;
        }

        virtual org::xtreemfs::interfaces::InternalReadLocalResponse
        xtreemfs_internal_read_local
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          uint64_t object_number,
          uint64_t object_version,
          uint64_t offset,
          uint64_t length,
          bool attach_object_list,
          const org::xtreemfs::interfaces::ObjectListSet& required_objects
        )
        {
          xtreemfs_internal_read_localRequest* __request = new xtreemfs_internal_read_localRequest( file_credentials, file_id, object_number, object_version, offset, length, attach_object_list, required_objects );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_internal_read_localResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_internal_read_localResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_internal_read_localResponse> __response = __response_queue->dequeue();
          org::xtreemfs::interfaces::InternalReadLocalResponse _return_value = __response->get__return_value();return _return_value;
        }

        virtual org::xtreemfs::interfaces::ObjectList
        xtreemfs_internal_get_object_set
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id
        )
        {
          xtreemfs_internal_get_object_setRequest* __request = new xtreemfs_internal_get_object_setRequest( file_credentials, file_id );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_internal_get_object_setResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_internal_get_object_setResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_internal_get_object_setResponse> __response = __response_queue->dequeue();
          org::xtreemfs::interfaces::ObjectList _return_value = __response->get__return_value();return _return_value;
        }

        virtual org::xtreemfs::interfaces::Lock
        xtreemfs_lock_acquire
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& client_uuid,
          int32_t client_pid,
          const string& file_id,
          uint64_t offset,
          uint64_t length,
          bool exclusive
        )
        {
          xtreemfs_lock_acquireRequest* __request = new xtreemfs_lock_acquireRequest( file_credentials, client_uuid, client_pid, file_id, offset, length, exclusive );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_lock_acquireResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_lock_acquireResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_lock_acquireResponse> __response = __response_queue->dequeue();
          org::xtreemfs::interfaces::Lock _return_value = __response->get__return_value();return _return_value;
        }

        virtual org::xtreemfs::interfaces::Lock
        xtreemfs_lock_check
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& client_uuid,
          int32_t client_pid,
          const string& file_id,
          uint64_t offset,
          uint64_t length,
          bool exclusive
        )
        {
          xtreemfs_lock_checkRequest* __request = new xtreemfs_lock_checkRequest( file_credentials, client_uuid, client_pid, file_id, offset, length, exclusive );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_lock_checkResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_lock_checkResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_lock_checkResponse> __response = __response_queue->dequeue();
          org::xtreemfs::interfaces::Lock _return_value = __response->get__return_value();return _return_value;
        }

        virtual void
        xtreemfs_lock_release
        (
          const org::xtreemfs::interfaces::FileCredentials& file_credentials,
          const string& file_id,
          const org::xtreemfs::interfaces::Lock& lock
        )
        {
          xtreemfs_lock_releaseRequest* __request = new xtreemfs_lock_releaseRequest( file_credentials, file_id, lock );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_lock_releaseResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_lock_releaseResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_lock_releaseResponse> __response = __response_queue->dequeue();
        }

        virtual void
        xtreemfs_ping
        (
          const org::xtreemfs::interfaces::VivaldiCoordinates& coordinates,
          org::xtreemfs::interfaces::VivaldiCoordinates& remote_coordinates
        )
        {
          xtreemfs_pingRequest* __request = new xtreemfs_pingRequest( coordinates );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_pingResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_pingResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_pingResponse> __response = __response_queue->dequeue();
          remote_coordinates = __response->get_remote_coordinates();
        }

        virtual void xtreemfs_shutdown()
        {
          xtreemfs_shutdownRequest* __request = new xtreemfs_shutdownRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> );
          __request->set_response_target( &__response_queue.get() );

          __event_target->send( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> __response = __response_queue->dequeue();
        }

        void set_event_target( ::yield::concurrency::EventTarget& event_target )
        {
          this->__event_target = &event_target;
        }

      private:
        // __event_target is not a counted reference, since that would create
        // a reference cycle when __event_target is a subclass of OSDInterfaceMessageSender
        ::yield::concurrency::EventTarget* __event_target;
      };
    };
  };
};
#endif
