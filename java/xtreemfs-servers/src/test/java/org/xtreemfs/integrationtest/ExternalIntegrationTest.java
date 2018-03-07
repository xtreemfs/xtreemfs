/*
 * Copyright (c) 2008-2011 by Jan Stender, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.integrationtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.test.TestHelper;

/**
 * This test case externally tests the integration of all XtreemFS components at
 * the vnode layer. It requires a complete XtreemFS infrastructure consisting of
 * a Directory Service, at least one MRC, at least one OSD and the Access Layer
 * with a local mountpoint. Moreover, a volume named "Test" has to exist.
 * <p>
 * In order to set up a valid environment, take the following steps:
 * <ul>
 * <li>start a Directory Serivce (e.g. on localhost:32638)
 * <li>start an OSD (e.g. on localhost:32637)
 * <li>start an MRC (e.g. on localhost:32636)
 * <li>create a directory for the XtreemFS root (e.g. /tmp/xtreemfs)
 * <li>mount the access layer (e.g.
 * <tt>xtreemfs -d -o volume_url=http://localhost:32636/Test,direct_io /tmp/xtreemfs</tt>)
 * <li>create a volume "Test" (e.g. <tt>mkvol http://localhost:32636/Test</tt>)
 * <li>change the 'xtreemFSMountPoint' variable to the mount point and compile
 * this test case
 * </ul>
 * 
 * @author stender
 * 
 */
public class ExternalIntegrationTest {
    @Rule
    public final TestRule testLog            = TestHelper.testLog;
    
    private static File xtreemFSMountPoint = new File("/tmp/xtreemfs");
    
    @Before
    public void setUp() throws Exception {
        FSUtils.delTree(xtreemFSMountPoint);
        xtreemFSMountPoint.mkdirs();
        
        assertEquals(0, xtreemFSMountPoint.list().length);
    }
    
    @After
    public void tearDown() throws Exception {
        
    }
    
    /**
     * Create and delete some files and directories.
     * 
     * @throws Exception
     */
    @Test
    public void testCreateDelete() throws Exception {
        
        assertEquals(0, xtreemFSMountPoint.listFiles().length);
        
        // create a new directory
        File dir1 = createDir(xtreemFSMountPoint, "/testDir");
        
        // create a path of depth 3 in the root directory
        File dir2 = createDir(xtreemFSMountPoint, "/someOtherDir");
        File dir3 = createDir(dir2, "/nestedDir");
        File dir4 = createDir(dir3, "/leafDir");
        
        // delete the leaf directory
        delete(dir4);
        
        // re-create the leaf directory
        dir4 = createDir(dir4.getParentFile(), dir4.getName());
        
        // create and test a tree of depth 5 with three children per node
        createTree(dir1, 5, 3);
        testTree(dir1, 5, 3);
        
        // create and delete a file in the root directory
        File file1 = createFile(xtreemFSMountPoint, "testfile.tmp");
        
        assertFalse(file1.createNewFile());
        delete(file1);
        createFile(file1.getParentFile(), file1.getName());
        delete(file1);
        
        // create and delete a file in a sub directory
        File file2 = createFile(new File(xtreemFSMountPoint + "/someOtherDir"), "testfile.tmp");
        
        assertFalse(file2.createNewFile());
        delete(file2);
        createFile(file2.getParentFile(), file2.getName());
        delete(file2);
    }
    
    @Test
    public void testRename() throws Exception {
        
        // create a file and a directory
        File sourceFile = createFile(xtreemFSMountPoint, "sourceFile.txt");
        File targetDir = createDir(xtreemFSMountPoint, "targetDir");
        
        // move the file to the directory
        File targetFile = new File(targetDir.getAbsolutePath() + "/sourceFile.txt");
        assertTrue(sourceFile.renameTo(targetFile));
        
        // afterwards, the target file should exist
        assertTrue(targetFile.exists());
        
        // ... and the source file should not exist anymore
        assertFalse(sourceFile.exists());
        
        // rename the target directory
        File newTargetDir = new File(xtreemFSMountPoint, "newTargetDir");
        assertTrue(targetDir.renameTo(newTargetDir));
        
        // afterwards, the former target directory should not exist anymore
        assertFalse(targetDir.exists());
        
        // ... and the new one should exist instead
        assertTrue(newTargetDir.exists());
        
        // ... and the nested file should have a new path
        File newTargetFile = new File(newTargetDir, targetFile.getName());
        assertTrue(newTargetFile.exists());
        
        // create a new directory and move the entire path to it while renaming
        // the moved path
        File topLevelTargetDir = createDir(xtreemFSMountPoint, "topLevelDir");
        File nestedDir = new File(topLevelTargetDir, "nestedDir");
        assertTrue(newTargetDir.renameTo(nestedDir));
        
        // ... afterwards, the file should still exist in the nested directory
        File nestedFile = new File(nestedDir, newTargetFile.getName());
        assertTrue(nestedFile.exists());
        
    }
    
