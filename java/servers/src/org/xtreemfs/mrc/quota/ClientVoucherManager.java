/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TODO: Brief description of the purpose of this type and its relation to other types.
 * 
 * 
 * This class is not thread-safe by itself, but the only access is via FileVoucherManager being thread-safe.
 * 
 * 
 * */
public class ClientVoucherManager {

    private final String          fileID;
    private final String          clientID;

    // manage xcaps by expire time <> voucher size
    private final Map<Long, Long> xcapExpireTimeMap = new HashMap<Long, Long>();

    /**
     * 
     */
    public ClientVoucherManager(String clientID, String fileID) {
        this.clientID = clientID;
        this.fileID = fileID;
    }

    public void addVoucher(long expireTime, long voucherSize) {

        if (!xcapExpireTimeMap.containsKey(expireTime)) {
            xcapExpireTimeMap.put(expireTime, voucherSize);
        } else {
            System.out.println("ERROR - FIXME given");
            // FIXME: throw exception?
            // throw new Exception("ExpireTime (" + expireTime + ") is already in the list for file " +
            // this.fileID);
        }
    }

    public void clearVouchers(Set<Long> expireTimes) {

        for (Long expireTime : expireTimes) {
            if (xcapExpireTimeMap.containsKey(expireTime)) {
                xcapExpireTimeMap.remove(expireTime);
            } else {
                System.out.println("ERROR - FIXME given");
                // FIXME: exception? this shouldnt occure, but do we care about it?
            }
        }
    }

    public boolean hasActiveExpireTime(long expireTime) {
        return xcapExpireTimeMap.containsKey(expireTime);
    }

    public boolean isEmpty() {
        return xcapExpireTimeMap.isEmpty();
    }

    /**
     * @return the fileID
     */
    public String getFileID() {
        return fileID;
    }

    /**
     * @return the clientID
     */
    public String getClientID() {
        return clientID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ClientVoucherManager [fileID=" + fileID + ", clientID=" + clientID + ", xcapExpireTimeMap="
                + xcapExpireTimeMap + "]";
    }
}
