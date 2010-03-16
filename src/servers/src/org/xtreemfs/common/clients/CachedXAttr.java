/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.clients;

import org.xtreemfs.common.TimeSync;

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
