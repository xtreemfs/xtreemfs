/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/interrupt.h"

namespace xtreemfs {
/** See interrupt.h. */
boost::thread_specific_ptr<int> SignalHandler::intr_pointer_(0);
/** See interrupt.h. */
int SignalHandler::dummy_ = 1;

} // namespace xtreemfs

