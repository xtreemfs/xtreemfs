/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.flease.proposer;

import org.xtreemfs.foundation.buffer.ASCIIString;

/**
 *
 * @author bjko
 */
public interface FleaseListener {

    public void proposalResult(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long masterEpochNumber);

    public void proposalFailed(ASCIIString cellId, Throwable cause);

}
