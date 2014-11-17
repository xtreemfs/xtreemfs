/*
 * Copyright (c) 2009-2011 by Tanja Baranova, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "libxtreemfs/stripe_translator_erasure_code.h"

#include <algorithm>
#include <vector>

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
  // object number for stripe (parity stripes are not counted)
  size_t obj_number = 0;
  // actual object number to calculate the osd number
  size_t real_obj_number = 0;

  size_t start = 0;
  size_t written_blocks = 0;

  while (written_blocks < total_size) {

    std::vector<size_t> osd_offsets;

    size_t osd_number = real_obj_number % width;
    osd_offsets.push_back(osd_number);

    //is it a parity chunk?
    if (osd_number >= width - parity_width) {
      buf_redundance = new char[stripe_size];

      for (size_t i = 0; i < stripe_size; ++i) {
        for (size_t j = width; j > 1; --j)
        buf_redundance[i] = buf[start - (j * stripe_size) + i]
            ^ buf[start - ((j-1) * stripe_size) + i];
      }

      /* use the last object number used for a data stripe
       * this caters towards the calculation of a correct  filesize
       * ...and hopefully nothing else */
      operations->push_back(
          WriteOperation(obj_number - 1, osd_offsets, stripe_size, 0,
              buf_redundance, true));
    } else {
      operations->push_back(
          WriteOperation(obj_number, osd_offsets, stripe_size, 0,
              buf + start));

      start += stripe_size;
      obj_number++;
    }
    real_obj_number++;
    written_blocks += stripe_size;
  }
}

size_t StripeTranslatorErasureCode::TranslateReadRequest(
    char *buf,
    size_t size,
    int64_t offset,
    PolicyContainer policies,
    std::vector<ReadOperation>* operations) const {
  std::vector<ReadOperation>::iterator par_reads = operations->end();
  // stripe size is stored in kB
  unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of OSD to distribute parity chunks
  unsigned int parity_width =(*policies.begin())->parity_width();
  // number of OSD to distribute chunks
  unsigned int width =(*policies.begin())->width();
  char *buf_redundance = NULL;

  size_t total_size = (size / (width - parity_width)) * width;
  size_t obj_number = 0;
  size_t real_obj_number = 0;

  size_t start = 0;
  size_t read_blocks = 0;

  while (read_blocks < total_size) {

    std::vector<size_t> osd_offsets;

    size_t osd_number = real_obj_number % (width);
    osd_offsets.push_back(osd_number);

    //is it a parity chunk?
    if (osd_number >= width - parity_width) {
      buf_redundance = new char[stripe_size];
      /* use the last object number used for a data stripe
       * this caters towards the calculation of a correct  filesize
       * ...and hopefully nothing else */
      operations->push_back(
          ReadOperation(obj_number - 1, osd_offsets, stripe_size, 0,
              buf_redundance));
    } else {
      par_reads = operations->insert(par_reads,
          ReadOperation(obj_number, osd_offsets, stripe_size, 0,
              buf + start));
      par_reads++;
      start += stripe_size;
      obj_number++;
    }
    read_blocks += stripe_size;
    real_obj_number++;
  }
  return obj_number;
}

}  // namespace xtreemfs
