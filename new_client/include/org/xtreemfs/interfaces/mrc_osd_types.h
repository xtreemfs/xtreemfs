#ifndef _75417722947_H
#define _75417722947_H

#include <string>
#include <vector>
#include "yield/platform.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
  
      class StringSet : public std::vector<std::string>, public YIELD::Serializable
      {
      public:
        StringSet() { }
        StringSet( size_type size ) : std::vector<std::string>( size ) { }
        virtual ~StringSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::StringSet", 1366254439UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { std::string item; input_stream.readString( YIELD::StructuredStream::Declaration( "item" ), item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<std::string>::size(); }
      };
  
      class XCap : public YIELD::Serializable
      {
      public:
        XCap() : access_mode( 0 ), expires( 0 ), truncate_epoch( 0 ) { }
        XCap( const std::string& file_id, uint32_t access_mode, uint64_t expires, const std::string& client_identity, uint32_t truncate_epoch, const std::string& server_signature ) : file_id( file_id ), access_mode( access_mode ), expires( expires ), client_identity( client_identity ), truncate_epoch( truncate_epoch ), server_signature( server_signature ) { }
        XCap( const char* file_id, size_t file_id_len, uint32_t access_mode, uint64_t expires, const char* client_identity, size_t client_identity_len, uint32_t truncate_epoch, const char* server_signature, size_t server_signature_len ) : file_id( file_id, file_id_len ), access_mode( access_mode ), expires( expires ), client_identity( client_identity, client_identity_len ), truncate_epoch( truncate_epoch ), server_signature( server_signature, server_signature_len ) { }
        virtual ~XCap() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_access_mode( uint32_t access_mode ) { this->access_mode = access_mode; }
        uint32_t get_access_mode() const { return access_mode; }
        void set_expires( uint64_t expires ) { this->expires = expires; }
        uint64_t get_expires() const { return expires; }
        void set_client_identity( const std::string& client_identity ) { set_client_identity( client_identity.c_str(), client_identity.size() ); }
        void set_client_identity( const char* client_identity, size_t client_identity_len = 0 ) { this->client_identity.assign( client_identity, ( client_identity_len != 0 ) ? client_identity_len : std::strlen( client_identity ) ); }
        const std::string& get_client_identity() const { return client_identity; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        void set_server_signature( const std::string& server_signature ) { set_server_signature( server_signature.c_str(), server_signature.size() ); }
        void set_server_signature( const char* server_signature, size_t server_signature_len = 0 ) { this->server_signature.assign( server_signature, ( server_signature_len != 0 ) ? server_signature_len : std::strlen( server_signature ) ); }
        const std::string& get_server_signature() const { return server_signature; }
  
        bool operator==( const XCap& other ) const { return file_id == other.file_id && access_mode == other.access_mode && expires == other.expires && client_identity == other.client_identity && truncate_epoch == other.truncate_epoch && server_signature == other.server_signature; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::XCap", 3149302578UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); access_mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "access_mode" ) ); expires = input_stream.readUint64( YIELD::StructuredStream::Declaration( "expires" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "client_identity" ), client_identity ); truncate_epoch = input_stream.readUint32( YIELD::StructuredStream::Declaration( "truncate_epoch" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "server_signature" ), server_signature ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "access_mode" ), access_mode ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "expires" ), expires ); output_stream.writeString( YIELD::StructuredStream::Declaration( "client_identity" ), client_identity ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "truncate_epoch" ), truncate_epoch ); output_stream.writeString( YIELD::StructuredStream::Declaration( "server_signature" ), server_signature ); }
  
      protected:
        std::string file_id;
        uint32_t access_mode;
        uint64_t expires;
        std::string client_identity;
        uint32_t truncate_epoch;
        std::string server_signature;
      };
  
      class StripingPolicy : public YIELD::Serializable
      {
      public:
        StripingPolicy() : policy( 0 ), stripe_size( 0 ), width( 0 ) { }
        StripingPolicy( uint8_t policy, uint32_t stripe_size, uint32_t width ) : policy( policy ), stripe_size( stripe_size ), width( width ) { }
        virtual ~StripingPolicy() { }
  
        void set_policy( uint8_t policy ) { this->policy = policy; }
        uint8_t get_policy() const { return policy; }
        void set_stripe_size( uint32_t stripe_size ) { this->stripe_size = stripe_size; }
        uint32_t get_stripe_size() const { return stripe_size; }
        void set_width( uint32_t width ) { this->width = width; }
        uint32_t get_width() const { return width; }
  
        bool operator==( const StripingPolicy& other ) const { return policy == other.policy && stripe_size == other.stripe_size && width == other.width; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::StripingPolicy", 2678528403UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { policy = input_stream.readUint8( YIELD::StructuredStream::Declaration( "policy" ) ); stripe_size = input_stream.readUint32( YIELD::StructuredStream::Declaration( "stripe_size" ) ); width = input_stream.readUint32( YIELD::StructuredStream::Declaration( "width" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint8( YIELD::StructuredStream::Declaration( "policy" ), policy ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "stripe_size" ), stripe_size ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "width" ), width ); }
  
      protected:
        uint8_t policy;
        uint32_t stripe_size;
        uint32_t width;
      };
  
      class Replica : public YIELD::Serializable
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
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::Replica", 541200194UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StripingPolicy", "striping_policy" ), &striping_policy ); replication_flags = input_stream.readUint32( YIELD::StructuredStream::Declaration( "replication_flags" ) ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "osd_uuids" ), &osd_uuids ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StripingPolicy", "striping_policy" ), striping_policy ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "replication_flags" ), replication_flags ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "osd_uuids" ), osd_uuids ); }
  
      protected:
        org::xtreemfs::interfaces::StripingPolicy striping_policy;
        uint32_t replication_flags;
        org::xtreemfs::interfaces::StringSet osd_uuids;
      };
  
      class ReplicaSet : public std::vector<org::xtreemfs::interfaces::Replica>, public YIELD::Serializable
      {
      public:
        ReplicaSet() { }
        ReplicaSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Replica>( size ) { }
        virtual ~ReplicaSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::ReplicaSet", 4051946467UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::Replica item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Replica", "item" ), &item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Replica", "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::Replica>::size(); }
      };
  
      class XLocSet : public YIELD::Serializable
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
        void set_repUpdatePolicy( const char* repUpdatePolicy, size_t repUpdatePolicy_len = 0 ) { this->repUpdatePolicy.assign( repUpdatePolicy, ( repUpdatePolicy_len != 0 ) ? repUpdatePolicy_len : std::strlen( repUpdatePolicy ) ); }
        const std::string& get_repUpdatePolicy() const { return repUpdatePolicy; }
        void set_read_only_file_size( uint64_t read_only_file_size ) { this->read_only_file_size = read_only_file_size; }
        uint64_t get_read_only_file_size() const { return read_only_file_size; }
  
        bool operator==( const XLocSet& other ) const { return replicas == other.replicas && version == other.version && repUpdatePolicy == other.repUpdatePolicy && read_only_file_size == other.read_only_file_size; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::XLocSet", 2669278184UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ReplicaSet", "replicas" ), &replicas ); version = input_stream.readUint32( YIELD::StructuredStream::Declaration( "version" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "repUpdatePolicy" ), repUpdatePolicy ); read_only_file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "read_only_file_size" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ReplicaSet", "replicas" ), replicas ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "version" ), version ); output_stream.writeString( YIELD::StructuredStream::Declaration( "repUpdatePolicy" ), repUpdatePolicy ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "read_only_file_size" ), read_only_file_size ); }
  
      protected:
        org::xtreemfs::interfaces::ReplicaSet replicas;
        uint32_t version;
        std::string repUpdatePolicy;
        uint64_t read_only_file_size;
      };
  
      class NewFileSize : public YIELD::Serializable
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
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::NewFileSize", 3946675201UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { size_in_bytes = input_stream.readUint64( YIELD::StructuredStream::Declaration( "size_in_bytes" ) ); truncate_epoch = input_stream.readUint32( YIELD::StructuredStream::Declaration( "truncate_epoch" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint64( YIELD::StructuredStream::Declaration( "size_in_bytes" ), size_in_bytes ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "truncate_epoch" ), truncate_epoch ); }
  
      protected:
        uint64_t size_in_bytes;
        uint32_t truncate_epoch;
      };
  
      class NewFileSizeSet : public std::vector<org::xtreemfs::interfaces::NewFileSize>, public YIELD::Serializable
      {
      public:
        NewFileSizeSet() { }
        NewFileSizeSet( size_type size ) : std::vector<org::xtreemfs::interfaces::NewFileSize>( size ) { }
        virtual ~NewFileSizeSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::NewFileSizeSet", 4266043619UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::NewFileSize item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::NewFileSize", "item" ), &item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::NewFileSize", "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::NewFileSize>::size(); }
      };
  
      class OSDtoMRCData : public YIELD::Serializable
      {
      public:
        OSDtoMRCData() : caching_policy( 0 ) { }
        OSDtoMRCData( uint8_t caching_policy, const std::string& data ) : caching_policy( caching_policy ), data( data ) { }
        OSDtoMRCData( uint8_t caching_policy, const char* data, size_t data_len ) : caching_policy( caching_policy ), data( data, data_len ) { }
        virtual ~OSDtoMRCData() { }
  
        void set_caching_policy( uint8_t caching_policy ) { this->caching_policy = caching_policy; }
        uint8_t get_caching_policy() const { return caching_policy; }
        void set_data( const std::string& data ) { set_data( data.c_str(), data.size() ); }
        void set_data( const char* data, size_t data_len = 0 ) { this->data.assign( data, ( data_len != 0 ) ? data_len : std::strlen( data ) ); }
        const std::string& get_data() const { return data; }
  
        bool operator==( const OSDtoMRCData& other ) const { return caching_policy == other.caching_policy && data == other.data; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::OSDtoMRCData", 3652194158UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { caching_policy = input_stream.readUint8( YIELD::StructuredStream::Declaration( "caching_policy" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "data" ), data ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint8( YIELD::StructuredStream::Declaration( "caching_policy" ), caching_policy ); output_stream.writeString( YIELD::StructuredStream::Declaration( "data" ), data ); }
  
      protected:
        uint8_t caching_policy;
        std::string data;
      };
  
      class OSDtoMRCDataSet : public std::vector<org::xtreemfs::interfaces::OSDtoMRCData>, public YIELD::Serializable
      {
      public:
        OSDtoMRCDataSet() { }
        OSDtoMRCDataSet( size_type size ) : std::vector<org::xtreemfs::interfaces::OSDtoMRCData>( size ) { }
        virtual ~OSDtoMRCDataSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::OSDtoMRCDataSet", 3900363312UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::OSDtoMRCData item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDtoMRCData", "item" ), &item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDtoMRCData", "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::OSDtoMRCData>::size(); }
      };
  
      class FileCredentials : public YIELD::Serializable
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
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::FileCredentials", 2631208704UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XLocSet", "xlocs" ), &xlocs ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "xcap" ), &xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XLocSet", "xlocs" ), xlocs ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "xcap" ), xcap ); }
  
      protected:
        org::xtreemfs::interfaces::XLocSet xlocs;
        org::xtreemfs::interfaces::XCap xcap;
      };
  
      class OSDWriteResponse : public YIELD::Serializable
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
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::OSDWriteResponse", 1477787725UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::NewFileSizeSet", "new_file_size" ), &new_file_size ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDtoMRCDataSet", "opaque_data" ), &opaque_data ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::NewFileSizeSet", "new_file_size" ), new_file_size ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDtoMRCDataSet", "opaque_data" ), opaque_data ); }
  
      protected:
        org::xtreemfs::interfaces::NewFileSizeSet new_file_size;
        org::xtreemfs::interfaces::OSDtoMRCDataSet opaque_data;
      };
  
  
  
    };
  
  
  
  };
  
  

};

#endif
