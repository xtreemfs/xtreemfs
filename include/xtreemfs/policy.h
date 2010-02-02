// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_POLICY_H_
#define _XTREEMFS_POLICY_H_

#ifdef _WIN32
#include <stdlib.h>
#else
#include <sys/types.h> // For size_t
#endif


#ifndef DLLEXPORT
#ifdef __cplusplus
#if defined(_MSC_VER)
#define DLLEXPORT extern "C" __declspec(dllexport)
#elif defined(__GNUC__) && __GNUC__ >= 4
#define DLLEXPORT extern "C" __attribute__ ( ( visibility( "default" ) ) )
#else
#define DLLEXPORT extern "C"
#endif
#else
#if defined(_MSC_VER)
#define DLLEXPORT __declspec(dllexport)
#elif defined(__GNUC__) && __GNUC__ >= 4
#define DLLEXPORT __attribute__ ( ( visibility( "default" ) ) )
#else
#define DLLEXPORT
#endif
#endif
#endif


#ifndef _WIN32
typedef
int
( *get_passwd_from_user_credentials_t )
(
  const char* user_id,
  const char* group_ids,
  uid_t* out_uid,
  gid_t* out_gid
);

typedef
int
( *get_user_credentials_from_passwd_t )
(
  uid_t uid,
  gid_t gid,
  char* out_user_id,
  size_t* out_user_id_size,
  char* out_group_ids,
  size_t* out_group_ids_size
);
#endif

//

#endif
