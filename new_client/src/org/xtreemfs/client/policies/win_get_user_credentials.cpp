#include "org/xtreemfs/client/policy.h"

#ifdef _WIN32

#define UNICODE
#include <windows.h>
#include <lm.h>
#pragma comment( lib, "Netapi32.lib" )


DLLEXPORT int get_user_credentials( int, int, char** user_id, char*** group_ids )
{
   DWORD dwLevel = 1;
   LPWKSTA_USER_INFO_1 user_info = NULL;
   if ( NetWkstaUserGetInfo( NULL, dwLevel, (LPBYTE *)&user_info ) == NERR_Success )
   {
     if ( user_info !=NULL )
     {
       size_t username_wcslen = wcslen( user_info->wkui1_username );   
       size_t username_strlen = WideCharToMultiByte( GetACP(), 0, user_info->wkui1_username, username_wcslen, NULL, 0, 0, NULL );
       *user_id = ( char* )malloc( username_strlen+1 );
       WideCharToMultiByte( GetACP(), 0, user_info->wkui1_username, username_wcslen, *user_id, username_strlen+1, 0, NULL );
       ( *user_id )[username_strlen] = 0;

       *group_ids = ( char** )malloc( sizeof( char* ) * 2 );
       size_t logon_domain_wcslen = wcslen( user_info->wkui1_logon_domain );
       size_t logon_domain_strlen = WideCharToMultiByte( GetACP(), 0, user_info->wkui1_logon_domain, logon_domain_wcslen, NULL, 0, 0, NULL );
       ( *group_ids )[0] = ( char* )malloc( logon_domain_strlen + 1 );
       WideCharToMultiByte( GetACP(), 0, user_info->wkui1_logon_domain, logon_domain_wcslen, ( *group_ids )[0], logon_domain_strlen+1, 0, NULL );
       ( *group_ids )[0][logon_domain_strlen] = 0;
       ( *group_ids )[1] = NULL;       

       NetApiBufferFree( user_info );

       return 0;
     }
   }

   return -1;
}

#endif
