/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.util.Vector;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;

public interface StripeTranslator {

    /**
     * Create a new {@link ReadOperation} from "size","offset" and "policy" and append the new
     * {@link ReadOperation} to operations.
     * 
     * @param size
     * @param offset
     * @param policy
     * @param operations
     */
    public abstract void translateReadRequest(int size, long offset, StripingPolicy policy,
            Vector<ReadOperation> operations);

    /**
     * Create a new {@link WriteOperation} from "size","offset" and "policy" and append the new
     * {@link WriteOperation} to operations.
     * 
     * @param size
     * @param offset
     * @param policy
     * @param buf
     * @param operations
     */
    public abstract void translateWriteRequest(int size, long offset, StripingPolicy policy,
            ReusableBuffer buf, Vector<WriteOperation> operations);
}
