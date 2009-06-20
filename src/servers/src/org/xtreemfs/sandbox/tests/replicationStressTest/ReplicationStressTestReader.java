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
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.interfaces.UserCredentials;

/**
 * Reader Thread (Client)
 * <br>
 * 08.06.2009
 */
public abstract class ReplicationStressTestReader implements Runnable {
    /**
     * one instance will be written once and will be not modified after this (deep)
     */
    public static class FileInfo {
        final String    filename;
        /**
         * start offsets of the holes
         */
        SortedSet<Long> holes;

        /**
         * 
         */
        public FileInfo(String filename) {
            this.filename = filename;
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
                long holeEndOffset = holeStartOffset + ReplicationStressTest.PART_SIZE;
                if (containsHoleInRange(holeStartOffset, startOffset, endOffset)) {
                    int from = ((holeStartOffset - startOffset) < 0) ? 0
                            : (int) (holeStartOffset - startOffset);
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
                    || (hole + ReplicationStressTest.PART_SIZE > startOffset && hole
                            + ReplicationStressTest.PART_SIZE <= endOffset) // hole ends in area
                    || (hole < startOffset && hole + ReplicationStressTest.PART_SIZE > endOffset)) // hole begins ahead and ends
                // after area
                result = true;
            return result;
        }
    }

    static final int                         SLEEP_TIME = 1000 * 1; // sleep 1 second

    protected final UserCredentials          userCredentials;
    protected final RPCNIOSocketClient       client;
    protected InetSocketAddress              mrcAddress;
    int                                      threadNo;

    protected CopyOnWriteArrayList<FileInfo> fileList;
    protected Random                         random;

    public ReplicationStressTestReader(int threadNo, InetSocketAddress mrcAddress,
            CopyOnWriteArrayList<FileInfo> fileList, Random random, UserCredentials userCredentials)
            throws Exception {
        this.mrcAddress = mrcAddress;
        this.fileList = fileList;
        this.random = random;
        this.userCredentials = userCredentials;
        this.threadNo = threadNo;

        client = new RPCNIOSocketClient(null, 10000, 5 * 60 * 1000);
        client.start();
        client.waitForStartup();
    }
    
    protected abstract double readFile(FileInfo file) throws Exception;

    public void shutdown() throws Exception {
        client.shutdown();
        client.waitForShutdown();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("ReaderThread " + threadNo);
        Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this, "%s started (waiting)", Thread
                .currentThread().getName());

        // wait until first file is written
        try {
            while (fileList.isEmpty())
                Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e1) {
        }

        int fileCounter = 0;
        double throughputSum = 0;
        while (!Thread.interrupted()) {
            try {
                // get any file from list
                FileInfo file = fileList.get(random.nextInt(fileList.size()));
                throughputSum += readFile(file); // KB/s

                if (++fileCounter % 100 == 0) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.test, this,
                            "%s has read %d files. Average throughput: %d KB/s.",
                            Thread.currentThread().getName(), fileCounter,
                            Math.round((throughputSum / 100) * 1000 ) / 1000); // KB/s
                    throughputSum = 0; // reset
                }

                // sleep some time, so the OSDs will not overload
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        // shutdown
        try {
            shutdown();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void log(String message, FileInfo file, long startOffset, long endOffset, long filesize,
            byte[] read, byte[] expected) {
        System.out
                .println("#####################################################################################################");
        Logging.logMessage(Logging.LEVEL_ERROR, Category.test, this, "ERROR: " + message + " Read file "
                + file.filename + " with filesize " + filesize + " from byte " + startOffset + " to byte "
                + endOffset + ".");
        System.out.println("read data contains " + countZeros(read) + " zeros.");
        System.out.println("expected data contains " + countZeros(expected) + " zeros.");

        StringBuffer sb = new StringBuffer();
        sb.append("offsets of holes (start-end): ");
        for (Long holeStartOffset : file.holes) {
            sb.append(holeStartOffset + "-" + (holeStartOffset + ReplicationStressTest.PART_SIZE));
            sb.append(", ");
        }
        System.out.println(sb.toString());

        System.out.println("first 32 bytes read:\t\t" + Arrays.toString(Arrays.copyOfRange(read, 0, 32)));
        System.out.println("first 32 bytes expected:\t"
                + Arrays.toString(Arrays.copyOfRange(expected, 0, 32)));
        System.out.println("last 32 bytes read:\t"
                + Arrays.toString(Arrays.copyOfRange(read, read.length - 32, read.length)));
        System.out.println("last 32 bytes expected:\t"
                + Arrays.toString(Arrays.copyOfRange(expected, expected.length - 32, expected.length)));

        for (Long holeStartOffset : file.holes) {
            long holeEndOffset = holeStartOffset + ReplicationStressTest.PART_SIZE;
            if (holeStartOffset >= startOffset && holeStartOffset <= endOffset) { // hole begins in area
                System.out.println("bytes (read) around hole start at position:"
                        + holeStartOffset
                        + "\t"
                        + Arrays.toString(Arrays.copyOfRange(read, (int) (holeStartOffset - startOffset) - 5,
                                (int) (holeStartOffset - startOffset) + 5)));
                System.out.println("bytes (expected) around hole start at position:"
                        + holeStartOffset
                        + "\t"
                        + Arrays.toString(Arrays.copyOfRange(read, (int) (holeStartOffset - startOffset) - 5,
                                (int) (holeStartOffset - startOffset) + 5)));
            }
            if (holeEndOffset >= startOffset && holeEndOffset <= endOffset) { // hole ends in area
                System.out.println("bytes (read) around hole end at position:"
                        + holeEndOffset
                        + "\t"
                        + Arrays.toString(Arrays.copyOfRange(read, (int) (holeEndOffset - startOffset) - 5,
                                (int) (holeEndOffset - startOffset) + 5)));
                System.out.println("bytes (expected) around hole end at position:"
                        + holeEndOffset
                        + "\t"
                        + Arrays.toString(Arrays.copyOfRange(read, (int) (holeEndOffset - startOffset) - 5,
                                (int) (holeEndOffset - startOffset) + 5)));
            }
        }
        System.out
                .println("#####################################################################################################");
    }

    private int countZeros(byte[] array) {
        int counter = 0;
        for (byte b : array) {
            if (b == 0)
                counter++;
        }
        return counter;
    }
}
