/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.flease;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.LinkedBlockingQueue;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.logging.Logging;

/**
 *
 * @author bjko
 */
public class SimpleMasterEpochHandler extends LifeCycleThread implements MasterEpochHandlerInterface {
    public static final int MAX_EPOCH_CACHE_SIZE = 10000;


    private final LinkedBlockingQueue<Request> requests;

    private final LRUCache<ASCIIString,Long>   epochs;

    private final String                       directory;

    public SimpleMasterEpochHandler(String directory) {
        super("SimpleMEHandler");
        requests = new LinkedBlockingQueue<Request>();
        epochs = new LRUCache<ASCIIString, Long>(MAX_EPOCH_CACHE_SIZE);
        this.directory = directory;
    }

    @Override
    public void run() {
        try {
            notifyStarted();
            do {
                Request rq = requests.take();

                switch (rq.type) {
                    case SEND: {
                        try {
                            Long epoch = epochs.get(rq.message.getCellId());
                            if (epoch == null) {

                                File f = new File(getFileName(rq.message.getCellId()));
                                if (f.exists()) {
                                    RandomAccessFile raf = new RandomAccessFile(f, "r");
                                    epoch = raf.readLong();
                                    raf.close();
                                } else {
                                    epoch = 0l;
                                }
                                epochs.put(rq.message.getCellId(),epoch);
                            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.all, this, "sent %d", epoch);
                            }
                            rq.message.setMasterEpochNumber(epoch);
                        } catch (Exception ex) {
                            rq.message.setMasterEpochNumber(-1);
                        } finally {
                            try {
                                rq.callback.processingFinished();
                            } catch (Exception ex) {
                                Logging.logError(Logging.LEVEL_ERROR, this, ex);
                            }
                        }
                        break;
                    }
                    case STORE: {
                        try {
                            File f = new File(getFileName(rq.message.getCellId()));
                            RandomAccessFile raf = new RandomAccessFile(f, "rw");
                            raf.writeLong(rq.message.getMasterEpochNumber());
                            raf.getFD().sync();
                            raf.close();
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.all, this, "stored %d",
                                rq.message.getMasterEpochNumber());
                            epochs.put(rq.message.getCellId(),rq.message.getMasterEpochNumber());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            try {
                                rq.callback.processingFinished();
                            } catch (Exception ex) {
                                Logging.logError(Logging.LEVEL_ERROR, this, ex);
                            }
                        }
                        break;
                    }
                }

            } while (!interrupted());
        } catch (InterruptedException ex) {
        } catch (Throwable ex) {
            notifyCrashed(ex);
        }
        notifyStopped();
    }

    private String getFileName(ASCIIString cellId) {
        return directory + cellId.toString() + ".me";
    }

    @Override
    public void shutdown() {
        this.interrupt();
    }

    @Override
    public void sendMasterEpoch(FleaseMessage response, Continuation callback) {
        Request rq = new Request();
        rq.callback = callback;
        rq.message = response;
        rq.type = Request.RequestType.SEND;
        requests.add(rq);
    }

    @Override
    public void storeMasterEpoch(FleaseMessage request, Continuation callback) {
        Request rq = new Request();
        rq.callback = callback;
        rq.message = request;
        rq.type = Request.RequestType.STORE;
        requests.add(rq);
    }

    private final static class Request {
        FleaseMessage message;
        Continuation callback;
        String fileName;
        enum RequestType {
            SEND, STORE;
        };
        RequestType  type;
    }


}
