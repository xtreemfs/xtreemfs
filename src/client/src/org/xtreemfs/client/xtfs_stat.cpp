// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "main.h"
using namespace org::xtreemfs::client;

#include <iostream>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_stat : public Main
      {
      public:
        xtfs_stat() 
          : Main( "xtfs_stat", "show information on a directory, file, or volume", "[oncrpc[s]://]<mrc host>[:port]/volume_name[/file]" )
        { }

      private:
        YIELD::auto_Object<YIELD::URI> mrc_uri;
        Path path;

        // YIELD::Main
        int _main( int, char** )
        {
          YIELD::auto_Object<MRCProxy> mrc_proxy = createMRCProxy( *mrc_uri );
          org::xtreemfs::interfaces::Stat stbuf;
          mrc_proxy->getattr( path, stbuf );

          std::cout << path << std::endl;
          size_t path_len = path.size();
          for ( size_t dash_i = 0; dash_i < path_len; dash_i++ ) std::cout << '-';
          std::cout << std::endl;

          std::cout << "  type           = ";
          if ( ( stbuf.get_mode() & org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_S_IFREG ) == org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_S_IFREG )
            std::cout << "file";
          else if ( ( stbuf.get_mode() & org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_S_IFDIR ) == org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_S_IFDIR )
            std::cout << "directory";
          else if ( ( stbuf.get_mode() & org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_S_IFLNK ) == org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_S_IFLNK )
            std::cout << "symlink";
          else
            std::cout << "unknown";
          std::cout << std::endl;

          std::cout << "  nlink          = " << stbuf.get_nlink() << std::endl;
          std::cout << "  size           = " << stbuf.get_size() << std::endl;

          std::cout << "  atime          = " << YIELD::Time( stbuf.get_atime_ns() ) << std::endl;
          std::cout << "  mtime          = " << YIELD::Time( stbuf.get_mtime_ns() ) << std::endl;
          std::cout << "  ctime          = " << YIELD::Time( stbuf.get_ctime_ns() ) << std::endl;

          std::cout << "  owner user id  = " << stbuf.get_user_id() << std::endl;
          std::cout << "  owner group id = " << stbuf.get_group_id() << std::endl;
          std::cout << "  file_id        = " << stbuf.get_file_id() << std::endl;
          if ( !stbuf.get_link_target().empty() )
            std::cout << "  link target    = " << stbuf.get_link_target() << std::endl;
          std::cout << "  truncate epoch = " << stbuf.get_truncate_epoch() << std::endl;

          org::xtreemfs::interfaces::ReplicaSet replicas;
          try
          {
            mrc_proxy->xtreemfs_replica_list( stbuf.get_file_id(), replicas );
          }
          catch ( YIELD::Exception& )
          { }

          if ( !replicas.empty() )
          {
            std::cout << "  Replicas:" << std::endl;
            for ( org::xtreemfs::interfaces::ReplicaSet::size_type replica_i = 0; replica_i != replicas.size(); replica_i++ )
            {
              std::cout << "    " << replica_i << ": ";
              const org::xtreemfs::interfaces::StringSet& osd_uuids = replicas[replica_i].get_osd_uuids();
              for ( org::xtreemfs::interfaces::StringSet::const_iterator osd_uuid_i = osd_uuids.begin(); osd_uuid_i != osd_uuids.end(); osd_uuid_i++ )
                std::cout << *osd_uuid_i << " ";
              std::cout << std::endl;
            }
          }

          return 0;
        }

        void parseFiles( int files_count, char** files )
        {
          if ( files_count >= 1 )
          {
            mrc_uri = parseURI( files[0] );
            if ( mrc_uri->get_resource().size() > 1 )
              path = Path( mrc_uri->get_resource().c_str() + 1 );
            else
              throw YIELD::Exception( "must specify a volume_name/[path]" );
          }
          else
            throw YIELD::Exception( "must specify an MRC URI" );
        }
      };
    };
  };
};

int main( int argc, char** argv )
{
  return xtfs_stat().main( argc, argv );
}
