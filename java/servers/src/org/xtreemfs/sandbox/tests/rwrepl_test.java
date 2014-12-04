/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox.tests;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.PBRPCServiceURL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;

import java.io.IOException;
import java.net.InetSocketAddress;

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
            final PBRPCServiceURL dir = new PBRPCServiceURL(args[0], "oncrpc", 32638);
            final int    numReplicas = Integer.valueOf(args[1]);
            final int    fileSize = Integer.valueOf(args[2])*1024*1024;
            String mode = "rw";
            if (args.length > 3) {
                mode = args[3];
            }

            Logging.start(Logging.LEVEL_INFO, Category.all);
            TimeSync.initializeLocal(50);

            final UserCredentials uc = UserCredentials.newBuilder().setUsername("test").addGroups("users").build();

            Client c = new Client(new InetSocketAddress[]{new InetSocketAddress(dir.getHost(), dir.getPort())}, 15*1000, 5*60*1000, null);
            c.start();

            try {
                StripingPolicy sp = StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0).setWidth(1).setStripeSize(128).build();
                c.createVolume(VOLNAME, RPCAuthentication.authNone, uc, sp, AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, 0777);
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

            if (!mode.contains("x"))
                addReplicas(f,numReplicas,false);

            RandomAccessFile io = f.open("rw", 0444);
            long datacnt = 0;
            
            System.out.println("replica: "+io.getCurrentReplica()+" = "+f.getReplica(io.getCurrentReplica()));

            if (mode.contains("w")) {
                
                while (datacnt < fileSize) {

                    ReusableBuffer data = BufferPool.allocate(BLKSIZE);
                    fillData(data);
                    data.flip();

                    io.write(data);
                    System.out.print("w");

                    BufferPool.free(data);
                    datacnt += BLKSIZE;

                }
                System.out.println("");
                System.out.println("writing complete");
            }

            if (mode.contains("t")) {
                System.out.print("t");
                io.setLength(fileSize/2);
                ReusableBuffer data = BufferPool.allocate(BLKSIZE);
                fillData(data);
                data.flip();

                io.write(data);
                System.out.println("w");

                BufferPool.free(data);
            }



            io.close();

            Thread.sleep(WAIT_FOR_CLOSE);



            if (mode.contains("x"))
                addReplicas(f,numReplicas,false);

            io = f.open("rw", 0444);

            int nextRepl = (int)(Math.random()*((double)(numReplicas)))+1;
            System.out.println("switch to replica "+nextRepl);
            io.forceReplica(nextRepl);
            System.out.println("replica: "+io.getCurrentReplica()+" = "+f.getReplica(io.getCurrentReplica()));

            datacnt = 0;
            if (mode.contains("r")) {
                while (datacnt < fileSize) {

                    ReusableBuffer data = BufferPool.allocate(BLKSIZE);

                    io.read(data);
                    System.out.print("r");
                    data.flip();
                    if (checkData(data) == false)
                        throw new Exception("invalid data read");

                    BufferPool.free(data);
                    datacnt += BLKSIZE;

                }
                System.out.println("");
                System.out.println("read complete");
            }
            

            io.close();

            c.stop();
            

            //do initial write

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void addReplicas(File f, int numReplicas, boolean quorum) throws Exception {
        System.out.println("adding "+numReplicas+" replicas");
        if (quorum)
            f.setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ);
        else
            f.setReplicaUpdatePolicy(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE);
        for (int i = 0; i < numReplicas; i++) {
            String[] suitableOSDs = f.getSuitableOSDs(1);
            System.out.println("suitable OSDs: "+suitableOSDs);
            if (suitableOSDs.length != 1)
                throw new IOException("cannot add OSDs, no suitable OSD");
            f.addReplica(1, suitableOSDs, 0);
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
