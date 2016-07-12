/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.hadoop.fs.contract;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Assert;

/**
 * Utilities used across test cases, trimmed down to accommodate
 * XtreemFSFileSystemContract Test only. Included as source because the Utils
 * are only available in Hadoop 2.5+, and we want to test earlier versions as
 * well.
 */
public class ReducedContractTestUtils extends Assert {

    /**
     * Block any operation on the root path. This is a safety check
     * 
     * @param path
     *            path in the filesystem
     * @param allowRootOperation
     *            can the root directory be manipulated?
     * @throws IOException
     *             if the operation was rejected
     */
    private static void rejectRootOperation(Path path,
            boolean allowRootOperation) throws IOException {
        if (path.isRoot() && !allowRootOperation) {
            throw new IOException("Root directory operation rejected: " + path);
        }
    }

    /**
     * Delete a file/dir and assert that delete() returned true <i>and</i> that
     * the path no longer exists. This variant rejects all operations on root
     * directories
     * 
     * @param fs
     *            filesystem
     * @param file
     *            path to delete
     * @param recursive
     *            flag to enable recursive delete
     * @throws IOException
     *             IO problems
     */
    public static void assertDeleted(FileSystem fs, Path file, boolean recursive)
            throws IOException {
        assertDeleted(fs, file, recursive, false);
    }

    /**
     * Delete a file/dir and assert that delete() returned true <i>and</i> that
     * the path no longer exists. This variant rejects all operations on root
     * directories
     * 
     * @param fs
     *            filesystem
     * @param file
     *            path to delete
     * @param recursive
     *            flag to enable recursive delete
     * @param allowRootOperations
     *            can the root dir be deleted?
     * @throws IOException
     *             IO problems
     */
    public static void assertDeleted(FileSystem fs, Path file,
            boolean recursive, boolean allowRootOperations) throws IOException {
        rejectRootOperation(file, allowRootOperations);
        assertPathExists(fs, "about to be deleted file", file);
        boolean deleted = fs.delete(file, recursive);
        String dir = ls(fs, file.getParent());
        assertTrue("Delete failed on " + file + ": " + dir, deleted);
        assertPathDoesNotExist(fs, "Deleted file", file);
    }

    /**
     * Take an array of filestats and convert to a string (prefixed w/ a [01]
     * counter
     * 
     * @param stats
     *            array of stats
     * @param separator
     *            separator after every entry
     * @return a stringified set
     */
    private static String fileStatsToString(FileStatus[] stats, String separator) {
        StringBuilder buf = new StringBuilder(stats.length * 128);
        for (int i = 0; i < stats.length; i++) {
            buf.append(String.format("[%02d] %s", i, stats[i])).append(
                    separator);
        }
        return buf.toString();
    }

    /**
     * List a directory
     * 
     * @param fileSystem
     *            FS
     * @param path
     *            path
     * @return a directory listing or failure message
     * @throws IOException
     */
    private static String ls(FileSystem fileSystem, Path path)
            throws IOException {
        if (path == null) {
            // surfaces when someone calls getParent() on something at the top
            // of the path
            return "/";
        }
        FileStatus[] stats;
        String pathtext = "ls " + path;
        try {
            stats = fileSystem.listStatus(path);
        } catch (FileNotFoundException e) {
            return pathtext + " -file not found";
        } catch (IOException e) {
            return pathtext + " -failed: " + e;
        }
        return dumpStats(pathtext, stats);
    }

    private static String dumpStats(String pathname, FileStatus[] stats) {
        return pathname + fileStatsToString(stats, "\n");
    }

    /**
     * Assert that a file exists and whose {@link FileStatus} entry declares
     * that this is a file and not a symlink or directory.
     * 
     * @param fileSystem
     *            filesystem to resolve path against
     * @param filename
     *            name of the file
     * @throws IOException
     *             IO problems during file operations
     */
    public static void assertIsFile(FileSystem fileSystem, Path filename)
            throws IOException {
        assertPathExists(fileSystem, "Expected file", filename);
        FileStatus status = fileSystem.getFileStatus(filename);
        assertIsFile(filename, status);
    }

    /**
     * Assert that a file exists and whose {@link FileStatus} entry declares
     * that this is a file and not a symlink or directory.
     * 
     * @param filename
     *            name of the file
     * @param status
     *            file status
     */
    public static void assertIsFile(Path filename, FileStatus status) {
        String fileInfo = filename + "  " + status;
        assertFalse("File claims to be a directory " + fileInfo,
                status.isDirectory());
        assertFalse("File claims to be a symlink " + fileInfo,
                status.isSymlink());
    }

    /**
     * Assert that a path exists -but make no assertions as to the type of that
     * entry
     *
     * @param fileSystem
     *            filesystem to examine
     * @param message
     *            message to include in the assertion failure message
     * @param path
     *            path in the filesystem
     * @throws FileNotFoundException
     *             raised if the path is missing
     * @throws IOException
     *             IO problems
     */
    private static void assertPathExists(FileSystem fileSystem, String message,
            Path path) throws IOException {
        if (!fileSystem.exists(path)) {
            // failure, report it
            ls(fileSystem, path.getParent());
            throw new FileNotFoundException(message + ": not found " + path
                    + " in " + path.getParent());
        }
    }

    /**
     * Assert that a path does not exist
     *
     * @param fileSystem
     *            filesystem to examine
     * @param message
     *            message to include in the assertion failure message
     * @param path
     *            path in the filesystem
     * @throws IOException
     *             IO problems
     */
    private static void assertPathDoesNotExist(FileSystem fileSystem,
            String message, Path path) throws IOException {
        try {
            FileStatus status = fileSystem.getFileStatus(path);
            fail(message + ": unexpectedly found " + path + " as  " + status);
        } catch (FileNotFoundException expected) {
            // this is expected
        }
    }
}