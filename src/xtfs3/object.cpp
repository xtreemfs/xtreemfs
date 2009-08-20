#include "object.h"
#include "bucket.h"
using namespace xtfs3;


Object::Object( auto_Bucket bucket, const std::string& key, YIELD::auto_Volume volume )
  : bucket( bucket ), key( key ), path( bucket->get_path() + key ), volume( volume )
{ }

void Object::delete_()
{
  volume->unlink( path );
}

yidl::auto_Buffer Object::get()
{
  return NULL;
}

void Object::marshal( yidl::Marshaller& marshaller ) const
{
  YIELD::auto_Stat stbuf( volume->stat( path ) );

  marshaller.writeString( "Key", 0, key );

  char mtime_as_iso_date_time[34];
  stbuf->get_mtime().as_iso_date_time( mtime_as_iso_date_time, 34 );
  marshaller.writeString( "LastModified", 0, mtime_as_iso_date_time );

  // ETag surrounded by &quot;

  marshaller.writeUint64( "Size", 0, stbuf->get_size() );  
  
  std::string owner_id;
  volume->getxattr( path, "xtfs3.owner.id", owner_id );  
  marshaller.writeString( "ID", 0, owner_id );
  
  std::string owner_display_name;
  volume->getxattr( path, "xtfs3.owner.display_name", owner_display_name );
  marshaller.writeString( "DisplayName", 0, owner_display_name );
}

void Object::put( yidl::auto_Buffer http_request_body )
{
}
