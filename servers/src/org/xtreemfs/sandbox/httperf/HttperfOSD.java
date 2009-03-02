/*
 * Main.java
 *
 * Created on 8. Dezember 2006, 10:21
 *
 * @author Bjoern Kolbeck, Zuse Institute Berlin (kolbeck@zib.de)
 *
 */

package org.xtreemfs.sandbox.httperf;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;

import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.sandbox.httperf.HttPerfRequestController;

/**
 * A sample OSD.
 *
 * @author bjko
 */
public class HttperfOSD {


//    private HttPerfRequestController controller;
//
//    private HeartbeatThread heartbeatThread = null;
//
//    public static final String PROTOCOL = "http://";
//
//    private DIRClient client;
//
//    /**
//     * Creates a new instance of Main
//     */
//    public HttperfOSD(OSDConfig config, OSDId me, boolean dirServiceInUse) {
//        try {
//            client = new DIRClient(null,config.getDirectoryService());
//            TimeSync.initialize(client,config.getRemoteTimeSync(),config.getLocalClockRenew(),"nullauth bla bla");
//            controller = new HttPerfRequestController(config,me);
//			controller.start();
//
//            if (dirServiceInUse) {
//                heartbeatThread = new HeartbeatThread(client,me.toString());
//                heartbeatThread.start();
//            }
//        } catch (IOException ex) {
//            Logging.logMessage(Logging.LEVEL_ERROR,this,ex);
//        }
//    }
//
//    public void shutdown() {
//        controller.shutdown();
//        if (heartbeatThread != null) {
//            heartbeatThread.shutdown();
//            try {
//                heartbeatThread.join();
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        client.shutdown();
//    }
//
//    public HttPerfRequestController getController() {
//        return controller;
//    }
//
//    /**
//     * Main routine
//     *
//     * @param args
//     *            the command line arguments
//     */
//    public static void main(String[] args) throws Exception {
//
//        String fname = (args.length > 0) ? args[0] : "../config/osdconfig.properties";
//
//        OSDConfig config = new OSDConfig(fname);
//
//        Logging.start(config.getDebugLevel());
//
//        Thread.currentThread().setName("OSD thr.");
//
//        String me = PROTOCOL;
//        try {
//            me += InetAddress.getLocalHost().getCanonicalHostName();
//        } catch (UnknownHostException ex) {
//            Logging.logMessage(Logging.LEVEL_ERROR,null,ex);
//            return;
//        }
//
//        me += ":" + config.getPort();
//        Logging.logMessage(Logging.LEVEL_INFO,null,"my ID is "+me);
//
//        new HttperfOSD(config,new OSDId(me, OSDId.SCHEME_HTTP), true);
//    }
//
//    static class HeartbeatThread extends Thread {
//
//        private InetSocketAddress dirserv;
//
//        private DIRClient client;
//
//        private boolean finished;
//
//        private final String uuid;
//
//        public HeartbeatThread(DIRClient client, String uuid) {
//            super("OSD HB thr.");
//            this.client = client;
//            this.uuid = uuid;
//        }
//
//        public void run() {
//
//            boolean register = true;
//            try {
//                //deregister old data
//                RPCResponse r = client.deregisterEntity(uuid,"nullauth " + uuid);
//                r.waitForResponse();
//                r.freeBuffers();
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//
//            while (!finished) {
//                try {
//
//                    if (register) {
//                        // update the timestamp
//                        RPCResponse response = client.registerEntity(uuid, RPCClient.generateMap("uuid",uuid,"type","OSD","free","1000000"), "nullauth " + uuid);
//                        response.waitForResponse();
//                        response.freeBuffers();
//                        register = false;
//                        Logging.logMessage(Logging.LEVEL_INFO,this,"registered with directory service");
//                    } else {
//                        // update the timestamp
//                        RPCResponse response = client.registerEntity(uuid, new HashMap<String, Object>(), "nullauth " + uuid);
//                        response.waitForResponse();
//                        response.freeBuffers();
//                        Logging.logMessage(Logging.LEVEL_DEBUG,this,"sent heartbeat signal to directory service");
//                    }
//
//                } catch (IOException ex) {
//                    Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
//                } catch (JSONException ex) {
//                    Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
//                } catch (InterruptedException ex) {
//                    Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
//                }
//
//                try {
//                    synchronized (this) {
//                        this.wait(1 * 60 * 1000);
//                    }
//                } catch (InterruptedException ex) {
//                    // ignore
//                }
//            }
//        }
//
//        public void shutdown() {
//            finished = true;
//            this.interrupt();
//        }
//    }

}
