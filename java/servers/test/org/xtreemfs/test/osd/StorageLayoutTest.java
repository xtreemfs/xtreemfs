/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.checksums.ChecksumFactory;
import org.xtreemfs.foundation.checksums.provider.JavaChecksumProvider;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.storage.CowPolicy;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.SingleFileStorageLayout;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.storage.CowPolicy.cowMode;
import org.xtreemfs.osd.storage.VersionManager.ObjectVersionInfo;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
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

    public void testHashStorageLayoutWithChecksumsBasics() throws Exception {

        JavaChecksumProvider j = new JavaChecksumProvider();
        ChecksumFactory.getInstance().addProvider(j);
        SetupUtils.CHECKSUMS_ON = true;
        OSDConfig configCSUM = SetupUtils.createOSD1Config();
        SetupUtils.CHECKSUMS_ON = false;
        HashStorageLayout layout = new HashStorageLayout(configCSUM, new MetadataCache());
        basicTests(layout);
    }

    public void testSingleFileLayout() throws Exception {
        SingleFileStorageLayout layout = new SingleFileStorageLayout(config, new MetadataCache());
        basicTests(layout);
    }

    public void testSingleFileStorageLayoutWithChecksumsBasics() throws Exception {
        JavaChecksumProvider j = new JavaChecksumProvider();
        ChecksumFactory.getInstance().addProvider(j);
        SetupUtils.CHECKSUMS_ON = true;
        OSDConfig configCSUM = SetupUtils.createOSD1Config();
        SetupUtils.CHECKSUMS_ON = false;
        SingleFileStorageLayout layout = new SingleFileStorageLayout(configCSUM, new MetadataCache());
        basicTests(layout);
    }

    public void testHashStorageLayoutGetObjectList() throws Exception {

        HashStorageLayout layout = new HashStorageLayout(config, new MetadataCache());
        getObjectListTest(layout);
    }

    public void testSingleFileStorageLayoutGetObjectList() throws Exception {

        SingleFileStorageLayout layout = new SingleFileStorageLayout(config, new MetadataCache());
        getObjectListTest(layout);
    }

    public void testHashStorageLayoutGetFileIDList() throws Exception {

        HashStorageLayout layout = new HashStorageLayout(config, new MetadataCache());
        getFileIDListTest(layout);
    }

    public void testSingleFileStorageLayoutGetFileIDList() throws Exception {

        SingleFileStorageLayout layout = new SingleFileStorageLayout(config, new MetadataCache());
        getFileIDListTest(layout);
    }

    /**
     * @param layout
     * @throws IOException
     */
    private void basicTests(StorageLayout layout) throws IOException {
        final String fileId = "ABCDEFG:0001";

        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, 64)).setReplicationFlags(0)
                .build();
        StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(r, 0);// new RAID0(64, 1);

        /*
         * FileMetadata md = new FileMetadata(sp); md.initLatestObjectVersions(new HashMap<Long, Long>());
         * md.initLargestObjectVersions(new HashMap<Long, Long>()); md.initObjectChecksums(new HashMap<String,
         * Long>());
         */

        FileMetadata md = layout.getFileMetadata(sp, fileId);
        assertNotNull(md);

        assertFalse(layout.fileExists(fileId));

        ReusableBuffer data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }
        data.flip();
        // write 64 bytes
        layout.writeObject(fileId, md, data, 0l, 0, 1l, 0, false, CowPolicy.PolicyNoCow);

        // read full object
        ObjectInformation oinfo = layout.readObject(fileId, md, 0l, 0, StorageLayout.FULL_OBJECT_LENGTH,
                new ObjectVersionInfo(1l, 0, 0));
        assertEquals(64, oinfo.getData().capacity());
        for (int i = 0; i < 64; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        // read object 1 (does not exist)
        oinfo = layout.readObject(fileId, md, 1l, 0, StorageLayout.FULL_OBJECT_LENGTH, new ObjectVersionInfo(1l, 0, 0));
        assertEquals(ObjectInformation.ObjectStatus.DOES_NOT_EXIST, oinfo.getStatus());

        // range test
        oinfo = layout.readObject(fileId, md, 0l, 32, 32, new ObjectVersionInfo(1l, 0, 0));
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 32; i < 64; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        // range test
        oinfo = layout.readObject(fileId, md, 0l, 32, 1, new ObjectVersionInfo(1l, 0, 0));
        assertEquals(1, oinfo.getData().capacity());
        for (int i = 32; i < 33; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        oinfo = layout.readObject(fileId, md, 0l, 32, 64, new ObjectVersionInfo(1l, 0, 0));
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 32; i < 64; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        // truncate to 32 byte
        layout.truncateObject(fileId, md, 0l, 32, 1, 0, false);
        oinfo = layout.readObject(fileId, md, 0l, 32, 64, new ObjectVersionInfo(1l, 0, 0));
        assertTrue(((oinfo.getData() == null) || (oinfo.getData().capacity() == 0)));
        BufferPool.free(oinfo.getData());

        // read (non-existent) data from offset 32
        oinfo = layout.readObject(fileId, md, 0l, 0, 32, new ObjectVersionInfo(1l, 0, 0));
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 0; i < 32; i++) {
            assertEquals((byte) (48 + i), oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        // truncate extend to 64 bytes
        layout.truncateObject(fileId, md, 0l, 64, 2, 0, false);
        oinfo = layout.readObject(fileId, md, 0l, 32, 64, new ObjectVersionInfo(2l, 0, 0));
        assertEquals(32, oinfo.getData().capacity());
        for (int i = 0; i < 32; i++) {
            assertEquals((byte) 0, oinfo.getData().get());
        }
        BufferPool.free(oinfo.getData());

        // write more objects...
        // obj 1 = hole
        // obj 2 = second half
        // obj 3 = full

        data = BufferPool.allocate(32);
        for (int i = 0; i < 32; i++) {
            data.put((byte) (48 + i));
        }
        data.flip();
        // write 64 bytes
        layout.writeObject(fileId, md, data, 2l, 0, 1l, 0, false, CowPolicy.PolicyNoCow);

        data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }
        data.flip();
        // write 64 bytes
        layout.writeObject(fileId, md, data, 3l, 0, 1l, 0, false, CowPolicy.PolicyNoCow);

        // read object 1... should be all zeros or zero padding
        oinfo = layout.readObject(fileId, md, 1l, 0, sp.getStripeSizeForObject(1), new ObjectVersionInfo(1l, 0, 0));
        if (oinfo.getStatus() == ObjectInformation.ObjectStatus.PADDING_OBJECT) {
            // fine
        } else if (oinfo.getStatus() == ObjectInformation.ObjectStatus.DOES_NOT_EXIST) {
            // also fine
        } else {
            // we expect full stripe size of zeros
            assertEquals(sp.getStripeSizeForObject(1), oinfo.getData().capacity());
            for (int i = 0; i < sp.getStripeSizeForObject(1); i++) {
                assertEquals((byte) 0, oinfo.getData().get());
            }
            BufferPool.free(oinfo.getData());
        }

    }

    private void getObjectListTest(StorageLayout layout) throws IOException {
        final String fileId = "ABCDEFG:0001";

        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, 64)).setReplicationFlags(0)
                .build();
        StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(r, 0);// new RAID0(64, 1);

        FileMetadata md = layout.getFileMetadata(sp, fileId);
        assertNotNull(md);

        assertFalse(layout.fileExists(fileId));
        assertEquals(0, layout.getObjectSet(fileId, md).size());

        ReusableBuffer data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }

        // objects to write
        long objectNos[] = { 0, 2, 4, 8, 10, 12, 20, 24, 32, 44, 46, 48, 50 };

        // write objects
        for (long objNo : objectNos) {
            layout.writeObject(fileId, md, data.createViewBuffer(), objNo, 0, 1, 0, false, CowPolicy.PolicyNoCow);
        }
        BufferPool.free(data);

        ObjectSet objectList = layout.getObjectSet(fileId, md);
        // check
        ObjectSet objectNosList = new ObjectSet(1, 0, objectNos.length);
        for (long object : objectNos)
            objectNosList.add(object);
        assertTrue(objectList.equals(objectNosList));
    }

    private void getFileIDListTest(StorageLayout layout) throws IOException {
        final ArrayList<String> fileIDs = new ArrayList<String>();
        fileIDs.add("0002:ABCDEF");
        fileIDs.add("0012:GHIJKL");
        fileIDs.add("0123:MNOPQR");
        fileIDs.add("1234:STUVWX");

        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(1, 64)).setReplicationFlags(0)
                .build();
        StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(r, 0);// new RAID0(64, 1);

        // create some data
        ReusableBuffer data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }
        data.flip();

        // create some files
        for (String f : fileIDs) {
            String fileId = f;
            FileMetadata md = layout.getFileMetadata(sp, fileId);

            assertNotNull(md);

            assertFalse(layout.fileExists(fileId));

            // write 64 bytes
            layout.writeObject(fileId, md, data.createViewBuffer(), 0l, 0, 1l, 0, false, CowPolicy.PolicyNoCow);
        }

        ArrayList<String> newFileIDs = layout.getFileIDList();
        // compare fileIDs with newFileIDs
        assertTrue(newFileIDs.containsAll(fileIDs));
        assertTrue(fileIDs.containsAll(newFileIDs));
    }

    public static void main(String[] args) {
        TestRunner.run(StorageLayoutTest.class);
    }

}
