/*
 * Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_XLOCSET_HANDLER_H_
#define CPP_INCLUDE_LIBXTREEMFS_XLOCSET_HANDLER_H_

namespace xtreemfs {

namespace pbrpc {
class XLocSet;
}  // namespace pbrpc

/** An interface which allows to retrieve the latest XLocSet and renew it if required. */
class XLocSetHandler {
 public:
  virtual ~XLocSetHandler() {}

  /**
   * Get the current xLocSet and update the given one.
   *
   * @remark   Ownership is not transferred.
   **/
  virtual void GetXLocSet(xtreemfs::pbrpc::XLocSet* xlocset) = 0;

  /** Renew the xLocSet synchronously. */
  virtual void RenewXLocSet() = 0;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_XLOCSET_HANDLER_H_
