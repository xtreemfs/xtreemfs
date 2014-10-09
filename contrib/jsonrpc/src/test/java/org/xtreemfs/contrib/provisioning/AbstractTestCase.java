package org.xtreemfs.contrib.provisioning;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.minidev.json.JSONObject;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.xtreemfs.contrib.provisioning.JsonRPC.METHOD;
import org.xtreemfs.contrib.provisioning.LibJSON.Addresses;
import org.xtreemfs.contrib.provisioning.LibJSON.Reservations;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

import com.google.gson.Gson;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;

public abstract class AbstractTestCase {

    static boolean STARTUP_LOCAL = true;
    static boolean INITIALIZED = false;

    static JsonRPC xtreemfsRPC = null;
    static TestEnvironment testEnv = null;

    static SchedulerRequestDispatcher scheduler;

    static int requestId = 0;

    static OSD osds[];
    static OSDConfig osdConfigs[];
    static int NUMBER_OF_OSDS = 1;

    static String owner = "CN=Patrick Schaefer,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE";//"myUser"; // TODO!!
    static String ownerGroup = "CN=Patrick Schaefer,OU=CSR,OU=Konrad-Zuse-Zentrum fuer Informationstechnik Berlin (ZIB),O=GridGermany,C=DE";//"myGroup"; // TODO!!
    static String mode = "777";

    static String dirAddress = "";

    public static Gson gson = new Gson();

    @BeforeClass public static void setUp() throws Exception {
        Logging.start(Logging.LEVEL_WARN, Category.all);
        requestId = 0;
    }

