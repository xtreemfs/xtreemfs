// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client.h"
#include "xtfs_bin.h"
using namespace org::xtreemfs::client;

#include "yield/platform.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_mkvol : public xtfs_bin
      {
      public:
        xtfs_mkvol()
          : xtfs_bin( "xtfs_mkvol", "create a new volume on a specified MRC", "[oncrpc[s]://]<mrc host>[:port]/<volume name>" )
        {
          addOption( XTFS_MKVOL_OPTION_ACCESS_CONTROL_POLICY, "-a", "--access-control-policy", "NULL|POSIX|VOLUME" );
          access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_DEFAULT;

          addOption( XTFS_MKVOL_OPTION_MODE, "-m", "--mode", "n" );
          mode = YIELD::Volume::DEFAULT_DIRECTORY_MODE;

          addOption( XTFS_MKVOL_OPTION_OSD_SELECTION_POLICY, "-o", "--osd-selection-policy", "SIMPLE" );
          osd_selection_policy = org::xtreemfs::interfaces::OSD_SELECTION_POLICY_DEFAULT;

          addOption( XTFS_MKVOL_OPTION_STRIPING_POLICY, "-p", "--striping-policy", "NONE|RAID0" );
          striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_DEFAULT;

          addOption( XTFS_MKVOL_OPTION_STRIPING_POLICY_STRIPE_SIZE, "-s", "--striping-policy-stripe-size", "n" );
          striping_policy_stripe_size = org::xtreemfs::interfaces::STRIPING_POLICY_STRIPE_SIZE_DEFAULT;

          addOption( XTFS_MKVOL_OPTION_STRIPING_POLICY_WIDTH, "-w", "--striping-policy-width", "n" );
          striping_policy_width = org::xtreemfs::interfaces::STRIPING_POLICY_WIDTH_DEFAULT;
        }

      private:
        enum
        {
          XTFS_MKVOL_OPTION_ACCESS_CONTROL_POLICY = 10,
          XTFS_MKVOL_OPTION_MODE = 11,
          XTFS_MKVOL_OPTION_OSD_SELECTION_POLICY = 12,
          XTFS_MKVOL_OPTION_STRIPING_POLICY = 13,
          XTFS_MKVOL_OPTION_STRIPING_POLICY_STRIPE_SIZE = 14,
          XTFS_MKVOL_OPTION_STRIPING_POLICY_WIDTH = 15
        };

        uint8_t access_control_policy;
        uint32_t mode;
        std::auto_ptr<YIELD::URI> mrc_uri;
        uint8_t osd_selection_policy;
        uint8_t striping_policy;
        uint32_t striping_policy_stripe_size;
        uint16_t striping_policy_width;
        std::string volume_name;

        // xtfs_bin
        int _main()
        {
          YIELD::auto_SharedObject<MRCProxy> mrc_proxy = createProxy<MRCProxy>( *mrc_uri.get() );
          mrc_proxy.get()->mkvol( org::xtreemfs::interfaces::Volume( volume_name, mode, osd_selection_policy, org::xtreemfs::interfaces::StripingPolicy( striping_policy, striping_policy_stripe_size, striping_policy_width ), access_control_policy, std::string(), std::string(), std::string() ) );
          return 0;
        }

        void parseOption( int id, char* arg )
        {
          if ( arg )
          {
            switch ( id )
            {
              case XTFS_MKVOL_OPTION_ACCESS_CONTROL_POLICY:
              {
                if ( strcmp( arg, "NULL" ) == 0 )
                  access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_NULL;
                else if ( strcmp( arg, "POSIX" ) == 0 )
                  access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_POSIX;
                else if ( strcmp( arg, "VOLUME" ) == 0 )
                  access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_VOLUME;
              }
              break;

              case XTFS_MKVOL_OPTION_MODE:
              {
                mode = strtol( arg, NULL, 0 );
                if ( mode == 0 )
                  mode = YIELD::Volume::DEFAULT_DIRECTORY_MODE;
              }
              break;

              case XTFS_MKVOL_OPTION_OSD_SELECTION_POLICY:
              {
                if ( strcmp( arg, "SIMPLE" ) == 0 )
                  osd_selection_policy = org::xtreemfs::interfaces::OSD_SELECTION_POLICY_SIMPLE;
              }
              break;

              case XTFS_MKVOL_OPTION_STRIPING_POLICY:
              {
                if ( strcmp( arg, "RAID0" ) == 0 )
                  striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_RAID0;
              }
              break;

              case XTFS_MKVOL_OPTION_STRIPING_POLICY_STRIPE_SIZE:
              {
                striping_policy_stripe_size = atoi( arg );
                if ( striping_policy_stripe_size == 0 )
                  striping_policy_stripe_size = org::xtreemfs::interfaces::STRIPING_POLICY_STRIPE_SIZE_DEFAULT;
              }
              break;

              case XTFS_MKVOL_OPTION_STRIPING_POLICY_WIDTH:
              {
                striping_policy_width = static_cast<uint16_t>( atoi( arg ) );
                if ( striping_policy_width == 0 )
                  striping_policy_width = org::xtreemfs::interfaces::STRIPING_POLICY_WIDTH_DEFAULT;
              }
              break;
            }
          }
        }

        void parseFiles( int files_count, char** files )
        {
          if ( files_count >= 1 )
          {
            mrc_uri = parseURI( files[0] );
            if ( strlen( mrc_uri.get()->get_resource() ) > 1 )
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
  return xtfs_mkvol().main( argc, argv );
}
