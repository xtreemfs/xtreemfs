/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_XCAP_HANDLER_H_
#define CPP_INCLUDE_LIBXTREEMFS_XCAP_HANDLER_H_

namespace xtreemfs {

namespace pbrpc {
class XCap;
}  // namespace pbrpc

/** An interface which allows to retrieve the latest XCap. */
class XCapHandler {
 public:
  virtual ~XCapHandler() {}

  /** Update "outdated_xcap" with latest XCap. */
  virtual void GetXCap(xtreemfs::pbrpc::XCap* outdated_xcap) = 0;
};

}  // namespace xtreemfs


#endif  // CPP_INCLUDE_LIBXTREEMFS_XCAP_HANDLER_H_