    /**
     * Startup servers
     * @throws Exception
     */
    @Before public void setUpServers() throws Exception {
        try {
            if (!INITIALIZED && STARTUP_LOCAL) {
                System.out.println("Starting xtreemfs-services on ports: ");
                System.out.println("DIR port: " + SetupUtils.getDIRAddr().getPort());
                System.out.println("MRC port: " + SetupUtils.getMRC1Addr().getPort());
                System.out.println("Scheduler port: " + SetupUtils.getSchedulerAddr().getPort());

                osdConfigs = SetupUtils.createMultipleOSDConfigs(NUMBER_OF_OSDS);

//      System.out.println("OSD port: " + osdConfigs[0].getPort());

                // startup: DIR && MRC
                testEnv = new TestEnvironment(
                        new TestEnvironment.Services[] {
                                TestEnvironment.Services.DIR_SERVICE,
                                TestEnvironment.Services.TIME_SYNC,
                                TestEnvironment.Services.UUID_RESOLVER,
                                TestEnvironment.Services.MRC,
                                TestEnvironment.Services.SCHEDULER_SERVICE,
                                TestEnvironment.Services.OSD
                        });

                testEnv.start();

//      osds = new OSD[NUMBER_OF_OSDS];
//      for (int i = 0; i < osds.length; i++) {
//        osdConfigs[i].getCustomParams().put(OSDConfig.OSD_CUSTOM_PROPERTY_PREFIX+"country", "DE");
//        osds[i] = new OSD(osdConfigs[i]);
//        System.out.println("OSD port: " + osdConfigs[i].getPort());
//      }

//      SchedulerConfig config = SetupUtils.createSchedulerConfig(false);
//      String capabilityFile = config.getOSDCapabilitiesFile();
//      System.out.println("Scheduler capability file: " + capabilityFile);
//      BufferedWriter output = new BufferedWriter(new FileWriter(capabilityFile));
//      for (OSDConfig osdConfig : osdConfigs) {
//        output.write(osdConfig.getUUID() + ";100.0;100.0;100.0,99.0,98.0,97.0,96.0,95.0\n");
//      }
//      output.flush();
//      output.close();

//      scheduler = new SchedulerRequestDispatcher(config, SetupUtils.createSchedulerdbsConfig());
//      scheduler.start();
//      scheduler.waitForStartup();

//      System.out.println("Scheduler port: " + scheduler.getConfig().getPort());
//      System.out.println("Scheduler DIR port: " + scheduler.getConfig().getDirectoryService().getPort());

                dirAddress = testEnv.getDIRAddress().getHostName();

                // initialize JSONPRC for local installation
                xtreemfsRPC = new JsonRPC("junit_dir");
                xtreemfsRPC.init();
                INITIALIZED = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Shutdown servers
     */
    @AfterClass public static void shutDown() {
        if (STARTUP_LOCAL) {
            try {
                xtreemfsRPC.stop();
            } catch (Exception e) {
            }

            if (osds!=null) {
                for (OSD osd : osds) {
                    if (osd != null) {
                        osd.shutdown();
                    }
                }
            }

            try {
                if (scheduler != null) {
                    scheduler.shutdown();
                    scheduler.waitForShutdown();
                }
            } catch (Exception e) {
                //        e.printStackTrace();
            }

            try {
                if (testEnv != null) {
                    testEnv.shutdown();
                }
            } catch (Exception e) {
                //        e.printStackTrace();
            }

        }
        INITIALIZED = false;
        System.out.println("shutdown successful");
    }

    /**
     * Delete all volumes after each testcase
     */
    @After
    public void cleanUp() throws Exception {
        if (STARTUP_LOCAL) {
            try {
                JSONRPC2Response res = callJSONRPC(METHOD.listReservations);
                checkSuccess(res, false);

                Addresses volumes = parseResult(res, Addresses.class);
                for (String volume: volumes.getAddresses()) {
                    System.out.println("deleting Volume " + volume);

                    // delete the second volume
                    Reservations reservations = new Reservations(
                            volume
                    );

                    res = callJSONRPC(
                            METHOD.releaseResources,
                            reservations);

                    checkSuccess(res, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @SuppressWarnings("unchecked")
    public <E> E parseResult(JSONRPC2Response res, Class<E> classType) {
        return (E) gson.fromJson(
                JSONObject.toJSONString((Map<String, ?>)res.getResult()),
                classType);
    }

    @SuppressWarnings("unchecked")
    protected static JSONRPC2Response callJSONRPC(METHOD method, String jsonRPC) throws JSONRPC2ParseException, JSONException {
        try {
            Map<String, Object> parameters = (Map<String, Object>) JSONParser.parseJSON(new JSONString(jsonRPC));

            JSONRPC2Request req = new JSONRPC2Request(
                    method.toString(),
                    parameters,
                    "id-"+(++requestId));

            System.out.println("\tRequest: \n\t" + req);
            return JSONRPC2Response.parse(xtreemfsRPC.execute(req.toString()), true, true);
        } catch (JSONRPC2ParseException e) {
            e.printStackTrace();
            throw e;
        } catch (JSONException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Helper method for calling JSONRPCs
     * @param method
     * @param parameters
     * @return
     * @throws JSONRPC2ParseException
     * @throws JSONException
     */
    protected static JSONRPC2Response callJSONRPC(METHOD method, JSONString parameters) throws JSONRPC2ParseException, JSONException {
        @SuppressWarnings("unchecked")
        JSONRPC2Request req = new JSONRPC2Request(
                method.toString(),
                (HashMap<String, ?>)JSONParser.parseJSON(parameters),
                "id-"+(++requestId));
        System.out.println("\tRequest: \n\t" + req);
        return JSONRPC2Response.parse(xtreemfsRPC.execute(req.toString()), true, true);
    }

    /**
     * Helper method for calling JSONRPCs
     * @param method
     * @param parameters
     * @return
     * @throws JSONRPC2ParseException
     */
    protected static JSONRPC2Response callJSONRPC(METHOD method, Map<String,?> parameters) throws JSONRPC2ParseException {
        JSONRPC2Request req = new JSONRPC2Request(
                method.toString(),
                parameters,
                "id-"+(++requestId));
        System.out.println("\tRequest: \n\t" + req);
        return JSONRPC2Response.parse(xtreemfsRPC.execute(req.toString()), true, true);
    }

    protected static JSONRPC2Response callJSONRPC(String method, Map<String,?> parameters) throws JSONRPC2ParseException {
        JSONRPC2Request req = new JSONRPC2Request(
                method,
                parameters,
                "id-"+(++requestId));
        System.out.println("\tRequest: \n\t" + req);
        return JSONRPC2Response.parse(xtreemfsRPC.execute(req.toString()), true, true);
    }

    protected static JSONRPC2Response callJSONRPC(JSONRPC2Session mySession, METHOD method, Map<String,?> parameters) throws JSONRPC2ParseException, JSONRPC2SessionException {
        JSONRPC2Request req = new JSONRPC2Request(
                method.toString(),
                parameters,
                "id-"+(++requestId));
        mySession.getOptions().setRequestContentType("application/json");
        System.out.println("\tRequest: \n\t" + req);
        return mySession.send(req);
    }

    protected static JSONRPC2Response callJSONRPC(METHOD method, Object parameters) throws JSONRPC2ParseException, JSONException {
        return callJSONRPC(method, gson.toJson(parameters));
    }

    /**
     * Helper method for calling JSONRPCs
     * @param method
     * @param parameters
     * @return
     * @throws JSONRPC2ParseException
     */
    protected static JSONRPC2Response callJSONRPC(METHOD method, Object... parameters) throws JSONRPC2ParseException {
        JSONRPC2Request req = new JSONRPC2Request(
                method.toString(),
                parameters != null? Arrays.asList(parameters):new ArrayList<String>(),
                "id-"+(++requestId));
        System.out.println("\tRequest: \n\t" + req);
        return JSONRPC2Response.parse(xtreemfsRPC.execute(req.toString()), true, true);
    }

    protected static JSONRPC2Response callJSONRPC(String method, Object... parameters) throws JSONRPC2ParseException {
        JSONRPC2Request req = new JSONRPC2Request(
                method.toString(),
                parameters != null? Arrays.asList(parameters):new ArrayList<String>(),
                "id-"+(++requestId));
        System.out.println("\tRequest: \n\t" + req);
        return JSONRPC2Response.parse(xtreemfsRPC.execute(req.toString()), true, true);
    }

    protected static JSONRPC2Response callJSONRPC(JSONRPC2Session mySession, METHOD method, Object... parameters) throws JSONRPC2ParseException, JSONRPC2SessionException {
        JSONRPC2Request req = new JSONRPC2Request(
                method.toString(),
                parameters != null? Arrays.asList(parameters):new ArrayList<String>(),
                "id-"+(++requestId));
        mySession.getOptions().setRequestContentType("application/json");
        System.out.println("\tRequest: \n\t" + req);
        return mySession.send(req);
    }

    protected static void checkSuccess(JSONRPC2Response res, boolean errorExpected) {
        System.out.println("\tResponse: \n\t" + res + "\n");

        if (!errorExpected) {
            assertTrue(res.indicatesSuccess());
            assertNull(res.getError());
            assertTrue(res.toString().contains("result"));
            assertTrue(!res.toString().contains("error"));
        }
        else {
            assertFalse(res.indicatesSuccess());
            assertNotNull(res.getError());
            assertTrue(!res.toString().contains("result"));
            assertTrue(res.toString().contains("error"));
        }
    }
}
