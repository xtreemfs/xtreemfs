// Copyright 2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

    #ifndef _XTREEMFS_INTERFACES_MRC_OSD_TYPES_H_
    #define _XTREEMFS_INTERFACES_MRC_OSD_TYPES_H_

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
      enum OSDSelectionPolicyType { OSD_SELECTION_POLICY_FILTER_DEFAULT = 1000, OSD_SELECTION_POLICY_FILTER_FQDN = 1001, OSD_SELECTION_POLICY_GROUP_DCMAP = 2000, OSD_SELECTION_POLICY_GROUP_FQDN = 2001, OSD_SELECTION_POLICY_SORT_DCMAP = 3000, OSD_SELECTION_POLICY_SORT_FQDN = 3001, OSD_SELECTION_POLICY_SORT_RANDOM = 3002, OSD_SELECTION_POLICY_SORT_VIVALDI = 3003 };
      enum ReplicaSelectionPolicyType { REPLICA_SELECTION_POLICY_SIMPLE = 1 };
      enum StripingPolicyType { STRIPING_POLICY_RAID0 = 0 };


      class NewFileSize : public ::yidl::runtime::Struct
      {
      public:
        NewFileSize() : size_in_bytes( 0 ), truncate_epoch( 0 ) { }
        NewFileSize( uint64_t size_in_bytes, uint32_t truncate_epoch ) : size_in_bytes( size_in_bytes ), truncate_epoch( truncate_epoch ) { }
        virtual ~NewFileSize() { }

        void set_size_in_bytes( uint64_t size_in_bytes ) { this->size_in_bytes = size_in_bytes; }
        uint64_t get_size_in_bytes() const { return size_in_bytes; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }

        bool operator==( const NewFileSize& other ) const { return size_in_bytes == other.size_in_bytes && truncate_epoch == other.truncate_epoch; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( NewFileSize, 2009120921 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint64( "size_in_bytes", 0, size_in_bytes ); marshaller.writeUint32( "truncate_epoch", 0, truncate_epoch ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { size_in_bytes = unmarshaller.readUint64( "size_in_bytes", 0 ); truncate_epoch = unmarshaller.readUint32( "truncate_epoch", 0 ); }

      protected:
        uint64_t size_in_bytes;
        uint32_t truncate_epoch;
      };

      class NewFileSizeSet : public ::yidl::runtime::Sequence, public std::vector<org::xtreemfs::interfaces::NewFileSize>
      {
      public:
        NewFileSizeSet() { }
        NewFileSizeSet( const org::xtreemfs::interfaces::NewFileSize& first_value ) { std::vector<org::xtreemfs::interfaces::NewFileSize>::push_back( first_value ); }
        NewFileSizeSet( size_type size ) : std::vector<org::xtreemfs::interfaces::NewFileSize>( size ) { }
        virtual ~NewFileSizeSet() { }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( NewFileSizeSet, 2009120922 );

        // yidl::Sequence
        size_t get_size() const { return size(); }
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( "value", 0, ( *this )[value_i] ); } }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::NewFileSize value; unmarshaller.readStruct( "value", 0, value ); push_back( value ); }
      };

      class OSDtoMRCData : public ::yidl::runtime::Struct
      {
      public:
        OSDtoMRCData() : caching_policy( 0 ) { }
        OSDtoMRCData( uint8_t caching_policy, const std::string& data ) : caching_policy( caching_policy ), data( data ) { }
        OSDtoMRCData( uint8_t caching_policy, const char* data, size_t data_len ) : caching_policy( caching_policy ), data( data, data_len ) { }
        virtual ~OSDtoMRCData() { }

        void set_caching_policy( uint8_t caching_policy ) { this->caching_policy = caching_policy; }
        uint8_t get_caching_policy() const { return caching_policy; }
        void set_data( const std::string& data ) { set_data( data.c_str(), data.size() ); }
        void set_data( const char* data, size_t data_len ) { this->data.assign( data, data_len ); }
        const std::string& get_data() const { return data; }

        bool operator==( const OSDtoMRCData& other ) const { return caching_policy == other.caching_policy && data == other.data; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( OSDtoMRCData, 2009120923 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint8( "caching_policy", 0, caching_policy ); marshaller.writeString( "data", 0, data ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { caching_policy = unmarshaller.readUint8( "caching_policy", 0 ); unmarshaller.readString( "data", 0, data ); }

      protected:
        uint8_t caching_policy;
        std::string data;
      };

      class OSDtoMRCDataSet : public ::yidl::runtime::Sequence, public std::vector<org::xtreemfs::interfaces::OSDtoMRCData>
      {
      public:
        OSDtoMRCDataSet() { }
        OSDtoMRCDataSet( const org::xtreemfs::interfaces::OSDtoMRCData& first_value ) { std::vector<org::xtreemfs::interfaces::OSDtoMRCData>::push_back( first_value ); }
        OSDtoMRCDataSet( size_type size ) : std::vector<org::xtreemfs::interfaces::OSDtoMRCData>( size ) { }
        virtual ~OSDtoMRCDataSet() { }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( OSDtoMRCDataSet, 2009120924 );

        // yidl::Sequence
        size_t get_size() const { return size(); }
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( "value", 0, ( *this )[value_i] ); } }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::OSDtoMRCData value; unmarshaller.readStruct( "value", 0, value ); push_back( value ); }
      };

      class OSDWriteResponse : public ::yidl::runtime::Struct
      {
      public:
        OSDWriteResponse() { }
        OSDWriteResponse( const org::xtreemfs::interfaces::NewFileSizeSet& new_file_size, const org::xtreemfs::interfaces::OSDtoMRCDataSet& opaque_data ) : new_file_size( new_file_size ), opaque_data( opaque_data ) { }
        virtual ~OSDWriteResponse() { }

        void set_new_file_size( const org::xtreemfs::interfaces::NewFileSizeSet&  new_file_size ) { this->new_file_size = new_file_size; }
        const org::xtreemfs::interfaces::NewFileSizeSet& get_new_file_size() const { return new_file_size; }
        void set_opaque_data( const org::xtreemfs::interfaces::OSDtoMRCDataSet&  opaque_data ) { this->opaque_data = opaque_data; }
        const org::xtreemfs::interfaces::OSDtoMRCDataSet& get_opaque_data() const { return opaque_data; }

        bool operator==( const OSDWriteResponse& other ) const { return new_file_size == other.new_file_size && opaque_data == other.opaque_data; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( OSDWriteResponse, 2009120927 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "new_file_size", 0, new_file_size ); marshaller.writeSequence( "opaque_data", 0, opaque_data ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "new_file_size", 0, new_file_size ); unmarshaller.readSequence( "opaque_data", 0, opaque_data ); }

      protected:
        org::xtreemfs::interfaces::NewFileSizeSet new_file_size;
        org::xtreemfs::interfaces::OSDtoMRCDataSet opaque_data;
      };

      class StripingPolicy : public ::yidl::runtime::Struct
      {
      public:
        StripingPolicy() : type( STRIPING_POLICY_RAID0 ), stripe_size( 0 ), width( 0 ) { }
        StripingPolicy( org::xtreemfs::interfaces::StripingPolicyType type, uint32_t stripe_size, uint32_t width ) : type( type ), stripe_size( stripe_size ), width( width ) { }
        virtual ~StripingPolicy() { }

        void set_type( org::xtreemfs::interfaces::StripingPolicyType type ) { this->type = type; }
        org::xtreemfs::interfaces::StripingPolicyType get_type() const { return type; }
        void set_stripe_size( uint32_t stripe_size ) { this->stripe_size = stripe_size; }
        uint32_t get_stripe_size() const { return stripe_size; }
        void set_width( uint32_t width ) { this->width = width; }
        uint32_t get_width() const { return width; }

        bool operator==( const StripingPolicy& other ) const { return type == other.type && stripe_size == other.stripe_size && width == other.width; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( StripingPolicy, 2009120929 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeInt32( "type", 0, static_cast<int32_t>( type ) ); marshaller.writeUint32( "stripe_size", 0, stripe_size ); marshaller.writeUint32( "width", 0, width ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { type = static_cast<org::xtreemfs::interfaces::StripingPolicyType>( unmarshaller.readInt32( "type", 0 ) ); stripe_size = unmarshaller.readUint32( "stripe_size", 0 ); width = unmarshaller.readUint32( "width", 0 ); }

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
        virtual ~Replica() { }

        void set_osd_uuids( const org::xtreemfs::interfaces::StringSet&  osd_uuids ) { this->osd_uuids = osd_uuids; }
        const org::xtreemfs::interfaces::StringSet& get_osd_uuids() const { return osd_uuids; }
        void set_replication_flags( uint32_t replication_flags ) { this->replication_flags = replication_flags; }
        uint32_t get_replication_flags() const { return replication_flags; }
        void set_striping_policy( const org::xtreemfs::interfaces::StripingPolicy&  striping_policy ) { this->striping_policy = striping_policy; }
        const org::xtreemfs::interfaces::StripingPolicy& get_striping_policy() const { return striping_policy; }

        bool operator==( const Replica& other ) const { return osd_uuids == other.osd_uuids && replication_flags == other.replication_flags && striping_policy == other.striping_policy; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( Replica, 2009120930 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "osd_uuids", 0, osd_uuids ); marshaller.writeUint32( "replication_flags", 0, replication_flags ); marshaller.writeStruct( "striping_policy", 0, striping_policy ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "osd_uuids", 0, osd_uuids ); replication_flags = unmarshaller.readUint32( "replication_flags", 0 ); unmarshaller.readStruct( "striping_policy", 0, striping_policy ); }

      protected:
        org::xtreemfs::interfaces::StringSet osd_uuids;
        uint32_t replication_flags;
        org::xtreemfs::interfaces::StripingPolicy striping_policy;
      };

      class ReplicaSet : public ::yidl::runtime::Sequence, public std::vector<org::xtreemfs::interfaces::Replica>
      {
      public:
        ReplicaSet() { }
        ReplicaSet( const org::xtreemfs::interfaces::Replica& first_value ) { std::vector<org::xtreemfs::interfaces::Replica>::push_back( first_value ); }
        ReplicaSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Replica>( size ) { }
        virtual ~ReplicaSet() { }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( ReplicaSet, 2009120931 );

        // yidl::Sequence
        size_t get_size() const { return size(); }
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( "value", 0, ( *this )[value_i] ); } }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::Replica value; unmarshaller.readStruct( "value", 0, value ); push_back( value ); }
      };

      class VivaldiCoordinates : public ::yidl::runtime::Struct
      {
      public:
        VivaldiCoordinates() : x_coordinate( 0 ), y_coordinate( 0 ), local_error( 0 ) { }
        VivaldiCoordinates( double x_coordinate, double y_coordinate, double local_error ) : x_coordinate( x_coordinate ), y_coordinate( y_coordinate ), local_error( local_error ) { }
        virtual ~VivaldiCoordinates() { }

        void set_x_coordinate( double x_coordinate ) { this->x_coordinate = x_coordinate; }
        double get_x_coordinate() const { return x_coordinate; }
        void set_y_coordinate( double y_coordinate ) { this->y_coordinate = y_coordinate; }
        double get_y_coordinate() const { return y_coordinate; }
        void set_local_error( double local_error ) { this->local_error = local_error; }
        double get_local_error() const { return local_error; }

        bool operator==( const VivaldiCoordinates& other ) const { return x_coordinate == other.x_coordinate && y_coordinate == other.y_coordinate && local_error == other.local_error; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( VivaldiCoordinates, 2009120932 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeDouble( "x_coordinate", 0, x_coordinate ); marshaller.writeDouble( "y_coordinate", 0, y_coordinate ); marshaller.writeDouble( "local_error", 0, local_error ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { x_coordinate = unmarshaller.readDouble( "x_coordinate", 0 ); y_coordinate = unmarshaller.readDouble( "y_coordinate", 0 ); local_error = unmarshaller.readDouble( "local_error", 0 ); }

      protected:
        double x_coordinate;
        double y_coordinate;
        double local_error;
      };

      class XCap : public ::yidl::runtime::Struct
      {
      public:
        XCap() : access_mode( 0 ), expire_time_s( 0 ), expire_timeout_s( 0 ), replicate_on_close( false ), truncate_epoch( 0 ) { }
        XCap( uint32_t access_mode, const std::string& client_identity, uint64_t expire_time_s, uint32_t expire_timeout_s, const std::string& file_id, bool replicate_on_close, const std::string& server_signature, uint32_t truncate_epoch ) : access_mode( access_mode ), client_identity( client_identity ), expire_time_s( expire_time_s ), expire_timeout_s( expire_timeout_s ), file_id( file_id ), replicate_on_close( replicate_on_close ), server_signature( server_signature ), truncate_epoch( truncate_epoch ) { }
        XCap( uint32_t access_mode, const char* client_identity, size_t client_identity_len, uint64_t expire_time_s, uint32_t expire_timeout_s, const char* file_id, size_t file_id_len, bool replicate_on_close, const char* server_signature, size_t server_signature_len, uint32_t truncate_epoch ) : access_mode( access_mode ), client_identity( client_identity, client_identity_len ), expire_time_s( expire_time_s ), expire_timeout_s( expire_timeout_s ), file_id( file_id, file_id_len ), replicate_on_close( replicate_on_close ), server_signature( server_signature, server_signature_len ), truncate_epoch( truncate_epoch ) { }
        virtual ~XCap() { }

        void set_access_mode( uint32_t access_mode ) { this->access_mode = access_mode; }
        uint32_t get_access_mode() const { return access_mode; }
        void set_client_identity( const std::string& client_identity ) { set_client_identity( client_identity.c_str(), client_identity.size() ); }
        void set_client_identity( const char* client_identity, size_t client_identity_len ) { this->client_identity.assign( client_identity, client_identity_len ); }
        const std::string& get_client_identity() const { return client_identity; }
        void set_expire_time_s( uint64_t expire_time_s ) { this->expire_time_s = expire_time_s; }
        uint64_t get_expire_time_s() const { return expire_time_s; }
        void set_expire_timeout_s( uint32_t expire_timeout_s ) { this->expire_timeout_s = expire_timeout_s; }
        uint32_t get_expire_timeout_s() const { return expire_timeout_s; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
        const std::string& get_file_id() const { return file_id; }
        void set_replicate_on_close( bool replicate_on_close ) { this->replicate_on_close = replicate_on_close; }
        bool get_replicate_on_close() const { return replicate_on_close; }
        void set_server_signature( const std::string& server_signature ) { set_server_signature( server_signature.c_str(), server_signature.size() ); }
        void set_server_signature( const char* server_signature, size_t server_signature_len ) { this->server_signature.assign( server_signature, server_signature_len ); }
        const std::string& get_server_signature() const { return server_signature; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }

        bool operator==( const XCap& other ) const { return access_mode == other.access_mode && client_identity == other.client_identity && expire_time_s == other.expire_time_s && expire_timeout_s == other.expire_timeout_s && file_id == other.file_id && replicate_on_close == other.replicate_on_close && server_signature == other.server_signature && truncate_epoch == other.truncate_epoch; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( XCap, 2009120933 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint32( "access_mode", 0, access_mode ); marshaller.writeString( "client_identity", 0, client_identity ); marshaller.writeUint64( "expire_time_s", 0, expire_time_s ); marshaller.writeUint32( "expire_timeout_s", 0, expire_timeout_s ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeBoolean( "replicate_on_close", 0, replicate_on_close ); marshaller.writeString( "server_signature", 0, server_signature ); marshaller.writeUint32( "truncate_epoch", 0, truncate_epoch ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { access_mode = unmarshaller.readUint32( "access_mode", 0 ); unmarshaller.readString( "client_identity", 0, client_identity ); expire_time_s = unmarshaller.readUint64( "expire_time_s", 0 ); expire_timeout_s = unmarshaller.readUint32( "expire_timeout_s", 0 ); unmarshaller.readString( "file_id", 0, file_id ); replicate_on_close = unmarshaller.readBoolean( "replicate_on_close", 0 ); unmarshaller.readString( "server_signature", 0, server_signature ); truncate_epoch = unmarshaller.readUint32( "truncate_epoch", 0 ); }

      protected:
        uint32_t access_mode;
        std::string client_identity;
        uint64_t expire_time_s;
        uint32_t expire_timeout_s;
        std::string file_id;
        bool replicate_on_close;
        std::string server_signature;
        uint32_t truncate_epoch;
      };

      class XLocSet : public ::yidl::runtime::Struct
      {
      public:
        XLocSet() : read_only_file_size( 0 ), version( 0 ) { }
        XLocSet( uint64_t read_only_file_size, const org::xtreemfs::interfaces::ReplicaSet& replicas, const std::string& replica_update_policy, uint32_t version ) : read_only_file_size( read_only_file_size ), replicas( replicas ), replica_update_policy( replica_update_policy ), version( version ) { }
        XLocSet( uint64_t read_only_file_size, const org::xtreemfs::interfaces::ReplicaSet& replicas, const char* replica_update_policy, size_t replica_update_policy_len, uint32_t version ) : read_only_file_size( read_only_file_size ), replicas( replicas ), replica_update_policy( replica_update_policy, replica_update_policy_len ), version( version ) { }
        virtual ~XLocSet() { }

        void set_read_only_file_size( uint64_t read_only_file_size ) { this->read_only_file_size = read_only_file_size; }
        uint64_t get_read_only_file_size() const { return read_only_file_size; }
        void set_replicas( const org::xtreemfs::interfaces::ReplicaSet&  replicas ) { this->replicas = replicas; }
        const org::xtreemfs::interfaces::ReplicaSet& get_replicas() const { return replicas; }
        void set_replica_update_policy( const std::string& replica_update_policy ) { set_replica_update_policy( replica_update_policy.c_str(), replica_update_policy.size() ); }
        void set_replica_update_policy( const char* replica_update_policy, size_t replica_update_policy_len ) { this->replica_update_policy.assign( replica_update_policy, replica_update_policy_len ); }
        const std::string& get_replica_update_policy() const { return replica_update_policy; }
        void set_version( uint32_t version ) { this->version = version; }
        uint32_t get_version() const { return version; }

        bool operator==( const XLocSet& other ) const { return read_only_file_size == other.read_only_file_size && replicas == other.replicas && replica_update_policy == other.replica_update_policy && version == other.version; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( XLocSet, 2009120934 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint64( "read_only_file_size", 0, read_only_file_size ); marshaller.writeSequence( "replicas", 0, replicas ); marshaller.writeString( "replica_update_policy", 0, replica_update_policy ); marshaller.writeUint32( "version", 0, version ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { read_only_file_size = unmarshaller.readUint64( "read_only_file_size", 0 ); unmarshaller.readSequence( "replicas", 0, replicas ); unmarshaller.readString( "replica_update_policy", 0, replica_update_policy ); version = unmarshaller.readUint32( "version", 0 ); }

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
        virtual ~FileCredentials() { }

        void set_xcap( const org::xtreemfs::interfaces::XCap&  xcap ) { this->xcap = xcap; }
        const org::xtreemfs::interfaces::XCap& get_xcap() const { return xcap; }
        void set_xlocs( const org::xtreemfs::interfaces::XLocSet&  xlocs ) { this->xlocs = xlocs; }
        const org::xtreemfs::interfaces::XLocSet& get_xlocs() const { return xlocs; }

        bool operator==( const FileCredentials& other ) const { return xcap == other.xcap && xlocs == other.xlocs; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( FileCredentials, 2009120935 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "xcap", 0, xcap ); marshaller.writeStruct( "xlocs", 0, xlocs ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "xcap", 0, xcap ); unmarshaller.readStruct( "xlocs", 0, xlocs ); }

      protected:
        org::xtreemfs::interfaces::XCap xcap;
        org::xtreemfs::interfaces::XLocSet xlocs;
      };

      class FileCredentialsSet : public ::yidl::runtime::Sequence, public std::vector<org::xtreemfs::interfaces::FileCredentials>
      {
      public:
        FileCredentialsSet() { }
        FileCredentialsSet( const org::xtreemfs::interfaces::FileCredentials& first_value ) { std::vector<org::xtreemfs::interfaces::FileCredentials>::push_back( first_value ); }
        FileCredentialsSet( size_type size ) : std::vector<org::xtreemfs::interfaces::FileCredentials>( size ) { }
        virtual ~FileCredentialsSet() { }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( FileCredentialsSet, 2009120936 );

        // yidl::Sequence
        size_t get_size() const { return size(); }
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( "value", 0, ( *this )[value_i] ); } }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::FileCredentials value; unmarshaller.readStruct( "value", 0, value ); push_back( value ); }
      };



    };



  };



};
#endif
