/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.metadata;

/** TODO: Brief description of the purpose of this type and its relation to other types. */
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
}
