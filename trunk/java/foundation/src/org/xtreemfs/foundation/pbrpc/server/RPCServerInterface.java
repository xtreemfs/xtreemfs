/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.server;

/**
 *
 * @author bjko
 */
public interface RPCServerInterface {
    
    public void sendResponse(RPCServerRequest request, RPCServerResponse response);

}
