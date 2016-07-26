/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox;

import java.io.File;
import java.io.RandomAccessFile;

/**
 *
 * @author bjko
 */
public class writeTruncTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        try {
            final String path = (args.length > 0) ? args[0] : "/tmp";
            final int    numObjs = (args.length > 1) ? Integer.valueOf(args[1]) : 8*1024;
            File f = new File(path+"/xtreemfs_testfile");
            f.delete();
            RandomAccessFile rf = new RandomAccessFile(f, "rw");
            long tStart = System.currentTimeMillis();
            long cnt = 0;
            long fsize = 0;
            for (int i = 0; i < 64*1024; i++) {
                if (cnt % 4096 == 0) {
                    fsize += 4096;
                    rf.setLength(fsize);
                }
                rf.writeLong(8l);
                rf.writeLong(64564516l);
                cnt += 16;

            }
            long tEnd = System.currentTimeMillis();
            System.out.println("duration (w/ trunc): "+(tEnd-tStart)+" ms");
            rf.close();

            f = new File(path+"/xtreemfs_testfile");
            f.delete();
            rf = new RandomAccessFile(f, "rw");
            tStart = System.currentTimeMillis();
            for (int i = 0; i < 64*1024; i++) {
                rf.writeLong(8l);
                rf.writeLong(64564516l);
            }
            tEnd = System.currentTimeMillis();
            System.out.println("duration (no trunc): "+(tEnd-tStart)+" ms");
            rf.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
