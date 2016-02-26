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
     * Returns the client id for the current object
     * 
     * @return client id
     */
    public String getClientId();

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
     * Compares the given expire time with all expire times of the object and returns true, if one of them is newer than
     * the given one
     * 
     * @param expireTime
     * @return
     */
    public boolean hasNewerExpireTime(long expireTime);

    /**
     * Returns the number of listed expire times.
     */
    public int getExpireTimeSetSize();

}
