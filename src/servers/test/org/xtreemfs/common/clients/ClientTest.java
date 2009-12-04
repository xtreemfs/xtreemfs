/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */


package org.xtreemfs.common.clients;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class ClientTest {

    private TestEnvironment       testEnv;

    private static final String  VOLUME_NAME = "testvol";

    public ClientTest() {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName());
        
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
            TestEnvironment.Services.MRC_CLIENT, TestEnvironment.Services.TIME_SYNC,
            TestEnvironment.Services.UUID_RESOLVER, TestEnvironment.Services.DIR_SERVICE,
            TestEnvironment.Services.MRC, TestEnvironment.Services.OSD});
        testEnv.start();

        List<String> groupIDs = new ArrayList(1);
        groupIDs.add("test");
        UserCredentials uc = MRCClient.getCredentials("test", groupIDs);

        RPCResponse r = testEnv.getMrcClient().mkvol(testEnv.getMRCAddress(), uc, VOLUME_NAME,
            new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 64, 1),
            AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL.intValue(), 0777);
        r.get();
        r.freeBuffers();
    }

    @After
    public void tearDown() {
        testEnv.shutdown();
    }

    @Test
    public void testMDOps() throws Exception {

        final Client c = new Client(new InetSocketAddress[]{testEnv.getDirClient().getDefaultServerAddress()}, 15000, 300000, null);
        c.start();

        Volume v = c.getVolume(VOLUME_NAME);

        long fspace = v.getFreeSpace();
        long uspace = v.getUsedSpace();
        System.out.println("free/used: "+fspace+"/"+uspace);

        File dir = v.getFile("dir");
        dir.mkdir();

        String[] entries = v.list("/");
        assertEquals(1,entries.length);
        assertEquals("dir",entries[0]);

        entries = v.list("/dir/");
        assertEquals(0,entries.length);

        assertTrue(dir.isDirectory());
        assertFalse(dir.isFile());
        assertTrue(dir.exists());

        File file = v.getFile("/dir/file");
        file.createFile();
        assertFalse(file.isDirectory());
        assertTrue(file.isFile());
        assertTrue(file.exists());

        File file2 = v.getFile("/file2");
        file.renameTo(file2);

        assertFalse(file.exists());
        assertTrue(file2.exists());
        
        entries = v.list("/");
        assertEquals(2,entries.length);

        file2.delete();
        

        c.stop();

    }

    @Test
    public void testData() throws Exception {

        final Client c = new Client(new InetSocketAddress[]{testEnv.getDirClient().getDefaultServerAddress()}, 15000, 300000, null);
        c.start();

        Volume v = c.getVolume(VOLUME_NAME);

        File f = v.getFile("/test");

        RandomAccessFile ra = f.open("rw");

        byte[] data = new byte[2048];
        ra.write(data, 0, data.length);

        ra.seek(0);
        ra.read(data, 0, data.length);

        ra.close();


        c.stop();

    }
    

}