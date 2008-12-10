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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.mrc.slices.SliceManager;
import org.xtreemfs.test.SetupUtils;

public class SliceManagerTest extends TestCase {

    public static final String DB_DIRECTORY = "/tmp/database";

    private SliceManager mngr;

    public SliceManagerTest() {
        super();
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        // File dbDir = new File(DB_DIRECTORY);
        // FSTools.delTree(dbDir);
        // dbDir.mkdir();
        //
        // mngr = new SliceManager(DB_DIRECTORY);
        // mngr.startup();
    }

    protected void tearDown() throws Exception {

        // TODO

        // mngr.shutdown();
    }

    public void testAll() throws Exception {

        // TODO

        // final String volumeId = "12";
        // final String ownerId = "me";
        // final String groupId = "myGroup";
        //
        // mngr.createVolume(volumeId, "myVolume", ownerId, groupId,
        // getDefaultStripingPolicy(), 1, 1, false);
        // assertEquals(mngr.getVolumes().size(), 1);
        //
        // VolumeInfo info = mngr.getVolumeByName("myVolume");
        // assertEquals(info.getId(), volumeId);
        //
        // // test backup
        // mngr.backupDB();
        // assertTrue(mngr.hasDBBackup());
        // mngr.restoreDBBackup();
        // assertFalse(mngr.hasDBBackup());
        // assertNotNull(mngr.getVolumeById(volumeId));
        //
        // // test sync and relink
        // long fileId = mngr.getStorageManager(volumeId).createFile("bla.txt",
        // null, 1, "me", "myGroup", null, false, null);
        // mngr.syncSliceDB();
        // mngr.getStorageManager(volumeId).deleteFile(fileId);
        // assertNull(mngr.getStorageManager(volumeId).getFileEntity(fileId));
        //
        // mngr.relinkVolume(volumeId);
        // assertEquals(mngr.getStorageManager(volumeId).getFileId("bla.txt"),
        // fileId);
        // assertNotNull(mngr.getVolumeById(volumeId));
        //
        // mngr.reset();
        // assertEquals(mngr.getVolumes().size(), 0);
    }

    public static void main(String[] args) {
        TestRunner.run(SliceManagerTest.class);
    }

    private static Map<String, Object> getDefaultStripingPolicy() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("policy", "RAID0");
        map.put("stripe-size", 1000L);
        map.put("width", 1L);
        return map;
    }
}
