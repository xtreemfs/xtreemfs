/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.common.checksums.algorithms;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;

import org.xtreemfs.common.checksums.ChecksumAlgorithm;

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
	public String getValue() {
		String value = Long.toHexString(realAlgorithm.getValue());
		realAlgorithm.reset();
		return value;
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
	public abstract JavaChecksumAlgorithm clone();
}
