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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.mrc.brain.metadata.ACL;
import org.xtreemfs.mrc.brain.metadata.BufferBackedACL;
import org.xtreemfs.mrc.brain.metadata.BufferBackedDirObject;
import org.xtreemfs.mrc.brain.metadata.BufferBackedFileObject;
import org.xtreemfs.mrc.brain.metadata.BufferBackedStripingPolicy;
import org.xtreemfs.mrc.brain.metadata.BufferBackedXAttrs;
import org.xtreemfs.mrc.brain.metadata.BufferBackedXLoc;
import org.xtreemfs.mrc.brain.metadata.BufferBackedXLocList;
import org.xtreemfs.mrc.brain.metadata.FSObject;
import org.xtreemfs.mrc.brain.metadata.FileObject;
import org.xtreemfs.mrc.brain.metadata.StripingPolicy;
import org.xtreemfs.mrc.brain.metadata.XAttrs;
import org.xtreemfs.mrc.brain.metadata.XLoc;
import org.xtreemfs.mrc.brain.metadata.XLocList;
import org.xtreemfs.test.SetupUtils;

public class BufferBackedMetadataTest extends TestCase {
    
    public BufferBackedMetadataTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }
    
    protected void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
    }
    
    protected void tearDown() throws Exception {
    }
    
    public void testBufferBackedACL() throws Exception {
        
        {
            final List<String> entities = generateStrList("me", "someone", "him", "us");
            final List<Integer> rights = generateIntList(32, 0, 222, 4873872);
            
            // create ACL
            BufferBackedACL acl1 = new BufferBackedACL(toArray(entities), toArray(rights));
            checkACL(entities, rights, acl1);
            
            // copy ACL
            BufferBackedACL acl2 = new BufferBackedACL(acl1.getBuffer(), true, true);
            checkACL(entities, rights, acl2);
            
            acl2.deleteEntry("someone");
            entities.remove(1);
            rights.remove(1);
            checkACL(entities, rights, acl2);
            
            acl2.editEntry("them", 433);
            entities.add("them");
            rights.add(433);
            checkACL(entities, rights, acl2);
            
            acl2.editEntry("me", 111);
            rights.remove(0);
            rights.add(0, 111);
            checkACL(entities, rights, acl2);
            
            // test iterator
            Iterator<ACL.Entry> it = acl2.iterator();
            while (it.hasNext())
                it.next();
            
            acl1.destroy();
            acl2.destroy();
        }
        
        {
            final List<String> entities = generateStrList("this");
            final List<Integer> rights = generateIntList(Integer.MAX_VALUE);
            
            // create ACL
            BufferBackedACL acl1 = new BufferBackedACL(toArray(entities), toArray(rights));
            checkACL(entities, rights, acl1);
            
            // copy ACL
            BufferBackedACL acl2 = new BufferBackedACL(acl1.getBuffer(), true, true);
            checkACL(entities, rights, acl2);
            
            acl2.deleteEntry("this");
            entities.remove(0);
            rights.remove(0);
            checkACL(entities, rights, acl2);
            
            acl1.destroy();
            acl2.destroy();
        }
        
        {
            final List<String> entities = generateStrList();
            final List<Integer> rights = generateIntList();
            
            // create ACL
            BufferBackedACL acl1 = new BufferBackedACL(toArray(entities), toArray(rights));
            checkACL(entities, rights, acl1);
            
            // copy ACL
            BufferBackedACL acl2 = new BufferBackedACL(acl1.getBuffer(), true, true);
            checkACL(entities, rights, acl2);
            
            acl2.editEntry("blubberbla", Integer.MAX_VALUE);
            acl2.editEntry("blubberbla", Integer.MAX_VALUE);
            acl2.editEntry("blubberbla", Integer.MAX_VALUE);
            entities.add("blubberbla");
            rights.add(Integer.MAX_VALUE);
            checkACL(entities, rights, acl2);
            
            acl1.destroy();
            acl2.destroy();
        }
    }
    
    public void testBufferBackedStripingPolicy() throws Exception {
        
        {
            String pattern = "RAID0";
            int stripeSize = 256;
            int width = 5;
            
            // create striping policy
            BufferBackedStripingPolicy sp1 = new BufferBackedStripingPolicy(pattern, stripeSize,
                width);
            checkSP(pattern, stripeSize, width, sp1);
            
            // copy striping policy
            BufferBackedStripingPolicy sp2 = new BufferBackedStripingPolicy(sp1.getBuffer(), true,
                true);
            checkSP(pattern, stripeSize, width, sp2);
            
            pattern = "AAAAAAAA";
            stripeSize = 432;
            width = 43333;
            sp2.setPattern(pattern);
            sp2.setStripeSize(stripeSize);
            sp2.setWidth(width);
            checkSP(pattern, stripeSize, width, sp2);
            
            sp1.destroy();
            sp2.destroy();
        }
        
        {
            final String pattern = "RAID0";
            final int stripeSize = 16;
            final int width = 1;
            
            // create striping policy
            BufferBackedStripingPolicy sp1 = new BufferBackedStripingPolicy(pattern, stripeSize,
                width);
            checkSP(pattern, stripeSize, width, sp1);
            
            // copy striping policy
            BufferBackedStripingPolicy sp2 = new BufferBackedStripingPolicy(sp1.getBuffer(), true,
                true);
            checkSP(pattern, stripeSize, width, sp2);
            
            sp1.destroy();
            sp2.destroy();
        }
    }
    
    public void testBufferBackedXAttrs() throws Exception {
        
        {
            final List<String> keys = generateStrList("someAttr", "anotherAttr", "attr");
            final List<String> values = generateStrList("someValue", "anotherValue", "attrValue");
            final List<String> uids = generateStrList("myUID", "me", "");
            
            // create XAttrs
            BufferBackedXAttrs xattrs1 = new BufferBackedXAttrs(toArray(keys), toArray(values),
                toArray(uids));
            checkXAttrs(keys, values, uids, xattrs1);
            
            // copy XAttrs
            BufferBackedXAttrs xattrs2 = new BufferBackedXAttrs(xattrs1.getBuffer(), true, true);
            checkXAttrs(keys, values, uids, xattrs2);
            
            xattrs1.destroy();
            xattrs2.destroy();
        }
        {
            final List<String> keys = generateStrList("k1", "k2", "k3", "k4", "k5", "k6", "k7",
                "k8");
            final List<String> values = generateStrList("v1", "v2", "v3", "v4", "v5", "v6", "v7",
                "v8");
            final List<String> uids = generateStrList("me1", "me2", "me3", "me4", "me5", "me6",
                "me7", "me8");
            
            // create XAttrs
            BufferBackedXAttrs xattrs1 = new BufferBackedXAttrs(toArray(keys), toArray(values),
                toArray(uids));
            checkXAttrs(keys, values, uids, xattrs1);
            
            // clone XAttrs
            BufferBackedXAttrs xattrs2 = new BufferBackedXAttrs(xattrs1.getBuffer(), true, true);
            checkXAttrs(keys, values, uids, xattrs2);
            
            // delete entry
            xattrs2.deleteEntry("k4", "me4");
            keys.remove(3);
            values.remove(3);
            uids.remove(3);
            checkXAttrs(keys, values, uids, xattrs2);
            
            // add new entry
            xattrs2.editEntry("new", "val", "someone");
            keys.add("new");
            values.add("val");
            uids.add("someone");
            checkXAttrs(keys, values, uids, xattrs2);
            
            // add trailing entry w/ existing key and different uid
            xattrs2.editEntry("k7", "bla", "me4");
            keys.add("k7");
            values.add("bla");
            uids.add("me4");
            checkXAttrs(keys, values, uids, xattrs2);
            
            // replace trailing entry
            xattrs2.editEntry("k7", "blub", "me4");
            values.remove("bla");
            values.add("blub");
            checkXAttrs(keys, values, uids, xattrs2);
            
            // replace inner entry
            xattrs2.editEntry("k5", "8282828", "me5");
            values.remove(3);
            values.add(3, "8282828");
            checkXAttrs(keys, values, uids, xattrs2);
            
            // test iterator
            Iterator<XAttrs.Entry> it = xattrs2.iterator();
            while (it.hasNext())
                it.next();
            
            xattrs1.destroy();
            xattrs2.destroy();
        }
        
    }
    
    public void testBufferBackedXLoc() throws Exception {
        
        {
            final String[] osds = { "someOSD", "anotherOSD", "myOSD" };
            final BufferBackedStripingPolicy sp = new BufferBackedStripingPolicy("RAID0", 1024, 4);
            
            // create XLoc
            BufferBackedXLoc xloc1 = new BufferBackedXLoc(sp, osds);
            checkXLoc(osds, sp, xloc1);
            
            // copy XLoc
            BufferBackedXLoc xloc2 = new BufferBackedXLoc(xloc1.getBuffer(), true, true);
            checkXLoc(osds, sp, xloc2);
            
            xloc1.destroy();
            xloc2.destroy();
            sp.destroy();
        }
        
    }
    
    public void testBufferBackedXLocList() throws Exception {
        
        {
            final List<BufferBackedStripingPolicy> sp = generateSPList(
                new BufferBackedStripingPolicy("RAID0", 5, 1), new BufferBackedStripingPolicy(
                    "RAID5", 99, 33), new BufferBackedStripingPolicy("asfd", 34, -1));
            
            final List<BufferBackedXLoc> replicas = generateXLocList(new BufferBackedXLoc(
                sp.get(0), new String[] { "dasfk", "asfd", "afastfads4" }), new BufferBackedXLoc(sp
                    .get(1), new String[] { "fdsay", "34", "4" }), new BufferBackedXLoc(sp.get(2),
                new String[] { "354", ",mn", "asdf" }));
            int version = 37;
            
            final BufferBackedXLoc newRepl = new BufferBackedXLoc(sp.get(1), new String[] {
                "324432", "kkakslfdllslfldslfd", "4554" });
            
            // create XLocList
            BufferBackedXLocList xlocList1 = new BufferBackedXLocList(toArray(replicas), version);
            checkXLocList(replicas, version, xlocList1);
            
            // copy XLocList
            BufferBackedXLocList xlocList2 = new BufferBackedXLocList(xlocList1.getBuffer(), true,
                true);
            checkXLocList(replicas, version, xlocList2);
            
            // add a replica
            xlocList2.addReplica(newRepl, true);
            replicas.add(newRepl);
            checkXLocList(replicas, ++version, xlocList2);
            
            // delete a replica
            xlocList2.removeReplica(2, false);
            replicas.remove(2).destroy();
            checkXLocList(replicas, version, xlocList2);
            
            // delete last replica
            int last = xlocList2.getReplicaCount() - 1;
            xlocList2.removeReplica(last, false);
            replicas.remove(last).destroy();
            checkXLocList(replicas, version, xlocList2);
            
            // test iterator
            Iterator<XLoc> it = xlocList2.iterator();
            while (it.hasNext())
                it.next();
            
            xlocList1.destroy();
            xlocList2.destroy();
            
            for (BufferBackedStripingPolicy spol : sp)
                spol.destroy();
            
            for (BufferBackedXLoc xloc : replicas)
                xloc.destroy();
        }
        
    }
    
    public void testBufferBackedDirObject() throws Exception {
        
        {
            final long id = 99999;
            final int atime = 999;
            final int ctime = 888;
            final int mtime = 777;
            final String owner = "someone";
            final String group = "somegroup";
            final String linkTarget = "linkTarget";
            final BufferBackedACL acl = null;
            final BufferBackedStripingPolicy defaultSP = null;
            final BufferBackedXAttrs xattrs = null;
            
            // create dir object
            BufferBackedDirObject dirObj1 = new BufferBackedDirObject(id, atime, ctime, mtime,
                owner, group, acl, defaultSP, linkTarget, xattrs);
            checkDirObject(id, atime, ctime, mtime, owner, group, linkTarget, acl, defaultSP,
                xattrs, dirObj1);
            
            // copy dir object
            BufferBackedDirObject dirObj2 = new BufferBackedDirObject(dirObj1.getBuffer(), true,
                true);
            checkDirObject(id, atime, ctime, mtime, owner, group, linkTarget, acl, defaultSP,
                xattrs, dirObj2);
            
            dirObj1.destroy();
            dirObj2.destroy();
        }
        
        {
            long id = 34223;
            int atime = 12;
            int ctime = 11;
            int mtime = 0;
            String owner = "me";
            String group = "mygroup";
            String linkTarget = null;
            final BufferBackedACL acl = new BufferBackedACL(new String[] { "me" }, new int[] { 23 });
            final BufferBackedACL acl2 = new BufferBackedACL(new String[] { "someone",
                "someoneelse" }, new int[] { 77, 32 });
            final BufferBackedStripingPolicy defaultSP = new BufferBackedStripingPolicy("RAID0",
                32, 99999);
            final BufferBackedStripingPolicy defaultSP2 = new BufferBackedStripingPolicy("344334",
                298, 12);
            final BufferBackedXAttrs xattrs = new BufferBackedXAttrs(new String[] { "attr1",
                "attr2" }, new String[] { "value1", "" }, new String[] { "me", "" });
            final BufferBackedXAttrs xattrs2 = new BufferBackedXAttrs(new String[] { "gds", },
                new String[] { "rwe432" }, new String[] { "us" });
            
            // create dir object
            BufferBackedDirObject dirObj1 = new BufferBackedDirObject(id, atime, ctime, mtime,
                owner, group, acl, defaultSP, linkTarget, xattrs);
            checkDirObject(id, atime, ctime, mtime, owner, group, linkTarget, acl, defaultSP,
                xattrs, dirObj1);
            
            // copy dir object
            BufferBackedDirObject dirObj2 = new BufferBackedDirObject(dirObj1.getBuffer(), true,
                true);
            checkDirObject(id, atime, ctime, mtime, owner, group, linkTarget, acl, defaultSP,
                xattrs, dirObj2);
            
            BufferBackedDirObject dirObj3 = new BufferBackedDirObject(dirObj2.getId(), dirObj2
                    .getAtime(), dirObj2.getCtime(), dirObj2.getMtime(), dirObj2.getOwnerId()
                    .toString(), dirObj2.getOwningGroupId().toString(), dirObj2.getAcl(), dirObj2
                    .getStripingPolicy(), null, dirObj2.getXAttrs());
            checkDirObject(id, atime, ctime, mtime, owner, group, null, acl, defaultSP, xattrs,
                dirObj3);
            
            id = 77;
            atime = 111;
            ctime = 111;
            mtime = 111;
            owner = "blub";
            group = "blubberbla";
            linkTarget = "somewhere";
            
            dirObj3.setId(id);
            dirObj3.setAtime(atime);
            dirObj3.setCtime(ctime);
            dirObj3.setMtime(mtime);
            dirObj3.setOwnerId(owner);
            dirObj3.setOwningGroupId(group);
            dirObj3.setLinkTarget(linkTarget);
            dirObj3.setACL(acl2);
            dirObj3.setStripingPolicy(defaultSP2);
            dirObj3.setXAttrs(xattrs2);
            checkDirObject(id, atime, ctime, mtime, owner, group, linkTarget, acl2, defaultSP2,
                xattrs2, dirObj3);
            
            dirObj1.destroy();
            dirObj2.destroy();
            dirObj3.destroy();
            
            xattrs.destroy();
            xattrs2.destroy();
            defaultSP.destroy();
            defaultSP2.destroy();
            acl.destroy();
            acl2.destroy();
        }
    }
    
    public void testBufferBackedFileObject() throws Exception {
        
        {
            final long id = 122111;
            final int atime = 43;
            final int ctime = Integer.MAX_VALUE;
            final int mtime = 0;
            final long size = 3298438;
            final short linkcount = 3;
            final int epoch = 4;
            final int issuedEpoch = 5;
            final boolean readonly = false;
            final String owner = "vyxcvcxy";
            final String group = "afdsafdsafds";
            final String linkTarget = "linkTarget";
            final BufferBackedACL acl = null;
            final BufferBackedXLocList xlocList = null;
            final BufferBackedStripingPolicy sp = null;
            final BufferBackedXAttrs xattrs = null;
            
            // create file object
            BufferBackedFileObject fileObj1 = new BufferBackedFileObject(id, atime, ctime, mtime,
                size, linkcount, epoch, issuedEpoch, readonly, owner, group, acl, xlocList, sp,
                linkTarget, xattrs);
            checkFileObject(id, atime, ctime, mtime, size, linkcount, epoch, issuedEpoch, readonly,
                owner, group, linkTarget, acl, xlocList, sp, xattrs, fileObj1);
            
            // copy file object
            BufferBackedFileObject fileObj2 = new BufferBackedFileObject(fileObj1.getBuffer(),
                true, true);
            checkFileObject(id, atime, ctime, mtime, size, linkcount, epoch, issuedEpoch, readonly,
                owner, group, linkTarget, acl, xlocList, sp, xattrs, fileObj2);
            
            fileObj1.destroy();
            fileObj2.destroy();
        }
        
        {
            long id = 43;
            int atime = 421;
            int ctime = 4343;
            int mtime = 2;
            long size = Long.MAX_VALUE;
            short linkcount = 1;
            int epoch = 0;
            int issuedEpoch = 0;
            boolean readonly = false;
            String owner = "fdsa";
            String group = "54";
            String linkTarget = null;
            
            final BufferBackedACL acl = new BufferBackedACL(new String[] { "me" }, new int[] { 23 });
            final BufferBackedACL acl2 = new BufferBackedACL(new String[] { "342fwa" },
                new int[] { 2435 });
            final BufferBackedStripingPolicy sp = new BufferBackedStripingPolicy("RAID0", 32, 99999);
            final BufferBackedStripingPolicy sp2 = new BufferBackedStripingPolicy("543gfsa", 111,
                95472);
            final BufferBackedXLoc[] xloc = new BufferBackedXLoc[] {
                new BufferBackedXLoc(sp, new String[] { "fasd", "fasd",
                    "http://faksfljdasdkjfjkads.dfd" }),
                new BufferBackedXLoc(sp, new String[] { "fasd", "fasd", "http://7413121.com" }) };
            final BufferBackedXLoc[] xloc2 = new BufferBackedXLoc[0];
            final BufferBackedXLocList xlocList = new BufferBackedXLocList(xloc, 43);
            final BufferBackedXLocList xlocList2 = new BufferBackedXLocList(xloc2, 1);
            final BufferBackedXAttrs xattrs = new BufferBackedXAttrs(new String[] { "attr1",
                "attr2" }, new String[] { "value1", "" }, new String[] { "me", "" });
            final BufferBackedXAttrs xattrs2 = new BufferBackedXAttrs(new String[0], new String[0],
                new String[0]);
            
            // create file object
            BufferBackedFileObject fileObj1 = new BufferBackedFileObject(id, atime, ctime, mtime,
                size, linkcount, epoch, issuedEpoch, readonly, owner, group, acl, xlocList, sp,
                linkTarget, xattrs);
            checkFileObject(id, atime, ctime, mtime, size, linkcount, epoch, issuedEpoch, readonly,
                owner, group, linkTarget, acl, xlocList, sp, xattrs, fileObj1);
            
            // copy file object
            BufferBackedFileObject fileObj2 = new BufferBackedFileObject(fileObj1.getBuffer(),
                true, true);
            checkFileObject(id, atime, ctime, mtime, size, linkcount, epoch, issuedEpoch, readonly,
                owner, group, linkTarget, acl, xlocList, sp, xattrs, fileObj2);
            
            BufferBackedFileObject fileObj3 = new BufferBackedFileObject(fileObj2.getId(), fileObj2
                    .getAtime(), fileObj2.getCtime(), fileObj2.getMtime(), fileObj2.getSize(),
                fileObj2.getLinkCount(), fileObj2.getEpoch(), fileObj2.getIssuedEpoch(), fileObj2
                        .isReadOnly(), fileObj2.getOwnerId().toString(), fileObj2
                        .getOwningGroupId().toString(), fileObj2.getAcl(), fileObj2.getXLocList(),
                fileObj2.getStripingPolicy(), null, fileObj2.getXAttrs());
            checkFileObject(id, atime, ctime, mtime, size, linkcount, epoch, issuedEpoch, readonly,
                owner, group, linkTarget, acl, xlocList, sp, xattrs, fileObj3);
            
            id = 8488484;
            atime = 422342343;
            ctime = 452156;
            mtime = 44;
            size = 73299;
            linkcount = 32;
            epoch = 5;
            issuedEpoch = 5;
            readonly = true;
            owner = "fasdfsad";
            group = "gdgfd";
            linkTarget = "43eagaasfdfdasdfg";
            
            fileObj3.setId(id);
            fileObj3.setAtime(atime);
            fileObj3.setCtime(ctime);
            fileObj3.setMtime(mtime);
            fileObj3.setSize(size);
            fileObj3.setLinkCount(linkcount);
            fileObj3.setEpoch(epoch);
            fileObj3.setIssuedEpoch(issuedEpoch);
            fileObj3.setReadOnly(readonly);
            fileObj3.setOwnerId(owner);
            fileObj3.setOwningGroupId(group);
            fileObj3.setLinkTarget(linkTarget);
            fileObj3.setACL(acl2);
            fileObj3.setStripingPolicy(sp2);
            fileObj3.setXAttrs(xattrs2);
            fileObj3.setXLocList(xlocList2);
            checkFileObject(id, atime, ctime, mtime, size, linkcount, epoch, issuedEpoch, readonly,
                owner, group, linkTarget, acl2, xlocList2, sp2, xattrs2, fileObj3);
            
            fileObj1.destroy();
            fileObj2.destroy();
            fileObj3.destroy();
            
            xattrs.destroy();
            xattrs2.destroy();
            sp.destroy();
            sp2.destroy();
            acl.destroy();
            acl2.destroy();
            for (XLoc loc : xloc)
                loc.destroy();
            xlocList.destroy();
            for (XLoc loc : xloc2)
                loc.destroy();
            xlocList2.destroy();
        }
    }
    
    private void checkACL(List<String> entities, List<Integer> rights, ACL acl) {
        
        ACL.Entry[] entries = new ACL.Entry[entities.size()];
        for (int i = 0; i < entities.size(); i++)
            entries[i] = new BufferBackedACL.Entry(entities.get(i), rights.get(i));
        
        assertEquals(entities.size(), acl.getEntryCount());
        for (ACL.Entry entry : entries)
            assertEquals(entry.getRights(), acl.getRights(entry.getEntity()).intValue());
    }
    
    private void checkSP(String pattern, int stripeSize, int width, BufferBackedStripingPolicy sp) {
        assertEquals(pattern, sp.getPattern().toString());
        assertEquals(width, sp.getWidth());
        assertEquals(stripeSize, sp.getStripeSize());
    }
    
    private void checkXAttrs(List<String> keys, List<String> values, List<String> uids,
        BufferBackedXAttrs xattrs) {
        
        XAttrs.Entry[] entries = new XAttrs.Entry[keys.size()];
        for (int i = 0; i < keys.size(); i++)
            entries[i] = new BufferBackedXAttrs.Entry(keys.get(i), uids.get(i), values.get(i));
        
        assertEquals(entries.length, xattrs.getEntryCount());
        for (XAttrs.Entry entry : entries)
            assertEquals(entry.getValue(), xattrs.getValue(entry.getKey(), entry.getUID())
                    .toString());
    }
    
    private void checkXLoc(String[] osds, StripingPolicy sp, BufferBackedXLoc xloc) {
        
        final StripingPolicy xlocSP = xloc.getStripingPolicy();
        
        assertEquals(sp.toString(), xlocSP.toString());
        assertEquals(sp.getPattern(), xlocSP.getPattern());
        assertEquals(sp.getWidth(), xlocSP.getWidth());
        assertEquals(sp.getStripeSize(), xlocSP.getStripeSize());
        
        assertEquals(osds.length, xloc.getOSDCount());
        for (int i = 0; i < osds.length; i++)
            assertEquals(osds[i], xloc.getOSD(i).toString());
    }
    
    private void checkXLocList(List<BufferBackedXLoc> replicas, int version,
        BufferBackedXLocList xlocList) {
        
        assertEquals(version, xlocList.getVersion());
        assertEquals(replicas.size(), xlocList.getReplicaCount());
        
        for (int i = 0; i < replicas.size(); i++)
            assertEquals(replicas.get(i).toString(), xlocList.getReplica(i).toString());
    }
    
    private void checkDirObject(long id, int atime, int ctime, int mtime, String owner,
        String group, String linkTarget, ACL acl, BufferBackedStripingPolicy defaultSP,
        BufferBackedXAttrs xattrs, FSObject obj) {
        
        assertEquals(id, obj.getId());
        assertEquals(atime, obj.getAtime());
        assertEquals(ctime, obj.getCtime());
        assertEquals(mtime, obj.getMtime());
        assertEquals(owner, obj.getOwnerId().toString());
        assertEquals(group, obj.getOwningGroupId().toString());
        
        if (linkTarget == null)
            assertNull(obj.getLinkTarget());
        else
            assertEquals(linkTarget, obj.getLinkTarget().toString());
        
        // check the ACL
        ACL objACL = obj.getAcl();
        if (acl == null)
            assertNull(objACL);
        else
            assertEquals(acl.toString(), objACL.toString());
        
        // check the default striping policy
        StripingPolicy objSP = obj.getStripingPolicy();
        if (defaultSP == null)
            assertNull(objSP);
        else
            assertEquals(defaultSP.toString(), objSP.toString());
        
        // check the XAttrs list
        XAttrs objxattrs = obj.getXAttrs();
        if (xattrs == null)
            assertNull(objxattrs);
        else
            assertEquals(xattrs.toString(), objxattrs.toString());
        
    }
    
    private void checkFileObject(long id, int atime, int ctime, int mtime, long size,
        short linkcount, int epoch, int issuedEpoch, boolean readonly, String owner, String group,
        String linkTarget, ACL acl, XLocList xlocList, BufferBackedStripingPolicy sp,
        BufferBackedXAttrs xattrs, FileObject obj) {
        
        assertEquals(id, obj.getId());
        assertEquals(atime, obj.getAtime());
        assertEquals(ctime, obj.getCtime());
        assertEquals(mtime, obj.getMtime());
        assertEquals(size, obj.getSize());
        assertEquals(linkcount, obj.getLinkCount());
        assertEquals(epoch, obj.getEpoch());
        assertEquals(issuedEpoch, obj.getIssuedEpoch());
        assertEquals(readonly, obj.isReadOnly());
        assertEquals(owner, obj.getOwnerId().toString());
        assertEquals(group, obj.getOwningGroupId().toString());
        
        if (linkTarget == null)
            assertNull(obj.getLinkTarget());
        else
            assertEquals(linkTarget, obj.getLinkTarget().toString());
        
        // check the ACL
        ACL objACL = obj.getAcl();
        if (acl == null)
            assertNull(objACL);
        else
            assertEquals(acl.toString(), objACL.toString());
        
        // check the XLocList
        XLocList xlocObj = obj.getXLocList();
        if (xlocObj == null)
            assertNull(xlocObj);
        else
            assertEquals(xlocList.toString(), xlocObj.toString());
        
        // check the striping policy
        StripingPolicy objSP = obj.getStripingPolicy();
        if (sp == null)
            assertNull(objSP);
        else
            assertEquals(sp.toString(), objSP.toString());
        
        // check the XAttrs list
        XAttrs objxattrs = obj.getXAttrs();
        if (xattrs == null)
            assertNull(objxattrs);
        else
            assertEquals(xattrs.toString(), objxattrs.toString());
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(BufferBackedMetadataTest.class);
    }
    
    private List<String> generateStrList(String... arr) {
        List<String> list = new ArrayList<String>(arr.length);
        for (String s : arr)
            list.add(s);
        
        return list;
    }
    
    private List<Integer> generateIntList(int... arr) {
        List<Integer> list = new ArrayList<Integer>(arr.length);
        for (int s : arr)
            list.add(s);
        
        return list;
    }
    
    public List<BufferBackedXLoc> generateXLocList(BufferBackedXLoc... arr) {
        List<BufferBackedXLoc> list = new ArrayList<BufferBackedXLoc>(arr.length);
        for (BufferBackedXLoc x : arr)
            list.add(x);
        
        return list;
    }
    
    public List<BufferBackedStripingPolicy> generateSPList(BufferBackedStripingPolicy... arr) {
        List<BufferBackedStripingPolicy> list = new ArrayList<BufferBackedStripingPolicy>(
            arr.length);
        for (BufferBackedStripingPolicy s : arr)
            list.add(s);
        
        return list;
    }
    
    private String[] toArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }
    
    private BufferBackedXLoc[] toArray(List<BufferBackedXLoc> list) {
        return list.toArray(new BufferBackedXLoc[list.size()]);
    }
    
    private int[] toArray(List<Integer> list) {
        int[] ints = new int[list.size()];
        for (int i = 0; i < ints.length; i++)
            ints[i] = list.get(i);
        
        return ints;
    }
    
}
