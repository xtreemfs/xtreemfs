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
    uint64_t offset,
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
          // or pass a pointer to the user buffer
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

void StripeTranslatorErasureCodes::TranslateReadRequest(
    char *buf,
    size_t size,
    uint64_t offset,
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
  size_t cbuf_pos = 0;

  cout << endl<< "translating new read request" << endl;
  cout << "size: " << size << " offset: " << offset << endl;

  // skip as many full lines as possible
  cout << "skipping line offset from " << offset;
  while (offset >= stripe_size * k) {
    offset -= stripe_size * k;
    obj_number += k;
  }
  cout << " to " << offset << endl;

  assert(offset < k * stripe_size);

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

      if (offset >= stripe_size || cbuf_pos == size) {
        // aux read before or after requested range
        operations->push_back(
            ReadOperation(obj_number, osd_offsets, stripe_size, 0,
              new char[stripe_size], true));
        cout << "pushing aux read" << endl;
        offset -= stripe_size;
      } else if (offset > 0 && cbuf_pos == 0) {
        // add a short aux read before the user request
        operations->push_back(
            ReadOperation(obj_number, osd_offsets, offset, 0,
              new char[offset], true));
        cout << "pushing aux read" << endl;
        // then mandatory op
        operations->push_back(
            ReadOperation(obj_number, osd_offsets, stripe_size - offset, offset,
              buf + cbuf_pos));
        cout << "pushing mandatory read to cbuf_pos " << cbuf_pos << endl;
        cbuf_pos += stripe_size - offset;
        offset = 0;
      } else if (offset == 0 && size - cbuf_pos < stripe_size) {
        // mandatory op
        operations->push_back(
            ReadOperation(obj_number, osd_offsets, size - cbuf_pos, 0,
              buf + cbuf_pos));
        cout << "pushing mandatory read to cbuf_pos " << cbuf_pos << endl;
        // add a short aux read after the user request
        operations->push_back(
            ReadOperation(obj_number, osd_offsets, stripe_size - (size - cbuf_pos), size - cbuf_pos,
              new char[stripe_size - (size - cbuf_pos)], true));
        cout << "pushing aux read" << endl;
        cbuf_pos = size;
      } else {
        operations->push_back(
            ReadOperation(obj_number, osd_offsets, stripe_size, 0,
              buf + cbuf_pos));
        cout << "pushing mandatory full read to cbuf_pos " << cbuf_pos << endl;
        cbuf_pos += stripe_size;
      }
      obj_number++;
    }

    int coding_obj_number = (obj_number - 1) - ((obj_number - 1) % k);
    for(int i = 0; i < m; i++) {
      std::vector<size_t> osd_offsets;
      osd_offsets.push_back(k + i);

      // push coding read; is always an aux read
      operations->push_back(
          ReadOperation(coding_obj_number, osd_offsets, stripe_size, 0,
            new char[stripe_size], true));
      cout << "pushed code read op to the back" << endl;
    }
  }
  cout << "translate read req is done..." << endl;
}

