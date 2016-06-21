/*
 * Copyright (c) 2012 by Lukas Kairies,
 *               2015-2016 by Robert Schmidtke,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.DelegateToFileSystem;
import org.apache.hadoop.fs.FileSystem;

public class XtreemFS extends DelegateToFileSystem {

	protected XtreemFS(URI theUri, FileSystem theFsImpl,
			Configuration conf, String supportedScheme,
			boolean authorityRequired) throws IOException, URISyntaxException {
		super(theUri, theFsImpl, conf, supportedScheme, authorityRequired);
	}

	protected XtreemFS(URI theUri, Configuration conf) throws IOException, URISyntaxException {
		this(theUri, new XtreemFSFileSystem(), conf, theUri.getScheme(), false);		
	}
}
