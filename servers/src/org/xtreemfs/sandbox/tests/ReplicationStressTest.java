///*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
//    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
//
//    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
//    Grid Operating System, see <http://www.xtreemos.eu> for more details.
//    The XtreemOS project has been developed with the financial support of the
//    European Commission's IST program under contract #FP6-033576.
//
//    XtreemFS is free software: you can redistribute it and/or modify it under
//    the terms of the GNU General Public License as published by the Free
//    Software Foundation, either version 2 of the License, or (at your option)
//    any later version.
//
//    XtreemFS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
// */
///*
// * AUTHORS: Christian Lorenz (ZIB)
// */
//package org.xtreemfs.sandbox.tests;
//
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//import org.xtreemfs.common.auth.NullAuthProvider;
//import org.xtreemfs.common.buffer.ReusableBuffer;
//import org.xtreemfs.common.clients.io.RandomAccessFile;
//import org.xtreemfs.common.clients.mrc.MRCClient;
//import org.xtreemfs.foundation.speedy.MultiSpeedy;
//
///**
// *
// * 24.02.2009
// *
// * @author clorenz
// */
//public class ReplicationStressTest implements Runnable {
//    private final static String volumeName = "replicationTestVolume";
//    private final static String filePath = "/replicationTest/";
//    private static InetSocketAddress mrcAddress;
//    private static String authString;
//
//    /**
//     * @param args
//     *            the command line arguments
//     */
//    public static void main(String[] args) throws Exception {
//        // parse arguments
//        mrcAddress = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
//        // "client" reading threads
//        int threadNumber = Integer.parseInt(args[2]);
//        // available OSDs
//        int osdNumber = Integer.parseInt(args[3]);
//
//        // prepare volume, ...
//        MRCClient client = new MRCClient();
//        authString = NullAuthProvider.createAuthString("userXY", MRCClient.generateStringList("groupZ"));
//        // create a volume (no access control)
//        client.createVolume(mrcAddress, volumeName, authString);
//        // create directory
//        client.createDir(mrcAddress, volumeName + filePath, authString);
//
//        // create file list
//        List<String> fileList = new ArrayList<String>();
//        for (int i = 0; i < 10; i++) { // adjust this setting
//            fileList.add("file" + i);
//        }
//
//        MultiSpeedy speedy = new MultiSpeedy();
//        speedy.start();
//
//        writeFiles(speedy, fileList);
//        prepareReplication(speedy, fileList);
//
//        // start reading threads
//        ExecutorService executor = Executors.newFixedThreadPool(threadNumber);
//        for (int i = 0; i < threadNumber; i++) {
//            // TODO: shuffle the fileList for each thread => different job processing sequence
//            executor.execute(new ReplicationStressTest(mrcAddress, fileList));
//        }
//        // wait until threads have ended
//        executor.awaitTermination(1, TimeUnit.MINUTES);
//
//        // shutdown
//        executor.shutdown();
//        client.shutdown();
//        speedy.shutdown();
//        client.waitForShutdown();
//        speedy.waitForShutdown();
//    }
//
//    /**
//     * fill files with data (different sizes)
//     */
//    public static void writeFiles(MultiSpeedy speedy, List<String> fileList) throws Exception {
//        // adjust this
//        int filesize = 128 * 1024;
//        int partsize = 8192 * 1024 * 1024;
//
//        Random random = new Random(20);
//
//        for (String fileName : fileList) {
//            RandomAccessFile raf = new RandomAccessFile("cw", mrcAddress, volumeName + filePath + fileName,
//                    speedy);
//
//            // write all parts
//            int part = 0;
//            ReusableBuffer data;
//            while (part < filesize) {
//                if (filesize < part + partsize) { // only one part or last part of file
//                    data = generateData(filesize - part);
//                    raf.write(data.array(), part, data.limit());
//                } else { // any part of file
//                    if (random.nextInt(100) > 10) { // 90% chance
//                        data = generateData(partsize);
//                        raf.write(data.array(), part, data.limit());
//                    }
//                    // else: skip writing => hole
//                }
//                part = part + partsize;
//            }
//
//            // increase filesize
//            filesize = filesize * 2;
//        }
//    }
//
//    /**
//     * set file read only and add replicas
//     */
//    public static void prepareReplication(MultiSpeedy speedy, List<String> fileList) throws Exception {
//        for (String fileName : fileList) {
//            RandomAccessFile raf = new RandomAccessFile("w", mrcAddress, volumeName + filePath + fileName,
//                    speedy);
//
//            raf.setReadOnly(true);
//
//            // add some replicas
//        }
//
//    }
//
//    /*
//     * thread
//     */
//    private MRCClient client;
//    private MultiSpeedy speedy;
//    private final List<String> fileList;
//    private Random random;
//
//    public ReplicationStressTest(InetSocketAddress mrcAddress, List<String> fileList) throws Exception {
//        client = new MRCClient();
//        speedy = new MultiSpeedy();
//        speedy.start();
//
//        String authString = NullAuthProvider.createAuthString("userXY", MRCClient
//                .generateStringList("groupZ"));
//
//        this.fileList = fileList;
//        random = new Random(10);
//    }
//
//    public void shutdown() throws Exception {
//        client.shutdown();
//        client.waitForShutdown();
//        speedy.shutdown();
//        speedy.waitForShutdown();
//    }
//
//    @Override
//    public void run() {
//        try {
//            for (String fileName : fileList) {
//                replicateFile(fileName);
//            }
//
//            // shutdown
//            shutdown();
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * read/replicate files
//     */
//    public void replicateFile(String fileName) throws Exception {
//        RandomAccessFile raf = new RandomAccessFile("r", mrcAddress, volumeName + filePath + fileName, speedy);
//
//        long filesize = raf.length();
//
//        // prepare ranges for reading file
//        List<List<Integer>> ranges = new ArrayList<List<Integer>>();
//        int startOffset = 0;
//        int length = (int) filesize / 19;
//        for (int i = 0; i < 20; i++) { // read EOF in the last range
//            List<Integer> range = new ArrayList<Integer>();
//            range.add(new Integer(startOffset));
//            range.add(new Integer(length));
//            ranges.add(range);
//
//            startOffset = startOffset + length;
//        }
//
//        // shuffle list for no straight forward reading
//        Collections.shuffle(ranges, random);
//
//        // TODO: read file
//        for (List<Integer> range : ranges) {
//            startOffset = range.get(0).intValue();
//            length = range.get(1).intValue();
//
//            byte[] result = new byte[length];
//            raf.read(result, startOffset, length);
//
//            // TODO: do something with the result
//        }
//    }
//
//    /*
//     * copied from test...SetupUtils
//     */
//    /**
//     * @param size
//     *            in byte
//     */
//    private static ReusableBuffer generateData(int size) {
//        Random random = new Random();
//        byte[] data = new byte[size];
//        random.nextBytes(data);
//        return ReusableBuffer.wrap(data);
//    }
//
//}
