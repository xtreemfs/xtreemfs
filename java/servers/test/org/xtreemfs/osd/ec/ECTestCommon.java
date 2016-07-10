/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;

public class ECTestCommon {

    public static StripingPolicyImpl getStripingPolicyImplementation(FileCredentials fileCredentials) {
        return StripingPolicyImpl.getPolicy(fileCredentials.getXlocs().getReplicas(0), 0);
    }

    public static StripingPolicy getECStripingPolicy(int width, int parity, int stripeSize) {
        return StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_ERASURECODE).setWidth(width)
                .setParityWidth(parity).setStripeSize(stripeSize).build();
    }

    public static void assertBufferEquals(ReusableBuffer expected, ReusableBuffer actual) {
        assertNotNull(actual);

        expected = expected.createViewBuffer();
        actual = actual.createViewBuffer();
        try {
            assertEquals(expected.remaining(), actual.remaining());
            byte[] ex = new byte[expected.remaining()];
            byte[] ac = new byte[expected.remaining()];
            expected.get(ex);
            actual.get(ac);

            assertArrayEquals(ex, ac);
        } finally {
            BufferPool.free(expected);
            BufferPool.free(actual);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void clearAll(List... lists) {
        for (List list : lists) {
            list.clear();
        }
    }
}
