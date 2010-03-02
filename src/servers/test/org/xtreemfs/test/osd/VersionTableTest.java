/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin,
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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.test.osd;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.osd.storage.VersionTable;

/**
 * 
 * @author stender
 */
public class VersionTableTest extends TestCase {
    
    public static final File VT_FILE = new File("/tmp/vttest");
    
    public VersionTableTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        VT_FILE.delete();
    }
    
    @Override
    protected void tearDown() throws Exception {
        VT_FILE.delete();
    }
    
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
    
    public static void main(String[] args) {
        TestRunner.run(VersionTableTest.class);
    }
    
}
