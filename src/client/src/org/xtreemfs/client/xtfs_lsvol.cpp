// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "main.h"
using namespace org::xtreemfs::client;

#include <iostream>
#include <sstream>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_lsvol : public Main
      {
      public:
        xtfs_lsvol()
          : Main( "xtfs_lsvol", "list volumes on a specified MRC", "[oncrpc[s]://]<mrc host>[:port][/<volume name>]" )
        {
          addOption( XTFS_LSVOL_OPTION_LONG_LISTING, "-l" );
          long_listing = false;
        }

      private:
        enum
        {
          XTFS_LSVOL_OPTION_LONG_LISTING = 20
        };

        bool long_listing;
        YIELD::auto_URI mrc_uri;
        std::string volume_name;

        // YIELD::Main
        int _main( int, char** )
        {
          YIELD::auto_Object<MRCProxy> mrc_proxy = createMRCProxy( *mrc_uri );
          org::xtreemfs::interfaces::VolumeSet volumes;
          mrc_proxy->xtreemfs_lsvol( volumes );

          for ( org::xtreemfs::interfaces::VolumeSet::const_iterator volume_i = volumes.begin(); volume_i != volumes.end(); volume_i++ )
          {
            std::ostringstream volume_str;

            if ( long_listing )
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

          return 0;
        }

        void parseOption( int id, char* arg )
        {
          switch ( id )
          {
            case XTFS_LSVOL_OPTION_LONG_LISTING: long_listing = true; break;
            default: Main::parseOption( id, arg ); break;
          }
        }

        void parseFiles( int files_count, char** files )
        {
          if ( files_count >= 1 )
          {
            mrc_uri = parseURI( files[0] );
            if ( mrc_uri->get_resource().size() > 1 )
              volume_name = mrc_uri->get_resource().c_str() + 1;
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
  return xtfs_lsvol().main( argc, argv );
}
