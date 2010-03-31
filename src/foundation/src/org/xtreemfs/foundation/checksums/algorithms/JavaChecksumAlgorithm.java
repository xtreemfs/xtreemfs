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
