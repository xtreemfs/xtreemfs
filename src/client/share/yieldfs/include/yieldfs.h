// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).

#ifndef YIELDFS_H
#define YIELDFS_H

#include "yield/platform.h"


namespace yieldfs
{
  class CachedStat;
#ifdef _WIN32
  class FUSEWin32;
#else
  class FUSEUnix;
#endif


  class StackableFile : public YIELD::File
  {
  protected:
    StackableFile( const YIELD::Path& path, YIELD::auto_Object<YIELD::File> underlying_file, YIELD::auto_Object<YIELD::Log> log )
      : path( path ), underlying_file( underlying_file ), log( log )
    { }

    virtual ~StackableFile()
    { }


    YIELD::Path path;
    YIELD::auto_Object<YIELD::File> underlying_file;
    YIELD::auto_Object<YIELD::Log> log;
  };


  class StackableVolume : public YIELD::Volume
  {
  public:
    virtual bool access( const YIELD::Path& path, int amode ) { return underlying_volume->access( path, amode ); }
    virtual bool chmod( const YIELD::Path& path, mode_t mode ) { return underlying_volume->chmod( path, mode ); }
    virtual bool chown( const YIELD::Path& path, int32_t uid, int32_t gid ) { return underlying_volume->chown( path, uid, gid ); }
    virtual YIELD::auto_Object<YIELD::Stat> getattr( const YIELD::Path& path ) { return underlying_volume->getattr( path ); }
    virtual bool getxattr( const YIELD::Path& path, const std::string& name, std::string& out_value ) { return underlying_volume->getxattr( path, name, out_value ); }
    virtual bool link( const YIELD::Path& old_path, const YIELD::Path& new_path ) { return underlying_volume->link( old_path, new_path ); }
    virtual bool listxattr( const YIELD::Path& path, std::vector<std::string>& out_names ) { return underlying_volume->listxattr( path, out_names ); }
    virtual bool mkdir( const YIELD::Path& path, mode_t mode ) { return underlying_volume->mkdir( path, mode ); }
    virtual YIELD::auto_Object<YIELD::File> open( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes ) { return underlying_volume->open( path, flags, mode, attributes ); }
    virtual bool readdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, YIELD::Volume::readdirCallback& callback ) { return underlying_volume->readdir( path, match_file_name_prefix, callback ); }
    virtual YIELD::auto_Object<YIELD::Path> readlink( const YIELD::Path& path ) { return underlying_volume->readlink( path ); }
    virtual bool removexattr( const YIELD::Path& path, const std::string& name ) { return underlying_volume->removexattr( path, name ); }
    virtual bool rename( const YIELD::Path& from_path, const YIELD::Path& to_path ) { return underlying_volume->rename( from_path, to_path ); }
    virtual bool rmdir( const YIELD::Path& path ) { return underlying_volume->rmdir( path ); }
    virtual bool setattr( const YIELD::Path& path, uint32_t file_attributes ) { return underlying_volume->setattr( path, file_attributes ); }
    virtual bool setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int flags ) { return underlying_volume->setxattr( path, name, value, flags ); }
    virtual bool statvfs( const YIELD::Path& path, struct statvfs* stvfsbuf ) { return underlying_volume->statvfs( path, stvfsbuf ); }
    virtual bool symlink( const YIELD::Path& old_path, const YIELD::Path& new_path ) { return underlying_volume->symlink( old_path, new_path ); }
    virtual bool truncate( const YIELD::Path& path, uint64_t new_size ) { return underlying_volume->truncate( path, new_size ); }
    virtual bool unlink( const YIELD::Path& path ) { return underlying_volume->unlink( path ); }
    virtual bool utimens( const YIELD::Path& path, const YIELD::Time& atime, const YIELD::Time& mtime, const YIELD::Time& ctime ) { return underlying_volume->utimens( path, atime, mtime, ctime ); }
    virtual YIELD::Path volname( const YIELD::Path& path ) { return underlying_volume->volname( path ); }

  protected:
    StackableVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume )
      : underlying_volume( underlying_volume ), log( NULL )
    { }

    StackableVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, YIELD::auto_Object<YIELD::Log> log )
      : underlying_volume( underlying_volume ), log( log )
    { }


    YIELD::auto_Object<YIELD::Volume> underlying_volume;
    YIELD::auto_Object<YIELD::Log> log;
  };


  class FileCachingVolume : public StackableVolume
  {
  public:
    FileCachingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, YIELD::auto_Object<YIELD::Log> log = NULL )
      : StackableVolume( underlying_volume )
    { }

    // YIELD::Volume
    YIELD::auto_Object<YIELD::File> open( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes );

  private:
    virtual ~FileCachingVolume() { }
  };


  class FUSE : public YIELD::Object
  {
  public:
	  const static uint32_t FUSE_FLAG_DEBUG = 1;
	  const static uint32_t FUSE_FLAG_DIRECT_IO = 2;
	  const static uint32_t FUSE_FLAGS_DEFAULT = 0;


    FUSE( YIELD::auto_Object<YIELD::Volume> volume, uint32_t flags = FUSE_FLAGS_DEFAULT );
    FUSE( YIELD::auto_Object<YIELD::Volume> volume, YIELD::auto_Object<YIELD::Log> log, uint32_t flags = FUSE_FLAGS_DEFAULT );

#ifdef _WIN32
    int main( const char* drive_letter );
#else
    int main( char* argv0, const char* mount_point );
    int main( struct fuse_args&, const char* mount_point );
#endif

  protected:
#ifdef _WIN32
    FUSEWin32* fuse_win32;
#else
    FUSEUnix* fuse_unix;
#endif

  private:
    ~FUSE();
  };


  class StatCachingVolume : public StackableVolume, private YIELD::HATTrie<CachedStat*>
  {
  public:
    StatCachingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, double ttl_s );
    StatCachingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, YIELD::auto_Object<YIELD::Log> log, double ttl_s );

    void evict( const YIELD::Path& path );
    YIELD::auto_Object<YIELD::Stat> find( const YIELD::Path& path );
    void insert( const YIELD::Path& path, YIELD::Stat& stat );

    // YIELD::Volume
    bool chmod( const YIELD::Path& path, mode_t mode );
    bool chown( const YIELD::Path& path, int32_t uid, int32_t gid );
    YIELD::auto_Object<YIELD::Stat> getattr( const YIELD::Path& path );
    bool link( const YIELD::Path& old_path, const YIELD::Path& new_path );
    bool mkdir( const YIELD::Path& path, mode_t mode );
    YIELD::auto_Object<YIELD::File> open( const YIELD::Path& path, uint32_t flags, mode_t mode );
    bool readdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, YIELD::Volume::readdirCallback& callback );
    bool removexattr( const YIELD::Path& path, const std::string& name );
    bool rename( const YIELD::Path& from_path, const YIELD::Path& to_path );
    bool rmdir( const YIELD::Path& path );
    bool setattr( const YIELD::Path& path, uint32_t file_attributes );
    bool setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int32_t flags );
    bool symlink( const YIELD::Path& to_path, const YIELD::Path& from_path );
    bool truncate( const YIELD::Path& path, uint64_t new_size );
    bool unlink( const YIELD::Path& path );
    bool utimens( const YIELD::Path& path, const YIELD::Time& atime, const YIELD::Time& mtime, const YIELD::Time& ctime );

  private:
    friend class StatCachingVolumereaddirCallback;

    ~StatCachingVolume();


    double ttl_s;

    YIELD::Mutex lock;


    YIELD::Path getParentDirectoryPath( const YIELD::Path& );
    void insert( const YIELD::Path& path, CachedStat* cached_stat );
  };


  class TracingVolume : public StackableVolume
  {
  public:
    TracingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume ); // Log to std::cout
    TracingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, YIELD::auto_Object<YIELD::Log> log ); // Steals a reference to log

    // YIELD::Volume
    YIELD_VOLUME_PROTOTYPES;
    virtual bool exists( const YIELD::Path& path );
    virtual bool listdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, listdirCallback& callback );
    virtual bool mktree( const YIELD::Path& path, mode_t mode );
    virtual bool rmtree( const YIELD::Path& path );

  private:
    ~TracingVolume() { }
  };
};

#endif
