/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums.algorithms;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;

import org.xtreemfs.foundation.checksums.ChecksumAlgorithm;

/**
 * An abstract wrapper for Java internal checksums.
 * 
 * 19.08.2008
 * 
 * @author clorenz
 */
abstract public class JavaChecksumAlgorithm<RealJavaAlgorithm extends Checksum>
		implements ChecksumAlgorithm {
	/**
	 * the class, which really implements the selected algorithm
	 */
	protected RealJavaAlgorithm realAlgorithm;

	protected String name;

	public JavaChecksumAlgorithm(RealJavaAlgorithm realAlgorithm, String name) {
		super();
		this.realAlgorithm = realAlgorithm;
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.common.checksum.ChecksumAlgorithm#digest(java.nio.ByteBuffer)
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

		realAlgorithm.update(array, 0, array.length);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.common.checksum.ChecksumAlgorithm#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.common.checksum.ChecksumAlgorithm#getValue()
	 */
	@Override
	public long getValue() {
		final long tmp = realAlgorithm.getValue();
		realAlgorithm.reset();
		return tmp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.common.checksum.ChecksumAlgorithm#reset()
	 */
	@Override
	public void reset() {
		realAlgorithm.reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.common.checksum.ChecksumAlgorithm#clone()
	 */
	@Override
	public abstract JavaChecksumAlgorithm<?> clone();
}
