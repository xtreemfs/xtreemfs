/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.foundation.checksums.StringChecksumAlgorithm;
import org.xtreemfs.foundation.checksums.algorithms.SDBM;
import org.xtreemfs.foundation.logging.Logging;

/**
 * some tests for the checksum algorithms, which are based on strings
 *
 * 02.09.2008
 *
 * @author clorenz
 */
public class StringChecksumAlgorithmTest {
    private ByteBuffer bufferData;
    private String     stringData;

    @Before
    public void setUp() throws Exception {
        Logging.start(Logging.LEVEL_ERROR);

        this.stringData = "";
        for (int i = 0; i < 1024; i++) {
            this.stringData += "Test, ";
        }
        this.bufferData = ByteBuffer.wrap(stringData.getBytes());
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * tests, if the SDBM algorithm generates the same checksum with a String-input and ByteBuffer-input
     * 
     * @throws Exception
     */
    @Test
    public void testSDBMStringBufferEquality() throws Exception {
        // compute checksum with xtreemfs ChecksumFactory
        StringChecksumAlgorithm algorithm = new SDBM();

        // string
        algorithm.digest(stringData);
        long stringValue = algorithm.getValue();

        // buffer
        algorithm.update(bufferData);
        long bufferValue = algorithm.getValue();

        // System.out.println(stringValue);
        // System.out.println(bufferValue);

        assertEquals(stringValue, bufferValue);
    }
}
