#ifndef ORG_XTREEMFS_CLIENT_PROXY_EXCEPTION_EVENT_h
#define ORG_XTREEMFS_CLIENT_PROXY_EXCEPTION_EVENT_h

#include "yield/arch.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class ProxyExceptionEvent : public YIELD::ExceptionEvent
      {
      public:
        virtual const std::string& get_stack_trace() const { return empty_stack_trace; }

      protected:
        ProxyExceptionEvent() 
        { }

        ProxyExceptionEvent( const char* what ) : YIELD::ExceptionEvent( what )
        { }       

      private:
        std::string empty_stack_trace;
      };

      typedef ProxyExceptionEvent ProxyException;
    };
  };
};

#define ORG_XTREEMFS_INTERFACES_EXCEPTION_EVENT_PARENT_CLASS org::xtreemfs::client::ProxyExceptionEvent

#endif
