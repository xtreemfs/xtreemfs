//package org.xtreemfs.sandbox.tests;
//
//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;
//
//import org.xtreemfs.common.Capability;
//import BufferPool;
//import ReusableBuffer;
//import org.xtreemfs.common.clients.HttpErrorException;
//import org.xtreemfs.common.clients.RPCResponse;
//import org.xtreemfs.common.clients.RPCResponseListener;
//import org.xtreemfs.common.clients.osd.OSDClient;
//import org.xtreemfs.foundation.logging.Logging;
//import org.xtreemfs.common.striping.Location;
//import org.xtreemfs.common.striping.Locations;
//import org.xtreemfs.common.striping.RAID0;
//import org.xtreemfs.common.striping.StripingPolicy;
//import org.xtreemfs.common.uuids.ServiceUUID;
//import org.xtreemfs.foundation.pinky.HTTPHeaders;
//
//public class OSDTestClient {
//
//    class ThroughputMonitor extends Thread {
//
//        private ResponseCollector rc;
//
//        private boolean           shutdown;
//
//        ThroughputMonitor(ResponseCollector rc) {
//            this.rc = rc;
//        }
//
//        public void run() {
//
//            long t = System.currentTimeMillis();
//            long resp = rc.getNumberOfResponses();
//
//            System.out.println("avrg. # ops/s:");
//            while (!shutdown) {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                long oldResp = resp;
//                long oldT = t;
//                resp = rc.getNumberOfResponses();
//                t = System.currentTimeMillis();
//
//                double rate = (double) (resp - oldResp) * 1000 / (t - oldT);
//
//                System.out.println(rate + ";" + resp + ";" + rc.getFailures()
//                    + ";" + rc.getRedirects());
//
//                if (rc.isDone()) {
//                    shutdown = true;
//                    System.out.println("failure rate: " + rc.getFailureRate()
//                        + " (" + rc.getFailures() + " failures)");
//                    System.out.println("redirects: " + rc.getRedirects());
//                    System.out.println("total time: " + rc.getTotalTime());
//                }
//            }
//        }
//    }
//
//    class ResponseCollector implements RPCResponseListener {
//
//        private int           responded;
//
//        private int           failed;
//
//        private int           redirected;
//
//        private long          startTime;
//
//        private long          endTime;
//
//        private Configuration config;
//
//        public ResponseCollector(Configuration config) {
//            this.responded = 0;
//            this.failed = 0;
//            this.redirected = 0;
//            this.config = config;
//        }
//
//        public void responseAvailable(RPCResponse response) {
//
//            if (startTime == 0)
//                startTime = System.currentTimeMillis();
//
//            try {
//
//                try {
//
//                    // check if an exception has occurred
//                    response.waitForResponse();
//
//                    Object[] context = (Object[]) response.getAttachment();
//                    // assert body size for read requests
//                    assert (context.length != 4 || response.getBody() != null
//                        && response.getBody().capacity() == config.stripeSize
//                            * KB);
//
//                } catch (HttpErrorException exc) {
//
//                    // handle redirect
//                    if (exc.getStatusCode() >= 300 && exc.getStatusCode() < 400) {
//                        String target = response.getSpeedyRequest().responseHeaders
//                                .getHeader(HTTPHeaders.HDR_LOCATION);
//                        assert (target != null);
//                        Logging.logMessage(Logging.LEVEL_INFO, this,
//                            "redirect to " + target);
//
//                        // get the request context
//                        Object[] context = (Object[]) response.getAttachment();
//                        Locations loc = (Locations) context[0];
//                        Capability cap = (Capability) context[1];
//                        int obj = (Integer) context[2];
//                        String fileId = (String) context[3];
//                        ReusableBuffer data = null;
//                        if (context.length == 5)
//                            data = (ReusableBuffer) context[4];
//
//                        int ind = target.lastIndexOf(':');
//                        InetSocketAddress osd = new InetSocketAddress(target
//                                .substring("http://".length(), ind), Integer
//                                .parseInt(target.substring(ind + 1)));
//
//                        // redirect request
//                        RPCResponse newRes = data == null ? client.get(osd,
//                            loc, cap, fileId, obj) : client.put(osd, loc, cap,
//                            fileId, obj, data.createViewBuffer());
//                        newRes.setAttachment(context);
//                        newRes.setResponseListener(this);
//
//                        redirected++;
//                        return;
//                    }
//
//                    failed++;
//                    exc.printStackTrace();
//
//                } catch (IOException exc) {
//                    exc.printStackTrace();
//                    failed++;
//                } finally {
//                    response.freeBuffers();
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            responded++;
//            if (responded >= config.numRequests)
//                endTime = System.currentTimeMillis();
//        }
//
//        public boolean isDone() {
//            return responded == config.numRequests;
//        }
//
//        public int getNumberOfResponses() {
//            return responded;
//        }
//
//        public double getFailureRate() {
//            return (double) failed / responded;
//        }
//
//        public int getFailures() {
//            return failed;
//        }
//
//        public int getRedirects() {
//            return redirected;
//        }
//
//        public long getTotalTime() {
//            return endTime - startTime;
//        }
//
//    }
//
//    class Configuration {
//
//        public static final String     PROP_OSD          = "osd";
//
//        public static final String     PROP_STRIPESIZE   = "stripeSize";
//
//        public static final String     PROP_NUM_REQUESTS = "numRequests";
//
//        public static final String     PROP_NUM_OBJECTS  = "numObjects";
//
//        public static final String     PROP_FIRST_OBJECT = "firstObject";
//
//        public static final String     PROP_STEP_SIZE    = "stepSize";
//
//        public static final String     PROP_POLICY       = "policy";
//
//        public static final String     PROP_FILE_ID      = "fileId";
//
//        public static final String     PROP_TARGET_OSD   = "targetOSD";
//
//        public static final String     PROP_OPERATION    = "operation";
//
//        public static final String     PROP_INIT_DELAY   = "initialDelay";
//
//        public static final String     SEQ_READ          = "sequentialRead";
//
//        public static final String     RND_READ          = "randomRead";
//
//        public static final String     SEQ_WRITE         = "sequentialWrite";
//
//        public static final String     RND_WRITE         = "randomWrite";
//
//        public List<InetSocketAddress> osds;
//
//        public int                     stripeSize;
//
//        public int                     numRequests;
//
//        public int                     numObjects;
//
//        public String                  policy;
//
//        public String                  fileId;
//
//        public int                     targetOSD;
//
//        public String                  operation;
//
//        public long                    initialDelay;
//
//        public int                     firstObject;
//
//        public int                     stepSize;
//
//        public Configuration() throws IOException {
//            this(null);
//
//            osds.add(new InetSocketAddress("csr-pc24.zib.de", 32640));
//            // osds.add(new InetSocketAddress("csr-pc24.zib.de", 32641));
//            osds.add(new InetSocketAddress("xtreem.zib.de", 32637));
//            // osds.add(new InetSocketAddress("opt.csc.ncsu.edu", 32637));
//        }
//
//        public Configuration(String file) throws IOException {
//
//            Properties props = new Properties();
//            if (file != null)
//                props.load(new FileInputStream(file));
//
//            // parse the OSD list
//            osds = new ArrayList<InetSocketAddress>();
//            for (int i = 1;; i++) {
//
//                if (!props.containsKey(PROP_OSD + i))
//                    break;
//
//                String osd = props.getProperty(PROP_OSD + i);
//                int colon = osd.lastIndexOf(':');
//                String host = osd.substring(0, colon);
//                int port = Integer.parseInt(osd.substring(colon + 1));
//                osds.add(new InetSocketAddress(host, port));
//            }
//
//            stripeSize = Integer.parseInt(props.getProperty(PROP_STRIPESIZE,
//                "4"));
//            numRequests = Integer.parseInt(props.getProperty(PROP_NUM_REQUESTS,
//                "20000"));
//            numObjects = Integer.parseInt(props.getProperty(PROP_NUM_OBJECTS,
//                "1000"));
//            firstObject = Integer.parseInt(props.getProperty(PROP_FIRST_OBJECT,
//                "0"));
//            stepSize = Integer.parseInt(props.getProperty(PROP_STEP_SIZE, "1"));
//            policy = props.getProperty(PROP_POLICY, "lazy");
//            fileId = props.getProperty(PROP_FILE_ID, Long.toHexString(
//                System.currentTimeMillis() / 1000).toUpperCase()
//                + ":1");
//            targetOSD = Integer.parseInt(props
//                    .getProperty(PROP_TARGET_OSD, "0")) - 1;
//            operation = props.getProperty(PROP_OPERATION, RND_WRITE);
//            initialDelay = Long.parseLong(props.getProperty(PROP_INIT_DELAY,
//                "-1"));
//        }
//
//        public String toString() {
//
//            StringBuffer buf = new StringBuffer();
//            buf.append("          OSD list:\n");
//            for (InetSocketAddress osd : osds)
//                buf.append("                    " + osd + "\n");
//
//            buf.append("         operation: " + operation + "\n");
//            buf.append("number of requests: " + numRequests + "\n");
//            buf.append(" number of objects: " + numObjects + "\n");
//            buf.append("      first object: " + firstObject + "\n");
//            buf.append("          stepSize: " + stepSize + "\n");
//            buf.append("        target OSD: "
//                + (targetOSD == -1 ? "random" : osds.get(targetOSD)) + "\n");
//            buf.append("     update policy: " + policy + "\n");
//            buf.append("           file ID: " + fileId + "\n");
//            buf.append("       stripe size: " + stripeSize + "\n");
//            buf.append("     initial delay: " + initialDelay + "\n");
//
//            return buf.toString();
//        }
//    }
//
//    private static final int    KB       = 1024;
//
//    private static final byte   PATTERN1 = (byte) 'X';
//
//    private static final byte   PATTERN2 = (byte) 'Y';
//
//    private static final long   TIMEOUT  = 5000;
//
//    private final OSDClient  client;
//
//    private final Configuration config;
//
//    public OSDTestClient(String configFile) throws Exception {
//        this.client = new OSDClient(null);
//        this.config = configFile == null ? new Configuration()
//            : new Configuration(configFile);
//        System.out.println(config.toString());
//    }
//
//    public void testRead(Configuration config) throws Exception {
//
//        Capability cap = null;/*new Capability(config.fileId, "rw", System
//                .currentTimeMillis() + 1000 * 60 * 60, 0, "secretPassphrase");*/
//
//        // create a locations list with the given replication policy
//        Locations loc = createLocations(config);
//
//        ResponseCollector rc = new ResponseCollector(config);
//        ThroughputMonitor calc = new ThroughputMonitor(rc);
//        calc.start();
//
//        // perform reads
//        for (int i = 0; i < config.numRequests; i++) {
//
//            InetSocketAddress osd = config.targetOSD == -1 ? config.osds
//                    .get((int) (Math.random() * config.osds.size()))
//                : config.osds.get(config.targetOSD);
//            int obj = config.operation.equals(Configuration.SEQ_READ) ? (config.firstObject + i
//                * config.stepSize)
//                % config.numObjects
//                : (int) (Math.random() * config.numObjects);
//
//            RPCResponse response = client
//                    .get(osd, loc, cap, config.fileId, obj);
//            response
//                    .setAttachment(new Object[] { loc, cap, obj, config.fileId });
//            response.setResponseListener(rc);
//        }
//
//        calc.join();
//    }
//
//    public void testWrite(Configuration config) throws Exception {
//
//        Capability cap = null;/*new Capability(config.fileId, "rw", System
//                .currentTimeMillis() + 1000 * 60 * 60, 0, "secretPassphrase");*/
//
//        // create a locations list with the given replication policy
//        Locations loc = createLocations(config);
//
//        ResponseCollector rc = new ResponseCollector(config);
//        ThroughputMonitor calc = new ThroughputMonitor(rc);
//        calc.start();
//
//        // perform writes
//        for (int i = 0; i < config.numRequests; i++) {
//
//            ReusableBuffer buf = allocateAndFillBuffer(config.stripeSize,
//                PATTERN1);
//
//            InetSocketAddress osd = config.targetOSD == -1 ? config.osds
//                    .get((int) (Math.random() * config.osds.size()))
//                : config.osds.get(config.targetOSD);
//            int obj = config.operation.equals(Configuration.SEQ_WRITE) ? (config.firstObject + i
//                * config.stepSize)
//                % config.numObjects
//                : (int) (Math.random() * config.numObjects);
//
//            RPCResponse response = client.put(osd, loc, cap, config.fileId,
//                obj, buf);
//            response.setAttachment(new Object[] { loc, cap, obj, config.fileId,
//                buf });
//            response.setResponseListener(rc);
//        }
//
//        calc.join();
//    }
//
//    public void runTest() throws Exception {
//
//        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
//
//        if (config.initialDelay == -1) {
//            System.out.println("press ENTER to start test run");
//            r.readLine();
//        } else {
//            Thread.sleep(config.initialDelay);
//        }
//
//        if (config.operation.equals(Configuration.RND_READ)
//            || config.operation.equals(Configuration.SEQ_READ))
//            testRead(config);
//        else if (config.operation.equals(Configuration.RND_WRITE)
//            || config.operation.equals(Configuration.SEQ_WRITE))
//            testWrite(config);
//        else
//            System.err.println("invalid operation: " + config.operation);
//
//        client.shutdown();
//        client.waitForShutdown();
//    }
//
//    private Locations createLocations(Configuration config) {
//        List<Location> locations = new ArrayList<Location>(config.osds.size());
//        for (InetSocketAddress addr : config.osds) {
//            StripingPolicy sp = new RAID0(config.stripeSize, 1);
//            List<ServiceUUID> osd = new ArrayList<ServiceUUID>(1);
//            osd.add(new ServiceUUID("http://"+addr.getHostName()+":"+addr.getPort()));
//            locations.add(new Location(sp, osd));
//        }
//        return new Locations(locations, 0, config.policy, 0);
//    }
//
//    private static ReusableBuffer allocateAndFillBuffer(int stripeSize,
//        byte pattern) {
//        ReusableBuffer buf = BufferPool.allocate(stripeSize * KB);
//        for (int i = 0; i < stripeSize * KB; i++)
//            buf.put(pattern);
//
//        return buf;
//    }
//
//    public static void main(String[] args) {
//
//        try {
//            Logging.start(Logging.LEVEL_ERROR);
//            OSDTestClient client = new OSDTestClient(args.length == 0 ? null
//                : args[0]);
//            client.runTest();
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            System.exit(1);
//        }
//
//    }
//
//}
