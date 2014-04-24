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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.contrib.provisioning.JsonRPC.METHOD;
import org.xtreemfs.contrib.provisioning.LibJSON.Addresses;
import org.xtreemfs.contrib.provisioning.LibJSON.Machines;
import org.xtreemfs.contrib.provisioning.LibJSON.Reservation;
import org.xtreemfs.contrib.provisioning.LibJSON.ReservationStatus;
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
   * Test creating volumes.
   * @throws JSONRPC2ParseException
   * @throws JSONException 
   */
  @Test
  public void createAndDeleteVolumes() throws JSONRPC2ParseException, JSONException {
    System.out.println("createAndDeleteVolumes");

    // create a volume
    String json = 
        "{\"Resources\": [{"
        +  "\"ID\": \"/"+dirAddress+"/storage/random\","
        +  "\"IP\": \"xxx.xxx.xxx.xxx\"," 
        +  "\"Type\": \"Storage\","
        +  "\"NumInstances\": 1,"
        +  "\"Attributes\": "
        +  "{"
        +  "  \"Capacity\": 100,"
        +  "  \"Throughput\": 10,"
        +  "  \"ReservationType\": \"RANDOM\""
        +  "}"    
        + "}]}";   

    JSONRPC2Response res = callJSONRPC(METHOD.reserveResources, json);
    checkSuccess(res, false);
    Machines volumeName = parseResult(res, Machines.class);   
    System.out.println("IResID: " + volumeName.getResources().iterator().next().IResID);
    
    // create a second volume
    res = callJSONRPC(METHOD.reserveResources, json);
    checkSuccess(res, false);
    Machines volumeName2 = parseResult(res, Machines.class);
    System.out.println("IResID: " + volumeName2.getResources().iterator().next().IResID);
    
    // delete the second volume
    HashMap<String, Object> parametersMap = new HashMap<String, Object>();
    parametersMap.put("IResID", volumeName2.getResources().iterator().next().IResID);
    res = callJSONRPC(METHOD.releaseResources, parametersMap);
    checkSuccess(res, false);
   
    // check if there is only one volume left
    res = callJSONRPC(METHOD.listReservations);
    checkSuccess(res, false);
    Addresses volumes = parseResult(res, Addresses.class);   
    
    assertTrue(volumes.Addresses.size() == 1);
    
    String volume1 = LibJSON.stripVolumeName(volumeName.getResources().iterator().next().IResID);
    String volume2 = LibJSON.stripVolumeName(volumeName2.getResources().iterator().next().IResID);
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
  @SuppressWarnings("unchecked")
  @Test
  public void createListAndDeleteVolumes() throws JSONRPC2ParseException, JSONException {
    System.out.println("createListAndDeleteVolumes");

    // create volumes

    for (int i = 0; i < 5; i++) {
      // create a volume
      String json = 
          "{\"Resources\": [{"
          +  "\"ID\": \"/"+dirAddress+"/storage/random\","
          +  "\"IP\": \"xxx.xxx.xxx.xxx\"," 
          +  "\"Type\": \"Storage\","
          +  "\"NumInstances\": 1,"
          +  "\"Attributes\": "
          +  "{"
          +  "  \"Capacity\": 100,"
          +  "  \"Throughput\": 10,"
          +  "  \"ReservationType\": \"RANDOM\""
          +  "}"    
          + "}]}";   
      
      // create a volume
      JSONRPC2Response res = callJSONRPC(METHOD.reserveResources, json);
      checkSuccess(res, false);     
      Machines volumeName = parseResult(res, Machines.class);      
      
      // check if the volume was created
      JSONRPC2Response res2 = callJSONRPC(
          METHOD.checkReservationStatus, 
          new JSONString(gson.toJson(volumeName.Resources.iterator().next())));
      checkSuccess(res2, false);
      ReservationStatus result2 = parseResult(res2, ReservationStatus.class);
      
      boolean found = false;
      for (String s : result2.Addresses) {
        if (s.equals(volumeName.getResources().iterator().next().IResID)) {
          found = true;
          break;
        }
      }
      assertTrue(found);
    }

    // list all volumes
    JSONRPC2Response res = callJSONRPC(METHOD.listReservations);
    checkSuccess(res, false);

    Map<String, List<String>> volumes = (Map<String, List<String>>) res.getResult();
    for (List<String> v : volumes.values()) {
      for (String volume : v) {
        System.out.println("deleting Volume " + volume);
              
        // remove each volume
        Reservation addresses = new Reservation(volume);
        res = callJSONRPC(
            METHOD.releaseResources, 
            new JSONString(gson.toJson(addresses)));
        checkSuccess(res, false);
      }
    }

    System.out.println("List volumes ");
    res = callJSONRPC(METHOD.listReservations);
    checkSuccess(res, false);

    Addresses result2 = parseResult(res, Addresses.class);   
    assertTrue(result2.Addresses.isEmpty());
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
    String json = 
        "{\"Resources\": [{"
        +  "\"ID\": \"/"+dirAddress+"/storage/random\","
        +  "\"IP\": \"xxx.xxx.xxx.xxx\"," 
        +  "\"Type\": \"Storage\","
        +  "\"NumInstances\": 1,"        
        +  "\"Attributes\": "
        +  "{"
        +  "  \"Capacity\": 1024,"
        +  "  \"Throughput\": 100,"
        +  "  \"ReservationType\": \"SEQUENTIAL\""
        +  "}"    
        + "},"
        + "{"
        +  "\"ID\": \"/DataCenter1/Rack1/IP2\","
        +  "\"IP\": \"xxx.xxx.xxx.xxx\"," 
        +  "\"Type\": \"FPGA\","
        +  "\"Number\": 5"
        + "}]}";  

    // parametersMap.put("password", "");
    JSONRPC2Response res = callJSONRPC(METHOD.reserveResources, json);
    checkSuccess(res, false);

    JSONRPC2Response res2 = callJSONRPC(METHOD.listReservations);
    checkSuccess(res2, false);

    res = callJSONRPC(METHOD.getAvailableResources);
    checkSuccess(res, false);
  }
  

}
