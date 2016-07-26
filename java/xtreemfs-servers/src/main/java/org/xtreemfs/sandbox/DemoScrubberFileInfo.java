///*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
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
// * AUTHORS: Björn Kolbeck(ZIB)
// */
//
//
//package org.xtreemfs.sandbox;
//
//import org.xtreemfs.common.clients.simplescrubber.*;
//import java.io.IOException;
//import org.xtreemfs.common.clients.io.RandomAccessFile;
//import org.xtreemfs.interfaces.Stat;
//
//
///**
// *
// * @author bjko
// */
//public class DemoScrubberFileInfo implements Runnable {
//
//    private final RandomAccessFile   file;
//
//    private final FileScrubbedListener listener;
//
//
//    public DemoScrubberFileInfo(RandomAccessFile file, FileScrubbedListener listener) {
//        this.file = file;
//        this.listener = listener;
//
//    }
//
//    public void run() {
//
//        boolean deleted = false;
//        try {
//            Stat stat = file.stat();
//            if (stat.getMtime_ns()/1000000l < System.currentTimeMillis()-60*60*1000) {
//                //file is old, remove it
//                file.delete();
//                deleted = true;
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        listener.fileScrubbed(file,deleted);
//    }
//
//
//    public static interface FileScrubbedListener {
//        public void fileScrubbed(RandomAccessFile file, boolean deleted);
//    }
//
//
//
//}
