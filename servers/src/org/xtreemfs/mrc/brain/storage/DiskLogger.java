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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.brain.storage;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.LifeCycleThread;

/**
 * Writes entries to the on disc append log and syncs after blocks of MAX_ENTRIES_PER_BLOCK.
 * @author bjko
 */
public class DiskLogger extends LifeCycleThread {

    public static final byte OTYPE_REPLICATION = 1;

    public static final byte OPTYPE_BRAIN = 2;

    public static final byte OPTYPE_MRC = 3;

    /**
     * NIO FileChannel used to write ByteBuffers directly to file.
     */
    private FileChannel channel;
    /**
     * Stream used to obtain the FileChannel and to flush file.
     */
    private FileOutputStream fos;
    /**
     * Used to sync file.
     */
    private FileDescriptor fdes;

    /**
     * The LogEntries to be written to disk.
     */
    private LinkedBlockingQueue<LogEntry> entries;

    /**
     * If set to true the thread will shutdown.
     */
    boolean quit;

    /**
     * If set to true the DiskLogger is down
     */
    boolean down;

    /**
     * LogFile name
     */
    private String logfile;


    /**
     *  if set to true, no fsync is executed after disk writes. DANGEROUS.
     */
    private final boolean noFsync;

    /**
     * Max number of LogEntries to write before sync.
     */
    public static final int MAX_ENTRIES_PER_BLOCK = 100;


    /**
     * Creates a new instance of DiskLogger
     * @param logfile Name and path of file to use for append log.
     * @throws java.io.FileNotFoundException If that file cannot be created.
     * @throws java.io.IOException If that file cannot be created.
     */
    public DiskLogger(String logfile, boolean noFsync) throws FileNotFoundException, IOException {

        super("DiskLogger thr.");

        if (logfile == null)
            throw new RuntimeException("expected a non-null logfile name!");
        File lf = new File(logfile);
        if(!lf.getParentFile().exists() && !lf.getParentFile().mkdirs())
            throw new IOException("could not create parent directory for database log file");

        fos = new FileOutputStream(lf,true);
        channel = fos.getChannel();
        fdes = fos.getFD();
        entries = new LinkedBlockingQueue();
        quit = false;
        this.logfile = logfile;
        this.down = false;
        this.noFsync = noFsync;
    }

    public long getLogFileSize() {
        return new File(logfile).length();
    }

    /**
     * Appends an entry to the write queue.
     * @param entry entry to write
     */
    public void append(LogEntry entry) {
        assert(entry != null);
        entries.add(entry);
    }

    /**
     * Main loop.
     */
    public void run() {

        ArrayList<LogEntry> tmpE = new ArrayList(MAX_ENTRIES_PER_BLOCK);
        Logging.logMessage(Logging.LEVEL_DEBUG,this,"operational");
        notifyStarted();

        down = false;
        while (!quit) {
            try {
                //wait for an entry
                tmpE.add( entries.take() );
                if (entries.size() > 0) {
                    while (tmpE.size() < MAX_ENTRIES_PER_BLOCK-1) {
                        LogEntry tmp = entries.poll();
                        if (tmp == null)
                            break;
                        tmpE.add(tmp);
                    }
                }
                for (LogEntry le : tmpE) {
                    assert (le != null) : "Entry must not be null";
                    ReusableBuffer buf = le.marshall();
                    channel.write(buf.getBuffer());
                    BufferPool.free(buf);
                }
                fos.flush();
                if (!noFsync)
                    fdes.sync();
                for (LogEntry le : tmpE) {
                    le.listener.synced(le);
                }
                tmpE.clear();
            } catch (SyncFailedException ex) {
                 Logging.logMessage(Logging.LEVEL_ERROR,this,ex);
                for (LogEntry le : tmpE) {
                    le.listener.failed(le,ex);
                }
                tmpE.clear();
            } catch (IOException ex) {
                Logging.logMessage(Logging.LEVEL_ERROR,this,ex);
                for (LogEntry le : tmpE) {
                    le.listener.failed(le,ex);
                }
                tmpE.clear();
            } catch (InterruptedException ex) {
            }
        }
        Logging.logMessage(Logging.LEVEL_DEBUG,this,"shutdown complete");
        down = true;
        notifyStopped();
    }

    /**
     * stops this thread
     */

    public void shutdown() {
        quit = true;
        synchronized (this) {
            this.interrupt();
        }
    }

    /** This operation assumes that the DiskLogger is dead already
     */
    public void cleanLog() throws IOException{
       if (!down) {
            channel.truncate(0);
       } else {
           File f = new File(logfile);
           f.delete();
       }
    }

    public boolean isDown() {
        return down;
    }


    /**
     * shut down files.
     */
    protected void finalize() throws Throwable {
        try {
            fos.flush();
            fdes.sync();
            fos.close();
        } catch (SyncFailedException ex) {
        } catch (IOException ex) {
        } finally {
            super.finalize();
        }
    }


    public ReusableBuffer getLog(SliceID slice, int sqStart, int sqEnd) throws IOException {
        File f = new File(logfile);
        LinkedList<LogEntry> entries = new LinkedList();
        int size = 0;
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        try {
            while (true) {
                LogEntry e = new LogEntry(fc);

                if (!e.slID.equals(slice))
                    continue;

                if (e.sequenceID < sqStart)
                    continue;

                if (e.sequenceID > sqEnd)
                    break;

                entries.add(e);
                size += e.binarySize();
            }
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR,this,ex);
        } catch (BufferUnderflowException ex) {
            //who cares! probably "a half entry"
            //ex.printStackTrace();
        }
        ReusableBuffer logbuf = BufferPool.allocate(size);
        for (LogEntry e : entries) {
            e.marshall(logbuf);
        }
        logbuf.position(0);
        Logging.logMessage(Logging.LEVEL_DEBUG,this,"sending..."+logbuf);
        return logbuf;
    }

    public int getQLength() {
        return this.entries.size();
    }

}
