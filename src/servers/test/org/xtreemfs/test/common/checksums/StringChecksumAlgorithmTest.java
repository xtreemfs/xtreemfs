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
package org.xtreemfs.test.common.checksums;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.xtreemfs.common.checksums.StringChecksumAlgorithm;
import org.xtreemfs.common.checksums.algorithms.SDBM;
import org.xtreemfs.common.logging.Logging;

/**
 * some tests for the checksum algorithms, which are based on strings
 *
 * 02.09.2008
 *
 * @author clorenz
 */
public class StringChecksumAlgorithmTest extends TestCase {
	private ByteBuffer bufferData;
	private String stringData;

	@Before
	public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "."
                + getName());
		Logging.start(Logging.LEVEL_ERROR);

		this.stringData = "";
		for(int i=0; i<1024; i++){
			this.stringData += "Test, ";
		}
		this.bufferData = ByteBuffer.wrap(stringData.getBytes());
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * tests, if the SDBM algorithm generates the same checksum with
	 * a String-input and ByteBuffer-input
	 * @throws Exception
	 */
	public void testSDBMStringBufferEquality() throws Exception {
		// compute checksum with xtreemfs ChecksumFactory
		StringChecksumAlgorithm algorithm = new SDBM();

		// string
		algorithm.digest(stringData);
		long stringValue = algorithm.getValue();

		// buffer
		algorithm.update(bufferData);
		long bufferValue = algorithm.getValue();

//		System.out.println(stringValue);
//		System.out.println(bufferValue);

		assertEquals(stringValue, bufferValue);
	}
}
