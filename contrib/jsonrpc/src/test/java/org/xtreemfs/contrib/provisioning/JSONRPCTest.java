/**
 * Copyright 2012 Zuse Institute Berlin
 * 
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 * Authors: Patrick Schäfer
 */

package org.xtreemfs.contrib.provisioning;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.contrib.provisioning.JsonRPC.METHOD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.test.SetupUtils;

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
   */
  @SuppressWarnings("unchecked")
  @Test
  public void createAndDeleteVolumes() throws JSONRPC2ParseException {
    System.out.println("createAndDeleteVolumes");

    // create a volume
    Map<String, String> parametersMap = new HashMap<String, String>();
    parametersMap.put("volume_name", "testVolume");
    parametersMap.put("owner", owner);
    parametersMap.put("owner_groupname", ownerGroup);
    parametersMap.put("mode", mode);

    // parametersMap.put("password", "");
    JSONRPC2Response res = callJSONRPC(METHOD.createReservation, parametersMap);
    checkSuccess(res, false);
    Map<String, String> vol = (Map<String, String>) res.getResult();
    System.out.println(vol.get("volume_name"));

//  TODO not working yet  // create the volume a second time
//    res = callJSONRPC(METHOD.createReservation, "testVolume", owner, ownerGroup, mode);
//    checkSuccess(res, true);

    // delete the volume
    res = callJSONRPC(METHOD.releaseReservation, "testVolume");
    checkSuccess(res, false);

    // create the volume with policies second time
    String policy = ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE;
    Integer factor = 2;
    Integer flags = REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber();
    res = callJSONRPC(METHOD.createReservation, "testVolume_policies", owner, ownerGroup, mode, policy,
        factor, flags);
    checkSuccess(res, false);

    res = callJSONRPC(METHOD.listReservations);
    checkSuccess(res, false);

    List<Map<String, Object>> volumes = (List<Map<String, Object>>) res.getResult();
    assertTrue(volumes.toString().contains(policy));
    assertTrue(volumes.toString().contains(""+factor));
    assertTrue(volumes.toString().contains(""+flags));
  }


  /**
   * Creates 10 volumes and cleans up all volumes.
   * Checks if all volumes have been deleted successfully
   * @throws JSONRPC2ParseException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void createListAndDeleteVolumes() throws JSONRPC2ParseException {
    System.out.println("createListAndDeleteVolumes");

    // create volumes
    Map<String, String> parametersMap = new HashMap<String, String>();

    for (int i = 0; i < 5; i++) {
      parametersMap.put("volume_name", "testVolume" + i);
      parametersMap.put("owner", owner);
      parametersMap.put("owner_groupname", ownerGroup);
      parametersMap.put("mode", mode);

      // parametersMap.put("password", "");
      JSONRPC2Response res = callJSONRPC(METHOD.createReservation, parametersMap);
      checkSuccess(res, false);
    }

    JSONRPC2Response res = callJSONRPC(METHOD.listReservations);
    checkSuccess(res, false);

    List<Map<String, Object>> volumes = (List<Map<String, Object>>) res.getResult();
    for (Map<String, Object> v : volumes) {
      String volume = (String) v.get("name");
      System.out.println("deleting Volume " + volume);
      res = callJSONRPC(METHOD.releaseReservation, volume);
      checkSuccess(res, false);
    }

    System.out.println("List volumes ");
    res = callJSONRPC(METHOD.listReservations);
    checkSuccess(res, false);
    assertTrue(((List<String>) res.getResult()).isEmpty());
  }

  

  /**
   * Creates a volume and lists the available resources
   * @throws JSONRPC2ParseException
   */
  @Test
  public void createListAndCheckReservation() throws JSONRPC2ParseException {
    System.out.println("createListAndCheckReservation");

    // create volumes
    Map<String, Object> parametersMap = new HashMap<String, Object>();

    parametersMap.put("volume_name", "testVolume");
    parametersMap.put("owner", owner);
    parametersMap.put("owner_groupname", ownerGroup);
    parametersMap.put("mode", mode);
    
    parametersMap.put("capacity", 1024);
    parametersMap.put("randbw", 100);
    parametersMap.put("seqbw", 0);
    parametersMap.put("coldStorage", false);

    // parametersMap.put("password", "");
    JSONRPC2Response res = callJSONRPC(METHOD.createReservation, parametersMap);
    checkSuccess(res, false);

    JSONRPC2Response res2 = callJSONRPC(METHOD.listReservations);
    checkSuccess(res2, false);

    res = callJSONRPC(METHOD.getAvailableNodeList);
    checkSuccess(res, false);
  }
  
  
  /**
   * Test listing OSDs.
   * @throws JSONRPC2ParseException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void listOSDsAndAttributes() throws JSONRPC2ParseException {
    System.out.println("listOSDsAndAttributes");

    // create a volume
    JSONRPC2Response res = callJSONRPC(METHOD.listOSDsAndAttributes);
    checkSuccess(res, false);
    Map<String, Object> object = (Map<String, Object>) res.getResult();
    for (Entry<String, Object> entry : object.entrySet()) {
      assertTrue(((Map<String, Object>)entry.getValue()).toString().contains(OSDConfig.OSD_CUSTOM_PROPERTY_PREFIX));
    }
  }

  /**
   * Test listing Servers.
   * 
   * @throws JSONRPC2ParseException
   * @throws UnknownUUIDException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void getServers() throws JSONRPC2ParseException, UnknownUUIDException {
    System.out.println("listServers");
    HashSet<String> addresses = new HashSet<String>();
    addresses.add(SetupUtils.getDIRAddr().getHostName());
    addresses.add(SetupUtils.getMRC1Addr().getHostName());
    for (int i = 0; i < NUMBER_OF_OSDS; i++) {
      OSDConfig osdConfig = osdConfigs[i];
      addresses.add(osdConfig.getHostName());
    }

    // get all servers
    JSONRPC2Response res = callJSONRPC(METHOD.getServers);
    checkSuccess(res, false);
    List<String> object = (List<String>) res.getResult();
    for (String entry : object) {
      assertTrue(addresses.contains(entry));
      System.out.println(entry);
    }
  }

  /**
   * Test listing and setting policies on volume level
   * @throws JSONRPC2ParseException
   */
  @Test
  public void addOsdRspPolicies() throws JSONRPC2ParseException {
    System.out.println("addOsdRspPolicies");

    String volumeName = "policy_test_volume";

    // create a volume
    JSONRPC2Response res = callJSONRPC(METHOD.createReservation, volumeName, owner, ownerGroup, mode);
    checkSuccess(res, false);

    // list policies
    res = callJSONRPC(METHOD.getOSDSelectionPolicies, volumeName);
    checkSuccess(res, false);

    res = callJSONRPC(METHOD.getReplicaSelectionPolicies, volumeName);
    checkSuccess(res, false);

    // set new policies
    String osdSelectionPolicies =
        ""+GlobalTypes.OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_UUID.getNumber()
        +","+GlobalTypes.OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_RANDOM.getNumber();
    res = callJSONRPC(METHOD.setOSDSelectionPolicies, volumeName, osdSelectionPolicies);
    checkSuccess(res, false);

    String replicaSelectionPolicies =
        ""+GlobalTypes.OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.getNumber()
        +","+GlobalTypes.OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_RANDOM.getNumber();
    res = callJSONRPC(METHOD.setReplicaSelectionPolicies, volumeName, replicaSelectionPolicies);
    checkSuccess(res, false);

    // list policies again
    res = callJSONRPC(METHOD.getOSDSelectionPolicies, volumeName);
    checkSuccess(res, false);
    assertTrue(((String)res.getResult()).contains(osdSelectionPolicies));

    res = callJSONRPC(METHOD.getReplicaSelectionPolicies, volumeName);
    checkSuccess(res, false);
    assertTrue(((String)res.getResult()).contains(replicaSelectionPolicies));

    // delete the volume
    res = callJSONRPC(METHOD.releaseReservation, volumeName);
    checkSuccess(res, false);
  }

  /**
   * Tests setting custom attributes for OSD selection
   * @throws JSONRPC2ParseException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void addCustomPolicyAttributes() throws JSONRPC2ParseException {
    System.out.println("addCustomPolicyAttributes");

    String volumeName = "policy_test_volume";

    // create a volume
    JSONRPC2Response res = callJSONRPC(METHOD.createReservation, volumeName, owner, ownerGroup, mode);
    checkSuccess(res, false);

    // set & list policies
    String attributeName = GlobalTypes.OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.getNumber()+".country";
    String attributeValue = "DE";
    res = callJSONRPC(METHOD.setPolicyAttribute, volumeName, attributeName, attributeValue);
    checkSuccess(res, false);

    attributeName = GlobalTypes.OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.getNumber()+".region";
    attributeValue = "B";
    res = callJSONRPC(METHOD.setPolicyAttribute, volumeName, attributeName, attributeValue);
    checkSuccess(res, false);

    res = callJSONRPC(METHOD.listPolicyAttributes, volumeName);
    checkSuccess(res, false);

    List<String> result = (List<String>) res.getResult();
    assertTrue(result.toString().contains("country"));
    assertTrue(result.toString().contains("region"));

    // test error: no policy set
    attributeName = "region";
    attributeValue = "B";
    res = callJSONRPC(METHOD.setPolicyAttribute, volumeName, attributeName, attributeValue);
    checkSuccess(res, true);

    // test error: no policy set and starts with "."
    attributeName = ".country";
    attributeValue = "DE";
    res = callJSONRPC(METHOD.setPolicyAttribute, volumeName, attributeName, attributeValue);
    checkSuccess(res, true);
  }

  /**
   * Tests ACLs on volume leven
   * @throws JSONRPC2ParseException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void addRemoveACLs() throws JSONRPC2ParseException {
    System.out.println("addRemoveACLs");

    String volumeName = "policy_test_volume";

    // create a volume
    JSONRPC2Response res = callJSONRPC(METHOD.createReservation, volumeName, owner, ownerGroup, mode);
    checkSuccess(res, false);

    // list ACLs
    res = callJSONRPC(METHOD.listACLs, volumeName);
    checkSuccess(res, false);

    // set ACLs
    res = callJSONRPC(METHOD.setACL, volumeName, "patrick", "xrw");
    checkSuccess(res, false);

    // list ACLs
    res = callJSONRPC(METHOD.listACLs, volumeName);
    checkSuccess(res, false);


    Map<String,Object> result = (Map<String,Object>) res.getResult();
    assertTrue(result.toString().contains("patrick"));

    // remove from ACLs
    res = callJSONRPC(METHOD.removeACL, volumeName, "patrick");
    checkSuccess(res, false);

    // list ACLs
    res = callJSONRPC(METHOD.listACLs, volumeName);
    checkSuccess(res, false);

    result = (Map<String,Object>) res.getResult();
    assertTrue(!result.toString().contains("patrick"));
  }
}
