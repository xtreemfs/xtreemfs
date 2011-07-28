/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums;

import java.nio.ByteBuffer;

/**
 * An interface which must be implemented by checksum algorithms for XtreemFS.
 * 
 * 19.08.2008
 * 
 * @author clorenz
 */
public interface ChecksumAlgorithm extends Cloneable {
	/**
	 * Returns a string that identifies the algorithm, independent of
	 * implementation details.
	 * 
	 * @return name of algorithm
	 */
	public String getName();

	/**
	 * Returns checksum value (as Hex-String) and resets the Algorithm.
	 * 
	 * @return checksum
	 */
	public long getValue();

	/**
	 * Resets checksum to initial value.
	 * 
	 * @return
	 */
	public void reset();

	/**
	 * Updates checksum with specified data.
	 * 
	 * @param data
	 */
	public void update(ByteBuffer data);

	/**
	 * returns a new instance of the checksum algorithm
	 * 
	 * @return
	 */
	public ChecksumAlgorithm clone();
}
