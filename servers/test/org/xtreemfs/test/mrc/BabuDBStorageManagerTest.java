/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.mrc;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.new_mrc.dbaccess.AtomicDBUpdate;
import org.xtreemfs.new_mrc.dbaccess.BabuDBStorageManager;
import org.xtreemfs.new_mrc.dbaccess.DBAccessResultAdapter;
import org.xtreemfs.new_mrc.dbaccess.DBAccessResultListener;
import org.xtreemfs.new_mrc.dbaccess.DatabaseException;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.dbaccess.DatabaseException.ExceptionType;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.test.SetupUtils;

public class BabuDBStorageManagerTest extends TestCase {
    
    public static final String     DB_DIRECTORY = "/tmp/xtreemfs-test";
    
    private StorageManager         mngr;
    
    private RequestController      dir;
    
    private DIRClient              dirClient;
    
    private BabuDB                 database;
    
    private Exception              exc;
    
    private Object                 lock         = "";
    
    private DBAccessResultListener listener     = new DBAccessResultAdapter() {
                                                    
                                                    @Override
                                                    public void insertFinished(Object context) {
                                                        synchronized (lock) {
                                                            lock.notify();
                                                        }
                                                    }
                                                    
                                                    @Override
                                                    public void requestFailed(Object context,
                                                        Throwable error) {
                                                        exc = (Exception) error;
                                                        synchronized (lock) {
                                                            lock.notify();
                                                        }
                                                    }
                                                };
    
