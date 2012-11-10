/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "cbfs/cbfs_enumeration_context.h"

#include "xtreemfs/MRC.pb.h"

namespace xtreemfs {

CbFSEnumerationContext::CbFSEnumerationContext()
    : offset(0), dir_entries(NULL), next_index(0) {}

CbFSEnumerationContext::~CbFSEnumerationContext() {
  delete dir_entries;
}

}  // namespace xtreemfs