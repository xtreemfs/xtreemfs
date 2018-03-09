/*
 * Copyright (c) 2010-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.osd.storage.VersionTable;
import org.xtreemfs.TestHelper;

/**
 * 
 * @author stender
 */
public class VersionTableTest {
    @Rule
    public final TestRule    testLog = TestHelper.testLog;

    public static final File VT_FILE = new File("/tmp/vttest");

    @Before
    public void setUp() throws Exception {
        VT_FILE.delete();
    }

    @After
    public void tearDown() throws Exception {
        VT_FILE.delete();
    }

    @Test
    public void testInsertLookup() throws Exception {

        Map<Long, int[]> map = new HashMap<Long, int[]>();
        map.put(10000L, new int[] { 2, 4, 5, 1, 2 });
        map.put(10005L, new int[] { 3, 4, 5, 2, 3 });
        map.put(10020L, new int[] { 5, 4, 6, 3, 3, 1 });
        map.put(11000L, new int[] { 5, 5, 8, 3, 3, 1, 1, 1 });
        map.put(12000L, new int[] { 5, 5 });
        map.put(32000L, new int[] {});

        VersionTable vt = new VersionTable(VT_FILE);
        for (Entry<Long, int[]> entry : map.entrySet())
            vt.addVersion(entry.getKey(), entry.getValue(), entry.getValue().length * 1024);

        for (int i = 0; i < 10; i++) {
            assertEquals(i < 5 ? map.get(10000L)[i] : 0, vt.getLatestVersionBefore(10001).getObjVersion(i));
            assertEquals(i < 5 ? map.get(10000L)[i] : 0, vt.getLatestVersionBefore(10005).getObjVersion(i));
            assertEquals(i < 5 ? map.get(10005L)[i] : 0, vt.getLatestVersionBefore(10012).getObjVersion(i));
            assertEquals(i < 5 ? map.get(10005L)[i] : 0, vt.getLatestVersionBefore(10014).getObjVersion(i));
            assertEquals(i < 6 ? map.get(10020L)[i] : 0, vt.getLatestVersionBefore(10200).getObjVersion(i));
            assertEquals(i < 6 ? map.get(10020L)[i] : 0, vt.getLatestVersionBefore(10800).getObjVersion(i));
            assertEquals(i < 8 ? map.get(11000L)[i] : 0, vt.getLatestVersionBefore(11001).getObjVersion(i));
            assertEquals(i < 8 ? map.get(11000L)[i] : 0, vt.getLatestVersionBefore(12000).getObjVersion(i));
            assertEquals(i < 2 ? map.get(12000L)[i] : 0, vt.getLatestVersionBefore(12010).getObjVersion(i));
            assertEquals(i < 2 ? map.get(12000L)[i] : 0, vt.getLatestVersionBefore(22000).getObjVersion(i));
            assertEquals(0, vt.getLatestVersionBefore(33000).getObjVersion(i));
            assertEquals(0, vt.getLatestVersionBefore(Long.MAX_VALUE).getObjVersion(i));
        }

    }

    @Test
    public void testLoadSave() throws Exception {

        Map<Long, int[]> map = new HashMap<Long, int[]>();
        map.put(10000L, new int[] { 2, 4, 5, 1, 2 });
        map.put(10005L, new int[] { 3, 4, 5, 2, 3 });
        map.put(10020L, new int[] { 5, 4, 6, 3, 3, 1 });
        map.put(11000L, new int[] { 5, 5, 8, 3, 3, 1, 1, 1 });
        map.put(12000L, new int[] { 5, 5 });
        map.put(32000L, new int[] {});

        VersionTable vt = new VersionTable(VT_FILE);
        for (Entry<Long, int[]> entry : map.entrySet())
            vt.addVersion(entry.getKey(), entry.getValue(), entry.getValue().length * 1024);

        vt.save();
        vt.load();

        for (int i = 0; i < 10; i++) {
            assertEquals(i < 5 ? map.get(10000L)[i] : 0, vt.getLatestVersionBefore(10001).getObjVersion(i));
            assertEquals(i < 5 ? map.get(10000L)[i] : 0, vt.getLatestVersionBefore(10005).getObjVersion(i));
            assertEquals(i < 5 ? map.get(10005L)[i] : 0, vt.getLatestVersionBefore(10012).getObjVersion(i));
            assertEquals(i < 5 ? map.get(10005L)[i] : 0, vt.getLatestVersionBefore(10014).getObjVersion(i));
            assertEquals(i < 6 ? map.get(10020L)[i] : 0, vt.getLatestVersionBefore(10200).getObjVersion(i));
            assertEquals(i < 6 ? map.get(10020L)[i] : 0, vt.getLatestVersionBefore(10800).getObjVersion(i));
            assertEquals(i < 8 ? map.get(11000L)[i] : 0, vt.getLatestVersionBefore(11001).getObjVersion(i));
            assertEquals(i < 8 ? map.get(11000L)[i] : 0, vt.getLatestVersionBefore(12000).getObjVersion(i));
            assertEquals(i < 2 ? map.get(12000L)[i] : 0, vt.getLatestVersionBefore(12010).getObjVersion(i));
            assertEquals(i < 2 ? map.get(12000L)[i] : 0, vt.getLatestVersionBefore(22000).getObjVersion(i));
            assertEquals(0, vt.getLatestVersionBefore(33000).getObjVersion(i));
            assertEquals(0, vt.getLatestVersionBefore(Long.MAX_VALUE).getObjVersion(i));
        }

    }

}
