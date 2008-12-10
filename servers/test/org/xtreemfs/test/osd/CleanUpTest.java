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
 * AUTHOR: Felix Langner (ZIB)
 */

package org.xtreemfs.test.osd;


import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.clients.osd.ConcurrentFileMap;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.utils.cleanup_osd;

/**
 * 
 * @author langner
 *
 */

public class CleanUpTest extends TestCase{

    private static String testVolume = "testVolume";
    
    private RequestController                   dir;
    private org.xtreemfs.mrc.RequestController  mrc;
    private MRCClient                           mrcClient;
    private OSD                                 osd;
    private String                              authString;
    private HashStorageLayout                   layout;  
    private ReusableBuffer                      data = new ReusableBuffer(ByteBuffer.wrap(((String) "zombie").getBytes()));
    private OSDClient                           client;
    private String                              volumeID;
    private Set<String>                         zombieNames;
    
    public CleanUpTest() {
        Logging.start(Logging.LEVEL_WARN);
    }    
    
    @Before
    public void setUp() throws Exception {    
        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);

        FSUtils.delTree(testDir);
        testDir.mkdirs();
        
        System.out.println("TEST: " + getClass().getSimpleName() + "."
                + getName());
        
        // startup: DIR
        dir = new RequestController(SetupUtils.createDIRConfig());
        dir.startup();
        
        // startup: OSD
        osd = new OSD(SetupUtils.createOSD1Config()); 
        
        // startup: MRC
        mrc = new org.xtreemfs.mrc.RequestController(SetupUtils.createMRC1Config());
        mrc.startup(); 
        
        authString = NullAuthProvider.createAuthString("", "");
        zombieNames = new HashSet<String>();
        zombieNames.add("666"); zombieNames.add("667");
        
        layout = new HashStorageLayout(SetupUtils.createOSD1Config(),new MetadataCache());
        
        client = SetupUtils.createOSDClient(OSDClient.DEFAULT_TIMEOUT);
        
        mrcClient = SetupUtils.createMRCClient(MRCClient.DEFAULT_TIMEOUT);
    }
    
    @After
    public void tearDown() throws Exception {
        mrcClient.shutdown();
        client.shutdown();
        mrc.shutdown();
        osd.shutdown();
        dir.shutdown();
        client.waitForShutdown();  
        mrcClient.waitForShutdown();
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, BufferPool.getStatus());
    }
    
    /**
     * Test the Cleanup function without any files on the OSD.
     * @throws Exception
     */
    public void testCleanUpEmpty() throws Exception{    
        Map<String,Map<String,Map<String,String>>> result = null;
        
        // start the cleanUp Operation
        result = client.cleanUp(SetupUtils.getOSD1Addr(),authString).get(); 
        
        assertNull(result);
    }
   
    /**
     * Test the Cleanup function with files without zombies on the OSD.
     * @throws Exception
     */
    public void testCleanUpFilesWithoutZombies() throws Exception{   
        Map<String,Map<String,Map<String,String>>> result = null;
        
        insertSomeTestData();
        // start the cleanUp Operation
        result = client.cleanUp(SetupUtils.getOSD1Addr(),authString).get();        
        
        assertNull(result);
    }
    
    /**
     * Test the Cleanup function with files and with zombies on the OSD.
     * @throws Exception
     */ 
    public void testCleanUpFilesWithZombies() throws Exception{  
        Map<String,Map<String,Map<String,String>>> response = null;
        ConcurrentFileMap result = null;
        
        insertSomeTestData();
       
        //insert zombies
        String[] zombieArray = zombieNames.toArray(new String[2]);      
        layout.writeObject(volumeID+":"+zombieArray[0], 1, data, 1, 0, "chksum1", null, 1);
        layout.writeObject(volumeID+":"+zombieArray[1], 2, data, 1, 0, "chksum2", null, 1);
        
        // start the cleanUp Operation  
        response = client.cleanUp(SetupUtils.getOSD1Addr(),authString).get(); 
        
        assertNotNull(response);
        
        result = new ConcurrentFileMap(response);
        
        assertTrue(result.getFileNumberSet(volumeID).equals(zombieNames));
        
        //restore the zombie files
        for (List<String> volume : result.keySetList()){
            for (String file : result.getFileIDSet(volume)){
                Long fileNumber = Long.valueOf(file.substring(file.indexOf(":")+1, file.length()));
                
                mrcClient.restoreFile(
                        new InetSocketAddress(volume.get(1),
                        Integer.parseInt(volume.get(2))), 
                        "lost+found", fileNumber, 
                        result.getFileSize(volume, file),null, 
                        authString,SetupUtils.getOSD1UUID().toString(),result.getObjectSize(volume,file),volume.get(0)); 
            }
        }
       
        //check the osd once again
        response = client.cleanUp(SetupUtils.getOSD1Addr(),authString).get();
        
        assertNull(response);
    }
    
    /**
     * Test the Cleanup function with a volume registered at the DIR but not on the MRC.
     * @throws Exception
     */ 
    public void testCleanUpLostVolume() throws Exception{   
        // can not be tested right now
        assertTrue(true);
    }
   
    /**
     * Test the Cleanup function with files and with zombies(files of an unknown volume) on the OSD.
     * @throws Exception
     */ 
    public void testCleanUpUnkownVolume() throws Exception{
        Map<String,Map<String,Map<String,String>>> response = null;
        ConcurrentFileMap result = null;
        
        insertSomeTestData();
        
        //insert zombies
        String[] zombieArray = zombieNames.toArray(new String[2]); 
        layout.writeObject("002302340"+":"+zombieArray[0], 1, data, 1, 0, "chksum3", null, 1);
        layout.writeObject("002302340"+":"+zombieArray[1], 2, data, 1, 0, "chksum4", null, 1);    
        
        // start the cleanUp Operation
        response = client.cleanUp(SetupUtils.getOSD1Addr(),authString).get(); 
        
        assertNotNull(response);
        
        result = new ConcurrentFileMap(response);
        
        assertTrue(result.getFileNumberSet("unknown").equals(zombieNames));
        
        //Delete the zombie files
        client.cleanUpDelete(SetupUtils.getOSD1Addr(), authString, "002302340"+":"+zombieArray[0]).waitForResponse(0);
        client.cleanUpDelete(SetupUtils.getOSD1Addr(), authString, "002302340"+":"+zombieArray[1]).waitForResponse(0);
        
        //check the osd once again
        response = client.cleanUp(SetupUtils.getOSD1Addr(),authString).get();
        
        System.out.println(response);
        assertNull(response);
    }
    
    /**
     * Test the Cleanup function UI with zombies.
     * @throws Exception

    public void testCleanUpUI() throws Exception{   
        insertSomeTestData();
        
        //insert zombies
        String[] zombieArray = zombieNames.toArray(new String[2]); 
        layout.writeObject("002302340"+":"+zombieArray[0], 1, data, 1, 0, "chksum5", null, 1);
        layout.writeObject("002302340"+":"+zombieArray[1], 2, data, 1, 0, "chksum6", null, 1);
        layout.writeObject(volumeID+":"+zombieArray[0]+"1", 1, data, 1, 0, "chksum1", null, 1);
        layout.writeObject(volumeID+":"+zombieArray[1]+"1", 2, data, 1, 0, "chksum2", null, 1);
        
        String[] args = new String[3];
        args[0] = "-d";
        args[1] = "http://"+SetupUtils.getDIRAddr().getHostName()+":"+SetupUtils.getDIRAddr().getPort()+"/";        
        args[2] = "http://"+SetupUtils.getOSD1Addr().getHostName()+":"+SetupUtils.getOSD1Addr().getPort()+"/";
        cleanup_osd.main(args);
        
        assertTrue(true);
    }
     */          
    
    /**
     * Test the Cleanup function UI with zombies and UUID support.
     * @throws Exception
      
    public void testCleanUpUIUUID() throws Exception{   
        insertSomeTestData();
        
        //insert zombies
        String[] zombieArray = zombieNames.toArray(new String[2]); 
        layout.writeObject("002302340"+":"+zombieArray[0], 1, data, 1, 0, "chksum5", null, 1);
        layout.writeObject("002302340"+":"+zombieArray[1], 2, data, 1, 0, "chksum6", null, 1);
        layout.writeObject(volumeID+":"+zombieArray[0]+"1", 1, data, 1, 0, "chksum1", null, 1);
        layout.writeObject(volumeID+":"+zombieArray[1]+"1", 2, data, 1, 0, "chksum2", null, 1);
        
        String[] args = new String[1];
        args[0] = "uuid:"+SetupUtils.getOSD1UUID();
        cleanup_osd.main(args);
        
        assertTrue(true);
    }
     */
