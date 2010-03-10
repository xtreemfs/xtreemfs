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

#ifndef _XTREEMFS_CRASH_REPORTER_H_
#define _XTREEMFS_CRASH_REPORTER_H_

#if defined(_WIN32)
#define XTREEMFS_HAVE_GOOGLE_BREAKPAD 1
#elif defined(__linux)
#define XTREEMFS_HAVE_GOOGLE_BREAKPAD 1
#endif

#ifdef XTREEMFS_HAVE_GOOGLE_BREAKPAD
namespace google_breakpad { class ExceptionHandler; }
#endif

#include "yield.h"


namespace xtreemfs
{
  using yield::ipc::URI;
  using yield::platform::Log;
  using yield::platform::Path;


  class CrashReporter
  {
  public:
    CrashReporter* create( const URI& put_crash_dump_uri, Log* log = NULL );
    ~CrashReporter();

#ifdef XTREEMFS_HAVE_GOOGLE_BREAKPAD
    bool MinidumpCallback( const Path&, const Path&, bool );
#endif

  private:
    CrashReporter( Log* log, const URI& put_crash_dump_uri );

  private:
#ifdef XTREEMFS_HAVE_GOOGLE_BREAKPAD
    google_breakpad::ExceptionHandler* exception_handler;
#endif
    Log* log;
    URI put_crash_dump_uri;
  };
};

#endif