    @Test
    public void testBatchCreateDeleteRename() throws Exception {
        
        final int numFiles = 100;
        
        // touch a bunch of files and check whether they are there
        for (int i = 0; i < numFiles; i++)
            new File(xtreemFSMountPoint.getAbsolutePath() + "/" + i).createNewFile();
        
        assertEquals(numFiles, new File(xtreemFSMountPoint.getAbsolutePath()).list().length);
        
        // delete the files and check whether the directory is empty
        for (int i = 0; i < numFiles; i++)
            new File(xtreemFSMountPoint.getAbsolutePath() + "/" + i).delete();
        
        assertEquals(0, new File(xtreemFSMountPoint.getAbsolutePath()).list().length);
        
        // write data to a bunch of files and check whether the files are there
        for (int i = 0; i < numFiles; i++) {
            FileWriter fw = new FileWriter(xtreemFSMountPoint.getAbsolutePath() + "/" + i);
            fw.write("test");
            fw.close();
        }
        
        assertEquals(numFiles, new File(xtreemFSMountPoint.getAbsolutePath()).list().length);
        
        // create a new directory and move all files to it
        new File(xtreemFSMountPoint.getAbsolutePath() + "/dir").mkdir();
        for (int i = 0; i < numFiles; i++)
            new File(xtreemFSMountPoint.getAbsolutePath() + "/" + i).renameTo(new File(
                xtreemFSMountPoint.getAbsolutePath() + "/dir/" + i));
        
        assertEquals(1, new File(xtreemFSMountPoint.getAbsolutePath()).list().length);
        assertEquals(numFiles,
            new File(xtreemFSMountPoint.getAbsolutePath() + "/dir").list().length);
        
        // delete the files and check whether the directory is empty
        for (int i = 0; i < numFiles; i++)
            new File(xtreemFSMountPoint.getAbsolutePath() + "/dir/" + i).delete();
        
        assertEquals(1, new File(xtreemFSMountPoint.getAbsolutePath()).list().length);
        assertEquals(0, new File(xtreemFSMountPoint.getAbsolutePath() + "/dir").list().length);
    }
    
