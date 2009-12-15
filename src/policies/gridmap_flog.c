#include "xtreemfs/policy.h"

#ifndef _WIN32
#include <errno.h>
#include <fcntl.h>
#include <pwd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#define PWD_BUF_LEN 256
#endif


int open_gridmap
( 
  int* out_fd, 
  char** out_gridmap, 
  char** out_gridmap_p, 
  char** out_gridmap_end 
)
{
  struct stat stbuf;

  *out_fd = open( "/etc/grid-security/grid-mapfile", O_RDONLY, 0 );
  if ( *out_fd != -1 )
  {
    if ( fstat( *out_fd, &stbuf ) != -1 )
    {
      *out_gridmap 
        = ( char* )mmap
          ( 
            0, 
            stbuf.st_size, 
            PROT_READ, 
            MAP_PRIVATE, 
            *out_fd, 
            0 
          );

      if ( *out_gridmap != 0 )
      {
        *out_gridmap_p = *out_gridmap;
        *out_gridmap_end = *out_gridmap + stbuf.st_size;
        return 0;
      }
    }
    
    close( *out_fd );
  }
      
  return -1; 
}

int get_next_gridmap_line
( 
  char** gridmap_p, 
  char* gridmap_end, 
  char** out_dn, 
  size_t* out_dn_len, 
  char** out_sn, 
  size_t* out_sn_len 
)
{
  if ( **gridmap_p == '\"' ) /* Line starts with " */
  {
    *gridmap_p = *gridmap_p + 1; /* Past the start " */
    *out_dn = *gridmap_p; 
    /* Advance the pointer until the closing " */
    while ( **gridmap_p != '\"' && *gridmap_p < gridmap_end ) 
      *gridmap_p = *gridmap_p + 1;
    if ( **gridmap_p == '\"' )
    {
      *out_dn_len = *gridmap_p - *out_dn;
      *gridmap_p += 2; /* Past the closing " and the space after it */
            
      *out_sn = *gridmap_p;
      /* Advance the pointer until the \n at the end of the line */
      while ( **gridmap_p != '\n' && *gridmap_p < gridmap_end )
        *gridmap_p = *gridmap_p + 1;
      if ( **gridmap_p == '\n' )
      {
        *out_sn_len = *gridmap_p - *out_sn;
        *gridmap_p = *gridmap_p + 1; /* Past the end \n */
        return 0;
      }
    }
  }
 
  return -1; 
}

void close_gridmap( int fd, char* gridmap, char* gridmap_end )
{
  munmap( 0, gridmap_end - gridmap ); 
  close( fd );
}


DLLEXPORT int get_passwd_from_user_credentials
( 
  const char* user_id, 
  const char* group_ids, 
  int* uid, 
  int* gid 
)
{
#ifdef _WIN32
  *uid = *gid = 0;
  return 0;
#else
  int fd;
  char *gridmap, *gridmap_p, *gridmap_end; 

  char *dn, *sn, *sn_copy;
  size_t dn_len, sn_len;

  struct passwd pwd, *pwd_res;
  char pwd_buf[PWD_BUF_LEN]; int pwd_buf_len = sizeof( pwd_buf );

  if ( open_gridmap( &fd, &gridmap, &gridmap_p, &gridmap_end ) == 0 )
  {
    while 
    ( 
      get_next_gridmap_line
      ( 
        &gridmap_p, 
        gridmap_end, 
        &dn, 
        &dn_len, 
        &sn, 
        &sn_len 
      ) == 0 
    )
    {
      if ( strncmp( user_id, dn, dn_len ) == 0 )
      {
        sn_copy = ( char* )malloc( sn_len + 1 );
        strncpy( sn_copy, sn, sn_len );

        if 
        ( 
          getpwnam_r
          ( 
            sn_copy, 
            &pwd, 
            pwd_buf, 
            pwd_buf_len, 
            &pwd_res 
          ) == 0 && 
          pwd_res != NULL 
        )
        {
          free( sn_copy );
          *uid = pwd_res->pw_uid;
          *gid = pwd_res->pw_gid;    
          close_gridmap( fd, gridmap, gridmap_end );
          return 0;
        }
        else
          free( sn_copy );                 
     }        
   }

   close_gridmap( fd, gridmap, gridmap_end ); 
 }

 return -1;
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
  struct passwd pwd, *pwd_res;
  char pwd_buf[PWD_BUF_LEN]; int pwd_buf_len = sizeof( pwd_buf );

  int fd;
  char *gridmap, *gridmap_p, *gridmap_end; 

  char *dn, *sn;
  size_t dn_len, sn_len;

  if ( getpwuid_r( uid, &pwd, pwd_buf, pwd_buf_len, &pwd_res ) == 0 )
  {
    if ( pwd_res != NULL && pwd_res->pw_name != NULL )
    {
      if ( open_gridmap( &fd, &gridmap, &gridmap_p, &gridmap_end ) == 0 )
      {
        while 
        ( 
          get_next_gridmap_line
          ( 
            &gridmap_p, 
            gridmap_end, 
            &dn, &dn_len, 
            &sn, &sn_len 
          ) == 0
        )
        {
          if ( strncmp( sn, pwd_res->pw_name, sn_len ) == 0 )
          {
            if ( user_id == 0 )
              *user_id_len = dn_len;
            else
            {
              if ( dn_len < *user_id_len )
              {
                strncpy( user_id, dn, dn_len );
                *user_id_len = dn_len;
              }
              else
                strncpy( user_id, dn, *user_id_len );
            }

            close_gridmap( fd, gridmap, gridmap_end );

            return 0;
          }
        }

        close_gridmap( fd, gridmap, gridmap_end ); 
      }        
    }
  }

  return -1;
}
