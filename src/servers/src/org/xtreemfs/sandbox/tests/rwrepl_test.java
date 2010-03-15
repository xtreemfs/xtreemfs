/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.ONCRPCServiceURL;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;

/**
 *
 * @author bjko
 */
public class rwrepl_test {

    public static final int WAIT_FOR_CLOSE = 90*1000;
    public static final int BLKSIZE = 128*1024;
    public static final String VOLNAME = "rwrtest";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        try {
            final ONCRPCServiceURL dir = new ONCRPCServiceURL(args[0], "oncrpc", 32638);
            final int    numReplicas = Integer.valueOf(args[1]);
            final int    fileSize = Integer.valueOf(args[2])*1024;

            Logging.start(Logging.LEVEL_INFO, Category.all);
            TimeSync.initializeLocal(5000, 50);

            StringSet grps = new StringSet();
            grps.add("users");
            final UserCredentials uc = new UserCredentials("test", grps, null);

            Client c = new Client(new InetSocketAddress[]{new InetSocketAddress(dir.getHost(), dir.getPort())}, 15*1000, 5*60*1000, null);
            c.start();

            try {
                c.createVolume(VOLNAME, uc, new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, 128, 1), AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX.intValue(), 0777);
            } catch (Exception ex) {
                System.out.println("create volume failed: "+ex);
            }

            Volume v = c.getVolume(VOLNAME, uc);

            File f = v.getFile("data.rwrtest");

            try {
                f.delete();
            } catch (Exception ex) {
                System.out.println("create volume failed: "+ex);
            }
            f.createFile();
            f.setReplicaUpdatePolicy(Constants.REPL_UPDATE_PC_WARONE);
            for (int i = 0; i < numReplicas; i++) {
                String[] suitableOSDs = f.getSuitableOSDs(1);
                System.out.println("suitable OSDs: "+suitableOSDs);
                if (suitableOSDs.length != 1)
                    throw new IOException("cannot add OSDs, no suitable OSD");
                f.addReplica(1, suitableOSDs, 0);
            }

            RandomAccessFile io = f.open("rw", 0444);

            long datacnt = 0;
            while (datacnt < fileSize) {

                ReusableBuffer data = BufferPool.allocate(BLKSIZE);
                fillData(data);
                data.flip();

                io.write(data);

                BufferPool.free(data);
                datacnt += BLKSIZE;

            }

            System.out.println("writing complete");

            Thread.sleep(WAIT_FOR_CLOSE);

            int nextRepl = (int)(Math.random()*((double)(numReplicas)))+1;
            System.out.println("switch to replica "+nextRepl);
            io.forceReplica(nextRepl);

            datacnt = 0;
            while (datacnt < fileSize) {

                ReusableBuffer data = BufferPool.allocate(BLKSIZE);

                io.read(data);
                data.flip();
                if (checkData(data) == false)
                    throw new Exception("invalid data read");

                BufferPool.free(data);
                datacnt += BLKSIZE;

            }

            io.close();

            //do initial write

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void fillData(ReusableBuffer data) {
        byte cnt = 65;
        while (data.hasRemaining()) {
            data.put(cnt++);
            if (cnt == 91)
                cnt = 65;
        }
    }

    private static boolean checkData(ReusableBuffer data) {
        byte cnt = 65;
        while (data.hasRemaining()) {
            if (data.get() != cnt++)
                return false;
            if (cnt == 91)
                cnt = 65;
        }
        return true;
    }

}
