// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client.h"
#include "options.h"
using namespace org::xtreemfs::client;

#include "yield/platform.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_rmvolOptions : public Options
      {
      public:
        xtfs_rmvolOptions( int argc, char** argv )
          : Options( "xtfs_rmvol", "remove a volume from a specified MRC", "[oncrpc[s]://]<mrc host>[:port]/<volume name>" )
        {
          mrc_uri = NULL;

          parseOptions( argc, argv );
        }

        ~xtfs_rmvolOptions()
        {
          delete mrc_uri;
        }

        YIELD::URI& get_mrc_uri() const { return *mrc_uri; }
        const std::string& get_volume_name() const { return volume_name; }

      private: 
        YIELD::URI* mrc_uri;
        std::string volume_name;

        // OptionParser
        void parseFiles( int files_count, char** files )
        {
          if ( files_count >= 1 )
          {
            mrc_uri = parseURI( files[0] );
            if ( strlen( mrc_uri->get_resource() ) > 1 )
              volume_name = mrc_uri->get_resource() + 1;
          }
          else
            throw YIELD::Exception( "must specify the MRC and volume name as a URI" );
        }
      };
    };
  };
};


int main( int argc, char** argv )
{
  try
  {
    xtfs_rmvolOptions options( argc, argv );

    if ( options.get_help() )
      options.printUsage();
    else
    {
      YIELD::auto_SharedObject<MRCProxy> mrc_proxy = options.createProxy<MRCProxy>( options.get_mrc_uri() );
      mrc_proxy.get()->rmvol( options.get_volume_name() );
    }

    return 0;
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error removing volume: " << exc.what() << std::endl;

    return 1;
  }
}
