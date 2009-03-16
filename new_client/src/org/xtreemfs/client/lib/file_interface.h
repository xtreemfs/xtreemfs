#ifndef _11103631976_H
#define _11103631976_H


#include "org/xtreemfs/interfaces/mrc_osd_types.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class FileInterface
      {
      public:
        virtual ~FileInterface() { }
  
        virtual YIELD::Stat getattr() = 0;
        virtual size_t read( char * rbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) = 0;
        virtual void truncate( off_t new_size, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) = 0;
        virtual size_t write( const char * wbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) = 0;
      };
  
      // Use this macro in an implementation class to get all of the prototypes for the operations in FileInterface
      #define ORG_XTREEMFS_CLIENT_FILEINTERFACE_PROTOTYPES \
      virtual YIELD::Stat getattr();\
      virtual size_t read( char * rbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials );\
      virtual void truncate( off_t new_size, const org::xtreemfs::interfaces::FileCredentials& file_credentials );\
      virtual size_t write( const char * wbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials );
  
      // Use this macro in an implementation class to get dummy implementations for the operations in this interface
      #define ORG_XTREEMFS_CLIENT_FILEINTERFACE_DUMMY_DEFINITIONS \
      virtual YIELD::Stat getattr() { return YIELD::Stat(); }\
      virtual size_t read( char * rbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) { return 0; }\
      virtual void truncate( off_t new_size, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) { }\
      virtual size_t write( const char * wbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) { return 0; }
  
    };
  
  
  
  };
  
  

};

#endif
