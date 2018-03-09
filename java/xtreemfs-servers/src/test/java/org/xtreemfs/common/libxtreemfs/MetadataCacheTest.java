/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Setattrs;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;
import org.xtreemfs.SetupUtils;
import org.xtreemfs.TestHelper;

/**
 * 
 * <br>
 * Sep 30, 2011
 */
public class MetadataCacheTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    private class MetadataCacheSmasherThread extends Thread {

        private final MetadataCache      mdCache;

        private final String[]           paths;

        private final Stat[]             stats;

        private final DirectoryEntries[] dirs;

        private boolean            failed;

        /**
         * 
         */
        public MetadataCacheSmasherThread(MetadataCache cache, String[] paths, Stat[] stats,
                DirectoryEntries[] dirs) {

            this.mdCache = cache;

            this.paths = paths;
            this.stats = stats;
            this.dirs = dirs;

            this.failed = false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {

            while (true) {

                int operation = new Random().nextInt(10) + 1;
                int object = new Random().nextInt(10);

                try {

                    switch (operation) {
                    case 1:
                        mdCache.updateStat(paths[object], stats[object]);
                        break;
                    case 2:
                        long time = System.currentTimeMillis() / 1000;
                        mdCache.updateStatTime(paths[object], time, Setattrs.SETATTR_ATIME.getNumber());
                        break;
                    case 3:
                        int i = 1;
                        i = i << new Random().nextInt(7);
                        mdCache.updateStatAttributes(paths[object], stats[object], i);
                        break;
                    case 4:
                        mdCache.getStat(paths[object]);
                        break;
                    case 5:
                        mdCache.size();
                        break;
                    case 6:
                        mdCache.invalidate(paths[object]);
                        break;
                    case 7:
                        mdCache.updateDirEntries(paths[object], dirs[object]);
                        break;
                    case 8:
                        mdCache.getDirEntries(paths[object], 0, 1024);
                        break;
                    case 9:
                        mdCache.invalidatePrefix(paths[object]);
                        break;
                    case 10:
                        mdCache.renamePrefix(paths[object], paths[new Random().nextInt(10)]);
                        break;
                    }

                    sleep(10);

                } catch (Exception e) {
                    e.printStackTrace();
                    failed = true;
                }

            }

        }

        public boolean getFailed() {
            return failed;
        }
    }

