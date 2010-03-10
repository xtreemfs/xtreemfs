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

    // TODO: URI needs a std::string operator
    string put_crash_dump_uri; //( put_crash_dump_uri );
    put_crash_dump_uri += dump_file_name;

    if ( log != NULL )
    {
      log->get_stream( Log::LOG_EMERG ) <<
        "CrashReporter: crashed on unknown exception, dumping to "
        << dump_file_path << " and trying to send to " << put_crash_dump_uri;
    }

    try
    {
      HTTPClient::PUT( put_crash_dump_uri, dump_file_path, log );
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