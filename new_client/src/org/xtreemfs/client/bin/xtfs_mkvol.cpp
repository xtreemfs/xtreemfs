#include "org/xtreemfs/client.h"
using namespace org::xtreemfs::client;

#include "yield/platform.h"

#include <string>
#include <exception>
#include <iostream>

#include "SimpleOpt.h"


enum { OPT_DEBUG, OPT_STRIPING_POLICY, OPT_OSD_SELECTION, OPT_ACCESS_POLICY, OPT_MODE, OPT_CERT, OPT_DIRSERVICE, OPT_HELP };

CSimpleOpt::SOption options[] = {
  { OPT_DEBUG, "-d", SO_NONE },
  { OPT_STRIPING_POLICY, "-p", SO_REQ_SEP },
  { OPT_STRIPING_POLICY, "--striping-policy", SO_REQ_SEP },
  { OPT_OSD_SELECTION, "-s", SO_REQ_SEP },
  { OPT_OSD_SELECTION, "--osd-selection", SO_REQ_SEP },
  { OPT_ACCESS_POLICY, "-a", SO_REQ_SEP },
  { OPT_ACCESS_POLICY, "--access-policy", SO_REQ_SEP },
  { OPT_MODE, "-m", SO_REQ_SEP },
  { OPT_MODE, "--mode", SO_REQ_SEP },
  { OPT_CERT, "-c", SO_REQ_SEP },
  { OPT_CERT, "--cert", SO_REQ_SEP },
  { OPT_DIRSERVICE, "-D", SO_REQ_SEP },
  { OPT_DIRSERVICE, "--dirservice", SO_REQ_SEP },
  { OPT_HELP, "-h", SO_REQ_SEP },
  { OPT_HELP, "--help", SO_REQ_SEP },
  SO_END_OF_OPTIONS
};

int main( int argc, char** argv )
{
  // Options to fill
  bool debug = false;
  std::string volume_uri_str; YIELD::URI* volume_uri = NULL;
  uint8_t striping_policy_id = org::xtreemfs::interfaces::STRIPING_POLICY_DEFAULT; size_t striping_policy_size = 4, striping_policy_width = 1;
  int osd_selection = 1, access_policy = 2, mode = 0;
  std::string cert_file, dirservice;

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
          case OPT_DEBUG: debug = true; break;

          case OPT_STRIPING_POLICY:
          {
            if ( strncmp( args.OptionArg(), "RAID", 4 ) == 0 && strlen( args.OptionArg() ) > 4 )
            {
              switch ( args.OptionArg()[4] )
              {
                case '0':
                case '1':
                case '5': break; // Set striping_policy_id
              }
            }
          }
          break;

          case OPT_OSD_SELECTION:
          {
            osd_selection = atoi( args.OptionArg() );
            if ( osd_selection < 1 || osd_selection > 2 )
              osd_selection = 1;
          }
          break;

          case OPT_ACCESS_POLICY:
          {
            access_policy = atoi( args.OptionArg() );
            if ( access_policy < 1 || access_policy > 3 )
              access_policy = 2;
          }
          break;

          case OPT_MODE: mode = atoi( args.OptionArg() ); break;
          case OPT_CERT: cert_file = args.OptionArg(); break;
          case OPT_DIRSERVICE: dirservice = args.OptionArg(); break;

          case OPT_HELP: YIELD::DebugBreak(); break;
        }
      }
    }

    // volume_uri after - options
    if ( args.FileCount() >= 1 )
      volume_uri_str = args.Files()[0];
    else
      throw YIELD::Exception( "must specify volume URI" );

    volume_uri = new YIELD::URI( volume_uri_str );
    if ( strlen( volume_uri->getResource() ) <= 1 )
      throw YIELD::Exception( "volume URI must include a volume name" );

    if ( debug )
      YIELD::SocketConnection::setTraceSocketIO( true );  

    MRCProxy mrc_proxy( *volume_uri );
    mrc_proxy.mkvol( org::xtreemfs::interfaces::Context( "user", org::xtreemfs::interfaces::StringSet() ), "", volume_uri->getResource()+1, org::xtreemfs::interfaces::OSD_SELECTION_POLICY_SIMPLE, org::xtreemfs::interfaces::StripingPolicy( striping_policy_id, striping_policy_size, striping_policy_width ), org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_NULL );

    delete volume_uri;

    return 0;
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error creating volume: " << exc.what() << std::endl;  

    delete volume_uri;

    return 1;
  }
  
}
