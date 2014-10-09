/**
 * Copyright 2012 Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 * Authors: Patrick Schäfer
 */

package org.xtreemfs.contrib.provisioning;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.contrib.provisioning.JsonRPC.METHOD;
import org.xtreemfs.contrib.provisioning.LibJSON.AccessTypes;
import org.xtreemfs.contrib.provisioning.LibJSON.Addresses;
import org.xtreemfs.contrib.provisioning.LibJSON.Attributes;
import org.xtreemfs.contrib.provisioning.LibJSON.ReservationStati;
import org.xtreemfs.contrib.provisioning.LibJSON.ReservationStatus;
import org.xtreemfs.contrib.provisioning.LibJSON.Reservations;
import org.xtreemfs.contrib.provisioning.LibJSON.Resource;
import org.xtreemfs.contrib.provisioning.LibJSON.ResourceCapacity;
import org.xtreemfs.contrib.provisioning.LibJSON.ResourceMapper;
import org.xtreemfs.contrib.provisioning.LibJSON.Resources;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONString;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;


public class JSONRPCTest extends AbstractTestCase {

    @BeforeClass public static void setUpTest() throws Exception {
        STARTUP_LOCAL = true;
    }

    /**
     * Test protocol errors.
     * @throws JSONRPC2ParseException
     */
    @Test
    public void parameterErrors() throws JSONRPC2ParseException {
        System.out.println("parameterErrors");

        // test unknown method
        JSONRPC2Response res = callJSONRPC("listVolumessss", "1");
        checkSuccess(res, true);

        // empty method-name
        res = callJSONRPC("");
        checkSuccess(res, true);

        // missing parameters
        res = callJSONRPC(METHOD.reserveResources);
        checkSuccess(res, true);

        res = callJSONRPC(METHOD.releaseResources);
        checkSuccess(res, true);

        String policy = ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE;
        res = callJSONRPC(METHOD.reserveResources, "testVolume_policies", owner, ownerGroup, mode, policy, "2", "2");
        checkSuccess(res, true);
    }

    /**
     * Test resource aggregation.
     * @throws JSONRPC2ParseException
     * @throws JSONException
     */
    @Test
    public void calculateResourceCapacity() throws JSONRPC2ParseException, JSONException {
        System.out.println("calculateResourceCapacity");

        double originalThroughput = 10.0;
        double reserveThroughput = 8.0;
        double releaseThroughput = 9.0;

        double originalCapacity = 100.0;
        double reserveCapacity = 80.0;
        double releaseCapacity = 90.0;

        ResourceCapacity rc = new ResourceCapacity(
                new Resource(
                        "/"+dirAddress+"/storage/random",
                        "xxx.xxx.xxx.xxx",
                        "Storage",
                        new Attributes(
                                originalCapacity,
                                originalThroughput,
                                AccessTypes.RANDOM),
                        new LibJSON.Cost()
                ),
                new LibJSON.ReserveResource(
                        new Attributes(
                                reserveCapacity,
                                reserveThroughput,
                                AccessTypes.RANDOM)
                ),
                new LibJSON.ReleaseResource(
                        new Attributes(
                                releaseCapacity,
                                releaseThroughput,
                                AccessTypes.RANDOM)
                )
        );

        JSONRPC2Response res = callJSONRPC(METHOD.calculateResourceCapacity, rc);
        ResourceMapper resources = parseResult(res, ResourceMapper.class);
        double capacity = resources.getResource().getAttributes().getCapacity();
        double throughput = resources.getResource().getAttributes().getThroughput();

        assertTrue(capacity == (originalCapacity + releaseCapacity - reserveCapacity));
        assertTrue(throughput == (originalThroughput + releaseThroughput - reserveThroughput));

        checkSuccess(res, false);
    }


    /**
     * Test getting all resource types.
     * @throws JSONRPC2ParseException
     * @throws JSONException
     */
    @Test
    public void getResourceTypes() throws JSONRPC2ParseException, JSONException {
        System.out.println("getResourceTypes");

        JSONRPC2Response res = callJSONRPC(METHOD.getResourceTypes);
        checkSuccess(res, false);
    }


    /**
     * Test creating volumes.
     * @throws JSONRPC2ParseException
     * @throws JSONException
     */
    @Test
    public void createAndDeleteVolumes() throws JSONRPC2ParseException, JSONException {
        System.out.println("createAndDeleteVolumes");

        // create a volume
        Resources resource = new Resources(
                new Resource(
                        "/"+dirAddress+"/storage/random",
                        "xxx.xxx.xxx.xxx",
                        "Storage",
                        new Attributes(
                                100.0,
                                10.0,
                                AccessTypes.RANDOM),
                        new LibJSON.Cost()));

        JSONRPC2Response res = callJSONRPC(METHOD.reserveResources, gson.toJson(resource));
        checkSuccess(res, false);
        Reservations resources = parseResult(res, Reservations.class);
        System.out.println("IResID: " + resources.getReservations().iterator().next());

        // create a second volume
        res = callJSONRPC(METHOD.reserveResources, resource);
        checkSuccess(res, false);
        Reservations resources2 = parseResult(res, Reservations.class);
        System.out.println("IResID: " + resources2.getReservations().iterator().next());

        // delete the second volume
        Reservations reservations = new Reservations(
                resources2.getReservations().iterator().next()
        );
        res = callJSONRPC(METHOD.releaseResources, reservations);
        checkSuccess(res, false);

        // check if there is only one volume left
        res = callJSONRPC(METHOD.listReservations);
        checkSuccess(res, false);
        Addresses volumes = parseResult(res, Addresses.class);

        assertTrue(volumes.Addresses.size() == 1);

        String volume1 = LibJSON.stripVolumeName(resources.getReservations().iterator().next());
        String volume2 = LibJSON.stripVolumeName(resources2.getReservations().iterator().next());

        String response = res.toString();
        assertTrue(response.contains(volume1));
        assertFalse(response.contains(volume2));
    }


