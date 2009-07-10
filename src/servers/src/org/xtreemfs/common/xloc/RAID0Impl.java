/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.common.xloc;

import java.util.Iterator;

import org.xtreemfs.interfaces.Replica;

/**
 * 
 * @author bjko
 */
public class RAID0Impl extends StripingPolicyImpl {

    protected final int stripe_size_in_bytes;

    RAID0Impl(Replica replica) {
        super(replica);
        stripe_size_in_bytes = policy.getStripe_size() * 1024;
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

    public String toString() {
        return "StripingPolicy RAID0: " + policy;
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
    public Iterator<Long> getObjectsOfOSD(final int osdIndex, final long startObjectNo,
            final long endObjectNo) {
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
}
