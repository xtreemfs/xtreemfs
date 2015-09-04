/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.metadata;

import java.util.Set;

public interface FileVoucherClientInfo {

    /**
     * Adds the given expire time for the current client.
     * 
     * @param expireTime
     */
    public void addExpireTime(long expireTime);

    /**
     * Adds the given expire time set for the current client.
     * 
     * @param expireTimeSet
     */
    public void addExpireTimeSet(Set<Long> expireTimeSet);

    /**
     * Removes the given expire time set for the current client.
     * 
     * @param expireTimeSet
     */
    public void removeExpireTimeSet(Set<Long> expireTimeSet);

    /**
     * Removes all saved expire time set, mostly used to delete the object.
     */
    public void clearExpireTimeSet();

    /**
     * Check, whether the expire time is already listed or not.
     * 
     * @param expireTime
     * @return
     */
    public boolean hasExpireTime(long expireTime);

    /**
     * Returns the number of listed expire times.
     */
    public int getExpireTimeSetSize();

}
