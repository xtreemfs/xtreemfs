/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.stages;

import org.xtreemfs.mrc.MRCRequest;

/**
 * Internal callbacks can be used to execute something in the context of the ProcessingStage. This is needed to ensure
 * database calls are exclusive and always from the same process.
 * 
 * @see ProcessingStage#enqueueInternalCallbackOperation(MRCRequest, InternalCallbackInterface)
 */
public interface InternalCallbackInterface {
    public void execute() throws Throwable;
}
