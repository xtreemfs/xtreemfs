/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums.algorithms;

/**
 * The CRC32 algorithm. It uses the Java internal implementation.
 * 
 * 19.08.2008
 * 
 * @author clorenz
 */
@SuppressWarnings("unchecked")
public class CRC32 extends JavaChecksumAlgorithm {
    
	public CRC32() {
		super(new java.util.zip.CRC32(), "CRC32");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.common.checksum.ChecksumAlgorithm#clone()
	 */
	@Override
	public CRC32 clone() {
		return new CRC32();
	}
}
