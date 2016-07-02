/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class ECMasterStageTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    private TestEnvironment  testEnv;
    private OSDConfig[]      configs;
    private OSDServiceClient osdClient;
    private XLocSet          xloc;
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

        List<String> osdUUIDs = Arrays.asList(testEnv.getOSDUUIDs());

        StripingPolicy sp = ECOperationsTest.getECStripingPolicy(2, 1, 1);
        Replica r = Replica.newBuilder().setStripingPolicy(sp).setReplicationFlags(0).addAllOsdUuids(osdUUIDs).build();
        xloc = XLocSet.newBuilder().setReadOnlyFileSize(0).setVersion(1).addReplicas(r).setReplicaUpdatePolicy("ec")
                .build();
    }

    private Capability getCap(String fileId) {
        return new Capability(fileId,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber(),
                60, 
                System.currentTimeMillis(),
                "", 
                0,
                false,
                SnapConfig.SNAP_CONFIG_SNAPS_DISABLED,
                0,
                configs[0].getCapabilitySecret());
    }


    @After
    public void tearDown() {
        testEnv.shutdown();
    }

    @Test
    public void testRedirect() throws Exception {
        Capability cap = getCap(fileId);
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xloc).build();

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
    public void testRead() throws Exception {
        Capability cap = getCap(fileId);
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xloc).build();

        InetSocketAddress masterAddress = null;
        int masterIdx = 0;

        long objNumber = 0;
        long objVersion = -1;
        int offset = 0;
        int length = 1;

        RPCResponse<ObjectData> RPCresponse;
        ObjectData response;
        try {
            for (int i = 0; i < configs.length; i++) {
                InetSocketAddress address = configs[i].getUUID().getAddress();
                RPCresponse = osdClient.read(address, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                        fileId, objNumber, objVersion, offset, length);
                response = RPCresponse.get();
                masterAddress = address;
                masterIdx = i;
            }
        } catch (PBRPCException ex) {
            if (ex.getErrorType() != ErrorType.REDIRECT) {
                throw ex;
            }
        }

        fail("Not implemented yet");

    }

    @Test
    public void testWrite() throws Exception {

        List<String> osdUUIDs = Arrays.asList(testEnv.getOSDUUIDs()).subList(0, 2);
        StripingPolicy sp = ECOperationsTest.getECStripingPolicy(2, 0, 1);
        Replica r = Replica.newBuilder().setStripingPolicy(sp).setReplicationFlags(0).addAllOsdUuids(osdUUIDs).build();
        XLocSet xloc = XLocSet.newBuilder().setReadOnlyFileSize(0).setVersion(1).addReplicas(r).setReplicaUpdatePolicy("ec")
                .build();
        
        Capability cap = getCap(fileId);
        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xloc).build();

        
        InetSocketAddress masterAddress = null;
        int masterIdx = 0;

        long objNumber = 0;
        long objVersion = -1;
        int offset = 0;
        int length = 1;
        long lease_timeout = 0;

        RPCResponse<ObjectData> RPCReadResponse = null;
        ObjectData readResponse;

        // Find (and elect) the master
        for (int i = 0; i < configs.length; i++) {
            try {
                InetSocketAddress address = configs[i].getUUID().getAddress();
                RPCReadResponse = osdClient.read(address, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                        fileId, objNumber, objVersion, offset, length);
                readResponse = RPCReadResponse.get();
                masterAddress = address;
                masterIdx = i;
                RPCReadResponse.freeBuffers();
                BufferPool.free(RPCReadResponse.getData());
                RPCReadResponse = null;
                break;

            } catch (PBRPCException ex) {
                if (RPCReadResponse != null) {
                    RPCReadResponse.freeBuffers();
                    BufferPool.free(RPCReadResponse.getData());
                }
                if (ex.getErrorType() != ErrorType.REDIRECT) {
                    throw ex;
                }
            }
        }


        RPCResponse<OSDWriteResponse> RPCWriteResponse;
        OSDWriteResponse writeResponse;
        ObjectData objData = ObjectData.newBuilder().setChecksum(0).setZeroPadding(0).setInvalidChecksumOnOsd(false)
                .build();
        ReusableBuffer data;

        data = SetupUtils.generateData(1 * 1024);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data);
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();
        BufferPool.free(data);

        data = SetupUtils.generateData(2 * 1024);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data);
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();
        BufferPool.free(data);

        data = SetupUtils.generateData(3 * 1024);
        RPCWriteResponse = osdClient.write(masterAddress, RPCAuthentication.authNone, RPCAuthentication.userService, fc,
                fileId, objNumber, objVersion, offset, lease_timeout, objData, data);
        writeResponse = RPCWriteResponse.get();
        RPCWriteResponse.freeBuffers();
        BufferPool.free(data);

        System.out.println("wait");

    }
}
