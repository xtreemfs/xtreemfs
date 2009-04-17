// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_INTERFACES_MRC_INTERFACE_H
#define ORG_XTREEMFS_INTERFACES_MRC_INTERFACE_H

#include "constants.h"
#include "mrc_osd_types.h"
#include "yield/arch.h"
#include "yield/platform.h"
#include <vector>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {

      class Stat : public YIELD::Object
      {
      public:
        Stat() : mode( 0 ), nlink( 0 ), uid( 0 ), gid( 0 ), unused_dev( 0 ), size( 0 ), atime_ns( 0 ), mtime_ns( 0 ), ctime_ns( 0 ), truncate_epoch( 0 ), attributes( 0 ) { }
        Stat( uint32_t mode, uint32_t nlink, uint32_t uid, uint32_t gid, int16_t unused_dev, uint64_t size, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, const std::string& user_id, const std::string& group_id, const std::string& file_id, const std::string& link_target, uint32_t truncate_epoch, uint32_t attributes ) : mode( mode ), nlink( nlink ), uid( uid ), gid( gid ), unused_dev( unused_dev ), size( size ), atime_ns( atime_ns ), mtime_ns( mtime_ns ), ctime_ns( ctime_ns ), user_id( user_id ), group_id( group_id ), file_id( file_id ), link_target( link_target ), truncate_epoch( truncate_epoch ), attributes( attributes ) { }
        Stat( uint32_t mode, uint32_t nlink, uint32_t uid, uint32_t gid, int16_t unused_dev, uint64_t size, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, const char* user_id, size_t user_id_len, const char* group_id, size_t group_id_len, const char* file_id, size_t file_id_len, const char* link_target, size_t link_target_len, uint32_t truncate_epoch, uint32_t attributes ) : mode( mode ), nlink( nlink ), uid( uid ), gid( gid ), unused_dev( unused_dev ), size( size ), atime_ns( atime_ns ), mtime_ns( mtime_ns ), ctime_ns( ctime_ns ), user_id( user_id, user_id_len ), group_id( group_id, group_id_len ), file_id( file_id, file_id_len ), link_target( link_target, link_target_len ), truncate_epoch( truncate_epoch ), attributes( attributes ) { }
        virtual ~Stat() { }

        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
        void set_nlink( uint32_t nlink ) { this->nlink = nlink; }
        uint32_t get_nlink() const { return nlink; }
        void set_uid( uint32_t uid ) { this->uid = uid; }
        uint32_t get_uid() const { return uid; }
        void set_gid( uint32_t gid ) { this->gid = gid; }
        uint32_t get_gid() const { return gid; }
        void set_unused_dev( int16_t unused_dev ) { this->unused_dev = unused_dev; }
        int16_t get_unused_dev() const { return unused_dev; }
        void set_size( uint64_t size ) { this->size = size; }
        uint64_t get_size() const { return size; }
        void set_atime_ns( uint64_t atime_ns ) { this->atime_ns = atime_ns; }
        uint64_t get_atime_ns() const { return atime_ns; }
        void set_mtime_ns( uint64_t mtime_ns ) { this->mtime_ns = mtime_ns; }
        uint64_t get_mtime_ns() const { return mtime_ns; }
        void set_ctime_ns( uint64_t ctime_ns ) { this->ctime_ns = ctime_ns; }
        uint64_t get_ctime_ns() const { return ctime_ns; }
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
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        void set_attributes( uint32_t attributes ) { this->attributes = attributes; }
        uint32_t get_attributes() const { return attributes; }

        bool operator==( const Stat& other ) const { return mode == other.mode && nlink == other.nlink && uid == other.uid && gid == other.gid && unused_dev == other.unused_dev && size == other.size && atime_ns == other.atime_ns && mtime_ns == other.mtime_ns && ctime_ns == other.ctime_ns && user_id == other.user_id && group_id == other.group_id && file_id == other.file_id && link_target == other.link_target && truncate_epoch == other.truncate_epoch && attributes == other.attributes; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::Stat", 3149044114UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); nlink = input_stream.readUint32( YIELD::StructuredStream::Declaration( "nlink" ) ); uid = input_stream.readUint32( YIELD::StructuredStream::Declaration( "uid" ) ); gid = input_stream.readUint32( YIELD::StructuredStream::Declaration( "gid" ) ); unused_dev = input_stream.readInt16( YIELD::StructuredStream::Declaration( "unused_dev" ) ); size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "size" ) ); atime_ns = input_stream.readUint64( YIELD::StructuredStream::Declaration( "atime_ns" ) ); mtime_ns = input_stream.readUint64( YIELD::StructuredStream::Declaration( "mtime_ns" ) ); ctime_ns = input_stream.readUint64( YIELD::StructuredStream::Declaration( "ctime_ns" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "group_id" ), group_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "link_target" ), link_target ); truncate_epoch = input_stream.readUint32( YIELD::StructuredStream::Declaration( "truncate_epoch" ) ); attributes = input_stream.readUint32( YIELD::StructuredStream::Declaration( "attributes" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "nlink" ), nlink ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "uid" ), uid ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "gid" ), gid ); output_stream.writeInt16( YIELD::StructuredStream::Declaration( "unused_dev" ), unused_dev ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "size" ), size ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "atime_ns" ), atime_ns ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "mtime_ns" ), mtime_ns ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "ctime_ns" ), ctime_ns ); output_stream.writeString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "group_id" ), group_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "link_target" ), link_target ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "truncate_epoch" ), truncate_epoch ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "attributes" ), attributes ); }

      protected:
        uint32_t mode;
        uint32_t nlink;
        uint32_t uid;
        uint32_t gid;
        int16_t unused_dev;
        uint64_t size;
        uint64_t atime_ns;
        uint64_t mtime_ns;
        uint64_t ctime_ns;
        std::string user_id;
        std::string group_id;
        std::string file_id;
        std::string link_target;
        uint32_t truncate_epoch;
        uint32_t attributes;
      };

      class DirectoryEntry : public YIELD::Object
      {
      public:
        DirectoryEntry() { }
        DirectoryEntry( const std::string& name, const org::xtreemfs::interfaces::Stat& stbuf ) : name( name ), stbuf( stbuf ) { }
        DirectoryEntry( const char* name, size_t name_len, const org::xtreemfs::interfaces::Stat& stbuf ) : name( name, name_len ), stbuf( stbuf ) { }
        virtual ~DirectoryEntry() { }

        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len = 0 ) { this->name.assign( name, ( name_len != 0 ) ? name_len : std::strlen( name ) ); }
        const std::string& get_name() const { return name; }
        void set_stbuf( const org::xtreemfs::interfaces::Stat&  stbuf ) { this->stbuf = stbuf; }
        const org::xtreemfs::interfaces::Stat& get_stbuf() const { return stbuf; }

        bool operator==( const DirectoryEntry& other ) const { return name == other.name && stbuf == other.stbuf; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::DirectoryEntry", 2507394841UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Stat", "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Stat", "stbuf" ), stbuf ); }

      protected:
        std::string name;
        org::xtreemfs::interfaces::Stat stbuf;
      };

      class DirectoryEntrySet : public std::vector<org::xtreemfs::interfaces::DirectoryEntry>, public YIELD::Object
      {
      public:
        DirectoryEntrySet() { }
        DirectoryEntrySet( const org::xtreemfs::interfaces::DirectoryEntry& first_value ) { std::vector<org::xtreemfs::interfaces::DirectoryEntry>::push_back( first_value ); }
        DirectoryEntrySet( size_type size ) : std::vector<org::xtreemfs::interfaces::DirectoryEntry>( size ) { }
        virtual ~DirectoryEntrySet() { }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::DirectoryEntrySet", 3851275498UL );
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::DirectoryEntry value; input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::DirectoryEntry", "value" ), &value ); push_back( value ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::DirectoryEntry", "value" ), ( *this )[value_i] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::DirectoryEntry>::size(); }
      };

      class StatVFS : public YIELD::Object
      {
      public:
        StatVFS() : bsize( 0 ), bfree( 0 ), namelen( 0 ) { }
        StatVFS( uint32_t bsize, uint64_t bfree, const std::string& fsid, uint32_t namelen ) : bsize( bsize ), bfree( bfree ), fsid( fsid ), namelen( namelen ) { }
        StatVFS( uint32_t bsize, uint64_t bfree, const char* fsid, size_t fsid_len, uint32_t namelen ) : bsize( bsize ), bfree( bfree ), fsid( fsid, fsid_len ), namelen( namelen ) { }
        virtual ~StatVFS() { }

        void set_bsize( uint32_t bsize ) { this->bsize = bsize; }
        uint32_t get_bsize() const { return bsize; }
        void set_bfree( uint64_t bfree ) { this->bfree = bfree; }
        uint64_t get_bfree() const { return bfree; }
        void set_fsid( const std::string& fsid ) { set_fsid( fsid.c_str(), fsid.size() ); }
        void set_fsid( const char* fsid, size_t fsid_len = 0 ) { this->fsid.assign( fsid, ( fsid_len != 0 ) ? fsid_len : std::strlen( fsid ) ); }
        const std::string& get_fsid() const { return fsid; }
        void set_namelen( uint32_t namelen ) { this->namelen = namelen; }
        uint32_t get_namelen() const { return namelen; }

        bool operator==( const StatVFS& other ) const { return bsize == other.bsize && bfree == other.bfree && fsid == other.fsid && namelen == other.namelen; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::StatVFS", 3984528461UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { bsize = input_stream.readUint32( YIELD::StructuredStream::Declaration( "bsize" ) ); bfree = input_stream.readUint64( YIELD::StructuredStream::Declaration( "bfree" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "fsid" ), fsid ); namelen = input_stream.readUint32( YIELD::StructuredStream::Declaration( "namelen" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "bsize" ), bsize ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "bfree" ), bfree ); output_stream.writeString( YIELD::StructuredStream::Declaration( "fsid" ), fsid ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "namelen" ), namelen ); }

      protected:
        uint32_t bsize;
        uint64_t bfree;
        std::string fsid;
        uint32_t namelen;
      };

      class Volume : public YIELD::Object
      {
      public:
        Volume() : mode( 0 ), osd_selection_policy( OSD_SELECTION_POLICY_SIMPLE ), access_control_policy( ACCESS_CONTROL_POLICY_NULL ) { }
        Volume( const std::string& name, uint32_t mode, org::xtreemfs::interfaces::OSDSelectionPolicyType osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy, const std::string& id, const std::string& owner_user_id, const std::string& owner_group_id ) : name( name ), mode( mode ), osd_selection_policy( osd_selection_policy ), default_striping_policy( default_striping_policy ), access_control_policy( access_control_policy ), id( id ), owner_user_id( owner_user_id ), owner_group_id( owner_group_id ) { }
        Volume( const char* name, size_t name_len, uint32_t mode, org::xtreemfs::interfaces::OSDSelectionPolicyType osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy, const char* id, size_t id_len, const char* owner_user_id, size_t owner_user_id_len, const char* owner_group_id, size_t owner_group_id_len ) : name( name, name_len ), mode( mode ), osd_selection_policy( osd_selection_policy ), default_striping_policy( default_striping_policy ), access_control_policy( access_control_policy ), id( id, id_len ), owner_user_id( owner_user_id, owner_user_id_len ), owner_group_id( owner_group_id, owner_group_id_len ) { }
        virtual ~Volume() { }

        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len = 0 ) { this->name.assign( name, ( name_len != 0 ) ? name_len : std::strlen( name ) ); }
        const std::string& get_name() const { return name; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
        void set_osd_selection_policy( org::xtreemfs::interfaces::OSDSelectionPolicyType osd_selection_policy ) { this->osd_selection_policy = osd_selection_policy; }
        org::xtreemfs::interfaces::OSDSelectionPolicyType get_osd_selection_policy() const { return osd_selection_policy; }
        void set_default_striping_policy( const org::xtreemfs::interfaces::StripingPolicy&  default_striping_policy ) { this->default_striping_policy = default_striping_policy; }
        const org::xtreemfs::interfaces::StripingPolicy& get_default_striping_policy() const { return default_striping_policy; }
        void set_access_control_policy( org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy ) { this->access_control_policy = access_control_policy; }
        org::xtreemfs::interfaces::AccessControlPolicyType get_access_control_policy() const { return access_control_policy; }
        void set_id( const std::string& id ) { set_id( id.c_str(), id.size() ); }
        void set_id( const char* id, size_t id_len = 0 ) { this->id.assign( id, ( id_len != 0 ) ? id_len : std::strlen( id ) ); }
        const std::string& get_id() const { return id; }
        void set_owner_user_id( const std::string& owner_user_id ) { set_owner_user_id( owner_user_id.c_str(), owner_user_id.size() ); }
        void set_owner_user_id( const char* owner_user_id, size_t owner_user_id_len = 0 ) { this->owner_user_id.assign( owner_user_id, ( owner_user_id_len != 0 ) ? owner_user_id_len : std::strlen( owner_user_id ) ); }
        const std::string& get_owner_user_id() const { return owner_user_id; }
        void set_owner_group_id( const std::string& owner_group_id ) { set_owner_group_id( owner_group_id.c_str(), owner_group_id.size() ); }
        void set_owner_group_id( const char* owner_group_id, size_t owner_group_id_len = 0 ) { this->owner_group_id.assign( owner_group_id, ( owner_group_id_len != 0 ) ? owner_group_id_len : std::strlen( owner_group_id ) ); }
        const std::string& get_owner_group_id() const { return owner_group_id; }

        bool operator==( const Volume& other ) const { return name == other.name && mode == other.mode && osd_selection_policy == other.osd_selection_policy && default_striping_policy == other.default_striping_policy && access_control_policy == other.access_control_policy && id == other.id && owner_user_id == other.owner_user_id && owner_group_id == other.owner_group_id; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( STRUCT, "org::xtreemfs::interfaces::Volume", 2968800897UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); osd_selection_policy = ( org::xtreemfs::interfaces::OSDSelectionPolicyType )input_stream.readInt32( YIELD::StructuredStream::Declaration( "osd_selection_policy" ) ); input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StripingPolicy", "default_striping_policy" ), &default_striping_policy ); access_control_policy = ( org::xtreemfs::interfaces::AccessControlPolicyType )input_stream.readInt32( YIELD::StructuredStream::Declaration( "access_control_policy" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "id" ), id ); input_stream.readString( YIELD::StructuredStream::Declaration( "owner_user_id" ), owner_user_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "owner_group_id" ), owner_group_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "osd_selection_policy" ), osd_selection_policy ); output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StripingPolicy", "default_striping_policy" ), default_striping_policy ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "access_control_policy" ), access_control_policy ); output_stream.writeString( YIELD::StructuredStream::Declaration( "id" ), id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "owner_user_id" ), owner_user_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "owner_group_id" ), owner_group_id ); }

      protected:
        std::string name;
        uint32_t mode;
        org::xtreemfs::interfaces::OSDSelectionPolicyType osd_selection_policy;
        org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
        org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy;
        std::string id;
        std::string owner_user_id;
        std::string owner_group_id;
      };

      class VolumeSet : public std::vector<org::xtreemfs::interfaces::Volume>, public YIELD::Object
      {
      public:
        VolumeSet() { }
        VolumeSet( const org::xtreemfs::interfaces::Volume& first_value ) { std::vector<org::xtreemfs::interfaces::Volume>::push_back( first_value ); }
        VolumeSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Volume>( size ) { }
        virtual ~VolumeSet() { }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( SEQUENCE, "org::xtreemfs::interfaces::VolumeSet", 2227996177UL );
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::Volume value; input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Volume", "value" ), &value ); push_back( value ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Volume", "value" ), ( *this )[value_i] ); } }
        size_t getSize() const { return std::vector<org::xtreemfs::interfaces::Volume>::size(); }
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

      virtual bool access( const std::string& path, uint32_t mode ) { return access( path, mode, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual bool access( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target ) { return access( path, mode, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual bool access( const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns ) { return access( path, mode, NULL, response_timeout_ns ); }
        virtual bool access( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { accessSyncRequest* __req = new accessSyncRequest( path, mode ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { accessResponse& __resp = ( accessResponse& )__req->waitForDefaultResponse( response_timeout_ns ); bool _return_value = __resp.get__return_value(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); return _return_value; } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void chmod( const std::string& path, uint32_t mode ) { chmod( path, mode, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void chmod( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target ) { chmod( path, mode, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void chmod( const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns ) { chmod( path, mode, NULL, response_timeout_ns ); }
        virtual void chmod( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { chmodSyncRequest* __req = new chmodSyncRequest( path, mode ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { chmodResponse& __resp = ( chmodResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id ) { chown( path, user_id, group_id, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id, YIELD::EventTarget* send_target ) { chown( path, user_id, group_id, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id, YIELD::timeout_ns_t response_timeout_ns ) { chown( path, user_id, group_id, NULL, response_timeout_ns ); }
        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { chownSyncRequest* __req = new chownSyncRequest( path, user_id, group_id ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { chownResponse& __resp = ( chownResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void create( const std::string& path, uint32_t mode ) { create( path, mode, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void create( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target ) { create( path, mode, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void create( const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns ) { create( path, mode, NULL, response_timeout_ns ); }
        virtual void create( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { createSyncRequest* __req = new createSyncRequest( path, mode ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { createResponse& __resp = ( createResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap ) { ftruncate( write_xcap, truncate_xcap, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap, YIELD::EventTarget* send_target ) { ftruncate( write_xcap, truncate_xcap, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap, YIELD::timeout_ns_t response_timeout_ns ) { ftruncate( write_xcap, truncate_xcap, NULL, response_timeout_ns ); }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { ftruncateSyncRequest* __req = new ftruncateSyncRequest( write_xcap ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { ftruncateResponse& __resp = ( ftruncateResponse& )__req->waitForDefaultResponse( response_timeout_ns ); truncate_xcap = __resp.get_truncate_xcap(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf ) { getattr( path, stbuf, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf, YIELD::EventTarget* send_target ) { getattr( path, stbuf, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf, YIELD::timeout_ns_t response_timeout_ns ) { getattr( path, stbuf, NULL, response_timeout_ns ); }
        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { getattrSyncRequest* __req = new getattrSyncRequest( path ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { getattrResponse& __resp = ( getattrResponse& )__req->waitForDefaultResponse( response_timeout_ns ); stbuf = __resp.get_stbuf(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void getxattr( const std::string& path, const std::string& name, std::string& value ) { getxattr( path, name, value, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void getxattr( const std::string& path, const std::string& name, std::string& value, YIELD::EventTarget* send_target ) { getxattr( path, name, value, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void getxattr( const std::string& path, const std::string& name, std::string& value, YIELD::timeout_ns_t response_timeout_ns ) { getxattr( path, name, value, NULL, response_timeout_ns ); }
        virtual void getxattr( const std::string& path, const std::string& name, std::string& value, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { getxattrSyncRequest* __req = new getxattrSyncRequest( path, name ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { getxattrResponse& __resp = ( getxattrResponse& )__req->waitForDefaultResponse( response_timeout_ns ); value = __resp.get_value(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void link( const std::string& target_path, const std::string& link_path ) { link( target_path, link_path, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void link( const std::string& target_path, const std::string& link_path, YIELD::EventTarget* send_target ) { link( target_path, link_path, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void link( const std::string& target_path, const std::string& link_path, YIELD::timeout_ns_t response_timeout_ns ) { link( target_path, link_path, NULL, response_timeout_ns ); }
        virtual void link( const std::string& target_path, const std::string& link_path, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { linkSyncRequest* __req = new linkSyncRequest( target_path, link_path ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { linkResponse& __resp = ( linkResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { listxattr( path, names, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::EventTarget* send_target ) { listxattr( path, names, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::timeout_ns_t response_timeout_ns ) { listxattr( path, names, NULL, response_timeout_ns ); }
        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { listxattrSyncRequest* __req = new listxattrSyncRequest( path ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { listxattrResponse& __resp = ( listxattrResponse& )__req->waitForDefaultResponse( response_timeout_ns ); names = __resp.get_names(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void mkdir( const std::string& path, uint32_t mode ) { mkdir( path, mode, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void mkdir( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target ) { mkdir( path, mode, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void mkdir( const std::string& path, uint32_t mode, YIELD::timeout_ns_t response_timeout_ns ) { mkdir( path, mode, NULL, response_timeout_ns ); }
        virtual void mkdir( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { mkdirSyncRequest* __req = new mkdirSyncRequest( path, mode ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { mkdirResponse& __resp = ( mkdirResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& file_credentials ) { open( path, flags, mode, file_credentials, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& file_credentials, YIELD::EventTarget* send_target ) { open( path, flags, mode, file_credentials, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& file_credentials, YIELD::timeout_ns_t response_timeout_ns ) { open( path, flags, mode, file_credentials, NULL, response_timeout_ns ); }
        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& file_credentials, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { openSyncRequest* __req = new openSyncRequest( path, flags, mode ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { openResponse& __resp = ( openResponse& )__req->waitForDefaultResponse( response_timeout_ns ); file_credentials = __resp.get_file_credentials(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) { readdir( path, directory_entries, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, YIELD::EventTarget* send_target ) { readdir( path, directory_entries, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, YIELD::timeout_ns_t response_timeout_ns ) { readdir( path, directory_entries, NULL, response_timeout_ns ); }
        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { readdirSyncRequest* __req = new readdirSyncRequest( path ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { readdirResponse& __resp = ( readdirResponse& )__req->waitForDefaultResponse( response_timeout_ns ); directory_entries = __resp.get_directory_entries(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void removexattr( const std::string& path, const std::string& name ) { removexattr( path, name, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void removexattr( const std::string& path, const std::string& name, YIELD::EventTarget* send_target ) { removexattr( path, name, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void removexattr( const std::string& path, const std::string& name, YIELD::timeout_ns_t response_timeout_ns ) { removexattr( path, name, NULL, response_timeout_ns ); }
        virtual void removexattr( const std::string& path, const std::string& name, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { removexattrSyncRequest* __req = new removexattrSyncRequest( path, name ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { removexattrResponse& __resp = ( removexattrResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { rename( source_path, target_path, file_credentials, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::EventTarget* send_target ) { rename( source_path, target_path, file_credentials, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::timeout_ns_t response_timeout_ns ) { rename( source_path, target_path, file_credentials, NULL, response_timeout_ns ); }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { renameSyncRequest* __req = new renameSyncRequest( source_path, target_path ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { renameResponse& __resp = ( renameResponse& )__req->waitForDefaultResponse( response_timeout_ns ); file_credentials = __resp.get_file_credentials(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void rmdir( const std::string& path ) { rmdir( path, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rmdir( const std::string& path, YIELD::EventTarget* send_target ) { rmdir( path, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void rmdir( const std::string& path, YIELD::timeout_ns_t response_timeout_ns ) { rmdir( path, NULL, response_timeout_ns ); }
        virtual void rmdir( const std::string& path, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { rmdirSyncRequest* __req = new rmdirSyncRequest( path ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { rmdirResponse& __resp = ( rmdirResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf ) { setattr( path, stbuf, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, YIELD::EventTarget* send_target ) { setattr( path, stbuf, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, YIELD::timeout_ns_t response_timeout_ns ) { setattr( path, stbuf, NULL, response_timeout_ns ); }
        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { setattrSyncRequest* __req = new setattrSyncRequest( path, stbuf ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { setattrResponse& __resp = ( setattrResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags ) { setxattr( path, name, value, flags, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags, YIELD::EventTarget* send_target ) { setxattr( path, name, value, flags, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags, YIELD::timeout_ns_t response_timeout_ns ) { setxattr( path, name, value, flags, NULL, response_timeout_ns ); }
        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { setxattrSyncRequest* __req = new setxattrSyncRequest( path, name, value, flags ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { setxattrResponse& __resp = ( setxattrResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf ) { statvfs( volume_name, stbuf, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf, YIELD::EventTarget* send_target ) { statvfs( volume_name, stbuf, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf, YIELD::timeout_ns_t response_timeout_ns ) { statvfs( volume_name, stbuf, NULL, response_timeout_ns ); }
        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { statvfsSyncRequest* __req = new statvfsSyncRequest( volume_name ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { statvfsResponse& __resp = ( statvfsResponse& )__req->waitForDefaultResponse( response_timeout_ns ); stbuf = __resp.get_stbuf(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void symlink( const std::string& target_path, const std::string& link_path ) { symlink( target_path, link_path, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void symlink( const std::string& target_path, const std::string& link_path, YIELD::EventTarget* send_target ) { symlink( target_path, link_path, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void symlink( const std::string& target_path, const std::string& link_path, YIELD::timeout_ns_t response_timeout_ns ) { symlink( target_path, link_path, NULL, response_timeout_ns ); }
        virtual void symlink( const std::string& target_path, const std::string& link_path, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { symlinkSyncRequest* __req = new symlinkSyncRequest( target_path, link_path ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { symlinkResponse& __resp = ( symlinkResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { unlink( path, file_credentials, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::EventTarget* send_target ) { unlink( path, file_credentials, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::timeout_ns_t response_timeout_ns ) { unlink( path, file_credentials, NULL, response_timeout_ns ); }
        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { unlinkSyncRequest* __req = new unlinkSyncRequest( path ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { unlinkResponse& __resp = ( unlinkResponse& )__req->waitForDefaultResponse( response_timeout_ns ); file_credentials = __resp.get_file_credentials(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) { utimens( path, atime_ns, mtime_ns, ctime_ns, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, YIELD::EventTarget* send_target ) { utimens( path, atime_ns, mtime_ns, ctime_ns, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, YIELD::timeout_ns_t response_timeout_ns ) { utimens( path, atime_ns, mtime_ns, ctime_ns, NULL, response_timeout_ns ); }
        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { utimensSyncRequest* __req = new utimensSyncRequest( path, atime_ns, mtime_ns, ctime_ns ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { utimensResponse& __resp = ( utimensResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_checkpoint() { xtreemfs_checkpoint( NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_checkpoint( YIELD::EventTarget* send_target ) { xtreemfs_checkpoint( send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_checkpoint( YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_checkpoint( NULL, response_timeout_ns ); }
        virtual void xtreemfs_checkpoint( YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_checkpointSyncRequest* __req = new xtreemfs_checkpointSyncRequest(); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_checkpointResponse& __resp = ( xtreemfs_checkpointResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap ) { xtreemfs_check_file_exists( volume_id, file_ids, bitmap, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap, YIELD::EventTarget* send_target ) { xtreemfs_check_file_exists( volume_id, file_ids, bitmap, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_check_file_exists( volume_id, file_ids, bitmap, NULL, response_timeout_ns ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_check_file_existsSyncRequest* __req = new xtreemfs_check_file_existsSyncRequest( volume_id, file_ids ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_check_file_existsResponse& __resp = ( xtreemfs_check_file_existsResponse& )__req->waitForDefaultResponse( response_timeout_ns ); bitmap = __resp.get_bitmap(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_dump_database( const std::string& dump_file ) { xtreemfs_dump_database( dump_file, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_dump_database( const std::string& dump_file, YIELD::EventTarget* send_target ) { xtreemfs_dump_database( dump_file, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_dump_database( const std::string& dump_file, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_dump_database( dump_file, NULL, response_timeout_ns ); }
        virtual void xtreemfs_dump_database( const std::string& dump_file, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_dump_databaseSyncRequest* __req = new xtreemfs_dump_databaseSyncRequest( dump_file ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_dump_databaseResponse& __resp = ( xtreemfs_dump_databaseResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids ) { xtreemfs_get_suitable_osds( file_id, osd_uuids, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids, YIELD::EventTarget* send_target ) { xtreemfs_get_suitable_osds( file_id, osd_uuids, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_get_suitable_osds( file_id, osd_uuids, NULL, response_timeout_ns ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_get_suitable_osdsSyncRequest* __req = new xtreemfs_get_suitable_osdsSyncRequest( file_id ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_get_suitable_osdsResponse& __resp = ( xtreemfs_get_suitable_osdsResponse& )__req->waitForDefaultResponse( response_timeout_ns ); osd_uuids = __resp.get_osd_uuids(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes ) { xtreemfs_lsvol( volumes, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes, YIELD::EventTarget* send_target ) { xtreemfs_lsvol( volumes, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_lsvol( volumes, NULL, response_timeout_ns ); }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_lsvolSyncRequest* __req = new xtreemfs_lsvolSyncRequest(); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_lsvolResponse& __resp = ( xtreemfs_lsvolResponse& )__req->waitForDefaultResponse( response_timeout_ns ); volumes = __resp.get_volumes(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { xtreemfs_listdir( path, names, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::EventTarget* send_target ) { xtreemfs_listdir( path, names, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_listdir( path, names, NULL, response_timeout_ns ); }
        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_listdirSyncRequest* __req = new xtreemfs_listdirSyncRequest( path ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_listdirResponse& __resp = ( xtreemfs_listdirResponse& )__req->waitForDefaultResponse( response_timeout_ns ); names = __resp.get_names(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume ) { xtreemfs_mkvol( volume, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume, YIELD::EventTarget* send_target ) { xtreemfs_mkvol( volume, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_mkvol( volume, NULL, response_timeout_ns ); }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_mkvolSyncRequest* __req = new xtreemfs_mkvolSyncRequest( volume ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_mkvolResponse& __resp = ( xtreemfs_mkvolResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, YIELD::EventTarget* send_target ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, NULL, response_timeout_ns ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_renew_capabilitySyncRequest* __req = new xtreemfs_renew_capabilitySyncRequest( old_xcap ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_renew_capabilityResponse& __resp = ( xtreemfs_renew_capabilityResponse& )__req->waitForDefaultResponse( response_timeout_ns ); renewed_xcap = __resp.get_renewed_xcap(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) { xtreemfs_replica_add( file_id, new_replica, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::EventTarget* send_target ) { xtreemfs_replica_add( file_id, new_replica, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_replica_add( file_id, new_replica, NULL, response_timeout_ns ); }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_replica_addSyncRequest* __req = new xtreemfs_replica_addSyncRequest( file_id, new_replica ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_replica_addResponse& __resp = ( xtreemfs_replica_addResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas ) { xtreemfs_replica_list( file_id, replicas, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas, YIELD::EventTarget* send_target ) { xtreemfs_replica_list( file_id, replicas, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_replica_list( file_id, replicas, NULL, response_timeout_ns ); }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_replica_listSyncRequest* __req = new xtreemfs_replica_listSyncRequest( file_id ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_replica_listResponse& __resp = ( xtreemfs_replica_listResponse& )__req->waitForDefaultResponse( response_timeout_ns ); replicas = __resp.get_replicas(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap ) { xtreemfs_replica_remove( file_id, osd_uuid, delete_xcap, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap, YIELD::EventTarget* send_target ) { xtreemfs_replica_remove( file_id, osd_uuid, delete_xcap, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_replica_remove( file_id, osd_uuid, delete_xcap, NULL, response_timeout_ns ); }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_replica_removeSyncRequest* __req = new xtreemfs_replica_removeSyncRequest( file_id, osd_uuid ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_replica_removeResponse& __resp = ( xtreemfs_replica_removeResponse& )__req->waitForDefaultResponse( response_timeout_ns ); delete_xcap = __resp.get_delete_xcap(); YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_restore_database( const std::string& dump_file ) { xtreemfs_restore_database( dump_file, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_restore_database( const std::string& dump_file, YIELD::EventTarget* send_target ) { xtreemfs_restore_database( dump_file, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_restore_database( const std::string& dump_file, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_restore_database( dump_file, NULL, response_timeout_ns ); }
        virtual void xtreemfs_restore_database( const std::string& dump_file, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_restore_databaseSyncRequest* __req = new xtreemfs_restore_databaseSyncRequest( dump_file ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_restore_databaseResponse& __resp = ( xtreemfs_restore_databaseResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, YIELD::EventTarget* send_target ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, NULL, response_timeout_ns ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_restore_fileSyncRequest* __req = new xtreemfs_restore_fileSyncRequest( file_path, file_id, file_size, osd_uuid, stripe_size ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_restore_fileResponse& __resp = ( xtreemfs_restore_fileResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_rmvol( const std::string& volume_name ) { xtreemfs_rmvol( volume_name, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_rmvol( const std::string& volume_name, YIELD::EventTarget* send_target ) { xtreemfs_rmvol( volume_name, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_rmvol( const std::string& volume_name, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_rmvol( volume_name, NULL, response_timeout_ns ); }
        virtual void xtreemfs_rmvol( const std::string& volume_name, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_rmvolSyncRequest* __req = new xtreemfs_rmvolSyncRequest( volume_name ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_rmvolResponse& __resp = ( xtreemfs_rmvolResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_shutdown() { xtreemfs_shutdown( NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( YIELD::EventTarget* send_target ) { xtreemfs_shutdown( send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_shutdown( NULL, response_timeout_ns ); }
        virtual void xtreemfs_shutdown( YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_shutdownSyncRequest* __req = new xtreemfs_shutdownSyncRequest(); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_shutdownResponse& __resp = ( xtreemfs_shutdownResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { xtreemfs_update_file_size( xcap, osd_write_response, NULL, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target ) { xtreemfs_update_file_size( xcap, osd_write_response, send_target, static_cast<YIELD::timeout_ns_t>( -1 ) ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_update_file_size( xcap, osd_write_response, NULL, response_timeout_ns ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target, YIELD::timeout_ns_t response_timeout_ns ) { xtreemfs_update_file_sizeSyncRequest* __req = new xtreemfs_update_file_sizeSyncRequest( xcap, osd_write_response ); if ( send_target == NULL ) send_target = this; send_target->send( YIELD::Object::incRef( *__req ) ); try { xtreemfs_update_file_sizeResponse& __resp = ( xtreemfs_update_file_sizeResponse& )__req->waitForDefaultResponse( response_timeout_ns );  YIELD::Object::decRef( __resp ); YIELD::Object::decRef( *__req ); } catch ( ... ) { YIELD::Object::decRef( *__req ); throw; } }  // Request/response pair Event type definitions for the operations in MRCInterface

      class accessResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        accessResponse() : _return_value( false ) { }
        accessResponse( bool _return_value ) : _return_value( _return_value ) { }
        virtual ~accessResponse() { }

        void set__return_value( bool _return_value ) { this->_return_value = _return_value; }
        bool get__return_value() const { return _return_value; }

        bool operator==( const accessResponse& other ) const { return _return_value == other._return_value; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::accessResponse", 1046856447UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { _return_value = input_stream.readBool( YIELD::StructuredStream::Declaration( "_return_value" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeBool( YIELD::StructuredStream::Declaration( "_return_value" ), _return_value ); }

      protected:
        bool _return_value;
      };

      class accessRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        accessRequest() : mode( 0 ) { }
        accessRequest( const std::string& path, uint32_t mode ) : path( path ), mode( mode ) { }
        accessRequest( const char* path, size_t path_len, uint32_t mode ) : path( path, path_len ), mode( mode ) { }
        virtual ~accessRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }

        bool operator==( const accessRequest& other ) const { return path == other.path && mode == other.mode; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::accessRequest", 1061689358UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 1; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1046856447UL; }
        virtual Event* createDefaultResponse() { return new accessResponse; }


      protected:
        std::string path;
        uint32_t mode;
      };

      class accessSyncRequest : public accessRequest
      {
      public:
        accessSyncRequest() : accessRequest( std::string(), 0 ) { }
        accessSyncRequest( const std::string& path, uint32_t mode ) : accessRequest( path, mode ) { }
        accessSyncRequest( const char* path, size_t path_len, uint32_t mode ) : accessRequest( path, path_len, mode ) { }
        virtual ~accessSyncRequest() { }

        bool operator==( const accessSyncRequest& ) const { return true; }


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

        bool operator==( const chmodResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::chmodResponse", 2600293463UL );

      };

      class chmodRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        chmodRequest() : mode( 0 ) { }
        chmodRequest( const std::string& path, uint32_t mode ) : path( path ), mode( mode ) { }
        chmodRequest( const char* path, size_t path_len, uint32_t mode ) : path( path, path_len ), mode( mode ) { }
        virtual ~chmodRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }

        bool operator==( const chmodRequest& other ) const { return path == other.path && mode == other.mode; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::chmodRequest", 382547319UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 2; }

        virtual uint32_t getDefaultResponseTypeId() const { return 2600293463UL; }
        virtual Event* createDefaultResponse() { return new chmodResponse; }


      protected:
        std::string path;
        uint32_t mode;
      };

      class chmodSyncRequest : public chmodRequest
      {
      public:
        chmodSyncRequest() : chmodRequest( std::string(), 0 ) { }
        chmodSyncRequest( const std::string& path, uint32_t mode ) : chmodRequest( path, mode ) { }
        chmodSyncRequest( const char* path, size_t path_len, uint32_t mode ) : chmodRequest( path, path_len, mode ) { }
        virtual ~chmodSyncRequest() { }

        bool operator==( const chmodSyncRequest& ) const { return true; }


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

        bool operator==( const chownResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::chownResponse", 2956591049UL );

      };

      class chownRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        chownRequest() { }
        chownRequest( const std::string& path, const std::string& user_id, const std::string& group_id ) : path( path ), user_id( user_id ), group_id( group_id ) { }
        chownRequest( const char* path, size_t path_len, const char* user_id, size_t user_id_len, const char* group_id, size_t group_id_len ) : path( path, path_len ), user_id( user_id, user_id_len ), group_id( group_id, group_id_len ) { }
        virtual ~chownRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_user_id( const std::string& user_id ) { set_user_id( user_id.c_str(), user_id.size() ); }
        void set_user_id( const char* user_id, size_t user_id_len = 0 ) { this->user_id.assign( user_id, ( user_id_len != 0 ) ? user_id_len : std::strlen( user_id ) ); }
        const std::string& get_user_id() const { return user_id; }
        void set_group_id( const std::string& group_id ) { set_group_id( group_id.c_str(), group_id.size() ); }
        void set_group_id( const char* group_id, size_t group_id_len = 0 ) { this->group_id.assign( group_id, ( group_id_len != 0 ) ? group_id_len : std::strlen( group_id ) ); }
        const std::string& get_group_id() const { return group_id; }

        bool operator==( const chownRequest& other ) const { return path == other.path && user_id == other.user_id && group_id == other.group_id; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::chownRequest", 1479455167UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "group_id" ), group_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "group_id" ), group_id ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 3; }

        virtual uint32_t getDefaultResponseTypeId() const { return 2956591049UL; }
        virtual Event* createDefaultResponse() { return new chownResponse; }


      protected:
        std::string path;
        std::string user_id;
        std::string group_id;
      };

      class chownSyncRequest : public chownRequest
      {
      public:
        chownSyncRequest() : chownRequest( std::string(), std::string(), std::string() ) { }
        chownSyncRequest( const std::string& path, const std::string& user_id, const std::string& group_id ) : chownRequest( path, user_id, group_id ) { }
        chownSyncRequest( const char* path, size_t path_len, const char* user_id, size_t user_id_len, const char* group_id, size_t group_id_len ) : chownRequest( path, path_len, user_id, user_id_len, group_id, group_id_len ) { }
        virtual ~chownSyncRequest() { }

        bool operator==( const chownSyncRequest& ) const { return true; }


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

        bool operator==( const createResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::createResponse", 198172638UL );

      };

      class createRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        createRequest() : mode( 0 ) { }
        createRequest( const std::string& path, uint32_t mode ) : path( path ), mode( mode ) { }
        createRequest( const char* path, size_t path_len, uint32_t mode ) : path( path, path_len ), mode( mode ) { }
        virtual ~createRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }

        bool operator==( const createRequest& other ) const { return path == other.path && mode == other.mode; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::createRequest", 736916640UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 4; }

        virtual uint32_t getDefaultResponseTypeId() const { return 198172638UL; }
        virtual Event* createDefaultResponse() { return new createResponse; }


      protected:
        std::string path;
        uint32_t mode;
      };

      class createSyncRequest : public createRequest
      {
      public:
        createSyncRequest() : createRequest( std::string(), 0 ) { }
        createSyncRequest( const std::string& path, uint32_t mode ) : createRequest( path, mode ) { }
        createSyncRequest( const char* path, size_t path_len, uint32_t mode ) : createRequest( path, path_len, mode ) { }
        virtual ~createSyncRequest() { }

        bool operator==( const createSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::createResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class ftruncateResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        ftruncateResponse() { }
        ftruncateResponse( const org::xtreemfs::interfaces::XCap& truncate_xcap ) : truncate_xcap( truncate_xcap ) { }
        virtual ~ftruncateResponse() { }

        void set_truncate_xcap( const org::xtreemfs::interfaces::XCap&  truncate_xcap ) { this->truncate_xcap = truncate_xcap; }
        const org::xtreemfs::interfaces::XCap& get_truncate_xcap() const { return truncate_xcap; }

        bool operator==( const ftruncateResponse& other ) const { return truncate_xcap == other.truncate_xcap; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::ftruncateResponse", 3573723824UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "truncate_xcap" ), &truncate_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "truncate_xcap" ), truncate_xcap ); }

      protected:
        org::xtreemfs::interfaces::XCap truncate_xcap;
      };

      class ftruncateRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        ftruncateRequest() { }
        ftruncateRequest( const org::xtreemfs::interfaces::XCap& write_xcap ) : write_xcap( write_xcap ) { }
        virtual ~ftruncateRequest() { }

        void set_write_xcap( const org::xtreemfs::interfaces::XCap&  write_xcap ) { this->write_xcap = write_xcap; }
        const org::xtreemfs::interfaces::XCap& get_write_xcap() const { return write_xcap; }

        bool operator==( const ftruncateRequest& other ) const { return write_xcap == other.write_xcap; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::ftruncateRequest", 360001814UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "write_xcap" ), &write_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "write_xcap" ), write_xcap ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 30; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3573723824UL; }
        virtual Event* createDefaultResponse() { return new ftruncateResponse; }


      protected:
        org::xtreemfs::interfaces::XCap write_xcap;
      };

      class ftruncateSyncRequest : public ftruncateRequest
      {
      public:
        ftruncateSyncRequest() : ftruncateRequest( org::xtreemfs::interfaces::XCap() ) { }
        ftruncateSyncRequest( const org::xtreemfs::interfaces::XCap& write_xcap ) : ftruncateRequest( write_xcap ) { }
        virtual ~ftruncateSyncRequest() { }

        bool operator==( const ftruncateSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::ftruncateResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class getattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        getattrResponse() { }
        getattrResponse( const org::xtreemfs::interfaces::Stat& stbuf ) : stbuf( stbuf ) { }
        virtual ~getattrResponse() { }

        void set_stbuf( const org::xtreemfs::interfaces::Stat&  stbuf ) { this->stbuf = stbuf; }
        const org::xtreemfs::interfaces::Stat& get_stbuf() const { return stbuf; }

        bool operator==( const getattrResponse& other ) const { return stbuf == other.stbuf; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::getattrResponse", 1150023493UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Stat", "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Stat", "stbuf" ), stbuf ); }

      protected:
        org::xtreemfs::interfaces::Stat stbuf;
      };

      class getattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        getattrRequest() { }
        getattrRequest( const std::string& path ) : path( path ) { }
        getattrRequest( const char* path, size_t path_len ) : path( path, path_len ) { }
        virtual ~getattrRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }

        bool operator==( const getattrRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::getattrRequest", 1335718504UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 5; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1150023493UL; }
        virtual Event* createDefaultResponse() { return new getattrResponse; }


      protected:
        std::string path;
      };

      class getattrSyncRequest : public getattrRequest
      {
      public:
        getattrSyncRequest() : getattrRequest( std::string() ) { }
        getattrSyncRequest( const std::string& path ) : getattrRequest( path ) { }
        getattrSyncRequest( const char* path, size_t path_len ) : getattrRequest( path, path_len ) { }
        virtual ~getattrSyncRequest() { }

        bool operator==( const getattrSyncRequest& ) const { return true; }


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
        getxattrResponse( const std::string& value ) : value( value ) { }
        getxattrResponse( const char* value, size_t value_len ) : value( value, value_len ) { }
        virtual ~getxattrResponse() { }

        void set_value( const std::string& value ) { set_value( value.c_str(), value.size() ); }
        void set_value( const char* value, size_t value_len = 0 ) { this->value.assign( value, ( value_len != 0 ) ? value_len : std::strlen( value ) ); }
        const std::string& get_value() const { return value; }

        bool operator==( const getxattrResponse& other ) const { return value == other.value; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::getxattrResponse", 72976609UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "value" ), value ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "value" ), value ); }

      protected:
        std::string value;
      };

      class getxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        getxattrRequest() { }
        getxattrRequest( const std::string& path, const std::string& name ) : path( path ), name( name ) { }
        getxattrRequest( const char* path, size_t path_len, const char* name, size_t name_len ) : path( path, path_len ), name( name, name_len ) { }
        virtual ~getxattrRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len = 0 ) { this->name.assign( name, ( name_len != 0 ) ? name_len : std::strlen( name ) ); }
        const std::string& get_name() const { return name; }

        bool operator==( const getxattrRequest& other ) const { return path == other.path && name == other.name; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::getxattrRequest", 1634969716UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 6; }

        virtual uint32_t getDefaultResponseTypeId() const { return 72976609UL; }
        virtual Event* createDefaultResponse() { return new getxattrResponse; }


      protected:
        std::string path;
        std::string name;
      };

      class getxattrSyncRequest : public getxattrRequest
      {
      public:
        getxattrSyncRequest() : getxattrRequest( std::string(), std::string() ) { }
        getxattrSyncRequest( const std::string& path, const std::string& name ) : getxattrRequest( path, name ) { }
        getxattrSyncRequest( const char* path, size_t path_len, const char* name, size_t name_len ) : getxattrRequest( path, path_len, name, name_len ) { }
        virtual ~getxattrSyncRequest() { }

        bool operator==( const getxattrSyncRequest& ) const { return true; }


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

        bool operator==( const linkResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::linkResponse", 1242382351UL );

      };

      class linkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        linkRequest() { }
        linkRequest( const std::string& target_path, const std::string& link_path ) : target_path( target_path ), link_path( link_path ) { }
        linkRequest( const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len ) : target_path( target_path, target_path_len ), link_path( link_path, link_path_len ) { }
        virtual ~linkRequest() { }

        void set_target_path( const std::string& target_path ) { set_target_path( target_path.c_str(), target_path.size() ); }
        void set_target_path( const char* target_path, size_t target_path_len = 0 ) { this->target_path.assign( target_path, ( target_path_len != 0 ) ? target_path_len : std::strlen( target_path ) ); }
        const std::string& get_target_path() const { return target_path; }
        void set_link_path( const std::string& link_path ) { set_link_path( link_path.c_str(), link_path.size() ); }
        void set_link_path( const char* link_path, size_t link_path_len = 0 ) { this->link_path.assign( link_path, ( link_path_len != 0 ) ? link_path_len : std::strlen( link_path ) ); }
        const std::string& get_link_path() const { return link_path; }

        bool operator==( const linkRequest& other ) const { return target_path == other.target_path && link_path == other.link_path; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::linkRequest", 666215785UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 7; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1242382351UL; }
        virtual Event* createDefaultResponse() { return new linkResponse; }


      protected:
        std::string target_path;
        std::string link_path;
      };

      class linkSyncRequest : public linkRequest
      {
      public:
        linkSyncRequest() : linkRequest( std::string(), std::string() ) { }
        linkSyncRequest( const std::string& target_path, const std::string& link_path ) : linkRequest( target_path, link_path ) { }
        linkSyncRequest( const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len ) : linkRequest( target_path, target_path_len, link_path, link_path_len ) { }
        virtual ~linkSyncRequest() { }

        bool operator==( const linkSyncRequest& ) const { return true; }


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

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::listxattrResponse", 3509031574UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "names" ), &names ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "names" ), names ); }

      protected:
        org::xtreemfs::interfaces::StringSet names;
      };

      class listxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        listxattrRequest() { }
        listxattrRequest( const std::string& path ) : path( path ) { }
        listxattrRequest( const char* path, size_t path_len ) : path( path, path_len ) { }
        virtual ~listxattrRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }

        bool operator==( const listxattrRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::listxattrRequest", 661933360UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 8; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3509031574UL; }
        virtual Event* createDefaultResponse() { return new listxattrResponse; }


      protected:
        std::string path;
      };

      class listxattrSyncRequest : public listxattrRequest
      {
      public:
        listxattrSyncRequest() : listxattrRequest( std::string() ) { }
        listxattrSyncRequest( const std::string& path ) : listxattrRequest( path ) { }
        listxattrSyncRequest( const char* path, size_t path_len ) : listxattrRequest( path, path_len ) { }
        virtual ~listxattrSyncRequest() { }

        bool operator==( const listxattrSyncRequest& ) const { return true; }


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

        bool operator==( const mkdirResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::mkdirResponse", 1461564105UL );

      };

      class mkdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        mkdirRequest() : mode( 0 ) { }
        mkdirRequest( const std::string& path, uint32_t mode ) : path( path ), mode( mode ) { }
        mkdirRequest( const char* path, size_t path_len, uint32_t mode ) : path( path, path_len ), mode( mode ) { }
        virtual ~mkdirRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }

        bool operator==( const mkdirRequest& other ) const { return path == other.path && mode == other.mode; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::mkdirRequest", 2418580920UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 9; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1461564105UL; }
        virtual Event* createDefaultResponse() { return new mkdirResponse; }


      protected:
        std::string path;
        uint32_t mode;
      };

      class mkdirSyncRequest : public mkdirRequest
      {
      public:
        mkdirSyncRequest() : mkdirRequest( std::string(), 0 ) { }
        mkdirSyncRequest( const std::string& path, uint32_t mode ) : mkdirRequest( path, mode ) { }
        mkdirSyncRequest( const char* path, size_t path_len, uint32_t mode ) : mkdirRequest( path, path_len, mode ) { }
        virtual ~mkdirSyncRequest() { }

        bool operator==( const mkdirSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::mkdirResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class openResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        openResponse() { }
        openResponse( const org::xtreemfs::interfaces::FileCredentials& file_credentials ) : file_credentials( file_credentials ) { }
        virtual ~openResponse() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentials&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentials& get_file_credentials() const { return file_credentials; }

        bool operator==( const openResponse& other ) const { return file_credentials == other.file_credentials; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::openResponse", 3891582700UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), &file_credentials ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentials", "file_credentials" ), file_credentials ); }

      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
      };

      class openRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        openRequest() : flags( 0 ), mode( 0 ) { }
        openRequest( const std::string& path, uint32_t flags, uint32_t mode ) : path( path ), flags( flags ), mode( mode ) { }
        openRequest( const char* path, size_t path_len, uint32_t flags, uint32_t mode ) : path( path, path_len ), flags( flags ), mode( mode ) { }
        virtual ~openRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_flags( uint32_t flags ) { this->flags = flags; }
        uint32_t get_flags() const { return flags; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }

        bool operator==( const openRequest& other ) const { return path == other.path && flags == other.flags && mode == other.mode; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::openRequest", 2208926316UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); flags = input_stream.readUint32( YIELD::StructuredStream::Declaration( "flags" ) ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "flags" ), flags ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 11; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3891582700UL; }
        virtual Event* createDefaultResponse() { return new openResponse; }


      protected:
        std::string path;
        uint32_t flags;
        uint32_t mode;
      };

      class openSyncRequest : public openRequest
      {
      public:
        openSyncRequest() : openRequest( std::string(), 0, 0 ) { }
        openSyncRequest( const std::string& path, uint32_t flags, uint32_t mode ) : openRequest( path, flags, mode ) { }
        openSyncRequest( const char* path, size_t path_len, uint32_t flags, uint32_t mode ) : openRequest( path, path_len, flags, mode ) { }
        virtual ~openSyncRequest() { }

        bool operator==( const openSyncRequest& ) const { return true; }


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

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::readdirResponse", 42716287UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::DirectoryEntrySet", "directory_entries" ), &directory_entries ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::DirectoryEntrySet", "directory_entries" ), directory_entries ); }

      protected:
        org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
      };

      class readdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        readdirRequest() { }
        readdirRequest( const std::string& path ) : path( path ) { }
        readdirRequest( const char* path, size_t path_len ) : path( path, path_len ) { }
        virtual ~readdirRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }

        bool operator==( const readdirRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::readdirRequest", 2159159247UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 12; }

        virtual uint32_t getDefaultResponseTypeId() const { return 42716287UL; }
        virtual Event* createDefaultResponse() { return new readdirResponse; }


      protected:
        std::string path;
      };

      class readdirSyncRequest : public readdirRequest
      {
      public:
        readdirSyncRequest() : readdirRequest( std::string() ) { }
        readdirSyncRequest( const std::string& path ) : readdirRequest( path ) { }
        readdirSyncRequest( const char* path, size_t path_len ) : readdirRequest( path, path_len ) { }
        virtual ~readdirSyncRequest() { }

        bool operator==( const readdirSyncRequest& ) const { return true; }


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

        bool operator==( const removexattrResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::removexattrResponse", 813148522UL );

      };

      class removexattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        removexattrRequest() { }
        removexattrRequest( const std::string& path, const std::string& name ) : path( path ), name( name ) { }
        removexattrRequest( const char* path, size_t path_len, const char* name, size_t name_len ) : path( path, path_len ), name( name, name_len ) { }
        virtual ~removexattrRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len = 0 ) { this->name.assign( name, ( name_len != 0 ) ? name_len : std::strlen( name ) ); }
        const std::string& get_name() const { return name; }

        bool operator==( const removexattrRequest& other ) const { return path == other.path && name == other.name; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::removexattrRequest", 4111450692UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 13; }

        virtual uint32_t getDefaultResponseTypeId() const { return 813148522UL; }
        virtual Event* createDefaultResponse() { return new removexattrResponse; }


      protected:
        std::string path;
        std::string name;
      };

      class removexattrSyncRequest : public removexattrRequest
      {
      public:
        removexattrSyncRequest() : removexattrRequest( std::string(), std::string() ) { }
        removexattrSyncRequest( const std::string& path, const std::string& name ) : removexattrRequest( path, name ) { }
        removexattrSyncRequest( const char* path, size_t path_len, const char* name, size_t name_len ) : removexattrRequest( path, path_len, name, name_len ) { }
        virtual ~removexattrSyncRequest() { }

        bool operator==( const removexattrSyncRequest& ) const { return true; }


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
        renameResponse( const org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) : file_credentials( file_credentials ) { }
        virtual ~renameResponse() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentialsSet&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentialsSet& get_file_credentials() const { return file_credentials; }

        bool operator==( const renameResponse& other ) const { return file_credentials == other.file_credentials; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::renameResponse", 4002167429UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentialsSet", "file_credentials" ), &file_credentials ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentialsSet", "file_credentials" ), file_credentials ); }

      protected:
        org::xtreemfs::interfaces::FileCredentialsSet file_credentials;
      };

      class renameRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        renameRequest() { }
        renameRequest( const std::string& source_path, const std::string& target_path ) : source_path( source_path ), target_path( target_path ) { }
        renameRequest( const char* source_path, size_t source_path_len, const char* target_path, size_t target_path_len ) : source_path( source_path, source_path_len ), target_path( target_path, target_path_len ) { }
        virtual ~renameRequest() { }

        void set_source_path( const std::string& source_path ) { set_source_path( source_path.c_str(), source_path.size() ); }
        void set_source_path( const char* source_path, size_t source_path_len = 0 ) { this->source_path.assign( source_path, ( source_path_len != 0 ) ? source_path_len : std::strlen( source_path ) ); }
        const std::string& get_source_path() const { return source_path; }
        void set_target_path( const std::string& target_path ) { set_target_path( target_path.c_str(), target_path.size() ); }
        void set_target_path( const char* target_path, size_t target_path_len = 0 ) { this->target_path.assign( target_path, ( target_path_len != 0 ) ? target_path_len : std::strlen( target_path ) ); }
        const std::string& get_target_path() const { return target_path; }

        bool operator==( const renameRequest& other ) const { return source_path == other.source_path && target_path == other.target_path; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::renameRequest", 229065239UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "source_path" ), source_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "source_path" ), source_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 14; }

        virtual uint32_t getDefaultResponseTypeId() const { return 4002167429UL; }
        virtual Event* createDefaultResponse() { return new renameResponse; }


      protected:
        std::string source_path;
        std::string target_path;
      };

      class renameSyncRequest : public renameRequest
      {
      public:
        renameSyncRequest() : renameRequest( std::string(), std::string() ) { }
        renameSyncRequest( const std::string& source_path, const std::string& target_path ) : renameRequest( source_path, target_path ) { }
        renameSyncRequest( const char* source_path, size_t source_path_len, const char* target_path, size_t target_path_len ) : renameRequest( source_path, source_path_len, target_path, target_path_len ) { }
        virtual ~renameSyncRequest() { }

        bool operator==( const renameSyncRequest& ) const { return true; }


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

        bool operator==( const rmdirResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::rmdirResponse", 161028879UL );

      };

      class rmdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        rmdirRequest() { }
        rmdirRequest( const std::string& path ) : path( path ) { }
        rmdirRequest( const char* path, size_t path_len ) : path( path, path_len ) { }
        virtual ~rmdirRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }

        bool operator==( const rmdirRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::rmdirRequest", 1480479915UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 15; }

        virtual uint32_t getDefaultResponseTypeId() const { return 161028879UL; }
        virtual Event* createDefaultResponse() { return new rmdirResponse; }


      protected:
        std::string path;
      };

      class rmdirSyncRequest : public rmdirRequest
      {
      public:
        rmdirSyncRequest() : rmdirRequest( std::string() ) { }
        rmdirSyncRequest( const std::string& path ) : rmdirRequest( path ) { }
        rmdirSyncRequest( const char* path, size_t path_len ) : rmdirRequest( path, path_len ) { }
        virtual ~rmdirSyncRequest() { }

        bool operator==( const rmdirSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::rmdirResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class setattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        setattrResponse() { }
        virtual ~setattrResponse() { }

        bool operator==( const setattrResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::setattrResponse", 1637936981UL );

      };

      class setattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        setattrRequest() { }
        setattrRequest( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf ) : path( path ), stbuf( stbuf ) { }
        setattrRequest( const char* path, size_t path_len, const org::xtreemfs::interfaces::Stat& stbuf ) : path( path, path_len ), stbuf( stbuf ) { }
        virtual ~setattrRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_stbuf( const org::xtreemfs::interfaces::Stat&  stbuf ) { this->stbuf = stbuf; }
        const org::xtreemfs::interfaces::Stat& get_stbuf() const { return stbuf; }

        bool operator==( const setattrRequest& other ) const { return path == other.path && stbuf == other.stbuf; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::setattrRequest", 1274791757UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Stat", "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Stat", "stbuf" ), stbuf ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 17; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1637936981UL; }
        virtual Event* createDefaultResponse() { return new setattrResponse; }


      protected:
        std::string path;
        org::xtreemfs::interfaces::Stat stbuf;
      };

      class setattrSyncRequest : public setattrRequest
      {
      public:
        setattrSyncRequest() : setattrRequest( std::string(), org::xtreemfs::interfaces::Stat() ) { }
        setattrSyncRequest( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf ) : setattrRequest( path, stbuf ) { }
        setattrSyncRequest( const char* path, size_t path_len, const org::xtreemfs::interfaces::Stat& stbuf ) : setattrRequest( path, path_len, stbuf ) { }
        virtual ~setattrSyncRequest() { }

        bool operator==( const setattrSyncRequest& ) const { return true; }


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

        bool operator==( const setxattrResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::setxattrResponse", 1656711865UL );

      };

      class setxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        setxattrRequest() : flags( 0 ) { }
        setxattrRequest( const std::string& path, const std::string& name, const std::string& value, int32_t flags ) : path( path ), name( name ), value( value ), flags( flags ) { }
        setxattrRequest( const char* path, size_t path_len, const char* name, size_t name_len, const char* value, size_t value_len, int32_t flags ) : path( path, path_len ), name( name, name_len ), value( value, value_len ), flags( flags ) { }
        virtual ~setxattrRequest() { }

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

        bool operator==( const setxattrRequest& other ) const { return path == other.path && name == other.name && value == other.value && flags == other.flags; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::setxattrRequest", 1676810928UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); input_stream.readString( YIELD::StructuredStream::Declaration( "value" ), value ); flags = input_stream.readInt32( YIELD::StructuredStream::Declaration( "flags" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); output_stream.writeString( YIELD::StructuredStream::Declaration( "value" ), value ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "flags" ), flags ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 18; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1656711865UL; }
        virtual Event* createDefaultResponse() { return new setxattrResponse; }


      protected:
        std::string path;
        std::string name;
        std::string value;
        int32_t flags;
      };

      class setxattrSyncRequest : public setxattrRequest
      {
      public:
        setxattrSyncRequest() : setxattrRequest( std::string(), std::string(), std::string(), 0 ) { }
        setxattrSyncRequest( const std::string& path, const std::string& name, const std::string& value, int32_t flags ) : setxattrRequest( path, name, value, flags ) { }
        setxattrSyncRequest( const char* path, size_t path_len, const char* name, size_t name_len, const char* value, size_t value_len, int32_t flags ) : setxattrRequest( path, path_len, name, name_len, value, value_len, flags ) { }
        virtual ~setxattrSyncRequest() { }

        bool operator==( const setxattrSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::setxattrResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class statvfsResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        statvfsResponse() { }
        statvfsResponse( const org::xtreemfs::interfaces::StatVFS& stbuf ) : stbuf( stbuf ) { }
        virtual ~statvfsResponse() { }

        void set_stbuf( const org::xtreemfs::interfaces::StatVFS&  stbuf ) { this->stbuf = stbuf; }
        const org::xtreemfs::interfaces::StatVFS& get_stbuf() const { return stbuf; }

        bool operator==( const statvfsResponse& other ) const { return stbuf == other.stbuf; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::statvfsResponse", 1545560239UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StatVFS", "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StatVFS", "stbuf" ), stbuf ); }

      protected:
        org::xtreemfs::interfaces::StatVFS stbuf;
      };

      class statvfsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        statvfsRequest() { }
        statvfsRequest( const std::string& volume_name ) : volume_name( volume_name ) { }
        statvfsRequest( const char* volume_name, size_t volume_name_len ) : volume_name( volume_name, volume_name_len ) { }
        virtual ~statvfsRequest() { }

        void set_volume_name( const std::string& volume_name ) { set_volume_name( volume_name.c_str(), volume_name.size() ); }
        void set_volume_name( const char* volume_name, size_t volume_name_len = 0 ) { this->volume_name.assign( volume_name, ( volume_name_len != 0 ) ? volume_name_len : std::strlen( volume_name ) ); }
        const std::string& get_volume_name() const { return volume_name; }

        bool operator==( const statvfsRequest& other ) const { return volume_name == other.volume_name; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::statvfsRequest", 2662804498UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 19; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1545560239UL; }
        virtual Event* createDefaultResponse() { return new statvfsResponse; }


      protected:
        std::string volume_name;
      };

      class statvfsSyncRequest : public statvfsRequest
      {
      public:
        statvfsSyncRequest() : statvfsRequest( std::string() ) { }
        statvfsSyncRequest( const std::string& volume_name ) : statvfsRequest( volume_name ) { }
        statvfsSyncRequest( const char* volume_name, size_t volume_name_len ) : statvfsRequest( volume_name, volume_name_len ) { }
        virtual ~statvfsSyncRequest() { }

        bool operator==( const statvfsSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::statvfsResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class symlinkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        symlinkResponse() { }
        virtual ~symlinkResponse() { }

        bool operator==( const symlinkResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::symlinkResponse", 3986079894UL );

      };

      class symlinkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        symlinkRequest() { }
        symlinkRequest( const std::string& target_path, const std::string& link_path ) : target_path( target_path ), link_path( link_path ) { }
        symlinkRequest( const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len ) : target_path( target_path, target_path_len ), link_path( link_path, link_path_len ) { }
        virtual ~symlinkRequest() { }

        void set_target_path( const std::string& target_path ) { set_target_path( target_path.c_str(), target_path.size() ); }
        void set_target_path( const char* target_path, size_t target_path_len = 0 ) { this->target_path.assign( target_path, ( target_path_len != 0 ) ? target_path_len : std::strlen( target_path ) ); }
        const std::string& get_target_path() const { return target_path; }
        void set_link_path( const std::string& link_path ) { set_link_path( link_path.c_str(), link_path.size() ); }
        void set_link_path( const char* link_path, size_t link_path_len = 0 ) { this->link_path.assign( link_path, ( link_path_len != 0 ) ? link_path_len : std::strlen( link_path ) ); }
        const std::string& get_link_path() const { return link_path; }

        bool operator==( const symlinkRequest& other ) const { return target_path == other.target_path && link_path == other.link_path; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::symlinkRequest", 617299237UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 20; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3986079894UL; }
        virtual Event* createDefaultResponse() { return new symlinkResponse; }


      protected:
        std::string target_path;
        std::string link_path;
      };

      class symlinkSyncRequest : public symlinkRequest
      {
      public:
        symlinkSyncRequest() : symlinkRequest( std::string(), std::string() ) { }
        symlinkSyncRequest( const std::string& target_path, const std::string& link_path ) : symlinkRequest( target_path, link_path ) { }
        symlinkSyncRequest( const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len ) : symlinkRequest( target_path, target_path_len, link_path, link_path_len ) { }
        virtual ~symlinkSyncRequest() { }

        bool operator==( const symlinkSyncRequest& ) const { return true; }


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
        unlinkResponse( const org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) : file_credentials( file_credentials ) { }
        virtual ~unlinkResponse() { }

        void set_file_credentials( const org::xtreemfs::interfaces::FileCredentialsSet&  file_credentials ) { this->file_credentials = file_credentials; }
        const org::xtreemfs::interfaces::FileCredentialsSet& get_file_credentials() const { return file_credentials; }

        bool operator==( const unlinkResponse& other ) const { return file_credentials == other.file_credentials; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::unlinkResponse", 35019552UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentialsSet", "file_credentials" ), &file_credentials ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::FileCredentialsSet", "file_credentials" ), file_credentials ); }

      protected:
        org::xtreemfs::interfaces::FileCredentialsSet file_credentials;
      };

      class unlinkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        unlinkRequest() { }
        unlinkRequest( const std::string& path ) : path( path ) { }
        unlinkRequest( const char* path, size_t path_len ) : path( path, path_len ) { }
        virtual ~unlinkRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }

        bool operator==( const unlinkRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::unlinkRequest", 3413690043UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 21; }

        virtual uint32_t getDefaultResponseTypeId() const { return 35019552UL; }
        virtual Event* createDefaultResponse() { return new unlinkResponse; }


      protected:
        std::string path;
      };

      class unlinkSyncRequest : public unlinkRequest
      {
      public:
        unlinkSyncRequest() : unlinkRequest( std::string() ) { }
        unlinkSyncRequest( const std::string& path ) : unlinkRequest( path ) { }
        unlinkSyncRequest( const char* path, size_t path_len ) : unlinkRequest( path, path_len ) { }
        virtual ~unlinkSyncRequest() { }

        bool operator==( const unlinkSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::unlinkResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class utimensResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        utimensResponse() { }
        virtual ~utimensResponse() { }

        bool operator==( const utimensResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::utimensResponse", 187175940UL );

      };

      class utimensRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        utimensRequest() : atime_ns( 0 ), mtime_ns( 0 ), ctime_ns( 0 ) { }
        utimensRequest( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) : path( path ), atime_ns( atime_ns ), mtime_ns( mtime_ns ), ctime_ns( ctime_ns ) { }
        utimensRequest( const char* path, size_t path_len, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) : path( path, path_len ), atime_ns( atime_ns ), mtime_ns( mtime_ns ), ctime_ns( ctime_ns ) { }
        virtual ~utimensRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }
        void set_atime_ns( uint64_t atime_ns ) { this->atime_ns = atime_ns; }
        uint64_t get_atime_ns() const { return atime_ns; }
        void set_mtime_ns( uint64_t mtime_ns ) { this->mtime_ns = mtime_ns; }
        uint64_t get_mtime_ns() const { return mtime_ns; }
        void set_ctime_ns( uint64_t ctime_ns ) { this->ctime_ns = ctime_ns; }
        uint64_t get_ctime_ns() const { return ctime_ns; }

        bool operator==( const utimensRequest& other ) const { return path == other.path && atime_ns == other.atime_ns && mtime_ns == other.mtime_ns && ctime_ns == other.ctime_ns; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::utimensRequest", 3089466576UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); atime_ns = input_stream.readUint64( YIELD::StructuredStream::Declaration( "atime_ns" ) ); mtime_ns = input_stream.readUint64( YIELD::StructuredStream::Declaration( "mtime_ns" ) ); ctime_ns = input_stream.readUint64( YIELD::StructuredStream::Declaration( "ctime_ns" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "atime_ns" ), atime_ns ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "mtime_ns" ), mtime_ns ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "ctime_ns" ), ctime_ns ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 22; }

        virtual uint32_t getDefaultResponseTypeId() const { return 187175940UL; }
        virtual Event* createDefaultResponse() { return new utimensResponse; }


      protected:
        std::string path;
        uint64_t atime_ns;
        uint64_t mtime_ns;
        uint64_t ctime_ns;
      };

      class utimensSyncRequest : public utimensRequest
      {
      public:
        utimensSyncRequest() : utimensRequest( std::string(), 0, 0, 0 ) { }
        utimensSyncRequest( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) : utimensRequest( path, atime_ns, mtime_ns, ctime_ns ) { }
        utimensSyncRequest( const char* path, size_t path_len, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) : utimensRequest( path, path_len, atime_ns, mtime_ns, ctime_ns ) { }
        virtual ~utimensSyncRequest() { }

        bool operator==( const utimensSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::utimensResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_checkpointResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_checkpointResponse() { }
        virtual ~xtreemfs_checkpointResponse() { }

        bool operator==( const xtreemfs_checkpointResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_checkpointResponse", 819133164UL );

      };

      class xtreemfs_checkpointRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_checkpointRequest() { }
        virtual ~xtreemfs_checkpointRequest() { }

        bool operator==( const xtreemfs_checkpointRequest& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_checkpointRequest", 161040777UL );


        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 51; }

        virtual uint32_t getDefaultResponseTypeId() const { return 819133164UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_checkpointResponse; }

      };

      class xtreemfs_checkpointSyncRequest : public xtreemfs_checkpointRequest
      {
      public:
        xtreemfs_checkpointSyncRequest() { }
        virtual ~xtreemfs_checkpointSyncRequest() { }

        bool operator==( const xtreemfs_checkpointSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_checkpointResponse>( timeout_ns ); }

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

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsResponse", 3802978278UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "bitmap" ), bitmap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "bitmap" ), bitmap ); }

      protected:
        std::string bitmap;
      };

      class xtreemfs_check_file_existsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_check_file_existsRequest() { }
        xtreemfs_check_file_existsRequest( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids ) : volume_id( volume_id ), file_ids( file_ids ) { }
        xtreemfs_check_file_existsRequest( const char* volume_id, size_t volume_id_len, const org::xtreemfs::interfaces::StringSet& file_ids ) : volume_id( volume_id, volume_id_len ), file_ids( file_ids ) { }
        virtual ~xtreemfs_check_file_existsRequest() { }

        void set_volume_id( const std::string& volume_id ) { set_volume_id( volume_id.c_str(), volume_id.size() ); }
        void set_volume_id( const char* volume_id, size_t volume_id_len = 0 ) { this->volume_id.assign( volume_id, ( volume_id_len != 0 ) ? volume_id_len : std::strlen( volume_id ) ); }
        const std::string& get_volume_id() const { return volume_id; }
        void set_file_ids( const org::xtreemfs::interfaces::StringSet&  file_ids ) { this->file_ids = file_ids; }
        const org::xtreemfs::interfaces::StringSet& get_file_ids() const { return file_ids; }

        bool operator==( const xtreemfs_check_file_existsRequest& other ) const { return volume_id == other.volume_id && file_ids == other.file_ids; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsRequest", 1290835505UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "volume_id" ), volume_id ); input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "file_ids" ), &file_ids ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_id" ), volume_id ); output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "file_ids" ), file_ids ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 23; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3802978278UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_check_file_existsResponse; }


      protected:
        std::string volume_id;
        org::xtreemfs::interfaces::StringSet file_ids;
      };

      class xtreemfs_check_file_existsSyncRequest : public xtreemfs_check_file_existsRequest
      {
      public:
        xtreemfs_check_file_existsSyncRequest() : xtreemfs_check_file_existsRequest( std::string(), org::xtreemfs::interfaces::StringSet() ) { }
        xtreemfs_check_file_existsSyncRequest( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids ) : xtreemfs_check_file_existsRequest( volume_id, file_ids ) { }
        xtreemfs_check_file_existsSyncRequest( const char* volume_id, size_t volume_id_len, const org::xtreemfs::interfaces::StringSet& file_ids ) : xtreemfs_check_file_existsRequest( volume_id, volume_id_len, file_ids ) { }
        virtual ~xtreemfs_check_file_existsSyncRequest() { }

        bool operator==( const xtreemfs_check_file_existsSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_check_file_existsResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_dump_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_dump_databaseResponse() { }
        virtual ~xtreemfs_dump_databaseResponse() { }

        bool operator==( const xtreemfs_dump_databaseResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_dump_databaseResponse", 995967380UL );

      };

      class xtreemfs_dump_databaseRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_dump_databaseRequest() { }
        xtreemfs_dump_databaseRequest( const std::string& dump_file ) : dump_file( dump_file ) { }
        xtreemfs_dump_databaseRequest( const char* dump_file, size_t dump_file_len ) : dump_file( dump_file, dump_file_len ) { }
        virtual ~xtreemfs_dump_databaseRequest() { }

        void set_dump_file( const std::string& dump_file ) { set_dump_file( dump_file.c_str(), dump_file.size() ); }
        void set_dump_file( const char* dump_file, size_t dump_file_len = 0 ) { this->dump_file.assign( dump_file, ( dump_file_len != 0 ) ? dump_file_len : std::strlen( dump_file ) ); }
        const std::string& get_dump_file() const { return dump_file; }

        bool operator==( const xtreemfs_dump_databaseRequest& other ) const { return dump_file == other.dump_file; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_dump_databaseRequest", 2901057887UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 52; }

        virtual uint32_t getDefaultResponseTypeId() const { return 995967380UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_dump_databaseResponse; }


      protected:
        std::string dump_file;
      };

      class xtreemfs_dump_databaseSyncRequest : public xtreemfs_dump_databaseRequest
      {
      public:
        xtreemfs_dump_databaseSyncRequest() : xtreemfs_dump_databaseRequest( std::string() ) { }
        xtreemfs_dump_databaseSyncRequest( const std::string& dump_file ) : xtreemfs_dump_databaseRequest( dump_file ) { }
        xtreemfs_dump_databaseSyncRequest( const char* dump_file, size_t dump_file_len ) : xtreemfs_dump_databaseRequest( dump_file, dump_file_len ) { }
        virtual ~xtreemfs_dump_databaseSyncRequest() { }

        bool operator==( const xtreemfs_dump_databaseSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_dump_databaseResponse>( timeout_ns ); }

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

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsResponse", 2352042645UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "osd_uuids" ), &osd_uuids ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "osd_uuids" ), osd_uuids ); }

      protected:
        org::xtreemfs::interfaces::StringSet osd_uuids;
      };

      class xtreemfs_get_suitable_osdsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_get_suitable_osdsRequest() { }
        xtreemfs_get_suitable_osdsRequest( const std::string& file_id ) : file_id( file_id ) { }
        xtreemfs_get_suitable_osdsRequest( const char* file_id, size_t file_id_len ) : file_id( file_id, file_id_len ) { }
        virtual ~xtreemfs_get_suitable_osdsRequest() { }

        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }

        bool operator==( const xtreemfs_get_suitable_osdsRequest& other ) const { return file_id == other.file_id; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsRequest", 3264623697UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 24; }

        virtual uint32_t getDefaultResponseTypeId() const { return 2352042645UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_get_suitable_osdsResponse; }


      protected:
        std::string file_id;
      };

      class xtreemfs_get_suitable_osdsSyncRequest : public xtreemfs_get_suitable_osdsRequest
      {
      public:
        xtreemfs_get_suitable_osdsSyncRequest() : xtreemfs_get_suitable_osdsRequest( std::string() ) { }
        xtreemfs_get_suitable_osdsSyncRequest( const std::string& file_id ) : xtreemfs_get_suitable_osdsRequest( file_id ) { }
        xtreemfs_get_suitable_osdsSyncRequest( const char* file_id, size_t file_id_len ) : xtreemfs_get_suitable_osdsRequest( file_id, file_id_len ) { }
        virtual ~xtreemfs_get_suitable_osdsSyncRequest() { }

        bool operator==( const xtreemfs_get_suitable_osdsSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_lsvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_lsvolResponse() { }
        xtreemfs_lsvolResponse( const org::xtreemfs::interfaces::VolumeSet& volumes ) : volumes( volumes ) { }
        virtual ~xtreemfs_lsvolResponse() { }

        void set_volumes( const org::xtreemfs::interfaces::VolumeSet&  volumes ) { this->volumes = volumes; }
        const org::xtreemfs::interfaces::VolumeSet& get_volumes() const { return volumes; }

        bool operator==( const xtreemfs_lsvolResponse& other ) const { return volumes == other.volumes; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_lsvolResponse", 2554565499UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::VolumeSet", "volumes" ), &volumes ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::VolumeSet", "volumes" ), volumes ); }

      protected:
        org::xtreemfs::interfaces::VolumeSet volumes;
      };

      class xtreemfs_lsvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_lsvolRequest() { }
        virtual ~xtreemfs_lsvolRequest() { }

        bool operator==( const xtreemfs_lsvolRequest& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_lsvolRequest", 738271786UL );


        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 31; }

        virtual uint32_t getDefaultResponseTypeId() const { return 2554565499UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_lsvolResponse; }

      };

      class xtreemfs_lsvolSyncRequest : public xtreemfs_lsvolRequest
      {
      public:
        xtreemfs_lsvolSyncRequest() { }
        virtual ~xtreemfs_lsvolSyncRequest() { }

        bool operator==( const xtreemfs_lsvolSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_lsvolResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_listdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_listdirResponse() { }
        xtreemfs_listdirResponse( const org::xtreemfs::interfaces::StringSet& names ) : names( names ) { }
        virtual ~xtreemfs_listdirResponse() { }

        void set_names( const org::xtreemfs::interfaces::StringSet&  names ) { this->names = names; }
        const org::xtreemfs::interfaces::StringSet& get_names() const { return names; }

        bool operator==( const xtreemfs_listdirResponse& other ) const { return names == other.names; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_listdirResponse", 3770781012UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "names" ), &names ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::StringSet", "names" ), names ); }

      protected:
        org::xtreemfs::interfaces::StringSet names;
      };

      class xtreemfs_listdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_listdirRequest() { }
        xtreemfs_listdirRequest( const std::string& path ) : path( path ) { }
        xtreemfs_listdirRequest( const char* path, size_t path_len ) : path( path, path_len ) { }
        virtual ~xtreemfs_listdirRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len = 0 ) { this->path.assign( path, ( path_len != 0 ) ? path_len : std::strlen( path ) ); }
        const std::string& get_path() const { return path; }

        bool operator==( const xtreemfs_listdirRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_listdirRequest", 2800551134UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 33; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3770781012UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_listdirResponse; }


      protected:
        std::string path;
      };

      class xtreemfs_listdirSyncRequest : public xtreemfs_listdirRequest
      {
      public:
        xtreemfs_listdirSyncRequest() : xtreemfs_listdirRequest( std::string() ) { }
        xtreemfs_listdirSyncRequest( const std::string& path ) : xtreemfs_listdirRequest( path ) { }
        xtreemfs_listdirSyncRequest( const char* path, size_t path_len ) : xtreemfs_listdirRequest( path, path_len ) { }
        virtual ~xtreemfs_listdirSyncRequest() { }

        bool operator==( const xtreemfs_listdirSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_listdirResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_mkvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_mkvolResponse() { }
        virtual ~xtreemfs_mkvolResponse() { }

        bool operator==( const xtreemfs_mkvolResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_mkvolResponse", 3778316902UL );

      };

      class xtreemfs_mkvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_mkvolRequest() { }
        xtreemfs_mkvolRequest( const org::xtreemfs::interfaces::Volume& volume ) : volume( volume ) { }
        virtual ~xtreemfs_mkvolRequest() { }

        void set_volume( const org::xtreemfs::interfaces::Volume&  volume ) { this->volume = volume; }
        const org::xtreemfs::interfaces::Volume& get_volume() const { return volume; }

        bool operator==( const xtreemfs_mkvolRequest& other ) const { return volume == other.volume; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_mkvolRequest", 1895169082UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Volume", "volume" ), &volume ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Volume", "volume" ), volume ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 10; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3778316902UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_mkvolResponse; }


      protected:
        org::xtreemfs::interfaces::Volume volume;
      };

      class xtreemfs_mkvolSyncRequest : public xtreemfs_mkvolRequest
      {
      public:
        xtreemfs_mkvolSyncRequest() : xtreemfs_mkvolRequest( org::xtreemfs::interfaces::Volume() ) { }
        xtreemfs_mkvolSyncRequest( const org::xtreemfs::interfaces::Volume& volume ) : xtreemfs_mkvolRequest( volume ) { }
        virtual ~xtreemfs_mkvolSyncRequest() { }

        bool operator==( const xtreemfs_mkvolSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_mkvolResponse>( timeout_ns ); }

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

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityResponse", 137996173UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "renewed_xcap" ), &renewed_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "renewed_xcap" ), renewed_xcap ); }

      protected:
        org::xtreemfs::interfaces::XCap renewed_xcap;
      };

      class xtreemfs_renew_capabilityRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_renew_capabilityRequest() { }
        xtreemfs_renew_capabilityRequest( const org::xtreemfs::interfaces::XCap& old_xcap ) : old_xcap( old_xcap ) { }
        virtual ~xtreemfs_renew_capabilityRequest() { }

        void set_old_xcap( const org::xtreemfs::interfaces::XCap&  old_xcap ) { this->old_xcap = old_xcap; }
        const org::xtreemfs::interfaces::XCap& get_old_xcap() const { return old_xcap; }

        bool operator==( const xtreemfs_renew_capabilityRequest& other ) const { return old_xcap == other.old_xcap; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityRequest", 526231386UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "old_xcap" ), &old_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "old_xcap" ), old_xcap ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 25; }

        virtual uint32_t getDefaultResponseTypeId() const { return 137996173UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_renew_capabilityResponse; }


      protected:
        org::xtreemfs::interfaces::XCap old_xcap;
      };

      class xtreemfs_renew_capabilitySyncRequest : public xtreemfs_renew_capabilityRequest
      {
      public:
        xtreemfs_renew_capabilitySyncRequest() : xtreemfs_renew_capabilityRequest( org::xtreemfs::interfaces::XCap() ) { }
        xtreemfs_renew_capabilitySyncRequest( const org::xtreemfs::interfaces::XCap& old_xcap ) : xtreemfs_renew_capabilityRequest( old_xcap ) { }
        virtual ~xtreemfs_renew_capabilitySyncRequest() { }

        bool operator==( const xtreemfs_renew_capabilitySyncRequest& ) const { return true; }


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

        bool operator==( const xtreemfs_replica_addResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addResponse", 112572185UL );

      };

      class xtreemfs_replica_addRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_replica_addRequest() { }
        xtreemfs_replica_addRequest( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) : file_id( file_id ), new_replica( new_replica ) { }
        xtreemfs_replica_addRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::Replica& new_replica ) : file_id( file_id, file_id_len ), new_replica( new_replica ) { }
        virtual ~xtreemfs_replica_addRequest() { }

        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_new_replica( const org::xtreemfs::interfaces::Replica&  new_replica ) { this->new_replica = new_replica; }
        const org::xtreemfs::interfaces::Replica& get_new_replica() const { return new_replica; }

        bool operator==( const xtreemfs_replica_addRequest& other ) const { return file_id == other.file_id && new_replica == other.new_replica; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addRequest", 3822735046UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Replica", "new_replica" ), &new_replica ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::Replica", "new_replica" ), new_replica ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 26; }

        virtual uint32_t getDefaultResponseTypeId() const { return 112572185UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_replica_addResponse; }


      protected:
        std::string file_id;
        org::xtreemfs::interfaces::Replica new_replica;
      };

      class xtreemfs_replica_addSyncRequest : public xtreemfs_replica_addRequest
      {
      public:
        xtreemfs_replica_addSyncRequest() : xtreemfs_replica_addRequest( std::string(), org::xtreemfs::interfaces::Replica() ) { }
        xtreemfs_replica_addSyncRequest( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) : xtreemfs_replica_addRequest( file_id, new_replica ) { }
        xtreemfs_replica_addSyncRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::Replica& new_replica ) : xtreemfs_replica_addRequest( file_id, file_id_len, new_replica ) { }
        virtual ~xtreemfs_replica_addSyncRequest() { }

        bool operator==( const xtreemfs_replica_addSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_addResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_replica_listResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_replica_listResponse() { }
        xtreemfs_replica_listResponse( const org::xtreemfs::interfaces::ReplicaSet& replicas ) : replicas( replicas ) { }
        virtual ~xtreemfs_replica_listResponse() { }

        void set_replicas( const org::xtreemfs::interfaces::ReplicaSet&  replicas ) { this->replicas = replicas; }
        const org::xtreemfs::interfaces::ReplicaSet& get_replicas() const { return replicas; }

        bool operator==( const xtreemfs_replica_listResponse& other ) const { return replicas == other.replicas; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_listResponse", 1736837504UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ReplicaSet", "replicas" ), &replicas ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::ReplicaSet", "replicas" ), replicas ); }

      protected:
        org::xtreemfs::interfaces::ReplicaSet replicas;
      };

      class xtreemfs_replica_listRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_replica_listRequest() { }
        xtreemfs_replica_listRequest( const std::string& file_id ) : file_id( file_id ) { }
        xtreemfs_replica_listRequest( const char* file_id, size_t file_id_len ) : file_id( file_id, file_id_len ) { }
        virtual ~xtreemfs_replica_listRequest() { }

        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }

        bool operator==( const xtreemfs_replica_listRequest& other ) const { return file_id == other.file_id; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_listRequest", 3179071099UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 32; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1736837504UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_replica_listResponse; }


      protected:
        std::string file_id;
      };

      class xtreemfs_replica_listSyncRequest : public xtreemfs_replica_listRequest
      {
      public:
        xtreemfs_replica_listSyncRequest() : xtreemfs_replica_listRequest( std::string() ) { }
        xtreemfs_replica_listSyncRequest( const std::string& file_id ) : xtreemfs_replica_listRequest( file_id ) { }
        xtreemfs_replica_listSyncRequest( const char* file_id, size_t file_id_len ) : xtreemfs_replica_listRequest( file_id, file_id_len ) { }
        virtual ~xtreemfs_replica_listSyncRequest() { }

        bool operator==( const xtreemfs_replica_listSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_listResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_replica_removeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_replica_removeResponse() { }
        xtreemfs_replica_removeResponse( const org::xtreemfs::interfaces::XCap& delete_xcap ) : delete_xcap( delete_xcap ) { }
        virtual ~xtreemfs_replica_removeResponse() { }

        void set_delete_xcap( const org::xtreemfs::interfaces::XCap&  delete_xcap ) { this->delete_xcap = delete_xcap; }
        const org::xtreemfs::interfaces::XCap& get_delete_xcap() const { return delete_xcap; }

        bool operator==( const xtreemfs_replica_removeResponse& other ) const { return delete_xcap == other.delete_xcap; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeResponse", 1412849496UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "delete_xcap" ), &delete_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "delete_xcap" ), delete_xcap ); }

      protected:
        org::xtreemfs::interfaces::XCap delete_xcap;
      };

      class xtreemfs_replica_removeRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_replica_removeRequest() { }
        xtreemfs_replica_removeRequest( const std::string& file_id, const std::string& osd_uuid ) : file_id( file_id ), osd_uuid( osd_uuid ) { }
        xtreemfs_replica_removeRequest( const char* file_id, size_t file_id_len, const char* osd_uuid, size_t osd_uuid_len ) : file_id( file_id, file_id_len ), osd_uuid( osd_uuid, osd_uuid_len ) { }
        virtual ~xtreemfs_replica_removeRequest() { }

        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len = 0 ) { this->file_id.assign( file_id, ( file_id_len != 0 ) ? file_id_len : std::strlen( file_id ) ); }
        const std::string& get_file_id() const { return file_id; }
        void set_osd_uuid( const std::string& osd_uuid ) { set_osd_uuid( osd_uuid.c_str(), osd_uuid.size() ); }
        void set_osd_uuid( const char* osd_uuid, size_t osd_uuid_len = 0 ) { this->osd_uuid.assign( osd_uuid, ( osd_uuid_len != 0 ) ? osd_uuid_len : std::strlen( osd_uuid ) ); }
        const std::string& get_osd_uuid() const { return osd_uuid; }

        bool operator==( const xtreemfs_replica_removeRequest& other ) const { return file_id == other.file_id && osd_uuid == other.osd_uuid; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeRequest", 992591294UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 27; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1412849496UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_replica_removeResponse; }


      protected:
        std::string file_id;
        std::string osd_uuid;
      };

      class xtreemfs_replica_removeSyncRequest : public xtreemfs_replica_removeRequest
      {
      public:
        xtreemfs_replica_removeSyncRequest() : xtreemfs_replica_removeRequest( std::string(), std::string() ) { }
        xtreemfs_replica_removeSyncRequest( const std::string& file_id, const std::string& osd_uuid ) : xtreemfs_replica_removeRequest( file_id, osd_uuid ) { }
        xtreemfs_replica_removeSyncRequest( const char* file_id, size_t file_id_len, const char* osd_uuid, size_t osd_uuid_len ) : xtreemfs_replica_removeRequest( file_id, file_id_len, osd_uuid, osd_uuid_len ) { }
        virtual ~xtreemfs_replica_removeSyncRequest() { }

        bool operator==( const xtreemfs_replica_removeSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_restore_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_restore_databaseResponse() { }
        virtual ~xtreemfs_restore_databaseResponse() { }

        bool operator==( const xtreemfs_restore_databaseResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_databaseResponse", 1807651157UL );

      };

      class xtreemfs_restore_databaseRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_restore_databaseRequest() { }
        xtreemfs_restore_databaseRequest( const std::string& dump_file ) : dump_file( dump_file ) { }
        xtreemfs_restore_databaseRequest( const char* dump_file, size_t dump_file_len ) : dump_file( dump_file, dump_file_len ) { }
        virtual ~xtreemfs_restore_databaseRequest() { }

        void set_dump_file( const std::string& dump_file ) { set_dump_file( dump_file.c_str(), dump_file.size() ); }
        void set_dump_file( const char* dump_file, size_t dump_file_len = 0 ) { this->dump_file.assign( dump_file, ( dump_file_len != 0 ) ? dump_file_len : std::strlen( dump_file ) ); }
        const std::string& get_dump_file() const { return dump_file; }

        bool operator==( const xtreemfs_restore_databaseRequest& other ) const { return dump_file == other.dump_file; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_databaseRequest", 576859361UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 53; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1807651157UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_restore_databaseResponse; }


      protected:
        std::string dump_file;
      };

      class xtreemfs_restore_databaseSyncRequest : public xtreemfs_restore_databaseRequest
      {
      public:
        xtreemfs_restore_databaseSyncRequest() : xtreemfs_restore_databaseRequest( std::string() ) { }
        xtreemfs_restore_databaseSyncRequest( const std::string& dump_file ) : xtreemfs_restore_databaseRequest( dump_file ) { }
        xtreemfs_restore_databaseSyncRequest( const char* dump_file, size_t dump_file_len ) : xtreemfs_restore_databaseRequest( dump_file, dump_file_len ) { }
        virtual ~xtreemfs_restore_databaseSyncRequest() { }

        bool operator==( const xtreemfs_restore_databaseSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_databaseResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_restore_fileResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_restore_fileResponse() { }
        virtual ~xtreemfs_restore_fileResponse() { }

        bool operator==( const xtreemfs_restore_fileResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileResponse", 3701413834UL );

      };

      class xtreemfs_restore_fileRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_restore_fileRequest() : file_size( 0 ), stripe_size( 0 ) { }
        xtreemfs_restore_fileRequest( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) : file_path( file_path ), file_id( file_id ), file_size( file_size ), osd_uuid( osd_uuid ), stripe_size( stripe_size ) { }
        xtreemfs_restore_fileRequest( const char* file_path, size_t file_path_len, const char* file_id, size_t file_id_len, uint64_t file_size, const char* osd_uuid, size_t osd_uuid_len, int32_t stripe_size ) : file_path( file_path, file_path_len ), file_id( file_id, file_id_len ), file_size( file_size ), osd_uuid( osd_uuid, osd_uuid_len ), stripe_size( stripe_size ) { }
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

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileRequest", 1511356569UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_path" ), file_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "file_size" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); stripe_size = input_stream.readInt32( YIELD::StructuredStream::Declaration( "stripe_size" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_path" ), file_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "file_size" ), file_size ); output_stream.writeString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "stripe_size" ), stripe_size ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 28; }

        virtual uint32_t getDefaultResponseTypeId() const { return 3701413834UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_restore_fileResponse; }


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
        xtreemfs_restore_fileSyncRequest() : xtreemfs_restore_fileRequest( std::string(), std::string(), 0, std::string(), 0 ) { }
        xtreemfs_restore_fileSyncRequest( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) : xtreemfs_restore_fileRequest( file_path, file_id, file_size, osd_uuid, stripe_size ) { }
        xtreemfs_restore_fileSyncRequest( const char* file_path, size_t file_path_len, const char* file_id, size_t file_id_len, uint64_t file_size, const char* osd_uuid, size_t osd_uuid_len, int32_t stripe_size ) : xtreemfs_restore_fileRequest( file_path, file_path_len, file_id, file_id_len, file_size, osd_uuid, osd_uuid_len, stripe_size ) { }
        virtual ~xtreemfs_restore_fileSyncRequest() { }

        bool operator==( const xtreemfs_restore_fileSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_restore_fileResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_rmvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_rmvolResponse() { }
        virtual ~xtreemfs_rmvolResponse() { }

        bool operator==( const xtreemfs_rmvolResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_rmvolResponse", 180525667UL );

      };

      class xtreemfs_rmvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_rmvolRequest() { }
        xtreemfs_rmvolRequest( const std::string& volume_name ) : volume_name( volume_name ) { }
        xtreemfs_rmvolRequest( const char* volume_name, size_t volume_name_len ) : volume_name( volume_name, volume_name_len ) { }
        virtual ~xtreemfs_rmvolRequest() { }

        void set_volume_name( const std::string& volume_name ) { set_volume_name( volume_name.c_str(), volume_name.size() ); }
        void set_volume_name( const char* volume_name, size_t volume_name_len = 0 ) { this->volume_name.assign( volume_name, ( volume_name_len != 0 ) ? volume_name_len : std::strlen( volume_name ) ); }
        const std::string& get_volume_name() const { return volume_name; }

        bool operator==( const xtreemfs_rmvolRequest& other ) const { return volume_name == other.volume_name; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_rmvolRequest", 981073148UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 16; }

        virtual uint32_t getDefaultResponseTypeId() const { return 180525667UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_rmvolResponse; }


      protected:
        std::string volume_name;
      };

      class xtreemfs_rmvolSyncRequest : public xtreemfs_rmvolRequest
      {
      public:
        xtreemfs_rmvolSyncRequest() : xtreemfs_rmvolRequest( std::string() ) { }
        xtreemfs_rmvolSyncRequest( const std::string& volume_name ) : xtreemfs_rmvolRequest( volume_name ) { }
        xtreemfs_rmvolSyncRequest( const char* volume_name, size_t volume_name_len ) : xtreemfs_rmvolRequest( volume_name, volume_name_len ) { }
        virtual ~xtreemfs_rmvolSyncRequest() { }

        bool operator==( const xtreemfs_rmvolSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_rmvolResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_shutdownResponse() { }
        virtual ~xtreemfs_shutdownResponse() { }

        bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_shutdownResponse", 1924750368UL );

      };

      class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_shutdownRequest() { }
        virtual ~xtreemfs_shutdownRequest() { }

        bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_shutdownRequest", 3905865114UL );


        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 50; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1924750368UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_shutdownResponse; }

      };

      class xtreemfs_shutdownSyncRequest : public xtreemfs_shutdownRequest
      {
      public:
        xtreemfs_shutdownSyncRequest() { }
        virtual ~xtreemfs_shutdownSyncRequest() { }

        bool operator==( const xtreemfs_shutdownSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_shutdownResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

      class xtreemfs_update_file_sizeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_update_file_sizeResponse() { }
        virtual ~xtreemfs_update_file_sizeResponse() { }

        bool operator==( const xtreemfs_update_file_sizeResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( RESPONSE, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeResponse", 1410569539UL );

      };

      class xtreemfs_update_file_sizeRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_update_file_sizeRequest() { }
        xtreemfs_update_file_sizeRequest( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) : xcap( xcap ), osd_write_response( osd_write_response ) { }
        virtual ~xtreemfs_update_file_sizeRequest() { }

        void set_xcap( const org::xtreemfs::interfaces::XCap&  xcap ) { this->xcap = xcap; }
        const org::xtreemfs::interfaces::XCap& get_xcap() const { return xcap; }
        void set_osd_write_response( const org::xtreemfs::interfaces::OSDWriteResponse&  osd_write_response ) { this->osd_write_response = osd_write_response; }
        const org::xtreemfs::interfaces::OSDWriteResponse& get_osd_write_response() const { return osd_write_response; }

        bool operator==( const xtreemfs_update_file_sizeRequest& other ) const { return xcap == other.xcap && osd_write_response == other.osd_write_response; }

        // YIELD::Object
        YIELD_OBJECT_TYPE_INFO( REQUEST, "org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeRequest", 152425201UL );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "xcap" ), &xcap ); input_stream.readObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), &osd_write_response ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::XCap", "xcap" ), xcap ); output_stream.writeObject( YIELD::StructuredStream::Declaration( "org::xtreemfs::interfaces::OSDWriteResponse", "osd_write_response" ), osd_write_response ); }

        // YIELD::Request
        virtual uint32_t getInterfaceNumber() const { return 2; }
        virtual uint32_t getOperationNumber() const { return 29; }

        virtual uint32_t getDefaultResponseTypeId() const { return 1410569539UL; }
        virtual Event* createDefaultResponse() { return new xtreemfs_update_file_sizeResponse; }


      protected:
        org::xtreemfs::interfaces::XCap xcap;
        org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
      };

      class xtreemfs_update_file_sizeSyncRequest : public xtreemfs_update_file_sizeRequest
      {
      public:
        xtreemfs_update_file_sizeSyncRequest() : xtreemfs_update_file_sizeRequest( org::xtreemfs::interfaces::XCap(), org::xtreemfs::interfaces::OSDWriteResponse() ) { }
        xtreemfs_update_file_sizeSyncRequest( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) : xtreemfs_update_file_sizeRequest( xcap, osd_write_response ) { }
        virtual ~xtreemfs_update_file_sizeSyncRequest() { }

        bool operator==( const xtreemfs_update_file_sizeSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Event& response_ev ) { return response_event_queue.enqueue( response_ev ); }
        YIELD::Event& waitForDefaultResponse( YIELD::timeout_ns_t timeout_ns ) { return response_event_queue.timed_dequeue_typed<org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeResponse>( timeout_ns ); }

      private:
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_event_queue;
      };

        class MRCException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_EVENT_PARENT_CLASS
        {
        public:
          MRCException() : error_code( 0 ) { }
        MRCException( uint32_t error_code, const std::string& error_message, const std::string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
        MRCException( uint32_t error_code, const char* error_message, size_t error_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), error_message( error_message, error_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
          MRCException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_EVENT_PARENT_CLASS( what ) { }
          virtual ~MRCException() throw() { }

        void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
        uint32_t get_error_code() const { return error_code; }
        void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
        void set_error_message( const char* error_message, size_t error_message_len = 0 ) { this->error_message.assign( error_message, ( error_message_len != 0 ) ? error_message_len : std::strlen( error_message ) ); }
        const std::string& get_error_message() const { return error_message; }
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len = 0 ) { this->stack_trace.assign( stack_trace, ( stack_trace_len != 0 ) ? stack_trace_len : std::strlen( stack_trace ) ); }
        const std::string& get_stack_trace() const { return stack_trace; }

          // YIELD::Object
          YIELD_OBJECT_TYPE_INFO( EXCEPTION_EVENT, "org::xtreemfs::interfaces::MRCInterface::MRCException", 1210568114UL );

          // YIELD::ExceptionEvent
          virtual ExceptionEvent* clone() const { return new MRCException( error_code, error_message, stack_trace); }
          virtual void throwStackClone() const { throw MRCException( error_code, error_message, stack_trace); }
        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

      protected:
        uint32_t error_code;
        std::string error_message;
        std::string stack_trace;
        };



        void registerObjectFactories( YIELD::ObjectFactories& object_factories )
        {
          object_factories.registerObjectFactory( 1061689358UL, new YIELD::ObjectFactoryImpl<accessRequest> ); object_factories.registerObjectFactory( 970871736UL, new YIELD::ObjectFactoryImpl<accessSyncRequest> ); object_factories.registerObjectFactory( 1046856447UL, new YIELD::ObjectFactoryImpl<accessResponse> );
          object_factories.registerObjectFactory( 382547319UL, new YIELD::ObjectFactoryImpl<chmodRequest> ); object_factories.registerObjectFactory( 3170294911UL, new YIELD::ObjectFactoryImpl<chmodSyncRequest> ); object_factories.registerObjectFactory( 2600293463UL, new YIELD::ObjectFactoryImpl<chmodResponse> );
          object_factories.registerObjectFactory( 1479455167UL, new YIELD::ObjectFactoryImpl<chownRequest> ); object_factories.registerObjectFactory( 3658998544UL, new YIELD::ObjectFactoryImpl<chownSyncRequest> ); object_factories.registerObjectFactory( 2956591049UL, new YIELD::ObjectFactoryImpl<chownResponse> );
          object_factories.registerObjectFactory( 736916640UL, new YIELD::ObjectFactoryImpl<createRequest> ); object_factories.registerObjectFactory( 14769888UL, new YIELD::ObjectFactoryImpl<createSyncRequest> ); object_factories.registerObjectFactory( 198172638UL, new YIELD::ObjectFactoryImpl<createResponse> );
          object_factories.registerObjectFactory( 360001814UL, new YIELD::ObjectFactoryImpl<ftruncateRequest> ); object_factories.registerObjectFactory( 2520433270UL, new YIELD::ObjectFactoryImpl<ftruncateSyncRequest> ); object_factories.registerObjectFactory( 3573723824UL, new YIELD::ObjectFactoryImpl<ftruncateResponse> );
          object_factories.registerObjectFactory( 1335718504UL, new YIELD::ObjectFactoryImpl<getattrRequest> ); object_factories.registerObjectFactory( 1454483433UL, new YIELD::ObjectFactoryImpl<getattrSyncRequest> ); object_factories.registerObjectFactory( 1150023493UL, new YIELD::ObjectFactoryImpl<getattrResponse> );
          object_factories.registerObjectFactory( 1634969716UL, new YIELD::ObjectFactoryImpl<getxattrRequest> ); object_factories.registerObjectFactory( 875919323UL, new YIELD::ObjectFactoryImpl<getxattrSyncRequest> ); object_factories.registerObjectFactory( 72976609UL, new YIELD::ObjectFactoryImpl<getxattrResponse> );
          object_factories.registerObjectFactory( 666215785UL, new YIELD::ObjectFactoryImpl<linkRequest> ); object_factories.registerObjectFactory( 2091207585UL, new YIELD::ObjectFactoryImpl<linkSyncRequest> ); object_factories.registerObjectFactory( 1242382351UL, new YIELD::ObjectFactoryImpl<linkResponse> );
          object_factories.registerObjectFactory( 661933360UL, new YIELD::ObjectFactoryImpl<listxattrRequest> ); object_factories.registerObjectFactory( 3667671471UL, new YIELD::ObjectFactoryImpl<listxattrSyncRequest> ); object_factories.registerObjectFactory( 3509031574UL, new YIELD::ObjectFactoryImpl<listxattrResponse> );
          object_factories.registerObjectFactory( 2418580920UL, new YIELD::ObjectFactoryImpl<mkdirRequest> ); object_factories.registerObjectFactory( 1459969840UL, new YIELD::ObjectFactoryImpl<mkdirSyncRequest> ); object_factories.registerObjectFactory( 1461564105UL, new YIELD::ObjectFactoryImpl<mkdirResponse> );
          object_factories.registerObjectFactory( 2208926316UL, new YIELD::ObjectFactoryImpl<openRequest> ); object_factories.registerObjectFactory( 2380774365UL, new YIELD::ObjectFactoryImpl<openSyncRequest> ); object_factories.registerObjectFactory( 3891582700UL, new YIELD::ObjectFactoryImpl<openResponse> );
          object_factories.registerObjectFactory( 2159159247UL, new YIELD::ObjectFactoryImpl<readdirRequest> ); object_factories.registerObjectFactory( 4016729359UL, new YIELD::ObjectFactoryImpl<readdirSyncRequest> ); object_factories.registerObjectFactory( 42716287UL, new YIELD::ObjectFactoryImpl<readdirResponse> );
          object_factories.registerObjectFactory( 4111450692UL, new YIELD::ObjectFactoryImpl<removexattrRequest> ); object_factories.registerObjectFactory( 1769414973UL, new YIELD::ObjectFactoryImpl<removexattrSyncRequest> ); object_factories.registerObjectFactory( 813148522UL, new YIELD::ObjectFactoryImpl<removexattrResponse> );
          object_factories.registerObjectFactory( 229065239UL, new YIELD::ObjectFactoryImpl<renameRequest> ); object_factories.registerObjectFactory( 107812048UL, new YIELD::ObjectFactoryImpl<renameSyncRequest> ); object_factories.registerObjectFactory( 4002167429UL, new YIELD::ObjectFactoryImpl<renameResponse> );
          object_factories.registerObjectFactory( 1480479915UL, new YIELD::ObjectFactoryImpl<rmdirRequest> ); object_factories.registerObjectFactory( 2324410537UL, new YIELD::ObjectFactoryImpl<rmdirSyncRequest> ); object_factories.registerObjectFactory( 161028879UL, new YIELD::ObjectFactoryImpl<rmdirResponse> );
          object_factories.registerObjectFactory( 1274791757UL, new YIELD::ObjectFactoryImpl<setattrRequest> ); object_factories.registerObjectFactory( 2656031770UL, new YIELD::ObjectFactoryImpl<setattrSyncRequest> ); object_factories.registerObjectFactory( 1637936981UL, new YIELD::ObjectFactoryImpl<setattrResponse> );
          object_factories.registerObjectFactory( 1676810928UL, new YIELD::ObjectFactoryImpl<setxattrRequest> ); object_factories.registerObjectFactory( 2121922426UL, new YIELD::ObjectFactoryImpl<setxattrSyncRequest> ); object_factories.registerObjectFactory( 1656711865UL, new YIELD::ObjectFactoryImpl<setxattrResponse> );
          object_factories.registerObjectFactory( 2662804498UL, new YIELD::ObjectFactoryImpl<statvfsRequest> ); object_factories.registerObjectFactory( 2821167296UL, new YIELD::ObjectFactoryImpl<statvfsSyncRequest> ); object_factories.registerObjectFactory( 1545560239UL, new YIELD::ObjectFactoryImpl<statvfsResponse> );
          object_factories.registerObjectFactory( 617299237UL, new YIELD::ObjectFactoryImpl<symlinkRequest> ); object_factories.registerObjectFactory( 171691642UL, new YIELD::ObjectFactoryImpl<symlinkSyncRequest> ); object_factories.registerObjectFactory( 3986079894UL, new YIELD::ObjectFactoryImpl<symlinkResponse> );
          object_factories.registerObjectFactory( 3413690043UL, new YIELD::ObjectFactoryImpl<unlinkRequest> ); object_factories.registerObjectFactory( 3727349253UL, new YIELD::ObjectFactoryImpl<unlinkSyncRequest> ); object_factories.registerObjectFactory( 35019552UL, new YIELD::ObjectFactoryImpl<unlinkResponse> );
          object_factories.registerObjectFactory( 3089466576UL, new YIELD::ObjectFactoryImpl<utimensRequest> ); object_factories.registerObjectFactory( 1476265017UL, new YIELD::ObjectFactoryImpl<utimensSyncRequest> ); object_factories.registerObjectFactory( 187175940UL, new YIELD::ObjectFactoryImpl<utimensResponse> );
          object_factories.registerObjectFactory( 161040777UL, new YIELD::ObjectFactoryImpl<xtreemfs_checkpointRequest> ); object_factories.registerObjectFactory( 2637105826UL, new YIELD::ObjectFactoryImpl<xtreemfs_checkpointSyncRequest> ); object_factories.registerObjectFactory( 819133164UL, new YIELD::ObjectFactoryImpl<xtreemfs_checkpointResponse> );
          object_factories.registerObjectFactory( 1290835505UL, new YIELD::ObjectFactoryImpl<xtreemfs_check_file_existsRequest> ); object_factories.registerObjectFactory( 2593325143UL, new YIELD::ObjectFactoryImpl<xtreemfs_check_file_existsSyncRequest> ); object_factories.registerObjectFactory( 3802978278UL, new YIELD::ObjectFactoryImpl<xtreemfs_check_file_existsResponse> );
          object_factories.registerObjectFactory( 2901057887UL, new YIELD::ObjectFactoryImpl<xtreemfs_dump_databaseRequest> ); object_factories.registerObjectFactory( 1815656759UL, new YIELD::ObjectFactoryImpl<xtreemfs_dump_databaseSyncRequest> ); object_factories.registerObjectFactory( 995967380UL, new YIELD::ObjectFactoryImpl<xtreemfs_dump_databaseResponse> );
          object_factories.registerObjectFactory( 3264623697UL, new YIELD::ObjectFactoryImpl<xtreemfs_get_suitable_osdsRequest> ); object_factories.registerObjectFactory( 1264948882UL, new YIELD::ObjectFactoryImpl<xtreemfs_get_suitable_osdsSyncRequest> ); object_factories.registerObjectFactory( 2352042645UL, new YIELD::ObjectFactoryImpl<xtreemfs_get_suitable_osdsResponse> );
          object_factories.registerObjectFactory( 738271786UL, new YIELD::ObjectFactoryImpl<xtreemfs_lsvolRequest> ); object_factories.registerObjectFactory( 2023175249UL, new YIELD::ObjectFactoryImpl<xtreemfs_lsvolSyncRequest> ); object_factories.registerObjectFactory( 2554565499UL, new YIELD::ObjectFactoryImpl<xtreemfs_lsvolResponse> );
          object_factories.registerObjectFactory( 2800551134UL, new YIELD::ObjectFactoryImpl<xtreemfs_listdirRequest> ); object_factories.registerObjectFactory( 2865950482UL, new YIELD::ObjectFactoryImpl<xtreemfs_listdirSyncRequest> ); object_factories.registerObjectFactory( 3770781012UL, new YIELD::ObjectFactoryImpl<xtreemfs_listdirResponse> );
          object_factories.registerObjectFactory( 1895169082UL, new YIELD::ObjectFactoryImpl<xtreemfs_mkvolRequest> ); object_factories.registerObjectFactory( 257131742UL, new YIELD::ObjectFactoryImpl<xtreemfs_mkvolSyncRequest> ); object_factories.registerObjectFactory( 3778316902UL, new YIELD::ObjectFactoryImpl<xtreemfs_mkvolResponse> );
          object_factories.registerObjectFactory( 526231386UL, new YIELD::ObjectFactoryImpl<xtreemfs_renew_capabilityRequest> ); object_factories.registerObjectFactory( 2840337294UL, new YIELD::ObjectFactoryImpl<xtreemfs_renew_capabilitySyncRequest> ); object_factories.registerObjectFactory( 137996173UL, new YIELD::ObjectFactoryImpl<xtreemfs_renew_capabilityResponse> );
          object_factories.registerObjectFactory( 3822735046UL, new YIELD::ObjectFactoryImpl<xtreemfs_replica_addRequest> ); object_factories.registerObjectFactory( 3351137176UL, new YIELD::ObjectFactoryImpl<xtreemfs_replica_addSyncRequest> ); object_factories.registerObjectFactory( 112572185UL, new YIELD::ObjectFactoryImpl<xtreemfs_replica_addResponse> );
          object_factories.registerObjectFactory( 3179071099UL, new YIELD::ObjectFactoryImpl<xtreemfs_replica_listRequest> ); object_factories.registerObjectFactory( 135992135UL, new YIELD::ObjectFactoryImpl<xtreemfs_replica_listSyncRequest> ); object_factories.registerObjectFactory( 1736837504UL, new YIELD::ObjectFactoryImpl<xtreemfs_replica_listResponse> );
          object_factories.registerObjectFactory( 992591294UL, new YIELD::ObjectFactoryImpl<xtreemfs_replica_removeRequest> ); object_factories.registerObjectFactory( 2117462941UL, new YIELD::ObjectFactoryImpl<xtreemfs_replica_removeSyncRequest> ); object_factories.registerObjectFactory( 1412849496UL, new YIELD::ObjectFactoryImpl<xtreemfs_replica_removeResponse> );
          object_factories.registerObjectFactory( 576859361UL, new YIELD::ObjectFactoryImpl<xtreemfs_restore_databaseRequest> ); object_factories.registerObjectFactory( 3542459549UL, new YIELD::ObjectFactoryImpl<xtreemfs_restore_databaseSyncRequest> ); object_factories.registerObjectFactory( 1807651157UL, new YIELD::ObjectFactoryImpl<xtreemfs_restore_databaseResponse> );
          object_factories.registerObjectFactory( 1511356569UL, new YIELD::ObjectFactoryImpl<xtreemfs_restore_fileRequest> ); object_factories.registerObjectFactory( 763347303UL, new YIELD::ObjectFactoryImpl<xtreemfs_restore_fileSyncRequest> ); object_factories.registerObjectFactory( 3701413834UL, new YIELD::ObjectFactoryImpl<xtreemfs_restore_fileResponse> );
          object_factories.registerObjectFactory( 981073148UL, new YIELD::ObjectFactoryImpl<xtreemfs_rmvolRequest> ); object_factories.registerObjectFactory( 1964309776UL, new YIELD::ObjectFactoryImpl<xtreemfs_rmvolSyncRequest> ); object_factories.registerObjectFactory( 180525667UL, new YIELD::ObjectFactoryImpl<xtreemfs_rmvolResponse> );
          object_factories.registerObjectFactory( 3905865114UL, new YIELD::ObjectFactoryImpl<xtreemfs_shutdownRequest> ); object_factories.registerObjectFactory( 3609568511UL, new YIELD::ObjectFactoryImpl<xtreemfs_shutdownSyncRequest> ); object_factories.registerObjectFactory( 1924750368UL, new YIELD::ObjectFactoryImpl<xtreemfs_shutdownResponse> );
          object_factories.registerObjectFactory( 152425201UL, new YIELD::ObjectFactoryImpl<xtreemfs_update_file_sizeRequest> ); object_factories.registerObjectFactory( 2236352939UL, new YIELD::ObjectFactoryImpl<xtreemfs_update_file_sizeSyncRequest> ); object_factories.registerObjectFactory( 1410569539UL, new YIELD::ObjectFactoryImpl<xtreemfs_update_file_sizeResponse> );
          object_factories.registerObjectFactory( 1210568114UL, new YIELD::ObjectFactoryImpl<MRCException> );;
        }

        // EventHandler
        virtual const char* getEventHandlerName() const { return "MRCInterface"; }

        virtual void handleEvent( YIELD::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.get_type_id() )
            {
              case 1061689358UL: handleaccessRequest( static_cast<accessRequest&>( ev ) ); return;
              case 382547319UL: handlechmodRequest( static_cast<chmodRequest&>( ev ) ); return;
              case 1479455167UL: handlechownRequest( static_cast<chownRequest&>( ev ) ); return;
              case 736916640UL: handlecreateRequest( static_cast<createRequest&>( ev ) ); return;
              case 360001814UL: handleftruncateRequest( static_cast<ftruncateRequest&>( ev ) ); return;
              case 1335718504UL: handlegetattrRequest( static_cast<getattrRequest&>( ev ) ); return;
              case 1634969716UL: handlegetxattrRequest( static_cast<getxattrRequest&>( ev ) ); return;
              case 666215785UL: handlelinkRequest( static_cast<linkRequest&>( ev ) ); return;
              case 661933360UL: handlelistxattrRequest( static_cast<listxattrRequest&>( ev ) ); return;
              case 2418580920UL: handlemkdirRequest( static_cast<mkdirRequest&>( ev ) ); return;
              case 2208926316UL: handleopenRequest( static_cast<openRequest&>( ev ) ); return;
              case 2159159247UL: handlereaddirRequest( static_cast<readdirRequest&>( ev ) ); return;
              case 4111450692UL: handleremovexattrRequest( static_cast<removexattrRequest&>( ev ) ); return;
              case 229065239UL: handlerenameRequest( static_cast<renameRequest&>( ev ) ); return;
              case 1480479915UL: handlermdirRequest( static_cast<rmdirRequest&>( ev ) ); return;
              case 1274791757UL: handlesetattrRequest( static_cast<setattrRequest&>( ev ) ); return;
              case 1676810928UL: handlesetxattrRequest( static_cast<setxattrRequest&>( ev ) ); return;
              case 2662804498UL: handlestatvfsRequest( static_cast<statvfsRequest&>( ev ) ); return;
              case 617299237UL: handlesymlinkRequest( static_cast<symlinkRequest&>( ev ) ); return;
              case 3413690043UL: handleunlinkRequest( static_cast<unlinkRequest&>( ev ) ); return;
              case 3089466576UL: handleutimensRequest( static_cast<utimensRequest&>( ev ) ); return;
              case 161040777UL: handlextreemfs_checkpointRequest( static_cast<xtreemfs_checkpointRequest&>( ev ) ); return;
              case 1290835505UL: handlextreemfs_check_file_existsRequest( static_cast<xtreemfs_check_file_existsRequest&>( ev ) ); return;
              case 2901057887UL: handlextreemfs_dump_databaseRequest( static_cast<xtreemfs_dump_databaseRequest&>( ev ) ); return;
              case 3264623697UL: handlextreemfs_get_suitable_osdsRequest( static_cast<xtreemfs_get_suitable_osdsRequest&>( ev ) ); return;
              case 738271786UL: handlextreemfs_lsvolRequest( static_cast<xtreemfs_lsvolRequest&>( ev ) ); return;
              case 2800551134UL: handlextreemfs_listdirRequest( static_cast<xtreemfs_listdirRequest&>( ev ) ); return;
              case 1895169082UL: handlextreemfs_mkvolRequest( static_cast<xtreemfs_mkvolRequest&>( ev ) ); return;
              case 526231386UL: handlextreemfs_renew_capabilityRequest( static_cast<xtreemfs_renew_capabilityRequest&>( ev ) ); return;
              case 3822735046UL: handlextreemfs_replica_addRequest( static_cast<xtreemfs_replica_addRequest&>( ev ) ); return;
              case 3179071099UL: handlextreemfs_replica_listRequest( static_cast<xtreemfs_replica_listRequest&>( ev ) ); return;
              case 992591294UL: handlextreemfs_replica_removeRequest( static_cast<xtreemfs_replica_removeRequest&>( ev ) ); return;
              case 576859361UL: handlextreemfs_restore_databaseRequest( static_cast<xtreemfs_restore_databaseRequest&>( ev ) ); return;
              case 1511356569UL: handlextreemfs_restore_fileRequest( static_cast<xtreemfs_restore_fileRequest&>( ev ) ); return;
              case 981073148UL: handlextreemfs_rmvolRequest( static_cast<xtreemfs_rmvolRequest&>( ev ) ); return;
              case 3905865114UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
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

          YIELD::Object::decRef( ev );
        }


      protected:
          virtual void handleaccessRequest( accessRequest& req ) { accessResponse* resp = NULL; try { resp = new accessResponse; bool _return_value = _access( req.get_path(), req.get_mode() ); resp->set__return_value( _return_value ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlechmodRequest( chmodRequest& req ) { chmodResponse* resp = NULL; try { resp = new chmodResponse; _chmod( req.get_path(), req.get_mode() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlechownRequest( chownRequest& req ) { chownResponse* resp = NULL; try { resp = new chownResponse; _chown( req.get_path(), req.get_user_id(), req.get_group_id() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlecreateRequest( createRequest& req ) { createResponse* resp = NULL; try { resp = new createResponse; _create( req.get_path(), req.get_mode() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleftruncateRequest( ftruncateRequest& req ) { ftruncateResponse* resp = NULL; try { resp = new ftruncateResponse; org::xtreemfs::interfaces::XCap truncate_xcap; _ftruncate( req.get_write_xcap(), truncate_xcap ); resp->set_truncate_xcap( truncate_xcap ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlegetattrRequest( getattrRequest& req ) { getattrResponse* resp = NULL; try { resp = new getattrResponse; org::xtreemfs::interfaces::Stat stbuf; _getattr( req.get_path(), stbuf ); resp->set_stbuf( stbuf ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlegetxattrRequest( getxattrRequest& req ) { getxattrResponse* resp = NULL; try { resp = new getxattrResponse; std::string value; _getxattr( req.get_path(), req.get_name(), value ); resp->set_value( value ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlelinkRequest( linkRequest& req ) { linkResponse* resp = NULL; try { resp = new linkResponse; _link( req.get_target_path(), req.get_link_path() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlelistxattrRequest( listxattrRequest& req ) { listxattrResponse* resp = NULL; try { resp = new listxattrResponse; org::xtreemfs::interfaces::StringSet names; _listxattr( req.get_path(), names ); resp->set_names( names ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlemkdirRequest( mkdirRequest& req ) { mkdirResponse* resp = NULL; try { resp = new mkdirResponse; _mkdir( req.get_path(), req.get_mode() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleopenRequest( openRequest& req ) { openResponse* resp = NULL; try { resp = new openResponse; org::xtreemfs::interfaces::FileCredentials file_credentials; _open( req.get_path(), req.get_flags(), req.get_mode(), file_credentials ); resp->set_file_credentials( file_credentials ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlereaddirRequest( readdirRequest& req ) { readdirResponse* resp = NULL; try { resp = new readdirResponse; org::xtreemfs::interfaces::DirectoryEntrySet directory_entries; _readdir( req.get_path(), directory_entries ); resp->set_directory_entries( directory_entries ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleremovexattrRequest( removexattrRequest& req ) { removexattrResponse* resp = NULL; try { resp = new removexattrResponse; _removexattr( req.get_path(), req.get_name() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlerenameRequest( renameRequest& req ) { renameResponse* resp = NULL; try { resp = new renameResponse; org::xtreemfs::interfaces::FileCredentialsSet file_credentials; _rename( req.get_source_path(), req.get_target_path(), file_credentials ); resp->set_file_credentials( file_credentials ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlermdirRequest( rmdirRequest& req ) { rmdirResponse* resp = NULL; try { resp = new rmdirResponse; _rmdir( req.get_path() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlesetattrRequest( setattrRequest& req ) { setattrResponse* resp = NULL; try { resp = new setattrResponse; _setattr( req.get_path(), req.get_stbuf() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlesetxattrRequest( setxattrRequest& req ) { setxattrResponse* resp = NULL; try { resp = new setxattrResponse; _setxattr( req.get_path(), req.get_name(), req.get_value(), req.get_flags() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlestatvfsRequest( statvfsRequest& req ) { statvfsResponse* resp = NULL; try { resp = new statvfsResponse; org::xtreemfs::interfaces::StatVFS stbuf; _statvfs( req.get_volume_name(), stbuf ); resp->set_stbuf( stbuf ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlesymlinkRequest( symlinkRequest& req ) { symlinkResponse* resp = NULL; try { resp = new symlinkResponse; _symlink( req.get_target_path(), req.get_link_path() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleunlinkRequest( unlinkRequest& req ) { unlinkResponse* resp = NULL; try { resp = new unlinkResponse; org::xtreemfs::interfaces::FileCredentialsSet file_credentials; _unlink( req.get_path(), file_credentials ); resp->set_file_credentials( file_credentials ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handleutimensRequest( utimensRequest& req ) { utimensResponse* resp = NULL; try { resp = new utimensResponse; _utimens( req.get_path(), req.get_atime_ns(), req.get_mtime_ns(), req.get_ctime_ns() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& req ) { xtreemfs_checkpointResponse* resp = NULL; try { resp = new xtreemfs_checkpointResponse; _xtreemfs_checkpoint(); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_check_file_existsRequest( xtreemfs_check_file_existsRequest& req ) { xtreemfs_check_file_existsResponse* resp = NULL; try { resp = new xtreemfs_check_file_existsResponse; std::string bitmap; _xtreemfs_check_file_exists( req.get_volume_id(), req.get_file_ids(), bitmap ); resp->set_bitmap( bitmap ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_dump_databaseRequest( xtreemfs_dump_databaseRequest& req ) { xtreemfs_dump_databaseResponse* resp = NULL; try { resp = new xtreemfs_dump_databaseResponse; _xtreemfs_dump_database( req.get_dump_file() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_get_suitable_osdsRequest( xtreemfs_get_suitable_osdsRequest& req ) { xtreemfs_get_suitable_osdsResponse* resp = NULL; try { resp = new xtreemfs_get_suitable_osdsResponse; org::xtreemfs::interfaces::StringSet osd_uuids; _xtreemfs_get_suitable_osds( req.get_file_id(), osd_uuids ); resp->set_osd_uuids( osd_uuids ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_lsvolRequest( xtreemfs_lsvolRequest& req ) { xtreemfs_lsvolResponse* resp = NULL; try { resp = new xtreemfs_lsvolResponse; org::xtreemfs::interfaces::VolumeSet volumes; _xtreemfs_lsvol( volumes ); resp->set_volumes( volumes ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_listdirRequest( xtreemfs_listdirRequest& req ) { xtreemfs_listdirResponse* resp = NULL; try { resp = new xtreemfs_listdirResponse; org::xtreemfs::interfaces::StringSet names; _xtreemfs_listdir( req.get_path(), names ); resp->set_names( names ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_mkvolRequest( xtreemfs_mkvolRequest& req ) { xtreemfs_mkvolResponse* resp = NULL; try { resp = new xtreemfs_mkvolResponse; _xtreemfs_mkvol( req.get_volume() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_renew_capabilityRequest( xtreemfs_renew_capabilityRequest& req ) { xtreemfs_renew_capabilityResponse* resp = NULL; try { resp = new xtreemfs_renew_capabilityResponse; org::xtreemfs::interfaces::XCap renewed_xcap; _xtreemfs_renew_capability( req.get_old_xcap(), renewed_xcap ); resp->set_renewed_xcap( renewed_xcap ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_replica_addRequest( xtreemfs_replica_addRequest& req ) { xtreemfs_replica_addResponse* resp = NULL; try { resp = new xtreemfs_replica_addResponse; _xtreemfs_replica_add( req.get_file_id(), req.get_new_replica() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_replica_listRequest( xtreemfs_replica_listRequest& req ) { xtreemfs_replica_listResponse* resp = NULL; try { resp = new xtreemfs_replica_listResponse; org::xtreemfs::interfaces::ReplicaSet replicas; _xtreemfs_replica_list( req.get_file_id(), replicas ); resp->set_replicas( replicas ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_replica_removeRequest( xtreemfs_replica_removeRequest& req ) { xtreemfs_replica_removeResponse* resp = NULL; try { resp = new xtreemfs_replica_removeResponse; org::xtreemfs::interfaces::XCap delete_xcap; _xtreemfs_replica_remove( req.get_file_id(), req.get_osd_uuid(), delete_xcap ); resp->set_delete_xcap( delete_xcap ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_restore_databaseRequest( xtreemfs_restore_databaseRequest& req ) { xtreemfs_restore_databaseResponse* resp = NULL; try { resp = new xtreemfs_restore_databaseResponse; _xtreemfs_restore_database( req.get_dump_file() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_restore_fileRequest( xtreemfs_restore_fileRequest& req ) { xtreemfs_restore_fileResponse* resp = NULL; try { resp = new xtreemfs_restore_fileResponse; _xtreemfs_restore_file( req.get_file_path(), req.get_file_id(), req.get_file_size(), req.get_osd_uuid(), req.get_stripe_size() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_rmvolRequest( xtreemfs_rmvolRequest& req ) { xtreemfs_rmvolResponse* resp = NULL; try { resp = new xtreemfs_rmvolResponse; _xtreemfs_rmvol( req.get_volume_name() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req ) { xtreemfs_shutdownResponse* resp = NULL; try { resp = new xtreemfs_shutdownResponse; _xtreemfs_shutdown(); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }
        virtual void handlextreemfs_update_file_sizeRequest( xtreemfs_update_file_sizeRequest& req ) { xtreemfs_update_file_sizeResponse* resp = NULL; try { resp = new xtreemfs_update_file_sizeResponse; _xtreemfs_update_file_size( req.get_xcap(), req.get_osd_write_response() ); req.respond( *resp ); YIELD::Object::decRef( req ); } catch ( ... ) { throw; }; }

      virtual bool _access( const std::string& path, uint32_t mode ) { return false; }
        virtual void _chmod( const std::string& path, uint32_t mode ) { }
        virtual void _chown( const std::string& path, const std::string& user_id, const std::string& group_id ) { }
        virtual void _create( const std::string& path, uint32_t mode ) { }
        virtual void _ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap ) { }
        virtual void _getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf ) { }
        virtual void _getxattr( const std::string& path, const std::string& name, std::string& value ) { }
        virtual void _link( const std::string& target_path, const std::string& link_path ) { }
        virtual void _listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { }
        virtual void _mkdir( const std::string& path, uint32_t mode ) { }
        virtual void _open( const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& file_credentials ) { }
        virtual void _readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) { }
        virtual void _removexattr( const std::string& path, const std::string& name ) { }
        virtual void _rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { }
        virtual void _rmdir( const std::string& path ) { }
        virtual void _setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf ) { }
        virtual void _setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags ) { }
        virtual void _statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf ) { }
        virtual void _symlink( const std::string& target_path, const std::string& link_path ) { }
        virtual void _unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { }
        virtual void _utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) { }
        virtual void _xtreemfs_checkpoint() { }
        virtual void _xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap ) { }
        virtual void _xtreemfs_dump_database( const std::string& dump_file ) { }
        virtual void _xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids ) { }
        virtual void _xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes ) { }
        virtual void _xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { }
        virtual void _xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume ) { }
        virtual void _xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap ) { }
        virtual void _xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) { }
        virtual void _xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas ) { }
        virtual void _xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap ) { }
        virtual void _xtreemfs_restore_database( const std::string& dump_file ) { }
        virtual void _xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) { }
        virtual void _xtreemfs_rmvol( const std::string& volume_name ) { }
        virtual void _xtreemfs_shutdown() { }
        virtual void _xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { }
      };

      // Use this macro in an implementation class to get all of the prototypes for the operations in MRCInterface
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_PROTOTYPES \
      virtual bool _access( const std::string& path, uint32_t mode );\
      virtual void _chmod( const std::string& path, uint32_t mode );\
      virtual void _chown( const std::string& path, const std::string& user_id, const std::string& group_id );\
      virtual void _create( const std::string& path, uint32_t mode );\
      virtual void _ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap );\
      virtual void _getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf );\
      virtual void _getxattr( const std::string& path, const std::string& name, std::string& value );\
      virtual void _link( const std::string& target_path, const std::string& link_path );\
      virtual void _listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names );\
      virtual void _mkdir( const std::string& path, uint32_t mode );\
      virtual void _open( const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& file_credentials );\
      virtual void _readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries );\
      virtual void _removexattr( const std::string& path, const std::string& name );\
      virtual void _rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials );\
      virtual void _rmdir( const std::string& path );\
      virtual void _setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf );\
      virtual void _setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags );\
      virtual void _statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf );\
      virtual void _symlink( const std::string& target_path, const std::string& link_path );\
      virtual void _unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials );\
      virtual void _utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns );\
      virtual void _xtreemfs_checkpoint();\
      virtual void _xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap );\
      virtual void _xtreemfs_dump_database( const std::string& dump_file );\
      virtual void _xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids );\
      virtual void _xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes );\
      virtual void _xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names );\
      virtual void _xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume );\
      virtual void _xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap );\
      virtual void _xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica );\
      virtual void _xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas );\
      virtual void _xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap );\
      virtual void _xtreemfs_restore_database( const std::string& dump_file );\
      virtual void _xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size );\
      virtual void _xtreemfs_rmvol( const std::string& volume_name );\
      virtual void _xtreemfs_shutdown();\
      virtual void _xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );

      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handleaccessRequestRequest( accessRequest& req );\
      virtual void handlechmodRequestRequest( chmodRequest& req );\
      virtual void handlechownRequestRequest( chownRequest& req );\
      virtual void handlecreateRequestRequest( createRequest& req );\
      virtual void handleftruncateRequestRequest( ftruncateRequest& req );\
      virtual void handlegetattrRequestRequest( getattrRequest& req );\
      virtual void handlegetxattrRequestRequest( getxattrRequest& req );\
      virtual void handlelinkRequestRequest( linkRequest& req );\
      virtual void handlelistxattrRequestRequest( listxattrRequest& req );\
      virtual void handlemkdirRequestRequest( mkdirRequest& req );\
      virtual void handleopenRequestRequest( openRequest& req );\
      virtual void handlereaddirRequestRequest( readdirRequest& req );\
      virtual void handleremovexattrRequestRequest( removexattrRequest& req );\
      virtual void handlerenameRequestRequest( renameRequest& req );\
      virtual void handlermdirRequestRequest( rmdirRequest& req );\
      virtual void handlesetattrRequestRequest( setattrRequest& req );\
      virtual void handlesetxattrRequestRequest( setxattrRequest& req );\
      virtual void handlestatvfsRequestRequest( statvfsRequest& req );\
      virtual void handlesymlinkRequestRequest( symlinkRequest& req );\
      virtual void handleunlinkRequestRequest( unlinkRequest& req );\
      virtual void handleutimensRequestRequest( utimensRequest& req );\
      virtual void handlextreemfs_checkpointRequestRequest( xtreemfs_checkpointRequest& req );\
      virtual void handlextreemfs_check_file_existsRequestRequest( xtreemfs_check_file_existsRequest& req );\
      virtual void handlextreemfs_dump_databaseRequestRequest( xtreemfs_dump_databaseRequest& req );\
      virtual void handlextreemfs_get_suitable_osdsRequestRequest( xtreemfs_get_suitable_osdsRequest& req );\
      virtual void handlextreemfs_lsvolRequestRequest( xtreemfs_lsvolRequest& req );\
      virtual void handlextreemfs_listdirRequestRequest( xtreemfs_listdirRequest& req );\
      virtual void handlextreemfs_mkvolRequestRequest( xtreemfs_mkvolRequest& req );\
      virtual void handlextreemfs_renew_capabilityRequestRequest( xtreemfs_renew_capabilityRequest& req );\
      virtual void handlextreemfs_replica_addRequestRequest( xtreemfs_replica_addRequest& req );\
      virtual void handlextreemfs_replica_listRequestRequest( xtreemfs_replica_listRequest& req );\
      virtual void handlextreemfs_replica_removeRequestRequest( xtreemfs_replica_removeRequest& req );\
      virtual void handlextreemfs_restore_databaseRequestRequest( xtreemfs_restore_databaseRequest& req );\
      virtual void handlextreemfs_restore_fileRequestRequest( xtreemfs_restore_fileRequest& req );\
      virtual void handlextreemfs_rmvolRequestRequest( xtreemfs_rmvolRequest& req );\
      virtual void handlextreemfs_shutdownRequestRequest( xtreemfs_shutdownRequest& req );\
      virtual void handlextreemfs_update_file_sizeRequestRequest( xtreemfs_update_file_sizeRequest& req );

    };



  };



};

#endif
