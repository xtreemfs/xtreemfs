#ifndef ORG_XTREEMFS_CLIENT_POLICY_H
#define ORG_XTREEMFS_CLIENT_POLICY_H

#ifndef DLLEXPORT
#if defined(_MSC_VER)
#define DLLEXPORT extern "C" __declspec(dllexport)
#elif defined(YIELD_HAVE_GCCVISIBILITIYPATCH) || ( defined(__GNUC__) && __GNUC__ >= 4 )
#define DLLEXPORT extern "C" __attribute__ ( ( visibility( "default" ) ) )
#else
#define DLLEXPORT extern "C"
#endif
#endif


typedef int ( *get_user_credentials_t )( int caller_uid, int caller_gid, char** out_user_id, char*** out_group_ids );

#endif
