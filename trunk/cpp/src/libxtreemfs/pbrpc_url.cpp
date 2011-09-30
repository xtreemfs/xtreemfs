/*
 * Copyright (c) 2009-2011 by Patrick Schaefer, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "libxtreemfs/pbrpc_url.h"

#include <string>
#include <utility>

#include "libxtreemfs/xtreemfs_exception.h"

namespace xtreemfs {

const std::string PBRPCURL::SCHEME_PBRPC = "pbrpc";
const std::string PBRPCURL::SCHEME_PBRPCS = "pbrpcs";
const std::string PBRPCURL::SCHEME_PBRPCG = "pbrpcg";
const std::string PBRPCURL::SCHEME_PBRPCU = "pbrpcu";

PBRPCURL::PBRPCURL(const std::string& scheme,
    const std::string& server, std::string& volume, uint16_t port)
    : scheme_(scheme), server_(server), port_(port), volume_(volume) {
}

PBRPCURL::PBRPCURL() : scheme_(""), server_(""), port_(0), volume_("") {
}

bool PBRPCURL::protocolContains(std::string url, std::string scheme) {
  size_t scheme_pos = url.find("://");
  if (scheme_pos != std::string::npos) {
    std::string s = url.substr(0, scheme_pos);
    return scheme == s;
  }
  return false;
}

void PBRPCURL::parseURL(
    const std::string& url,
    const std::string& default_scheme,
    const uint16_t default_port) {
  scheme_ = default_scheme;
  size_t scheme_pos = url.find("://");
  size_t address_pos = 0;
  if (scheme_pos != std::string::npos) {
    // scheme specified
    scheme_ = url.substr(0, scheme_pos);
    if ((scheme_ != SCHEME_PBRPC)
        && (scheme_ != SCHEME_PBRPCS)
        && (scheme_ != SCHEME_PBRPCG)
        && (scheme_ != SCHEME_PBRPCU)) {
      throw InvalidURLException(scheme_ + " is not a valid scheme");
    }
    address_pos = scheme_pos + 3;
  }
  port_ = default_port;

  size_t last_slash = url.find_last_of("/");
  size_t last_colon = url.find_last_of(":");
  if (last_colon != std::string::npos) {
    // Port found.
    if (last_colon > address_pos) {
      try {
        if (last_slash !=  std::string::npos && last_slash > last_colon+1) {
          port_ = boost::lexical_cast<uint16_t>(
              url.substr(last_colon + 1, last_slash-last_colon-1));
        }
      } catch(const boost::bad_lexical_cast&) {
        throw InvalidURLException("invalid port: " +
            url.substr(last_colon + 1, last_slash-last_colon-1));
      }
    } else {
      last_colon = last_slash;
    }
  } else {
    last_colon = last_slash;
  }

  // volume is optional
  if (last_slash > address_pos && last_slash != std::string::npos) {
    volume_ = url.substr(last_slash+1, url.length() - last_slash);
  }

  server_ = url.substr(address_pos, last_colon-address_pos);
}

}  // namespace xtreemfs
