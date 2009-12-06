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

package org.xtreemfs.common.clients.internal;

import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.internal.ObjectMapper.ObjectRequest;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;

/**
 *
 * @author bjko
 */
public class RAID0ObjectMapperTest {

    public RAID0ObjectMapperTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of readRequest method, of class RAID0ObjectMapper.
     */
    @Test
    public void testReadRequest() {
        System.out.println("readRequest");

        StripingPolicy sp = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 7, 3);
        StringSet s = new StringSet();
        s.add("1");
        s.add("2");
        s.add("3");
        org.xtreemfs.interfaces.Replica tmpR = new org.xtreemfs.interfaces.Replica();
        tmpR.setOsd_uuids(s);
        tmpR.setStriping_policy(sp);

        Replica r = new Replica(tmpR,null);
        

        RAID0ObjectMapper instance = new RAID0ObjectMapper(sp);

        List<ObjectRequest> result = instance.readRequest(7, 0, r);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(0, result.get(0).getOffset());
        assertEquals(7, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        result = instance.readRequest(5, 2, r);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(2, result.get(0).getOffset());
        assertEquals(5, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        result = instance.readRequest(8, 0, r);
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(0, result.get(0).getOffset());
        assertEquals(7, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        assertEquals(1, result.get(1).getObjNo());
        assertEquals(0, result.get(1).getOffset());
        assertEquals(1, result.get(1).getLength());
        assertEquals("2", result.get(1).getOsdUUID().toString());

        result = instance.readRequest(14, 0, r);
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(0, result.get(0).getOffset());
        assertEquals(7, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        assertEquals(1, result.get(1).getObjNo());
        assertEquals(0, result.get(1).getOffset());
        assertEquals(7, result.get(1).getLength());
        assertEquals("2", result.get(1).getOsdUUID().toString());

        result = instance.readRequest(14, 2, r);
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(2, result.get(0).getOffset());
        assertEquals(5, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        assertEquals(1, result.get(1).getObjNo());
        assertEquals(0, result.get(1).getOffset());
        assertEquals(7, result.get(1).getLength());
        assertEquals("2", result.get(1).getOsdUUID().toString());

        assertEquals(2, result.get(2).getObjNo());
        assertEquals(0, result.get(2).getOffset());
        assertEquals(2, result.get(2).getLength());
        assertEquals("3", result.get(2).getOsdUUID().toString());

    }


    /**
     * Test of readRequest method, of class RAID0ObjectMapper.
     */
    @Test
    public void testWriteRequest() {
        System.out.println("writeRequest");

        StripingPolicy sp = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 7, 3);
        StringSet s = new StringSet();
        s.add("1");
        s.add("2");
        s.add("3");
        org.xtreemfs.interfaces.Replica tmpR = new org.xtreemfs.interfaces.Replica();
        tmpR.setOsd_uuids(s);
        tmpR.setStriping_policy(sp);

        Replica r = new Replica(tmpR,null);

        RAID0ObjectMapper instance = new RAID0ObjectMapper(sp);

        byte[] bytes = new byte[14];
        for (int i = 0; i < 14; i++)
            bytes[i] = 'a';
        ReusableBuffer data = ReusableBuffer.wrap(bytes);
        data.position(0);

        List<ObjectRequest> result = instance.writeRequest(data, 2, r);
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(2, result.get(0).getOffset());
        assertEquals(5, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());
        assertEquals(5, result.get(0).getData().capacity());

        assertEquals(1, result.get(1).getObjNo());
        assertEquals(0, result.get(1).getOffset());
        assertEquals(7, result.get(1).getLength());
        assertEquals("2", result.get(1).getOsdUUID().toString());
        assertEquals(7, result.get(1).getData().capacity());
        assertEquals(7, result.get(1).getData().remaining());

        assertEquals(2, result.get(2).getObjNo());
        assertEquals(0, result.get(2).getOffset());
        assertEquals(2, result.get(2).getLength());
        assertEquals("3", result.get(2).getOsdUUID().toString());
        assertEquals(2, result.get(2).getData().capacity());
        

    }

}