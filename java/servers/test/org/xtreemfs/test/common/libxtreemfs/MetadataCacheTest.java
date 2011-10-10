/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.common.libxtreemfs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Random;

import junit.framework.TestCase;

import org.xtreemfs.common.libxtreemfs.MetadataCache;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Setattrs;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;

/**
 * 
 * <br>
 * Sep 30, 2011
 */
public class MetadataCacheTest extends TestCase {

    private class MetadataCacheSmasherThread extends Thread {

        private MetadataCache      mdCache;

        private String[]           paths;

        private Stat[]             stats;

        private DirectoryEntries[] dirs;

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
                        updateStat.invoke(mdCache, paths[object], stats[object]);
                        break;
                    case 2:
                        long time = System.currentTimeMillis() / 1000;
                        updateStatTime.invoke(mdCache, paths[object], time,
                                Setattrs.SETATTR_ATIME.getNumber());
                        break;
                    case 3:
                        int i = 1;
                        i = i << new Random().nextInt(7);
                        updateStatAttributes.invoke(mdCache, paths[object], stats[object], i);
                        break;
                    case 4:
                        getStat.invoke(mdCache, paths[object]);
                        break;
                    case 5:
                        size.invoke(mdCache);
                        break;
                    case 6:
                        invalidate.invoke(mdCache, paths[object]);
                        break;
                    case 7:
                        updateDirEntries.invoke(mdCache, paths[object], dirs[object]);
                        break;
                    case 8:
                        getDirEntries.invoke(mdCache, paths[object], 0, 1024);
                        break;
                    case 9:
                        invalidatePrefix.invoke(mdCache, paths[object]);
                        break;
                    case 10:
                        renamePrefix.invoke(mdCache, paths[object], paths[new Random().nextInt(10)]);
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

    private Constructor<MetadataCache> constructor;

    private MetadataCache              metadataCache;

    private Method                     updateStat, updateStatTime, updateStatAttributes, getStat, size,
            invalidate, updateDirEntries, getDirEntries, invalidatePrefix, renamePrefix;

