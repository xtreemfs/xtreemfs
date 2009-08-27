#include "xtreemfs/policy.h"


DLLEXPORT int get_osd_ping_interval_s( const char* osd_uuid )
{
  return 5;
}

DLLEXPORT int select_file_replica( const char* file_id, int access_mode, struct file_replica_t* file_replicas, size_t file_replicas_len )
{
  return 0;
}
