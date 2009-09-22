// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_PATH_H_
#define _XTREEMFS_PATH_H_

#include "yield.h"


namespace xtreemfs
{
  class Path
  {
  public:
    Path() { }
    Path( const std::string& volume_name, const YIELD::platform::Path& local_path );
    Path( const std::string& global_path );

    operator const std::string&() const { return get_global_path(); }
    operator const YIELD::platform::Path&() const { return get_local_path(); }
    operator const char*() const { return get_global_path().c_str(); }
    // strings are all in UTF-8
    const std::string& get_volume_name() const { return volume_name; }
    const YIELD::platform::Path& get_local_path() const { return local_path; }
    const std::string& get_global_path() const { return global_path; } // Volume + /-separated local path
    size_t size() const { return get_global_path().size(); }

  private:
    std::string volume_name;
    YIELD::platform::Path local_path;
    std::string global_path;
  };
};

#endif
