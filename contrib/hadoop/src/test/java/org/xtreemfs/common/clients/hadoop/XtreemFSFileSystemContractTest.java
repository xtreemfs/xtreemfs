/*
 * Copyright (c) 2015 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

import java.net.URI;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileSystemContractBaseTest;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

public class XtreemFSFileSystemContractTest extends FileSystemContractBaseTest {

    private Client client;

    private UserCredentials userCredentials;

    @Override
    protected void setUp() throws Exception {
        userCredentials = UserCredentials.newBuilder().setUsername("test")
                .addGroups("test").build();
        client = ClientFactory.createClient(ClientFactory.ClientType.NATIVE,
                "localhost:32638", userCredentials, null, new Options());
        client.start();
        client.createVolume("localhost:32636", RPCAuthentication.authNone,
                userCredentials, "hadoopJUnitTest");

        Configuration.addDefaultResource(XtreemFSFileSystemContractTest.class
                .getClassLoader().getResource("core-site.xml").getPath());
        Configuration conf = new Configuration();
        conf.set("test.xtreemfs.defaultVolumeName", "hadoopJUnitTest");
        conf.set("test.xtreemfs.jni.libraryPath", System.getenv("XTREEMFS")
                + "/cpp/build");
        fs = FileSystem.get(URI.create("xtreemfs://localhost:32638"),
                conf);

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        client.deleteVolume("localhost:32636", RPCAuthentication.authNone,
                userCredentials, "hadoopJUnitTest");
        client.shutdown();
    }

}