/*
 * private functions
 */
    
    private void insertSomeTestData() throws Exception{
        MRCClient mrcClient = SetupUtils.createMRCClient(10000);
        mrcClient.createVolume(SetupUtils.getMRC1Addr(), testVolume, authString);  
        mrcClient.createDir(SetupUtils.getMRC1Addr(), testVolume+"/test", authString);
        mrcClient.createDir(SetupUtils.getMRC1Addr(), testVolume+"/emptyDir", authString);
        mrcClient.createDir(SetupUtils.getMRC1Addr(), testVolume+"/anotherDir", authString);
        mrcClient.createFile(SetupUtils.getMRC1Addr(), testVolume+"/test/test1", authString);              
        mrcClient.createFile(SetupUtils.getMRC1Addr(), testVolume+"/test/test2", authString);  
        mrcClient.createFile(SetupUtils.getMRC1Addr(), testVolume+"/anotherDir/test3", authString); 
        
        RandomAccessFile test1 = new RandomAccessFile("r",SetupUtils.getMRC1Addr(),testVolume+"/test/test1",mrcClient.getSpeedy(),authString);
        RandomAccessFile test3 = new RandomAccessFile("r",SetupUtils.getMRC1Addr(),testVolume+"/anotherDir/test3",mrcClient.getSpeedy(),authString);
        
        String fileVolume = mrcClient.stat(SetupUtils.getMRC1Addr(), testVolume+"/test/test2", false, true, false, authString).get("fileId").toString();
        volumeID = fileVolume.substring(0, fileVolume.indexOf(':'));
        
        
        String content = "";
        for (int i = 0; i < 6000; i++)
            content = content.concat("Hello World ");
        byte[] bytesIn = content.getBytes();
        assertEquals(bytesIn.length, 72000);

        int length = bytesIn.length;

        test1.write(bytesIn, 0, length);
        test3.write(bytesIn, 0, 65536);
        
        mrcClient.shutdown();
        mrcClient.waitForShutdown();
    }
}
