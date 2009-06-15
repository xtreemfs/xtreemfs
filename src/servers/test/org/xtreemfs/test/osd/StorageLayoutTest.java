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
 * AUTHORS: Jan Stender (ZIB), Jesús Malo (BSC), Björn Kolbeck (ZIB),
 *          Eugenio Cesario (CNR)
 */

package org.xtreemfs.test.osd;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author bjko
 */
public class StorageLayoutTest extends TestCase {

    final OSDConfig config;

    public StorageLayoutTest(String testName) throws IOException {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);
        config = SetupUtils.createOSD1Config();
    }

    protected void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        FSUtils.delTree(new File(config.getObjDir()));
    }

    protected void tearDown() throws Exception {
    }

    public void testHashStorageLayoutBasics() throws Exception {
        HashStorageLayout layout = new HashStorageLayout(config, new MetadataCache());
        basicTests(layout);
    }

    public void testHashStorageLayoutGetObjectList() throws Exception {
        HashStorageLayout layout = new HashStorageLayout(config, new MetadataCache());
        getObjectListTest(layout);
    }

    /**
     * @param layout
     * @throws IOException
     */
    private void basicTests(StorageLayout layout) throws IOException {
        final String fileId = "ABCDEFG:0001";
	    StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(new Replica(new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 64, 1), 0, new StringSet()));//new RAID0(64, 1);

        assertFalse(layout.fileExists(fileId));

        ReusableBuffer data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }

        layout.writeObject(fileId, 0l, data, 1, 0, 0, sp, false);
        BufferPool.free(data);

        ObjectInformation oinfo = layout.readObject(fileId, 0l, 1, 0, sp);
        assertEquals(64, oinfo.getData().capacity());
        for (int i = 0; i < 64; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        oinfo = layout.readObject(fileId, 1l, 1, 0, sp);
        assertEquals(ObjectInformation.ObjectStatus.DOES_NOT_EXIST,oinfo.getStatus());

        //range test
        oinfo = layout.readObject(fileId, 0l, 1, 0, sp, 32,32);
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 32; i < 64; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        //range test
        oinfo = layout.readObject(fileId, 0l, 1, 0, sp, 32,1);
        assertEquals(1, oinfo.getData().capacity());
        for (int i = 32; i < 33; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        oinfo = layout.readObject(fileId, 0l, 1, 0, sp, 32,64);
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 32; i < 64; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        oinfo = layout.readObject(fileId, 0l, 1, 0, sp, 66,1);
        assertEquals(0, oinfo.getData().capacity());
        BufferPool.free(oinfo.getData());
    }

    private void getObjectListTest(StorageLayout layout) throws IOException {
        final String fileId = "ABCDEFG:0001";
        StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(new Replica(new StripingPolicy(
                StripingPolicyType.STRIPING_POLICY_RAID0, 64, 1), 0, new StringSet()));// new RAID0(64, 1);

        assertFalse(layout.fileExists(fileId));
        assertEquals(0, layout.getObjectList(fileId).length);

        ReusableBuffer data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }

        // objects to write
        long objectNos[] = { 0, 2, 4, 8, 10, 12, 20, 24, 32, 44, 46, 48, 50 };

        // write objects
        for (long objNo : objectNos) {
            layout.writeObject(fileId, objNo, data.createViewBuffer(), 1, 0, 0, sp, false);
        }
        BufferPool.free(data);

        long[] objectList = layout.getObjectList(fileId);
        // check
        Arrays.sort(objectNos);
        Arrays.sort(objectList);
        assertTrue(Arrays.equals(objectNos, objectList));
    }

    public static void main(String[] args) {
        TestRunner.run(StorageLayoutTest.class);
    }

}
