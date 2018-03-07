/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.api.BabuDB;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.babudb.log.DiskLogger.SyncMode;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.babudb.BabuDBStorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestEnvironment;
import org.xtreemfs.TestHelper;

public class BabuDBStorageManagerTest {
    @Rule
    public final TestRule                  testLog      = TestHelper.testLog;
    
    public static final String             DB_DIRECTORY = "/tmp/xtreemfs-test";
    
    private BabuDBStorageManager           mngr;
    
    private BabuDB                         database;
    
    private Exception                      exc;
    
    private Object                         lock         = "";
    
    private boolean                        cont;
    
    private TestEnvironment                testEnv;
    
    private DBAccessResultListener<Object> listener     = new DBAccessResultListener<Object>() {
                                                            
                                                            @Override
                                                            public void finished(Object o, Object context) {
                                                                synchronized (lock) {
                                                                    cont = true;
                                                                    lock.notify();
                                                                }
                                                            }
                                                            
                                                            @Override
                                                            public void failed(Throwable error, Object context) {
                                                                exc = (Exception) error;
                                                                synchronized (lock) {
                                                                    lock.notify();
                                                                }
                                                            }
                                                        };
    
    public BabuDBStorageManagerTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }
    
    @Before
    public void setUp() throws Exception {
        // initialize Directory Service (for synchronized clocks...)
        DIRConfig config = SetupUtils.createDIRConfig();
        BabuDBConfig dbsConfig = SetupUtils.createDIRdbsConfig();
        
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.DIR_CLIENT, TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.UUID_RESOLVER });
        testEnv.start();

        // reset database
        File dbDir = new File(DB_DIRECTORY);
        FSUtils.delTree(dbDir);
        dbDir.mkdirs();
        database = BabuDBFactory.createBabuDB(new BabuDBConfig(DB_DIRECTORY, DB_DIRECTORY, 2,
            1024 * 1024 * 16, 5 * 60, SyncMode.FDATASYNC, 300, 1000, false, 16, 1024 * 1024 * 512));
        mngr = new BabuDBStorageManager(database, "volId", "volume", (short) 1, new short[] { 1 },
 new short[0], "me",
                "myGrp", 511, null, null, false, 0, null);
        
        exc = null;
    }
    
    @After
    public void tearDown() throws Exception {
        database.shutdown();
        
        testEnv.shutdown();
    }
    
    @Test
    public void testCreateDelete() throws Exception {
        
        // retrieve root directory
        FileMetadata rootDir = mngr.getMetadata(0, "volume");
        assertTrue(rootDir.isDirectory());
        assertTrue(rootDir.getAtime() > 0);
        assertTrue(rootDir.getCtime() > 0);
        assertTrue(rootDir.getMtime() > 0);
        assertEquals(1, rootDir.getId());
        
        // create nested file
        final long fileId1 = mngr.getNextFileId();
        final String fileName = "testfile.txt";
        final String userId = "me";
        final String groupId = "myGrp";
        final short perms = 511;
        final long w32Attrs = 43287473;
        
        AtomicDBUpdate update = mngr.createAtomicDBUpdate(listener, null);
        FileMetadata nextDir = mngr.createFile(fileId1, rootDir.getId(), fileName, 1, 1, 1, userId, groupId,
            perms, w32Attrs, 12, true, 5, 6, update);
        mngr.setLastFileId(fileId1, update);
        update.execute();
        waitForResponse();
        
        FileMetadata metadata = mngr.getMetadata(rootDir.getId(), fileName);
        assertFalse(metadata.isDirectory());
        assertTrue(metadata.getAtime() > 0);
        assertTrue(metadata.getCtime() > 0);
        assertTrue(metadata.getMtime() > 0);
        assertTrue(metadata.isReadOnly());
        assertEquals(5, metadata.getEpoch());
        assertEquals(6, metadata.getIssuedEpoch());
        assertEquals(nextDir.getId(), metadata.getId());
        assertEquals(12, metadata.getSize());
        assertEquals(1, metadata.getLinkCount());
        assertEquals(perms, metadata.getPerms());
        assertEquals(w32Attrs, metadata.getW32Attrs());
        assertEquals(fileName, metadata.getFileName());
        assertEquals(userId, metadata.getOwnerId());
        assertEquals(groupId, metadata.getOwningGroupId());
        
        // create nested dir
        final long fileId2 = mngr.getNextFileId();
        final String dirName = "someDir";
        
        update = mngr.createAtomicDBUpdate(listener, null);
        FileMetadata dir = mngr.createDir(fileId2, rootDir.getId(), dirName, 1, 1, 1, userId, groupId, perms,
            w32Attrs, update);
        mngr.setLastFileId(fileId2, update);
        update.execute();
        waitForResponse();
        
        metadata = mngr.getMetadata(rootDir.getId(), dirName);
        assertTrue(metadata.isDirectory());
        assertTrue(metadata.getAtime() > 0);
        assertTrue(metadata.getCtime() > 0);
        assertTrue(metadata.getMtime() > 0);
        assertEquals(dir.getId(), metadata.getId());
        assertEquals(perms, metadata.getPerms());
        assertEquals(dirName, metadata.getFileName());
        assertEquals(userId, metadata.getOwnerId());
        assertEquals(groupId, metadata.getOwningGroupId());
        
        // list files; both the nested directory and file should be in 'rootDir'
        DatabaseResultSet<FileMetadata> children = mngr.getChildren(rootDir.getId(), 0, Integer.MAX_VALUE);
        List<String> tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        children.destroy();
        
        assertEquals(2, tmp.size());
        assertTrue(tmp.contains(dirName));
        assertTrue(tmp.contains(fileName));
        
        // delete file
        update = mngr.createAtomicDBUpdate(listener, null);
        mngr.delete(rootDir.getId(), fileName, update);
        update.execute();
        waitForResponse();
        
        // list files; only the nested directory should be in 'rootDir'
        children = mngr.getChildren(rootDir.getId(), 0, Integer.MAX_VALUE);
        tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        
        assertEquals(1, tmp.size());
        assertEquals(dirName, tmp.get(0));
        
        long fileId3 = mngr.getNextFileId();
        
        // create file again
        update = mngr.createAtomicDBUpdate(listener, null);
        mngr.createFile(fileId3, rootDir.getId(), fileName, 0, 0, 0, userId, groupId, perms, w32Attrs, 11,
            true, 3, 4, update);
        mngr.setLastFileId(fileId3, update);
        update.execute();
        waitForResponse();
        
        // list files; both the nested directory and file should be in 'rootDir'
        // again
        children = mngr.getChildren(rootDir.getId(), 0, Integer.MAX_VALUE);
        tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        
        assertEquals(2, tmp.size());
        assertTrue(tmp.contains(dirName));
        assertTrue(tmp.contains(fileName));
    }

    @Test
    public void testCreateDeleteWithCollidingHashCodes() throws Exception {
        
        // create two files w/ colliding hash codes in nested dir
        final long dirId = 2;
        final String name1 = "Cfat";
        final String name2 = "CgCU";
        final String uid1 = "uid1";
        final String uid2 = "uid2";
        final String gid1 = "gid1";
        final String gid2 = "gid2";
        final short perms1 = 0;
        final short perms2 = Short.MAX_VALUE;
        
        AtomicDBUpdate update = mngr.createAtomicDBUpdate(listener, null);
        mngr.createDir(3, dirId, name1, 0, 0, 0, uid1, gid1, perms1, 23, update);
        update.execute();
        waitForResponse();
        
        update = mngr.createAtomicDBUpdate(listener, null);
        mngr.createDir(4, dirId, name2, 0, 0, 0, uid2, gid2, perms2, 55, update);
        update.execute();
        waitForResponse();
        
        // list files; both the nested files are found and contain the correct
        // data
        DatabaseResultSet<FileMetadata> children = mngr.getChildren(dirId, 0, Integer.MAX_VALUE);
        Map<String, FileMetadata> map = new HashMap<String, FileMetadata>();
        while (children.hasNext()) {
            FileMetadata child = children.next();
            map.put(child.getFileName(), child);
        }
        children.destroy();
        
        assertEquals(2, map.size());
        assertEquals(uid1, map.get(name1).getOwnerId());
        assertEquals(uid2, map.get(name2).getOwnerId());
        assertEquals(gid1, map.get(name1).getOwningGroupId());
        assertEquals(gid2, map.get(name2).getOwningGroupId());
        assertEquals(perms1, map.get(name1).getPerms());
        assertEquals(perms2, map.get(name2).getPerms());
        
        // delete first file
        update = mngr.createAtomicDBUpdate(listener, null);
        mngr.delete(dirId, name1, update);
        update.execute();
        waitForResponse();
        
        // list files; ensure that only file 2 remains
        children = mngr.getChildren(dirId, 0, Integer.MAX_VALUE);
        assertTrue(children.hasNext());
        assertEquals(name2, children.next().getFileName());
        assertFalse(children.hasNext());
    }

    @Test
    public void testPathResolution() throws Exception {
        
        final String userId = "me";
        final String groupId = "myGroup";
        final short perms = 511;
        final long w32Attrs = Long.MIN_VALUE;
        exc = null;
        
        // create a small directory tree and test path resolution
        long nextId = 0;
        
        AtomicDBUpdate update = mngr.createAtomicDBUpdate(listener, null);
        long comp1Id = nextId = mngr.createDir(1, 0, "comp1", 0, 0, 0, userId, groupId, perms, w32Attrs,
            update).getId();
        update.execute();
        waitForResponse();
        update = mngr.createAtomicDBUpdate(listener, null);
        nextId = mngr.createDir(2, nextId, "comp2", 0, 0, 0, userId, groupId, perms, w32Attrs, update)
                .getId();
        update.execute();
        waitForResponse();
        update = mngr.createAtomicDBUpdate(listener, null);
        nextId = mngr.createDir(3, nextId, "comp3", 0, 0, 0, userId, groupId, perms, w32Attrs, update)
                .getId();
        update.execute();
        waitForResponse();
        update = mngr.createAtomicDBUpdate(listener, null);
        nextId = mngr.createFile(4, nextId, "file.txt", 0, 0, 0, "usr", "grp", perms, w32Attrs, 4711, false,
            3, 4, update).getId();
        update.execute();
        waitForResponse();
        
        long id = mngr.resolvePath(new Path("comp1"))[0].getId();
        assertEquals(comp1Id, id);
        
        id = mngr.resolvePath(new Path("comp1/comp2/comp3/file.txt"))[3].getId();
        assertEquals(nextId, id);
        
        // test path resolution conrner cases
        id = mngr.resolvePath(new Path("comp1/"))[0].getId();
        assertEquals(comp1Id, id);
        
        id = mngr.resolvePath(new Path("comp1/comp2/"))[0].getId();
        assertEquals(comp1Id, id);
        
        // list files in root dir; there should only be one
        DatabaseResultSet<FileMetadata> children = mngr.getChildren(1, 0, Integer.MAX_VALUE);
        List<String> tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        children.destroy();
        
        assertEquals(1, tmp.size());
        assertTrue(tmp.contains("comp2"));
    }

    @Test
    public void testPartialReaddir() throws Exception {
        
        final String userId = "me";
        final String groupId = "myGroup";
        final short perms = 511;
        final long w32Attrs = Long.MIN_VALUE;
        exc = null;
        
        AtomicDBUpdate update = mngr.createAtomicDBUpdate(listener, null);
        mngr.createDir(1, 0, "root", 0, 0, 0, userId, groupId, perms, w32Attrs, update).getId();
        update.execute();
        waitForResponse();
        
        // create 10 nested directories
        for (int i = 0; i < 10; i++) {
            
            update = mngr.createAtomicDBUpdate(listener, null);
            mngr.createDir(i + 2, 1, "entry" + i, 0, 0, 0, userId, groupId, perms, w32Attrs, update).getId();
            update.execute();
            waitForResponse();
        }
        
        // list different subsets of files in root dir
        
        DatabaseResultSet<FileMetadata> children = mngr.getChildren(1, 0, 3);
        List<String> tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        children.destroy();
        
        assertEquals(3, tmp.size());
        for (int i = 0; i < 3; i++)
            assertTrue(tmp.contains("entry" + i));
        
        children = mngr.getChildren(1, 3, 3);
        tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        
        assertEquals(3, tmp.size());
        for (int i = 3; i < 6; i++)
            assertTrue(tmp.contains("entry" + i));
        
        children = mngr.getChildren(1, 6, 1);
        tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        
        assertEquals(1, tmp.size());
        for (int i = 6; i < 7; i++)
            assertTrue(tmp.contains("entry" + i));
        
        children = mngr.getChildren(1, 8, 10);
        tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        
        assertEquals(2, tmp.size());
        for (int i = 8; i < 10; i++)
            assertTrue(tmp.contains("entry" + i));
        
        children = mngr.getChildren(1, 0, 10);
        tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        
        assertEquals(10, tmp.size());
        for (int i = 0; i < 10; i++)
            assertTrue(tmp.contains("entry" + i));
        
    }
    
    private void waitForResponse() throws Exception {
        
        synchronized (lock) {
            if (!cont)
                lock.wait();
        }
        
        cont = false;
        
        if (exc != null)
            throw exc;
    }
    
    //    
    // public void testSymlink() throws Exception {
    //        
    // final String userId = "me";
    // final String groupId = "myGroup";
    // final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();
    //        
    // long fileId = mngr.createFile("blub/bla.txt", userId, groupId,
    // stripingPolicy, false, null);
    // mngr.linkFile("test.txt", fileId, 1);
    // assertEquals(mngr.getFileReference(fileId), "blub/bla.txt");
    // }
    //    
    // public void testAttributes() throws Exception {
    //        
    // final String userId = "me";
    // final String groupId = "myGroup";
    // final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();
    //        
    // Map<String, Object> attrs = new HashMap<String, Object>();
    // attrs.put("myKey", "myValue");
    // attrs.put("blaKey", "blaValue");
    //        
    // long fileId = mngr.createFile(null, userId, groupId, stripingPolicy,
    // false, null);
    // mngr.linkFile("test.txt", fileId, 1);
    // mngr.addXAttributes(fileId, attrs);
    //        
    // attrs = mngr.getXAttributes(fileId);
    // assertEquals(attrs.size(), 2);
    //        
    // List<Object> list = new ArrayList<Object>();
    // list.add("myKey");
    // mngr.deleteXAttributes(fileId, list);
    // assertEquals(mngr.getXAttributes(fileId).size(), 1);
    // assertEquals(mngr.getXAttributes(fileId).get("blaKey"), "blaValue");
    // assertNull(mngr.getXAttributes(fileId).get("myKey"));
    //        
    // mngr.addXAttributes(fileId, attrs);
    // assertEquals(mngr.getXAttributes(fileId).size(), 2);
    // mngr.deleteXAttributes(fileId, null);
    // assertEquals(mngr.getXAttributes(fileId).size(), 0);
    // }
    //    
    // public void testPosixAttributes() throws Exception {
    //        
    // final String userId = "me";
    // final String groupId = "myGroup";
    // final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();
    //        
    // long fileId = mngr.createFile(null, userId, groupId, stripingPolicy,
    // false, null);
    // mngr.linkFile("test.txt", fileId, 1);
    //        
    // mngr.setFileSize(fileId, 121, 0, 0);
    // assertEquals(mngr.getFileEntity("test.txt").getId(), fileId);
    // assertEquals(((FileEntity) mngr.getFileEntity("test.txt")).getSize(),
    // 121);
    // }
    //    
    // public void testACLs() throws Exception {
    //        
    // final String userId = "me";
    // final String groupId = "myGroup";
    // final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();
    //        
    // Map<String, Object> acl = new HashMap<String, Object>();
    // acl.put("1", 3L);
    // acl.put("2", 7L);
    // acl.put("3", 1L);
    //        
    // long fileId = mngr.createFile(null, userId, groupId, stripingPolicy,
    // false, acl);
    // mngr.linkFile("test.txt", fileId, 1);
    //        
    // ACLEntry[] aclArray = mngr.getFileEntity(fileId).getAcl();
    // assertEquals(aclArray.length, 3);
    // for (ACLEntry entry : aclArray)
    // assertTrue((entry.getEntity().equals("1") && entry.getRights() == 3)
    // || (entry.getEntity().equals("2") && entry.getRights() == 7)
    // || (entry.getEntity().equals("3") && entry.getRights() == 1));
    //        
    // acl.clear();
    // acl.put("4", 4L);
    // mngr.setFileACL(fileId, acl);
    // aclArray = mngr.getFileEntity(fileId).getAcl();
    // assertEquals(aclArray.length, 1);
    // assertEquals(aclArray[0].getEntity(), "4");
    // assertEquals(aclArray[0].getRights(), 4L);
    // }
    //    
    // // public void testMisc() throws Exception {
    // //
    // // long fileId = mngr.createFile(null, "me", "myGroup",
    // // getDefaultStripingPolicy(), false, null);
    // // mngr.linkFile("newFile", fileId, 1);
    // // AbstractFileEntity file = mngr.getFileEntity(fileId);
    // //
    // // AbstractFileEntity copy = Converter
    // // .mapToFile((Map<String, Object>) Converter.fileTreeToList(mngr,
    // // file).get(0));
    // //
    // // assertEquals(file.getAtime(), copy.getAtime());
    // // assertEquals(file.getCtime(), copy.getCtime());
    // // assertEquals(file.getMtime(), copy.getMtime());
    // // assertEquals(file.getAcl(), copy.getAcl());
    // // assertEquals(file.getUserId(), copy.getUserId());
    // // assertEquals(file.getGroupId(), copy.getGroupId());
    // // assertEquals(((FileEntity) file).getSize(), ((FileEntity) copy)
    // // .getSize());
    // // assertEquals(((FileEntity) file).getXLocationsList(),
    // // ((FileEntity) copy).getXLocationsList());
    // // }
    
    
    private static Map<String, Object> getDefaultStripingPolicy() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("policy", "RAID0");
        map.put("stripe-size", 1000L);
        map.put("width", 1L);
        return map;
    }
}
