/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums;

/**
 * An interface for checksum algorithms, which are based on computations on
 * strings.
 * 
 * 02.09.2008
 * 
 * @author clorenz
 */
public interface StringChecksumAlgorithm extends ChecksumAlgorithm {
	/**
	 * Updates checksum with specified data.
	 * 
	 * @param data
	 */
	public void digest(String data);
}
