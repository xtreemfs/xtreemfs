// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/main.h"


namespace mkfs_xtreemfs
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main( "mkfs.xtreemfs", "create a new volume on a specified MRC", "[oncrpc://]<mrc host>[:port]/<volume name>" )
    {
      addOption( MKFS_XTREEMFS_OPTION_ACCESS_CONTROL_POLICY, "-a", "--access-control-policy", "NULL|POSIX|VOLUME" );
      access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_POSIX;

      addOption( MKFS_XTREEMFS_OPTION_MODE, "-m", "--mode", "n" );
      mode = YIELD::platform::Volume::DEFAULT_DIRECTORY_MODE;

      addOption( MKFS_XTREEMFS_OPTION_OWNER_GROUP_ID, "-g", "--owner-group-id", "group id of owner" );

      addOption( MKFS_XTREEMFS_OPTION_PASSWORD, "--password", NULL, "MRC's administrator password" );

      addOption( MKFS_XTREEMFS_OPTION_STRIPING_POLICY, "-p", "--striping-policy", "NONE|RAID0" );
      striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_RAID0;

      addOption( MKFS_XTREEMFS_OPTION_STRIPING_POLICY_STRIPE_SIZE, "-s", "--striping-policy-stripe-size", "n" );
      striping_policy_stripe_size = 128;

      addOption( MKFS_XTREEMFS_OPTION_OWNER_USER_ID, "-u", "--owner-user-id", "user id of owner" );

      addOption( MKFS_XTREEMFS_OPTION_STRIPING_POLICY_WIDTH, "-w", "--striping-policy-width", "n" );
      striping_policy_width = 1;
    }

  private:
    enum
    {
      MKFS_XTREEMFS_OPTION_ACCESS_CONTROL_POLICY = 20,
      MKFS_XTREEMFS_OPTION_MODE = 21,
      MKFS_XTREEMFS_OPTION_OWNER_GROUP_ID = 22,
      MKFS_XTREEMFS_OPTION_OWNER_USER_ID = 23,      
      MKFS_XTREEMFS_OPTION_PASSWORD = 24,
      MKFS_XTREEMFS_OPTION_STRIPING_POLICY = 25,
      MKFS_XTREEMFS_OPTION_STRIPING_POLICY_STRIPE_SIZE = 26,
      MKFS_XTREEMFS_OPTION_STRIPING_POLICY_WIDTH = 27
    };

    org::xtreemfs::interfaces::AccessControlPolicyType access_control_policy;
    uint32_t mode;
    YIELD::ipc::auto_URI mrc_uri;
    std::string owner_group_id, owner_user_id;
    std::string password;
    org::xtreemfs::interfaces::StripingPolicyType striping_policy;
    uint32_t striping_policy_stripe_size;
    uint32_t striping_policy_width;
    std::string volume_name;

    // YIELD::Main
    int _main( int, char** )
    {
      createMRCProxy( *mrc_uri, password.c_str() )->xtreemfs_mkvol( 
        org::xtreemfs::interfaces::Volume(
          access_control_policy,
          org::xtreemfs::interfaces::StripingPolicy( striping_policy, striping_policy_stripe_size, striping_policy_width ),
          std::string(),
          mode,
          volume_name,
          owner_group_id,
          owner_user_id
        )
      );
      return 0;
    }

    void parseOption( int id, char* arg )
    {
      if ( arg )
      {
        switch ( id )
        {
          case MKFS_XTREEMFS_OPTION_ACCESS_CONTROL_POLICY:
          {
            if ( strcmp( arg, "NULL" ) == 0 )
              access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_NULL;
            else if ( strcmp( arg, "POSIX" ) == 0 )
              access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_POSIX;
            else if ( strcmp( arg, "VOLUME" ) == 0 )
              access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_VOLUME;
          }
          break;

          case MKFS_XTREEMFS_OPTION_MODE:
          {
            mode = strtol( arg, NULL, 0 );
            if ( mode == 0 )
              mode = YIELD::platform::Volume::DEFAULT_DIRECTORY_MODE;
          }
          break;

          case MKFS_XTREEMFS_OPTION_OWNER_GROUP_ID: owner_group_id = arg; break;
          case MKFS_XTREEMFS_OPTION_OWNER_USER_ID: owner_user_id = arg; break;
          case MKFS_XTREEMFS_OPTION_PASSWORD: password = arg; break;

          case MKFS_XTREEMFS_OPTION_STRIPING_POLICY:
          {
            if ( strcmp( arg, "RAID0" ) == 0 )
              striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_RAID0;
          }
          break;

          case MKFS_XTREEMFS_OPTION_STRIPING_POLICY_STRIPE_SIZE:
          {
            uint32_t new_striping_policy_stripe_size = atoi( arg );
            if ( new_striping_policy_stripe_size != 0 )
              striping_policy_stripe_size = new_striping_policy_stripe_size;
          }
          break;

          case MKFS_XTREEMFS_OPTION_STRIPING_POLICY_WIDTH:
          {
            uint32_t new_striping_policy_width = static_cast<uint16_t>( atoi( arg ) );
            if ( new_striping_policy_width != 0 )
              striping_policy_width = new_striping_policy_width;
          }
          break;

          default: xtreemfs::Main::parseOption( id, arg ); break;
        }
      }
    }

    void parseFiles( int files_count, char** files )
    {
      if ( files_count >= 1 )
        mrc_uri = parseVolumeURI( files[0], volume_name );
      else
        throw YIELD::platform::Exception( "must specify the MRC and volume name as a URI" );
    }
  };
};

int main( int argc, char** argv )
{
  return mkfs_xtreemfs::Main().main( argc, argv );
}
