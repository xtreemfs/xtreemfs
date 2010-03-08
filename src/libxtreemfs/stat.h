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


#ifndef _LIBXTREEMFS_STAT_H_
#define _LIBXTREEMFS_STAT_H_

#include "yield.h"

namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      class Stat;
      class OSDWriteResponse;
    };
  };
};


namespace xtreemfs
{
  class Stat : public yield::platform::Stat
  {
  public:
    Stat( const Stat& );
    Stat( const org::xtreemfs::interfaces::Stat& );
    Stat( const org::xtreemfs::interfaces::OSDWriteResponse& );
    
    uint64_t get_etag() const { return etag; }
    const std::string& get_group_id() const { return group_id; }
    uint32_t get_truncate_epoch() const { return truncate_epoch; }
    const std::string& get_user_id() const { return user_id; }

    operator org::xtreemfs::interfaces::Stat() const;

    void set_etag( uint64_t etag );
    void set_group_id( const std::string& group_id ); 
    void set_truncate_epoch( uint32_t truncate_epoch );
    void set_user_id( const std::string& user_id );

    // yidl::runtime::Object
    Stat& inc_ref() { return Object::inc_ref( *this ); }

  private:
    uint64_t etag;
    std::string group_id;
    uint32_t truncate_epoch;
    std::string user_id;
  };
};

#endif
