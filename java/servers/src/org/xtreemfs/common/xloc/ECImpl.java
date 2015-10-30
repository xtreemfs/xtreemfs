/*
 * Copyright (c) 2015 by Jan Fajerski, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.xloc;

import java.util.Iterator;

import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;

/** TODO(janf): Brief description of the purpose of this type and its relation to other types. */
public class ECImpl extends StripingPolicyImpl {
    protected final int stripe_size_in_bytes;

    /**
     * @param replica
     * @param relOsdPosition
     */
    ECImpl(Replica replica, int relOsdPosition) {
        super(replica, relOsdPosition);
        stripe_size_in_bytes = policy.getStripeSize() * 1024;
        if (stripe_size_in_bytes <= 0)
            throw new IllegalArgumentException("size must be > 0");
    }

    @Override
    public long getObjectNoForOffset(long fileOffset) {
        return (fileOffset / stripe_size_in_bytes);
    }

    @Override
    public int getOSDforOffset(long fileOffset) {
        return getOSDforObject(getObjectNoForOffset(fileOffset));
    }

    @Override
    public int getOSDforObject(long objectNo) {
        return (int) (objectNo % getWidth());
    }

    @Override
    public long getRow(long objectNo) {
        return objectNo / this.getWidth();
    }

    @Override
    public long getObjectStartOffset(long objectNo) {
        return objectNo * stripe_size_in_bytes;
    }

    @Override
    public long getObjectEndOffset(long objectNo) {
        return getObjectStartOffset(objectNo + 1) - 1;
    }

    @Override
    public String toString() {
        return "StripingPolicy ErasureCodes: " + policy;
    }

    @Override
    public int getStripeSizeForObject(long objectNo) {
        return stripe_size_in_bytes;
    }

    @Override
    public boolean isLocalObject(long objNo, int relativeOsdNo) {
        return objNo % getWidth() == relativeOsdNo;
    }

    @Override
    public Iterator<Long> getObjectsOfOSD(final int osdIndex, final long startObjectNo, final long endObjectNo) {
        return new Iterator<Long>() {
            // first correct objectNo will be set if the first time "next()" is called
            private long object = (getRow(startObjectNo) * getWidth() + osdIndex) - getWidth();

            @Override
            public boolean hasNext() {
                return (object + getWidth() <= endObjectNo);
            }

            @Override
            public Long next() {
                object += getWidth();
                return object;
            }

            /**
             * method does nothing, because it's a virtual iterator
             */
            @Override
            public void remove() {
                // nothing to do
            }
        };
    }

    @Override
    public long getLocalObjectNumber(long objectNo) {
        return getRow(objectNo);
    }

    @Override
    public long getGlobalObjectNumber(long osdLocalObjNo) {
        return osdLocalObjNo * getWidth() + this.relOsdPosition;
    }
}
