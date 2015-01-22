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
  unsigned int stripe_size = (*policies.begin())->stripe_size() * 1024;
  // number of parity OSDs
  unsigned int m =(*policies.begin())->parity_width();
  // number of OSDs
  unsigned int n =(*policies.begin())->width();
  // number of data OSDs
  unsigned int k = n - m;
  // word size for the encoder
  int w = 32;
  const char *data[k];
  char *coding[m];

  // assume complete full stripes for now
  assert (size % stripe_size == 0);

  int lines = (size / stripe_size) / k;

  for (int l = 0; l < lines; l++) {
    int line_offset = l * k;
    for (int i = 0; i < k; i++) {
      int obj_number = line_offset + i;
      int buffer_offset = (line_offset + i) * stripe_size;

      std::vector<size_t> osd_offsets;
      osd_offsets.push_back(i);

      data[i] = buf + buffer_offset;

      operations->push_back(
          WriteOperation(obj_number, osd_offsets, stripe_size, 0,
            data[i]));
    }

    for(int i = 0; i < m; i++) {
      std::vector<size_t> osd_offsets;
      osd_offsets.push_back(k + i);
      coding[i] = new char[stripe_size];

      operations->push_back(
          WriteOperation(line_offset, osd_offsets, stripe_size, 0,
            coding[i], true));
    }

    this->Encode(k, m, w, data, coding, stripe_size);
  }

}

size_t StripeTranslatorErasureCodes::TranslateReadRequest(
    char *buf,
    size_t size,
    int64_t offset,
    PolicyContainer policies,
    std::vector<ReadOperation>* operations) const {

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
  char *data[k];
  char *coding[m];

  // assume complete full stripes for now
  assert (size % stripe_size == 0);

  int lines = (size / stripe_size) / k;

  for (int l = 0; l < lines; l++) {
    int line_offset = l * k;
    for (int i = 0; i < k; i++) {
      int obj_number = line_offset + i;
      int buffer_offset = (line_offset + i) * stripe_size;

      std::vector<size_t> osd_offsets;
      osd_offsets.push_back(i);

      data[i] = buf + buffer_offset;

      operations->insert(operations->begin() + obj_number,
          ReadOperation(obj_number, osd_offsets, stripe_size, 0,
            data[i]));
    }

    for(int i = 0; i < m; i++) {
      std::vector<size_t> osd_offsets;
      osd_offsets.push_back(k + i);
      coding[i] = new char[stripe_size];

      operations->push_back(
          ReadOperation(line_offset, osd_offsets, stripe_size, 0,
            coding[i]));
    }

  }
  // return how many successful reads are necessary
  return lines * k;
}

  size_t StripeTranslatorErasureCodes::ProcessReads(
      std::vector<ReadOperation>* operations,
      boost::dynamic_bitset<>* successful_reads,
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
  if (((*successful_reads) << lines).count() == operations->size() - lines) {
    for (size_t i = operations->size() - lines; i < operations->size(); i++){
      delete (*operations)[i].data;
    }
    for (int i = 0; i < (offset / stripe_size) % k; i++) {
      delete (*operations)[i].data;
    }
    // all data stripes have successfully been read
    return k * lines * stripe_size;
  }

  // TODO set pointers to data buffers of operations...data and coding
  // set erasure vektor (must have -1 as last element)
  // then decode
  for (int l = 0; l < lines; l++) {
    int line_offset = l * k;
    int *erasures = new int[n + 1];
    int erased = 0;

    for (int i = 0; i < k; i++) {
      cout << "setting data device to read op " << (line_offset + i) << endl;
      data[i] = (*operations)[line_offset + i].data;
      if (!successful_reads->test(line_offset + i)) {
        cout << "setting device " << i << " as erased" << endl;
        erasures[erased] = i;
        erased++;
      }
    }

    for(int i = 0; i < m; i++) {
      cout << "setting coding device to read op " << (lines*k+l*m+i) << endl;
      coding[i] = (*operations)[lines * k + l * m + i].data;
      if (!successful_reads->test(lines * k + l * m + i)) {
        cout << "setting device " << (k + i) << " as erased" << endl;
        erasures[erased] = k + i;
        erased++;
      }
    }

    erasures[erased] = -1;
    cout << "erasures:";
    for (int i = 0; i < n; i++)
      cout << " " << erasures[i];
    cout << endl;

    this->Decode(k, m, w, data, coding, erasures, stripe_size);
  }

  return k * lines * stripe_size;
}

}  // namespace xtreemfs
