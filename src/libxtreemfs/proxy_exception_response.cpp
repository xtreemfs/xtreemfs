// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


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
