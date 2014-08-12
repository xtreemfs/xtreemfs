/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/scoped_ptr.hpp>
#include <string>

#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "xtreemfs/GlobalTypes.pb.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

class PBRPCURLTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);

    volume_name = "test";
    default_scheme = PBRPCURL::GetSchemePBRPC();
    default_port = DIR_PBRPC_PORT_DEFAULT;

    servers.push_back("localhost");
    servers.push_back("127.0.0.1");
    servers.push_back("somehost");

    ports.push_back(1234);
    ports.push_back(4242);
    ports.push_back(31337);

    pbrpc_url_.reset(new PBRPCURL());
  }

  virtual void TearDown() {
    shutdown_logger();
  }

  typedef list<string> ServerList;
  typedef list<uint16_t> PortList;

  ServerList servers;
  PortList ports;
  string volume_name;
  string default_scheme;
  uint16_t default_port;

  boost::scoped_ptr<PBRPCURL> pbrpc_url_;
};

TEST_F(PBRPCURLTest, URLWithOneAddressAndVolume) {
  stringstream url_to_parse;
  url_to_parse << servers.front() << '/' << volume_name;
  pbrpc_url_->ParseURL(url_to_parse.str(), default_scheme, default_port);

  ServiceAddresses addresses = pbrpc_url_->GetAddresses();

  EXPECT_FALSE(addresses.empty());
  EXPECT_EQ(volume_name, pbrpc_url_->volume());
  EXPECT_EQ(PBRPCURL::GetSchemePBRPC(), pbrpc_url_->scheme());

  stringstream expected_address;
  expected_address << "localhost:" << DIR_PBRPC_PORT_DEFAULT;
  EXPECT_EQ(expected_address.str(), addresses.GetAddresses().front());
}

TEST_F(PBRPCURLTest, URLWithMultipleAddressesAndVolume) {
  // Build an URL to parse
  stringstream url_to_parse;
  ServerList::const_iterator servers_it = servers.begin();

  for (; servers_it != servers.end(); ++servers_it) {
    if(servers_it != servers.begin()) {
      url_to_parse << ',';
    }
    url_to_parse << *servers_it;
  }
  url_to_parse << '/' << volume_name;

  // Parse URL and get addresses
  pbrpc_url_->ParseURL(url_to_parse.str(), default_scheme, default_port);
  ServiceAddresses addresses = pbrpc_url_->GetAddresses();

  // Check expectations
  EXPECT_EQ(servers.size(), addresses.size());
  EXPECT_EQ(volume_name, pbrpc_url_->volume());
  EXPECT_EQ(default_scheme, pbrpc_url_->scheme());

  servers_it = servers.begin();
  stringstream expected_address;
  ServiceAddresses::Addresses services = addresses.GetAddresses();
  for(ServiceAddresses::Addresses::iterator it = services.begin();
      it != services.end();
      ++it, ++servers_it) {
    expected_address.str("");
    expected_address << *servers_it << ':' << default_port;
    EXPECT_EQ(expected_address.str(), *it);
  }
}

TEST_F(PBRPCURLTest, URLWithMultipleAddressesPortsAndVolume) {
  // Build an URL to parse
  stringstream url_to_parse;
  ServerList::const_iterator servers_it = servers.begin();
  PortList::const_iterator ports_it = ports.begin();

  for (; servers_it != servers.end(); ++servers_it, ++ports_it) {
    if(servers_it != servers.begin()) {
      url_to_parse << ',';
    }
    url_to_parse << *servers_it << ':' << *ports_it;
  }
  url_to_parse << '/' << volume_name;

  // Parse URL and get addresses
  pbrpc_url_->ParseURL(url_to_parse.str(), default_scheme, default_port);
  ServiceAddresses addresses = pbrpc_url_->GetAddresses();

  // Check expectations
  EXPECT_EQ(servers.size(), addresses.size());
  EXPECT_EQ(volume_name, pbrpc_url_->volume());
  EXPECT_EQ(default_scheme, pbrpc_url_->scheme());

  servers_it = servers.begin();
  ports_it = ports.begin();
  stringstream expected_address;
  ServiceAddresses::Addresses services = addresses.GetAddresses();
  for(ServiceAddresses::Addresses::iterator it = services.begin();
      it != services.end();
      ++it, ++servers_it, ++ports_it) {
    expected_address.str("");
    expected_address << *servers_it << ':' << *ports_it;
    EXPECT_EQ(expected_address.str(), *it);
  }
}

TEST_F(PBRPCURLTest, URLWithMultipleAddressesProtocolsPortsAndVolume) {
  // Build an URL to parse
  stringstream url_to_parse;
  ServerList::const_iterator servers_it = servers.begin();
  PortList::const_iterator ports_it = ports.begin();

  for (; servers_it != servers.end(); ++servers_it, ++ports_it) {
    if(servers_it != servers.begin()) {
      url_to_parse << ',';
    }
    url_to_parse << default_scheme << "://" << *servers_it << ':' << *ports_it;
  }
  url_to_parse << '/' << volume_name;
  // Parse URL and get addresses
  pbrpc_url_->ParseURL(url_to_parse.str(), default_scheme, default_port);
  ServiceAddresses addresses = pbrpc_url_->GetAddresses();

  // Check expectations
  EXPECT_EQ(servers.size(), addresses.size());
  EXPECT_EQ(volume_name, pbrpc_url_->volume());
  EXPECT_EQ(default_scheme, pbrpc_url_->scheme());

  servers_it = servers.begin();
  ports_it = ports.begin();
  stringstream expected_address;
  ServiceAddresses::Addresses services = addresses.GetAddresses();
  for(ServiceAddresses::Addresses::iterator it = services.begin();
      it != services.end();
      ++it, ++servers_it, ++ports_it) {
    expected_address.str("");
    expected_address << *servers_it << ':' << *ports_it;
    EXPECT_EQ(expected_address.str(), *it);
  }
}

TEST_F(PBRPCURLTest, URLWithMultipleAddressesProtocolsAndVolume) {
  // Build an URL to parse
  stringstream url_to_parse;
  ServerList::const_iterator servers_it = servers.begin();

  for (; servers_it != servers.end(); ++servers_it) {
    if(servers_it != servers.begin()) {
      url_to_parse << ',';
    }
    url_to_parse << default_scheme << "://" << *servers_it;
  }
  url_to_parse << '/' << volume_name;
  // Parse URL and get addresses
  pbrpc_url_->ParseURL(url_to_parse.str(), default_scheme, default_port);
  ServiceAddresses addresses = pbrpc_url_->GetAddresses();

  // Check expectations
  EXPECT_EQ(servers.size(), addresses.size());
  EXPECT_EQ(volume_name, pbrpc_url_->volume());
  EXPECT_EQ(default_scheme, pbrpc_url_->scheme());

  servers_it = servers.begin();
  stringstream expected_address;
  ServiceAddresses::Addresses services = addresses.GetAddresses();
  for(ServiceAddresses::Addresses::iterator it = services.begin();
      it != services.end();
      ++it, ++servers_it) {
    expected_address.str("");
    expected_address << *servers_it << ':' << default_port;
    EXPECT_EQ(expected_address.str(), *it);
  }
}

TEST_F(PBRPCURLTest, URLAddressesWithDifferentProtocols) {
  EXPECT_THROW({
      pbrpc_url_->ParseURL("pbrpc://localhost,pbrpcg://remote/" + volume_name,
                           PBRPCURL::GetSchemePBRPC(),
                           DIR_PBRPC_PORT_DEFAULT);
      }, InvalidURLException);
}

}  // namespace xtreemfs
