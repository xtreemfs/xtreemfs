/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd;

/**
 *
 * @author bjko
 */
public class AdvisoryLock {


    public static final long LOCK_TO_EOF = 0;

    private final boolean exclusive;

    private final long lockStart;

    private final long lockEnd;

    private final String clientUuid;

    private final int    clientPid;

    public AdvisoryLock(long start, long len, boolean exclusive, String clientUuid, int clientPid) {
        this.exclusive = exclusive;
        this.lockStart = start;
        if (len == LOCK_TO_EOF) {
            this.lockEnd = LOCK_TO_EOF;
        } else {
            long end = lockStart+len-1;
            if (end < 0)
                end = 0;
            lockEnd = end;
        }
        this.clientPid = clientPid;
        this.clientUuid = clientUuid;
    }

    public boolean isOverlappingRanges(final AdvisoryLock other) {

        if (this.lockEnd == LOCK_TO_EOF) {
            return (other.lockEnd >= this.lockStart) || (other.lockEnd == LOCK_TO_EOF);
        }
        if (other.lockEnd == LOCK_TO_EOF) {
            return (this.lockEnd >= other.lockStart) || (this.lockEnd == LOCK_TO_EOF);
        }
        if ( (this.lockEnd < other.lockStart)
              || (this.lockStart > other.lockEnd) ) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isConflicting(AdvisoryLock other) {
        if (isOverlappingRanges(other)) {
            return (this.exclusive || other.exclusive);
        } else {
            return false;
        }
    }

    /**
     * @return the clientUuid
     */
    public String getClientUuid() {
        return clientUuid;
    }

    /**
     * @return the clientPid
     */
    public int getClientPid() {
        return clientPid;
    }

}
