/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *               2009-2011 by Patrick Schaefer, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "libxtreemfs/pbrpc_url.h"

#include <boost/algorithm/string.hpp>
#include <iostream>
#include <string>
#include <sstream>

#include "libxtreemfs/xtreemfs_exception.h"

using namespace std;

namespace xtreemfs {

PBRPCURL::PBRPCURL() : scheme_(""), servers_(), ports_(), volume_("") {}

void PBRPCURL::ParseURL(const std::string& original_url,
                        const std::string& default_scheme,
                        const uint16_t default_port) {
  string url(original_url);
  boost::trim(url);
  scheme_ = default_scheme;

  // URL will have the form:
  // [pbrpc://]service-hostname[:port](,[pbrpc://]service-hostname2[:port])*[/volume_name].  // NOLINT

  // Split URL by "," first to retrieve every address.
  // At last, read the optional volume name from the last address.
  vector<string> addresses;
  boost::split(addresses, url, boost::is_any_of(","));
  for (size_t i = 0; i < addresses.size(); i++) {
    const string& address = addresses[i];
    size_t address_pos = 0;

    string scheme = default_scheme;
    size_t scheme_pos = address.find("://");
    if (scheme_pos != string::npos) {
      // scheme specified
      scheme = address.substr(0, scheme_pos);
      if ((scheme != GetSchemePBRPC())
          && (scheme != GetSchemePBRPCS())
          && (scheme != GetSchemePBRPCG())
          && (scheme != GetSchemePBRPCU())) {
        throw InvalidURLException(scheme_ + " is not a valid scheme");
      }

      if (i == 0) {
        scheme_ = scheme;
      } else {
        if (scheme_ != scheme) {
          throw InvalidURLException("The current client does not support to"
              " connect to replicas with different protocols. Different"
              " protocols seen are: " + scheme_ + " and: " + scheme);
        }
      }
      address_pos = scheme_pos + 3;
    }

    uint16_t port = default_port;
    size_t last_colon = address.find_last_of(":");
    size_t last_slash = address.find_last_of("/");
    if (last_colon != string::npos) {
      // Port found.
      if (last_colon > address_pos) {
        try {
          if (last_slash != string::npos && last_slash > last_colon + 1) {
            // there is a volume in this address
            port = boost::lexical_cast<uint16_t>(
                address.substr(last_colon + 1, last_slash - last_colon - 1));
          } else {
            port = boost::lexical_cast<uint16_t>(address.substr(last_colon + 1));
          }
        } catch(const boost::bad_lexical_cast&) {
          throw InvalidURLException("invalid port: " +
              address.substr(last_colon + 1, last_slash - last_colon - 1));
        }
      } else {
        last_colon = last_slash;
      }
    } else {
      last_colon = last_slash;
    }
    string server = address.substr(address_pos, last_colon - address_pos);

    servers_.push_back(server);
    ports_.push_back(port);

    // Volume is optional.
    if (last_slash > address_pos && last_slash != string::npos) {
      volume_ = address.substr(last_slash + 1, address.length() - last_slash);
    }
  } // for
}

ServiceAddresses PBRPCURL::GetAddresses() const {
  ServiceAddresses addresses;
  ostringstream host;
  assert(servers_.size() == ports_.size());

  ServerList::const_iterator servers_it = servers_.begin();
  PortList::const_iterator ports_it = ports_.begin();

  for (; servers_it != servers_.end(); ++servers_it, ++ports_it) {
    host << *servers_it << ":" << *ports_it;
    addresses.Add(host.str());
    host.str("");
  }
  return addresses;
}

}  // namespace xtreemfs
