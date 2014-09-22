/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Vector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.test.TestHelper;

/**
 * Test against the bug from Issue 277 (erroneous calculation of object offsets for read requests). The test
 * iterates over an offset range from 1.5 to 2.5 GiB in steps of 109 ensuring that offset calculation only
 * produces offsets greater equal 0. The range is chosen to include offsets lesser equal and greater than
 * Integer.MAX_VALUE. A prime is used as step to enlarge the coverage of the range of offsets.
 * 
 * @author jensvfischer
 */
public class StripeTranslatorTest {
    @Rule
    public final TestRule           testLog                      = TestHelper.testLog;

    private static final int        XTREEMFS_BLOCK_SIZE_IN_BYTES = 128 * 1024;
    private static final int        GiB_IN_BYTES                 = 1024 * 1024 * 1024;
    private static int              count                        = XTREEMFS_BLOCK_SIZE_IN_BYTES;
    private static long             maxOffset                    = (long) (2.5 * (long) GiB_IN_BYTES);
    private static StripeTranslator translator                   = new StripeTranslatorRaid0();

    /**
     * Test for read requests
     */
    @Test
    public void testReadRequestOffsetCalculation() throws NoSuchFieldException, IllegalAccessException {

        for (long offset = (long) (1.5 * (long) GiB_IN_BYTES); offset < maxOffset; offset += 997) {

            // Map offsets
            Vector<ReadOperation> operations = new Vector<ReadOperation>();
            translator.translateReadRequest(count, offset,
                    StripingPolicy.newBuilder().setStripeSize(128).setType(StripingPolicyType.STRIPING_POLICY_RAID0)
                            .setWidth(1).build(), operations);

            // make the private field 'reqOffset" of ReadOperation accessible
            Field reqOffset = ReadOperation.class.getDeclaredField("reqOffset");
            reqOffset.setAccessible(true);

            // assert that all object offsets are greater equal zero
            for (ReadOperation operation : operations) {
                assertTrue((Integer) reqOffset.get(operation) >= 0);
            }
        }
    }

    /**
     * Test for write requests. A larger prime is chosen because the offset calculation for WriteRequest is
     * much slower.
     */
    @Test
    public void testWriteRequestOffsetCalculation() throws NoSuchFieldException, IllegalAccessException {

        byte[] data = new byte[XTREEMFS_BLOCK_SIZE_IN_BYTES];
        ReusableBuffer buffer = ReusableBuffer.wrap(data);

        for (long offset = (long) (1.5 * (long) GiB_IN_BYTES); offset < maxOffset; offset += 9973) {

            // Map offsets
            Vector<WriteOperation> operations = new Vector<WriteOperation>();
            translator.translateWriteRequest(count, offset,
                    StripingPolicy.newBuilder().setStripeSize(128).setType(StripingPolicyType.STRIPING_POLICY_RAID0)
                            .setWidth(1).build(), buffer, operations);

            // make the private field 'reqOffset" of WriteOperation accessible
            Field reqOffset = WriteOperation.class.getDeclaredField("reqOffset");
            reqOffset.setAccessible(true);

            // assert that all object offsets are greater equal zero
            for (WriteOperation operation : operations) {
                assertTrue((Integer) reqOffset.get(operation) >= 0);
            }

        }
    }
}
