/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin,
Barcelona Supercomputing Center - Centro Nacional de Supercomputacion and
Consiglio Nazionale delle Ricerche.

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
 * AUTHORS: Christian Lorenz (ZIB), Jan Stender (ZIB), Jesús Malo (BSC), Björn Kolbeck (ZIB),
 *          Eugenio Cesario (CNR)
 */
package org.xtreemfs.test.common.striping;


import junit.framework.TestCase;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.test.SetupUtils;

/**
 * It tests the RAID0 class
 * 
 * @author clorenz
 */
public class RAID0Test extends TestCase {

    private static final long KILOBYTE = 1024L;

    /** Creates a new instance of RAID0Test */
    public RAID0Test(String testName) {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    protected void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
    }

    protected void tearDown() throws Exception {
    }


    public void testGetObjectsAndBytes() throws Exception {
        Replica r = new Replica(new StripingPolicy(Constants.STRIPING_POLICY_RAID0, 128, 3), 0, new StringSet());
        StripingPolicyImpl policy = StripingPolicyImpl.getPolicy(r);

        long objectID, offset;

        objectID = policy.getObjectNoForOffset(20);
        assertEquals(0, objectID);

        objectID = policy.getObjectNoForOffset(20 * KILOBYTE);
        assertEquals(0, objectID);

        objectID = policy.getObjectNoForOffset(255 * KILOBYTE);
        assertEquals(1, objectID);

        objectID = policy.getObjectNoForOffset(256 * KILOBYTE);
        assertEquals(2, objectID);

        offset = policy.getObjectStartOffset(5);
        assertEquals(640 * KILOBYTE, offset);

        offset = policy.getObjectEndOffset(5);
        assertEquals(768 * KILOBYTE - 1, offset);

        offset = policy.getObjectStartOffset(6);
        assertEquals(768 * KILOBYTE, offset);
    }

    public void testGetOSDs() throws Exception {
        Replica r = new Replica(new StripingPolicy(Constants.STRIPING_POLICY_RAID0, 128, 8), 0, new StringSet());
        StripingPolicyImpl policy = StripingPolicyImpl.getPolicy(r);

        int osd0 = policy.getOSDforObject(0);
        assertEquals(0, osd0);

        int osd1 = policy.getOSDforObject(1);
        assertEquals(1, osd1);

        int osd7 = policy.getOSDforObject(7);
        assertEquals(7, osd7);

        int osd8 = policy.getOSDforObject(8);
        assertEquals(0, osd8);

        int osd21 = policy.getOSDforObject(2125648682);
        assertEquals(2, osd21);

        int osd0b = policy.getOSDforOffset(20);
        assertEquals(osd0, osd0b);

        int osd0c = policy.getOSDforOffset(20 * KILOBYTE);
        assertEquals(0, osd0c);

        int osd7b = policy.getOSDforOffset(7 * 128 * KILOBYTE);
        assertEquals(osd7, osd7b);

        int osd8b = policy.getOSDforOffset(8 * 128 * KILOBYTE);
        assertEquals(osd8, osd8b);

        int osd21b = policy.getOSDforOffset(2125648682 * 128 * KILOBYTE);
        assertEquals(osd21, osd21b);
    }

    public void testGetStripeSize() throws Exception {
        Replica r = new Replica(new StripingPolicy(Constants.STRIPING_POLICY_RAID0, 256, 3), 0, new StringSet());
        StripingPolicyImpl policy = StripingPolicyImpl.getPolicy(r);
        assertEquals(256 * KILOBYTE, policy.getStripeSizeForObject(5));
    }

    public void testCalculateLastObject() throws Exception {
        Replica r = new Replica(new StripingPolicy(Constants.STRIPING_POLICY_RAID0, 256, 3), 0, new StringSet());
        StripingPolicyImpl policy = StripingPolicyImpl.getPolicy(r);
        assertEquals(41, policy.getObjectNoForOffset(256L * KILOBYTE * 42-1)); // filesize
        // =
        // offset
        // +
        // 1
        assertEquals(42, policy.getObjectNoForOffset(256L * KILOBYTE * 42 + 32000-1));
        assertEquals(42, policy.getObjectNoForOffset(256L * KILOBYTE * 43 - 1-1));
    }

}
