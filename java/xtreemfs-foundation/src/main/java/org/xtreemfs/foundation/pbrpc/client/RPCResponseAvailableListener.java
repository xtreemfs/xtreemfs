/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.foundation.pbrpc.client;

import com.google.protobuf.Message;


/**
 *
 * @author bjko
 */
public interface RPCResponseAvailableListener<V extends Message> {

    public void responseAvailable(RPCResponse<V> r);

}
