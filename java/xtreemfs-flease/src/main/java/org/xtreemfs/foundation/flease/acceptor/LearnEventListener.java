/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.acceptor;

import org.xtreemfs.foundation.buffer.ASCIIString;


/**
 *
 * @author bjko
 */
public interface LearnEventListener {

    public void learnedEvent(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long masterEpochNumber);

}
