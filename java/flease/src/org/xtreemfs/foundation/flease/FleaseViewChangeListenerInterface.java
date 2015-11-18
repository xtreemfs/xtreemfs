/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease;

import org.xtreemfs.foundation.buffer.ASCIIString;


/**
 * The view change listener is called whenever flease
 * (proposor or acceptor) see a new viewId > current viewId
 * @author bjko
 */
public interface FleaseViewChangeListenerInterface {

    public void viewIdChangeEvent(ASCIIString cellId, int viewId, boolean onProposal);

}
