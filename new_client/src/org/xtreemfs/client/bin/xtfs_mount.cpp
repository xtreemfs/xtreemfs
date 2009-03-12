#include "org/xtreemfs/client.h"
using namespace org::xtreemfs::client;

#include "yield/platform.h"
#include "yieldfs/fuse.h"

#include <string>
#include <vector>
#include <exception>
#include <iostream>

#include "SimpleOpt.h"


enum { OPT_FOREGROUND, OPT_DEBUG, OPT_DIR, OPT_OLD, OPT_VOLUME_URI, OPT_VOLUME_UUID };

CSimpleOpt::SOption options[] = {
  { OPT_DEBUG, "-d", SO_NONE },
  { OPT_DEBUG, "--debug", SO_NONE },
  { OPT_DIR, "--dir", SO_REQ_SEP },
  { OPT_DIR, "--dirservice", SO_REQ_SEP },
  { OPT_FOREGROUND, "-f", SO_NONE },
  { OPT_OLD, "-o", SO_REQ_SEP },
  { OPT_VOLUME_URI, "--volume-url", SO_REQ_SEP },
  { OPT_VOLUME_URI, "--volume-uri", SO_REQ_SEP },
  { OPT_VOLUME_UUID, "--volume", SO_REQ_SEP },
  { OPT_VOLUME_UUID, "--volume-uuid", SO_REQ_SEP },
  { OPT_VOLUME_UUID, "--volume_name", SO_REQ_SEP },
  SO_END_OF_OPTIONS
};


