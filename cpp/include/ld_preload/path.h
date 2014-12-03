/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef PRELOAD_PATH_H_
#define PRELOAD_PATH_H_

#include <string>

class Path {
 public:
  Path(const char* pathname);

  bool IsXtreemFS();
  void Parse();
  const char* GetXtreemFSPath();
  static void SetXtreemFSPrefix(const std::string& prefix);

private:
  static std::string& GetXtreemFSPrefix();

  const char* pathname_;
  const char* xtreemfs_path_;
};

#endif  // PRELOAD_PATH_H_
