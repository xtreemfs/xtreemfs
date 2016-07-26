/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

/**
 *
 * @author bjko
 */
public class AdvisoryLock {

    /** Representation of the end of file for the *length* of the locked range,
     *  given by fcntl(). */
    public static final long LENGTH_LOCK_TO_EOF = 0;
    
    /** This object's representation of the end of file for the lockEnd value
     *  (*offset*) of the locked range. */
    public static final long END_LOCK_TO_EOF = -1;

    private final boolean exclusive;

    private final long lockStart;

    private final long lockEnd;

    private final String clientUuid;

    private final int    clientPid;

    public AdvisoryLock(long start, long len, boolean exclusive, String clientUuid, int clientPid) {
        this.exclusive = exclusive;
        this.lockStart = start;
        if (len == LENGTH_LOCK_TO_EOF) {
            this.lockEnd = END_LOCK_TO_EOF;
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

        if (this.lockEnd == END_LOCK_TO_EOF) {
            return (other.lockEnd >= this.lockStart) || (other.lockEnd == END_LOCK_TO_EOF);
        }
        if (other.lockEnd == END_LOCK_TO_EOF) {
            return (this.lockEnd >= other.lockStart) || (this.lockEnd == END_LOCK_TO_EOF);
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

    public long getOffset() {
        return lockStart;
    }

    public long getLength() {
        if (lockEnd == END_LOCK_TO_EOF)
            return LENGTH_LOCK_TO_EOF;
        return lockEnd-lockStart+1;
    }

    public boolean isExclusive() {
        return exclusive;
    }
}
