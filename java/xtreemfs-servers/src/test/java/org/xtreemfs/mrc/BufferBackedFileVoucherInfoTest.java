/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper;
import org.xtreemfs.mrc.metadata.BufferBackedFileVoucherInfo;
import org.xtreemfs.mrc.metadata.FileVoucherInfo;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestHelper;

public class BufferBackedFileVoucherInfoTest {

    @Rule
    public final TestRule testLog = TestHelper.testLog;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    @Test
    public void testRegularValues() {
        final long fileId = 1234;
        final long filesize = 192837465;
        final long blockedSpace = 100 * 1024;
        final int replicaCount = 2;
        final int clientCount = 1;

        // value constructor
        BufferBackedFileVoucherInfo fileVoucherInfo = new BufferBackedFileVoucherInfo(fileId, filesize, replicaCount,
                blockedSpace);
        checkFileVoucherInfoValues(fileVoucherInfo, filesize, replicaCount, blockedSpace, clientCount);
        checkFileVoucherInfoKey(fileVoucherInfo, fileId);

        // byte buffer constructor
        BufferBackedFileVoucherInfo fileVoucherInfo2 = new BufferBackedFileVoucherInfo(fileVoucherInfo.getKeyBuf(),
                fileVoucherInfo.getValBuf());
        checkFileVoucherInfoValues(fileVoucherInfo2, filesize, replicaCount, blockedSpace, clientCount);
        checkFileVoucherInfoKey(fileVoucherInfo2, fileId);
    }

    @Test
    public void testChangedValues() {
        final long fileId = 0;
        final long filesize = 0;

        long blockedSpace = 100 * 1024;
        int replicaCount = 0;
        int clientCount = 1;

        // value constructor
        BufferBackedFileVoucherInfo fileVoucherInfo = new BufferBackedFileVoucherInfo(fileId, filesize, replicaCount,
                blockedSpace);
        checkFileVoucherInfoValues(fileVoucherInfo, filesize, replicaCount, blockedSpace, clientCount);
        checkFileVoucherInfoKey(fileVoucherInfo, fileId);

        // change values
        fileVoucherInfo.increaseClientCount();
        clientCount++;
        fileVoucherInfo.increaseClientCount();
        clientCount++;

        long additionalBlockedSpace = 250 * 1024 * 1024;
        fileVoucherInfo.increaseBlockedSpaceByValue(additionalBlockedSpace);
        blockedSpace += additionalBlockedSpace;

        fileVoucherInfo.increaseClientCount();
        clientCount++;
        fileVoucherInfo.increaseClientCount();
        clientCount++;

        fileVoucherInfo.increaseReplicaCount();
        replicaCount++;
        fileVoucherInfo.increaseReplicaCount();
        replicaCount++;

        checkFileVoucherInfoValues(fileVoucherInfo, filesize, replicaCount, blockedSpace, clientCount);

        // byte buffer constructor
        BufferBackedFileVoucherInfo fileVoucherInfo2 = new BufferBackedFileVoucherInfo(fileVoucherInfo.getKeyBuf(),
                fileVoucherInfo.getValBuf());
        checkFileVoucherInfoValues(fileVoucherInfo2, filesize, replicaCount, blockedSpace, clientCount);
        checkFileVoucherInfoKey(fileVoucherInfo2, fileId);

        fileVoucherInfo2.decreaseClientCount();
        clientCount--;

        fileVoucherInfo2.decreaseReplicaCount();
        replicaCount--;

        checkFileVoucherInfoValues(fileVoucherInfo2, filesize, replicaCount, blockedSpace, clientCount);

    }

    /**
     * @param fileVoucherInfo
     * @param fileId
     */
    private void checkFileVoucherInfoKey(BufferBackedFileVoucherInfo fileVoucherInfo, long fileId) {
        assertArrayEquals(BabuDBStorageHelper.createFileVoucherInfoKey(fileId), fileVoucherInfo.getKeyBuf());
    }

    /**
     * @param fileVoucherInfo
     * @param filesize
     * @param replicaCount
     * @param blockedSpace
     */
    private void checkFileVoucherInfoValues(FileVoucherInfo fileVoucherInfo, long filesize, int replicaCount,
            long blockedSpace, long clientCount) {

        assertEquals(filesize, fileVoucherInfo.getFilesize());
        assertEquals(replicaCount, fileVoucherInfo.getReplicaCount());
        assertEquals(blockedSpace, fileVoucherInfo.getBlockedSpace());
        assertEquals(clientCount, fileVoucherInfo.getClientCount());
    }
}