    /**
     * 
     */
    public MetadataCacheTest() throws Exception {

    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Max 2 entries, 1 hour
        constructor = MetadataCache.class.getDeclaredConstructor(new Class[] { long.class, long.class });
        constructor.setAccessible(true);
        metadataCache = constructor.newInstance(2, 3600);

        updateStat = MetadataCache.class.getDeclaredMethod("updateStat", new Class[] { String.class,
                Stat.class });
        updateStat.setAccessible(true);

        updateStatTime = MetadataCache.class.getDeclaredMethod("updateStatTime", new Class[] { String.class,
                long.class, int.class });
        updateStatTime.setAccessible(true);

        getStat = MetadataCache.class.getDeclaredMethod("getStat", new Class[] { String.class });
        getStat.setAccessible(true);

        size = MetadataCache.class.getDeclaredMethod("size");
        size.setAccessible(true);

        invalidate = MetadataCache.class.getDeclaredMethod("invalidate", new Class[] { String.class });
        invalidate.setAccessible(true);

        updateDirEntries = MetadataCache.class.getDeclaredMethod("updateDirEntries", new Class[] {
                String.class, DirectoryEntries.class });
        updateDirEntries.setAccessible(true);

        getDirEntries = MetadataCache.class.getDeclaredMethod("getDirEntries", new Class[] { String.class,
                int.class, int.class });
        getDirEntries.setAccessible(true);

        invalidatePrefix = MetadataCache.class.getDeclaredMethod("invalidatePrefix",
                new Class[] { String.class });
        invalidatePrefix.setAccessible(true);

        renamePrefix = MetadataCache.class.getDeclaredMethod("renamePrefix", new Class[] { String.class,
                String.class });
        renamePrefix.setAccessible(true);

        updateStatAttributes = MetadataCache.class.getDeclaredMethod("updateStatAttributes", new Class[] {
                String.class, Stat.class, int.class });
        updateStatAttributes.setAccessible(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * If a Stat entry gets updated through UpdateStatTime(), the new timeout must be respected in case of an
     * eviction.
     * 
     **/
    public void testUpdateStatTimeKeepsSequentialTimeoutOrder() throws Exception {
        Stat.Builder a, b, c;
        a = getIntializedStatBuilder();
        b = getIntializedStatBuilder();
        c = getIntializedStatBuilder();

        a.setIno(0);
        b.setIno(1);
        c.setIno(2);

        updateStat.invoke(metadataCache, "/a", a.build());
        updateStat.invoke(metadataCache, "/b", b.build());
        // Cache is full now. a would should be first item to get evicted.
        updateStatTime.invoke(metadataCache, "/a", 0, Setattrs.SETATTR_MTIME.getNumber());
        // "b" should be the oldest Stat element now and get evicted.
        updateStat.invoke(metadataCache, "/c", c.build());
        // Was "a" found or did "b" survive?
        Stat aStat = (Stat) getStat.invoke(metadataCache, "/a");
        assertNotNull(aStat);
        assertEquals(0, aStat.getIno());
        // "c" ist also still there?!
        Stat cStat = (Stat) getStat.invoke(metadataCache, "/c");
        assertNotNull(cStat);
        assertEquals(2, cStat.getIno());

    }

    /**
     * If a Stat entry gets updated through UpdateStat(), the new timeout must be respected in case of an
     * eviction.
     **/
    public void testUpdateStatKeepsSequentialTimeoutOrder() throws Exception {
        Stat.Builder a, b, c;
        a = getIntializedStatBuilder();
        b = getIntializedStatBuilder();
        c = getIntializedStatBuilder();

        a.setIno(0);
        b.setIno(1);
        c.setIno(2);

        Stat aa = a.build();

        updateStat.invoke(metadataCache, "/a", aa);
        updateStat.invoke(metadataCache, "/b", b.build());
        // Cache is full now. a would should be first item to get evicted.
        updateStat.invoke(metadataCache, "/a", aa);
        // "b" should be the oldest Stat element now and get evicted.
        updateStat.invoke(metadataCache, "/c", c.build());
        // Was "a" found or did "b" survive?
        Stat aStat = (Stat) getStat.invoke(metadataCache, "/a");
        assertNotNull(aStat);
        assertEquals(0, aStat.getIno());
        // "c" ist also still there?!
        Stat cStat = (Stat) getStat.invoke(metadataCache, "/c");
        assertNotNull(cStat);
        assertEquals(2, cStat.getIno());

    }

    /**
     * Test if Size is updated correctly after UpdateStat() or Invalidate().
     **/
    public void testCheckSizeAfterUpdateAndInvalidate() throws Exception {
        Stat.Builder a, b, c;
        a = getIntializedStatBuilder();
        b = getIntializedStatBuilder();
        c = getIntializedStatBuilder();

        assertEquals(0l, size.invoke(metadataCache));
        updateStat.invoke(metadataCache, "/a", a.build());
        assertEquals(1l, size.invoke(metadataCache));
        updateStat.invoke(metadataCache, "/b", b.build());
        assertEquals(2l, size.invoke(metadataCache));
        updateStat.invoke(metadataCache, "/c", c.build());
        // metadatacache has only room for two entries.
        assertEquals(2l, size.invoke(metadataCache));

        invalidate.invoke(metadataCache, "/b");
        assertEquals(1l, size.invoke(metadataCache));
        invalidate.invoke(metadataCache, "/c");
        assertEquals(0l, size.invoke(metadataCache));

    }

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

        updateDirEntries.invoke(metadataCache, dir, dirEntriesBuilder.build());

        // Read all dir entries.
        DirectoryEntries dirEntriesRead = (DirectoryEntries) getDirEntries.invoke(metadataCache, dir, 0,
                entryCount);

        assertEquals(entryCount, dirEntriesRead.getEntriesCount());
        for (int i = 0; i < dirEntriesRead.getEntriesCount(); i++) {
            String pathToStat = dir + i;
            assertEquals(i, dirEntriesRead.getEntries(i).getStbuf().getIno());
            assertEquals(pathToStat, dirEntriesRead.getEntries(i).getName());
        }

        // Read a subset.
        int offset = entryCount / 2;

        dirEntriesRead = (DirectoryEntries) getDirEntries.invoke(metadataCache, dir, offset,
                entryCount / 2 - 1);
        assertEquals(entryCount / 2 - 1, dirEntriesRead.getEntriesCount());

        for (int i = 0; i < dirEntriesRead.getEntriesCount(); i++) {
            String pathToStat = dir + (offset + i);
            assertEquals(pathToStat, dirEntriesRead.getEntries(i).getName());
            assertEquals(offset + i, dirEntriesRead.getEntries(i).getStbuf().getIno());
        }
    }

    /**
     * If a Stat entry gets updated through UpdateStat(), the new timeout must be respected in case of an
     * eviction.
     */
    public void testInvalidatePrefix() throws Exception {

        // create new metadataCache with 1024 entries.
        metadataCache = constructor.newInstance(1024, 3600);

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

        updateStat.invoke(metadataCache, dir, a.build());
        updateStat.invoke(metadataCache, dir + "/file1", b.build());
        updateStat.invoke(metadataCache, dir + ".file1", c.build());
        updateStat.invoke(metadataCache, dir + "Zfile1", d.build());

        invalidatePrefix.invoke(metadataCache, dir);

        // invalidation of all matching entries successful?
        assertNull(getStat.invoke(metadataCache, dir));
        assertNull(getStat.invoke(metadataCache, dir + "/file1"));

        // Similiar entries which do not match the prefix "/dir/" have not been
        // invalidated.

        Stat statC = (Stat) getStat.invoke(metadataCache, dir + ".file1");
        assertNotNull(statC);
        assertEquals(2, statC.getIno());
        Stat statD = (Stat) getStat.invoke(metadataCache, dir + "Zfile1");
        assertNotNull(statD);
        assertEquals(3, statD.getIno());

    }

    /**
     * If a Stat entry gets updated through UpdateStat(), the new timeout must be respected in case of an
     * eviction.
     */
    public void testRenamePrefix() throws Exception {

        // create new metadataCache with 1024 entries.
        metadataCache = constructor.newInstance(1024, 3600);

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

        updateStat.invoke(metadataCache, dir, a.build());
        updateStat.invoke(metadataCache, dir + "/file1", b.build());
        updateStat.invoke(metadataCache, dir + ".file1", c.build());
        updateStat.invoke(metadataCache, dir + "Zfile1", d.build());
        assertEquals(4l, size.invoke(metadataCache));

        renamePrefix.invoke(metadataCache, dir, "/newDir");
        assertEquals(4l, size.invoke(metadataCache));

        // Renaming of all matching entries was successful?
        Stat statA = (Stat) getStat.invoke(metadataCache, "/newDir");
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        Stat statB = (Stat) getStat.invoke(metadataCache, "/newDir" + "/file1");
        assertNotNull(statB);
        assertEquals(1, statB.getIno());

        // Similiar entries which do not match the prefix "/dir/" hat not been renamed
        Stat statC = (Stat) getStat.invoke(metadataCache, dir + ".file1");
        assertNotNull(statC);
        assertEquals(2, statC.getIno());
        Stat statD = (Stat) getStat.invoke(metadataCache, dir + "Zfile1");
        assertNotNull(statD);
        assertEquals(3, statD.getIno());
    }

    /**
     * Are large nanoseconds values correctly updated by UpdateStatAttributes?
     */
    public void testUpdateStatAttributes() throws Exception {

        // create new metadataCache with 1024 entries.
        metadataCache = constructor.newInstance(1024, 3600);

        String path = "/file";
        Stat.Builder stat = getIntializedStatBuilder();
        Stat.Builder newStat = getIntializedStatBuilder();
        stat.setIno(0);
        newStat.setIno(1);

        updateStat.invoke(metadataCache, path, stat.build());
        assertEquals(1l, size.invoke(metadataCache));
        Stat statA = (Stat) getStat.invoke(metadataCache, path);
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        assertEquals(0, statA.getMtimeNs());

        long time = 1234567890;
        time *= 1000000000;
        newStat.setAtimeNs(time);
        newStat.setMtimeNs(time);

        updateStatAttributes.invoke(metadataCache, path, newStat.build(), Setattrs.SETATTR_ATIME.getNumber()
                | Setattrs.SETATTR_MTIME.getNumber());
        assertEquals(1l, size.invoke(metadataCache));
        Stat statB = (Stat) getStat.invoke(metadataCache, path);
        assertNotNull(statB);
        assertEquals(0, statB.getIno());
        assertEquals(time, statB.getAtimeNs());
        assertEquals(time, statB.getMtimeNs());
    }

    /**
     * Changing the file access mode may only modify the last 12 bits (3 bits for sticky bit, set GID and set
     * UID and 3 * 3 bits for the file access mode).
     */
    public void testUpdateStatAttributesPreservesModeBits() throws Exception {

        String path = "/file";
        Stat.Builder stat = getIntializedStatBuilder();
        Stat.Builder cachedStat = getIntializedStatBuilder();

        stat.setIno(0);
        stat.setMode(33188); // Octal: 100644 ( regular file + 644).

        updateStat.invoke(metadataCache, path, stat.build());
        assertEquals(1l, size.invoke(metadataCache));
        Stat statA = (Stat) getStat.invoke(metadataCache, path);
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        assertEquals(33188, statA.getMode());

        stat = getIntializedStatBuilder();
        stat.setMode(420); // Octal: 644
        updateStatAttributes.invoke(metadataCache, path, stat.build(), Setattrs.SETATTR_MODE.getNumber());
        assertEquals(1l, size.invoke(metadataCache));
        statA = (Stat) getStat.invoke(metadataCache, path);
        assertNotNull(statA);
        assertEquals(0, cachedStat.getIno());
        assertEquals(33188, statA.getMode());

        stat = getIntializedStatBuilder();
        stat.setMode(263076); // Octal : 1001644 (regular file + sticky bit + 644).
        updateStat.invoke(metadataCache, path, stat.build());
        assertEquals(1l, size.invoke(metadataCache));
        statA = (Stat) getStat.invoke(metadataCache, path);
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        assertEquals(263076, statA.getMode());

        stat = getIntializedStatBuilder();
        stat.setMode(511); // Octal: 0777 (no sticky bit + 777).
        updateStatAttributes.invoke(metadataCache, path, stat.build(), Setattrs.SETATTR_MODE.getNumber());
        assertEquals(1l, size.invoke(metadataCache));
        statA = (Stat) getStat.invoke(metadataCache, path);
        assertNotNull(statA);
        assertEquals(0, statA.getIno());
        assertEquals(262655, statA.getMode()); // Octal: 1000777
    }

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

}
