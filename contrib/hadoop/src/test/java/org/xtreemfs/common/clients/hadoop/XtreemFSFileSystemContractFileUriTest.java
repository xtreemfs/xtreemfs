/*
 * Copyright (c) 2015 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

/**
 * Executes the file system contract tests with XtreemFS initialized from a file
 * URI.
 */
public class XtreemFSFileSystemContractFileUriTest extends XtreemFSFileSystemContractTest {

    public XtreemFSFileSystemContractFileUriTest() {
        super("xtreemfs://localhost:32638/" + DEFAULT_VOLUME_NAME + "/path/to/file",
                "xtreemfs://localhost:32638", "xtreemfs://localhost:32636", false);
    }

}
