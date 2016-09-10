/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.mrc;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.metadata.BufferBackedACLEntry;
import org.xtreemfs.mrc.metadata.BufferBackedFileMetadata;
import org.xtreemfs.mrc.metadata.BufferBackedStripingPolicy;
import org.xtreemfs.mrc.metadata.BufferBackedXAttr;
import org.xtreemfs.mrc.metadata.BufferBackedXLoc;
import org.xtreemfs.mrc.metadata.BufferBackedXLocList;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestHelper;

public class BufferBackedMetadataTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBufferBackedACLEntry() throws Exception {

        {
            final long fileId = 3322;
            final String entity = "test";
            final short rights = 509;

            // create ACL entry
            BufferBackedACLEntry acl1 = new BufferBackedACLEntry(fileId, entity, rights);
            checkACLEntry(entity, rights, acl1);

            // copy ACL entry
            BufferBackedACLEntry acl2 = new BufferBackedACLEntry(acl1.getKeyBuf(), acl1.getValBuf());
            checkACLEntry(entity, rights, acl2);
        }

        {
            final long fileId = 0;
            final String entity = "";
            final short rights = Short.MAX_VALUE;

            // create ACL entry
            BufferBackedACLEntry acl1 = new BufferBackedACLEntry(fileId, entity, rights);
            checkACLEntry(entity, rights, acl1);

            // copy ACL entry
            BufferBackedACLEntry acl2 = new BufferBackedACLEntry(acl1.getKeyBuf(), acl1.getValBuf());
            checkACLEntry(entity, rights, acl2);
        }
    }

    @Test
    public void testBufferBackedStripingPolicy() throws Exception {

        {
            final String pattern = "RAID0";
            final int stripeSize = 256;
            final int width = 5;

            // create striping policy
            BufferBackedStripingPolicy sp1 = new BufferBackedStripingPolicy(pattern, stripeSize, width, 0, 0);
            checkSP(pattern, stripeSize, width, 0, 0, sp1);

            // copy striping policy
            BufferBackedStripingPolicy sp2 = new BufferBackedStripingPolicy(sp1.getBuffer());
            checkSP(pattern, stripeSize, width, 0, 0, sp2);
        }

        {
            final String pattern = "RAID0";
            final int stripeSize = 16;
            final int width = 1;

            // create striping policy
            BufferBackedStripingPolicy sp1 = new BufferBackedStripingPolicy(pattern, stripeSize, width, 0, 0);
            checkSP(pattern, stripeSize, width, 0, 0, sp1);

            // copy striping policy
            BufferBackedStripingPolicy sp2 = new BufferBackedStripingPolicy(sp1.getBuffer());
            checkSP(pattern, stripeSize, width, 0, 0, sp2);
        }


        {
            final String pattern = "EC";
            final int stripeSize = 128;
            final int width = 3;
            final int parity = 2;
            final int ec_quorum = 5;

            // create striping policy
            BufferBackedStripingPolicy sp1 = new BufferBackedStripingPolicy(pattern, stripeSize, width, parity, ec_quorum);
            checkSP(pattern, stripeSize, width, parity, ec_quorum, sp1);

            // copy striping policy
            BufferBackedStripingPolicy sp2 = new BufferBackedStripingPolicy(sp1.getBuffer());
            checkSP(pattern, stripeSize, width, parity, ec_quorum, sp2);
        }
    }

    @Test
    public void testBufferBackedXAttrs() throws Exception {

        {
            final long fileId = 4389;
            final String key = "someAttr";
            final byte[] val = "fadsjkkj".getBytes();
            final String uid = "myUID";

            // create XAttrs
            BufferBackedXAttr xattr1 = new BufferBackedXAttr(fileId, uid, key, val, (short) 0);
            checkXAttr(key, val, uid, xattr1);

            // copy XAttrs
            BufferBackedXAttr xattr2 = new BufferBackedXAttr(xattr1.getKeyBuf(), xattr1.getValBuf());
            checkXAttr(key, val, uid, xattr2);
        }

        {
            final long fileId = 32;
            final String key = "fasd";
            final byte[] val = "".getBytes();
            final String uid = "gffg";

            // create XAttrs
            BufferBackedXAttr xattr1 = new BufferBackedXAttr(fileId, uid, key, val, (short) 0);
            checkXAttr(key, val, uid, xattr1);

            // copy XAttrs
            BufferBackedXAttr xattr2 = new BufferBackedXAttr(xattr1.getKeyBuf(), xattr1.getValBuf());
            checkXAttr(key, val, uid, xattr2);
        }

        {
            final long fileId = 11;
            final String key = "";
            final byte[] val = "".getBytes();
            final String uid = "";

            // create XAttrs
            BufferBackedXAttr xattr1 = new BufferBackedXAttr(fileId, uid, key, val, (short) 0);
            checkXAttr(key, val, uid, xattr1);

            // copy XAttrs
            BufferBackedXAttr xattr2 = new BufferBackedXAttr(xattr1.getKeyBuf(), xattr1.getValBuf());
            checkXAttr(key, val, uid, xattr2);
        }

    }

    @Test
    public void testBufferBackedXLoc() throws Exception {

        {
            final String[] osds = { "someOSD", "anotherOSD", "myOSD" };
            final BufferBackedStripingPolicy sp = new BufferBackedStripingPolicy("RAID0", 1024, 4);
            final int replFlags = 237;

            // create XLoc
            BufferBackedXLoc xloc1 = new BufferBackedXLoc(sp, osds, replFlags);
            checkXLoc(osds, sp, replFlags, xloc1);

            byte[] tmpBuf = new byte[xloc1.getBuffer().length + 10];
            System.arraycopy(xloc1.getBuffer(), 0, tmpBuf, 3, xloc1.getBuffer().length);

            // copy XLoc
            BufferBackedXLoc xloc2 = new BufferBackedXLoc(tmpBuf, 3, xloc1.getBuffer().length);
            checkXLoc(osds, sp, replFlags, xloc2);

            final int newReplFlags = Integer.MIN_VALUE;
            xloc2.setReplicationFlags(newReplFlags);
            assertEquals(Integer.MIN_VALUE, xloc2.getReplicationFlags());
        }

        {
            final String[] osds = { "dataOSD0", "dataOSD1", "dataOSD2", "parityOSD0", "parityOSD1" };
            final BufferBackedStripingPolicy sp = new BufferBackedStripingPolicy("EC", 1024, 3, 2, 5);
            final int replFlags = 0;

            // create XLoc
            BufferBackedXLoc xloc1 = new BufferBackedXLoc(sp, osds, replFlags);
            checkXLoc(osds, sp, replFlags, xloc1);

            byte[] tmpBuf = new byte[xloc1.getBuffer().length + 10];
            System.arraycopy(xloc1.getBuffer(), 0, tmpBuf, 3, xloc1.getBuffer().length);

            // copy XLoc
            BufferBackedXLoc xloc2 = new BufferBackedXLoc(tmpBuf, 3, xloc1.getBuffer().length);
            checkXLoc(osds, sp, replFlags, xloc2);
        }

    }

    @Test
    public void testBufferBackedXLocList() throws Exception {

        {
            final List<BufferBackedStripingPolicy> sp = generateSPList(new BufferBackedStripingPolicy("RAID0", 5, 1),
                    new BufferBackedStripingPolicy("RAID5", 99, 33), new BufferBackedStripingPolicy("asfd", 34, -1));

            final List<BufferBackedXLoc> replicas = generateXLocList(new BufferBackedXLoc(sp.get(0), new String[] {
                    "11111", "22222", "33333" }, 43), new BufferBackedXLoc(sp.get(1),
                    new String[] { "fdsay", "34", "4" }, 99), new BufferBackedXLoc(sp.get(2), new String[] { "354",
                    ",mn", "asdf" }, 45));
            int version = 37;
            String updatePolicy = "bla";

            // create XLocList
            BufferBackedXLocList xlocList1 = new BufferBackedXLocList(toArray(replicas), updatePolicy, version);
            checkXLocList(replicas, version, updatePolicy, xlocList1);

            // copy XLocList
            BufferBackedXLocList xlocList2 = new BufferBackedXLocList(xlocList1.getBuffer(), 0,
                    xlocList1.getBuffer().length);
            checkXLocList(replicas, version, updatePolicy, xlocList2);

            // test iterator
            Iterator<XLoc> it = xlocList2.iterator();
            while (it.hasNext())
                it.next();
        }

        {
            final List<BufferBackedStripingPolicy> sp = generateSPList(new BufferBackedStripingPolicy("EC", 5, 3, 2, 5));

            final List<BufferBackedXLoc> replicas = generateXLocList(
                    new BufferBackedXLoc(sp.get(0), new String[] { "11111", "22222", "33333", "44444", "55555" }, 0));
            int version = 37;
            String updatePolicy = "EC";

            // create XLocList
            BufferBackedXLocList xlocList1 = new BufferBackedXLocList(toArray(replicas), updatePolicy, version);
            checkXLocList(replicas, version, updatePolicy, xlocList1);

            // copy XLocList
            BufferBackedXLocList xlocList2 = new BufferBackedXLocList(xlocList1.getBuffer(), 0,
                    xlocList1.getBuffer().length);
            checkXLocList(replicas, version, updatePolicy, xlocList2);

            // test iterator
            Iterator<XLoc> it = xlocList2.iterator();
            while (it.hasNext())
                it.next();
        }

    }

    @Test
    public void testBufferBackedDirObject() throws Exception {

        {
            long parentId = 99999;
            String dirName = "bla";
            long fileId = 434873;
            int atime = 999;
            int ctime = 888;
            int mtime = 777;
            short perms = 255;
            long w32Attrs = 12;
            String owner = "someone";
            String group = "somegroup";

            // create dir object
            BufferBackedFileMetadata dirObj = new BufferBackedFileMetadata(parentId, dirName, owner, group, fileId,
                    atime, ctime, mtime, perms, w32Attrs, (short) 1);
            checkDirObject(owner, group, fileId, atime, ctime, mtime, perms, w32Attrs, dirObj);

            fileId = 77;
            atime = 111;
            ctime = 111;
            mtime = 111;
            perms = 277;
            w32Attrs = 3232;

            dirObj.setAtime(atime);
            dirObj.setCtime(ctime);
            dirObj.setMtime(mtime);
            dirObj.setId(fileId);
            dirObj.setPerms(perms);
            dirObj.setW32Attrs(w32Attrs);
            checkDirObject(owner, group, fileId, atime, ctime, mtime, perms, w32Attrs, dirObj);
        }

    }

    @Test
    public void testBufferBackedFileObject() throws Exception {

        {
            long parentId = 4343;
            String fileName = "asfd";
            long fileId = 122111;
            int atime = 43;
            int ctime = Integer.MAX_VALUE;
            int mtime = 0;
            long size = 3298438;
            short perms = 322;
            long w32Attrs = Integer.MAX_VALUE;
            short linkcount = 3;
            int epoch = 4;
            int issuedEpoch = 5;
            boolean readonly = false;
            String owner = "vyxcvcxy";
            String group = "afdsafdsafds";
            // create file object
            BufferBackedFileMetadata fileObj = new BufferBackedFileMetadata(parentId, fileName, owner, group, fileId,
                    atime, ctime, mtime, size, perms, w32Attrs, linkcount, epoch, issuedEpoch, readonly);
            checkFileObject(owner, group, fileId, atime, ctime, mtime, perms, w32Attrs, size, linkcount, epoch,
                    issuedEpoch, readonly, fileObj);
        }

    }

    private void checkACLEntry(String entity, short rights, BufferBackedACLEntry entry) {
        assertEquals(entity, entry.getEntity());
        assertEquals(rights, entry.getRights());
    }

    private void checkSP(String pattern, int stripeSize, int width, int parity, int ec_quorum, BufferBackedStripingPolicy sp) {
        assertEquals(pattern, sp.getPattern().toString());
        assertEquals(width, sp.getWidth());
        assertEquals(parity, sp.getParityWidth());
        assertEquals(ec_quorum, sp.getECWriteQuorum());
        assertEquals(stripeSize, sp.getStripeSize());
    }

    private void checkXAttr(String key, byte[] val, String owner, BufferBackedXAttr xattr) {

        assertEquals(key, xattr.getKey());

        assertEquals(val.length, xattr.getValue().length);
        for (int i = 0; i < val.length; i++)
            assertEquals(val[i], xattr.getValue()[i]);

        assertEquals(owner, xattr.getOwner());
    }

    private void checkXLoc(String[] osds, StripingPolicy sp, int flags, BufferBackedXLoc xloc) {

        final StripingPolicy xlocSP = xloc.getStripingPolicy();

        assertEquals(sp.toString(), xlocSP.toString());
        assertEquals(sp.getPattern(), xlocSP.getPattern());
        assertEquals(sp.getWidth(), xlocSP.getWidth());
        assertEquals(sp.getParityWidth(), xlocSP.getParityWidth());
        assertEquals(sp.getStripeSize(), xlocSP.getStripeSize());

        assertEquals(osds.length, xloc.getOSDCount());
        for (int i = 0; i < osds.length; i++)
            assertEquals(osds[i], xloc.getOSD(i).toString());

        assertEquals(flags, xloc.getReplicationFlags());
    }

    private void checkXLocList(List<BufferBackedXLoc> replicas, int version, String updatePolicy,
            BufferBackedXLocList xlocList) {

        assertEquals(version, xlocList.getVersion());
        assertEquals(updatePolicy, xlocList.getReplUpdatePolicy());
        assertEquals(replicas.size(), xlocList.getReplicaCount());

        for (int i = 0; i < replicas.size(); i++)
            assertEquals(replicas.get(i).toString(), xlocList.getReplica(i).toString());
    }

    private void checkDirObject(String owner, String group, long fileId, int atime, int ctime, int mtime, short perms,
            long w32Attrs, FileMetadata obj) {

        assertEquals(fileId, obj.getId());
        assertEquals(atime, obj.getAtime());
        assertEquals(ctime, obj.getCtime());
        assertEquals(mtime, obj.getMtime());
        assertEquals(perms, obj.getPerms());
        assertEquals(w32Attrs, obj.getW32Attrs());
        assertEquals(owner, obj.getOwnerId().toString());
        assertEquals(group, obj.getOwningGroupId().toString());

    }

    private void checkFileObject(String owner, String group, long fileId, int atime, int ctime, int mtime, short perms,
            long w32Attrs, long size, short linkcount, int epoch, int issuedEpoch, boolean readonly, FileMetadata obj) {

        assertEquals(fileId, obj.getId());
        assertEquals(atime, obj.getAtime());
        assertEquals(ctime, obj.getCtime());
        assertEquals(mtime, obj.getMtime());
        assertEquals(perms, obj.getPerms());
        assertEquals(w32Attrs, obj.getW32Attrs());
        assertEquals(size, obj.getSize());
        assertEquals(linkcount, obj.getLinkCount());
        assertEquals(epoch, obj.getEpoch());
        assertEquals(issuedEpoch, obj.getIssuedEpoch());
        assertEquals(readonly, obj.isReadOnly());
        assertEquals(owner, obj.getOwnerId().toString());
        assertEquals(group, obj.getOwningGroupId().toString());
    }

    public List<BufferBackedXLoc> generateXLocList(BufferBackedXLoc... arr) {
        List<BufferBackedXLoc> list = new ArrayList<BufferBackedXLoc>(arr.length);
        for (BufferBackedXLoc x : arr)
            list.add(x);

        return list;
    }

    public List<BufferBackedStripingPolicy> generateSPList(BufferBackedStripingPolicy... arr) {
        List<BufferBackedStripingPolicy> list = new ArrayList<BufferBackedStripingPolicy>(arr.length);
        for (BufferBackedStripingPolicy s : arr)
            list.add(s);

        return list;
    }

    private BufferBackedXLoc[] toArray(List<BufferBackedXLoc> list) {
        return list.toArray(new BufferBackedXLoc[list.size()]);
    }

}
