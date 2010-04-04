#ifndef _1000665608_H_
#define _1000665608_H_


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

        InternalGmax( const InternalGmax& other )
          : epoch( other.get_epoch() ),
            file_size( other.get_file_size() ),
            last_object_id( other.get_last_object_id() )
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
          return get_epoch() == other.get_epoch()
                 &&
                 get_file_size() == other.get_file_size()
                 &&
                 get_last_object_id() == other.get_last_object_id();
        }

        // yidl::runtime::Object
        InternalGmax& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( InternalGmax, 2010030966 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "epoch", 0 ), get_epoch() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_size", 0 ), get_file_size() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "last_object_id", 0 ), get_last_object_id() );
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

        Lock( const Lock& other )
          : client_pid( other.get_client_pid() ),
            client_uuid( other.get_client_uuid() ),
            length( other.get_length() ),
            offset( other.get_offset() )
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
          return get_client_pid() == other.get_client_pid()
                 &&
                 get_client_uuid() == other.get_client_uuid()
                 &&
                 get_length() == other.get_length()
                 &&
                 get_offset() == other.get_offset();
        }

        // yidl::runtime::Object
        Lock& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Lock, 2010030970 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_pid", 0 ), get_client_pid() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_uuid", 0 ), get_client_uuid() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), get_length() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), get_offset() );
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
          : checksum( 0 ),
            invalid_checksum_on_osd( false ),
            zero_padding( 0 ),
            data( NULL )
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

        ObjectData( const ObjectData& other )
          : checksum( other.get_checksum() ),
            invalid_checksum_on_osd( other.get_invalid_checksum_on_osd() ),
            zero_padding( other.get_zero_padding() ),
            data( ::yidl::runtime::Object::inc_ref( other.get_data() ) )
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
          return get_checksum() == other.get_checksum()
                 &&
                 get_invalid_checksum_on_osd() == other.get_invalid_checksum_on_osd()
                 &&
                 get_zero_padding() == other.get_zero_padding()
                 &&
                 get_data() == other.get_data();
        }

        // yidl::runtime::Object
        ObjectData& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ObjectData, 2010030967 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "checksum", 0 ), get_checksum() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "invalid_checksum_on_osd", 0 ), get_invalid_checksum_on_osd() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "zero_padding", 0 ), get_zero_padding() );
          if ( get_data() != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), *get_data() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          checksum = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "checksum", 0 ) );
          invalid_checksum_on_osd = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "invalid_checksum_on_osd", 0 ) );
          zero_padding = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "zero_padding", 0 ) );
          if ( data != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), *data ); else data = unmarshaller.read_buffer( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ) );
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

        ObjectList( const ObjectList& other )
          : set( ::yidl::runtime::Object::inc_ref( other.get_set() ) ),
            stripe_width( other.get_stripe_width() ),
            first_( other.get_first_() )
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
          return get_set() == other.get_set()
                 &&
                 get_stripe_width() == other.get_stripe_width()
                 &&
                 get_first_() == other.get_first_();
        }

        // yidl::runtime::Object
        ObjectList& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ObjectList, 2010030971 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          if ( get_set() != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "set", 0 ), *get_set() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stripe_width", 0 ), get_stripe_width() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "first_", 0 ), get_first_() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          if ( set != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "set", 0 ), *set ); else set = unmarshaller.read_buffer( ::yidl::runtime::Unmarshaller::StringLiteralKey( "set", 0 ) );
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

        // yidl::runtime::Object
        ObjectListSet& inc_ref() { return Object::inc_ref( *this ); }

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

        ObjectVersion( const ObjectVersion& other )
          : object_number( other.get_object_number() ),
            object_version( other.get_object_version() )
        { }

        virtual ~ObjectVersion() {  }

        uint64_t get_object_number() const { return object_number; }
        uint64_t get_object_version() const { return object_version; }
        void set_object_number( uint64_t object_number ) { this->object_number = object_number; }
        void set_object_version( uint64_t object_version ) { this->object_version = object_version; }

        bool operator==( const ObjectVersion& other ) const
        {
          return get_object_number() == other.get_object_number()
                 &&
                 get_object_version() == other.get_object_version();
        }

        // yidl::runtime::Object
        ObjectVersion& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ObjectVersion, 2010030973 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), get_object_number() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), get_object_version() );
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

        // yidl::runtime::Object
        ObjectVersionList& inc_ref() { return Object::inc_ref( *this ); }

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

        ReplicaStatus( const ReplicaStatus& other )
          : truncate_epoch( other.get_truncate_epoch() ),
            file_size( other.get_file_size() ),
            max_obj_version( other.get_max_obj_version() ),
            objectVersions( other.get_objectVersions() )
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
          return get_truncate_epoch() == other.get_truncate_epoch()
                 &&
                 get_file_size() == other.get_file_size()
                 &&
                 get_max_obj_version() == other.get_max_obj_version()
                 &&
                 get_objectVersions() == other.get_objectVersions();
        }

        // yidl::runtime::Object
        ReplicaStatus& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ReplicaStatus, 2010030975 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "truncate_epoch", 0 ), get_truncate_epoch() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_size", 0 ), get_file_size() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "max_obj_version", 0 ), get_max_obj_version() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "objectVersions", 0 ), get_objectVersions() );
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

        InternalReadLocalResponse( const InternalReadLocalResponse& other )
          : data( other.get_data() ),
            object_set( other.get_object_set() )
        { }

        virtual ~InternalReadLocalResponse() {  }

        const org::xtreemfs::interfaces::ObjectData& get_data() const { return data; }
        const org::xtreemfs::interfaces::ObjectListSet& get_object_set() const { return object_set; }
        void set_data( const org::xtreemfs::interfaces::ObjectData&  data ) { this->data = data; }
        void set_object_set( const org::xtreemfs::interfaces::ObjectListSet&  object_set ) { this->object_set = object_set; }

        bool operator==( const InternalReadLocalResponse& other ) const
        {
          return get_data() == other.get_data()
                 &&
                 get_object_set() == other.get_object_set();
        }

        // yidl::runtime::Object
        InternalReadLocalResponse& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( InternalReadLocalResponse, 2010030968 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), get_data() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_set", 0 ), get_object_set() );
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
        const static uint32_t ONC_RPC_PORT_DEFAULT = 32640;const static uint32_t TAG = 2010031216;

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


      #ifndef ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS ORG_EXCEPTION_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS ::yield::concurrency::Exception
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
            : object_number( 0 ),
              object_version( 0 ),
              offset( 0 ),
              length( 0 )
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

          readRequest( const readRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              object_number( other.get_object_number() ),
              object_version( other.get_object_version() ),
              offset( other.get_offset() ),
              length( other.get_length() ),
              object_data( other.get_object_data() )
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

          bool operator==( const readRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_object_number() == other.get_object_number()
                   &&
                   get_object_version() == other.get_object_version()
                   &&
                   get_offset() == other.get_offset()
                   &&
                   get_length() == other.get_length()
                   &&
                   get_object_data() == other.get_object_data();
          }

          // yidl::runtime::Object
          readRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( readRequest, 2010031226 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), get_object_number() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), get_object_version() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), get_offset() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), get_length() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), get_object_data() );
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

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new readResponse
                       (
                         get_object_data()
                       );
          }

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

          readResponse( const readResponse& other )
            : object_data( other.get_object_data() )
          { }

          virtual ~readResponse() {  }

          const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }
          void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }

          bool operator==( const readResponse& other ) const
          {
            return get_object_data() == other.get_object_data();
          }

          // yidl::runtime::Object
          readResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( readResponse, 2010031226 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), get_object_data() );
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

          truncateRequest( const truncateRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              new_file_size( other.get_new_file_size() )
          { }

          virtual ~truncateRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_new_file_size() const { return new_file_size; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }

          bool operator==( const truncateRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_new_file_size() == other.get_new_file_size();
          }

          // yidl::runtime::Object
          truncateRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( truncateRequest, 2010031227 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "new_file_size", 0 ), get_new_file_size() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new truncateResponse;
          }

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

          truncateResponse( const truncateResponse& other )
            : osd_write_response( other.get_osd_write_response() )
          { }

          virtual ~truncateResponse() {  }

          const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }
          void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }

          bool operator==( const truncateResponse& other ) const
          {
            return get_osd_write_response() == other.get_osd_write_response();
          }

          // yidl::runtime::Object
          truncateResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( truncateResponse, 2010031227 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "osd_write_response", 0 ), get_osd_write_response() );
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

          unlinkRequest( const unlinkRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() )
          { }

          virtual ~unlinkRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }

          bool operator==( const unlinkRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id();
          }

          // yidl::runtime::Object
          unlinkRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( unlinkRequest, 2010031228 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new unlinkResponse;
          }

          virtual void respond()
          {
            respond( *new unlinkResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
        };


        class unlinkResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~unlinkResponse() {  }

          bool operator==( const unlinkResponse& ) const { return true; }

          // yidl::runtime::Object
          unlinkResponse& inc_ref() { return Object::inc_ref( *this ); }

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
            : object_number( 0 ),
              object_version( 0 ),
              offset( 0 ),
              lease_timeout( 0 )
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

          writeRequest( const writeRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              object_number( other.get_object_number() ),
              object_version( other.get_object_version() ),
              offset( other.get_offset() ),
              lease_timeout( other.get_lease_timeout() ),
              object_data( other.get_object_data() )
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

          bool operator==( const writeRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_object_number() == other.get_object_number()
                   &&
                   get_object_version() == other.get_object_version()
                   &&
                   get_offset() == other.get_offset()
                   &&
                   get_lease_timeout() == other.get_lease_timeout()
                   &&
                   get_object_data() == other.get_object_data();
          }

          // yidl::runtime::Object
          writeRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( writeRequest, 2010031229 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), get_object_number() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), get_object_version() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), get_offset() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "lease_timeout", 0 ), get_lease_timeout() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), get_object_data() );
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

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new writeResponse;
          }

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

          writeResponse( const writeResponse& other )
            : osd_write_response( other.get_osd_write_response() )
          { }

          virtual ~writeResponse() {  }

          const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }
          void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }

          bool operator==( const writeResponse& other ) const
          {
            return get_osd_write_response() == other.get_osd_write_response();
          }

          // yidl::runtime::Object
          writeResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( writeResponse, 2010031229 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "osd_write_response", 0 ), get_osd_write_response() );
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
            : truncate_epoch( 0 ),
              last_object( 0 ),
              file_size( 0 )
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

          xtreemfs_broadcast_gmaxRequest( const xtreemfs_broadcast_gmaxRequest& other )
            : file_id( other.get_file_id() ),
              truncate_epoch( other.get_truncate_epoch() ),
              last_object( other.get_last_object() ),
              file_size( other.get_file_size() )
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

          bool operator==( const xtreemfs_broadcast_gmaxRequest& other ) const
          {
            return get_file_id() == other.get_file_id()
                   &&
                   get_truncate_epoch() == other.get_truncate_epoch()
                   &&
                   get_last_object() == other.get_last_object()
                   &&
                   get_file_size() == other.get_file_size();
          }

          // yidl::runtime::Object
          xtreemfs_broadcast_gmaxRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_broadcast_gmaxRequest, 2010031236 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "truncate_epoch", 0 ), get_truncate_epoch() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "last_object", 0 ), get_last_object() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_size", 0 ), get_file_size() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "truncate_epoch", 0 ), truncate_epoch );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "last_object", 0 ), last_object );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_size", 0 ), file_size );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_broadcast_gmaxResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_broadcast_gmaxResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
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
          virtual ~xtreemfs_broadcast_gmaxResponse() {  }

          bool operator==( const xtreemfs_broadcast_gmaxResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_broadcast_gmaxResponse& inc_ref() { return Object::inc_ref( *this ); }

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

          xtreemfs_check_objectRequest( const xtreemfs_check_objectRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              object_number( other.get_object_number() ),
              object_version( other.get_object_version() )
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

          bool operator==( const xtreemfs_check_objectRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_object_number() == other.get_object_number()
                   &&
                   get_object_version() == other.get_object_version();
          }

          // yidl::runtime::Object
          xtreemfs_check_objectRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_check_objectRequest, 2010031237 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), get_object_number() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), get_object_version() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_number", 0 ), object_number );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_check_objectResponse;
          }

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

          xtreemfs_check_objectResponse( const xtreemfs_check_objectResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_check_objectResponse() {  }

          const org::xtreemfs::interfaces::ObjectData& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::ObjectData&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_check_objectResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_check_objectResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_check_objectResponse, 2010031237 );

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
          org::xtreemfs::interfaces::ObjectData _return_value;
        };


        class xtreemfs_cleanup_get_resultsRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_cleanup_get_resultsRequest() {  }

          bool operator==( const xtreemfs_cleanup_get_resultsRequest& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_cleanup_get_resultsRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_get_resultsRequest, 2010031246 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_cleanup_get_resultsResponse;
          }

          virtual void respond( const org::xtreemfs::interfaces::StringSet& results )
          {
            respond( *new xtreemfs_cleanup_get_resultsResponse( results ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }
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

          xtreemfs_cleanup_get_resultsResponse( const xtreemfs_cleanup_get_resultsResponse& other )
            : results( other.get_results() )
          { }

          virtual ~xtreemfs_cleanup_get_resultsResponse() {  }

          const org::xtreemfs::interfaces::StringSet& get_results() const { return results; }
          void set_results( const org::xtreemfs::interfaces::StringSet&  results ) { this->results = results; }

          bool operator==( const xtreemfs_cleanup_get_resultsResponse& other ) const
          {
            return get_results() == other.get_results();
          }

          // yidl::runtime::Object
          xtreemfs_cleanup_get_resultsResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_get_resultsResponse, 2010031246 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "results", 0 ), get_results() );
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
          virtual ~xtreemfs_cleanup_is_runningRequest() {  }

          bool operator==( const xtreemfs_cleanup_is_runningRequest& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_cleanup_is_runningRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_is_runningRequest, 2010031247 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_cleanup_is_runningResponse;
          }

          virtual void respond( bool is_running )
          {
            respond( *new xtreemfs_cleanup_is_runningResponse( is_running ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }
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

          xtreemfs_cleanup_is_runningResponse( const xtreemfs_cleanup_is_runningResponse& other )
            : is_running( other.get_is_running() )
          { }

          virtual ~xtreemfs_cleanup_is_runningResponse() {  }

          bool get_is_running() const { return is_running; }
          void set_is_running( bool is_running ) { this->is_running = is_running; }

          bool operator==( const xtreemfs_cleanup_is_runningResponse& other ) const
          {
            return get_is_running() == other.get_is_running();
          }

          // yidl::runtime::Object
          xtreemfs_cleanup_is_runningResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_is_runningResponse, 2010031247 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "is_running", 0 ), get_is_running() );
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
            : remove_zombies( false ),
              remove_unavail_volume( false ),
              lost_and_found( false )
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

          xtreemfs_cleanup_startRequest( const xtreemfs_cleanup_startRequest& other )
            : remove_zombies( other.get_remove_zombies() ),
              remove_unavail_volume( other.get_remove_unavail_volume() ),
              lost_and_found( other.get_lost_and_found() )
          { }

          virtual ~xtreemfs_cleanup_startRequest() {  }

          bool get_remove_zombies() const { return remove_zombies; }
          bool get_remove_unavail_volume() const { return remove_unavail_volume; }
          bool get_lost_and_found() const { return lost_and_found; }
          void set_remove_zombies( bool remove_zombies ) { this->remove_zombies = remove_zombies; }
          void set_remove_unavail_volume( bool remove_unavail_volume ) { this->remove_unavail_volume = remove_unavail_volume; }
          void set_lost_and_found( bool lost_and_found ) { this->lost_and_found = lost_and_found; }

          bool operator==( const xtreemfs_cleanup_startRequest& other ) const
          {
            return get_remove_zombies() == other.get_remove_zombies()
                   &&
                   get_remove_unavail_volume() == other.get_remove_unavail_volume()
                   &&
                   get_lost_and_found() == other.get_lost_and_found();
          }

          // yidl::runtime::Object
          xtreemfs_cleanup_startRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_startRequest, 2010031248 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "remove_zombies", 0 ), get_remove_zombies() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "remove_unavail_volume", 0 ), get_remove_unavail_volume() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "lost_and_found", 0 ), get_lost_and_found() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            remove_zombies = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "remove_zombies", 0 ) );
            remove_unavail_volume = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "remove_unavail_volume", 0 ) );
            lost_and_found = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "lost_and_found", 0 ) );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_cleanup_startResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_cleanup_startResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          bool remove_zombies;
          bool remove_unavail_volume;
          bool lost_and_found;
        };


        class xtreemfs_cleanup_startResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_cleanup_startResponse() {  }

          bool operator==( const xtreemfs_cleanup_startResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_cleanup_startResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_startResponse, 2010031248 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class xtreemfs_cleanup_statusRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_cleanup_statusRequest() {  }

          bool operator==( const xtreemfs_cleanup_statusRequest& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_cleanup_statusRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_statusRequest, 2010031249 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_cleanup_statusResponse;
          }

          virtual void respond( const string& status )
          {
            respond( *new xtreemfs_cleanup_statusResponse( status ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }
        };


        class xtreemfs_cleanup_statusResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_cleanup_statusResponse() { }

          xtreemfs_cleanup_statusResponse( const string& status )
            : status( status )
          { }

          xtreemfs_cleanup_statusResponse( const xtreemfs_cleanup_statusResponse& other )
            : status( other.get_status() )
          { }

          virtual ~xtreemfs_cleanup_statusResponse() {  }

          const string& get_status() const { return status; }
          void set_status( const string& status ) { this->status = status; }

          bool operator==( const xtreemfs_cleanup_statusResponse& other ) const
          {
            return get_status() == other.get_status();
          }

          // yidl::runtime::Object
          xtreemfs_cleanup_statusResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_statusResponse, 2010031249 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "status", 0 ), get_status() );
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
          virtual ~xtreemfs_cleanup_stopRequest() {  }

          bool operator==( const xtreemfs_cleanup_stopRequest& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_cleanup_stopRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_cleanup_stopRequest, 2010031250 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_cleanup_stopResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_cleanup_stopResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }
        };


        class xtreemfs_cleanup_stopResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_cleanup_stopResponse() {  }

          bool operator==( const xtreemfs_cleanup_stopResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_cleanup_stopResponse& inc_ref() { return Object::inc_ref( *this ); }

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

          xtreemfs_rwr_fetchRequest( const xtreemfs_rwr_fetchRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              object_number( other.get_object_number() ),
              object_version( other.get_object_version() )
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

          bool operator==( const xtreemfs_rwr_fetchRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_object_number() == other.get_object_number()
                   &&
                   get_object_version() == other.get_object_version();
          }

          // yidl::runtime::Object
          xtreemfs_rwr_fetchRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_fetchRequest, 2010031289 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), get_object_number() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), get_object_version() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_number", 0 ), object_number );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_rwr_fetchResponse;
          }

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

          xtreemfs_rwr_fetchResponse( const xtreemfs_rwr_fetchResponse& other )
            : object_data( other.get_object_data() )
          { }

          virtual ~xtreemfs_rwr_fetchResponse() {  }

          const org::xtreemfs::interfaces::ObjectData& get_object_data() const { return object_data; }
          void set_object_data( const org::xtreemfs::interfaces::ObjectData&  object_data ) { this->object_data = object_data; }

          bool operator==( const xtreemfs_rwr_fetchResponse& other ) const
          {
            return get_object_data() == other.get_object_data();
          }

          // yidl::runtime::Object
          xtreemfs_rwr_fetchResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_fetchResponse, 2010031289 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), get_object_data() );
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

          xtreemfs_rwr_flease_msgRequest( const xtreemfs_rwr_flease_msgRequest& other )
            : fleaseMessage( ::yidl::runtime::Object::inc_ref( other.get_fleaseMessage() ) ),
              senderHostname( other.get_senderHostname() ),
              senderPort( other.get_senderPort() )
          { }

          virtual ~xtreemfs_rwr_flease_msgRequest() { ::yidl::runtime::Buffer::dec_ref( fleaseMessage ); }

          ::yidl::runtime::Buffer* get_fleaseMessage() const { return fleaseMessage; }
          const string& get_senderHostname() const { return senderHostname; }
          uint32_t get_senderPort() const { return senderPort; }
          void set_fleaseMessage( ::yidl::runtime::Buffer* fleaseMessage ) { ::yidl::runtime::Buffer::dec_ref( this->fleaseMessage ); this->fleaseMessage = ::yidl::runtime::Object::inc_ref( fleaseMessage ); }
          void set_senderHostname( const string& senderHostname ) { this->senderHostname = senderHostname; }
          void set_senderPort( uint32_t senderPort ) { this->senderPort = senderPort; }

          bool operator==( const xtreemfs_rwr_flease_msgRequest& other ) const
          {
            return get_fleaseMessage() == other.get_fleaseMessage()
                   &&
                   get_senderHostname() == other.get_senderHostname()
                   &&
                   get_senderPort() == other.get_senderPort();
          }

          // yidl::runtime::Object
          xtreemfs_rwr_flease_msgRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_flease_msgRequest, 2010031287 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            if ( get_fleaseMessage() != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "fleaseMessage", 0 ), *get_fleaseMessage() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "senderHostname", 0 ), get_senderHostname() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "senderPort", 0 ), get_senderPort() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            if ( fleaseMessage != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "fleaseMessage", 0 ), *fleaseMessage ); else fleaseMessage = unmarshaller.read_buffer( ::yidl::runtime::Unmarshaller::StringLiteralKey( "fleaseMessage", 0 ) );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "senderHostname", 0 ), senderHostname );
            senderPort = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "senderPort", 0 ) );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_rwr_flease_msgResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_rwr_flease_msgResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          ::yidl::runtime::Buffer* fleaseMessage;
          string senderHostname;
          uint32_t senderPort;
        };


        class xtreemfs_rwr_flease_msgResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_rwr_flease_msgResponse() {  }

          bool operator==( const xtreemfs_rwr_flease_msgResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_rwr_flease_msgResponse& inc_ref() { return Object::inc_ref( *this ); }

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

          xtreemfs_rwr_notifyRequest( const xtreemfs_rwr_notifyRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() )
          { }

          virtual ~xtreemfs_rwr_notifyRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }

          bool operator==( const xtreemfs_rwr_notifyRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id();
          }

          // yidl::runtime::Object
          xtreemfs_rwr_notifyRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_notifyRequest, 2010031291 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_rwr_notifyResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_rwr_notifyResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
        };


        class xtreemfs_rwr_notifyResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_rwr_notifyResponse() {  }

          bool operator==( const xtreemfs_rwr_notifyResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_rwr_notifyResponse& inc_ref() { return Object::inc_ref( *this ); }

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

          xtreemfs_rwr_statusRequest( const xtreemfs_rwr_statusRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              max_local_obj_version( other.get_max_local_obj_version() )
          { }

          virtual ~xtreemfs_rwr_statusRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          int64_t get_max_local_obj_version() const { return max_local_obj_version; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_max_local_obj_version( int64_t max_local_obj_version ) { this->max_local_obj_version = max_local_obj_version; }

          bool operator==( const xtreemfs_rwr_statusRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_max_local_obj_version() == other.get_max_local_obj_version();
          }

          // yidl::runtime::Object
          xtreemfs_rwr_statusRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_statusRequest, 2010031292 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "max_local_obj_version", 0 ), get_max_local_obj_version() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "max_local_obj_version", 0 ), max_local_obj_version );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_rwr_statusResponse;
          }

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

          xtreemfs_rwr_statusResponse( const xtreemfs_rwr_statusResponse& other )
            : local_state( other.get_local_state() )
          { }

          virtual ~xtreemfs_rwr_statusResponse() {  }

          const org::xtreemfs::interfaces::ReplicaStatus& get_local_state() const { return local_state; }
          void set_local_state( const org::xtreemfs::interfaces::ReplicaStatus&  local_state ) { this->local_state = local_state; }

          bool operator==( const xtreemfs_rwr_statusResponse& other ) const
          {
            return get_local_state() == other.get_local_state();
          }

          // yidl::runtime::Object
          xtreemfs_rwr_statusResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_statusResponse, 2010031292 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "local_state", 0 ), get_local_state() );
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

          xtreemfs_rwr_truncateRequest( const xtreemfs_rwr_truncateRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              new_file_size( other.get_new_file_size() ),
              object_version( other.get_object_version() )
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

          bool operator==( const xtreemfs_rwr_truncateRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_new_file_size() == other.get_new_file_size()
                   &&
                   get_object_version() == other.get_object_version();
          }

          // yidl::runtime::Object
          xtreemfs_rwr_truncateRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_truncateRequest, 2010031290 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "new_file_size", 0 ), get_new_file_size() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), get_object_version() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "object_version", 0 ), object_version );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_rwr_truncateResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_rwr_truncateResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
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
          virtual ~xtreemfs_rwr_truncateResponse() {  }

          bool operator==( const xtreemfs_rwr_truncateResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_rwr_truncateResponse& inc_ref() { return Object::inc_ref( *this ); }

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
            : object_number( 0 ),
              object_version( 0 ),
              offset( 0 )
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

          xtreemfs_rwr_updateRequest( const xtreemfs_rwr_updateRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              object_number( other.get_object_number() ),
              object_version( other.get_object_version() ),
              offset( other.get_offset() ),
              object_data( other.get_object_data() )
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

          bool operator==( const xtreemfs_rwr_updateRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_object_number() == other.get_object_number()
                   &&
                   get_object_version() == other.get_object_version()
                   &&
                   get_offset() == other.get_offset()
                   &&
                   get_object_data() == other.get_object_data();
          }

          // yidl::runtime::Object
          xtreemfs_rwr_updateRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rwr_updateRequest, 2010031288 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), get_object_number() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), get_object_version() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), get_offset() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_data", 0 ), get_object_data() );
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

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_rwr_updateResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_rwr_updateResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
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
          virtual ~xtreemfs_rwr_updateResponse() {  }

          bool operator==( const xtreemfs_rwr_updateResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_rwr_updateResponse& inc_ref() { return Object::inc_ref( *this ); }

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

          xtreemfs_internal_get_gmaxRequest( const xtreemfs_internal_get_gmaxRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() )
          { }

          virtual ~xtreemfs_internal_get_gmaxRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }

          bool operator==( const xtreemfs_internal_get_gmaxRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id();
          }

          // yidl::runtime::Object
          xtreemfs_internal_get_gmaxRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_gmaxRequest, 2010031256 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_internal_get_gmaxResponse;
          }

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

          xtreemfs_internal_get_gmaxResponse( const xtreemfs_internal_get_gmaxResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_internal_get_gmaxResponse() {  }

          const org::xtreemfs::interfaces::InternalGmax& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::InternalGmax&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_internal_get_gmaxResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_internal_get_gmaxResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_gmaxResponse, 2010031256 );

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

          xtreemfs_internal_truncateRequest( const xtreemfs_internal_truncateRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              new_file_size( other.get_new_file_size() )
          { }

          virtual ~xtreemfs_internal_truncateRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          uint64_t get_new_file_size() const { return new_file_size; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_new_file_size( uint64_t new_file_size ) { this->new_file_size = new_file_size; }

          bool operator==( const xtreemfs_internal_truncateRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_new_file_size() == other.get_new_file_size();
          }

          // yidl::runtime::Object
          xtreemfs_internal_truncateRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_truncateRequest, 2010031257 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "new_file_size", 0 ), get_new_file_size() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_internal_truncateResponse;
          }

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

          xtreemfs_internal_truncateResponse( const xtreemfs_internal_truncateResponse& other )
            : osd_write_response( other.get_osd_write_response() )
          { }

          virtual ~xtreemfs_internal_truncateResponse() {  }

          const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }
          void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }

          bool operator==( const xtreemfs_internal_truncateResponse& other ) const
          {
            return get_osd_write_response() == other.get_osd_write_response();
          }

          // yidl::runtime::Object
          xtreemfs_internal_truncateResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_truncateResponse, 2010031257 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "osd_write_response", 0 ), get_osd_write_response() );
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

          xtreemfs_internal_get_file_sizeRequest( const xtreemfs_internal_get_file_sizeRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() )
          { }

          virtual ~xtreemfs_internal_get_file_sizeRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }

          bool operator==( const xtreemfs_internal_get_file_sizeRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id();
          }

          // yidl::runtime::Object
          xtreemfs_internal_get_file_sizeRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_file_sizeRequest, 2010031258 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_internal_get_file_sizeResponse;
          }

          virtual void respond( uint64_t _return_value )
          {
            respond( *new xtreemfs_internal_get_file_sizeResponse( _return_value ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
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

          xtreemfs_internal_get_file_sizeResponse( const xtreemfs_internal_get_file_sizeResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_internal_get_file_sizeResponse() {  }

          uint64_t get__return_value() const { return _return_value; }
          void set__return_value( uint64_t _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_internal_get_file_sizeResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_internal_get_file_sizeResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_file_sizeResponse, 2010031258 );

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

          xtreemfs_internal_read_localRequest( const xtreemfs_internal_read_localRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              object_number( other.get_object_number() ),
              object_version( other.get_object_version() ),
              offset( other.get_offset() ),
              length( other.get_length() ),
              attach_object_list( other.get_attach_object_list() ),
              required_objects( other.get_required_objects() )
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

          bool operator==( const xtreemfs_internal_read_localRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_object_number() == other.get_object_number()
                   &&
                   get_object_version() == other.get_object_version()
                   &&
                   get_offset() == other.get_offset()
                   &&
                   get_length() == other.get_length()
                   &&
                   get_attach_object_list() == other.get_attach_object_list()
                   &&
                   get_required_objects() == other.get_required_objects();
          }

          // yidl::runtime::Object
          xtreemfs_internal_read_localRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_read_localRequest, 2010031259 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_number", 0 ), get_object_number() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "object_version", 0 ), get_object_version() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), get_offset() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), get_length() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "attach_object_list", 0 ), get_attach_object_list() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "required_objects", 0 ), get_required_objects() );
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

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_internal_read_localResponse;
          }

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

          xtreemfs_internal_read_localResponse( const xtreemfs_internal_read_localResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_internal_read_localResponse() {  }

          const org::xtreemfs::interfaces::InternalReadLocalResponse& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::InternalReadLocalResponse&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_internal_read_localResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_internal_read_localResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_read_localResponse, 2010031259 );

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

          xtreemfs_internal_get_object_setRequest( const xtreemfs_internal_get_object_setRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() )
          { }

          virtual ~xtreemfs_internal_get_object_setRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }

          bool operator==( const xtreemfs_internal_get_object_setRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id();
          }

          // yidl::runtime::Object
          xtreemfs_internal_get_object_setRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_object_setRequest, 2010031260 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_internal_get_object_setResponse;
          }

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

          xtreemfs_internal_get_object_setResponse( const xtreemfs_internal_get_object_setResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_internal_get_object_setResponse() {  }

          const org::xtreemfs::interfaces::ObjectList& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::ObjectList&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_internal_get_object_setResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_internal_get_object_setResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_get_object_setResponse, 2010031260 );

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
          org::xtreemfs::interfaces::ObjectList _return_value;
        };


        class xtreemfs_lock_acquireRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lock_acquireRequest()
            : client_pid( 0 ),
              offset( 0 ),
              length( 0 ),
              exclusive( false )
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

          xtreemfs_lock_acquireRequest( const xtreemfs_lock_acquireRequest& other )
            : file_credentials( other.get_file_credentials() ),
              client_uuid( other.get_client_uuid() ),
              client_pid( other.get_client_pid() ),
              file_id( other.get_file_id() ),
              offset( other.get_offset() ),
              length( other.get_length() ),
              exclusive( other.get_exclusive() )
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

          bool operator==( const xtreemfs_lock_acquireRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_client_uuid() == other.get_client_uuid()
                   &&
                   get_client_pid() == other.get_client_pid()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_offset() == other.get_offset()
                   &&
                   get_length() == other.get_length()
                   &&
                   get_exclusive() == other.get_exclusive();
          }

          // yidl::runtime::Object
          xtreemfs_lock_acquireRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_acquireRequest, 2010031266 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_uuid", 0 ), get_client_uuid() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_pid", 0 ), get_client_pid() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), get_offset() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), get_length() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "exclusive", 0 ), get_exclusive() );
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

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_lock_acquireResponse;
          }

          virtual void respond( const org::xtreemfs::interfaces::Lock& _return_value )
          {
            respond( *new xtreemfs_lock_acquireResponse( _return_value ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
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

          xtreemfs_lock_acquireResponse( const xtreemfs_lock_acquireResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_lock_acquireResponse() {  }

          const org::xtreemfs::interfaces::Lock& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::Lock&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_lock_acquireResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_lock_acquireResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_acquireResponse, 2010031266 );

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
          org::xtreemfs::interfaces::Lock _return_value;
        };


        class xtreemfs_lock_checkRequest : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lock_checkRequest()
            : client_pid( 0 ),
              offset( 0 ),
              length( 0 ),
              exclusive( false )
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

          xtreemfs_lock_checkRequest( const xtreemfs_lock_checkRequest& other )
            : file_credentials( other.get_file_credentials() ),
              client_uuid( other.get_client_uuid() ),
              client_pid( other.get_client_pid() ),
              file_id( other.get_file_id() ),
              offset( other.get_offset() ),
              length( other.get_length() ),
              exclusive( other.get_exclusive() )
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

          bool operator==( const xtreemfs_lock_checkRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_client_uuid() == other.get_client_uuid()
                   &&
                   get_client_pid() == other.get_client_pid()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_offset() == other.get_offset()
                   &&
                   get_length() == other.get_length()
                   &&
                   get_exclusive() == other.get_exclusive();
          }

          // yidl::runtime::Object
          xtreemfs_lock_checkRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_checkRequest, 2010031267 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_uuid", 0 ), get_client_uuid() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_pid", 0 ), get_client_pid() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "offset", 0 ), get_offset() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "length", 0 ), get_length() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "exclusive", 0 ), get_exclusive() );
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

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_lock_checkResponse;
          }

          virtual void respond( const org::xtreemfs::interfaces::Lock& _return_value )
          {
            respond( *new xtreemfs_lock_checkResponse( _return_value ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
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

          xtreemfs_lock_checkResponse( const xtreemfs_lock_checkResponse& other )
            : _return_value( other.get__return_value() )
          { }

          virtual ~xtreemfs_lock_checkResponse() {  }

          const org::xtreemfs::interfaces::Lock& get__return_value() const { return _return_value; }
          void set__return_value( const org::xtreemfs::interfaces::Lock&  _return_value ) { this->_return_value = _return_value; }

          bool operator==( const xtreemfs_lock_checkResponse& other ) const
          {
            return get__return_value() == other.get__return_value();
          }

          // yidl::runtime::Object
          xtreemfs_lock_checkResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_checkResponse, 2010031267 );

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

          xtreemfs_lock_releaseRequest( const xtreemfs_lock_releaseRequest& other )
            : file_credentials( other.get_file_credentials() ),
              file_id( other.get_file_id() ),
              lock( other.get_lock() )
          { }

          virtual ~xtreemfs_lock_releaseRequest() {  }

          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          const string& get_file_id() const { return file_id; }
          const org::xtreemfs::interfaces::Lock& get_lock() const { return lock; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
          void set_file_id( const string& file_id ) { this->file_id = file_id; }
          void set_lock( const org::xtreemfs::interfaces::Lock&  lock ) { this->lock = lock; }

          bool operator==( const xtreemfs_lock_releaseRequest& other ) const
          {
            return get_file_credentials() == other.get_file_credentials()
                   &&
                   get_file_id() == other.get_file_id()
                   &&
                   get_lock() == other.get_lock();
          }

          // yidl::runtime::Object
          xtreemfs_lock_releaseRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lock_releaseRequest, 2010031268 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_credentials", 0 ), get_file_credentials() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "lock", 0 ), get_lock() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_credentials", 0 ), file_credentials );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "lock", 0 ), lock );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_lock_releaseResponse;
          }

          virtual void respond()
          {
            respond( *new xtreemfs_lock_releaseResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          string file_id;
          org::xtreemfs::interfaces::Lock lock;
        };


        class xtreemfs_lock_releaseResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_lock_releaseResponse() {  }

          bool operator==( const xtreemfs_lock_releaseResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_lock_releaseResponse& inc_ref() { return Object::inc_ref( *this ); }

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

          xtreemfs_pingRequest( const xtreemfs_pingRequest& other )
            : coordinates( other.get_coordinates() )
          { }

          virtual ~xtreemfs_pingRequest() {  }

          const org::xtreemfs::interfaces::VivaldiCoordinates& get_coordinates() const { return coordinates; }
          void set_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  coordinates ) { this->coordinates = coordinates; }

          bool operator==( const xtreemfs_pingRequest& other ) const
          {
            return get_coordinates() == other.get_coordinates();
          }

          // yidl::runtime::Object
          xtreemfs_pingRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_pingRequest, 2010031276 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "coordinates", 0 ), get_coordinates() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "coordinates", 0 ), coordinates );
          }

          // yield::concurrency::Request
          virtual ::yield::concurrency::Response* createDefaultResponse()
          {
            return new xtreemfs_pingResponse;
          }

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

          xtreemfs_pingResponse( const xtreemfs_pingResponse& other )
            : remote_coordinates( other.get_remote_coordinates() )
          { }

          virtual ~xtreemfs_pingResponse() {  }

          const org::xtreemfs::interfaces::VivaldiCoordinates& get_remote_coordinates() const { return remote_coordinates; }
          void set_remote_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  remote_coordinates ) { this->remote_coordinates = remote_coordinates; }

          bool operator==( const xtreemfs_pingResponse& other ) const
          {
            return get_remote_coordinates() == other.get_remote_coordinates();
          }

          // yidl::runtime::Object
          xtreemfs_pingResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_pingResponse, 2010031276 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "remote_coordinates", 0 ), get_remote_coordinates() );
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
          virtual ~xtreemfs_shutdownRequest() {  }

          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_shutdownRequest& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 2010031286 );

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


        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          virtual ~xtreemfs_shutdownResponse() {  }

          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

          // yidl::runtime::Object
          xtreemfs_shutdownResponse& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 2010031286 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class ConcurrentModificationException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS
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
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ConcurrentModificationException, 2010031217 );

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


        class errnoException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          errnoException() { }
          errnoException( uint32_t error_code ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code ) { }
          errnoException( const char* error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          errnoException( const string& error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          errnoException( uint32_t error_code, const char* error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code, error_message ) { }
          errnoException( uint32_t error_code, const string& error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code, error_message ) { }
          errnoException( uint32_t error_code, const string& error_message, const string& stack_trace ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code, error_message ), stack_trace( stack_trace ) { }
          virtual ~errnoException() throw() { ; }

          const string& get_stack_trace() const { return stack_trace; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::Object
          errnoException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( errnoException, 2010031218 );

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
            return *new errnoException( get_error_code(), get_error_message(), get_stack_trace() );
          }

          virtual void throwStackClone() const
          {
            throw errnoException( get_error_code(), get_error_message(), get_stack_trace() );
          }

        protected:
          string stack_trace;
        };


        class InvalidArgumentException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          InvalidArgumentException() { }
          InvalidArgumentException( const char* error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          InvalidArgumentException( const string& error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          virtual ~InvalidArgumentException() throw() { ; }

          // yidl::runtime::Object
          InvalidArgumentException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( InvalidArgumentException, 2010031219 );

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


        class OSDException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          OSDException() { }
          OSDException( uint32_t error_code ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code ) { }
          OSDException( const char* error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          OSDException( const string& error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_message ) { }
          OSDException( uint32_t error_code, const char* error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code, error_message ) { }
          OSDException( uint32_t error_code, const string& error_message ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code, error_message ) { }
          OSDException( uint32_t error_code, const string& error_message, const string& stack_trace ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code, error_message ), stack_trace( stack_trace ) { }
          virtual ~OSDException() throw() { ; }

          const string& get_stack_trace() const { return stack_trace; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::Object
          OSDException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( OSDException, 2010031220 );

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
            return *new OSDException( get_error_code(), get_error_message(), get_stack_trace() );
          }

          virtual void throwStackClone() const
          {
            throw OSDException( get_error_code(), get_error_message(), get_stack_trace() );
          }

        protected:
          string stack_trace;
        };


        class ProtocolException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          ProtocolException() : accept_stat( 0 ) { }
          ProtocolException( uint32_t error_code ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code ), accept_stat( 0 ) { }
          ProtocolException( uint32_t accept_stat, uint32_t error_code, const string& stack_trace ) : ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS( error_code ), accept_stat( accept_stat ), stack_trace( stack_trace ) { }
          virtual ~ProtocolException() throw() { ; }

          uint32_t get_accept_stat() const { return accept_stat; }
          void set_accept_stat( uint32_t accept_stat ) { this->accept_stat = accept_stat; }
          const string& get_stack_trace() const { return stack_trace; }
          void set_stack_trace( const string& stack_trace ) { this->stack_trace = stack_trace; }

          // yidl::runtime::Object
          ProtocolException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ProtocolException, 2010031221 );

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


        class RedirectException : public ORG_XTREEMFS_INTERFACES_OSDINTERFACE_EXCEPTION_PARENT_CLASS
        {
        public:
          RedirectException() { }
          RedirectException( const string& to_uuid ) : to_uuid( to_uuid ) { }
          virtual ~RedirectException() throw() { ; }

          const string& get_to_uuid() const { return to_uuid; }
          void set_to_uuid( const string& to_uuid ) { this->to_uuid = to_uuid; }

          // yidl::runtime::Object
          RedirectException& inc_ref() { return Object::inc_ref( *this ); }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( RedirectException, 2010031222 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "to_uuid", 0 ), get_to_uuid() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "to_uuid", 0 ), to_uuid );
          }

          // yield::concurrency::Exception
          virtual ::yield::concurrency::Exception& clone() const
          {
            return *new RedirectException( get_to_uuid() );
          }

          virtual void throwStackClone() const
          {
            throw RedirectException( get_to_uuid() );
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
        virtual ::yield::concurrency::Exception* createException( uint32_t type_id )
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

        virtual ::yield::concurrency::Exception*
        createException
        (
          const char* type_name,
          size_t type_name_len
        )
        {
          if ( type_name_len == 31 && strncmp( type_name, "ConcurrentModificationException", 31 ) == 0 ) return new ConcurrentModificationException;
          else if ( type_name_len == 14 && strncmp( type_name, "errnoException", 14 ) == 0 ) return new errnoException;
          else if ( type_name_len == 24 && strncmp( type_name, "InvalidArgumentException", 24 ) == 0 ) return new InvalidArgumentException;
          else if ( type_name_len == 12 && strncmp( type_name, "OSDException", 12 ) == 0 ) return new OSDException;
          else if ( type_name_len == 17 && strncmp( type_name, "ProtocolException", 17 ) == 0 ) return new ProtocolException;
          else if ( type_name_len == 17 && strncmp( type_name, "RedirectException", 17 ) == 0 ) return new RedirectException;
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

        virtual ::yield::concurrency::Request*
        createRequest
        (
          const char* type_name,
          size_t type_name_len
        )
        {
          if ( type_name_len == 11 && strncmp( type_name, "readRequest", 11 ) == 0 ) return new readRequest;
          else if ( type_name_len == 15 && strncmp( type_name, "truncateRequest", 15 ) == 0 ) return new truncateRequest;
          else if ( type_name_len == 13 && strncmp( type_name, "unlinkRequest", 13 ) == 0 ) return new unlinkRequest;
          else if ( type_name_len == 12 && strncmp( type_name, "writeRequest", 12 ) == 0 ) return new writeRequest;
          else if ( type_name_len == 30 && strncmp( type_name, "xtreemfs_broadcast_gmaxRequest", 30 ) == 0 ) return new xtreemfs_broadcast_gmaxRequest;
          else if ( type_name_len == 28 && strncmp( type_name, "xtreemfs_check_objectRequest", 28 ) == 0 ) return new xtreemfs_check_objectRequest;
          else if ( type_name_len == 35 && strncmp( type_name, "xtreemfs_cleanup_get_resultsRequest", 35 ) == 0 ) return new xtreemfs_cleanup_get_resultsRequest;
          else if ( type_name_len == 34 && strncmp( type_name, "xtreemfs_cleanup_is_runningRequest", 34 ) == 0 ) return new xtreemfs_cleanup_is_runningRequest;
          else if ( type_name_len == 29 && strncmp( type_name, "xtreemfs_cleanup_startRequest", 29 ) == 0 ) return new xtreemfs_cleanup_startRequest;
          else if ( type_name_len == 30 && strncmp( type_name, "xtreemfs_cleanup_statusRequest", 30 ) == 0 ) return new xtreemfs_cleanup_statusRequest;
          else if ( type_name_len == 28 && strncmp( type_name, "xtreemfs_cleanup_stopRequest", 28 ) == 0 ) return new xtreemfs_cleanup_stopRequest;
          else if ( type_name_len == 25 && strncmp( type_name, "xtreemfs_rwr_fetchRequest", 25 ) == 0 ) return new xtreemfs_rwr_fetchRequest;
          else if ( type_name_len == 30 && strncmp( type_name, "xtreemfs_rwr_flease_msgRequest", 30 ) == 0 ) return new xtreemfs_rwr_flease_msgRequest;
          else if ( type_name_len == 26 && strncmp( type_name, "xtreemfs_rwr_notifyRequest", 26 ) == 0 ) return new xtreemfs_rwr_notifyRequest;
          else if ( type_name_len == 26 && strncmp( type_name, "xtreemfs_rwr_statusRequest", 26 ) == 0 ) return new xtreemfs_rwr_statusRequest;
          else if ( type_name_len == 28 && strncmp( type_name, "xtreemfs_rwr_truncateRequest", 28 ) == 0 ) return new xtreemfs_rwr_truncateRequest;
          else if ( type_name_len == 26 && strncmp( type_name, "xtreemfs_rwr_updateRequest", 26 ) == 0 ) return new xtreemfs_rwr_updateRequest;
          else if ( type_name_len == 33 && strncmp( type_name, "xtreemfs_internal_get_gmaxRequest", 33 ) == 0 ) return new xtreemfs_internal_get_gmaxRequest;
          else if ( type_name_len == 33 && strncmp( type_name, "xtreemfs_internal_truncateRequest", 33 ) == 0 ) return new xtreemfs_internal_truncateRequest;
          else if ( type_name_len == 38 && strncmp( type_name, "xtreemfs_internal_get_file_sizeRequest", 38 ) == 0 ) return new xtreemfs_internal_get_file_sizeRequest;
          else if ( type_name_len == 35 && strncmp( type_name, "xtreemfs_internal_read_localRequest", 35 ) == 0 ) return new xtreemfs_internal_read_localRequest;
          else if ( type_name_len == 39 && strncmp( type_name, "xtreemfs_internal_get_object_setRequest", 39 ) == 0 ) return new xtreemfs_internal_get_object_setRequest;
          else if ( type_name_len == 28 && strncmp( type_name, "xtreemfs_lock_acquireRequest", 28 ) == 0 ) return new xtreemfs_lock_acquireRequest;
          else if ( type_name_len == 26 && strncmp( type_name, "xtreemfs_lock_checkRequest", 26 ) == 0 ) return new xtreemfs_lock_checkRequest;
          else if ( type_name_len == 28 && strncmp( type_name, "xtreemfs_lock_releaseRequest", 28 ) == 0 ) return new xtreemfs_lock_releaseRequest;
          else if ( type_name_len == 20 && strncmp( type_name, "xtreemfs_pingRequest", 20 ) == 0 ) return new xtreemfs_pingRequest;
          else if ( type_name_len == 24 && strncmp( type_name, "xtreemfs_shutdownRequest", 24 ) == 0 ) return new xtreemfs_shutdownRequest;
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

        virtual ::yield::concurrency::Response*
        createResponse
        (
          const char* type_name,
          size_t type_name_len
        )
        {
          if ( type_name_len == 12 && strncmp( type_name, "readResponse", 12 ) == 0 ) return new readResponse;
          else if ( type_name_len == 16 && strncmp( type_name, "truncateResponse", 16 ) == 0 ) return new truncateResponse;
          else if ( type_name_len == 14 && strncmp( type_name, "unlinkResponse", 14 ) == 0 ) return new unlinkResponse;
          else if ( type_name_len == 13 && strncmp( type_name, "writeResponse", 13 ) == 0 ) return new writeResponse;
          else if ( type_name_len == 31 && strncmp( type_name, "xtreemfs_broadcast_gmaxResponse", 31 ) == 0 ) return new xtreemfs_broadcast_gmaxResponse;
          else if ( type_name_len == 29 && strncmp( type_name, "xtreemfs_check_objectResponse", 29 ) == 0 ) return new xtreemfs_check_objectResponse;
          else if ( type_name_len == 36 && strncmp( type_name, "xtreemfs_cleanup_get_resultsResponse", 36 ) == 0 ) return new xtreemfs_cleanup_get_resultsResponse;
          else if ( type_name_len == 35 && strncmp( type_name, "xtreemfs_cleanup_is_runningResponse", 35 ) == 0 ) return new xtreemfs_cleanup_is_runningResponse;
          else if ( type_name_len == 30 && strncmp( type_name, "xtreemfs_cleanup_startResponse", 30 ) == 0 ) return new xtreemfs_cleanup_startResponse;
          else if ( type_name_len == 31 && strncmp( type_name, "xtreemfs_cleanup_statusResponse", 31 ) == 0 ) return new xtreemfs_cleanup_statusResponse;
          else if ( type_name_len == 29 && strncmp( type_name, "xtreemfs_cleanup_stopResponse", 29 ) == 0 ) return new xtreemfs_cleanup_stopResponse;
          else if ( type_name_len == 26 && strncmp( type_name, "xtreemfs_rwr_fetchResponse", 26 ) == 0 ) return new xtreemfs_rwr_fetchResponse;
          else if ( type_name_len == 31 && strncmp( type_name, "xtreemfs_rwr_flease_msgResponse", 31 ) == 0 ) return new xtreemfs_rwr_flease_msgResponse;
          else if ( type_name_len == 27 && strncmp( type_name, "xtreemfs_rwr_notifyResponse", 27 ) == 0 ) return new xtreemfs_rwr_notifyResponse;
          else if ( type_name_len == 27 && strncmp( type_name, "xtreemfs_rwr_statusResponse", 27 ) == 0 ) return new xtreemfs_rwr_statusResponse;
          else if ( type_name_len == 29 && strncmp( type_name, "xtreemfs_rwr_truncateResponse", 29 ) == 0 ) return new xtreemfs_rwr_truncateResponse;
          else if ( type_name_len == 27 && strncmp( type_name, "xtreemfs_rwr_updateResponse", 27 ) == 0 ) return new xtreemfs_rwr_updateResponse;
          else if ( type_name_len == 34 && strncmp( type_name, "xtreemfs_internal_get_gmaxResponse", 34 ) == 0 ) return new xtreemfs_internal_get_gmaxResponse;
          else if ( type_name_len == 34 && strncmp( type_name, "xtreemfs_internal_truncateResponse", 34 ) == 0 ) return new xtreemfs_internal_truncateResponse;
          else if ( type_name_len == 39 && strncmp( type_name, "xtreemfs_internal_get_file_sizeResponse", 39 ) == 0 ) return new xtreemfs_internal_get_file_sizeResponse;
          else if ( type_name_len == 36 && strncmp( type_name, "xtreemfs_internal_read_localResponse", 36 ) == 0 ) return new xtreemfs_internal_read_localResponse;
          else if ( type_name_len == 40 && strncmp( type_name, "xtreemfs_internal_get_object_setResponse", 40 ) == 0 ) return new xtreemfs_internal_get_object_setResponse;
          else if ( type_name_len == 29 && strncmp( type_name, "xtreemfs_lock_acquireResponse", 29 ) == 0 ) return new xtreemfs_lock_acquireResponse;
          else if ( type_name_len == 27 && strncmp( type_name, "xtreemfs_lock_checkResponse", 27 ) == 0 ) return new xtreemfs_lock_checkResponse;
          else if ( type_name_len == 29 && strncmp( type_name, "xtreemfs_lock_releaseResponse", 29 ) == 0 ) return new xtreemfs_lock_releaseResponse;
          else if ( type_name_len == 21 && strncmp( type_name, "xtreemfs_pingResponse", 21 ) == 0 ) return new xtreemfs_pingResponse;
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
            case 2010030933: return new OSDWriteResponse;
            case 2010030942: return new FileCredentialsSet;
            case 2010030973: return new ObjectVersion;
            case 2010030927: return new NewFileSize;
            case 2010030917: return new StringSet;
            case 2010030974: return new ObjectVersionList;
            case 2010030966: return new InternalGmax;
            case 2010030939: return new XCap;
            case 2010030972: return new ObjectListSet;
            case 2010030975: return new ReplicaStatus;
            case 2010030918: return new UserCredentials;
            case 2010030967: return new ObjectData;
            case 2010030938: return new VivaldiCoordinates;
            case 2010030937: return new ReplicaSet;
            case 2010030968: return new InternalReadLocalResponse;
            case 2010030970: return new Lock;
            case 2010030935: return new StripingPolicy;
            case 2010030928: return new NewFileSizeSet;
            case 2010030941: return new FileCredentials;
            case 2010030936: return new Replica;
            case 2010030940: return new XLocSet;
            case 2010030971: return new ObjectList;
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
          if ( type_name_len == 16 && strncmp( type_name, "OSDWriteResponse", 16 ) == 0 ) return new OSDWriteResponse;
          else if ( type_name_len == 18 && strncmp( type_name, "FileCredentialsSet", 18 ) == 0 ) return new FileCredentialsSet;
          else if ( type_name_len == 13 && strncmp( type_name, "ObjectVersion", 13 ) == 0 ) return new ObjectVersion;
          else if ( type_name_len == 11 && strncmp( type_name, "NewFileSize", 11 ) == 0 ) return new NewFileSize;
          else if ( type_name_len == 9 && strncmp( type_name, "StringSet", 9 ) == 0 ) return new StringSet;
          else if ( type_name_len == 17 && strncmp( type_name, "ObjectVersionList", 17 ) == 0 ) return new ObjectVersionList;
          else if ( type_name_len == 12 && strncmp( type_name, "InternalGmax", 12 ) == 0 ) return new InternalGmax;
          else if ( type_name_len == 4 && strncmp( type_name, "XCap", 4 ) == 0 ) return new XCap;
          else if ( type_name_len == 13 && strncmp( type_name, "ObjectListSet", 13 ) == 0 ) return new ObjectListSet;
          else if ( type_name_len == 13 && strncmp( type_name, "ReplicaStatus", 13 ) == 0 ) return new ReplicaStatus;
          else if ( type_name_len == 15 && strncmp( type_name, "UserCredentials", 15 ) == 0 ) return new UserCredentials;
          else if ( type_name_len == 10 && strncmp( type_name, "ObjectData", 10 ) == 0 ) return new ObjectData;
          else if ( type_name_len == 18 && strncmp( type_name, "VivaldiCoordinates", 18 ) == 0 ) return new VivaldiCoordinates;
          else if ( type_name_len == 10 && strncmp( type_name, "ReplicaSet", 10 ) == 0 ) return new ReplicaSet;
          else if ( type_name_len == 25 && strncmp( type_name, "InternalReadLocalResponse", 25 ) == 0 ) return new InternalReadLocalResponse;
          else if ( type_name_len == 4 && strncmp( type_name, "Lock", 4 ) == 0 ) return new Lock;
          else if ( type_name_len == 14 && strncmp( type_name, "StripingPolicy", 14 ) == 0 ) return new StripingPolicy;
          else if ( type_name_len == 14 && strncmp( type_name, "NewFileSizeSet", 14 ) == 0 ) return new NewFileSizeSet;
          else if ( type_name_len == 15 && strncmp( type_name, "FileCredentials", 15 ) == 0 ) return new FileCredentials;
          else if ( type_name_len == 7 && strncmp( type_name, "Replica", 7 ) == 0 ) return new Replica;
          else if ( type_name_len == 7 && strncmp( type_name, "XLocSet", 7 ) == 0 ) return new XLocSet;
          else if ( type_name_len == 10 && strncmp( type_name, "ObjectList", 10 ) == 0 ) return new ObjectList;
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

        // yidl::runtime::RTTIObject
        virtual const char* get_type_name() const
        {
          return "OSDInterface";
        }

        // yield::concurrency::RequestHandler
        virtual void handle( ::yield::concurrency::Request& request )
        {
          // Switch on the request types that this interface handles, unwrap the corresponding requests and delegate to _interface
          switch ( request.get_type_id() )
          {
            case 2010031226UL: handle( static_cast<readRequest&>( request ) ); return;
            case 2010031227UL: handle( static_cast<truncateRequest&>( request ) ); return;
            case 2010031228UL: handle( static_cast<unlinkRequest&>( request ) ); return;
            case 2010031229UL: handle( static_cast<writeRequest&>( request ) ); return;
            case 2010031236UL: handle( static_cast<xtreemfs_broadcast_gmaxRequest&>( request ) ); return;
            case 2010031237UL: handle( static_cast<xtreemfs_check_objectRequest&>( request ) ); return;
            case 2010031246UL: handle( static_cast<xtreemfs_cleanup_get_resultsRequest&>( request ) ); return;
            case 2010031247UL: handle( static_cast<xtreemfs_cleanup_is_runningRequest&>( request ) ); return;
            case 2010031248UL: handle( static_cast<xtreemfs_cleanup_startRequest&>( request ) ); return;
            case 2010031249UL: handle( static_cast<xtreemfs_cleanup_statusRequest&>( request ) ); return;
            case 2010031250UL: handle( static_cast<xtreemfs_cleanup_stopRequest&>( request ) ); return;
            case 2010031289UL: handle( static_cast<xtreemfs_rwr_fetchRequest&>( request ) ); return;
            case 2010031287UL: handle( static_cast<xtreemfs_rwr_flease_msgRequest&>( request ) ); return;
            case 2010031291UL: handle( static_cast<xtreemfs_rwr_notifyRequest&>( request ) ); return;
            case 2010031292UL: handle( static_cast<xtreemfs_rwr_statusRequest&>( request ) ); return;
            case 2010031290UL: handle( static_cast<xtreemfs_rwr_truncateRequest&>( request ) ); return;
            case 2010031288UL: handle( static_cast<xtreemfs_rwr_updateRequest&>( request ) ); return;
            case 2010031256UL: handle( static_cast<xtreemfs_internal_get_gmaxRequest&>( request ) ); return;
            case 2010031257UL: handle( static_cast<xtreemfs_internal_truncateRequest&>( request ) ); return;
            case 2010031258UL: handle( static_cast<xtreemfs_internal_get_file_sizeRequest&>( request ) ); return;
            case 2010031259UL: handle( static_cast<xtreemfs_internal_read_localRequest&>( request ) ); return;
            case 2010031260UL: handle( static_cast<xtreemfs_internal_get_object_setRequest&>( request ) ); return;
            case 2010031266UL: handle( static_cast<xtreemfs_lock_acquireRequest&>( request ) ); return;
            case 2010031267UL: handle( static_cast<xtreemfs_lock_checkRequest&>( request ) ); return;
            case 2010031268UL: handle( static_cast<xtreemfs_lock_releaseRequest&>( request ) ); return;
            case 2010031276UL: handle( static_cast<xtreemfs_pingRequest&>( request ) ); return;
            case 2010031286UL: handle( static_cast<xtreemfs_shutdownRequest&>( request ) ); return;
          }
        }

      protected:

        virtual void handle( readRequest& __request )
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

        virtual void handle( truncateRequest& __request )
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

        virtual void handle( unlinkRequest& __request )
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

        virtual void handle( writeRequest& __request )
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

        virtual void handle( xtreemfs_broadcast_gmaxRequest& __request )
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

        virtual void handle( xtreemfs_check_objectRequest& __request )
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

        virtual void handle( xtreemfs_cleanup_get_resultsRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              org::xtreemfs::interfaces::StringSet results;

              _interface->xtreemfs_cleanup_get_results( results );

              __request.respond( results );
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

        virtual void handle( xtreemfs_cleanup_is_runningRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              bool is_running;

              _interface->xtreemfs_cleanup_is_running( is_running );

              __request.respond( is_running );
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

        virtual void handle( xtreemfs_cleanup_startRequest& __request )
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

        virtual void handle( xtreemfs_cleanup_statusRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              string status;

              _interface->xtreemfs_cleanup_status( status );

              __request.respond( status );
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

        virtual void handle( xtreemfs_cleanup_stopRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->xtreemfs_cleanup_stop();
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

        virtual void handle( xtreemfs_rwr_fetchRequest& __request )
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

        virtual void handle( xtreemfs_rwr_flease_msgRequest& __request )
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

        virtual void handle( xtreemfs_rwr_notifyRequest& __request )
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

        virtual void handle( xtreemfs_rwr_statusRequest& __request )
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

        virtual void handle( xtreemfs_rwr_truncateRequest& __request )
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

        virtual void handle( xtreemfs_rwr_updateRequest& __request )
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

        virtual void handle( xtreemfs_internal_get_gmaxRequest& __request )
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

        virtual void handle( xtreemfs_internal_truncateRequest& __request )
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

        virtual void handle( xtreemfs_internal_get_file_sizeRequest& __request )
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

        virtual void handle( xtreemfs_internal_read_localRequest& __request )
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

        virtual void handle( xtreemfs_internal_get_object_setRequest& __request )
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

        virtual void handle( xtreemfs_lock_acquireRequest& __request )
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

        virtual void handle( xtreemfs_lock_checkRequest& __request )
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

        virtual void handle( xtreemfs_lock_releaseRequest& __request )
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

        virtual void handle( xtreemfs_pingRequest& __request )
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
        OSDInterface* _interface;
      };

      #define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_REQUEST_HANDLER_PROTOTYPES \
      virtual void handle( readRequest& __request );\
      virtual void handle( truncateRequest& __request );\
      virtual void handle( unlinkRequest& __request );\
      virtual void handle( writeRequest& __request );\
      virtual void handle( xtreemfs_broadcast_gmaxRequest& __request );\
      virtual void handle( xtreemfs_check_objectRequest& __request );\
      virtual void handle( xtreemfs_cleanup_get_resultsRequest& __request );\
      virtual void handle( xtreemfs_cleanup_is_runningRequest& __request );\
      virtual void handle( xtreemfs_cleanup_startRequest& __request );\
      virtual void handle( xtreemfs_cleanup_statusRequest& __request );\
      virtual void handle( xtreemfs_cleanup_stopRequest& __request );\
      virtual void handle( xtreemfs_rwr_fetchRequest& __request );\
      virtual void handle( xtreemfs_rwr_flease_msgRequest& __request );\
      virtual void handle( xtreemfs_rwr_notifyRequest& __request );\
      virtual void handle( xtreemfs_rwr_statusRequest& __request );\
      virtual void handle( xtreemfs_rwr_truncateRequest& __request );\
      virtual void handle( xtreemfs_rwr_updateRequest& __request );\
      virtual void handle( xtreemfs_internal_get_gmaxRequest& __request );\
      virtual void handle( xtreemfs_internal_truncateRequest& __request );\
      virtual void handle( xtreemfs_internal_get_file_sizeRequest& __request );\
      virtual void handle( xtreemfs_internal_read_localRequest& __request );\
      virtual void handle( xtreemfs_internal_get_object_setRequest& __request );\
      virtual void handle( xtreemfs_lock_acquireRequest& __request );\
      virtual void handle( xtreemfs_lock_checkRequest& __request );\
      virtual void handle( xtreemfs_lock_releaseRequest& __request );\
      virtual void handle( xtreemfs_pingRequest& __request );\
      virtual void handle( xtreemfs_shutdownRequest& __request );


      class OSDInterfaceProxy
        : public OSDInterface,
          public ::yield::concurrency::RequestHandler,
          private OSDInterfaceMessages
      {
      public:
        OSDInterfaceProxy( ::yield::concurrency::EventHandler& request_handler )
          : __request_handler( request_handler )
        { }

        ~OSDInterfaceProxy()
        {
          ::yield::concurrency::EventHandler::dec_ref( __request_handler );
        }

        // yidl::runtime::RTTIObject
        virtual const char* get_type_name() const
        {
          return "OSDInterfaceProxy";
        }

        // yield::concurrency::RequestHandler
        virtual void handle( ::yield::concurrency::Request& request )
        {
          __request_handler.handle( request );
        }

        // OSDInterface
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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_cleanup_get_resultsResponse> __response = __response_queue->dequeue();
          results = __response->get_results();
        }

        virtual void xtreemfs_cleanup_is_running( bool& is_running )
        {
          xtreemfs_cleanup_is_runningRequest* __request = new xtreemfs_cleanup_is_runningRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_is_runningResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_is_runningResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_cleanup_startResponse> __response = __response_queue->dequeue();
        }

        virtual void xtreemfs_cleanup_status( string& status )
        {
          xtreemfs_cleanup_statusRequest* __request = new xtreemfs_cleanup_statusRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_statusResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_statusResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_cleanup_statusResponse> __response = __response_queue->dequeue();
          status = __response->get_status();
        }

        virtual void xtreemfs_cleanup_stop()
        {
          xtreemfs_cleanup_stopRequest* __request = new xtreemfs_cleanup_stopRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_stopResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_cleanup_stopResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

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
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<xtreemfs_pingResponse> __response = __response_queue->dequeue();
          remote_coordinates = __response->get_remote_coordinates();
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
        // a reference cycle when __request_handler is a subclass of OSDInterfaceProxy
        ::yield::concurrency::EventHandler& __request_handler;
      };};
  };
};
#endif
