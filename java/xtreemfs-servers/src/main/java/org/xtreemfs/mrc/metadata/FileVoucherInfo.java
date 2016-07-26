/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.metadata;

/**
 * API for a representation of a file voucher tuple.
 */
public interface FileVoucherInfo {

    /**
     * Increases the client counter by 1.
     */
    public void increaseClientCount();

    /**
     * Reduces the client counter by 1.
     */
    public void decreaseClientCount();

    /**
     * Increases the blocked space by the parameter.
     * 
     * @param additionalBlockedSpace
     */
    public void increaseBlockedSpaceByValue(long additionalBlockedSpace);

    /**
     * Increases the replica count.
     */
    public void increaseReplicaCount();

    /**
     * Decreases the replica count.
     */
    public void decreaseReplicaCount();

    // Getter

    /**
     * @return the clientCount
     */
    public int getClientCount();

    /**
     * @return the filesize
     */
    public long getFilesize();

    /**
     * @return the blockedSpace
     */
    public long getBlockedSpace();

    /**
     * @return the replica count
     */
    public int getReplicaCount();
}
