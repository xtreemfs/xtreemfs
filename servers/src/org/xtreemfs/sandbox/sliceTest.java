/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.KeyValuePair;
import org.xtreemfs.interfaces.KeyValuePairSet;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.interfaces.ServiceRegistryDataMap;
import org.xtreemfs.interfaces.ServiceRegistrySet;

/**
 *
 * @author bjko
 */
public class sliceTest {


    public static void main(String[] args) {
        try {

            /*ReusableBuffer buf = BufferPool.allocate(128);

            long nanoStart = System.nanoTime();

            for (int i = 0; i < 10000; i++) {
                ReusableBuffer vbuf = buf.createViewBuffer();
                vbuf.range(28, 100);
                 
                /*ReusableBuffer rbuf = BufferPool.allocate(100);
                buf.position(28);
                rbuf.put(buf);
                BufferPool.free(rbuf);* /
            }

            long nanoEnd = System.nanoTime();

            double dur = nanoEnd-nanoStart;

            System.out.println("took "+dur/1e6+"ms");*/

            ServiceRegistrySet sregset = new ServiceRegistrySet();
            KeyValuePairSet kvset = new KeyValuePairSet();
            kvset.add(new KeyValuePair("yagga", "bla"));
            sregset.add(new ServiceRegistry("123", 1, 1, "yagga OSD", 0, new ServiceRegistryDataMap()));
            Object[] data = (Object[])sregset.serialize();
            for (Object o : data) {
                System.out.println("entry: "+o);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
