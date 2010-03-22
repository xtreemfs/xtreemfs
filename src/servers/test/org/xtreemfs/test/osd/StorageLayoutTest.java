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
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.checksums.ChecksumFactory;
import org.xtreemfs.common.checksums.provider.JavaChecksumProvider;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.storage.FileMetadata;
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

    /*public void testHashStorageLayoutBasics() throws Exception {

        HashStorageLayout layout = new HashStorageLayout(config, new MetadataCache());
        basicTests(layout);
    }*/

    public void testHashStorageLayoutWithChecksumsBasics() throws Exception {

        JavaChecksumProvider j = new JavaChecksumProvider();
        ChecksumFactory.getInstance().addProvider(j);
        SetupUtils.CHECKSUMS_ON = true;
        OSDConfig configCSUM = SetupUtils.createOSD1Config();
        SetupUtils.CHECKSUMS_ON = false;
        HashStorageLayout layout = new HashStorageLayout(configCSUM, new MetadataCache());
        basicTests(layout);
    }

    /*public void testSingleFileLayout() throws Exception {
        SingleFileStorageLayout layout = new SingleFileStorageLayout(config, new MetadataCache());
        basicTests(layout);
    }*/

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

        StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(new Replica(new StringSet(), 0, new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 64, 1)),0);//new RAID0(64, 1);

        FileMetadata md = new FileMetadata(sp);
        md.initLatestObjectVersions(new HashMap<Long, Long>());
        md.initLargestObjectVersions(new HashMap<Long, Long>());
        md.initObjectChecksums(new HashMap<Long, Map<Long, Long>>());
        
        assertFalse(layout.fileExists(fileId));

        ReusableBuffer data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }
                
        //write 64 bytes
        layout.writeObject(fileId, md, data, 0l, 0, 1l, false, false);

        //read full object
        ObjectInformation oinfo = layout.readObject(fileId, md, 0l, 0, StorageLayout.FULL_OBJECT_LENGTH, 1l);
        assertEquals(64, oinfo.getData().capacity());
        for (int i = 0; i < 64; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        //read object 1 (does not exist)
        oinfo = layout.readObject(fileId,md, 1l, 0, StorageLayout.FULL_OBJECT_LENGTH, 1l);
        assertEquals(ObjectInformation.ObjectStatus.DOES_NOT_EXIST,oinfo.getStatus());

        //range test
        oinfo = layout.readObject(fileId,md, 0l, 32, 32, 1l);
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 32; i < 64; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        //range test
        oinfo = layout.readObject(fileId,md, 0l, 32, 1, 1l);
        assertEquals(1, oinfo.getData().capacity());
        for (int i = 32; i < 33; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        oinfo = layout.readObject(fileId,md, 0l, 32, 64, 1l);
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 32; i < 64; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        //truncate to 32 byte
        layout.truncateObject(fileId, md, 0l, 32, 1, false);
        oinfo = layout.readObject(fileId,md, 0l, 32, 64, 1l);
        assertEquals(0, oinfo.getData().capacity());
        BufferPool.free(oinfo.getData());

        //read (non-existent) data from offset 32
        oinfo = layout.readObject(fileId,md, 0l, 0, 32, 1l);
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 0; i < 32; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        //truncate extend to 64 bytes
        layout.truncateObject(fileId, md, 0l, 64, 2, false);
        oinfo = layout.readObject(fileId,md, 0l, 32, 64, 2l);
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 0; i < 32; i++) {
            assertEquals((byte) 0, oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());


        //write more objects...
        //obj 1 = hole
        //obj 2 = second half
        //obj 3 = full

        data = BufferPool.allocate(32);
        for (int i = 0; i < 32; i++) {
            data.put((byte) (48 + i));
        }
        //write 64 bytes
        layout.writeObject(fileId, md, data, 2l, 0, 1l, false, false);

        data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }
        //write 64 bytes
        layout.writeObject(fileId, md, data, 3l, 0, 1l, false, false);

        //read object 1... should be all zeros or zero padding
        oinfo = layout.readObject(fileId,md, 1l, 0, 32, 1l);
        if (oinfo.getStatus() == ObjectInformation.ObjectStatus.PADDING_OBJECT) {
            //fine
        } else if (oinfo.getStatus() == ObjectInformation.ObjectStatus.DOES_NOT_EXIST) {
            //also fine
        } else {
            //we expect full stripe size of zeros
            assertEquals(sp.getStripeSizeForObject(1),oinfo.getData().capacity());
            for (int i = 0; i < sp.getStripeSizeForObject(1); i++) {
                assertEquals((byte) 0, oinfo.getData().get());
            }
            BufferPool.free(oinfo.getData());
        }



    }

    private void getObjectListTest(StorageLayout layout) throws IOException {
        final String fileId = "ABCDEFG:0001";

        StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(new Replica(new StringSet(), 0, new StripingPolicy(
                StripingPolicyType.STRIPING_POLICY_RAID0, 64, 1)),0);// new RAID0(64, 1);

        assertFalse(layout.fileExists(fileId));
        assertEquals(0, layout.getObjectSet(fileId).size());

        FileMetadata md = new FileMetadata(sp);
        md.initLatestObjectVersions(new HashMap<Long, Long>());
        md.initLargestObjectVersions(new HashMap<Long, Long>());
        md.initObjectChecksums(new HashMap<Long, Map<Long, Long>>());

        ReusableBuffer data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }

        // objects to write
        long objectNos[] = { 0, 2, 4, 8, 10, 12, 20, 24, 32, 44, 46, 48, 50 };

        // write objects
        for (long objNo : objectNos) {
            layout.writeObject(fileId, md, data.createViewBuffer(), objNo, 0, 1, false, false);
        }
        BufferPool.free(data);

        ObjectSet objectList = layout.getObjectSet(fileId);
        // check
        ObjectSet objectNosList = new ObjectSet(1, 0, objectNos.length);
        for (long object : objectNos)
            objectNosList.add(object);
        assertTrue(objectList.equals(objectNosList));
    }

    public static void main(String[] args) {
        TestRunner.run(StorageLayoutTest.class);
    }

}