    public BabuDBStorageManagerTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }
    
    protected void setUp() throws Exception {
        
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        // initialize Directory Service (for synchronized clocks...)
        DIRConfig config = SetupUtils.createDIRConfig();
        dir = new RequestController(config);
        dir.startup();
        dirClient = SetupUtils.createDIRClient(60000);
        TimeSync.initialize(dirClient, 60000, 50, NullAuthProvider.createAuthString("bla", "bla"));
        
        // reset database
        File dbDir = new File(DB_DIRECTORY);
        FSUtils.delTree(dbDir);
        dbDir.mkdirs();
        database = new BabuDB(DB_DIRECTORY, DB_DIRECTORY, 2, 1024 * 1024 * 16, 5 * 60, false);
        mngr = new BabuDBStorageManager(database, "test");
    }
    
    protected void tearDown() throws Exception {
        database.shutdown();
        dirClient.shutdown();
        dir.shutdown();
        
        dirClient.waitForShutdown();
    }
    
    public void testCreateDelete() throws Exception {
        
        final String userId = "me";
        final String groupId = "myGroup";
        final short perms = 511;
        final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();
        exc = null;
        
        // create root directory
        final String rootDirName = "";
        
        AtomicDBUpdate update = mngr.createAtomicDBUpdate(listener, null);
        FileMetadata rootDir = mngr.create(0, rootDirName, userId, groupId, stripingPolicy, perms,
            null, true, update);
        update.execute();
        waitForResponse();
        
        assertEquals(rootDir.getId(), 1);
        
        // retrieve root directory
        FileMetadata metadata = mngr.getMetadata(0, rootDirName);
        assertTrue(metadata.isDirectory());
        assertTrue(metadata.getAtime() > 0);
        assertTrue(metadata.getCtime() > 0);
        assertTrue(metadata.getMtime() > 0);
        assertEquals(rootDir.getId(), metadata.getId());
        assertEquals(perms, metadata.getPerms());
        assertEquals(rootDirName, metadata.getFileName());
        assertEquals(userId, metadata.getOwnerId());
        assertEquals(groupId, metadata.getOwningGroupId());
        
        // create nested file
        final String fileName = "testfile.txt";
        
        update = mngr.createAtomicDBUpdate(listener, null);
        FileMetadata nextDir = mngr.create(rootDir.getId(), fileName, userId, groupId,
            stripingPolicy, perms, null, false, update);
        update.execute();
        waitForResponse();
        
        assertEquals(nextDir.getId(), 2);
        
        metadata = mngr.getMetadata(rootDir.getId(), fileName);
        assertFalse(metadata.isDirectory());
        assertTrue(metadata.getAtime() > 0);
        assertTrue(metadata.getCtime() > 0);
        assertTrue(metadata.getMtime() > 0);
        assertEquals(0, metadata.getEpoch());
        assertEquals(0, metadata.getIssuedEpoch());
        assertEquals(nextDir.getId(), metadata.getId());
        assertEquals(0, metadata.getSize());
        assertEquals(1, metadata.getLinkCount());
        assertEquals(perms, metadata.getPerms());
        assertEquals(fileName, metadata.getFileName());
        assertEquals(userId, metadata.getOwnerId());
        assertEquals(groupId, metadata.getOwningGroupId());
        
        // create nested dir
        final String dirName = "someDir";
        
        update = mngr.createAtomicDBUpdate(listener, null);
        FileMetadata dir = mngr.create(rootDir.getId(), dirName, userId, groupId, stripingPolicy,
            perms, null, true, update);
        update.execute();
        waitForResponse();
        
        assertEquals(dir.getId(), 3);
        
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
        Iterator<FileMetadata> children = mngr.getChildren(rootDir.getId());
        List<String> tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        
        assertEquals(2, tmp.size());
        assertTrue(tmp.contains(dirName));
        assertTrue(tmp.contains(fileName));
        
        // try to create an existing file
        try {
            update = mngr.createAtomicDBUpdate(listener, null);
            mngr.create(rootDir.getId(), fileName, userId, groupId, stripingPolicy, perms, null,
                false, update);
            update.execute();
            waitForResponse();
            
            fail();
            
        } catch (DatabaseException exc) {
            assertEquals(exc.getType(), ExceptionType.FILE_EXISTS);
        }
        
        // delete file
        update = mngr.createAtomicDBUpdate(listener, null);
        mngr.delete(rootDir.getId(), fileName, update);
        update.execute();
        waitForResponse();
        
        // list files; only the nested directory should be in 'rootDir'
        children = mngr.getChildren(rootDir.getId());
        tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        
        assertEquals(1, tmp.size());
        assertEquals(dirName, tmp.get(0));
        
        // create file again
        update = mngr.createAtomicDBUpdate(listener, null);
        mngr.create(rootDir.getId(), fileName, userId, groupId, stripingPolicy, perms, null, false,
            update);
        update.execute();
        waitForResponse();
        
        // list files; both the nested directory and file should be in 'rootDir'
        // again
        children = mngr.getChildren(rootDir.getId());
        tmp = new LinkedList<String>();
        while (children.hasNext())
            tmp.add(children.next().getFileName());
        
        assertEquals(2, tmp.size());
        assertTrue(tmp.contains(dirName));
        assertTrue(tmp.contains(fileName));
    }
    
    public void testCreateDeleteWithCollidingHashCodes() throws Exception {
        
        // create two files w/ colliding hash codes in nested dir
        final long dirId = 0;
        final String name1 = "Cfat";
        final String name2 = "CgCU";
        final String uid1 = "uid1";
        final String uid2 = "uid2";
        final String gid1 = "gid1";
        final String gid2 = "gid2";
        final short perms1 = 0;
        final short perms2 = Short.MAX_VALUE;
        
        AtomicDBUpdate update = mngr.createAtomicDBUpdate(listener, null);
        mngr.create(dirId, name1, uid1, gid1, null, perms1, null, false, update);
        update.execute();
        waitForResponse();
        
        update = mngr.createAtomicDBUpdate(listener, null);
        mngr.create(dirId, name2, uid2, gid2, null, perms2, null, false, update);
        update.execute();
        waitForResponse();
        
        // list files; both the nested files are found and contain the correct
        // data
        Iterator<FileMetadata> children = mngr.getChildren(dirId);
        Map<String, FileMetadata> map = new HashMap<String, FileMetadata>();
        while (children.hasNext()) {
            FileMetadata child = children.next();
            map.put(child.getFileName(), child);
        }
        
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
        children = mngr.getChildren(dirId);
        assertTrue(children.hasNext());
        assertEquals(name2, children.next().getFileName());
        assertFalse(children.hasNext());
    }
    
    public void testPathResolution() throws Exception {
        
        final String userId = "me";
        final String groupId = "myGroup";
        final short perms = 511;
        final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();
        exc = null;
        
        // crete the root directory
        AtomicDBUpdate update = mngr.createAtomicDBUpdate(listener, null);
        long rootDirId = mngr.create(0, "", userId, groupId, null, perms, null, true, update)
                .getId();
        update.execute();
        waitForResponse();
        
        // create a small directory tree and test path resolution
        long nextId = 0;
        
        update = mngr.createAtomicDBUpdate(listener, null);
        long comp1Id = nextId = mngr.create(rootDirId, "comp1", userId, groupId, stripingPolicy,
            perms, null, true, update).getId();
        update.execute();
        waitForResponse();
        update = mngr.createAtomicDBUpdate(listener, null);
        nextId = mngr.create(nextId, "comp2", userId, groupId, stripingPolicy, perms, null, true,
            update).getId();
        update.execute();
        waitForResponse();
        update = mngr.createAtomicDBUpdate(listener, null);
        nextId = mngr.create(nextId, "comp3", userId, groupId, stripingPolicy, perms, null, true,
            update).getId();
        update.execute();
        waitForResponse();
        update = mngr.createAtomicDBUpdate(listener, null);
        nextId = mngr.create(nextId, "file.txt", "usr", "grp", stripingPolicy, perms, null, true,
            update).getId();
        update.execute();
        waitForResponse();
        
        long id = mngr.resolvePath("comp1");
        assertEquals(comp1Id, id);
        
        id = mngr.resolvePath("comp1/comp2/comp3/file.txt");
        assertEquals(nextId, id);
        
        // test path resolution conrner cases
        id = mngr.resolvePath("comp1/");
        assertEquals(comp1Id, id);
        
        id = mngr.resolvePath("/comp1/");
        assertEquals(comp1Id, id);
        
        id = mngr.resolvePath("");
        assertEquals(rootDirId, id);
        
        id = mngr.resolvePath("/");
        assertEquals(rootDirId, id);
        
    }
    
    private void waitForResponse() throws Exception {
        
        synchronized (lock) {
            lock.wait();
        }
        
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
    
    public static void main(String[] args) {
        TestRunner.run(BabuDBStorageManagerTest.class);
    }
    
    private static Map<String, Object> getDefaultStripingPolicy() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("policy", "RAID0");
        map.put("stripe-size", 1000L);
        map.put("width", 1L);
        return map;
    }
}
