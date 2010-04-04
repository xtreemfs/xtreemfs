#ifndef _1252488516_H_
#define _1252488516_H_


#include "types.h"
#include "yidl.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      enum AccessControlPolicyType { ACCESS_CONTROL_POLICY_NULL = 1, ACCESS_CONTROL_POLICY_POSIX = 2, ACCESS_CONTROL_POLICY_VOLUME = 3 };
      enum OSDSelectionPolicyType { OSD_SELECTION_POLICY_FILTER_DEFAULT = 1000, OSD_SELECTION_POLICY_FILTER_FQDN = 1001, OSD_SELECTION_POLICY_FILTER_UUID = 1002, OSD_SELECTION_POLICY_GROUP_DCMAP = 2000, OSD_SELECTION_POLICY_GROUP_FQDN = 2001, OSD_SELECTION_POLICY_SORT_DCMAP = 3000, OSD_SELECTION_POLICY_SORT_FQDN = 3001, OSD_SELECTION_POLICY_SORT_RANDOM = 3002, OSD_SELECTION_POLICY_SORT_VIVALDI = 3003 };
      enum ReplicaSelectionPolicyType { REPLICA_SELECTION_POLICY_SIMPLE = 1 };
      enum SnapConfig { SNAP_CONFIG_SNAPS_DISABLED = 0, SNAP_CONFIG_ACCESS_CURRENT = 1, SNAP_CONFIG_ACCESS_SNAP = 2 };
      enum StripingPolicyType { STRIPING_POLICY_RAID0 = 0 };


      class NewFileSize : public ::yidl::runtime::Struct
      {
      public:
        NewFileSize()
          : size_in_bytes( 0 ), truncate_epoch( 0 )
        { }

        NewFileSize( uint64_t size_in_bytes, uint32_t truncate_epoch )
          : size_in_bytes( size_in_bytes ), truncate_epoch( truncate_epoch )
        { }

        NewFileSize( const NewFileSize& other )
          : size_in_bytes( other.get_size_in_bytes() ),
            truncate_epoch( other.get_truncate_epoch() )
        { }

        virtual ~NewFileSize() {  }

        uint64_t get_size_in_bytes() const { return size_in_bytes; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        void set_size_in_bytes( uint64_t size_in_bytes ) { this->size_in_bytes = size_in_bytes; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }

        bool operator==( const NewFileSize& other ) const
        {
          return get_size_in_bytes() == other.get_size_in_bytes()
                 &&
                 get_truncate_epoch() == other.get_truncate_epoch();
        }

        // yidl::runtime::Object
        NewFileSize& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( NewFileSize, 2010030927 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "size_in_bytes", 0 ), get_size_in_bytes() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "truncate_epoch", 0 ), get_truncate_epoch() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "size_in_bytes", 0 ), size_in_bytes );
          truncate_epoch = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "truncate_epoch", 0 ) );
        }

      protected:
        uint64_t size_in_bytes;
        uint32_t truncate_epoch;
      };

      class NewFileSizeSet
        : public ::yidl::runtime::Sequence,
          public vector<org::xtreemfs::interfaces::NewFileSize>
      {
      public:
        NewFileSizeSet() { }
        NewFileSizeSet( const org::xtreemfs::interfaces::NewFileSize& first_value ) { vector<org::xtreemfs::interfaces::NewFileSize>::push_back( first_value ); }
        NewFileSizeSet( size_type size ) : vector<org::xtreemfs::interfaces::NewFileSize>( size ) { }
        virtual ~NewFileSizeSet() { }

        // yidl::runtime::Object
        NewFileSizeSet& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( NewFileSizeSet, 2010030928 );

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
          org::xtreemfs::interfaces::NewFileSize value;
          unmarshaller.read( ::yidl::runtime::Marshaller::Key(), value );
          push_back( value );
        }

        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };

      class OSDWriteResponse : public ::yidl::runtime::Struct
      {
      public:
        OSDWriteResponse() { }

        OSDWriteResponse
        (
          const org::xtreemfs::interfaces::NewFileSizeSet& new_file_size
        )
          : new_file_size( new_file_size )
        { }

        OSDWriteResponse( const OSDWriteResponse& other )
          : new_file_size( other.get_new_file_size() )
        { }

        virtual ~OSDWriteResponse() {  }

        const org::xtreemfs::interfaces::NewFileSizeSet& get_new_file_size() const { return new_file_size; }
        void set_new_file_size( const org::xtreemfs::interfaces::NewFileSizeSet&  new_file_size ) { this->new_file_size = new_file_size; }

        bool operator==( const OSDWriteResponse& other ) const
        {
          return get_new_file_size() == other.get_new_file_size();
        }

        // yidl::runtime::Object
        OSDWriteResponse& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( OSDWriteResponse, 2010030933 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "new_file_size", 0 ), get_new_file_size() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "new_file_size", 0 ), new_file_size );
        }

      protected:
        org::xtreemfs::interfaces::NewFileSizeSet new_file_size;
      };

      class StripingPolicy : public ::yidl::runtime::Struct
      {
      public:
        StripingPolicy()
          : type( STRIPING_POLICY_RAID0 ), stripe_size( 0 ), width( 0 )
        { }

        StripingPolicy
        (
          org::xtreemfs::interfaces::StripingPolicyType type,
          uint32_t stripe_size,
          uint32_t width
        )
          : type( type ), stripe_size( stripe_size ), width( width )
        { }

        StripingPolicy( const StripingPolicy& other )
          : type( other.get_type() ),
            stripe_size( other.get_stripe_size() ),
            width( other.get_width() )
        { }

        virtual ~StripingPolicy() {  }

        org::xtreemfs::interfaces::StripingPolicyType get_type() const { return type; }
        uint32_t get_stripe_size() const { return stripe_size; }
        uint32_t get_width() const { return width; }
        void set_type( org::xtreemfs::interfaces::StripingPolicyType type ) { this->type = type; }
        void set_stripe_size( uint32_t stripe_size ) { this->stripe_size = stripe_size; }
        void set_width( uint32_t width ) { this->width = width; }

        bool operator==( const StripingPolicy& other ) const
        {
          return get_type() == other.get_type()
                 &&
                 get_stripe_size() == other.get_stripe_size()
                 &&
                 get_width() == other.get_width();
        }

        // yidl::runtime::Object
        StripingPolicy& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StripingPolicy, 2010030935 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "type", 0 ), static_cast<int32_t>( get_type() ) );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "stripe_size", 0 ), get_stripe_size() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "width", 0 ), get_width() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          type = static_cast<org::xtreemfs::interfaces::StripingPolicyType>( unmarshaller.read_int32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "type", 0 ) ) );
          stripe_size = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "stripe_size", 0 ) );
          width = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "width", 0 ) );
        }

      protected:
        org::xtreemfs::interfaces::StripingPolicyType type;
        uint32_t stripe_size;
        uint32_t width;
      };

      class Replica : public ::yidl::runtime::Struct
      {
      public:
        Replica()
          : replication_flags( 0 )
        { }

        Replica
        (
          const org::xtreemfs::interfaces::StringSet& osd_uuids,
          uint32_t replication_flags,
          const org::xtreemfs::interfaces::StripingPolicy& striping_policy
        )
          : osd_uuids( osd_uuids ),
            replication_flags( replication_flags ),
            striping_policy( striping_policy )
        { }

        Replica( const Replica& other )
          : osd_uuids( other.get_osd_uuids() ),
            replication_flags( other.get_replication_flags() ),
            striping_policy( other.get_striping_policy() )
        { }

        virtual ~Replica() {  }

        const org::xtreemfs::interfaces::StringSet& get_osd_uuids() const { return osd_uuids; }
        uint32_t get_replication_flags() const { return replication_flags; }
        const org::xtreemfs::interfaces::StripingPolicy& get_striping_policy() const { return striping_policy; }
        void set_osd_uuids( const org::xtreemfs::interfaces::StringSet&  osd_uuids ) { this->osd_uuids = osd_uuids; }
        void set_replication_flags( uint32_t replication_flags ) { this->replication_flags = replication_flags; }
        void set_striping_policy( const org::xtreemfs::interfaces::StripingPolicy&  striping_policy ) { this->striping_policy = striping_policy; }

        bool operator==( const Replica& other ) const
        {
          return get_osd_uuids() == other.get_osd_uuids()
                 &&
                 get_replication_flags() == other.get_replication_flags()
                 &&
                 get_striping_policy() == other.get_striping_policy();
        }

        // yidl::runtime::Object
        Replica& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Replica, 2010030936 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "osd_uuids", 0 ), get_osd_uuids() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "replication_flags", 0 ), get_replication_flags() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "striping_policy", 0 ), get_striping_policy() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "osd_uuids", 0 ), osd_uuids );
          replication_flags = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "replication_flags", 0 ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "striping_policy", 0 ), striping_policy );
        }

      protected:
        org::xtreemfs::interfaces::StringSet osd_uuids;
        uint32_t replication_flags;
        org::xtreemfs::interfaces::StripingPolicy striping_policy;
      };

      class ReplicaSet
        : public ::yidl::runtime::Sequence,
          public vector<org::xtreemfs::interfaces::Replica>
      {
      public:
        ReplicaSet() { }
        ReplicaSet( const org::xtreemfs::interfaces::Replica& first_value ) { vector<org::xtreemfs::interfaces::Replica>::push_back( first_value ); }
        ReplicaSet( size_type size ) : vector<org::xtreemfs::interfaces::Replica>( size ) { }
        virtual ~ReplicaSet() { }

        // yidl::runtime::Object
        ReplicaSet& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ReplicaSet, 2010030937 );

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
          org::xtreemfs::interfaces::Replica value;
          unmarshaller.read( ::yidl::runtime::Marshaller::Key(), value );
          push_back( value );
        }

        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };

      class VivaldiCoordinates : public ::yidl::runtime::Struct
      {
      public:
        VivaldiCoordinates()
          : x_coordinate( 0 ), y_coordinate( 0 ), local_error( 0 )
        { }

        VivaldiCoordinates
        (
          double x_coordinate,
          double y_coordinate,
          double local_error
        )
          : x_coordinate( x_coordinate ),
            y_coordinate( y_coordinate ),
            local_error( local_error )
        { }

        VivaldiCoordinates( const VivaldiCoordinates& other )
          : x_coordinate( other.get_x_coordinate() ),
            y_coordinate( other.get_y_coordinate() ),
            local_error( other.get_local_error() )
        { }

        virtual ~VivaldiCoordinates() {  }

        double get_x_coordinate() const { return x_coordinate; }
        double get_y_coordinate() const { return y_coordinate; }
        double get_local_error() const { return local_error; }
        void set_x_coordinate( double x_coordinate ) { this->x_coordinate = x_coordinate; }
        void set_y_coordinate( double y_coordinate ) { this->y_coordinate = y_coordinate; }
        void set_local_error( double local_error ) { this->local_error = local_error; }

        bool operator==( const VivaldiCoordinates& other ) const
        {
          return get_x_coordinate() == other.get_x_coordinate()
                 &&
                 get_y_coordinate() == other.get_y_coordinate()
                 &&
                 get_local_error() == other.get_local_error();
        }

        // yidl::runtime::Object
        VivaldiCoordinates& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( VivaldiCoordinates, 2010030938 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "x_coordinate", 0 ), get_x_coordinate() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "y_coordinate", 0 ), get_y_coordinate() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "local_error", 0 ), get_local_error() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "x_coordinate", 0 ), x_coordinate );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "y_coordinate", 0 ), y_coordinate );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "local_error", 0 ), local_error );
        }

      protected:
        double x_coordinate;
        double y_coordinate;
        double local_error;
      };

      class XCap : public ::yidl::runtime::Struct
      {
      public:
        XCap()
          : access_mode( 0 ),
            expire_time_s( 0 ),
            expire_timeout_s( 0 ),
            replicate_on_close( false ),
            truncate_epoch( 0 ),
            snap_config( SNAP_CONFIG_SNAPS_DISABLED ),
            snap_timestamp( 0 )
        { }

        XCap
        (
          uint32_t access_mode,
          const string& client_identity,
          uint64_t expire_time_s,
          uint32_t expire_timeout_s,
          const string& file_id,
          bool replicate_on_close,
          const string& server_signature,
          uint32_t truncate_epoch,
          org::xtreemfs::interfaces::SnapConfig snap_config,
          uint64_t snap_timestamp
        )
          : access_mode( access_mode ),
            client_identity( client_identity ),
            expire_time_s( expire_time_s ),
            expire_timeout_s( expire_timeout_s ),
            file_id( file_id ),
            replicate_on_close( replicate_on_close ),
            server_signature( server_signature ),
            truncate_epoch( truncate_epoch ),
            snap_config( snap_config ),
            snap_timestamp( snap_timestamp )
        { }

        XCap( const XCap& other )
          : access_mode( other.get_access_mode() ),
            client_identity( other.get_client_identity() ),
            expire_time_s( other.get_expire_time_s() ),
            expire_timeout_s( other.get_expire_timeout_s() ),
            file_id( other.get_file_id() ),
            replicate_on_close( other.get_replicate_on_close() ),
            server_signature( other.get_server_signature() ),
            truncate_epoch( other.get_truncate_epoch() ),
            snap_config( other.get_snap_config() ),
            snap_timestamp( other.get_snap_timestamp() )
        { }

        virtual ~XCap() {  }

        uint32_t get_access_mode() const { return access_mode; }
        const string& get_client_identity() const { return client_identity; }
        uint64_t get_expire_time_s() const { return expire_time_s; }
        uint32_t get_expire_timeout_s() const { return expire_timeout_s; }
        const string& get_file_id() const { return file_id; }
        bool get_replicate_on_close() const { return replicate_on_close; }
        const string& get_server_signature() const { return server_signature; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        org::xtreemfs::interfaces::SnapConfig get_snap_config() const { return snap_config; }
        uint64_t get_snap_timestamp() const { return snap_timestamp; }
        void set_access_mode( uint32_t access_mode ) { this->access_mode = access_mode; }
        void set_client_identity( const string& client_identity ) { this->client_identity = client_identity; }
        void set_expire_time_s( uint64_t expire_time_s ) { this->expire_time_s = expire_time_s; }
        void set_expire_timeout_s( uint32_t expire_timeout_s ) { this->expire_timeout_s = expire_timeout_s; }
        void set_file_id( const string& file_id ) { this->file_id = file_id; }
        void set_replicate_on_close( bool replicate_on_close ) { this->replicate_on_close = replicate_on_close; }
        void set_server_signature( const string& server_signature ) { this->server_signature = server_signature; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        void set_snap_config( org::xtreemfs::interfaces::SnapConfig snap_config ) { this->snap_config = snap_config; }
        void set_snap_timestamp( uint64_t snap_timestamp ) { this->snap_timestamp = snap_timestamp; }

        bool operator==( const XCap& other ) const
        {
          return get_access_mode() == other.get_access_mode()
                 &&
                 get_client_identity() == other.get_client_identity()
                 &&
                 get_expire_time_s() == other.get_expire_time_s()
                 &&
                 get_expire_timeout_s() == other.get_expire_timeout_s()
                 &&
                 get_file_id() == other.get_file_id()
                 &&
                 get_replicate_on_close() == other.get_replicate_on_close()
                 &&
                 get_server_signature() == other.get_server_signature()
                 &&
                 get_truncate_epoch() == other.get_truncate_epoch()
                 &&
                 get_snap_config() == other.get_snap_config()
                 &&
                 get_snap_timestamp() == other.get_snap_timestamp();
        }

        // yidl::runtime::Object
        XCap& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( XCap, 2010030939 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "access_mode", 0 ), get_access_mode() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "client_identity", 0 ), get_client_identity() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "expire_time_s", 0 ), get_expire_time_s() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "expire_timeout_s", 0 ), get_expire_timeout_s() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "file_id", 0 ), get_file_id() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "replicate_on_close", 0 ), get_replicate_on_close() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "server_signature", 0 ), get_server_signature() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "truncate_epoch", 0 ), get_truncate_epoch() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "snap_config", 0 ), static_cast<int32_t>( get_snap_config() ) );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "snap_timestamp", 0 ), get_snap_timestamp() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          access_mode = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "access_mode", 0 ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "client_identity", 0 ), client_identity );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "expire_time_s", 0 ), expire_time_s );
          expire_timeout_s = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "expire_timeout_s", 0 ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "file_id", 0 ), file_id );
          replicate_on_close = unmarshaller.read_bool( ::yidl::runtime::Unmarshaller::StringLiteralKey( "replicate_on_close", 0 ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "server_signature", 0 ), server_signature );
          truncate_epoch = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "truncate_epoch", 0 ) );
          snap_config = static_cast<org::xtreemfs::interfaces::SnapConfig>( unmarshaller.read_int32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "snap_config", 0 ) ) );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "snap_timestamp", 0 ), snap_timestamp );
        }

      protected:
        uint32_t access_mode;
        string client_identity;
        uint64_t expire_time_s;
        uint32_t expire_timeout_s;
        string file_id;
        bool replicate_on_close;
        string server_signature;
        uint32_t truncate_epoch;
        org::xtreemfs::interfaces::SnapConfig snap_config;
        uint64_t snap_timestamp;
      };

      class XLocSet : public ::yidl::runtime::Struct
      {
      public:
        XLocSet()
          : read_only_file_size( 0 ), version( 0 )
        { }

        XLocSet
        (
          uint64_t read_only_file_size,
          const org::xtreemfs::interfaces::ReplicaSet& replicas,
          const string& replica_update_policy,
          uint32_t version
        )
          : read_only_file_size( read_only_file_size ),
            replicas( replicas ),
            replica_update_policy( replica_update_policy ),
            version( version )
        { }

        XLocSet( const XLocSet& other )
          : read_only_file_size( other.get_read_only_file_size() ),
            replicas( other.get_replicas() ),
            replica_update_policy( other.get_replica_update_policy() ),
            version( other.get_version() )
        { }

        virtual ~XLocSet() {  }

        uint64_t get_read_only_file_size() const { return read_only_file_size; }
        const org::xtreemfs::interfaces::ReplicaSet& get_replicas() const { return replicas; }
        const string& get_replica_update_policy() const { return replica_update_policy; }
        uint32_t get_version() const { return version; }
        void set_read_only_file_size( uint64_t read_only_file_size ) { this->read_only_file_size = read_only_file_size; }
        void set_replicas( const org::xtreemfs::interfaces::ReplicaSet&  replicas ) { this->replicas = replicas; }
        void set_replica_update_policy( const string& replica_update_policy ) { this->replica_update_policy = replica_update_policy; }
        void set_version( uint32_t version ) { this->version = version; }

        bool operator==( const XLocSet& other ) const
        {
          return get_read_only_file_size() == other.get_read_only_file_size()
                 &&
                 get_replicas() == other.get_replicas()
                 &&
                 get_replica_update_policy() == other.get_replica_update_policy()
                 &&
                 get_version() == other.get_version();
        }

        // yidl::runtime::Object
        XLocSet& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( XLocSet, 2010030940 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "read_only_file_size", 0 ), get_read_only_file_size() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "replicas", 0 ), get_replicas() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "replica_update_policy", 0 ), get_replica_update_policy() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "version", 0 ), get_version() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "read_only_file_size", 0 ), read_only_file_size );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "replicas", 0 ), replicas );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "replica_update_policy", 0 ), replica_update_policy );
          version = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "version", 0 ) );
        }

      protected:
        uint64_t read_only_file_size;
        org::xtreemfs::interfaces::ReplicaSet replicas;
        string replica_update_policy;
        uint32_t version;
      };

      class FileCredentials : public ::yidl::runtime::Struct
      {
      public:
        FileCredentials() { }

        FileCredentials
        (
          const org::xtreemfs::interfaces::XCap& xcap,
          const org::xtreemfs::interfaces::XLocSet& xlocs
        )
          : xcap( xcap ), xlocs( xlocs )
        { }

        FileCredentials( const FileCredentials& other )
          : xcap( other.get_xcap() ),
            xlocs( other.get_xlocs() )
        { }

        virtual ~FileCredentials() {  }

        const org::xtreemfs::interfaces::XCap& get_xcap() const { return xcap; }
        const org::xtreemfs::interfaces::XLocSet& get_xlocs() const { return xlocs; }
        void set_xcap( const org::xtreemfs::interfaces::XCap&  xcap ) { this->xcap = xcap; }
        void set_xlocs( const org::xtreemfs::interfaces::XLocSet&  xlocs ) { this->xlocs = xlocs; }

        bool operator==( const FileCredentials& other ) const
        {
          return get_xcap() == other.get_xcap()
                 &&
                 get_xlocs() == other.get_xlocs();
        }

        // yidl::runtime::Object
        FileCredentials& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( FileCredentials, 2010030941 );

        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "xcap", 0 ), get_xcap() );
          marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "xlocs", 0 ), get_xlocs() );
        }

        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "xcap", 0 ), xcap );
          unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "xlocs", 0 ), xlocs );
        }

      protected:
        org::xtreemfs::interfaces::XCap xcap;
        org::xtreemfs::interfaces::XLocSet xlocs;
      };

      class FileCredentialsSet
        : public ::yidl::runtime::Sequence,
          public vector<org::xtreemfs::interfaces::FileCredentials>
      {
      public:
        FileCredentialsSet() { }
        FileCredentialsSet( const org::xtreemfs::interfaces::FileCredentials& first_value ) { vector<org::xtreemfs::interfaces::FileCredentials>::push_back( first_value ); }
        FileCredentialsSet( size_type size ) : vector<org::xtreemfs::interfaces::FileCredentials>( size ) { }
        virtual ~FileCredentialsSet() { }

        // yidl::runtime::Object
        FileCredentialsSet& inc_ref() { return Object::inc_ref( *this ); }

        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( FileCredentialsSet, 2010030942 );

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
          org::xtreemfs::interfaces::FileCredentials value;
          unmarshaller.read( ::yidl::runtime::Marshaller::Key(), value );
          push_back( value );
        }

        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };

    };
  };
};
#endif
