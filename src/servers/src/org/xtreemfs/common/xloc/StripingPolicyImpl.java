/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.xloc;

import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;

/**
 *
 * @author bjko
 */
public abstract class StripingPolicyImpl {

    protected final StripingPolicy policy;

    StripingPolicyImpl(Replica replica) {
        policy = replica.getStriping_policy();
    }

    public int getWidth() {
        return policy.getWidth();
    }

    public int getPolicyId() {
        return policy.getType().intValue();
    }

    public StripingPolicy getPolicy() {
        return this.policy;
    }

    public abstract long getObjectNoForOffset(long fileOffset);

    public abstract int getOSDforOffset(long fileOffset);

    public abstract int getOSDforObject(long objectNo);

    public abstract long getRow(long objectNo);

    public abstract long getObjectStartOffset(long objectNo);

    public abstract long getObjectEndOffset(long objectNo);

    public abstract int  getStripeSizeForObject(long objectNo);

    public static StripingPolicyImpl getPolicy(Replica replica) {
        if (replica.getStriping_policy().getType() == StripingPolicyType.STRIPING_POLICY_RAID0) {
            return new RAID0Impl(replica);
        } else {
            throw new IllegalArgumentException("unknown striping polciy requested");
        }
    }

    public abstract boolean isLocalObject(long objNo, int relativeOsdNo);

}
