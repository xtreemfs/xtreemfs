/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper;

/**
 * Contains all client specific expire times regarding its voucher of a specific file.
 */
public class BufferBackedFileVoucherClientInfo extends BufferBackedIndexMetadata implements FileVoucherClientInfo {

    private final Set<Long> expireTimeSet = new HashSet<Long>();

    public BufferBackedFileVoucherClientInfo(byte[] key, byte[] val) {
        super(key, 0, key.length, val, 0, val.length);

        ByteBuffer tmp = ByteBuffer.wrap(valBuf);
        for (int i = 0; i < val.length; i += Long.SIZE / Byte.SIZE) {
            expireTimeSet.add(tmp.getLong(i));
        }
    }

    public BufferBackedFileVoucherClientInfo(long fileId, String clientId, long expireTime) {

        super(null, 0, 0, null, 0, 0);

        keyBuf = BabuDBStorageHelper.createFileVoucherClientInfoKey(fileId, clientId);
        keyLen = keyBuf.length;

        addExpireTime(expireTime);
        updateValueBuffer();
    }

    @Override
    public void addExpireTime(long expireTime) {
        expireTimeSet.add(expireTime);
    }

    @Override
    public void addExpireTimeSet(Set<Long> expireTimeSet) {
        this.expireTimeSet.addAll(expireTimeSet);
    }

    @Override
    public void removeExpireTimeSet(Set<Long> expireTimeSet) {
        this.expireTimeSet.removeAll(expireTimeSet);
    }

    @Override
    public void clearExpireTimeSet() {
        expireTimeSet.clear();
    }

    @Override
    public boolean hasExpireTime(long expireTime) {
        return expireTimeSet.contains(expireTime);
    }

    /**
     * Updates the value buffer with the expire times. Iff the set of expireTime is empty, it will be set to null in
     * order to delete the entry.
     */
    private void updateValueBuffer() {

        if (expireTimeSet.size() == 0) {
            valLen = 0;
            valBuf = null;
        } else {
            int curExpireTimeOffset = 0;
            int longSizeBytes = Long.SIZE / Byte.SIZE;

            valLen = longSizeBytes * expireTimeSet.size();
            valBuf = new byte[valLen];
            ByteBuffer tmp = ByteBuffer.wrap(valBuf);

            for (Long expireTime : expireTimeSet) {
                tmp.putLong(curExpireTimeOffset, expireTime);
                curExpireTimeOffset += longSizeBytes;
            }
        }
    }

    // Getter

    /**
     * Returns the value buffer after refreshing it with the current values.
     */
    @Override
    public byte[] getValBuf() {
        updateValueBuffer();

        return super.getValBuf();
    }

    @Override
    public int getExpireTimeSetSize() {
        return expireTimeSet.size();
    }
}