    private MetadataCache metadataCache;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
        // Max 2 entries, 1 hour
        metadataCache = new MetadataCache(2, 3600);
    }

    /**
     * If a Stat entry gets updated through UpdateStatTime(), the new timeout must be respected in case of an
     * eviction.
     * 
     **/
    @Test
    public void testUpdateStatTimeKeepsSequentialTimeoutOrder() throws Exception {
        Stat.Builder a, b, c;
        a = getIntializedStatBuilder();
        b = getIntializedStatBuilder();
        c = getIntializedStatBuilder();

        a.setIno(0);
        b.setIno(1);
        c.setIno(2);

        metadataCache.updateStat("/a", a.build());
        metadataCache.updateStat("/b", b.build());
        // Cache is full now. a would should be first item to get evicted.
        metadataCache.updateStatTime("/a", 0, Setattrs.SETATTR_MTIME.getNumber());
        // "b" should be the oldest Stat element now and get evicted.
        metadataCache.updateStat("/c", c.build());
        // Was "a" found or did "b" survive?
        Stat aStat = metadataCache.getStat("/a");
        assertNotNull(aStat);
        assertEquals(0, aStat.getIno());
        // "c" ist also still there?!
        Stat cStat = metadataCache.getStat("/c");
        assertNotNull(cStat);
        assertEquals(2, cStat.getIno());

    }

    /**
     * If a Stat entry gets updated through UpdateStat(), the new timeout must be respected in case of an
     * eviction.
     **/
    @Test
    public void testUpdateStatKeepsSequentialTimeoutOrder() throws Exception {
        Stat.Builder a, b, c;
        a = getIntializedStatBuilder();
        b = getIntializedStatBuilder();
        c = getIntializedStatBuilder();

        a.setIno(0);
        b.setIno(1);
        c.setIno(2);

        Stat aa = a.build();

        metadataCache.updateStat("/a", aa);
        metadataCache.updateStat("/b", b.build());
        // Cache is full now. a would should be first item to get evicted.
        metadataCache.updateStat("/a", aa);
        // "b" should be the oldest Stat element now and get evicted.
        metadataCache.updateStat("/c", c.build());
        // Was "a" found or did "b" survive?
        Stat aStat = metadataCache.getStat("/a");
        assertNotNull(aStat);
        assertEquals(0, aStat.getIno());
        // "c" ist also still there?!
        Stat cStat = metadataCache.getStat("/c");
        assertNotNull(cStat);
        assertEquals(2, cStat.getIno());
    }

    /**
     * Test if Size is updated correctly after UpdateStat() or Invalidate().
     **/
    @Test
    public void testCheckSizeAfterUpdateAndInvalidate() throws Exception {
        Stat.Builder a, b, c;
        a = getIntializedStatBuilder();
        b = getIntializedStatBuilder();
        c = getIntializedStatBuilder();

        assertEquals(0l, metadataCache.size());
        metadataCache.updateStat("/a", a.build());
        assertEquals(1l, metadataCache.size());
        metadataCache.updateStat("/b", b.build());
        assertEquals(2l, metadataCache.size());
        metadataCache.updateStat("/c", c.build());
        // metadatacache has only room for two entries.
        assertEquals(2l, metadataCache.size());

        metadataCache.invalidate("/b");
        assertEquals(1l, metadataCache.size());
        metadataCache.invalidate("/c");
        assertEquals(0l, metadataCache.size());

    }

    @Test
    public void testGetUpdateDirEntries() throws Exception {

        DirectoryEntries.Builder dirEntriesBuilder = DirectoryEntries.newBuilder();
        int chunkSize = 1024;
        int entryCount = chunkSize;
        String dir = "/";

        // Fill dirEntriesBuilder
        for (int i = 0; i < entryCount; i++) {

            // create new Stat object for a new entry object
            Stat.Builder a = getIntializedStatBuilder();
            a.setIno(i);
            DirectoryEntry entry = DirectoryEntry.newBuilder().setName(dir + i).setStbuf(a.build()).build();

            dirEntriesBuilder.addEntries(entry);
        }

        metadataCache.updateDirEntries(dir, dirEntriesBuilder.build());

        // Read all dir entries.
        DirectoryEntries dirEntriesRead = metadataCache.getDirEntries(dir, 0, entryCount);

        assertEquals(entryCount, dirEntriesRead.getEntriesCount());
        for (int i = 0; i < dirEntriesRead.getEntriesCount(); i++) {
            String pathToStat = dir + i;
            assertEquals(i, dirEntriesRead.getEntries(i).getStbuf().getIno());
            assertEquals(pathToStat, dirEntriesRead.getEntries(i).getName());
        }

        // Read a subset.
        int offset = entryCount / 2;

        dirEntriesRead = metadataCache.getDirEntries(dir, offset, entryCount / 2 - 1);
        assertEquals(entryCount / 2 - 1, dirEntriesRead.getEntriesCount());

        for (int i = 0; i < dirEntriesRead.getEntriesCount(); i++) {
            String pathToStat = dir + (offset + i);
            assertEquals(pathToStat, dirEntriesRead.getEntries(i).getName());
            assertEquals(offset + i, dirEntriesRead.getEntries(i).getStbuf().getIno());
        }

        dirEntriesBuilder = DirectoryEntries.newBuilder();

        // Fill dirEntriesBuilder with other entries for this dir
        for (int i = 10; i < entryCount + 10; i++) {

            // create new Stat object for a new entry object
            Stat.Builder a = getIntializedStatBuilder();
            a.setIno(i);
            DirectoryEntry entry = DirectoryEntry.newBuilder().setName(dir + i).setStbuf(a.build()).build();

            dirEntriesBuilder.addEntries(entry);
        }

        metadataCache.updateDirEntries(dir, dirEntriesBuilder.build());

        // Read all dir entries.
        dirEntriesRead = metadataCache.getDirEntries(dir, 0, entryCount);

        assertEquals(entryCount, dirEntriesRead.getEntriesCount());
        for (int i = 0; i < dirEntriesRead.getEntriesCount(); i++) {
            String pathToStat = dir + (i + 10);
            assertEquals(i + 10, dirEntriesRead.getEntries(i).getStbuf().getIno());
            assertEquals(pathToStat, dirEntriesRead.getEntries(i).getName());
        }
    }

    /**
     * If a Stat entry gets updated through UpdateStat(), the new timeout must be respected in case of an
     * eviction.
     */
    @Test
    public void testInvalidatePrefix() throws Exception {

        // create new metadataCache with 1024 entries.
        metadataCache = new MetadataCache(1024, 3600);

        Stat.Builder a, b, c, d;
        a = getIntializedStatBuilder();
        b = getIntializedStatBuilder();
        c = getIntializedStatBuilder();
        d = getIntializedStatBuilder();
        a.setIno(0);
        b.setIno(1);
        c.setIno(2);
        d.setIno(3);

        String dir = "/dir";

        metadataCache.updateStat(dir, a.build());
        metadataCache.updateStat(dir + "/file1", b.build());
        metadataCache.updateStat(dir + ".file1", c.build());
        metadataCache.updateStat(dir + "Zfile1", d.build());

        metadataCache.invalidatePrefix(dir);

        // invalidation of all matching entries successful?
        assertNull(metadataCache.getStat(dir));
        assertNull(metadataCache.getStat(dir + "/file1"));

        // Similiar entries which do not match the prefix "/dir/" have not been
        // invalidated.

        Stat statC = metadataCache.getStat(dir + ".file1");
        assertNotNull(statC);
        assertEquals(2, statC.getIno());
        Stat statD = metadataCache.getStat(dir + "Zfile1");
        assertNotNull(statD);
        assertEquals(3, statD.getIno());

    }

    /**
     * If a Stat entry gets updated through UpdateStat(), the new timeout must be respected in case of an
     * eviction.
     */
    @Test
    public void testRenamePrefix() throws Exception {

        // create new metadataCache with 1024 entries.
        metadataCache = new MetadataCache(1024, 3600);

        Stat.Builder a, b, c, d;
        a = getIntializedStatBuilder();
        b = getIntializedStatBuilder();
        c = getIntializedStatBuilder();
        d = getIntializedStatBuilder();
        a.setIno(0);
        b.setIno(1);
        c.setIno(2);
        d.setIno(3);

        String dir = "/dir";

        metadataCache.updateStat(dir, a.build());
        metadataCache.updateStat(dir + "/file1", b.build());
        metadataCache.updateStat(dir + ".file1", c.build());
        metadataCache.updateStat(dir + "Zfile1", d.build());
        assertEquals(4l, metadataCache.size());

        metadataCache.renamePrefix(dir, "/newDir");
        assertEquals(4l, metadataCache.size());

        // Renaming of all matching entries was successful?
        Stat statA = metadataCache.getStat("/newDir");
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        Stat statB = metadataCache.getStat("/newDir" + "/file1");
        assertNotNull(statB);
        assertEquals(1, statB.getIno());

        // Similiar entries which do not match the prefix "/dir/" hat not been renamed
        Stat statC = metadataCache.getStat(dir + ".file1");
        assertNotNull(statC);
        assertEquals(2, statC.getIno());
        Stat statD = metadataCache.getStat(dir + "Zfile1");
        assertNotNull(statD);
        assertEquals(3, statD.getIno());
    }

    /**
     * Are large nanoseconds values correctly updated by UpdateStatAttributes?
     */
    @Test
    public void testUpdateStatAttributes() throws Exception {

        // create new metadataCache with 1024 entries.
        metadataCache = new MetadataCache(1024, 3600);

        String path = "/file";
        Stat.Builder stat = getIntializedStatBuilder();
        Stat.Builder newStat = getIntializedStatBuilder();
        stat.setIno(0);
        newStat.setIno(1);

        metadataCache.updateStat(path, stat.build());
        assertEquals(1l, metadataCache.size());
        Stat statA = metadataCache.getStat(path);
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        assertEquals(0, statA.getMtimeNs());

        long time = 1234567890;
        time *= 1000000000;
        newStat.setAtimeNs(time);
        newStat.setMtimeNs(time);

        metadataCache.updateStatAttributes(path, newStat.build(), Setattrs.SETATTR_ATIME.getNumber()
                | Setattrs.SETATTR_MTIME.getNumber());
        assertEquals(1l, metadataCache.size());
        Stat statB = metadataCache.getStat(path);
        assertNotNull(statB);
        assertEquals(0, statB.getIno());
        assertEquals(time, statB.getAtimeNs());
        assertEquals(time, statB.getMtimeNs());
    }

    /**
     * Changing the file access mode may only modify the last 12 bits (3 bits for sticky bit, set GID and set
     * UID and 3 * 3 bits for the file access mode).
     */
    @Test
    public void testUpdateStatAttributesPreservesModeBits() throws Exception {

        String path = "/file";
        Stat.Builder stat = getIntializedStatBuilder();
        Stat.Builder cachedStat = getIntializedStatBuilder();

        stat.setIno(0);
        stat.setMode(33188); // Octal: 100644 ( regular file + 644).

        metadataCache.updateStat(path, stat.build());
        assertEquals(1l, metadataCache.size());
        Stat statA = metadataCache.getStat(path);
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        assertEquals(33188, statA.getMode());

        stat = getIntializedStatBuilder();
        stat.setMode(420); // Octal: 644
        metadataCache.updateStatAttributes(path, stat.build(), Setattrs.SETATTR_MODE.getNumber());
        assertEquals(1l, metadataCache.size());
        statA = metadataCache.getStat(path);
        assertNotNull(statA);
        assertEquals(0, cachedStat.getIno());
        assertEquals(33188, statA.getMode());

        stat = getIntializedStatBuilder();
        stat.setMode(263076); // Octal : 1001644 (regular file + sticky bit + 644).
        metadataCache.updateStat(path, stat.build());
        assertEquals(1l, metadataCache.size());
        statA = metadataCache.getStat(path);
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        assertEquals(263076, statA.getMode());

        stat = getIntializedStatBuilder();
        stat.setMode(511); // Octal: 0777 (no sticky bit + 777).
        metadataCache.updateStatAttributes(path, stat.build(), Setattrs.SETATTR_MODE.getNumber());
        assertEquals(1l, metadataCache.size());
        statA = metadataCache.getStat(path);
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        assertEquals(262655, statA.getMode()); // Octal: 1000777
    }

     @Test
     public void testConcurrentModifications() throws Exception {
    
     final int DATA_SIZE = 10;
     final java.lang.String FILENAME = "/foobarfile";
     final int DIR_COUNT = 64;
     final int THREAD_COUNT = 10;
    
     // generate Data
     String[] paths = new String[DATA_SIZE];
     Stat[] stats = new Stat[DATA_SIZE];
     DirectoryEntries[] dirs = new DirectoryEntries[DATA_SIZE];
    
     for (int i = 0; i < DATA_SIZE; i++) {
     paths[i] = new String(FILENAME + i + '/');
     stats[i] = getIntializedStatBuilder().setIno(i).build();
     DirectoryEntries.Builder dirBuilder = DirectoryEntries.newBuilder();
     for (int j = 0; j < DIR_COUNT; j++) {
     Stat a = getIntializedStatBuilder().setIno(i * 10000 + j).build();
     dirBuilder
     .addEntries(DirectoryEntry.newBuilder().setName(FILENAME + i + '/' + j).setStbuf(a));
     }
     dirs[i] = dirBuilder.build();
     }
    
     MetadataCacheSmasherThread[] threads = new MetadataCacheSmasherThread[THREAD_COUNT];
     for (int i = 0; i < THREAD_COUNT; i++) {
     threads[i] = new MetadataCacheSmasherThread(metadataCache, paths, stats, dirs);
     threads[i].start();
     }
    
     Thread.sleep(10000); // sleep 10 seconds and let the other threads work
    
     for (int i = 0; i < THREAD_COUNT; i++) {
     assertEquals(false, threads[i].getFailed());
     }
    
     }

    @Test
    public void testUnenabledMdCache() {
        metadataCache = new MetadataCache(0, 10000);
        assertEquals(0, metadataCache.size());
        assertEquals(0, metadataCache.capacity());
        assertNull(metadataCache.getDirEntries("/", 10, 100));
        assertNull(metadataCache.getStat("fewf"));
        assertNull(metadataCache.getXAttr("tttgreg", "wefwe").getFirst());
        assertFalse(metadataCache.getXAttr("tttgreg", "wefwe").getSecond());
        assertNull(metadataCache.getXAttrs("asdf"));
        assertEquals(0, metadataCache.getXAttrSize("zxcv", "naste").getFirst().intValue());
        assertFalse(metadataCache.getXAttrSize("zxcv", "naste").getSecond());

        metadataCache.invalidate("bla");
        metadataCache.invalidateDirEntries("blub");
        metadataCache.invalidateDirEntry("puuuh", "noooooo");
        metadataCache.invalidatePrefix("praefeix");
        metadataCache.invalidateStat("stat");
        metadataCache.invalidateXAttr("Xattr", "dont care");
        metadataCache.invalidateXAttrs("jea, finished");

        metadataCache.updateDirEntries("fsa", DirectoryEntries.getDefaultInstance());
        metadataCache.updateStat("fsa", Stat.getDefaultInstance());
        metadataCache.updateStatAttributes("fwef", Stat.getDefaultInstance(), 100);
        metadataCache.updateStatFromOSDWriteResponse("fefew", OSDWriteResponse.getDefaultInstance());
        metadataCache.updateStatTime("fsa", 1034l, 100);
        metadataCache.updateXAttr("fsa", "wefwe", "dfd");
        metadataCache.updateXAttrs("sdfs", listxattrResponse.getDefaultInstance());

        metadataCache.renamePrefix("foo", "bar");
    }

    private Stat.Builder getIntializedStatBuilder() {
        Stat.Builder statBuilder = Stat.newBuilder();

        statBuilder.setDev(0);
        statBuilder.setIno(0);
        statBuilder.setMode(0);
        // if not set to 1 an exception in the metadatacache is triggered
        statBuilder.setNlink(1);
        statBuilder.setUserId("");
        statBuilder.setGroupId("");
        statBuilder.setSize(0);
        statBuilder.setAtimeNs(0);
        statBuilder.setMtimeNs(0);
        statBuilder.setCtimeNs(0);
        statBuilder.setBlksize(0);
        statBuilder.setTruncateEpoch(0);

        return statBuilder;

    }

    @Test
    public void testUpdateStatFromOSDWriteResponse() throws Exception {
        OSDWriteResponse osdWriteResponse = OSDWriteResponse.newBuilder().setSizeInBytes(1337)
                .setTruncateEpoch(1338).build();
        metadataCache.updateStat("foobar", getIntializedStatBuilder().setSize(100).build());
        assertEquals(100, metadataCache.getStat("foobar").getSize());
        metadataCache.updateStatFromOSDWriteResponse("foobar", osdWriteResponse);
        assertEquals(1337, metadataCache.getStat("foobar").getSize());

        // Test with entry but no Stat
        DirectoryEntries entries = getDummyDirEntries();
        metadataCache.updateDirEntries("hasDirEntriesButNoStat", entries);
        metadataCache.updateStatFromOSDWriteResponse("hasDirEntriesButNoStat", osdWriteResponse);
        assertNull(metadataCache.getStat("hasDirEntriesButNoStat"));

        // Test with equal truncate epoch and higher size
        osdWriteResponse = OSDWriteResponse.newBuilder().setSizeInBytes(20000).setTruncateEpoch(20000)
                .build();
        metadataCache.updateStatFromOSDWriteResponse("foobar", osdWriteResponse);
        assertEquals(20000, metadataCache.getStat("foobar").getSize());
        metadataCache.updateStatFromOSDWriteResponse("foobar", osdWriteResponse);
        assertEquals(20000, metadataCache.getStat("foobar").getSize());
    }

    @Test
    public void testGetStatExpired() throws Exception {
        metadataCache = new MetadataCache(100, 1);
        Stat aStat = getIntializedStatBuilder().setSize(333).build();
        metadataCache.updateStat("foobar", aStat);
        assertEquals(aStat, metadataCache.getStat("foobar"));
        Thread.sleep(2000);
        assertNull(metadataCache.getStat("foobar"));
    }

    @Test
    public void testGetDirEntriesExpired() throws Exception {
        metadataCache = new MetadataCache(100, 1);
        DirectoryEntries entries = getDummyDirEntries();
        metadataCache.updateDirEntries("foobar", entries);
        assertEquals(entries, metadataCache.getDirEntries("foobar", 0, 1));
        Thread.sleep(2000);
        assertNull(metadataCache.getDirEntries("foobar", 0, 1));
    }

    @Test
    public void testGetNonExistingDirEntries() throws Exception {
        assertNull(metadataCache.getDirEntries("do not exist", 0, 100));
    }

    @Test
    public void testInvalidateStat() throws Exception {
        Stat aStat = getIntializedStatBuilder().build();
        metadataCache.updateStat("foobar", aStat);
        assertEquals(aStat, metadataCache.getStat("foobar"));
        metadataCache.invalidateStat("foobar");
        assertNull(metadataCache.getStat("foobar"));
    }

    @Test
    public void testInvalidateXattrs() throws Exception {
        listxattrResponse xattrs = getDummyXattrs();
        metadataCache.updateXAttrs("foobar", xattrs);
        assertEquals(xattrs, metadataCache.getXAttrs("foobar"));
        metadataCache.invalidateXAttrs("foobar");
        assertNull(metadataCache.getXAttrs("foobar"));
    }

    @Test
    public void testInvalidateXattr() throws Exception {
        metadataCache.invalidateXAttr("foobar", "where are you?");
        assertNull(metadataCache.getXAttr("foobar", "where are you?").getFirst());

        metadataCache.updateStat("foobar", getIntializedStatBuilder().build());
        metadataCache.invalidateXAttr("foobar", "still not there?");
        assertNull(metadataCache.getXAttr("foobar", "still not there?").getFirst());

        XAttr attr = XAttr.newBuilder().setName("deleteme").setValue("bla").build();
        listxattrResponse xattrs = getDummyXattrs().toBuilder().addXattrs(attr).build();
        metadataCache.updateXAttrs("foobar", xattrs);
        assertEquals("bla", metadataCache.getXAttr("foobar", "deleteme").getFirst());
        metadataCache.invalidateXAttr("foobar", "deleteme");
        assertNull(metadataCache.getXAttr("foobar", "deleteme").getFirst());
    }

    @Test
    public void testUpdateXattr() throws Exception {
        metadataCache.updateXAttr("foobar", "do not exist", "nothing to do");
        assertNull(metadataCache.getXAttr("foobar", "do not exist").getFirst());

        metadataCache.updateStat("foobar", getIntializedStatBuilder().build());
        metadataCache.updateXAttr("foobar", "do not exist", "nothing to do");
        assertNull(metadataCache.getXAttr("foobar", "do not exist").getFirst());

        // update existing xattr
        XAttr attr = XAttr.newBuilder().setName("newAttr").setValue("bla").build();
        listxattrResponse xattrs = getDummyXattrs().toBuilder().addXattrs(attr).build();
        metadataCache.updateXAttrs("foobar", xattrs);
        assertEquals("bla", metadataCache.getXAttr("foobar", "newAttr").getFirst());
        metadataCache.updateXAttr("foobar", "newAttr", "blub");
        assertEquals("blub", metadataCache.getXAttr("foobar", "newAttr").getFirst());

        // update non-existing xattr
        metadataCache.updateXAttr("foobar", "nonExistingXattr", "bar");
        assertEquals("bar", metadataCache.getXAttr("foobar", "nonExistingXattr").getFirst());
    }

    @Test
    public void testUpdateStatTime() throws Exception {
        metadataCache.updateStatTime("do not exists", 10000, Setattrs.SETATTR_ATIME.getNumber());
        metadataCache.updateXAttrs("foobar", getDummyXattrs());
        metadataCache.updateStatTime("foobar", 10000, Setattrs.SETATTR_ATIME.getNumber());

        metadataCache.updateStat("foobar", getIntializedStatBuilder().build());
        int toSet = Setattrs.SETATTR_ATIME.getNumber() | Setattrs.SETATTR_CTIME.getNumber()
                | Setattrs.SETATTR_MTIME.getNumber();
        metadataCache.updateStatTime("foobar", 13337, toSet);
        Stat aStat = metadataCache.getStat("foobar");
        assertEquals(13337l * 1000 * 1000 * 1000, aStat.getCtimeNs());
        assertEquals(13337l * 1000 * 1000 * 1000, aStat.getMtimeNs());
        assertEquals(13337l * 1000 * 1000 * 1000, aStat.getAtimeNs());
    }

    @Test
    public void testUpdateAttributes() throws Exception {
        metadataCache.updateStatAttributes("do not exist", getIntializedStatBuilder().build(),
                Setattrs.SETATTR_GID.getNumber());
        metadataCache.updateXAttrs("foobar", getDummyXattrs());
        metadataCache.updateStatAttributes("foobar", getIntializedStatBuilder().build(),
                Setattrs.SETATTR_GID.getNumber());

        metadataCache.updateStat("foobar", getIntializedStatBuilder().setTruncateEpoch(10).build());
        // Update stat with greater Truncate Epoch
        Stat updateStat = getIntializedStatBuilder().setUserId("TESTUSER").setGroupId("TESTGROUP")
                .setAtimeNs(13337).setCtimeNs(13337).setMtimeNs(13337).setSize(11111).setTruncateEpoch(11)
                .setAttributes(1000).build();
        int toSet = Setattrs.SETATTR_ATIME.getNumber() | Setattrs.SETATTR_CTIME.getNumber()
                | Setattrs.SETATTR_MTIME.getNumber() | Setattrs.SETATTR_GID.getNumber()
                | Setattrs.SETATTR_UID.getNumber() | Setattrs.SETATTR_SIZE.getNumber()
                | Setattrs.SETATTR_ATTRIBUTES.getNumber();
        metadataCache.updateStatAttributes("foobar", updateStat, toSet);
        Stat getStat = metadataCache.getStat("foobar");
        assertEquals(13337l, getStat.getCtimeNs());
        assertEquals(13337l, getStat.getMtimeNs());
        assertEquals(13337l, getStat.getAtimeNs());
        assertEquals("TESTUSER", getStat.getUserId());
        assertEquals("TESTGROUP", getStat.getGroupId());
        assertEquals(1000, getStat.getAttributes());
        assertEquals(11, getStat.getTruncateEpoch());
        assertEquals(11111, getStat.getSize());

        // Update stat with equal Truncate Epoch.
        Stat secondStat = getIntializedStatBuilder().setTruncateEpoch(11).setSize(22222).build();
        metadataCache.updateStatAttributes("foobar", secondStat, toSet);
        getStat = metadataCache.getStat("foobar");
        assertEquals(secondStat.getCtimeNs(), getStat.getCtimeNs());
        assertEquals(secondStat.getMtimeNs(), getStat.getMtimeNs());
        assertEquals(secondStat.getAtimeNs(), getStat.getAtimeNs());
        assertEquals(secondStat.getUserId(), getStat.getUserId());
        assertEquals(secondStat.getGroupId(), getStat.getGroupId());
        assertEquals(secondStat.getAttributes(), getStat.getAttributes());
        assertEquals(11, getStat.getTruncateEpoch());
        assertEquals(22222, getStat.getSize());
    }

    @Test
    public void testInvalidateDirEntry() throws Exception {
        metadataCache.invalidateDirEntry("not exists", "no name");
        metadataCache.updateStat("foobar", getIntializedStatBuilder().build());
        metadataCache.invalidateDirEntry("foobar", "no name");

        DirectoryEntries entries = getDummyDirEntries();
        DirectoryEntry entry = DirectoryEntry.newBuilder().setName("dir1")
                .setStbuf(getIntializedStatBuilder()).build();
        DirectoryEntry entry2 = DirectoryEntry.newBuilder().setName("dir2")
                .setStbuf(getIntializedStatBuilder()).build();

        entries = entries.toBuilder().addEntries(entry).addEntries(entry2).build();
        metadataCache.updateDirEntries("foobar", entries);

        DirectoryEntries getEntries = metadataCache.getDirEntries("foobar", 0, 10);
        assertEquals(entries, getEntries);
        metadataCache.invalidateDirEntry("foobar", "dir2");
        getEntries = metadataCache.getDirEntries("foobar", 0, 10);

        assertEquals(2, getEntries.getEntriesCount());
        for (DirectoryEntry dirEntry : getEntries.getEntriesList()) {
            assertNotSame("dir2", dirEntry.getName());
        }
    }

    @Test
    public void testInvalidateDirEntries() throws Exception {
        metadataCache.updateDirEntries("foobar", getDummyDirEntries());
        assertEquals(getDummyDirEntries(), metadataCache.getDirEntries("foobar", 0, 100));
        metadataCache.invalidateDirEntries("foobar");
        assertNull(metadataCache.getDirEntries("foobar", 0, 100));
    }

    @Test
    public void testGetXAttrSize() throws Exception {
        metadataCache.updateXAttrs("foobar", getDummyXattrs());
        metadataCache.updateXAttr("foobar", "aNewXattr", "0123456789");
        Tupel<Integer, Boolean> tupel = metadataCache.getXAttrSize("foobar", "aNewXattr");
        assertEquals(10, tupel.getFirst().intValue());
        assertTrue(tupel.getSecond());
    }

    private listxattrResponse getDummyXattrs() {
        return listxattrResponse.newBuilder()
                .addXattrs(XAttr.newBuilder().setName("foo").setValue("bar").build()).build();
    }

    private DirectoryEntries getDummyDirEntries() {
        DirectoryEntry entry = DirectoryEntry.newBuilder().setName("foo")
                .setStbuf(getIntializedStatBuilder()).build();
        DirectoryEntries entries = DirectoryEntries.newBuilder().addEntries(entry).build();
        return entries;
    }

}
