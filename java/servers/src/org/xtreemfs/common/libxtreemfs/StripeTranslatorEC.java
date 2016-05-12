/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.util.Vector;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;

/**
 * The ErasureCode (EC) Stripe Translator won't translate the requests to stripes, since it is required to send the
 * whole requests to the EC master to ensure atomic operations.
 *
 */
public class StripeTranslatorEC implements StripeTranslator {

    public void translateWriteRequest(int size, long offset, StripingPolicy policy, ReusableBuffer buf,
            Vector<WriteOperation> operations) {
        int stripeSize = policy.getStripeSize() * 1024; // stripe size in kB

        // objNumber of the first object that will be read
        long objNumber = offset / stripeSize;
        int reqOffset = (int) (offset % stripeSize);

        operations.add(new WriteOperation(objNumber, 0, size, reqOffset, buf));
    }

    public void translateReadRequest(int size, long offset, StripingPolicy policy,
            Vector<ReadOperation> operations) {
        int stripeSize = policy.getStripeSize() * 1024; // stripe size in kB

        // objNumber of the first object that will be read
        long objNumber = offset / stripeSize;
        int reqOffset = (int) (offset % stripeSize);

        operations.add(new ReadOperation(objNumber, 0, size, reqOffset, 0));
    }
}
