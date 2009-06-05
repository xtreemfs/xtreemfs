// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_INTERFACES_MRC_OSD_TYPES_H_
#define _ORG_XTREEMFS_INTERFACES_MRC_OSD_TYPES_H_

#include "types.h"
#include "yield/platform.h"
#include <vector>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      enum AccessControlPolicyType { ACCESS_CONTROL_POLICY_NULL = 1, ACCESS_CONTROL_POLICY_POSIX = 2, ACCESS_CONTROL_POLICY_VOLUME = 3 };
      enum OSDSelectionPolicyType { OSD_SELECTION_POLICY_SIMPLE = 1, OSD_SELECTION_POLICY_PROXIMITY = 2, OSD_SELECTION_POLICY_DNS = 3 };
      enum ReplicaSelectionPolicyType { REPLICA_SELECTION_POLICY_SIMPLE = 1 };
      enum StripingPolicyType { STRIPING_POLICY_RAID0 = 0 };


      class NewFileSize : public YIELD::Object
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

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( NewFileSize, 1011 );

        // YIELD::Object
        void marshal( YIELD::Marshaller& marshaller ) { marshaller.writeUint64( YIELD::Marshaller::Declaration( "size_in_bytes" ), size_in_bytes ); marshaller.writeUint32( YIELD::Marshaller::Declaration( "truncate_epoch" ), truncate_epoch ); }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { size_in_bytes = unmarshaller.readUint64( YIELD::Unmarshaller::Declaration( "size_in_bytes" ) ); truncate_epoch = unmarshaller.readUint32( YIELD::Unmarshaller::Declaration( "truncate_epoch" ) ); }

      protected:
        uint64_t size_in_bytes;
        uint32_t truncate_epoch;
      };

      class NewFileSizeSet : public std::vector<org::xtreemfs::interfaces::NewFileSize>, public YIELD::Object
      {
      public:
        NewFileSizeSet() { }
        NewFileSizeSet( const org::xtreemfs::interfaces::NewFileSize& first_value ) { std::vector<org::xtreemfs::interfaces::NewFileSize>::push_back( first_value ); }
        NewFileSizeSet( size_type size ) : std::vector<org::xtreemfs::interfaces::NewFileSize>( size ) { }
        virtual ~NewFileSizeSet() { }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( NewFileSizeSet, 1012 );
        void marshal( YIELD::Marshaller& marshaller ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( YIELD::Marshaller::Declaration( "value" ), ( *this )[value_i] ); } }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::NewFileSize value; unmarshaller.readStruct( YIELD::Unmarshaller::Declaration( "value" ), &value ); push_back( value ); }
      };

      class OSDtoMRCData : public YIELD::Object
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

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( OSDtoMRCData, 1013 );

        // YIELD::Object
        void marshal( YIELD::Marshaller& marshaller ) { marshaller.writeUint8( YIELD::Marshaller::Declaration( "caching_policy" ), caching_policy ); marshaller.writeString( YIELD::Marshaller::Declaration( "data" ), data ); }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { caching_policy = unmarshaller.readUint8( YIELD::Unmarshaller::Declaration( "caching_policy" ) ); unmarshaller.readString( YIELD::Unmarshaller::Declaration( "data" ), data ); }

      protected:
        uint8_t caching_policy;
        std::string data;
      };

      class OSDtoMRCDataSet : public std::vector<org::xtreemfs::interfaces::OSDtoMRCData>, public YIELD::Object
      {
      public:
        OSDtoMRCDataSet() { }
        OSDtoMRCDataSet( const org::xtreemfs::interfaces::OSDtoMRCData& first_value ) { std::vector<org::xtreemfs::interfaces::OSDtoMRCData>::push_back( first_value ); }
        OSDtoMRCDataSet( size_type size ) : std::vector<org::xtreemfs::interfaces::OSDtoMRCData>( size ) { }
        virtual ~OSDtoMRCDataSet() { }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( OSDtoMRCDataSet, 1014 );
        void marshal( YIELD::Marshaller& marshaller ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( YIELD::Marshaller::Declaration( "value" ), ( *this )[value_i] ); } }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::OSDtoMRCData value; unmarshaller.readStruct( YIELD::Unmarshaller::Declaration( "value" ), &value ); push_back( value ); }
      };

      class OSDWriteResponse : public YIELD::Object
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

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( OSDWriteResponse, 1017 );

        // YIELD::Object
        void marshal( YIELD::Marshaller& marshaller ) { marshaller.writeSequence( YIELD::Marshaller::Declaration( "new_file_size" ), new_file_size, new_file_size.size() ); marshaller.writeSequence( YIELD::Marshaller::Declaration( "opaque_data" ), opaque_data, opaque_data.size() ); }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( YIELD::Unmarshaller::Declaration( "new_file_size" ), &new_file_size ); unmarshaller.readSequence( YIELD::Unmarshaller::Declaration( "opaque_data" ), &opaque_data ); }

      protected:
        org::xtreemfs::interfaces::NewFileSizeSet new_file_size;
        org::xtreemfs::interfaces::OSDtoMRCDataSet opaque_data;
      };

      class StripingPolicy : public YIELD::Object
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

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( StripingPolicy, 1019 );

        // YIELD::Object
        void marshal( YIELD::Marshaller& marshaller ) { marshaller.writeInt32( YIELD::Marshaller::Declaration( "type" ), type ); marshaller.writeUint32( YIELD::Marshaller::Declaration( "stripe_size" ), stripe_size ); marshaller.writeUint32( YIELD::Marshaller::Declaration( "width" ), width ); }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { type = ( org::xtreemfs::interfaces::StripingPolicyType )unmarshaller.readInt32( YIELD::Unmarshaller::Declaration( "type" ) ); stripe_size = unmarshaller.readUint32( YIELD::Unmarshaller::Declaration( "stripe_size" ) ); width = unmarshaller.readUint32( YIELD::Unmarshaller::Declaration( "width" ) ); }

      protected:
        org::xtreemfs::interfaces::StripingPolicyType type;
        uint32_t stripe_size;
        uint32_t width;
      };

      class Replica : public YIELD::Object
      {
      public:
        Replica() : replication_flags( 0 ) { }
        Replica( const org::xtreemfs::interfaces::StripingPolicy& striping_policy, uint32_t replication_flags, const org::xtreemfs::interfaces::StringSet& osd_uuids ) : striping_policy( striping_policy ), replication_flags( replication_flags ), osd_uuids( osd_uuids ) { }
        virtual ~Replica() { }

        void set_striping_policy( const org::xtreemfs::interfaces::StripingPolicy&  striping_policy ) { this->striping_policy = striping_policy; }
        const org::xtreemfs::interfaces::StripingPolicy& get_striping_policy() const { return striping_policy; }
        void set_replication_flags( uint32_t replication_flags ) { this->replication_flags = replication_flags; }
        uint32_t get_replication_flags() const { return replication_flags; }
        void set_osd_uuids( const org::xtreemfs::interfaces::StringSet&  osd_uuids ) { this->osd_uuids = osd_uuids; }
        const org::xtreemfs::interfaces::StringSet& get_osd_uuids() const { return osd_uuids; }

        bool operator==( const Replica& other ) const { return striping_policy == other.striping_policy && replication_flags == other.replication_flags && osd_uuids == other.osd_uuids; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( Replica, 1020 );

        // YIELD::Object
        void marshal( YIELD::Marshaller& marshaller ) { marshaller.writeStruct( YIELD::Marshaller::Declaration( "striping_policy" ), striping_policy ); marshaller.writeUint32( YIELD::Marshaller::Declaration( "replication_flags" ), replication_flags ); marshaller.writeSequence( YIELD::Marshaller::Declaration( "osd_uuids" ), osd_uuids, osd_uuids.size() ); }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( YIELD::Unmarshaller::Declaration( "striping_policy" ), &striping_policy ); replication_flags = unmarshaller.readUint32( YIELD::Unmarshaller::Declaration( "replication_flags" ) ); unmarshaller.readSequence( YIELD::Unmarshaller::Declaration( "osd_uuids" ), &osd_uuids ); }

      protected:
        org::xtreemfs::interfaces::StripingPolicy striping_policy;
        uint32_t replication_flags;
        org::xtreemfs::interfaces::StringSet osd_uuids;
      };

      class ReplicaSet : public std::vector<org::xtreemfs::interfaces::Replica>, public YIELD::Object
      {
      public:
        ReplicaSet() { }
        ReplicaSet( const org::xtreemfs::interfaces::Replica& first_value ) { std::vector<org::xtreemfs::interfaces::Replica>::push_back( first_value ); }
        ReplicaSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Replica>( size ) { }
        virtual ~ReplicaSet() { }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( ReplicaSet, 1021 );
        void marshal( YIELD::Marshaller& marshaller ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( YIELD::Marshaller::Declaration( "value" ), ( *this )[value_i] ); } }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::Replica value; unmarshaller.readStruct( YIELD::Unmarshaller::Declaration( "value" ), &value ); push_back( value ); }
      };

      class XCap : public YIELD::Object
      {
      public:
        XCap() : access_mode( 0 ), expires_s( 0 ), truncate_epoch( 0 ) { }
        XCap( const std::string& file_id, uint32_t access_mode, uint64_t expires_s, const std::string& client_identity, uint32_t truncate_epoch, const std::string& server_signature ) : file_id( file_id ), access_mode( access_mode ), expires_s( expires_s ), client_identity( client_identity ), truncate_epoch( truncate_epoch ), server_signature( server_signature ) { }
        XCap( const char* file_id, size_t file_id_len, uint32_t access_mode, uint64_t expires_s, const char* client_identity, size_t client_identity_len, uint32_t truncate_epoch, const char* server_signature, size_t server_signature_len ) : file_id( file_id, file_id_len ), access_mode( access_mode ), expires_s( expires_s ), client_identity( client_identity, client_identity_len ), truncate_epoch( truncate_epoch ), server_signature( server_signature, server_signature_len ) { }
        virtual ~XCap() { }

        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
        const std::string& get_file_id() const { return file_id; }
        void set_access_mode( uint32_t access_mode ) { this->access_mode = access_mode; }
        uint32_t get_access_mode() const { return access_mode; }
        void set_expires_s( uint64_t expires_s ) { this->expires_s = expires_s; }
        uint64_t get_expires_s() const { return expires_s; }
        void set_client_identity( const std::string& client_identity ) { set_client_identity( client_identity.c_str(), client_identity.size() ); }
        void set_client_identity( const char* client_identity, size_t client_identity_len ) { this->client_identity.assign( client_identity, client_identity_len ); }
        const std::string& get_client_identity() const { return client_identity; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        void set_server_signature( const std::string& server_signature ) { set_server_signature( server_signature.c_str(), server_signature.size() ); }
        void set_server_signature( const char* server_signature, size_t server_signature_len ) { this->server_signature.assign( server_signature, server_signature_len ); }
        const std::string& get_server_signature() const { return server_signature; }

        bool operator==( const XCap& other ) const { return file_id == other.file_id && access_mode == other.access_mode && expires_s == other.expires_s && client_identity == other.client_identity && truncate_epoch == other.truncate_epoch && server_signature == other.server_signature; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( XCap, 1022 );

        // YIELD::Object
        void marshal( YIELD::Marshaller& marshaller ) { marshaller.writeString( YIELD::Marshaller::Declaration( "file_id" ), file_id ); marshaller.writeUint32( YIELD::Marshaller::Declaration( "access_mode" ), access_mode ); marshaller.writeUint64( YIELD::Marshaller::Declaration( "expires_s" ), expires_s ); marshaller.writeString( YIELD::Marshaller::Declaration( "client_identity" ), client_identity ); marshaller.writeUint32( YIELD::Marshaller::Declaration( "truncate_epoch" ), truncate_epoch ); marshaller.writeString( YIELD::Marshaller::Declaration( "server_signature" ), server_signature ); }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { unmarshaller.readString( YIELD::Unmarshaller::Declaration( "file_id" ), file_id ); access_mode = unmarshaller.readUint32( YIELD::Unmarshaller::Declaration( "access_mode" ) ); expires_s = unmarshaller.readUint64( YIELD::Unmarshaller::Declaration( "expires_s" ) ); unmarshaller.readString( YIELD::Unmarshaller::Declaration( "client_identity" ), client_identity ); truncate_epoch = unmarshaller.readUint32( YIELD::Unmarshaller::Declaration( "truncate_epoch" ) ); unmarshaller.readString( YIELD::Unmarshaller::Declaration( "server_signature" ), server_signature ); }

      protected:
        std::string file_id;
        uint32_t access_mode;
        uint64_t expires_s;
        std::string client_identity;
        uint32_t truncate_epoch;
        std::string server_signature;
      };

      class XLocSet : public YIELD::Object
      {
      public:
        XLocSet() : version( 0 ), read_only_file_size( 0 ) { }
        XLocSet( const org::xtreemfs::interfaces::ReplicaSet& replicas, uint32_t version, const std::string& repUpdatePolicy, uint64_t read_only_file_size ) : replicas( replicas ), version( version ), repUpdatePolicy( repUpdatePolicy ), read_only_file_size( read_only_file_size ) { }
        XLocSet( const org::xtreemfs::interfaces::ReplicaSet& replicas, uint32_t version, const char* repUpdatePolicy, size_t repUpdatePolicy_len, uint64_t read_only_file_size ) : replicas( replicas ), version( version ), repUpdatePolicy( repUpdatePolicy, repUpdatePolicy_len ), read_only_file_size( read_only_file_size ) { }
        virtual ~XLocSet() { }

        void set_replicas( const org::xtreemfs::interfaces::ReplicaSet&  replicas ) { this->replicas = replicas; }
        const org::xtreemfs::interfaces::ReplicaSet& get_replicas() const { return replicas; }
        void set_version( uint32_t version ) { this->version = version; }
        uint32_t get_version() const { return version; }
        void set_repUpdatePolicy( const std::string& repUpdatePolicy ) { set_repUpdatePolicy( repUpdatePolicy.c_str(), repUpdatePolicy.size() ); }
        void set_repUpdatePolicy( const char* repUpdatePolicy, size_t repUpdatePolicy_len ) { this->repUpdatePolicy.assign( repUpdatePolicy, repUpdatePolicy_len ); }
        const std::string& get_repUpdatePolicy() const { return repUpdatePolicy; }
        void set_read_only_file_size( uint64_t read_only_file_size ) { this->read_only_file_size = read_only_file_size; }
        uint64_t get_read_only_file_size() const { return read_only_file_size; }

        bool operator==( const XLocSet& other ) const { return replicas == other.replicas && version == other.version && repUpdatePolicy == other.repUpdatePolicy && read_only_file_size == other.read_only_file_size; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( XLocSet, 1023 );

        // YIELD::Object
        void marshal( YIELD::Marshaller& marshaller ) { marshaller.writeSequence( YIELD::Marshaller::Declaration( "replicas" ), replicas, replicas.size() ); marshaller.writeUint32( YIELD::Marshaller::Declaration( "version" ), version ); marshaller.writeString( YIELD::Marshaller::Declaration( "repUpdatePolicy" ), repUpdatePolicy ); marshaller.writeUint64( YIELD::Marshaller::Declaration( "read_only_file_size" ), read_only_file_size ); }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( YIELD::Unmarshaller::Declaration( "replicas" ), &replicas ); version = unmarshaller.readUint32( YIELD::Unmarshaller::Declaration( "version" ) ); unmarshaller.readString( YIELD::Unmarshaller::Declaration( "repUpdatePolicy" ), repUpdatePolicy ); read_only_file_size = unmarshaller.readUint64( YIELD::Unmarshaller::Declaration( "read_only_file_size" ) ); }

      protected:
        org::xtreemfs::interfaces::ReplicaSet replicas;
        uint32_t version;
        std::string repUpdatePolicy;
        uint64_t read_only_file_size;
      };

      class FileCredentials : public YIELD::Object
      {
      public:
        FileCredentials() { }
        FileCredentials( const org::xtreemfs::interfaces::XLocSet& xlocs, const org::xtreemfs::interfaces::XCap& xcap ) : xlocs( xlocs ), xcap( xcap ) { }
        virtual ~FileCredentials() { }

        void set_xlocs( const org::xtreemfs::interfaces::XLocSet&  xlocs ) { this->xlocs = xlocs; }
        const org::xtreemfs::interfaces::XLocSet& get_xlocs() const { return xlocs; }
        void set_xcap( const org::xtreemfs::interfaces::XCap&  xcap ) { this->xcap = xcap; }
        const org::xtreemfs::interfaces::XCap& get_xcap() const { return xcap; }

        bool operator==( const FileCredentials& other ) const { return xlocs == other.xlocs && xcap == other.xcap; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( FileCredentials, 1024 );

        // YIELD::Object
        void marshal( YIELD::Marshaller& marshaller ) { marshaller.writeStruct( YIELD::Marshaller::Declaration( "xlocs" ), xlocs ); marshaller.writeStruct( YIELD::Marshaller::Declaration( "xcap" ), xcap ); }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( YIELD::Unmarshaller::Declaration( "xlocs" ), &xlocs ); unmarshaller.readStruct( YIELD::Unmarshaller::Declaration( "xcap" ), &xcap ); }

      protected:
        org::xtreemfs::interfaces::XLocSet xlocs;
        org::xtreemfs::interfaces::XCap xcap;
      };

      class FileCredentialsSet : public std::vector<org::xtreemfs::interfaces::FileCredentials>, public YIELD::Object
      {
      public:
        FileCredentialsSet() { }
        FileCredentialsSet( const org::xtreemfs::interfaces::FileCredentials& first_value ) { std::vector<org::xtreemfs::interfaces::FileCredentials>::push_back( first_value ); }
        FileCredentialsSet( size_type size ) : std::vector<org::xtreemfs::interfaces::FileCredentials>( size ) { }
        virtual ~FileCredentialsSet() { }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( FileCredentialsSet, 1025 );
        void marshal( YIELD::Marshaller& marshaller ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( YIELD::Marshaller::Declaration( "value" ), ( *this )[value_i] ); } }
        void unmarshal( YIELD::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::FileCredentials value; unmarshaller.readStruct( YIELD::Unmarshaller::Declaration( "value" ), &value ); push_back( value ); }
      };



    };



  };



};
#endif
