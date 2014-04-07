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
import org.xtreemfs.foundation.json.JSONException;

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
    res = callJSONRPC(METHOD.createReservation);
    checkSuccess(res, true);

    res = callJSONRPC(METHOD.releaseReservation);
    checkSuccess(res, true);

    String policy = ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE;
    res = callJSONRPC(METHOD.createReservation, "testVolume_policies", owner, ownerGroup, mode, policy, "2", "2");
    checkSuccess(res, true);
  }

  /**
   * Test creating volumes.
   * @throws JSONRPC2ParseException
   * @throws JSONException 
   */
  @SuppressWarnings("unchecked")
  @Test
  public void createAndDeleteVolumes() throws JSONRPC2ParseException, JSONException {
    System.out.println("createAndDeleteVolumes");

    // create a volume
    String json = 
        "{\"Resources\": [{"
        +  "\"ID\": \"/"+dirAddress+"/storage/random\","
        +  "\"IP\": \"xxx.xxx.xxx.xxx\"," 
        +  "\"Type\": \"Storage\","
        +  "\"Attributes\": "
        +  "{"
        +  "  \"Capacity\": 100,"
        +  "  \"Throughput\": 10,"
        +  "  \"ReservationType\": \"RANDOM\""
        +  "}"    
        + "}]}";   

    JSONRPC2Response res = callJSONRPC(METHOD.createReservation, json);
    checkSuccess(res, false);
    Map<String, String> vol = (Map<String, String>) res.getResult();
    System.out.println("InfReservID: " + vol.get("InfReservID"));
    
    // create the volume a second time
    res = callJSONRPC(METHOD.createReservation, json);
    checkSuccess(res, false);
    Map<String, String> vol2 = (Map<String, String>) res.getResult();
    System.out.println("InfReservID: " + vol2.get("InfReservID"));
    
    // delete the volume
    HashMap<String, Object> parametersMap = new HashMap<String, Object>();
    parametersMap.put("InfReservID", vol2.get("InfReservID"));
    res = callJSONRPC(METHOD.releaseReservation, parametersMap);
    checkSuccess(res, false);
   
    res = callJSONRPC(METHOD.listReservations);
    checkSuccess(res, false);
    
    Map<String, List<String>> volumes = (Map<String, List<String>>) res.getResult();
    assertTrue(volumes.size() == 1);
    
    String volume1 = AbstractRequestHandler.stripVolumeName(vol.get("InfReservID"));
    String volume2 = AbstractRequestHandler.stripVolumeName(vol2.get("InfReservID"));
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
          +  "\"Attributes\": "
          +  "{"
          +  "  \"Capacity\": 100,"
          +  "  \"Throughput\": 10,"
          +  "  \"ReservationType\": \"RANDOM\""
          +  "}"    
          + "}]}";   
      
      // parametersMap.put("password", "");
      JSONRPC2Response res = callJSONRPC(METHOD.createReservation, json);
      checkSuccess(res, false);
      Map<String, String> result = (Map<String, String>) res.getResult();
      String volumeName = result.get("InfReservID");
      
      String json2 = 
           "{"
          +  "\"InfReservID\": \""+volumeName+"\""
          +"}";
      
      JSONRPC2Response res2 = callJSONRPC(METHOD.checkReservation, json2);
      checkSuccess(res2, false);

      boolean found = false;
      Map<String, List<String>> result2 = (Map<String, List<String>>) res2.getResult();
      for (String s : result2.get("Addresses")) {
        if (s.equals(volumeName)) {
          found = true;
          break;
        }
      }
      assertTrue(found);
    }

    JSONRPC2Response res = callJSONRPC(METHOD.listReservations);
    checkSuccess(res, false);

    Map<String, List<String>> volumes = (Map<String, List<String>>) res.getResult();
    for (List<String> v : volumes.values()) {
      for (String volume : v) {
        System.out.println("deleting Volume " + volume);
        HashMap<String, Object> parametersMap = new HashMap<String, Object>();
        parametersMap.put("InfReservID", volume);      
        res = callJSONRPC(METHOD.releaseReservation, parametersMap);
        checkSuccess(res, false);
      }
    }

    System.out.println("List volumes ");
    res = callJSONRPC(METHOD.listReservations);
    checkSuccess(res, false);
    assertTrue(((Map<String, List<String>>) res.getResult()).get("Addresses").isEmpty());
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
        +  "\"Number\": 5,"
        +  "\"Cost\": \"0.001\","
        + "}]}";  

    // parametersMap.put("password", "");
    JSONRPC2Response res = callJSONRPC(METHOD.createReservation, json);
    checkSuccess(res, false);

    JSONRPC2Response res2 = callJSONRPC(METHOD.listReservations);
    checkSuccess(res2, false);

    res = callJSONRPC(METHOD.getAvailableResources);
    checkSuccess(res, false);
  }
  

}
