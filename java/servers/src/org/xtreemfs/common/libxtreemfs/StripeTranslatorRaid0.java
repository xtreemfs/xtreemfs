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

/**
 * 
 * TODO: Write Description.
 */
public class StripeTranslatorRaid0 implements StripeTranslator {

    public void translateWriteRequest(int size, int offset, StripingPolicy policy, ReusableBuffer buf,
            Vector<WriteOperation> operations) {
        // need to know stripe size and stripe width
        int stripeSize = policy.getStripeSize() * 1024; // stripe size in kB
        int osdCount = policy.getWidth();

        int start = 0;
        while (start < size) {
            int objNumber = (start + offset) / stripeSize;
            int osdOffset = objNumber % osdCount;
            int reqOffset = (start + offset) % stripeSize;
            // min((size - start), (stripeSize - reqOffset))
            int reqSize = (size - start) < (stripeSize - reqOffset) ? (size - start)
                    : (stripeSize - reqOffset);

            operations.add(new WriteOperation(objNumber, osdOffset, reqSize, reqOffset, buf));

            start += reqSize;
        }
    }

    public void translateReadRequest(int size, int offset, StripingPolicy policy,
            Vector<ReadOperation> operations) {
        // need to know stripe size and stripe width
        int stripeSize = policy.getStripeSize() * 1024; // strip size in kB
        int osdCount = policy.getWidth();

        int start = 0;
        while (start < size) {
            int objNumber = (start + offset) / stripeSize;
            int osdOffset = objNumber % osdCount;
            int reqOffset = (start + offset) % stripeSize;
            // min((size - start), (stripeSize - reqOffset))
            int reqSize = (size - start) < (stripeSize - reqOffset) ? (size - start)
                    : (stripeSize - reqOffset);

            operations.add(new ReadOperation(objNumber, osdOffset, reqSize, reqOffset, start));

            start += reqSize;
        }
    }
}
