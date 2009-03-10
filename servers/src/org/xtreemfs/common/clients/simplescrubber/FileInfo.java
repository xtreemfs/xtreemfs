/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck(ZIB)
 */


package org.xtreemfs.common.clients.simplescrubber;

import java.io.IOException;
import org.xtreemfs.common.clients.io.RandomAccessFile;


/**
 *
 * @author bjko
 */
public class FileInfo implements Runnable {

    private final RandomAccessFile   file;

    private final FileScrubbedListener listener;

    private long                     nextObjectToScrub;

    private long                     byteCounter;

    public FileInfo(RandomAccessFile file, FileScrubbedListener listener) {
        this.file = file;
        this.listener = listener;

        nextObjectToScrub = 0;
        byteCounter = 0;
    }

    public void run() {

        boolean eof = false;
        do {
            try {
                int objSize = file.checkObject(nextObjectToScrub++);
                if (objSize < file.getStripeSize()) {
                    eof = true;
                }
                byteCounter += objSize;
            } catch (IOException ex) {
                listener.fileScrubbed(file, byteCounter, false, "unable to check object #"+(nextObjectToScrub-1)+": "+ex);
                return;
            }
        } while (!eof);
        try {
            long mrcFileSize = file.length();
            if (byteCounter != mrcFileSize) {
                file.forceFileSize(byteCounter);
                System.out.println("fileID: "+file.getFileId()+" - corrected file size from "+mrcFileSize+" to "+byteCounter+" bytes");
            }
            
        } catch (IOException ex) {
            listener.fileScrubbed(file, byteCounter, false, "unable to get file size: "+ex);
            return;
        }
        listener.fileScrubbed(file, byteCounter, true, "");
    }


    public static interface FileScrubbedListener {
        public void fileScrubbed(RandomAccessFile file, long bytesScrubbed, boolean isOk, String errorMessage);
    }



}
