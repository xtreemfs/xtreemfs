//package org.xtreemfs.sandbox;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//import java.util.StringTokenizer;
//
//import org.xtreemfs.common.Capability;
//import BufferPool;
//import ReusableBuffer;
//import org.xtreemfs.common.clients.RPCResponse;
//import org.xtreemfs.common.clients.RPCResponseListener;
//import org.xtreemfs.common.clients.osd.OSDClient;
//import org.xtreemfs.foundation.logging.Logging;
//import org.xtreemfs.common.striping.Locations;
//import org.xtreemfs.foundation.pinky.HTTPHeaders;
//
//public class ThroughputTest {
//
//    private static int                              responses = 0;
//
//    private static Object                           lock      = new Object();
//
//    private static OSDClient[]                      clients;
//
//    private static Map<InetSocketAddress, Object[]> osdThroughputs;
//
//    /**
//     * @param args
//     */
//    public static void main(String[] args) throws Exception {
//
//        if (args.length != 3) {
//            System.out
//                    .println("usage: ThroughputTest <read|write> <numOSDs> <config.properties>");
//            System.exit(1);
//        }
//
//        boolean write = args[0].equalsIgnoreCase("write");
//        int numOSDs = Integer.parseInt(args[1]);
//
//        Properties props = new Properties();
//        props.load(new FileInputStream(args[2]));
//
//        final int stripeSize = Integer
//                .parseInt(props.getProperty("stripeSize"));
//        final int fileSize = Integer.parseInt(props.getProperty("fileSize"));
//        final boolean debug = props.getProperty("debug").equalsIgnoreCase(
//            "true");
//        final int numClients = Integer
//                .parseInt(props.getProperty("numClients"));
//        final int rwAhead = Integer.parseInt(props.getProperty("rwAhead"));
//
//        final int burstSize = Integer.parseInt(props.getProperty("burstSize"));
//
//        final String secret = props.getProperty("capSecret");
//
//        List<String> osds = new ArrayList<String>(numOSDs);
//        for (int i = 0;; i++) {
//            String osd = props.getProperty("osd" + i);
//            if (osds.size() > 0 && osd == null)
//                break;
//            else if (osd == null)
//                continue;
//            else {
//                osds.add(osd);
//                if (osds.size() == numOSDs)
//                    break;
//            }
//        }
//
//        Logging.start(Logging.LEVEL_ERROR);
//
//        InetSocketAddress[] uris = new InetSocketAddress[osds.size()];
//        for (int i = 0; i < osds.size(); i++) {
//            StringTokenizer st = new StringTokenizer(osds.get(i), ":");
//            uris[i] = new InetSocketAddress(st.nextToken(), Integer.parseInt(st
//                    .nextToken()));
//        }
//
//        final String fileId = "ABC:1";
//        final Capability cap = null;//new Capability(fileId, "w", 0, secret);
//
//
//
//        long t0 = System.nanoTime();
//
//        ReusableBuffer buf = write ? BufferPool.allocate(stripeSize * 1024)
//            : null;
//
//        clients = new OSDClient[numClients];
//        for (int i = 0; i < numClients; i++)
//            clients[i] = new OSDClient(null);
//
//        osdThroughputs = new HashMap<InetSocketAddress, Object[]>();
//
//        for (long i = 0; i < fileSize / stripeSize; i++) {
//
//            final int osd = (int) (i % uris.length);
//            final int client = (int) (i % clients.length);
//
//            HTTPHeaders headers = new HTTPHeaders();
//            headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, i + "");
//
//            if (debug)
//                System.out.println("sending request " + i + " to OSD "
//                    + uris[osd] + " ...");
//            try {
//
//                // send write requests
//                if (write) {
//
//                    ReusableBuffer viewBuf = buf.createViewBuffer();
//
//                    viewBuf.put((byte) 66);
//
//                    final RPCResponse res = clients[client].put(uris[osd], null,
//                        cap, fileId, i, viewBuf);
//                    res.setResponseListener(new RPCResponseListener() {
//                        public void responseAvailable(RPCResponse response) {
//                            synchronized (lock) {
//
//                                responses++;
//
//                                byte[] body = response.getSpeedyRequest()
//                                        .getResponseBody();
//                                if (debug) {
//                                    if (body != null)
//                                        System.out.println("body: "
//                                            + new String(body));
//                                    System.out.println("write complete, "
//                                        + responses + " responses received");
//                                }
//                                res.freeBuffers();
//                                lock.notify();
//                            }
//                        }
//                    });
//                }
//
//                // send read requests
//                else {
//
//                    final RPCResponse res = clients[client].get(uris[osd], null,
//                        cap, fileId, i);
//                    res.setResponseListener(new RPCResponseListener() {
//                        public void responseAvailable(RPCResponse response) {
//                            synchronized (lock) {
//
//                                Object[] array = osdThroughputs.get(response
//                                        .getSpeedyRequest().getServer());
//                                if (array == null) {
//                                    array = new Object[] { new Long(0),
//                                        new Long(0) };
//                                    osdThroughputs.put(response
//                                            .getSpeedyRequest().getServer(),
//                                        array);
//                                }
//                                try {
//                                    array[0] = new Long(((Long) array[0])
//                                            .longValue()
//                                        + response.getBody().capacity());
//                                    array[1] = System.nanoTime();
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                    System.exit(1);
//                                }
//
//                                responses++;
//
//                                // byte[] body = response.getSpeedyRequest()
//                                // .getResponseBody();
//
//                                // ReusableBuffer buf = null;
//                                // try {
//                                // buf = response.getBody();
//                                // } catch (Exception e) {
//                                // e.printStackTrace();
//                                // System.exit(1);
//                                // }
//                                //
//                                // if (buf.capacity() != stripeSize * 1024) {
//                                // System.err.println("wrong body length: "
//                                // + buf.capacity());
//                                // System.exit(1);
//                                // }
//                                //
//                                // if (buf.get() != (byte) 65) {
//                                // System.err.println("invalid body content");
//                                // System.exit(1);
//                                // }
//
//                                if (debug)
//                                    System.out.println("read complete, "
//                                        + responses + " responses received");
//
//                                res.freeBuffers();
//                                lock.notify();
//                            }
//                        }
//                    });
//                }
//
//                if (burstSize == 0 || i % (uris.length * burstSize) == 0) {
//                    synchronized (lock) {
//                        while (i - responses > rwAhead)
//                            lock.wait();
//                    }
//                }
//
//            } catch (IOException exc) {
//                exc.printStackTrace();
//                System.exit(1);
//            }
//        }
//
//        synchronized (lock) {
//            while (responses < fileSize / stripeSize)
//                lock.wait();
//        }
//
//        long time = System.nanoTime() - t0;
//        time = time / 1000000;
//        System.out.println("time elapsed for reading/writing " + fileSize
//            + "kb in " + stripeSize + "kb stripes: " + time + "ms");
//        System.out
//                .println(((fileSize) / (stripeSize * time / 1000)) + " ops/s");
//        System.out.println((fileSize / ((float) time / 1000)) + " kb/s");
//        System.out.println("throughput for OSDs:");
//        for (InetSocketAddress osd : osdThroughputs.keySet()) {
//            Object[] array = osdThroughputs.get(osd);
//            double t = ((Long) array[1] - t0) / 1000000.0f;
//            long size = (Long) array[0] / 1000;
//            System.out
//                    .println(osd + ": " + size / ((float) t / 1000) + " kb/s");
//        }
//
//        for (OSDClient client : clients)
//            client.shutdown();
//
//        System.out.println(BufferPool.getStatus());
//    }
//}