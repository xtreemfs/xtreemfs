// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client.h"
#include "options.h"
using namespace org::xtreemfs::client;

#include "yield/platform.h"

#include <iostream>
#include <sstream>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_lsvolOptions : public Options
      {
      public:
        xtfs_lsvolOptions( int argc, char** argv )
          : Options( "xtfs_lsvol", "list volumes on a specified MRC", "[oncrpc[s]://]<mrc host>[:port][/<volume name>]" )
        {
          addOption( XTFS_LSVOL_OPTION_LONG_LISTING, "-l" );
          long_listing = false;

          mrc_uri = NULL;

          parseOptions( argc, argv );
        }

        ~xtfs_lsvolOptions()
        {
          delete mrc_uri;
        }

        bool get_long_listing() const { return long_listing; }
        YIELD::URI& get_mrc_uri() const { return *mrc_uri; }
        const std::string& get_volume_name() const { return volume_name; }

      private:
        enum
        {
          XTFS_LSVOL_OPTION_LONG_LISTING,
        };

        bool long_listing;
        YIELD::URI* mrc_uri;
        std::string volume_name;

        void parseOption( int id, char* arg )
        {
          switch ( id )
          {
            case XTFS_LSVOL_OPTION_LONG_LISTING: long_listing = true; break;
          }
        }


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
            throw YIELD::Exception( "must specify an MRC URI" );
        }
      };
    };
  };
};

int main( int argc, char** argv )
{
  try
  {
    xtfs_lsvolOptions options( argc, argv );

    if ( options.get_help() )
      options.printUsage();
    else
    {
      YIELD::auto_SharedObject<MRCProxy> mrc_proxy = options.createProxy<MRCProxy>( options.get_mrc_uri() );
      org::xtreemfs::interfaces::VolumeSet volumes;
      mrc_proxy.get()->lsvol( volumes );

      for ( org::xtreemfs::interfaces::VolumeSet::const_iterator volume_i = volumes.begin(); volume_i != volumes.end(); volume_i++ )
      {
        std::ostringstream volume_str;

        if ( options.get_long_listing() )
        {
          volume_str << "Volume '" << ( *volume_i ).get_name() << "'" << std::endl;
          size_t volume_str_len = volume_str.str().size();
          for ( size_t dash_i = 0; dash_i < volume_str_len; dash_i++ )
            volume_str << '-';
          volume_str << std::endl;
          volume_str << "\tID:       " << ( *volume_i ).get_id() << std::endl;
          volume_str << "\tOwner:    " << ( *volume_i ).get_owner_user_id() << std::endl;
          volume_str << "\tGroup:    " << ( *volume_i ).get_owner_group_id() << std::endl;
          volume_str << "\tAccess:   " << ( *volume_i ).get_mode() << std::endl;
          volume_str << std::endl;
        }
        else
          volume_str << ( *volume_i ).get_name() << "  ->  " << ( *volume_i ).get_id() << std::endl;

        std::cout << volume_str.str();
      }
    }

    return 0;
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error listing volumes: " << exc.what() << std::endl;

    return 1;
  }
}
