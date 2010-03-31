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
 * AUTHORS: Bjoern Kolbeck (ZIB), Jan Stender (ZIB), Felix Langner (ZIB)
 */

package org.xtreemfs.foundation.util;

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
