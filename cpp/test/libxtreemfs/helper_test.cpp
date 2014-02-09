/*
 * Copyright (c) 2014 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <arpa/inet.h>
#include <ifaddrs.h>
#include <netdb.h>
#include <sys/socket.h>

#include <google/protobuf/stubs/common.h>

#include "libxtreemfs/helper.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

/** Tests for various helper methods. */
class HelperTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);
  }

  virtual void TearDown() {
    shutdown_logger();
    atexit(google::protobuf::ShutdownProtobufLibrary);
  }
};

#ifdef __linux__
TEST_F(HelperTest, GetNetworkStringUnixIPv4) {
  // Linux's "getifaddrs" returns for each network interface a "struct ifaddrs".
  // Check that the network is correctly determined based on such a struct.
  struct ifaddrs ifaddr = {};
  struct addrinfo hints = {};
  hints.ai_family = AF_INET;
  hints.ai_flags = AI_NUMERICHOST;

  struct addrinfo* ai_addr;
  ASSERT_EQ(0, getaddrinfo("127.0.0.1", NULL, &hints, &ai_addr));

  struct addrinfo* ai_netmask;
  ASSERT_EQ(0, getaddrinfo("255.0.0.0", NULL, &hints, &ai_netmask));

  ifaddr.ifa_next = NULL;
  ifaddr.ifa_name = (char*) "eth0";
  ifaddr.ifa_addr = ai_addr->ai_addr;
  ifaddr.ifa_netmask = ai_netmask->ai_addr;

  EXPECT_EQ("127.0.0.0/8", GetNetworkStringUnix(&ifaddr));

  freeaddrinfo(ai_addr);
  freeaddrinfo(ai_netmask);
}
#endif  // __linux__

#ifdef __linux__
TEST_F(HelperTest, GetNetworkStringUnixIPv6) {
  // Linux's "getifaddrs" returns for each network interface a "struct ifaddrs".
  // Check that the network is correctly determined based on such a struct.
  struct ifaddrs ifaddr = {};
  struct addrinfo hints = {};
  hints.ai_family = AF_INET6;
  hints.ai_flags = AI_NUMERICHOST;

  struct addrinfo* ai_addr;
  ASSERT_EQ(0, getaddrinfo("fe80::b4ff:fe58:a410", NULL, &hints, &ai_addr));

  struct addrinfo* ai_netmask;
  ASSERT_EQ(0, getaddrinfo("ffff:ffff:ffff:ffff::", NULL, &hints, &ai_netmask));

  ifaddr.ifa_next = NULL;
  ifaddr.ifa_name = (char*) "eth0";
  ifaddr.ifa_addr = ai_addr->ai_addr;
  ifaddr.ifa_netmask = ai_netmask->ai_addr;

  EXPECT_EQ("fe80::/64", GetNetworkStringUnix(&ifaddr));

  freeaddrinfo(ai_addr);
  freeaddrinfo(ai_netmask);
}
#endif  // __linux__

}  // namespace xtreemfs
