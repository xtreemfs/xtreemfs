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
        void set_user_id( const char* user_id, size_t user_id_len ) { this->user_id.assign( user_id, user_id_len ); }
        const std::string& get_user_id() const { return user_id; }
        void set_group_id( const std::string& group_id ) { set_group_id( group_id.c_str(), group_id.size() ); }
        void set_group_id( const char* group_id, size_t group_id_len ) { this->group_id.assign( group_id, group_id_len ); }
        const std::string& get_group_id() const { return group_id; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
        const std::string& get_file_id() const { return file_id; }
        void set_link_target( const std::string& link_target ) { set_link_target( link_target.c_str(), link_target.size() ); }
        void set_link_target( const char* link_target, size_t link_target_len ) { this->link_target.assign( link_target, link_target_len ); }
        const std::string& get_link_target() const { return link_target; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        void set_attributes( uint32_t attributes ) { this->attributes = attributes; }
        uint32_t get_attributes() const { return attributes; }

        bool operator==( const Stat& other ) const { return mode == other.mode && nlink == other.nlink && uid == other.uid && gid == other.gid && unused_dev == other.unused_dev && size == other.size && atime_ns == other.atime_ns && mtime_ns == other.mtime_ns && ctime_ns == other.ctime_ns && user_id == other.user_id && group_id == other.group_id && file_id == other.file_id && link_target == other.link_target && truncate_epoch == other.truncate_epoch && attributes == other.attributes; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( Stat, 1040 );

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
        void set_name( const char* name, size_t name_len ) { this->name.assign( name, name_len ); }
        const std::string& get_name() const { return name; }
        void set_stbuf( const org::xtreemfs::interfaces::Stat&  stbuf ) { this->stbuf = stbuf; }
        const org::xtreemfs::interfaces::Stat& get_stbuf() const { return stbuf; }

        bool operator==( const DirectoryEntry& other ) const { return name == other.name && stbuf == other.stbuf; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( DirectoryEntry, 1041 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); input_stream.readStruct( YIELD::StructuredStream::Declaration( "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); output_stream.writeStruct( YIELD::StructuredStream::Declaration( "stbuf" ), stbuf ); }

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
        YIELD_OBJECT_PROTOTYPES( DirectoryEntrySet, 1042 );
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::DirectoryEntry value; input_stream.readStruct( YIELD::StructuredStream::Declaration( "value" ), &value ); push_back( value ); }
        uint64_t get_size() const { return std::vector<org::xtreemfs::interfaces::DirectoryEntry>::size(); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "value" ), ( *this )[value_i] ); } }
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
        void set_fsid( const char* fsid, size_t fsid_len ) { this->fsid.assign( fsid, fsid_len ); }
        const std::string& get_fsid() const { return fsid; }
        void set_namelen( uint32_t namelen ) { this->namelen = namelen; }
        uint32_t get_namelen() const { return namelen; }

        bool operator==( const StatVFS& other ) const { return bsize == other.bsize && bfree == other.bfree && fsid == other.fsid && namelen == other.namelen; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( StatVFS, 1043 );

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
        void set_name( const char* name, size_t name_len ) { this->name.assign( name, name_len ); }
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
        void set_id( const char* id, size_t id_len ) { this->id.assign( id, id_len ); }
        const std::string& get_id() const { return id; }
        void set_owner_user_id( const std::string& owner_user_id ) { set_owner_user_id( owner_user_id.c_str(), owner_user_id.size() ); }
        void set_owner_user_id( const char* owner_user_id, size_t owner_user_id_len ) { this->owner_user_id.assign( owner_user_id, owner_user_id_len ); }
        const std::string& get_owner_user_id() const { return owner_user_id; }
        void set_owner_group_id( const std::string& owner_group_id ) { set_owner_group_id( owner_group_id.c_str(), owner_group_id.size() ); }
        void set_owner_group_id( const char* owner_group_id, size_t owner_group_id_len ) { this->owner_group_id.assign( owner_group_id, owner_group_id_len ); }
        const std::string& get_owner_group_id() const { return owner_group_id; }

        bool operator==( const Volume& other ) const { return name == other.name && mode == other.mode && osd_selection_policy == other.osd_selection_policy && default_striping_policy == other.default_striping_policy && access_control_policy == other.access_control_policy && id == other.id && owner_user_id == other.owner_user_id && owner_group_id == other.owner_group_id; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( Volume, 1044 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); osd_selection_policy = ( org::xtreemfs::interfaces::OSDSelectionPolicyType )input_stream.readInt32( YIELD::StructuredStream::Declaration( "osd_selection_policy" ) ); input_stream.readStruct( YIELD::StructuredStream::Declaration( "default_striping_policy" ), &default_striping_policy ); access_control_policy = ( org::xtreemfs::interfaces::AccessControlPolicyType )input_stream.readInt32( YIELD::StructuredStream::Declaration( "access_control_policy" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "id" ), id ); input_stream.readString( YIELD::StructuredStream::Declaration( "owner_user_id" ), owner_user_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "owner_group_id" ), owner_group_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "osd_selection_policy" ), osd_selection_policy ); output_stream.writeStruct( YIELD::StructuredStream::Declaration( "default_striping_policy" ), default_striping_policy ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "access_control_policy" ), access_control_policy ); output_stream.writeString( YIELD::StructuredStream::Declaration( "id" ), id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "owner_user_id" ), owner_user_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "owner_group_id" ), owner_group_id ); }

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
        YIELD_OBJECT_PROTOTYPES( VolumeSet, 1045 );
        void deserialize( YIELD::StructuredInputStream& input_stream ) { org::xtreemfs::interfaces::Volume value; input_stream.readStruct( YIELD::StructuredStream::Declaration( "value" ), &value ); push_back( value ); }
        uint64_t get_size() const { return std::vector<org::xtreemfs::interfaces::Volume>::size(); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "value" ), ( *this )[value_i] ); } }
      };



      #ifndef ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS YIELD::Interface
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

      #ifndef ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_EXCEPTION_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS YIELD::ExceptionResponse
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

      virtual bool access( const std::string& path, uint32_t mode ) { return access( path, mode, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual bool access( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target ) { return access( path, mode, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual bool access( const std::string& path, uint32_t mode, uint64_t response_timeout_ns ) { return access( path, mode, NULL, response_timeout_ns ); }
        virtual bool access( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<accessSyncRequest> __req( new accessSyncRequest( path, mode ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<accessResponse> __resp = __req->response_queue.dequeue_typed<accessResponse>( response_timeout_ns ); bool _return_value = __resp->get__return_value(); return _return_value; }
        virtual void chmod( const std::string& path, uint32_t mode ) { chmod( path, mode, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void chmod( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target ) { chmod( path, mode, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void chmod( const std::string& path, uint32_t mode, uint64_t response_timeout_ns ) { chmod( path, mode, NULL, response_timeout_ns ); }
        virtual void chmod( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<chmodSyncRequest> __req( new chmodSyncRequest( path, mode ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<chmodResponse> __resp = __req->response_queue.dequeue_typed<chmodResponse>( response_timeout_ns ); }
        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id ) { chown( path, user_id, group_id, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id, YIELD::EventTarget* send_target ) { chown( path, user_id, group_id, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id, uint64_t response_timeout_ns ) { chown( path, user_id, group_id, NULL, response_timeout_ns ); }
        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<chownSyncRequest> __req( new chownSyncRequest( path, user_id, group_id ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<chownResponse> __resp = __req->response_queue.dequeue_typed<chownResponse>( response_timeout_ns ); }
        virtual void creat( const std::string& path, uint32_t mode ) { creat( path, mode, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void creat( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target ) { creat( path, mode, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void creat( const std::string& path, uint32_t mode, uint64_t response_timeout_ns ) { creat( path, mode, NULL, response_timeout_ns ); }
        virtual void creat( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<creatSyncRequest> __req( new creatSyncRequest( path, mode ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<creatResponse> __resp = __req->response_queue.dequeue_typed<creatResponse>( response_timeout_ns ); }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap ) { ftruncate( write_xcap, truncate_xcap, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap, YIELD::EventTarget* send_target ) { ftruncate( write_xcap, truncate_xcap, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap, uint64_t response_timeout_ns ) { ftruncate( write_xcap, truncate_xcap, NULL, response_timeout_ns ); }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<ftruncateSyncRequest> __req( new ftruncateSyncRequest( write_xcap ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<ftruncateResponse> __resp = __req->response_queue.dequeue_typed<ftruncateResponse>( response_timeout_ns ); truncate_xcap = __resp->get_truncate_xcap(); }
        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf ) { getattr( path, stbuf, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf, YIELD::EventTarget* send_target ) { getattr( path, stbuf, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf, uint64_t response_timeout_ns ) { getattr( path, stbuf, NULL, response_timeout_ns ); }
        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<getattrSyncRequest> __req( new getattrSyncRequest( path ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<getattrResponse> __resp = __req->response_queue.dequeue_typed<getattrResponse>( response_timeout_ns ); stbuf = __resp->get_stbuf(); }
        virtual void getxattr( const std::string& path, const std::string& name, std::string& value ) { getxattr( path, name, value, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void getxattr( const std::string& path, const std::string& name, std::string& value, YIELD::EventTarget* send_target ) { getxattr( path, name, value, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void getxattr( const std::string& path, const std::string& name, std::string& value, uint64_t response_timeout_ns ) { getxattr( path, name, value, NULL, response_timeout_ns ); }
        virtual void getxattr( const std::string& path, const std::string& name, std::string& value, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<getxattrSyncRequest> __req( new getxattrSyncRequest( path, name ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<getxattrResponse> __resp = __req->response_queue.dequeue_typed<getxattrResponse>( response_timeout_ns ); value = __resp->get_value(); }
        virtual void link( const std::string& target_path, const std::string& link_path ) { link( target_path, link_path, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void link( const std::string& target_path, const std::string& link_path, YIELD::EventTarget* send_target ) { link( target_path, link_path, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void link( const std::string& target_path, const std::string& link_path, uint64_t response_timeout_ns ) { link( target_path, link_path, NULL, response_timeout_ns ); }
        virtual void link( const std::string& target_path, const std::string& link_path, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<linkSyncRequest> __req( new linkSyncRequest( target_path, link_path ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<linkResponse> __resp = __req->response_queue.dequeue_typed<linkResponse>( response_timeout_ns ); }
        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { listxattr( path, names, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::EventTarget* send_target ) { listxattr( path, names, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names, uint64_t response_timeout_ns ) { listxattr( path, names, NULL, response_timeout_ns ); }
        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<listxattrSyncRequest> __req( new listxattrSyncRequest( path ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<listxattrResponse> __resp = __req->response_queue.dequeue_typed<listxattrResponse>( response_timeout_ns ); names = __resp->get_names(); }
        virtual void mkdir( const std::string& path, uint32_t mode ) { mkdir( path, mode, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void mkdir( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target ) { mkdir( path, mode, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void mkdir( const std::string& path, uint32_t mode, uint64_t response_timeout_ns ) { mkdir( path, mode, NULL, response_timeout_ns ); }
        virtual void mkdir( const std::string& path, uint32_t mode, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<mkdirSyncRequest> __req( new mkdirSyncRequest( path, mode ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<mkdirResponse> __resp = __req->response_queue.dequeue_typed<mkdirResponse>( response_timeout_ns ); }
        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, org::xtreemfs::interfaces::FileCredentials& file_credentials ) { open( path, flags, mode, attributes, file_credentials, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, org::xtreemfs::interfaces::FileCredentials& file_credentials, YIELD::EventTarget* send_target ) { open( path, flags, mode, attributes, file_credentials, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, org::xtreemfs::interfaces::FileCredentials& file_credentials, uint64_t response_timeout_ns ) { open( path, flags, mode, attributes, file_credentials, NULL, response_timeout_ns ); }
        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, org::xtreemfs::interfaces::FileCredentials& file_credentials, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<openSyncRequest> __req( new openSyncRequest( path, flags, mode, attributes ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<openResponse> __resp = __req->response_queue.dequeue_typed<openResponse>( response_timeout_ns ); file_credentials = __resp->get_file_credentials(); }
        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) { readdir( path, directory_entries, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, YIELD::EventTarget* send_target ) { readdir( path, directory_entries, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, uint64_t response_timeout_ns ) { readdir( path, directory_entries, NULL, response_timeout_ns ); }
        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<readdirSyncRequest> __req( new readdirSyncRequest( path ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<readdirResponse> __resp = __req->response_queue.dequeue_typed<readdirResponse>( response_timeout_ns ); directory_entries = __resp->get_directory_entries(); }
        virtual void removexattr( const std::string& path, const std::string& name ) { removexattr( path, name, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void removexattr( const std::string& path, const std::string& name, YIELD::EventTarget* send_target ) { removexattr( path, name, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void removexattr( const std::string& path, const std::string& name, uint64_t response_timeout_ns ) { removexattr( path, name, NULL, response_timeout_ns ); }
        virtual void removexattr( const std::string& path, const std::string& name, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<removexattrSyncRequest> __req( new removexattrSyncRequest( path, name ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<removexattrResponse> __resp = __req->response_queue.dequeue_typed<removexattrResponse>( response_timeout_ns ); }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { rename( source_path, target_path, file_credentials, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::EventTarget* send_target ) { rename( source_path, target_path, file_credentials, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, uint64_t response_timeout_ns ) { rename( source_path, target_path, file_credentials, NULL, response_timeout_ns ); }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<renameSyncRequest> __req( new renameSyncRequest( source_path, target_path ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<renameResponse> __resp = __req->response_queue.dequeue_typed<renameResponse>( response_timeout_ns ); file_credentials = __resp->get_file_credentials(); }
        virtual void rmdir( const std::string& path ) { rmdir( path, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void rmdir( const std::string& path, YIELD::EventTarget* send_target ) { rmdir( path, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void rmdir( const std::string& path, uint64_t response_timeout_ns ) { rmdir( path, NULL, response_timeout_ns ); }
        virtual void rmdir( const std::string& path, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<rmdirSyncRequest> __req( new rmdirSyncRequest( path ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<rmdirResponse> __resp = __req->response_queue.dequeue_typed<rmdirResponse>( response_timeout_ns ); }
        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf ) { setattr( path, stbuf, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, YIELD::EventTarget* send_target ) { setattr( path, stbuf, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, uint64_t response_timeout_ns ) { setattr( path, stbuf, NULL, response_timeout_ns ); }
        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<setattrSyncRequest> __req( new setattrSyncRequest( path, stbuf ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<setattrResponse> __resp = __req->response_queue.dequeue_typed<setattrResponse>( response_timeout_ns ); }
        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags ) { setxattr( path, name, value, flags, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags, YIELD::EventTarget* send_target ) { setxattr( path, name, value, flags, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags, uint64_t response_timeout_ns ) { setxattr( path, name, value, flags, NULL, response_timeout_ns ); }
        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<setxattrSyncRequest> __req( new setxattrSyncRequest( path, name, value, flags ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<setxattrResponse> __resp = __req->response_queue.dequeue_typed<setxattrResponse>( response_timeout_ns ); }
        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf ) { statvfs( volume_name, stbuf, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf, YIELD::EventTarget* send_target ) { statvfs( volume_name, stbuf, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf, uint64_t response_timeout_ns ) { statvfs( volume_name, stbuf, NULL, response_timeout_ns ); }
        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<statvfsSyncRequest> __req( new statvfsSyncRequest( volume_name ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<statvfsResponse> __resp = __req->response_queue.dequeue_typed<statvfsResponse>( response_timeout_ns ); stbuf = __resp->get_stbuf(); }
        virtual void symlink( const std::string& target_path, const std::string& link_path ) { symlink( target_path, link_path, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void symlink( const std::string& target_path, const std::string& link_path, YIELD::EventTarget* send_target ) { symlink( target_path, link_path, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void symlink( const std::string& target_path, const std::string& link_path, uint64_t response_timeout_ns ) { symlink( target_path, link_path, NULL, response_timeout_ns ); }
        virtual void symlink( const std::string& target_path, const std::string& link_path, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<symlinkSyncRequest> __req( new symlinkSyncRequest( target_path, link_path ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<symlinkResponse> __resp = __req->response_queue.dequeue_typed<symlinkResponse>( response_timeout_ns ); }
        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { unlink( path, file_credentials, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::EventTarget* send_target ) { unlink( path, file_credentials, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, uint64_t response_timeout_ns ) { unlink( path, file_credentials, NULL, response_timeout_ns ); }
        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<unlinkSyncRequest> __req( new unlinkSyncRequest( path ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<unlinkResponse> __resp = __req->response_queue.dequeue_typed<unlinkResponse>( response_timeout_ns ); file_credentials = __resp->get_file_credentials(); }
        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) { utimens( path, atime_ns, mtime_ns, ctime_ns, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, YIELD::EventTarget* send_target ) { utimens( path, atime_ns, mtime_ns, ctime_ns, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, uint64_t response_timeout_ns ) { utimens( path, atime_ns, mtime_ns, ctime_ns, NULL, response_timeout_ns ); }
        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<utimensSyncRequest> __req( new utimensSyncRequest( path, atime_ns, mtime_ns, ctime_ns ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<utimensResponse> __resp = __req->response_queue.dequeue_typed<utimensResponse>( response_timeout_ns ); }
        virtual void xtreemfs_checkpoint() { xtreemfs_checkpoint( NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_checkpoint( YIELD::EventTarget* send_target ) { xtreemfs_checkpoint( send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_checkpoint( uint64_t response_timeout_ns ) { xtreemfs_checkpoint( NULL, response_timeout_ns ); }
        virtual void xtreemfs_checkpoint( YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_checkpointSyncRequest> __req( new xtreemfs_checkpointSyncRequest() ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_checkpointResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_checkpointResponse>( response_timeout_ns ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap ) { xtreemfs_check_file_exists( volume_id, file_ids, bitmap, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap, YIELD::EventTarget* send_target ) { xtreemfs_check_file_exists( volume_id, file_ids, bitmap, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap, uint64_t response_timeout_ns ) { xtreemfs_check_file_exists( volume_id, file_ids, bitmap, NULL, response_timeout_ns ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, std::string& bitmap, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_check_file_existsSyncRequest> __req( new xtreemfs_check_file_existsSyncRequest( volume_id, file_ids ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_check_file_existsResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_check_file_existsResponse>( response_timeout_ns ); bitmap = __resp->get_bitmap(); }
        virtual void xtreemfs_dump_database( const std::string& dump_file ) { xtreemfs_dump_database( dump_file, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_dump_database( const std::string& dump_file, YIELD::EventTarget* send_target ) { xtreemfs_dump_database( dump_file, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_dump_database( const std::string& dump_file, uint64_t response_timeout_ns ) { xtreemfs_dump_database( dump_file, NULL, response_timeout_ns ); }
        virtual void xtreemfs_dump_database( const std::string& dump_file, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_dump_databaseSyncRequest> __req( new xtreemfs_dump_databaseSyncRequest( dump_file ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_dump_databaseResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_dump_databaseResponse>( response_timeout_ns ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids ) { xtreemfs_get_suitable_osds( file_id, osd_uuids, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids, YIELD::EventTarget* send_target ) { xtreemfs_get_suitable_osds( file_id, osd_uuids, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids, uint64_t response_timeout_ns ) { xtreemfs_get_suitable_osds( file_id, osd_uuids, NULL, response_timeout_ns ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, org::xtreemfs::interfaces::StringSet& osd_uuids, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_get_suitable_osdsSyncRequest> __req( new xtreemfs_get_suitable_osdsSyncRequest( file_id ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_get_suitable_osdsResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_get_suitable_osdsResponse>( response_timeout_ns ); osd_uuids = __resp->get_osd_uuids(); }
        virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result ) { xtreemfs_internal_debug( operation, result, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result, YIELD::EventTarget* send_target ) { xtreemfs_internal_debug( operation, result, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result, uint64_t response_timeout_ns ) { xtreemfs_internal_debug( operation, result, NULL, response_timeout_ns ); }
        virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_internal_debugSyncRequest> __req( new xtreemfs_internal_debugSyncRequest( operation ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_internal_debugResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_internal_debugResponse>( response_timeout_ns ); result = __resp->get_result(); }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes ) { xtreemfs_lsvol( volumes, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes, YIELD::EventTarget* send_target ) { xtreemfs_lsvol( volumes, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes, uint64_t response_timeout_ns ) { xtreemfs_lsvol( volumes, NULL, response_timeout_ns ); }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_lsvolSyncRequest> __req( new xtreemfs_lsvolSyncRequest() ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_lsvolResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_lsvolResponse>( response_timeout_ns ); volumes = __resp->get_volumes(); }
        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { xtreemfs_listdir( path, names, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::EventTarget* send_target ) { xtreemfs_listdir( path, names, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names, uint64_t response_timeout_ns ) { xtreemfs_listdir( path, names, NULL, response_timeout_ns ); }
        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_listdirSyncRequest> __req( new xtreemfs_listdirSyncRequest( path ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_listdirResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_listdirResponse>( response_timeout_ns ); names = __resp->get_names(); }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume ) { xtreemfs_mkvol( volume, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume, YIELD::EventTarget* send_target ) { xtreemfs_mkvol( volume, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume, uint64_t response_timeout_ns ) { xtreemfs_mkvol( volume, NULL, response_timeout_ns ); }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_mkvolSyncRequest> __req( new xtreemfs_mkvolSyncRequest( volume ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_mkvolResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_mkvolResponse>( response_timeout_ns ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, YIELD::EventTarget* send_target ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, uint64_t response_timeout_ns ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, NULL, response_timeout_ns ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_renew_capabilitySyncRequest> __req( new xtreemfs_renew_capabilitySyncRequest( old_xcap ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_renew_capabilityResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_renew_capabilityResponse>( response_timeout_ns ); renewed_xcap = __resp->get_renewed_xcap(); }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) { xtreemfs_replica_add( file_id, new_replica, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::EventTarget* send_target ) { xtreemfs_replica_add( file_id, new_replica, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, uint64_t response_timeout_ns ) { xtreemfs_replica_add( file_id, new_replica, NULL, response_timeout_ns ); }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_replica_addSyncRequest> __req( new xtreemfs_replica_addSyncRequest( file_id, new_replica ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_replica_addResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_replica_addResponse>( response_timeout_ns ); }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas ) { xtreemfs_replica_list( file_id, replicas, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas, YIELD::EventTarget* send_target ) { xtreemfs_replica_list( file_id, replicas, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas, uint64_t response_timeout_ns ) { xtreemfs_replica_list( file_id, replicas, NULL, response_timeout_ns ); }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_replica_listSyncRequest> __req( new xtreemfs_replica_listSyncRequest( file_id ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_replica_listResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_replica_listResponse>( response_timeout_ns ); replicas = __resp->get_replicas(); }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap ) { xtreemfs_replica_remove( file_id, osd_uuid, delete_xcap, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap, YIELD::EventTarget* send_target ) { xtreemfs_replica_remove( file_id, osd_uuid, delete_xcap, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap, uint64_t response_timeout_ns ) { xtreemfs_replica_remove( file_id, osd_uuid, delete_xcap, NULL, response_timeout_ns ); }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_replica_removeSyncRequest> __req( new xtreemfs_replica_removeSyncRequest( file_id, osd_uuid ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_replica_removeResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_replica_removeResponse>( response_timeout_ns ); delete_xcap = __resp->get_delete_xcap(); }
        virtual void xtreemfs_restore_database( const std::string& dump_file ) { xtreemfs_restore_database( dump_file, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_restore_database( const std::string& dump_file, YIELD::EventTarget* send_target ) { xtreemfs_restore_database( dump_file, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_restore_database( const std::string& dump_file, uint64_t response_timeout_ns ) { xtreemfs_restore_database( dump_file, NULL, response_timeout_ns ); }
        virtual void xtreemfs_restore_database( const std::string& dump_file, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_restore_databaseSyncRequest> __req( new xtreemfs_restore_databaseSyncRequest( dump_file ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_restore_databaseResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_restore_databaseResponse>( response_timeout_ns ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, YIELD::EventTarget* send_target ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, uint64_t response_timeout_ns ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, NULL, response_timeout_ns ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_restore_fileSyncRequest> __req( new xtreemfs_restore_fileSyncRequest( file_path, file_id, file_size, osd_uuid, stripe_size ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_restore_fileResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_restore_fileResponse>( response_timeout_ns ); }
        virtual void xtreemfs_rmvol( const std::string& volume_name ) { xtreemfs_rmvol( volume_name, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_rmvol( const std::string& volume_name, YIELD::EventTarget* send_target ) { xtreemfs_rmvol( volume_name, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_rmvol( const std::string& volume_name, uint64_t response_timeout_ns ) { xtreemfs_rmvol( volume_name, NULL, response_timeout_ns ); }
        virtual void xtreemfs_rmvol( const std::string& volume_name, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_rmvolSyncRequest> __req( new xtreemfs_rmvolSyncRequest( volume_name ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_rmvolResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_rmvolResponse>( response_timeout_ns ); }
        virtual void xtreemfs_shutdown() { xtreemfs_shutdown( NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( YIELD::EventTarget* send_target ) { xtreemfs_shutdown( send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( uint64_t response_timeout_ns ) { xtreemfs_shutdown( NULL, response_timeout_ns ); }
        virtual void xtreemfs_shutdown( YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_shutdownSyncRequest> __req( new xtreemfs_shutdownSyncRequest() ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_shutdownResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_shutdownResponse>( response_timeout_ns ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { xtreemfs_update_file_size( xcap, osd_write_response, NULL, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target ) { xtreemfs_update_file_size( xcap, osd_write_response, send_target, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, uint64_t response_timeout_ns ) { xtreemfs_update_file_size( xcap, osd_write_response, NULL, response_timeout_ns ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, YIELD::EventTarget* send_target, uint64_t response_timeout_ns ) { YIELD::auto_Object<xtreemfs_update_file_sizeSyncRequest> __req( new xtreemfs_update_file_sizeSyncRequest( xcap, osd_write_response ) ); if ( send_target == NULL ) send_target = this; send_target->send( __req->incRef() ); YIELD::auto_Object<xtreemfs_update_file_sizeResponse> __resp = __req->response_queue.dequeue_typed<xtreemfs_update_file_sizeResponse>( response_timeout_ns ); }  // Request/response pair definitions for the operations in MRCInterface

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
        YIELD_OBJECT_PROTOTYPES( accessResponse, 1201 );

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
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }

        bool operator==( const accessRequest& other ) const { return path == other.path && mode == other.mode; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( accessRequest, 1201 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class chmodResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        chmodResponse() { }
        virtual ~chmodResponse() { }

        bool operator==( const chmodResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( chmodResponse, 1202 );

      };

      class chmodRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        chmodRequest() : mode( 0 ) { }
        chmodRequest( const std::string& path, uint32_t mode ) : path( path ), mode( mode ) { }
        chmodRequest( const char* path, size_t path_len, uint32_t mode ) : path( path, path_len ), mode( mode ) { }
        virtual ~chmodRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }

        bool operator==( const chmodRequest& other ) const { return path == other.path && mode == other.mode; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( chmodRequest, 1202 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class chownResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        chownResponse() { }
        virtual ~chownResponse() { }

        bool operator==( const chownResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( chownResponse, 1203 );

      };

      class chownRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        chownRequest() { }
        chownRequest( const std::string& path, const std::string& user_id, const std::string& group_id ) : path( path ), user_id( user_id ), group_id( group_id ) { }
        chownRequest( const char* path, size_t path_len, const char* user_id, size_t user_id_len, const char* group_id, size_t group_id_len ) : path( path, path_len ), user_id( user_id, user_id_len ), group_id( group_id, group_id_len ) { }
        virtual ~chownRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_user_id( const std::string& user_id ) { set_user_id( user_id.c_str(), user_id.size() ); }
        void set_user_id( const char* user_id, size_t user_id_len ) { this->user_id.assign( user_id, user_id_len ); }
        const std::string& get_user_id() const { return user_id; }
        void set_group_id( const std::string& group_id ) { set_group_id( group_id.c_str(), group_id.size() ); }
        void set_group_id( const char* group_id, size_t group_id_len ) { this->group_id.assign( group_id, group_id_len ); }
        const std::string& get_group_id() const { return group_id; }

        bool operator==( const chownRequest& other ) const { return path == other.path && user_id == other.user_id && group_id == other.group_id; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( chownRequest, 1203 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "group_id" ), group_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "user_id" ), user_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "group_id" ), group_id ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class creatResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        creatResponse() { }
        virtual ~creatResponse() { }

        bool operator==( const creatResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( creatResponse, 1204 );

      };

      class creatRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        creatRequest() : mode( 0 ) { }
        creatRequest( const std::string& path, uint32_t mode ) : path( path ), mode( mode ) { }
        creatRequest( const char* path, size_t path_len, uint32_t mode ) : path( path, path_len ), mode( mode ) { }
        virtual ~creatRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }

        bool operator==( const creatRequest& other ) const { return path == other.path && mode == other.mode; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( creatRequest, 1204 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }

      protected:
        std::string path;
        uint32_t mode;
      };

      class creatSyncRequest : public creatRequest
      {
      public:
        creatSyncRequest() : creatRequest( std::string(), 0 ) { }
        creatSyncRequest( const std::string& path, uint32_t mode ) : creatRequest( path, mode ) { }
        creatSyncRequest( const char* path, size_t path_len, uint32_t mode ) : creatRequest( path, path_len, mode ) { }
        virtual ~creatSyncRequest() { }

        bool operator==( const creatSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( ftruncateResponse, 1230 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "truncate_xcap" ), &truncate_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "truncate_xcap" ), truncate_xcap ); }

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
        YIELD_OBJECT_PROTOTYPES( ftruncateRequest, 1230 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "write_xcap" ), &write_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "write_xcap" ), write_xcap ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( getattrResponse, 1205 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "stbuf" ), stbuf ); }

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
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }

        bool operator==( const getattrRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( getattrRequest, 1205 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class getxattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        getxattrResponse() { }
        getxattrResponse( const std::string& value ) : value( value ) { }
        getxattrResponse( const char* value, size_t value_len ) : value( value, value_len ) { }
        virtual ~getxattrResponse() { }

        void set_value( const std::string& value ) { set_value( value.c_str(), value.size() ); }
        void set_value( const char* value, size_t value_len ) { this->value.assign( value, value_len ); }
        const std::string& get_value() const { return value; }

        bool operator==( const getxattrResponse& other ) const { return value == other.value; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( getxattrResponse, 1206 );

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
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len ) { this->name.assign( name, name_len ); }
        const std::string& get_name() const { return name; }

        bool operator==( const getxattrRequest& other ) const { return path == other.path && name == other.name; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( getxattrRequest, 1206 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class linkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        linkResponse() { }
        virtual ~linkResponse() { }

        bool operator==( const linkResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( linkResponse, 1207 );

      };

      class linkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        linkRequest() { }
        linkRequest( const std::string& target_path, const std::string& link_path ) : target_path( target_path ), link_path( link_path ) { }
        linkRequest( const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len ) : target_path( target_path, target_path_len ), link_path( link_path, link_path_len ) { }
        virtual ~linkRequest() { }

        void set_target_path( const std::string& target_path ) { set_target_path( target_path.c_str(), target_path.size() ); }
        void set_target_path( const char* target_path, size_t target_path_len ) { this->target_path.assign( target_path, target_path_len ); }
        const std::string& get_target_path() const { return target_path; }
        void set_link_path( const std::string& link_path ) { set_link_path( link_path.c_str(), link_path.size() ); }
        void set_link_path( const char* link_path, size_t link_path_len ) { this->link_path.assign( link_path, link_path_len ); }
        const std::string& get_link_path() const { return link_path; }

        bool operator==( const linkRequest& other ) const { return target_path == other.target_path && link_path == other.link_path; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( linkRequest, 1207 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( listxattrResponse, 1208 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "names" ), &names ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "names" ), names ); }

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
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }

        bool operator==( const listxattrRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( listxattrRequest, 1208 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class mkdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        mkdirResponse() { }
        virtual ~mkdirResponse() { }

        bool operator==( const mkdirResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( mkdirResponse, 1209 );

      };

      class mkdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        mkdirRequest() : mode( 0 ) { }
        mkdirRequest( const std::string& path, uint32_t mode ) : path( path ), mode( mode ) { }
        mkdirRequest( const char* path, size_t path_len, uint32_t mode ) : path( path, path_len ), mode( mode ) { }
        virtual ~mkdirRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }

        bool operator==( const mkdirRequest& other ) const { return path == other.path && mode == other.mode; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( mkdirRequest, 1209 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( openResponse, 1211 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); }

      protected:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
      };

      class openRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        openRequest() : flags( 0 ), mode( 0 ), attributes( 0 ) { }
        openRequest( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes ) : path( path ), flags( flags ), mode( mode ), attributes( attributes ) { }
        openRequest( const char* path, size_t path_len, uint32_t flags, uint32_t mode, uint32_t attributes ) : path( path, path_len ), flags( flags ), mode( mode ), attributes( attributes ) { }
        virtual ~openRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_flags( uint32_t flags ) { this->flags = flags; }
        uint32_t get_flags() const { return flags; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
        void set_attributes( uint32_t attributes ) { this->attributes = attributes; }
        uint32_t get_attributes() const { return attributes; }

        bool operator==( const openRequest& other ) const { return path == other.path && flags == other.flags && mode == other.mode && attributes == other.attributes; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( openRequest, 1211 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); flags = input_stream.readUint32( YIELD::StructuredStream::Declaration( "flags" ) ); mode = input_stream.readUint32( YIELD::StructuredStream::Declaration( "mode" ) ); attributes = input_stream.readUint32( YIELD::StructuredStream::Declaration( "attributes" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "flags" ), flags ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "mode" ), mode ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "attributes" ), attributes ); }

      protected:
        std::string path;
        uint32_t flags;
        uint32_t mode;
        uint32_t attributes;
      };

      class openSyncRequest : public openRequest
      {
      public:
        openSyncRequest() : openRequest( std::string(), 0, 0, 0 ) { }
        openSyncRequest( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes ) : openRequest( path, flags, mode, attributes ) { }
        openSyncRequest( const char* path, size_t path_len, uint32_t flags, uint32_t mode, uint32_t attributes ) : openRequest( path, path_len, flags, mode, attributes ) { }
        virtual ~openSyncRequest() { }

        bool operator==( const openSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( readdirResponse, 1212 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "directory_entries" ), &directory_entries ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "directory_entries" ), directory_entries ); }

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
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }

        bool operator==( const readdirRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( readdirRequest, 1212 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class removexattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        removexattrResponse() { }
        virtual ~removexattrResponse() { }

        bool operator==( const removexattrResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( removexattrResponse, 1213 );

      };

      class removexattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        removexattrRequest() { }
        removexattrRequest( const std::string& path, const std::string& name ) : path( path ), name( name ) { }
        removexattrRequest( const char* path, size_t path_len, const char* name, size_t name_len ) : path( path, path_len ), name( name, name_len ) { }
        virtual ~removexattrRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len ) { this->name.assign( name, name_len ); }
        const std::string& get_name() const { return name; }

        bool operator==( const removexattrRequest& other ) const { return path == other.path && name == other.name; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( removexattrRequest, 1213 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( renameResponse, 1214 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); }

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
        void set_source_path( const char* source_path, size_t source_path_len ) { this->source_path.assign( source_path, source_path_len ); }
        const std::string& get_source_path() const { return source_path; }
        void set_target_path( const std::string& target_path ) { set_target_path( target_path.c_str(), target_path.size() ); }
        void set_target_path( const char* target_path, size_t target_path_len ) { this->target_path.assign( target_path, target_path_len ); }
        const std::string& get_target_path() const { return target_path; }

        bool operator==( const renameRequest& other ) const { return source_path == other.source_path && target_path == other.target_path; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( renameRequest, 1214 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "source_path" ), source_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "source_path" ), source_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class rmdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        rmdirResponse() { }
        virtual ~rmdirResponse() { }

        bool operator==( const rmdirResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( rmdirResponse, 1215 );

      };

      class rmdirRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        rmdirRequest() { }
        rmdirRequest( const std::string& path ) : path( path ) { }
        rmdirRequest( const char* path, size_t path_len ) : path( path, path_len ) { }
        virtual ~rmdirRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }

        bool operator==( const rmdirRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( rmdirRequest, 1215 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class setattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        setattrResponse() { }
        virtual ~setattrResponse() { }

        bool operator==( const setattrResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( setattrResponse, 1217 );

      };

      class setattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        setattrRequest() { }
        setattrRequest( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf ) : path( path ), stbuf( stbuf ) { }
        setattrRequest( const char* path, size_t path_len, const org::xtreemfs::interfaces::Stat& stbuf ) : path( path, path_len ), stbuf( stbuf ) { }
        virtual ~setattrRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_stbuf( const org::xtreemfs::interfaces::Stat&  stbuf ) { this->stbuf = stbuf; }
        const org::xtreemfs::interfaces::Stat& get_stbuf() const { return stbuf; }

        bool operator==( const setattrRequest& other ) const { return path == other.path && stbuf == other.stbuf; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( setattrRequest, 1217 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readStruct( YIELD::StructuredStream::Declaration( "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeStruct( YIELD::StructuredStream::Declaration( "stbuf" ), stbuf ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class setxattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        setxattrResponse() { }
        virtual ~setxattrResponse() { }

        bool operator==( const setxattrResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( setxattrResponse, 1218 );

      };

      class setxattrRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        setxattrRequest() : flags( 0 ) { }
        setxattrRequest( const std::string& path, const std::string& name, const std::string& value, int32_t flags ) : path( path ), name( name ), value( value ), flags( flags ) { }
        setxattrRequest( const char* path, size_t path_len, const char* name, size_t name_len, const char* value, size_t value_len, int32_t flags ) : path( path, path_len ), name( name, name_len ), value( value, value_len ), flags( flags ) { }
        virtual ~setxattrRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len ) { this->name.assign( name, name_len ); }
        const std::string& get_name() const { return name; }
        void set_value( const std::string& value ) { set_value( value.c_str(), value.size() ); }
        void set_value( const char* value, size_t value_len ) { this->value.assign( value, value_len ); }
        const std::string& get_value() const { return value; }
        void set_flags( int32_t flags ) { this->flags = flags; }
        int32_t get_flags() const { return flags; }

        bool operator==( const setxattrRequest& other ) const { return path == other.path && name == other.name && value == other.value && flags == other.flags; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( setxattrRequest, 1218 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); input_stream.readString( YIELD::StructuredStream::Declaration( "name" ), name ); input_stream.readString( YIELD::StructuredStream::Declaration( "value" ), value ); flags = input_stream.readInt32( YIELD::StructuredStream::Declaration( "flags" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "name" ), name ); output_stream.writeString( YIELD::StructuredStream::Declaration( "value" ), value ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "flags" ), flags ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( statvfsResponse, 1219 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "stbuf" ), &stbuf ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "stbuf" ), stbuf ); }

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
        void set_volume_name( const char* volume_name, size_t volume_name_len ) { this->volume_name.assign( volume_name, volume_name_len ); }
        const std::string& get_volume_name() const { return volume_name; }

        bool operator==( const statvfsRequest& other ) const { return volume_name == other.volume_name; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( statvfsRequest, 1219 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class symlinkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        symlinkResponse() { }
        virtual ~symlinkResponse() { }

        bool operator==( const symlinkResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( symlinkResponse, 1220 );

      };

      class symlinkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        symlinkRequest() { }
        symlinkRequest( const std::string& target_path, const std::string& link_path ) : target_path( target_path ), link_path( link_path ) { }
        symlinkRequest( const char* target_path, size_t target_path_len, const char* link_path, size_t link_path_len ) : target_path( target_path, target_path_len ), link_path( link_path, link_path_len ) { }
        virtual ~symlinkRequest() { }

        void set_target_path( const std::string& target_path ) { set_target_path( target_path.c_str(), target_path.size() ); }
        void set_target_path( const char* target_path, size_t target_path_len ) { this->target_path.assign( target_path, target_path_len ); }
        const std::string& get_target_path() const { return target_path; }
        void set_link_path( const std::string& link_path ) { set_link_path( link_path.c_str(), link_path.size() ); }
        void set_link_path( const char* link_path, size_t link_path_len ) { this->link_path.assign( link_path, link_path_len ); }
        const std::string& get_link_path() const { return link_path; }

        bool operator==( const symlinkRequest& other ) const { return target_path == other.target_path && link_path == other.link_path; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( symlinkRequest, 1220 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "target_path" ), target_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "link_path" ), link_path ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( unlinkResponse, 1221 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "file_credentials" ), &file_credentials ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "file_credentials" ), file_credentials ); }

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
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }

        bool operator==( const unlinkRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( unlinkRequest, 1221 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class utimensResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        utimensResponse() { }
        virtual ~utimensResponse() { }

        bool operator==( const utimensResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( utimensResponse, 1222 );

      };

      class utimensRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        utimensRequest() : atime_ns( 0 ), mtime_ns( 0 ), ctime_ns( 0 ) { }
        utimensRequest( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) : path( path ), atime_ns( atime_ns ), mtime_ns( mtime_ns ), ctime_ns( ctime_ns ) { }
        utimensRequest( const char* path, size_t path_len, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) : path( path, path_len ), atime_ns( atime_ns ), mtime_ns( mtime_ns ), ctime_ns( ctime_ns ) { }
        virtual ~utimensRequest() { }

        void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }
        void set_atime_ns( uint64_t atime_ns ) { this->atime_ns = atime_ns; }
        uint64_t get_atime_ns() const { return atime_ns; }
        void set_mtime_ns( uint64_t mtime_ns ) { this->mtime_ns = mtime_ns; }
        uint64_t get_mtime_ns() const { return mtime_ns; }
        void set_ctime_ns( uint64_t ctime_ns ) { this->ctime_ns = ctime_ns; }
        uint64_t get_ctime_ns() const { return ctime_ns; }

        bool operator==( const utimensRequest& other ) const { return path == other.path && atime_ns == other.atime_ns && mtime_ns == other.mtime_ns && ctime_ns == other.ctime_ns; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( utimensRequest, 1222 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); atime_ns = input_stream.readUint64( YIELD::StructuredStream::Declaration( "atime_ns" ) ); mtime_ns = input_stream.readUint64( YIELD::StructuredStream::Declaration( "mtime_ns" ) ); ctime_ns = input_stream.readUint64( YIELD::StructuredStream::Declaration( "ctime_ns" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "atime_ns" ), atime_ns ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "mtime_ns" ), mtime_ns ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "ctime_ns" ), ctime_ns ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_checkpointResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_checkpointResponse() { }
        virtual ~xtreemfs_checkpointResponse() { }

        bool operator==( const xtreemfs_checkpointResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_checkpointResponse, 1251 );

      };

      class xtreemfs_checkpointRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_checkpointRequest() { }
        virtual ~xtreemfs_checkpointRequest() { }

        bool operator==( const xtreemfs_checkpointRequest& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_checkpointRequest, 1251 );

      };

      class xtreemfs_checkpointSyncRequest : public xtreemfs_checkpointRequest
      {
      public:
        xtreemfs_checkpointSyncRequest() { }
        virtual ~xtreemfs_checkpointSyncRequest() { }

        bool operator==( const xtreemfs_checkpointSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_check_file_existsResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_check_file_existsResponse() { }
        xtreemfs_check_file_existsResponse( const std::string& bitmap ) : bitmap( bitmap ) { }
        xtreemfs_check_file_existsResponse( const char* bitmap, size_t bitmap_len ) : bitmap( bitmap, bitmap_len ) { }
        virtual ~xtreemfs_check_file_existsResponse() { }

        void set_bitmap( const std::string& bitmap ) { set_bitmap( bitmap.c_str(), bitmap.size() ); }
        void set_bitmap( const char* bitmap, size_t bitmap_len ) { this->bitmap.assign( bitmap, bitmap_len ); }
        const std::string& get_bitmap() const { return bitmap; }

        bool operator==( const xtreemfs_check_file_existsResponse& other ) const { return bitmap == other.bitmap; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_check_file_existsResponse, 1223 );

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
        void set_volume_id( const char* volume_id, size_t volume_id_len ) { this->volume_id.assign( volume_id, volume_id_len ); }
        const std::string& get_volume_id() const { return volume_id; }
        void set_file_ids( const org::xtreemfs::interfaces::StringSet&  file_ids ) { this->file_ids = file_ids; }
        const org::xtreemfs::interfaces::StringSet& get_file_ids() const { return file_ids; }

        bool operator==( const xtreemfs_check_file_existsRequest& other ) const { return volume_id == other.volume_id && file_ids == other.file_ids; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_check_file_existsRequest, 1223 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "volume_id" ), volume_id ); input_stream.readSequence( YIELD::StructuredStream::Declaration( "file_ids" ), &file_ids ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_id" ), volume_id ); output_stream.writeSequence( YIELD::StructuredStream::Declaration( "file_ids" ), file_ids ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_dump_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_dump_databaseResponse() { }
        virtual ~xtreemfs_dump_databaseResponse() { }

        bool operator==( const xtreemfs_dump_databaseResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_dump_databaseResponse, 1252 );

      };

      class xtreemfs_dump_databaseRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_dump_databaseRequest() { }
        xtreemfs_dump_databaseRequest( const std::string& dump_file ) : dump_file( dump_file ) { }
        xtreemfs_dump_databaseRequest( const char* dump_file, size_t dump_file_len ) : dump_file( dump_file, dump_file_len ) { }
        virtual ~xtreemfs_dump_databaseRequest() { }

        void set_dump_file( const std::string& dump_file ) { set_dump_file( dump_file.c_str(), dump_file.size() ); }
        void set_dump_file( const char* dump_file, size_t dump_file_len ) { this->dump_file.assign( dump_file, dump_file_len ); }
        const std::string& get_dump_file() const { return dump_file; }

        bool operator==( const xtreemfs_dump_databaseRequest& other ) const { return dump_file == other.dump_file; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_dump_databaseRequest, 1252 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_get_suitable_osdsResponse, 1224 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "osd_uuids" ), &osd_uuids ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "osd_uuids" ), osd_uuids ); }

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
        void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
        const std::string& get_file_id() const { return file_id; }

        bool operator==( const xtreemfs_get_suitable_osdsRequest& other ) const { return file_id == other.file_id; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_get_suitable_osdsRequest, 1224 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_internal_debugResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_internal_debugResponse() { }
        xtreemfs_internal_debugResponse( const std::string& result ) : result( result ) { }
        xtreemfs_internal_debugResponse( const char* result, size_t result_len ) : result( result, result_len ) { }
        virtual ~xtreemfs_internal_debugResponse() { }

        void set_result( const std::string& result ) { set_result( result.c_str(), result.size() ); }
        void set_result( const char* result, size_t result_len ) { this->result.assign( result, result_len ); }
        const std::string& get_result() const { return result; }

        bool operator==( const xtreemfs_internal_debugResponse& other ) const { return result == other.result; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_debugResponse, 1254 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "result" ), result ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "result" ), result ); }

      protected:
        std::string result;
      };

      class xtreemfs_internal_debugRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_internal_debugRequest() { }
        xtreemfs_internal_debugRequest( const std::string& operation ) : operation( operation ) { }
        xtreemfs_internal_debugRequest( const char* operation, size_t operation_len ) : operation( operation, operation_len ) { }
        virtual ~xtreemfs_internal_debugRequest() { }

        void set_operation( const std::string& operation ) { set_operation( operation.c_str(), operation.size() ); }
        void set_operation( const char* operation, size_t operation_len ) { this->operation.assign( operation, operation_len ); }
        const std::string& get_operation() const { return operation; }

        bool operator==( const xtreemfs_internal_debugRequest& other ) const { return operation == other.operation; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_internal_debugRequest, 1254 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "operation" ), operation ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "operation" ), operation ); }

      protected:
        std::string operation;
      };

      class xtreemfs_internal_debugSyncRequest : public xtreemfs_internal_debugRequest
      {
      public:
        xtreemfs_internal_debugSyncRequest() : xtreemfs_internal_debugRequest( std::string() ) { }
        xtreemfs_internal_debugSyncRequest( const std::string& operation ) : xtreemfs_internal_debugRequest( operation ) { }
        xtreemfs_internal_debugSyncRequest( const char* operation, size_t operation_len ) : xtreemfs_internal_debugRequest( operation, operation_len ) { }
        virtual ~xtreemfs_internal_debugSyncRequest() { }

        bool operator==( const xtreemfs_internal_debugSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_lsvolResponse, 1231 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "volumes" ), &volumes ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "volumes" ), volumes ); }

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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_lsvolRequest, 1231 );

      };

      class xtreemfs_lsvolSyncRequest : public xtreemfs_lsvolRequest
      {
      public:
        xtreemfs_lsvolSyncRequest() { }
        virtual ~xtreemfs_lsvolSyncRequest() { }

        bool operator==( const xtreemfs_lsvolSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_listdirResponse, 1233 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "names" ), &names ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "names" ), names ); }

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
        void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
        const std::string& get_path() const { return path; }

        bool operator==( const xtreemfs_listdirRequest& other ) const { return path == other.path; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_listdirRequest, 1233 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "path" ), path ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "path" ), path ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_mkvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_mkvolResponse() { }
        virtual ~xtreemfs_mkvolResponse() { }

        bool operator==( const xtreemfs_mkvolResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_mkvolResponse, 1210 );

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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_mkvolRequest, 1210 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "volume" ), &volume ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "volume" ), volume ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_renew_capabilityResponse, 1225 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "renewed_xcap" ), &renewed_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "renewed_xcap" ), renewed_xcap ); }

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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_renew_capabilityRequest, 1225 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "old_xcap" ), &old_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "old_xcap" ), old_xcap ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_replica_addResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_replica_addResponse() { }
        virtual ~xtreemfs_replica_addResponse() { }

        bool operator==( const xtreemfs_replica_addResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_replica_addResponse, 1226 );

      };

      class xtreemfs_replica_addRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_replica_addRequest() { }
        xtreemfs_replica_addRequest( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) : file_id( file_id ), new_replica( new_replica ) { }
        xtreemfs_replica_addRequest( const char* file_id, size_t file_id_len, const org::xtreemfs::interfaces::Replica& new_replica ) : file_id( file_id, file_id_len ), new_replica( new_replica ) { }
        virtual ~xtreemfs_replica_addRequest() { }

        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
        const std::string& get_file_id() const { return file_id; }
        void set_new_replica( const org::xtreemfs::interfaces::Replica&  new_replica ) { this->new_replica = new_replica; }
        const org::xtreemfs::interfaces::Replica& get_new_replica() const { return new_replica; }

        bool operator==( const xtreemfs_replica_addRequest& other ) const { return file_id == other.file_id && new_replica == other.new_replica; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_replica_addRequest, 1226 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readStruct( YIELD::StructuredStream::Declaration( "new_replica" ), &new_replica ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeStruct( YIELD::StructuredStream::Declaration( "new_replica" ), new_replica ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_replica_listResponse, 1232 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readSequence( YIELD::StructuredStream::Declaration( "replicas" ), &replicas ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeSequence( YIELD::StructuredStream::Declaration( "replicas" ), replicas ); }

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
        void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
        const std::string& get_file_id() const { return file_id; }

        bool operator==( const xtreemfs_replica_listRequest& other ) const { return file_id == other.file_id; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_replica_listRequest, 1232 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_replica_removeResponse, 1227 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "delete_xcap" ), &delete_xcap ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "delete_xcap" ), delete_xcap ); }

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
        void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
        const std::string& get_file_id() const { return file_id; }
        void set_osd_uuid( const std::string& osd_uuid ) { set_osd_uuid( osd_uuid.c_str(), osd_uuid.size() ); }
        void set_osd_uuid( const char* osd_uuid, size_t osd_uuid_len ) { this->osd_uuid.assign( osd_uuid, osd_uuid_len ); }
        const std::string& get_osd_uuid() const { return osd_uuid; }

        bool operator==( const xtreemfs_replica_removeRequest& other ) const { return file_id == other.file_id && osd_uuid == other.osd_uuid; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_replica_removeRequest, 1227 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); input_stream.readString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_restore_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_restore_databaseResponse() { }
        virtual ~xtreemfs_restore_databaseResponse() { }

        bool operator==( const xtreemfs_restore_databaseResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_restore_databaseResponse, 1253 );

      };

      class xtreemfs_restore_databaseRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_restore_databaseRequest() { }
        xtreemfs_restore_databaseRequest( const std::string& dump_file ) : dump_file( dump_file ) { }
        xtreemfs_restore_databaseRequest( const char* dump_file, size_t dump_file_len ) : dump_file( dump_file, dump_file_len ) { }
        virtual ~xtreemfs_restore_databaseRequest() { }

        void set_dump_file( const std::string& dump_file ) { set_dump_file( dump_file.c_str(), dump_file.size() ); }
        void set_dump_file( const char* dump_file, size_t dump_file_len ) { this->dump_file.assign( dump_file, dump_file_len ); }
        const std::string& get_dump_file() const { return dump_file; }

        bool operator==( const xtreemfs_restore_databaseRequest& other ) const { return dump_file == other.dump_file; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_restore_databaseRequest, 1253 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "dump_file" ), dump_file ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_restore_fileResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_restore_fileResponse() { }
        virtual ~xtreemfs_restore_fileResponse() { }

        bool operator==( const xtreemfs_restore_fileResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_restore_fileResponse, 1228 );

      };

      class xtreemfs_restore_fileRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_restore_fileRequest() : file_size( 0 ), stripe_size( 0 ) { }
        xtreemfs_restore_fileRequest( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) : file_path( file_path ), file_id( file_id ), file_size( file_size ), osd_uuid( osd_uuid ), stripe_size( stripe_size ) { }
        xtreemfs_restore_fileRequest( const char* file_path, size_t file_path_len, const char* file_id, size_t file_id_len, uint64_t file_size, const char* osd_uuid, size_t osd_uuid_len, int32_t stripe_size ) : file_path( file_path, file_path_len ), file_id( file_id, file_id_len ), file_size( file_size ), osd_uuid( osd_uuid, osd_uuid_len ), stripe_size( stripe_size ) { }
        virtual ~xtreemfs_restore_fileRequest() { }

        void set_file_path( const std::string& file_path ) { set_file_path( file_path.c_str(), file_path.size() ); }
        void set_file_path( const char* file_path, size_t file_path_len ) { this->file_path.assign( file_path, file_path_len ); }
        const std::string& get_file_path() const { return file_path; }
        void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
        void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
        const std::string& get_file_id() const { return file_id; }
        void set_file_size( uint64_t file_size ) { this->file_size = file_size; }
        uint64_t get_file_size() const { return file_size; }
        void set_osd_uuid( const std::string& osd_uuid ) { set_osd_uuid( osd_uuid.c_str(), osd_uuid.size() ); }
        void set_osd_uuid( const char* osd_uuid, size_t osd_uuid_len ) { this->osd_uuid.assign( osd_uuid, osd_uuid_len ); }
        const std::string& get_osd_uuid() const { return osd_uuid; }
        void set_stripe_size( int32_t stripe_size ) { this->stripe_size = stripe_size; }
        int32_t get_stripe_size() const { return stripe_size; }

        bool operator==( const xtreemfs_restore_fileRequest& other ) const { return file_path == other.file_path && file_id == other.file_id && file_size == other.file_size && osd_uuid == other.osd_uuid && stripe_size == other.stripe_size; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_restore_fileRequest, 1228 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "file_path" ), file_path ); input_stream.readString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); file_size = input_stream.readUint64( YIELD::StructuredStream::Declaration( "file_size" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); stripe_size = input_stream.readInt32( YIELD::StructuredStream::Declaration( "stripe_size" ) ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "file_path" ), file_path ); output_stream.writeString( YIELD::StructuredStream::Declaration( "file_id" ), file_id ); output_stream.writeUint64( YIELD::StructuredStream::Declaration( "file_size" ), file_size ); output_stream.writeString( YIELD::StructuredStream::Declaration( "osd_uuid" ), osd_uuid ); output_stream.writeInt32( YIELD::StructuredStream::Declaration( "stripe_size" ), stripe_size ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_rmvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_rmvolResponse() { }
        virtual ~xtreemfs_rmvolResponse() { }

        bool operator==( const xtreemfs_rmvolResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_rmvolResponse, 1216 );

      };

      class xtreemfs_rmvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_rmvolRequest() { }
        xtreemfs_rmvolRequest( const std::string& volume_name ) : volume_name( volume_name ) { }
        xtreemfs_rmvolRequest( const char* volume_name, size_t volume_name_len ) : volume_name( volume_name, volume_name_len ) { }
        virtual ~xtreemfs_rmvolRequest() { }

        void set_volume_name( const std::string& volume_name ) { set_volume_name( volume_name.c_str(), volume_name.size() ); }
        void set_volume_name( const char* volume_name, size_t volume_name_len ) { this->volume_name.assign( volume_name, volume_name_len ); }
        const std::string& get_volume_name() const { return volume_name; }

        bool operator==( const xtreemfs_rmvolRequest& other ) const { return volume_name == other.volume_name; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_rmvolRequest, 1216 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "volume_name" ), volume_name ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_shutdownResponse() { }
        virtual ~xtreemfs_shutdownResponse() { }

        bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 1250 );

      };

      class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
      {
      public:
        xtreemfs_shutdownRequest() { }
        virtual ~xtreemfs_shutdownRequest() { }

        bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 1250 );

      };

      class xtreemfs_shutdownSyncRequest : public xtreemfs_shutdownRequest
      {
      public:
        xtreemfs_shutdownSyncRequest() { }
        virtual ~xtreemfs_shutdownSyncRequest() { }

        bool operator==( const xtreemfs_shutdownSyncRequest& ) const { return true; }


        // YIELD::Request
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

      class xtreemfs_update_file_sizeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
      {
      public:
        xtreemfs_update_file_sizeResponse() { }
        virtual ~xtreemfs_update_file_sizeResponse() { }

        bool operator==( const xtreemfs_update_file_sizeResponse& ) const { return true; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( xtreemfs_update_file_sizeResponse, 1229 );

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
        YIELD_OBJECT_PROTOTYPES( xtreemfs_update_file_sizeRequest, 1229 );

        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readStruct( YIELD::StructuredStream::Declaration( "xcap" ), &xcap ); input_stream.readStruct( YIELD::StructuredStream::Declaration( "osd_write_response" ), &osd_write_response ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeStruct( YIELD::StructuredStream::Declaration( "xcap" ), xcap ); output_stream.writeStruct( YIELD::StructuredStream::Declaration( "osd_write_response" ), osd_write_response ); }

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
        bool respond( YIELD::Response& response ) { return response_queue.enqueue( response ); }

      private:
        friend class MRCInterface;
        YIELD::OneSignalEventQueue< YIELD::NonBlockingFiniteQueue<YIELD::Event*, 16 > > response_queue;
      };

        class ConcurrentModificationException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          ConcurrentModificationException() { }
        ConcurrentModificationException( const std::string& stack_trace ) : stack_trace( stack_trace ) { }
        ConcurrentModificationException( const char* stack_trace, size_t stack_trace_len ) : stack_trace( stack_trace, stack_trace_len ) { }
          ConcurrentModificationException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~ConcurrentModificationException() throw() { }

        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
        const std::string& get_stack_trace() const { return stack_trace; }

          // YIELD::ExceptionResponse
          virtual ExceptionResponse* clone() const { return new ConcurrentModificationException( stack_trace); }
          virtual void throwStackClone() const { throw ConcurrentModificationException( stack_trace); }
        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

      protected:
        std::string stack_trace;
        };

        class errnoException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          errnoException() : error_code( 0 ) { }
        errnoException( uint32_t error_code, const std::string& error_message, const std::string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
        errnoException( uint32_t error_code, const char* error_message, size_t error_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), error_message( error_message, error_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
          errnoException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~errnoException() throw() { }

        void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
        uint32_t get_error_code() const { return error_code; }
        void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
        void set_error_message( const char* error_message, size_t error_message_len ) { this->error_message.assign( error_message, error_message_len ); }
        const std::string& get_error_message() const { return error_message; }
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
        const std::string& get_stack_trace() const { return stack_trace; }

          // YIELD::ExceptionResponse
          virtual ExceptionResponse* clone() const { return new errnoException( error_code, error_message, stack_trace); }
          virtual void throwStackClone() const { throw errnoException( error_code, error_message, stack_trace); }
        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

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
        InvalidArgumentException( const char* error_message, size_t error_message_len ) : error_message( error_message, error_message_len ) { }
          InvalidArgumentException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~InvalidArgumentException() throw() { }

        void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
        void set_error_message( const char* error_message, size_t error_message_len ) { this->error_message.assign( error_message, error_message_len ); }
        const std::string& get_error_message() const { return error_message; }

          // YIELD::ExceptionResponse
          virtual ExceptionResponse* clone() const { return new InvalidArgumentException( error_message); }
          virtual void throwStackClone() const { throw InvalidArgumentException( error_message); }
        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); }

      protected:
        std::string error_message;
        };

        class MRCException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          MRCException() : error_code( 0 ) { }
        MRCException( uint32_t error_code, const std::string& error_message, const std::string& stack_trace ) : error_code( error_code ), error_message( error_message ), stack_trace( stack_trace ) { }
        MRCException( uint32_t error_code, const char* error_message, size_t error_message_len, const char* stack_trace, size_t stack_trace_len ) : error_code( error_code ), error_message( error_message, error_message_len ), stack_trace( stack_trace, stack_trace_len ) { }
          MRCException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~MRCException() throw() { }

        void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
        uint32_t get_error_code() const { return error_code; }
        void set_error_message( const std::string& error_message ) { set_error_message( error_message.c_str(), error_message.size() ); }
        void set_error_message( const char* error_message, size_t error_message_len ) { this->error_message.assign( error_message, error_message_len ); }
        const std::string& get_error_message() const { return error_message; }
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
        const std::string& get_stack_trace() const { return stack_trace; }

          // YIELD::ExceptionResponse
          virtual ExceptionResponse* clone() const { return new MRCException( error_code, error_message, stack_trace); }
          virtual void throwStackClone() const { throw MRCException( error_code, error_message, stack_trace); }
        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "error_message" ), error_message ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

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
        ProtocolException( uint32_t accept_stat, uint32_t error_code, const char* stack_trace, size_t stack_trace_len ) : accept_stat( accept_stat ), error_code( error_code ), stack_trace( stack_trace, stack_trace_len ) { }
          ProtocolException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~ProtocolException() throw() { }

        void set_accept_stat( uint32_t accept_stat ) { this->accept_stat = accept_stat; }
        uint32_t get_accept_stat() const { return accept_stat; }
        void set_error_code( uint32_t error_code ) { this->error_code = error_code; }
        uint32_t get_error_code() const { return error_code; }
        void set_stack_trace( const std::string& stack_trace ) { set_stack_trace( stack_trace.c_str(), stack_trace.size() ); }
        void set_stack_trace( const char* stack_trace, size_t stack_trace_len ) { this->stack_trace.assign( stack_trace, stack_trace_len ); }
        const std::string& get_stack_trace() const { return stack_trace; }

          // YIELD::ExceptionResponse
          virtual ExceptionResponse* clone() const { return new ProtocolException( accept_stat, error_code, stack_trace); }
          virtual void throwStackClone() const { throw ProtocolException( accept_stat, error_code, stack_trace); }
        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { accept_stat = input_stream.readUint32( YIELD::StructuredStream::Declaration( "accept_stat" ) ); error_code = input_stream.readUint32( YIELD::StructuredStream::Declaration( "error_code" ) ); input_stream.readString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeUint32( YIELD::StructuredStream::Declaration( "accept_stat" ), accept_stat ); output_stream.writeUint32( YIELD::StructuredStream::Declaration( "error_code" ), error_code ); output_stream.writeString( YIELD::StructuredStream::Declaration( "stack_trace" ), stack_trace ); }

      protected:
        uint32_t accept_stat;
        uint32_t error_code;
        std::string stack_trace;
        };

        class RedirectException : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
        {
        public:
          RedirectException() { }
        RedirectException( const std::string& to_uuid ) : to_uuid( to_uuid ) { }
        RedirectException( const char* to_uuid, size_t to_uuid_len ) : to_uuid( to_uuid, to_uuid_len ) { }
          RedirectException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
          virtual ~RedirectException() throw() { }

        void set_to_uuid( const std::string& to_uuid ) { set_to_uuid( to_uuid.c_str(), to_uuid.size() ); }
        void set_to_uuid( const char* to_uuid, size_t to_uuid_len ) { this->to_uuid.assign( to_uuid, to_uuid_len ); }
        const std::string& get_to_uuid() const { return to_uuid; }

          // YIELD::ExceptionResponse
          virtual ExceptionResponse* clone() const { return new RedirectException( to_uuid); }
          virtual void throwStackClone() const { throw RedirectException( to_uuid); }
        // YIELD::Object
        void deserialize( YIELD::StructuredInputStream& input_stream ) { input_stream.readString( YIELD::StructuredStream::Declaration( "to_uuid" ), to_uuid ); }
        void serialize( YIELD::StructuredOutputStream& output_stream ) { output_stream.writeString( YIELD::StructuredStream::Declaration( "to_uuid" ), to_uuid ); }

      protected:
        std::string to_uuid;
        };



        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( MRCInterface, 1200 );

        // YIELD::EventHandler
        virtual void handleEvent( YIELD::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.get_tag() )
            {
              case 1201UL: handleaccessRequest( static_cast<accessRequest&>( ev ) ); return;
              case 1202UL: handlechmodRequest( static_cast<chmodRequest&>( ev ) ); return;
              case 1203UL: handlechownRequest( static_cast<chownRequest&>( ev ) ); return;
              case 1204UL: handlecreatRequest( static_cast<creatRequest&>( ev ) ); return;
              case 1230UL: handleftruncateRequest( static_cast<ftruncateRequest&>( ev ) ); return;
              case 1205UL: handlegetattrRequest( static_cast<getattrRequest&>( ev ) ); return;
              case 1206UL: handlegetxattrRequest( static_cast<getxattrRequest&>( ev ) ); return;
              case 1207UL: handlelinkRequest( static_cast<linkRequest&>( ev ) ); return;
              case 1208UL: handlelistxattrRequest( static_cast<listxattrRequest&>( ev ) ); return;
              case 1209UL: handlemkdirRequest( static_cast<mkdirRequest&>( ev ) ); return;
              case 1211UL: handleopenRequest( static_cast<openRequest&>( ev ) ); return;
              case 1212UL: handlereaddirRequest( static_cast<readdirRequest&>( ev ) ); return;
              case 1213UL: handleremovexattrRequest( static_cast<removexattrRequest&>( ev ) ); return;
              case 1214UL: handlerenameRequest( static_cast<renameRequest&>( ev ) ); return;
              case 1215UL: handlermdirRequest( static_cast<rmdirRequest&>( ev ) ); return;
              case 1217UL: handlesetattrRequest( static_cast<setattrRequest&>( ev ) ); return;
              case 1218UL: handlesetxattrRequest( static_cast<setxattrRequest&>( ev ) ); return;
              case 1219UL: handlestatvfsRequest( static_cast<statvfsRequest&>( ev ) ); return;
              case 1220UL: handlesymlinkRequest( static_cast<symlinkRequest&>( ev ) ); return;
              case 1221UL: handleunlinkRequest( static_cast<unlinkRequest&>( ev ) ); return;
              case 1222UL: handleutimensRequest( static_cast<utimensRequest&>( ev ) ); return;
              case 1251UL: handlextreemfs_checkpointRequest( static_cast<xtreemfs_checkpointRequest&>( ev ) ); return;
              case 1223UL: handlextreemfs_check_file_existsRequest( static_cast<xtreemfs_check_file_existsRequest&>( ev ) ); return;
              case 1252UL: handlextreemfs_dump_databaseRequest( static_cast<xtreemfs_dump_databaseRequest&>( ev ) ); return;
              case 1224UL: handlextreemfs_get_suitable_osdsRequest( static_cast<xtreemfs_get_suitable_osdsRequest&>( ev ) ); return;
              case 1254UL: handlextreemfs_internal_debugRequest( static_cast<xtreemfs_internal_debugRequest&>( ev ) ); return;
              case 1231UL: handlextreemfs_lsvolRequest( static_cast<xtreemfs_lsvolRequest&>( ev ) ); return;
              case 1233UL: handlextreemfs_listdirRequest( static_cast<xtreemfs_listdirRequest&>( ev ) ); return;
              case 1210UL: handlextreemfs_mkvolRequest( static_cast<xtreemfs_mkvolRequest&>( ev ) ); return;
              case 1225UL: handlextreemfs_renew_capabilityRequest( static_cast<xtreemfs_renew_capabilityRequest&>( ev ) ); return;
              case 1226UL: handlextreemfs_replica_addRequest( static_cast<xtreemfs_replica_addRequest&>( ev ) ); return;
              case 1232UL: handlextreemfs_replica_listRequest( static_cast<xtreemfs_replica_listRequest&>( ev ) ); return;
              case 1227UL: handlextreemfs_replica_removeRequest( static_cast<xtreemfs_replica_removeRequest&>( ev ) ); return;
              case 1253UL: handlextreemfs_restore_databaseRequest( static_cast<xtreemfs_restore_databaseRequest&>( ev ) ); return;
              case 1228UL: handlextreemfs_restore_fileRequest( static_cast<xtreemfs_restore_fileRequest&>( ev ) ); return;
              case 1216UL: handlextreemfs_rmvolRequest( static_cast<xtreemfs_rmvolRequest&>( ev ) ); return;
              case 1250UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
              case 1229UL: handlextreemfs_update_file_sizeRequest( static_cast<xtreemfs_update_file_sizeRequest&>( ev ) ); return;
              default: handleUnknownEvent( ev ); return;
            }
          }
          catch( YIELD::ExceptionResponse* exception_response )
          {
            static_cast<YIELD::Request&>( ev ).respond( *exception_response );
          }
          catch ( YIELD::ExceptionResponse& exception_response )
          {
            static_cast<YIELD::Request&>( ev ).respond( *exception_response.clone() );
          }
          catch ( YIELD::Exception& exc )
          {
            static_cast<YIELD::Request&>( ev ).respond( *( new YIELD::ExceptionResponse( exc ) ) );
          }

          YIELD::Object::decRef( ev );
        }


        // YIELD::Interface
          virtual YIELD::auto_Object<YIELD::Request> createRequest( uint32_t tag )
          {
            switch ( tag )
            {
              case 1201: return new accessRequest;
              case 1202: return new chmodRequest;
              case 1203: return new chownRequest;
              case 1204: return new creatRequest;
              case 1230: return new ftruncateRequest;
              case 1205: return new getattrRequest;
              case 1206: return new getxattrRequest;
              case 1207: return new linkRequest;
              case 1208: return new listxattrRequest;
              case 1209: return new mkdirRequest;
              case 1211: return new openRequest;
              case 1212: return new readdirRequest;
              case 1213: return new removexattrRequest;
              case 1214: return new renameRequest;
              case 1215: return new rmdirRequest;
              case 1217: return new setattrRequest;
              case 1218: return new setxattrRequest;
              case 1219: return new statvfsRequest;
              case 1220: return new symlinkRequest;
              case 1221: return new unlinkRequest;
              case 1222: return new utimensRequest;
              case 1251: return new xtreemfs_checkpointRequest;
              case 1223: return new xtreemfs_check_file_existsRequest;
              case 1252: return new xtreemfs_dump_databaseRequest;
              case 1224: return new xtreemfs_get_suitable_osdsRequest;
              case 1254: return new xtreemfs_internal_debugRequest;
              case 1231: return new xtreemfs_lsvolRequest;
              case 1233: return new xtreemfs_listdirRequest;
              case 1210: return new xtreemfs_mkvolRequest;
              case 1225: return new xtreemfs_renew_capabilityRequest;
              case 1226: return new xtreemfs_replica_addRequest;
              case 1232: return new xtreemfs_replica_listRequest;
              case 1227: return new xtreemfs_replica_removeRequest;
              case 1253: return new xtreemfs_restore_databaseRequest;
              case 1228: return new xtreemfs_restore_fileRequest;
              case 1216: return new xtreemfs_rmvolRequest;
              case 1250: return new xtreemfs_shutdownRequest;
              case 1229: return new xtreemfs_update_file_sizeRequest;
              default: return NULL;
            }
          }

          virtual YIELD::auto_Object<YIELD::Response> createResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 1201: return new accessResponse;
              case 1202: return new chmodResponse;
              case 1203: return new chownResponse;
              case 1204: return new creatResponse;
              case 1230: return new ftruncateResponse;
              case 1205: return new getattrResponse;
              case 1206: return new getxattrResponse;
              case 1207: return new linkResponse;
              case 1208: return new listxattrResponse;
              case 1209: return new mkdirResponse;
              case 1211: return new openResponse;
              case 1212: return new readdirResponse;
              case 1213: return new removexattrResponse;
              case 1214: return new renameResponse;
              case 1215: return new rmdirResponse;
              case 1217: return new setattrResponse;
              case 1218: return new setxattrResponse;
              case 1219: return new statvfsResponse;
              case 1220: return new symlinkResponse;
              case 1221: return new unlinkResponse;
              case 1222: return new utimensResponse;
              case 1251: return new xtreemfs_checkpointResponse;
              case 1223: return new xtreemfs_check_file_existsResponse;
              case 1252: return new xtreemfs_dump_databaseResponse;
              case 1224: return new xtreemfs_get_suitable_osdsResponse;
              case 1254: return new xtreemfs_internal_debugResponse;
              case 1231: return new xtreemfs_lsvolResponse;
              case 1233: return new xtreemfs_listdirResponse;
              case 1210: return new xtreemfs_mkvolResponse;
              case 1225: return new xtreemfs_renew_capabilityResponse;
              case 1226: return new xtreemfs_replica_addResponse;
              case 1232: return new xtreemfs_replica_listResponse;
              case 1227: return new xtreemfs_replica_removeResponse;
              case 1253: return new xtreemfs_restore_databaseResponse;
              case 1228: return new xtreemfs_restore_fileResponse;
              case 1216: return new xtreemfs_rmvolResponse;
              case 1250: return new xtreemfs_shutdownResponse;
              case 1229: return new xtreemfs_update_file_sizeResponse;
              default: return NULL;
            }
          }

          virtual YIELD::auto_Object<YIELD::ExceptionResponse> createExceptionResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 1206: return new ConcurrentModificationException;
              case 1207: return new errnoException;
              case 1208: return new InvalidArgumentException;
              case 1211: return new MRCException;
              case 1209: return new ProtocolException;
              case 1210: return new RedirectException;
              default: return NULL;
            }
          }



      protected:
        virtual void handleaccessRequest( accessRequest& req ) { YIELD::auto_Object<accessResponse> resp( new accessResponse ); bool _return_value = _access( req.get_path(), req.get_mode() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlechmodRequest( chmodRequest& req ) { YIELD::auto_Object<chmodResponse> resp( new chmodResponse ); _chmod( req.get_path(), req.get_mode() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlechownRequest( chownRequest& req ) { YIELD::auto_Object<chownResponse> resp( new chownResponse ); _chown( req.get_path(), req.get_user_id(), req.get_group_id() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlecreatRequest( creatRequest& req ) { YIELD::auto_Object<creatResponse> resp( new creatResponse ); _creat( req.get_path(), req.get_mode() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handleftruncateRequest( ftruncateRequest& req ) { YIELD::auto_Object<ftruncateResponse> resp( new ftruncateResponse ); org::xtreemfs::interfaces::XCap truncate_xcap; _ftruncate( req.get_write_xcap(), truncate_xcap ); resp->set_truncate_xcap( truncate_xcap ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlegetattrRequest( getattrRequest& req ) { YIELD::auto_Object<getattrResponse> resp( new getattrResponse ); org::xtreemfs::interfaces::Stat stbuf; _getattr( req.get_path(), stbuf ); resp->set_stbuf( stbuf ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlegetxattrRequest( getxattrRequest& req ) { YIELD::auto_Object<getxattrResponse> resp( new getxattrResponse ); std::string value; _getxattr( req.get_path(), req.get_name(), value ); resp->set_value( value ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlelinkRequest( linkRequest& req ) { YIELD::auto_Object<linkResponse> resp( new linkResponse ); _link( req.get_target_path(), req.get_link_path() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlelistxattrRequest( listxattrRequest& req ) { YIELD::auto_Object<listxattrResponse> resp( new listxattrResponse ); org::xtreemfs::interfaces::StringSet names; _listxattr( req.get_path(), names ); resp->set_names( names ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlemkdirRequest( mkdirRequest& req ) { YIELD::auto_Object<mkdirResponse> resp( new mkdirResponse ); _mkdir( req.get_path(), req.get_mode() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handleopenRequest( openRequest& req ) { YIELD::auto_Object<openResponse> resp( new openResponse ); org::xtreemfs::interfaces::FileCredentials file_credentials; _open( req.get_path(), req.get_flags(), req.get_mode(), req.get_attributes(), file_credentials ); resp->set_file_credentials( file_credentials ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlereaddirRequest( readdirRequest& req ) { YIELD::auto_Object<readdirResponse> resp( new readdirResponse ); org::xtreemfs::interfaces::DirectoryEntrySet directory_entries; _readdir( req.get_path(), directory_entries ); resp->set_directory_entries( directory_entries ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handleremovexattrRequest( removexattrRequest& req ) { YIELD::auto_Object<removexattrResponse> resp( new removexattrResponse ); _removexattr( req.get_path(), req.get_name() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlerenameRequest( renameRequest& req ) { YIELD::auto_Object<renameResponse> resp( new renameResponse ); org::xtreemfs::interfaces::FileCredentialsSet file_credentials; _rename( req.get_source_path(), req.get_target_path(), file_credentials ); resp->set_file_credentials( file_credentials ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlermdirRequest( rmdirRequest& req ) { YIELD::auto_Object<rmdirResponse> resp( new rmdirResponse ); _rmdir( req.get_path() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlesetattrRequest( setattrRequest& req ) { YIELD::auto_Object<setattrResponse> resp( new setattrResponse ); _setattr( req.get_path(), req.get_stbuf() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlesetxattrRequest( setxattrRequest& req ) { YIELD::auto_Object<setxattrResponse> resp( new setxattrResponse ); _setxattr( req.get_path(), req.get_name(), req.get_value(), req.get_flags() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlestatvfsRequest( statvfsRequest& req ) { YIELD::auto_Object<statvfsResponse> resp( new statvfsResponse ); org::xtreemfs::interfaces::StatVFS stbuf; _statvfs( req.get_volume_name(), stbuf ); resp->set_stbuf( stbuf ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlesymlinkRequest( symlinkRequest& req ) { YIELD::auto_Object<symlinkResponse> resp( new symlinkResponse ); _symlink( req.get_target_path(), req.get_link_path() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handleunlinkRequest( unlinkRequest& req ) { YIELD::auto_Object<unlinkResponse> resp( new unlinkResponse ); org::xtreemfs::interfaces::FileCredentialsSet file_credentials; _unlink( req.get_path(), file_credentials ); resp->set_file_credentials( file_credentials ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handleutimensRequest( utimensRequest& req ) { YIELD::auto_Object<utimensResponse> resp( new utimensResponse ); _utimens( req.get_path(), req.get_atime_ns(), req.get_mtime_ns(), req.get_ctime_ns() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& req ) { YIELD::auto_Object<xtreemfs_checkpointResponse> resp( new xtreemfs_checkpointResponse ); _xtreemfs_checkpoint(); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_check_file_existsRequest( xtreemfs_check_file_existsRequest& req ) { YIELD::auto_Object<xtreemfs_check_file_existsResponse> resp( new xtreemfs_check_file_existsResponse ); std::string bitmap; _xtreemfs_check_file_exists( req.get_volume_id(), req.get_file_ids(), bitmap ); resp->set_bitmap( bitmap ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_dump_databaseRequest( xtreemfs_dump_databaseRequest& req ) { YIELD::auto_Object<xtreemfs_dump_databaseResponse> resp( new xtreemfs_dump_databaseResponse ); _xtreemfs_dump_database( req.get_dump_file() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_get_suitable_osdsRequest( xtreemfs_get_suitable_osdsRequest& req ) { YIELD::auto_Object<xtreemfs_get_suitable_osdsResponse> resp( new xtreemfs_get_suitable_osdsResponse ); org::xtreemfs::interfaces::StringSet osd_uuids; _xtreemfs_get_suitable_osds( req.get_file_id(), osd_uuids ); resp->set_osd_uuids( osd_uuids ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_internal_debugRequest( xtreemfs_internal_debugRequest& req ) { YIELD::auto_Object<xtreemfs_internal_debugResponse> resp( new xtreemfs_internal_debugResponse ); std::string result; _xtreemfs_internal_debug( req.get_operation(), result ); resp->set_result( result ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_lsvolRequest( xtreemfs_lsvolRequest& req ) { YIELD::auto_Object<xtreemfs_lsvolResponse> resp( new xtreemfs_lsvolResponse ); org::xtreemfs::interfaces::VolumeSet volumes; _xtreemfs_lsvol( volumes ); resp->set_volumes( volumes ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_listdirRequest( xtreemfs_listdirRequest& req ) { YIELD::auto_Object<xtreemfs_listdirResponse> resp( new xtreemfs_listdirResponse ); org::xtreemfs::interfaces::StringSet names; _xtreemfs_listdir( req.get_path(), names ); resp->set_names( names ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_mkvolRequest( xtreemfs_mkvolRequest& req ) { YIELD::auto_Object<xtreemfs_mkvolResponse> resp( new xtreemfs_mkvolResponse ); _xtreemfs_mkvol( req.get_volume() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_renew_capabilityRequest( xtreemfs_renew_capabilityRequest& req ) { YIELD::auto_Object<xtreemfs_renew_capabilityResponse> resp( new xtreemfs_renew_capabilityResponse ); org::xtreemfs::interfaces::XCap renewed_xcap; _xtreemfs_renew_capability( req.get_old_xcap(), renewed_xcap ); resp->set_renewed_xcap( renewed_xcap ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_replica_addRequest( xtreemfs_replica_addRequest& req ) { YIELD::auto_Object<xtreemfs_replica_addResponse> resp( new xtreemfs_replica_addResponse ); _xtreemfs_replica_add( req.get_file_id(), req.get_new_replica() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_replica_listRequest( xtreemfs_replica_listRequest& req ) { YIELD::auto_Object<xtreemfs_replica_listResponse> resp( new xtreemfs_replica_listResponse ); org::xtreemfs::interfaces::ReplicaSet replicas; _xtreemfs_replica_list( req.get_file_id(), replicas ); resp->set_replicas( replicas ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_replica_removeRequest( xtreemfs_replica_removeRequest& req ) { YIELD::auto_Object<xtreemfs_replica_removeResponse> resp( new xtreemfs_replica_removeResponse ); org::xtreemfs::interfaces::XCap delete_xcap; _xtreemfs_replica_remove( req.get_file_id(), req.get_osd_uuid(), delete_xcap ); resp->set_delete_xcap( delete_xcap ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_restore_databaseRequest( xtreemfs_restore_databaseRequest& req ) { YIELD::auto_Object<xtreemfs_restore_databaseResponse> resp( new xtreemfs_restore_databaseResponse ); _xtreemfs_restore_database( req.get_dump_file() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_restore_fileRequest( xtreemfs_restore_fileRequest& req ) { YIELD::auto_Object<xtreemfs_restore_fileResponse> resp( new xtreemfs_restore_fileResponse ); _xtreemfs_restore_file( req.get_file_path(), req.get_file_id(), req.get_file_size(), req.get_osd_uuid(), req.get_stripe_size() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_rmvolRequest( xtreemfs_rmvolRequest& req ) { YIELD::auto_Object<xtreemfs_rmvolResponse> resp( new xtreemfs_rmvolResponse ); _xtreemfs_rmvol( req.get_volume_name() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req ) { YIELD::auto_Object<xtreemfs_shutdownResponse> resp( new xtreemfs_shutdownResponse ); _xtreemfs_shutdown(); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }
        virtual void handlextreemfs_update_file_sizeRequest( xtreemfs_update_file_sizeRequest& req ) { YIELD::auto_Object<xtreemfs_update_file_sizeResponse> resp( new xtreemfs_update_file_sizeResponse ); _xtreemfs_update_file_size( req.get_xcap(), req.get_osd_write_response() ); req.respond( *resp.release() ); YIELD::Object::decRef( req );; }

      virtual bool _access( const std::string& path, uint32_t mode ) { return false; }
        virtual void _chmod( const std::string& path, uint32_t mode ) { }
        virtual void _chown( const std::string& path, const std::string& user_id, const std::string& group_id ) { }
        virtual void _creat( const std::string& path, uint32_t mode ) { }
        virtual void _ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap ) { }
        virtual void _getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf ) { }
        virtual void _getxattr( const std::string& path, const std::string& name, std::string& value ) { }
        virtual void _link( const std::string& target_path, const std::string& link_path ) { }
        virtual void _listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { }
        virtual void _mkdir( const std::string& path, uint32_t mode ) { }
        virtual void _open( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, org::xtreemfs::interfaces::FileCredentials& file_credentials ) { }
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
        virtual void _xtreemfs_internal_debug( const std::string& operation, std::string& result ) { }
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
      virtual void _creat( const std::string& path, uint32_t mode );\
      virtual void _ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap );\
      virtual void _getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf );\
      virtual void _getxattr( const std::string& path, const std::string& name, std::string& value );\
      virtual void _link( const std::string& target_path, const std::string& link_path );\
      virtual void _listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names );\
      virtual void _mkdir( const std::string& path, uint32_t mode );\
      virtual void _open( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, org::xtreemfs::interfaces::FileCredentials& file_credentials );\
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
      virtual void _xtreemfs_internal_debug( const std::string& operation, std::string& result );\
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
      virtual void handlecreatRequestRequest( creatRequest& req );\
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
      virtual void handlextreemfs_internal_debugRequestRequest( xtreemfs_internal_debugRequest& req );\
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
