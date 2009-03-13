#ifndef _28341834929_H
#define _28341834929_H





namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
        class MRCInterfaceFuzzer
      {
      public:
          MRCInterfaceFuzzer( MRCInterface& test_interface )
              : test_interface( test_interface )
          { }
  
          void fuzz()
          {
           fuzz_access();
           fuzz_chmod();
           fuzz_chown();
           fuzz_create();
           fuzz_getattr();
           fuzz_getxattr();
           fuzz_link();
           fuzz_listxattr();
           fuzz_mkdir();
           fuzz_mkvol();
           fuzz_open();
           fuzz_readdir();
           fuzz_removexattr();
           fuzz_rename();
           fuzz_rmdir();
           fuzz_rmvol();
           fuzz_setattr();
           fuzz_setxattr();
           fuzz_statfs();
           fuzz_symlink();
           fuzz_unlink();
           fuzz_utime();
           fuzz_xtreemfs_check_file_exists();
           fuzz_xtreemfs_get_suitable_osds();
           fuzz_xtreemfs_renew_capability();
           fuzz_xtreemfs_replica_add();
           fuzz_xtreemfs_replica_remove();
           fuzz_xtreemfs_restore_file();
           fuzz_xtreemfs_update_file_size();
          }
  
  
           void fuzz_access()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t mode = 0;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.access( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_chmod()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t mode = 0;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.chmod( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_chown()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string userId;
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string userId;
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string userId( "bogus string" );
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string userId( "bogus string" );
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string userId;
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string userId;
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string userId( "bogus string" );
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string userId( "bogus string" );
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string userId;
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string userId;
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string userId( "bogus string" );
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string userId( "bogus string" );
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string userId;
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string userId;
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string userId( "bogus string" );
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string userId( "bogus string" );
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string userId;
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string userId;
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string userId( "bogus string" );
           std::string groupId;
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string userId( "bogus string" );
           std::string groupId( "bogus string" );
  
             try
             {
              test_interface.chown( context, path, userId, groupId );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_create()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t mode = 0;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.create( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_getattr()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           org::xtreemfs::interfaces::stat_ stbuf;
  
             try
             {
              test_interface.getattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           org::xtreemfs::interfaces::stat_ stbuf;
  
             try
             {
              test_interface.getattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           org::xtreemfs::interfaces::stat_ stbuf;
  
             try
             {
              test_interface.getattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           org::xtreemfs::interfaces::stat_ stbuf;
  
             try
             {
              test_interface.getattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           org::xtreemfs::interfaces::stat_ stbuf;
  
             try
             {
              test_interface.getattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_getxattr()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name;
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name( "bogus string" );
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name;
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name( "bogus string" );
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name;
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name( "bogus string" );
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name;
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name( "bogus string" );
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name;
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name( "bogus string" );
  
             try
             {
              test_interface.getxattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_link()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string target_path;
           std::string link_path;
  
             try
             {
              test_interface.link( context, target_path, link_path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string target_path;
           std::string link_path( "bogus string" );
  
             try
             {
              test_interface.link( context, target_path, link_path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string target_path( "bogus string" );
           std::string link_path;
  
             try
             {
              test_interface.link( context, target_path, link_path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string target_path( "bogus string" );
           std::string link_path( "bogus string" );
  
             try
             {
              test_interface.link( context, target_path, link_path );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_listxattr()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           org::xtreemfs::interfaces::StringSet names;
  
             try
             {
              test_interface.listxattr( context, path, names );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           org::xtreemfs::interfaces::StringSet names;
  
             try
             {
              test_interface.listxattr( context, path, names );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           org::xtreemfs::interfaces::StringSet names;
  
             try
             {
              test_interface.listxattr( context, path, names );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           org::xtreemfs::interfaces::StringSet names;
  
             try
             {
              test_interface.listxattr( context, path, names );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           org::xtreemfs::interfaces::StringSet names;
  
             try
             {
              test_interface.listxattr( context, path, names );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_mkdir()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t mode = 0;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t mode = 0;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t mode = UINT32_MAX;
  
             try
             {
              test_interface.mkdir( context, path, mode );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_mkvol()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name;
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name;
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name;
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name;
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol/" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol/" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol/" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol/" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol//" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol//" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol//" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol//" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol\"" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol\"" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol\"" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol\"" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name;
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name;
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name;
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name;
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol/" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol/" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol/" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol/" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol//" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol//" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol//" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol//" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol\"" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol\"" );
           uint32_t osd_selection_policy = 0;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol\"" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = 0;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol\"" );
           uint32_t osd_selection_policy = UINT32_MAX;
           org::xtreemfs::interfaces::StripingPolicy default_striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            default_striping_policy.set_policy( policy );
            default_striping_policy.set_stripe_size( stripe_size );
            default_striping_policy.set_width( width );
           }
           uint32_t access_control_policy = UINT32_MAX;
  
             try
             {
              test_interface.mkvol( context, password, volume_name, osd_selection_policy, default_striping_policy, access_control_policy );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_open()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t flags = 0;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t flags = 0;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t flags = UINT32_MAX;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint32_t flags = UINT32_MAX;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t flags = 0;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t flags = 0;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t flags = UINT32_MAX;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint32_t flags = UINT32_MAX;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t flags = 0;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t flags = 0;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t flags = UINT32_MAX;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint32_t flags = UINT32_MAX;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t flags = 0;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t flags = 0;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t flags = UINT32_MAX;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint32_t flags = UINT32_MAX;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t flags = 0;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t flags = 0;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t flags = UINT32_MAX;
           uint32_t mode = 0;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint32_t flags = UINT32_MAX;
           uint32_t mode = UINT32_MAX;
           org::xtreemfs::interfaces::FileCredentials credentials;
  
             try
             {
              test_interface.open( context, path, flags, mode, credentials );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_readdir()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
  
             try
             {
              test_interface.readdir( context, path, directory_entries );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
  
             try
             {
              test_interface.readdir( context, path, directory_entries );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
  
             try
             {
              test_interface.readdir( context, path, directory_entries );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
  
             try
             {
              test_interface.readdir( context, path, directory_entries );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
  
             try
             {
              test_interface.readdir( context, path, directory_entries );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_removexattr()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name;
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name( "bogus string" );
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name;
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name( "bogus string" );
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name;
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name( "bogus string" );
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name;
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name( "bogus string" );
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name;
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name( "bogus string" );
  
             try
             {
              test_interface.removexattr( context, path, name );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_rename()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string source_path;
           std::string target_path;
           org::xtreemfs::interfaces::FileCredentialsSet credentials;
  
             try
             {
              test_interface.rename( context, source_path, target_path, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string source_path;
           std::string target_path( "bogus string" );
           org::xtreemfs::interfaces::FileCredentialsSet credentials;
  
             try
             {
              test_interface.rename( context, source_path, target_path, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string source_path( "bogus string" );
           std::string target_path;
           org::xtreemfs::interfaces::FileCredentialsSet credentials;
  
             try
             {
              test_interface.rename( context, source_path, target_path, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string source_path( "bogus string" );
           std::string target_path( "bogus string" );
           org::xtreemfs::interfaces::FileCredentialsSet credentials;
  
             try
             {
              test_interface.rename( context, source_path, target_path, credentials );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_rmdir()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
  
             try
             {
              test_interface.rmdir( context, path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
  
             try
             {
              test_interface.rmdir( context, path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
  
             try
             {
              test_interface.rmdir( context, path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
  
             try
             {
              test_interface.rmdir( context, path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
  
             try
             {
              test_interface.rmdir( context, path );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_rmvol()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name;
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol" );
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol/" );
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol//" );
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password;
           std::string volume_name( "testvol\"" );
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name;
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol" );
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol/" );
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol//" );
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string password( "bogus string" );
           std::string volume_name( "testvol\"" );
  
             try
             {
              test_interface.rmvol( context, password, volume_name );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_setattr()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           org::xtreemfs::interfaces::stat_ stbuf;
           {
           uint32_t mode = 0;
           uint32_t nlink = 0;
           uint64_t size = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
           uint64_t ctime = 0;
           std::string user_id;
           std::string group_id;
           std::string file_id;
           std::string link_target;
           uint8_t object_type = 0;
           uint32_t truncate_epoch = 0;
           uint32_t attributes = 0;
            stbuf.set_mode( mode );
            stbuf.set_nlink( nlink );
            stbuf.set_size( size );
            stbuf.set_atime( atime );
            stbuf.set_mtime( mtime );
            stbuf.set_ctime( ctime );
            stbuf.set_user_id( user_id );
            stbuf.set_group_id( group_id );
            stbuf.set_file_id( file_id );
            stbuf.set_link_target( link_target );
            stbuf.set_object_type( object_type );
            stbuf.set_truncate_epoch( truncate_epoch );
            stbuf.set_attributes( attributes );
           }
  
             try
             {
              test_interface.setattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           org::xtreemfs::interfaces::stat_ stbuf;
           {
           uint32_t mode = 0;
           uint32_t nlink = 0;
           uint64_t size = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
           uint64_t ctime = 0;
           std::string user_id;
           std::string group_id;
           std::string file_id;
           std::string link_target;
           uint8_t object_type = 0;
           uint32_t truncate_epoch = 0;
           uint32_t attributes = 0;
            stbuf.set_mode( mode );
            stbuf.set_nlink( nlink );
            stbuf.set_size( size );
            stbuf.set_atime( atime );
            stbuf.set_mtime( mtime );
            stbuf.set_ctime( ctime );
            stbuf.set_user_id( user_id );
            stbuf.set_group_id( group_id );
            stbuf.set_file_id( file_id );
            stbuf.set_link_target( link_target );
            stbuf.set_object_type( object_type );
            stbuf.set_truncate_epoch( truncate_epoch );
            stbuf.set_attributes( attributes );
           }
  
             try
             {
              test_interface.setattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           org::xtreemfs::interfaces::stat_ stbuf;
           {
           uint32_t mode = 0;
           uint32_t nlink = 0;
           uint64_t size = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
           uint64_t ctime = 0;
           std::string user_id;
           std::string group_id;
           std::string file_id;
           std::string link_target;
           uint8_t object_type = 0;
           uint32_t truncate_epoch = 0;
           uint32_t attributes = 0;
            stbuf.set_mode( mode );
            stbuf.set_nlink( nlink );
            stbuf.set_size( size );
            stbuf.set_atime( atime );
            stbuf.set_mtime( mtime );
            stbuf.set_ctime( ctime );
            stbuf.set_user_id( user_id );
            stbuf.set_group_id( group_id );
            stbuf.set_file_id( file_id );
            stbuf.set_link_target( link_target );
            stbuf.set_object_type( object_type );
            stbuf.set_truncate_epoch( truncate_epoch );
            stbuf.set_attributes( attributes );
           }
  
             try
             {
              test_interface.setattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           org::xtreemfs::interfaces::stat_ stbuf;
           {
           uint32_t mode = 0;
           uint32_t nlink = 0;
           uint64_t size = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
           uint64_t ctime = 0;
           std::string user_id;
           std::string group_id;
           std::string file_id;
           std::string link_target;
           uint8_t object_type = 0;
           uint32_t truncate_epoch = 0;
           uint32_t attributes = 0;
            stbuf.set_mode( mode );
            stbuf.set_nlink( nlink );
            stbuf.set_size( size );
            stbuf.set_atime( atime );
            stbuf.set_mtime( mtime );
            stbuf.set_ctime( ctime );
            stbuf.set_user_id( user_id );
            stbuf.set_group_id( group_id );
            stbuf.set_file_id( file_id );
            stbuf.set_link_target( link_target );
            stbuf.set_object_type( object_type );
            stbuf.set_truncate_epoch( truncate_epoch );
            stbuf.set_attributes( attributes );
           }
  
             try
             {
              test_interface.setattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           org::xtreemfs::interfaces::stat_ stbuf;
           {
           uint32_t mode = 0;
           uint32_t nlink = 0;
           uint64_t size = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
           uint64_t ctime = 0;
           std::string user_id;
           std::string group_id;
           std::string file_id;
           std::string link_target;
           uint8_t object_type = 0;
           uint32_t truncate_epoch = 0;
           uint32_t attributes = 0;
            stbuf.set_mode( mode );
            stbuf.set_nlink( nlink );
            stbuf.set_size( size );
            stbuf.set_atime( atime );
            stbuf.set_mtime( mtime );
            stbuf.set_ctime( ctime );
            stbuf.set_user_id( user_id );
            stbuf.set_group_id( group_id );
            stbuf.set_file_id( file_id );
            stbuf.set_link_target( link_target );
            stbuf.set_object_type( object_type );
            stbuf.set_truncate_epoch( truncate_epoch );
            stbuf.set_attributes( attributes );
           }
  
             try
             {
              test_interface.setattr( context, path, stbuf );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_setxattr()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name;
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name;
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name;
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name;
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name;
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name;
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name;
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name;
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name;
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name;
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name;
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name( "bogus string" );
           std::string value;
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = 0;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           std::string name( "bogus string" );
           std::string value( "bogus string" );
           int32_t flags = INT32_MAX;
  
             try
             {
              test_interface.setxattr( context, path, name, value, flags );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_statfs()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string volume_name;
           org::xtreemfs::interfaces::statfs_ statfsbuf;
  
             try
             {
              test_interface.statfs( context, volume_name, statfsbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string volume_name( "testvol" );
           org::xtreemfs::interfaces::statfs_ statfsbuf;
  
             try
             {
              test_interface.statfs( context, volume_name, statfsbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string volume_name( "testvol/" );
           org::xtreemfs::interfaces::statfs_ statfsbuf;
  
             try
             {
              test_interface.statfs( context, volume_name, statfsbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string volume_name( "testvol//" );
           org::xtreemfs::interfaces::statfs_ statfsbuf;
  
             try
             {
              test_interface.statfs( context, volume_name, statfsbuf );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string volume_name( "testvol\"" );
           org::xtreemfs::interfaces::statfs_ statfsbuf;
  
             try
             {
              test_interface.statfs( context, volume_name, statfsbuf );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_symlink()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string target_path;
           std::string link_path;
  
             try
             {
              test_interface.symlink( context, target_path, link_path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string target_path;
           std::string link_path( "bogus string" );
  
             try
             {
              test_interface.symlink( context, target_path, link_path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string target_path( "bogus string" );
           std::string link_path;
  
             try
             {
              test_interface.symlink( context, target_path, link_path );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string target_path( "bogus string" );
           std::string link_path( "bogus string" );
  
             try
             {
              test_interface.symlink( context, target_path, link_path );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_unlink()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           org::xtreemfs::interfaces::FileCredentialsSet credentials;
  
             try
             {
              test_interface.unlink( context, path, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           org::xtreemfs::interfaces::FileCredentialsSet credentials;
  
             try
             {
              test_interface.unlink( context, path, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           org::xtreemfs::interfaces::FileCredentialsSet credentials;
  
             try
             {
              test_interface.unlink( context, path, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           org::xtreemfs::interfaces::FileCredentialsSet credentials;
  
             try
             {
              test_interface.unlink( context, path, credentials );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           org::xtreemfs::interfaces::FileCredentialsSet credentials;
  
             try
             {
              test_interface.unlink( context, path, credentials );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_utime()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path;
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol/" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol//" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint64_t ctime = 0;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint64_t ctime = 0;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = 0;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = 0;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string path( "testvol\"" );
           uint64_t ctime = UINT64_MAX;
           uint64_t atime = UINT64_MAX;
           uint64_t mtime = UINT64_MAX;
  
             try
             {
              test_interface.utime( context, path, ctime, atime, mtime );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_check_file_exists()
           {
            {
           std::string volume_id;
           org::xtreemfs::interfaces::StringSet file_ids;
           std::string bitmap;
  
             try
             {
              test_interface.xtreemfs_check_file_exists( volume_id, file_ids, bitmap );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string volume_id( "bogus string" );
           org::xtreemfs::interfaces::StringSet file_ids;
           std::string bitmap;
  
             try
             {
              test_interface.xtreemfs_check_file_exists( volume_id, file_ids, bitmap );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_get_suitable_osds()
           {
            {
           std::string file_id;
           org::xtreemfs::interfaces::StringSet osd_uuids;
  
             try
             {
              test_interface.xtreemfs_get_suitable_osds( file_id, osd_uuids );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::StringSet osd_uuids;
  
             try
             {
              test_interface.xtreemfs_get_suitable_osds( file_id, osd_uuids );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_renew_capability()
           {
            {
           org::xtreemfs::interfaces::XCap old_xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            old_xcap.set_file_id( file_id );
            old_xcap.set_access_mode( access_mode );
            old_xcap.set_expires( expires );
            old_xcap.set_client_identity( client_identity );
            old_xcap.set_truncate_epoch( truncate_epoch );
            old_xcap.set_server_signature( server_signature );
           }
           org::xtreemfs::interfaces::XCap renewed_xcap;
  
             try
             {
              test_interface.xtreemfs_renew_capability( old_xcap, renewed_xcap );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_replica_add()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string file_id;
           org::xtreemfs::interfaces::Replica new_replica;
           {
           org::xtreemfs::interfaces::StripingPolicy striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            striping_policy.set_policy( policy );
            striping_policy.set_stripe_size( stripe_size );
            striping_policy.set_width( width );
           }
           uint32_t replication_flags = 0;
           org::xtreemfs::interfaces::StringSet osd_uuids;
            new_replica.set_striping_policy( striping_policy );
            new_replica.set_replication_flags( replication_flags );
            new_replica.set_osd_uuids( osd_uuids );
           }
  
             try
             {
              test_interface.xtreemfs_replica_add( context, file_id, new_replica );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string file_id( "bogus string" );
           org::xtreemfs::interfaces::Replica new_replica;
           {
           org::xtreemfs::interfaces::StripingPolicy striping_policy;
           {
           uint8_t policy = 0;
           uint32_t stripe_size = 0;
           uint32_t width = 0;
            striping_policy.set_policy( policy );
            striping_policy.set_stripe_size( stripe_size );
            striping_policy.set_width( width );
           }
           uint32_t replication_flags = 0;
           org::xtreemfs::interfaces::StringSet osd_uuids;
            new_replica.set_striping_policy( striping_policy );
            new_replica.set_replication_flags( replication_flags );
            new_replica.set_osd_uuids( osd_uuids );
           }
  
             try
             {
              test_interface.xtreemfs_replica_add( context, file_id, new_replica );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_replica_remove()
           {
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string file_id;
           std::string osd_uuid;
  
             try
             {
              test_interface.xtreemfs_replica_remove( context, file_id, osd_uuid );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string file_id;
           std::string osd_uuid( "bogus string" );
  
             try
             {
              test_interface.xtreemfs_replica_remove( context, file_id, osd_uuid );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string file_id( "bogus string" );
           std::string osd_uuid;
  
             try
             {
              test_interface.xtreemfs_replica_remove( context, file_id, osd_uuid );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           StringSet group_ids; group_ids.push_back( "test" ); org::xtreemfs::interfaces::Context context( "test", group_ids );
           std::string file_id( "bogus string" );
           std::string osd_uuid( "bogus string" );
  
             try
             {
              test_interface.xtreemfs_replica_remove( context, file_id, osd_uuid );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_restore_file()
           {
            {
           std::string file_path;
           std::string file_id;
           uint64_t file_size = 0;
           std::string osd_uuid;
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id;
           uint64_t file_size = 0;
           std::string osd_uuid;
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id;
           uint64_t file_size = 0;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id;
           uint64_t file_size = 0;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id;
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid;
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id;
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid;
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id;
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id;
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id( "bogus string" );
           uint64_t file_size = 0;
           std::string osd_uuid;
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id( "bogus string" );
           uint64_t file_size = 0;
           std::string osd_uuid;
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id( "bogus string" );
           uint64_t file_size = 0;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id( "bogus string" );
           uint64_t file_size = 0;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id( "bogus string" );
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid;
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id( "bogus string" );
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid;
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id( "bogus string" );
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path;
           std::string file_id( "bogus string" );
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id;
           uint64_t file_size = 0;
           std::string osd_uuid;
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id;
           uint64_t file_size = 0;
           std::string osd_uuid;
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id;
           uint64_t file_size = 0;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id;
           uint64_t file_size = 0;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id;
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid;
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id;
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid;
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id;
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id;
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id( "bogus string" );
           uint64_t file_size = 0;
           std::string osd_uuid;
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id( "bogus string" );
           uint64_t file_size = 0;
           std::string osd_uuid;
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id( "bogus string" );
           uint64_t file_size = 0;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id( "bogus string" );
           uint64_t file_size = 0;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id( "bogus string" );
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid;
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id( "bogus string" );
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid;
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id( "bogus string" );
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = 0;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
  
            {
           std::string file_path( "bogus string" );
           std::string file_id( "bogus string" );
           uint64_t file_size = UINT64_MAX;
           std::string osd_uuid( "bogus string" );
           int32_t stripe_size = INT32_MAX;
  
             try
             {
              test_interface.xtreemfs_restore_file( file_path, file_id, file_size, osd_uuid, stripe_size );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
           void fuzz_xtreemfs_update_file_size()
           {
            {
           org::xtreemfs::interfaces::XCap xcap;
           {
           std::string file_id;
           uint32_t access_mode = 0;
           uint64_t expires = 0;
           std::string client_identity;
           uint32_t truncate_epoch = 0;
           std::string server_signature;
            xcap.set_file_id( file_id );
            xcap.set_access_mode( access_mode );
            xcap.set_expires( expires );
            xcap.set_client_identity( client_identity );
            xcap.set_truncate_epoch( truncate_epoch );
            xcap.set_server_signature( server_signature );
           }
           org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
           {
           org::xtreemfs::interfaces::NewFileSizeSet new_file_size;
           org::xtreemfs::interfaces::OSDtoMRCDataSet opaque_data;
            osd_write_response.set_new_file_size( new_file_size );
            osd_write_response.set_opaque_data( opaque_data );
           }
  
             try
             {
              test_interface.xtreemfs_update_file_size( xcap, osd_write_response );
             }
             catch ( std::exception& )
             { }
            }
           }
  
  
      private:
          MRCInterface& test_interface;
      };
  
    };
  
  
  
  };
  
  
  
};

#endif