    // /**
    // * Perform some sequential read/write operations on a file.
    // *
    // * @throws Exception
    // */
    // public void testSeqReadWrite() throws Exception {
    //        
    // // create a new file for sequential r/w access
    // File f = createFile(xtreemFSMountPoint, "/testfile.tmp");
    //        
    // FileOutputStream fout = new FileOutputStream(f);
    // fout.write(65);
    // fout.write(66);
    // fout.close();
    // assertEquals(2, f.length());
    //        
    // FileInputStream fin = new FileInputStream(f);
    // assertEquals(65, fin.read());
    // assertEquals(66, fin.read());
    // assertEquals(-1, fin.read());
    // fin.close();
    // }
    //    
    // /**
    // * Perform some random read/write operations on a file.
    // *
    // * @throws Exception
    // */
    // public void testRndReadWrite() throws Exception {
    //        
    // File file = createFile(xtreemFSMountPoint, "/testfile2.tmp");
    //        
    // // create a new file for random r/w access
    // RandomAccessFile f2 = new RandomAccessFile(file, "rw");
    // f2.writeBytes("Hello World!");
    // assertEquals(12, f2.length());
    //        
    // f2.seek(4);
    // assertEquals('o', f2.read());
    // f2.seek(6);
    // f2.write('w');
    // f2.seek(6);
    // assertEquals('w', f2.read());
    // f2.seek(6);
    // f2.writeBytes("XtreemFS!");
    // f2.seek(0);
    //        
    // byte[] chars = new byte[(int) f2.length()];
    // f2.readFully(chars);
    //        
    // assertEquals("Hello XtreemFS!", new String(chars));
    //        
    // f2.seek(16384);
    // f2.writeBytes("This is a string at offset 16384");
    //        
    // byte[] buf = new byte[2048];
    // f2.seek(8192);
    // f2.read(buf);
    // for (int i = 0; i < buf.length; i++)
    // assertEquals(0, buf[i]);
    //        
    // f2.seek(16384);
    // f2.read(buf);
    // assertEquals("This is a string at offset 16384", new String(buf, 0, 32));
    //        
    // f2.close();
    // assertEquals(16416, file.length());
    // }
    //    
    // /**
    // * Read and write random strides.
    // *
    // * @throws Exception
    // */
    // public void testRndReadWrite2() throws Exception {
    //        
    // final File file = createFile(xtreemFSMountPoint, "/testfile3.tmp");
    // final RandomAccessFile raf = new RandomAccessFile(file, "rw");
    // final int maxAccesses = 1000;
    // final int maxFileSize = 1024 * 1024;
    // final int maxNumberOfBytes = 1024 * 128;
    // final double readWriteRatio = .15;
    //        
    // // allocate 10M
    // byte[] buf = new byte[maxFileSize];
    //        
    // for (int i = 0; i < maxAccesses; i++) {
    //            
    // boolean write = Math.random() > readWriteRatio;
    //            
    // int numberOfBytes = (int) (Math.random() * maxNumberOfBytes);
    // int offset = (int) (Math.random() * buf.length);
    // offset = Math.min(offset, buf.length - numberOfBytes);
    // byte[] stride = new byte[numberOfBytes];
    //            
    // if (write) {
    // // write a stride
    //                
    // for (int j = 0; j < stride.length; j++)
    // stride[j] = (byte) (Math.random() * 256 - 128);
    //                
    // raf.seek(offset);
    // raf.write(stride);
    //                
    // System.arraycopy(stride, 0, buf, offset, stride.length);
    //                
    // } else {
    // // read a stride
    //                
    // raf.seek(offset);
    // raf.read(stride);
    //                
    // for (int j = 0; j < stride.length; j++)
    // assertEquals(stride[j], buf[offset + j]);
    // }
    // }
    //        
    // // finally, read and compare the complete file
    // byte[] readBuf = new byte[maxFileSize];
    // raf.seek(0);
    // raf.read(readBuf);
    //        
    // for (int j = 0; j < maxFileSize; j++)
    // assertEquals(buf[j], readBuf[j]);
    //        
    // raf.close();
    // }
    
    
    private File createDir(File parentDir, String name) {
        
        final long numberOfChildren = parentDir.list().length;
        
        // a new directory ...
        File dir = new File(parentDir, name);
        
        // ... must not exist before
        assertTrue(!dir.exists());
        
        dir.mkdir();
        
        // ... must exist afterwards
        assertTrue(dir.exists());
        
        // ... must be a directory
        assertTrue(dir.isDirectory());
        
        // ... must be an additional element in its parent directory
        assertEquals(numberOfChildren + 1, parentDir.list().length);
        
        // ... must be an empty directory
        assertEquals(0, dir.list().length);
        
        return dir;
    }
    
    private File createFile(File parentDir, String name) throws Exception {
        
        final long numberOfChildren = parentDir.list().length;
        
        // a new file ...
        File file = new File(parentDir, name);
        
        // ... must not exist before
        assertTrue(!file.exists());
        
        assertTrue(file.createNewFile());
        
        // ... must exist afterwards
        assertTrue(file.exists());
        
        // ... must be a file
        assertTrue(file.isFile());
        
        // ... must be an additional element in its parent directory
        assertEquals(numberOfChildren + 1, parentDir.list().length);
        
        // ... must not have any content
        assertEquals(0, file.length());
        
        return file;
    }
    
    private void delete(File fileOrDir) {
        
        final long numberOfChildren = fileOrDir.getParentFile().list().length;
        
        // a file or directory that is deleted ...
        
        // ... must exist before
        assertTrue(fileOrDir.exists());
        
        assertTrue(fileOrDir.delete());
        
        // ... must not exist afterwards
        assertTrue(!fileOrDir.exists());
        
        // ... must not exist in its parent directory anymore
        assertEquals(numberOfChildren - 1, fileOrDir.getParentFile().list().length);
    }
    
    private void createTree(File root, int depth, int breadth) throws Exception {
        
        if (depth == -1)
            return;
        
        for (int j = 0; j < breadth; j++) {
            File f = new File(root, j + "");
            f.mkdir();
            createTree(f, depth - 1, breadth);
        }
    }
    
    private void testTree(File root, int depth, int breadth) throws Exception {
        
        if (depth == -1)
            return;
        
        assertEquals(breadth, root.list().length);
        
        for (int j = 0; j < breadth; j++) {
            File f = new File(root, j + "");
            assertTrue(f.exists());
            testTree(f, depth - 1, breadth);
        }
        
    }
}
