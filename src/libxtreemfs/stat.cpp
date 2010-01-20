#include "stat.h"
using namespace xtreemfs;


Stat::Stat( const org::xtreemfs::interfaces::Stat& xtreemfs_stat )
: YIELD::platform::Stat  
  (
#ifdef _WIN32
    xtreemfs_stat.get_mode(), 
    xtreemfs_stat.get_size(), 
    xtreemfs_stat.get_atime_ns(), 
    xtreemfs_stat.get_mtime_ns(), 
    xtreemfs_stat.get_ctime_ns(), 
    xtreemfs_stat.get_attributes() 
#else
    xtreemfs_stat.get_dev(), 
    xtreemfs_stat.get_ino(), 
    xtreemfs_stat.get_mode(), 
    xtreemfs_stat.get_nlink(),
    0, // uid
    0, // gid
    0, // rdev
    xtreemfs_stat.get_size(), 
    xtreemfs_stat.get_atime_ns(), 
    xtreemfs_stat.get_mtime_ns(), 
    xtreemfs_stat.get_ctime_ns(),
    xtreemfs_stat.get_blksize(),
    0 // blocks
#endif
  )
{ }

mode_t Stat::get_mode() const 
{ 
  return mode; 
}

#ifndef _WIN32
nlink_t Stat::get_nlink() const 
{ 
  return nlink; 
}

uid_t Stat::get_uid() const 
{ 
  return uid; 
}

gid_t Stat::get_gid() const 
{ 
  return gid; 
}
#endif

uint64_t Stat::get_size() const 
{ 
  return size; 
}

const YIELD::platform::Time& Stat::get_atime() const 
{ 
  return atime; 
}

const YIELD::platform::Time& Stat::get_mtime() const 
{ 
  return mtime; 
}

const YIELD::platform::Time& Stat::get_ctime() const 
{ 
  return ctime; 
}

#ifdef _WIN32
uint32_t Stat::get_attributes() const
{
  return YIELD::platform::Stat::get_attributes();
}
#else
blksize_t Stat::get_blksize() const 
{ 
  return blksize; 
}

blkcnt_t Stat::get_blocks() const 
{ 
  return blocks; 
}
#endif

void Stat::set_mode( mode_t mode )
{
  this->mode = mode;
}

#ifndef _WIN32
void Stat::set_nlink( nlink_t nlink )
{
  this->nlink = nlink;
}

void Stat::set_uid( uid_t uid )
{
  this->uid = uid;
}

void Stat::set_gid( gid_t gid )
{
  this->gid = gid;
}
#endif

void Stat::set_size( uint64_t size )
{
  this->size = size;
}

void Stat::set_atime( const YIELD::platform::Time& atime )
{
  this->atime = atime;
}

void Stat::set_mtime( const YIELD::platform::Time& mtime )
{
  this->mtime = mtime;
}

void Stat::set_ctime( const YIELD::platform::Time& ctime )
{
  this->ctime = ctime;
}

#ifdef _WIN32
void Stat::set_attributes( uint32_t attributes )
{
  this->attributes = attributes;
}
#else
void Stat::set_blksize( blksize_t blksize )
{
  this->blksize = blksize;
}

void Stat::set_blocks( blkcnt_t blocks )
{
  this->blocks = blocks;
}
#endif
