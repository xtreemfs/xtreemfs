#include "org/xtreemfs/client.h"
using namespace org::xtreemfs::client;

#include "yield/platform.h"

#include <string>
#include <exception>
#include <iostream>
using std::cout;
using std::endl;

#include "SimpleOpt.h"


enum 
{ 
 OPT_ACCESS_CONTROL_POLICY,
 OPT_CERTIFICATE_FILE_PATH, 
 OPT_DEBUG, 
 OPT_HELP,
 OPT_MODE, 
 OPT_OSD_SELECTION_POLICY, 
 OPT_STRIPING_POLICY,
 OPT_STRIPING_POLICY_STRIPE_SIZE,
 OPT_STRIPING_POLICY_WIDTH,
};

CSimpleOpt::SOption options[] = 
{
  { OPT_ACCESS_CONTROL_POLICY, "-a", SO_REQ_SEP },
  { OPT_ACCESS_CONTROL_POLICY, "--access-policy", SO_REQ_SEP },
  { OPT_ACCESS_CONTROL_POLICY, "--access-control-policy", SO_REQ_SEP },
  { OPT_CERTIFICATE_FILE_PATH, "-c", SO_REQ_SEP },
  { OPT_CERTIFICATE_FILE_PATH, "--cert", SO_REQ_SEP },
  { OPT_CERTIFICATE_FILE_PATH, "--certificate-file-path", SO_REQ_SEP },
  { OPT_HELP, "-h", SO_NONE },
  { OPT_HELP, "--help", SO_NONE },
  { OPT_HELP, "-u", SO_NONE },
  { OPT_HELP, "--usage", SO_NONE },
  { OPT_DEBUG, "-d", SO_NONE },
  { OPT_MODE, "-m", SO_REQ_SEP },
  { OPT_MODE, "--mode", SO_REQ_SEP },
  { OPT_OSD_SELECTION_POLICY, "-o", SO_REQ_SEP },
  { OPT_OSD_SELECTION_POLICY, "--osd-selection", SO_REQ_SEP },
  { OPT_OSD_SELECTION_POLICY, "--osd-selection-selection", SO_REQ_SEP },
  { OPT_STRIPING_POLICY, "-p", SO_REQ_SEP },
  { OPT_STRIPING_POLICY, "--striping-policy", SO_REQ_SEP },
  { OPT_STRIPING_POLICY_STRIPE_SIZE, "-s", SO_REQ_SEP },
  { OPT_STRIPING_POLICY_STRIPE_SIZE, "--stripe-size", SO_REQ_SEP },
  { OPT_STRIPING_POLICY_STRIPE_SIZE, "--striping-policy-stripe-size", SO_REQ_SEP },
  { OPT_STRIPING_POLICY_WIDTH, "-w", SO_REQ_SEP },
  { OPT_STRIPING_POLICY_WIDTH, "--width", SO_REQ_SEP },
  { OPT_STRIPING_POLICY_WIDTH, "--striping-policy-width", SO_REQ_SEP },
  SO_END_OF_OPTIONS
};

