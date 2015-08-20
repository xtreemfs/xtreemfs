/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

import java.util.ArrayList;
import java.util.List;
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
    private final List<Long> xcapExpireTimeList = new ArrayList<Long>();

    /**
     * 
     */
    public ClientVoucherManager(String clientID, String fileID) {
        this.clientID = clientID;
        this.fileID = fileID;
    }

    public void addVoucher(long expireTime) {

        if (!xcapExpireTimeList.contains(expireTime)) {
            xcapExpireTimeList.add(expireTime);
        } else {
            System.out.println("ERROR - FIXME given: " + getClass() + " Method: addVoucher");
            // FIXME(baerhold): throw exception?
            // throw new Exception("ExpireTime (" + expireTime + ") is already in the list for file " +
            // this.fileID);
        }
    }

    public void clearVouchers(Set<Long> expireTimes) {

        for (Long expireTime : expireTimes) {
            if (xcapExpireTimeList.contains(expireTime)) {
                xcapExpireTimeList.remove(expireTime);
            } else {
                System.out.println("ERROR - FIXME given: " + getClass() + " Method: clearVouchers");
                // FIXME(baerhold): exception? this shouldnt occure, but do we care about it?
            }
        }
    }

    public boolean hasActiveExpireTime(long expireTime) {
        return xcapExpireTimeList.contains(expireTime);
    }

    public boolean isEmpty() {
        return xcapExpireTimeList.isEmpty();
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
        return "ClientVoucherManager [fileID=" + fileID + ", clientID=" + clientID + ", xcapExpireTimeList="
                + xcapExpireTimeList + "]";
    }
}
