/*
 * Copyright (c) 2015 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileSystemContractBaseTest;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 * Executes file system contract tests with XtreemFS initialized from
 * <code>xtreemfs.defaultVolumeName</code>.
 */
public class XtreemFSFileSystemContractTest extends FileSystemContractBaseTest {

    protected final Client client;

    protected final UserCredentials userCredentials;

    protected final Path fileSystemPath;

    protected final Path dirPath, mrcPath;

    protected static final String DEFAULT_VOLUME_NAME = "hadoopJUnitTest",
            UNUSED_VOLUME_NAME = DEFAULT_VOLUME_NAME + "_";

    protected final Configuration conf;

    public XtreemFSFileSystemContractTest() {
        this("xtreemfs://localhost:32638", "xtreemfs://localhost:32638",
                "xtreemfs://localhost:32636", true);
    }

    protected XtreemFSFileSystemContractTest(String fileSystemUri,
            String dirUri, String mrcUri, boolean setXtreemFSDefaultVolumeName) {
        this.fileSystemPath = new Path(fileSystemUri);
        this.dirPath = new Path(dirUri);
        this.mrcPath = new Path(mrcUri);

        Configuration.addDefaultResource(XtreemFSFileSystemContractTest.class
                .getClassLoader().getResource("core-site.xml").getPath());
        conf = new Configuration();
        conf.set("xtreemfs.jni.libraryPath", System.getenv("XTREEMFS")
                + "/cpp/build");
        if (setXtreemFSDefaultVolumeName) {
            conf.set("xtreemfs.defaultVolumeName", DEFAULT_VOLUME_NAME);
        }

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

        fs = FileSystem.get(fileSystemPath.toUri(), conf);

        super.setUp();
    }

    /**
     * Tests proper handling of correct URIs, in particular:<br />
     * <code>
     * xtreemfs://server:port/volume/path/to/file<br />
     * xtreemfs://server/volume/path/to/file<br />
     * xtreemfs:/path/to/file<br />
     * //server:port/volume/path/to/file<br />
     * //server/volume/path/to/file<br />
     * /path/to/file<br />
     * path/to/file<br />
     * ./path/to/file<br />
     * ../path/to/file
     * </code>
     *
     * @throws Exception
     */
    public void testFileURIs() throws Exception {
        String expected = "xtreemfs://" + dirPath.toUri().getAuthority() + "/"
                + DEFAULT_VOLUME_NAME + "/path/to/file";

        // xtreemfs://localhost:32638/hadoopJUnitTest/path/to/file
        assertFileCreated(expected, expected);

        // xtreemfs://localhost/hadoopJUnitTest/path/to/file
        assertFileCreated(
                "xtreemfs://" + dirPath.toUri().getHost() + "/" + DEFAULT_VOLUME_NAME + "/path/to/file",
                expected);

        // xtreemfs:/path/to/file
        assertFileCreated("xtreemfs:/path/to/file", expected);

        // //localhost:32638/hadoopJUnitTest/path/to/file
        assertFileCreated(
                "//" + dirPath.toUri().getAuthority() + "/" + DEFAULT_VOLUME_NAME + "/path/to/file",
                expected);

        // //localhost/hadoopJUnitTest/path/to/file
        assertFileCreated(
                "//" + dirPath.toUri().getHost() + "/" + DEFAULT_VOLUME_NAME + "/path/to/file",
                expected);

        // /path/to/file
        assertFileCreated("/path/to/file", expected);

        expected = "xtreemfs://" + dirPath.toUri().getAuthority() + "/"
                + DEFAULT_VOLUME_NAME + "/user/" + System.getProperty("user.name")
                + "/path/to/file";

        // path/to/file
        assertFileCreated("path/to/file", expected);

        // ./path/to/file
        assertFileCreated("./path/to/file", expected);

        expected = "xtreemfs://" + dirPath.toUri().getAuthority() + "/"
                + DEFAULT_VOLUME_NAME + "/user/path/to/file";

        // ../path/to/file
        assertFileCreated("../path/to/file", expected);
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

    protected void assertFileCreated(String path, String expected) throws IOException {
        FSDataOutputStream file = fs.create(new Path(path));
        ContractTestUtils.assertIsFile(fs, new Path(expected));
        ContractTestUtils.assertDeleted(fs, new Path(expected), true);
        file.close();
    }

}
