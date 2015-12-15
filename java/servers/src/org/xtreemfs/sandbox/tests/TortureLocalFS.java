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
 * AUTHORS: Björn Kolbeck (ZIB)
 */


package org.xtreemfs.sandbox.tests;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.CLIParser.CliOption;

/**
 *
 * @author bjko
 */
public class TortureLocalFS {

    public static void main(String[] args) {
        try {

            Map<String, CliOption> options = new HashMap<String, CliOption>();
            List<String> arguments = new ArrayList<String>(0);
            options.put("p", new CliOption(CliOption.OPTIONTYPE.STRING));
            options.put("r", new CliOption(CliOption.OPTIONTYPE.SWITCH));

            CLIParser.parseCLI(args, options, arguments);

            Logging.start(Logging.LEVEL_WARN);
            TimeSync.initialize(null, 10000, 50);

            if (arguments.size() != 0) {
                usage();
                return;
            }


            final String path = (options.get("p").stringValue != null) ?
                                options.get("p").stringValue : "./torture.data";


            final int MIN_FS = 64*1024;
            final int MAX_FS = 512*1024*1024;

            final int MIN_REC = 4*1024;
            final int MAX_REC = 1024*1024;

            if (options.get("r").switchValue == false) {
                for (int fsize = MIN_FS; fsize <= MAX_FS; fsize = fsize * 2) {
                    for (int recsize = MIN_REC; recsize <= MAX_REC; recsize = recsize *2) {
                        if (testSequential(fsize, recsize, path)) {
                            continue;
                        }
                    }
                }
            }
            
            System.out.println("\nrandom test\n");

            for (int fsize = MIN_FS; fsize <= MAX_FS; fsize = fsize * 2) {
                for (int recsize = MIN_REC; recsize <= MAX_REC; recsize = recsize *2) {
                    if (testRandom(fsize, recsize, path)) {
                        continue;
                    }
                }
            }

            System.out.println("finished");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

    }

    private static boolean testSequential(int fsize, int recsize, final String path) throws InterruptedException, Exception, IOException {
        final int numRecs = fsize / recsize;
        if (numRecs == 0) {
            return true;
        }
        byte[] sendBuffer = new byte[recsize];
        for (int i = 0; i < recsize; i++) {
            sendBuffer[i] = (byte) ((i%26) + 65);
        }
        long tStart = System.currentTimeMillis();
        RandomAccessFile raf = new RandomAccessFile(path,"rw");
        long tOpen = System.currentTimeMillis();
        long bytesWritten = 0;
        //do writes
        long tWrite = 0;
        for (int rec = 0; rec < numRecs; rec++) {
            long tmpStart = System.currentTimeMillis();
            raf.write(sendBuffer, 0, recsize);
            bytesWritten += recsize;
            tWrite += System.currentTimeMillis()-tmpStart;
        }
        assert (bytesWritten == numRecs * recsize);
        raf.getFD().sync();
        raf.seek(0);
        final long tFlush = System.currentTimeMillis();
        long tRead = 0;
        //do writes
        byte[] readBuffer = new byte[recsize];
        for (int rec = 0; rec < numRecs; rec++) {
            long tmpStart = System.currentTimeMillis();
            final int bytesRead = raf.read(readBuffer, 0, recsize);
            tRead += System.currentTimeMillis()-tmpStart;
            if (bytesRead != recsize) {
                System.out.println("PREMATURE END-OF-FILE AT " + rec * recsize);
                System.out.println("expected " + recsize + " bytes");
                System.out.println("got " + bytesRead + " bytes");
                System.exit(1);
            }
            for (int i = 0; i < recsize; i++) {
                if (readBuffer[i] != (byte) ((i%26) + 65)) {
                    System.out.println("INVALID CONTENT AT " + (rec * recsize + i));
                    System.out.println("expected:  " + (byte) ((i%26) + 65));
                    System.out.println("got     : " + readBuffer[i]);
                    System.exit(1);
                }
            }
        }
        raf.close();
        
        File f = new File(path);
        f.delete();
        final long tDelete = System.currentTimeMillis();
        double writeRate = ((double) fsize) / 1024.0 / (((double) (tWrite)) / 1000.0);
        double readRate = ((double) fsize) / 1024.0 / (((double) (tRead)) / 1000.0);
        System.out.format("fs: %8d   bs: %8d    write: %6d ms   %6.0f kb/s    read: %6d ms   %6.0f kb/s\n", fsize / 1024, recsize, tWrite, writeRate, tRead, readRate);
        return false;
    }

    private static boolean testRandom(int fsize, int recsize, final String path) throws InterruptedException, Exception, IOException {
        final int numRecs = fsize / recsize;
        int[] skips = new int[numRecs];
        if (numRecs == 0) {
            return true;
        }
        byte[] sendBuffer = new byte[recsize];
        for (int i = 0; i < recsize; i++) {
            sendBuffer[i] = (byte) ((i%26) + 65);
        }
        long tStart = System.currentTimeMillis();
        RandomAccessFile raf = new RandomAccessFile(path,"rw");
        long tOpen = System.currentTimeMillis();
        long bytesWritten = 0;
        long tWrite = 0;
        //do writes
        for (int rec = 0; rec < numRecs; rec++) {
            skips[rec] = (int) (Math.random()*((double)recsize));
            raf.seek(raf.getFilePointer()+skips[rec]);
            long tmpStart = System.currentTimeMillis();
            raf.write(sendBuffer, 0, recsize);
            bytesWritten += recsize;
            tWrite += System.currentTimeMillis()-tmpStart;
        }
        if (bytesWritten != numRecs * recsize) {
            System.out.println("not all data was written!");
            System.exit(1);
        }
        raf.getFD().sync();
        raf.seek(0);
        final long tFlush = System.currentTimeMillis();
        long tRead = 0;
        //do writes
        byte[] readBuffer = new byte[recsize];
        for (int rec = 0; rec < numRecs; rec++) {
            raf.seek(raf.getFilePointer()+skips[rec]);
            long tmpStart = System.currentTimeMillis();
            final int bytesRead = raf.read(readBuffer, 0, recsize);
            tRead += System.currentTimeMillis()-tmpStart;
            if (bytesRead != recsize) {
                System.out.println("PREMATURE END-OF-FILE AT " + rec * recsize);
                System.out.println("expected " + recsize + " bytes");
                System.out.println("got " + bytesRead + " bytes");
                System.exit(1);
            }
            for (int i = 0; i < recsize; i++) {
                if (readBuffer[i] != (byte) ((i%26) + 65)) {
                    System.out.println("INVALID CONTENT AT " + (rec * recsize + i));
                    System.out.println("expected:  " + (byte) ((i%26) + 65));
                    System.out.println("got     : " + readBuffer[i]);
                    System.exit(1);
                }
            }
        }
        raf.close();

        File f = new File(path);
        f.delete();
        final long tDelete = System.currentTimeMillis();
        double writeRate = ((double) fsize) / 1024.0 / (((double) (tWrite)) / 1000.0);
        double readRate = ((double) fsize) / 1024.0 / (((double) (tRead)) / 1000.0);
        System.out.format("fs: %8d   bs: %8d    write: %6d ms   %6.0f kb/s    read: %6d ms   %6.0f kb/s\n", fsize / 1024, recsize, tWrite, writeRate, tRead, readRate);
        return false;
    }


    private static void usage() {
        System.out.println("usage: torture [options]");
        System.out.println("  -p <path>   filename to use for measurements (default: /torture.dat)");
        System.out.println("  -r random only");
        System.out.println("  -h        show usage info");
    }

}
