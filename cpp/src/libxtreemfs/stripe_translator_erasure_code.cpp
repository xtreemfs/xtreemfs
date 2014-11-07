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
  unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of OSD to distribute parity chunks
  unsigned int parity_width =(*policies.begin())->parity_width();
  // number of OSD to distribute chunks
  unsigned int width =(*policies.begin())->width();
  char *buf_redundance = NULL;

  size_t total_size = (size / (width - parity_width)) * width;
  size_t obj_number = 0;

  size_t start = 0;
  size_t written_blocks = 0;

  while (written_blocks < total_size) {

    bool parity_block = false;

    std::vector<size_t> osd_offsets;

    size_t osd_number = obj_number % (width);
    osd_offsets.push_back(osd_number);

    //is it a parity chunk?
    if (osd_number >= width - parity_width) {
      buf_redundance = new char[stripe_size];

      for (size_t i = 0; i < stripe_size; ++i) {
        for (size_t j = width; j > 1; --j)
        buf_redundance[i] = buf[start - (j * stripe_size) + i]
            ^ buf[start - ((j-1) * stripe_size) + i];
      }

      operations->push_back(
          WriteOperation(obj_number, osd_offsets, stripe_size, 0,
              buf_redundance, true));

      parity_block = true;

    }

    if (!parity_block) {
      operations->push_back(
          WriteOperation(obj_number, osd_offsets, stripe_size, 0,
              buf + start));

      start += stripe_size;
    }
    written_blocks += stripe_size;
    obj_number++;
  }
}

void StripeTranslatorErasureCode::TranslateReadRequest(
    char *buf,
    size_t size,
    int64_t offset,
    PolicyContainer policies,
    std::vector<ReadOperation>* operations) const {
  // stripe size is stored in kB
  unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of OSD to distribute parity chunks
  unsigned int parity_width =(*policies.begin())->parity_width();
  // number of OSD to distribute chunks
  unsigned int width =(*policies.begin())->width();
  char *buf_redundance = NULL;

  size_t total_size = (size / (width - parity_width)) * width;
  size_t obj_number = 0;

  size_t start = 0;
  size_t read_blocks = 0;

  while (read_blocks < total_size) {

    bool parity_block = false;

    std::vector<size_t> osd_offsets;

    size_t osd_number = obj_number % (width);
    osd_offsets.push_back(osd_number);

    //is it a parity chunk?
    if (osd_number >= width - parity_width) {
      buf_redundance = new char[stripe_size];
      // operations->push_back(
      //     ReadOperation(obj_number, osd_offsets, stripe_size, 0,
      //         buf_redundance));

      parity_block = true;

    }

    if (!parity_block) {
      operations->push_back(
          ReadOperation(obj_number, osd_offsets, stripe_size, 0,
              buf + start));

      start += stripe_size;
    }
    read_blocks += stripe_size;
    obj_number++;
  }
}

}  // namespace xtreemfs
