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

public class XtreemFSFileSystemContractTest extends FileSystemContractBaseTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Configuration.addDefaultResource(XtreemFSFileSystemContractTest.class
                .getClassLoader().getResource("core-site.xml").getPath());
        fs = FileSystem.get(URI.create("xtreemfs://localhost:32638"),
                new Configuration());
    }

}
