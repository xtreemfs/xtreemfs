/*
 * Copyright (c) 2015 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

import java.net.URL;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Hadoop1FileSystemContractBaseTest;
import org.apache.hadoop.fs.Path;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 * Executes Hadoop 1 file system contract tests with XtreemFS initialized from
 * <code>xtreemfs.defaultVolumeName</code>.
 */
public final class XtreemFSHadoop1FileSystemContractTest extends Hadoop1FileSystemContractBaseTest {

    private final Client client;

    private final UserCredentials userCredentials;

    private final Path fileSystemPath;

    private final Path dirPath, mrcPath;

    private static final String DEFAULT_VOLUME_NAME = "hadoopJUnitTest",
            UNUSED_VOLUME_NAME = DEFAULT_VOLUME_NAME + "_";

    private final Configuration conf;

    public XtreemFSHadoop1FileSystemContractTest() {
        this.fileSystemPath = new Path("xtreemfs://localhost:32638");
        this.dirPath = new Path("xtreemfs://localhost:32638");
        this.mrcPath = new Path("xtreemfs://localhost:32636");

        URL coreSiteUrl = XtreemFSHadoop1FileSystemContractTest.class.getClassLoader()
                .getResource("core-site.xml");
        if (coreSiteUrl == null) {
            throw new RuntimeException("core-site.xml not found on classpath");
        }

        Configuration.addDefaultResource(coreSiteUrl.getPath());
        conf = new Configuration();

        userCredentials = UserCredentials.newBuilder().setUsername("test")
                .addGroups("test").build();

        client = ClientFactory.createClient(ClientFactory.ClientType.NATIVE,
                dirPath.toUri().getAuthority(), userCredentials, null, new Options());
    }

    @Override
    protected void setUp() throws Exception {
        System.out.println("Executing TestCase: '" + getName() + "'.");

        client.start();
        client.createVolume(mrcPath.toUri().getAuthority(), RPCAuthentication.authNone,
                userCredentials, DEFAULT_VOLUME_NAME);
        client.createVolume(mrcPath.toUri().getAuthority(), RPCAuthentication.authNone,
                userCredentials, UNUSED_VOLUME_NAME);

        conf.set("xtreemfs.jni.libraryPath", System.getenv("XTREEMFS")
                + "/cpp/build");
        conf.set("xtreemfs.defaultVolumeName", DEFAULT_VOLUME_NAME);
        conf.set("xtreemfs.hadoop.version", "1");

        fs = FileSystem.get(fileSystemPath.toUri(), conf);

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        client.deleteVolume(mrcPath.toUri().getAuthority(), RPCAuthentication.authNone,
                userCredentials, DEFAULT_VOLUME_NAME);
        client.deleteVolume(mrcPath.toUri().getAuthority(), RPCAuthentication.authNone,
                userCredentials, UNUSED_VOLUME_NAME);
        client.shutdown();
    }

}
