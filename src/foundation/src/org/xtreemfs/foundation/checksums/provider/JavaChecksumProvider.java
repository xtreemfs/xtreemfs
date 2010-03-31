/*
 * Copyright (c) 2008-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Christian Lorenz (ZIB)
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
