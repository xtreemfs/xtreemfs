/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.comm.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;


/**
 *
 * @author bjko
 */
public class EchoServer implements NIOServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        try {
            Logging.start(Logging.LEVEL_DEBUG);
            TimeSync.initializeLocal(50);

            EchoServer s = new EchoServer();
            TCPCommunicator srv = new TCPCommunicator(s, 3333, null);
            srv.start();
            srv.waitForStartup();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public EchoServer() {

    }

    public void onAccept(NIOConnection connection) {
        //ignore
        System.out.println("connected: "+connection);
        connection.read(BufferPool.allocate(1024));
    }

    public void onConnect(NIOConnection connection) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onRead(NIOConnection connection, ReusableBuffer buffer) {
        boolean done = false;
        System.out.println("do read: "+connection);
        for (int i = 0; i < buffer.position(); i++) {
            if (buffer.get(i) == '\n') {
                done = true;
                break;
            }
        }
        if (done || !buffer.hasRemaining()) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String contents = new String(data);
            if (contents.startsWith("quit")) {
                try {
                    connection.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                //we have a full line
                System.out.println("new buffer: "+connection);
                buffer.clear();
                
                if (contents.startsWith("stats")) {
                    contents = BufferPool.getStatus();
                } else {
                    contents = "you said: "+contents;
                }
                buffer.put(contents.getBytes());
                buffer.flip();
                connection.write(buffer,null);
                connection.read(BufferPool.allocate(1024));
            }
        }
    }

    public void onClose(NIOConnection connection) {
        System.out.println("disconnected: "+connection);
    }

    public void onWriteFailed(IOException exception, Object context) {
        System.out.println("an error occurred: "+exception);
    }

    public void onConnectFailed(InetSocketAddress endpoint, IOException exception, Object context) {
        //ignore
    }

}
