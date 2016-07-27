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
 * The SDBM algorithm.
 * 
 * 02.09.2008
 * 
 * @author clorenz
 */
public class SDBM implements StringChecksumAlgorithm {
	private Long hash = null;

	private String name = "SDBM";

	/**
	 * Updates checksum with specified data.
	 * 
	 * @param data
	 */
	public void digest(String data) {
		this.hash = sdbmHash(data);
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

		this.hash = sdbmHash(new String(array));
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
	public SDBM clone() {
		return new SDBM();
	}

	/**
	 * SDBM algorithm
	 * 
	 * @param str
	 * @return
	 */
	protected static long sdbmHash(String str) {
		long hash = 0;
		for (int c : str.toCharArray()) {
			hash = c + (hash << 6) + (hash << 16) - hash;
		}
		return hash;
	}
}
