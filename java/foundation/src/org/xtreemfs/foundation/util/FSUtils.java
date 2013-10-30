/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Jan Stender,
 *               Felix Langner, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * A class containing helper functions for working with the local file system.
 *
 * @author stender
 */
public class FSUtils {

    /**
     * Recursively deletes all contents of the given directory.
     *
     * @param file
     *            the directory to delete
     */
    public static void delTree(File file) {

        if (!file.exists())
            return;

        File[] fileList;
        if ((fileList = file.listFiles())!=null){
            for (File f : fileList) {
                if (f.isDirectory())
                    delTree(f);
                else
                    f.delete();
            }
        }
        
        file.delete();
    }

    /**
     * Copies a whole directory tree to another directory.
     *
     * @param srcFile
     *            the source tree
     * @param trgFile
     *            the target point where to copy the source tree
     * @throws IOException
     *             if an I/O error occurs
     */
    public static void copyTree(File srcFile, File trgFile) throws IOException {

        if (srcFile.isDirectory()) {

            trgFile.mkdir();
            for (File file : srcFile.listFiles()) {
                copyTree(file, new File(trgFile, file.getName()));
            }
        } else {

            FileChannel in = null, out = null;
            try {
                in = new FileInputStream(srcFile).getChannel();
                out = new FileOutputStream(trgFile).getChannel();

                in.transferTo(0, in.size(), out);
            } finally {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            }
        }
    }

    /**
     * Returns the free disk space on the partition storing the given directory.
     *
     * @param dir
     *            the directory stored in the partition
     * @return the free disk space
     */
    public static long getFreeSpace(String dir) {
        return new File(dir).getFreeSpace();
    }

    /**
     * Returns the available disk space on the partition storing the given directory.
     *
     * @param dir
     *            the directory stored in the partition
     * @return the available disk space (for non-privileged users)
     */
    public static long getUsableSpace(String dir) {
        return new File(dir).getUsableSpace();
    }

    public static File[] listRecursively(File rootDir, FileFilter filter) {
        List<File> list = new ArrayList<File>();
        listRecursively(rootDir, filter, list);
        return list.toArray(new File[list.size()]);
    }

    private static void listRecursively(File rootDir, FileFilter filter, List<File> list) {

        if (!rootDir.exists())
            return;

        // first, all files in subdirectories
        File[] nestedDirs = rootDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (File dir : nestedDirs)
            listRecursively(dir, filter, list);

        for (File f : rootDir.listFiles(filter))
            list.add(f);
    }
}
