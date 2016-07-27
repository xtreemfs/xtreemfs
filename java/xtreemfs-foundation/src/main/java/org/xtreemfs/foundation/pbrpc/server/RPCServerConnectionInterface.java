/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.foundation.pbrpc.server;

import java.net.SocketAddress;

import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;

/**
 *
 * @author bjko
 */
public interface RPCServerConnectionInterface {

   public RPCServerInterface getServer();

   public SocketAddress  getSender();
   
   public ChannelIO getChannel();

}
