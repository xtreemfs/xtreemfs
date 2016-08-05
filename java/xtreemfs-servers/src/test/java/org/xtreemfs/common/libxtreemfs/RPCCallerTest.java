/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.ClientFactory.ClientType;
import org.xtreemfs.common.libxtreemfs.RPCCaller.CallGenerator;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SERVICES;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.openRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.openResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.writeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class RPCCallerTest {
    @Rule
    public final TestRule               testLog = TestHelper.testLog;

    private static DIRRequestDispatcher dir;

    private static DIRConfig            dirConfig;

    private static TestEnvironment      testEnv;

    private static UserCredentials      userCredentials;

    private static Auth                 auth;

    private static String               dirServiceAddress;
    private static String               mrcServiceAddress;

    private static MRCServiceClient     mrcServiceClient;

    private static OSDServiceClient     osdServiceClient;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);

        FSUtils.delTree(new java.io.File(SetupUtils.TEST_DIR));

        dirConfig = SetupUtils.createDIRConfig();
        dir = new DIRRequestDispatcher(dirConfig, SetupUtils.createDIRdbsConfig());
        dir.startup();
        dir.waitForStartup();

        testEnv = new TestEnvironment(new TestEnvironment.Services[] { TestEnvironment.Services.DIR_CLIENT,
                TestEnvironment.Services.TIME_SYNC, TestEnvironment.Services.RPC_CLIENT,
                TestEnvironment.Services.MRC, TestEnvironment.Services.OSD, TestEnvironment.Services.OSD });
        testEnv.start();

        userCredentials = UserCredentials.newBuilder().setUsername("test").addGroups("test").build();
        auth = RPCAuthentication.authNone;
        dirServiceAddress = testEnv.getDIRAddress().getHostName() + ":" + testEnv.getDIRAddress().getPort();
        mrcServiceAddress = testEnv.getMRCAddress().getHostName() + ":" + testEnv.getMRCAddress().getPort();

        mrcServiceClient = new MRCServiceClient(testEnv.getRpcClient(), null);
        osdServiceClient = new OSDServiceClient(testEnv.getRpcClient(), null);
    }

    @AfterClass
    public static void shutdownTest() throws Exception {
        testEnv.shutdown();
        dir.shutdown();
        dir.waitForShutdown();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRedirect() throws Exception {
        final String VOLUME_NAME = "testVolume";
        final String FILE_PATH = "replicatedFile";
        final int NUMBER_OF_REPLICAS = 2;

        Options options = new Options();
        ClientImplementation client = (ClientImplementation) ClientFactory.createClient(ClientType.NATIVE, 
        		dirServiceAddress, userCredentials, null, options);
        client.start();
        client.createVolume(mrcServiceAddress, auth, userCredentials, VOLUME_NAME);

        Volume volume = client.openVolume(VOLUME_NAME, null, options);

        // set default replication policy for root dir
        volume.setDefaultReplicationPolicy(userCredentials, "/", ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE,
                NUMBER_OF_REPLICAS, REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber());

        String replPolString = volume.getXAttr(userCredentials, "/", "xtreemfs.default_rp");
        JSONString policy = new JSONString(replPolString);
        Map<String, Object> jsonMap = (Map<String, Object>) JSONParser.parseJSON(policy);

        assertEquals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE, jsonMap.get("update-policy").toString());

        UUIDIterator mrcUUIDIterator = new UUIDIterator();
        mrcUUIDIterator.addUUID(mrcServiceAddress);

        openRequest request = openRequest
                .newBuilder()
                .setVolumeName(VOLUME_NAME)
                .setPath(FILE_PATH)
                .setFlags(
                        SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()
                                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()).setMode(0777)
                .setAttributes(0).build();

        openResponse response = RPCCaller.<openRequest, openResponse> syncCall(SERVICES.MRC, userCredentials,
                auth, options, client, mrcUUIDIterator, true, request,
                new CallGenerator<openRequest, openResponse>() {
                    @Override
                    public RPCResponse<openResponse> executeCall(InetSocketAddress server, Auth authHeader,
                            UserCredentials userCreds, openRequest input) throws IOException {
                        return mrcServiceClient.open(server, authHeader, userCreds, input);
                    }
                });

        FileInfo fileInfo = new FileInfo((VolumeImplementation) volume, Helper.extractFileIdFromXcap(response
                .getCreds().getXcap()), FILE_PATH, false, response.getCreds().getXlocs(),
                Helper.generateVersion4UUID());

        FileHandleImplementation fileHandle = fileInfo.createFileHandle(response.getCreds().getXcap(), false);

        // write testfile
        String data = "Need a testfile? Why not (\\|)(+,,,+)(|/)?";
        fileHandle.write(userCredentials, data.getBytes(), data.length(), 0);

        // create uuidIterator with switched OSDs in order force a Redirect Exception
        UUIDIterator uuidIterator = new UUIDIterator();
        uuidIterator.addUUID(response.getCreds().getXlocs().getReplicas(1).getOsdUuids(0));
        uuidIterator.addUUID(response.getCreds().getXlocs().getReplicas(0).getOsdUuids(0));

        // make a write with the new uuidIterator
        String overwriteData = "1111111111111111111111111111111111111111111111111111";
        final ReusableBuffer overwriteBuf = ReusableBuffer.wrap(overwriteData.getBytes());
        writeRequest.Builder writeReq = writeRequest.newBuilder();
        writeReq.setFileCredentials(response.getCreds());
        writeReq.setFileId(response.getCreds().getXcap().getFileId());
        writeReq.setObjectNumber(0);
        writeReq.setObjectVersion(0);
        writeReq.setOffset(0);
        writeReq.setLeaseTimeout(0);

        ObjectData objectData = ObjectData.newBuilder().setChecksum(0).setInvalidChecksumOnOsd(false)
                .setZeroPadding(0).build();
        writeReq.setObjectData(objectData);

        RPCCaller.<writeRequest, OSDWriteResponse> syncCall(SERVICES.OSD, userCredentials, auth, options,
                client, uuidIterator, false, writeReq.build(),
                new CallGenerator<writeRequest, OSDWriteResponse>() {

                    @Override
                    public RPCResponse<OSDWriteResponse> executeCall(InetSocketAddress server,
                            Auth authHeader, UserCredentials userCreds, writeRequest input)
                            throws IOException {
                        // TODO Auto-generated method stub
                        return osdServiceClient.write(server, authHeader, userCreds, input, overwriteBuf);
                    }
                });

        // Read from file.
        byte[] readData = new byte[overwriteData.length()];
        int readCount = fileHandle.read(userCredentials, readData, overwriteData.length(), 0);

        assertEquals(overwriteData.length(), readCount);
        for (int i = 0; i < overwriteData.length(); i++) {
            assertEquals(readData[i], overwriteData.getBytes()[i]);
        }
    }
}
