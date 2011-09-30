/*
 * Copyright (c) 2008 by Nele Andersen,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients.io;

import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;


public class ByteMapperFactory {

    public static ByteMapper createByteMapper(int policy, int stripeSize, ObjectStore store) {
        if( StripingPolicyType.valueOf(policy) == StripingPolicyType.STRIPING_POLICY_RAID0)
            return new ByteMapperRAID0(stripeSize, store);
        throw new IllegalArgumentException("Unknown striping policy ID");
   }
}
