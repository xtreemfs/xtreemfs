/*
 * Copyright (c) 2009-2011 by Patrick Schaefer, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "libxtreemfs/stripe_translator.h"

#include <algorithm>
#include <vector>

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

void StripeTranslatorRaid0::TranslateWriteRequest(
    const char *buf,
    size_t size,
    off_t offset,
    const StripingPolicy& policy,
    std::vector<WriteOperation>* operations) const {
  // need to know stripe size and stripe width
  unsigned int stripe_size = policy.stripe_size() * 1024;  // strip size in kB
  unsigned int osdCount = policy.width();

  size_t start = 0;
  while (start < size) {
    size_t obj_number = (start + offset) / stripe_size;
    size_t osd_offset = obj_number % osdCount;
    size_t req_offset = (start + offset) % stripe_size;
    size_t req_size
      = min(size - start, static_cast<size_t>(stripe_size - req_offset));

    operations->push_back(WriteOperation(
        obj_number, osd_offset, req_size, req_offset, buf + start));

    start += req_size;
  }
}

void StripeTranslatorRaid0::TranslateReadRequest(
    char *buf,
    size_t size,
    off_t offset,
    const StripingPolicy& policy,
    std::vector<ReadOperation>* operations) const {
  // need to know stripe size and stripe width
  unsigned int stripe_size = policy.stripe_size() * 1024;  // strip size in kB
  unsigned int osdCount = policy.width();

  size_t start = 0;
  while (start < size) {
    size_t obj_number = (start + offset) / stripe_size;
    size_t osd_offset = obj_number % osdCount;
    off_t req_offset = (start + offset) % stripe_size;
    size_t req_size
      = min(size - start, static_cast<size_t>(stripe_size - req_offset));

    operations->push_back(ReadOperation(
        obj_number, osd_offset, req_size, req_offset, buf + start));

    start += req_size;
  }
}

}  // namespace xtreemfs
