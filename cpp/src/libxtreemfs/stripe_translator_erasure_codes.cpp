/*
 * Copyright (c) 2009-2015 by Jan Fajerski, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 * TODO
 * add docs:
 * talk about order of operations, ceil(x/y) trick
 *
 */
#include "libxtreemfs/stripe_translator_erasure_codes.h"
#include "libxtreemfs/xtreemfs_exception.h"

#include <algorithm>
#include <boost/dynamic_bitset.hpp>
#include <boost/tuple/tuple.hpp>
#include <utility>
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

  /* TODO put this somewhere else
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
  cout << "skipping line offset from " << offset;
  while (offset > 0) {
    offset -= stripe_size * k;
    obj_number += k;
  }
  cout << " to " << offset << endl;

  assert (offset == 0);

  int objects = 1 + ((size - 1) / stripe_size);
  int lines = 1 + ((objects - 1) / k);
  cout << objects << " objects need to be covered" << endl;
  cout << lines << " lines need to be covered" << endl;

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
        if (i == 0 && req_size < stripe_size) {
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

    int coding_obj_number = (obj_number - 1) - ((obj_number - 1) % k);
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
            WriteOperation(coding_obj_number, osd_offsets, coding_size, 0,
              coded, true));
      } else {
        operations->push_back(
            WriteOperation(coding_obj_number, osd_offsets, stripe_size, 0,
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

  cout << "size: " << size << " offset: " << offset << endl;

  // skip as many full lines as possible
  cout << "skipping line offset from " << offset;
  while (offset > stripe_size * k) {
    offset -= stripe_size * k;
    obj_number += k;
  }
  cout << " to " << offset << endl;

  int objects = 1 + ((size + offset - 1) / stripe_size);
  int lines = 1 + ((objects - 1) / k);
  cout << objects << " objects need to be covered" << endl;
  cout << lines << " lines need to be covered" << endl;

  // TODO make sure to always read complete stripes...no offsets into stripes in case erasure
  // happens...cannot decode with half a stripe
  // actual read offset must be reconstructed in ProcessReads
  for (int l = 0; l < lines; l++) {
    for (int i = 0; i < k; i++) {
      std::vector<size_t> osd_offsets;
      osd_offsets.push_back(i);

      operations->push_back(
          ReadOperation(obj_number, osd_offsets, stripe_size, 0,
            new char[stripe_size], true));
      data_reads++;
      cout << "added data read operation at end" << endl;
      obj_number++;
    }

    int coding_obj_number = (obj_number - 1) - ((obj_number - 1) % k);
    for(int i = 0; i < m; i++) {
      std::vector<size_t> osd_offsets;
      osd_offsets.push_back(k + i);

      // push coding read
      operations->push_back(
          ReadOperation(coding_obj_number, osd_offsets, stripe_size, 0,
            new char[stripe_size], true));
      cout << "pushed code read op to the back" << endl;
    }

  }
  cout << "translate read req is done..." << endl;
  // return how many successful reads are necessary
  return data_reads;
}

size_t StripeTranslatorErasureCodes::ProcessReads(
    std::vector<ReadOperation>* operations,
    char *buf,
    size_t size,
    int64_t offset,
    boost::dynamic_bitset<>* successful_reads,
    PolicyContainer policies,
    size_t received_data,
    size_t min_reads) const {

  // number of OSDs
  unsigned int n =(*policies.begin())->width();
  // how many (poss. incomplete)  lines does this operation contain
  // (x + y - 1) / y is apperantly a common idiom for quick ceil(x / y) can overflow in x + y though
  // 1 + ((x - 1) / y avoids overflow problem
  int lines = 1 + ((operations->size() - 1) / n);

  //  stripe size is stored in kB
  size_t stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of parity OSDs
  unsigned int m =(*policies.begin())->parity_width();
  // number of data OSDs
  unsigned int k = n - m;
  // word size for the encoder
  int w = 32;
  // char bufs of operations can not be used for decoding since gf-complete expects src and dest
  // buffers to be aligned on 16 byte boundries with respect to each other
  // this limitation prohibtes certain offsets (when offset % 16 != 0)
  char *data[k];
  for (int i = 0; i < k; i++) {
    data[i] = new char[stripe_size];
  }

  char *coding[m];
  for (int i = 0; i < m; i++) {
    coding[i] = new char[stripe_size];
  }
  cout << lines << " lines must be reconstructed" << endl;
  cout << "successful_reads is " << *successful_reads << endl;

  long to_process = received_data;
  size_t buf_pos = 0;

  for (int l = 0; l < lines; l++) {
    cout << endl << to_process << " bytes to decode" << endl;
    if (to_process > 0) {
      vector<int> erasures;
      bool erased = 0;
      int line_offset = l * n;

      for (int i = 0; i < k; i++) {
        //is the first op starting at osd 0?

        int data_read = line_offset + i;
        if (!successful_reads->test(data_read)) {
          cout << "read op " << data_read << " is erased and needs to be reconstructed" << endl;
          // push i since i is the position int the current line
          erasures.push_back(i);
          erased = 1;
        } else {
          cout << "copy " << stripe_size << " bytes from op " << data_read << " to data device " << (i) << endl;
          memcpy(data[i], operations->at(data_read).data, stripe_size);
        }
      }

      for(int i = 0; i < m; i++) {

        int code_read = line_offset + i + k;
        if (!successful_reads->test(code_read)) {
          cout << "read op " << code_read << " is erased and needs to be reconstructed" << endl;
          // push i since i is the position int the current line
          erasures.push_back(i);
        } else {
          cout << "copy data from op " << code_read << " to code device " << (i) << endl;
          memcpy(coding[i], operations->at(code_read).data, stripe_size);
        }
      }

      // exit(1);
      if (erased) {
        cout << "decoding line " << l << endl;
        this->Decode(k, m, w, data, coding, erasures, stripe_size);
      }

      while (offset > k * stripe_size) {
        offset -= k * stripe_size;
        cout << "line skipped" << endl;
      }

      for (int i = 0; i < k; i++) {
        int data_read = line_offset + i;
        if (offset >= stripe_size) {
          cout << "skipping object due to offset" << endl;
          offset -= stripe_size;
          continue;
        }
        size_t op_size = min(operations->at(data_read).req_size - offset, size);
        if (op_size == 0) {
          break;
        }
        cout << "size: " << size << ", req - off: " << operations->at(data_read).req_size - offset << endl;
        cout << "copy " << op_size << " bytes from data device " << i << " at position " << offset << " to buffer at " << (buf_pos) << endl;
        memcpy(buf + buf_pos, data[i] + offset, op_size);
        size -= op_size;
        buf_pos += op_size;
        offset = 0;
      }
    }
  }

  cout << "deleteing data and coding buffers..." << endl;
  for (int i = 0; i < k; i++) {
    delete[] data[i];
  }

  for (int i = 0; i < m; i++) {
    delete[] coding[i];
  }

  cout << received_data << " have been received" << endl;
  return buf_pos;
}

}  // namespace xtreemfs
