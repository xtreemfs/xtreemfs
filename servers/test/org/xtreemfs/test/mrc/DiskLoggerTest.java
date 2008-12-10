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

import junit.framework.*;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.mrc.brain.storage.DiskLogger;
import org.xtreemfs.mrc.brain.storage.LogEntry;
import org.xtreemfs.mrc.brain.storage.SliceID;
import org.xtreemfs.mrc.brain.storage.SyncListener;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author bjko
 */
public class DiskLoggerTest extends TestCase {

    public static boolean success;

    private DiskLogger dl;

    public DiskLoggerTest(String testName) {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    protected void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

        dl = new DiskLogger("/tmp/testlog.mrc",false);
        dl.start();
    }

    protected void tearDown() throws Exception {
        dl.shutdown();
    }

    public void testSliceID() throws Exception {

        SliceID si1 = new SliceID(1);
        SliceID si2 = new SliceID(si1,1);

        assertEquals(si1.toString().length(),SliceID.SIZE_IN_BYTES*2);

        assertEquals(si1,si2);

        String srs1 = si1.toString();
        SliceID si3 = new SliceID(srs1);

        assertEquals(si1,si3);

        ReusableBuffer buf = BufferPool.allocate(SliceID.SIZE_IN_BYTES);
        si1.write(buf);
        buf.position(0);
        si3 = new SliceID(buf);
        assertEquals(si1,si3);
        BufferPool.free(buf);
    }

    public void testMarshalling() throws Exception {

        LogEntry e = new LogEntry(0xFF11, 0xFFAA7700, new SliceID(1),
                (byte) 1, "test", "", "",
                ReusableBuffer.wrap(new byte[]{1,3,2}), null);

        ReusableBuffer me = e.marshall();

        LogEntry cmp = new LogEntry(me);

        assertTrue(e.equals(cmp));
        cmp.payload.position(0);
        assertTrue(cmp.payload.hasRemaining());
        assertEquals(cmp.payload.get(),(byte)1);
        assertEquals(cmp.payload.get(),(byte)3);
        assertEquals(cmp.payload.get(),(byte)2);

        BufferPool.free(me);
    }

    public void testLog() throws Exception {

        LogEntry e = new LogEntry(0xFF11, 0xFFAA7700, new SliceID(1),
                (byte) 1, "test", "", "",
                ReusableBuffer.wrap(new byte[]{1,3,2}), null);

        success = false;
        e.registerListener(new SyncListener() {
            public void failed(LogEntry entry, Exception ex) {
                fail("Sync failed:"+ex);
            }
            public void synced(LogEntry entry) {
                success = true;
            }
        }
        );
        dl.append(e);
        synchronized (this) {
            this.wait(500);
        }
        assertTrue(success);
    }

}
