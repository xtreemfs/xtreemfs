/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.client;

import com.google.protobuf.Message;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public interface RPCResponseListener<V extends Message> {

    public void responseAvailable(RPCClientRequest<V> request, ReusableBuffer message, ReusableBuffer data);

    public void requestFailed(String reason);

}
