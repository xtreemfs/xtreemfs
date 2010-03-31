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
