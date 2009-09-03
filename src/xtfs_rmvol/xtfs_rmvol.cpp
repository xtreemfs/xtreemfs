// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/main.h"


namespace xtfs_rmvol
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main( "xtfs_rmvol", "remove a volume from a specified MRC", "[oncrpc[s]://]<mrc host>[:port]/<volume name>" )
    {
      addOption( XTFS_RMVOL_OPTION_PASSWORD, "--password", NULL, "password for volume" );
    }

  private:
    enum
    {
      XTFS_RMVOL_OPTION_PASSWORD = 20
    };


    YIELD::auto_URI mrc_uri;
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
        throw YIELD::Exception( "must specify the MRC and volume name as a URI" );
    }

    void parseOption( int id, char* arg )
    {
      if ( arg && id == XTFS_RMVOL_OPTION_PASSWORD )
        password = arg;
      else
        xtreemfs::Main::parseOption( id, arg );
    }
  };
};

int main( int argc, char** argv )
{
  return xtfs_rmvol::Main().main( argc, argv );
}
