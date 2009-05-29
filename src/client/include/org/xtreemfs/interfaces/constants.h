// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_INTERFACES_CONSTANTS_H_
#define _ORG_XTREEMFS_INTERFACES_CONSTANTS_H_

#include <string>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      const static char* ONCRPC_SCHEME = "oncrpc";
      const static char* ONCRPCS_SCHEME = "oncrpcs";
      const static char* ONCRPCU_SCHEME = "oncrpcu";
      const static uint32_t ONCRPC_AUTH_FLAVOR = 1326;
      const static char* REPL_UPDATE_PC_NONE = "";
      const static char* REPL_UPDATE_PC_RONLY = "ronly";
      const static uint32_t SYSTEM_V_FCNTL_H_O_RDONLY = 0x0000;
      const static uint32_t SYSTEM_V_FCNTL_H_O_WRONLY = 0x0001;
      const static uint32_t SYSTEM_V_FCNTL_H_O_RDWR = 0x0002;
      const static uint32_t SYSTEM_V_FCNTL_H_O_APPEND = 0x0008;
      const static uint32_t SYSTEM_V_FCNTL_H_O_CREAT = 0x0100;
      const static uint32_t SYSTEM_V_FCNTL_H_O_TRUNC = 0x0200;
      const static uint32_t SYSTEM_V_FCNTL_H_O_EXCL = 0x0400;
      const static uint32_t SYSTEM_V_FCNTL_H_O_SYNC = 0x0010;
      const static uint32_t SYSTEM_V_FCNTL_H_S_IFREG = 0x8000;
      const static uint32_t SYSTEM_V_FCNTL_H_S_IFDIR = 0x4000;
      const static uint32_t SYSTEM_V_FCNTL_H_S_IFLNK = 0xA000;


    };



  };



};
#endif
