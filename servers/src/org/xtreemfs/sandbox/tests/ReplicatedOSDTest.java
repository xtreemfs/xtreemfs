/*
 * ReplicatedOSDTest.java
 *
 * Created on August 9, 2007, 10:38 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.json.JSONException;

/**
 * This test requires a manual setup of two OSDs. Several access
 * patterns are simulated to trigger data replication.
 * Data consistency is checked
 * @author bjko
 */
public class ReplicatedOSDTest {

    public final String FILE_ID;

    public static final int    STRIPE_SIZE = 4;

    public static final int    KB = 1024;

    public static final int    WAIT_FOR_LEASE_RETURN = 6000;

    private final List<InetSocketAddress> osds;

    private final Capability cap;

    private final OSDClient client;

    private final Locations loc;

    private static final long RESPONSE_TO = 5000;

    /** Creates a new instance of ReplicatedOSDTest */
    public ReplicatedOSDTest(List<InetSocketAddress> osds) throws IOException, JSONException {

        this.FILE_ID = Long.toHexString(System.currentTimeMillis()/1000).toUpperCase()+":1";
        Logging.logMessage(Logging.LEVEL_INFO,this,"file id is "+FILE_ID);

        this.osds = osds;
        client = new OSDClient(null);
        cap = new Capability(FILE_ID,"rw",System.currentTimeMillis()+1000*60*60,0,"secretPassphrase");

        List<Location> locations = new ArrayList<Location>(osds.size());
        for (InetSocketAddress addr : osds) {
            StripingPolicy sp = new RAID0(STRIPE_SIZE,1);
            List<ServiceUUID> osd = new ArrayList<ServiceUUID>(1);
            osd.add(new ServiceUUID("http://"+addr.getHostName()+":"+addr.getPort()));
            locations.add(new Location(sp,osd));
        }
        loc = new Locations(locations);
        System.out.println("locations: "+loc.asJSONString());
    }

    public void testWriteRead(int numObjects, byte pattern) throws Exception {

        RPCResponse r = client.truncate(osds.get(0),loc,cap,FILE_ID,0);
        r.waitForResponse();
        r.freeBuffers();

        for (int obj = 0; obj < numObjects; obj++) {
            ReusableBuffer buf = BufferPool.allocate(STRIPE_SIZE*KB);
            for (int i = 0; i < STRIPE_SIZE*KB; i++) {
                buf.put(pattern);
            }

            RPCResponse response = client.put(osds.get(0),loc,cap,FILE_ID,obj,buf);
            response.waitForResponse();
            response.freeBuffers();
        }
        Logging.logMessage(Logging.LEVEL_INFO,this,"data writen");

        Thread.sleep(WAIT_FOR_LEASE_RETURN);

        Logging.logMessage(Logging.LEVEL_INFO,this,"start reading");
        for (int obj = 0; obj < numObjects; obj++) {
            RPCResponse response = client.get(osds.get(1),loc,cap,FILE_ID,obj);
            ReusableBuffer data = response.getBody();
            data.position(0);
            for (int i = 0; i < STRIPE_SIZE*KB; i++) {
                if (data.get() != pattern) {
                    throw new IllegalArgumentException("invalid data");
                }
            }

            response.freeBuffers();
        }
        Logging.logMessage(Logging.LEVEL_INFO,this,"testWriteRead successful!");

    }

