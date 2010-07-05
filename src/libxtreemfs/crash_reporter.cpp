// Copyright (c) 2010 NEC HPC Europe
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
// DISCLAIMED. IN NO EVENT SHALL NEC HPC Europe BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "xtreemfs/crash_reporter.h"
using namespace xtreemfs;

#include "yield.h"
using yield::ipc::HTTPClient;

#ifdef XTREEMFS_HAVE_GOOGLE_BREAKPAD

#if defined(_WIN32)
#include "client/windows/handler/exception_handler.h"
#elif defined(__linux)
#include "client/linux/handler/exception_handler.h"
#endif


#if defined(_WIN32)
static bool
MinidumpCallback
(
  const wchar_t* dump_path,
  const wchar_t* minidump_id,
  void* context,
  EXCEPTION_POINTERS*,
  MDRawAssertionInfo*,
  bool succeeded
)
{
  return static_cast<CrashReporter*>( context )->
    MinidumpCallback( dump_path, minidump_id, succeeded );
}
#elif defined(__linux)
static bool MinidumpCallback
(
  const char *dump_path,
  const char *minidump_id,
  void* context,
  bool succeeded
)
{
  return static_cast<CrashReporter*>( context )->
    MinidumpCallback( dump_path, minidump_id, succeeded );
}
#else
#error
#endif


CrashReporter::CrashReporter( Log* log, const URI& put_crash_dump_uri )
  : log( yidl::runtime::Object::inc_ref( log ) ),
    put_crash_dump_uri( put_crash_dump_uri )
{
  exception_handler =
    new google_breakpad::ExceptionHandler
    (
      Path( "." ) + Path::SEPARATOR,
      NULL,
      ::MinidumpCallback,
      this,
#if defined(_WIN32)
      google_breakpad::ExceptionHandler::HANDLER_ALL
#elif defined(__linux)
      true
#endif
    );
}

bool
CrashReporter::MinidumpCallback
(
  const Path& dump_path,
  const Path& minidump_id,
  bool succeeded
)
{
  if ( succeeded )
  {
    Path dump_file_name( minidump_id );
    dump_file_name =
      static_cast<const string&>( dump_file_name ) + ".dmp";

    Path dump_file_path( dump_path );
    dump_file_path = dump_file_path + dump_file_name;

    string put_crash_dump_uri( put_crash_dump_uri );
#ifdef _WIN32
    put_crash_dump_uri += dump_file_name;
#else
    put_crash_dump_uri += static_cast<const string&>( dump_file_name );
#endif

    if ( log != NULL )
    {
      log->get_stream( Log::LOG_EMERG ) <<
        "CrashReporter: crashed on unknown exception, dumping to "
        << dump_file_path << " and trying to send to " << put_crash_dump_uri;
    }

    try
    {
      HTTPClient::PUT( put_crash_dump_uri, dump_file_path );
    }
    catch ( std::exception& exc )
    {
      if ( log != NULL )
      {
        log->get_stream( Log::LOG_EMERG ) <<
          "CrashReporter: exception trying to send dump to the server: "
          << exc.what();
      }
    }
  }

  return succeeded;
}

#endif
