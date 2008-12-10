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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.mrc.brain.storage.SliceID;
import org.xtreemfs.mrc.brain.storage.StorageManager;
import org.xtreemfs.mrc.brain.storage.entities.ACLEntry;
import org.xtreemfs.mrc.brain.storage.entities.AbstractFileEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileEntity;
import org.xtreemfs.test.SetupUtils;

public class StorageManagerTest extends TestCase {

    public static final String DB_DIRECTORY = "/tmp/xtreemfs-test";

    private StorageManager     mngr;

    private RequestController  dir;

    private DIRClient          dirClient;

    public StorageManagerTest() {
        super();
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
        mngr = new StorageManager(DB_DIRECTORY, new SliceID(1));
        mngr.startup();
    }

    protected void tearDown() throws Exception {
        mngr.cleanup();
        dirClient.shutdown();
        dir.shutdown();

        dirClient.waitForShutdown();
    }

    public void testCreateDelete() throws Exception {

        final String userId = "me";
        final String groupId = "myGroup";
        final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();

        long rootDirId = mngr.createFile(null, userId, groupId, stripingPolicy, true, null);
        mngr.linkFile("rootDir", rootDirId, 1);
        assertFalse(rootDirId == -1);

        long subDirId = mngr.createFile(null, userId, groupId, stripingPolicy, true, null);
        mngr.linkFile("subDir", subDirId, rootDirId);
        assertFalse(subDirId == -1);

        long fileId = mngr.createFile(null, userId, groupId, stripingPolicy, false, null);
        mngr.linkFile("file.txt", fileId, subDirId);
        assertFalse(fileId == -1);
        assertFalse(mngr.hasChildren(fileId));

        assertEquals(mngr.getFileEntity("rootDir").getId(), rootDirId);
        assertEquals(mngr.getFileEntity("rootDir/subDir").getId(), subDirId);
        assertEquals(mngr.getFileEntity("rootDir/subDir/file.txt").getId(), fileId);

        assertTrue(mngr.getFileEntity(subDirId).isDirectory());
        assertTrue(mngr.hasChildren(subDirId));
        assertEquals(mngr.getChildren(subDirId).size(), 1);
        assertEquals(mngr.getChildren(subDirId).get(0), "file.txt");
        assertEquals(mngr.getChildren(fileId).size(), 0);

        assertTrue(mngr.fileExists(rootDirId, "subDir"));

        Map<String, AbstractFileEntity> fileData = mngr.getChildData(subDirId);
        assertEquals(fileData.size(), 1);
        mngr.unlinkFile("file.txt", fileId, subDirId);

        AbstractFileEntity file = fileData.values().iterator().next();
        file.setLinkCount(0);
        file.setId(0);
        fileId = mngr.createFile(file, null);
        mngr.linkFile("newFile.txt", fileId, rootDirId);
        assertEquals(1, mngr.getFileEntity(fileId).getLinkCount());
        mngr.linkFile("newFile.txt", fileId, subDirId);
        assertEquals(2, mngr.getFileEntity(fileId).getLinkCount());
        assertEquals(mngr.getFileEntity("rootDir/subDir/newFile.txt").getId(), fileId);
        assertEquals(mngr.getFileEntity("rootDir/newFile.txt").getId(), fileId);

        List<String> children = mngr.getChildren(rootDirId);
        assertEquals(children.size(), 2);
        assertTrue(children.contains("newFile.txt") && children.contains("subDir"));
        assertEquals(mngr.getChildren(subDirId).size(), 1);

        assertEquals(mngr.getStripingPolicy(rootDirId).getPolicy(), "RAID0");
        assertEquals(mngr.getStripingPolicy(rootDirId).getWidth(), 1L);
        assertEquals(mngr.getStripingPolicy(rootDirId).getStripeSize(), 1000L);

        mngr.unlinkFile("newFile.txt", fileId, rootDirId);
        assertNotNull(mngr.getFileEntity(fileId));
        assertEquals(1, mngr.getFileEntity(fileId).getLinkCount());
        mngr.unlinkFile("newFile.txt", fileId, subDirId);
        assertNull(mngr.getFileEntity(fileId));

        mngr.unlinkFile("subDir", subDirId, rootDirId);
        assertEquals(mngr.getChildren(rootDirId).size(), 0);
        mngr.unlinkFile("rootDir", rootDirId, 1);
        assertNull(mngr.getFileEntity(rootDirId));

        long id1 = mngr.createFile(null, userId, groupId, stripingPolicy, false, null);
        long id2 = mngr.createFile(null, userId, groupId, stripingPolicy, false, null);
        long id3 = mngr.createFile(null, userId, groupId, stripingPolicy, false, null);
        mngr.linkFile("bla1.txt", id1, 1);
        mngr.linkFile("bla2.txt", id2, 1);
        mngr.linkFile("bla3.txt", id3, 1);

        children = mngr.getChildren(1);
        assertTrue(children.contains("bla1.txt"));
        assertTrue(children.contains("bla2.txt"));
        assertTrue(children.contains("bla3.txt"));
        assertEquals(children.size(), 3);

        mngr.unlinkFile("bla1.txt", id1, 1);
        mngr.unlinkFile("bla2.txt", id2, 1);
        mngr.unlinkFile("bla3.txt", id3, 1);

        assertFalse(mngr.fileExists(subDirId, "file.txt"));
    }

