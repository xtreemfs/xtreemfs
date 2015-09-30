/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.comm.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;

/**
 *
 * @author bjko
 */
public class EchoClient {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        Logging.start(Logging.LEVEL_DEBUG);

        try {
            TimeSync ts = TimeSync.initializeLocal(50);

            TCPClient com = new TCPClient(3334,null, new NIOServer() {

                public void onAccept(NIOConnection connection) {
                    onConnect(connection);
                }

                public void onConnect(NIOConnection connection) {
                    System.out.println("connected to "+connection.getEndpoint());
                    connection.read(BufferPool.allocate(1024));
                    connection.setContext(new AtomicBoolean(false));
                }

                public void onRead(NIOConnection connection, ReusableBuffer buffer) {
                    System.out.println("read from "+connection);
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String contents = new String(data);
                    BufferPool.free(buffer);
                    connection.read(BufferPool.allocate(1024));
                    System.out.println(">> "+contents);
                }

                public void onClose(NIOConnection connection) {
                    System.out.println("connection from "+connection.getEndpoint()+" closed ");
                }

                public void onWriteFailed(IOException exception, Object context) {
                    System.out.println("could not write, context: "+context);
                }

                public void onConnectFailed(InetSocketAddress endpoint, IOException exception, Object context) {
                    System.out.println("could not connect to: "+endpoint+", context: "+context);
                }
            });
            com.start();
            com.waitForStartup();

            ReusableBuffer data = ReusableBuffer.wrap("Hello world!\n".getBytes());
            com.write(new InetSocketAddress("localhost", 3333), data, "Yagg");

            Thread.sleep(100);

            data = ReusableBuffer.wrap("Hello world!\n".getBytes());
            com.write(new InetSocketAddress("localhost", 3333), data, "Yagga");

            Thread.sleep(30000);

            data = ReusableBuffer.wrap("YaggaYagga!\n".getBytes());
            com.write(new InetSocketAddress("localhost", 3333), data, null);

            Thread.sleep(2000);
            com.shutdown();
            ts.close();
            
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

    }

}
