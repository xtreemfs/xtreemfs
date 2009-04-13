// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_INTERFACES_CONSTANTS_H
#define ORG_XTREEMFS_INTERFACES_CONSTANTS_H

#include <string>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      const static uint8_t ACCESS_CONTROL_POLICY_NULL = 1;
      const static uint8_t ACCESS_CONTROL_POLICY_POSIX = 2;
      const static uint8_t ACCESS_CONTROL_POLICY_VOLUME = 3;
      const static uint8_t ACCESS_CONTROL_POLICY_DEFAULT = 2;
      const static char* ONCRPC_SCHEME = "oncrpc";
      const static char* ONCRPCS_SCHEME = "oncrpcs";
      const static uint32_t ONCRPC_AUTH_FLAVOR = 1326;
      const static uint8_t OSD_SELECTION_POLICY_SIMPLE = 1;
      const static uint8_t OSD_SELECTION_POLICY_DEFAULT = 1;
      const static char* REPL_UPDATE_PC_NONE = "";
      const static char* REPL_UPDATE_PC_RONLY = "ronly";
      const static uint16_t SERVICE_TYPE_MRC = 1;
      const static uint16_t SERVICE_TYPE_OSD = 2;
      const static uint16_t SERVICE_TYPE_VOLUME = 3;
      const static uint8_t STRIPING_POLICY_RAID0 = 0;
      const static uint8_t STRIPING_POLICY_DEFAULT = 0;
      const static uint8_t STRIPING_POLICY_STRIPE_SIZE_DEFAULT = 128;
      const static uint8_t STRIPING_POLICY_WIDTH_DEFAULT = 1;
      const static uint32_t SYSTEM_V_FCNTL_H_O_RDONLY = 0x0000;
      const static uint32_t SYSTEM_V_FCNTL_H_O_WRONLY = 0x0001;
      const static uint32_t SYSTEM_V_FCNTL_H_O_RDWR = 0x0002;
      const static uint32_t SYSTEM_V_FCNTL_H_O_APPEND = 0x0008;
      const static uint32_t SYSTEM_V_FCNTL_H_O_CREAT = 0x0100;
      const static uint32_t SYSTEM_V_FCNTL_H_O_TRUNC = 0x0200;
      const static uint32_t SYSTEM_V_FCNTL_H_O_EXCL = 0x0400;
      const static uint32_t SYSTEM_V_FCNTL_H_S_IFREG = 0x8000;
      const static uint32_t SYSTEM_V_FCNTL_H_S_IFDIR = 0x4000;
      const static uint32_t SYSTEM_V_FCNTL_H_S_IFLNK = 0xA000;

      enum DebugLevel { DEBUG_LEVEL_ERROR = 0, DEBUG_LEVEL_WARN = 1, DEBUG_LEVEL_INFO = 2, DEBUG_LEVEL_DEBUG = 3, DEBUG_LEVEL_TRACE = 4, DEBUG_LEVEL_DEFAULT = 0, DEBUG_LEVEL_MAX = 4 };


    };



  };



};

#endif
