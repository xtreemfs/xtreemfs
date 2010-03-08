#ifndef _2138699406_H_
#define _2138699406_H_


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
  
      class Stat : public ::yidl::runtime::Struct
      {
      public:
        Stat() : dev( 0 ), ino( 0 ), mode( 0 ), nlink( 0 ), size( 0 ), atime_ns( 0 ), mtime_ns( 0 ), ctime_ns( 0 ), blksize( 0 ), etag( 0 ), truncate_epoch( 0 ), attributes( 0 ) { }
        Stat( uint64_t dev, uint64_t ino, uint32_t mode, uint32_t nlink, const std::string& user_id, const std::string& group_id, uint64_t size, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, uint32_t blksize, uint64_t etag, uint32_t truncate_epoch, uint32_t attributes ) : dev( dev ), ino( ino ), mode( mode ), nlink( nlink ), user_id( user_id ), group_id( group_id ), size( size ), atime_ns( atime_ns ), mtime_ns( mtime_ns ), ctime_ns( ctime_ns ), blksize( blksize ), etag( etag ), truncate_epoch( truncate_epoch ), attributes( attributes ) { }
        virtual ~Stat() {  }
  
        uint64_t get_dev() const { return dev; }
        uint64_t get_ino() const { return ino; }
        uint32_t get_mode() const { return mode; }
        uint32_t get_nlink() const { return nlink; }
        const std::string& get_user_id() const { return user_id; }
        const std::string& get_group_id() const { return group_id; }
        uint64_t get_size() const { return size; }
        uint64_t get_atime_ns() const { return atime_ns; }
        uint64_t get_mtime_ns() const { return mtime_ns; }
        uint64_t get_ctime_ns() const { return ctime_ns; }
        uint32_t get_blksize() const { return blksize; }
        uint64_t get_etag() const { return etag; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        uint32_t get_attributes() const { return attributes; }
        void set_dev( uint64_t dev ) { this->dev = dev; }
        void set_ino( uint64_t ino ) { this->ino = ino; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        void set_nlink( uint32_t nlink ) { this->nlink = nlink; }
        void set_user_id( const std::string& user_id ) { this->user_id = user_id; }
        void set_group_id( const std::string& group_id ) { this->group_id = group_id; }
        void set_size( uint64_t size ) { this->size = size; }
        void set_atime_ns( uint64_t atime_ns ) { this->atime_ns = atime_ns; }
        void set_mtime_ns( uint64_t mtime_ns ) { this->mtime_ns = mtime_ns; }
        void set_ctime_ns( uint64_t ctime_ns ) { this->ctime_ns = ctime_ns; }
        void set_blksize( uint32_t blksize ) { this->blksize = blksize; }
        void set_etag( uint64_t etag ) { this->etag = etag; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        void set_attributes( uint32_t attributes ) { this->attributes = attributes; }
  
        bool operator==( const Stat& other ) const { return dev == other.dev && ino == other.ino && mode == other.mode && nlink == other.nlink && user_id == other.user_id && group_id == other.group_id && size == other.size && atime_ns == other.atime_ns && mtime_ns == other.mtime_ns && ctime_ns == other.ctime_ns && blksize == other.blksize && etag == other.etag && truncate_epoch == other.truncate_epoch && attributes == other.attributes; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Stat, 2010030354 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "dev", 0, dev ); marshaller.write( "ino", 0, ino ); marshaller.write( "mode", 0, mode ); marshaller.write( "nlink", 0, nlink ); marshaller.write( "user_id", 0, user_id ); marshaller.write( "group_id", 0, group_id ); marshaller.write( "size", 0, size ); marshaller.write( "atime_ns", 0, atime_ns ); marshaller.write( "mtime_ns", 0, mtime_ns ); marshaller.write( "ctime_ns", 0, ctime_ns ); marshaller.write( "blksize", 0, blksize ); marshaller.write( "etag", 0, etag ); marshaller.write( "truncate_epoch", 0, truncate_epoch ); marshaller.write( "attributes", 0, attributes ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { dev = unmarshaller.read_uint64( "dev", 0 ); ino = unmarshaller.read_uint64( "ino", 0 ); mode = unmarshaller.read_uint32( "mode", 0 ); nlink = unmarshaller.read_uint32( "nlink", 0 ); unmarshaller.read( "user_id", 0, user_id ); unmarshaller.read( "group_id", 0, group_id ); size = unmarshaller.read_uint64( "size", 0 ); atime_ns = unmarshaller.read_uint64( "atime_ns", 0 ); mtime_ns = unmarshaller.read_uint64( "mtime_ns", 0 ); ctime_ns = unmarshaller.read_uint64( "ctime_ns", 0 ); blksize = unmarshaller.read_uint32( "blksize", 0 ); etag = unmarshaller.read_uint64( "etag", 0 ); truncate_epoch = unmarshaller.read_uint32( "truncate_epoch", 0 ); attributes = unmarshaller.read_uint32( "attributes", 0 ); }
  
      protected:
        uint64_t dev;
        uint64_t ino;
        uint32_t mode;
        uint32_t nlink;
        std::string user_id;
        std::string group_id;
        uint64_t size;
        uint64_t atime_ns;
        uint64_t mtime_ns;
        uint64_t ctime_ns;
        uint32_t blksize;
        uint64_t etag;
        uint32_t truncate_epoch;
        uint32_t attributes;
      };
  
      class StatSet
        : public ::yidl::runtime::Sequence,
          public std::vector<org::xtreemfs::interfaces::Stat>
      {
      public:
        StatSet() { }
        StatSet( const org::xtreemfs::interfaces::Stat& first_value ) { std::vector<org::xtreemfs::interfaces::Stat>::push_back( first_value ); }
        StatSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Stat>( size ) { }
        virtual ~StatSet() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StatSet, 2010030355 );
  
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
          org::xtreemfs::interfaces::Stat value;
          unmarshaller.read( "value", 0, value );
          push_back( value );
        }
  
        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };
  
      class DirectoryEntry : public ::yidl::runtime::Struct
      {
      public:
        DirectoryEntry() { }
        DirectoryEntry( const std::string& name, const org::xtreemfs::interfaces::StatSet& stbuf ) : name( name ), stbuf( stbuf ) { }
        virtual ~DirectoryEntry() {  }
  
        const std::string& get_name() const { return name; }
        const org::xtreemfs::interfaces::StatSet& get_stbuf() const { return stbuf; }
        void set_name( const std::string& name ) { this->name = name; }
        void set_stbuf( const org::xtreemfs::interfaces::StatSet&  stbuf ) { this->stbuf = stbuf; }
  
        bool operator==( const DirectoryEntry& other ) const { return name == other.name && stbuf == other.stbuf; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( DirectoryEntry, 2010030356 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "name", 0, name ); marshaller.write( "stbuf", 0, stbuf ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "name", 0, name ); unmarshaller.read( "stbuf", 0, stbuf ); }
  
      protected:
        std::string name;
        org::xtreemfs::interfaces::StatSet stbuf;
      };
  
      class DirectoryEntrySet
        : public ::yidl::runtime::Sequence,
          public std::vector<org::xtreemfs::interfaces::DirectoryEntry>
      {
      public:
        DirectoryEntrySet() { }
        DirectoryEntrySet( const org::xtreemfs::interfaces::DirectoryEntry& first_value ) { std::vector<org::xtreemfs::interfaces::DirectoryEntry>::push_back( first_value ); }
        DirectoryEntrySet( size_type size ) : std::vector<org::xtreemfs::interfaces::DirectoryEntry>( size ) { }
        virtual ~DirectoryEntrySet() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( DirectoryEntrySet, 2010030357 );
  
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
          org::xtreemfs::interfaces::DirectoryEntry value;
          unmarshaller.read( "value", 0, value );
          push_back( value );
        }
  
        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };
  
      class StatVFS : public ::yidl::runtime::Struct
      {
      public:
        StatVFS() : bsize( 0 ), bavail( 0 ), blocks( 0 ), namemax( 0 ), access_control_policy( ACCESS_CONTROL_POLICY_NULL ), etag( 0 ), mode( 0 ) { }
        StatVFS( uint32_t bsize, uint64_t bavail, uint64_t blocks, const std::string& fsid, uint32_t namemax, org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint64_t etag, uint32_t mode, const std::string& name, const std::string& owner_group_id, const std::string& owner_user_id ) : bsize( bsize ), bavail( bavail ), blocks( blocks ), fsid( fsid ), namemax( namemax ), access_control_policy( access_control_policy ), default_striping_policy( default_striping_policy ), etag( etag ), mode( mode ), name( name ), owner_group_id( owner_group_id ), owner_user_id( owner_user_id ) { }
        virtual ~StatVFS() {  }
  
        uint32_t get_bsize() const { return bsize; }
        uint64_t get_bavail() const { return bavail; }
        uint64_t get_blocks() const { return blocks; }
        const std::string& get_fsid() const { return fsid; }
        uint32_t get_namemax() const { return namemax; }
        org::xtreemfs::interfaces::AccessControlPolicyType get_access_control_policy() const { return access_control_policy; }
        const org::xtreemfs::interfaces::StripingPolicy& get_default_striping_policy() const { return default_striping_policy; }
        uint64_t get_etag() const { return etag; }
        uint32_t get_mode() const { return mode; }
        const std::string& get_name() const { return name; }
        const std::string& get_owner_group_id() const { return owner_group_id; }
        const std::string& get_owner_user_id() const { return owner_user_id; }
        void set_bsize( uint32_t bsize ) { this->bsize = bsize; }
        void set_bavail( uint64_t bavail ) { this->bavail = bavail; }
        void set_blocks( uint64_t blocks ) { this->blocks = blocks; }
        void set_fsid( const std::string& fsid ) { this->fsid = fsid; }
        void set_namemax( uint32_t namemax ) { this->namemax = namemax; }
        void set_access_control_policy( org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy ) { this->access_control_policy = access_control_policy; }
        void set_default_striping_policy( const org::xtreemfs::interfaces::StripingPolicy&  default_striping_policy ) { this->default_striping_policy = default_striping_policy; }
        void set_etag( uint64_t etag ) { this->etag = etag; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        void set_name( const std::string& name ) { this->name = name; }
        void set_owner_group_id( const std::string& owner_group_id ) { this->owner_group_id = owner_group_id; }
        void set_owner_user_id( const std::string& owner_user_id ) { this->owner_user_id = owner_user_id; }
  
        bool operator==( const StatVFS& other ) const { return bsize == other.bsize && bavail == other.bavail && blocks == other.blocks && fsid == other.fsid && namemax == other.namemax && access_control_policy == other.access_control_policy && default_striping_policy == other.default_striping_policy && etag == other.etag && mode == other.mode && name == other.name && owner_group_id == other.owner_group_id && owner_user_id == other.owner_user_id; }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StatVFS, 2010030358 );
  
        // yidl::runtime::MarshallableObject
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "bsize", 0, bsize ); marshaller.write( "bavail", 0, bavail ); marshaller.write( "blocks", 0, blocks ); marshaller.write( "fsid", 0, fsid ); marshaller.write( "namemax", 0, namemax ); marshaller.write( "access_control_policy", 0, static_cast<int32_t>( access_control_policy ) ); marshaller.write( "default_striping_policy", 0, default_striping_policy ); marshaller.write( "etag", 0, etag ); marshaller.write( "mode", 0, mode ); marshaller.write( "name", 0, name ); marshaller.write( "owner_group_id", 0, owner_group_id ); marshaller.write( "owner_user_id", 0, owner_user_id ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { bsize = unmarshaller.read_uint32( "bsize", 0 ); bavail = unmarshaller.read_uint64( "bavail", 0 ); blocks = unmarshaller.read_uint64( "blocks", 0 ); unmarshaller.read( "fsid", 0, fsid ); namemax = unmarshaller.read_uint32( "namemax", 0 ); access_control_policy = static_cast<org::xtreemfs::interfaces::AccessControlPolicyType>( unmarshaller.read_int32( "access_control_policy", 0 ) ); unmarshaller.read( "default_striping_policy", 0, default_striping_policy ); etag = unmarshaller.read_uint64( "etag", 0 ); mode = unmarshaller.read_uint32( "mode", 0 ); unmarshaller.read( "name", 0, name ); unmarshaller.read( "owner_group_id", 0, owner_group_id ); unmarshaller.read( "owner_user_id", 0, owner_user_id ); }
  
      protected:
        uint32_t bsize;
        uint64_t bavail;
        uint64_t blocks;
        std::string fsid;
        uint32_t namemax;
        org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy;
        org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
        uint64_t etag;
        uint32_t mode;
        std::string name;
        std::string owner_group_id;
        std::string owner_user_id;
      };
  
      class StatVFSSet
        : public ::yidl::runtime::Sequence,
          public std::vector<org::xtreemfs::interfaces::StatVFS>
      {
      public:
        StatVFSSet() { }
        StatVFSSet( const org::xtreemfs::interfaces::StatVFS& first_value ) { std::vector<org::xtreemfs::interfaces::StatVFS>::push_back( first_value ); }
        StatVFSSet( size_type size ) : std::vector<org::xtreemfs::interfaces::StatVFS>( size ) { }
        virtual ~StatVFSSet() { }
  
        // yidl::runtime::RTTIObject
        YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StatVFSSet, 2010030359 );
  
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
          org::xtreemfs::interfaces::StatVFS value;
          unmarshaller.read( "value", 0, value );
          push_back( value );
        }
  
        // yidl::runtime::Sequence
        size_t get_size() const { return size(); }
      };
  
  
      #ifndef ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_EXCEPTION_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ::yield::concurrency::ExceptionResponse
      #endif
      #endif
      #ifndef ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS ORG_XTREEMFS_REQUEST_PARENT_CLASS
      #elif defined( ORG_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS ORG_REQUEST_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS ::yield::concurrency::Request
      #endif
      #endif
      #ifndef ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS ORG_XTREEMFS_RESPONSE_PARENT_CLASS
      #elif defined( ORG_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS ORG_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS ::yield::concurrency::Response
      #endif
      #endif
  
  
      class MRCInterface
      {
      public:
        const static uint32_t TAG = 2010030514;
  
        virtual ~MRCInterface() { }
  
        uint32_t get_tag() const { return 2010030514; }
  
        const static uint32_t HTTP_PORT_DEFAULT = 30636;
        const static uint32_t ONCRPC_PORT_DEFAULT = 32636;
        const static uint32_t ONCRPCG_PORT_DEFAULT = 32636;
        const static uint32_t ONCRPCS_PORT_DEFAULT = 32636;
        const static uint32_t ONCRPCU_PORT_DEFAULT = 32636;
        const static uint32_t SETATTR_MODE = 1;
        const static uint32_t SETATTR_UID = 2;
        const static uint32_t SETATTR_GID = 4;
        const static uint32_t SETATTR_SIZE = 8;
        const static uint32_t SETATTR_ATIME = 16;
        const static uint32_t SETATTR_MTIME = 32;
        const static uint32_t SETATTR_CTIME = 64;
        const static uint32_t SETATTR_ATTRIBUTES = 128;
  
        virtual void close( const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, const org::xtreemfs::interfaces::XCap& write_xcap ) { }
        virtual void fsetattr( const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set, const org::xtreemfs::interfaces::XCap& xcap ) { }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap ) { }
        virtual void getattr( const std::string& volume_name, const std::string& path, uint64_t known_etag, org::xtreemfs::interfaces::StatSet& stbuf ) { }
        virtual void getxattr( const std::string& volume_name, const std::string& path, const std::string& name, std::string& value ) { }
        virtual void link( const std::string& target_path, const std::string& link_path ) { }
        virtual void listxattr( const std::string& volume_name, const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { }
        virtual void mkdir( const std::string& volume_name, const std::string& path, uint32_t mode ) { }
        virtual void open( const std::string& volume_name, const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, org::xtreemfs::interfaces::FileCredentials& file_credentials ) { }
        virtual void readdir( const std::string& volume_name, const std::string& path, uint64_t known_etag, uint16_t limit_directory_entries_count, bool names_only, uint64_t seen_directory_entries_count, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) { }
        virtual void readlink( const std::string& volume_name, const std::string& path, std::string& link_target_path ) { }
        virtual void removexattr( const std::string& volume_name, const std::string& path, const std::string& name ) { }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { }
        virtual void rmdir( const std::string& volume_name, const std::string& path ) { }
        virtual void setattr( const std::string& volume_name, const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set ) { }
        virtual void setxattr( const std::string& volume_name, const std::string& path, const std::string& name, const std::string& value, int32_t flags ) { }
        virtual void statvfs( const std::string& volume_name, uint64_t known_etag, org::xtreemfs::interfaces::StatVFSSet& stbuf ) { }
        virtual void symlink( const std::string& target_path, const std::string& link_path ) { }
        virtual void unlink( const std::string& volume_name, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { }
        virtual void xtreemfs_checkpoint() { }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, const std::string& osd_uuid, std::string& bitmap ) { }
        virtual void xtreemfs_dump_database( const std::string& dump_file ) { }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, uint32_t num_osds, org::xtreemfs::interfaces::StringSet& osd_uuids ) { }
        virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result ) { }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::StatVFSSet& volumes ) { }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::StatVFS& volume ) { }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap ) { }
        virtual void xtreemfs_replication_to_master() { }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) { }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas ) { }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap ) { }
        virtual void xtreemfs_restore_database( const std::string& dump_file ) { }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) { }
        virtual void xtreemfs_rmvol( const std::string& volume_name ) { }
        virtual void xtreemfs_shutdown() { }
      };
  
      // Use this macro in an implementation class to get all of the prototypes for the operations in MRCInterface
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_PROTOTYPES \
      virtual void close( const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, const org::xtreemfs::interfaces::XCap& write_xcap );\
      virtual void fsetattr( const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set, const org::xtreemfs::interfaces::XCap& xcap );\
      virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap );\
      virtual void getattr( const std::string& volume_name, const std::string& path, uint64_t known_etag, org::xtreemfs::interfaces::StatSet& stbuf );\
      virtual void getxattr( const std::string& volume_name, const std::string& path, const std::string& name, std::string& value );\
      virtual void link( const std::string& target_path, const std::string& link_path );\
      virtual void listxattr( const std::string& volume_name, const std::string& path, org::xtreemfs::interfaces::StringSet& names );\
      virtual void mkdir( const std::string& volume_name, const std::string& path, uint32_t mode );\
      virtual void open( const std::string& volume_name, const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, org::xtreemfs::interfaces::FileCredentials& file_credentials );\
      virtual void readdir( const std::string& volume_name, const std::string& path, uint64_t known_etag, uint16_t limit_directory_entries_count, bool names_only, uint64_t seen_directory_entries_count, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries );\
      virtual void readlink( const std::string& volume_name, const std::string& path, std::string& link_target_path );\
      virtual void removexattr( const std::string& volume_name, const std::string& path, const std::string& name );\
      virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials );\
      virtual void rmdir( const std::string& volume_name, const std::string& path );\
      virtual void setattr( const std::string& volume_name, const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set );\
      virtual void setxattr( const std::string& volume_name, const std::string& path, const std::string& name, const std::string& value, int32_t flags );\
      virtual void statvfs( const std::string& volume_name, uint64_t known_etag, org::xtreemfs::interfaces::StatVFSSet& stbuf );\
      virtual void symlink( const std::string& target_path, const std::string& link_path );\
      virtual void unlink( const std::string& volume_name, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials );\
      virtual void xtreemfs_checkpoint();\
      virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, const std::string& osd_uuid, std::string& bitmap );\
      virtual void xtreemfs_dump_database( const std::string& dump_file );\
      virtual void xtreemfs_get_suitable_osds( const std::string& file_id, uint32_t num_osds, org::xtreemfs::interfaces::StringSet& osd_uuids );\
      virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result );\
      virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::StatVFSSet& volumes );\
      virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::StatVFS& volume );\
      virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap );\
      virtual void xtreemfs_replication_to_master();\
      virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica );\
      virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas );\
      virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap );\
      virtual void xtreemfs_restore_database( const std::string& dump_file );\
      virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size );\
      virtual void xtreemfs_rmvol( const std::string& volume_name );\
      virtual void xtreemfs_shutdown();
  
  
      class MRCInterfaceEvents
      {
      public:
        // Request/response pair definitions for the operations in MRCInterface
  
        class closeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          closeResponse() { }
          virtual ~closeResponse() {  }
  
          bool operator==( const closeResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( closeResponse, 2010030515 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class closeRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          closeRequest() { }
          closeRequest( const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, const org::xtreemfs::interfaces::XCap& write_xcap ) : client_vivaldi_coordinates( client_vivaldi_coordinates ), write_xcap( write_xcap ) { }
          virtual ~closeRequest() {  }
  
          const org::xtreemfs::interfaces::VivaldiCoordinates& get_client_vivaldi_coordinates() const { return client_vivaldi_coordinates; }
          const org::xtreemfs::interfaces::XCap& get_write_xcap() const { return write_xcap; }
          void set_client_vivaldi_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  client_vivaldi_coordinates ) { this->client_vivaldi_coordinates = client_vivaldi_coordinates; }
          void set_write_xcap( const org::xtreemfs::interfaces::XCap&  write_xcap ) { this->write_xcap = write_xcap; }
  
          bool operator==( const closeRequest& other ) const { return client_vivaldi_coordinates == other.client_vivaldi_coordinates && write_xcap == other.write_xcap; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( closeRequest, 2010030515 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "client_vivaldi_coordinates", 0, client_vivaldi_coordinates ); marshaller.write( "write_xcap", 0, write_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "client_vivaldi_coordinates", 0, client_vivaldi_coordinates ); unmarshaller.read( "write_xcap", 0, write_xcap ); }
  
        protected:
          org::xtreemfs::interfaces::VivaldiCoordinates client_vivaldi_coordinates;
          org::xtreemfs::interfaces::XCap write_xcap;
        };
  
        class fsetattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          fsetattrResponse() { }
          virtual ~fsetattrResponse() {  }
  
          bool operator==( const fsetattrResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( fsetattrResponse, 2010030516 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class fsetattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          fsetattrRequest() : to_set( 0 ) { }
          fsetattrRequest( const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set, const org::xtreemfs::interfaces::XCap& xcap ) : stbuf( stbuf ), to_set( to_set ), xcap( xcap ) { }
          virtual ~fsetattrRequest() {  }
  
          const org::xtreemfs::interfaces::Stat& get_stbuf() const { return stbuf; }
          uint32_t get_to_set() const { return to_set; }
          const org::xtreemfs::interfaces::XCap& get_xcap() const { return xcap; }
          void set_stbuf( const org::xtreemfs::interfaces::Stat&  stbuf ) { this->stbuf = stbuf; }
          void set_to_set( uint32_t to_set ) { this->to_set = to_set; }
          void set_xcap( const org::xtreemfs::interfaces::XCap&  xcap ) { this->xcap = xcap; }
  
          bool operator==( const fsetattrRequest& other ) const { return stbuf == other.stbuf && to_set == other.to_set && xcap == other.xcap; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( fsetattrRequest, 2010030516 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "stbuf", 0, stbuf ); marshaller.write( "to_set", 0, to_set ); marshaller.write( "xcap", 0, xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "stbuf", 0, stbuf ); to_set = unmarshaller.read_uint32( "to_set", 0 ); unmarshaller.read( "xcap", 0, xcap ); }
  
        protected:
          org::xtreemfs::interfaces::Stat stbuf;
          uint32_t to_set;
          org::xtreemfs::interfaces::XCap xcap;
        };
  
        class ftruncateResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          ftruncateResponse() { }
          ftruncateResponse( const org::xtreemfs::interfaces::XCap& truncate_xcap ) : truncate_xcap( truncate_xcap ) { }
          virtual ~ftruncateResponse() {  }
  
          const org::xtreemfs::interfaces::XCap& get_truncate_xcap() const { return truncate_xcap; }
          void set_truncate_xcap( const org::xtreemfs::interfaces::XCap&  truncate_xcap ) { this->truncate_xcap = truncate_xcap; }
  
          bool operator==( const ftruncateResponse& other ) const { return truncate_xcap == other.truncate_xcap; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ftruncateResponse, 2010030517 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "truncate_xcap", 0, truncate_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "truncate_xcap", 0, truncate_xcap ); }
  
        protected:
          org::xtreemfs::interfaces::XCap truncate_xcap;
        };
  
        class ftruncateRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          ftruncateRequest() { }
          ftruncateRequest( const org::xtreemfs::interfaces::XCap& write_xcap ) : write_xcap( write_xcap ) { }
          virtual ~ftruncateRequest() {  }
  
          const org::xtreemfs::interfaces::XCap& get_write_xcap() const { return write_xcap; }
          void set_write_xcap( const org::xtreemfs::interfaces::XCap&  write_xcap ) { this->write_xcap = write_xcap; }
  
          bool operator==( const ftruncateRequest& other ) const { return write_xcap == other.write_xcap; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ftruncateRequest, 2010030517 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "write_xcap", 0, write_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "write_xcap", 0, write_xcap ); }
  
        protected:
          org::xtreemfs::interfaces::XCap write_xcap;
        };
  
        class getattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          getattrResponse() { }
          getattrResponse( const org::xtreemfs::interfaces::StatSet& stbuf ) : stbuf( stbuf ) { }
          virtual ~getattrResponse() {  }
  
          const org::xtreemfs::interfaces::StatSet& get_stbuf() const { return stbuf; }
          void set_stbuf( const org::xtreemfs::interfaces::StatSet&  stbuf ) { this->stbuf = stbuf; }
  
          bool operator==( const getattrResponse& other ) const { return stbuf == other.stbuf; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( getattrResponse, 2010030518 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "stbuf", 0, stbuf ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "stbuf", 0, stbuf ); }
  
        protected:
          org::xtreemfs::interfaces::StatSet stbuf;
        };
  
        class getattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          getattrRequest() : known_etag( 0 ) { }
          getattrRequest( const std::string& volume_name, const std::string& path, uint64_t known_etag ) : volume_name( volume_name ), path( path ), known_etag( known_etag ) { }
          virtual ~getattrRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          uint64_t get_known_etag() const { return known_etag; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
          void set_known_etag( uint64_t known_etag ) { this->known_etag = known_etag; }
  
          bool operator==( const getattrRequest& other ) const { return volume_name == other.volume_name && path == other.path && known_etag == other.known_etag; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( getattrRequest, 2010030518 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); marshaller.write( "known_etag", 0, known_etag ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); known_etag = unmarshaller.read_uint64( "known_etag", 0 ); }
  
        protected:
          std::string volume_name;
          std::string path;
          uint64_t known_etag;
        };
  
        class getxattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          getxattrResponse() { }
          getxattrResponse( const std::string& value ) : value( value ) { }
          virtual ~getxattrResponse() {  }
  
          const std::string& get_value() const { return value; }
          void set_value( const std::string& value ) { this->value = value; }
  
          bool operator==( const getxattrResponse& other ) const { return value == other.value; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( getxattrResponse, 2010030519 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "value", 0, value ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "value", 0, value ); }
  
        protected:
          std::string value;
        };
  
        class getxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          getxattrRequest() { }
          getxattrRequest( const std::string& volume_name, const std::string& path, const std::string& name ) : volume_name( volume_name ), path( path ), name( name ) { }
          virtual ~getxattrRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          const std::string& get_name() const { return name; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
          void set_name( const std::string& name ) { this->name = name; }
  
          bool operator==( const getxattrRequest& other ) const { return volume_name == other.volume_name && path == other.path && name == other.name; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( getxattrRequest, 2010030519 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); marshaller.write( "name", 0, name ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); unmarshaller.read( "name", 0, name ); }
  
        protected:
          std::string volume_name;
          std::string path;
          std::string name;
        };
  
        class linkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          linkResponse() { }
          virtual ~linkResponse() {  }
  
          bool operator==( const linkResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( linkResponse, 2010030520 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class linkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          linkRequest() { }
          linkRequest( const std::string& target_path, const std::string& link_path ) : target_path( target_path ), link_path( link_path ) { }
          virtual ~linkRequest() {  }
  
          const std::string& get_target_path() const { return target_path; }
          const std::string& get_link_path() const { return link_path; }
          void set_target_path( const std::string& target_path ) { this->target_path = target_path; }
          void set_link_path( const std::string& link_path ) { this->link_path = link_path; }
  
          bool operator==( const linkRequest& other ) const { return target_path == other.target_path && link_path == other.link_path; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( linkRequest, 2010030520 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "target_path", 0, target_path ); marshaller.write( "link_path", 0, link_path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "target_path", 0, target_path ); unmarshaller.read( "link_path", 0, link_path ); }
  
        protected:
          std::string target_path;
          std::string link_path;
        };
  
        class listxattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          listxattrResponse() { }
          listxattrResponse( const org::xtreemfs::interfaces::StringSet& names ) : names( names ) { }
          virtual ~listxattrResponse() {  }
  
          const org::xtreemfs::interfaces::StringSet& get_names() const { return names; }
          void set_names( const org::xtreemfs::interfaces::StringSet&  names ) { this->names = names; }
  
          bool operator==( const listxattrResponse& other ) const { return names == other.names; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( listxattrResponse, 2010030521 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "names", 0, names ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "names", 0, names ); }
  
        protected:
          org::xtreemfs::interfaces::StringSet names;
        };
  
        class listxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          listxattrRequest() { }
          listxattrRequest( const std::string& volume_name, const std::string& path ) : volume_name( volume_name ), path( path ) { }
          virtual ~listxattrRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
  
          bool operator==( const listxattrRequest& other ) const { return volume_name == other.volume_name && path == other.path; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( listxattrRequest, 2010030521 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); }
  
        protected:
          std::string volume_name;
          std::string path;
        };
  
        class mkdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          mkdirResponse() { }
          virtual ~mkdirResponse() {  }
  
          bool operator==( const mkdirResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( mkdirResponse, 2010030522 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class mkdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          mkdirRequest() : mode( 0 ) { }
          mkdirRequest( const std::string& volume_name, const std::string& path, uint32_t mode ) : volume_name( volume_name ), path( path ), mode( mode ) { }
          virtual ~mkdirRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          uint32_t get_mode() const { return mode; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
          void set_mode( uint32_t mode ) { this->mode = mode; }
  
          bool operator==( const mkdirRequest& other ) const { return volume_name == other.volume_name && path == other.path && mode == other.mode; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( mkdirRequest, 2010030522 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); marshaller.write( "mode", 0, mode ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); mode = unmarshaller.read_uint32( "mode", 0 ); }
  
        protected:
          std::string volume_name;
          std::string path;
          uint32_t mode;
        };
  
        class openResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          openResponse() { }
          openResponse( const org::xtreemfs::interfaces::FileCredentials& file_credentials ) : file_credentials( file_credentials ) { }
          virtual ~openResponse() {  }
  
          const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
  
          bool operator==( const openResponse& other ) const { return file_credentials == other.file_credentials; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( openResponse, 2010030523 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "file_credentials", 0, file_credentials ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "file_credentials", 0, file_credentials ); }
  
        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
        };
  
        class openRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          openRequest() : flags( 0 ), mode( 0 ), attributes( 0 ) { }
          openRequest( const std::string& volume_name, const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates ) : volume_name( volume_name ), path( path ), flags( flags ), mode( mode ), attributes( attributes ), client_vivaldi_coordinates( client_vivaldi_coordinates ) { }
          virtual ~openRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          uint32_t get_flags() const { return flags; }
          uint32_t get_mode() const { return mode; }
          uint32_t get_attributes() const { return attributes; }
          const org::xtreemfs::interfaces::VivaldiCoordinates& get_client_vivaldi_coordinates() const { return client_vivaldi_coordinates; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
          void set_flags( uint32_t flags ) { this->flags = flags; }
          void set_mode( uint32_t mode ) { this->mode = mode; }
          void set_attributes( uint32_t attributes ) { this->attributes = attributes; }
          void set_client_vivaldi_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  client_vivaldi_coordinates ) { this->client_vivaldi_coordinates = client_vivaldi_coordinates; }
  
          bool operator==( const openRequest& other ) const { return volume_name == other.volume_name && path == other.path && flags == other.flags && mode == other.mode && attributes == other.attributes && client_vivaldi_coordinates == other.client_vivaldi_coordinates; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( openRequest, 2010030523 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); marshaller.write( "flags", 0, flags ); marshaller.write( "mode", 0, mode ); marshaller.write( "attributes", 0, attributes ); marshaller.write( "client_vivaldi_coordinates", 0, client_vivaldi_coordinates ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); flags = unmarshaller.read_uint32( "flags", 0 ); mode = unmarshaller.read_uint32( "mode", 0 ); attributes = unmarshaller.read_uint32( "attributes", 0 ); unmarshaller.read( "client_vivaldi_coordinates", 0, client_vivaldi_coordinates ); }
  
        protected:
          std::string volume_name;
          std::string path;
          uint32_t flags;
          uint32_t mode;
          uint32_t attributes;
          org::xtreemfs::interfaces::VivaldiCoordinates client_vivaldi_coordinates;
        };
  
        class readdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          readdirResponse() { }
          readdirResponse( const org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) : directory_entries( directory_entries ) { }
          virtual ~readdirResponse() {  }
  
          const org::xtreemfs::interfaces::DirectoryEntrySet& get_directory_entries() const { return directory_entries; }
          void set_directory_entries( const org::xtreemfs::interfaces::DirectoryEntrySet&  directory_entries ) { this->directory_entries = directory_entries; }
  
          bool operator==( const readdirResponse& other ) const { return directory_entries == other.directory_entries; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( readdirResponse, 2010030524 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "directory_entries", 0, directory_entries ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "directory_entries", 0, directory_entries ); }
  
        protected:
          org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
        };
  
        class readdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          readdirRequest() : known_etag( 0 ), limit_directory_entries_count( 0 ), names_only( false ), seen_directory_entries_count( 0 ) { }
          readdirRequest( const std::string& volume_name, const std::string& path, uint64_t known_etag, uint16_t limit_directory_entries_count, bool names_only, uint64_t seen_directory_entries_count ) : volume_name( volume_name ), path( path ), known_etag( known_etag ), limit_directory_entries_count( limit_directory_entries_count ), names_only( names_only ), seen_directory_entries_count( seen_directory_entries_count ) { }
          virtual ~readdirRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          uint64_t get_known_etag() const { return known_etag; }
          uint16_t get_limit_directory_entries_count() const { return limit_directory_entries_count; }
          bool get_names_only() const { return names_only; }
          uint64_t get_seen_directory_entries_count() const { return seen_directory_entries_count; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
          void set_known_etag( uint64_t known_etag ) { this->known_etag = known_etag; }
          void set_limit_directory_entries_count( uint16_t limit_directory_entries_count ) { this->limit_directory_entries_count = limit_directory_entries_count; }
          void set_names_only( bool names_only ) { this->names_only = names_only; }
          void set_seen_directory_entries_count( uint64_t seen_directory_entries_count ) { this->seen_directory_entries_count = seen_directory_entries_count; }
  
          bool operator==( const readdirRequest& other ) const { return volume_name == other.volume_name && path == other.path && known_etag == other.known_etag && limit_directory_entries_count == other.limit_directory_entries_count && names_only == other.names_only && seen_directory_entries_count == other.seen_directory_entries_count; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( readdirRequest, 2010030524 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); marshaller.write( "known_etag", 0, known_etag ); marshaller.write( "limit_directory_entries_count", 0, limit_directory_entries_count ); marshaller.write( "names_only", 0, names_only ); marshaller.write( "seen_directory_entries_count", 0, seen_directory_entries_count ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); known_etag = unmarshaller.read_uint64( "known_etag", 0 ); limit_directory_entries_count = unmarshaller.read_uint16( "limit_directory_entries_count", 0 ); names_only = unmarshaller.read_bool( "names_only", 0 ); seen_directory_entries_count = unmarshaller.read_uint64( "seen_directory_entries_count", 0 ); }
  
        protected:
          std::string volume_name;
          std::string path;
          uint64_t known_etag;
          uint16_t limit_directory_entries_count;
          bool names_only;
          uint64_t seen_directory_entries_count;
        };
  
        class readlinkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          readlinkResponse() { }
          readlinkResponse( const std::string& link_target_path ) : link_target_path( link_target_path ) { }
          virtual ~readlinkResponse() {  }
  
          const std::string& get_link_target_path() const { return link_target_path; }
          void set_link_target_path( const std::string& link_target_path ) { this->link_target_path = link_target_path; }
  
          bool operator==( const readlinkResponse& other ) const { return link_target_path == other.link_target_path; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( readlinkResponse, 2010030525 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "link_target_path", 0, link_target_path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "link_target_path", 0, link_target_path ); }
  
        protected:
          std::string link_target_path;
        };
  
        class readlinkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          readlinkRequest() { }
          readlinkRequest( const std::string& volume_name, const std::string& path ) : volume_name( volume_name ), path( path ) { }
          virtual ~readlinkRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
  
          bool operator==( const readlinkRequest& other ) const { return volume_name == other.volume_name && path == other.path; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( readlinkRequest, 2010030525 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); }
  
        protected:
          std::string volume_name;
          std::string path;
        };
  
        class removexattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          removexattrResponse() { }
          virtual ~removexattrResponse() {  }
  
          bool operator==( const removexattrResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( removexattrResponse, 2010030526 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class removexattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          removexattrRequest() { }
          removexattrRequest( const std::string& volume_name, const std::string& path, const std::string& name ) : volume_name( volume_name ), path( path ), name( name ) { }
          virtual ~removexattrRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          const std::string& get_name() const { return name; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
          void set_name( const std::string& name ) { this->name = name; }
  
          bool operator==( const removexattrRequest& other ) const { return volume_name == other.volume_name && path == other.path && name == other.name; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( removexattrRequest, 2010030526 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); marshaller.write( "name", 0, name ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); unmarshaller.read( "name", 0, name ); }
  
        protected:
          std::string volume_name;
          std::string path;
          std::string name;
        };
  
        class renameResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          renameResponse() { }
          renameResponse( const org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) : file_credentials( file_credentials ) { }
          virtual ~renameResponse() {  }
  
          const org::xtreemfs::interfaces::FileCredentialsSet& get_file_credentials() const { return file_credentials; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentialsSet&  file_credentials ) { this->file_credentials = file_credentials; }
  
          bool operator==( const renameResponse& other ) const { return file_credentials == other.file_credentials; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( renameResponse, 2010030527 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "file_credentials", 0, file_credentials ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "file_credentials", 0, file_credentials ); }
  
        protected:
          org::xtreemfs::interfaces::FileCredentialsSet file_credentials;
        };
  
        class renameRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          renameRequest() { }
          renameRequest( const std::string& source_path, const std::string& target_path ) : source_path( source_path ), target_path( target_path ) { }
          virtual ~renameRequest() {  }
  
          const std::string& get_source_path() const { return source_path; }
          const std::string& get_target_path() const { return target_path; }
          void set_source_path( const std::string& source_path ) { this->source_path = source_path; }
          void set_target_path( const std::string& target_path ) { this->target_path = target_path; }
  
          bool operator==( const renameRequest& other ) const { return source_path == other.source_path && target_path == other.target_path; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( renameRequest, 2010030527 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "source_path", 0, source_path ); marshaller.write( "target_path", 0, target_path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "source_path", 0, source_path ); unmarshaller.read( "target_path", 0, target_path ); }
  
        protected:
          std::string source_path;
          std::string target_path;
        };
  
        class rmdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          rmdirResponse() { }
          virtual ~rmdirResponse() {  }
  
          bool operator==( const rmdirResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( rmdirResponse, 2010030528 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class rmdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          rmdirRequest() { }
          rmdirRequest( const std::string& volume_name, const std::string& path ) : volume_name( volume_name ), path( path ) { }
          virtual ~rmdirRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
  
          bool operator==( const rmdirRequest& other ) const { return volume_name == other.volume_name && path == other.path; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( rmdirRequest, 2010030528 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); }
  
        protected:
          std::string volume_name;
          std::string path;
        };
  
        class setattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          setattrResponse() { }
          virtual ~setattrResponse() {  }
  
          bool operator==( const setattrResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( setattrResponse, 2010030529 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class setattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          setattrRequest() : to_set( 0 ) { }
          setattrRequest( const std::string& volume_name, const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set ) : volume_name( volume_name ), path( path ), stbuf( stbuf ), to_set( to_set ) { }
          virtual ~setattrRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          const org::xtreemfs::interfaces::Stat& get_stbuf() const { return stbuf; }
          uint32_t get_to_set() const { return to_set; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
          void set_stbuf( const org::xtreemfs::interfaces::Stat&  stbuf ) { this->stbuf = stbuf; }
          void set_to_set( uint32_t to_set ) { this->to_set = to_set; }
  
          bool operator==( const setattrRequest& other ) const { return volume_name == other.volume_name && path == other.path && stbuf == other.stbuf && to_set == other.to_set; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( setattrRequest, 2010030529 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); marshaller.write( "stbuf", 0, stbuf ); marshaller.write( "to_set", 0, to_set ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); unmarshaller.read( "stbuf", 0, stbuf ); to_set = unmarshaller.read_uint32( "to_set", 0 ); }
  
        protected:
          std::string volume_name;
          std::string path;
          org::xtreemfs::interfaces::Stat stbuf;
          uint32_t to_set;
        };
  
        class setxattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          setxattrResponse() { }
          virtual ~setxattrResponse() {  }
  
          bool operator==( const setxattrResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( setxattrResponse, 2010030530 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class setxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          setxattrRequest() : flags( 0 ) { }
          setxattrRequest( const std::string& volume_name, const std::string& path, const std::string& name, const std::string& value, int32_t flags ) : volume_name( volume_name ), path( path ), name( name ), value( value ), flags( flags ) { }
          virtual ~setxattrRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          const std::string& get_name() const { return name; }
          const std::string& get_value() const { return value; }
          int32_t get_flags() const { return flags; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
          void set_name( const std::string& name ) { this->name = name; }
          void set_value( const std::string& value ) { this->value = value; }
          void set_flags( int32_t flags ) { this->flags = flags; }
  
          bool operator==( const setxattrRequest& other ) const { return volume_name == other.volume_name && path == other.path && name == other.name && value == other.value && flags == other.flags; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( setxattrRequest, 2010030530 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); marshaller.write( "name", 0, name ); marshaller.write( "value", 0, value ); marshaller.write( "flags", 0, flags ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); unmarshaller.read( "name", 0, name ); unmarshaller.read( "value", 0, value ); flags = unmarshaller.read_int32( "flags", 0 ); }
  
        protected:
          std::string volume_name;
          std::string path;
          std::string name;
          std::string value;
          int32_t flags;
        };
  
        class statvfsResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          statvfsResponse() { }
          statvfsResponse( const org::xtreemfs::interfaces::StatVFSSet& stbuf ) : stbuf( stbuf ) { }
          virtual ~statvfsResponse() {  }
  
          const org::xtreemfs::interfaces::StatVFSSet& get_stbuf() const { return stbuf; }
          void set_stbuf( const org::xtreemfs::interfaces::StatVFSSet&  stbuf ) { this->stbuf = stbuf; }
  
          bool operator==( const statvfsResponse& other ) const { return stbuf == other.stbuf; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( statvfsResponse, 2010030531 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "stbuf", 0, stbuf ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "stbuf", 0, stbuf ); }
  
        protected:
          org::xtreemfs::interfaces::StatVFSSet stbuf;
        };
  
        class statvfsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          statvfsRequest() : known_etag( 0 ) { }
          statvfsRequest( const std::string& volume_name, uint64_t known_etag ) : volume_name( volume_name ), known_etag( known_etag ) { }
          virtual ~statvfsRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          uint64_t get_known_etag() const { return known_etag; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_known_etag( uint64_t known_etag ) { this->known_etag = known_etag; }
  
          bool operator==( const statvfsRequest& other ) const { return volume_name == other.volume_name && known_etag == other.known_etag; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( statvfsRequest, 2010030531 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "known_etag", 0, known_etag ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); known_etag = unmarshaller.read_uint64( "known_etag", 0 ); }
  
        protected:
          std::string volume_name;
          uint64_t known_etag;
        };
  
        class symlinkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          symlinkResponse() { }
          virtual ~symlinkResponse() {  }
  
          bool operator==( const symlinkResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( symlinkResponse, 2010030532 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class symlinkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          symlinkRequest() { }
          symlinkRequest( const std::string& target_path, const std::string& link_path ) : target_path( target_path ), link_path( link_path ) { }
          virtual ~symlinkRequest() {  }
  
          const std::string& get_target_path() const { return target_path; }
          const std::string& get_link_path() const { return link_path; }
          void set_target_path( const std::string& target_path ) { this->target_path = target_path; }
          void set_link_path( const std::string& link_path ) { this->link_path = link_path; }
  
          bool operator==( const symlinkRequest& other ) const { return target_path == other.target_path && link_path == other.link_path; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( symlinkRequest, 2010030532 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "target_path", 0, target_path ); marshaller.write( "link_path", 0, link_path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "target_path", 0, target_path ); unmarshaller.read( "link_path", 0, link_path ); }
  
        protected:
          std::string target_path;
          std::string link_path;
        };
  
        class unlinkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          unlinkResponse() { }
          unlinkResponse( const org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) : file_credentials( file_credentials ) { }
          virtual ~unlinkResponse() {  }
  
          const org::xtreemfs::interfaces::FileCredentialsSet& get_file_credentials() const { return file_credentials; }
          void set_file_credentials( const org::xtreemfs::interfaces::FileCredentialsSet&  file_credentials ) { this->file_credentials = file_credentials; }
  
          bool operator==( const unlinkResponse& other ) const { return file_credentials == other.file_credentials; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( unlinkResponse, 2010030533 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "file_credentials", 0, file_credentials ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "file_credentials", 0, file_credentials ); }
  
        protected:
          org::xtreemfs::interfaces::FileCredentialsSet file_credentials;
        };
  
        class unlinkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          unlinkRequest() { }
          unlinkRequest( const std::string& volume_name, const std::string& path ) : volume_name( volume_name ), path( path ) { }
          virtual ~unlinkRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          const std::string& get_path() const { return path; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
          void set_path( const std::string& path ) { this->path = path; }
  
          bool operator==( const unlinkRequest& other ) const { return volume_name == other.volume_name && path == other.path; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( unlinkRequest, 2010030533 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); marshaller.write( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); unmarshaller.read( "path", 0, path ); }
  
        protected:
          std::string volume_name;
          std::string path;
        };
  
        class xtreemfs_checkpointResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointResponse() { }
          virtual ~xtreemfs_checkpointResponse() {  }
  
          bool operator==( const xtreemfs_checkpointResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_checkpointResponse, 2010030544 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_checkpointRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointRequest() { }
          virtual ~xtreemfs_checkpointRequest() {  }
  
          bool operator==( const xtreemfs_checkpointRequest& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_checkpointRequest, 2010030544 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_check_file_existsResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_check_file_existsResponse() { }
          xtreemfs_check_file_existsResponse( const std::string& bitmap ) : bitmap( bitmap ) { }
          virtual ~xtreemfs_check_file_existsResponse() {  }
  
          const std::string& get_bitmap() const { return bitmap; }
          void set_bitmap( const std::string& bitmap ) { this->bitmap = bitmap; }
  
          bool operator==( const xtreemfs_check_file_existsResponse& other ) const { return bitmap == other.bitmap; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_check_file_existsResponse, 2010030545 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "bitmap", 0, bitmap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "bitmap", 0, bitmap ); }
  
        protected:
          std::string bitmap;
        };
  
        class xtreemfs_check_file_existsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_check_file_existsRequest() { }
          xtreemfs_check_file_existsRequest( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, const std::string& osd_uuid ) : volume_id( volume_id ), file_ids( file_ids ), osd_uuid( osd_uuid ) { }
          virtual ~xtreemfs_check_file_existsRequest() {  }
  
          const std::string& get_volume_id() const { return volume_id; }
          const org::xtreemfs::interfaces::StringSet& get_file_ids() const { return file_ids; }
          const std::string& get_osd_uuid() const { return osd_uuid; }
          void set_volume_id( const std::string& volume_id ) { this->volume_id = volume_id; }
          void set_file_ids( const org::xtreemfs::interfaces::StringSet&  file_ids ) { this->file_ids = file_ids; }
          void set_osd_uuid( const std::string& osd_uuid ) { this->osd_uuid = osd_uuid; }
  
          bool operator==( const xtreemfs_check_file_existsRequest& other ) const { return volume_id == other.volume_id && file_ids == other.file_ids && osd_uuid == other.osd_uuid; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_check_file_existsRequest, 2010030545 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_id", 0, volume_id ); marshaller.write( "file_ids", 0, file_ids ); marshaller.write( "osd_uuid", 0, osd_uuid ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_id", 0, volume_id ); unmarshaller.read( "file_ids", 0, file_ids ); unmarshaller.read( "osd_uuid", 0, osd_uuid ); }
  
        protected:
          std::string volume_id;
          org::xtreemfs::interfaces::StringSet file_ids;
          std::string osd_uuid;
        };
  
        class xtreemfs_dump_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_dump_databaseResponse() { }
          virtual ~xtreemfs_dump_databaseResponse() {  }
  
          bool operator==( const xtreemfs_dump_databaseResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_dump_databaseResponse, 2010030546 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_dump_databaseRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_dump_databaseRequest() { }
          xtreemfs_dump_databaseRequest( const std::string& dump_file ) : dump_file( dump_file ) { }
          virtual ~xtreemfs_dump_databaseRequest() {  }
  
          const std::string& get_dump_file() const { return dump_file; }
          void set_dump_file( const std::string& dump_file ) { this->dump_file = dump_file; }
  
          bool operator==( const xtreemfs_dump_databaseRequest& other ) const { return dump_file == other.dump_file; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_dump_databaseRequest, 2010030546 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "dump_file", 0, dump_file ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "dump_file", 0, dump_file ); }
  
        protected:
          std::string dump_file;
        };
  
        class xtreemfs_get_suitable_osdsResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_get_suitable_osdsResponse() { }
          xtreemfs_get_suitable_osdsResponse( const org::xtreemfs::interfaces::StringSet& osd_uuids ) : osd_uuids( osd_uuids ) { }
          virtual ~xtreemfs_get_suitable_osdsResponse() {  }
  
          const org::xtreemfs::interfaces::StringSet& get_osd_uuids() const { return osd_uuids; }
          void set_osd_uuids( const org::xtreemfs::interfaces::StringSet&  osd_uuids ) { this->osd_uuids = osd_uuids; }
  
          bool operator==( const xtreemfs_get_suitable_osdsResponse& other ) const { return osd_uuids == other.osd_uuids; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_get_suitable_osdsResponse, 2010030547 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "osd_uuids", 0, osd_uuids ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "osd_uuids", 0, osd_uuids ); }
  
        protected:
          org::xtreemfs::interfaces::StringSet osd_uuids;
        };
  
        class xtreemfs_get_suitable_osdsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_get_suitable_osdsRequest() : num_osds( 0 ) { }
          xtreemfs_get_suitable_osdsRequest( const std::string& file_id, uint32_t num_osds ) : file_id( file_id ), num_osds( num_osds ) { }
          virtual ~xtreemfs_get_suitable_osdsRequest() {  }
  
          const std::string& get_file_id() const { return file_id; }
          uint32_t get_num_osds() const { return num_osds; }
          void set_file_id( const std::string& file_id ) { this->file_id = file_id; }
          void set_num_osds( uint32_t num_osds ) { this->num_osds = num_osds; }
  
          bool operator==( const xtreemfs_get_suitable_osdsRequest& other ) const { return file_id == other.file_id && num_osds == other.num_osds; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_get_suitable_osdsRequest, 2010030547 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "file_id", 0, file_id ); marshaller.write( "num_osds", 0, num_osds ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "file_id", 0, file_id ); num_osds = unmarshaller.read_uint32( "num_osds", 0 ); }
  
        protected:
          std::string file_id;
          uint32_t num_osds;
        };
  
        class xtreemfs_internal_debugResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_internal_debugResponse() { }
          xtreemfs_internal_debugResponse( const std::string& result ) : result( result ) { }
          virtual ~xtreemfs_internal_debugResponse() {  }
  
          const std::string& get_result() const { return result; }
          void set_result( const std::string& result ) { this->result = result; }
  
          bool operator==( const xtreemfs_internal_debugResponse& other ) const { return result == other.result; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_debugResponse, 2010030548 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "result", 0, result ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "result", 0, result ); }
  
        protected:
          std::string result;
        };
  
        class xtreemfs_internal_debugRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_internal_debugRequest() { }
          xtreemfs_internal_debugRequest( const std::string& operation ) : operation( operation ) { }
          virtual ~xtreemfs_internal_debugRequest() {  }
  
          const std::string& get_operation() const { return operation; }
          void set_operation( const std::string& operation ) { this->operation = operation; }
  
          bool operator==( const xtreemfs_internal_debugRequest& other ) const { return operation == other.operation; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_internal_debugRequest, 2010030548 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "operation", 0, operation ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "operation", 0, operation ); }
  
        protected:
          std::string operation;
        };
  
        class xtreemfs_lsvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_lsvolResponse() { }
          xtreemfs_lsvolResponse( const org::xtreemfs::interfaces::StatVFSSet& volumes ) : volumes( volumes ) { }
          virtual ~xtreemfs_lsvolResponse() {  }
  
          const org::xtreemfs::interfaces::StatVFSSet& get_volumes() const { return volumes; }
          void set_volumes( const org::xtreemfs::interfaces::StatVFSSet&  volumes ) { this->volumes = volumes; }
  
          bool operator==( const xtreemfs_lsvolResponse& other ) const { return volumes == other.volumes; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lsvolResponse, 2010030549 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volumes", 0, volumes ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volumes", 0, volumes ); }
  
        protected:
          org::xtreemfs::interfaces::StatVFSSet volumes;
        };
  
        class xtreemfs_lsvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lsvolRequest() { }
          virtual ~xtreemfs_lsvolRequest() {  }
  
          bool operator==( const xtreemfs_lsvolRequest& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_lsvolRequest, 2010030549 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_mkvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_mkvolResponse() { }
          virtual ~xtreemfs_mkvolResponse() {  }
  
          bool operator==( const xtreemfs_mkvolResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_mkvolResponse, 2010030550 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_mkvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_mkvolRequest() { }
          xtreemfs_mkvolRequest( const org::xtreemfs::interfaces::StatVFS& volume ) : volume( volume ) { }
          virtual ~xtreemfs_mkvolRequest() {  }
  
          const org::xtreemfs::interfaces::StatVFS& get_volume() const { return volume; }
          void set_volume( const org::xtreemfs::interfaces::StatVFS&  volume ) { this->volume = volume; }
  
          bool operator==( const xtreemfs_mkvolRequest& other ) const { return volume == other.volume; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_mkvolRequest, 2010030550 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume", 0, volume ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume", 0, volume ); }
  
        protected:
          org::xtreemfs::interfaces::StatVFS volume;
        };
  
        class xtreemfs_renew_capabilityResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_renew_capabilityResponse() { }
          xtreemfs_renew_capabilityResponse( const org::xtreemfs::interfaces::XCap& renewed_xcap ) : renewed_xcap( renewed_xcap ) { }
          virtual ~xtreemfs_renew_capabilityResponse() {  }
  
          const org::xtreemfs::interfaces::XCap& get_renewed_xcap() const { return renewed_xcap; }
          void set_renewed_xcap( const org::xtreemfs::interfaces::XCap&  renewed_xcap ) { this->renewed_xcap = renewed_xcap; }
  
          bool operator==( const xtreemfs_renew_capabilityResponse& other ) const { return renewed_xcap == other.renewed_xcap; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_renew_capabilityResponse, 2010030551 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "renewed_xcap", 0, renewed_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "renewed_xcap", 0, renewed_xcap ); }
  
        protected:
          org::xtreemfs::interfaces::XCap renewed_xcap;
        };
  
        class xtreemfs_renew_capabilityRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_renew_capabilityRequest() { }
          xtreemfs_renew_capabilityRequest( const org::xtreemfs::interfaces::XCap& old_xcap ) : old_xcap( old_xcap ) { }
          virtual ~xtreemfs_renew_capabilityRequest() {  }
  
          const org::xtreemfs::interfaces::XCap& get_old_xcap() const { return old_xcap; }
          void set_old_xcap( const org::xtreemfs::interfaces::XCap&  old_xcap ) { this->old_xcap = old_xcap; }
  
          bool operator==( const xtreemfs_renew_capabilityRequest& other ) const { return old_xcap == other.old_xcap; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_renew_capabilityRequest, 2010030551 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "old_xcap", 0, old_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "old_xcap", 0, old_xcap ); }
  
        protected:
          org::xtreemfs::interfaces::XCap old_xcap;
        };
  
        class xtreemfs_replication_to_masterResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_replication_to_masterResponse() { }
          virtual ~xtreemfs_replication_to_masterResponse() {  }
  
          bool operator==( const xtreemfs_replication_to_masterResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replication_to_masterResponse, 2010030552 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_replication_to_masterRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_replication_to_masterRequest() { }
          virtual ~xtreemfs_replication_to_masterRequest() {  }
  
          bool operator==( const xtreemfs_replication_to_masterRequest& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replication_to_masterRequest, 2010030552 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_replica_addResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_replica_addResponse() { }
          virtual ~xtreemfs_replica_addResponse() {  }
  
          bool operator==( const xtreemfs_replica_addResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replica_addResponse, 2010030553 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_replica_addRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_replica_addRequest() { }
          xtreemfs_replica_addRequest( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) : file_id( file_id ), new_replica( new_replica ) { }
          virtual ~xtreemfs_replica_addRequest() {  }
  
          const std::string& get_file_id() const { return file_id; }
          const org::xtreemfs::interfaces::Replica& get_new_replica() const { return new_replica; }
          void set_file_id( const std::string& file_id ) { this->file_id = file_id; }
          void set_new_replica( const org::xtreemfs::interfaces::Replica&  new_replica ) { this->new_replica = new_replica; }
  
          bool operator==( const xtreemfs_replica_addRequest& other ) const { return file_id == other.file_id && new_replica == other.new_replica; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replica_addRequest, 2010030553 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "file_id", 0, file_id ); marshaller.write( "new_replica", 0, new_replica ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "file_id", 0, file_id ); unmarshaller.read( "new_replica", 0, new_replica ); }
  
        protected:
          std::string file_id;
          org::xtreemfs::interfaces::Replica new_replica;
        };
  
        class xtreemfs_replica_listResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_replica_listResponse() { }
          xtreemfs_replica_listResponse( const org::xtreemfs::interfaces::ReplicaSet& replicas ) : replicas( replicas ) { }
          virtual ~xtreemfs_replica_listResponse() {  }
  
          const org::xtreemfs::interfaces::ReplicaSet& get_replicas() const { return replicas; }
          void set_replicas( const org::xtreemfs::interfaces::ReplicaSet&  replicas ) { this->replicas = replicas; }
  
          bool operator==( const xtreemfs_replica_listResponse& other ) const { return replicas == other.replicas; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replica_listResponse, 2010030554 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "replicas", 0, replicas ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "replicas", 0, replicas ); }
  
        protected:
          org::xtreemfs::interfaces::ReplicaSet replicas;
        };
  
        class xtreemfs_replica_listRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_replica_listRequest() { }
          xtreemfs_replica_listRequest( const std::string& file_id ) : file_id( file_id ) { }
          virtual ~xtreemfs_replica_listRequest() {  }
  
          const std::string& get_file_id() const { return file_id; }
          void set_file_id( const std::string& file_id ) { this->file_id = file_id; }
  
          bool operator==( const xtreemfs_replica_listRequest& other ) const { return file_id == other.file_id; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replica_listRequest, 2010030554 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "file_id", 0, file_id ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "file_id", 0, file_id ); }
  
        protected:
          std::string file_id;
        };
  
        class xtreemfs_replica_removeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_replica_removeResponse() { }
          xtreemfs_replica_removeResponse( const org::xtreemfs::interfaces::XCap& delete_xcap ) : delete_xcap( delete_xcap ) { }
          virtual ~xtreemfs_replica_removeResponse() {  }
  
          const org::xtreemfs::interfaces::XCap& get_delete_xcap() const { return delete_xcap; }
          void set_delete_xcap( const org::xtreemfs::interfaces::XCap&  delete_xcap ) { this->delete_xcap = delete_xcap; }
  
          bool operator==( const xtreemfs_replica_removeResponse& other ) const { return delete_xcap == other.delete_xcap; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replica_removeResponse, 2010030555 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "delete_xcap", 0, delete_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "delete_xcap", 0, delete_xcap ); }
  
        protected:
          org::xtreemfs::interfaces::XCap delete_xcap;
        };
  
        class xtreemfs_replica_removeRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_replica_removeRequest() { }
          xtreemfs_replica_removeRequest( const std::string& file_id, const std::string& osd_uuid ) : file_id( file_id ), osd_uuid( osd_uuid ) { }
          virtual ~xtreemfs_replica_removeRequest() {  }
  
          const std::string& get_file_id() const { return file_id; }
          const std::string& get_osd_uuid() const { return osd_uuid; }
          void set_file_id( const std::string& file_id ) { this->file_id = file_id; }
          void set_osd_uuid( const std::string& osd_uuid ) { this->osd_uuid = osd_uuid; }
  
          bool operator==( const xtreemfs_replica_removeRequest& other ) const { return file_id == other.file_id && osd_uuid == other.osd_uuid; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_replica_removeRequest, 2010030555 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "file_id", 0, file_id ); marshaller.write( "osd_uuid", 0, osd_uuid ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "file_id", 0, file_id ); unmarshaller.read( "osd_uuid", 0, osd_uuid ); }
  
        protected:
          std::string file_id;
          std::string osd_uuid;
        };
  
        class xtreemfs_restore_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_restore_databaseResponse() { }
          virtual ~xtreemfs_restore_databaseResponse() {  }
  
          bool operator==( const xtreemfs_restore_databaseResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_restore_databaseResponse, 2010030556 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_restore_databaseRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_restore_databaseRequest() { }
          xtreemfs_restore_databaseRequest( const std::string& dump_file ) : dump_file( dump_file ) { }
          virtual ~xtreemfs_restore_databaseRequest() {  }
  
          const std::string& get_dump_file() const { return dump_file; }
          void set_dump_file( const std::string& dump_file ) { this->dump_file = dump_file; }
  
          bool operator==( const xtreemfs_restore_databaseRequest& other ) const { return dump_file == other.dump_file; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_restore_databaseRequest, 2010030556 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "dump_file", 0, dump_file ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "dump_file", 0, dump_file ); }
  
        protected:
          std::string dump_file;
        };
  
        class xtreemfs_restore_fileResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_restore_fileResponse() { }
          virtual ~xtreemfs_restore_fileResponse() {  }
  
          bool operator==( const xtreemfs_restore_fileResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_restore_fileResponse, 2010030557 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_restore_fileRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_restore_fileRequest() : file_size( 0 ), stripe_size( 0 ) { }
          xtreemfs_restore_fileRequest( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) : file_path( file_path ), file_id( file_id ), file_size( file_size ), osd_uuid( osd_uuid ), stripe_size( stripe_size ) { }
          virtual ~xtreemfs_restore_fileRequest() {  }
  
          const std::string& get_file_path() const { return file_path; }
          const std::string& get_file_id() const { return file_id; }
          uint64_t get_file_size() const { return file_size; }
          const std::string& get_osd_uuid() const { return osd_uuid; }
          int32_t get_stripe_size() const { return stripe_size; }
          void set_file_path( const std::string& file_path ) { this->file_path = file_path; }
          void set_file_id( const std::string& file_id ) { this->file_id = file_id; }
          void set_file_size( uint64_t file_size ) { this->file_size = file_size; }
          void set_osd_uuid( const std::string& osd_uuid ) { this->osd_uuid = osd_uuid; }
          void set_stripe_size( int32_t stripe_size ) { this->stripe_size = stripe_size; }
  
          bool operator==( const xtreemfs_restore_fileRequest& other ) const { return file_path == other.file_path && file_id == other.file_id && file_size == other.file_size && osd_uuid == other.osd_uuid && stripe_size == other.stripe_size; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_restore_fileRequest, 2010030557 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "file_path", 0, file_path ); marshaller.write( "file_id", 0, file_id ); marshaller.write( "file_size", 0, file_size ); marshaller.write( "osd_uuid", 0, osd_uuid ); marshaller.write( "stripe_size", 0, stripe_size ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "file_path", 0, file_path ); unmarshaller.read( "file_id", 0, file_id ); file_size = unmarshaller.read_uint64( "file_size", 0 ); unmarshaller.read( "osd_uuid", 0, osd_uuid ); stripe_size = unmarshaller.read_int32( "stripe_size", 0 ); }
  
        protected:
          std::string file_path;
          std::string file_id;
          uint64_t file_size;
          std::string osd_uuid;
          int32_t stripe_size;
        };
  
        class xtreemfs_rmvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_rmvolResponse() { }
          virtual ~xtreemfs_rmvolResponse() {  }
  
          bool operator==( const xtreemfs_rmvolResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rmvolResponse, 2010030558 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_rmvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_rmvolRequest() { }
          xtreemfs_rmvolRequest( const std::string& volume_name ) : volume_name( volume_name ) { }
          virtual ~xtreemfs_rmvolRequest() {  }
  
          const std::string& get_volume_name() const { return volume_name; }
          void set_volume_name( const std::string& volume_name ) { this->volume_name = volume_name; }
  
          bool operator==( const xtreemfs_rmvolRequest& other ) const { return volume_name == other.volume_name; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_rmvolRequest, 2010030558 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "volume_name", 0, volume_name ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "volume_name", 0, volume_name ); }
  
        protected:
          std::string volume_name;
        };
  
        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownResponse() { }
          virtual ~xtreemfs_shutdownResponse() {  }
  
          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 2010030559 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownRequest() { }
          virtual ~xtreemfs_shutdownRequest() {  }
  
          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }
  
          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 2010030559 );
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
        };
  
        class ConcurrentModificationException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          ConcurrentModificationException() { }
          ConcurrentModificationException( const std::string& stack_trace ) : stack_trace( stack_trace ) { }
          ConcurrentModificationException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~ConcurrentModificationException() throw() { }
  
          const std::string& get_stack_trace() const { return stack_trace; }
          void set_stack_trace( const std::string& stack_trace ) { this->stack_trace = stack_trace; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "stack_trace", 0, stack_trace ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "stack_trace", 0, stack_trace ); }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const { return new ConcurrentModificationException( stack_trace ); }
          virtual void throwStackClone() const { throw ConcurrentModificationException( stack_trace ); }
  
        protected:
          std::string stack_trace;
        };
  
        class errnoException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          errnoException() : error_code( 0 ) { }
          errnoException( uint32_t error_code, const std::string& error_message, const std::string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
          errnoException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~errnoException() throw() { }
  
          uint32_t get_error_code() const { return error_code; }
          const std::string& get_error_message() const { return error_message; }
          const std::string& get_stack_trace() const { return stack_trace; }
          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          void set_error_message( const std::string& error_message ) { this->error_message = error_message; }
          void set_stack_trace( const std::string& stack_trace ) { this->stack_trace = stack_trace; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "error_code", 0, error_code ); marshaller.write( "error_message", 0, error_message ); marshaller.write( "stack_trace", 0, stack_trace ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { error_code = unmarshaller.read_uint32( "error_code", 0 ); unmarshaller.read( "error_message", 0, error_message ); unmarshaller.read( "stack_trace", 0, stack_trace ); }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const { return new errnoException( error_code, error_message, stack_trace ); }
          virtual void throwStackClone() const { throw errnoException( error_code, error_message, stack_trace ); }
  
        protected:
          uint32_t error_code;
          std::string error_message;
          std::string stack_trace;
        };
  
        class InvalidArgumentException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          InvalidArgumentException() { }
          InvalidArgumentException( const std::string& error_message ) : error_message( error_message ) { }
          InvalidArgumentException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~InvalidArgumentException() throw() { }
  
          const std::string& get_error_message() const { return error_message; }
          void set_error_message( const std::string& error_message ) { this->error_message = error_message; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "error_message", 0, error_message ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "error_message", 0, error_message ); }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const { return new InvalidArgumentException( error_message ); }
          virtual void throwStackClone() const { throw InvalidArgumentException( error_message ); }
  
        protected:
          std::string error_message;
        };
  
        class MRCException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          MRCException() : error_code( 0 ) { }
          MRCException( uint32_t error_code, const std::string& error_message, const std::string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
          MRCException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~MRCException() throw() { }
  
          uint32_t get_error_code() const { return error_code; }
          const std::string& get_error_message() const { return error_message; }
          const std::string& get_stack_trace() const { return stack_trace; }
          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          void set_error_message( const std::string& error_message ) { this->error_message = error_message; }
          void set_stack_trace( const std::string& stack_trace ) { this->stack_trace = stack_trace; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "error_code", 0, error_code ); marshaller.write( "error_message", 0, error_message ); marshaller.write( "stack_trace", 0, stack_trace ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { error_code = unmarshaller.read_uint32( "error_code", 0 ); unmarshaller.read( "error_message", 0, error_message ); unmarshaller.read( "stack_trace", 0, stack_trace ); }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const { return new MRCException( error_code, error_message, stack_trace ); }
          virtual void throwStackClone() const { throw MRCException( error_code, error_message, stack_trace ); }
  
        protected:
          uint32_t error_code;
          std::string error_message;
          std::string stack_trace;
        };
  
        class ProtocolException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          ProtocolException() : accept_stat( 0 ), error_code( 0 ) { }
          ProtocolException( uint32_t accept_stat, uint32_t error_code, const std::string& stack_trace ) : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace ) { }
          ProtocolException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~ProtocolException() throw() { }
  
          uint32_t get_accept_stat() const { return accept_stat; }
          uint32_t get_error_code() const { return error_code; }
          const std::string& get_stack_trace() const { return stack_trace; }
          void set_accept_stat( uint32_t accept_stat ) { this->accept_stat = accept_stat; }
          void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
          void set_stack_trace( const std::string& stack_trace ) { this->stack_trace = stack_trace; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "accept_stat", 0, accept_stat ); marshaller.write( "error_code", 0, error_code ); marshaller.write( "stack_trace", 0, stack_trace ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { accept_stat = unmarshaller.read_uint32( "accept_stat", 0 ); error_code = unmarshaller.read_uint32( "error_code", 0 ); unmarshaller.read( "stack_trace", 0, stack_trace ); }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const { return new ProtocolException( accept_stat, error_code, stack_trace ); }
          virtual void throwStackClone() const { throw ProtocolException( accept_stat, error_code, stack_trace ); }
  
        protected:
          uint32_t accept_stat;
          uint32_t error_code;
          std::string stack_trace;
        };
  
        class RedirectException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          RedirectException() : port( 0 ) { }
          RedirectException( const std::string& address, uint16_t port ) : address( address ), port( port ) { }
          RedirectException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~RedirectException() throw() { }
  
          const std::string& get_address() const { return address; }
          uint16_t get_port() const { return port; }
          void set_address( const std::string& address ) { this->address = address; }
          void set_port( uint16_t port ) { this->port = port; }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.write( "address", 0, address ); marshaller.write( "port", 0, port ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.read( "address", 0, address ); port = unmarshaller.read_uint16( "port", 0 ); }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const { return new RedirectException( address, port ); }
          virtual void throwStackClone() const { throw RedirectException( address, port ); }
  
        protected:
          std::string address;
          uint16_t port;
        };
  
        class StaleETagException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          StaleETagException() { }
          StaleETagException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~StaleETagException() throw() { }
  
          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const {  }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) {  }
  
          // yield::concurrency::ExceptionResponse
          virtual ::yield::concurrency::ExceptionResponse* clone() const { return new StaleETagException(); }
          virtual void throwStackClone() const { throw StaleETagException(); }
        };
      };
  
  
      class MRCInterfaceEventFactory : public ::yield::concurrency::EventFactory, private MRCInterfaceEvents
      {
      public:
        // yield::concurrency::EventFactory
        virtual ::yield::concurrency::ExceptionResponse* createExceptionResponse( uint32_t type_id )
        {
          switch ( type_id )
          {
            case 2010030564: return new ConcurrentModificationException;
            case 2010030565: return new errnoException;
            case 2010030566: return new InvalidArgumentException;
            case 2010030567: return new MRCException;
            case 2010030568: return new ProtocolException;
            case 2010030569: return new RedirectException;
            case 2010030570: return new StaleETagException;
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::ExceptionResponse* createExceptionResponse( const char* type_name )
        {
          if ( strcmp( type_name, "ConcurrentModificationException" ) == 0 ) return new ConcurrentModificationException;
          else if ( strcmp( type_name, "errnoException" ) == 0 ) return new errnoException;
          else if ( strcmp( type_name, "InvalidArgumentException" ) == 0 ) return new InvalidArgumentException;
          else if ( strcmp( type_name, "MRCException" ) == 0 ) return new MRCException;
          else if ( strcmp( type_name, "ProtocolException" ) == 0 ) return new ProtocolException;
          else if ( strcmp( type_name, "RedirectException" ) == 0 ) return new RedirectException;
          else if ( strcmp( type_name, "StaleETagException" ) == 0 ) return new StaleETagException;
          else return NULL;
        }
  
        virtual ::yield::concurrency::Request* createRequest( uint32_t type_id )
        {
          switch ( type_id )
          {
            case 2010030515: return new closeRequest;
            case 2010030516: return new fsetattrRequest;
            case 2010030517: return new ftruncateRequest;
            case 2010030518: return new getattrRequest;
            case 2010030519: return new getxattrRequest;
            case 2010030520: return new linkRequest;
            case 2010030521: return new listxattrRequest;
            case 2010030522: return new mkdirRequest;
            case 2010030523: return new openRequest;
            case 2010030524: return new readdirRequest;
            case 2010030525: return new readlinkRequest;
            case 2010030526: return new removexattrRequest;
            case 2010030527: return new renameRequest;
            case 2010030528: return new rmdirRequest;
            case 2010030529: return new setattrRequest;
            case 2010030530: return new setxattrRequest;
            case 2010030531: return new statvfsRequest;
            case 2010030532: return new symlinkRequest;
            case 2010030533: return new unlinkRequest;
            case 2010030544: return new xtreemfs_checkpointRequest;
            case 2010030545: return new xtreemfs_check_file_existsRequest;
            case 2010030546: return new xtreemfs_dump_databaseRequest;
            case 2010030547: return new xtreemfs_get_suitable_osdsRequest;
            case 2010030548: return new xtreemfs_internal_debugRequest;
            case 2010030549: return new xtreemfs_lsvolRequest;
            case 2010030550: return new xtreemfs_mkvolRequest;
            case 2010030551: return new xtreemfs_renew_capabilityRequest;
            case 2010030552: return new xtreemfs_replication_to_masterRequest;
            case 2010030553: return new xtreemfs_replica_addRequest;
            case 2010030554: return new xtreemfs_replica_listRequest;
            case 2010030555: return new xtreemfs_replica_removeRequest;
            case 2010030556: return new xtreemfs_restore_databaseRequest;
            case 2010030557: return new xtreemfs_restore_fileRequest;
            case 2010030558: return new xtreemfs_rmvolRequest;
            case 2010030559: return new xtreemfs_shutdownRequest;
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::Request* createRequest( const char* type_name )
        {
          if ( strcmp( type_name, "closeRequest" ) == 0 ) return new closeRequest;
          else if ( strcmp( type_name, "fsetattrRequest" ) == 0 ) return new fsetattrRequest;
          else if ( strcmp( type_name, "ftruncateRequest" ) == 0 ) return new ftruncateRequest;
          else if ( strcmp( type_name, "getattrRequest" ) == 0 ) return new getattrRequest;
          else if ( strcmp( type_name, "getxattrRequest" ) == 0 ) return new getxattrRequest;
          else if ( strcmp( type_name, "linkRequest" ) == 0 ) return new linkRequest;
          else if ( strcmp( type_name, "listxattrRequest" ) == 0 ) return new listxattrRequest;
          else if ( strcmp( type_name, "mkdirRequest" ) == 0 ) return new mkdirRequest;
          else if ( strcmp( type_name, "openRequest" ) == 0 ) return new openRequest;
          else if ( strcmp( type_name, "readdirRequest" ) == 0 ) return new readdirRequest;
          else if ( strcmp( type_name, "readlinkRequest" ) == 0 ) return new readlinkRequest;
          else if ( strcmp( type_name, "removexattrRequest" ) == 0 ) return new removexattrRequest;
          else if ( strcmp( type_name, "renameRequest" ) == 0 ) return new renameRequest;
          else if ( strcmp( type_name, "rmdirRequest" ) == 0 ) return new rmdirRequest;
          else if ( strcmp( type_name, "setattrRequest" ) == 0 ) return new setattrRequest;
          else if ( strcmp( type_name, "setxattrRequest" ) == 0 ) return new setxattrRequest;
          else if ( strcmp( type_name, "statvfsRequest" ) == 0 ) return new statvfsRequest;
          else if ( strcmp( type_name, "symlinkRequest" ) == 0 ) return new symlinkRequest;
          else if ( strcmp( type_name, "unlinkRequest" ) == 0 ) return new unlinkRequest;
          else if ( strcmp( type_name, "xtreemfs_checkpointRequest" ) == 0 ) return new xtreemfs_checkpointRequest;
          else if ( strcmp( type_name, "xtreemfs_check_file_existsRequest" ) == 0 ) return new xtreemfs_check_file_existsRequest;
          else if ( strcmp( type_name, "xtreemfs_dump_databaseRequest" ) == 0 ) return new xtreemfs_dump_databaseRequest;
          else if ( strcmp( type_name, "xtreemfs_get_suitable_osdsRequest" ) == 0 ) return new xtreemfs_get_suitable_osdsRequest;
          else if ( strcmp( type_name, "xtreemfs_internal_debugRequest" ) == 0 ) return new xtreemfs_internal_debugRequest;
          else if ( strcmp( type_name, "xtreemfs_lsvolRequest" ) == 0 ) return new xtreemfs_lsvolRequest;
          else if ( strcmp( type_name, "xtreemfs_mkvolRequest" ) == 0 ) return new xtreemfs_mkvolRequest;
          else if ( strcmp( type_name, "xtreemfs_renew_capabilityRequest" ) == 0 ) return new xtreemfs_renew_capabilityRequest;
          else if ( strcmp( type_name, "xtreemfs_replication_to_masterRequest" ) == 0 ) return new xtreemfs_replication_to_masterRequest;
          else if ( strcmp( type_name, "xtreemfs_replica_addRequest" ) == 0 ) return new xtreemfs_replica_addRequest;
          else if ( strcmp( type_name, "xtreemfs_replica_listRequest" ) == 0 ) return new xtreemfs_replica_listRequest;
          else if ( strcmp( type_name, "xtreemfs_replica_removeRequest" ) == 0 ) return new xtreemfs_replica_removeRequest;
          else if ( strcmp( type_name, "xtreemfs_restore_databaseRequest" ) == 0 ) return new xtreemfs_restore_databaseRequest;
          else if ( strcmp( type_name, "xtreemfs_restore_fileRequest" ) == 0 ) return new xtreemfs_restore_fileRequest;
          else if ( strcmp( type_name, "xtreemfs_rmvolRequest" ) == 0 ) return new xtreemfs_rmvolRequest;
          else if ( strcmp( type_name, "xtreemfs_shutdownRequest" ) == 0 ) return new xtreemfs_shutdownRequest;
          else return NULL;
        }
  
        virtual ::yield::concurrency::Response* createResponse( uint32_t type_id )
        {
          switch ( type_id )
          {
            case 2010030515: return new closeResponse;
            case 2010030516: return new fsetattrResponse;
            case 2010030517: return new ftruncateResponse;
            case 2010030518: return new getattrResponse;
            case 2010030519: return new getxattrResponse;
            case 2010030520: return new linkResponse;
            case 2010030521: return new listxattrResponse;
            case 2010030522: return new mkdirResponse;
            case 2010030523: return new openResponse;
            case 2010030524: return new readdirResponse;
            case 2010030525: return new readlinkResponse;
            case 2010030526: return new removexattrResponse;
            case 2010030527: return new renameResponse;
            case 2010030528: return new rmdirResponse;
            case 2010030529: return new setattrResponse;
            case 2010030530: return new setxattrResponse;
            case 2010030531: return new statvfsResponse;
            case 2010030532: return new symlinkResponse;
            case 2010030533: return new unlinkResponse;
            case 2010030544: return new xtreemfs_checkpointResponse;
            case 2010030545: return new xtreemfs_check_file_existsResponse;
            case 2010030546: return new xtreemfs_dump_databaseResponse;
            case 2010030547: return new xtreemfs_get_suitable_osdsResponse;
            case 2010030548: return new xtreemfs_internal_debugResponse;
            case 2010030549: return new xtreemfs_lsvolResponse;
            case 2010030550: return new xtreemfs_mkvolResponse;
            case 2010030551: return new xtreemfs_renew_capabilityResponse;
            case 2010030552: return new xtreemfs_replication_to_masterResponse;
            case 2010030553: return new xtreemfs_replica_addResponse;
            case 2010030554: return new xtreemfs_replica_listResponse;
            case 2010030555: return new xtreemfs_replica_removeResponse;
            case 2010030556: return new xtreemfs_restore_databaseResponse;
            case 2010030557: return new xtreemfs_restore_fileResponse;
            case 2010030558: return new xtreemfs_rmvolResponse;
            case 2010030559: return new xtreemfs_shutdownResponse;
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::Response* createResponse( const char* type_name )
        {
          if ( strcmp( type_name, "closeResponse" ) == 0 ) return new closeResponse;
          else if ( strcmp( type_name, "fsetattrResponse" ) == 0 ) return new fsetattrResponse;
          else if ( strcmp( type_name, "ftruncateResponse" ) == 0 ) return new ftruncateResponse;
          else if ( strcmp( type_name, "getattrResponse" ) == 0 ) return new getattrResponse;
          else if ( strcmp( type_name, "getxattrResponse" ) == 0 ) return new getxattrResponse;
          else if ( strcmp( type_name, "linkResponse" ) == 0 ) return new linkResponse;
          else if ( strcmp( type_name, "listxattrResponse" ) == 0 ) return new listxattrResponse;
          else if ( strcmp( type_name, "mkdirResponse" ) == 0 ) return new mkdirResponse;
          else if ( strcmp( type_name, "openResponse" ) == 0 ) return new openResponse;
          else if ( strcmp( type_name, "readdirResponse" ) == 0 ) return new readdirResponse;
          else if ( strcmp( type_name, "readlinkResponse" ) == 0 ) return new readlinkResponse;
          else if ( strcmp( type_name, "removexattrResponse" ) == 0 ) return new removexattrResponse;
          else if ( strcmp( type_name, "renameResponse" ) == 0 ) return new renameResponse;
          else if ( strcmp( type_name, "rmdirResponse" ) == 0 ) return new rmdirResponse;
          else if ( strcmp( type_name, "setattrResponse" ) == 0 ) return new setattrResponse;
          else if ( strcmp( type_name, "setxattrResponse" ) == 0 ) return new setxattrResponse;
          else if ( strcmp( type_name, "statvfsResponse" ) == 0 ) return new statvfsResponse;
          else if ( strcmp( type_name, "symlinkResponse" ) == 0 ) return new symlinkResponse;
          else if ( strcmp( type_name, "unlinkResponse" ) == 0 ) return new unlinkResponse;
          else if ( strcmp( type_name, "xtreemfs_checkpointResponse" ) == 0 ) return new xtreemfs_checkpointResponse;
          else if ( strcmp( type_name, "xtreemfs_check_file_existsResponse" ) == 0 ) return new xtreemfs_check_file_existsResponse;
          else if ( strcmp( type_name, "xtreemfs_dump_databaseResponse" ) == 0 ) return new xtreemfs_dump_databaseResponse;
          else if ( strcmp( type_name, "xtreemfs_get_suitable_osdsResponse" ) == 0 ) return new xtreemfs_get_suitable_osdsResponse;
          else if ( strcmp( type_name, "xtreemfs_internal_debugResponse" ) == 0 ) return new xtreemfs_internal_debugResponse;
          else if ( strcmp( type_name, "xtreemfs_lsvolResponse" ) == 0 ) return new xtreemfs_lsvolResponse;
          else if ( strcmp( type_name, "xtreemfs_mkvolResponse" ) == 0 ) return new xtreemfs_mkvolResponse;
          else if ( strcmp( type_name, "xtreemfs_renew_capabilityResponse" ) == 0 ) return new xtreemfs_renew_capabilityResponse;
          else if ( strcmp( type_name, "xtreemfs_replication_to_masterResponse" ) == 0 ) return new xtreemfs_replication_to_masterResponse;
          else if ( strcmp( type_name, "xtreemfs_replica_addResponse" ) == 0 ) return new xtreemfs_replica_addResponse;
          else if ( strcmp( type_name, "xtreemfs_replica_listResponse" ) == 0 ) return new xtreemfs_replica_listResponse;
          else if ( strcmp( type_name, "xtreemfs_replica_removeResponse" ) == 0 ) return new xtreemfs_replica_removeResponse;
          else if ( strcmp( type_name, "xtreemfs_restore_databaseResponse" ) == 0 ) return new xtreemfs_restore_databaseResponse;
          else if ( strcmp( type_name, "xtreemfs_restore_fileResponse" ) == 0 ) return new xtreemfs_restore_fileResponse;
          else if ( strcmp( type_name, "xtreemfs_rmvolResponse" ) == 0 ) return new xtreemfs_rmvolResponse;
          else if ( strcmp( type_name, "xtreemfs_shutdownResponse" ) == 0 ) return new xtreemfs_shutdownResponse;
          else return NULL;
        }
  
        virtual ::yield::concurrency::ExceptionResponse* isExceptionResponse( ::yidl::runtime::MarshallableObject& marshallable_object ) const
        {
          switch ( marshallable_object.get_type_id() )
          {
            case 2010030564: return static_cast<ConcurrentModificationException*>( &marshallable_object );
            case 2010030565: return static_cast<errnoException*>( &marshallable_object );
            case 2010030566: return static_cast<InvalidArgumentException*>( &marshallable_object );
            case 2010030567: return static_cast<MRCException*>( &marshallable_object );
            case 2010030568: return static_cast<ProtocolException*>( &marshallable_object );
            case 2010030569: return static_cast<RedirectException*>( &marshallable_object );
            case 2010030570: return static_cast<StaleETagException*>( &marshallable_object );
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::Request* isRequest( ::yidl::runtime::MarshallableObject& marshallable_object ) const
        {
          switch ( marshallable_object.get_type_id() )
          {
            case 2010030515: return static_cast<closeRequest*>( &marshallable_object );
            case 2010030516: return static_cast<fsetattrRequest*>( &marshallable_object );
            case 2010030517: return static_cast<ftruncateRequest*>( &marshallable_object );
            case 2010030518: return static_cast<getattrRequest*>( &marshallable_object );
            case 2010030519: return static_cast<getxattrRequest*>( &marshallable_object );
            case 2010030520: return static_cast<linkRequest*>( &marshallable_object );
            case 2010030521: return static_cast<listxattrRequest*>( &marshallable_object );
            case 2010030522: return static_cast<mkdirRequest*>( &marshallable_object );
            case 2010030523: return static_cast<openRequest*>( &marshallable_object );
            case 2010030524: return static_cast<readdirRequest*>( &marshallable_object );
            case 2010030525: return static_cast<readlinkRequest*>( &marshallable_object );
            case 2010030526: return static_cast<removexattrRequest*>( &marshallable_object );
            case 2010030527: return static_cast<renameRequest*>( &marshallable_object );
            case 2010030528: return static_cast<rmdirRequest*>( &marshallable_object );
            case 2010030529: return static_cast<setattrRequest*>( &marshallable_object );
            case 2010030530: return static_cast<setxattrRequest*>( &marshallable_object );
            case 2010030531: return static_cast<statvfsRequest*>( &marshallable_object );
            case 2010030532: return static_cast<symlinkRequest*>( &marshallable_object );
            case 2010030533: return static_cast<unlinkRequest*>( &marshallable_object );
            case 2010030544: return static_cast<xtreemfs_checkpointRequest*>( &marshallable_object );
            case 2010030545: return static_cast<xtreemfs_check_file_existsRequest*>( &marshallable_object );
            case 2010030546: return static_cast<xtreemfs_dump_databaseRequest*>( &marshallable_object );
            case 2010030547: return static_cast<xtreemfs_get_suitable_osdsRequest*>( &marshallable_object );
            case 2010030548: return static_cast<xtreemfs_internal_debugRequest*>( &marshallable_object );
            case 2010030549: return static_cast<xtreemfs_lsvolRequest*>( &marshallable_object );
            case 2010030550: return static_cast<xtreemfs_mkvolRequest*>( &marshallable_object );
            case 2010030551: return static_cast<xtreemfs_renew_capabilityRequest*>( &marshallable_object );
            case 2010030552: return static_cast<xtreemfs_replication_to_masterRequest*>( &marshallable_object );
            case 2010030553: return static_cast<xtreemfs_replica_addRequest*>( &marshallable_object );
            case 2010030554: return static_cast<xtreemfs_replica_listRequest*>( &marshallable_object );
            case 2010030555: return static_cast<xtreemfs_replica_removeRequest*>( &marshallable_object );
            case 2010030556: return static_cast<xtreemfs_restore_databaseRequest*>( &marshallable_object );
            case 2010030557: return static_cast<xtreemfs_restore_fileRequest*>( &marshallable_object );
            case 2010030558: return static_cast<xtreemfs_rmvolRequest*>( &marshallable_object );
            case 2010030559: return static_cast<xtreemfs_shutdownRequest*>( &marshallable_object );
            default: return NULL;
          }
        }
  
        virtual ::yield::concurrency::Response* isResponse( ::yidl::runtime::MarshallableObject& marshallable_object ) const
        {
          switch ( marshallable_object.get_type_id() )
          {
            case 2010030515: return static_cast<closeResponse*>( &marshallable_object );
            case 2010030516: return static_cast<fsetattrResponse*>( &marshallable_object );
            case 2010030517: return static_cast<ftruncateResponse*>( &marshallable_object );
            case 2010030518: return static_cast<getattrResponse*>( &marshallable_object );
            case 2010030519: return static_cast<getxattrResponse*>( &marshallable_object );
            case 2010030520: return static_cast<linkResponse*>( &marshallable_object );
            case 2010030521: return static_cast<listxattrResponse*>( &marshallable_object );
            case 2010030522: return static_cast<mkdirResponse*>( &marshallable_object );
            case 2010030523: return static_cast<openResponse*>( &marshallable_object );
            case 2010030524: return static_cast<readdirResponse*>( &marshallable_object );
            case 2010030525: return static_cast<readlinkResponse*>( &marshallable_object );
            case 2010030526: return static_cast<removexattrResponse*>( &marshallable_object );
            case 2010030527: return static_cast<renameResponse*>( &marshallable_object );
            case 2010030528: return static_cast<rmdirResponse*>( &marshallable_object );
            case 2010030529: return static_cast<setattrResponse*>( &marshallable_object );
            case 2010030530: return static_cast<setxattrResponse*>( &marshallable_object );
            case 2010030531: return static_cast<statvfsResponse*>( &marshallable_object );
            case 2010030532: return static_cast<symlinkResponse*>( &marshallable_object );
            case 2010030533: return static_cast<unlinkResponse*>( &marshallable_object );
            case 2010030544: return static_cast<xtreemfs_checkpointResponse*>( &marshallable_object );
            case 2010030545: return static_cast<xtreemfs_check_file_existsResponse*>( &marshallable_object );
            case 2010030546: return static_cast<xtreemfs_dump_databaseResponse*>( &marshallable_object );
            case 2010030547: return static_cast<xtreemfs_get_suitable_osdsResponse*>( &marshallable_object );
            case 2010030548: return static_cast<xtreemfs_internal_debugResponse*>( &marshallable_object );
            case 2010030549: return static_cast<xtreemfs_lsvolResponse*>( &marshallable_object );
            case 2010030550: return static_cast<xtreemfs_mkvolResponse*>( &marshallable_object );
            case 2010030551: return static_cast<xtreemfs_renew_capabilityResponse*>( &marshallable_object );
            case 2010030552: return static_cast<xtreemfs_replication_to_masterResponse*>( &marshallable_object );
            case 2010030553: return static_cast<xtreemfs_replica_addResponse*>( &marshallable_object );
            case 2010030554: return static_cast<xtreemfs_replica_listResponse*>( &marshallable_object );
            case 2010030555: return static_cast<xtreemfs_replica_removeResponse*>( &marshallable_object );
            case 2010030556: return static_cast<xtreemfs_restore_databaseResponse*>( &marshallable_object );
            case 2010030557: return static_cast<xtreemfs_restore_fileResponse*>( &marshallable_object );
            case 2010030558: return static_cast<xtreemfs_rmvolResponse*>( &marshallable_object );
            case 2010030559: return static_cast<xtreemfs_shutdownResponse*>( &marshallable_object );
            default: return NULL;
          }
        }
  
      };
  
  
      class MRCInterfaceEventHandler : public ::yield::concurrency::EventHandler, private MRCInterfaceEvents
      {
      public:
        // Steals the reference to interface_ to allow for *new
        MRCInterfaceEventHandler( MRCInterface& _interface )
          : _interface( _interface )
        { }
  
        // yield::concurrency::EventHandler
        virtual const char* get_event_handler_name() const
        {
          return "MRCInterface";
        }
  
        virtual void handleEvent( ::yield::concurrency::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to _interface
            switch ( ev.get_type_id() )
            {
              case 2010030515UL: handlecloseRequest( static_cast<closeRequest&>( ev ) ); return;
              case 2010030516UL: handlefsetattrRequest( static_cast<fsetattrRequest&>( ev ) ); return;
              case 2010030517UL: handleftruncateRequest( static_cast<ftruncateRequest&>( ev ) ); return;
              case 2010030518UL: handlegetattrRequest( static_cast<getattrRequest&>( ev ) ); return;
              case 2010030519UL: handlegetxattrRequest( static_cast<getxattrRequest&>( ev ) ); return;
              case 2010030520UL: handlelinkRequest( static_cast<linkRequest&>( ev ) ); return;
              case 2010030521UL: handlelistxattrRequest( static_cast<listxattrRequest&>( ev ) ); return;
              case 2010030522UL: handlemkdirRequest( static_cast<mkdirRequest&>( ev ) ); return;
              case 2010030523UL: handleopenRequest( static_cast<openRequest&>( ev ) ); return;
              case 2010030524UL: handlereaddirRequest( static_cast<readdirRequest&>( ev ) ); return;
              case 2010030525UL: handlereadlinkRequest( static_cast<readlinkRequest&>( ev ) ); return;
              case 2010030526UL: handleremovexattrRequest( static_cast<removexattrRequest&>( ev ) ); return;
              case 2010030527UL: handlerenameRequest( static_cast<renameRequest&>( ev ) ); return;
              case 2010030528UL: handlermdirRequest( static_cast<rmdirRequest&>( ev ) ); return;
              case 2010030529UL: handlesetattrRequest( static_cast<setattrRequest&>( ev ) ); return;
              case 2010030530UL: handlesetxattrRequest( static_cast<setxattrRequest&>( ev ) ); return;
              case 2010030531UL: handlestatvfsRequest( static_cast<statvfsRequest&>( ev ) ); return;
              case 2010030532UL: handlesymlinkRequest( static_cast<symlinkRequest&>( ev ) ); return;
              case 2010030533UL: handleunlinkRequest( static_cast<unlinkRequest&>( ev ) ); return;
              case 2010030544UL: handlextreemfs_checkpointRequest( static_cast<xtreemfs_checkpointRequest&>( ev ) ); return;
              case 2010030545UL: handlextreemfs_check_file_existsRequest( static_cast<xtreemfs_check_file_existsRequest&>( ev ) ); return;
              case 2010030546UL: handlextreemfs_dump_databaseRequest( static_cast<xtreemfs_dump_databaseRequest&>( ev ) ); return;
              case 2010030547UL: handlextreemfs_get_suitable_osdsRequest( static_cast<xtreemfs_get_suitable_osdsRequest&>( ev ) ); return;
              case 2010030548UL: handlextreemfs_internal_debugRequest( static_cast<xtreemfs_internal_debugRequest&>( ev ) ); return;
              case 2010030549UL: handlextreemfs_lsvolRequest( static_cast<xtreemfs_lsvolRequest&>( ev ) ); return;
              case 2010030550UL: handlextreemfs_mkvolRequest( static_cast<xtreemfs_mkvolRequest&>( ev ) ); return;
              case 2010030551UL: handlextreemfs_renew_capabilityRequest( static_cast<xtreemfs_renew_capabilityRequest&>( ev ) ); return;
              case 2010030552UL: handlextreemfs_replication_to_masterRequest( static_cast<xtreemfs_replication_to_masterRequest&>( ev ) ); return;
              case 2010030553UL: handlextreemfs_replica_addRequest( static_cast<xtreemfs_replica_addRequest&>( ev ) ); return;
              case 2010030554UL: handlextreemfs_replica_listRequest( static_cast<xtreemfs_replica_listRequest&>( ev ) ); return;
              case 2010030555UL: handlextreemfs_replica_removeRequest( static_cast<xtreemfs_replica_removeRequest&>( ev ) ); return;
              case 2010030556UL: handlextreemfs_restore_databaseRequest( static_cast<xtreemfs_restore_databaseRequest&>( ev ) ); return;
              case 2010030557UL: handlextreemfs_restore_fileRequest( static_cast<xtreemfs_restore_fileRequest&>( ev ) ); return;
              case 2010030558UL: handlextreemfs_rmvolRequest( static_cast<xtreemfs_rmvolRequest&>( ev ) ); return;
              case 2010030559UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
              default: ::yield::concurrency::Event::dec_ref( ev ); return;
            }
          }
          catch( ::yield::concurrency::ExceptionResponse* exception_response )
          {
            static_cast< ::yield::concurrency::Request& >( ev ).respond( *exception_response );
          }
          catch ( ::yield::concurrency::ExceptionResponse& exception_response )
          {
            static_cast< ::yield::concurrency::Request& >( ev ).respond( *exception_response.clone() );
          }
          catch ( ::yield::platform::Exception& exception )
          {
            static_cast< ::yield::concurrency::Request& >( ev ).respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
          }
  
          ::yidl::runtime::Object::dec_ref( ev );
        }
  
      protected:
        virtual void handlecloseRequest( closeRequest& __request )
        {
          ::yidl::runtime::auto_Object<closeResponse> response( new closeResponse );
          _interface.close( __request.get_client_vivaldi_coordinates(), __request.get_write_xcap() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlefsetattrRequest( fsetattrRequest& __request )
        {
          ::yidl::runtime::auto_Object<fsetattrResponse> response( new fsetattrResponse );
          _interface.fsetattr( __request.get_stbuf(), __request.get_to_set(), __request.get_xcap() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handleftruncateRequest( ftruncateRequest& __request )
        {
          ::yidl::runtime::auto_Object<ftruncateResponse> response( new ftruncateResponse );
          org::xtreemfs::interfaces::XCap truncate_xcap;
          _interface.ftruncate( __request.get_write_xcap(), truncate_xcap );
          response->set_truncate_xcap( truncate_xcap );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlegetattrRequest( getattrRequest& __request )
        {
          ::yidl::runtime::auto_Object<getattrResponse> response( new getattrResponse );
          org::xtreemfs::interfaces::StatSet stbuf;
          _interface.getattr( __request.get_volume_name(), __request.get_path(), __request.get_known_etag(), stbuf );
          response->set_stbuf( stbuf );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlegetxattrRequest( getxattrRequest& __request )
        {
          ::yidl::runtime::auto_Object<getxattrResponse> response( new getxattrResponse );
          std::string value;
          _interface.getxattr( __request.get_volume_name(), __request.get_path(), __request.get_name(), value );
          response->set_value( value );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlelinkRequest( linkRequest& __request )
        {
          ::yidl::runtime::auto_Object<linkResponse> response( new linkResponse );
          _interface.link( __request.get_target_path(), __request.get_link_path() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlelistxattrRequest( listxattrRequest& __request )
        {
          ::yidl::runtime::auto_Object<listxattrResponse> response( new listxattrResponse );
          org::xtreemfs::interfaces::StringSet names;
          _interface.listxattr( __request.get_volume_name(), __request.get_path(), names );
          response->set_names( names );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlemkdirRequest( mkdirRequest& __request )
        {
          ::yidl::runtime::auto_Object<mkdirResponse> response( new mkdirResponse );
          _interface.mkdir( __request.get_volume_name(), __request.get_path(), __request.get_mode() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handleopenRequest( openRequest& __request )
        {
          ::yidl::runtime::auto_Object<openResponse> response( new openResponse );
          org::xtreemfs::interfaces::FileCredentials file_credentials;
          _interface.open( __request.get_volume_name(), __request.get_path(), __request.get_flags(), __request.get_mode(), __request.get_attributes(), __request.get_client_vivaldi_coordinates(), file_credentials );
          response->set_file_credentials( file_credentials );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlereaddirRequest( readdirRequest& __request )
        {
          ::yidl::runtime::auto_Object<readdirResponse> response( new readdirResponse );
          org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
          _interface.readdir( __request.get_volume_name(), __request.get_path(), __request.get_known_etag(), __request.get_limit_directory_entries_count(), __request.get_names_only(), __request.get_seen_directory_entries_count(), directory_entries );
          response->set_directory_entries( directory_entries );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlereadlinkRequest( readlinkRequest& __request )
        {
          ::yidl::runtime::auto_Object<readlinkResponse> response( new readlinkResponse );
          std::string link_target_path;
          _interface.readlink( __request.get_volume_name(), __request.get_path(), link_target_path );
          response->set_link_target_path( link_target_path );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handleremovexattrRequest( removexattrRequest& __request )
        {
          ::yidl::runtime::auto_Object<removexattrResponse> response( new removexattrResponse );
          _interface.removexattr( __request.get_volume_name(), __request.get_path(), __request.get_name() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlerenameRequest( renameRequest& __request )
        {
          ::yidl::runtime::auto_Object<renameResponse> response( new renameResponse );
          org::xtreemfs::interfaces::FileCredentialsSet file_credentials;
          _interface.rename( __request.get_source_path(), __request.get_target_path(), file_credentials );
          response->set_file_credentials( file_credentials );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlermdirRequest( rmdirRequest& __request )
        {
          ::yidl::runtime::auto_Object<rmdirResponse> response( new rmdirResponse );
          _interface.rmdir( __request.get_volume_name(), __request.get_path() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlesetattrRequest( setattrRequest& __request )
        {
          ::yidl::runtime::auto_Object<setattrResponse> response( new setattrResponse );
          _interface.setattr( __request.get_volume_name(), __request.get_path(), __request.get_stbuf(), __request.get_to_set() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlesetxattrRequest( setxattrRequest& __request )
        {
          ::yidl::runtime::auto_Object<setxattrResponse> response( new setxattrResponse );
          _interface.setxattr( __request.get_volume_name(), __request.get_path(), __request.get_name(), __request.get_value(), __request.get_flags() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlestatvfsRequest( statvfsRequest& __request )
        {
          ::yidl::runtime::auto_Object<statvfsResponse> response( new statvfsResponse );
          org::xtreemfs::interfaces::StatVFSSet stbuf;
          _interface.statvfs( __request.get_volume_name(), __request.get_known_etag(), stbuf );
          response->set_stbuf( stbuf );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlesymlinkRequest( symlinkRequest& __request )
        {
          ::yidl::runtime::auto_Object<symlinkResponse> response( new symlinkResponse );
          _interface.symlink( __request.get_target_path(), __request.get_link_path() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handleunlinkRequest( unlinkRequest& __request )
        {
          ::yidl::runtime::auto_Object<unlinkResponse> response( new unlinkResponse );
          org::xtreemfs::interfaces::FileCredentialsSet file_credentials;
          _interface.unlink( __request.get_volume_name(), __request.get_path(), file_credentials );
          response->set_file_credentials( file_credentials );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> response( new xtreemfs_checkpointResponse );
          _interface.xtreemfs_checkpoint();
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_check_file_existsRequest( xtreemfs_check_file_existsRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_check_file_existsResponse> response( new xtreemfs_check_file_existsResponse );
          std::string bitmap;
          _interface.xtreemfs_check_file_exists( __request.get_volume_id(), __request.get_file_ids(), __request.get_osd_uuid(), bitmap );
          response->set_bitmap( bitmap );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_dump_databaseRequest( xtreemfs_dump_databaseRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_dump_databaseResponse> response( new xtreemfs_dump_databaseResponse );
          _interface.xtreemfs_dump_database( __request.get_dump_file() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_get_suitable_osdsRequest( xtreemfs_get_suitable_osdsRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_get_suitable_osdsResponse> response( new xtreemfs_get_suitable_osdsResponse );
          org::xtreemfs::interfaces::StringSet osd_uuids;
          _interface.xtreemfs_get_suitable_osds( __request.get_file_id(), __request.get_num_osds(), osd_uuids );
          response->set_osd_uuids( osd_uuids );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_internal_debugRequest( xtreemfs_internal_debugRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_internal_debugResponse> response( new xtreemfs_internal_debugResponse );
          std::string result;
          _interface.xtreemfs_internal_debug( __request.get_operation(), result );
          response->set_result( result );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_lsvolRequest( xtreemfs_lsvolRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_lsvolResponse> response( new xtreemfs_lsvolResponse );
          org::xtreemfs::interfaces::StatVFSSet volumes;
          _interface.xtreemfs_lsvol( volumes );
          response->set_volumes( volumes );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_mkvolRequest( xtreemfs_mkvolRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_mkvolResponse> response( new xtreemfs_mkvolResponse );
          _interface.xtreemfs_mkvol( __request.get_volume() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_renew_capabilityRequest( xtreemfs_renew_capabilityRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_renew_capabilityResponse> response( new xtreemfs_renew_capabilityResponse );
          org::xtreemfs::interfaces::XCap renewed_xcap;
          _interface.xtreemfs_renew_capability( __request.get_old_xcap(), renewed_xcap );
          response->set_renewed_xcap( renewed_xcap );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_replication_to_masterRequest( xtreemfs_replication_to_masterRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> response( new xtreemfs_replication_to_masterResponse );
          _interface.xtreemfs_replication_to_master();
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_replica_addRequest( xtreemfs_replica_addRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_replica_addResponse> response( new xtreemfs_replica_addResponse );
          _interface.xtreemfs_replica_add( __request.get_file_id(), __request.get_new_replica() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_replica_listRequest( xtreemfs_replica_listRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_replica_listResponse> response( new xtreemfs_replica_listResponse );
          org::xtreemfs::interfaces::ReplicaSet replicas;
          _interface.xtreemfs_replica_list( __request.get_file_id(), replicas );
          response->set_replicas( replicas );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_replica_removeRequest( xtreemfs_replica_removeRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_replica_removeResponse> response( new xtreemfs_replica_removeResponse );
          org::xtreemfs::interfaces::XCap delete_xcap;
          _interface.xtreemfs_replica_remove( __request.get_file_id(), __request.get_osd_uuid(), delete_xcap );
          response->set_delete_xcap( delete_xcap );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_restore_databaseRequest( xtreemfs_restore_databaseRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_restore_databaseResponse> response( new xtreemfs_restore_databaseResponse );
          _interface.xtreemfs_restore_database( __request.get_dump_file() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_restore_fileRequest( xtreemfs_restore_fileRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_restore_fileResponse> response( new xtreemfs_restore_fileResponse );
          _interface.xtreemfs_restore_file( __request.get_file_path(), __request.get_file_id(), __request.get_file_size(), __request.get_osd_uuid(), __request.get_stripe_size() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_rmvolRequest( xtreemfs_rmvolRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_rmvolResponse> response( new xtreemfs_rmvolResponse );
          _interface.xtreemfs_rmvol( __request.get_volume_name() );
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& __request )
        {
          ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> response( new xtreemfs_shutdownResponse );
          _interface.xtreemfs_shutdown();
          __request.respond( response->inc_ref() );
          ::yield::concurrency::Request::dec_ref( __request );
        }
  
      private:
        MRCInterface& _interface;
      };
  
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EVENT_HANDLER_PROTOTYPES \
      virtual void handlecloseRequest( closeRequest& __request );\
      virtual void handlefsetattrRequest( fsetattrRequest& __request );\
      virtual void handleftruncateRequest( ftruncateRequest& __request );\
      virtual void handlegetattrRequest( getattrRequest& __request );\
      virtual void handlegetxattrRequest( getxattrRequest& __request );\
      virtual void handlelinkRequest( linkRequest& __request );\
      virtual void handlelistxattrRequest( listxattrRequest& __request );\
      virtual void handlemkdirRequest( mkdirRequest& __request );\
      virtual void handleopenRequest( openRequest& __request );\
      virtual void handlereaddirRequest( readdirRequest& __request );\
      virtual void handlereadlinkRequest( readlinkRequest& __request );\
      virtual void handleremovexattrRequest( removexattrRequest& __request );\
      virtual void handlerenameRequest( renameRequest& __request );\
      virtual void handlermdirRequest( rmdirRequest& __request );\
      virtual void handlesetattrRequest( setattrRequest& __request );\
      virtual void handlesetxattrRequest( setxattrRequest& __request );\
      virtual void handlestatvfsRequest( statvfsRequest& __request );\
      virtual void handlesymlinkRequest( symlinkRequest& __request );\
      virtual void handleunlinkRequest( unlinkRequest& __request );\
      virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& __request );\
      virtual void handlextreemfs_check_file_existsRequest( xtreemfs_check_file_existsRequest& __request );\
      virtual void handlextreemfs_dump_databaseRequest( xtreemfs_dump_databaseRequest& __request );\
      virtual void handlextreemfs_get_suitable_osdsRequest( xtreemfs_get_suitable_osdsRequest& __request );\
      virtual void handlextreemfs_internal_debugRequest( xtreemfs_internal_debugRequest& __request );\
      virtual void handlextreemfs_lsvolRequest( xtreemfs_lsvolRequest& __request );\
      virtual void handlextreemfs_mkvolRequest( xtreemfs_mkvolRequest& __request );\
      virtual void handlextreemfs_renew_capabilityRequest( xtreemfs_renew_capabilityRequest& __request );\
      virtual void handlextreemfs_replication_to_masterRequest( xtreemfs_replication_to_masterRequest& __request );\
      virtual void handlextreemfs_replica_addRequest( xtreemfs_replica_addRequest& __request );\
      virtual void handlextreemfs_replica_listRequest( xtreemfs_replica_listRequest& __request );\
      virtual void handlextreemfs_replica_removeRequest( xtreemfs_replica_removeRequest& __request );\
      virtual void handlextreemfs_restore_databaseRequest( xtreemfs_restore_databaseRequest& __request );\
      virtual void handlextreemfs_restore_fileRequest( xtreemfs_restore_fileRequest& __request );\
      virtual void handlextreemfs_rmvolRequest( xtreemfs_rmvolRequest& __request );\
      virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& __request );
  
  
      class MRCInterfaceEventSender : public MRCInterface, private MRCInterfaceEvents
      {
      public:
        MRCInterfaceEventSender( ::yield::concurrency::EventTarget& event_target )
          : event_target( event_target.inc_ref() )
        { }
  
        virtual ~MRCInterfaceEventSender()
        {
          ::yield::concurrency::EventTarget::dec_ref( event_target );
        }
          virtual void close( const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, const org::xtreemfs::interfaces::XCap& write_xcap )
          {
            ::yidl::runtime::auto_Object<closeRequest> __request( new closeRequest( client_vivaldi_coordinates, write_xcap ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<closeResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<closeResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<closeResponse> __response = __response_queue->dequeue();
          }
  
          virtual void close( const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, const org::xtreemfs::interfaces::XCap& write_xcap, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<closeRequest> __request( new closeRequest( client_vivaldi_coordinates, write_xcap ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<closeResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<closeResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<closeResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void fsetattr( const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set, const org::xtreemfs::interfaces::XCap& xcap )
          {
            ::yidl::runtime::auto_Object<fsetattrRequest> __request( new fsetattrRequest( stbuf, to_set, xcap ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<fsetattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<fsetattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<fsetattrResponse> __response = __response_queue->dequeue();
          }
  
          virtual void fsetattr( const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set, const org::xtreemfs::interfaces::XCap& xcap, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<fsetattrRequest> __request( new fsetattrRequest( stbuf, to_set, xcap ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<fsetattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<fsetattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<fsetattrResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap )
          {
            ::yidl::runtime::auto_Object<ftruncateRequest> __request( new ftruncateRequest( write_xcap ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<ftruncateResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<ftruncateResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<ftruncateResponse> __response = __response_queue->dequeue();truncate_xcap = __response->get_truncate_xcap();
          }
  
          virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<ftruncateRequest> __request( new ftruncateRequest( write_xcap ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<ftruncateResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<ftruncateResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<ftruncateResponse> __response = __response_queue->dequeue( response_timeout_ns );truncate_xcap = __response->get_truncate_xcap();
          }
  
          virtual void getattr( const std::string& volume_name, const std::string& path, uint64_t known_etag, org::xtreemfs::interfaces::StatSet& stbuf )
          {
            ::yidl::runtime::auto_Object<getattrRequest> __request( new getattrRequest( volume_name, path, known_etag ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<getattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<getattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<getattrResponse> __response = __response_queue->dequeue();stbuf = __response->get_stbuf();
          }
  
          virtual void getattr( const std::string& volume_name, const std::string& path, uint64_t known_etag, org::xtreemfs::interfaces::StatSet& stbuf, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<getattrRequest> __request( new getattrRequest( volume_name, path, known_etag ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<getattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<getattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<getattrResponse> __response = __response_queue->dequeue( response_timeout_ns );stbuf = __response->get_stbuf();
          }
  
          virtual void getxattr( const std::string& volume_name, const std::string& path, const std::string& name, std::string& value )
          {
            ::yidl::runtime::auto_Object<getxattrRequest> __request( new getxattrRequest( volume_name, path, name ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<getxattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<getxattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<getxattrResponse> __response = __response_queue->dequeue();value = __response->get_value();
          }
  
          virtual void getxattr( const std::string& volume_name, const std::string& path, const std::string& name, std::string& value, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<getxattrRequest> __request( new getxattrRequest( volume_name, path, name ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<getxattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<getxattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<getxattrResponse> __response = __response_queue->dequeue( response_timeout_ns );value = __response->get_value();
          }
  
          virtual void link( const std::string& target_path, const std::string& link_path )
          {
            ::yidl::runtime::auto_Object<linkRequest> __request( new linkRequest( target_path, link_path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<linkResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<linkResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<linkResponse> __response = __response_queue->dequeue();
          }
  
          virtual void link( const std::string& target_path, const std::string& link_path, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<linkRequest> __request( new linkRequest( target_path, link_path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<linkResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<linkResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<linkResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void listxattr( const std::string& volume_name, const std::string& path, org::xtreemfs::interfaces::StringSet& names )
          {
            ::yidl::runtime::auto_Object<listxattrRequest> __request( new listxattrRequest( volume_name, path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<listxattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<listxattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<listxattrResponse> __response = __response_queue->dequeue();names = __response->get_names();
          }
  
          virtual void listxattr( const std::string& volume_name, const std::string& path, org::xtreemfs::interfaces::StringSet& names, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<listxattrRequest> __request( new listxattrRequest( volume_name, path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<listxattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<listxattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<listxattrResponse> __response = __response_queue->dequeue( response_timeout_ns );names = __response->get_names();
          }
  
          virtual void mkdir( const std::string& volume_name, const std::string& path, uint32_t mode )
          {
            ::yidl::runtime::auto_Object<mkdirRequest> __request( new mkdirRequest( volume_name, path, mode ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<mkdirResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<mkdirResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<mkdirResponse> __response = __response_queue->dequeue();
          }
  
          virtual void mkdir( const std::string& volume_name, const std::string& path, uint32_t mode, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<mkdirRequest> __request( new mkdirRequest( volume_name, path, mode ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<mkdirResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<mkdirResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<mkdirResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void open( const std::string& volume_name, const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, org::xtreemfs::interfaces::FileCredentials& file_credentials )
          {
            ::yidl::runtime::auto_Object<openRequest> __request( new openRequest( volume_name, path, flags, mode, attributes, client_vivaldi_coordinates ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<openResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<openResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<openResponse> __response = __response_queue->dequeue();file_credentials = __response->get_file_credentials();
          }
  
          virtual void open( const std::string& volume_name, const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, org::xtreemfs::interfaces::FileCredentials& file_credentials, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<openRequest> __request( new openRequest( volume_name, path, flags, mode, attributes, client_vivaldi_coordinates ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<openResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<openResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<openResponse> __response = __response_queue->dequeue( response_timeout_ns );file_credentials = __response->get_file_credentials();
          }
  
          virtual void readdir( const std::string& volume_name, const std::string& path, uint64_t known_etag, uint16_t limit_directory_entries_count, bool names_only, uint64_t seen_directory_entries_count, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries )
          {
            ::yidl::runtime::auto_Object<readdirRequest> __request( new readdirRequest( volume_name, path, known_etag, limit_directory_entries_count, names_only, seen_directory_entries_count ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<readdirResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<readdirResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<readdirResponse> __response = __response_queue->dequeue();directory_entries = __response->get_directory_entries();
          }
  
          virtual void readdir( const std::string& volume_name, const std::string& path, uint64_t known_etag, uint16_t limit_directory_entries_count, bool names_only, uint64_t seen_directory_entries_count, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<readdirRequest> __request( new readdirRequest( volume_name, path, known_etag, limit_directory_entries_count, names_only, seen_directory_entries_count ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<readdirResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<readdirResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<readdirResponse> __response = __response_queue->dequeue( response_timeout_ns );directory_entries = __response->get_directory_entries();
          }
  
          virtual void readlink( const std::string& volume_name, const std::string& path, std::string& link_target_path )
          {
            ::yidl::runtime::auto_Object<readlinkRequest> __request( new readlinkRequest( volume_name, path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<readlinkResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<readlinkResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<readlinkResponse> __response = __response_queue->dequeue();link_target_path = __response->get_link_target_path();
          }
  
          virtual void readlink( const std::string& volume_name, const std::string& path, std::string& link_target_path, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<readlinkRequest> __request( new readlinkRequest( volume_name, path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<readlinkResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<readlinkResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<readlinkResponse> __response = __response_queue->dequeue( response_timeout_ns );link_target_path = __response->get_link_target_path();
          }
  
          virtual void removexattr( const std::string& volume_name, const std::string& path, const std::string& name )
          {
            ::yidl::runtime::auto_Object<removexattrRequest> __request( new removexattrRequest( volume_name, path, name ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<removexattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<removexattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<removexattrResponse> __response = __response_queue->dequeue();
          }
  
          virtual void removexattr( const std::string& volume_name, const std::string& path, const std::string& name, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<removexattrRequest> __request( new removexattrRequest( volume_name, path, name ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<removexattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<removexattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<removexattrResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials )
          {
            ::yidl::runtime::auto_Object<renameRequest> __request( new renameRequest( source_path, target_path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<renameResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<renameResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<renameResponse> __response = __response_queue->dequeue();file_credentials = __response->get_file_credentials();
          }
  
          virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<renameRequest> __request( new renameRequest( source_path, target_path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<renameResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<renameResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<renameResponse> __response = __response_queue->dequeue( response_timeout_ns );file_credentials = __response->get_file_credentials();
          }
  
          virtual void rmdir( const std::string& volume_name, const std::string& path )
          {
            ::yidl::runtime::auto_Object<rmdirRequest> __request( new rmdirRequest( volume_name, path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<rmdirResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<rmdirResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<rmdirResponse> __response = __response_queue->dequeue();
          }
  
          virtual void rmdir( const std::string& volume_name, const std::string& path, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<rmdirRequest> __request( new rmdirRequest( volume_name, path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<rmdirResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<rmdirResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<rmdirResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void setattr( const std::string& volume_name, const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set )
          {
            ::yidl::runtime::auto_Object<setattrRequest> __request( new setattrRequest( volume_name, path, stbuf, to_set ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<setattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<setattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<setattrResponse> __response = __response_queue->dequeue();
          }
  
          virtual void setattr( const std::string& volume_name, const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, uint32_t to_set, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<setattrRequest> __request( new setattrRequest( volume_name, path, stbuf, to_set ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<setattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<setattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<setattrResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void setxattr( const std::string& volume_name, const std::string& path, const std::string& name, const std::string& value, int32_t flags )
          {
            ::yidl::runtime::auto_Object<setxattrRequest> __request( new setxattrRequest( volume_name, path, name, value, flags ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<setxattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<setxattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<setxattrResponse> __response = __response_queue->dequeue();
          }
  
          virtual void setxattr( const std::string& volume_name, const std::string& path, const std::string& name, const std::string& value, int32_t flags, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<setxattrRequest> __request( new setxattrRequest( volume_name, path, name, value, flags ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<setxattrResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<setxattrResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<setxattrResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void statvfs( const std::string& volume_name, uint64_t known_etag, org::xtreemfs::interfaces::StatVFSSet& stbuf )
          {
            ::yidl::runtime::auto_Object<statvfsRequest> __request( new statvfsRequest( volume_name, known_etag ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<statvfsResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<statvfsResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<statvfsResponse> __response = __response_queue->dequeue();stbuf = __response->get_stbuf();
          }
  
          virtual void statvfs( const std::string& volume_name, uint64_t known_etag, org::xtreemfs::interfaces::StatVFSSet& stbuf, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<statvfsRequest> __request( new statvfsRequest( volume_name, known_etag ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<statvfsResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<statvfsResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<statvfsResponse> __response = __response_queue->dequeue( response_timeout_ns );stbuf = __response->get_stbuf();
          }
  
          virtual void symlink( const std::string& target_path, const std::string& link_path )
          {
            ::yidl::runtime::auto_Object<symlinkRequest> __request( new symlinkRequest( target_path, link_path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<symlinkResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<symlinkResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<symlinkResponse> __response = __response_queue->dequeue();
          }
  
          virtual void symlink( const std::string& target_path, const std::string& link_path, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<symlinkRequest> __request( new symlinkRequest( target_path, link_path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<symlinkResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<symlinkResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<symlinkResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void unlink( const std::string& volume_name, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials )
          {
            ::yidl::runtime::auto_Object<unlinkRequest> __request( new unlinkRequest( volume_name, path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<unlinkResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<unlinkResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<unlinkResponse> __response = __response_queue->dequeue();file_credentials = __response->get_file_credentials();
          }
  
          virtual void unlink( const std::string& volume_name, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<unlinkRequest> __request( new unlinkRequest( volume_name, path ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<unlinkResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<unlinkResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<unlinkResponse> __response = __response_queue->dequeue( response_timeout_ns );file_credentials = __response->get_file_credentials();
          }
  
          virtual void xtreemfs_checkpoint()
          {
            ::yidl::runtime::auto_Object<xtreemfs_checkpointRequest> __request( new xtreemfs_checkpointRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_checkpoint( uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_checkpointRequest> __request( new xtreemfs_checkpointRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_checkpointResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, const std::string& osd_uuid, std::string& bitmap )
          {
            ::yidl::runtime::auto_Object<xtreemfs_check_file_existsRequest> __request( new xtreemfs_check_file_existsRequest( volume_id, file_ids, osd_uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_check_file_existsResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_check_file_existsResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_check_file_existsResponse> __response = __response_queue->dequeue();bitmap = __response->get_bitmap();
          }
  
          virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, const std::string& osd_uuid, std::string& bitmap, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_check_file_existsRequest> __request( new xtreemfs_check_file_existsRequest( volume_id, file_ids, osd_uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_check_file_existsResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_check_file_existsResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_check_file_existsResponse> __response = __response_queue->dequeue( response_timeout_ns );bitmap = __response->get_bitmap();
          }
  
          virtual void xtreemfs_dump_database( const std::string& dump_file )
          {
            ::yidl::runtime::auto_Object<xtreemfs_dump_databaseRequest> __request( new xtreemfs_dump_databaseRequest( dump_file ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_dump_databaseResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_dump_databaseResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_dump_databaseResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_dump_database( const std::string& dump_file, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_dump_databaseRequest> __request( new xtreemfs_dump_databaseRequest( dump_file ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_dump_databaseResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_dump_databaseResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_dump_databaseResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_get_suitable_osds( const std::string& file_id, uint32_t num_osds, org::xtreemfs::interfaces::StringSet& osd_uuids )
          {
            ::yidl::runtime::auto_Object<xtreemfs_get_suitable_osdsRequest> __request( new xtreemfs_get_suitable_osdsRequest( file_id, num_osds ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_get_suitable_osdsResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_get_suitable_osdsResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_get_suitable_osdsResponse> __response = __response_queue->dequeue();osd_uuids = __response->get_osd_uuids();
          }
  
          virtual void xtreemfs_get_suitable_osds( const std::string& file_id, uint32_t num_osds, org::xtreemfs::interfaces::StringSet& osd_uuids, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_get_suitable_osdsRequest> __request( new xtreemfs_get_suitable_osdsRequest( file_id, num_osds ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_get_suitable_osdsResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_get_suitable_osdsResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_get_suitable_osdsResponse> __response = __response_queue->dequeue( response_timeout_ns );osd_uuids = __response->get_osd_uuids();
          }
  
          virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result )
          {
            ::yidl::runtime::auto_Object<xtreemfs_internal_debugRequest> __request( new xtreemfs_internal_debugRequest( operation ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_internal_debugResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_internal_debugResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_internal_debugResponse> __response = __response_queue->dequeue();result = __response->get_result();
          }
  
          virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_internal_debugRequest> __request( new xtreemfs_internal_debugRequest( operation ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_internal_debugResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_internal_debugResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_internal_debugResponse> __response = __response_queue->dequeue( response_timeout_ns );result = __response->get_result();
          }
  
          virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::StatVFSSet& volumes )
          {
            ::yidl::runtime::auto_Object<xtreemfs_lsvolRequest> __request( new xtreemfs_lsvolRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_lsvolResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_lsvolResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_lsvolResponse> __response = __response_queue->dequeue();volumes = __response->get_volumes();
          }
  
          virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::StatVFSSet& volumes, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_lsvolRequest> __request( new xtreemfs_lsvolRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_lsvolResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_lsvolResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_lsvolResponse> __response = __response_queue->dequeue( response_timeout_ns );volumes = __response->get_volumes();
          }
  
          virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::StatVFS& volume )
          {
            ::yidl::runtime::auto_Object<xtreemfs_mkvolRequest> __request( new xtreemfs_mkvolRequest( volume ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_mkvolResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_mkvolResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_mkvolResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::StatVFS& volume, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_mkvolRequest> __request( new xtreemfs_mkvolRequest( volume ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_mkvolResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_mkvolResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_mkvolResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap )
          {
            ::yidl::runtime::auto_Object<xtreemfs_renew_capabilityRequest> __request( new xtreemfs_renew_capabilityRequest( old_xcap ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_renew_capabilityResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_renew_capabilityResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_renew_capabilityResponse> __response = __response_queue->dequeue();renewed_xcap = __response->get_renewed_xcap();
          }
  
          virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_renew_capabilityRequest> __request( new xtreemfs_renew_capabilityRequest( old_xcap ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_renew_capabilityResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_renew_capabilityResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_renew_capabilityResponse> __response = __response_queue->dequeue( response_timeout_ns );renewed_xcap = __response->get_renewed_xcap();
          }
  
          virtual void xtreemfs_replication_to_master()
          {
            ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterRequest> __request( new xtreemfs_replication_to_masterRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_replication_to_master( uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterRequest> __request( new xtreemfs_replication_to_masterRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica )
          {
            ::yidl::runtime::auto_Object<xtreemfs_replica_addRequest> __request( new xtreemfs_replica_addRequest( file_id, new_replica ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replica_addResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replica_addResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replica_addResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_replica_addRequest> __request( new xtreemfs_replica_addRequest( file_id, new_replica ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replica_addResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replica_addResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replica_addResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas )
          {
            ::yidl::runtime::auto_Object<xtreemfs_replica_listRequest> __request( new xtreemfs_replica_listRequest( file_id ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replica_listResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replica_listResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replica_listResponse> __response = __response_queue->dequeue();replicas = __response->get_replicas();
          }
  
          virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_replica_listRequest> __request( new xtreemfs_replica_listRequest( file_id ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replica_listResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replica_listResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replica_listResponse> __response = __response_queue->dequeue( response_timeout_ns );replicas = __response->get_replicas();
          }
  
          virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap )
          {
            ::yidl::runtime::auto_Object<xtreemfs_replica_removeRequest> __request( new xtreemfs_replica_removeRequest( file_id, osd_uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replica_removeResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replica_removeResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replica_removeResponse> __response = __response_queue->dequeue();delete_xcap = __response->get_delete_xcap();
          }
  
          virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_replica_removeRequest> __request( new xtreemfs_replica_removeRequest( file_id, osd_uuid ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_replica_removeResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_replica_removeResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_replica_removeResponse> __response = __response_queue->dequeue( response_timeout_ns );delete_xcap = __response->get_delete_xcap();
          }
  
          virtual void xtreemfs_restore_database( const std::string& dump_file )
          {
            ::yidl::runtime::auto_Object<xtreemfs_restore_databaseRequest> __request( new xtreemfs_restore_databaseRequest( dump_file ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_restore_databaseResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_restore_databaseResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_restore_databaseResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_restore_database( const std::string& dump_file, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_restore_databaseRequest> __request( new xtreemfs_restore_databaseRequest( dump_file ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_restore_databaseResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_restore_databaseResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_restore_databaseResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size )
          {
            ::yidl::runtime::auto_Object<xtreemfs_restore_fileRequest> __request( new xtreemfs_restore_fileRequest( file_path, file_id, file_size, osd_uuid, stripe_size ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_restore_fileResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_restore_fileResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_restore_fileResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_restore_fileRequest> __request( new xtreemfs_restore_fileRequest( file_path, file_id, file_size, osd_uuid, stripe_size ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_restore_fileResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_restore_fileResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_restore_fileResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_rmvol( const std::string& volume_name )
          {
            ::yidl::runtime::auto_Object<xtreemfs_rmvolRequest> __request( new xtreemfs_rmvolRequest( volume_name ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_rmvolResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_rmvolResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_rmvolResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_rmvol( const std::string& volume_name, uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_rmvolRequest> __request( new xtreemfs_rmvolRequest( volume_name ) );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_rmvolResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_rmvolResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_rmvolResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
          virtual void xtreemfs_shutdown()
          {
            ::yidl::runtime::auto_Object<xtreemfs_shutdownRequest> __request( new xtreemfs_shutdownRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> __response = __response_queue->dequeue();
          }
  
          virtual void xtreemfs_shutdown( uint64_t response_timeout_ns )
          {
            ::yidl::runtime::auto_Object<xtreemfs_shutdownRequest> __request( new xtreemfs_shutdownRequest() );
            ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> > __response_queue( new ::yield::concurrency::ResponseQueue<xtreemfs_shutdownResponse> );
            __request->set_response_target( &__response_queue.get() );
            event_target.send( __request->inc_ref() );
            ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> __response = __response_queue->dequeue( response_timeout_ns );
          }
  
      private:
        ::yield::concurrency::EventTarget& event_target;
      };
      };
    };
  };
#endif
