/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease;

import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.proposer.FleaseException;

/**
 *
 * @author bjko
 */
public interface FleaseStatusListener {

    public void statusChanged(ASCIIString cellId, Flease lease);

    public void leaseFailed(ASCIIString cellId, FleaseException error);

}
