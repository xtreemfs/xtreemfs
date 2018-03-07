/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.comm;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 *
 * @author bjko
 */
public interface FleaseCommunicationInterface {

    public void sendMessage(FleaseMessage msg, InetSocketAddress receiver) throws IOException;

    public void requestTimer(FleaseMessage msg, long timestamp);


}
