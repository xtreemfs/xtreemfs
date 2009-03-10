#ifndef ORG_XTREEMFS_CLIENT_XTREEMFS_PATH_H
#define ORG_XTREEMFS_CLIENT_XTREEMFS_PATH_H

#include "yield/arch.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Path : public YIELD::SerializableString
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

        // RTTI
        TYPE_INFO( RTTI::STRING, "xtreemfs::client::Path", 921263543UL );

        // Serializable
        virtual void deserialize( YIELD::StructuredInputStream& );
        virtual size_t getSize() const { return getGlobalPath().size(); }

        // SerializableString
        virtual const char* getString() const { return getGlobalPath().c_str(); }

      private:
        std::string volume_name;
        std::string global_path; 
        YIELD::Path local_path;

        void init( const std::string& global_path ); // Also used by deserialize
      };
    };
  };
};

#endif
