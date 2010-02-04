// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "nettest_proxy.h"
#include "xtreemfs/main.h"

#include <iostream>


namespace nettest_xtreemfs
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main
        ( 
          "nettest.xtreemfs", 
          "test the network connection to an XtreemFS server",
          "[oncrpc://]<host>:port"
        )
    { }

  private:
    YIELD::ipc::auto_URI uri;

    // YIELD::Main
    int _main( int, char** )
    {
      auto_NettestProxy nettest_proxy( NettestProxy::create( *uri ) );
    
      nettest_proxy->nop();

      return 0;
    }

    void parseFiles( int files_count, char** files )
    {
      if ( files_count >= 1 )
      {
        uri = parseURI( files[0] );
        if ( uri->get_port() == 0 )
          throw YIELD::platform::Exception( "must specify a port" );
      }
      else
        throw YIELD::platform::Exception( "must specify a URI" );
    }
  };
};

int main( int argc, char** argv )
{
  return nettest_xtreemfs::Main().main( argc, argv );
}
