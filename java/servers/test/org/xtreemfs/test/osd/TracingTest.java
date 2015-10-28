/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.osd;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.osd.tracing.TraceInfo;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class TracingTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    private TestEnvironment env;
    private Client client;
    private RPC.UserCredentials uc;
    private RPC.Auth auth = RPCAuthentication.authNone;

    private final String SOURCE_VOLUME = "traceSource";
    private final String FILE_BASED_TRACING_POLICY = "6001";
    private final String SOCKET_BASED_TRACING_POLICY = "6002";
    private final String FILE_BASED_TRACING_CONFIG = "/tmp/XtreemFS-Log-" + System.currentTimeMillis();
    private final int SOCKET_BASED_TRACING_PORT = 9999;
    private final String TEST_FILE ="/test";
    private final int MAX_WRITES = 100;

    private AtomicLong tracedLines;

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
    public void testFileBasedTracing() throws Exception {
        client.createVolume(env.getMRCAddress().getHostName() + ":" + env.getMRCAddress().getPort(), auth, uc,
                SOURCE_VOLUME);
        Volume sourceVolume = client.openVolume(SOURCE_VOLUME, null, new Options());

        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_enabled", "1",
                MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);
        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_policy_config", FILE_BASED_TRACING_CONFIG,
                MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);
        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_policy", FILE_BASED_TRACING_POLICY,
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

        File traceFile = new File(FILE_BASED_TRACING_CONFIG);
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
        File f = new File(FILE_BASED_TRACING_CONFIG);
        if (f.exists()) {
            f.delete();
        }
    }

    @Test
    public void testSocketBasedTracing() throws Exception {
        tracedLines = new AtomicLong();

        Runnable reader = new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    Thread.sleep(1000);
                    Socket s = new Socket("localhost", SOCKET_BASED_TRACING_PORT);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                    while((line = in.readLine()) != null) {
                        System.out.println(line);
                        if(!line.equals(""))
                            tracedLines.incrementAndGet();

                        try {
                            TraceInfo traceInfo = new TraceInfo(line);
                            assertTrue(traceInfo.getOperation().equals("w"));
                            assertTrue(traceInfo.getLength() > 0);
                        } catch (IllegalArgumentException ex) {
                            fail();
                        }
                    }
                    s.close();
                } catch(Exception ex) {
                    System.out.println(ex);
                }
            }
        };
        Thread t = new Thread(reader);
        t.start();

        client.createVolume(env.getMRCAddress().getHostName() + ":" + env.getMRCAddress().getPort(), auth, uc,
                 SOURCE_VOLUME);
        Volume sourceVolume = client.openVolume(SOURCE_VOLUME, null, new Options());

        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_enabled", "1",
                MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);
        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_policy", SOCKET_BASED_TRACING_POLICY,
                MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);

        FileHandle f = sourceVolume.openFile(uc, TEST_FILE,
                                             GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber() |
                                             GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber());

        byte[] writeBuffer = new byte[255];

        for (long i=0; i<=MAX_WRITES; i++)
            f.write(uc, writeBuffer, writeBuffer.length, i * writeBuffer.length);

        f.close();

        sourceVolume.unlink(uc, TEST_FILE);
        Thread.sleep(1000);
        assertTrue(tracedLines.get() > 0L);
    }
}
