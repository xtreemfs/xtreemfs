#include "org/xtreemfs/client.h"
#include "options.h"
using namespace org::xtreemfs::client;

#include "yield/platform.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_mkvolOptions : public Options
      {
      public:
        xtfs_mkvolOptions( int argc, char** argv )
          : Options( "xtfs_mkvol", "create a new volume on a specified MRC", "[oncrpc[s]://]<mrc host>[:port]/<volume name>" )
        {
          addOption( XTFS_MKVOL_OPTION_ACCESS_CONTROL_POLICY, "-a", "--access-control-policy", "NULL|POSIX|VOLUME" );
          access_control_policy = org::xtreemfs::interfaces::ACCESS_CONTROL_POLICY_DEFAULT;

          addOption( XTFS_MKVOL_OPTION_CERTIFICATE_FILE_PATH, "-c", "--certificate-file-path", "path" );

          addOption( XTFS_MKVOL_OPTION_CERTIFICATE_FILE_PATH, "-m", "--mode", "n" );
          mode = org::xtreemfs::interfaces::MODE_DEFAULT;

          addOption( XTFS_MKVOL_OPTION_OSD_SELECTION_POLICY, "-o", "--osd-selection-policy", "SIMPLE" );
          osd_selection_policy = org::xtreemfs::interfaces::OSD_SELECTION_POLICY_DEFAULT;

          addOption( XTFS_MKVOL_OPTION_STRIPING_POLICY, "-p", "--striping-policy", "NONE|RAID0" );
          striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_DEFAULT;

          addOption( XTFS_MKVOL_OPTION_STRIPING_POLICY_STRIPE_SIZE, "-s", "--striping-policy-stripe-size", "n" );
          striping_policy_stripe_size = org::xtreemfs::interfaces::STRIPING_POLICY_STRIPE_SIZE_DEFAULT;

          addOption( XTFS_MKVOL_OPTION_STRIPING_POLICY_WIDTH, "-w", "--striping-policy-width", "n" );
          striping_policy_width = org::xtreemfs::interfaces::STRIPING_POLICY_WIDTH_DEFAULT;

          mrc_uri = NULL;

          parseOptions( argc, argv );
        }

        ~xtfs_mkvolOptions()
        {
          delete mrc_uri;
        }

        uint8_t get_access_control_policy() const { return access_control_policy; }
        const std::string& get_certificate_file_path() const { return certificate_file_path; }
        uint32_t get_mode() const { return mode; }
        YIELD::URI& get_mrc_uri() const { return *mrc_uri; }
        uint8_t get_osd_selection_policy() const { return osd_selection_policy; }
        uint8_t get_striping_policy() const { return striping_policy; }
        uint32_t get_striping_policy_stripe_size() const { return striping_policy_stripe_size; }
        uint16_t get_striping_policy_width() const { return striping_policy_width; }
        const std::string& get_volume_name() const { return volume_name; }

      private:
        enum
        {
         XTFS_MKVOL_OPTION_ACCESS_CONTROL_POLICY,
         XTFS_MKVOL_OPTION_CERTIFICATE_FILE_PATH,
         XTFS_MKVOL_OPTION_MODE,
         XTFS_MKVOL_OPTION_OSD_SELECTION_POLICY,
         XTFS_MKVOL_OPTION_STRIPING_POLICY,
         XTFS_MKVOL_OPTION_STRIPING_POLICY_STRIPE_SIZE,
         XTFS_MKVOL_OPTION_STRIPING_POLICY_WIDTH
        };

        uint8_t access_control_policy;
        std::string certificate_file_path;
        uint32_t mode;
        YIELD::URI* mrc_uri;
        uint8_t osd_selection_policy;
        uint8_t striping_policy;
        uint32_t striping_policy_stripe_size;
        uint16_t striping_policy_width;
        std::string volume_name;

        // OptionParser
        void parseOption( int id, char* arg )
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

            case XTFS_MKVOL_OPTION_CERTIFICATE_FILE_PATH:
            {
              certificate_file_path = arg;
            }
            break;

            case XTFS_MKVOL_OPTION_MODE:
            {
              mode = atoi( arg );
              if ( mode == 0 )
                mode = org::xtreemfs::interfaces::MODE_DEFAULT;
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
              if ( strcmp( arg, "NONE" ) == 0 || strcmp( arg, "NULL" ) == 0 )
                striping_policy = org::xtreemfs::interfaces::STRIPING_POLICY_NONE;
              else if ( strcmp( arg, "RAID0" ) == 0 )
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
              striping_policy_width = atoi( arg );
              if ( striping_policy_width == 0 )
                striping_policy_width = org::xtreemfs::interfaces::STRIPING_POLICY_WIDTH_DEFAULT;
            }
            break;
          }
        }

        void parseFiles( int files_count, char** files )
        {
          if ( files_count >= 1 )
          {
            mrc_uri = parseURI( files[0] );
            if ( strlen( mrc_uri->get_resource() ) > 1 )
              volume_name = mrc_uri->get_resource() + 1;
            else if ( files_count >= 2 )
              volume_name = files[1];
            else
              throw YIELD::URI( "must specify a volume name, either as part of the MRC URI or as a separate parameter" );
          }
          else
            throw YIELD::Exception( "must specify a volume URI" );
        }
      };
    };
  };
};


int main( int argc, char** argv )
{
  try
  {
    xtfs_mkvolOptions options( argc, argv );

    if ( options.get_help() )
      options.printUsage();
    else
    {
      if ( options.get_debug() )
        YIELD::SocketConnection::setTraceSocketIO( true );

      MRCProxy mrc_proxy( options.get_mrc_uri() );
      mrc_proxy.set_operation_timeout_ms( options.get_timeout_ms() );
      mrc_proxy.mkvol( options.get_volume_name(), options.get_osd_selection_policy(), org::xtreemfs::interfaces::StripingPolicy( options.get_striping_policy(), options.get_striping_policy_stripe_size(), options.get_striping_policy_width() ), options.get_access_control_policy() );
    }

    return 0;
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error creating volume: " << exc.what() << std::endl;

    return 1;
  }
}
