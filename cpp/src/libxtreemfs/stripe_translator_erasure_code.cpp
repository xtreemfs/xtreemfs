/*
 * Copyright (c) 2009-2011 by Tanja Baranova, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "libxtreemfs/stripe_translator_erasure_code.h"

#include <algorithm>
#include <vector>
#include <boost/scoped_array.hpp>

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

void StripeTranslatorErasureCode::TranslateWriteRequest(
    const char *buf,
    size_t size,
    int64_t offset,
    PolicyContainer policies,
    std::vector<WriteOperation>* operations) const {
  // stripe size is stored in kB
  //unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;
  unsigned int stripe_size = (*policies.begin())->stripe_size()*16;
  // number of OSD to distribute parity chunks
  unsigned int parity_width =(*policies.begin())->parity_width();
  // number of OSD to distribute chunks
  unsigned int width =(*policies.begin())->width();
  //std::vector<char> buf_redundance;
  //buf_redundance.resize(stripe_size);
  char *buf_redundance = NULL;

  size_t total_size=size+(size/width);

  size_t start = 0;
  size_t global_obj_number = 0;
  size_t written_blocks = 0;

  while (written_blocks < total_size) {
    size_t obj_number = max(global_obj_number,
        static_cast<size_t>(start + offset) / stripe_size);
    size_t req_offset = (start + offset) % stripe_size;
    size_t req_size = min(size - start,
        static_cast<size_t>(stripe_size - req_offset));

    bool parity_block = false;

    std::vector<size_t> osd_offsets;
    for (PolicyContainer::iterator i = policies.begin(); i != policies.end();
        ++i) {

      size_t osd_number = obj_number % ((*i)->width() + parity_width);
      osd_offsets.push_back(osd_number);

      //is it a parity chunk?
      if (osd_number == (*i)->width() + parity_width - 1) {
        buf_redundance = new char[stripe_size];

        for (size_t i = 0; i < stripe_size; ++i) {
          for (size_t j = width; j > 1; --j)
          buf_redundance[i] = buf[start - (j * stripe_size) + i]
              ^ buf[start - ((j-1) * stripe_size) + i];
        }

        operations->push_back(
            WriteOperation(obj_number, osd_offsets, stripe_size, 0,
                buf_redundance, true));
        global_obj_number = obj_number + 1;

        parity_block = true;

      }

    }

    if (!parity_block) {
      operations->push_back(
          WriteOperation(obj_number, osd_offsets, req_size, req_offset,
              buf + start));

      start += req_size;
    }
    written_blocks += stripe_size;

  }
}

void StripeTranslatorErasureCode::TranslateReadRequest(
    char *buf,
    size_t size,
    int64_t offset,
    PolicyContainer policies,
    std::vector<ReadOperation>* operations) const {
  // stripe size is stored in kB

  //unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;
  unsigned int stripe_size = (*policies.begin())->stripe_size()*16;
  // number of OSD to distribute parity chunks
  unsigned int parity_width =(*policies.begin())->parity_width();
  // number of OSD to distribute chunks
  unsigned int width =(*policies.begin())->width();

  size_t total_size=size+(size/width);;

  size_t start = 0;
  size_t global_obj_number = 0;
  size_t read_blocks = 0;

  while (read_blocks < total_size) {
    size_t obj_number = max(global_obj_number,
        static_cast<size_t>(start + offset) / stripe_size);
    size_t req_offset = (start + offset) % stripe_size;
    size_t req_size = min(size - start,
        static_cast<size_t>(stripe_size - req_offset));

    bool parity_block = false;

    std::vector<size_t> osd_offsets;
    for (PolicyContainer::iterator i = policies.begin(); i != policies.end();
        ++i) {

      size_t osd_number = obj_number % ((*i)->width() + parity_width);
      osd_offsets.push_back(osd_number);

      //is it a parity chunk?
      if (osd_number == (*i)->width() + parity_width - 1) {
        parity_block = true;
        start += req_size;
      }

      // for now simply read the stripes. if one stripe is not available parity info needs
      // to be used to reconstruct missing stripe
      if (!parity_block) {
        operations->push_back(
            ReadOperation(obj_number, osd_offsets, req_size, req_offset,
              buf + start));

        start += req_size;
      }
      read_blocks += stripe_size;
    }
  }
}

}  // namespace xtreemfs
