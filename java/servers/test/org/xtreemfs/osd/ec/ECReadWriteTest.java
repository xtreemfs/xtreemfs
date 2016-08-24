/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class ECReadWriteTest extends ECTestCommon {
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
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
        // Logging.start(Logging.LEVEL_INFO);
        // Logging.start(Logging.LEVEL_DEBUG);
    }

    @Before
    public void setUp() throws Exception {
        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_SERVICE,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.UUID_RESOLVER,
                TestEnvironment.Services.OSD_CLIENT, 
                TestEnvironment.Services.OSD, 
                TestEnvironment.Services.OSD,
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
        fc = getFileCredentials(fileId, 3, 2, 1, 4, osdUUIDs.subList(0, 5));
    }

    @After
    public void tearDown() {
        testEnv.shutdown();
    }

    @Test
    public void testWriteRead32() throws Exception {

        String fileId = "WriteParity32:0";
        FileCredentials fc = getFileCredentials(fileId, 3, 2, 1, 4, osdUUIDs.subList(0, 5));

        // Run the tests
        testWriteRead(fileId, fc, 3, 1024);
    }

    @Test
    public void testWriteRead41() throws Exception {

        String fileId = "WriteParity41:0";
        FileCredentials fc = getFileCredentials(fileId, 4, 1, 1, 4, osdUUIDs.subList(0, 5));

        // Run the tests
        testWriteRead(fileId, fc, 4, 1024);
    }

    @Test
    public void testWriteRead50() throws Exception {

        String fileId = "WriteParity50:0";
        FileCredentials fc = getFileCredentials(fileId, 5, 0, 1, 5, osdUUIDs.subList(0, 5));

        // Run the tests
        testWriteRead(fileId, fc, 5, 1024);
    }

    @Test
    public void testWriteReadDegraded32() throws Exception {

        for (int i = 0; i < 5; i++) {
            String fileId = "WriteParityDegraded32:" + i;
            FileCredentials fc = getFileCredentials(fileId, 3, 2, 1, 4, osdUUIDs.subList(0, 5));

            // Stop the OSD
            testEnv.stopOSD(configs[i].getUUID().toString());
            // Run the tests
            System.out.println("Failing no " + i);
            testWriteRead(fileId, fc, 3, 1024);
            // Restart the OSD
            testEnv.startOSD(configs[i].getUUID().toString());
            Thread.sleep(5 * 1000);
        }
    }

    @Test
    public void testWriteReadDegraded32Manual() throws Exception {
        int i = 1;
        String fileId = "WriteParityDegraded32Manual:" + i;
        FileCredentials fc = getFileCredentials(fileId, 3, 2, 1, 4, osdUUIDs.subList(0, 5));

        // Stop the OSD
        testEnv.stopOSD(configs[i].getUUID().toString());
        // Run the tests
        System.out.println("Failing no " + i);
        testWriteRead(fileId, fc, 3, 1024);
        // Restart the OSD
        testEnv.startOSD(configs[i].getUUID().toString());
    }


    public void testWriteRead(String fileId, FileCredentials fc, int dataWidth, int chunkSize) throws Exception {

        InetSocketAddress masterAddress = electMaster(fileId, fc);

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
        ReusableBuffer data, dout;

        // Read non existing data (0) from every data OSD
        // **********************************************
        data = ECHelper.zeroPad(null, chunkSize);
        for (int i = 0; i < dataWidth; i++) {
            objNumber = i;
            length = chunkSize;
            RPCReadResponse = osdClient.read(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService,
                    fc, fileId, objNumber, objVersion, offset, length);
            readResponse = RPCReadResponse.get();
            RPCReadResponse.freeBuffers();
            dout = RPCReadResponse.getData();
            RPCReadResponse = null;

            assertBufferEquals(data, dout);
            BufferPool.free(dout);
        }
        BufferPool.free(data);


        // Write a single object on the first data OSD
        // *******************************************
        objNumber = 0;
        length = chunkSize;
        data = SetupUtils.generateData(length, (byte) 1);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();

        RPCReadResponse = osdClient.read(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, length);
        readResponse = RPCReadResponse.get();
        RPCReadResponse.freeBuffers();
        dout = RPCReadResponse.getData();
        RPCReadResponse = null;

        assertBufferEquals(data, dout);
        BufferPool.free(data);
        BufferPool.free(dout);



        // Write a single object on the last data OSD: provokes a gap
        // **********************************************************
        objNumber = dataWidth - 1;
        length = chunkSize;
        data = SetupUtils.generateData(length, (byte) 2);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();

        RPCReadResponse = osdClient.read(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, length);
        readResponse = RPCReadResponse.get();
        RPCReadResponse.freeBuffers();
        dout = RPCReadResponse.getData();
        RPCReadResponse = null;

        assertBufferEquals(data, dout);
        BufferPool.free(data);
        BufferPool.free(dout);

        nowDebug = true;
        // Try reading from the gap
        objNumber = dataWidth - 2;
        length = chunkSize;
        data = ECHelper.zeroPad(null, chunkSize);
        RPCReadResponse = osdClient.read(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, length);
        readResponse = RPCReadResponse.get();
        RPCReadResponse.freeBuffers();
        dout = RPCReadResponse.getData();
        RPCReadResponse = null;

        assertBufferEquals(data, dout);
        BufferPool.free(data);
        BufferPool.free(dout);



        // Write the whole first stripe over all three OSDs
        // ************************************************
        objNumber = 0;
        length = dataWidth * chunkSize;
        data = SetupUtils.generateData(length);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();

        RPCReadResponse = osdClient.read(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, length);
        readResponse = RPCReadResponse.get();
        RPCReadResponse.freeBuffers();
        dout = RPCReadResponse.getData();
        RPCReadResponse = null;

        assertBufferEquals(data, dout);
        BufferPool.free(data);
        BufferPool.free(dout);



        // Write the whole first stripe and part of the second over all three OSDs
        // ***********************************************************************
        length = (dataWidth + 1) * chunkSize;
        data = SetupUtils.generateData(length);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();

        RPCReadResponse = osdClient.read(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, length);
        readResponse = RPCReadResponse.get();
        RPCReadResponse.freeBuffers();
        dout = RPCReadResponse.getData();
        RPCReadResponse = null;

        assertBufferEquals(data, dout);
        BufferPool.free(data);
        BufferPool.free(dout);



        // Write between first and second chunk (no full stripes)
        // ******************************************************
        objNumber = 0;
        offset = chunkSize / 2;
        length = chunkSize;
        data = SetupUtils.generateData(length);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();

        RPCReadResponse = osdClient.read(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, length);
        readResponse = RPCReadResponse.get();
        RPCReadResponse.freeBuffers();
        dout = RPCReadResponse.getData();
        RPCReadResponse = null;

        assertBufferEquals(data, dout);
        BufferPool.free(data);
        BufferPool.free(dout);



        // Write between first and second stripe (no full stripes)
        // ******************************************************
        objNumber = dataWidth;
        offset = chunkSize / 2;
        length = chunkSize;
        data = SetupUtils.generateData(length);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data.createViewBuffer());
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();

        RPCReadResponse = osdClient.read(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, length);
        readResponse = RPCReadResponse.get();
        RPCReadResponse.freeBuffers();
        dout = RPCReadResponse.getData();
        RPCReadResponse = null;

        assertBufferEquals(data, dout);
        BufferPool.free(data);
        BufferPool.free(dout);


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
