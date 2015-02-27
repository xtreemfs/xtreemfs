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
    size_t coding_size = stripe_size;
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
          if (i == 0 && req_size < stripe_size && processed_size >= size) {
            coding_size = req_size;
            cout << "setting coding_size to " << req_size << endl;
          }
          cout << "added data read operation at position " << (data_reads - 1) << endl;
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
          ReadOperation(coding_obj_number, osd_offsets, coding_size, 0,
            new char[coding_size], true));
      cout << "pushed code read op to the back" << endl;
    }

  }
  cout << "translate read req is done..." << endl;
  // return how many successful reads are necessary
  return data_reads;
}

namespace {

  boost::tuple<int, int, size_t, size_t> helper(
      char** data,
      char** coding,
      std::vector<xtreemfs::ReadOperation>* operations,
      int k,
      int m,
      size_t stripe_size,
      size_t to_process,
      int data_read,
      int aux_read,
      boost::dynamic_bitset<>* successful_reads,
      std::vector<int>* erasures,
      bool after_encoding) {

    size_t read_data = 0;

    for (int i = 0; i < k; i++) {
      int line_pos = operations->at(data_read).osd_offsets[0];
      if (line_pos != i) {
        assert(i == operations->at(aux_read).osd_offsets[0]);
        assert(operations->at(aux_read).req_offset == 0);

        size_t req_size = operations->at(aux_read).req_size;

        if(after_encoding) {
          cout << "copy data from data device " << i << " to op " << (aux_read) << endl;
          memcpy(operations->at(aux_read).data, data[i], req_size);
        } else {
          cout << "copy data from op " << aux_read << " to data device " << (i) << endl;
          memcpy(data[i], operations->at(aux_read).data, req_size);

          if (!successful_reads->test(aux_read)) {
            erasures->push_back(i);
          }
        }
        if (successful_reads->test(aux_read)) {
          cout << "\tdecreasing to_process by " << req_size << " bytes" << endl;
          to_process -= req_size;
        }
        ++aux_read;
      } else {
        assert(i == operations->at(data_read).osd_offsets[0]);

        size_t req_offset = operations->at(data_read).req_offset;
        size_t req_size = operations->at(data_read).req_size;
        if(after_encoding) {
          cout << "copy data from data device " << i << " to op " << (data_read) << " size, offset " << req_size << " , " << req_offset << endl;
          memcpy(operations->at(data_read).data, data[i] + req_offset, req_size);
          read_data += req_size;
          cout << "\tincreasing read_data by " << req_size << " bytes to " << read_data << endl;
        } else {
          cout << "copy data from op " << data_read << " to data device " << (i) << " size, offset " << req_size << " , " << req_offset << endl;
          memset(data[i], 0, req_offset);
          if (req_offset + req_size < stripe_size) {
            memset(data[i] + req_offset + req_size, 0, stripe_size - req_offset - req_size);
          }
          memcpy(data[i] + req_offset, operations->at(data_read).data, req_size);
          if (!successful_reads->test(data_read)) {
            erasures->push_back(i);
          }
        }
        if (successful_reads->test(data_read)) {
          cout << "\tdecreasing to_process by " << req_size << " bytes" << endl;
          to_process -= req_size;
        }
        ++data_read;

        if (req_size < stripe_size && operations->at(data_read).osd_offsets[0] != data_read) {
          cout << "first object short..." << endl;
          if(!after_encoding){
            for (int j = 1; j < k; j++) {
              cout << "set data device " << j << " to zero" << endl;
              memset(data[j], 0, stripe_size);
            }
          }
          break;
        }
      }
    }

    for(int i = 0; i < m; i++) {
      assert(i + k == operations->at(aux_read).osd_offsets[0]);

      size_t req_offset = operations->at(aux_read).req_offset;
      size_t req_size = operations->at(aux_read).req_size;
      if(after_encoding) {
        cout << "copy data from coding device " << i << " to op " << (aux_read) << endl;
        memcpy(operations->at(aux_read).data, coding[i] + req_offset, req_size);
      } else {
          cout << "copy data from op " << aux_read << " to coding device " << (i) << endl;
        memset(coding[i], 0, req_offset);
        if (req_offset + req_size < stripe_size) {
          memset(coding[i] + req_offset + req_size, 0, stripe_size - req_offset - req_size);
        }
        memcpy(coding[i] + req_offset, operations->at(aux_read).data, req_size);
        if (!successful_reads->test(aux_read)) {
          erasures->push_back(i);
        }
      }
      if (successful_reads->test(aux_read)) {
        cout << "\tdecreasing to_process by " << req_size << " bytes" << endl;
        to_process -= req_size;
      }
      ++aux_read;
    }

    return boost::make_tuple(data_read, aux_read, read_data, to_process);
  }

} // namespace

// TODO TODO any offset that is not divideable by 16 will cause gf-complete to crap out since it
// wants src and dest buffers aligned to each other on 16 byte boundries. so either make it mor
// complex or copy every line to its own n*stripe_size buffer and do coding there.
size_t StripeTranslatorErasureCodes::ProcessReads(
    std::vector<ReadOperation>* operations,
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
  size_t read_data = 0;
  cout << lines << " lines must be reconstructed" << endl;
  cout << "successful_reads is " << *successful_reads << endl;

  int data_read = 0;
  int aux_read = min_reads;
  long to_process = received_data;

  for (int l = 0; l < lines; l++) {
    cout << to_process << " bytes to decode" << endl;
    if (to_process > 0) {
      vector<int> erasures;

      helper(data, coding, operations, k, m, stripe_size, received_data - read_data, data_read, aux_read,
          successful_reads, &erasures, false);

      // exit(1);
      cout << "decoding line " << l << endl;
      this->Decode(k, m, w, data, coding, erasures, stripe_size);
      // TODO clean up allocated buffers and copy data to correct locations

      int new_data_read;
      int new_aux_read;
      size_t stripe_read;
      boost::tie(new_data_read, new_aux_read, stripe_read, to_process) = helper(data, coding, operations, k, m,
          stripe_size, received_data - read_data, data_read, aux_read,
          successful_reads, &erasures, true);
      data_read = new_data_read;
      aux_read = new_aux_read;
      read_data += stripe_read;
      cout << endl;
    }
  }

  return min(received_data, read_data);
}

}  // namespace xtreemfs
