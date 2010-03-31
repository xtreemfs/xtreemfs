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
 * AUTHORS: Nele Andersen (ZIB)
 */
package org.xtreemfs.test.io;

import java.io.IOException;

import junit.framework.TestCase;

import org.xtreemfs.common.clients.io.ByteMapper;
import org.xtreemfs.common.clients.io.ByteMapperFactory;
import org.xtreemfs.common.clients.io.ObjectStore;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.StripingPolicyType;

public class ByteMapperTest extends TestCase{
    
    public void setUp() {

        System.out.println("TEST: " + getClass().getSimpleName() + "."
                + getName());
    }

    public void tearDown(){
    }

    public void testRead() throws Exception{

        ByteMapper byteMapperRAID0 = ByteMapperFactory.createByteMapper(StripingPolicyType.STRIPING_POLICY_RAID0.intValue(), 2, new TestObjectStore());
        int offset = 0;
        int bytesToRead = 6;
        byte[] resultBuffer = new byte[bytesToRead];
        assertEquals(byteMapperRAID0.read(resultBuffer, offset, bytesToRead,0), 6);
        
        bytesToRead = 2;
        resultBuffer = new byte[bytesToRead];
        assertEquals(byteMapperRAID0.read(resultBuffer, offset, bytesToRead,0), 2);
        
        bytesToRead = 1;
        resultBuffer = new byte[bytesToRead];
        assertEquals(byteMapperRAID0.read(resultBuffer, offset, bytesToRead,0), 1);
        
        offset = 2;
        bytesToRead = 6;
        resultBuffer = new byte[bytesToRead+2];
        assertEquals(byteMapperRAID0.read(resultBuffer, offset, bytesToRead,0), 6);

        resultBuffer = new byte[bytesToRead -1];
        try{
            byteMapperRAID0.read(resultBuffer, offset, bytesToRead,0);
            fail("the resultBuffer is to small");
        }catch(Exception e){}
        
        byteMapperRAID0 = ByteMapperFactory.createByteMapper(StripingPolicyType.STRIPING_POLICY_RAID0.intValue(), 2, new EmptyObjectStore());
        bytesToRead = 1;
        offset = 0;
        resultBuffer = new byte[bytesToRead];
        assertEquals(byteMapperRAID0.read(resultBuffer, offset, bytesToRead,0), 0);

    }
    
    public void testWrite() throws Exception{
        ByteMapper byteMapperRAID0 = ByteMapperFactory.createByteMapper(StripingPolicyType.STRIPING_POLICY_RAID0.intValue(), 2, new TestObjectStore());
        byte[] writeFromBuffer = "Hello World".getBytes();
        int offset = 0;
        int bytesToWrite = 6;
        assertEquals(byteMapperRAID0.write(writeFromBuffer, offset, bytesToWrite,0), 6);
        bytesToWrite = 11;
        assertEquals(byteMapperRAID0.write(writeFromBuffer, offset, bytesToWrite,0),11);
        bytesToWrite = 12;
        try{
            byteMapperRAID0.write(writeFromBuffer, offset, bytesToWrite,0);
            fail("bytesToWrite > length of writeFromBuffer");
        }catch(Exception e){}
    }
    
    class TestObjectStore implements ObjectStore{
        public ReusableBuffer readObject(long objectNo, int offset, int length){
            String content = "Hallo World";
            return ReusableBuffer.wrap(content.substring((int) offset, (int) (offset+length)).getBytes());
        }
        
        public void writeObject(long objectNo, long offset, ReusableBuffer buffer) throws IOException
        {
            
        }
    }
    
    class EmptyObjectStore implements ObjectStore{
        public ReusableBuffer readObject(long objectNo, int offset, int length){
            return ReusableBuffer.wrap("".getBytes());
        }
        
        public void writeObject(long objectNo, long offset, ReusableBuffer buffer) throws IOException,
        InterruptedException {
            
        }
    }
    
    
    
}
