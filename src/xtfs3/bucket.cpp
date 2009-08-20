#include "bucket.h"
using namespace xtfs3;


Bucket::Bucket( const std::string& name, YIELD::auto_Volume volume )
  : name( name ), volume( volume )
{ }

void Bucket::delete_()
{
}

yidl::auto_Buffer Bucket::get()
{
  return NULL;
}

void Bucket::put()
{
}
