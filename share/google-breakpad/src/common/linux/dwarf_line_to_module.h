// -*- mode: c++ -*-

// Copyright (c) 2010 Google Inc.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// Original author: Jim Blandy <jimb@mozilla.com> <jimb@red-bean.com>

// The DwarfLineToModule class accepts line number information from a
// DWARF parser and adds it to a google_breakpad::Module. The Module
// can write that data out as a Breakpad symbol file.

#ifndef COMMON_LINUX_DWARF_LINE_TO_MODULE_H
#define COMMON_LINUX_DWARF_LINE_TO_MODULE_H

#include "common/linux/module.h"
#include "common/dwarf/dwarf2reader.h"

namespace google_breakpad {

// A class for producing a vector of google_breakpad::Module::Line
// instances from parsed DWARF line number data.  
//
// An instance of this class can be provided as a handler to a
// dwarf2reader::LineInfo DWARF line number information parser. The
// handler accepts source location information from the parser and
// uses it to produce a vector of google_breakpad::Module::Line
// objects, referring to google_breakpad::Module::File objects added
// to a particular google_breakpad::Module.
class DwarfLineToModule: public dwarf2reader::LineInfoHandler {
 public:
  // As the DWARF line info parser passes us line records, add source
  // files to MODULE, and add all lines to the end of LINES. LINES
  // need not be empty. If the parser hands us a zero-length line, we
  // omit it. If the parser hands us a line that extends beyond the
  // end of the address space, we clip it. It's up to our client to
  // sort out which lines belong to which functions; we don't add them
  // to any particular function in MODULE ourselves.
  DwarfLineToModule(Module *module, vector<Module::Line> *lines)
      : module_(module),
        lines_(lines),
        highest_file_number_(-1),
        warned_bad_file_number_(false),
        warned_bad_directory_number_(false) { }
  
  ~DwarfLineToModule() { }

  void DefineDir(const std::string &name, uint32 dir_num);
  void DefineFile(const std::string &name, int32 file_num,
                  uint32 dir_num, uint64 mod_time,
                  uint64 length);
  void AddLine(uint64 address, uint64 length,
               uint32 file_num, uint32 line_num, uint32 column_num);

 private:

  typedef std::map<uint32, std::string> DirectoryTable;
  typedef std::map<uint32, Module::File *> FileTable;

  // The module we're contributing debugging info to. Owned by our
  // client.
  Module *module_;

  // The vector of lines we're accumulating. Owned by our client.
  //
  // In a Module, as in a breakpad symbol file, lines belong to
  // specific functions, but DWARF simply assigns lines to addresses;
  // one must infer the line/function relationship using the
  // functions' beginning and ending addresses. So we can't add these
  // to the appropriate function from module_ until we've read the
  // function info as well. Instead, we accumulate lines here, and let
  // whoever constructed this sort it all out.
  vector<Module::Line> *lines_;

  // A table mapping directory numbers to paths.
  DirectoryTable directories_;

  // A table mapping file numbers to Module::File pointers.
  FileTable files_;

  // The highest file number we've seen so far, or -1 if we've seen
  // none.  Used for dynamically defined file numbers.
  int32 highest_file_number_;

  // True if we've warned about:
  bool warned_bad_file_number_; // bad file numbers
  bool warned_bad_directory_number_; // bad directory numbers
};

} // namespace google_breakpad

#endif // COMMON_LINUX_DWARF_LINE_TO_MODULE_H
