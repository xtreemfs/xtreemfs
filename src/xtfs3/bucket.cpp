#include "bucket.h"
#include "exceptions.h"
#include "object.h"
#include "xml_marshaller.h"
using namespace xtfs3;

#ifdef _WIN32
#include <windows.h>
#endif


namespace xtfs3
{
  class ListBucketResult : public yidl::Struct
  {
  public:
    ListBucketResult( const std::string& bucket_name, const std::string& prefix, const std::string& marker, uint32_t max_keys, bool is_truncated, const std::vector<xtfs3::Object*>& contents )
      : bucket_name( bucket_name ), prefix( prefix ), marker( marker ), max_keys( max_keys ), is_truncated( is_truncated ), contents( contents )
    { }

    ListBucketResult& operator=( const ListBucketResult& ) { return *this; }

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( ListBucketResult, 0 );

    void marshal( yidl::Marshaller& marshaller ) const
    {
      marshaller.writeString( "Name", 0, bucket_name );
      marshaller.writeString( "Prefix", 0, prefix );
      marshaller.writeString( "Marker", 0, marker );
      marshaller.writeUint32( "MaxKeys", 0, max_keys );
      marshaller.writeBoolean( "IsTruncated", 0, is_truncated );
      for ( std::vector<xtfs3::Object*>::const_iterator contents_i = contents.begin(); contents_i != contents.end(); contents_i++ )
        marshaller.writeStruct( "Contents", 0, **contents_i );
    }

  private:
    const std::string& bucket_name;
    const std::string& prefix;
    const std::string& marker;
    uint32_t max_keys;
    bool is_truncated;
    const std::vector<xtfs3::Object*>& contents;
  };    
};


Bucket::Bucket( const std::string& name, YIELD::auto_Volume volume )
  : name( name ), path( name ), volume( volume )
{ }

void Bucket::delete_()
{
  if ( volume->rmdir( path ) )
    return;
  else
    DebugBreak();
}

yidl::auto_Buffer Bucket::get( const std::string& prefix, const std::string& marker, uint32_t max_keys, const std::string& delimiter )
{
  YIELD::auto_Stat stbuf( volume->stat( path ) );
  if ( stbuf != NULL && stbuf->ISDIR() )    
  {
    bool is_truncated = false;
    std::vector<xtfs3::Object*> contents;

    if ( max_keys != 0 )
    {
      std::vector<YIELD::Path> names;
      volume->listdir( path, prefix, names );

      for ( std::vector<YIELD::Path>::const_iterator name_i = names.begin(); name_i != names.end(); name_i++ )
      {
        if ( static_cast<const std::string&>( *name_i ).find( prefix ) == 0 )
        {
          contents.push_back( new xtfs3::Object( incRef(), *name_i, volume ) );

          if ( contents.size() == max_keys )
          {
            is_truncated = true;
            break;
          }
        }
      }
    }

    XMLMarshaller xml_marshaller;
    xml_marshaller.writeStruct( "ListBucketResult", 0, ListBucketResult( name, prefix, marker, max_keys, is_truncated, contents ) );
    yidl::auto_Buffer xml_buffer( xml_marshaller.get_buffer() );

    for ( std::vector<xtfs3::Object*>::iterator contents_i = contents.begin(); contents_i != contents.end(); contents_i++ )
      yidl::Object::decRef( **contents_i );

    return xml_buffer;
  }
  else
    throw exceptions::NoSuchBucket();
}

void Bucket::put()
{
  if ( volume->mkdir( name ) )
    return;
  else
  {
    switch ( Exception::get_errno() )
    {
#ifdef _WIN32
      case ERROR_ACCESS_DENIED: throw exceptions::AccessDenied();
      case ERROR_ALREADY_EXISTS: throw exceptions::BucketAlreadyOwnedByYou();
#else
      case EACCES: throw exceptions::AccessDenied();
      case EEXIST: throw exceptions::BucketAlreadyOwnedByYou();
#endif
      default: DebugBreak();
    }
  }
}
