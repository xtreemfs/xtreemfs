/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.intervals;

public abstract class Interval {

    /**
     * @return start of the interval.
     */
    public abstract long getStart();

    /**
     * @return end of the interval.
     */
    public abstract long getEnd();

    /**
     * @return version of the interval.
     */
    public abstract long getVersion();

    /**
     * @return id of the related operation.
     */
    public abstract long getId();

    /**
     * @return start of the related operation or {@link #getStart()} if no explicit operation range is defined.
     */
    public long getOpStart() {
        return getStart();
    }

    /**
     * @return end of the related operation or {@link #getEnd()} if no explicit operation range is defined.
     */
    public long getOpEnd() {
        return getEnd();
    }

    /**
     * Special Interval subclasses could have an attachment.
     * 
     * @return the attachment if existent or null
     */
    public Object getAttachment() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
    
        // if (getClass() != obj.getClass())
        // return false;
    
        if (!(obj instanceof Interval))
            return false;
    
        Interval other = (Interval) obj;
        if (getStart() != other.getStart())
            return false;
        if (getEnd() != other.getEnd())
            return false;
        if (getVersion() != other.getVersion())
            return false;
        if (getId() != other.getId())
            return false;
        return true;
    }

    public boolean equalsVersionId(Interval o) {
        if (o == null) {
            return false;
        }
        return (getVersion() == o.getVersion() && getId() == o.getId());
    }

    @Override
    public String toString() {
        return String.format("([%d:%d], %d, %d)", getStart(), getEnd(), getVersion(), getId());
    }
}