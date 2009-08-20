#include "object.h"
using namespace xtfs3;


Object::Object( const std::string& key, YIELD::auto_Volume volume )
  : key( key ), volume( volume )
{ }

void Object::delete_()
{
}

yidl::auto_Buffer Object::get()
{
  return NULL;
}

void Object::put( yidl::auto_Buffer http_request_body )
{
}