int main( int argc, char** argv )
{
  // Options to fill
  bool debug = false;
  uint8_t striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_DEFAULT; 
  uint32_t striping_policy_stripe_size = org::xtreemfs::interfaces::STRIPING_POLICY_STRIPE_SIZE_DEFAULT;
  uint16_t striping_policy_width = org::xtreemfs::interfaces::STRIPING_POLICY_WIDTH_DEFAULT;
  uint8_t osd_selection_policy = org::xtreemfs::interfaces::OSD_SELECTION_POLICY_DEFAULT;
  uint8_t access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_DEFAULT;
  uint32_t mode = org::xtreemfs::interfaces::MODE_DEFAULT;
  std::string certificate_file_path;

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
          case OPT_ACCESS_CONTROL_POLICY:
          {
            if ( strcmp( args.OptionArg(), "NULL" ) == 0 )
              access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_NULL;
            else if ( strcmp( args.OptionArg(), "POSIX" ) == 0 )
              access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_POSIX;
            else if ( strcmp( args.OptionArg(), "VOLUME" ) == 0 )
              access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_VOLUME;
          }
          break;

          case OPT_CERTIFICATE_FILE_PATH:
          {
           certificate_file_path = args.OptionArg();
          }
          break;

          case OPT_DEBUG: debug = true; break;

          case OPT_HELP:
          {
            cout << "mkvol: create a new volume on a specified MRC." << endl;
            cout << endl;
            cout << "Usage:" << endl;
            cout << "  mkvol [options] [oncrpc[s]://]mrc_host[:port]/volume_name" << endl;
            cout << endl;
            cout << "Options:" << endl;
            cout << "  -a/--access-control-policy=NULL|POSIX|VOLUME" << endl;
            cout << "  -c/--certificate-file-path=path" << endl;
            cout << "  -m/--mode=n" << endl;
            cout << "  -o/--osd-selection-policy=SIMPLE" << endl;
            cout << "  -p/--striping-policy=NONE|RAID0" << endl;
            cout << "  -s/--striping-policy-stripe-size=n" << endl;
            cout << "  -w/--striping-policy-width=n" << endl;
            cout << endl;
            return 0;
          }
          break;

          case OPT_MODE: 
          {
            mode = atoi( args.OptionArg() );
            if ( mode == 0 )
              mode = org::xtreemfs::interfaces::MODE_DEFAULT;
          }
          break;

          case OPT_OSD_SELECTION_POLICY: 
          {
            if ( strcmp( args.OptionArg(), "SIMPLE" ) == 0 )
              osd_selection_policy = org::xtreemfs::interfaces::OSD_SELECTION_POLICY_SIMPLE;
          }
          break;           

          case OPT_STRIPING_POLICY:
          {
            if ( strcmp( args.OptionArg(), "NONE" ) == 0 || strcmp( args.OptionArg(), "NULL" ) == 0 )
              striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_NONE;
            else if ( strcmp( args.OptionArg(), "RAID0" ) == 0 )
              striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_RAID0;
          }
          break;

          case OPT_STRIPING_POLICY_STRIPE_SIZE:
          {
            striping_policy_stripe_size = atoi( args.OptionArg() );
            if ( striping_policy_stripe_size == 0 )
              striping_policy_stripe_size = org::xtreemfs::interfaces::STRIPING_POLICY_STRIPE_SIZE_DEFAULT;
          }
          break;

          case OPT_STRIPING_POLICY_WIDTH:
          {
            striping_policy_width = atoi( args.OptionArg() );
            if ( striping_policy_width == 0 )
              striping_policy_width = org::xtreemfs::interfaces::STRIPING_POLICY_WIDTH_DEFAULT;
          }
          break;
        }
      }
    }

    if ( args.FileCount() >= 1 )
    {
      std::string volume_uri_str( args.Files()[0] );
      if ( volume_uri_str.find( "://" ) == std::string::npos )
        volume_uri_str = org::xtreemfs::interfaces::ONCRPC_SCHEME + std::string( "://" ) + volume_uri_str;
      YIELD::URI volume_uri( volume_uri_str );
      if ( strlen( volume_uri.getResource() ) <= 1 )
        throw YIELD::Exception( "volume URI must include a volume name" );

      if ( debug )
        YIELD::SocketConnection::setTraceSocketIO( true );  

      MRCProxy( volume_uri ).mkvol( volume_uri.getResource()+1, osd_selection_policy, org::xtreemfs::interfaces::StripingPolicy( striping_policy, striping_policy_stripe_size, striping_policy_width ), access_control_policy );

      return 0;
    }
    else
      throw YIELD::Exception( "must specify volume URI" );
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error creating volume: " << exc.what() << endl;  

    return 1;
  }
  
}
