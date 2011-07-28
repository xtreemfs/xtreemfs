/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums.provider;

import org.xtreemfs.foundation.checksums.ChecksumProvider;

/**
 * A provider for Java internal checksums. offers the following algorithms:
 * Adler32, CRC32, MD5, Java-Hash
 * 
 * 19.08.2008
 * 
 * @author clorenz
 */
public class JavaChecksumProvider extends ChecksumProvider {
	private static String NAME = "Java Checksum Provider";

	/**
	 * creates a new JavaChecksumProvider
	 */
	public JavaChecksumProvider() {
		super();

		addAlgorithm(new org.xtreemfs.foundation.checksums.algorithms.Adler32());
		addAlgorithm(new org.xtreemfs.foundation.checksums.algorithms.CRC32());
		/*try {
			addAlgorithm(new org.xtreemfs.foundation.checksums.algorithms.JavaMessageDigestAlgorithm(
					"MD5", "MD5"));
			addAlgorithm(new org.xtreemfs.foundation.checksums.algorithms.JavaMessageDigestAlgorithm(
					"SHA1", "SHA-1"));
		} catch (NoSuchAlgorithmException e) {
			Logging.logMessage(Logging.LEVEL_WARN, this, e.getMessage()
					+ " in your java-installation");
		}*/
		addAlgorithm(new org.xtreemfs.foundation.checksums.algorithms.JavaHash());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.foundation.checksums.ChecksumProvider#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}
}