size_t StripeTranslatorErasureCodes::ProcessReads(
    std::vector<ReadOperation>* operations,
    char *buf,
    size_t size,
    uint64_t offset,
    PolicyContainer policies,
    bool erasure) const {

  size_t return_size = 0;
  if (!erasure) {
    // only count bytes if no erasure were detected
    cout << "no decoding necessary" << endl;
    for (std::vector<ReadOperation>::iterator it = operations->begin(); it != operations->end(); it++) {
      if (!it->is_aux) {
        return_size += it->recv_size;
      }
    }
  } else {
    //decode otherwise
    //  stripe size is stored in kB
    size_t stripe_size = (*policies.begin())->stripe_size() * 1024;
    // number of parity OSDs
    unsigned int m =(*policies.begin())->parity_width();
    // number of OSDs
    unsigned int n =(*policies.begin())->width();
    // number of data OSDs
    unsigned int k = n - m;
    // word size for the encoder
    int w = 32;

    cout << endl<< "processing reads with erasures" << endl;
    cout << "size: " << size << " offset: " << offset << endl;

    // skip as many full lines as possible
    cout << "skipping line offset from " << offset;
    while (offset >= stripe_size * k) {
      offset -= stripe_size * k;
    }
    cout << " to " << offset << endl;

    assert(offset < k * stripe_size);

    // how many (poss. incomplete)  lines does this operation contain
    // (x + y - 1) / y is apperantly a common idiom for quick ceil(x / y) can overflow in x + y though
    // 1 + ((x - 1) / y avoids overflow problem
    int objects = 1 + ((size + offset - 1) / stripe_size);
    int lines = 1 + ((objects - 1) / k);

    // char bufs of operations can not be used for decoding since gf-complete expects src and dest
    // buffers to be aligned on 16 byte boundries with respect to each other
    // this limitation prohibtes certain offsets (when offset % 16 != 0)
    char *data[k];
    char *coding[m];

    std::vector<char*> h_bufs;
    std::vector<char*>::iterator cur_h_buf;

    std::vector<ReadOperation>::iterator cur_op = operations->begin();


    for (int l = 0; l < lines; l++) {

      cur_h_buf = h_bufs.begin();
      std::vector<ReadOperation>::iterator decode_cur_op = cur_op;

      vector<int> erasures;

      for (int i = 0; i < k; i++) {
        if (cur_op->req_size == stripe_size) {
          if (!cur_op->success) {
            cout << "read op " << cur_op - operations->begin() << "(" << i << ")" << " is erased and needs to be reconstructed" << endl;
            erasures.push_back(i);
          } else {
            if (cur_op->recv_size < stripe_size) {
              memset(cur_op->data + cur_op->recv_size, 0, stripe_size - cur_op->recv_size);
              cout << "padding data with " << stripe_size - cur_op->recv_size << " 0s";
            }
          }
          data[i] = cur_op->data;
          cout << "setting data* to op data" << endl;
        } else {
          // allocate another helper buffer if needed
          cout << "split operation...using h_buf" << endl;
          if(cur_h_buf == h_bufs.end()) {
            h_bufs.push_back(new char[stripe_size]);
            cur_h_buf = h_bufs.end() - 1;
            cout << " adding new h_buf...now managing " << h_bufs.size() << " buffers" << endl;
          }
          if (!cur_op->success || !(cur_op + 1)->success) {
            cout << "partial read op " << cur_op - operations->begin() << "(" << i << ")" << " is erased and needs to be reconstructed" << endl;
            erasures.push_back(i);
            cur_op++;
            assert(cur_op != operations->end());
          } else {
            memcpy(*cur_h_buf, cur_op->data, cur_op->recv_size);
            cout << "copying " << cur_op->recv_size << " bytes to hbuf";
            if (cur_op->req_size > cur_op->recv_size){
              //pad with 0s
              memset(*cur_h_buf + cur_op->recv_size, 0, cur_op->req_size - cur_op->recv_size);
              cout << " and padding with 0s";
            }
            cout << endl;

            size_t cur_h_offset = cur_op->req_size;
            cur_op++;
            assert(cur_op != operations->end());

            memcpy(*cur_h_buf + cur_h_offset, cur_op->data, cur_op->recv_size);
            cout << "copying " << cur_op->recv_size << " bytes to the end of hbuf";
            if (cur_op->req_size > cur_op->recv_size){
              //pad with 0s
              memset(*cur_h_buf + cur_h_offset + cur_op->recv_size, 0, cur_op->req_size - cur_op->recv_size);
              cout << " and padding with 0s";
            }
            cout << endl;
          }
          cout << "setting data* to h_buf" << endl;
          data[i] = *cur_h_buf;
          cur_h_buf++;
        }
        cur_op++;
        assert(cur_op != operations->end());
      }

      for(int i = 0; i < m; i++) {
        if (cur_op->req_size == stripe_size) {
          coding[i] = cur_op->data;
          cout << "setting coding* to op data";
          if (cur_op->recv_size < stripe_size && cur_op->success) {
            memset(coding[i] + cur_op->recv_size, 0, stripe_size - cur_op->recv_size);
            cout << " and padding with " << stripe_size - cur_op->recv_size << " 0s";
          }
          cout << endl;
        } else {
          // allocate another helper buffer if needed
          cout << "split operation...using h_buf" << endl;
          if(cur_h_buf == h_bufs.end()) {
            h_bufs.push_back(new char[stripe_size]);
            cur_h_buf = h_bufs.end() - 1;
            cout << " adding new h_buf...now managing " << h_bufs.size() << " buffers" << endl;
          }
          if(cur_op->success) {
            memcpy(*cur_h_buf, cur_op->data, cur_op->recv_size);
            cout << "copying " << cur_op->recv_size << " bytes to hbuf";
            if (cur_op->req_size > cur_op->recv_size){
              //pad with 0s
              memset(*cur_h_buf + cur_op->recv_size, 0, cur_op->req_size - cur_op->recv_size);
              cout << " and padding with 0s";
            }
            cout << endl;
          } else { cout << "nothing to copy...erasure" << endl;}

          size_t cur_h_offset = cur_op->req_size;
          cur_op++;

          if(cur_op->success) {
            memcpy(*cur_h_buf + cur_h_offset, cur_op->data, cur_op->recv_size);
            cout << "copying " << cur_op->recv_size << " bytes to the end of hbuf";
            if (cur_op->req_size > cur_op->recv_size){
              //pad with 0s
              memset(*cur_h_buf + cur_h_offset + cur_op->recv_size, 0, cur_op->req_size - cur_op->recv_size);
              cout << " and padding with 0s";
            }
            cout << endl;
          } else { cout << "nothing to copy...erasure" << endl;}
          cout << "setting coding* to h_buf" << endl;
          coding[i] = *cur_h_buf;
        }
        cur_op++;
        cur_h_buf++;
      }

      if (erasures.size() <= m) {
        cout << "decoding line " << l << endl;
        this->Decode(k, m, w, data, coding, erasures, stripe_size);
      } else {
        throw new IOException;
      }

      for (int i = 0; i < k; i++) {
        // only copy partial operations that needed decoding (i.e. have been erased) back
        cout << "start decoding.." << endl;
        if (decode_cur_op->req_size != stripe_size) {
          if (!decode_cur_op->success && !decode_cur_op->is_aux) {
            // copy data to corrent buffer position...must be either at start or end - req_size
            if (l == 0) {
              memcpy(buf, data[i], decode_cur_op->req_size);
              cout << "copy " << decode_cur_op->req_size << " to buf at pos 0" << endl;
            } else {
              memcpy(buf + size - decode_cur_op->req_size, data[i], decode_cur_op->req_size);
              cout << "copy " << decode_cur_op->req_size << " to buf at pos " << size - decode_cur_op->req_size << endl;
            }
          }
          if (!decode_cur_op->is_aux) {
            return_size += decode_cur_op->req_size;
            cout << "adding " << decode_cur_op->req_size << " to return_size" << endl;
          }
          decode_cur_op++;
          if (!decode_cur_op->success && !decode_cur_op->is_aux) {
            // copy data to corrent buffer position...must be either at start or end - req_size
            if (l == 0) {
              memcpy(buf, data[i] + decode_cur_op->req_offset, decode_cur_op->req_size);
              cout << "copy " << decode_cur_op->req_size << " to buf at pos 0" << endl;
            } else {
              memcpy(buf + size - decode_cur_op->req_size, data[i] + decode_cur_op->req_offset, decode_cur_op->req_size);
              cout << "copy " << decode_cur_op->req_size << " to buf at pos " << size - decode_cur_op->req_size << endl;
            }
          }
        }
        if (!decode_cur_op->is_aux) {
          if (decode_cur_op->success) {
            return_size += decode_cur_op->recv_size;
            cout << "adding " << decode_cur_op->recv_size << " to return_size" << endl;
          } else {
            return_size += decode_cur_op->req_size;
            cout << "adding " << decode_cur_op->req_size << " to return_size" << endl;
          }
        }
        decode_cur_op++;
      }
    }

    for (std::vector<char*>::iterator it = h_bufs.begin(); it != h_bufs.end(); it++){
      cout << "deleting helper buffers" << endl;
      delete[] *it;
    }
  }

  return return_size;
}

}  // namespace xtreemfs
