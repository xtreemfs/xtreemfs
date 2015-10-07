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
    int64_t offset,
    PolicyContainer policies,
    std::vector<WriteOperation>* operations) const {
  // stripe size is stored in kB
  unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;

  size_t start = 0;
  while (start < size) {
    size_t obj_number = static_cast<size_t>(start + offset) / stripe_size;
    size_t req_offset = (start + offset) % stripe_size;
    size_t req_size
      = min(size - start, static_cast<size_t>(stripe_size - req_offset));

    std::vector<size_t> osd_offsets;
    for (PolicyContainer::iterator i = policies.begin();
         i != policies.end();
         ++i) {
      osd_offsets.push_back(obj_number % ((*i)->width() - (*i)->parity_width()));
    }

    operations->push_back(WriteOperation(
        obj_number, osd_offsets, req_size, req_offset, buf + start));

    start += req_size;
  }
}

void StripeTranslatorRaid0::TranslateReadRequest(
    char *buf,
    size_t size,
    int64_t offset,
    PolicyContainer policies,
    std::vector<ReadOperation>* operations) const {
  // stripe size is stored in kB
  unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;

  size_t start = 0;
  while (start < size) {
    size_t obj_number = static_cast<size_t>(start + offset) / stripe_size;
    size_t req_offset = (start + offset) % stripe_size;
    size_t req_size
      = min(size - start, static_cast<size_t>(stripe_size - req_offset));

    std::vector<size_t> osd_offsets;
    for (PolicyContainer::iterator i = policies.begin();
         i != policies.end();
         ++i) {
      osd_offsets.push_back(obj_number % ((*i)->width() - (*i)->parity_width()));
    }

    operations->push_back(ReadOperation(
        obj_number, osd_offsets, req_size, req_offset, buf + start));

    start += req_size;
  }
}

}  // namespace xtreemfs
