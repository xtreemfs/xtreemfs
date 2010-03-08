#ifndef _1810472102_H_
#define _1810472102_H_


#include "types.h"
#include "yidl.h"
#include <vector>


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
        NewFileSize() : size_in_bytes( 0 ), truncate_epoch( 0 ) { }
        NewFileSize( uint64_t size_in_bytes, uint32_t truncate_epoch ) : size_in_bytes( size_in_bytes ), truncate_epoch( truncate_epoch ) { }
        virtual ~NewFileSize() {  }
  
        uint64_t get_size_in_bytes() const { return size_in_bytes; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        void set_size_in_bytes( uint64_t size_in_bytes ) { this->size_in_bytes = size_in_bytes; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
  
        bool operator==( const NewFileSize& other ) const { return size_in_bytes == other.size_in_bytes && truncate_epoch == other.truncate_epoch; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( NewFileSize, 2010030325 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "size_in_bytes", 0, size_in_bytes ); marshaller.write( "truncate_epoch", 0, truncate_epoch ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { size_in_bytes = unmarshaller.read_uint64( "size_in_bytes", 0 ); truncate_epoch = unmarshaller.read_uint32( "truncate_epoch", 0 ); }
  
      protected:
        uint64_t size_in_bytes;
        uint32_t truncate_epoch;
      };
  
      class NewFileSizeSet
        : public ::yidl::runtime::Sequence,
          public std::vector<org::xtreemfs::interfaces::NewFileSize>
      {
      public:
        NewFileSizeSet() { }
        NewFileSizeSet( const org::xtreemfs::interfaces::NewFileSize& first_value ) { std::vector<org::xtreemfs::interfaces::NewFileSize>::push_back( first_value ); }
        NewFileSizeSet( size_type size ) : std::vector<org::xtreemfs::interfaces::NewFileSize>( size ) { }
        virtual ~NewFileSizeSet() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( NewFileSizeSet, 2010030326 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          size_type value_i_max = size();
          for ( size_type value_i = 0; value_i < value_i_max; value_i++ )
          {
            marshaller.write( "value", 0, ( *this )[value_i] );
          }
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          org::xtreemfs::interfaces::NewFileSize value;
          unmarshaller.read( "value", 0, value );
          push_back( value );
        }
  
        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };
  
      class OSDWriteResponse : public ::yidl::runtime::Struct
      {
      public:
        OSDWriteResponse() { }
        OSDWriteResponse( const org::xtreemfs::interfaces::NewFileSizeSet& new_file_size ) : new_file_size( new_file_size ) { }
        virtual ~OSDWriteResponse() {  }
  
        const org::xtreemfs::interfaces::NewFileSizeSet& get_new_file_size() const { return new_file_size; }
        void set_new_file_size( const org::xtreemfs::interfaces::NewFileSizeSet&  new_file_size ) { this->new_file_size = new_file_size; }
  
        bool operator==( const OSDWriteResponse& other ) const { return new_file_size == other.new_file_size; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( OSDWriteResponse, 2010030331 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "new_file_size", 0, new_file_size ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "new_file_size", 0, new_file_size ); }
  
      protected:
        org::xtreemfs::interfaces::NewFileSizeSet new_file_size;
      };
  
      class StripingPolicy : public ::yidl::runtime::Struct
      {
      public:
        StripingPolicy() : type( STRIPING_POLICY_RAID0 ), stripe_size( 0 ), width( 0 ) { }
        StripingPolicy( org::xtreemfs::interfaces::StripingPolicyType type, uint32_t stripe_size, uint32_t width ) : type( type ), stripe_size( stripe_size ), width( width ) { }
        virtual ~StripingPolicy() {  }
  
        org::xtreemfs::interfaces::StripingPolicyType get_type() const { return type; }
        uint32_t get_stripe_size() const { return stripe_size; }
        uint32_t get_width() const { return width; }
        void set_type( org::xtreemfs::interfaces::StripingPolicyType type ) { this->type = type; }
        void set_stripe_size( uint32_t stripe_size ) { this->stripe_size = stripe_size; }
        void set_width( uint32_t width ) { this->width = width; }
  
        bool operator==( const StripingPolicy& other ) const { return type == other.type && stripe_size == other.stripe_size && width == other.width; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StripingPolicy, 2010030333 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "type", 0, static_cast<int32_t>( type ) ); marshaller.write( "stripe_size", 0, stripe_size ); marshaller.write( "width", 0, width ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { type = static_cast<org::xtreemfs::interfaces::StripingPolicyType>( unmarshaller.read_int32( "type", 0 ) ); stripe_size = unmarshaller.read_uint32( "stripe_size", 0 ); width = unmarshaller.read_uint32( "width", 0 ); }
  
      protected:
        org::xtreemfs::interfaces::StripingPolicyType type;
        uint32_t stripe_size;
        uint32_t width;
      };
  
      class Replica : public ::yidl::runtime::Struct
      {
      public:
        Replica() : replication_flags( 0 ) { }
        Replica( const org::xtreemfs::interfaces::StringSet& osd_uuids, uint32_t replication_flags, const org::xtreemfs::interfaces::StripingPolicy& striping_policy ) : osd_uuids( osd_uuids ), replication_flags( replication_flags ), striping_policy( striping_policy ) { }
        virtual ~Replica() {  }
  
        const org::xtreemfs::interfaces::StringSet& get_osd_uuids() const { return osd_uuids; }
        uint32_t get_replication_flags() const { return replication_flags; }
        const org::xtreemfs::interfaces::StripingPolicy& get_striping_policy() const { return striping_policy; }
        void set_osd_uuids( const org::xtreemfs::interfaces::StringSet&  osd_uuids ) { this->osd_uuids = osd_uuids; }
        void set_replication_flags( uint32_t replication_flags ) { this->replication_flags = replication_flags; }
        void set_striping_policy( const org::xtreemfs::interfaces::StripingPolicy&  striping_policy ) { this->striping_policy = striping_policy; }
  
        bool operator==( const Replica& other ) const { return osd_uuids == other.osd_uuids && replication_flags == other.replication_flags && striping_policy == other.striping_policy; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Replica, 2010030334 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "osd_uuids", 0, osd_uuids ); marshaller.write( "replication_flags", 0, replication_flags ); marshaller.write( "striping_policy", 0, striping_policy ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "osd_uuids", 0, osd_uuids ); replication_flags = unmarshaller.read_uint32( "replication_flags", 0 ); unmarshaller.read( "striping_policy", 0, striping_policy ); }
  
      protected:
        org::xtreemfs::interfaces::StringSet osd_uuids;
        uint32_t replication_flags;
        org::xtreemfs::interfaces::StripingPolicy striping_policy;
      };
  
      class ReplicaSet
        : public ::yidl::runtime::Sequence,
          public std::vector<org::xtreemfs::interfaces::Replica>
      {
      public:
        ReplicaSet() { }
        ReplicaSet( const org::xtreemfs::interfaces::Replica& first_value ) { std::vector<org::xtreemfs::interfaces::Replica>::push_back( first_value ); }
        ReplicaSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Replica>( size ) { }
        virtual ~ReplicaSet() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ReplicaSet, 2010030335 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          size_type value_i_max = size();
          for ( size_type value_i = 0; value_i < value_i_max; value_i++ )
          {
            marshaller.write( "value", 0, ( *this )[value_i] );
          }
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          org::xtreemfs::interfaces::Replica value;
          unmarshaller.read( "value", 0, value );
          push_back( value );
        }
  
        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };
  
      class VivaldiCoordinates : public ::yidl::runtime::Struct
      {
      public:
        VivaldiCoordinates() : x_coordinate( 0 ), y_coordinate( 0 ), local_error( 0 ) { }
        VivaldiCoordinates( double x_coordinate, double y_coordinate, double local_error ) : x_coordinate( x_coordinate ), y_coordinate( y_coordinate ), local_error( local_error ) { }
        virtual ~VivaldiCoordinates() {  }
  
        double get_x_coordinate() const { return x_coordinate; }
        double get_y_coordinate() const { return y_coordinate; }
        double get_local_error() const { return local_error; }
        void set_x_coordinate( double x_coordinate ) { this->x_coordinate = x_coordinate; }
        void set_y_coordinate( double y_coordinate ) { this->y_coordinate = y_coordinate; }
        void set_local_error( double local_error ) { this->local_error = local_error; }
  
        bool operator==( const VivaldiCoordinates& other ) const { return x_coordinate == other.x_coordinate && y_coordinate == other.y_coordinate && local_error == other.local_error; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( VivaldiCoordinates, 2010030336 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "x_coordinate", 0, x_coordinate ); marshaller.write( "y_coordinate", 0, y_coordinate ); marshaller.write( "local_error", 0, local_error ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { x_coordinate = unmarshaller.read_double( "x_coordinate", 0 ); y_coordinate = unmarshaller.read_double( "y_coordinate", 0 ); local_error = unmarshaller.read_double( "local_error", 0 ); }
  
      protected:
        double x_coordinate;
        double y_coordinate;
        double local_error;
      };
  
      class XCap : public ::yidl::runtime::Struct
      {
      public:
        XCap() : access_mode( 0 ), expire_time_s( 0 ), expire_timeout_s( 0 ), replicate_on_close( false ), truncate_epoch( 0 ), snap_config( SNAP_CONFIG_SNAPS_DISABLED ), snap_timestamp( 0 ) { }
        XCap( uint32_t access_mode, const std::string& client_identity, uint64_t expire_time_s, uint32_t expire_timeout_s, const std::string& file_id, bool replicate_on_close, const std::string& server_signature, uint32_t truncate_epoch, org::xtreemfs::interfaces::SnapConfig snap_config, uint64_t snap_timestamp ) : access_mode( access_mode ), client_identity( client_identity ), expire_time_s( expire_time_s ), expire_timeout_s( expire_timeout_s ), file_id( file_id ), replicate_on_close( replicate_on_close ), server_signature( server_signature ), truncate_epoch( truncate_epoch ), snap_config( snap_config ), snap_timestamp( snap_timestamp ) { }
        virtual ~XCap() {  }
  
        uint32_t get_access_mode() const { return access_mode; }
        const std::string& get_client_identity() const { return client_identity; }
        uint64_t get_expire_time_s() const { return expire_time_s; }
        uint32_t get_expire_timeout_s() const { return expire_timeout_s; }
        const std::string& get_file_id() const { return file_id; }
        bool get_replicate_on_close() const { return replicate_on_close; }
        const std::string& get_server_signature() const { return server_signature; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        org::xtreemfs::interfaces::SnapConfig get_snap_config() const { return snap_config; }
        uint64_t get_snap_timestamp() const { return snap_timestamp; }
        void set_access_mode( uint32_t access_mode ) { this->access_mode = access_mode; }
        void set_client_identity( const std::string& client_identity ) { this->client_identity = client_identity; }
        void set_expire_time_s( uint64_t expire_time_s ) { this->expire_time_s = expire_time_s; }
        void set_expire_timeout_s( uint32_t expire_timeout_s ) { this->expire_timeout_s = expire_timeout_s; }
        void set_file_id( const std::string& file_id ) { this->file_id = file_id; }
        void set_replicate_on_close( bool replicate_on_close ) { this->replicate_on_close = replicate_on_close; }
        void set_server_signature( const std::string& server_signature ) { this->server_signature = server_signature; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        void set_snap_config( org::xtreemfs::interfaces::SnapConfig snap_config ) { this->snap_config = snap_config; }
        void set_snap_timestamp( uint64_t snap_timestamp ) { this->snap_timestamp = snap_timestamp; }
  
        bool operator==( const XCap& other ) const { return access_mode == other.access_mode && client_identity == other.client_identity && expire_time_s == other.expire_time_s && expire_timeout_s == other.expire_timeout_s && file_id == other.file_id && replicate_on_close == other.replicate_on_close && server_signature == other.server_signature && truncate_epoch == other.truncate_epoch && snap_config == other.snap_config && snap_timestamp == other.snap_timestamp; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( XCap, 2010030337 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "access_mode", 0, access_mode ); marshaller.write( "client_identity", 0, client_identity ); marshaller.write( "expire_time_s", 0, expire_time_s ); marshaller.write( "expire_timeout_s", 0, expire_timeout_s ); marshaller.write( "file_id", 0, file_id ); marshaller.write( "replicate_on_close", 0, replicate_on_close ); marshaller.write( "server_signature", 0, server_signature ); marshaller.write( "truncate_epoch", 0, truncate_epoch ); marshaller.write( "snap_config", 0, static_cast<int32_t>( snap_config ) ); marshaller.write( "snap_timestamp", 0, snap_timestamp ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { access_mode = unmarshaller.read_uint32( "access_mode", 0 ); unmarshaller.read( "client_identity", 0, client_identity ); expire_time_s = unmarshaller.read_uint64( "expire_time_s", 0 ); expire_timeout_s = unmarshaller.read_uint32( "expire_timeout_s", 0 ); unmarshaller.read( "file_id", 0, file_id ); replicate_on_close = unmarshaller.read_bool( "replicate_on_close", 0 ); unmarshaller.read( "server_signature", 0, server_signature ); truncate_epoch = unmarshaller.read_uint32( "truncate_epoch", 0 ); snap_config = static_cast<org::xtreemfs::interfaces::SnapConfig>( unmarshaller.read_int32( "snap_config", 0 ) ); snap_timestamp = unmarshaller.read_uint64( "snap_timestamp", 0 ); }
  
      protected:
        uint32_t access_mode;
        std::string client_identity;
        uint64_t expire_time_s;
        uint32_t expire_timeout_s;
        std::string file_id;
        bool replicate_on_close;
        std::string server_signature;
        uint32_t truncate_epoch;
        org::xtreemfs::interfaces::SnapConfig snap_config;
        uint64_t snap_timestamp;
      };
  
      class XLocSet : public ::yidl::runtime::Struct
      {
      public:
        XLocSet() : read_only_file_size( 0 ), version( 0 ) { }
        XLocSet( uint64_t read_only_file_size, const org::xtreemfs::interfaces::ReplicaSet& replicas, const std::string& replica_update_policy, uint32_t version ) : read_only_file_size( read_only_file_size ), replicas( replicas ), replica_update_policy( replica_update_policy ), version( version ) { }
        virtual ~XLocSet() {  }
  
        uint64_t get_read_only_file_size() const { return read_only_file_size; }
        const org::xtreemfs::interfaces::ReplicaSet& get_replicas() const { return replicas; }
        const std::string& get_replica_update_policy() const { return replica_update_policy; }
        uint32_t get_version() const { return version; }
        void set_read_only_file_size( uint64_t read_only_file_size ) { this->read_only_file_size = read_only_file_size; }
        void set_replicas( const org::xtreemfs::interfaces::ReplicaSet&  replicas ) { this->replicas = replicas; }
        void set_replica_update_policy( const std::string& replica_update_policy ) { this->replica_update_policy = replica_update_policy; }
        void set_version( uint32_t version ) { this->version = version; }
  
        bool operator==( const XLocSet& other ) const { return read_only_file_size == other.read_only_file_size && replicas == other.replicas && replica_update_policy == other.replica_update_policy && version == other.version; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( XLocSet, 2010030338 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "read_only_file_size", 0, read_only_file_size ); marshaller.write( "replicas", 0, replicas ); marshaller.write( "replica_update_policy", 0, replica_update_policy ); marshaller.write( "version", 0, version ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { read_only_file_size = unmarshaller.read_uint64( "read_only_file_size", 0 ); unmarshaller.read( "replicas", 0, replicas ); unmarshaller.read( "replica_update_policy", 0, replica_update_policy ); version = unmarshaller.read_uint32( "version", 0 ); }
  
      protected:
        uint64_t read_only_file_size;
        org::xtreemfs::interfaces::ReplicaSet replicas;
        std::string replica_update_policy;
        uint32_t version;
      };
  
      class FileCredentials : public ::yidl::runtime::Struct
      {
      public:
        FileCredentials() { }
        FileCredentials( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::XLocSet& xlocs ) : xcap( xcap ), xlocs( xlocs ) { }
        virtual ~FileCredentials() {  }
  
        const org::xtreemfs::interfaces::XCap& get_xcap() const { return xcap; }
        const org::xtreemfs::interfaces::XLocSet& get_xlocs() const { return xlocs; }
        void set_xcap( const org::xtreemfs::interfaces::XCap&  xcap ) { this->xcap = xcap; }
        void set_xlocs( const org::xtreemfs::interfaces::XLocSet&  xlocs ) { this->xlocs = xlocs; }
  
        bool operator==( const FileCredentials& other ) const { return xcap == other.xcap && xlocs == other.xlocs; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( FileCredentials, 2010030339 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "xcap", 0, xcap ); marshaller.write( "xlocs", 0, xlocs ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "xcap", 0, xcap ); unmarshaller.read( "xlocs", 0, xlocs ); }
  
      protected:
        org::xtreemfs::interfaces::XCap xcap;
        org::xtreemfs::interfaces::XLocSet xlocs;
      };
  
      class FileCredentialsSet
        : public ::yidl::runtime::Sequence,
          public std::vector<org::xtreemfs::interfaces::FileCredentials>
      {
      public:
        FileCredentialsSet() { }
        FileCredentialsSet( const org::xtreemfs::interfaces::FileCredentials& first_value ) { std::vector<org::xtreemfs::interfaces::FileCredentials>::push_back( first_value ); }
        FileCredentialsSet( size_type size ) : std::vector<org::xtreemfs::interfaces::FileCredentials>( size ) { }
        virtual ~FileCredentialsSet() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( FileCredentialsSet, 2010030340 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const
        {
          size_type value_i_max = size();
          for ( size_type value_i = 0; value_i < value_i_max; value_i++ )
          {
            marshaller.write( "value", 0, ( *this )[value_i] );
          }
        }
  
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
        {
          org::xtreemfs::interfaces::FileCredentials value;
          unmarshaller.read( "value", 0, value );
          push_back( value );
        }
  
        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };
  
      };
    };
  };
#endif
