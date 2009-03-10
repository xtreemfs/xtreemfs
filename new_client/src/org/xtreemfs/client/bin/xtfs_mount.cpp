#include "org/xtreemfs/client.h"
using namespace org::xtreemfs::client;

#include "yield/platform.h"
#include "yieldfs/fuse.h"

#include <string>
#include <vector>
#include <exception>
#include <iostream>
using namespace std;

#include "SimpleOpt.h"


enum { OPT_FOREGROUND, OPT_DEBUG, OPT_VOLUME_URL, OPT_DIRSERVICE, OPT_OLD };

CSimpleOpt::SOption options[] = {
  { OPT_FOREGROUND, "-f", SO_NONE },
  { OPT_DEBUG, "-d", SO_NONE },
  { OPT_DEBUG, "--debug", SO_NONE },
  { OPT_VOLUME_URL, "--volume_url", SO_REQ_SEP },
  { OPT_DIRSERVICE, "--dirservice", SO_REQ_SEP },
  { OPT_OLD, "-o", SO_REQ_SEP },
  SO_END_OF_OPTIONS
};


int main( int argc, char** argv )
{
  // Options to fill
  bool foreground = false, debug = false;
  string volume_url, mount_point, dirservice;
  YIELD::URI *parsed_volume_url = NULL, *parsed_dirservice = NULL;

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
          case OPT_VOLUME_URL: volume_url = args.OptionArg(); break;
          case OPT_DIRSERVICE: dirservice = args.OptionArg(); break;

          case OPT_OLD:
          {
            string old_args_str( args.OptionArg() );
            vector<string> old_args;

            string::size_type last_comma = old_args_str.find_first_not_of( ",", 0 );
            string::size_type comma = old_args_str.find_first_of( ",", last_comma);
            while ( comma != string::npos || last_comma != string::npos )
            {
              old_args.push_back( old_args_str.substr( last_comma, comma - last_comma ) );
              last_comma = old_args_str.find_first_not_of( ",", comma );
              comma = old_args_str.find_first_of( ",", last_comma );
            }

            for ( vector<string>::iterator old_arg_i = old_args.begin(); old_arg_i != old_args.end(); old_arg_i++ )
            {
              string old_arg_key, old_arg_value;
              string::size_type equals = ( *old_arg_i ).find_first_of( "=" );
              if ( equals == string::npos || equals == ( *old_arg_i ).size() - 1 )
                old_arg_key = *old_arg_i;
              else
              {
                old_arg_key = ( *old_arg_i ).substr( 0, equals );
                old_arg_value = ( *old_arg_i ).substr( equals + 1 );
              }

              if ( old_arg_key == "volume_url" )
                volume_url = old_arg_value;
              else if ( old_arg_key == "dirservice" )
                dirservice = dirservice;
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
      case 2: volume_url = args.Files()[0]; mount_point = args.Files()[1]; break;
      case 3: dirservice = args.Files()[0]; volume_url = args.Files()[1]; mount_point = args.Files()[2]; break;
    }

    if ( !dirservice.empty() )
      parsed_dirservice = new YIELD::URI( dirservice );
    else
      throw YIELD::Exception( "must specify dirservice" );

    if ( !volume_url.empty() )
    {
      parsed_volume_url = new YIELD::URI( volume_url );
      if ( strlen( parsed_volume_url->getResource() ) < 2 )
        throw YIELD::Exception( "must specify volume name in volume_url" );
      if ( mount_point.empty() )
        throw YIELD::Exception( "must specify mount point" );
    }
    else
      throw YIELD::Exception( "must specify volume_url" );
  }
  catch ( exception& exc )
  {
    cerr << "Error parsing command line arguments: " << exc.what() << endl;
    delete parsed_volume_url;
    delete parsed_dirservice;
    return 1;
  }

  if ( debug )
    YIELD::SocketConnection::setTraceSocketIO( true );

  int ret;
  try
  {
    YIELD::SEDAStageGroup& main_stage_group = YIELD::SEDAStageGroup::createStageGroup();
    DIRProxy dir_proxy( *parsed_dirservice );
    MRCProxy mrc_proxy( *parsed_volume_url ); main_stage_group.createStage( mrc_proxy );
    OSDProxyFactory osd_proxy_factory( dir_proxy, main_stage_group );
    Volume volume( parsed_volume_url->getResource() + 1, dir_proxy, mrc_proxy, osd_proxy_factory );
    ret = yieldfs::FUSE( volume ).main( argv[0], mount_point.c_str(), foreground, debug );
    YIELD::SEDAStageGroup::destroyStageGroup( main_stage_group ); // Must destroy the stage group before the event handlers go out of scope so the stages aren't holding dead pointers
  }
  catch ( ProxyException& exc )
  {
    cerr << "Error mounting volume: " << exc.what() << endl;
//    cerr << "  exceptionName: " << exc.get_exceptionName() << endl;
    cerr << "  errno: " << exc.get_error_code() << endl;
//    cerr << "  stackTrace: " << exc.get_stackTrace() << endl;
    ret = 1;
  }
  catch ( exception& exc )
  {
    cerr << "Error creating volume: " << exc.what() << endl;
    ret = 1;
  }

  delete parsed_volume_url;
  delete parsed_dirservice;

  return ret;
}
