// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).

#ifndef _YIELDFS_H_
#define _YIELDFS_H_

#include "yield/platform.h"

#ifndef _WIN32
#include <unistd.h>
#endif

#include <map>
#include <string>

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
  class PageCache;


  class StackableFile : public YIELD::platform::File
  {
  public:
    const YIELD::platform::Path& get_path() const { return path; }

    // YIELD::platform::File
    // virtual YIELD::platform::File methods that delegate to underlying_file
    YIELD_PLATFORM_FILE_PROTOTYPES;

  protected:
    StackableFile( const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log )
      : path( path ), underlying_file( underlying_file ), log( log )
    { }

    virtual ~StackableFile()
    { }


    YIELD::platform::Path path;
    YIELD::platform::auto_File underlying_file;
    YIELD::platform::auto_Log log;
  };


  class StackableVolume : public YIELD::platform::Volume
  {
  public:
    // YIELD::platform::Volume
    // virtual YIELD::platform::Volume methods that delegate to underlying_volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;

  protected:
    StackableVolume()
    {
      underlying_volume = new YIELD::platform::Volume;
    }

    StackableVolume( YIELD::platform::auto_Volume underlying_volume )
      : underlying_volume( underlying_volume )
    { }

    StackableVolume( YIELD::platform::auto_Volume underlying_volume, YIELD::platform::auto_Log log )
      : underlying_volume( underlying_volume ), log( log )
    { }


    YIELD::platform::auto_Volume underlying_volume;
    YIELD::platform::auto_Log log;
  };


  class FUSE
  {
  public:
    const static uint32_t FUSE_FLAG_DEBUG = 1;
    const static uint32_t FUSE_FLAG_DIRECT_IO = 2;

    FUSE( YIELD::platform::auto_Volume volume, uint32_t flags = 0 );
    ~FUSE();

    static uint32_t getpid();
#ifdef _WIN32
    int main( const char* drive_letter );
#else
    static uid_t geteuid();
    static gid_t getegid();
    int main( char* argv0, const char* mount_point );
    int main( struct fuse_args&, const char* mount_point );
#endif

  protected:
#ifdef _WIN32
    FUSEWin32* fuse_win32;
#else
    FUSEUnix* fuse_unix;
    static bool is_running;
#endif
  };


  class MetadataCachingVolume : public StackableVolume, private std::map<std::string, CachedStat*> // YIELD::HATTrie<CachedStat*>
  {
  public:
    MetadataCachingVolume(); // For testing
    MetadataCachingVolume( YIELD::platform::auto_Volume underlying_volume, double ttl_s );
    MetadataCachingVolume( YIELD::platform::auto_Volume underlying_volume, YIELD::platform::auto_Log log, double ttl_s );

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( MetadataCachingVolume, 0 );

    // YIELD::platform::Volume
    bool chmod( const YIELD::platform::Path& path, mode_t mode );
    bool chown( const YIELD::platform::Path& path, int32_t uid, int32_t gid );
    bool link( const YIELD::platform::Path& old_path, const YIELD::platform::Path& new_path );
    bool mkdir( const YIELD::platform::Path& path, mode_t mode );
    YIELD::platform::auto_File open( const YIELD::platform::Path& path, uint32_t flags, mode_t mode, uint32_t attributes );
    bool readdir( const YIELD::platform::Path& path, const YIELD::platform::Path& match_file_name_prefix, YIELD::platform::Volume::readdirCallback& callback );
    bool removexattr( const YIELD::platform::Path& path, const std::string& name );
    bool rename( const YIELD::platform::Path& from_path, const YIELD::platform::Path& to_path );
    bool rmdir( const YIELD::platform::Path& path );
    bool setattr( const YIELD::platform::Path& path, uint32_t file_attributes );
    bool setxattr( const YIELD::platform::Path& path, const std::string& name, const std::string& value, int32_t flags );
    YIELD::platform::auto_Stat stat( const YIELD::platform::Path& path );
    bool symlink( const YIELD::platform::Path& to_path, const YIELD::platform::Path& from_path );
    bool truncate( const YIELD::platform::Path& path, uint64_t new_size );
    bool unlink( const YIELD::platform::Path& path );
    bool utimens( const YIELD::platform::Path& path, const YIELD::platform::Time& atime, const YIELD::platform::Time& mtime, const YIELD::platform::Time& ctime );

  private:
    friend class MetadataCachingFile;
    friend class MetadataCachingVolumereaddirCallback;

    ~MetadataCachingVolume();


    double ttl_s;

    YIELD::platform::Mutex lock;

    yidl::runtime::auto_Object<CachedStat> evict( const YIELD::platform::Path& path );
    yidl::runtime::auto_Object<CachedStat> find( const YIELD::platform::Path& path );
    YIELD::platform::Path getParentDirectoryPath( const YIELD::platform::Path& );
    void insert( CachedStat* cached_stat );
    void updateCachedFileSize( const YIELD::platform::Path& path, uint64_t new_file_size );
  };


  class TracingVolume : public StackableVolume
  {
  public:
    TracingVolume(); // For testing
    TracingVolume( YIELD::platform::auto_Volume underlying_volume ); // Log to std::cout
    TracingVolume( YIELD::platform::auto_Volume underlying_volume, YIELD::platform::auto_Log log ); // Steals a reference to log

    // YIELD::platform::Volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;
    virtual bool exists( const YIELD::platform::Path& path );
    virtual bool listdir( const YIELD::platform::Path& path, const YIELD::platform::Path& match_file_name_prefix, listdirCallback& callback );
    virtual bool mktree( const YIELD::platform::Path& path, mode_t mode );
    virtual bool rmtree( const YIELD::platform::Path& path );

  private:
    friend class TracingFile;

    ~TracingVolume() { }

    static bool trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& path, bool operation_result );
    static bool trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& path, mode_t mode, bool operation_result );
    static bool trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& old_path, const YIELD::platform::Path& new_path, bool operation_result );
    static bool trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& path, const std::string& xattr_name, bool operation_result );
    static bool trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& path, uint64_t size, uint64_t offset, bool operation_result );
    static bool trace( YIELD::platform::Log::Stream& log_stream, bool operation_result );
  };


  class WriteBackCachingVolume : public StackableVolume
  {
  public:
    WriteBackCachingVolume(); // For testing
    WriteBackCachingVolume( size_t cache_capacity_bytes, uint32_t cache_flush_timeout_ms, YIELD::platform::auto_Volume underlying_volume );
    WriteBackCachingVolume( size_t cache_capacity_bytes, uint32_t cache_flush_timeout_ms, YIELD::platform::auto_Volume underlying_volume, YIELD::platform::auto_Log log );

    // YIELD::platform::Volume
    YIELD::platform::auto_File open( const YIELD::platform::Path& path, uint32_t flags, mode_t mode, uint32_t attributes );

  private:
    virtual ~WriteBackCachingVolume();

    PageCache* page_cache;
  };


  class WriteThroughCachingVolume : public StackableVolume
  {
  public:
    WriteThroughCachingVolume(); // For testing
    WriteThroughCachingVolume( YIELD::platform::auto_Volume underlying_volume );
    WriteThroughCachingVolume( YIELD::platform::auto_Volume underlying_volume, YIELD::platform::auto_Log log );

    // YIELD::platform::Volume
    YIELD::platform::auto_File open( const YIELD::platform::Path& path, uint32_t flags, mode_t mode, uint32_t attributes );

  private:
    virtual ~WriteThroughCachingVolume();

    PageCache* page_cache;
  };
};

#endif
