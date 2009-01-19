/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin,
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
 * AUTHORS: Jan Stender (ZIB), Jesús Malo (BSC), Björn Kolbeck (ZIB),
 *          Eugenio Cesario (CNR)
 */

package org.xtreemfs.test.osd;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.SimpleStorageLayout;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.test.SetupUtils;

/**
 *
 * @author bjko
 */
public class StorageLayoutTest extends TestCase {

    final OSDConfig config;

    public StorageLayoutTest(String testName) throws IOException {
        super(testName);
        Logging.start(SetupUtils.DEBUG_LEVEL);
        config = SetupUtils.createOSD1Config();
    }

    protected void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        FSUtils.delTree(new File(config.getObjDir()));
    }

    protected void tearDown() throws Exception {
    }

    public void testSimpleStorageLayoutBasics() throws Exception {
        SimpleStorageLayout layout = new SimpleStorageLayout(config, new MetadataCache());
        basicTests(layout);
    }

    public void testHashStorageLayoutBasics() throws Exception {
        HashStorageLayout layout = new HashStorageLayout(config, new MetadataCache());
        basicTests(layout);
    }

    public void testSimpleStorageLayoutAdvanced() throws Exception {
        SimpleStorageLayout layout = new SimpleStorageLayout(config, new MetadataCache());
        advancedTests(layout);
    }

    public void testHashStorageLayoutAdvanced() throws Exception {
        HashStorageLayout layout = new HashStorageLayout(config, new MetadataCache());
        advancedTests(layout);
    }

    /**
     * @param layout
     * @throws IOException
     */
    private void basicTests(StorageLayout layout) throws IOException {
        final String fileId = "ABCDEFG:0001";
	StripingPolicy sp = new RAID0(64, 1);

        assertFalse(layout.fileExists(fileId));

        ReusableBuffer data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }

        layout.writeObject(fileId, 0l, data, 1, 0, null, sp, 0l);
        BufferPool.free(data);

        data = layout.readObject(fileId, 0l, 1, null, sp, 0l);
        assertEquals(64, data.capacity());
        for (int i = 0; i < 64; i++) {
            assertEquals((byte) (48 + i), data.get());
        }
        BufferPool.free(data);

        data = layout.readObject(fileId, 1l, 1, null, sp, 0l);
        assertEquals(0, data.capacity());
        BufferPool.free(data);
    }

    /**
     * 
     * @param layout
     * @throws IOException
     */
    private void advancedTests(StorageLayout layout) throws IOException {
        final String fileId = "ABCDEFG:0001";
	StripingPolicy sp = new RAID0(64, 1);

        assertFalse(layout.fileExists(fileId));

        ReusableBuffer data = BufferPool.allocate(64);
        for (int i = 0; i < 64; i++) {
            data.put((byte) (48 + i));
        }

        layout.writeObject(fileId, 0l, data, 1, 0, null, sp, 0l);
        BufferPool.free(data);

        data = layout.readObjectNotPOSIX(fileId, 0l, 1, null, sp, 0l);
        assertEquals(64, data.capacity());
        for (int i = 0; i < 64; i++) {
            assertEquals((byte) (48 + i), data.get());
        }
        BufferPool.free(data);

        data = layout.readObjectNotPOSIX(fileId, 1l, 1, null, sp, 0l);
        assertNull(data);
        BufferPool.free(data);
    }

    public static void main(String[] args) {
        TestRunner.run(StorageLayoutTest.class);
    }

}
