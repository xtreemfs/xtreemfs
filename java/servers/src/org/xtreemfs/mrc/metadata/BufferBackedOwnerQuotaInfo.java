/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;

import org.xtreemfs.mrc.database.babudb.BabuDBStorageHelper;

/**
 * Handles the quoa, blockedSpace or usedSpace value for a user or group quota, defined on creation very the generic
 * parameter.
 */
public class BufferBackedOwnerQuotaInfo extends BufferBackedIndexMetadata implements OwnerQuotaInfo {

    public final static String QUOTA_KEY_IDENTIFIER = "q";

    private final String       id;
    private final OwnerType    ownerType;
    private final QuotaInfo    quotaInfo;

    private long               value;

    public BufferBackedOwnerQuotaInfo(byte[] key, byte[] val) {
        super(key, 0, key.length, val, 0, val.length);

        id = parseId();
        ownerType = parseOwnerType();
        quotaInfo = parseQuotaInfo();

        ByteBuffer tmp = ByteBuffer.wrap(valBuf);
        value = tmp.getLong();
    }

    public BufferBackedOwnerQuotaInfo(OwnerType ownerType, QuotaInfo quotaInfo, String id, long value) {
        super(null, 0, 0, null, 0, 0);

        this.ownerType = ownerType;
        this.quotaInfo = quotaInfo;
        this.id = id;

        keyBuf = BabuDBStorageHelper.createOwnerQuotaInfoKey(ownerType, quotaInfo, id);
        keyLen = keyBuf.length;

        setValue(value);
    }

    private String parseId() {
        String key = new String(keyBuf).substring((QUOTA_KEY_IDENTIFIER + ".").length());
        int idSubstringStart = key.indexOf(".") + 1;
        int idSubstringEnd = key.lastIndexOf(".");

        return key.substring(idSubstringStart, idSubstringEnd);
    }

    public OwnerType parseOwnerType() {
        String key = new String(keyBuf).substring((QUOTA_KEY_IDENTIFIER + ".").length());
        int idSubstringEnd = key.indexOf(".");

        return OwnerType.getByDbAttrSubkey(key.substring(0, idSubstringEnd));
    }

    public QuotaInfo parseQuotaInfo() {
        String key = new String(keyBuf).substring((QUOTA_KEY_IDENTIFIER + ".").length());
        int idSubstringStart = key.lastIndexOf(".") + 1;

        return QuotaInfo.getByDbAttrSubkey(key.substring(idSubstringStart));
    }

    // Getter

    @Override
    public String getId() {
        return id;
    }

    @Override
    public OwnerType getOwnerType() {
        return ownerType;
    }

    @Override
    public QuotaInfo getQuotaInfo() {
        return quotaInfo;
    }

    @Override
    public long getValue() {
        return value;
    }

    // Setter

    @Override
    public void setValue(long value) {
        if (valBuf == null) {
            valLen = Long.SIZE / Byte.SIZE;
            valBuf = new byte[valLen];
        }

        ByteBuffer tmp = ByteBuffer.wrap(valBuf);
        tmp.putLong(value);
    }

    // Used Enums

    public enum OwnerType {

        USER("u"), GROUP("g");

        private final String dbAttrSubKey;

        private OwnerType(String dbAttrSubKey) {
            this.dbAttrSubKey = dbAttrSubKey;
        }

        public static OwnerType getByDbAttrSubkey(String dbAttrSubkey) {
            for (OwnerType ownerType : OwnerType.values()) {
                if (ownerType.getDbAttrSubKey().equals(dbAttrSubkey)) {
                    return ownerType;
                }
            }

            return null;
        }

        /**
         * @return the dbAttrSubKey
         */
        public String getDbAttrSubKey() {
            return dbAttrSubKey;
        }
    }

    public enum QuotaInfo {
        QUOTA("q"), USED("u"), BLOCKED("b");

        private final String dbAttrSubKey;

        private QuotaInfo(String dbAttrSubKey) {
            this.dbAttrSubKey = dbAttrSubKey;
        }

        public static QuotaInfo getByDbAttrSubkey(String dbAttrSubkey) {
            for (QuotaInfo quotaInfo : QuotaInfo.values()) {
                if (quotaInfo.getDbAttrSubKey().equals(dbAttrSubkey)) {
                    return quotaInfo;
                }
            }

            return null;
        }

        /**
         * @return the dbAttrSubKey
         */
        public String getDbAttrSubKey() {
            return dbAttrSubKey;
        }
    }
}
