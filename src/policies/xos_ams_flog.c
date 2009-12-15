#include "xtreemfs/policy.h"

#include <string.h>

#ifndef _WIN32
#include <errno.h>
#include <xos_ams.h>
#include <xos_protocol.h>
#endif


DLLEXPORT int get_passwd_from_user_credentials
( 
  const char* user_id, 
  const char* group_ids, 
  int* uid, 
  int* gid 
)
{
#ifdef _WIN32
  uid = gid = 0;
  return 0;
#else
  int ams_fd;
  AMS_GPASSWD gpwd;
  AMS_GGROUPS ggrp;

  ams_fd = amsclient_connect_open();
  if ( ams_fd != -1 )
  {
    if 
    ( 
      amsclient_mappinginfo_internal
      ( 
        ams_fd,
        user_id, 
        group_ids, 
        group_ids, 
        &gpwd, 
        &ggrp 
      ) >= 0 
    )
    {
      *uid = gpwd.l_idtoken.g_mappeduid;
      *gid = gpwd.l_idtoken.g_mappedgid;
      return 0;
    }
    else
      return -1 * errno;
  }
  else
    return -1 * errno;
#endif
}

DLLEXPORT int get_user_credentials_from_passwd
( 
  int uid, 
  int gid, 
  char* user_id, 
  size_t* user_id_len, 
  char* group_ids, 
  size_t* group_ids_len 
)
{
  const char *ams_user_id, *ams_group_id;
  size_t ams_user_id_len, ams_group_id_len;
  
#ifdef _WIN32
  ams_user_id = "ams_user_id";
  ams_group_id = "ams_group_id";
#else
  int ams_fd;
  AMS_GPASSWD gpwd;
  AMS_GGROUPS ggrp;
  
  ams_fd = amsclient_connect_open();
  if ( ams_fd != -1 )
  {
    if 
    ( 
      amsclient_invmappinginfo_internal
      ( 
        ams_fd, 
        NULL, 
        uid, 
        NULL, 
        gid, 
        &gpwd, 
        &ggrp 
      ) == 0 
    )
    {
      ams_user_id = gpwd.g_idtoken.g_dn;
      ams_group_id = ggrp.g_grptoken.g_vo;
    }
    else
      return -1 * errno;
  }
  else
    return -1 * errno;
#endif

  ams_user_id_len = strlen( ams_user_id ) + 1;
  if ( user_id == NULL )
    *user_id_len = ams_user_id_len;
  else
  {
    if ( *user_id_len > ams_user_id_len )
      *user_id_len = ams_user_id_len;
    strncpy( user_id, ams_user_id, *user_id_len );
  }

  ams_group_id_len = strlen( ams_group_id ) + 1;
  if ( group_ids == NULL )
    *group_ids_len = ams_group_id_len;
  else
  {
    if ( *group_ids_len > ams_group_id_len )
      *group_ids_len = ams_group_id_len;
    strncpy( group_ids, ams_group_id, *group_ids_len );
  }

  return 0;
}

