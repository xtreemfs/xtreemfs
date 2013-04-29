/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.stages;

import org.xtreemfs.mrc.MRCRequest;

public interface InternalCallbackInterface {
    public void startCallback(MRCRequest rq) throws Throwable;
}
