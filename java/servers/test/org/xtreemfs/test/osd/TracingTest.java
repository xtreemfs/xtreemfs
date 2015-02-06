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
    private final String TARGET_VOLUME = "traceTarget";
    private final String TEST_FILE ="/test";

    @Before
    public void setUp() throws Exception {
        Logging.start(Logging.LEVEL_ERROR, Logging.Category.all);
        env = new TestEnvironment(new TestEnvironment.Services[]{TestEnvironment.Services.TIME_SYNC,
                TestEnvironment.Services.DIR_SERVICE, TestEnvironment.Services.MRC,
                TestEnvironment.Services.OSD});
        env.start();

        uc = RPC.UserCredentials.newBuilder().setUsername("test")
                .addGroups("test").build();

        client = ClientFactory.createClient(env.getDIRAddress().getHostName() + ":" + env.getDIRAddress().getPort(),
                                            uc, null, new Options());
        client.start(true);
    }

    @After
    public void tearDown() throws Exception {
        client.shutdown();
        env.shutdown();
    }

    @Test
    public void testTracing() throws Exception {
        client.createVolume(env.getMRCAddress().getHostName() + ":" + env.getMRCAddress().getPort(), auth, uc,
                            SOURCE_VOLUME);
        client.createVolume(env.getMRCAddress().getHostName() + ":" + env.getMRCAddress().getPort(), auth, uc,
                            TARGET_VOLUME);

        Volume sourceVolume = client.openVolume(SOURCE_VOLUME, null, new Options());
        Volume targetVolume = client.openVolume(TARGET_VOLUME, null, new Options());

        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_enabled", "1",
                              MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);
        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.trace_target", TARGET_VOLUME,
                              MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);
        sourceVolume.setXAttr(uc, auth, "/", "xtreemfs.tracing_policy", "default",
                              MRC.XATTR_FLAGS.XATTR_FLAGS_CREATE);

        FileHandle f = sourceVolume.openFile(uc, TEST_FILE,
                                             GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber() |
                                             GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber());

        byte[] writeBuffer = new byte[255];

        for (long i=0; i<=100; i++)
            f.write(uc, writeBuffer, writeBuffer.length, i * writeBuffer.length);

        f.close();
        sourceVolume.unlink(uc, TEST_FILE);

        client.deleteVolume(auth, uc, SOURCE_VOLUME);
        client.deleteVolume(auth, uc, TARGET_VOLUME);
    }
}
