/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
package org.xtreemfs.sandbox.tests.replicationStressTest;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.SortedSet;

import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;

/**
 * one instance will be written once and will be not modified after this (deep) <br>
 * 22.06.2009
 */
class TestFile {
    static String       diskDir;
    static final String DISK_FILENAME = "replicatedFile";
    static final String VOLUME_NAME   = "replicationTestVolume";
    static final String DIR_PATH      = "/replicationTest/";

    private static int  fileNumber    = 1;

    static String getNewFileName() {
        return "test" + fileNumber++;
    }

    static UserCredentials   userCredentials;
    static {
        // user credentials
        StringSet groupIDs = new StringSet();
        groupIDs.add("root");
        userCredentials = new UserCredentials("root", groupIDs, "");
    }

    static long              diskFileFilesize;
    static InetSocketAddress mrcAddress;

    final String             filename;
    final long               filesize;
    /**
     * start offsets of the holes
     */
    final SortedSet<Long>    holes;

    /**
         * 
         */
    public TestFile(String filename, long filesize, SortedSet<Long> holes) {
        this.filename = filename;
        this.filesize = filesize;
        this.holes = holes;
    }

    public void readFromXtreemFS(byte[] buffer, RandomAccessFile in, long startOffset) throws Exception {
        in.seek(startOffset);
        in.read(buffer, 0, buffer.length);
    }

    public void readFromDisk(byte[] buffer, java.io.RandomAccessFile in, long startOffset, long filesize)
            throws Exception {
        int bufferSize = buffer.length;
        long endOffset = startOffset + bufferSize;

        // read data from file
        in.seek(startOffset);
        in.read(buffer);

        // check for holes => modify expected data
        for (Long holeStartOffset : holes) {
            long holeEndOffset = holeStartOffset + StressTest.PART_SIZE;
            if (containsHoleInRange(holeStartOffset, startOffset, endOffset)) {
                int from = ((holeStartOffset - startOffset) < 0) ? 0 : (int) (holeStartOffset - startOffset);
                int to = ((holeEndOffset - startOffset) > bufferSize) ? bufferSize
                        : (int) (holeEndOffset - startOffset);
                // swap some data to zeros
                Arrays.fill(buffer, from, to, (byte) 0);
            }
        }

        // check for EOF => modify expected data
        if (endOffset > filesize) { // EOF with data
            // swap data to zeros
            Arrays.fill(buffer, (int) (filesize - startOffset), bufferSize, (byte) 0);
        }
    }

    public boolean containsHoleInRange(long hole, long startOffset, long endOffset) {
        boolean result = false;
        if ((hole >= startOffset && hole < endOffset) // hole begins in area
                || (hole + StressTest.PART_SIZE > startOffset && hole
                        + StressTest.PART_SIZE <= endOffset) // hole ends in area
                || (hole < startOffset && hole + StressTest.PART_SIZE > endOffset))
            // hole begins ahead and ends after area
            result = true;
        return result;
    }
}
