/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums.algorithms;

import java.nio.ByteBuffer;

import org.xtreemfs.foundation.checksums.StringChecksumAlgorithm;

/**
 * The Java algorithm, which is used for string.hashCode(). It uses the Java
 * internal implementation.
 * 
 * 02.09.2008
 * 
 * @author clorenz
 */
public class JavaHash implements StringChecksumAlgorithm {
	private Long hash = null;

	private String name = "Java-Hash";

	/**
	 * Updates checksum with specified data.
	 * 
	 * @param data
	 */
	public void digest(String data) {
		this.hash = Long.valueOf(data.hashCode());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.foundation.checksums.ChecksumAlgorithm#digest(java.nio.ByteBuffer)
	 */
	@Override
	public void update(ByteBuffer data) {
		byte[] array;

		if (data.hasArray()) {
			array = data.array();
		} else {
			array = new byte[data.capacity()];
			final int oldPos = data.position();
			data.position(0);
			data.get(array);
			data.position(oldPos);
		}

		this.hash = (long)new String(array).hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.foundation.checksums.ChecksumAlgorithm#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.foundation.checksums.ChecksumAlgorithm#getValue()
	 */
	@Override
	public long getValue() {
		long value;
		if (this.hash != null)
			value = this.hash;
		else
			value = 0;
		reset();
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.foundation.checksums.ChecksumAlgorithm#reset()
	 */
	@Override
	public void reset() {
		hash = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.common.checksum.ChecksumAlgorithm#clone()
	 */
	@Override
	public JavaHash clone() {
		return new JavaHash();
	}
}