int main( int argc, char** argv )
{
  // Options to fill
  bool foreground = false, debug = false;
  std::string dir, mount_point, volume_uri_str, volume_name;
  YIELD::URI *dir_uri = NULL, *mrc_uri = NULL;

  try
  {
    CSimpleOpt args( argc, argv, options );

    // - options
    while ( args.Next() )
    {
      if ( args.LastError() == SO_SUCCESS )
      {
        switch ( args.OptionId() )
        {
          case OPT_FOREGROUND: foreground = true; break;
          case OPT_DEBUG: debug = true; break;
          case OPT_DIR: dir = args.OptionArg(); break;

          case OPT_VOLUME_UUID: volume_name = args.OptionArg(); break;
          case OPT_VOLUME_URI: volume_uri_str = args.OptionArg(); break;

          case OPT_OLD:
          {
            std::string old_args_str( args.OptionArg() );
            std::vector<std::string> old_args;

            std::string::size_type last_comma = old_args_str.find_first_not_of( ",", 0 );
            std::string::size_type comma = old_args_str.find_first_of( ",", last_comma);
            while ( comma != std::string::npos || last_comma != std::string::npos )
            {
              old_args.push_back( old_args_str.substr( last_comma, comma - last_comma ) );
              last_comma = old_args_str.find_first_not_of( ",", comma );
              comma = old_args_str.find_first_of( ",", last_comma );
            }

            for ( std::vector<std::string>::iterator old_arg_i = old_args.begin(); old_arg_i != old_args.end(); old_arg_i++ )
            {
              std::string old_arg_key, old_arg_value;
              std::string::size_type equals = ( *old_arg_i ).find_first_of( "=" );
              if ( equals == std::string::npos || equals == ( *old_arg_i ).size() - 1 )
                old_arg_key = *old_arg_i;
              else
              {
                old_arg_key = ( *old_arg_i ).substr( 0, equals );
                old_arg_value = ( *old_arg_i ).substr( equals + 1 );
              }

              if ( old_arg_key == "volume_url" )
                volume_uri_str = old_arg_value;
              else if ( old_arg_key == "dirservice" )
                dir = old_arg_value;
              else if ( old_arg_key == "debug" )
                debug = true;
            }
          }
        }
      }
    }

    // [dirservice] [volume_url] mount point after - options
    if ( args.FileCount() == 0 )
      throw YIELD::Exception( "must specify dirservice, volume_url, and mount point" );
    switch ( args.FileCount() )
    {
      case 1: mount_point = args.Files()[0]; break;
      case 2: volume_name = args.Files()[0]; mount_point = args.Files()[1]; break;
      case 3: dir = args.Files()[0]; volume_name = args.Files()[1]; mount_point = args.Files()[2]; break;
    }

    if ( !dir.empty() )
    {
      dir_uri = YIELD::URI::parseURI( dir );
      if ( dir_uri == NULL ) // Assume dir is a host[:port]
      {
        std::string complete_dir( dir );
        if ( complete_dir.find( "://" ) != std::string::npos )
          complete_dir = "oncrpc://" + complete_dir;
        dir_uri = YIELD::URI::parseURI( complete_dir );        
        if ( dir_uri == NULL )
          throw YIELD::Exception( "invalid directory service URI" );
      }
    }
    else
      throw YIELD::Exception( "must specify directory service URI" );

    if ( !volume_uri_str.empty() )
    {
        mrc_uri = YIELD::URI::parseURI( volume_uri_str );
        if ( mrc_uri != NULL )
        {
          if ( strlen( mrc_uri->getResource() ) >= 2 )
            volume_name = mrc_uri->getResource();
          else
            throw YIELD::Exception( "must specify volume name in volume URI" );
        }
        else
          volume_name = volume_uri_str;
    }
    else if ( volume_name.empty() )
      throw YIELD::Exception( "must specify volume" );

    if ( mount_point.empty() )
      throw YIELD::Exception( "must specify mount point" );
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error parsing command line arguments: " << exc.what() << std::endl;
    delete dir_uri;
    delete mrc_uri;
    return 1;
  }

  if ( debug )
    YIELD::SocketConnection::setTraceSocketIO( true );

  int ret;
  try
  {
    YIELD::SEDAStageGroup& main_stage_group = YIELD::SEDAStageGroup::createStageGroup();

    DIRProxy dir_proxy( *dir_uri ); main_stage_group.createStage( dir_proxy );

    if ( mrc_uri == NULL )
    {
      org::xtreemfs::interfaces::ServiceRegistrySet service_registries;
      dir_proxy.service_get_by_name( volume_name, service_registries );
      if ( !service_registries.empty() )
      {
        for ( org::xtreemfs::interfaces::ServiceRegistrySet::const_iterator service_registry_i = service_registries.begin(); service_registry_i != service_registries.end(); service_registry_i++ )
        {
          const org::xtreemfs::interfaces::ServiceRegistryDataMap& data = ( *service_registry_i ).get_data();
          for ( org::xtreemfs::interfaces::ServiceRegistryDataMap::const_iterator data_i = data.begin(); data_i != data.end(); data_i++ )
          {
            if ( data_i->first == "mrc" )
            {
              std::string mrc_uuid = data_i->second;
              org::xtreemfs::interfaces::AddressMappingSet mrc_address_mappings;
              dir_proxy.address_mappings_get( mrc_uuid, mrc_address_mappings );
              if ( !mrc_address_mappings.empty() )
              {
                std::ostringstream mrc_uri_str;
                mrc_uri_str << mrc_address_mappings[0].get_protocol() << "://" << mrc_address_mappings[0].get_address() << ":" << mrc_address_mappings[0].get_port() << "/";
                mrc_uri = YIELD::URI::parseURI( mrc_uri_str.str() );
                if ( mrc_uri == NULL )
                  throw YIELD::Exception( "received invalid MRC URI from DIR" );

                break;
              }
              else
                throw YIELD::Exception( "unknown volume" );
            }

            if ( mrc_uri != NULL )
              break;
          }
        }
      }
      else
        throw YIELD::Exception( "unknown volume" );
    }

    MRCProxy mrc_proxy( *mrc_uri ); main_stage_group.createStage( mrc_proxy );

    OSDProxyFactory osd_proxy_factory( dir_proxy, main_stage_group );

    Volume volume( volume_name, dir_proxy, mrc_proxy, osd_proxy_factory );
    ret = yieldfs::FUSE( volume ).main( argv[0], mount_point.c_str(), foreground, debug );

    YIELD::SEDAStageGroup::destroyStageGroup( main_stage_group ); // Must destroy the stage group before the event handlers go out of scope so the stages aren't holding dead pointers
  }
  catch ( ProxyException& exc )
  {
    std::cerr << "Error mounting volume: " << exc.getTypeName() << ": " << exc.what() << std::endl;
    std::cerr << "  errno: " << exc.get_error_code() << std::endl;
    std::cerr << "  stack trace: " << exc.get_stack_trace() << std::endl;
    ret = 1;
  }
  catch ( YIELD::Exception& exc )
  {
    std::cerr << "Error mounting volume: " << exc.what() << std::endl;
    std::cerr << "  errno: " << exc.get_error_code() << std::endl;
    ret = 1;
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error mounting volume: " << exc.what() << std::endl;
    ret = 1;
  }

  delete dir_uri;
  delete mrc_uri;

  return ret;
}
