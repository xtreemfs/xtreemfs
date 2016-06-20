/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.xloc;

import java.util.Iterator;

import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;


/**
 * 
 * @author bjko
 */
public abstract class StripingPolicyImpl {

    protected final StripingPolicy policy;

    protected final int relOsdPosition;

    StripingPolicyImpl(Replica replica, int relOsdPosition) {
        policy = replica.getStripingPolicy();
        this.relOsdPosition = relOsdPosition;
    }

    /**
     *
     * @param replica replica to use
     * @param relOsdPosition relative OSD position in replica (0..width-1)
     * @return
     */
    public static StripingPolicyImpl getPolicy(Replica replica, int relOsdPosition) {
        if (replica.getStripingPolicy().getType() == StripingPolicyType.STRIPING_POLICY_RAID0) {
            return new RAID0Impl(replica,relOsdPosition);
        } else if (replica.getStripingPolicy().getType() == StripingPolicyType.STRIPING_POLICY_ERASURECODE) {
            return new ECStripingPolicyImpl(replica, relOsdPosition);
        } else {
            throw new IllegalArgumentException("unknown striping policy requested...request was " +
                    replica.getStripingPolicy().getType());
        }
    }

    /**
     * returns the width (number of OSDs) of the striping policy
     * 
     * @return
     */
    public int getWidth() {
        return policy.getWidth();
    }

    /**
     * This is only relevant for erasure coded files.
     * 
     * @return the width (number of OSDs) used for parity blocks.
     */
    public int getParityWidth() {
        return policy.getParityWidth();
    }

    public int getPolicyId() {
        return policy.getType().getNumber();
    }

    public StripingPolicy getPolicy() {
        return this.policy;
    }

    /**
     * returns the relative position of the current OSD regarding the ordering of the stripped object
     * 
     * @return the relative OSD position
     */
    public int getRelativeOSDPosition() {
        return relOsdPosition;
    }

    /**
     * returns the object number for the given offset
     * 
     * @param fileOffset
     * @return
     */
    public abstract long getObjectNoForOffset(long fileOffset);

    /**
     * returns the index of the OSD for the given offset
     * 
     * @param fileOffset
     * @return
     */
    public abstract int getOSDforOffset(long fileOffset);

    /**
     * returns the index of the OSD for the given object
     * 
     * @param objectNo
     * @return
     */
    public abstract int getOSDforObject(long objectNo);

    public abstract long getRow(long objectNo);

    /**
     * returns the first offset of this object
     * 
     * @param objectNo
     * @return
     */
    public abstract long getObjectStartOffset(long objectNo);

    /**
     * returns the last offset of this object
     * 
     * @param objectNo
     * @return
     */
    public abstract long getObjectEndOffset(long objectNo);

    public abstract int getStripeSizeForObject(long objectNo);

    public abstract boolean isLocalObject(long objNo, int relativeOsdNo);


    @Deprecated
    public abstract long getLocalObjectNumber(long objectNo);

    @Deprecated
    public abstract long getGloablObjectNumber(long osdLocalObjNo);

    /**
     * Returns a virtual iterator which iterates over all objects the given OSD should save. It starts with
     * the correct object in the row of startObjectNo (inclusive) and ends with endObjectNo (maybe inclusive).
     * 
     * @param osdIndex
     * @param filesize
     * @return
     */
    public abstract Iterator<Long> getObjectsOfOSD(final int osdIndex, final long startObjectNo,
            final long endObjectNo);
}