    public void testInterleavingWrite(int numObjects) throws Exception {

        RPCResponse r = client.truncate(osds.get(0),loc,cap,FILE_ID,0);
        r.waitForResponse();
        r.freeBuffers();

        if (numObjects%2 != 0)
            throw new IllegalArgumentException("numObjects must be an even integer");


        for (int obj = 0; obj < numObjects; obj += 2) {

            ReusableBuffer bufA = BufferPool.allocate(STRIPE_SIZE*KB);
            for (int i = 0; i < STRIPE_SIZE*KB; i++) {
                bufA.put((byte)'a');
            }

            ReusableBuffer bufB = BufferPool.allocate(STRIPE_SIZE*KB);
            for (int i = 0; i < STRIPE_SIZE*KB; i++) {
                bufB.put((byte)'b');
            }
            bufB.position(0);
            bufA.position(0);
            RPCResponse responseA = client.put(osds.get(0),loc,cap,FILE_ID,obj,bufA);
            Logging.logMessage(Logging.LEVEL_DEBUG,this,"wrote a to "+obj);
            RPCResponse responseB = client.put(osds.get(1),loc,cap,FILE_ID,obj+1,bufB);
            Logging.logMessage(Logging.LEVEL_DEBUG,this,"wrote b to "+(obj+1));
            responseA.waitForResponse();
            responseB.waitForResponse();
            responseA.freeBuffers();
            responseB.freeBuffers();
        }

        Thread.sleep(WAIT_FOR_LEASE_RETURN);


        for (int obj = 0; obj < numObjects; obj += 2) {
            RPCResponse responseA = client.get(osds.get(1),loc,cap,FILE_ID,obj);
            RPCResponse responseB = client.get(osds.get(0),loc,cap,FILE_ID,obj+1);
            ReusableBuffer dataA = responseA.getBody();
            ReusableBuffer dataB = responseB.getBody();

            dataA.position(0);
            dataB.position(0);

            for (int i = 0; i < STRIPE_SIZE*KB; i++) {
                byte tmp = dataA.get();
                if (tmp != (byte)'a') {
                    throw new IllegalArgumentException("invalid data: "+tmp+"/"+((char)tmp));
                }
                tmp = dataB.get();
                if (tmp != (byte)'b') {
                    throw new IllegalArgumentException("invalid data: "+tmp+"/"+((char)tmp));
                }
            }

            responseA.freeBuffers();
            responseB.freeBuffers();
        }
        Logging.logMessage(Logging.LEVEL_INFO,this,"interleaving test successful!");

    }


    public void testRoundRobin(int numRounds) throws Exception {

        final int numOsds = osds.size();

        RPCResponse r = client.truncate(osds.get(0),loc,cap,FILE_ID,0);
        r.waitForResponse();
        r.freeBuffers();

        for (int obj = 0; obj < numRounds; obj++) {

            for (int j = 0; j < numOsds; j++) {
                ReusableBuffer bufA = BufferPool.allocate(STRIPE_SIZE*KB);
                for (int i = 0; i < STRIPE_SIZE*KB; i++) {
                    bufA.put((byte)('a'+j));
                }

                bufA.position(0);
                RPCResponse responseA = client.put(osds.get(j),loc,cap,FILE_ID,obj*numOsds+j,bufA);
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"wrote "+((char)('a'+j))+" to "+osds.get(j));
                responseA.waitForResponse();
                responseA.freeBuffers();
            }
        }

        //Thread.sleep(10001);
        Thread.sleep(WAIT_FOR_LEASE_RETURN/2);

        for (int obj = 0; obj < numRounds; obj++) {

            for (int j = 0; j < numOsds; j++) {
                RPCResponse responseA = client.get(osds.get(numOsds-j-1),loc,cap,FILE_ID,obj*numOsds+j);
                ReusableBuffer dataA = responseA.getBody();
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"read from "+osds.get(numOsds-j-1));
                dataA.position(0);
                for (int i = 0; i < STRIPE_SIZE*KB; i++) {
                    byte tmp = dataA.get();
                    if (tmp != (byte)('a'+j)) {
                        throw new IllegalArgumentException("invalid data: "+(char)tmp+"/"+((char)('a'+j)));
                    }
                }

                responseA.freeBuffers();
            }
        }

        Logging.logMessage(Logging.LEVEL_INFO,this,"roundrobin test successful!");

    }

    public void shutdown() {
        client.shutdown();
        client.waitForShutdown();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {

            Logging.start(Logging.LEVEL_INFO);

            List<InetSocketAddress> osds = new ArrayList(2);
            osds.add(new InetSocketAddress("xtreem.zib.de",32640));
            //osds.add(new InetSocketAddress("farnsworth.zib.de",32641));
            osds.add(new InetSocketAddress("pub2-s.ane.cmc.osaka-u.ac.jp",32641));
            osds.add(new InetSocketAddress("planetlab5.flux.utah.edu",32641));

            ReplicatedOSDTest test = new ReplicatedOSDTest(osds);

            Thread.sleep(100);

            //test.testInterleavingWrite(100);
            test.testRoundRobin(50);

            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

    }

}
