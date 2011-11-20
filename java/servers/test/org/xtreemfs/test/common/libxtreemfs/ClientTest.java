/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.common.libxtreemfs;

import java.io.IOException;
import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.UUIDIterator;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volumes;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

/**
 * 
 * <br>
 * Sep 3, 2011
 */
public class ClientTest extends TestCase {

    private DIRRequestDispatcher dir;

    private TestEnvironment      testEnv;

    private DIRConfig            dirConfig;

    private UserCredentials      userCredentials;

    private Auth                 auth = RPCAuthentication.authNone;

    private DIRClient            dirClient;

    /**
     * 
     */
    public ClientTest() throws IOException {

        dirConfig = SetupUtils.createDIRConfig();
        Logging.start(Logging.LEVEL_DEBUG);

    }

    @Before
    public void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName());

        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC, TestEnvironment.Services.OSD });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();

        dirClient = new DIRClient(new DIRServiceClient(testEnv.getRpcClient(), null),
                new InetSocketAddress[] { testEnv.getDIRAddress() }, 3, 1000);

    }

    @After
    public void tearDown() throws Exception {

        testEnv.shutdown();

        dir.shutdown();

        dir.waitForShutdown();

    }

//    public void testCreateOpenRemoveListVolume() throws Exception {
//
//        final String VOLUME_NAME_1 = "foobar";
//
//        // TODO: Create pseudo commandline and parse it.
//        Options options = new Options(5000, 10000, 4, 2);
//
//        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
//
//        Client client = Client.createClient(dirAddress, userCredentials, null, options);
//        client.start();
//
//        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();
//
//        // Create volume
//        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
//
//        ServiceSet sSet = null;
//        sSet = dirClient.xtreemfs_service_get_by_name(testEnv.getDIRAddress(), auth, userCredentials,
//                VOLUME_NAME_1);
//
//        assertEquals(1, sSet.getServicesCount());
//        assertEquals(VOLUME_NAME_1, sSet.getServices(0).getName());
//
//        // List volumes
//        Volumes volumes = client.listVolumes(mrcAddress);
//        assertEquals(1, volumes.getVolumesCount());
//        assertEquals(VOLUME_NAME_1, volumes.getVolumes(0).getName());
//
//        // Delete volume
//        client.deleteVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
//
//        sSet = null;
//
//        sSet = dirClient.xtreemfs_service_get_by_name(testEnv.getDIRAddress(), auth, userCredentials,
//                VOLUME_NAME_1);
//
//        assertEquals(0, sSet.getServicesCount());
//
//        // List volumes
//        volumes = client.listVolumes(mrcAddress);
//        assertEquals(0, volumes.getVolumesCount());
//
//        // shutdown the client
//        client.shutdown();
//    }
//
//    public void testUUIDResolver() throws Exception {
//        final String VOLUME_NAME_1 = "foobar";
//        final String VOLUME_NAME_2 = "barfoo";
//
//        // TODO: Create pseudo commandline and parse it.
//        Options options = new Options(5000, 10000, 4, 2);
//
//        String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
//
//        Client client = Client.createClient(dirAddress, userCredentials, null, options);
//        client.start();
//
//        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();
//
//        // Create volumes
//        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
//        client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_2);
//
//       
//        // get and MRC UUID for the volume. Should be that from the only MRC.
//        String uuidString = client.volumeNameToMRCUUID(VOLUME_NAME_1);
//        assertEquals(SetupUtils.getMRC1UUID().toString(), uuidString);
//        
//        // same for the second volume
//        uuidString = client.volumeNameToMRCUUID(VOLUME_NAME_2);
//        assertEquals(SetupUtils.getMRC1UUID().toString(), uuidString);
//            
//        // this should work if we use UUIDIterator, too.
//        UUIDIterator uuidIterator = new UUIDIterator();
//        client.volumeNameToMRCUUID(VOLUME_NAME_1, uuidIterator);
//        assertEquals(SetupUtils.getMRC1UUID().toString(), uuidIterator.getUUID());
//        
//        // resolve MRC UUID
//        String address = client.uuidToAddress(SetupUtils.getMRC1UUID().toString());
//        assertEquals(testEnv.getMRCAddress().getHostName()+":"+testEnv.getMRCAddress().getPort(), address);
//
//        // resolve OSD UUID
//        address = client.uuidToAddress(SetupUtils.createMultipleOSDConfigs(1)[0].getUUID().toString());
//        assertEquals(testEnv.getOSDAddress().getHostName()+":"+testEnv.getOSDAddress().getPort(), address);
//        
//        // shutdown the client
//        client.shutdown();
//    }
    
    @Test
    public void testMinimalExample() throws Exception {
    	final String VOLUME_NAME_1 = "foobar";
    	
    	Options options = new Options(5000, 10000, 4, 2);
    	String dirAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        String mrcAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();
    	
    	Client client = Client.createClient(dirAddress, userCredentials, null, options);
    	client.start();
    	
    	//Open a volume named "foobar".
    	client.createVolume(mrcAddress, auth, userCredentials, VOLUME_NAME_1);
    	Volume volume = client.openVolume(VOLUME_NAME_1, null, options);
    	
    	// Open a file.
    	FileHandle fileHandle = volume.openFile(userCredentials, "/bla.tzt", 
    			SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber() |
    			SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber() |
    			SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());
    	
    	// Get file attributes
    	Stat stat = volume.getAttr(userCredentials, "/bla.tzt");
    	assertEquals(0, stat.getSize());
    	
    	// Write to file.
    	String data = "Need a testfile? Why not (\\|)(+,,,+)(|/)?";
    	ReusableBuffer buf = ReusableBuffer.wrap(data.getBytes());
    	fileHandle.write(userCredentials, buf, buf.capacity(), 0);
//    	fileHandle.close();
    	
    	
//TODO: Check how to get fileSize    	stat = volume.getAttr(userCredentials, "/bla.tzt");
//    	assertEquals(data.length(), stat.getSize());
    	
    	// Read from file.
    	byte[] readData = new byte[data.length()];
    	ReusableBuffer readBuf = ReusableBuffer.wrap(readData);
    	int readCount = fileHandle.read(userCredentials, readBuf, data.length(), 0);
    	
    	assertEquals(data.length(), readCount);
    	for (int i = 0; i < data.length(); i++) {
			assertEquals(readData[i], data.getBytes()[i]);
		}
    	
    	fileHandle.close();

    	//TODO: join of renewal thread don't work. fix it.
    	//client.shutdown();

    }
}
