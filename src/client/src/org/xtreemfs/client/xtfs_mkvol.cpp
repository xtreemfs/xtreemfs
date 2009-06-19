// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "main.h"
using namespace org::xtreemfs::client;


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_mkvol : public Main
      {
      public:
        xtfs_mkvol()
          : Main( "xtfs_mkvol", "create a new volume on a specified MRC", "[oncrpc[s]://]<mrc host>[:port]/<volume name>" )
        {
          addOption( XTFS_MKVOL_OPTION_ACCESS_CONTROL_POLICY, "-a", "--access-control-policy", "NULL|POSIX|VOLUME" );
          access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_POSIX;

          addOption( XTFS_MKVOL_OPTION_MODE, "-m", "--mode", "n" );
          mode = YIELD::Volume::DEFAULT_DIRECTORY_MODE;

          addOption( XTFS_MKVOL_OPTION_OWNER_GROUP_ID, "-g", "--owner-group-id", "group id of owner" );

          addOption( XTFS_MKVOL_OPTION_OSD_SELECTION_POLICY, "-o", "--osd-selection-policy", "SIMPLE" );
          osd_selection_policy = org::xtreemfs::interfaces::OSD_SELECTION_POLICY_SIMPLE;

          addOption( XTFS_MKVOL_OPTION_STRIPING_POLICY, "-p", "--striping-policy", "NONE|RAID0" );
          striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_RAID0;

          addOption( XTFS_MKVOL_OPTION_STRIPING_POLICY_STRIPE_SIZE, "-s", "--striping-policy-stripe-size", "n" );
          striping_policy_stripe_size = 128;

          addOption( XTFS_MKVOL_OPTION_OWNER_USER_ID, "-u", "--owner-user-id", "user id of owner" );

          addOption( XTFS_MKVOL_OPTION_STRIPING_POLICY_WIDTH, "-w", "--striping-policy-width", "n" );
          striping_policy_width = 1;
        }

      private:
        enum
        {
          XTFS_MKVOL_OPTION_ACCESS_CONTROL_POLICY = 10,
          XTFS_MKVOL_OPTION_MODE = 11,
          XTFS_MKVOL_OPTION_OWNER_GROUP_ID = 16,
          XTFS_MKVOL_OPTION_OWNER_USER_ID = 17,
          XTFS_MKVOL_OPTION_OSD_SELECTION_POLICY = 12,
          XTFS_MKVOL_OPTION_STRIPING_POLICY = 13,
          XTFS_MKVOL_OPTION_STRIPING_POLICY_STRIPE_SIZE = 14,
          XTFS_MKVOL_OPTION_STRIPING_POLICY_WIDTH = 15
        };

        org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy;
        uint32_t mode;
        YIELD::auto_Object<YIELD::URI> mrc_uri;
        std::string owner_group_id, owner_user_id;
        org::xtreemfs::interfaces::OSDSelectionPolicyType osd_selection_policy;
        org::xtreemfs::interfaces::StripingPolicyType striping_policy;
        uint32_t striping_policy_stripe_size;
        uint32_t striping_policy_width;
        std::string volume_name;

        // YIELD::Main
        int _main( int, char** )
        {
          YIELD::auto_Object<MRCProxy> mrc_proxy = createMRCProxy( *mrc_uri );
          mrc_proxy->xtreemfs_mkvol( org::xtreemfs::interfaces::Volume( volume_name, mode, osd_selection_policy, org::xtreemfs::interfaces::StripingPolicy( striping_policy, striping_policy_stripe_size, striping_policy_width ), access_control_policy, std::string(), owner_user_id, owner_group_id ) );
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

              case XTFS_MKVOL_OPTION_OWNER_GROUP_ID: owner_group_id = arg; break;
              case XTFS_MKVOL_OPTION_OWNER_USER_ID: owner_user_id = arg; break;

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
                uint32_t new_striping_policy_stripe_size = atoi( arg );
                if ( new_striping_policy_stripe_size != 0 )
                  striping_policy_stripe_size = new_striping_policy_stripe_size;
              }
              break;

              case XTFS_MKVOL_OPTION_STRIPING_POLICY_WIDTH:
              {
                uint32_t new_striping_policy_width = static_cast<uint16_t>( atoi( arg ) );
                if ( new_striping_policy_width != 0 )
                  striping_policy_width = new_striping_policy_width;
              }
              break;

              default: Main::parseOption( id, arg ); break;
            }
          }
        }

        void parseFiles( int files_count, char** files )
        {
          if ( files_count >= 1 )
            mrc_uri = parseVolumeURI( files[0], volume_name );
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
