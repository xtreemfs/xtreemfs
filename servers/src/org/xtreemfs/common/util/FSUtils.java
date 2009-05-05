/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
 Barcelona Supercomputing Center - Centro Nacional de Supercomputacion.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB), Jesús Malo (BSC)
 */

package org.xtreemfs.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
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
            for (File file : srcFile.listFiles())
                copyTree(file, new File(trgFile, file.getName()));

        } else {

            FileChannel in = null, out = null;

            try {

                try {
                    in = new FileInputStream(srcFile).getChannel();
                    out = new FileOutputStream(trgFile).getChannel();

                    long size = in.size();
                    MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);

                    out.write(buf);

                } finally {
                    if (in != null)
                        in.close();
                    if (out != null)
                        out.close();
                }

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
     * @return the free disk space (for non-privileged users)
     */
    public static long getFreeSpace(String dir) {

        BufferedReader buf = null;

        // try to retrieve the file size via the native 'stat' command
//        try {
//            Process p = Runtime.getRuntime().exec("stat -f --format %a " + dir);
//            buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            long result = Long.parseLong(buf.readLine()) * 4096;
//
//            return result;
//
//        } catch (Exception exc) {
//
//            Logging
//                    .logMessage(Logging.LEVEL_DEBUG, null,
//                        "a problem with 'stat' occurred - command probably not available on local platform");
//            Logging.logMessage(Logging.LEVEL_DEBUG, null,
//                "using the Java mechanism for retrieving free space on the object partition");
//
            // if some problem occurs, use the dedicated Java mechanism instead
            return new File(dir).getUsableSpace();

//        } finally {
//            if (buf != null)
//                try {
//                    buf.close();
//                } catch (IOException e) {
//                    Logging.logMessage(Logging.LEVEL_ERROR, null, e);
//                }
//        }
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
