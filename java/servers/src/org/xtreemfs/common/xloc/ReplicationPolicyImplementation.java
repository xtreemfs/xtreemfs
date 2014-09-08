/*
 * Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.xloc;

import org.xtreemfs.mrc.metadata.ReplicationPolicy;

public class ReplicationPolicyImplementation implements ReplicationPolicy {

    final String updatePolicy;
    final int    replicationFactor;
    final int    replicationFlags;

    public ReplicationPolicyImplementation(String updatePolicy, int replicationFactor, int replicationFlags) {
        this.updatePolicy = updatePolicy;
        this.replicationFactor = replicationFactor;
        this.replicationFlags = replicationFlags;
    }

    @Override
    public String getName() {
        return updatePolicy;
    }

    @Override
    public int getFactor() {
        return replicationFactor;
    }

    @Override
    public int getFlags() {
        return replicationFlags;
    }

}
