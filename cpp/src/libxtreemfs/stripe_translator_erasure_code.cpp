/*
 * Copyright (c) 2009-2011 by Tanja Baranova, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "libxtreemfs/stripe_translator_erasure_code.h"
#include "libxtreemfs/xtreemfs_exception.h"

#include <algorithm>
#include <boost/dynamic_bitset.hpp>
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
  unsigned int data_width = width - parity_width;
  char *buf_redundance = NULL;

  size_t obj_offset = offset / stripe_size;

  // check for illegal offset
  assert(offset % (stripe_size * data_width) == 0);

  size_t total_size = (size / data_width) * width;
  // object number for stripe (parity stripes are not counted)
  size_t obj_number = obj_offset;
  // actual object number to calculate the osd number
  size_t real_obj_number =  (obj_offset / data_width * width);

  size_t start = 0;
  size_t written_blocks = 0;

  while (written_blocks < total_size) {

    std::vector<size_t> osd_offsets;

    size_t osd_number = real_obj_number % width;
    osd_offsets.push_back(osd_number);

    // if partial stripe at the end of the file
    if (total_size - written_blocks < 2 * stripe_size) {
      // size of incomplete object
      size_t tail_size = size - start;

      operations->push_back(
          WriteOperation(obj_number, osd_offsets, tail_size, 0,
            buf + start));
      osd_offsets.clear();
      osd_offsets.push_back(data_width);
      if (obj_number % data_width == 0){
        // partial object at the start of the stripe
        // write data to parity object and enque par object with correct osd_offsets
        operations->push_back(
            WriteOperation(obj_number, osd_offsets, tail_size, 0,
              buf + start));
      } else {
        buf_redundance = new char[stripe_size];
        memcpy(buf_redundance, buf + start, tail_size);
        memset(buf_redundance + tail_size, 0, stripe_size - tail_size);

        // XOR all obects for the length of the incomplete object
        for (size_t i = 0; i < stripe_size; ++i) {
          for (size_t j = obj_number % width; j > 0; --j)
            buf_redundance[i] = buf[start - (j * stripe_size) + i]
              ^ buf_redundance[i];
        }
        operations->push_back(
            WriteOperation(obj_number + 1 - (obj_number % width), osd_offsets, stripe_size, 0,
              buf_redundance, true));
        // full objects have been written in this line
        // create parity object and enque at correct position..remember N+1 striping
      }
      return;
    }

    //is it a parity chunk?
    if (osd_number >= data_width) {
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
          WriteOperation(obj_number - data_width, osd_offsets, stripe_size, 0,
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
  // stripe size is stored in kB
  unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of OSD to distribute parity chunks
  unsigned int parity_width =(*policies.begin())->parity_width();
  // number of OSD to distribute chunks
  unsigned int width =(*policies.begin())->width();
  unsigned int data_width = width - parity_width;
  char *buf_redundance = NULL;

  size_t obj_offset = offset / stripe_size;

  size_t total_size = (size / data_width) * width;
  // object number for stripe (parity stripes are not counted)
  size_t obj_number = obj_offset;
  // actual object number to calculate the osd number
  size_t real_obj_number =  (obj_offset / data_width * width);
  size_t par_reads = 0;

  size_t start = 0;
  size_t read_blocks = 0;

  while (read_blocks < total_size) {

    std::vector<size_t> osd_offsets;

    size_t osd_number = real_obj_number % (width);
    osd_offsets.push_back(osd_number);

    // if partial stripe at the end of the file
    if (total_size - read_blocks < 2 * stripe_size) {
      // size of incomplete object
      size_t tail_size = size - start;

      operations->insert(operations->begin() + par_reads,
          ReadOperation(obj_number, osd_offsets, tail_size, 0,
            buf + start));
      osd_offsets.clear();
      osd_offsets.push_back(data_width);
      if (obj_number % data_width == 0){
        // partial object at the start of the stripe
        // write data to parity object and enque par object with correct osd_offsets
        buf_redundance = new char[tail_size];
        operations->push_back(
            ReadOperation(obj_number, osd_offsets, tail_size, 0,
              buf_redundance));
      } else {
        buf_redundance = new char[stripe_size];
        operations->push_back(
            ReadOperation(obj_number + 1 - (obj_number % width), osd_offsets, stripe_size, 0,
              buf_redundance));
        // full objects have been written in this line
        // create parity object and enque at correct position..remember N+1 striping
      }
      return obj_number + 1;
    }

    //is it a parity chunk?
    if (osd_number >= data_width) {
      buf_redundance = new char[stripe_size];
      // TODO fix comment here
      /* use the last object number used for a data stripe
       * this caters towards the calculation of a correct  filesize
       * ...and hopefully nothing else */
      operations->push_back(
          ReadOperation(obj_number - data_width, osd_offsets, stripe_size, 0,
              buf_redundance));
    } else {
      // operations->push_back(
      operations->insert(operations->begin() + par_reads,
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

// TODO free all redundance buffers here!!!
void StripeTranslatorErasureCode::ProcessReads(
    std::vector<ReadOperation>* operations,
    boost::dynamic_bitset<>* successful_reads,
    PolicyContainer policies) const {
  // stripe size is stored in kB
  unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of OSD to distribute parity chunks
  unsigned int parity_width = (*policies.begin())->parity_width();
  // number of OSD to distribute chunks
  unsigned int width = (*policies.begin())->width();
  // number of data stripes
  unsigned int data_width = width - parity_width;
  // how many whole lines does this operation contain
  unsigned int lines = operations->size() / width;

  assert(operations->size() == successful_reads->size());

  // TODO check if only data stripes have been read and if so return immediately

  for (size_t i = 0; i < lines; i++) {

    // create successful_reads bitvector for each line (data stripes + parity stripes)
    boost::dynamic_bitset<> line_mask(width);
    // normal reads
    for (size_t j = 0; j < data_width; j++)
      line_mask[j] = successful_reads->test(j + i * data_width);
    // parity reads
    for (size_t j = 0; j < parity_width; j++)
      // parity reads are at the end of the successful_reads vector because they are read iff normal
      // reads ahve failed
      line_mask[data_width + j] = successful_reads->test(lines * data_width + i * parity_width + j);

    // check is enough data exists...at least data_width stripes must be present
    if (line_mask.count() < data_width)
      throw IOException("to many failed reads");

    // if all data stripes have been read there is nothing todo
    if((line_mask << parity_width).count() == data_width){
      continue;
    }

    char buf[stripe_size];
    memset(buf, 0, stripe_size);

    // fix data by XORing all successfully read stripes
    for (size_t j = 0; j < data_width; j++){
      if (line_mask[j]){
        char *d = (*operations)[j + i * data_width].data;
        for (int k = 0; k < stripe_size; k++){
          buf[k] = buf[k] ^ d[k];
        }
      }
    }
    for (size_t j = 0; j < parity_width; j++) {
      if (line_mask[data_width + j]) {
        char *d = (*operations)[lines * data_width + i * parity_width + j].data;
        for (int k = 0; k < stripe_size; k++){
          buf[k] = buf[k] ^ d[k];
        }
      }
    }

    // copy fixed data to correct location
    for (size_t j = 0; j < data_width; j++){
      if (!line_mask[j]) {
        char *d = (*operations)[j + i * data_width].data;
        for (int k = 0; k < stripe_size; k++){
          d[k] = buf[k];
        }
      }
    }
  }
  // free parity buffers
  for (size_t i = lines * data_width; i < operations->size(); i++)
    delete (*operations)[i].data;
}

}  // namespace xtreemfs
