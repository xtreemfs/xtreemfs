#include "org/xtreemfs/client.h"
using namespace org::xtreemfs::client;

#include "dir_interface_fuzzer.h"
#include "mrc_interface_fuzzer.h"
#include "osd_interface_fuzzer.h"

#include "yield.h"

#include <string>
#include <exception>
#include <iostream>

#include "SimpleOpt.h"


enum { OPT_DEBUG, OPT_DIR, OPT_MRC, OPT_OSD };

CSimpleOpt::SOption options[] = {
  { OPT_DEBUG, "-d", SO_NONE },
  { OPT_DIR, "--dir", SO_NONE },
  { OPT_MRC, "--mrc", SO_NONE },
  { OPT_OSD, "--osd", SO_NONE },
  SO_END_OF_OPTIONS
};


int main( int argc, char** argv )
{
  // Arguments to parse
  bool debug = false, dir = false, mrc = true, osd = false, trace_socket_io = false;

  try
  {
    CSimpleOpt args( argc, argv, options );

    // - options
    while ( args.Next() )
    {
      if ( args.LastError() == SO_SUCCESS )
      {
        switch ( args.OptionId() )
        {
          case OPT_DEBUG: debug = true; break;
          case OPT_DIR: dir = true; break;
          case OPT_MRC: mrc = true; break;
          case OPT_OSD: osd = true; break;
        }
      }
    }

    // proxy_uri after - options
    if ( args.FileCount() >= 1 )
    {
      YIELD::URI proxy_uri( args.Files()[0] );
      uint32_t proxy_flags = debug ? Proxy::PROXY_FLAG_PRINT_OPERATIONS : 0;  

      if ( dir )
      {
        DIRProxy dir_proxy( proxy_uri, 3, proxy_flags );
        org::xtreemfs::interfaces::DIRInterfaceFuzzer( dir_proxy ).fuzz();
      }
      else if ( osd )
      {
        OSDProxy osd_proxy( proxy_uri, 3, proxy_flags );
        org::xtreemfs::interfaces::OSDInterfaceFuzzer( osd_proxy ).fuzz();
      }
      else 
      {
        MRCProxy mrc_proxy( proxy_uri, 3, proxy_flags );
        org::xtreemfs::interfaces::MRCInterfaceFuzzer( mrc_proxy ).fuzz();
      }

      return 0;
    }
    else
      throw YIELD::Exception( "must specify proxy URI" );
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error on fuzz: " << exc.what() << std::endl;  
    return 1;
  }  
}
