/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients;

import org.xtreemfs.foundation.TimeSync;

/**
 *
 * @author bjko
 */
class CachedXAttr {
    private String   value;
    private long timestamp;

    CachedXAttr(String value) {
        this.timestamp = TimeSync.getLocalSystemTime();
        this.value = value;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
        this.timestamp = TimeSync.getLocalSystemTime();
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }



}
