package org.xtreemfs.common.clients.hadoop;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.DelegateToFileSystem;
import org.apache.hadoop.fs.FileSystem;

public class XtfsFileSystem extends DelegateToFileSystem {

	protected XtfsFileSystem(URI theUri, FileSystem theFsImpl,
			Configuration conf, String supportedScheme,
			boolean authorityRequired) throws IOException, URISyntaxException {
		super(theUri, theFsImpl, conf, supportedScheme, authorityRequired);
	}

	protected XtfsFileSystem(URI theUri, Configuration conf) throws IOException, URISyntaxException {
		this(theUri, new XtreemFSFileSystem(), conf, theUri.getScheme(), false);		
	}
}
