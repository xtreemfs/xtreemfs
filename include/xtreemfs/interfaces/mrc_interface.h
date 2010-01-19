// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_INTERFACES_MRC_INTERFACE_H_
#define _XTREEMFS_INTERFACES_MRC_INTERFACE_H_

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
        Stat() : dev( 0 ), ino( 0 ), mode( 0 ), nlink( 0 ), uid( 0 ), gid( 0 ), size( 0 ), atime_ns( 0 ), mtime_ns( 0 ), ctime_ns( 0 ), blksize( 0 ), truncate_epoch( 0 ), attributes( 0 ) { }
        Stat( uint64_t dev, uint64_t ino, uint32_t mode, uint32_t nlink, uint32_t uid, uint32_t gid, uint64_t size, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, uint32_t blksize, const std::string& user_id, const std::string& group_id, uint32_t truncate_epoch, uint32_t attributes ) : dev( dev ), ino( ino ), mode( mode ), nlink( nlink ), uid( uid ), gid( gid ), size( size ), atime_ns( atime_ns ), mtime_ns( mtime_ns ), ctime_ns( ctime_ns ), blksize( blksize ), user_id( user_id ), group_id( group_id ), truncate_epoch( truncate_epoch ), attributes( attributes ) { }
        Stat( uint64_t dev, uint64_t ino, uint32_t mode, uint32_t nlink, uint32_t uid, uint32_t gid, uint64_t size, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, uint32_t blksize, const char* user_id, size_t user_id_len, const char* group_id, size_t group_id_len, uint32_t truncate_epoch, uint32_t attributes ) : dev( dev ), ino( ino ), mode( mode ), nlink( nlink ), uid( uid ), gid( gid ), size( size ), atime_ns( atime_ns ), mtime_ns( mtime_ns ), ctime_ns( ctime_ns ), blksize( blksize ), user_id( user_id, user_id_len ), group_id( group_id, group_id_len ), truncate_epoch( truncate_epoch ), attributes( attributes ) { }
        virtual ~Stat() { }

        void set_dev( uint64_t dev ) { this->dev = dev; }
        uint64_t get_dev() const { return dev; }
        void set_ino( uint64_t ino ) { this->ino = ino; }
        uint64_t get_ino() const { return ino; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
        void set_nlink( uint32_t nlink ) { this->nlink = nlink; }
        uint32_t get_nlink() const { return nlink; }
        void set_uid( uint32_t uid ) { this->uid = uid; }
        uint32_t get_uid() const { return uid; }
        void set_gid( uint32_t gid ) { this->gid = gid; }
        uint32_t get_gid() const { return gid; }
        void set_size( uint64_t size ) { this->size = size; }
        uint64_t get_size() const { return size; }
        void set_atime_ns( uint64_t atime_ns ) { this->atime_ns = atime_ns; }
        uint64_t get_atime_ns() const { return atime_ns; }
        void set_mtime_ns( uint64_t mtime_ns ) { this->mtime_ns = mtime_ns; }
        uint64_t get_mtime_ns() const { return mtime_ns; }
        void set_ctime_ns( uint64_t ctime_ns ) { this->ctime_ns = ctime_ns; }
        uint64_t get_ctime_ns() const { return ctime_ns; }
        void set_blksize( uint32_t blksize ) { this->blksize = blksize; }
        uint32_t get_blksize() const { return blksize; }
        void set_user_id( const std::string& user_id ) { set_user_id( user_id.c_str(), user_id.size() ); }
        void set_user_id( const char* user_id, size_t user_id_len ) { this->user_id.assign( user_id, user_id_len ); }
        const std::string& get_user_id() const { return user_id; }
        void set_group_id( const std::string& group_id ) { set_group_id( group_id.c_str(), group_id.size() ); }
        void set_group_id( const char* group_id, size_t group_id_len ) { this->group_id.assign( group_id, group_id_len ); }
        const std::string& get_group_id() const { return group_id; }
        void set_truncate_epoch( uint32_t truncate_epoch ) { this->truncate_epoch = truncate_epoch; }
        uint32_t get_truncate_epoch() const { return truncate_epoch; }
        void set_attributes( uint32_t attributes ) { this->attributes = attributes; }
        uint32_t get_attributes() const { return attributes; }

        bool operator==( const Stat& other ) const { return dev == other.dev && ino == other.ino && mode == other.mode && nlink == other.nlink && uid == other.uid && gid == other.gid && size == other.size && atime_ns == other.atime_ns && mtime_ns == other.mtime_ns && ctime_ns == other.ctime_ns && blksize == other.blksize && user_id == other.user_id && group_id == other.group_id && truncate_epoch == other.truncate_epoch && attributes == other.attributes; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( Stat, 2010011951 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint64( "dev", 0, dev ); marshaller.writeUint64( "ino", 0, ino ); marshaller.writeUint32( "mode", 0, mode ); marshaller.writeUint32( "nlink", 0, nlink ); marshaller.writeUint32( "uid", 0, uid ); marshaller.writeUint32( "gid", 0, gid ); marshaller.writeUint64( "size", 0, size ); marshaller.writeUint64( "atime_ns", 0, atime_ns ); marshaller.writeUint64( "mtime_ns", 0, mtime_ns ); marshaller.writeUint64( "ctime_ns", 0, ctime_ns ); marshaller.writeUint32( "blksize", 0, blksize ); marshaller.writeString( "user_id", 0, user_id ); marshaller.writeString( "group_id", 0, group_id ); marshaller.writeUint32( "truncate_epoch", 0, truncate_epoch ); marshaller.writeUint32( "attributes", 0, attributes ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { dev = unmarshaller.readUint64( "dev", 0 ); ino = unmarshaller.readUint64( "ino", 0 ); mode = unmarshaller.readUint32( "mode", 0 ); nlink = unmarshaller.readUint32( "nlink", 0 ); uid = unmarshaller.readUint32( "uid", 0 ); gid = unmarshaller.readUint32( "gid", 0 ); size = unmarshaller.readUint64( "size", 0 ); atime_ns = unmarshaller.readUint64( "atime_ns", 0 ); mtime_ns = unmarshaller.readUint64( "mtime_ns", 0 ); ctime_ns = unmarshaller.readUint64( "ctime_ns", 0 ); blksize = unmarshaller.readUint32( "blksize", 0 ); unmarshaller.readString( "user_id", 0, user_id ); unmarshaller.readString( "group_id", 0, group_id ); truncate_epoch = unmarshaller.readUint32( "truncate_epoch", 0 ); attributes = unmarshaller.readUint32( "attributes", 0 ); }

      protected:
        uint64_t dev;
        uint64_t ino;
        uint32_t mode;
        uint32_t nlink;
        uint32_t uid;
        uint32_t gid;
        uint64_t size;
        uint64_t atime_ns;
        uint64_t mtime_ns;
        uint64_t ctime_ns;
        uint32_t blksize;
        std::string user_id;
        std::string group_id;
        uint32_t truncate_epoch;
        uint32_t attributes;
      };

      class DirectoryEntry : public ::yidl::runtime::Struct
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

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( DirectoryEntry, 2010011952 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "name", 0, name ); marshaller.writeStruct( "stbuf", 0, stbuf ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "name", 0, name ); unmarshaller.readStruct( "stbuf", 0, stbuf ); }

      protected:
        std::string name;
        org::xtreemfs::interfaces::Stat stbuf;
      };

      class DirectoryEntrySet : public ::yidl::runtime::Sequence, public std::vector<org::xtreemfs::interfaces::DirectoryEntry>
      {
      public:
        DirectoryEntrySet() { }
        DirectoryEntrySet( const org::xtreemfs::interfaces::DirectoryEntry& first_value ) { std::vector<org::xtreemfs::interfaces::DirectoryEntry>::push_back( first_value ); }
        DirectoryEntrySet( size_type size ) : std::vector<org::xtreemfs::interfaces::DirectoryEntry>( size ) { }
        virtual ~DirectoryEntrySet() { }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( DirectoryEntrySet, 2010011953 );

        // yidl::Sequence
        size_t get_size() const { return size(); }
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( "value", 0, ( *this )[value_i] ); } }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::DirectoryEntry value; unmarshaller.readStruct( "value", 0, value ); push_back( value ); }
      };

      class StatVFS : public ::yidl::runtime::Struct
      {
      public:
        StatVFS() : bsize( 0 ), bavail( 0 ), blocks( 0 ), namelen( 0 ) { }
        StatVFS( uint32_t bsize, uint64_t bavail, uint64_t blocks, const std::string& fsid, uint32_t namelen ) : bsize( bsize ), bavail( bavail ), blocks( blocks ), fsid( fsid ), namelen( namelen ) { }
        StatVFS( uint32_t bsize, uint64_t bavail, uint64_t blocks, const char* fsid, size_t fsid_len, uint32_t namelen ) : bsize( bsize ), bavail( bavail ), blocks( blocks ), fsid( fsid, fsid_len ), namelen( namelen ) { }
        virtual ~StatVFS() { }

        void set_bsize( uint32_t bsize ) { this->bsize = bsize; }
        uint32_t get_bsize() const { return bsize; }
        void set_bavail( uint64_t bavail ) { this->bavail = bavail; }
        uint64_t get_bavail() const { return bavail; }
        void set_blocks( uint64_t blocks ) { this->blocks = blocks; }
        uint64_t get_blocks() const { return blocks; }
        void set_fsid( const std::string& fsid ) { set_fsid( fsid.c_str(), fsid.size() ); }
        void set_fsid( const char* fsid, size_t fsid_len ) { this->fsid.assign( fsid, fsid_len ); }
        const std::string& get_fsid() const { return fsid; }
        void set_namelen( uint32_t namelen ) { this->namelen = namelen; }
        uint32_t get_namelen() const { return namelen; }

        bool operator==( const StatVFS& other ) const { return bsize == other.bsize && bavail == other.bavail && blocks == other.blocks && fsid == other.fsid && namelen == other.namelen; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( StatVFS, 2010011954 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint32( "bsize", 0, bsize ); marshaller.writeUint64( "bavail", 0, bavail ); marshaller.writeUint64( "blocks", 0, blocks ); marshaller.writeString( "fsid", 0, fsid ); marshaller.writeUint32( "namelen", 0, namelen ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { bsize = unmarshaller.readUint32( "bsize", 0 ); bavail = unmarshaller.readUint64( "bavail", 0 ); blocks = unmarshaller.readUint64( "blocks", 0 ); unmarshaller.readString( "fsid", 0, fsid ); namelen = unmarshaller.readUint32( "namelen", 0 ); }

      protected:
        uint32_t bsize;
        uint64_t bavail;
        uint64_t blocks;
        std::string fsid;
        uint32_t namelen;
      };

      class Volume : public ::yidl::runtime::Struct
      {
      public:
        Volume() : access_control_policy( ACCESS_CONTROL_POLICY_NULL ), mode( 0 ) { }
        Volume( org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, const std::string& id, uint32_t mode, const std::string& name, const std::string& owner_group_id, const std::string& owner_user_id ) : access_control_policy( access_control_policy ), default_striping_policy( default_striping_policy ), id( id ), mode( mode ), name( name ), owner_group_id( owner_group_id ), owner_user_id( owner_user_id ) { }
        Volume( org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, const char* id, size_t id_len, uint32_t mode, const char* name, size_t name_len, const char* owner_group_id, size_t owner_group_id_len, const char* owner_user_id, size_t owner_user_id_len ) : access_control_policy( access_control_policy ), default_striping_policy( default_striping_policy ), id( id, id_len ), mode( mode ), name( name, name_len ), owner_group_id( owner_group_id, owner_group_id_len ), owner_user_id( owner_user_id, owner_user_id_len ) { }
        virtual ~Volume() { }

        void set_access_control_policy( org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy ) { this->access_control_policy = access_control_policy; }
        org::xtreemfs::interfaces::AccessControlPolicyType get_access_control_policy() const { return access_control_policy; }
        void set_default_striping_policy( const org::xtreemfs::interfaces::StripingPolicy&  default_striping_policy ) { this->default_striping_policy = default_striping_policy; }
        const org::xtreemfs::interfaces::StripingPolicy& get_default_striping_policy() const { return default_striping_policy; }
        void set_id( const std::string& id ) { set_id( id.c_str(), id.size() ); }
        void set_id( const char* id, size_t id_len ) { this->id.assign( id, id_len ); }
        const std::string& get_id() const { return id; }
        void set_mode( uint32_t mode ) { this->mode = mode; }
        uint32_t get_mode() const { return mode; }
        void set_name( const std::string& name ) { set_name( name.c_str(), name.size() ); }
        void set_name( const char* name, size_t name_len ) { this->name.assign( name, name_len ); }
        const std::string& get_name() const { return name; }
        void set_owner_group_id( const std::string& owner_group_id ) { set_owner_group_id( owner_group_id.c_str(), owner_group_id.size() ); }
        void set_owner_group_id( const char* owner_group_id, size_t owner_group_id_len ) { this->owner_group_id.assign( owner_group_id, owner_group_id_len ); }
        const std::string& get_owner_group_id() const { return owner_group_id; }
        void set_owner_user_id( const std::string& owner_user_id ) { set_owner_user_id( owner_user_id.c_str(), owner_user_id.size() ); }
        void set_owner_user_id( const char* owner_user_id, size_t owner_user_id_len ) { this->owner_user_id.assign( owner_user_id, owner_user_id_len ); }
        const std::string& get_owner_user_id() const { return owner_user_id; }

        bool operator==( const Volume& other ) const { return access_control_policy == other.access_control_policy && default_striping_policy == other.default_striping_policy && id == other.id && mode == other.mode && name == other.name && owner_group_id == other.owner_group_id && owner_user_id == other.owner_user_id; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( Volume, 2010011955 );

        // yidl::Struct
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeInt32( "access_control_policy", 0, static_cast<int32_t>( access_control_policy ) ); marshaller.writeStruct( "default_striping_policy", 0, default_striping_policy ); marshaller.writeString( "id", 0, id ); marshaller.writeUint32( "mode", 0, mode ); marshaller.writeString( "name", 0, name ); marshaller.writeString( "owner_group_id", 0, owner_group_id ); marshaller.writeString( "owner_user_id", 0, owner_user_id ); }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { access_control_policy = static_cast<org::xtreemfs::interfaces::AccessControlPolicyType>( unmarshaller.readInt32( "access_control_policy", 0 ) ); unmarshaller.readStruct( "default_striping_policy", 0, default_striping_policy ); unmarshaller.readString( "id", 0, id ); mode = unmarshaller.readUint32( "mode", 0 ); unmarshaller.readString( "name", 0, name ); unmarshaller.readString( "owner_group_id", 0, owner_group_id ); unmarshaller.readString( "owner_user_id", 0, owner_user_id ); }

      protected:
        org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy;
        org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
        std::string id;
        uint32_t mode;
        std::string name;
        std::string owner_group_id;
        std::string owner_user_id;
      };

      class VolumeSet : public ::yidl::runtime::Sequence, public std::vector<org::xtreemfs::interfaces::Volume>
      {
      public:
        VolumeSet() { }
        VolumeSet( const org::xtreemfs::interfaces::Volume& first_value ) { std::vector<org::xtreemfs::interfaces::Volume>::push_back( first_value ); }
        VolumeSet( size_type size ) : std::vector<org::xtreemfs::interfaces::Volume>( size ) { }
        virtual ~VolumeSet() { }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( VolumeSet, 2010011956 );

        // yidl::Sequence
        size_t get_size() const { return size(); }
        void marshal( ::yidl::runtime::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( "value", 0, ( *this )[value_i] ); } }
        void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { org::xtreemfs::interfaces::Volume value; unmarshaller.readStruct( "value", 0, value ); push_back( value ); }
      };



      #ifndef ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS ::YIELD::concurrency::Interface
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
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS ::YIELD::concurrency::Request
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
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS ::YIELD::concurrency::Response
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
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ::YIELD::concurrency::ExceptionResponse
      #endif
      #endif



      class MRCInterface : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS
      {
      public:
        MRCInterface() { }
        virtual ~MRCInterface() { }


      const static uint32_t HTTP_PORT_DEFAULT = 30636;
      const static uint32_t ONCRPC_PORT_DEFAULT = 32636;
      const static uint32_t ONCRPCG_PORT_DEFAULT = 32636;
      const static uint32_t ONCRPCS_PORT_DEFAULT = 32636;
      const static uint32_t ONCRPCU_PORT_DEFAULT = 32636;


        virtual bool access( const std::string& path, uint32_t mode ) { return access( path, mode, static_cast<uint64_t>( -1 ) ); }
        virtual bool access( const std::string& path, uint32_t mode, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<accessRequest> __request( new accessRequest( path, mode ) ); ::YIELD::concurrency::auto_ResponseQueue<accessResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<accessResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<accessResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); bool _return_value = __response->get__return_value(); return _return_value; }

        virtual void chmod( const std::string& path, uint32_t mode ) { chmod( path, mode, static_cast<uint64_t>( -1 ) ); }
        virtual void chmod( const std::string& path, uint32_t mode, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<chmodRequest> __request( new chmodRequest( path, mode ) ); ::YIELD::concurrency::auto_ResponseQueue<chmodResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<chmodResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<chmodResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id ) { chown( path, user_id, group_id, static_cast<uint64_t>( -1 ) ); }
        virtual void chown( const std::string& path, const std::string& user_id, const std::string& group_id, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<chownRequest> __request( new chownRequest( path, user_id, group_id ) ); ::YIELD::concurrency::auto_ResponseQueue<chownResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<chownResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<chownResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void close( const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, const org::xtreemfs::interfaces::XCap& write_xcap ) { close( client_vivaldi_coordinates, write_xcap, static_cast<uint64_t>( -1 ) ); }
        virtual void close( const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, const org::xtreemfs::interfaces::XCap& write_xcap, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<closeRequest> __request( new closeRequest( client_vivaldi_coordinates, write_xcap ) ); ::YIELD::concurrency::auto_ResponseQueue<closeResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<closeResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<closeResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void creat( const std::string& path, uint32_t mode ) { creat( path, mode, static_cast<uint64_t>( -1 ) ); }
        virtual void creat( const std::string& path, uint32_t mode, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<creatRequest> __request( new creatRequest( path, mode ) ); ::YIELD::concurrency::auto_ResponseQueue<creatResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<creatResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<creatResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap ) { ftruncate( write_xcap, truncate_xcap, static_cast<uint64_t>( -1 ) ); }
        virtual void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<ftruncateRequest> __request( new ftruncateRequest( write_xcap ) ); ::YIELD::concurrency::auto_ResponseQueue<ftruncateResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<ftruncateResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<ftruncateResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); truncate_xcap = __response->get_truncate_xcap(); }

        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf ) { getattr( path, stbuf, static_cast<uint64_t>( -1 ) ); }
        virtual void getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<getattrRequest> __request( new getattrRequest( path ) ); ::YIELD::concurrency::auto_ResponseQueue<getattrResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<getattrResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<getattrResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); stbuf = __response->get_stbuf(); }

        virtual void getxattr( const std::string& path, const std::string& name, std::string& value ) { getxattr( path, name, value, static_cast<uint64_t>( -1 ) ); }
        virtual void getxattr( const std::string& path, const std::string& name, std::string& value, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<getxattrRequest> __request( new getxattrRequest( path, name ) ); ::YIELD::concurrency::auto_ResponseQueue<getxattrResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<getxattrResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<getxattrResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); value = __response->get_value(); }

        virtual void link( const std::string& target_path, const std::string& link_path ) { link( target_path, link_path, static_cast<uint64_t>( -1 ) ); }
        virtual void link( const std::string& target_path, const std::string& link_path, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<linkRequest> __request( new linkRequest( target_path, link_path ) ); ::YIELD::concurrency::auto_ResponseQueue<linkResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<linkResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<linkResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { listxattr( path, names, static_cast<uint64_t>( -1 ) ); }
        virtual void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<listxattrRequest> __request( new listxattrRequest( path ) ); ::YIELD::concurrency::auto_ResponseQueue<listxattrResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<listxattrResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<listxattrResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); names = __response->get_names(); }

        virtual void mkdir( const std::string& path, uint32_t mode ) { mkdir( path, mode, static_cast<uint64_t>( -1 ) ); }
        virtual void mkdir( const std::string& path, uint32_t mode, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<mkdirRequest> __request( new mkdirRequest( path, mode ) ); ::YIELD::concurrency::auto_ResponseQueue<mkdirResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<mkdirResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<mkdirResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, org::xtreemfs::interfaces::FileCredentials& file_credentials ) { open( path, flags, mode, attributes, client_vivaldi_coordinates, file_credentials, static_cast<uint64_t>( -1 ) ); }
        virtual void open( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, org::xtreemfs::interfaces::FileCredentials& file_credentials, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<openRequest> __request( new openRequest( path, flags, mode, attributes, client_vivaldi_coordinates ) ); ::YIELD::concurrency::auto_ResponseQueue<openResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<openResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<openResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); file_credentials = __response->get_file_credentials(); }

        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries ) { readdir( path, directory_entries, static_cast<uint64_t>( -1 ) ); }
        virtual void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<readdirRequest> __request( new readdirRequest( path ) ); ::YIELD::concurrency::auto_ResponseQueue<readdirResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<readdirResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<readdirResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); directory_entries = __response->get_directory_entries(); }

        virtual void readlink( const std::string& path, std::string& link_target_path ) { readlink( path, link_target_path, static_cast<uint64_t>( -1 ) ); }
        virtual void readlink( const std::string& path, std::string& link_target_path, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<readlinkRequest> __request( new readlinkRequest( path ) ); ::YIELD::concurrency::auto_ResponseQueue<readlinkResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<readlinkResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<readlinkResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); link_target_path = __response->get_link_target_path(); }

        virtual void removexattr( const std::string& path, const std::string& name ) { removexattr( path, name, static_cast<uint64_t>( -1 ) ); }
        virtual void removexattr( const std::string& path, const std::string& name, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<removexattrRequest> __request( new removexattrRequest( path, name ) ); ::YIELD::concurrency::auto_ResponseQueue<removexattrResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<removexattrResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<removexattrResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { rename( source_path, target_path, file_credentials, static_cast<uint64_t>( -1 ) ); }
        virtual void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<renameRequest> __request( new renameRequest( source_path, target_path ) ); ::YIELD::concurrency::auto_ResponseQueue<renameResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<renameResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<renameResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); file_credentials = __response->get_file_credentials(); }

        virtual void rmdir( const std::string& path ) { rmdir( path, static_cast<uint64_t>( -1 ) ); }
        virtual void rmdir( const std::string& path, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<rmdirRequest> __request( new rmdirRequest( path ) ); ::YIELD::concurrency::auto_ResponseQueue<rmdirResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<rmdirResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<rmdirResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf ) { setattr( path, stbuf, static_cast<uint64_t>( -1 ) ); }
        virtual void setattr( const std::string& path, const org::xtreemfs::interfaces::Stat& stbuf, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<setattrRequest> __request( new setattrRequest( path, stbuf ) ); ::YIELD::concurrency::auto_ResponseQueue<setattrResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<setattrResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<setattrResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags ) { setxattr( path, name, value, flags, static_cast<uint64_t>( -1 ) ); }
        virtual void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<setxattrRequest> __request( new setxattrRequest( path, name, value, flags ) ); ::YIELD::concurrency::auto_ResponseQueue<setxattrResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<setxattrResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<setxattrResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf ) { statvfs( volume_name, stbuf, static_cast<uint64_t>( -1 ) ); }
        virtual void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& stbuf, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<statvfsRequest> __request( new statvfsRequest( volume_name ) ); ::YIELD::concurrency::auto_ResponseQueue<statvfsResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<statvfsResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<statvfsResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); stbuf = __response->get_stbuf(); }

        virtual void symlink( const std::string& target_path, const std::string& link_path ) { symlink( target_path, link_path, static_cast<uint64_t>( -1 ) ); }
        virtual void symlink( const std::string& target_path, const std::string& link_path, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<symlinkRequest> __request( new symlinkRequest( target_path, link_path ) ); ::YIELD::concurrency::auto_ResponseQueue<symlinkResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<symlinkResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<symlinkResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials ) { unlink( path, file_credentials, static_cast<uint64_t>( -1 ) ); }
        virtual void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<unlinkRequest> __request( new unlinkRequest( path ) ); ::YIELD::concurrency::auto_ResponseQueue<unlinkResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<unlinkResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<unlinkResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); file_credentials = __response->get_file_credentials(); }

        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns ) { utimens( path, atime_ns, mtime_ns, ctime_ns, static_cast<uint64_t>( -1 ) ); }
        virtual void utimens( const std::string& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<utimensRequest> __request( new utimensRequest( path, atime_ns, mtime_ns, ctime_ns ) ); ::YIELD::concurrency::auto_ResponseQueue<utimensResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<utimensResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<utimensResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_checkpoint() { xtreemfs_checkpoint( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_checkpoint( uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_checkpointRequest> __request( new xtreemfs_checkpointRequest() ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_checkpointResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_checkpointResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, const std::string& osd_uuid, std::string& bitmap ) { xtreemfs_check_file_exists( volume_id, file_ids, osd_uuid, bitmap, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, const std::string& osd_uuid, std::string& bitmap, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_check_file_existsRequest> __request( new xtreemfs_check_file_existsRequest( volume_id, file_ids, osd_uuid ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_check_file_existsResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_check_file_existsResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_check_file_existsResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); bitmap = __response->get_bitmap(); }

        virtual void xtreemfs_dump_database( const std::string& dump_file ) { xtreemfs_dump_database( dump_file, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_dump_database( const std::string& dump_file, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_dump_databaseRequest> __request( new xtreemfs_dump_databaseRequest( dump_file ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_dump_databaseResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_dump_databaseResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_dump_databaseResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, uint32_t num_osds, org::xtreemfs::interfaces::StringSet& osd_uuids ) { xtreemfs_get_suitable_osds( file_id, num_osds, osd_uuids, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_get_suitable_osds( const std::string& file_id, uint32_t num_osds, org::xtreemfs::interfaces::StringSet& osd_uuids, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_get_suitable_osdsRequest> __request( new xtreemfs_get_suitable_osdsRequest( file_id, num_osds ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_get_suitable_osdsResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_get_suitable_osdsResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_get_suitable_osdsResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); osd_uuids = __response->get_osd_uuids(); }

        virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result ) { xtreemfs_internal_debug( operation, result, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_internal_debug( const std::string& operation, std::string& result, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_internal_debugRequest> __request( new xtreemfs_internal_debugRequest( operation ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_internal_debugResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_internal_debugResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_internal_debugResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); result = __response->get_result(); }

        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names ) { xtreemfs_listdir( path, names, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_listdirRequest> __request( new xtreemfs_listdirRequest( path ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_listdirResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_listdirResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_listdirResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); names = __response->get_names(); }

        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes ) { xtreemfs_lsvol( volumes, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_lsvolRequest> __request( new xtreemfs_lsvolRequest() ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_lsvolResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_lsvolResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_lsvolResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); volumes = __response->get_volumes(); }

        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume ) { xtreemfs_mkvol( volume, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_mkvolRequest> __request( new xtreemfs_mkvolRequest( volume ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_mkvolResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_mkvolResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_mkvolResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap ) { xtreemfs_renew_capability( old_xcap, renewed_xcap, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_renew_capabilityRequest> __request( new xtreemfs_renew_capabilityRequest( old_xcap ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_renew_capabilityResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_renew_capabilityResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_renew_capabilityResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); renewed_xcap = __response->get_renewed_xcap(); }

        virtual void xtreemfs_replication_to_master() { xtreemfs_replication_to_master( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replication_to_master( uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterRequest> __request( new xtreemfs_replication_to_masterRequest() ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_replication_to_masterResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_replication_to_masterResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica ) { xtreemfs_replica_add( file_id, new_replica, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_replica_addRequest> __request( new xtreemfs_replica_addRequest( file_id, new_replica ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_replica_addResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_replica_addResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_replica_addResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas ) { xtreemfs_replica_list( file_id, replicas, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_replica_listRequest> __request( new xtreemfs_replica_listRequest( file_id ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_replica_listResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_replica_listResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_replica_listResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); replicas = __response->get_replicas(); }

        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap ) { xtreemfs_replica_remove( file_id, osd_uuid, delete_xcap, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_replica_removeRequest> __request( new xtreemfs_replica_removeRequest( file_id, osd_uuid ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_replica_removeResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_replica_removeResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_replica_removeResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); delete_xcap = __response->get_delete_xcap(); }

        virtual void xtreemfs_restore_database( const std::string& dump_file ) { xtreemfs_restore_database( dump_file, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_restore_database( const std::string& dump_file, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_restore_databaseRequest> __request( new xtreemfs_restore_databaseRequest( dump_file ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_restore_databaseResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_restore_databaseResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_restore_databaseResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size ) { xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_restore_fileRequest> __request( new xtreemfs_restore_fileRequest( file_path, file_id, file_size, osd_uuid, stripe_size ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_restore_fileResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_restore_fileResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_restore_fileResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_rmvol( const std::string& volume_name ) { xtreemfs_rmvol( volume_name, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_rmvol( const std::string& volume_name, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_rmvolRequest> __request( new xtreemfs_rmvolRequest( volume_name ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_rmvolResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_rmvolResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_rmvolResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_shutdown() { xtreemfs_shutdown( static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_shutdown( uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_shutdownRequest> __request( new xtreemfs_shutdownRequest() ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_shutdownResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_shutdownResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }

        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response ) { xtreemfs_update_file_size( xcap, osd_write_response, static_cast<uint64_t>( -1 ) ); }
        virtual void xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<xtreemfs_update_file_sizeRequest> __request( new xtreemfs_update_file_sizeRequest( xcap, osd_write_response ) ); ::YIELD::concurrency::auto_ResponseQueue<xtreemfs_update_file_sizeResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<xtreemfs_update_file_sizeResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<xtreemfs_update_file_sizeResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }


        // Request/response pair definitions for the operations in MRCInterface

        class accessResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          accessResponse() : _return_value( false ) { }
          accessResponse( bool _return_value ) : _return_value( _return_value ) { }
          virtual ~accessResponse() { }

          void set__return_value( bool _return_value ) { this->_return_value = _return_value; }
          bool get__return_value() const { return _return_value; }

          bool operator==( const accessResponse& other ) const { return _return_value == other._return_value; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( accessResponse, 2010012112 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeBoolean( "_return_value", 0, _return_value ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { _return_value = unmarshaller.readBoolean( "_return_value", 0 ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( accessRequest, 2010012112 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeUint32( "mode", 0, mode ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); mode = unmarshaller.readUint32( "mode", 0 ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new accessResponse; }


        protected:
          std::string path;
          uint32_t mode;
        };

        class chmodResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          chmodResponse() { }
          virtual ~chmodResponse() { }

          bool operator==( const chmodResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( chmodResponse, 2010012113 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( chmodRequest, 2010012113 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeUint32( "mode", 0, mode ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); mode = unmarshaller.readUint32( "mode", 0 ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new chmodResponse; }


        protected:
          std::string path;
          uint32_t mode;
        };

        class chownResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          chownResponse() { }
          virtual ~chownResponse() { }

          bool operator==( const chownResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( chownResponse, 2010012114 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( chownRequest, 2010012114 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeString( "user_id", 0, user_id ); marshaller.writeString( "group_id", 0, group_id ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); unmarshaller.readString( "user_id", 0, user_id ); unmarshaller.readString( "group_id", 0, group_id ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new chownResponse; }


        protected:
          std::string path;
          std::string user_id;
          std::string group_id;
        };

        class closeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          closeResponse() { }
          virtual ~closeResponse() { }

          bool operator==( const closeResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( closeResponse, 2010012133 );

        };

        class closeRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          closeRequest() { }
          closeRequest( const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, const org::xtreemfs::interfaces::XCap& write_xcap ) : client_vivaldi_coordinates( client_vivaldi_coordinates ), write_xcap( write_xcap ) { }
          virtual ~closeRequest() { }

          void set_client_vivaldi_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  client_vivaldi_coordinates ) { this->client_vivaldi_coordinates = client_vivaldi_coordinates; }
          const org::xtreemfs::interfaces::VivaldiCoordinates& get_client_vivaldi_coordinates() const { return client_vivaldi_coordinates; }
          void set_write_xcap( const org::xtreemfs::interfaces::XCap&  write_xcap ) { this->write_xcap = write_xcap; }
          const org::xtreemfs::interfaces::XCap& get_write_xcap() const { return write_xcap; }

          bool operator==( const closeRequest& other ) const { return client_vivaldi_coordinates == other.client_vivaldi_coordinates && write_xcap == other.write_xcap; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( closeRequest, 2010012133 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "client_vivaldi_coordinates", 0, client_vivaldi_coordinates ); marshaller.writeStruct( "write_xcap", 0, write_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "client_vivaldi_coordinates", 0, client_vivaldi_coordinates ); unmarshaller.readStruct( "write_xcap", 0, write_xcap ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new closeResponse; }


        protected:
          org::xtreemfs::interfaces::VivaldiCoordinates client_vivaldi_coordinates;
          org::xtreemfs::interfaces::XCap write_xcap;
        };

        class creatResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          creatResponse() { }
          virtual ~creatResponse() { }

          bool operator==( const creatResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( creatResponse, 2010012115 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( creatRequest, 2010012115 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeUint32( "mode", 0, mode ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); mode = unmarshaller.readUint32( "mode", 0 ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new creatResponse; }


        protected:
          std::string path;
          uint32_t mode;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( ftruncateResponse, 2010012116 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "truncate_xcap", 0, truncate_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "truncate_xcap", 0, truncate_xcap ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( ftruncateRequest, 2010012116 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "write_xcap", 0, write_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "write_xcap", 0, write_xcap ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new ftruncateResponse; }


        protected:
          org::xtreemfs::interfaces::XCap write_xcap;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( getattrResponse, 2010012117 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "stbuf", 0, stbuf ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "stbuf", 0, stbuf ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( getattrRequest, 2010012117 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new getattrResponse; }


        protected:
          std::string path;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( getxattrResponse, 2010012118 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "value", 0, value ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "value", 0, value ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( getxattrRequest, 2010012118 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeString( "name", 0, name ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); unmarshaller.readString( "name", 0, name ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new getxattrResponse; }


        protected:
          std::string path;
          std::string name;
        };

        class linkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          linkResponse() { }
          virtual ~linkResponse() { }

          bool operator==( const linkResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( linkResponse, 2010012119 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( linkRequest, 2010012119 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "target_path", 0, target_path ); marshaller.writeString( "link_path", 0, link_path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "target_path", 0, target_path ); unmarshaller.readString( "link_path", 0, link_path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new linkResponse; }


        protected:
          std::string target_path;
          std::string link_path;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( listxattrResponse, 2010012120 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "names", 0, names ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "names", 0, names ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( listxattrRequest, 2010012120 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new listxattrResponse; }


        protected:
          std::string path;
        };

        class mkdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          mkdirResponse() { }
          virtual ~mkdirResponse() { }

          bool operator==( const mkdirResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( mkdirResponse, 2010012121 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( mkdirRequest, 2010012121 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeUint32( "mode", 0, mode ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); mode = unmarshaller.readUint32( "mode", 0 ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new mkdirResponse; }


        protected:
          std::string path;
          uint32_t mode;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( openResponse, 2010012122 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "file_credentials", 0, file_credentials ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "file_credentials", 0, file_credentials ); }

        protected:
          org::xtreemfs::interfaces::FileCredentials file_credentials;
        };

        class openRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          openRequest() : flags( 0 ), mode( 0 ), attributes( 0 ) { }
          openRequest( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates ) : path( path ), flags( flags ), mode( mode ), attributes( attributes ), client_vivaldi_coordinates( client_vivaldi_coordinates ) { }
          openRequest( const char* path, size_t path_len, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates ) : path( path, path_len ), flags( flags ), mode( mode ), attributes( attributes ), client_vivaldi_coordinates( client_vivaldi_coordinates ) { }
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
          void set_client_vivaldi_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates&  client_vivaldi_coordinates ) { this->client_vivaldi_coordinates = client_vivaldi_coordinates; }
          const org::xtreemfs::interfaces::VivaldiCoordinates& get_client_vivaldi_coordinates() const { return client_vivaldi_coordinates; }

          bool operator==( const openRequest& other ) const { return path == other.path && flags == other.flags && mode == other.mode && attributes == other.attributes && client_vivaldi_coordinates == other.client_vivaldi_coordinates; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( openRequest, 2010012122 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeUint32( "flags", 0, flags ); marshaller.writeUint32( "mode", 0, mode ); marshaller.writeUint32( "attributes", 0, attributes ); marshaller.writeStruct( "client_vivaldi_coordinates", 0, client_vivaldi_coordinates ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); flags = unmarshaller.readUint32( "flags", 0 ); mode = unmarshaller.readUint32( "mode", 0 ); attributes = unmarshaller.readUint32( "attributes", 0 ); unmarshaller.readStruct( "client_vivaldi_coordinates", 0, client_vivaldi_coordinates ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new openResponse; }


        protected:
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
          virtual ~readdirResponse() { }

          void set_directory_entries( const org::xtreemfs::interfaces::DirectoryEntrySet&  directory_entries ) { this->directory_entries = directory_entries; }
          const org::xtreemfs::interfaces::DirectoryEntrySet& get_directory_entries() const { return directory_entries; }

          bool operator==( const readdirResponse& other ) const { return directory_entries == other.directory_entries; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( readdirResponse, 2010012123 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "directory_entries", 0, directory_entries ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "directory_entries", 0, directory_entries ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( readdirRequest, 2010012123 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new readdirResponse; }


        protected:
          std::string path;
        };

        class readlinkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          readlinkResponse() { }
          readlinkResponse( const std::string& link_target_path ) : link_target_path( link_target_path ) { }
          readlinkResponse( const char* link_target_path, size_t link_target_path_len ) : link_target_path( link_target_path, link_target_path_len ) { }
          virtual ~readlinkResponse() { }

          void set_link_target_path( const std::string& link_target_path ) { set_link_target_path( link_target_path.c_str(), link_target_path.size() ); }
          void set_link_target_path( const char* link_target_path, size_t link_target_path_len ) { this->link_target_path.assign( link_target_path, link_target_path_len ); }
          const std::string& get_link_target_path() const { return link_target_path; }

          bool operator==( const readlinkResponse& other ) const { return link_target_path == other.link_target_path; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( readlinkResponse, 2010012124 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "link_target_path", 0, link_target_path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "link_target_path", 0, link_target_path ); }

        protected:
          std::string link_target_path;
        };

        class readlinkRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          readlinkRequest() { }
          readlinkRequest( const std::string& path ) : path( path ) { }
          readlinkRequest( const char* path, size_t path_len ) : path( path, path_len ) { }
          virtual ~readlinkRequest() { }

          void set_path( const std::string& path ) { set_path( path.c_str(), path.size() ); }
          void set_path( const char* path, size_t path_len ) { this->path.assign( path, path_len ); }
          const std::string& get_path() const { return path; }

          bool operator==( const readlinkRequest& other ) const { return path == other.path; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( readlinkRequest, 2010012124 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new readlinkResponse; }


        protected:
          std::string path;
        };

        class removexattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          removexattrResponse() { }
          virtual ~removexattrResponse() { }

          bool operator==( const removexattrResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( removexattrResponse, 2010012125 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( removexattrRequest, 2010012125 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeString( "name", 0, name ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); unmarshaller.readString( "name", 0, name ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new removexattrResponse; }


        protected:
          std::string path;
          std::string name;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( renameResponse, 2010012126 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "file_credentials", 0, file_credentials ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "file_credentials", 0, file_credentials ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( renameRequest, 2010012126 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "source_path", 0, source_path ); marshaller.writeString( "target_path", 0, target_path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "source_path", 0, source_path ); unmarshaller.readString( "target_path", 0, target_path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new renameResponse; }


        protected:
          std::string source_path;
          std::string target_path;
        };

        class rmdirResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          rmdirResponse() { }
          virtual ~rmdirResponse() { }

          bool operator==( const rmdirResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( rmdirResponse, 2010012127 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( rmdirRequest, 2010012127 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new rmdirResponse; }


        protected:
          std::string path;
        };

        class setattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          setattrResponse() { }
          virtual ~setattrResponse() { }

          bool operator==( const setattrResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( setattrResponse, 2010012128 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( setattrRequest, 2010012128 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeStruct( "stbuf", 0, stbuf ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); unmarshaller.readStruct( "stbuf", 0, stbuf ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new setattrResponse; }


        protected:
          std::string path;
          org::xtreemfs::interfaces::Stat stbuf;
        };

        class setxattrResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          setxattrResponse() { }
          virtual ~setxattrResponse() { }

          bool operator==( const setxattrResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( setxattrResponse, 2010012129 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( setxattrRequest, 2010012129 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeString( "name", 0, name ); marshaller.writeString( "value", 0, value ); marshaller.writeInt32( "flags", 0, flags ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); unmarshaller.readString( "name", 0, name ); unmarshaller.readString( "value", 0, value ); flags = unmarshaller.readInt32( "flags", 0 ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new setxattrResponse; }


        protected:
          std::string path;
          std::string name;
          std::string value;
          int32_t flags;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( statvfsResponse, 2010012130 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "stbuf", 0, stbuf ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "stbuf", 0, stbuf ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( statvfsRequest, 2010012130 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "volume_name", 0, volume_name ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "volume_name", 0, volume_name ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new statvfsResponse; }


        protected:
          std::string volume_name;
        };

        class symlinkResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          symlinkResponse() { }
          virtual ~symlinkResponse() { }

          bool operator==( const symlinkResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( symlinkResponse, 2010012131 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( symlinkRequest, 2010012131 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "target_path", 0, target_path ); marshaller.writeString( "link_path", 0, link_path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "target_path", 0, target_path ); unmarshaller.readString( "link_path", 0, link_path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new symlinkResponse; }


        protected:
          std::string target_path;
          std::string link_path;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( unlinkResponse, 2010012132 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "file_credentials", 0, file_credentials ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "file_credentials", 0, file_credentials ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( unlinkRequest, 2010012132 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new unlinkResponse; }


        protected:
          std::string path;
        };

        class utimensResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          utimensResponse() { }
          virtual ~utimensResponse() { }

          bool operator==( const utimensResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( utimensResponse, 2010012133 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( utimensRequest, 2010012133 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); marshaller.writeUint64( "atime_ns", 0, atime_ns ); marshaller.writeUint64( "mtime_ns", 0, mtime_ns ); marshaller.writeUint64( "ctime_ns", 0, ctime_ns ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); atime_ns = unmarshaller.readUint64( "atime_ns", 0 ); mtime_ns = unmarshaller.readUint64( "mtime_ns", 0 ); ctime_ns = unmarshaller.readUint64( "ctime_ns", 0 ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new utimensResponse; }


        protected:
          std::string path;
          uint64_t atime_ns;
          uint64_t mtime_ns;
          uint64_t ctime_ns;
        };

        class xtreemfs_checkpointResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointResponse() { }
          virtual ~xtreemfs_checkpointResponse() { }

          bool operator==( const xtreemfs_checkpointResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_checkpointResponse, 2010012141 );

        };

        class xtreemfs_checkpointRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_checkpointRequest() { }
          virtual ~xtreemfs_checkpointRequest() { }

          bool operator==( const xtreemfs_checkpointRequest& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_checkpointRequest, 2010012141 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_checkpointResponse; }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_check_file_existsResponse, 2010012142 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "bitmap", 0, bitmap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "bitmap", 0, bitmap ); }

        protected:
          std::string bitmap;
        };

        class xtreemfs_check_file_existsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_check_file_existsRequest() { }
          xtreemfs_check_file_existsRequest( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, const std::string& osd_uuid ) : volume_id( volume_id ), file_ids( file_ids ), osd_uuid( osd_uuid ) { }
          xtreemfs_check_file_existsRequest( const char* volume_id, size_t volume_id_len, const org::xtreemfs::interfaces::StringSet& file_ids, const char* osd_uuid, size_t osd_uuid_len ) : volume_id( volume_id, volume_id_len ), file_ids( file_ids ), osd_uuid( osd_uuid, osd_uuid_len ) { }
          virtual ~xtreemfs_check_file_existsRequest() { }

          void set_volume_id( const std::string& volume_id ) { set_volume_id( volume_id.c_str(), volume_id.size() ); }
          void set_volume_id( const char* volume_id, size_t volume_id_len ) { this->volume_id.assign( volume_id, volume_id_len ); }
          const std::string& get_volume_id() const { return volume_id; }
          void set_file_ids( const org::xtreemfs::interfaces::StringSet&  file_ids ) { this->file_ids = file_ids; }
          const org::xtreemfs::interfaces::StringSet& get_file_ids() const { return file_ids; }
          void set_osd_uuid( const std::string& osd_uuid ) { set_osd_uuid( osd_uuid.c_str(), osd_uuid.size() ); }
          void set_osd_uuid( const char* osd_uuid, size_t osd_uuid_len ) { this->osd_uuid.assign( osd_uuid, osd_uuid_len ); }
          const std::string& get_osd_uuid() const { return osd_uuid; }

          bool operator==( const xtreemfs_check_file_existsRequest& other ) const { return volume_id == other.volume_id && file_ids == other.file_ids && osd_uuid == other.osd_uuid; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_check_file_existsRequest, 2010012142 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "volume_id", 0, volume_id ); marshaller.writeSequence( "file_ids", 0, file_ids ); marshaller.writeString( "osd_uuid", 0, osd_uuid ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "volume_id", 0, volume_id ); unmarshaller.readSequence( "file_ids", 0, file_ids ); unmarshaller.readString( "osd_uuid", 0, osd_uuid ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_check_file_existsResponse; }


        protected:
          std::string volume_id;
          org::xtreemfs::interfaces::StringSet file_ids;
          std::string osd_uuid;
        };

        class xtreemfs_dump_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_dump_databaseResponse() { }
          virtual ~xtreemfs_dump_databaseResponse() { }

          bool operator==( const xtreemfs_dump_databaseResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_dump_databaseResponse, 2010012143 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_dump_databaseRequest, 2010012143 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "dump_file", 0, dump_file ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "dump_file", 0, dump_file ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_dump_databaseResponse; }


        protected:
          std::string dump_file;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_get_suitable_osdsResponse, 2010012144 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "osd_uuids", 0, osd_uuids ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "osd_uuids", 0, osd_uuids ); }

        protected:
          org::xtreemfs::interfaces::StringSet osd_uuids;
        };

        class xtreemfs_get_suitable_osdsRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_get_suitable_osdsRequest() : num_osds( 0 ) { }
          xtreemfs_get_suitable_osdsRequest( const std::string& file_id, uint32_t num_osds ) : file_id( file_id ), num_osds( num_osds ) { }
          xtreemfs_get_suitable_osdsRequest( const char* file_id, size_t file_id_len, uint32_t num_osds ) : file_id( file_id, file_id_len ), num_osds( num_osds ) { }
          virtual ~xtreemfs_get_suitable_osdsRequest() { }

          void set_file_id( const std::string& file_id ) { set_file_id( file_id.c_str(), file_id.size() ); }
          void set_file_id( const char* file_id, size_t file_id_len ) { this->file_id.assign( file_id, file_id_len ); }
          const std::string& get_file_id() const { return file_id; }
          void set_num_osds( uint32_t num_osds ) { this->num_osds = num_osds; }
          uint32_t get_num_osds() const { return num_osds; }

          bool operator==( const xtreemfs_get_suitable_osdsRequest& other ) const { return file_id == other.file_id && num_osds == other.num_osds; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_get_suitable_osdsRequest, 2010012144 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint32( "num_osds", 0, num_osds ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "file_id", 0, file_id ); num_osds = unmarshaller.readUint32( "num_osds", 0 ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_get_suitable_osdsResponse; }


        protected:
          std::string file_id;
          uint32_t num_osds;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_internal_debugResponse, 2010012145 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "result", 0, result ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "result", 0, result ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_internal_debugRequest, 2010012145 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "operation", 0, operation ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "operation", 0, operation ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_internal_debugResponse; }


        protected:
          std::string operation;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_listdirResponse, 2010012147 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "names", 0, names ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "names", 0, names ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_listdirRequest, 2010012147 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "path", 0, path ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "path", 0, path ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_listdirResponse; }


        protected:
          std::string path;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_lsvolResponse, 2010012146 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "volumes", 0, volumes ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "volumes", 0, volumes ); }

        protected:
          org::xtreemfs::interfaces::VolumeSet volumes;
        };

        class xtreemfs_lsvolRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_lsvolRequest() { }
          virtual ~xtreemfs_lsvolRequest() { }

          bool operator==( const xtreemfs_lsvolRequest& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_lsvolRequest, 2010012146 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_lsvolResponse; }

        };

        class xtreemfs_mkvolResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_mkvolResponse() { }
          virtual ~xtreemfs_mkvolResponse() { }

          bool operator==( const xtreemfs_mkvolResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_mkvolResponse, 2010012148 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_mkvolRequest, 2010012148 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "volume", 0, volume ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "volume", 0, volume ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_mkvolResponse; }


        protected:
          org::xtreemfs::interfaces::Volume volume;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_renew_capabilityResponse, 2010012149 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "renewed_xcap", 0, renewed_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "renewed_xcap", 0, renewed_xcap ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_renew_capabilityRequest, 2010012149 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "old_xcap", 0, old_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "old_xcap", 0, old_xcap ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_renew_capabilityResponse; }


        protected:
          org::xtreemfs::interfaces::XCap old_xcap;
        };

        class xtreemfs_replication_to_masterResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_replication_to_masterResponse() { }
          virtual ~xtreemfs_replication_to_masterResponse() { }

          bool operator==( const xtreemfs_replication_to_masterResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replication_to_masterResponse, 2010012150 );

        };

        class xtreemfs_replication_to_masterRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_replication_to_masterRequest() { }
          virtual ~xtreemfs_replication_to_masterRequest() { }

          bool operator==( const xtreemfs_replication_to_masterRequest& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replication_to_masterRequest, 2010012150 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_replication_to_masterResponse; }

        };

        class xtreemfs_replica_addResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_replica_addResponse() { }
          virtual ~xtreemfs_replica_addResponse() { }

          bool operator==( const xtreemfs_replica_addResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replica_addResponse, 2010012151 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replica_addRequest, 2010012151 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "file_id", 0, file_id ); marshaller.writeStruct( "new_replica", 0, new_replica ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "file_id", 0, file_id ); unmarshaller.readStruct( "new_replica", 0, new_replica ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_replica_addResponse; }


        protected:
          std::string file_id;
          org::xtreemfs::interfaces::Replica new_replica;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replica_listResponse, 2010012152 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeSequence( "replicas", 0, replicas ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readSequence( "replicas", 0, replicas ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replica_listRequest, 2010012152 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "file_id", 0, file_id ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "file_id", 0, file_id ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_replica_listResponse; }


        protected:
          std::string file_id;
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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replica_removeResponse, 2010012153 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "delete_xcap", 0, delete_xcap ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "delete_xcap", 0, delete_xcap ); }

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_replica_removeRequest, 2010012153 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "file_id", 0, file_id ); marshaller.writeString( "osd_uuid", 0, osd_uuid ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "file_id", 0, file_id ); unmarshaller.readString( "osd_uuid", 0, osd_uuid ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_replica_removeResponse; }


        protected:
          std::string file_id;
          std::string osd_uuid;
        };

        class xtreemfs_restore_databaseResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_restore_databaseResponse() { }
          virtual ~xtreemfs_restore_databaseResponse() { }

          bool operator==( const xtreemfs_restore_databaseResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_restore_databaseResponse, 2010012154 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_restore_databaseRequest, 2010012154 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "dump_file", 0, dump_file ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "dump_file", 0, dump_file ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_restore_databaseResponse; }


        protected:
          std::string dump_file;
        };

        class xtreemfs_restore_fileResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_restore_fileResponse() { }
          virtual ~xtreemfs_restore_fileResponse() { }

          bool operator==( const xtreemfs_restore_fileResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_restore_fileResponse, 2010012155 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_restore_fileRequest, 2010012155 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "file_path", 0, file_path ); marshaller.writeString( "file_id", 0, file_id ); marshaller.writeUint64( "file_size", 0, file_size ); marshaller.writeString( "osd_uuid", 0, osd_uuid ); marshaller.writeInt32( "stripe_size", 0, stripe_size ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "file_path", 0, file_path ); unmarshaller.readString( "file_id", 0, file_id ); file_size = unmarshaller.readUint64( "file_size", 0 ); unmarshaller.readString( "osd_uuid", 0, osd_uuid ); stripe_size = unmarshaller.readInt32( "stripe_size", 0 ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_restore_fileResponse; }


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
          virtual ~xtreemfs_rmvolResponse() { }

          bool operator==( const xtreemfs_rmvolResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_rmvolResponse, 2010012156 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_rmvolRequest, 2010012156 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "volume_name", 0, volume_name ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "volume_name", 0, volume_name ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_rmvolResponse; }


        protected:
          std::string volume_name;
        };

        class xtreemfs_shutdownResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownResponse() { }
          virtual ~xtreemfs_shutdownResponse() { }

          bool operator==( const xtreemfs_shutdownResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_shutdownResponse, 2010012157 );

        };

        class xtreemfs_shutdownRequest : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          xtreemfs_shutdownRequest() { }
          virtual ~xtreemfs_shutdownRequest() { }

          bool operator==( const xtreemfs_shutdownRequest& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_shutdownRequest, 2010012157 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_shutdownResponse; }

        };

        class xtreemfs_update_file_sizeResponse : public ORG_XTREEMFS_INTERFACES_MRCINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          xtreemfs_update_file_sizeResponse() { }
          virtual ~xtreemfs_update_file_sizeResponse() { }

          bool operator==( const xtreemfs_update_file_sizeResponse& ) const { return true; }

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_update_file_sizeResponse, 2010012158 );

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

          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( xtreemfs_update_file_sizeRequest, 2010012158 );

          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeStruct( "xcap", 0, xcap ); marshaller.writeStruct( "osd_write_response", 0, osd_write_response ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readStruct( "xcap", 0, xcap ); unmarshaller.readStruct( "osd_write_response", 0, osd_write_response ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new xtreemfs_update_file_sizeResponse; }


        protected:
          org::xtreemfs::interfaces::XCap xcap;
          org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
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

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new ConcurrentModificationException( stack_trace); }
            virtual void throwStackClone() const { throw ConcurrentModificationException( stack_trace); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "stack_trace", 0, stack_trace ); }

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

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new errnoException( error_code, error_message, stack_trace); }
            virtual void throwStackClone() const { throw errnoException( error_code, error_message, stack_trace); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { error_code = unmarshaller.readUint32( "error_code", 0 ); unmarshaller.readString( "error_message", 0, error_message ); unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint32( "error_code", 0, error_code ); marshaller.writeString( "error_message", 0, error_message ); marshaller.writeString( "stack_trace", 0, stack_trace ); }

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

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new InvalidArgumentException( error_message); }
            virtual void throwStackClone() const { throw InvalidArgumentException( error_message); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "error_message", 0, error_message ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "error_message", 0, error_message ); }

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

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new MRCException( error_code, error_message, stack_trace); }
            virtual void throwStackClone() const { throw MRCException( error_code, error_message, stack_trace); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { error_code = unmarshaller.readUint32( "error_code", 0 ); unmarshaller.readString( "error_message", 0, error_message ); unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint32( "error_code", 0, error_code ); marshaller.writeString( "error_message", 0, error_message ); marshaller.writeString( "stack_trace", 0, stack_trace ); }

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

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new ProtocolException( accept_stat, error_code, stack_trace); }
            virtual void throwStackClone() const { throw ProtocolException( accept_stat, error_code, stack_trace); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { accept_stat = unmarshaller.readUint32( "accept_stat", 0 ); error_code = unmarshaller.readUint32( "error_code", 0 ); unmarshaller.readString( "stack_trace", 0, stack_trace ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint32( "accept_stat", 0, accept_stat ); marshaller.writeUint32( "error_code", 0, error_code ); marshaller.writeString( "stack_trace", 0, stack_trace ); }

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
          RedirectException( const char* address, size_t address_len, uint16_t port ) : address( address, address_len ), port( port ) { }
            RedirectException( const char* what ) : ORG_XTREEMFS_INTERFACES_MRCINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS( what ) { }
            virtual ~RedirectException() throw() { }

          void set_address( const std::string& address ) { set_address( address.c_str(), address.size() ); }
          void set_address( const char* address, size_t address_len ) { this->address.assign( address, address_len ); }
          const std::string& get_address() const { return address; }
          void set_port( uint16_t port ) { this->port = port; }
          uint16_t get_port() const { return port; }

            // YIELD::concurrency::ExceptionResponse
            virtual ExceptionResponse* clone() const { return new RedirectException( address, port); }
            virtual void throwStackClone() const { throw RedirectException( address, port); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readString( "address", 0, address ); port = unmarshaller.readUint16( "port", 0 ); }
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeString( "address", 0, address ); marshaller.writeUint16( "port", 0, port ); }

        protected:
          std::string address;
          uint16_t port;
          };



        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( MRCInterface, 2010012111 );

        // YIELD::concurrency::EventHandler
        virtual void handleEvent( ::YIELD::concurrency::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.get_type_id() )
            {
              case 2010012112UL: handleaccessRequest( static_cast<accessRequest&>( ev ) ); return;
              case 2010012113UL: handlechmodRequest( static_cast<chmodRequest&>( ev ) ); return;
              case 2010012114UL: handlechownRequest( static_cast<chownRequest&>( ev ) ); return;
              case 2010012133UL: handlecloseRequest( static_cast<closeRequest&>( ev ) ); return;
              case 2010012115UL: handlecreatRequest( static_cast<creatRequest&>( ev ) ); return;
              case 2010012116UL: handleftruncateRequest( static_cast<ftruncateRequest&>( ev ) ); return;
              case 2010012117UL: handlegetattrRequest( static_cast<getattrRequest&>( ev ) ); return;
              case 2010012118UL: handlegetxattrRequest( static_cast<getxattrRequest&>( ev ) ); return;
              case 2010012119UL: handlelinkRequest( static_cast<linkRequest&>( ev ) ); return;
              case 2010012120UL: handlelistxattrRequest( static_cast<listxattrRequest&>( ev ) ); return;
              case 2010012121UL: handlemkdirRequest( static_cast<mkdirRequest&>( ev ) ); return;
              case 2010012122UL: handleopenRequest( static_cast<openRequest&>( ev ) ); return;
              case 2010012123UL: handlereaddirRequest( static_cast<readdirRequest&>( ev ) ); return;
              case 2010012124UL: handlereadlinkRequest( static_cast<readlinkRequest&>( ev ) ); return;
              case 2010012125UL: handleremovexattrRequest( static_cast<removexattrRequest&>( ev ) ); return;
              case 2010012126UL: handlerenameRequest( static_cast<renameRequest&>( ev ) ); return;
              case 2010012127UL: handlermdirRequest( static_cast<rmdirRequest&>( ev ) ); return;
              case 2010012128UL: handlesetattrRequest( static_cast<setattrRequest&>( ev ) ); return;
              case 2010012129UL: handlesetxattrRequest( static_cast<setxattrRequest&>( ev ) ); return;
              case 2010012130UL: handlestatvfsRequest( static_cast<statvfsRequest&>( ev ) ); return;
              case 2010012131UL: handlesymlinkRequest( static_cast<symlinkRequest&>( ev ) ); return;
              case 2010012132UL: handleunlinkRequest( static_cast<unlinkRequest&>( ev ) ); return;
              case 2010012133UL: handleutimensRequest( static_cast<utimensRequest&>( ev ) ); return;
              case 2010012141UL: handlextreemfs_checkpointRequest( static_cast<xtreemfs_checkpointRequest&>( ev ) ); return;
              case 2010012142UL: handlextreemfs_check_file_existsRequest( static_cast<xtreemfs_check_file_existsRequest&>( ev ) ); return;
              case 2010012143UL: handlextreemfs_dump_databaseRequest( static_cast<xtreemfs_dump_databaseRequest&>( ev ) ); return;
              case 2010012144UL: handlextreemfs_get_suitable_osdsRequest( static_cast<xtreemfs_get_suitable_osdsRequest&>( ev ) ); return;
              case 2010012145UL: handlextreemfs_internal_debugRequest( static_cast<xtreemfs_internal_debugRequest&>( ev ) ); return;
              case 2010012147UL: handlextreemfs_listdirRequest( static_cast<xtreemfs_listdirRequest&>( ev ) ); return;
              case 2010012146UL: handlextreemfs_lsvolRequest( static_cast<xtreemfs_lsvolRequest&>( ev ) ); return;
              case 2010012148UL: handlextreemfs_mkvolRequest( static_cast<xtreemfs_mkvolRequest&>( ev ) ); return;
              case 2010012149UL: handlextreemfs_renew_capabilityRequest( static_cast<xtreemfs_renew_capabilityRequest&>( ev ) ); return;
              case 2010012150UL: handlextreemfs_replication_to_masterRequest( static_cast<xtreemfs_replication_to_masterRequest&>( ev ) ); return;
              case 2010012151UL: handlextreemfs_replica_addRequest( static_cast<xtreemfs_replica_addRequest&>( ev ) ); return;
              case 2010012152UL: handlextreemfs_replica_listRequest( static_cast<xtreemfs_replica_listRequest&>( ev ) ); return;
              case 2010012153UL: handlextreemfs_replica_removeRequest( static_cast<xtreemfs_replica_removeRequest&>( ev ) ); return;
              case 2010012154UL: handlextreemfs_restore_databaseRequest( static_cast<xtreemfs_restore_databaseRequest&>( ev ) ); return;
              case 2010012155UL: handlextreemfs_restore_fileRequest( static_cast<xtreemfs_restore_fileRequest&>( ev ) ); return;
              case 2010012156UL: handlextreemfs_rmvolRequest( static_cast<xtreemfs_rmvolRequest&>( ev ) ); return;
              case 2010012157UL: handlextreemfs_shutdownRequest( static_cast<xtreemfs_shutdownRequest&>( ev ) ); return;
              case 2010012158UL: handlextreemfs_update_file_sizeRequest( static_cast<xtreemfs_update_file_sizeRequest&>( ev ) ); return;
              default: handleUnknownEvent( ev ); return;
            }
          }
          catch( ::YIELD::concurrency::ExceptionResponse* exception_response )
          {
            static_cast< ::YIELD::concurrency::Request& >( ev ).respond( *exception_response );
          }
          catch ( ::YIELD::concurrency::ExceptionResponse& exception_response )
          {
            static_cast< ::YIELD::concurrency::Request& >( ev ).respond( *exception_response.clone() );
          }
          catch ( ::YIELD::platform::Exception& exception )
          {
            static_cast< ::YIELD::concurrency::Request& >( ev ).respond( *( new ::YIELD::concurrency::ExceptionResponse( exception ) ) );
          }

          ::yidl::runtime::Object::decRef( ev );
        }


        // YIELD::concurrency::Interface
          virtual ::YIELD::concurrency::Request* checkRequest( Object& request )
          {
            switch ( request.get_type_id() )
            {
              case 2010012112: return static_cast<accessRequest*>( &request );
              case 2010012113: return static_cast<chmodRequest*>( &request );
              case 2010012114: return static_cast<chownRequest*>( &request );
              case 2010012133: return static_cast<closeRequest*>( &request );
              case 2010012115: return static_cast<creatRequest*>( &request );
              case 2010012116: return static_cast<ftruncateRequest*>( &request );
              case 2010012117: return static_cast<getattrRequest*>( &request );
              case 2010012118: return static_cast<getxattrRequest*>( &request );
              case 2010012119: return static_cast<linkRequest*>( &request );
              case 2010012120: return static_cast<listxattrRequest*>( &request );
              case 2010012121: return static_cast<mkdirRequest*>( &request );
              case 2010012122: return static_cast<openRequest*>( &request );
              case 2010012123: return static_cast<readdirRequest*>( &request );
              case 2010012124: return static_cast<readlinkRequest*>( &request );
              case 2010012125: return static_cast<removexattrRequest*>( &request );
              case 2010012126: return static_cast<renameRequest*>( &request );
              case 2010012127: return static_cast<rmdirRequest*>( &request );
              case 2010012128: return static_cast<setattrRequest*>( &request );
              case 2010012129: return static_cast<setxattrRequest*>( &request );
              case 2010012130: return static_cast<statvfsRequest*>( &request );
              case 2010012131: return static_cast<symlinkRequest*>( &request );
              case 2010012132: return static_cast<unlinkRequest*>( &request );
              case 2010012133: return static_cast<utimensRequest*>( &request );
              case 2010012141: return static_cast<xtreemfs_checkpointRequest*>( &request );
              case 2010012142: return static_cast<xtreemfs_check_file_existsRequest*>( &request );
              case 2010012143: return static_cast<xtreemfs_dump_databaseRequest*>( &request );
              case 2010012144: return static_cast<xtreemfs_get_suitable_osdsRequest*>( &request );
              case 2010012145: return static_cast<xtreemfs_internal_debugRequest*>( &request );
              case 2010012147: return static_cast<xtreemfs_listdirRequest*>( &request );
              case 2010012146: return static_cast<xtreemfs_lsvolRequest*>( &request );
              case 2010012148: return static_cast<xtreemfs_mkvolRequest*>( &request );
              case 2010012149: return static_cast<xtreemfs_renew_capabilityRequest*>( &request );
              case 2010012150: return static_cast<xtreemfs_replication_to_masterRequest*>( &request );
              case 2010012151: return static_cast<xtreemfs_replica_addRequest*>( &request );
              case 2010012152: return static_cast<xtreemfs_replica_listRequest*>( &request );
              case 2010012153: return static_cast<xtreemfs_replica_removeRequest*>( &request );
              case 2010012154: return static_cast<xtreemfs_restore_databaseRequest*>( &request );
              case 2010012155: return static_cast<xtreemfs_restore_fileRequest*>( &request );
              case 2010012156: return static_cast<xtreemfs_rmvolRequest*>( &request );
              case 2010012157: return static_cast<xtreemfs_shutdownRequest*>( &request );
              case 2010012158: return static_cast<xtreemfs_update_file_sizeRequest*>( &request );
              default: return NULL;
            }
          }

          virtual ::YIELD::concurrency::Response* checkResponse( Object& response )
          {
            switch ( response.get_type_id() )
            {
              case 2010012112: return static_cast<accessResponse*>( &response );
              case 2010012113: return static_cast<chmodResponse*>( &response );
              case 2010012114: return static_cast<chownResponse*>( &response );
              case 2010012133: return static_cast<closeResponse*>( &response );
              case 2010012115: return static_cast<creatResponse*>( &response );
              case 2010012116: return static_cast<ftruncateResponse*>( &response );
              case 2010012117: return static_cast<getattrResponse*>( &response );
              case 2010012118: return static_cast<getxattrResponse*>( &response );
              case 2010012119: return static_cast<linkResponse*>( &response );
              case 2010012120: return static_cast<listxattrResponse*>( &response );
              case 2010012121: return static_cast<mkdirResponse*>( &response );
              case 2010012122: return static_cast<openResponse*>( &response );
              case 2010012123: return static_cast<readdirResponse*>( &response );
              case 2010012124: return static_cast<readlinkResponse*>( &response );
              case 2010012125: return static_cast<removexattrResponse*>( &response );
              case 2010012126: return static_cast<renameResponse*>( &response );
              case 2010012127: return static_cast<rmdirResponse*>( &response );
              case 2010012128: return static_cast<setattrResponse*>( &response );
              case 2010012129: return static_cast<setxattrResponse*>( &response );
              case 2010012130: return static_cast<statvfsResponse*>( &response );
              case 2010012131: return static_cast<symlinkResponse*>( &response );
              case 2010012132: return static_cast<unlinkResponse*>( &response );
              case 2010012133: return static_cast<utimensResponse*>( &response );
              case 2010012141: return static_cast<xtreemfs_checkpointResponse*>( &response );
              case 2010012142: return static_cast<xtreemfs_check_file_existsResponse*>( &response );
              case 2010012143: return static_cast<xtreemfs_dump_databaseResponse*>( &response );
              case 2010012144: return static_cast<xtreemfs_get_suitable_osdsResponse*>( &response );
              case 2010012145: return static_cast<xtreemfs_internal_debugResponse*>( &response );
              case 2010012147: return static_cast<xtreemfs_listdirResponse*>( &response );
              case 2010012146: return static_cast<xtreemfs_lsvolResponse*>( &response );
              case 2010012148: return static_cast<xtreemfs_mkvolResponse*>( &response );
              case 2010012149: return static_cast<xtreemfs_renew_capabilityResponse*>( &response );
              case 2010012150: return static_cast<xtreemfs_replication_to_masterResponse*>( &response );
              case 2010012151: return static_cast<xtreemfs_replica_addResponse*>( &response );
              case 2010012152: return static_cast<xtreemfs_replica_listResponse*>( &response );
              case 2010012153: return static_cast<xtreemfs_replica_removeResponse*>( &response );
              case 2010012154: return static_cast<xtreemfs_restore_databaseResponse*>( &response );
              case 2010012155: return static_cast<xtreemfs_restore_fileResponse*>( &response );
              case 2010012156: return static_cast<xtreemfs_rmvolResponse*>( &response );
              case 2010012157: return static_cast<xtreemfs_shutdownResponse*>( &response );
              case 2010012158: return static_cast<xtreemfs_update_file_sizeResponse*>( &response );
              case 2010012161: return static_cast<ConcurrentModificationException*>( &response );
              case 2010012162: return static_cast<errnoException*>( &response );
              case 2010012163: return static_cast<InvalidArgumentException*>( &response );
              case 2010012164: return static_cast<MRCException*>( &response );
              case 2010012165: return static_cast<ProtocolException*>( &response );
              case 2010012166: return static_cast<RedirectException*>( &response );
              default: return NULL;
            }
          }

          virtual ::YIELD::concurrency::auto_Request createRequest( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012112: return new accessRequest;
              case 2010012113: return new chmodRequest;
              case 2010012114: return new chownRequest;
              case 2010012133: return new closeRequest;
              case 2010012115: return new creatRequest;
              case 2010012116: return new ftruncateRequest;
              case 2010012117: return new getattrRequest;
              case 2010012118: return new getxattrRequest;
              case 2010012119: return new linkRequest;
              case 2010012120: return new listxattrRequest;
              case 2010012121: return new mkdirRequest;
              case 2010012122: return new openRequest;
              case 2010012123: return new readdirRequest;
              case 2010012124: return new readlinkRequest;
              case 2010012125: return new removexattrRequest;
              case 2010012126: return new renameRequest;
              case 2010012127: return new rmdirRequest;
              case 2010012128: return new setattrRequest;
              case 2010012129: return new setxattrRequest;
              case 2010012130: return new statvfsRequest;
              case 2010012131: return new symlinkRequest;
              case 2010012132: return new unlinkRequest;
              case 2010012133: return new utimensRequest;
              case 2010012141: return new xtreemfs_checkpointRequest;
              case 2010012142: return new xtreemfs_check_file_existsRequest;
              case 2010012143: return new xtreemfs_dump_databaseRequest;
              case 2010012144: return new xtreemfs_get_suitable_osdsRequest;
              case 2010012145: return new xtreemfs_internal_debugRequest;
              case 2010012147: return new xtreemfs_listdirRequest;
              case 2010012146: return new xtreemfs_lsvolRequest;
              case 2010012148: return new xtreemfs_mkvolRequest;
              case 2010012149: return new xtreemfs_renew_capabilityRequest;
              case 2010012150: return new xtreemfs_replication_to_masterRequest;
              case 2010012151: return new xtreemfs_replica_addRequest;
              case 2010012152: return new xtreemfs_replica_listRequest;
              case 2010012153: return new xtreemfs_replica_removeRequest;
              case 2010012154: return new xtreemfs_restore_databaseRequest;
              case 2010012155: return new xtreemfs_restore_fileRequest;
              case 2010012156: return new xtreemfs_rmvolRequest;
              case 2010012157: return new xtreemfs_shutdownRequest;
              case 2010012158: return new xtreemfs_update_file_sizeRequest;
              default: return NULL;
            }
          }

          virtual ::YIELD::concurrency::auto_Response createResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012112: return new accessResponse;
              case 2010012113: return new chmodResponse;
              case 2010012114: return new chownResponse;
              case 2010012133: return new closeResponse;
              case 2010012115: return new creatResponse;
              case 2010012116: return new ftruncateResponse;
              case 2010012117: return new getattrResponse;
              case 2010012118: return new getxattrResponse;
              case 2010012119: return new linkResponse;
              case 2010012120: return new listxattrResponse;
              case 2010012121: return new mkdirResponse;
              case 2010012122: return new openResponse;
              case 2010012123: return new readdirResponse;
              case 2010012124: return new readlinkResponse;
              case 2010012125: return new removexattrResponse;
              case 2010012126: return new renameResponse;
              case 2010012127: return new rmdirResponse;
              case 2010012128: return new setattrResponse;
              case 2010012129: return new setxattrResponse;
              case 2010012130: return new statvfsResponse;
              case 2010012131: return new symlinkResponse;
              case 2010012132: return new unlinkResponse;
              case 2010012133: return new utimensResponse;
              case 2010012141: return new xtreemfs_checkpointResponse;
              case 2010012142: return new xtreemfs_check_file_existsResponse;
              case 2010012143: return new xtreemfs_dump_databaseResponse;
              case 2010012144: return new xtreemfs_get_suitable_osdsResponse;
              case 2010012145: return new xtreemfs_internal_debugResponse;
              case 2010012147: return new xtreemfs_listdirResponse;
              case 2010012146: return new xtreemfs_lsvolResponse;
              case 2010012148: return new xtreemfs_mkvolResponse;
              case 2010012149: return new xtreemfs_renew_capabilityResponse;
              case 2010012150: return new xtreemfs_replication_to_masterResponse;
              case 2010012151: return new xtreemfs_replica_addResponse;
              case 2010012152: return new xtreemfs_replica_listResponse;
              case 2010012153: return new xtreemfs_replica_removeResponse;
              case 2010012154: return new xtreemfs_restore_databaseResponse;
              case 2010012155: return new xtreemfs_restore_fileResponse;
              case 2010012156: return new xtreemfs_rmvolResponse;
              case 2010012157: return new xtreemfs_shutdownResponse;
              case 2010012158: return new xtreemfs_update_file_sizeResponse;
              default: return NULL;
            }
          }

          virtual ::YIELD::concurrency::auto_ExceptionResponse createExceptionResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012161: return new ConcurrentModificationException;
              case 2010012162: return new errnoException;
              case 2010012163: return new InvalidArgumentException;
              case 2010012164: return new MRCException;
              case 2010012165: return new ProtocolException;
              case 2010012166: return new RedirectException;
              default: return NULL;
            }
          }



      protected:
        virtual void handleaccessRequest( accessRequest& req ) { ::yidl::runtime::auto_Object<accessResponse> resp( new accessResponse ); bool _return_value = _access( req.get_path(), req.get_mode() ); resp->set__return_value( _return_value ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlechmodRequest( chmodRequest& req ) { ::yidl::runtime::auto_Object<chmodResponse> resp( new chmodResponse ); _chmod( req.get_path(), req.get_mode() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlechownRequest( chownRequest& req ) { ::yidl::runtime::auto_Object<chownResponse> resp( new chownResponse ); _chown( req.get_path(), req.get_user_id(), req.get_group_id() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlecloseRequest( closeRequest& req ) { ::yidl::runtime::auto_Object<closeResponse> resp( new closeResponse ); _close( req.get_client_vivaldi_coordinates(), req.get_write_xcap() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlecreatRequest( creatRequest& req ) { ::yidl::runtime::auto_Object<creatResponse> resp( new creatResponse ); _creat( req.get_path(), req.get_mode() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handleftruncateRequest( ftruncateRequest& req ) { ::yidl::runtime::auto_Object<ftruncateResponse> resp( new ftruncateResponse ); org::xtreemfs::interfaces::XCap truncate_xcap; _ftruncate( req.get_write_xcap(), truncate_xcap ); resp->set_truncate_xcap( truncate_xcap ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlegetattrRequest( getattrRequest& req ) { ::yidl::runtime::auto_Object<getattrResponse> resp( new getattrResponse ); org::xtreemfs::interfaces::Stat stbuf; _getattr( req.get_path(), stbuf ); resp->set_stbuf( stbuf ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlegetxattrRequest( getxattrRequest& req ) { ::yidl::runtime::auto_Object<getxattrResponse> resp( new getxattrResponse ); std::string value; _getxattr( req.get_path(), req.get_name(), value ); resp->set_value( value ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlelinkRequest( linkRequest& req ) { ::yidl::runtime::auto_Object<linkResponse> resp( new linkResponse ); _link( req.get_target_path(), req.get_link_path() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlelistxattrRequest( listxattrRequest& req ) { ::yidl::runtime::auto_Object<listxattrResponse> resp( new listxattrResponse ); org::xtreemfs::interfaces::StringSet names; _listxattr( req.get_path(), names ); resp->set_names( names ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlemkdirRequest( mkdirRequest& req ) { ::yidl::runtime::auto_Object<mkdirResponse> resp( new mkdirResponse ); _mkdir( req.get_path(), req.get_mode() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handleopenRequest( openRequest& req ) { ::yidl::runtime::auto_Object<openResponse> resp( new openResponse ); org::xtreemfs::interfaces::FileCredentials file_credentials; _open( req.get_path(), req.get_flags(), req.get_mode(), req.get_attributes(), req.get_client_vivaldi_coordinates(), file_credentials ); resp->set_file_credentials( file_credentials ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlereaddirRequest( readdirRequest& req ) { ::yidl::runtime::auto_Object<readdirResponse> resp( new readdirResponse ); org::xtreemfs::interfaces::DirectoryEntrySet directory_entries; _readdir( req.get_path(), directory_entries ); resp->set_directory_entries( directory_entries ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlereadlinkRequest( readlinkRequest& req ) { ::yidl::runtime::auto_Object<readlinkResponse> resp( new readlinkResponse ); std::string link_target_path; _readlink( req.get_path(), link_target_path ); resp->set_link_target_path( link_target_path ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handleremovexattrRequest( removexattrRequest& req ) { ::yidl::runtime::auto_Object<removexattrResponse> resp( new removexattrResponse ); _removexattr( req.get_path(), req.get_name() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlerenameRequest( renameRequest& req ) { ::yidl::runtime::auto_Object<renameResponse> resp( new renameResponse ); org::xtreemfs::interfaces::FileCredentialsSet file_credentials; _rename( req.get_source_path(), req.get_target_path(), file_credentials ); resp->set_file_credentials( file_credentials ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlermdirRequest( rmdirRequest& req ) { ::yidl::runtime::auto_Object<rmdirResponse> resp( new rmdirResponse ); _rmdir( req.get_path() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlesetattrRequest( setattrRequest& req ) { ::yidl::runtime::auto_Object<setattrResponse> resp( new setattrResponse ); _setattr( req.get_path(), req.get_stbuf() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlesetxattrRequest( setxattrRequest& req ) { ::yidl::runtime::auto_Object<setxattrResponse> resp( new setxattrResponse ); _setxattr( req.get_path(), req.get_name(), req.get_value(), req.get_flags() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlestatvfsRequest( statvfsRequest& req ) { ::yidl::runtime::auto_Object<statvfsResponse> resp( new statvfsResponse ); org::xtreemfs::interfaces::StatVFS stbuf; _statvfs( req.get_volume_name(), stbuf ); resp->set_stbuf( stbuf ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlesymlinkRequest( symlinkRequest& req ) { ::yidl::runtime::auto_Object<symlinkResponse> resp( new symlinkResponse ); _symlink( req.get_target_path(), req.get_link_path() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handleunlinkRequest( unlinkRequest& req ) { ::yidl::runtime::auto_Object<unlinkResponse> resp( new unlinkResponse ); org::xtreemfs::interfaces::FileCredentialsSet file_credentials; _unlink( req.get_path(), file_credentials ); resp->set_file_credentials( file_credentials ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handleutimensRequest( utimensRequest& req ) { ::yidl::runtime::auto_Object<utimensResponse> resp( new utimensResponse ); _utimens( req.get_path(), req.get_atime_ns(), req.get_mtime_ns(), req.get_ctime_ns() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_checkpointResponse> resp( new xtreemfs_checkpointResponse ); _xtreemfs_checkpoint(); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_check_file_existsRequest( xtreemfs_check_file_existsRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_check_file_existsResponse> resp( new xtreemfs_check_file_existsResponse ); std::string bitmap; _xtreemfs_check_file_exists( req.get_volume_id(), req.get_file_ids(), req.get_osd_uuid(), bitmap ); resp->set_bitmap( bitmap ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_dump_databaseRequest( xtreemfs_dump_databaseRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_dump_databaseResponse> resp( new xtreemfs_dump_databaseResponse ); _xtreemfs_dump_database( req.get_dump_file() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_get_suitable_osdsRequest( xtreemfs_get_suitable_osdsRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_get_suitable_osdsResponse> resp( new xtreemfs_get_suitable_osdsResponse ); org::xtreemfs::interfaces::StringSet osd_uuids; _xtreemfs_get_suitable_osds( req.get_file_id(), req.get_num_osds(), osd_uuids ); resp->set_osd_uuids( osd_uuids ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_internal_debugRequest( xtreemfs_internal_debugRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_internal_debugResponse> resp( new xtreemfs_internal_debugResponse ); std::string result; _xtreemfs_internal_debug( req.get_operation(), result ); resp->set_result( result ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_listdirRequest( xtreemfs_listdirRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_listdirResponse> resp( new xtreemfs_listdirResponse ); org::xtreemfs::interfaces::StringSet names; _xtreemfs_listdir( req.get_path(), names ); resp->set_names( names ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_lsvolRequest( xtreemfs_lsvolRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_lsvolResponse> resp( new xtreemfs_lsvolResponse ); org::xtreemfs::interfaces::VolumeSet volumes; _xtreemfs_lsvol( volumes ); resp->set_volumes( volumes ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_mkvolRequest( xtreemfs_mkvolRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_mkvolResponse> resp( new xtreemfs_mkvolResponse ); _xtreemfs_mkvol( req.get_volume() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_renew_capabilityRequest( xtreemfs_renew_capabilityRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_renew_capabilityResponse> resp( new xtreemfs_renew_capabilityResponse ); org::xtreemfs::interfaces::XCap renewed_xcap; _xtreemfs_renew_capability( req.get_old_xcap(), renewed_xcap ); resp->set_renewed_xcap( renewed_xcap ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_replication_to_masterRequest( xtreemfs_replication_to_masterRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_replication_to_masterResponse> resp( new xtreemfs_replication_to_masterResponse ); _xtreemfs_replication_to_master(); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_replica_addRequest( xtreemfs_replica_addRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_replica_addResponse> resp( new xtreemfs_replica_addResponse ); _xtreemfs_replica_add( req.get_file_id(), req.get_new_replica() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_replica_listRequest( xtreemfs_replica_listRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_replica_listResponse> resp( new xtreemfs_replica_listResponse ); org::xtreemfs::interfaces::ReplicaSet replicas; _xtreemfs_replica_list( req.get_file_id(), replicas ); resp->set_replicas( replicas ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_replica_removeRequest( xtreemfs_replica_removeRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_replica_removeResponse> resp( new xtreemfs_replica_removeResponse ); org::xtreemfs::interfaces::XCap delete_xcap; _xtreemfs_replica_remove( req.get_file_id(), req.get_osd_uuid(), delete_xcap ); resp->set_delete_xcap( delete_xcap ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_restore_databaseRequest( xtreemfs_restore_databaseRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_restore_databaseResponse> resp( new xtreemfs_restore_databaseResponse ); _xtreemfs_restore_database( req.get_dump_file() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_restore_fileRequest( xtreemfs_restore_fileRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_restore_fileResponse> resp( new xtreemfs_restore_fileResponse ); _xtreemfs_restore_file( req.get_file_path(), req.get_file_id(), req.get_file_size(), req.get_osd_uuid(), req.get_stripe_size() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_rmvolRequest( xtreemfs_rmvolRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_rmvolResponse> resp( new xtreemfs_rmvolResponse ); _xtreemfs_rmvol( req.get_volume_name() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_shutdownResponse> resp( new xtreemfs_shutdownResponse ); _xtreemfs_shutdown(); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlextreemfs_update_file_sizeRequest( xtreemfs_update_file_sizeRequest& req ) { ::yidl::runtime::auto_Object<xtreemfs_update_file_sizeResponse> resp( new xtreemfs_update_file_sizeResponse ); _xtreemfs_update_file_size( req.get_xcap(), req.get_osd_write_response() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }

      virtual bool _access( const std::string& , uint32_t ) { return false; }
        virtual void _chmod( const std::string& , uint32_t ) { }
        virtual void _chown( const std::string& , const std::string& , const std::string&  ) { }
        virtual void _close( const org::xtreemfs::interfaces::VivaldiCoordinates& , const org::xtreemfs::interfaces::XCap&  ) { }
        virtual void _creat( const std::string& , uint32_t ) { }
        virtual void _ftruncate( const org::xtreemfs::interfaces::XCap& , org::xtreemfs::interfaces::XCap&  ) { }
        virtual void _getattr( const std::string& , org::xtreemfs::interfaces::Stat&  ) { }
        virtual void _getxattr( const std::string& , const std::string& , std::string&  ) { }
        virtual void _link( const std::string& , const std::string&  ) { }
        virtual void _listxattr( const std::string& , org::xtreemfs::interfaces::StringSet&  ) { }
        virtual void _mkdir( const std::string& , uint32_t ) { }
        virtual void _open( const std::string& , uint32_t, uint32_t, uint32_t, const org::xtreemfs::interfaces::VivaldiCoordinates& , org::xtreemfs::interfaces::FileCredentials&  ) { }
        virtual void _readdir( const std::string& , org::xtreemfs::interfaces::DirectoryEntrySet&  ) { }
        virtual void _readlink( const std::string& , std::string&  ) { }
        virtual void _removexattr( const std::string& , const std::string&  ) { }
        virtual void _rename( const std::string& , const std::string& , org::xtreemfs::interfaces::FileCredentialsSet&  ) { }
        virtual void _rmdir( const std::string&  ) { }
        virtual void _setattr( const std::string& , const org::xtreemfs::interfaces::Stat&  ) { }
        virtual void _setxattr( const std::string& , const std::string& , const std::string& , int32_t ) { }
        virtual void _statvfs( const std::string& , org::xtreemfs::interfaces::StatVFS&  ) { }
        virtual void _symlink( const std::string& , const std::string&  ) { }
        virtual void _unlink( const std::string& , org::xtreemfs::interfaces::FileCredentialsSet&  ) { }
        virtual void _utimens( const std::string& , uint64_t, uint64_t, uint64_t ) { }
        virtual void _xtreemfs_checkpoint() { }
        virtual void _xtreemfs_check_file_exists( const std::string& , const org::xtreemfs::interfaces::StringSet& , const std::string& , std::string&  ) { }
        virtual void _xtreemfs_dump_database( const std::string&  ) { }
        virtual void _xtreemfs_get_suitable_osds( const std::string& , uint32_t, org::xtreemfs::interfaces::StringSet&  ) { }
        virtual void _xtreemfs_internal_debug( const std::string& , std::string&  ) { }
        virtual void _xtreemfs_listdir( const std::string& , org::xtreemfs::interfaces::StringSet&  ) { }
        virtual void _xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet&  ) { }
        virtual void _xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume&  ) { }
        virtual void _xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& , org::xtreemfs::interfaces::XCap&  ) { }
        virtual void _xtreemfs_replication_to_master() { }
        virtual void _xtreemfs_replica_add( const std::string& , const org::xtreemfs::interfaces::Replica&  ) { }
        virtual void _xtreemfs_replica_list( const std::string& , org::xtreemfs::interfaces::ReplicaSet&  ) { }
        virtual void _xtreemfs_replica_remove( const std::string& , const std::string& , org::xtreemfs::interfaces::XCap&  ) { }
        virtual void _xtreemfs_restore_database( const std::string&  ) { }
        virtual void _xtreemfs_restore_file( const std::string& , const std::string& , uint64_t, const std::string& , int32_t ) { }
        virtual void _xtreemfs_rmvol( const std::string&  ) { }
        virtual void _xtreemfs_shutdown() { }
        virtual void _xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& , const org::xtreemfs::interfaces::OSDWriteResponse&  ) { }
      };

      // Use this macro in an implementation class to get all of the prototypes for the operations in MRCInterface
      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_PROTOTYPES \
      virtual bool _access( const std::string& path, uint32_t mode );\
      virtual void _chmod( const std::string& path, uint32_t mode );\
      virtual void _chown( const std::string& path, const std::string& user_id, const std::string& group_id );\
      virtual void _close( const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, const org::xtreemfs::interfaces::XCap& write_xcap );\
      virtual void _creat( const std::string& path, uint32_t mode );\
      virtual void _ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap );\
      virtual void _getattr( const std::string& path, org::xtreemfs::interfaces::Stat& stbuf );\
      virtual void _getxattr( const std::string& path, const std::string& name, std::string& value );\
      virtual void _link( const std::string& target_path, const std::string& link_path );\
      virtual void _listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names );\
      virtual void _mkdir( const std::string& path, uint32_t mode );\
      virtual void _open( const std::string& path, uint32_t flags, uint32_t mode, uint32_t attributes, const org::xtreemfs::interfaces::VivaldiCoordinates& client_vivaldi_coordinates, org::xtreemfs::interfaces::FileCredentials& file_credentials );\
      virtual void _readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries );\
      virtual void _readlink( const std::string& path, std::string& link_target_path );\
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
      virtual void _xtreemfs_check_file_exists( const std::string& volume_id, const org::xtreemfs::interfaces::StringSet& file_ids, const std::string& osd_uuid, std::string& bitmap );\
      virtual void _xtreemfs_dump_database( const std::string& dump_file );\
      virtual void _xtreemfs_get_suitable_osds( const std::string& file_id, uint32_t num_osds, org::xtreemfs::interfaces::StringSet& osd_uuids );\
      virtual void _xtreemfs_internal_debug( const std::string& operation, std::string& result );\
      virtual void _xtreemfs_listdir( const std::string& path, org::xtreemfs::interfaces::StringSet& names );\
      virtual void _xtreemfs_lsvol( org::xtreemfs::interfaces::VolumeSet& volumes );\
      virtual void _xtreemfs_mkvol( const org::xtreemfs::interfaces::Volume& volume );\
      virtual void _xtreemfs_renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap );\
      virtual void _xtreemfs_replication_to_master();\
      virtual void _xtreemfs_replica_add( const std::string& file_id, const org::xtreemfs::interfaces::Replica& new_replica );\
      virtual void _xtreemfs_replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas );\
      virtual void _xtreemfs_replica_remove( const std::string& file_id, const std::string& osd_uuid, org::xtreemfs::interfaces::XCap& delete_xcap );\
      virtual void _xtreemfs_restore_database( const std::string& dump_file );\
      virtual void _xtreemfs_restore_file( const std::string& file_path, const std::string& file_id, uint64_t file_size, const std::string& osd_uuid, int32_t stripe_size );\
      virtual void _xtreemfs_rmvol( const std::string& volume_name );\
      virtual void _xtreemfs_shutdown();\
      virtual void _xtreemfs_update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );

      #define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handleaccessRequest( accessRequest& req );\
      virtual void handlechmodRequest( chmodRequest& req );\
      virtual void handlechownRequest( chownRequest& req );\
      virtual void handlecloseRequest( closeRequest& req );\
      virtual void handlecreatRequest( creatRequest& req );\
      virtual void handleftruncateRequest( ftruncateRequest& req );\
      virtual void handlegetattrRequest( getattrRequest& req );\
      virtual void handlegetxattrRequest( getxattrRequest& req );\
      virtual void handlelinkRequest( linkRequest& req );\
      virtual void handlelistxattrRequest( listxattrRequest& req );\
      virtual void handlemkdirRequest( mkdirRequest& req );\
      virtual void handleopenRequest( openRequest& req );\
      virtual void handlereaddirRequest( readdirRequest& req );\
      virtual void handlereadlinkRequest( readlinkRequest& req );\
      virtual void handleremovexattrRequest( removexattrRequest& req );\
      virtual void handlerenameRequest( renameRequest& req );\
      virtual void handlermdirRequest( rmdirRequest& req );\
      virtual void handlesetattrRequest( setattrRequest& req );\
      virtual void handlesetxattrRequest( setxattrRequest& req );\
      virtual void handlestatvfsRequest( statvfsRequest& req );\
      virtual void handlesymlinkRequest( symlinkRequest& req );\
      virtual void handleunlinkRequest( unlinkRequest& req );\
      virtual void handleutimensRequest( utimensRequest& req );\
      virtual void handlextreemfs_checkpointRequest( xtreemfs_checkpointRequest& req );\
      virtual void handlextreemfs_check_file_existsRequest( xtreemfs_check_file_existsRequest& req );\
      virtual void handlextreemfs_dump_databaseRequest( xtreemfs_dump_databaseRequest& req );\
      virtual void handlextreemfs_get_suitable_osdsRequest( xtreemfs_get_suitable_osdsRequest& req );\
      virtual void handlextreemfs_internal_debugRequest( xtreemfs_internal_debugRequest& req );\
      virtual void handlextreemfs_listdirRequest( xtreemfs_listdirRequest& req );\
      virtual void handlextreemfs_lsvolRequest( xtreemfs_lsvolRequest& req );\
      virtual void handlextreemfs_mkvolRequest( xtreemfs_mkvolRequest& req );\
      virtual void handlextreemfs_renew_capabilityRequest( xtreemfs_renew_capabilityRequest& req );\
      virtual void handlextreemfs_replication_to_masterRequest( xtreemfs_replication_to_masterRequest& req );\
      virtual void handlextreemfs_replica_addRequest( xtreemfs_replica_addRequest& req );\
      virtual void handlextreemfs_replica_listRequest( xtreemfs_replica_listRequest& req );\
      virtual void handlextreemfs_replica_removeRequest( xtreemfs_replica_removeRequest& req );\
      virtual void handlextreemfs_restore_databaseRequest( xtreemfs_restore_databaseRequest& req );\
      virtual void handlextreemfs_restore_fileRequest( xtreemfs_restore_fileRequest& req );\
      virtual void handlextreemfs_rmvolRequest( xtreemfs_rmvolRequest& req );\
      virtual void handlextreemfs_shutdownRequest( xtreemfs_shutdownRequest& req );\
      virtual void handlextreemfs_update_file_sizeRequest( xtreemfs_update_file_sizeRequest& req );

    };



  };



};
#endif
