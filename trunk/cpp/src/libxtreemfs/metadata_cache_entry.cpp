/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/metadata_cache_entry.h"
#include "xtreemfs/MRC.pb.h"

namespace xtreemfs {

MetadataCacheEntry::MetadataCacheEntry()
    : dir_entries(NULL), stat(NULL), xattrs(NULL) {}

MetadataCacheEntry::~MetadataCacheEntry() {
  delete dir_entries;
  delete stat;
  delete xattrs;
}

}  // namespace xtreemfs
