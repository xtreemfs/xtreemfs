// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "xtreemfs/main.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;

#include <cstdlib>
#include <ctime>


namespace mkfs_xtreemfs
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main
        (
          "mkfs.xtreemfs",
          "create a new volume",
          "[oncrpc://]<dir or mrc host>[:port]/<volume name>"
        )
    {
      addOption
      (
        MKFS_XTREEMFS_OPTION_ACCESS_CONTROL_POLICY,
        "-a",
        "--access-control-policy",
        "NULL|POSIX|VOLUME"
      );
      fs.set_access_control_policy( ACCESS_CONTROL_POLICY_POSIX );

      addOption
      (
        MKFS_XTREEMFS_OPTION_MODE,
        "-m",
        "--mode",
        "n"
      );
      fs.set_mode( YIELD::platform::Volume::DIRECTORY_MODE_DEFAULT );

      addOption
      (
        MKFS_XTREEMFS_OPTION_OWNER_GROUP_ID,
        "-g",
        "--owner-group-id",
        "group id of owner"
      );

      addOption
      (
        MKFS_XTREEMFS_OPTION_OWNER_USER_ID,
        "-u",
        "--owner-user-id",
        "user id of owner"
      );

      addOption
      (
        MKFS_XTREEMFS_OPTION_PASSWORD,
        "--password", NULL,
        "MRC's administrator password"
      );

      addOption
      (
        MKFS_XTREEMFS_OPTION_STRIPING_POLICY,
        "-p",
        "--striping-policy",
        "NONE|RAID0"
      );
      fs_default_striping_policy.set_type( STRIPING_POLICY_RAID0 );

      addOption
      (
        MKFS_XTREEMFS_OPTION_STRIPING_POLICY_STRIPE_SIZE,
        "-s",
        "--striping-policy-stripe-size",
        "n"
      );
      fs_default_striping_policy.set_stripe_size( 128 );

      addOption
      (
        MKFS_XTREEMFS_OPTION_STRIPING_POLICY_WIDTH,
        "-w",
        "--striping-policy-width",
        "n"
      );
      fs_default_striping_policy.set_width( 1 );
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


    YIELD::ipc::auto_URI dir_or_mrc_uri;
    StatVFS fs;
    StripingPolicy fs_default_striping_policy;
    std::string mrc_password;


    // YIELD::Main
    int _main( int, char** )
    {
      YIELD::ipc::auto_URI mrc_uri; // What we ultimately need to contact

      // Check if the URI passed on the command line has a port that's
      // the same as an MRC default port
      if
      (
        dir_or_mrc_uri->get_port() != 0
        &&
        (
          (
            dir_or_mrc_uri->get_scheme() == ONCRPC_SCHEME &&
            dir_or_mrc_uri->get_port() == MRCInterface::ONCRPC_PORT_DEFAULT
          )
          ||
          (
            dir_or_mrc_uri->get_scheme() == ONCRPCS_SCHEME &&
            dir_or_mrc_uri->get_port() == MRCInterface::ONCRPCS_PORT_DEFAULT
          )
          ||
          (
            dir_or_mrc_uri->get_scheme() == ONCRPCG_SCHEME &&
            dir_or_mrc_uri->get_port() == MRCInterface::ONCRPCG_PORT_DEFAULT
          )
          ||
          (
            dir_or_mrc_uri->get_scheme() == ONCRPCU_SCHEME &&
            dir_or_mrc_uri->get_port() == MRCInterface::ONCRPCU_PORT_DEFAULT
          )
        )
      )
      {
        mrc_uri = dir_or_mrc_uri;
      }
      else
      {
        // Assume dir_or_mrc_uri is a DIR URI
        auto_DIRProxy dir_proxy( createDIRProxy( *dir_or_mrc_uri ) );

        // Get a list of all MRC services
        ServiceSet mrc_services;
        try
        {
          dir_proxy->xtreemfs_service_get_by_type
          (
            SERVICE_TYPE_MRC,
            mrc_services
          );
        }
        catch ( YIELD::platform::Exception& )
        {
          // dir_or_mrc_uri is an MRC URI after all
          mrc_uri = dir_or_mrc_uri;
        }

        if ( mrc_uri == NULL )
        {
          if ( mrc_services.empty() )
            throw YIELD::platform::Exception( "could not find an MRC" );

          // Select a random MRC
          std::srand( static_cast<unsigned int>( std::time( NULL ) ) );
          Service& mrc_service
            = mrc_services[std::rand() % mrc_services.size()];

          // Find an apppropriate address mapping
          yidl::runtime::auto_Object<AddressMappingSet> mrc_address_mappings
            = dir_proxy->getAddressMappingsFromUUID( mrc_service.get_uuid() );

          for
          (
            AddressMappingSet::const_iterator mrc_address_mapping_i =
              mrc_address_mappings->begin();
            mrc_address_mapping_i != mrc_address_mappings->end();
            ++mrc_address_mapping_i
          )
          {
            if
            (
              ( *mrc_address_mapping_i ).get_protocol() ==
              dir_or_mrc_uri->get_scheme()
            )
            {
              mrc_uri
                = new YIELD::ipc::URI( ( *mrc_address_mapping_i ).get_uri() );
              break;
            }
          }

          if ( mrc_uri == NULL )
          {
            // No address mapping with the same scheme as the dir_uri
            // Default to SSL if we have an SSL context, otherwise TCP
            std::string match_protocol;
            if ( get_proxy_ssl_context() != NULL )
              match_protocol = ONCRPCS_SCHEME;
            else
              match_protocol = ONCRPC_SCHEME;

            for
            (
              AddressMappingSet::const_iterator mrc_address_mapping_i =
                mrc_address_mappings->begin();
              mrc_address_mapping_i != mrc_address_mappings->end();
              ++mrc_address_mapping_i
            )
            {
              if ( ( *mrc_address_mapping_i ).get_protocol() == match_protocol )
              {
                mrc_uri
                 = new YIELD::ipc::URI( ( *mrc_address_mapping_i ).get_uri() );
                break;
              }
            }

            if ( mrc_uri == NULL )
             throw YIELD::platform::Exception( "could not an MRC" );
          }
        }
      }
  
      fs.set_default_striping_policy( fs_default_striping_policy );
      createMRCProxy( *mrc_uri, mrc_password.c_str() )->xtreemfs_mkvol( fs );

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
              fs.set_access_control_policy( ACCESS_CONTROL_POLICY_NULL );
            else if ( strcmp( arg, "POSIX" ) == 0 )
              fs.set_access_control_policy( ACCESS_CONTROL_POLICY_POSIX );
            else if ( strcmp( arg, "VOLUME" ) == 0 )
              fs.set_access_control_policy( ACCESS_CONTROL_POLICY_VOLUME );
          }
          break;

          case MKFS_XTREEMFS_OPTION_MODE:
          {
            uint32_t mode = strtol( arg, NULL, 0 );
            if ( mode != 0 )
              fs.set_mode( mode );
          }
          break;

          case MKFS_XTREEMFS_OPTION_OWNER_GROUP_ID:
          {
            fs.set_owner_group_id( arg );
          }
          break;

          case MKFS_XTREEMFS_OPTION_OWNER_USER_ID:
          {
            fs.set_owner_user_id( arg );
          }
          break;

          case MKFS_XTREEMFS_OPTION_PASSWORD:
          {
            mrc_password = arg;
          }
          break;

          case MKFS_XTREEMFS_OPTION_STRIPING_POLICY:
          {
            if ( strcmp( arg, "RAID0" ) == 0 )
              fs_default_striping_policy.set_type( STRIPING_POLICY_RAID0 );
          }
          break;

          case MKFS_XTREEMFS_OPTION_STRIPING_POLICY_STRIPE_SIZE:
          {
            uint32_t stripe_size = atoi( arg );
            if ( stripe_size != 0 )
              fs_default_striping_policy.set_stripe_size( stripe_size );
          }
          break;

          case MKFS_XTREEMFS_OPTION_STRIPING_POLICY_WIDTH:
          {
            uint32_t width = static_cast<uint16_t>( atoi( arg ) );
            if ( width != 0 )
              fs_default_striping_policy.set_width( width );
          }
          break;

          default: xtreemfs::Main::parseOption( id, arg ); break;
        }
      }
    }

    void parseFiles( int files_count, char** files )
    {
      if ( files_count == 1 )
      {
        std::string fs_name;
        dir_or_mrc_uri = parseVolumeURI( files[0], fs_name );
        fs.set_name( fs_name );
      }
      else if ( files_count == 0 )
      {
        throw YIELD::platform::Exception
        (
          "must specify the <DIR|MRC>/volume URI"
        );
      }
      else
      {
        throw YIELD::platform::Exception
        (
          "extra parameters after the <DIR|MRC>/volume URI"
        );
      }
    }
  };
};

int main( int argc, char** argv )
{
  return mkfs_xtreemfs::Main().main( argc, argv );
}
