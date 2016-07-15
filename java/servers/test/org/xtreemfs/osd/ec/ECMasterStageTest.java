/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class ECMasterStageTest extends ECTestCommon {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    private TestEnvironment     testEnv;
    private OSDConfig[]         configs;
    private OSDServiceClient    osdClient;
    private List<String>        osdUUIDs;
    private FileCredentials     fc;
    private static final String fileId  = "ABCDEF:1";

    @BeforeClass
    public static void initializeTest() throws Exception {
        // Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
        Logging.start(Logging.LEVEL_INFO);
    }

    @Before
    public void setUp() throws Exception {
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.OSD_CLIENT, 
                TestEnvironment.Services.OSD, 
                TestEnvironment.Services.OSD,
                TestEnvironment.Services.OSD });
        testEnv.start();
        
        configs = testEnv.getOSDConfigs();

        osdClient = testEnv.getOSDClient();
        RPCNIOSocketClient client = new RPCNIOSocketClient(null, 5 * 60 * 1000, 10 * 60 * 1000, "EC1");
        osdClient = new OSDServiceClient(client, null);
        client.start();
        client.waitForStartup();

        osdUUIDs = Arrays.asList(testEnv.getOSDUUIDs());
        fc = getFileCredentials(fileId, 2, 1, 1, 2, osdUUIDs);
    }

    @After
    public void tearDown() {
        testEnv.shutdown();
    }

    @Test
    public void testElection() throws Exception {
        String fileId;
        FileCredentials fc;

        long objNumber = 0;
        long objVersion = -1;
        int offset = 0;
        int length = 1;

        RPCResponse<ObjectData> RPCresponse = null;
        ObjectData response;

        // Elect the master while all OSDs are available
        fileId = "testElection:0";
        // Create FileCredentials with a WriteQuorum of 2 => ReadQuorum 3
        fc = getFileCredentials(fileId, 2, 1, 1, 2, osdUUIDs);


        try {
            InetSocketAddress address = configs[0].getUUID().getAddress();
            RPCresponse = osdClient.read(address, RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileId,
                    objNumber, objVersion, offset, length);
            response = RPCresponse.get();
        } finally {
            if (RPCresponse != null) {
                RPCresponse.freeBuffers();
                BufferPool.free(RPCresponse.getData());
                RPCresponse = null;
            }
        }

        // Elect the master while one OSD (second) is down
        testEnv.stopOSD(configs[1].getUUID().toString());
        fileId = "testElection:1";
        // Create FileCredentials with a WriteQuorum of 3 => ReadQuorum 2
        fc = getFileCredentials(fileId, 2, 1, 1, 3, osdUUIDs);
        try {
            InetSocketAddress address = configs[0].getUUID().getAddress();
            RPCresponse = osdClient.read(address, RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileId,
                    objNumber, objVersion, offset, length);
            response = RPCresponse.get();
        } finally {
            if (RPCresponse != null) {
                RPCresponse.freeBuffers();
                BufferPool.free(RPCresponse.getData());
                RPCresponse = null;
            }
        }

        // Provove election error while one OSD (second) is down with ReadQuorum = 3
        fileId = "testElection:2";
        // Create FileCredentials with a WriteQuorum of 2 => ReadQuorum 3
        fc = getFileCredentials(fileId, 2, 1, 1, 2, osdUUIDs);
        try {
            InetSocketAddress address = configs[0].getUUID().getAddress();
            RPCresponse = osdClient.read(address, RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileId,
                    objNumber, objVersion, offset, length);
            response = RPCresponse.get();
            fail();
        } catch (IOException ex) {
            // expected
            ex.printStackTrace();
        } finally {
            if (RPCresponse != null) {
                RPCresponse.freeBuffers();
                BufferPool.free(RPCresponse.getData());
                RPCresponse = null;
            }
        }
    }

    @Test
    public void testRedirect() throws Exception {
        int masterIdx = 0;

        long objNumber = 0;
        long objVersion = -1;
        int offset = 0;
        int length = 1;

        RPCResponse<ObjectData> RPCresponse = null;
        ObjectData response;

        // Find (and elect) the master
        for (int i = 0; i < configs.length; i++) {
            try {
                InetSocketAddress address = configs[i].getUUID().getAddress();
                RPCresponse = osdClient.read(address, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                        fileId, objNumber, objVersion, offset, length);
                response = RPCresponse.get();
                masterIdx = i;
                RPCresponse.freeBuffers();
                BufferPool.free(RPCresponse.getData());
                RPCresponse = null;
                break;

            } catch (PBRPCException ex) {
                if (RPCresponse != null) {
                    RPCresponse.freeBuffers();
                    BufferPool.free(RPCresponse.getData());
                }
                if (ex.getErrorType() != ErrorType.REDIRECT) {
                    throw ex;
                }
            }
        }

        // Access a slave
        try {
            int j = (masterIdx + 1) % configs.length;
            InetSocketAddress address = configs[j].getUUID().getAddress();
            RPCresponse = osdClient.read(address, RPCAuthentication.authNone, RPCAuthentication.userService, fc, fileId,
                    objNumber, objVersion, offset, length);
            response = RPCresponse.get();
            fail();
        } catch (PBRPCException ex) {
            if (ex.getErrorType() != ErrorType.REDIRECT) {
                throw ex;
            }
        }
    }


    @Test
    public void testWriteNoParity() throws Exception {

        List<String> osdUUIDs = Arrays.asList(testEnv.getOSDUUIDs()).subList(0, 2);
        FileCredentials fc = getFileCredentials(fileId, 2, 0, 1, 2, osdUUIDs);
        StripingPolicyImpl spi = ECOperationsTest.getStripingPolicyImplementation(fc);
        
        HashStorageLayout layout;
        FileMetadata fi;
        ObjectInformation objInfo;
        String fileIdNext = fileId + ECStorage.FILEID_NEXT_SUFFIX;
        
        InetSocketAddress masterAddress = electMaster(fileIdNext, fc);

        long objNumber = 0;
        long objVersion = -1;
        int offset = 0;
        int length = 1;
        long lease_timeout = 0;

        RPCResponse<ObjectData> RPCReadResponse = null;
        ObjectData readResponse;

        RPCResponse<OSDWriteResponse> RPCWriteResponse;
        OSDWriteResponse writeResponse;
        ObjectData objData = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        ReusableBuffer data, expected;

        

        // Write the first object on a single OSD
        // **************************************
        length = 1 * 1024;
        data = SetupUtils.generateData(length);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();
        
        layout = new HashStorageLayout(configs[0], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 0, 0, 1024, 1);
        assertBufferEquals(data, objInfo.getData());
        BufferPool.free(data);
        BufferPool.free(objInfo.getData());
        
        
        
        // Write the whole first stripe over both OSDS
        // *******************************************
        length = 2 * 1024;
        data = SetupUtils.generateData(length);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();

        layout = new HashStorageLayout(configs[0], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 0, 0, 1024, 1);
        expected = data.createViewBuffer();
        expected.range(0, 1024);

        assertBufferEquals(expected, objInfo.getData());
        BufferPool.free(expected);
        BufferPool.free(objInfo.getData());

        layout = new HashStorageLayout(configs[1], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 1, 0, 1024, 1);
        expected = data.createViewBuffer();
        expected.range(1024, 1024);

        assertBufferEquals(expected, objInfo.getData());
        BufferPool.free(expected);
        BufferPool.free(objInfo.getData());
        BufferPool.free(data);



        // Write the whole first stripe and half of the second over both OSDS
        // ******************************************************************
        length = 3 * 1024;
        data = SetupUtils.generateData(length);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();
        BufferPool.free(data);

        layout = new HashStorageLayout(configs[0], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 0, 0, 1024, 1);
        expected = data.createViewBuffer();
        expected.range(0, 1024);

        assertBufferEquals(expected, objInfo.getData());
        BufferPool.free(expected);
        BufferPool.free(objInfo.getData());

        layout = new HashStorageLayout(configs[0], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 2, 0, 1024, 1);
        expected = data.createViewBuffer();
        expected.range(2048, 1024);

        assertBufferEquals(expected, objInfo.getData());
        BufferPool.free(expected);
        BufferPool.free(objInfo.getData());

        layout = new HashStorageLayout(configs[1], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 1, 0, 1024, 1);
        expected = data.createViewBuffer();
        expected.range(1024, 1024);

        assertBufferEquals(expected, objInfo.getData());
        BufferPool.free(expected);
        BufferPool.free(objInfo.getData());
        BufferPool.free(data);



        // Write between first and second chunk (no full stripes)
        // ******************************************************
        length = 1024;
        objNumber = 0;
        offset = 512;
        data = SetupUtils.generateData(length);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();
        BufferPool.free(data);

        layout = new HashStorageLayout(configs[0], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 0, 512, 512, 1);
        expected = data.createViewBuffer();
        expected.range(0, 512);

        assertBufferEquals(expected, objInfo.getData());
        BufferPool.free(expected);
        BufferPool.free(objInfo.getData());

        layout = new HashStorageLayout(configs[1], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 1, 0, 512, 1);
        expected = data.createViewBuffer();
        expected.range(512, 512);

        assertBufferEquals(expected, objInfo.getData());
        BufferPool.free(expected);
        BufferPool.free(objInfo.getData());
        BufferPool.free(data);

        
        
        // Write between first and second stripe (no full stripes)
        // ******************************************************
        length = 1024;
        objNumber = 1;
        offset = 512;
        data = SetupUtils.generateData(length);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();
        BufferPool.free(data);

        layout = new HashStorageLayout(configs[0], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 2, 0, 512, 1);
        expected = data.createViewBuffer();
        expected.range(512, 512);

        assertBufferEquals(expected, objInfo.getData());
        BufferPool.free(expected);
        BufferPool.free(objInfo.getData());

        layout = new HashStorageLayout(configs[1], new MetadataCache());
        fi = layout.getFileMetadata(spi, fileId);
        objInfo = layout.readObject(fileIdNext, fi, 1, 512, 512, 1);
        expected = data.createViewBuffer();
        expected.range(0, 512);

        assertBufferEquals(expected, objInfo.getData());
        BufferPool.free(expected);
        BufferPool.free(objInfo.getData());
        BufferPool.free(data);

    }

    InetSocketAddress electMaster(String fileId, FileCredentials fc) throws Exception {
        return electMaster(fileId, fc, 0);
    }

    InetSocketAddress electMaster(String fileId, FileCredentials fc, int skip) throws Exception {
        long objNumber = 0;
        long objVersion = -1;
        int offset = 0;
        int length = 1;
        long lease_timeout = 0;

        RPCResponse<ObjectData> RPCReadResponse = null;
        ObjectData readResponse;

        skip = skip % configs.length;

        InetSocketAddress masterAddress = null;
        // Find (and elect) the master
        for (int i = skip; i < configs.length; i++) {
            try {
                InetSocketAddress address = configs[i].getUUID().getAddress();
                RPCReadResponse = osdClient.read(address, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                        fileId, objNumber, objVersion, offset, length);
                readResponse = RPCReadResponse.get();
                masterAddress = address;
                RPCReadResponse.freeBuffers();
                BufferPool.free(RPCReadResponse.getData());
                RPCReadResponse = null;
                break;

            } catch (Exception ex) {
                if (RPCReadResponse != null) {
                    RPCReadResponse.freeBuffers();
                    BufferPool.free(RPCReadResponse.getData());
                }
            }
        }

        if (masterAddress == null) {
            throw new Exception("Could not elect master");
        }
        return masterAddress;
    }

}
