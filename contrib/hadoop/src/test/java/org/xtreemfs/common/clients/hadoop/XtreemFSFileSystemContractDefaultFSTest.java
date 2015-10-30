/*
 * Copyright (c) 2015 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

public class XtreemFSFileSystemContractDefaultFSTest extends XtreemFSFileSystemContractTest {

    public XtreemFSFileSystemContractDefaultFSTest() {
        super("/", "xtreemfs://localhost:32638", "xtreemfs://localhost:32636");
        conf.set("fs.defaultFS", "xtreemfs://localhost:32638");
    }

}
