#ifndef _28746533285_H
#define _28746533285_H

#include "constants.h"
#include "mrc_osd_types.h"
#include <vector>
#include "yield/arch.h"
#include "yield/platform.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
  
      class Context : public YIELD::Serializable
      {
      public:
        Context() { }
        Context( const std::string& user_id, const org::xtreemfs::interfaces::StringSet& group_ids ) : user_id( user_id ), group_ids( group_ids ) { }
        Context( const char* user_id, size_t user_id_len, const org::xtreemfs::interfaces::StringSet& group_ids ) : user_id( user_id, user_id_len ), group_ids( group_ids ) { }
        virtual ~Context() { }
  
        void set_user_id( const std::string& user_id ) { set_user_id( user_id.c_str(), user_id.size() ); }
        void set_user_id( const char* user_id, size_t user_id_len = 0 ) { this->user_id.assign( user_id, ( user_id_len != 0 ) ? user_id_len : std::strlen( user_id ) ); }
        const std::string& get_user_id() const { return user_id; }
        void set_group_ids( const org::xtreemfs::interfaces::StringSet&  group_ids ) { this->group_ids = group_ids; }
        const org::xtreemfs::interfaces::StringSet& get_group_ids() const { return group_ids; }
  
        bool operator==( const Context& other ) const { return user_id == other.user_id && group_ids == other.group_ids; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::Context", 2273805342UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "group_ids" ), &group_ids ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "group_ids" ), group_ids ); }
  
      protected:
        std::string user_id;
        org::xtreemfs::interfaces::StringSet group_ids;
      };
  
      class stat_ : public YIELD::Serializable
      {
      public:
        stat_() : mode( 0 ), nlink( 0 ), unused_uid( 0 ), unused_gid( 0 ), unused_dev( 0 ), size( 0 ), atime( 0 ), mtime( 0 ), ctime( 0 ), object_type( 0 ), truncate_epoch( 0 ), attributes( 0 ) { }
        stat_( uint32_t mode, uint32_t nlink, uint32_t unused_uid, uint32_t unused_gid, int16_t unused_dev, uint64_t size, uint64_t atime, uint64_t mtime, uint64_t ctime, const std::string& user_id, const std::string& group_id, const std::string& file_id, const std::string& link_target, uint8_t object_type, uint32_t truncate_epoch, uint32_t attributes ) : mode( mode ), nlink( nlink ), unused_uid( unused_uid ), unused_gid( unused_gid ), unused_dev( unused_dev ), size( size ), atime( atime ), mtime( mtime ), ctime( ctime ), user_id( user_id ), group_id( group_id ), file_id( file_id ), link_target( link_target ), object_type( object_type ), truncate_epoch( truncate_epoch ), attributes( attributes ) { }
        stat_( uint32_t mode, uint32_t nlink, uint32_t unused_uid, uint32_t unused_gid, int16_t unused_dev, uint64_t size, uint64_t atime, uint64_t mtime, uint64_t ctime, const char* user_id, size_t user_id_len, const char* group_id, size_t group_id_len, const char* file_id, size_t file_id_len, const char* link_target, size_t link_target_len, uint8_t object_type, uint32_t truncate_epoch, uint32_t attributes ) : mode( mode ), nlink( nlink ), unused_uid( unused_uid ), unused_gid( unused_gid ), unused_dev( unused_dev ), size( size ), atime( atime ), mtime( mtime ), ctime( ctime ), user_id( user_id, user_id_len ), group_id( group_id, group_id_len ), file_id( file_id, file_id_len ), link_target( link_target, link_target_len ), object_type( object_type ), truncate_epoch( truncate_epoch ), attributes( attributes ) { }
        virtual ~stat_() { }
  
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
        void set_nlink( uint32_t nlink ) { this->nlink = nlink; }
        uint32_t get_nlink() const { return nlink; }
        void set_unused_uid( uint32_t unused_uid ) { this->unused_uid = unused_uid; }
        uint32_t get_unused_uid() const { return unused_uid; }
        void set_unused_gid( uint32_t unused_gid ) { this->unused_gid = unused_gid; }
        uint32_t get_unused_gid() const { return unused_gid; }
        void set_unused_dev( int16_t unused_dev ) { this->unused_dev = unused_dev; }
        int16_t get_unused_dev() const { return unused_dev; }
        void set_size( uint64_t size ) { this->size = size; }
        uint64_t get_size() const { return size; }
        void set_atime( uint64_t atime ) { this->atime = atime; }
        uint64_t get_atime() const { return atime; }
        void set_mtime( uint64_t mtime ) { this->mtime = mtime; }
        uint64_t get_mtime() const { return mtime; }
        void set_ctime( uint64_t ctime ) { this->ctime = ctime; }
        uint64_t get_ctime() const { return ctime; }
        void set_user_id( const std::string& user_id ) { set_user_id( user_id.c_str(), user_id.size() ); }
        void set_user_id( const char* user_id, size_t user_id_len = 0 ) { this->user_id.assign( user_id, ( user_id_len != 0 ) ? user_id_len : std::strlen( user_id ) ); }
        const std::string& get_user_id() const { return user_id; }
        void set_group_id( const std::string& group_id ) { set_group_id( group_id.c_str(), group_id.size() ); }
        void set_group_id( const char* group_id, size_t group_id_len = 0 ) { this->group_id.assign( group_id, ( group_id_len != 0 ) ? group_id_len : std::strlen( group_id ) ); }
        const std::string& get_group_id() const { return group_id; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_link_target( const std::string& link_target ) { set_link_target( link_target.c_str(), link_target.size() ); }
        void set_link_target( const char* link_target, size_t link_target_len = 0 ) { this->link_target.assign( link_target, ( link_target_len != 0 ) ? link_target_len : std::strlen( link_target ) ); }
        const std::string& get_link_target() const { return link_target; }
        void set_object_type( uint8_t object_type ) { this->object_type = object_type; }
        uint8_t get_object_type() const { return object_type; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        void set_attributes( uint32_t attributes ) { this->attributes = attributes; }
        uint32_t get_attributes() const { return attributes; }
  
        bool operator==( const stat_& other ) const { return mode == other.mode && nlink == other.nlink && unused_uid == other.unused_uid && unused_gid == other.unused_gid && unused_dev == other.unused_dev && size == other.size && atime == other.atime && mtime == other.mtime && ctime == other.ctime && user_id == other.user_id && group_id == other.group_id && file_id == other.file_id && link_target == other.link_target && object_type == other.object_type && truncate_epoch == other.truncate_epoch && attributes == other.attributes; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::stat_", 2292404502UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); nlink = input_stream.readUint32( YIELD::StructuredStream::Declaration( "nlink" ) ); unused_uid = input_stream.readUint32( YIELD::StructuredStream::Declaration( "unused_uid" ) ); unused_gid = input_stream.readUint32( YIELD::StructuredStream::Declaration( "unused_gid" ) ); unused_dev = input_stream.readInt16( YIELD::StructuredStream::Declaration( "unused_dev" ) ); size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "size" ) ); atime = input_stream.readUint64( YIELD::StructuredStream::Declaration( "atime" ) ); mtime = input_stream.readUint64( YIELD::StructuredStream::Declaration( "mtime" ) ); ctime = input_stream.readUint64( YIELD::StructuredStream::Declaration( "ctime" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "group_id" ), group_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "link_target" ), link_target ); object_type = input_stream.readUint8( YIELD::StructuredStream::Declaration( "object_type" ) ); truncate_epoch = input_stream.readUint32( YIELD::StructuredStream::Declaration( "truncate_epoch" ) ); attributes = input_stream.readUint32( YIELD::StructuredStream::Declaration( "attributes" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "nlink" ), nlink ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "unused_uid" ), unused_uid ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "unused_gid" ), unused_gid ); output_stream.writeInt16( YIELD::StructuredStream::Declaration( "unused_dev" ), unused_dev ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "size" ), size ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "atime" ), atime ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "mtime" ), mtime ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "ctime" ), ctime ); output_stream.writeString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "group_id" ), group_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "link_target" ), link_target ); output_stream.writeUint8( YIELD::StructuredStream::Declaration( "object_type" ), object_type ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "truncate_epoch" ), truncate_epoch ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "attributes" ), attributes ); }
  
      protected:
        uint32_t mode;
        uint32_t nlink;
        uint32_t unused_uid;
        uint32_t unused_gid;
        int16_t unused_dev;
        uint64_t size;
        uint64_t atime;
        uint64_t mtime;
        uint64_t ctime;
        std::string user_id;
        std::string group_id;
        std::string file_id;
        std::string link_target;
        uint8_t object_type;
        uint32_t truncate_epoch;
        uint32_t attributes;
      };
  
      class DirectoryEntry : public YIELD::Serializable
      {
      public:
        DirectoryEntry() { }
        DirectoryEntry( const std::string& entry_name, const org::xtreemfs::interfaces::stat_& stbuf, const std::string& link_target ) : entry_name( entry_name ), stbuf( stbuf ), link_target( link_target ) { }
        DirectoryEntry( const char* entry_name, size_t entry_name_len, const org::xtreemfs::interfaces::stat_& stbuf, const char* link_target, size_t link_target_len ) : entry_name( entry_name, entry_name_len ), stbuf( stbuf ), link_target( link_target, link_target_len ) { }
        virtual ~DirectoryEntry() { }
  
        void set_entry_name( const std::string& entry_name ) { set_entry_name( entry_name.c_str(), entry_name.size() ); }
        void set_entry_name( const char* entry_name, size_t entry_name_len = 0 ) { this->entry_name.assign( entry_name, ( entry_name_len != 0 ) ? entry_name_len : std::strlen( entry_name ) ); }
        const std::string& get_entry_name() const { return entry_name; }
        void set_stbuf( const org::xtreemfs::interfaces::stat_&  stbuf ) { this->stbuf = stbuf; }
        const org::xtreemfs::interfaces::stat_& get_stbuf() const { return stbuf; }
        void set_link_target( const std::string& link_target ) { set_link_target( link_target.c_str(), link_target.size() ); }
        void set_link_target( const char* link_target, size_t link_target_len = 0 ) { this->link_target.assign( link_target, ( link_target_len != 0 ) ? link_target_len : std::strlen( link_target ) ); }
        const std::string& get_link_target() const { return link_target; }
  
        bool operator==( const DirectoryEntry& other ) const { return entry_name == other.entry_name && stbuf == other.stbuf && link_target == other.link_target; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::DirectoryEntry", 2507394841UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "entry_name" ), entry_name ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::stat_", "stbuf" ), &stbuf ); input_stream.readString( YIELD::StructuredStream::Declaration( "link_target" ), link_target ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "entry_name" ), entry_name ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::stat_", "stbuf" ), stbuf ); output_stream.writeString( YIELD::StructuredStream::Declaration( "link_target" ), link_target ); }
  
      protected:
        std::string entry_name;
        org::xtreemfs::interfaces::stat_ stbuf;
        std::string link_target;
      };
  
      class DirectoryEntrySet : public std::vector<org::xtreemfs::interfaces::DirectoryEntry>, public YIELD::Serializable
      {
      public:
        DirectoryEntrySet() { }
        DirectoryEntrySet( size_type size ) : std::vector<org::xtreemfs::interfaces::DirectoryEntry>( size ) { }
        virtual ~DirectoryEntrySet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::DirectoryEntrySet", 3851275498UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::DirectoryEntry item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::DirectoryEntry", "item" ), &item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::DirectoryEntry", "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::DirectoryEntry>::size(); }
      };
  
      class statfs_ : public YIELD::Serializable
      {
      public:
        statfs_() : bsize( 0 ), bfree( 0 ), namelen( 0 ) { }
        statfs_( uint32_t bsize, uint64_t bfree, const std::string& fsid, uint32_t namelen ) : bsize( bsize ), bfree( bfree ), fsid( fsid ), namelen( namelen ) { }
        statfs_( uint32_t bsize, uint64_t bfree, const char* fsid, size_t fsid_len, uint32_t namelen ) : bsize( bsize ), bfree( bfree ), fsid( fsid, fsid_len ), namelen( namelen ) { }
        virtual ~statfs_() { }
  
        void set_bsize( uint32_t bsize ) { this->bsize = bsize; }
        uint32_t get_bsize() const { return bsize; }
        void set_bfree( uint64_t bfree ) { this->bfree = bfree; }
        uint64_t get_bfree() const { return bfree; }
        void set_fsid( const std::string& fsid ) { set_fsid( fsid.c_str(), fsid.size() ); }
        void set_fsid( const char* fsid, size_t fsid_len = 0 ) { this->fsid.assign( fsid, ( fsid_len != 0 ) ? fsid_len : std::strlen( fsid ) ); }
        const std::string& get_fsid() const { return fsid; }
        void set_namelen( uint32_t namelen ) { this->namelen = namelen; }
        uint32_t get_namelen() const { return namelen; }
  
        bool operator==( const statfs_& other ) const { return bsize == other.bsize && bfree == other.bfree && fsid == other.fsid && namelen == other.namelen; }
  
        // YIELD::RTTI
        TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::statfs_", 1274084092UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { bsize = input_stream.readUint32( YIELD::StructuredStream::Declaration( "bsize" ) ); bfree = input_stream.readUint64( YIELD::StructuredStream::Declaration( "bfree" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "fsid" ), fsid ); namelen = input_stream.readUint32( YIELD::StructuredStream::Declaration( "namelen" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "bsize" ), bsize ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "bfree" ), bfree ); output_stream.writeString( YIELD::StructuredStream::Declaration( "fsid" ), fsid ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "namelen" ), namelen ); }
  
      protected:
        uint32_t bsize;
        uint64_t bfree;
        std::string fsid;
        uint32_t namelen;
      };
  
      class FileCredentialsSet : public std::vector<org::xtreemfs::interfaces::FileCredentials>, public YIELD::Serializable
      {
      public:
        FileCredentialsSet() { }
        FileCredentialsSet( size_type size ) : std::vector<org::xtreemfs::interfaces::FileCredentials>( size ) { }
        virtual ~FileCredentialsSet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::FileCredentialsSet", 1629291456UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::FileCredentials item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "item" ), &item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::FileCredentials>::size(); }
      };
  
      class StripingPolicySet : public std::vector<org::xtreemfs::interfaces::StripingPolicy>, public YIELD::Serializable
      {
      public:
        StripingPolicySet() { }
        StripingPolicySet( size_type size ) : std::vector<org::xtreemfs::interfaces::StripingPolicy>( size ) { }
        virtual ~StripingPolicySet() { }
  
        // YIELD::RTTI
        TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::StripingPolicySet", 2815459990UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::StripingPolicy item; input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StripingPolicy", "item" ), &item ); push_back( item ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type i_max = size(); for ( size_type i = 0; i < i_max; i++ ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StripingPolicy", "item" ), ( *this )[size() - 1] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::StripingPolicy>::size(); }
      };
  
  
  
      #ifndef ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS YIELD::EventHandler
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
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS YIELD::Request
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
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS YIELD::Response
      #endif
      #endif
  
      #ifndef ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_EVENT_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_EVENT_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_EVENT_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_EVENT_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_EVENT_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_EVENT_PARENT_CLASS ORG_EXCEPTION_EVENT_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_EVENT_PARENT_CLASS YIELD::ExceptionEvent
      #endif
      #endif
  
  
  
      class MRCInterface : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS
      {
      public:
        MRCInterface() { }
        virtual ~MRCInterface() { }
  
  
      const static uint32_t DEFAULT_ONCRPC_PORT = 32636;
      const static uint32_t DEFAULT_ONCRPCS_PORT = 32636;
      const static uint32_t DEFAULT_HTTP_PORT = 30636;
  
      virtual void admin_shutdown( const std::string& password ) { admin_shutdown( password, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_shutdown( const std::string& password, const char* send_target ) { admin_shutdown( password, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_shutdown( const std::string& password, YIELD::timeout_ns_t send_timeout_ns ) { admin_shutdown( password, NULL, send_timeout_ns ); }
        virtual void admin_shutdown( const std::string& password, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { admin_shutdownSyncRequest* __req = new admin_shutdownSyncRequest( password, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); admin_shutdownResponse& __resp = ( admin_shutdownResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void admin_checkpoint( const std::string& password ) { admin_checkpoint( password, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_checkpoint( const std::string& password, const char* send_target ) { admin_checkpoint( password, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_checkpoint( const std::string& password, YIELD::timeout_ns_t send_timeout_ns ) { admin_checkpoint( password, NULL, send_timeout_ns ); }
        virtual void admin_checkpoint( const std::string& password, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { admin_checkpointSyncRequest* __req = new admin_checkpointSyncRequest( password, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); admin_checkpointResponse& __resp = ( admin_checkpointResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void admin_dump_database( const std::string& password, const std::string& dump_file ) { admin_dump_database( password, dump_file, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_dump_database( const std::string& password, const std::string& dump_file, const char* send_target ) { admin_dump_database( password, dump_file, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_dump_database( const std::string& password, const std::string& dump_file, YIELD::timeout_ns_t send_timeout_ns ) { admin_dump_database( password, dump_file, NULL, send_timeout_ns ); }
        virtual void admin_dump_database( const std::string& password, const std::string& dump_file, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { admin_dump_databaseSyncRequest* __req = new admin_dump_databaseSyncRequest( password, dump_file, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); admin_dump_databaseResponse& __resp = ( admin_dump_databaseResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void admin_restore_database( const std::string& password, const std::string& dump_file ) { admin_restore_database( password, dump_file, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_restore_database( const std::string& password, const std::string& dump_file, const char* send_target ) { admin_restore_database( password, dump_file, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void admin_restore_database( const std::string& password, const std::string& dump_file, YIELD::timeout_ns_t send_timeout_ns ) { admin_restore_database( password, dump_file, NULL, send_timeout_ns ); }
        virtual void admin_restore_database( const std::string& password, const std::string& dump_file, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { admin_restore_databaseSyncRequest* __req = new admin_restore_databaseSyncRequest( password, dump_file, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); admin_restore_databaseResponse& __resp = ( admin_restore_databaseResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual bool access( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { return access( context, path, mode, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual bool access( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, const char* send_target ) { return access( context, path, mode, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual bool access( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t send_timeout_ns ) { return access( context, path, mode, NULL, send_timeout_ns ); }
        virtual bool access( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { accessSyncRequest* __req = new accessSyncRequest( context, path, mode, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); accessResponse& __resp = ( accessResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); bool _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void chmod( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { chmod( context, path, mode, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void chmod( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, const char* send_target ) { chmod( context, path, mode, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void chmod( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t send_timeout_ns ) { chmod( context, path, mode, NULL, send_timeout_ns ); }
        virtual void chmod( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { chmodSyncRequest* __req = new chmodSyncRequest( context, path, mode, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); chmodResponse& __resp = ( chmodResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void chown( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& userId, const std::string& groupId ) { chown( context, path, userId, groupId, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void chown( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& userId, const std::string& groupId, const char* send_target ) { chown( context, path, userId, groupId, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void chown( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& userId, const std::string& groupId, YIELD::timeout_ns_t send_timeout_ns ) { chown( context, path, userId, groupId, NULL, send_timeout_ns ); }
        virtual void chown( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& userId, const std::string& groupId, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { chownSyncRequest* __req = new chownSyncRequest( context, path, userId, groupId, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); chownResponse& __resp = ( chownResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void create( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { create( context, path, mode, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void create( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, const char* send_target ) { create( context, path, mode, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void create( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t send_timeout_ns ) { create( context, path, mode, NULL, send_timeout_ns ); }
        virtual void create( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { createSyncRequest* __req = new createSyncRequest( context, path, mode, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); createResponse& __resp = ( createResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void getattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::stat_& stbuf ) { getattr( context, path, stbuf, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void getattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::stat_& stbuf, const char* send_target ) { getattr( context, path, stbuf, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void getattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::stat_& stbuf, YIELD::timeout_ns_t send_timeout_ns ) { getattr( context, path, stbuf, NULL, send_timeout_ns ); }
        virtual void getattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::stat_& stbuf, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { getattrSyncRequest* __req = new getattrSyncRequest( context, path, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); getattrResponse& __resp = ( getattrResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); stbuf = __resp.get_stbuf(); YIELD::SharedObject::decRef( __resp ); }
        virtual std::string getxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name ) { return getxattr( context, path, name, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual std::string getxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const char* send_target ) { return getxattr( context, path, name, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual std::string getxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, YIELD::timeout_ns_t send_timeout_ns ) { return getxattr( context, path, name, NULL, send_timeout_ns ); }
        virtual std::string getxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { getxattrSyncRequest* __req = new getxattrSyncRequest( context, path, name, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); getxattrResponse& __resp = ( getxattrResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); std::string _return_value = __resp.get__return_value(); YIELD::SharedObject::decRef( __resp ); return _return_value; }
        virtual void link( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path ) { link( context, target_path, link_path, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void link( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, const char* send_target ) { link( context, target_path, link_path, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void link( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, YIELD::timeout_ns_t send_timeout_ns ) { link( context, target_path, link_path, NULL, send_timeout_ns ); }
        virtual void link( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { linkSyncRequest* __req = new linkSyncRequest( context, target_path, link_path, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); linkResponse& __resp = ( linkResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void listxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { listxattr( context, path, names, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void listxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::StringSet& names, const char* send_target ) { listxattr( context, path, names, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void listxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::timeout_ns_t send_timeout_ns ) { listxattr( context, path, names, NULL, send_timeout_ns ); }
        virtual void listxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::StringSet& names, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { listxattrSyncRequest* __req = new listxattrSyncRequest( context, path, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); listxattrResponse& __resp = ( listxattrResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); names = __resp.get_names(); YIELD::SharedObject::decRef( __resp ); }
        virtual void mkdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { mkdir( context, path, mode, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void mkdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, const char* send_target ) { mkdir( context, path, mode, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void mkdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t send_timeout_ns ) { mkdir( context, path, mode, NULL, send_timeout_ns ); }
        virtual void mkdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { mkdirSyncRequest* __req = new mkdirSyncRequest( context, path, mode, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); mkdirResponse& __resp = ( mkdirResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void mkvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy ) { mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void mkvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy, const char* send_target ) { mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void mkvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy, YIELD::timeout_ns_t send_timeout_ns ) { mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy, NULL, send_timeout_ns ); }
        virtual void mkvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { mkvolSyncRequest* __req = new mkvolSyncRequest( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); mkvolResponse& __resp = ( mkvolResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void open( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& credentials ) { open( context, path, flags, mode, credentials, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void open( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& credentials, const char* send_target ) { open( context, path, flags, mode, credentials, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void open( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& credentials, YIELD::timeout_ns_t send_timeout_ns ) { open( context, path, flags, mode, credentials, NULL, send_timeout_ns ); }
        virtual void open( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& credentials, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { openSyncRequest* __req = new openSyncRequest( context, path, flags, mode, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); openResponse& __resp = ( openResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); credentials = __resp.get_credentials(); YIELD::SharedObject::decRef( __resp ); }
        virtual void readdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) { readdir( context, path, directory_entries, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void readdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, const char* send_target ) { readdir( context, path, directory_entries, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void readdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, YIELD::timeout_ns_t send_timeout_ns ) { readdir( context, path, directory_entries, NULL, send_timeout_ns ); }
        virtual void readdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { readdirSyncRequest* __req = new readdirSyncRequest( context, path, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); readdirResponse& __resp = ( readdirResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); directory_entries = __resp.get_directory_entries(); YIELD::SharedObject::decRef( __resp ); }
        virtual void removexattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name ) { removexattr( context, path, name, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void removexattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const char* send_target ) { removexattr( context, path, name, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void removexattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, YIELD::timeout_ns_t send_timeout_ns ) { removexattr( context, path, name, NULL, send_timeout_ns ); }
        virtual void removexattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { removexattrSyncRequest* __req = new removexattrSyncRequest( context, path, name, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); removexattrResponse& __resp = ( removexattrResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void rename( const org::xtreemfs::interfaces::Context& context, const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& credentials ) { rename( context, source_path, target_path, credentials, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rename( const org::xtreemfs::interfaces::Context& context, const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& credentials, const char* send_target ) { rename( context, source_path, target_path, credentials, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rename( const org::xtreemfs::interfaces::Context& context, const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& credentials, YIELD::timeout_ns_t send_timeout_ns ) { rename( context, source_path, target_path, credentials, NULL, send_timeout_ns ); }
        virtual void rename( const org::xtreemfs::interfaces::Context& context, const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& credentials, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { renameSyncRequest* __req = new renameSyncRequest( context, source_path, target_path, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); renameResponse& __resp = ( renameResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); credentials = __resp.get_credentials(); YIELD::SharedObject::decRef( __resp ); }
        virtual void rmdir( const org::xtreemfs::interfaces::Context& context, const std::string& path ) { rmdir( context, path, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rmdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, const char* send_target ) { rmdir( context, path, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rmdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t send_timeout_ns ) { rmdir( context, path, NULL, send_timeout_ns ); }
        virtual void rmdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { rmdirSyncRequest* __req = new rmdirSyncRequest( context, path, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); rmdirResponse& __resp = ( rmdirResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void rmvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name ) { rmvol( context, password, volume_name, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rmvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, const char* send_target ) { rmvol( context, password, volume_name, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rmvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, YIELD::timeout_ns_t send_timeout_ns ) { rmvol( context, password, volume_name, NULL, send_timeout_ns ); }
        virtual void rmvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { rmvolSyncRequest* __req = new rmvolSyncRequest( context, password, volume_name, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); rmvolResponse& __resp = ( rmvolResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void setattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf ) { setattr( context, path, stbuf, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void setattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf, const char* send_target ) { setattr( context, path, stbuf, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void setattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf, YIELD::timeout_ns_t send_timeout_ns ) { setattr( context, path, stbuf, NULL, send_timeout_ns ); }
        virtual void setattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { setattrSyncRequest* __req = new setattrSyncRequest( context, path, stbuf, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); setattrResponse& __resp = ( setattrResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void setxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const std::string& value, int32_t flags ) { setxattr( context, path, name, value, flags, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void setxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const std::string& value, int32_t flags, const char* send_target ) { setxattr( context, path, name, value, flags, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void setxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const std::string& value, int32_t flags, YIELD::timeout_ns_t send_timeout_ns ) { setxattr( context, path, name, value, flags, NULL, send_timeout_ns ); }
        virtual void setxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const std::string& value, int32_t flags, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { setxattrSyncRequest* __req = new setxattrSyncRequest( context, path, name, value, flags, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); setxattrResponse& __resp = ( setxattrResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void statfs( const org::xtreemfs::interfaces::Context& context, const std::string& volume_name, org::xtreemfs::interfaces::statfs_& statfsbuf ) { statfs( context, volume_name, statfsbuf, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void statfs( const org::xtreemfs::interfaces::Context& context, const std::string& volume_name, org::xtreemfs::interfaces::statfs_& statfsbuf, const char* send_target ) { statfs( context, volume_name, statfsbuf, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void statfs( const org::xtreemfs::interfaces::Context& context, const std::string& volume_name, org::xtreemfs::interfaces::statfs_& statfsbuf, YIELD::timeout_ns_t send_timeout_ns ) { statfs( context, volume_name, statfsbuf, NULL, send_timeout_ns ); }
        virtual void statfs( const org::xtreemfs::interfaces::Context& context, const std::string& volume_name, org::xtreemfs::interfaces::statfs_& statfsbuf, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { statfsSyncRequest* __req = new statfsSyncRequest( context, volume_name, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); statfsResponse& __resp = ( statfsResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); statfsbuf = __resp.get_statfsbuf(); YIELD::SharedObject::decRef( __resp ); }
        virtual void symlink( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path ) { symlink( context, target_path, link_path, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void symlink( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, const char* send_target ) { symlink( context, target_path, link_path, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void symlink( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, YIELD::timeout_ns_t send_timeout_ns ) { symlink( context, target_path, link_path, NULL, send_timeout_ns ); }
        virtual void symlink( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { symlinkSyncRequest* __req = new symlinkSyncRequest( context, target_path, link_path, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); symlinkResponse& __resp = ( symlinkResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void unlink( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& credentials ) { unlink( context, path, credentials, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void unlink( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& credentials, const char* send_target ) { unlink( context, path, credentials, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void unlink( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& credentials, YIELD::timeout_ns_t send_timeout_ns ) { unlink( context, path, credentials, NULL, send_timeout_ns ); }
        virtual void unlink( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& credentials, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { unlinkSyncRequest* __req = new unlinkSyncRequest( context, path, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); unlinkResponse& __resp = ( unlinkResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); credentials = __resp.get_credentials(); YIELD::SharedObject::decRef( __resp ); }
        virtual void utime( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime ) { utime( context, path, ctime, atime, mtime, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void utime( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime, const char* send_target ) { utime( context, path, ctime, atime, mtime, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void utime( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime, YIELD::timeout_ns_t send_timeout_ns ) { utime( context, path, ctime, atime, mtime, NULL, send_timeout_ns ); }
        virtual void utime( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { utimeSyncRequest* __req = new utimeSyncRequest( context, path, ctime, atime, mtime, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); utimeResponse& __resp = ( utimeResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap ) { xtreemfs_check_file_exists( volume_id, file_ids, bitmap, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap, const char* send_target ) { xtreemfs_check_file_exists( volume_id, file_ids, bitmap, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_check_file_exists( volume_id, file_ids, bitmap, NULL, send_timeout_ns ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_check_file_existsSyncRequest* __req = new xtreemfs_check_file_existsSyncRequest( volume_id, file_ids, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_check_file_existsResponse& __resp = ( xtreemfs_check_file_existsResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); bitmap = __resp.get_bitmap(); YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids ) { xtreemfs_get_suitable_osds( file_id, osd_uuids, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids, const char* send_target ) { xtreemfs_get_suitable_osds( file_id, osd_uuids, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_get_suitable_osds( file_id, osd_uuids, NULL, send_timeout_ns ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_get_suitable_osdsSyncRequest* __req = new xtreemfs_get_suitable_osdsSyncRequest( file_id, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_get_suitable_osdsResponse& __resp = ( xtreemfs_get_suitable_osdsResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); osd_uuids = __resp.get_osd_uuids(); YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, const char* send_target ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, NULL, send_timeout_ns ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_renew_capabilitySyncRequest* __req = new xtreemfs_renew_capabilitySyncRequest( old_xcap, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_renew_capabilityResponse& __resp = ( xtreemfs_renew_capabilityResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req ); renewed_xcap = __resp.get_renewed_xcap(); YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_replica_add( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) { xtreemfs_replica_add( context, file_id, new_replica, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_add( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, const char* send_target ) { xtreemfs_replica_add( context, file_id, new_replica, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_add( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_replica_add( context, file_id, new_replica, NULL, send_timeout_ns ); }
        virtual void xtreemfs_replica_add( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_replica_addSyncRequest* __req = new xtreemfs_replica_addSyncRequest( context, file_id, new_replica, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_replica_addResponse& __resp = ( xtreemfs_replica_addResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_replica_remove( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const std::string& osd_uuid ) { xtreemfs_replica_remove( context, file_id, osd_uuid, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_remove( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const std::string& osd_uuid, const char* send_target ) { xtreemfs_replica_remove( context, file_id, osd_uuid, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_remove( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const std::string& osd_uuid, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_replica_remove( context, file_id, osd_uuid, NULL, send_timeout_ns ); }
        virtual void xtreemfs_replica_remove( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const std::string& osd_uuid, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_replica_removeSyncRequest* __req = new xtreemfs_replica_removeSyncRequest( context, file_id, osd_uuid, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_replica_removeResponse& __resp = ( xtreemfs_replica_removeResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, const char* send_target ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, NULL, send_timeout_ns ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_restore_fileSyncRequest* __req = new xtreemfs_restore_fileSyncRequest( file_path, file_id, file_size, osd_uuid, stripe_size, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_restore_fileResponse& __resp = ( xtreemfs_restore_fileResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { xtreemfs_update_file_size( xcap, osd_write_response, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, const char* send_target ) { xtreemfs_update_file_size( xcap, osd_write_response, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_update_file_size( xcap, osd_write_response, NULL, send_timeout_ns ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, const char* send_target, YIELD::timeout_ns_t send_timeout_ns ) { xtreemfs_update_file_sizeSyncRequest* __req = new xtreemfs_update_file_sizeSyncRequest( xcap, osd_write_response, send_timeout_ns ); this->send( YIELD::SharedObject::incRef( *__req ) ); xtreemfs_update_file_sizeResponse& __resp = ( xtreemfs_update_file_sizeResponse& )__req->waitForDefaultResponse( send_timeout_ns ); YIELD::SharedObject::decRef( *__req );  YIELD::SharedObject::decRef( __resp ); }  // Request/response pair Event type definitions for the operations in MRCInterface
  
      class admin_shutdownResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        admin_shutdownResponse() { }
        virtual ~admin_shutdownResponse() { }
  
        bool operator==( const admin_shutdownResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::admin_shutdownResponse", 1117889079UL );
  
      };
  
      class admin_shutdownRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        admin_shutdownRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_shutdownRequest( const std::string& password, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password ) { }
        admin_shutdownRequest( const char* password, size_t password_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password, password_len ) { }
        virtual ~admin_shutdownRequest() { }
  
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }
  
        bool operator==( const admin_shutdownRequest& other ) const { return password == other.password; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::admin_shutdownRequest", 440700645UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 50; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1117889079UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::admin_shutdownResponse; }
  
  
      protected:
        std::string password;
      };
  
      class admin_shutdownSyncRequest : public admin_shutdownRequest
      {
      public:
        admin_shutdownSyncRequest() : admin_shutdownRequest( std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_shutdownSyncRequest( const std::string& password, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_shutdownRequest( password, response_timeout_ns ) { }
        admin_shutdownSyncRequest( const char* password, size_t password_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_shutdownRequest( password, password_len, response_timeout_ns ) { }
        virtual ~admin_shutdownSyncRequest() { }
  
        bool operator==( const admin_shutdownSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::admin_shutdownResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class admin_checkpointResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        admin_checkpointResponse() { }
        virtual ~admin_checkpointResponse() { }
  
        bool operator==( const admin_checkpointResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::admin_checkpointResponse", 695599363UL );
  
      };
  
      class admin_checkpointRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        admin_checkpointRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_checkpointRequest( const std::string& password, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password ) { }
        admin_checkpointRequest( const char* password, size_t password_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password, password_len ) { }
        virtual ~admin_checkpointRequest() { }
  
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }
  
        bool operator==( const admin_checkpointRequest& other ) const { return password == other.password; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::admin_checkpointRequest", 1430126549UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 51; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 695599363UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::admin_checkpointResponse; }
  
  
      protected:
        std::string password;
      };
  
      class admin_checkpointSyncRequest : public admin_checkpointRequest
      {
      public:
        admin_checkpointSyncRequest() : admin_checkpointRequest( std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_checkpointSyncRequest( const std::string& password, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_checkpointRequest( password, response_timeout_ns ) { }
        admin_checkpointSyncRequest( const char* password, size_t password_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_checkpointRequest( password, password_len, response_timeout_ns ) { }
        virtual ~admin_checkpointSyncRequest() { }
  
        bool operator==( const admin_checkpointSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::admin_checkpointResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class admin_dump_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        admin_dump_databaseResponse() { }
        virtual ~admin_dump_databaseResponse() { }
  
        bool operator==( const admin_dump_databaseResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::admin_dump_databaseResponse", 976095961UL );
  
      };
  
      class admin_dump_databaseRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        admin_dump_databaseRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_dump_databaseRequest( const std::string& password, const std::string& dump_file, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password ), dump_file( dump_file ) { }
        admin_dump_databaseRequest( const char* password, size_t password_len, const char* dump_file, size_t dump_file_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password, password_len ), dump_file( dump_file, dump_file_len ) { }
        virtual ~admin_dump_databaseRequest() { }
  
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }
        void set_dump_file( const std::string& dump_file ) { set_dump_file( dump_file.c_str(), dump_file.size() ); }
        void set_dump_file( const char* dump_file, size_t dump_file_len = 0 ) { this->dump_file.assign( dump_file, ( dump_file_len != 0 ) ? dump_file_len : std::strlen( dump_file ) ); }
        const std::string& get_dump_file() const { return dump_file; }
  
        bool operator==( const admin_dump_databaseRequest& other ) const { return password == other.password && dump_file == other.dump_file; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::admin_dump_databaseRequest", 3734644125UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); input_stream.readString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); output_stream.writeString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 52; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 976095961UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::admin_dump_databaseResponse; }
  
  
      protected:
        std::string password;
        std::string dump_file;
      };
  
      class admin_dump_databaseSyncRequest : public admin_dump_databaseRequest
      {
      public:
        admin_dump_databaseSyncRequest() : admin_dump_databaseRequest( std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_dump_databaseSyncRequest( const std::string& password, const std::string& dump_file, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_dump_databaseRequest( password, dump_file, response_timeout_ns ) { }
        admin_dump_databaseSyncRequest( const char* password, size_t password_len, const char* dump_file, size_t dump_file_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_dump_databaseRequest( password, password_len, dump_file, dump_file_len, response_timeout_ns ) { }
        virtual ~admin_dump_databaseSyncRequest() { }
  
        bool operator==( const admin_dump_databaseSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::admin_dump_databaseResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class admin_restore_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        admin_restore_databaseResponse() { }
        virtual ~admin_restore_databaseResponse() { }
  
        bool operator==( const admin_restore_databaseResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::admin_restore_databaseResponse", 894572093UL );
  
      };
  
      class admin_restore_databaseRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        admin_restore_databaseRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_restore_databaseRequest( const std::string& password, const std::string& dump_file, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password ), dump_file( dump_file ) { }
        admin_restore_databaseRequest( const char* password, size_t password_len, const char* dump_file, size_t dump_file_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), password( password, password_len ), dump_file( dump_file, dump_file_len ) { }
        virtual ~admin_restore_databaseRequest() { }
  
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }
        void set_dump_file( const std::string& dump_file ) { set_dump_file( dump_file.c_str(), dump_file.size() ); }
        void set_dump_file( const char* dump_file, size_t dump_file_len = 0 ) { this->dump_file.assign( dump_file, ( dump_file_len != 0 ) ? dump_file_len : std::strlen( dump_file ) ); }
        const std::string& get_dump_file() const { return dump_file; }
  
        bool operator==( const admin_restore_databaseRequest& other ) const { return password == other.password && dump_file == other.dump_file; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::admin_restore_databaseRequest", 676678086UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); input_stream.readString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); output_stream.writeString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 53; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 894572093UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::admin_restore_databaseResponse; }
  
  
      protected:
        std::string password;
        std::string dump_file;
      };
  
      class admin_restore_databaseSyncRequest : public admin_restore_databaseRequest
      {
      public:
        admin_restore_databaseSyncRequest() : admin_restore_databaseRequest( std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        admin_restore_databaseSyncRequest( const std::string& password, const std::string& dump_file, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_restore_databaseRequest( password, dump_file, response_timeout_ns ) { }
        admin_restore_databaseSyncRequest( const char* password, size_t password_len, const char* dump_file, size_t dump_file_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : admin_restore_databaseRequest( password, password_len, dump_file, dump_file_len, response_timeout_ns ) { }
        virtual ~admin_restore_databaseSyncRequest() { }
  
        bool operator==( const admin_restore_databaseSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::admin_restore_databaseResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class accessResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        accessResponse() : _return_value( false ) { }
        accessResponse( bool _return_value ) : _return_value( _return_value ) { }
        virtual ~accessResponse() { }
  
        void set__return_value( bool _return_value ) { this->_return_value = _return_value; }
        bool get__return_value() const { return _return_value; }
  
        bool operator==( const accessResponse& other ) const { return _return_value == other._return_value; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::accessResponse", 1046856447UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { _return_value = input_stream.readBool( YIELD::StructuredStream::Declaration( "_return_value" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeBool( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }
  
      protected:
        bool _return_value;
      };
  
      class accessRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        accessRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), mode( 0 ) { }
        accessRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), mode( mode ) { }
        accessRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), mode( mode ) { }
        virtual ~accessRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
  
        bool operator==( const accessRequest& other ) const { return context == other.context && path == other.path && mode == other.mode; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::accessRequest", 1061689358UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 1; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1046856447UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::accessResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        uint32_t mode;
      };
  
      class accessSyncRequest : public accessRequest
      {
      public:
        accessSyncRequest() : accessRequest( org::xtreemfs::interfaces::Context(), std::string(), 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        accessSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : accessRequest( context, path, mode, response_timeout_ns ) { }
        accessSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : accessRequest( context, path, path_len, mode, response_timeout_ns ) { }
        virtual ~accessSyncRequest() { }
  
        bool operator==( const accessSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::accessResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class chmodResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        chmodResponse() { }
        virtual ~chmodResponse() { }
  
        bool operator==( const chmodResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::chmodResponse", 2600293463UL );
  
      };
  
      class chmodRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        chmodRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), mode( 0 ) { }
        chmodRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), mode( mode ) { }
        chmodRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), mode( mode ) { }
        virtual ~chmodRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
  
        bool operator==( const chmodRequest& other ) const { return context == other.context && path == other.path && mode == other.mode; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::chmodRequest", 382547319UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 2; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 2600293463UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::chmodResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        uint32_t mode;
      };
  
      class chmodSyncRequest : public chmodRequest
      {
      public:
        chmodSyncRequest() : chmodRequest( org::xtreemfs::interfaces::Context(), std::string(), 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        chmodSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : chmodRequest( context, path, mode, response_timeout_ns ) { }
        chmodSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : chmodRequest( context, path, path_len, mode, response_timeout_ns ) { }
        virtual ~chmodSyncRequest() { }
  
        bool operator==( const chmodSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::chmodResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class chownResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        chownResponse() { }
        virtual ~chownResponse() { }
  
        bool operator==( const chownResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::chownResponse", 2956591049UL );
  
      };
  
      class chownRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        chownRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        chownRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& userId, const std::string& groupId, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), userId( userId ), groupId( groupId ) { }
        chownRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const char* userId, size_t userId_len, const char* groupId, size_t groupId_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), userId( userId, userId_len ), groupId( groupId, groupId_len ) { }
        virtual ~chownRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_userId( const std::string& userId ) { set_userId( userId.c_str(), userId.size() ); }
        void set_userId( const char* userId, size_t userId_len = 0 ) { this->userId.assign( userId, ( userId_len != 0 ) ? userId_len : std::strlen( userId ) ); }
        const std::string& get_userId() const { return userId; }
        void set_groupId( const std::string& groupId ) { set_groupId( groupId.c_str(), groupId.size() ); }
        void set_groupId( const char* groupId, size_t groupId_len = 0 ) { this->groupId.assign( groupId, ( groupId_len != 0 ) ? groupId_len : std::strlen( groupId ) ); }
        const std::string& get_groupId() const { return groupId; }
  
        bool operator==( const chownRequest& other ) const { return context == other.context && path == other.path && userId == other.userId && groupId == other.groupId; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::chownRequest", 1479455167UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "userId" ), userId ); input_stream.readString( YIELD::StructuredStream::Declaration( "groupId" ), groupId ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "userId" ), userId ); output_stream.writeString( YIELD::StructuredStream::Declaration( "groupId" ), groupId ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 3; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 2956591049UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::chownResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        std::string userId;
        std::string groupId;
      };
  
      class chownSyncRequest : public chownRequest
      {
      public:
        chownSyncRequest() : chownRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        chownSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& userId, const std::string& groupId, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : chownRequest( context, path, userId, groupId, response_timeout_ns ) { }
        chownSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const char* userId, size_t userId_len, const char* groupId, size_t groupId_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : chownRequest( context, path, path_len, userId, userId_len, groupId, groupId_len, response_timeout_ns ) { }
        virtual ~chownSyncRequest() { }
  
        bool operator==( const chownSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::chownResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class createResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        createResponse() { }
        virtual ~createResponse() { }
  
        bool operator==( const createResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::createResponse", 198172638UL );
  
      };
  
      class createRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        createRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), mode( 0 ) { }
        createRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), mode( mode ) { }
        createRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), mode( mode ) { }
        virtual ~createRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
  
        bool operator==( const createRequest& other ) const { return context == other.context && path == other.path && mode == other.mode; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::createRequest", 736916640UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 4; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 198172638UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::createResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        uint32_t mode;
      };
  
      class createSyncRequest : public createRequest
      {
      public:
        createSyncRequest() : createRequest( org::xtreemfs::interfaces::Context(), std::string(), 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        createSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : createRequest( context, path, mode, response_timeout_ns ) { }
        createSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : createRequest( context, path, path_len, mode, response_timeout_ns ) { }
        virtual ~createSyncRequest() { }
  
        bool operator==( const createSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::createResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class getattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        getattrResponse() { }
        getattrResponse( const org::xtreemfs::interfaces::stat_& stbuf ) : stbuf( stbuf ) { }
        virtual ~getattrResponse() { }
  
        void set_stbuf( const org::xtreemfs::interfaces::stat_&  stbuf ) { this->stbuf = stbuf; }
        const org::xtreemfs::interfaces::stat_& get_stbuf() const { return stbuf; }
  
        bool operator==( const getattrResponse& other ) const { return stbuf == other.stbuf; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::getattrResponse", 1150023493UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::stat_", "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::stat_", "stbuf" ), stbuf ); }
  
      protected:
        org::xtreemfs::interfaces::stat_ stbuf;
      };
  
      class getattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        getattrRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        getattrRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ) { }
        getattrRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ) { }
        virtual ~getattrRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
  
        bool operator==( const getattrRequest& other ) const { return context == other.context && path == other.path; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::getattrRequest", 1335718504UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 5; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1150023493UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::getattrResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
      };
  
      class getattrSyncRequest : public getattrRequest
      {
      public:
        getattrSyncRequest() : getattrRequest( org::xtreemfs::interfaces::Context(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        getattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : getattrRequest( context, path, response_timeout_ns ) { }
        getattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : getattrRequest( context, path, path_len, response_timeout_ns ) { }
        virtual ~getattrSyncRequest() { }
  
        bool operator==( const getattrSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::getattrResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class getxattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        getxattrResponse() { }
        getxattrResponse( const std::string& _return_value ) : _return_value( _return_value ) { }
        getxattrResponse( const char* _return_value, size_t _return_value_len ) : _return_value( _return_value, _return_value_len ) { }
        virtual ~getxattrResponse() { }
  
        void set__return_value( const std::string& _return_value ) { set__return_value( _return_value.c_str(), _return_value.size() ); }
        void set__return_value( const char* _return_value, size_t _return_value_len = 0 ) { this->_return_value.assign( _return_value, ( _return_value_len != 0 ) ? _return_value_len : std::strlen( _return_value ) ); }
        const std::string& get__return_value() const { return _return_value; }
  
        bool operator==( const getxattrResponse& other ) const { return _return_value == other._return_value; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::getxattrResponse", 72976609UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }
  
      protected:
        std::string _return_value;
      };
  
      class getxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        getxattrRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        getxattrRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), name( name ) { }
        getxattrRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const char* name, size_t name_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), name( name, name_len ) { }
        virtual ~getxattrRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len = 0 ) { this->name.assign( name, ( name_len != 0 ) ? name_len : std::strlen( name ) ); }
        const std::string& get_name() const { return name; }
  
        bool operator==( const getxattrRequest& other ) const { return context == other.context && path == other.path && name == other.name; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::getxattrRequest", 1634969716UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 6; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 72976609UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::getxattrResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        std::string name;
      };
  
      class getxattrSyncRequest : public getxattrRequest
      {
      public:
        getxattrSyncRequest() : getxattrRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        getxattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : getxattrRequest( context, path, name, response_timeout_ns ) { }
        getxattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const char* name, size_t name_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : getxattrRequest( context, path, path_len, name, name_len, response_timeout_ns ) { }
        virtual ~getxattrSyncRequest() { }
  
        bool operator==( const getxattrSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::getxattrResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class linkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        linkResponse() { }
        virtual ~linkResponse() { }
  
        bool operator==( const linkResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::linkResponse", 1242382351UL );
  
      };
  
      class linkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        linkRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        linkRequest( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), target_path( target_path ), link_path( link_path ) { }
        linkRequest( const org::xtreemfs::interfaces::Context& context, const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), target_path( target_path, target_path_len ), link_path( link_path, link_path_len ) { }
        virtual ~linkRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_target_path( const std::string& target_path ) { set_target_path( target_path.c_str(), target_path.size() ); }
        void set_target_path( const char* target_path, size_t target_path_len = 0 ) { this->target_path.assign( target_path, ( target_path_len != 0 ) ? target_path_len : std::strlen( target_path ) ); }
        const std::string& get_target_path() const { return target_path; }
        void set_link_path( const std::string& link_path ) { set_link_path( link_path.c_str(), link_path.size() ); }
        void set_link_path( const char* link_path, size_t link_path_len = 0 ) { this->link_path.assign( link_path, ( link_path_len != 0 ) ? link_path_len : std::strlen( link_path ) ); }
        const std::string& get_link_path() const { return link_path; }
  
        bool operator==( const linkRequest& other ) const { return context == other.context && target_path == other.target_path && link_path == other.link_path; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::linkRequest", 666215785UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 7; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1242382351UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::linkResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string target_path;
        std::string link_path;
      };
  
      class linkSyncRequest : public linkRequest
      {
      public:
        linkSyncRequest() : linkRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        linkSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : linkRequest( context, target_path, link_path, response_timeout_ns ) { }
        linkSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : linkRequest( context, target_path, target_path_len, link_path, link_path_len, response_timeout_ns ) { }
        virtual ~linkSyncRequest() { }
  
        bool operator==( const linkSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::linkResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class listxattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        listxattrResponse() { }
        listxattrResponse( const org::xtreemfs::interfaces::StringSet& names ) : names( names ) { }
        virtual ~listxattrResponse() { }
  
        void set_names( const org::xtreemfs::interfaces::StringSet&  names ) { this->names = names; }
        const org::xtreemfs::interfaces::StringSet& get_names() const { return names; }
  
        bool operator==( const listxattrResponse& other ) const { return names == other.names; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::listxattrResponse", 3509031574UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "names" ), &names ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "names" ), names ); }
  
      protected:
        org::xtreemfs::interfaces::StringSet names;
      };
  
      class listxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        listxattrRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        listxattrRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ) { }
        listxattrRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ) { }
        virtual ~listxattrRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
  
        bool operator==( const listxattrRequest& other ) const { return context == other.context && path == other.path; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::listxattrRequest", 661933360UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 8; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3509031574UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::listxattrResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
      };
  
      class listxattrSyncRequest : public listxattrRequest
      {
      public:
        listxattrSyncRequest() : listxattrRequest( org::xtreemfs::interfaces::Context(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        listxattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : listxattrRequest( context, path, response_timeout_ns ) { }
        listxattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : listxattrRequest( context, path, path_len, response_timeout_ns ) { }
        virtual ~listxattrSyncRequest() { }
  
        bool operator==( const listxattrSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::listxattrResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class mkdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        mkdirResponse() { }
        virtual ~mkdirResponse() { }
  
        bool operator==( const mkdirResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::mkdirResponse", 1461564105UL );
  
      };
  
      class mkdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        mkdirRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), mode( 0 ) { }
        mkdirRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), mode( mode ) { }
        mkdirRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), mode( mode ) { }
        virtual ~mkdirRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
  
        bool operator==( const mkdirRequest& other ) const { return context == other.context && path == other.path && mode == other.mode; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::mkdirRequest", 2418580920UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 9; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1461564105UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::mkdirResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        uint32_t mode;
      };
  
      class mkdirSyncRequest : public mkdirRequest
      {
      public:
        mkdirSyncRequest() : mkdirRequest( org::xtreemfs::interfaces::Context(), std::string(), 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        mkdirSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : mkdirRequest( context, path, mode, response_timeout_ns ) { }
        mkdirSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : mkdirRequest( context, path, path_len, mode, response_timeout_ns ) { }
        virtual ~mkdirSyncRequest() { }
  
        bool operator==( const mkdirSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::mkdirResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class mkvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        mkvolResponse() { }
        virtual ~mkvolResponse() { }
  
        bool operator==( const mkvolResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::mkvolResponse", 1350127602UL );
  
      };
  
      class mkvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        mkvolRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), osd_selection_policy( 0 ), access_control_policy( 0 ) { }
        mkvolRequest( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), password( password ), volume_name( volume_name ), osd_selection_policy( osd_selection_policy ), default_striping_policy( default_striping_policy ), access_control_policy( access_control_policy ) { }
        mkvolRequest( const org::xtreemfs::interfaces::Context& context, const char* password, size_t password_len, const char* volume_name, size_t volume_name_len, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), password( password, password_len ), volume_name( volume_name, volume_name_len ), osd_selection_policy( osd_selection_policy ), default_striping_policy( default_striping_policy ), access_control_policy( access_control_policy ) { }
        virtual ~mkvolRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }
        void set_volume_name( const std::string& volume_name ) { set_volume_name( volume_name.c_str(), volume_name.size() ); }
        void set_volume_name( const char* volume_name, size_t volume_name_len = 0 ) { this->volume_name.assign( volume_name, ( volume_name_len != 0 ) ? volume_name_len : std::strlen( volume_name ) ); }
        const std::string& get_volume_name() const { return volume_name; }
        void set_osd_selection_policy( uint32_t osd_selection_policy ) { this->osd_selection_policy = osd_selection_policy; }
        uint32_t get_osd_selection_policy() const { return osd_selection_policy; }
        void set_default_striping_policy( const org::xtreemfs::interfaces::StripingPolicy&  default_striping_policy ) { this->default_striping_policy = default_striping_policy; }
        const org::xtreemfs::interfaces::StripingPolicy& get_default_striping_policy() const { return default_striping_policy; }
        void set_access_control_policy( uint32_t access_control_policy ) { this->access_control_policy = access_control_policy; }
        uint32_t get_access_control_policy() const { return access_control_policy; }
  
        bool operator==( const mkvolRequest& other ) const { return context == other.context && password == other.password && volume_name == other.volume_name && osd_selection_policy == other.osd_selection_policy && default_striping_policy == other.default_striping_policy && access_control_policy == other.access_control_policy; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::mkvolRequest", 2457144040UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); input_stream.readString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); osd_selection_policy = input_stream.readUint32( YIELD::StructuredStream::Declaration( "osd_selection_policy" ) ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StripingPolicy", "default_striping_policy" ), &default_striping_policy ); access_control_policy = input_stream.readUint32( YIELD::StructuredStream::Declaration( "access_control_policy" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "osd_selection_policy" ), osd_selection_policy ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StripingPolicy", "default_striping_policy" ), default_striping_policy ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "access_control_policy" ), access_control_policy ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 10; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1350127602UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::mkvolResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string password;
        std::string volume_name;
        uint32_t osd_selection_policy;
        org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
        uint32_t access_control_policy;
      };
  
      class mkvolSyncRequest : public mkvolRequest
      {
      public:
        mkvolSyncRequest() : mkvolRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), 0, org::xtreemfs::interfaces::StripingPolicy(), 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        mkvolSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : mkvolRequest( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy, response_timeout_ns ) { }
        mkvolSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* password, size_t password_len, const char* volume_name, size_t volume_name_len, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : mkvolRequest( context, password, password_len, volume_name, volume_name_len, osd_selection_policy, default_striping_policy, access_control_policy, response_timeout_ns ) { }
        virtual ~mkvolSyncRequest() { }
  
        bool operator==( const mkvolSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::mkvolResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class openResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        openResponse() { }
        openResponse( const org::xtreemfs::interfaces::FileCredentials& credentials ) : credentials( credentials ) { }
        virtual ~openResponse() { }
  
        void set_credentials( const org::xtreemfs::interfaces::FileCredentials&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_credentials() const { return credentials; }
  
        bool operator==( const openResponse& other ) const { return credentials == other.credentials; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::openResponse", 3891582700UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), &credentials ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "credentials" ), credentials ); }
  
      protected:
        org::xtreemfs::interfaces::FileCredentials credentials;
      };
  
      class openRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        openRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), flags( 0 ), mode( 0 ) { }
        openRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t flags, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), flags( flags ), mode( mode ) { }
        openRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t flags, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), flags( flags ), mode( mode ) { }
        virtual ~openRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_flags( uint32_t flags ) { this->flags = flags; }
        uint32_t get_flags() const { return flags; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
  
        bool operator==( const openRequest& other ) const { return context == other.context && path == other.path && flags == other.flags && mode == other.mode; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::openRequest", 2208926316UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); flags = input_stream.readUint32( YIELD::StructuredStream::Declaration( "flags" ) ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "flags" ), flags ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 11; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3891582700UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::openResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        uint32_t flags;
        uint32_t mode;
      };
  
      class openSyncRequest : public openRequest
      {
      public:
        openSyncRequest() : openRequest( org::xtreemfs::interfaces::Context(), std::string(), 0, 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        openSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t flags, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : openRequest( context, path, flags, mode, response_timeout_ns ) { }
        openSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint32_t flags, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : openRequest( context, path, path_len, flags, mode, response_timeout_ns ) { }
        virtual ~openSyncRequest() { }
  
        bool operator==( const openSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::openResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class readdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        readdirResponse() { }
        readdirResponse( const org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) : directory_entries( directory_entries ) { }
        virtual ~readdirResponse() { }
  
        void set_directory_entries( const org::xtreemfs::interfaces::DirectoryEntrySet&  directory_entries ) { this->directory_entries = directory_entries; }
        const org::xtreemfs::interfaces::DirectoryEntrySet& get_directory_entries() const { return directory_entries; }
  
        bool operator==( const readdirResponse& other ) const { return directory_entries == other.directory_entries; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::readdirResponse", 42716287UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::DirectoryEntrySet", "directory_entries" ), &directory_entries ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::DirectoryEntrySet", "directory_entries" ), directory_entries ); }
  
      protected:
        org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
      };
  
      class readdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        readdirRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        readdirRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ) { }
        readdirRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ) { }
        virtual ~readdirRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
  
        bool operator==( const readdirRequest& other ) const { return context == other.context && path == other.path; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::readdirRequest", 2159159247UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 12; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 42716287UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::readdirResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
      };
  
      class readdirSyncRequest : public readdirRequest
      {
      public:
        readdirSyncRequest() : readdirRequest( org::xtreemfs::interfaces::Context(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        readdirSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : readdirRequest( context, path, response_timeout_ns ) { }
        readdirSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : readdirRequest( context, path, path_len, response_timeout_ns ) { }
        virtual ~readdirSyncRequest() { }
  
        bool operator==( const readdirSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::readdirResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class removexattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        removexattrResponse() { }
        virtual ~removexattrResponse() { }
  
        bool operator==( const removexattrResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::removexattrResponse", 813148522UL );
  
      };
  
      class removexattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        removexattrRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        removexattrRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), name( name ) { }
        removexattrRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const char* name, size_t name_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), name( name, name_len ) { }
        virtual ~removexattrRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len = 0 ) { this->name.assign( name, ( name_len != 0 ) ? name_len : std::strlen( name ) ); }
        const std::string& get_name() const { return name; }
  
        bool operator==( const removexattrRequest& other ) const { return context == other.context && path == other.path && name == other.name; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::removexattrRequest", 4111450692UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 13; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 813148522UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::removexattrResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        std::string name;
      };
  
      class removexattrSyncRequest : public removexattrRequest
      {
      public:
        removexattrSyncRequest() : removexattrRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        removexattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : removexattrRequest( context, path, name, response_timeout_ns ) { }
        removexattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const char* name, size_t name_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : removexattrRequest( context, path, path_len, name, name_len, response_timeout_ns ) { }
        virtual ~removexattrSyncRequest() { }
  
        bool operator==( const removexattrSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::removexattrResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class renameResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        renameResponse() { }
        renameResponse( const org::xtreemfs::interfaces::FileCredentialsSet& credentials ) : credentials( credentials ) { }
        virtual ~renameResponse() { }
  
        void set_credentials( const org::xtreemfs::interfaces::FileCredentialsSet&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentialsSet& get_credentials() const { return credentials; }
  
        bool operator==( const renameResponse& other ) const { return credentials == other.credentials; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::renameResponse", 4002167429UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentialsSet", "credentials" ), &credentials ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentialsSet", "credentials" ), credentials ); }
  
      protected:
        org::xtreemfs::interfaces::FileCredentialsSet credentials;
      };
  
      class renameRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        renameRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        renameRequest( const org::xtreemfs::interfaces::Context& context, const std::string& source_path, const std::string& target_path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), source_path( source_path ), target_path( target_path ) { }
        renameRequest( const org::xtreemfs::interfaces::Context& context, const char* source_path, size_t source_path_len, const char* target_path, size_t target_path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), source_path( source_path, source_path_len ), target_path( target_path, target_path_len ) { }
        virtual ~renameRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_source_path( const std::string& source_path ) { set_source_path( source_path.c_str(), source_path.size() ); }
        void set_source_path( const char* source_path, size_t source_path_len = 0 ) { this->source_path.assign( source_path, ( source_path_len != 0 ) ? source_path_len : std::strlen( source_path ) ); }
        const std::string& get_source_path() const { return source_path; }
        void set_target_path( const std::string& target_path ) { set_target_path( target_path.c_str(), target_path.size() ); }
        void set_target_path( const char* target_path, size_t target_path_len = 0 ) { this->target_path.assign( target_path, ( target_path_len != 0 ) ? target_path_len : std::strlen( target_path ) ); }
        const std::string& get_target_path() const { return target_path; }
  
        bool operator==( const renameRequest& other ) const { return context == other.context && source_path == other.source_path && target_path == other.target_path; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::renameRequest", 229065239UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "source_path" ), source_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "source_path" ), source_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 14; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 4002167429UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::renameResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string source_path;
        std::string target_path;
      };
  
      class renameSyncRequest : public renameRequest
      {
      public:
        renameSyncRequest() : renameRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        renameSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& source_path, const std::string& target_path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : renameRequest( context, source_path, target_path, response_timeout_ns ) { }
        renameSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* source_path, size_t source_path_len, const char* target_path, size_t target_path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : renameRequest( context, source_path, source_path_len, target_path, target_path_len, response_timeout_ns ) { }
        virtual ~renameSyncRequest() { }
  
        bool operator==( const renameSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::renameResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class rmdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        rmdirResponse() { }
        virtual ~rmdirResponse() { }
  
        bool operator==( const rmdirResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::rmdirResponse", 161028879UL );
  
      };
  
      class rmdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        rmdirRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        rmdirRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ) { }
        rmdirRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ) { }
        virtual ~rmdirRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
  
        bool operator==( const rmdirRequest& other ) const { return context == other.context && path == other.path; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::rmdirRequest", 1480479915UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 15; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 161028879UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::rmdirResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
      };
  
      class rmdirSyncRequest : public rmdirRequest
      {
      public:
        rmdirSyncRequest() : rmdirRequest( org::xtreemfs::interfaces::Context(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        rmdirSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : rmdirRequest( context, path, response_timeout_ns ) { }
        rmdirSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : rmdirRequest( context, path, path_len, response_timeout_ns ) { }
        virtual ~rmdirSyncRequest() { }
  
        bool operator==( const rmdirSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::rmdirResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class rmvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        rmvolResponse() { }
        virtual ~rmvolResponse() { }
  
        bool operator==( const rmvolResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::rmvolResponse", 3343228781UL );
  
      };
  
      class rmvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        rmvolRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        rmvolRequest( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), password( password ), volume_name( volume_name ) { }
        rmvolRequest( const org::xtreemfs::interfaces::Context& context, const char* password, size_t password_len, const char* volume_name, size_t volume_name_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), password( password, password_len ), volume_name( volume_name, volume_name_len ) { }
        virtual ~rmvolRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_password( const std::string& password ) { set_password( password.c_str(), password.size() ); }
        void set_password( const char* password, size_t password_len = 0 ) { this->password.assign( password, ( password_len != 0 ) ? password_len : std::strlen( password ) ); }
        const std::string& get_password() const { return password; }
        void set_volume_name( const std::string& volume_name ) { set_volume_name( volume_name.c_str(), volume_name.size() ); }
        void set_volume_name( const char* volume_name, size_t volume_name_len = 0 ) { this->volume_name.assign( volume_name, ( volume_name_len != 0 ) ? volume_name_len : std::strlen( volume_name ) ); }
        const std::string& get_volume_name() const { return volume_name; }
  
        bool operator==( const rmvolRequest& other ) const { return context == other.context && password == other.password && volume_name == other.volume_name; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::rmvolRequest", 2429915851UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "password" ), password ); input_stream.readString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "password" ), password ); output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 16; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3343228781UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::rmvolResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string password;
        std::string volume_name;
      };
  
      class rmvolSyncRequest : public rmvolRequest
      {
      public:
        rmvolSyncRequest() : rmvolRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        rmvolSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : rmvolRequest( context, password, volume_name, response_timeout_ns ) { }
        rmvolSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* password, size_t password_len, const char* volume_name, size_t volume_name_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : rmvolRequest( context, password, password_len, volume_name, volume_name_len, response_timeout_ns ) { }
        virtual ~rmvolSyncRequest() { }
  
        bool operator==( const rmvolSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::rmvolResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class setattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        setattrResponse() { }
        virtual ~setattrResponse() { }
  
        bool operator==( const setattrResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::setattrResponse", 1637936981UL );
  
      };
  
      class setattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        setattrRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        setattrRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), stbuf( stbuf ) { }
        setattrRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const org::xtreemfs::interfaces::stat_& stbuf, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), stbuf( stbuf ) { }
        virtual ~setattrRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_stbuf( const org::xtreemfs::interfaces::stat_&  stbuf ) { this->stbuf = stbuf; }
        const org::xtreemfs::interfaces::stat_& get_stbuf() const { return stbuf; }
  
        bool operator==( const setattrRequest& other ) const { return context == other.context && path == other.path && stbuf == other.stbuf; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::setattrRequest", 1274791757UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::stat_", "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::stat_", "stbuf" ), stbuf ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 17; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1637936981UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::setattrResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        org::xtreemfs::interfaces::stat_ stbuf;
      };
  
      class setattrSyncRequest : public setattrRequest
      {
      public:
        setattrSyncRequest() : setattrRequest( org::xtreemfs::interfaces::Context(), std::string(), org::xtreemfs::interfaces::stat_(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        setattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : setattrRequest( context, path, stbuf, response_timeout_ns ) { }
        setattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const org::xtreemfs::interfaces::stat_& stbuf, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : setattrRequest( context, path, path_len, stbuf, response_timeout_ns ) { }
        virtual ~setattrSyncRequest() { }
  
        bool operator==( const setattrSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::setattrResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class setxattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        setxattrResponse() { }
        virtual ~setxattrResponse() { }
  
        bool operator==( const setxattrResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::setxattrResponse", 1656711865UL );
  
      };
  
      class setxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        setxattrRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), flags( 0 ) { }
        setxattrRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const std::string& value, int32_t flags, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), name( name ), value( value ), flags( flags ) { }
        setxattrRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const char* name, size_t name_len, const char* value, size_t value_len, int32_t flags, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), name( name, name_len ), value( value, value_len ), flags( flags ) { }
        virtual ~setxattrRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len = 0 ) { this->name.assign( name, ( name_len != 0 ) ? name_len : std::strlen( name ) ); }
        const std::string& get_name() const { return name; }
        void set_value( const std::string& value ) { set_value( value.c_str(), value.size() ); }
        void set_value( const char* value, size_t value_len = 0 ) { this->value.assign( value, ( value_len != 0 ) ? value_len : std::strlen( value ) ); }
        const std::string& get_value() const { return value; }
        void set_flags( int32_t flags ) { this->flags = flags; }
        int32_t get_flags() const { return flags; }
  
        bool operator==( const setxattrRequest& other ) const { return context == other.context && path == other.path && name == other.name && value == other.value && flags == other.flags; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::setxattrRequest", 1676810928UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); input_stream.readString( YIELD::StructuredStream::Declaration( "value" ), value ); flags = input_stream.readInt32( YIELD::StructuredStream::Declaration( "flags" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); output_stream.writeString( YIELD::StructuredStream::Declaration( "value" ), value ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "flags" ), flags ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 18; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1656711865UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::setxattrResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        std::string name;
        std::string value;
        int32_t flags;
      };
  
      class setxattrSyncRequest : public setxattrRequest
      {
      public:
        setxattrSyncRequest() : setxattrRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), std::string(), 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        setxattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const std::string& value, int32_t flags, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : setxattrRequest( context, path, name, value, flags, response_timeout_ns ) { }
        setxattrSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, const char* name, size_t name_len, const char* value, size_t value_len, int32_t flags, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : setxattrRequest( context, path, path_len, name, name_len, value, value_len, flags, response_timeout_ns ) { }
        virtual ~setxattrSyncRequest() { }
  
        bool operator==( const setxattrSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::setxattrResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class statfsResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        statfsResponse() { }
        statfsResponse( const org::xtreemfs::interfaces::statfs_& statfsbuf ) : statfsbuf( statfsbuf ) { }
        virtual ~statfsResponse() { }
  
        void set_statfsbuf( const org::xtreemfs::interfaces::statfs_&  statfsbuf ) { this->statfsbuf = statfsbuf; }
        const org::xtreemfs::interfaces::statfs_& get_statfsbuf() const { return statfsbuf; }
  
        bool operator==( const statfsResponse& other ) const { return statfsbuf == other.statfsbuf; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::statfsResponse", 2403576867UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::statfs_", "statfsbuf" ), &statfsbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::statfs_", "statfsbuf" ), statfsbuf ); }
  
      protected:
        org::xtreemfs::interfaces::statfs_ statfsbuf;
      };
  
      class statfsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        statfsRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        statfsRequest( const org::xtreemfs::interfaces::Context& context, const std::string& volume_name, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), volume_name( volume_name ) { }
        statfsRequest( const org::xtreemfs::interfaces::Context& context, const char* volume_name, size_t volume_name_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), volume_name( volume_name, volume_name_len ) { }
        virtual ~statfsRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_volume_name( const std::string& volume_name ) { set_volume_name( volume_name.c_str(), volume_name.size() ); }
        void set_volume_name( const char* volume_name, size_t volume_name_len = 0 ) { this->volume_name.assign( volume_name, ( volume_name_len != 0 ) ? volume_name_len : std::strlen( volume_name ) ); }
        const std::string& get_volume_name() const { return volume_name; }
  
        bool operator==( const statfsRequest& other ) const { return context == other.context && volume_name == other.volume_name; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::statfsRequest", 1451010089UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 19; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 2403576867UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::statfsResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string volume_name;
      };
  
      class statfsSyncRequest : public statfsRequest
      {
      public:
        statfsSyncRequest() : statfsRequest( org::xtreemfs::interfaces::Context(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        statfsSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& volume_name, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : statfsRequest( context, volume_name, response_timeout_ns ) { }
        statfsSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* volume_name, size_t volume_name_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : statfsRequest( context, volume_name, volume_name_len, response_timeout_ns ) { }
        virtual ~statfsSyncRequest() { }
  
        bool operator==( const statfsSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::statfsResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class symlinkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        symlinkResponse() { }
        virtual ~symlinkResponse() { }
  
        bool operator==( const symlinkResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::symlinkResponse", 3986079894UL );
  
      };
  
      class symlinkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        symlinkRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        symlinkRequest( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), target_path( target_path ), link_path( link_path ) { }
        symlinkRequest( const org::xtreemfs::interfaces::Context& context, const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), target_path( target_path, target_path_len ), link_path( link_path, link_path_len ) { }
        virtual ~symlinkRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_target_path( const std::string& target_path ) { set_target_path( target_path.c_str(), target_path.size() ); }
        void set_target_path( const char* target_path, size_t target_path_len = 0 ) { this->target_path.assign( target_path, ( target_path_len != 0 ) ? target_path_len : std::strlen( target_path ) ); }
        const std::string& get_target_path() const { return target_path; }
        void set_link_path( const std::string& link_path ) { set_link_path( link_path.c_str(), link_path.size() ); }
        void set_link_path( const char* link_path, size_t link_path_len = 0 ) { this->link_path.assign( link_path, ( link_path_len != 0 ) ? link_path_len : std::strlen( link_path ) ); }
        const std::string& get_link_path() const { return link_path; }
  
        bool operator==( const symlinkRequest& other ) const { return context == other.context && target_path == other.target_path && link_path == other.link_path; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::symlinkRequest", 617299237UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 20; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3986079894UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::symlinkResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string target_path;
        std::string link_path;
      };
  
      class symlinkSyncRequest : public symlinkRequest
      {
      public:
        symlinkSyncRequest() : symlinkRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        symlinkSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : symlinkRequest( context, target_path, link_path, response_timeout_ns ) { }
        symlinkSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : symlinkRequest( context, target_path, target_path_len, link_path, link_path_len, response_timeout_ns ) { }
        virtual ~symlinkSyncRequest() { }
  
        bool operator==( const symlinkSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::symlinkResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class unlinkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        unlinkResponse() { }
        unlinkResponse( const org::xtreemfs::interfaces::FileCredentialsSet& credentials ) : credentials( credentials ) { }
        virtual ~unlinkResponse() { }
  
        void set_credentials( const org::xtreemfs::interfaces::FileCredentialsSet&  credentials ) { this->credentials = credentials; }
        const org::xtreemfs::interfaces::FileCredentialsSet& get_credentials() const { return credentials; }
  
        bool operator==( const unlinkResponse& other ) const { return credentials == other.credentials; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::unlinkResponse", 35019552UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentialsSet", "credentials" ), &credentials ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentialsSet", "credentials" ), credentials ); }
  
      protected:
        org::xtreemfs::interfaces::FileCredentialsSet credentials;
      };
  
      class unlinkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        unlinkRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        unlinkRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ) { }
        unlinkRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ) { }
        virtual ~unlinkRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
  
        bool operator==( const unlinkRequest& other ) const { return context == other.context && path == other.path; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::unlinkRequest", 3413690043UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 21; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 35019552UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::unlinkResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
      };
  
      class unlinkSyncRequest : public unlinkRequest
      {
      public:
        unlinkSyncRequest() : unlinkRequest( org::xtreemfs::interfaces::Context(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        unlinkSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : unlinkRequest( context, path, response_timeout_ns ) { }
        unlinkSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : unlinkRequest( context, path, path_len, response_timeout_ns ) { }
        virtual ~unlinkSyncRequest() { }
  
        bool operator==( const unlinkSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::unlinkResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class utimeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        utimeResponse() { }
        virtual ~utimeResponse() { }
  
        bool operator==( const utimeResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::utimeResponse", 1258487299UL );
  
      };
  
      class utimeRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        utimeRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), ctime( 0 ), atime( 0 ), mtime( 0 ) { }
        utimeRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path ), ctime( ctime ), atime( atime ), mtime( mtime ) { }
        utimeRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint64_t ctime, uint64_t atime, uint64_t mtime, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), path( path, path_len ), ctime( ctime ), atime( atime ), mtime( mtime ) { }
        virtual ~utimeRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_ctime( uint64_t ctime ) { this->ctime = ctime; }
        uint64_t get_ctime() const { return ctime; }
        void set_atime( uint64_t atime ) { this->atime = atime; }
        uint64_t get_atime() const { return atime; }
        void set_mtime( uint64_t mtime ) { this->mtime = mtime; }
        uint64_t get_mtime() const { return mtime; }
  
        bool operator==( const utimeRequest& other ) const { return context == other.context && path == other.path && ctime == other.ctime && atime == other.atime && mtime == other.mtime; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::utimeRequest", 1774137591UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); ctime = input_stream.readUint64( YIELD::StructuredStream::Declaration( "ctime" ) ); atime = input_stream.readUint64( YIELD::StructuredStream::Declaration( "atime" ) ); mtime = input_stream.readUint64( YIELD::StructuredStream::Declaration( "mtime" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "ctime" ), ctime ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "atime" ), atime ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "mtime" ), mtime ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 22; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1258487299UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::utimeResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string path;
        uint64_t ctime;
        uint64_t atime;
        uint64_t mtime;
      };
  
      class utimeSyncRequest : public utimeRequest
      {
      public:
        utimeSyncRequest() : utimeRequest( org::xtreemfs::interfaces::Context(), std::string(), 0, 0, 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        utimeSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : utimeRequest( context, path, ctime, atime, mtime, response_timeout_ns ) { }
        utimeSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* path, size_t path_len, uint64_t ctime, uint64_t atime, uint64_t mtime, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : utimeRequest( context, path, path_len, ctime, atime, mtime, response_timeout_ns ) { }
        virtual ~utimeSyncRequest() { }
  
        bool operator==( const utimeSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::utimeResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_check_file_existsResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_check_file_existsResponse() { }
        xtreemfs_check_file_existsResponse( const std::string& bitmap ) : bitmap( bitmap ) { }
        xtreemfs_check_file_existsResponse( const char* bitmap, size_t bitmap_len ) : bitmap( bitmap, bitmap_len ) { }
        virtual ~xtreemfs_check_file_existsResponse() { }
  
        void set_bitmap( const std::string& bitmap ) { set_bitmap( bitmap.c_str(), bitmap.size() ); }
        void set_bitmap( const char* bitmap, size_t bitmap_len = 0 ) { this->bitmap.assign( bitmap, ( bitmap_len != 0 ) ? bitmap_len : std::strlen( bitmap ) ); }
        const std::string& get_bitmap() const { return bitmap; }
  
        bool operator==( const xtreemfs_check_file_existsResponse& other ) const { return bitmap == other.bitmap; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsResponse", 3802978278UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "bitmap" ), bitmap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "bitmap" ), bitmap ); }
  
      protected:
        std::string bitmap;
      };
  
      class xtreemfs_check_file_existsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_check_file_existsRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_check_file_existsRequest( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), volume_id( volume_id ), file_ids( file_ids ) { }
        xtreemfs_check_file_existsRequest( const char* volume_id, size_t volume_id_len, const org::xtreemfs::interfaces::StringSet& file_ids, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), volume_id( volume_id, volume_id_len ), file_ids( file_ids ) { }
        virtual ~xtreemfs_check_file_existsRequest() { }
  
        void set_volume_id( const std::string& volume_id ) { set_volume_id( volume_id.c_str(), volume_id.size() ); }
        void set_volume_id( const char* volume_id, size_t volume_id_len = 0 ) { this->volume_id.assign( volume_id, ( volume_id_len != 0 ) ? volume_id_len : std::strlen( volume_id ) ); }
        const std::string& get_volume_id() const { return volume_id; }
        void set_file_ids( const org::xtreemfs::interfaces::StringSet&  file_ids ) { this->file_ids = file_ids; }
        const org::xtreemfs::interfaces::StringSet& get_file_ids() const { return file_ids; }
  
        bool operator==( const xtreemfs_check_file_existsRequest& other ) const { return volume_id == other.volume_id && file_ids == other.file_ids; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsRequest", 1290835505UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "volume_id" ), volume_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "file_ids" ), &file_ids ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_id" ), volume_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "file_ids" ), file_ids ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 23; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3802978278UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsResponse; }
  
  
      protected:
        std::string volume_id;
        org::xtreemfs::interfaces::StringSet file_ids;
      };
  
      class xtreemfs_check_file_existsSyncRequest : public xtreemfs_check_file_existsRequest
      {
      public:
        xtreemfs_check_file_existsSyncRequest() : xtreemfs_check_file_existsRequest( std::string(), org::xtreemfs::interfaces::StringSet(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_check_file_existsSyncRequest( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_check_file_existsRequest( volume_id, file_ids, response_timeout_ns ) { }
        xtreemfs_check_file_existsSyncRequest( const char* volume_id, size_t volume_id_len, const org::xtreemfs::interfaces::StringSet& file_ids, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_check_file_existsRequest( volume_id, volume_id_len, file_ids, response_timeout_ns ) { }
        virtual ~xtreemfs_check_file_existsSyncRequest() { }
  
        bool operator==( const xtreemfs_check_file_existsSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_get_suitable_osdsResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_get_suitable_osdsResponse() { }
        xtreemfs_get_suitable_osdsResponse( const org::xtreemfs::interfaces::StringSet& osd_uuids ) : osd_uuids( osd_uuids ) { }
        virtual ~xtreemfs_get_suitable_osdsResponse() { }
  
        void set_osd_uuids( const org::xtreemfs::interfaces::StringSet&  osd_uuids ) { this->osd_uuids = osd_uuids; }
        const org::xtreemfs::interfaces::StringSet& get_osd_uuids() const { return osd_uuids; }
  
        bool operator==( const xtreemfs_get_suitable_osdsResponse& other ) const { return osd_uuids == other.osd_uuids; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsResponse", 2352042645UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "osd_uuids" ), &osd_uuids ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "osd_uuids" ), osd_uuids ); }
  
      protected:
        org::xtreemfs::interfaces::StringSet osd_uuids;
      };
  
      class xtreemfs_get_suitable_osdsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_get_suitable_osdsRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_get_suitable_osdsRequest( const std::string& file_id, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id ) { }
        xtreemfs_get_suitable_osdsRequest( const char* file_id, size_t file_id_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_id( file_id, file_id_len ) { }
        virtual ~xtreemfs_get_suitable_osdsRequest() { }
  
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
  
        bool operator==( const xtreemfs_get_suitable_osdsRequest& other ) const { return file_id == other.file_id; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsRequest", 3264623697UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 24; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 2352042645UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsResponse; }
  
  
      protected:
        std::string file_id;
      };
  
      class xtreemfs_get_suitable_osdsSyncRequest : public xtreemfs_get_suitable_osdsRequest
      {
      public:
        xtreemfs_get_suitable_osdsSyncRequest() : xtreemfs_get_suitable_osdsRequest( std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_get_suitable_osdsSyncRequest( const std::string& file_id, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_get_suitable_osdsRequest( file_id, response_timeout_ns ) { }
        xtreemfs_get_suitable_osdsSyncRequest( const char* file_id, size_t file_id_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_get_suitable_osdsRequest( file_id, file_id_len, response_timeout_ns ) { }
        virtual ~xtreemfs_get_suitable_osdsSyncRequest() { }
  
        bool operator==( const xtreemfs_get_suitable_osdsSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_renew_capabilityResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_renew_capabilityResponse() { }
        xtreemfs_renew_capabilityResponse( const org::xtreemfs::interfaces::XCap& renewed_xcap ) : renewed_xcap( renewed_xcap ) { }
        virtual ~xtreemfs_renew_capabilityResponse() { }
  
        void set_renewed_xcap( const org::xtreemfs::interfaces::XCap&  renewed_xcap ) { this->renewed_xcap = renewed_xcap; }
        const org::xtreemfs::interfaces::XCap& get_renewed_xcap() const { return renewed_xcap; }
  
        bool operator==( const xtreemfs_renew_capabilityResponse& other ) const { return renewed_xcap == other.renewed_xcap; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityResponse", 137996173UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "renewed_xcap" ), &renewed_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "renewed_xcap" ), renewed_xcap ); }
  
      protected:
        org::xtreemfs::interfaces::XCap renewed_xcap;
      };
  
      class xtreemfs_renew_capabilityRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_renew_capabilityRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_renew_capabilityRequest( const org::xtreemfs::interfaces::XCap& old_xcap, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), old_xcap( old_xcap ) { }
        virtual ~xtreemfs_renew_capabilityRequest() { }
  
        void set_old_xcap( const org::xtreemfs::interfaces::XCap&  old_xcap ) { this->old_xcap = old_xcap; }
        const org::xtreemfs::interfaces::XCap& get_old_xcap() const { return old_xcap; }
  
        bool operator==( const xtreemfs_renew_capabilityRequest& other ) const { return old_xcap == other.old_xcap; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityRequest", 526231386UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "old_xcap" ), &old_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "old_xcap" ), old_xcap ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 25; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 137996173UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::XCap old_xcap;
      };
  
      class xtreemfs_renew_capabilitySyncRequest : public xtreemfs_renew_capabilityRequest
      {
      public:
        xtreemfs_renew_capabilitySyncRequest() : xtreemfs_renew_capabilityRequest( org::xtreemfs::interfaces::XCap(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_renew_capabilitySyncRequest( const org::xtreemfs::interfaces::XCap& old_xcap, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_renew_capabilityRequest( old_xcap, response_timeout_ns ) { }
        virtual ~xtreemfs_renew_capabilitySyncRequest() { }
  
        bool operator==( const xtreemfs_renew_capabilitySyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_replica_addResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_replica_addResponse() { }
        virtual ~xtreemfs_replica_addResponse() { }
  
        bool operator==( const xtreemfs_replica_addResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addResponse", 112572185UL );
  
      };
  
      class xtreemfs_replica_addRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_replica_addRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_replica_addRequest( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), file_id( file_id ), new_replica( new_replica ) { }
        xtreemfs_replica_addRequest( const org::xtreemfs::interfaces::Context& context, const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), file_id( file_id, file_id_len ), new_replica( new_replica ) { }
        virtual ~xtreemfs_replica_addRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_new_replica( const org::xtreemfs::interfaces::Replica&  new_replica ) { this->new_replica = new_replica; }
        const org::xtreemfs::interfaces::Replica& get_new_replica() const { return new_replica; }
  
        bool operator==( const xtreemfs_replica_addRequest& other ) const { return context == other.context && file_id == other.file_id && new_replica == other.new_replica; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addRequest", 3822735046UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Replica", "new_replica" ), &new_replica ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Replica", "new_replica" ), new_replica ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 26; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 112572185UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string file_id;
        org::xtreemfs::interfaces::Replica new_replica;
      };
  
      class xtreemfs_replica_addSyncRequest : public xtreemfs_replica_addRequest
      {
      public:
        xtreemfs_replica_addSyncRequest() : xtreemfs_replica_addRequest( org::xtreemfs::interfaces::Context(), std::string(), org::xtreemfs::interfaces::Replica(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_replica_addSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_replica_addRequest( context, file_id, new_replica, response_timeout_ns ) { }
        xtreemfs_replica_addSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_replica_addRequest( context, file_id, file_id_len, new_replica, response_timeout_ns ) { }
        virtual ~xtreemfs_replica_addSyncRequest() { }
  
        bool operator==( const xtreemfs_replica_addSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_replica_removeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_replica_removeResponse() { }
        virtual ~xtreemfs_replica_removeResponse() { }
  
        bool operator==( const xtreemfs_replica_removeResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeResponse", 1412849496UL );
  
      };
  
      class xtreemfs_replica_removeRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_replica_removeRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_replica_removeRequest( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const std::string& osd_uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), file_id( file_id ), osd_uuid( osd_uuid ) { }
        xtreemfs_replica_removeRequest( const org::xtreemfs::interfaces::Context& context, const char* file_id, size_t file_id_len, const char* osd_uuid, size_t osd_uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), context( context ), file_id( file_id, file_id_len ), osd_uuid( osd_uuid, osd_uuid_len ) { }
        virtual ~xtreemfs_replica_removeRequest() { }
  
        void set_context( const org::xtreemfs::interfaces::Context&  context ) { this->context = context; }
        const org::xtreemfs::interfaces::Context& get_context() const { return context; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_osd_uuid( const std::string& osd_uuid ) { set_osd_uuid( osd_uuid.c_str(), osd_uuid.size() ); }
        void set_osd_uuid( const char* osd_uuid, size_t osd_uuid_len = 0 ) { this->osd_uuid.assign( osd_uuid, ( osd_uuid_len != 0 ) ? osd_uuid_len : std::strlen( osd_uuid ) ); }
        const std::string& get_osd_uuid() const { return osd_uuid; }
  
        bool operator==( const xtreemfs_replica_removeRequest& other ) const { return context == other.context && file_id == other.file_id && osd_uuid == other.osd_uuid; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeRequest", 992591294UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), &context ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Context", "context" ), context ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 27; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1412849496UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::Context context;
        std::string file_id;
        std::string osd_uuid;
      };
  
      class xtreemfs_replica_removeSyncRequest : public xtreemfs_replica_removeRequest
      {
      public:
        xtreemfs_replica_removeSyncRequest() : xtreemfs_replica_removeRequest( org::xtreemfs::interfaces::Context(), std::string(), std::string(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_replica_removeSyncRequest( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const std::string& osd_uuid, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_replica_removeRequest( context, file_id, osd_uuid, response_timeout_ns ) { }
        xtreemfs_replica_removeSyncRequest( const org::xtreemfs::interfaces::Context& context, const char* file_id, size_t file_id_len, const char* osd_uuid, size_t osd_uuid_len, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_replica_removeRequest( context, file_id, file_id_len, osd_uuid, osd_uuid_len, response_timeout_ns ) { }
        virtual ~xtreemfs_replica_removeSyncRequest() { }
  
        bool operator==( const xtreemfs_replica_removeSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_restore_fileResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_restore_fileResponse() { }
        virtual ~xtreemfs_restore_fileResponse() { }
  
        bool operator==( const xtreemfs_restore_fileResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileResponse", 3701413834UL );
  
      };
  
      class xtreemfs_restore_fileRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_restore_fileRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ), file_size( 0 ), stripe_size( 0 ) { }
        xtreemfs_restore_fileRequest( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_path( file_path ), file_id( file_id ), file_size( file_size ), osd_uuid( osd_uuid ), stripe_size( stripe_size ) { }
        xtreemfs_restore_fileRequest( const char* file_path, size_t file_path_len, const char* file_id, size_t file_id_len, uint64_t file_size, const char* osd_uuid, size_t osd_uuid_len, int32_t stripe_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), file_path( file_path, file_path_len ), file_id( file_id, file_id_len ), file_size( file_size ), osd_uuid( osd_uuid, osd_uuid_len ), stripe_size( stripe_size ) { }
        virtual ~xtreemfs_restore_fileRequest() { }
  
        void set_file_path( const std::string& file_path ) { set_file_path( file_path.c_str(), file_path.size() ); }
        void set_file_path( const char* file_path, size_t file_path_len = 0 ) { this->file_path.assign( file_path, ( file_path_len != 0 ) ? file_path_len : std::strlen( file_path ) ); }
        const std::string& get_file_path() const { return file_path; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_file_size( uint64_t file_size ) { this->file_size = file_size; }
        uint64_t get_file_size() const { return file_size; }
        void set_osd_uuid( const std::string& osd_uuid ) { set_osd_uuid( osd_uuid.c_str(), osd_uuid.size() ); }
        void set_osd_uuid( const char* osd_uuid, size_t osd_uuid_len = 0 ) { this->osd_uuid.assign( osd_uuid, ( osd_uuid_len != 0 ) ? osd_uuid_len : std::strlen( osd_uuid ) ); }
        const std::string& get_osd_uuid() const { return osd_uuid; }
        void set_stripe_size( int32_t stripe_size ) { this->stripe_size = stripe_size; }
        int32_t get_stripe_size() const { return stripe_size; }
  
        bool operator==( const xtreemfs_restore_fileRequest& other ) const { return file_path == other.file_path && file_id == other.file_id && file_size == other.file_size && osd_uuid == other.osd_uuid && stripe_size == other.stripe_size; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileRequest", 1511356569UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_path" ), file_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "file_size" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); stripe_size = input_stream.readInt32( YIELD::StructuredStream::Declaration( "stripe_size" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_path" ), file_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "file_size" ), file_size ); output_stream.writeString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "stripe_size" ), stripe_size ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 28; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 3701413834UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileResponse; }
  
  
      protected:
        std::string file_path;
        std::string file_id;
        uint64_t file_size;
        std::string osd_uuid;
        int32_t stripe_size;
      };
  
      class xtreemfs_restore_fileSyncRequest : public xtreemfs_restore_fileRequest
      {
      public:
        xtreemfs_restore_fileSyncRequest() : xtreemfs_restore_fileRequest( std::string(), std::string(), 0, std::string(), 0, static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_restore_fileSyncRequest( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_restore_fileRequest( file_path, file_id, file_size, osd_uuid, stripe_size, response_timeout_ns ) { }
        xtreemfs_restore_fileSyncRequest( const char* file_path, size_t file_path_len, const char* file_id, size_t file_id_len, uint64_t file_size, const char* osd_uuid, size_t osd_uuid_len, int32_t stripe_size, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_restore_fileRequest( file_path, file_path_len, file_id, file_id_len, file_size, osd_uuid, osd_uuid_len, stripe_size, response_timeout_ns ) { }
        virtual ~xtreemfs_restore_fileSyncRequest() { }
  
        bool operator==( const xtreemfs_restore_fileSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
      class xtreemfs_update_file_sizeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_update_file_sizeResponse() { }
        virtual ~xtreemfs_update_file_sizeResponse() { }
  
        bool operator==( const xtreemfs_update_file_sizeResponse& other ) const { return true; }
  
        // YIELD::RTTI
        TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeResponse", 1410569539UL );
  
      };
  
      class xtreemfs_update_file_sizeRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_update_file_sizeRequest() : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_update_file_sizeRequest( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS( response_timeout_ns ), xcap( xcap ), osd_write_response( osd_write_response ) { }
        virtual ~xtreemfs_update_file_sizeRequest() { }
  
        void set_xcap( const org::xtreemfs::interfaces::XCap&  xcap ) { this->xcap = xcap; }
        const org::xtreemfs::interfaces::XCap& get_xcap() const { return xcap; }
        void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }
        const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }
  
        bool operator==( const xtreemfs_update_file_sizeRequest& other ) const { return xcap == other.xcap && osd_write_response == other.osd_write_response; }
  
        // YIELD::RTTI
        TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeRequest", 152425201UL );
  
        // YIELD::Serializable
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "xcap" ), &xcap ); input_stream.readSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), &osd_write_response ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "xcap" ), xcap ); output_stream.writeSerializable( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), osd_write_response ); }
  
        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 29; }
  
        virtual uint32_t getDefaultResponseTypeId() const { return 1410569539UL; }
        virtual Event* createDefaultResponse() { return new org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeResponse; }
  
  
      protected:
        org::xtreemfs::interfaces::XCap xcap;
        org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
      };
  
      class xtreemfs_update_file_sizeSyncRequest : public xtreemfs_update_file_sizeRequest
      {
      public:
        xtreemfs_update_file_sizeSyncRequest() : xtreemfs_update_file_sizeRequest( org::xtreemfs::interfaces::XCap(), org::xtreemfs::interfaces::OSDWriteResponse(), static_cast<YIELD::timeout_ns_t>( -1 ) ) { }
        xtreemfs_update_file_sizeSyncRequest( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::timeout_ns_t response_timeout_ns = static_cast<YIELD::timeout_ns_t>( -1 ) ) : xtreemfs_update_file_sizeRequest( xcap, osd_write_response, response_timeout_ns ) { }
        virtual ~xtreemfs_update_file_sizeSyncRequest() { }
  
        bool operator==( const xtreemfs_update_file_sizeSyncRequest& other ) const { return true; }
  
  
        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeResponse>( timeout_ns ); }
  
        private:
          YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };
  
  
  
        void registerSerializableFactories( YIELD::SerializableFactories& serializable_factories )
        {
          serializable_factories.registerSerializableFactory( 1117889079UL, new YIELD::SerializableFactoryImpl<admin_shutdownResponse> ); serializable_factories.registerSerializableFactory( 440700645UL, new YIELD::SerializableFactoryImpl<admin_shutdownRequest> ); serializable_factories.registerSerializableFactory( 2451351697UL, new YIELD::SerializableFactoryImpl<admin_shutdownSyncRequest> );
          serializable_factories.registerSerializableFactory( 695599363UL, new YIELD::SerializableFactoryImpl<admin_checkpointResponse> ); serializable_factories.registerSerializableFactory( 1430126549UL, new YIELD::SerializableFactoryImpl<admin_checkpointRequest> ); serializable_factories.registerSerializableFactory( 2512734138UL, new YIELD::SerializableFactoryImpl<admin_checkpointSyncRequest> );
          serializable_factories.registerSerializableFactory( 976095961UL, new YIELD::SerializableFactoryImpl<admin_dump_databaseResponse> ); serializable_factories.registerSerializableFactory( 3734644125UL, new YIELD::SerializableFactoryImpl<admin_dump_databaseRequest> ); serializable_factories.registerSerializableFactory( 3541449980UL, new YIELD::SerializableFactoryImpl<admin_dump_databaseSyncRequest> );
          serializable_factories.registerSerializableFactory( 894572093UL, new YIELD::SerializableFactoryImpl<admin_restore_databaseResponse> ); serializable_factories.registerSerializableFactory( 676678086UL, new YIELD::SerializableFactoryImpl<admin_restore_databaseRequest> ); serializable_factories.registerSerializableFactory( 1722314326UL, new YIELD::SerializableFactoryImpl<admin_restore_databaseSyncRequest> );
          serializable_factories.registerSerializableFactory( 1046856447UL, new YIELD::SerializableFactoryImpl<accessResponse> ); serializable_factories.registerSerializableFactory( 1061689358UL, new YIELD::SerializableFactoryImpl<accessRequest> ); serializable_factories.registerSerializableFactory( 970871736UL, new YIELD::SerializableFactoryImpl<accessSyncRequest> );
          serializable_factories.registerSerializableFactory( 2600293463UL, new YIELD::SerializableFactoryImpl<chmodResponse> ); serializable_factories.registerSerializableFactory( 382547319UL, new YIELD::SerializableFactoryImpl<chmodRequest> ); serializable_factories.registerSerializableFactory( 3170294911UL, new YIELD::SerializableFactoryImpl<chmodSyncRequest> );
          serializable_factories.registerSerializableFactory( 2956591049UL, new YIELD::SerializableFactoryImpl<chownResponse> ); serializable_factories.registerSerializableFactory( 1479455167UL, new YIELD::SerializableFactoryImpl<chownRequest> ); serializable_factories.registerSerializableFactory( 3658998544UL, new YIELD::SerializableFactoryImpl<chownSyncRequest> );
          serializable_factories.registerSerializableFactory( 198172638UL, new YIELD::SerializableFactoryImpl<createResponse> ); serializable_factories.registerSerializableFactory( 736916640UL, new YIELD::SerializableFactoryImpl<createRequest> ); serializable_factories.registerSerializableFactory( 14769888UL, new YIELD::SerializableFactoryImpl<createSyncRequest> );
          serializable_factories.registerSerializableFactory( 1150023493UL, new YIELD::SerializableFactoryImpl<getattrResponse> ); serializable_factories.registerSerializableFactory( 1335718504UL, new YIELD::SerializableFactoryImpl<getattrRequest> ); serializable_factories.registerSerializableFactory( 1454483433UL, new YIELD::SerializableFactoryImpl<getattrSyncRequest> );
          serializable_factories.registerSerializableFactory( 72976609UL, new YIELD::SerializableFactoryImpl<getxattrResponse> ); serializable_factories.registerSerializableFactory( 1634969716UL, new YIELD::SerializableFactoryImpl<getxattrRequest> ); serializable_factories.registerSerializableFactory( 875919323UL, new YIELD::SerializableFactoryImpl<getxattrSyncRequest> );
          serializable_factories.registerSerializableFactory( 1242382351UL, new YIELD::SerializableFactoryImpl<linkResponse> ); serializable_factories.registerSerializableFactory( 666215785UL, new YIELD::SerializableFactoryImpl<linkRequest> ); serializable_factories.registerSerializableFactory( 2091207585UL, new YIELD::SerializableFactoryImpl<linkSyncRequest> );
          serializable_factories.registerSerializableFactory( 3509031574UL, new YIELD::SerializableFactoryImpl<listxattrResponse> ); serializable_factories.registerSerializableFactory( 661933360UL, new YIELD::SerializableFactoryImpl<listxattrRequest> ); serializable_factories.registerSerializableFactory( 3667671471UL, new YIELD::SerializableFactoryImpl<listxattrSyncRequest> );
          serializable_factories.registerSerializableFactory( 1461564105UL, new YIELD::SerializableFactoryImpl<mkdirResponse> ); serializable_factories.registerSerializableFactory( 2418580920UL, new YIELD::SerializableFactoryImpl<mkdirRequest> ); serializable_factories.registerSerializableFactory( 1459969840UL, new YIELD::SerializableFactoryImpl<mkdirSyncRequest> );
          serializable_factories.registerSerializableFactory( 1350127602UL, new YIELD::SerializableFactoryImpl<mkvolResponse> ); serializable_factories.registerSerializableFactory( 2457144040UL, new YIELD::SerializableFactoryImpl<mkvolRequest> ); serializable_factories.registerSerializableFactory( 3847848853UL, new YIELD::SerializableFactoryImpl<mkvolSyncRequest> );
          serializable_factories.registerSerializableFactory( 3891582700UL, new YIELD::SerializableFactoryImpl<openResponse> ); serializable_factories.registerSerializableFactory( 2208926316UL, new YIELD::SerializableFactoryImpl<openRequest> ); serializable_factories.registerSerializableFactory( 2380774365UL, new YIELD::SerializableFactoryImpl<openSyncRequest> );
          serializable_factories.registerSerializableFactory( 42716287UL, new YIELD::SerializableFactoryImpl<readdirResponse> ); serializable_factories.registerSerializableFactory( 2159159247UL, new YIELD::SerializableFactoryImpl<readdirRequest> ); serializable_factories.registerSerializableFactory( 4016729359UL, new YIELD::SerializableFactoryImpl<readdirSyncRequest> );
          serializable_factories.registerSerializableFactory( 813148522UL, new YIELD::SerializableFactoryImpl<removexattrResponse> ); serializable_factories.registerSerializableFactory( 4111450692UL, new YIELD::SerializableFactoryImpl<removexattrRequest> ); serializable_factories.registerSerializableFactory( 1769414973UL, new YIELD::SerializableFactoryImpl<removexattrSyncRequest> );
          serializable_factories.registerSerializableFactory( 4002167429UL, new YIELD::SerializableFactoryImpl<renameResponse> ); serializable_factories.registerSerializableFactory( 229065239UL, new YIELD::SerializableFactoryImpl<renameRequest> ); serializable_factories.registerSerializableFactory( 107812048UL, new YIELD::SerializableFactoryImpl<renameSyncRequest> );
          serializable_factories.registerSerializableFactory( 161028879UL, new YIELD::SerializableFactoryImpl<rmdirResponse> ); serializable_factories.registerSerializableFactory( 1480479915UL, new YIELD::SerializableFactoryImpl<rmdirRequest> ); serializable_factories.registerSerializableFactory( 2324410537UL, new YIELD::SerializableFactoryImpl<rmdirSyncRequest> );
          serializable_factories.registerSerializableFactory( 3343228781UL, new YIELD::SerializableFactoryImpl<rmvolResponse> ); serializable_factories.registerSerializableFactory( 2429915851UL, new YIELD::SerializableFactoryImpl<rmvolRequest> ); serializable_factories.registerSerializableFactory( 2907622709UL, new YIELD::SerializableFactoryImpl<rmvolSyncRequest> );
          serializable_factories.registerSerializableFactory( 1637936981UL, new YIELD::SerializableFactoryImpl<setattrResponse> ); serializable_factories.registerSerializableFactory( 1274791757UL, new YIELD::SerializableFactoryImpl<setattrRequest> ); serializable_factories.registerSerializableFactory( 2656031770UL, new YIELD::SerializableFactoryImpl<setattrSyncRequest> );
          serializable_factories.registerSerializableFactory( 1656711865UL, new YIELD::SerializableFactoryImpl<setxattrResponse> ); serializable_factories.registerSerializableFactory( 1676810928UL, new YIELD::SerializableFactoryImpl<setxattrRequest> ); serializable_factories.registerSerializableFactory( 2121922426UL, new YIELD::SerializableFactoryImpl<setxattrSyncRequest> );
          serializable_factories.registerSerializableFactory( 2403576867UL, new YIELD::SerializableFactoryImpl<statfsResponse> ); serializable_factories.registerSerializableFactory( 1451010089UL, new YIELD::SerializableFactoryImpl<statfsRequest> ); serializable_factories.registerSerializableFactory( 4272710675UL, new YIELD::SerializableFactoryImpl<statfsSyncRequest> );
          serializable_factories.registerSerializableFactory( 3986079894UL, new YIELD::SerializableFactoryImpl<symlinkResponse> ); serializable_factories.registerSerializableFactory( 617299237UL, new YIELD::SerializableFactoryImpl<symlinkRequest> ); serializable_factories.registerSerializableFactory( 171691642UL, new YIELD::SerializableFactoryImpl<symlinkSyncRequest> );
          serializable_factories.registerSerializableFactory( 35019552UL, new YIELD::SerializableFactoryImpl<unlinkResponse> ); serializable_factories.registerSerializableFactory( 3413690043UL, new YIELD::SerializableFactoryImpl<unlinkRequest> ); serializable_factories.registerSerializableFactory( 3727349253UL, new YIELD::SerializableFactoryImpl<unlinkSyncRequest> );
          serializable_factories.registerSerializableFactory( 1258487299UL, new YIELD::SerializableFactoryImpl<utimeResponse> ); serializable_factories.registerSerializableFactory( 1774137591UL, new YIELD::SerializableFactoryImpl<utimeRequest> ); serializable_factories.registerSerializableFactory( 1876713517UL, new YIELD::SerializableFactoryImpl<utimeSyncRequest> );
          serializable_factories.registerSerializableFactory( 3802978278UL, new YIELD::SerializableFactoryImpl<xtreemfs_check_file_existsResponse> ); serializable_factories.registerSerializableFactory( 1290835505UL, new YIELD::SerializableFactoryImpl<xtreemfs_check_file_existsRequest> ); serializable_factories.registerSerializableFactory( 2593325143UL, new YIELD::SerializableFactoryImpl<xtreemfs_check_file_existsSyncRequest> );
          serializable_factories.registerSerializableFactory( 2352042645UL, new YIELD::SerializableFactoryImpl<xtreemfs_get_suitable_osdsResponse> ); serializable_factories.registerSerializableFactory( 3264623697UL, new YIELD::SerializableFactoryImpl<xtreemfs_get_suitable_osdsRequest> ); serializable_factories.registerSerializableFactory( 1264948882UL, new YIELD::SerializableFactoryImpl<xtreemfs_get_suitable_osdsSyncRequest> );
          serializable_factories.registerSerializableFactory( 137996173UL, new YIELD::SerializableFactoryImpl<xtreemfs_renew_capabilityResponse> ); serializable_factories.registerSerializableFactory( 526231386UL, new YIELD::SerializableFactoryImpl<xtreemfs_renew_capabilityRequest> ); serializable_factories.registerSerializableFactory( 2840337294UL, new YIELD::SerializableFactoryImpl<xtreemfs_renew_capabilitySyncRequest> );
          serializable_factories.registerSerializableFactory( 112572185UL, new YIELD::SerializableFactoryImpl<xtreemfs_replica_addResponse> ); serializable_factories.registerSerializableFactory( 3822735046UL, new YIELD::SerializableFactoryImpl<xtreemfs_replica_addRequest> ); serializable_factories.registerSerializableFactory( 3351137176UL, new YIELD::SerializableFactoryImpl<xtreemfs_replica_addSyncRequest> );
          serializable_factories.registerSerializableFactory( 1412849496UL, new YIELD::SerializableFactoryImpl<xtreemfs_replica_removeResponse> ); serializable_factories.registerSerializableFactory( 992591294UL, new YIELD::SerializableFactoryImpl<xtreemfs_replica_removeRequest> ); serializable_factories.registerSerializableFactory( 2117462941UL, new YIELD::SerializableFactoryImpl<xtreemfs_replica_removeSyncRequest> );
          serializable_factories.registerSerializableFactory( 3701413834UL, new YIELD::SerializableFactoryImpl<xtreemfs_restore_fileResponse> ); serializable_factories.registerSerializableFactory( 1511356569UL, new YIELD::SerializableFactoryImpl<xtreemfs_restore_fileRequest> ); serializable_factories.registerSerializableFactory( 763347303UL, new YIELD::SerializableFactoryImpl<xtreemfs_restore_fileSyncRequest> );
          serializable_factories.registerSerializableFactory( 1410569539UL, new YIELD::SerializableFactoryImpl<xtreemfs_update_file_sizeResponse> ); serializable_factories.registerSerializableFactory( 152425201UL, new YIELD::SerializableFactoryImpl<xtreemfs_update_file_sizeRequest> ); serializable_factories.registerSerializableFactory( 2236352939UL, new YIELD::SerializableFactoryImpl<xtreemfs_update_file_sizeSyncRequest> );
        }
  
  
        // EventHandler
        virtual const char* getEventHandlerName() const { return "MRCInterface"; }
  
        virtual void handleEvent( YIELD::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.getTypeId() )
            {
              case 440700645UL: handleadmin_shutdownRequest( static_cast<admin_shutdownRequest&>( ev ) ); return;
              case 1430126549UL: handleadmin_checkpointRequest( static_cast<admin_checkpointRequest&>( ev ) ); return;
              case 3734644125UL: handleadmin_dump_databaseRequest( static_cast<admin_dump_databaseRequest&>( ev ) ); return;
              case 676678086UL: handleadmin_restore_databaseRequest( static_cast<admin_restore_databaseRequest&>( ev ) ); return;
              case 1061689358UL: handleaccessRequest( static_cast<accessRequest&>( ev ) ); return;
              case 382547319UL: handlechmodRequest( static_cast<chmodRequest&>( ev ) ); return;
              case 1479455167UL: handlechownRequest( static_cast<chownRequest&>( ev ) ); return;
              case 736916640UL: handlecreateRequest( static_cast<createRequest&>( ev ) ); return;
              case 1335718504UL: handlegetattrRequest( static_cast<getattrRequest&>( ev ) ); return;
              case 1634969716UL: handlegetxattrRequest( static_cast<getxattrRequest&>( ev ) ); return;
              case 666215785UL: handlelinkRequest( static_cast<linkRequest&>( ev ) ); return;
              case 661933360UL: handlelistxattrRequest( static_cast<listxattrRequest&>( ev ) ); return;
              case 2418580920UL: handlemkdirRequest( static_cast<mkdirRequest&>( ev ) ); return;
              case 2457144040UL: handlemkvolRequest( static_cast<mkvolRequest&>( ev ) ); return;
              case 2208926316UL: handleopenRequest( static_cast<openRequest&>( ev ) ); return;
              case 2159159247UL: handlereaddirRequest( static_cast<readdirRequest&>( ev ) ); return;
              case 4111450692UL: handleremovexattrRequest( static_cast<removexattrRequest&>( ev ) ); return;
              case 229065239UL: handlerenameRequest( static_cast<renameRequest&>( ev ) ); return;
              case 1480479915UL: handlermdirRequest( static_cast<rmdirRequest&>( ev ) ); return;
              case 2429915851UL: handlermvolRequest( static_cast<rmvolRequest&>( ev ) ); return;
              case 1274791757UL: handlesetattrRequest( static_cast<setattrRequest&>( ev ) ); return;
              case 1676810928UL: handlesetxattrRequest( static_cast<setxattrRequest&>( ev ) ); return;
              case 1451010089UL: handlestatfsRequest( static_cast<statfsRequest&>( ev ) ); return;
              case 617299237UL: handlesymlinkRequest( static_cast<symlinkRequest&>( ev ) ); return;
              case 3413690043UL: handleunlinkRequest( static_cast<unlinkRequest&>( ev ) ); return;
              case 1774137591UL: handleutimeRequest( static_cast<utimeRequest&>( ev ) ); return;
              case 1290835505UL: handlextreemfs_check_file_existsRequest( static_cast<xtreemfs_check_file_existsRequest&>( ev ) ); return;
              case 3264623697UL: handlextreemfs_get_suitable_osdsRequest( static_cast<xtreemfs_get_suitable_osdsRequest&>( ev ) ); return;
              case 526231386UL: handlextreemfs_renew_capabilityRequest( static_cast<xtreemfs_renew_capabilityRequest&>( ev ) ); return;
              case 3822735046UL: handlextreemfs_replica_addRequest( static_cast<xtreemfs_replica_addRequest&>( ev ) ); return;
              case 992591294UL: handlextreemfs_replica_removeRequest( static_cast<xtreemfs_replica_removeRequest&>( ev ) ); return;
              case 1511356569UL: handlextreemfs_restore_fileRequest( static_cast<xtreemfs_restore_fileRequest&>( ev ) ); return;
              case 152425201UL: handlextreemfs_update_file_sizeRequest( static_cast<xtreemfs_update_file_sizeRequest&>( ev ) ); return;
              default: handleUnknownEvent( ev ); return;
            }
          }
          catch( YIELD::ExceptionEvent* exc_ev )
          {
            static_cast<YIELD::Request&>( ev ).respond( *exc_ev );
          }
          catch ( YIELD::ExceptionEvent& exc_ev )
          {
            static_cast<YIELD::Request&>( ev ).respond( *exc_ev.clone() );
          }
          catch ( YIELD::Exception& exc )
          {
              static_cast<YIELD::Request&>( ev ).respond( *( new YIELD::ExceptionEvent( exc.get_error_code(), exc.what() ) ) );
          }
  
          YIELD::SharedObject::decRef( ev );
        }
  
  
      protected:
          virtual void handleadmin_shutdownRequest( admin_shutdownRequest& req ) { admin_shutdownResponse* resp = NULL; try { resp = new admin_shutdownResponse; _admin_shutdown( req.get_password() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleadmin_checkpointRequest( admin_checkpointRequest& req ) { admin_checkpointResponse* resp = NULL; try { resp = new admin_checkpointResponse; _admin_checkpoint( req.get_password() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleadmin_dump_databaseRequest( admin_dump_databaseRequest& req ) { admin_dump_databaseResponse* resp = NULL; try { resp = new admin_dump_databaseResponse; _admin_dump_database( req.get_password(), req.get_dump_file() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleadmin_restore_databaseRequest( admin_restore_databaseRequest& req ) { admin_restore_databaseResponse* resp = NULL; try { resp = new admin_restore_databaseResponse; _admin_restore_database( req.get_password(), req.get_dump_file() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleaccessRequest( accessRequest& req ) { accessResponse* resp = NULL; try { resp = new accessResponse; bool _return_value = _access( req.get_context(), req.get_path(), req.get_mode() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlechmodRequest( chmodRequest& req ) { chmodResponse* resp = NULL; try { resp = new chmodResponse; _chmod( req.get_context(), req.get_path(), req.get_mode() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlechownRequest( chownRequest& req ) { chownResponse* resp = NULL; try { resp = new chownResponse; _chown( req.get_context(), req.get_path(), req.get_userId(), req.get_groupId() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlecreateRequest( createRequest& req ) { createResponse* resp = NULL; try { resp = new createResponse; _create( req.get_context(), req.get_path(), req.get_mode() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlegetattrRequest( getattrRequest& req ) { getattrResponse* resp = NULL; try { resp = new getattrResponse; org::xtreemfs::interfaces::stat_ stbuf; _getattr( req.get_context(), req.get_path(), stbuf ); resp->set_stbuf( stbuf ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlegetxattrRequest( getxattrRequest& req ) { getxattrResponse* resp = NULL; try { resp = new getxattrResponse; std::string _return_value = _getxattr( req.get_context(), req.get_path(), req.get_name() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlelinkRequest( linkRequest& req ) { linkResponse* resp = NULL; try { resp = new linkResponse; _link( req.get_context(), req.get_target_path(), req.get_link_path() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlelistxattrRequest( listxattrRequest& req ) { listxattrResponse* resp = NULL; try { resp = new listxattrResponse; org::xtreemfs::interfaces::StringSet names; _listxattr( req.get_context(), req.get_path(), names ); resp->set_names( names ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlemkdirRequest( mkdirRequest& req ) { mkdirResponse* resp = NULL; try { resp = new mkdirResponse; _mkdir( req.get_context(), req.get_path(), req.get_mode() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlemkvolRequest( mkvolRequest& req ) { mkvolResponse* resp = NULL; try { resp = new mkvolResponse; _mkvol( req.get_context(), req.get_password(), req.get_volume_name(), req.get_osd_selection_policy(), req.get_default_striping_policy(), req.get_access_control_policy() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleopenRequest( openRequest& req ) { openResponse* resp = NULL; try { resp = new openResponse; org::xtreemfs::interfaces::FileCredentials credentials; _open( req.get_context(), req.get_path(), req.get_flags(), req.get_mode(), credentials ); resp->set_credentials( credentials ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlereaddirRequest( readdirRequest& req ) { readdirResponse* resp = NULL; try { resp = new readdirResponse; org::xtreemfs::interfaces::DirectoryEntrySet directory_entries; _readdir( req.get_context(), req.get_path(), directory_entries ); resp->set_directory_entries( directory_entries ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleremovexattrRequest( removexattrRequest& req ) { removexattrResponse* resp = NULL; try { resp = new removexattrResponse; _removexattr( req.get_context(), req.get_path(), req.get_name() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlerenameRequest( renameRequest& req ) { renameResponse* resp = NULL; try { resp = new renameResponse; org::xtreemfs::interfaces::FileCredentialsSet credentials; _rename( req.get_context(), req.get_source_path(), req.get_target_path(), credentials ); resp->set_credentials( credentials ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlermdirRequest( rmdirRequest& req ) { rmdirResponse* resp = NULL; try { resp = new rmdirResponse; _rmdir( req.get_context(), req.get_path() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlermvolRequest( rmvolRequest& req ) { rmvolResponse* resp = NULL; try { resp = new rmvolResponse; _rmvol( req.get_context(), req.get_password(), req.get_volume_name() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlesetattrRequest( setattrRequest& req ) { setattrResponse* resp = NULL; try { resp = new setattrResponse; _setattr( req.get_context(), req.get_path(), req.get_stbuf() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlesetxattrRequest( setxattrRequest& req ) { setxattrResponse* resp = NULL; try { resp = new setxattrResponse; _setxattr( req.get_context(), req.get_path(), req.get_name(), req.get_value(), req.get_flags() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlestatfsRequest( statfsRequest& req ) { statfsResponse* resp = NULL; try { resp = new statfsResponse; org::xtreemfs::interfaces::statfs_ statfsbuf; _statfs( req.get_context(), req.get_volume_name(), statfsbuf ); resp->set_statfsbuf( statfsbuf ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlesymlinkRequest( symlinkRequest& req ) { symlinkResponse* resp = NULL; try { resp = new symlinkResponse; _symlink( req.get_context(), req.get_target_path(), req.get_link_path() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleunlinkRequest( unlinkRequest& req ) { unlinkResponse* resp = NULL; try { resp = new unlinkResponse; org::xtreemfs::interfaces::FileCredentialsSet credentials; _unlink( req.get_context(), req.get_path(), credentials ); resp->set_credentials( credentials ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleutimeRequest( utimeRequest& req ) { utimeResponse* resp = NULL; try { resp = new utimeResponse; _utime( req.get_context(), req.get_path(), req.get_ctime(), req.get_atime(), req.get_mtime() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_check_file_existsRequest( xtreemfs_check_file_existsRequest& req ) { xtreemfs_check_file_existsResponse* resp = NULL; try { resp = new xtreemfs_check_file_existsResponse; std::string bitmap; _xtreemfs_check_file_exists( req.get_volume_id(), req.get_file_ids(), bitmap ); resp->set_bitmap( bitmap ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_get_suitable_osdsRequest( xtreemfs_get_suitable_osdsRequest& req ) { xtreemfs_get_suitable_osdsResponse* resp = NULL; try { resp = new xtreemfs_get_suitable_osdsResponse; org::xtreemfs::interfaces::StringSet osd_uuids; _xtreemfs_get_suitable_osds( req.get_file_id(), osd_uuids ); resp->set_osd_uuids( osd_uuids ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_renew_capabilityRequest( xtreemfs_renew_capabilityRequest& req ) { xtreemfs_renew_capabilityResponse* resp = NULL; try { resp = new xtreemfs_renew_capabilityResponse; org::xtreemfs::interfaces::XCap renewed_xcap; _xtreemfs_renew_capability( req.get_old_xcap(), renewed_xcap ); resp->set_renewed_xcap( renewed_xcap ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_replica_addRequest( xtreemfs_replica_addRequest& req ) { xtreemfs_replica_addResponse* resp = NULL; try { resp = new xtreemfs_replica_addResponse; _xtreemfs_replica_add( req.get_context(), req.get_file_id(), req.get_new_replica() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_replica_removeRequest( xtreemfs_replica_removeRequest& req ) { xtreemfs_replica_removeResponse* resp = NULL; try { resp = new xtreemfs_replica_removeResponse; _xtreemfs_replica_remove( req.get_context(), req.get_file_id(), req.get_osd_uuid() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_restore_fileRequest( xtreemfs_restore_fileRequest& req ) { xtreemfs_restore_fileResponse* resp = NULL; try { resp = new xtreemfs_restore_fileResponse; _xtreemfs_restore_file( req.get_file_path(), req.get_file_id(), req.get_file_size(), req.get_osd_uuid(), req.get_stripe_size() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_update_file_sizeRequest( xtreemfs_update_file_sizeRequest& req ) { xtreemfs_update_file_sizeResponse* resp = NULL; try { resp = new xtreemfs_update_file_sizeResponse; _xtreemfs_update_file_size( req.get_xcap(), req.get_osd_write_response() ); req.respond( *resp ); YIELD::SharedObject::decRef( req ); } catch ( ... ) { throw; }; }
  
      virtual void _admin_shutdown( const std::string& password ) { }
        virtual void _admin_checkpoint( const std::string& password ) { }
        virtual void _admin_dump_database( const std::string& password, const std::string& dump_file ) { }
        virtual void _admin_restore_database( const std::string& password, const std::string& dump_file ) { }
        virtual bool _access( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { return false; }
        virtual void _chmod( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { }
        virtual void _chown( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& userId, const std::string& groupId ) { }
        virtual void _create( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { }
        virtual void _getattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::stat_& stbuf ) { }
        virtual std::string _getxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name ) { return std::string(); }
        virtual void _link( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path ) { }
        virtual void _listxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { }
        virtual void _mkdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { }
        virtual void _mkvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy ) { }
        virtual void _open( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& credentials ) { }
        virtual void _readdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) { }
        virtual void _removexattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name ) { }
        virtual void _rename( const org::xtreemfs::interfaces::Context& context, const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& credentials ) { }
        virtual void _rmdir( const org::xtreemfs::interfaces::Context& context, const std::string& path ) { }
        virtual void _rmvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name ) { }
        virtual void _setattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf ) { }
        virtual void _setxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const std::string& value, int32_t flags ) { }
        virtual void _statfs( const org::xtreemfs::interfaces::Context& context, const std::string& volume_name, org::xtreemfs::interfaces::statfs_& statfsbuf ) { }
        virtual void _symlink( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path ) { }
        virtual void _unlink( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& credentials ) { }
        virtual void _utime( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime ) { }
        virtual void _xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap ) { }
        virtual void _xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids ) { }
        virtual void _xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap ) { }
        virtual void _xtreemfs_replica_add( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) { }
        virtual void _xtreemfs_replica_remove( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const std::string& osd_uuid ) { }
        virtual void _xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) { }
        virtual void _xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { }
      };
  
      // Use this macro in an implementation class to get all of the prototypes for the operations in MRCInterface
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_PROTOTYPES \
      virtual void _admin_shutdown( const std::string& password );\
      virtual void _admin_checkpoint( const std::string& password );\
      virtual void _admin_dump_database( const std::string& password, const std::string& dump_file );\
      virtual void _admin_restore_database( const std::string& password, const std::string& dump_file );\
      virtual bool _access( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode );\
      virtual void _chmod( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode );\
      virtual void _chown( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& userId, const std::string& groupId );\
      virtual void _create( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode );\
      virtual void _getattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::stat_& stbuf );\
      virtual std::string _getxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name );\
      virtual void _link( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path );\
      virtual void _listxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::StringSet& names );\
      virtual void _mkdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode );\
      virtual void _mkvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy );\
      virtual void _open( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& credentials );\
      virtual void _readdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries );\
      virtual void _removexattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name );\
      virtual void _rename( const org::xtreemfs::interfaces::Context& context, const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& credentials );\
      virtual void _rmdir( const org::xtreemfs::interfaces::Context& context, const std::string& path );\
      virtual void _rmvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name );\
      virtual void _setattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf );\
      virtual void _setxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const std::string& value, int32_t flags );\
      virtual void _statfs( const org::xtreemfs::interfaces::Context& context, const std::string& volume_name, org::xtreemfs::interfaces::statfs_& statfsbuf );\
      virtual void _symlink( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path );\
      virtual void _unlink( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& credentials );\
      virtual void _utime( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime );\
      virtual void _xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap );\
      virtual void _xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids );\
      virtual void _xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap );\
      virtual void _xtreemfs_replica_add( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica );\
      virtual void _xtreemfs_replica_remove( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const std::string& osd_uuid );\
      virtual void _xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size );\
      virtual void _xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );
  
      // Use this macro in an implementation class to get dummy implementations for the operations in this interface
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_DUMMY_DEFINITIONS \
      virtual void _admin_shutdown( const std::string& password ) { }\
      virtual void _admin_checkpoint( const std::string& password ) { }\
      virtual void _admin_dump_database( const std::string& password, const std::string& dump_file ) { }\
      virtual void _admin_restore_database( const std::string& password, const std::string& dump_file ) { }\
      virtual bool _access( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { return false; }\
      virtual void _chmod( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { }\
      virtual void _chown( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& userId, const std::string& groupId ) { }\
      virtual void _create( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { }\
      virtual void _getattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::stat_& stbuf ) { }\
      virtual std::string _getxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name ) { return std::string(); }\
      virtual void _link( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path ) { }\
      virtual void _listxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { }\
      virtual void _mkdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t mode ) { }\
      virtual void _mkvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy ) { }\
      virtual void _open( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& credentials ) { }\
      virtual void _readdir( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) { }\
      virtual void _removexattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name ) { }\
      virtual void _rename( const org::xtreemfs::interfaces::Context& context, const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& credentials ) { }\
      virtual void _rmdir( const org::xtreemfs::interfaces::Context& context, const std::string& path ) { }\
      virtual void _rmvol( const org::xtreemfs::interfaces::Context& context, const std::string& password, const std::string& volume_name ) { }\
      virtual void _setattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf ) { }\
      virtual void _setxattr( const org::xtreemfs::interfaces::Context& context, const std::string& path, const std::string& name, const std::string& value, int32_t flags ) { }\
      virtual void _statfs( const org::xtreemfs::interfaces::Context& context, const std::string& volume_name, org::xtreemfs::interfaces::statfs_& statfsbuf ) { }\
      virtual void _symlink( const org::xtreemfs::interfaces::Context& context, const std::string& target_path, const std::string& link_path ) { }\
      virtual void _unlink( const org::xtreemfs::interfaces::Context& context, const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& credentials ) { }\
      virtual void _utime( const org::xtreemfs::interfaces::Context& context, const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime ) { }\
      virtual void _xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap ) { }\
      virtual void _xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids ) { }\
      virtual void _xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap ) { }\
      virtual void _xtreemfs_replica_add( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) { }\
      virtual void _xtreemfs_replica_remove( const org::xtreemfs::interfaces::Context& context, const std::string& file_id, const std::string& osd_uuid ) { }\
      virtual void _xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) { }\
      virtual void _xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { }
  
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handleadmin_shutdownRequest( admin_shutdownRequest& req );\
      virtual void handleadmin_checkpointRequest( admin_checkpointRequest& req );\
      virtual void handleadmin_dump_databaseRequest( admin_dump_databaseRequest& req );\
      virtual void handleadmin_restore_databaseRequest( admin_restore_databaseRequest& req );\
      virtual void handleaccessRequest( accessRequest& req );\
      virtual void handlechmodRequest( chmodRequest& req );\
      virtual void handlechownRequest( chownRequest& req );\
      virtual void handlecreateRequest( createRequest& req );\
      virtual void handlegetattrRequest( getattrRequest& req );\
      virtual void handlegetxattrRequest( getxattrRequest& req );\
      virtual void handlelinkRequest( linkRequest& req );\
      virtual void handlelistxattrRequest( listxattrRequest& req );\
      virtual void handlemkdirRequest( mkdirRequest& req );\
      virtual void handlemkvolRequest( mkvolRequest& req );\
      virtual void handleopenRequest( openRequest& req );\
      virtual void handlereaddirRequest( readdirRequest& req );\
      virtual void handleremovexattrRequest( removexattrRequest& req );\
      virtual void handlerenameRequest( renameRequest& req );\
      virtual void handlermdirRequest( rmdirRequest& req );\
      virtual void handlermvolRequest( rmvolRequest& req );\
      virtual void handlesetattrRequest( setattrRequest& req );\
      virtual void handlesetxattrRequest( setxattrRequest& req );\
      virtual void handlestatfsRequest( statfsRequest& req );\
      virtual void handlesymlinkRequest( symlinkRequest& req );\
      virtual void handleunlinkRequest( unlinkRequest& req );\
      virtual void handleutimeRequest( utimeRequest& req );\
      virtual void handlextreemfs_check_file_existsRequest( xtreemfs_check_file_existsRequest& req );\
      virtual void handlextreemfs_get_suitable_osdsRequest( xtreemfs_get_suitable_osdsRequest& req );\
      virtual void handlextreemfs_renew_capabilityRequest( xtreemfs_renew_capabilityRequest& req );\
      virtual void handlextreemfs_replica_addRequest( xtreemfs_replica_addRequest& req );\
      virtual void handlextreemfs_replica_removeRequest( xtreemfs_replica_removeRequest& req );\
      virtual void handlextreemfs_restore_fileRequest( xtreemfs_restore_fileRequest& req );\
      virtual void handlextreemfs_update_file_sizeRequest( xtreemfs_update_file_sizeRequest& req );
  
    };
  
  
  
  };
  
  

};

#endif
