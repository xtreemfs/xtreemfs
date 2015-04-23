/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.libxtreemfs.*;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC;
import org.xtreemfs.test.TestEnvironment;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class TracingTest {
    private TestEnvironment env;
    private Client client;
    private RPC.UserCredentials uc;
    private RPC.Auth auth = RPCAuthentication.authNone;

    private final String SOURCE_VOLUME = "traceSource";
    private final String TRACING_POLICY = "6001";
    private final String TRACING_POLICY_CONFIG = "/tmp/XtreemFS-Log-" + System.currentTimeMillis();
    private final String TEST_FILE ="/test";
    private final int MAX_WRITES = 10;

    @Before
    public void setUp() throws Exception {
        deleteTraceFile();

        Logging.start(Logging.LEVEL_ERROR, Logging.Category.all);
        env = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.DIR_SERVICE, TestEnvironment.Services.MRC,
                TestEnvironment.Services.OSD});
        env.start();

        uc = RPC.UserCredentials.newBuilder().setUsername("service")
                .addGroups("service").build();

        client = ClientFactory.createClient(env.getDIRAddress().getHostName() + ":" + env.getDIRAddress().getPort(),
                                            uc, null, new Options());
        client.start(true);
    }

    @After
    public void tearDown() throws Exception {
        client.shutdown();
        env.shutdown();
        deleteTraceFile();
   }

    @Test
    public void testTracing() throws Exception {
        client.createVolume(env.getMRCAddress().getHostName() + ":" + env.getMRCAddress().getPort(), auth, uc,
                SOURCE_VOLUME);
        Volume sourceVolume = client.openVolume(SOURCE_VOLUME, null, new Options());

        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_enabled", "1",
                MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);
        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_policy_config", TRACING_POLICY_CONFIG,
                MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);
        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_policy", TRACING_POLICY,
                              MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);

        FileHandle f = sourceVolume.openFile(uc, TEST_FILE,
                                             GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber() |
                                             GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber());

        byte[] writeBuffer = new byte[255];

        for (long i=0; i<=MAX_WRITES; i++)
            f.write(uc, writeBuffer, writeBuffer.length, i * writeBuffer.length);

        f.close();
        sourceVolume.unlink(uc, TEST_FILE);

        client.deleteVolume(auth, uc, SOURCE_VOLUME);

        File traceFile = new File(TRACING_POLICY_CONFIG);
        assertTrue(traceFile.exists());

        int lines = 0;
        FileInputStream fstream = new FileInputStream(traceFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            lines++;
        }

        assertTrue(lines >= MAX_WRITES);
    }

    private void deleteTraceFile() {
        File f = new File(TRACING_POLICY_CONFIG);
        if (f.exists()) {
            f.delete();
        }
    }
}
