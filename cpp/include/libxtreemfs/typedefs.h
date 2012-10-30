/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_TYPEDEFS_H_
#define CPP_INCLUDE_LIBXTREEMFS_TYPEDEFS_H_

#include <list>

namespace xtreemfs {

/** This file contains typedefs which are used by different classes.
 * @file
 */

/** List of DIR or MRC replicas. Addresses have the form "hostname:port". */
typedef std::list<std::string> ServiceAddresses;

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_TYPEDEFS_H_
