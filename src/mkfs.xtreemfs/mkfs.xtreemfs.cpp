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


#include <cstdlib>
using std::srand;
#include <ctime>
using std::time;

#include "xtreemfs.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;

#include "yidl.h"
using yidl::runtime::auto_Object;


int main( int argc, char** argv )
{
  OptionParser::Options mkfs_options;
  mkfs_options.add( "-a", "access control policy: NULL|POSIX|VOLUME" );
  mkfs_options.add( "--access-control-policy", "NULL|POSIX|VOLUME" );
  mkfs_options.add( "-m", "mode for root directory of the volume" );
  mkfs_options.add( "--mode", "mode for root directory of the volume" );
  mkfs_options.add( "-g", "group id of volume owner" );
  mkfs_options.add( "--owner-group-id" );
  mkfs_options.add( "-u", "user id of owner" );
  mkfs_options.add( "--owner-user-id" );
  mkfs_options.add( "--password", "MRC administrator password" );
  mkfs_options.add( "-p", "striping policy: NONE|RAID0" );
  mkfs_options.add( "--striping-policy", "NONE|RAID0" );
  mkfs_options.add( "-s", "striping policy stripe size" );
  mkfs_options.add( "--striping-policy-stripe-size" );
  mkfs_options.add( "-w", "striping policy width" );
  mkfs_options.add( "--striping-policy-width" );

  if ( argc == 1 )
  {
    cout << "mkfs.xtreemfs: create a new volume" << endl;
    cout << "Usage: mkfs.xtreemfs <options>" << 
            " [oncrpc://]<dir or mrc host>[:port]/<volume name>" << endl;
    cout << Options::usage( mkfs_options );    
    return 0;
  }

  try
  {
    StatVFS fs;
    fs.set_access_control_policy( ACCESS_CONTROL_POLICY_POSIX );
    fs.set_mode( yield::platform::Volume::DIRECTORY_MODE_DEFAULT );

    StripingPolicy fs_striping_policy( STRIPING_POLICY_RAID0, 128, 1 );

    string mrc_password;

    Options options = Options::parse( argc, argv, mkfs_options );

    for 
    (
      Options::const_iterator parsed_option_i = options.begin();
      parsed_option_i != options.end();
      ++parsed_option_i
    )
    {
      const OptionParser::ParsedOption& popt = *parsed_option_i;
      const string& arg = popt.get_argument();

      if ( popt == "-a" || popt == "--access-control-policy" )
      {
        if ( arg == "NULL" )
          fs.set_access_control_policy( ACCESS_CONTROL_POLICY_NULL );
        else if ( arg == "POSIX" )
          fs.set_access_control_policy( ACCESS_CONTROL_POLICY_POSIX );
        else if ( arg == "VOLUME" )
          fs.set_access_control_policy( ACCESS_CONTROL_POLICY_VOLUME );
      }
      else if ( popt == "-m" || popt == "--mode" )
      {
        uint32_t mode = strtol( arg.c_str(), NULL, 0 );
        if ( mode != 0 )
          fs.set_mode( mode );
      }
      else if ( popt == "-g" || popt == "--owner-group-id" )
        fs.set_owner_group_id( arg );
      else if ( popt == "-u" || popt == "--owner-user-id" )
        fs.set_owner_user_id( arg );
      else if ( popt == "--password" )
        mrc_password = arg;
      else if ( popt == "--striping-policy" )
      {
        if ( arg == "RAID0" )
          fs_striping_policy.set_type( STRIPING_POLICY_RAID0 );
      }
      else if ( popt == "-s" || popt == "--striping-policy-stripe-size" )
      {
        uint32_t stripe_size = atoi( arg.c_str() );
        if ( stripe_size != 0 )
          fs_striping_policy.set_stripe_size( stripe_size );
      }
      else if ( popt == "-w" || popt == "--striping-policy-width" )
      {
        uint32_t width = static_cast<uint16_t>( atoi( arg.c_str() ) );
        if ( width != 0 )
          fs_striping_policy.set_width( width );
      }
    }

    fs.set_default_striping_policy( fs_striping_policy );


    URI* dir_or_mrc_uri = options.get_uri();
    if ( dir_or_mrc_uri == NULL || dir_or_mrc_uri->get_resource() == "/" )
      throw Exception( "must specify the <DIR|MRC>/<volume name> URI" );
    dir_or_mrc_uri->inc_ref();
    fs.set_name( dir_or_mrc_uri->get_resource().substr( 1 ) ); 


    URI* mrc_uri = NULL; // What we ultimately need to contact

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
      auto_Object<DIRProxy> dir_proxy = DIRProxy::create( options );

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
      catch ( Exception& )
      {
        // dir_or_mrc_uri is an MRC URI after all
        mrc_uri = dir_or_mrc_uri;
      }

      if ( mrc_uri == NULL )
      {
        if ( mrc_services.empty() )
          throw Exception( "could not find an MRC" );

        // Select a random MRC
        srand( static_cast<unsigned int>( time( NULL ) ) );
        Service& mrc_service
          = mrc_services[rand() % mrc_services.size()];

        // Find an apppropriate address mapping
        auto_Object<AddressMappingSet> mrc_address_mappings
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
            mrc_uri = new URI( ( *mrc_address_mapping_i ).get_uri() );
            break;
          }
        }

        if ( mrc_uri == NULL )
        {
          // No address mapping with the same scheme as the dir_uri
          // Default to SSL if we have an SSL context, otherwise TCP
          string match_protocol;
          if ( options.get_ssl_context() != NULL )
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
              mrc_uri = new URI( ( *mrc_address_mapping_i ).get_uri() );
              break;
            }
          }

          if ( mrc_uri == NULL )
            throw Exception( "could not find an appropriate MRC" );
        }
      }
    }

    auto_Object<MRCProxy> mrc_proxy 
      = MRCProxy::create( *mrc_uri, options, mrc_password );

    mrc_proxy->xtreemfs_mkvol( fs );

    return 0;
  }
  catch ( Exception& exception )
  {
    cerr << "mkfs.xtreemfs: error: " << exception.what() << endl;
    return exception.get_error_code();      
  }
}
