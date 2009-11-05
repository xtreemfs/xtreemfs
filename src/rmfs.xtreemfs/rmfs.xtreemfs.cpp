// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/main.h"


namespace rmfs_xtreemfs
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main( "rmfs.xtreemfs", "remove a volume from a specified MRC", "[oncrpc://]<mrc host>[:port]/<volume name>" )
    {
      addOption( RMFS_XTREEMFS_OPTION_PASSWORD, "--password", NULL, "password for volume" );
    }

  private:
    enum
    {
      RMFS_XTREEMFS_OPTION_PASSWORD = 20
    };


    YIELD::ipc::auto_URI mrc_uri;
    std::string password;
    std::string volume_name;


    // YIELD::Main
    int _main( int, char** )
    {
      createMRCProxy( *mrc_uri, password.c_str() )->xtreemfs_rmvol( volume_name );
      return 0;
    }

    void parseFiles( int files_count, char** files )
    {
      if ( files_count >= 1 )
        mrc_uri = parseVolumeURI( files[0], volume_name );
      else
        throw YIELD::platform::Exception( "must specify the MRC and volume name as a URI" );
    }

    void parseOption( int id, char* arg )
    {
      if ( arg && id == RMFS_XTREEMFS_OPTION_PASSWORD )
        password = arg;
      else
        xtreemfs::Main::parseOption( id, arg );
    }
  };
};

int main( int argc, char** argv )
{
  return rmfs_xtreemfs::Main().main( argc, argv );
}
