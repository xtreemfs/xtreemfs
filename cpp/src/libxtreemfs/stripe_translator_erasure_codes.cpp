/*
 * Copyright (c) 2009-2015 by Jan Fajerski, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "libxtreemfs/stripe_translator_erasure_codes.h"
#include "libxtreemfs/xtreemfs_exception.h"

#include <algorithm>
#include <boost/dynamic_bitset.hpp>
#include <vector>
#include <iostream>

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

/**
 * create all necessary write operations for a write call.
 * All writes must be aligned with full lines, so offsets must be multiples
 * of stripe_size * k.
 * All requests must also write full lines with the exception of the last line. It may also end in
 * an incomplete stripe.
 */
void StripeTranslatorErasureCodes::TranslateWriteRequest(
    const char *buf,
    size_t size,
    int64_t offset,
    PolicyContainer policies,
    std::vector<WriteOperation>* operations) const {

  /*
   * In XtreemFS a stripe refers to the chunk of data that resides on one OSD.
   * Thus k + m = n stripes make up a line.
   *
   */

  //  stripe size is stored in kB
  size_t stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of parity OSDs
  unsigned int m =(*policies.begin())->parity_width();
  // number of OSDs
  unsigned int n =(*policies.begin())->width();
  // number of data OSDs
  unsigned int k = n - m;
  size_t obj_number = 0;
  // word size for the encoder
  int w = 32;
  char *data[k];
  char *coding[m];
  size_t processed_size = 0;

  // skip as many full lines as possible
  while (offset > 0) {
    offset -= stripe_size * k;
    obj_number += k;
  }

  assert (offset == 0);

  int objects = 1 + ((size - 1) / stripe_size);
  int lines = 1 + ((objects - 1) / k);

  for (int l = 0; l < lines; l++) {
    int line_offset = l * k;
    size_t coding_size = stripe_size;
    for (int i = 0; i < k; i++) {
      if (processed_size < size) {
        int buffer_offset = (line_offset + i) * stripe_size;
        size_t req_size = min(stripe_size, size - processed_size);

        std::vector<size_t> osd_offsets;
        osd_offsets.push_back(i);

        // get 0-padded version of the data if operation is smaller then stripe_size
        if (req_size < stripe_size) {
          data[i] = new char[stripe_size];
          memcpy(data[i], buf + buffer_offset, req_size);
          memset(data[i] + req_size, 0, stripe_size - req_size);
        } else {
          data[i] = const_cast<char*>(buf + buffer_offset);
        }

        operations->push_back(
            WriteOperation(obj_number, osd_offsets, req_size, 0,
              buf + buffer_offset));
        processed_size += req_size;
        obj_number++;
        if (i == 0) {
          coding_size = req_size;
        }
      // if the write operation ends before a line was completed, fill up the line with
      // a zeroed buffers for the coding operation
      } else {
        data[i] = new char[stripe_size];
        memset(data[i], 0, stripe_size);
      }
    }

    // allocate coding buffers
    for(int i = 0; i < m; i++) {
      coding[i] = new char[stripe_size];
    }

    this->Encode(k, m, w, data, coding, stripe_size);

    for (int i = 0; i < m; i++){
      std::vector<size_t> osd_offsets;
      osd_offsets.push_back(k + i);

      if (coding_size < stripe_size) {
        // if only the first object of a line has been written to and its smaller then stripe_size
        // the coding objects must be of the same size so the filesize calculation works
        char *coded = new char[coding_size];
        memcpy(coded, coding[i], coding_size);
        delete[] coding[i];
        operations->push_back(
            WriteOperation(line_offset, osd_offsets, coding_size, 0,
              coded, true));
      } else {
        operations->push_back(
            WriteOperation(line_offset, osd_offsets, stripe_size, 0,
              coding[i], true));
      }
    }

    // delete 0-padded data copies
    for (int i = 0; i < k; i++) {
      if (operations->size() <= line_offset + i){
        delete[] data[i];
      } else {
        if ((*operations)[line_offset + i].req_size < stripe_size) {
          delete[] data[i];
        }
      }
    }
  }
}

