/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.common.checksums.provider;

import java.security.NoSuchAlgorithmException;

import org.xtreemfs.common.checksums.ChecksumProvider;
import org.xtreemfs.common.logging.Logging;

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

		addAlgorithm(new org.xtreemfs.common.checksums.algorithms.Adler32());
		addAlgorithm(new org.xtreemfs.common.checksums.algorithms.CRC32());
		/*try {
			addAlgorithm(new org.xtreemfs.common.checksums.algorithms.JavaMessageDigestAlgorithm(
					"MD5", "MD5"));
			addAlgorithm(new org.xtreemfs.common.checksums.algorithms.JavaMessageDigestAlgorithm(
					"SHA1", "SHA-1"));
		} catch (NoSuchAlgorithmException e) {
			Logging.logMessage(Logging.LEVEL_WARN, this, e.getMessage()
					+ " in your java-installation");
		}*/
		addAlgorithm(new org.xtreemfs.common.checksums.algorithms.JavaHash());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.common.checksums.ChecksumProvider#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}
}
