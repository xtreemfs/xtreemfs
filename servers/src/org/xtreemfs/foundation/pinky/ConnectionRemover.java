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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.foundation.pinky;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.common.logging.Logging;

/**
 * Removes lingering connections.
 *
 * @author bjko
 */
public class ConnectionRemover extends Thread {

    private final Queue<ConnectionState> connections;

    private final AtomicInteger counter;

    int cleanupInterval;

    boolean quit;

    private final Selector selector;

    /**
     * Creates a new instance of ConnectionRemover
     *
     * @param connections
     *            the list of all active connections
     * @param interval
     *            time between runs
     */
    public ConnectionRemover(Queue<ConnectionState> connections, AtomicInteger counter, Selector selector, int interval) {
        super("Pinky Connection Remover");
        this.connections = connections;
        this.cleanupInterval = interval;
        this.quit = false;
        this.counter = counter;
        this.selector = selector;
    }

    /**
     * Shuts the thread down.
     */
    public void quitThread() {
        this.quit = true;
        this.interrupt();
    }

    /**
     * Main loop.
     */
    public void run() {
        while (!quit) {
            Iterator<ConnectionState> conIt = connections.iterator();
            while (conIt.hasNext()) {
                ConnectionState con = conIt.next();
                if (!con.active.get() && (con.pipeline.isEmpty())) {
                    if (con.channel.isOpen()) {
                        try {
                            if (con.channel.shutdown(con.channel.keyFor(selector))) {
                                counter.decrementAndGet();
                                con.channel.close();
                                con.active.set(false);
                                con.freeBuffers();
                                //con.requestHeaders = null;
                                Logging.logMessage(Logging.LEVEL_DEBUG, this, "connection to " + con.channel.socket().getRemoteSocketAddress() + " closed");

                                conIt.remove();
                            }
                        } catch (IOException e) {
                            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
                        }
                    }
                } else {
                    con.active.set(false);
                }
            }
            try {
                this.sleep(cleanupInterval);
            } catch (InterruptedException ex) {
            }
        }
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "shutdown complete");
    }
}
