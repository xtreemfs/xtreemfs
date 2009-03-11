#ifndef _90452076755_H
#define _90452076755_H

#include "yield/arch.h"

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
  
        virtual YIELD::Stat fgetattr() = 0;
        virtual void ftruncate( uint64_t new_size, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) = 0;
        virtual size_t read( char * rbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) = 0;
        virtual size_t write( const char * wbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) = 0;
      };
  
      // Use this macro in an implementation class to get all of the prototypes for the operations in FileInterface
      #define ORG_XTREEMFS_CLIENT_FILEINTERFACE_PROTOTYPES \
      virtual YIELD::Stat fgetattr();\
      virtual void ftruncate( uint64_t new_size, const org::xtreemfs::interfaces::FileCredentials& file_credentials );\
      virtual size_t read( char * rbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials );\
      virtual size_t write( const char * wbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials );
  
      // Use this macro in an implementation class to get dummy implementations for the operations in this interface
      #define ORG_XTREEMFS_CLIENT_FILEINTERFACE_DUMMY_DEFINITIONS \
      virtual YIELD::Stat fgetattr() { return YIELD::Stat(); }\
      virtual void ftruncate( uint64_t new_size, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) { }\
      virtual size_t read( char * rbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) { return 0; }\
      virtual size_t write( const char * wbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials ) { return 0; }
  
    };
  
  
  
  };
  
  

};

#endif
