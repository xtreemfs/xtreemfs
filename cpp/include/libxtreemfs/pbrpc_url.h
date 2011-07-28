/*
 * Copyright (c)      2011 by Michael Berlin, Zuse Institute Berlin
 *               2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_PBRPC_URL_H_
#define CPP_INCLUDE_LIBXTREEMFS_PBRPC_URL_H_

#include <boost/lexical_cast.hpp>
#include <boost/cstdint.hpp>

#include <string>
#include <utility>

namespace xtreemfs {

using boost::uint16_t;

class PBRPCURL {
 public:

  static const std::string SCHEME_PBRPC;
  static const std::string SCHEME_PBRPCG;
  static const std::string SCHEME_PBRPCS;
  static const std::string SCHEME_PBRPCU;

  PBRPCURL(
      const std::string& scheme, const std::string& server,
      std::string& volume, uint16_t port);

  PBRPCURL();

  static bool protocolContains(std::string url, std::string scheme);

  /** Parses the URL of the form [scheme://]host[:port][/volume].
   *
   * @throws InvalidURLException
   */
  void parseURL(const std::string& url, const std::string& default_scheme,
                const uint16_t default_port);

  uint16_t getPort_() const {
    return port_;
  }

  const std::string& server() const {
    return server_;
  }

  const std::string& scheme() const {
    return scheme_;
  }

  const std::string& volume() const {
    return volume_;
  }

  std::string getAddress() const {
    if (!server_.empty()) {
      return server_+std::string(":")+boost::lexical_cast<std::string>(port_);
    } else {
      return "";
    }
  }

 private:
  std::string scheme_;
  std::string server_;
  uint16_t port_;
  std::string volume_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_PBRPC_URL_H_
