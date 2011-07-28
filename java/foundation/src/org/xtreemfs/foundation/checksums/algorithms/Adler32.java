/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums.algorithms;

/**
 * The Adler32 algorithm. It uses the Java internal implementation.
 * 
 * 19.08.2008
 * 
 * @author clorenz
 */
@SuppressWarnings("unchecked")
public class Adler32 extends JavaChecksumAlgorithm {
    
        public Adler32() {
		super(new java.util.zip.Adler32(), "Adler32");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.common.checksum.ChecksumAlgorithm#clone()
	 */
	@Override
	public Adler32 clone() {
		return new Adler32();
	}
}
