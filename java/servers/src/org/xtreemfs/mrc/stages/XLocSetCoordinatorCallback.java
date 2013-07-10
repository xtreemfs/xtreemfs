/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.stages;

import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.metadata.XLocList;

public interface XLocSetCoordinatorCallback {
    public void installXLocSet(MRCRequest rq, String fileId, XLocList newXLocList, XLocList prevXLocList)
            throws Throwable;
}
