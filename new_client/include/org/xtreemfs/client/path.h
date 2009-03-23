#ifndef ORG_XTREEMFS_CLIENT_XTREEMFS_PATH_H
#define ORG_XTREEMFS_CLIENT_XTREEMFS_PATH_H

#include "yield.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Path
      {
      public:
        Path() { }
        Path( const std::string& volume_name, const YIELD::Path& local_path );
        Path( const std::string& global_path );

        operator const std::string&() const { return getGlobalPath(); }
        operator const char*() const { return getGlobalPath().c_str(); }

        // strings are all in UTF-8
        const std::string& getVolumeName() const { return volume_name; }
        const YIELD::Path& getLocalPath() const { return local_path; }
        const std::string& getGlobalPath() const { return global_path; } // Volume + /-separated local path

      private:
        std::string volume_name;
        std::string global_path; 
        YIELD::Path local_path;
      };
    };
  };
};

#endif
