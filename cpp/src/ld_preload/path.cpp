/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "ld_preload/path.h"

#include <cstring>

#include "ld_preload/passthrough.h"

Path::Path(const char* pathname) : pathname_(pathname), xtreemfs_path_(NULL) {
}

void Path::SetXtreemFSPrefix(const std::string& prefix) {
  GetXtreemFSPrefix() = prefix;
}

bool Path::IsXtreemFS() {
  xprintf("Debug: IsXtreemFS: pathname=%s, prefix=%s\n", pathname_, GetXtreemFSPrefix().c_str());
  return strstr(pathname_, GetXtreemFSPrefix().c_str()) == pathname_;
}

std::string& Path::GetXtreemFSPrefix() {
  static std::string xtreemFSPrefix;
  return xtreemFSPrefix;
}

void Path::Parse() {
  xtreemfs_path_ = &pathname_[GetXtreemFSPrefix().size()];
  xprintf("Info: Path::Parse(): path=%s\n", xtreemfs_path_);
}

const char* Path::GetXtreemFSPath() {
  return xtreemfs_path_;
}
