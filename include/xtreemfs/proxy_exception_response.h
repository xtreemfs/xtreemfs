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


#ifndef _XTREEMFS_PROXY_EXCEPTION_RESPONSE_H_
#define _XTREEMFS_PROXY_EXCEPTION_RESPONSE_H_

#include "yield.h"


namespace xtreemfs
{
  class ProxyExceptionResponse : public yield::concurrency::ExceptionResponse
  {
  public:
    virtual ~ProxyExceptionResponse() throw()
    { }

    virtual uint32_t get_error_code() const { return 0; }
    uint32_t get_platform_error_code() const;

    virtual const std::string& get_error_message() const
    {
      return empty_error_message;
    }

    virtual const std::string& get_stack_trace() const
    {
      return empty_stack_trace;
    }

    virtual void set_error_code( uint32_t ) { }

    operator const char*() const throw()
    {
      if ( what_str.empty() )
      {
        std::ostringstream what_oss;
        what_oss << "errno = " << get_error_code();
        what_oss << ", strerror = " << get_error_message();
        if ( !get_stack_trace().empty() )
          what_oss << ", stack = " << get_stack_trace();
        const_cast<ProxyExceptionResponse*>( this )->what_str = what_oss.str();
      }

      return what_str.c_str();
    }

  protected:
    ProxyExceptionResponse()
    { }

    ProxyExceptionResponse( const char* )
    {
      DebugBreak();
    }

  private:
    std::string empty_error_message, empty_stack_trace, what_str;
  };
};

#define ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS ::xtreemfs::ProxyExceptionResponse

#endif
