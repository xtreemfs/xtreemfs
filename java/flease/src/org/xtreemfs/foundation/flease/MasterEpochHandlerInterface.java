/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease;

import org.xtreemfs.foundation.flease.comm.FleaseMessage;

/**
 *
 * @author bjko
 */
public interface MasterEpochHandlerInterface {

    public void sendMasterEpoch(FleaseMessage response, Continuation callback);

    public void storeMasterEpoch(FleaseMessage request, Continuation callback);

    public static interface Continuation {
        void processingFinished();
    }

}
