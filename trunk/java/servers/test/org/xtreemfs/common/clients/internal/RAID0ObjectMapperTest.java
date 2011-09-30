/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients.internal;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xtreemfs.common.clients.internal.ObjectMapper.ObjectRequest;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.test.SetupUtils;

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

        StripingPolicy sp = SetupUtils.getStripingPolicy(3, 7);
        List<String> s = new ArrayList(3);
        s.add("1");
        s.add("2");
        s.add("3");
        org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica tmpR =
                org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica.newBuilder().
        addAllOsdUuids(s).setStripingPolicy(sp).setReplicationFlags(0).build();

        Replica r = new Replica(tmpR,null);
        

        RAID0ObjectMapper instance = new RAID0ObjectMapper(sp);

        List<ObjectRequest> result = instance.readRequest(7*1024, 0, r);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(0, result.get(0).getOffset());
        assertEquals(7*1024, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        result = instance.readRequest(5*1024, 2*1024, r);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(2*1024, result.get(0).getOffset());
        assertEquals(5*1024, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        result = instance.readRequest(8*1024, 0, r);
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(0, result.get(0).getOffset());
        assertEquals(7*1024, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        assertEquals(1, result.get(1).getObjNo());
        assertEquals(0, result.get(1).getOffset());
        assertEquals(1*1024, result.get(1).getLength());
        assertEquals("2", result.get(1).getOsdUUID().toString());

        result = instance.readRequest(14*1024, 0, r);
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(0, result.get(0).getOffset());
        assertEquals(7*1024, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        assertEquals(1, result.get(1).getObjNo());
        assertEquals(0, result.get(1).getOffset());
        assertEquals(7*1024, result.get(1).getLength());
        assertEquals("2", result.get(1).getOsdUUID().toString());

        result = instance.readRequest(14*1024, 2*1024, r);
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(2*1024, result.get(0).getOffset());
        assertEquals(5*1024, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());

        assertEquals(1, result.get(1).getObjNo());
        assertEquals(0, result.get(1).getOffset());
        assertEquals(7*1024, result.get(1).getLength());
        assertEquals("2", result.get(1).getOsdUUID().toString());

        assertEquals(2, result.get(2).getObjNo());
        assertEquals(0, result.get(2).getOffset());
        assertEquals(2*1024, result.get(2).getLength());
        assertEquals("3", result.get(2).getOsdUUID().toString());

    }


    /**
     * Test of readRequest method, of class RAID0ObjectMapper.
     */
    @Test
    public void testWriteRequest() {
        System.out.println("writeRequest");

        StripingPolicy sp = SetupUtils.getStripingPolicy(3, 7);
        List<String> s = new ArrayList(3);
        s.add("1");
        s.add("2");
        s.add("3");
        org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica tmpR =
                org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica.newBuilder().
        addAllOsdUuids(s).setStripingPolicy(sp).setReplicationFlags(0).build();

        Replica r = new Replica(tmpR,null);

        RAID0ObjectMapper instance = new RAID0ObjectMapper(sp);

        byte[] bytes = new byte[14*1024];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = 'a';
        ReusableBuffer data = ReusableBuffer.wrap(bytes);
        data.position(0);

        List<ObjectRequest> result = instance.writeRequest(data, 2*1024, r);
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).getObjNo());
        assertEquals(2*1024, result.get(0).getOffset());
        assertEquals(5*1024, result.get(0).getLength());
        assertEquals("1", result.get(0).getOsdUUID().toString());
        assertEquals(5*1024, result.get(0).getData().capacity());

        assertEquals(1, result.get(1).getObjNo());
        assertEquals(0, result.get(1).getOffset());
        assertEquals(7*1024, result.get(1).getLength());
        assertEquals("2", result.get(1).getOsdUUID().toString());
        assertEquals(7*1024, result.get(1).getData().capacity());
        assertEquals(7*1024, result.get(1).getData().remaining());

        assertEquals(2, result.get(2).getObjNo());
        assertEquals(0, result.get(2).getOffset());
        assertEquals(2*1024, result.get(2).getLength());
        assertEquals("3", result.get(2).getOsdUUID().toString());
        assertEquals(2*1024, result.get(2).getData().capacity());
        

    }

}