size_t StripeTranslatorErasureCodes::TranslateReadRequest(
    char *buf,
    size_t size,
    int64_t offset,
    PolicyContainer policies,
    std::vector<ReadOperation>* operations) const {

  //  stripe size is stored in kB
  size_t stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of parity OSDs
  unsigned int m =(*policies.begin())->parity_width();
  // number of OSDs
  unsigned int n =(*policies.begin())->width();
  // number of data OSDs
  unsigned int k = n - m;
  size_t obj_number = 0;
  // index where data reads are inserted...corresponds to the minimum number of reads when no
  // erasures are present
  size_t data_reads = 0;
  size_t processed_size = 0;

  // skip as many full lines as possible
  while (offset > stripe_size * k) {
    offset -= stripe_size * k;
    obj_number += k;
  }

  int objects = 1 + ((size + offset - 1) / stripe_size);
  int lines = 1 + ((objects - 1) / k);

  for (int l = 0; l < lines; l++) {
    for (int i = 0; i < k; i++) {
      if (processed_size < size) {

        std::vector<size_t> osd_offsets;
        osd_offsets.push_back(i);

        if (offset > stripe_size) {

          // push extra read when an offset is present
          // it is necessary in case erasure are present and decoding is needed
          operations->push_back(
              ReadOperation(obj_number, osd_offsets, stripe_size, 0,
                new char[stripe_size], true));
          offset -= stripe_size;
        } else {
          size_t req_size = min(stripe_size - offset, size - processed_size);

          // push normal read op
          operations->insert(operations->begin() + data_reads,
              ReadOperation(obj_number, osd_offsets, req_size, offset,
                buf + processed_size));
          data_reads++;
          processed_size += req_size;
          // TODO theres got to be a better way then setting it to 0 every time
          offset = 0;
        }
        obj_number++;
      }
    }

    int coding_obj_number = (obj_number - 1) - ((obj_number - 1) % k);
    for(int i = 0; i < m; i++) {
      std::vector<size_t> osd_offsets;
      osd_offsets.push_back(k + i);

      // push coding read
      operations->push_back(
          ReadOperation(coding_obj_number, osd_offsets, stripe_size, 0,
            new char[stripe_size], true));
    }

  }
  // return how many successful reads are necessary
  return data_reads;
}

  size_t StripeTranslatorErasureCodes::ProcessReads(
      std::vector<ReadOperation>* operations,
      std::vector<int> &erasures,
      PolicyContainer policies,
      size_t received_data,
    int64_t offset) const {
  //  stripe size is stored in kB
  unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of parity OSDs
  unsigned int m =(*policies.begin())->parity_width();
  // number of OSDs
  unsigned int n =(*policies.begin())->width();
  // number of data OSDs
  unsigned int k = n - m;
  // word size for the encoder
  int w = 32;
  // how many (poss. incomplete)  lines does this operation contain
  // (x + y - 1) / y is apperantly a common idiom for quick ceil(x / y) can overflow in x + y though
  // 1 + ((x - 1) / y avoids overflow problem
  int lines = 1 + ((operations->size() - 1) / n);
  char *data[k];
  char *coding[m];

  // if only data objects have been read cleanup and exit...nothing to do
  if (erasures.size() == 0) {
    // all data stripes have successfully been read
    // since read ops are ordered so that necessary data reads come first received_data can simply
    // be returned
    return received_data;
  }


  // TODO if a request is smaller then stripe_size copy buffer to padded buffer and set
  // data[i] to padded buffer for coding
  // TODO set pointers to data buffers of operations...data and coding
  // set erasure vektor (must have -1 as last element)
  // then decode
  for (int l = 0; l < lines; l++) {
    int line_offset = l * k;

    for (int i = 0; i < k; i++) {
      cout << "setting data device to read op " << (line_offset + i) << endl;
      data[i] = (*operations)[line_offset + i].data;
    }

    for(int i = 0; i < m; i++) {
      cout << "setting coding device to read op " << (lines*k+l*m+i) << endl;
      coding[i] = (*operations)[lines * k + l * m + i].data;
    }

    cout << "erasures:";
    cout << endl;

    this->Decode(k, m, w, data, coding, erasures, stripe_size);
  }

  return k * lines * stripe_size;
}

}  // namespace xtreemfs
