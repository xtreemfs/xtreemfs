// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/proxy_exception_response.h"
using namespace xtreemfs;

#ifdef _WIN32
#include <windows.h>
#endif

#include <errno.h>


uint32_t ProxyExceptionResponse::get_platform_error_code() const
{
  uint32_t error_code = get_error_code();

  switch ( error_code )
  {
#if defined(_WIN32)
    case EACCES: return ERROR_ACCESS_DENIED;
    case EEXIST: return ERROR_ALREADY_EXISTS;
    case EINVAL: return ERROR_INVALID_PARAMETER;
    case ENOENT: return ERROR_FILE_NOT_FOUND;
    case WSAETIMEDOUT: return ERROR_NETWORK_BUSY;
#elif defined(__FreeBSD__) || defined(__MACH__)
    case 11: return EAGAIN; // Not sure why they renumbered this one.
    case 39: return ENOTEMPTY; // 39 is EDESTADDRREQ on FreeBSD
    case 61: return ENOATTR; // 61 is ENODATA on Linux, 
                             // returned when an xattr is not present
#endif
    case 0: DebugBreak(); return 0;
    default: return error_code;
  }
}
