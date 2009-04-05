// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_PROXY_EXCEPTION_EVENT_H
#define ORG_XTREEMFS_CLIENT_PROXY_EXCEPTION_EVENT_H

#include "yield.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class ProxyExceptionEvent : public YIELD::ExceptionEvent
      {
      public:
        virtual ~ProxyExceptionEvent() throw()
        { }

        virtual const std::string& get_error_message() const { return empty_error_message; }
        virtual const std::string& get_stack_trace() const { return empty_stack_trace; }

        // std::exception
        virtual const char* what() const throw()
        {
          if ( _what[0] != 0 )
            return _what;
          else
          {
#ifdef _WIN32
            _snprintf_s( ( char* )&what_buffer, YIELD_PLATFORM_PLATFORM_EXCEPTION_WHAT_BUFFER_LENGTH, _TRUNCATE,
#else
            snprintf( ( char* )&what_buffer, YIELD_PLATFORM_PLATFORM_EXCEPTION_WHAT_BUFFER_LENGTH,
#endif
              "ProxyExceptionEvent: error_message = \"%s\", error_code = %u", get_error_message().c_str(), get_error_code() );

            return what_buffer;
          }
        }

      protected:
        ProxyExceptionEvent()
        { }

        ProxyExceptionEvent( const char* what ) : YIELD::ExceptionEvent( what )
        { }

      private:
        std::string empty_error_message, empty_stack_trace;
        char what_buffer[YIELD_PLATFORM_PLATFORM_EXCEPTION_WHAT_BUFFER_LENGTH];
      };
    };
  };
};

#define ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS org::xtreemfs::client::ProxyExceptionEvent

#endif
