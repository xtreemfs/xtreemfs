/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper;
import org.xtreemfs.mrc.metadata.BufferBackedFileVoucherClientInfo;
import org.xtreemfs.mrc.metadata.FileVoucherClientInfo;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestHelper;

public class BufferBackedFileVoucherClientInfoTest {

    @Rule
    public final TestRule testLog = TestHelper.testLog;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    @SuppressWarnings("serial")
    @Test
    public void test() {

        final long firstExpireTime = 948372651 * 10;
        final long secondExpireTime = 98765;
        final long thirdExpireTime = 1234567;

        final long fileId = 987;
        final String clientId = "127.0.0.1";
        final Set<Long> expireTimeSet = new HashSet<Long>();
        expireTimeSet.add(firstExpireTime);
        expireTimeSet.add(secondExpireTime);
        expireTimeSet.add(thirdExpireTime);

        // value constructor
        BufferBackedFileVoucherClientInfo fileVoucherClientInfo = new BufferBackedFileVoucherClientInfo(fileId,
                clientId, firstExpireTime);
        checkFileVoucherClientInfoKey(fileVoucherClientInfo, fileId, clientId);
        checkExpireTimes(fileVoucherClientInfo, new HashSet<Long>() {
            {
                add(firstExpireTime);
            }
        });

        // add single value
        fileVoucherClientInfo.addExpireTime(secondExpireTime);
        checkExpireTimes(fileVoucherClientInfo, new HashSet<Long>() {
            {
                add(firstExpireTime);
                add(secondExpireTime);
            }
        });

        // byte buffer constructor
        BufferBackedFileVoucherClientInfo fileVoucherClientInfo2 = new BufferBackedFileVoucherClientInfo(
                fileVoucherClientInfo.getKeyBuf(), fileVoucherClientInfo.getValBuf());
        checkFileVoucherClientInfoKey(fileVoucherClientInfo, fileId, clientId);
        checkExpireTimes(fileVoucherClientInfo2, new HashSet<Long>() {
            {
                add(firstExpireTime);
                add(secondExpireTime);
            }
        });

        // add Set
        fileVoucherClientInfo2.addExpireTimeSet(expireTimeSet);
        checkExpireTimes(fileVoucherClientInfo2, expireTimeSet);

        // remove values
        fileVoucherClientInfo2.removeExpireTimeSet(new HashSet<Long>() {
            {
                add(firstExpireTime);
                add(thirdExpireTime);
            }
        });
        checkExpireTimes(fileVoucherClientInfo2, new HashSet<Long>() {
            {
                add(secondExpireTime);
            }
        });

        // clear all values
        fileVoucherClientInfo2.clearExpireTimeSet();
        checkExpireTimes(fileVoucherClientInfo2, new HashSet<Long>());
    }

    public void checkExpireTimes(FileVoucherClientInfo fileVoucherClientInfo, Set<Long> expireTimeSet) {

        assertEquals(expireTimeSet.size(), fileVoucherClientInfo.getExpireTimeSetSize());

        for (Long expireTime : expireTimeSet) {
            assertTrue(fileVoucherClientInfo.hasExpireTime(expireTime));
        }
    }

    private void checkFileVoucherClientInfoKey(BufferBackedFileVoucherClientInfo fileVoucherClientInfo, long fileId,
            String clientId) {
        assertArrayEquals(BabuDBStorageHelper.createFileVoucherClientInfoKey(fileId, clientId),
                fileVoucherClientInfo.getKeyBuf());
    }

}
