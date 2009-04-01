#ifndef ORG_XTREEMFS_CLIENT_PLATFORM_EXCEPTION_EVENT_H
#define ORG_XTREEMFS_CLIENT_PLATFORM_EXCEPTION_EVENT_H

#include "yield.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class PlatformExceptionEvent : public YIELD::ExceptionEvent
      {
      public:
        PlatformExceptionEvent() : YIELD::ExceptionEvent( YIELD::PlatformException::errno_() ) { }
        PlatformExceptionEvent( unsigned long error_code ) : YIELD::ExceptionEvent( error_code ) { }
        virtual ~PlatformExceptionEvent() throw() { }

        // RTTI
        TYPE_INFO( EXCEPTION_EVENT, "PlatformExceptionEvent", 2506799510UL )

        // ExceptionEvent
        virtual ExceptionEvent* clone() const { return new PlatformExceptionEvent( error_code ); }
        virtual void throwStackClone() const { throw PlatformExceptionEvent( error_code ); }

        // std::exception
        virtual const char* what() const throw()
        {
          YIELD::PlatformException::strerror( error_code, ( char* )what_buffer, YIELD_PLATFORM_PLATFORM_EXCEPTION_WHAT_BUFFER_LENGTH - 1 );
          return what_buffer;
        }

      private:
        char what_buffer[YIELD_PLATFORM_PLATFORM_EXCEPTION_WHAT_BUFFER_LENGTH];
      };
    };
  };
};

#endif