    public void testSymlink() throws Exception {

        final String userId = "me";
        final String groupId = "myGroup";
        final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();

        long fileId = mngr.createFile("blub/bla.txt", userId, groupId, stripingPolicy, false, null);
        mngr.linkFile("test.txt", fileId, 1);
        assertEquals(mngr.getFileReference(fileId), "blub/bla.txt");
    }

    public void testAttributes() throws Exception {

        final String userId = "me";
        final String groupId = "myGroup";
        final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("myKey", "myValue");
        attrs.put("blaKey", "blaValue");

        long fileId = mngr.createFile(null, userId, groupId, stripingPolicy, false, null);
        mngr.linkFile("test.txt", fileId, 1);
        mngr.addXAttributes(fileId, attrs);

        attrs = mngr.getXAttributes(fileId);
        assertEquals(attrs.size(), 2);

        List<Object> list = new ArrayList<Object>();
        list.add("myKey");
        mngr.deleteXAttributes(fileId, list);
        assertEquals(mngr.getXAttributes(fileId).size(), 1);
        assertEquals(mngr.getXAttributes(fileId).get("blaKey"), "blaValue");
        assertNull(mngr.getXAttributes(fileId).get("myKey"));

        mngr.addXAttributes(fileId, attrs);
        assertEquals(mngr.getXAttributes(fileId).size(), 2);
        mngr.deleteXAttributes(fileId, null);
        assertEquals(mngr.getXAttributes(fileId).size(), 0);
    }

    public void testPosixAttributes() throws Exception {

        final String userId = "me";
        final String groupId = "myGroup";
        final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();

        long fileId = mngr.createFile(null, userId, groupId, stripingPolicy, false, null);
        mngr.linkFile("test.txt", fileId, 1);

        mngr.setFileSize(fileId, 121, 0, 0);
        assertEquals(mngr.getFileEntity("test.txt").getId(), fileId);
        assertEquals(((FileEntity) mngr.getFileEntity("test.txt")).getSize(), 121);
    }

    public void testACLs() throws Exception {

        final String userId = "me";
        final String groupId = "myGroup";
        final Map<String, Object> stripingPolicy = getDefaultStripingPolicy();

        Map<String, Object> acl = new HashMap<String, Object>();
        acl.put("1", 3L);
        acl.put("2", 7L);
        acl.put("3", 1L);

        long fileId = mngr.createFile(null, userId, groupId, stripingPolicy, false, acl);
        mngr.linkFile("test.txt", fileId, 1);

        ACLEntry[] aclArray = mngr.getFileEntity(fileId).getAcl();
        assertEquals(aclArray.length, 3);
        for (ACLEntry entry : aclArray)
            assertTrue((entry.getEntity().equals("1") && entry.getRights() == 3)
                || (entry.getEntity().equals("2") && entry.getRights() == 7)
                || (entry.getEntity().equals("3") && entry.getRights() == 1));

        acl.clear();
        acl.put("4", 4L);
        mngr.setFileACL(fileId, acl);
        aclArray = mngr.getFileEntity(fileId).getAcl();
        assertEquals(aclArray.length, 1);
        assertEquals(aclArray[0].getEntity(), "4");
        assertEquals(aclArray[0].getRights(), 4L);
    }

    // public void testMisc() throws Exception {
    //
    // long fileId = mngr.createFile(null, "me", "myGroup",
    // getDefaultStripingPolicy(), false, null);
    // mngr.linkFile("newFile", fileId, 1);
    // AbstractFileEntity file = mngr.getFileEntity(fileId);
    //
    // AbstractFileEntity copy = Converter
    // .mapToFile((Map<String, Object>) Converter.fileTreeToList(mngr,
    // file).get(0));
    //
    // assertEquals(file.getAtime(), copy.getAtime());
    // assertEquals(file.getCtime(), copy.getCtime());
    // assertEquals(file.getMtime(), copy.getMtime());
    // assertEquals(file.getAcl(), copy.getAcl());
    // assertEquals(file.getUserId(), copy.getUserId());
    // assertEquals(file.getGroupId(), copy.getGroupId());
    // assertEquals(((FileEntity) file).getSize(), ((FileEntity) copy)
    // .getSize());
    // assertEquals(((FileEntity) file).getXLocationsList(),
    // ((FileEntity) copy).getXLocationsList());
    // }

    public static void main(String[] args) {
        TestRunner.run(StorageManagerTest.class);
    }

    private static Map<String, Object> getDefaultStripingPolicy() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("policy", "RAID0");
        map.put("stripe-size", 1000L);
        map.put("width", 1L);
        return map;
    }
}
