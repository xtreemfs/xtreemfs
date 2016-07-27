package org.xtreemfs.sandbox.tests;

import java.net.InetSocketAddress;

import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

public class JavaClientTest {
    
    private static final byte[] buf        = new byte[131072];
    
    static {
        for(int i = 0; i < buf.length; i++)
            buf[i] = -1;
    }
    
    private static final int    numThreads = 40;
    
    private static final int    numFiles   = 3000;
    
    private static final int    numAppends = 3;
    
    private Client              c;
    
    private Volume              v;
    
    public void init() throws Exception {
        
        UserCredentials uc = UserCredentials.newBuilder().setUsername("stender").addGroups("users").build();
        
        c = new Client(new InetSocketAddress[] { new InetSocketAddress(32638) }, 60000, 120000, null);
        c.start();
        
        v = c.getVolume("test", uc);
    }
    
    public void run() throws Exception {
        
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < threads.length; i++)
            threads[i] = getWriter(i);
        
        for (Thread th : threads)
            th.start();
        
        for (Thread th : threads)
            th.join();
        
    }
    
    public void stop() {
        c.stop();
    }
    
    private Thread getWriter(final int num) {
        
        return new Thread() {
            
            public void run() {
                
                try {
                    
                    File dir = v.getFile(num + "");
                    if (!dir.exists())
                        dir.mkdir(0777);
                    
                    for (int i = 0; i < numFiles; i++) {
                        File file = v.getFile(num + "/" + i + ".txt");
                        
                        RandomAccessFile raf = file.open("rw", 0777);
                        for (int j = 0; j < numAppends; j++)
                            raf.write(buf, 0, buf.length);
                        raf.close();
                    }
                    
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
                
            }
            
        };
    }
    
    public static void main(String[] args) throws Exception {
        
        Logging.start(Logging.LEVEL_WARN);
        
        JavaClientTest t = new JavaClientTest();
        t.init();
        t.run();
        t.stop();
    }
    
}
