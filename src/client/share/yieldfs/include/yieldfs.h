// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).

#ifndef _YIELDFS_H_
#define _YIELDFS_H_

#include "yield/platform.h"

#ifndef _WIN32
struct fuse_args;
#endif


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
  public:
    const YIELD::Path& get_path() const { return path; }

    // YIELD::File
    // virtual YIELD::File methods that delegate to underlying_file
    YIELD_FILE_PROTOTYPES;

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
    // YIELD::Volume
    // virtual YIELD::Volume methods that delegate to underlying_volume
    YIELD_VOLUME_PROTOTYPES;

  protected:
    StackableVolume()
    {
      underlying_volume = new YIELD::Volume;
    }

    StackableVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume )
      : underlying_volume( underlying_volume )
    { }

    StackableVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, YIELD::auto_Object<YIELD::Log> log )
      : underlying_volume( underlying_volume ), log( log )
    { }


    YIELD::auto_Object<YIELD::Volume> underlying_volume;
    YIELD::auto_Object<YIELD::Log> log;
  };


  class DataCachingVolume : public StackableVolume
  {
  public:
    DataCachingVolume() // For testing
    { }

    DataCachingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume )
      : StackableVolume( underlying_volume )
    { }

    DataCachingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, YIELD::auto_Object<YIELD::Log> log )
      : StackableVolume( underlying_volume, log )
    { }

    // YIELD::Volume
    YIELD::auto_Object<YIELD::File> open( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes );

  private:
    virtual ~DataCachingVolume() { }
  };


  class FUSE : public YIELD::Object
  {
  public:
	  const static uint32_t FUSE_FLAG_DEBUG = 1;
	  const static uint32_t FUSE_FLAG_DIRECT_IO = 2;

    FUSE( YIELD::auto_Object<YIELD::Volume> volume, uint32_t flags = 0, YIELD::auto_Object<YIELD::Log> log = NULL );

#ifdef _WIN32
    int main( const char* drive_letter );
#else
    int main( char* argv0, const char* mount_point );
    int main( struct fuse_args&, const char* mount_point );
#endif

    // YIELD::Object
    YIELD_OBJECT_PROTOTYPES( yieldfs::FUSE, 2891744549UL );

  protected:
#ifdef _WIN32
    FUSEWin32* fuse_win32;
#else
    FUSEUnix* fuse_unix;
#endif

  private:
    ~FUSE();
  };


  class MetadataCachingVolume : public StackableVolume, private YIELD::HATTrie<CachedStat*>
  {
  public:
    MetadataCachingVolume(); // For testing
    MetadataCachingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, double ttl_s );
    MetadataCachingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, YIELD::auto_Object<YIELD::Log> log, double ttl_s );

    // YIELD::Volume
    bool chmod( const YIELD::Path& path, mode_t mode );
    bool chown( const YIELD::Path& path, int32_t uid, int32_t gid );
    YIELD::auto_Object<YIELD::Stat> getattr( const YIELD::Path& path );
    bool link( const YIELD::Path& old_path, const YIELD::Path& new_path );
    bool mkdir( const YIELD::Path& path, mode_t mode );
    YIELD::auto_Object<YIELD::File> open( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes );
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
    friend class MetadataCachingFile;
    friend class MetadataCachingVolumereaddirCallback;

    ~MetadataCachingVolume();


    double ttl_s;

    YIELD::Mutex lock;

    YIELD::auto_Object<CachedStat> evict( const YIELD::Path& path );
    YIELD::auto_Object<CachedStat> find( const YIELD::Path& path );
    YIELD::Path getParentDirectoryPath( const YIELD::Path& );
    void insert( CachedStat* cached_stat );
    void updateCachedFileSize( const YIELD::Path& path, uint64_t new_file_size );
  };


  class TracingVolume : public StackableVolume
  {
  public:
    TracingVolume(); // For testing
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
