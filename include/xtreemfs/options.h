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


#ifndef _XTREEMFS_OPTIONS_H_
#define _XTREEMFS_OPTIONS_H_

#include "yield.h"


namespace xtreemfs
{
  using yield::ipc::SSLContext;
  using yield::ipc::URI;
  using yield::platform::OptionParser;
  using yield::platform::Time;


  class Options : public vector<OptionParser::ParsedOption>
  {
  public:
    virtual ~Options();

    const std::string& get_log_file_path() const { return log_file_path; }
    const std::string& get_log_level() const { return log_level; }
    const vector<string>& get_positional_arguments() const;
    uint32_t get_proxy_flags() const { return proxy_flags; }
    SSLContext* get_ssl_context() const { return ssl_context; }
    const Time& get_timeout() const { return timeout; }
    URI* get_uri() const { return uri; }

    static Options parse( int argc, char** argv );
    static Options parse( int argc, char** argv, OptionParser& );

  private:
    Options
    (
      const string& log_file_path,
      const string& log_level,
      const vector<OptionParser::ParsedOption>& parsed_options,
      const vector<string>& positional_arguments,
      uint32_t proxy_flags,
      SSLContext* ssl_context,
      const Time& timeout,
      URI* uri
    );

  private:
    string log_file_path, log_level;
    uint32_t proxy_flags;
    Time timeout;
    SSLContext* ssl_context;
    URI* uri;
    vector<string> positional_arguments;
  };
};

#endif