    /**
     * Creates 10 volumes and cleans up all volumes.
     * Checks if all volumes have been deleted successfully
     * @throws JSONRPC2ParseException
     * @throws JSONException
     */
    @Test
    public void createListAndDeleteVolumes() throws JSONRPC2ParseException, JSONException {
        System.out.println("createListAndDeleteVolumes");

        // create volumes

        for (int i = 0; i < 5; i++) {
            // create a volume
            Resources resource = new Resources(
                    new Resource(
                            "/"+dirAddress+"/storage/random",
                            "xxx.xxx.xxx.xxx",
                            "Storage",
                            new Attributes(
                                    100.0,
                                    10.0,
                                    AccessTypes.RANDOM),
                            null));

            // create a volume
            JSONRPC2Response res = callJSONRPC(METHOD.reserveResources, resource);
            checkSuccess(res, false);
            Reservations volumeName = parseResult(res, Reservations.class);

            // check if the volume was created
            Reservations reservations = new Reservations(
                    volumeName.getReservations().iterator().next());
            JSONRPC2Response res2 = callJSONRPC(METHOD.verifyResources, reservations);
            checkSuccess(res2, false);
            ReservationStati result2 = parseResult(res2, ReservationStati.class);

            boolean found = false;
            for (ReservationStatus status : result2.getReservations()) {
                if (status.Address.equals(volumeName.getReservations().iterator().next())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

        // list all volumes
        JSONRPC2Response res = callJSONRPC(METHOD.listReservations);
        checkSuccess(res, false);
        Addresses volumes = parseResult(res, Addresses.class);

        for (String volume : volumes.getAddresses()) {
            System.out.println("deleting Volume " + volume);

            // remove each volume
            Reservations addresses = new Reservations(volume);
            res = callJSONRPC(
                    METHOD.releaseResources,
                    new JSONString(gson.toJson(addresses)));
            checkSuccess(res, false);
        }

        System.out.println("List volumes ");
        res = callJSONRPC(METHOD.listReservations);
        checkSuccess(res, false);

        Addresses result2 = parseResult(res, Addresses.class);
        assertTrue(result2.Addresses.isEmpty());
    }


    /**
     * List the available resources
     * @throws JSONRPC2ParseException
     * @throws JSONException
     */
    @Test
    public void getAvailableResources() throws JSONRPC2ParseException, JSONException {
        System.out.println("getAvailableResources");

        JSONRPC2Response res = callJSONRPC(METHOD.getAvailableResources);
        checkSuccess(res, false);
    }


    /**
     * Creates a volume and lists the available resources
     * @throws JSONRPC2ParseException
     * @throws JSONException
     */
    @Test
    public void calculateResourceAgg() throws JSONRPC2ParseException, JSONException {
        System.out.println("calculateResourceAgg");

        // create a volume
        Resources resource = new Resources(
                new Resource(
                        "/"+dirAddress+"/storage/random",
                        "xxx.xxx.xxx.xxx",
                        "Storage",
                        new Attributes(
                                100.0,
                                100.0,
                                AccessTypes.SEQUENTIAL),
                        null));

        JSONRPC2Response res = callJSONRPC(METHOD.calculateResourceAgg, resource);
        checkSuccess(res, false);
    }


    /**
     * Creates a volume and lists the available resources
     * @throws JSONRPC2ParseException
     * @throws JSONException
     */
    @Test
    public void createListAndCheckReservation() throws JSONRPC2ParseException, JSONException {
        System.out.println("createListAndCheckReservation");

        // create a volume
        Resources resource = new Resources(
                new Resource(
                        "/"+dirAddress+"/storage/random",
                        "xxx.xxx.xxx.xxx",
                        "Storage",
                        new Attributes(
                                100.0,
                                100.0,
                                AccessTypes.SEQUENTIAL),
                        null));

//    String json = 
//        "{\"Resources\": [{"
//        +  "\"ID\": \"/"+dirAddress+"/storage/random\","
//        +  "\"IP\": \"xxx.xxx.xxx.xxx\"," 
//        +  "\"Type\": \"Storage\","
//        +  "\"NumInstances\": 1,"        
//        +  "\"Attributes\": "
//        +  "{"
//        +  "  \"Capacity\": 1024,"
//        +  "  \"Throughput\": 100,"
//        +  "  \"ReservationType\": \"SEQUENTIAL\""
//        +  "}"    
//        + "},"
//        + "{"
//        +  "\"ID\": \"/DataCenter1/Rack1/IP2\","
//        +  "\"IP\": \"xxx.xxx.xxx.xxx\"," 
//        +  "\"Type\": \"FPGA\","
//        +  "\"Number\": 5"
//        + "}]}";  

        // parametersMap.put("password", "");
        JSONRPC2Response res = callJSONRPC(METHOD.reserveResources, resource);
        checkSuccess(res, false);

        JSONRPC2Response res2 = callJSONRPC(METHOD.listReservations);
        checkSuccess(res2, false);

        res = callJSONRPC(METHOD.getAvailableResources);
        checkSuccess(res, false);
    }


}
