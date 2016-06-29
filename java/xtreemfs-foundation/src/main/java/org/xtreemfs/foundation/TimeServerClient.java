/*
 * Copyright (c) 2010 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation;

import java.net.InetSocketAddress;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;


/**
 * Provides methods to synchronize with a XtreemFS TimeServer, usually provided
 * by a DIR service.
 * 
 * @author flangner
 * @since 03/01/2010
 */

public interface TimeServerClient {

    /**
     * Requests the global time at the given server.
     * 
     * @param server - if null, the default will be used.
     * @return a {@link RPCResponse} future for an UNIX time-stamp.
     */
    public long xtreemfs_global_time_get(InetSocketAddress server);
}