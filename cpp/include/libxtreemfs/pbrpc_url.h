/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *               2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_PBRPC_URL_H_
#define CPP_INCLUDE_LIBXTREEMFS_PBRPC_URL_H_

#include <stdint.h>

#include <boost/lexical_cast.hpp>
#include <list>
#include <string>

#include "libxtreemfs/typedefs.h"

namespace xtreemfs {

class PBRPCURL {
 public:
  PBRPCURL();

  /** Parses the URL of the form [scheme://]host[:port][/volume].
   *
   * Multiple entries of "[scheme://]host[:port]" can be given as comma
   * separated list e.g., the following URL would also be valid:
   *
   * [scheme://]host0[:port],[scheme://]host1[:port][/volume]
   *
   * @throws InvalidURLException
   */
  void ParseURL(const std::string& url,
                const std::string& default_scheme,
                const uint16_t default_port);

  const std::string& scheme() const {
    return scheme_;
  }

  const std::string& volume() const {
    return volume_;
  }

  ServiceAddresses GetAddresses() const;

  static const std::string& GetSchemePBRPC() {
    static const std::string SCHEME_PBRPC_STRING = "pbrpc";
    return SCHEME_PBRPC_STRING;
  }
  static const std::string& GetSchemePBRPCS() {
    static const std::string SCHEME_PBRPCS_STRING = "pbrpcs";
    return SCHEME_PBRPCS_STRING;
  }
  static const std::string& GetSchemePBRPCG() {
    static const std::string SCHEME_PBRPCG_STRING = "pbrpcg";
    return SCHEME_PBRPCG_STRING;
  }
  static const std::string GetSchemePBRPCU() {
    static const std::string SCHEME_PBRPCU_STRING = "pbrpcu";
    return SCHEME_PBRPCU_STRING;
  }

 private:
  /** List of servers (hostnames only) */
  typedef std::list<std::string> ServerList;
  /** List of ports */
  typedef std::list<uint16_t> PortList;

  std::string scheme_;
  /** List of all parsed server's hostnames. Ports are stored in ports_ */
  ServerList servers_;
  /** Ports for the hostnames in servers. Server and ports with the same list
   *  position belong together. */
  PortList ports_;
  std::string volume_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_PBRPC_URL_H_
