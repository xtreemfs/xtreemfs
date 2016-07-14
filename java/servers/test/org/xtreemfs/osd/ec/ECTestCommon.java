/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.test.SetupUtils;

public class ECTestCommon {

    public static StripingPolicyImpl getStripingPolicyImplementation(FileCredentials fileCredentials) {
        return StripingPolicyImpl.getPolicy(fileCredentials.getXlocs().getReplicas(0), 0);
    }

    public static StripingPolicy getECStripingPolicy(int width, int parity, int stripeSize) {
        return StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_ERASURECODE).setWidth(width)
                .setParityWidth(parity).setStripeSize(stripeSize).build();
    }

    public static StripingPolicy getECStripingPolicy(int width, int parity, int stripeSize, int qw) {
        return StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_ERASURECODE).setWidth(width)
                .setParityWidth(parity).setStripeSize(stripeSize).setEcWriteQuorum(qw).build();
    }

    public static Capability getCap(String fileId) {
        try {
            return new Capability(fileId,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                            | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(),
                    60, 
                    System.currentTimeMillis(),
                    "", 
                    0,
                    false,
                    SnapConfig.SNAP_CONFIG_SNAPS_DISABLED,
                    0,
                    SetupUtils.createOSD1Config().getCapabilitySecret());
        } catch (IOException ex) {
            // can't happen
            return null;
        }
    }

    public FileCredentials getFileCredentials(String fileId, int width, int parity, int stripeSize, int qw,
            List<String> osdUUIDs) {
        StripingPolicy sp = ECOperationsTest.getECStripingPolicy(width, parity, stripeSize, qw);
        Replica r = Replica.newBuilder().setStripingPolicy(sp).setReplicationFlags(0).addAllOsdUuids(osdUUIDs).build();
        XLocSet xloc = XLocSet.newBuilder().setReadOnlyFileSize(0).setVersion(1).addReplicas(r)
                .setReplicaUpdatePolicy("ec").build();
        Capability cap = getCap(fileId);
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xloc).build();
        return fc;
    }
    
    public static void assertBufferEquals(ReusableBuffer expected, ReusableBuffer actual) {
        assertNotNull(actual);

        int exPos = expected.position();
        int acPos = actual.position();

        assertEquals(expected.remaining(), actual.remaining());
        byte[] ex = new byte[expected.remaining()];
        byte[] ac = new byte[expected.remaining()];
        expected.get(ex);
        actual.get(ac);

        assertArrayEquals(ex, ac);

        expected.position(exPos);
        expected.position(acPos);
    }

    @SuppressWarnings("rawtypes")
    public static void clearAll(List... lists) {
        for (List list : lists) {
            list.clear();
        }
    }
}
