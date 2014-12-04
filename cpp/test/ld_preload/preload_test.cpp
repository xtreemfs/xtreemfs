/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <iostream>
#include <cstdio>
#include <cstring>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

using namespace std;

int main(int argc, char* argv[]) {

  const char default_path[] = "/xtreemfs/temp_b.file";
  const char* path = default_path;

  // take path from command line if specified
  if (argc > 1) {
    path = argv[1];
  }

  // open() file
  std::cout << "TEST: open()" << std::endl;
  //FILE* file_a = std::fopen("/xtreemfs/demo/temp_a.file", "w");
  int file_b = open(path, O_WRONLY | O_CREAT | O_TRUNC, 0777);
  //int file_b = open("/xtreemfs/demo/temp.file", O_WRONLY | O_CREAT); // this is wrong by open specification, mode is not set, but O_CREAT is

  const char data_to_write[] = "Hello World!";
  char data_to_read[256] = "";

  ssize_t write_ret = write(file_b, (void*)data_to_write, sizeof(data_to_write));

  lseek(file_b, 0, SEEK_SET);

  // close() file
  close(file_b);

  // open() same file for reading
  file_b = open(path, O_RDONLY, 0);

  ssize_t read_ret = read(file_b, (void*)data_to_read, sizeof(data_to_write));

  // close() file
  std::cout << "TEST: close()" << std::endl;
  //std::fclose(file_a);
  close(file_b);

  // compare written and read
  if (0 == std::strcmp(data_to_write, data_to_read))
    std::cout << "PASS" << std::endl;
  else
    std::cout << "FAIL" << std::endl;

  // stat(), fstat()

  // access()

  // ftruncate()

  // getcwd()

  std::cout << "TEST: done()" << std::endl;

  return 0;
}
