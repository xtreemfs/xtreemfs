/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;

import com.google.protobuf.Message;

public interface InternalOperationCallback<M extends Message> {
    void localResultAvailable(M result, ReusableBuffer data);
    void localRequestFailed(ErrorResponse error);
}
