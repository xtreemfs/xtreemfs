/**
 * Copyright 2011-2012 Zuse Institute Berlin
 * 
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 * Authors: Michael Berlin, Patrick Schäfer
 */
package org.xtreemfs.contrib.provisioning;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler.freeResourcesResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceClient;
import org.xtreemfs.scheduler.SchedulerClient;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

@Controller
public class JsonRPC implements ResourceLoaderAware {

  private static final int DEFAULT_DIR_PORT = 32638;

  private static final String OSD_SELECTION_POLICY = "xtreemfs.osel_policy";
  private static final String REPLICA_SELECTION_POLICY = "xtreemfs.rsel_policy";
  private String DEFAULT_DIR_CONFIG = "default_dir";
  private static final String SECONDS_SINCE_LAST_UPDATE = "seconds_since_last_update";

  protected Client client = null;
  
  protected Dispatcher dispatcher = null;
  protected InetSocketAddress[] dirAddresses = null;
  protected InetSocketAddress schedulerAddress = null;

  protected int dir_port = DEFAULT_DIR_PORT;
  private String adminPassword = "";

  protected ResourceLoader resourceLoader;

  protected SSLOptions sslOptions = null;

  enum METHOD {
    listOSDsAndAttributes,
    
    getServers,
    
    getOSDSelectionPolicies, setOSDSelectionPolicies,
    getReplicaSelectionPolicies, setReplicaSelectionPolicies,
    setPolicyAttribute,listPolicyAttributes,
    
    listACLs, setACL, removeACL,
    
    createReservation, releaseReservation, listReservations,
    
    isNodeRunning, getStaticNodeInfo, getDynamicNodeInfo,
    
    getAvailableNodeList
  }

  public JsonRPC(String defaultDirConfigFile) {
    this.DEFAULT_DIR_CONFIG = defaultDirConfigFile;
  }

  public JsonRPC(int defaultPort) {
    this.dir_port = defaultPort;
  }

  public JsonRPC() {
    this.dir_port = DEFAULT_DIR_PORT;
  }

  @Override
  public void setResourceLoader(ResourceLoader arg) {
    this.resourceLoader = arg;
  }

  public File getResource(String location) throws IOException{
    if (this.resourceLoader!=null) {
      return this.resourceLoader.getResource(location).getFile();
    }
    return null;
  }

  @PostConstruct
  public void init() throws FileNotFoundException, IOException {

    // Start Xtreemfs
    Logging.start(Logging.LEVEL_DEBUG, Category.tool);

    // If /etc/xos/xtreemfs/default_dir exists, get DIR address from it.
    File defaultDirConfigFile = getResource("WEB-INF/"+this.DEFAULT_DIR_CONFIG);
    if (defaultDirConfigFile == null) { // for j-unit tests check this path too
      defaultDirConfigFile = new File("src/main/webapp/WEB-INF/"+this.DEFAULT_DIR_CONFIG);
    }
    MRCConfig config = null;
    if (defaultDirConfigFile != null && defaultDirConfigFile.exists()) {
      Logger.getLogger(JsonRPC.class.getName()).log(Level.INFO, "Found a config file: " + defaultDirConfigFile.getAbsolutePath());
      // the DIRConfig does not contain a "dir_service." property
      config = new MRCConfig(defaultDirConfigFile.getAbsolutePath());
      config.read();      
      this.dirAddresses = config.getDirectoryServices();
      this.schedulerAddress = config.getSchedulerService();
      this.adminPassword = config.getAdminPassword();
    } else {
      Logger.getLogger(JsonRPC.class.getName()).log(Level.INFO, "No config file found!");
      this.dirAddresses = new InetSocketAddress[]{new InetSocketAddress("localhost", this.dir_port)};
    }

    if (this.adminPassword == null) {
      this.adminPassword = "";
    }

    Logger.getLogger(JsonRPC.class.getName()).log(Level.INFO, "Connecting to DIR-Address: " + this.dirAddresses[0].getAddress().getCanonicalHostName());

    try {
      Options options = new Options();

      // SSL options
      if (config != null && config.isUsingSSL()) {
        final boolean gridSSL = true;
        this.sslOptions = new SSLOptions(
            new FileInputStream(
                config.getServiceCredsFile()), config.getServiceCredsPassphrase(), config.getServiceCredsContainer(),
                null, null, "none", false, gridSSL, null);
      }

      String[] dirAddressesString = generateDirAdresses();

      this.client = ClientFactory.createClient(dirAddressesString, AbstractRequestHandler.getGroups(), this.sslOptions, options);
      this.client.start();            
          
      // Create a new JSON-RPC 2.0 request dispatcher
      // Register the Volume, OSP/RSP, ACL, etc. handlers
      this.dispatcher =  new Dispatcher();

      // Volume handlers
      this.dispatcher.register(new ListVolumesHandler(this.client));
      this.dispatcher.register(new CreateVolumeHandler(this.client));
      this.dispatcher.register(new DeleteVolumeHandler(this.client));

      // Policies for Replica and OSD selection
      // i.e. 1001, 2000
      this.dispatcher.register(new GetServersHandler(this.client));
      this.dispatcher.register(new ListOSDsAndAttributesHandler(this.client));
      this.dispatcher.register(new GetOSDSelectionPoliciesHandler(this.client));
      this.dispatcher.register(new SetOSDSelectionPolicyHandler(this.client));

      // Policies on custom attributes
      // i.e. country = DE
      this.dispatcher.register(new ListPolicyAttributesHandler(this.client));
      this.dispatcher.register(new SetPolicyAttributeHandler(this.client));

      // ACL handlers
      this.dispatcher.register(new ListACLsHandler(this.client));
      this.dispatcher.register(new SetACLHandler(this.client));
      this.dispatcher.register(new RemoveACLHandler(this.client));
           
      // Harness handlers
      this.dispatcher.register(new NodeRunningHandler(this.client));
      this.dispatcher.register(new StaticNodeInfoHandler(this.client));
      this.dispatcher.register(new DynamicNodeInfo(this.client));
      this.dispatcher.register(new AvailableNodeList(this.client));

      
    } catch (Exception e) {
      e.printStackTrace();

      // stop client
      stop();

      throw new IOException("client.start() failed (threw an exception)");
    }
  }

  public String[] generateDirAdresses() {
    String[] dirAddressesString = new String[this.dirAddresses.length];
    for (int i = 0; i < this.dirAddresses.length; i++) {
      InetSocketAddress address = this.dirAddresses[i];
      dirAddressesString[i] = address.getHostName() + ":" + address.getPort();
    }
    return dirAddressesString;
  }
  
  public String generateSchedulerAddress() {
    InetSocketAddress address = this.schedulerAddress;
    return address.getHostName() + ":" + address.getPort();
  }

  @PreDestroy
  public void stop() {
    if (this.client != null) {
      this.client.shutdown();
      this.client = null;
    }
  }


  @RequestMapping(
      value = "/executeMethod",
      method = RequestMethod.POST,
      produces = "application/json")
  public @ResponseBody String executeMethod(@RequestBody String json_string) {
    JSONRPC2Response resp = null;
    try {      
      Logger.getLogger(JsonRPC.class.getName()).log(
          Level.FINE, "received request for method: " + json_string) ;

      JSONRPC2Request req = JSONRPC2Request.parse(json_string, true, true);

      Logger.getLogger(JsonRPC.class.getName()).log(
          Level.FINE, "received request for method: " + req.getMethod() + " params: " + req.getParams());

      resp = this.dispatcher.process(req, null);

    } catch (JSONRPC2ParseException e) {
      Logger.getLogger(JsonRPC.class.getName()).log(Level.WARNING, null, e);
      resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.PARSE_ERROR.getCode(), e.getMessage()), 0);
    }
    // Write JSON response
    return resp.toJSONString();
  }

  /**
   *  Implements a handler for the
   *    "getOSDSelectionPolicies volume_name" and
   *    "getReplicaSelectionPolicies volume_name"
   *  JSON-RPC method
   */
  public class GetOSDSelectionPoliciesHandler extends AbstractRequestHandler {

    public GetOSDSelectionPoliciesHandler(Client c) {
      super(c, new METHOD[]{
          METHOD.getOSDSelectionPolicies,
          METHOD.getReplicaSelectionPolicies});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      // is osd-selection of replica-placement called?
      boolean osdSelection = METHOD.valueOf(req.getMethod()) == METHOD.getOSDSelectionPolicies;
      String policyKey = osdSelection? OSD_SELECTION_POLICY : REPLICA_SELECTION_POLICY;

      String volume_name = getStringParam(req, "volume_name", false); // required

      Volume volume = openVolume(volume_name, JsonRPC.this.sslOptions);

      // contains a list of policies separated by ","
      String xAttr = volume.getXAttr(getGroups(), "/", policyKey);
      return new JSONRPC2Response(xAttr, req.getID());
    }
  }


  /**
   *  Implements a handler for the
   *    "setOSDSelectionPolicy volume_name policies" and
   *    "setReplicaSelectionPolicies volume_name policies"
   *  JSON-RPC methods
   */
  public class SetOSDSelectionPolicyHandler extends AbstractRequestHandler {

    public SetOSDSelectionPolicyHandler(Client c) {
      super(c, new METHOD[]{
          METHOD.setOSDSelectionPolicies,
          METHOD.setReplicaSelectionPolicies});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      // is osd-selection of replica-placement called?
      boolean osdSelection = METHOD.valueOf(req.getMethod()) == METHOD.setOSDSelectionPolicies;
      String policyKey = osdSelection? OSD_SELECTION_POLICY : REPLICA_SELECTION_POLICY;

      String volume_name = getStringParam(req, "volume_name", false); // required
      String policy = getStringParam(req, "policies", false); // required

      Volume volume = openVolume(volume_name, JsonRPC.this.sslOptions);
      
      final UserCredentials uc = getGroups();      
      final Auth auth = getAuth(JsonRPC.this.adminPassword);
      
      // overwrites old policies
      volume.setXAttr(
          uc, auth, "/", 
          policyKey, policy, 
          XATTR_FLAGS.XATTR_FLAGS_REPLACE);

      List<String> usableOsds = getUsableOSDs(volume, uc);
      
      String description = osdSelection? 
          "OSD selection policy" : "Replica selection policy";
      
      String msg = description + " on " + volume_name + " set to " + policy;
      
      HashMap<String, Object> returnMessage = new HashMap<String, Object>();
      returnMessage.put("message", msg);
      returnMessage.put("volume_name", volume_name);
      returnMessage.put("policy", policy);
      returnMessage.put("usable_osds", usableOsds.size());
//      returnMessage.put("list_of_usable_osds", usableOsds);
      
      return new JSONRPC2Response(returnMessage, req.getID());
    }
  }

  

  /**
   *  Implements a handler for the
   *    "setPolicyAttribute volume_name attribute_name attribute_value"
   *  JSON-RPC method
   */
  public class SetPolicyAttributeHandler extends AbstractRequestHandler {

    public SetPolicyAttributeHandler(Client c) {
      super(c, new METHOD[]{METHOD.setPolicyAttribute});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      String volume_name = getStringParam(req, "volume_name", false); // required
      String attribute_name = getStringParam(req, "attribute_name", false); // required
      String attribute_value = getStringParam(req, "attribute_value", false); // required

      Volume volume = openVolume(volume_name, JsonRPC.this.sslOptions);
      final UserCredentials uc = getGroups();

      attribute_name = MRCHelper.XTREEMFS_POLICY_ATTR_PREFIX + attribute_name;

      final Auth auth = getAuth(JsonRPC.this.adminPassword);

      // overwrites old policies
      volume.setXAttr(
          uc, auth, "/",
          attribute_name, attribute_value,
          XATTR_FLAGS.XATTR_FLAGS_REPLACE);

      List<String> usableOsds = getUsableOSDs(volume, uc);
      
      String msg = "Attribute " + attribute_name
          + " set to " + attribute_value + " on " + volume_name;
      
      HashMap<String, Object> returnMessage = new HashMap<String, Object>();
      returnMessage.put("message", msg);
      returnMessage.put("volume_name", volume_name);
      returnMessage.put("usable_osds", usableOsds.size());
//      returnMessage.put("list_of_usable_osds", usableOsds);
      
      return new JSONRPC2Response(returnMessage, req.getID());
    }
  }


  /**
   *  Implements a handler for the
   *    "listPolicyAttributes volume_name"
   *  JSON-RPC method
   */
  public class ListPolicyAttributesHandler extends AbstractRequestHandler {

    public ListPolicyAttributesHandler(Client c) {
      super(c, new METHOD[]{METHOD.listPolicyAttributes});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      String volume_name = getStringParam(req, "volume_name", false); // required

      Volume volume = openVolume(volume_name, JsonRPC.this.sslOptions);

      // list all xattr and filter for custom policy attributes
      List<String> attributes = new ArrayList<String>();
      listxattrResponse resp = volume.listXAttrs(getGroups(), "/");
      for (XAttr xattr : resp.getXattrsList()) {
        if (xattr.getName().startsWith(MRCHelper.XTREEMFS_POLICY_ATTR_PREFIX)) {
          // remove "xtreemfs.policies" from the name
          attributes.add(
              xattr.getName().substring(MRCHelper.XTREEMFS_POLICY_ATTR_PREFIX.length())
              + "=" + xattr.getValue());
        }
      }

      return new JSONRPC2Response(attributes, req.getID());
    }
  }


  /**
   *  Implements a handler for the
   *    "listACLs volume_name"
   *  JSON-RPC method
   */
  public class ListACLsHandler extends AbstractRequestHandler {

    public ListACLsHandler(Client c) {
      super(c, new METHOD[]{METHOD.listACLs});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      String volume_name = getStringParam(req, "volume_name", false); // required

      Volume volume = openVolume(volume_name, JsonRPC.this.sslOptions);

      // list all xattr and filter for custom policy attributes
      Map<String, Object> resp = volume.listACL(getGroups(), "/");
      return new JSONRPC2Response(resp, req.getID());
    }
  }


  /**
   *  Implements a handler for the
   *    "setACL volume_name user_name user_accessrights
   *  JSON-RPC method
   */
  public class SetACLHandler extends AbstractRequestHandler {

    public SetACLHandler(Client c) {
      super(c, new METHOD[]{METHOD.setACL});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      String volume_name = getStringParam(req, "volume_name", false); // required
      String user_name = getStringParam(req, "user_name", false); // required
      String user_accessrights = getStringParam(req, "user_accessrights", false); // required

      Volume volume = openVolume(volume_name, JsonRPC.this.sslOptions);

      // set accessrights for the user
      volume.setACL(getGroups(), "/", user_name, user_accessrights);

      return new JSONRPC2Response("Added user '" + user_name + "' '" + user_accessrights + "' to ACL.", req.getID());
    }
  }


  /**
   *  Implements a handler for the
   *    "removeACL volume_name user_name
   *  JSON-RPC method
   */
  public class RemoveACLHandler extends AbstractRequestHandler {

    public RemoveACLHandler(Client c) {
      super(c, new METHOD[]{METHOD.removeACL});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      String volume_name = getStringParam(req, "volume_name", false); // required
      String user_name = getStringParam(req, "user_name", false); // required

      Volume volume = openVolume(volume_name, JsonRPC.this.sslOptions);

      // set accessrights for the user
      volume.removeACL(getGroups(), "/", user_name);

      return new JSONRPC2Response("Removed user '" + user_name + "' from ACL.", req.getID());
    }
  }


  /**
   *  Implements a handler for the
   *    "listOSDsAndAttributes"
   *  JSON-RPC method
   */
  public class ListOSDsAndAttributesHandler extends AbstractRequestHandler {

    public ListOSDsAndAttributesHandler(Client c) {
      super(c, new METHOD[]{METHOD.listOSDsAndAttributes});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      Map<String, Service> configs = JsonRPC.this.client.listOSDsAndAttributes();
      Map<String, Map<String, String>> osds = new HashMap<String, Map<String, String>>();
      for (Entry<String, Service> s : configs.entrySet()) {
        HashMap<String, String> attributes = new HashMap<String, String>();

        for (KeyValuePair pair : s.getValue().getData().getDataList()) {
          // filter list of attributes for the attributes starting with the custom prefix "config."
          if (pair.getKey().startsWith(ServiceConfig.OSD_CUSTOM_PROPERTY_PREFIX)) {
            attributes.put(pair.getKey(), pair.getValue());
          }
        }

        // check if the OSD is alive (last seen within the last 300 seconds)
        String seconds = getParameter(SECONDS_SINCE_LAST_UPDATE, s.getValue());
        if (Integer.valueOf(seconds) < 300) {
          osds.put(s.getValue().getUuid(), attributes);
        }
      }
      return new JSONRPC2Response(osds, req.getID());
    }
  }

  /**
   * Implements a handler for the "getServers" JSON-RPC method
   */
  public class GetServersHandler extends AbstractRequestHandler {

    public GetServersHandler(Client c) {
      super(c, new METHOD[] { METHOD.getServers });
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx)
        throws Exception {
      Map<String, Service> configs = JsonRPC.this.client.listServers();
      Set<String> servers = new LinkedHashSet<String>();

      // add the DIR address in first place
      servers.add(JsonRPC.this.dirAddresses[0].getHostName());

      // add all other servers
      for (Entry<String, Service> s : configs.entrySet()) {
        // test if there is a port
        String host = s.getKey();
        if (host.contains(":")) {
          host = host.substring(0, host.indexOf(":"));
        }
        servers.add(host);
      }

      return new JSONRPC2Response(Arrays.asList(servers.toArray(new String[] {})), req.getID());
    }
  }

  /**
   * Implements a handler for the
   * "createVolume volume_name [policy factor flag]" JSON-RPC method
   */
  public class CreateVolumeHandler extends AbstractRequestHandler {

    public CreateVolumeHandler(Client c) {
      super(c, new METHOD[]{METHOD.createReservation});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      // build credentials
      String volume_name = getStringParam(req, "volume_name", false);           // required
      // String password = getStringParam(req, "password", true);               // optional

      String owner = getStringParam(req, "owner", false);                       // required
      String ownerGroupname = getStringParam(req, "owner_groupname", false);    // required
      String mode = getStringParam(req, "mode", false);                         // required

      String policy = getStringParam(req, "policy", true);                      // optional
      
      Integer capacity = getIntParam(req, "capacity", true, 10);                  // optional
      Integer randomTP = getIntParam(req, "ranbw", true, 2);                     // optional
      Integer seqTP = getIntParam(req, "seqbw", true, 0);                        // optional
      Boolean coldStorage = getBooleanParam(req, "coldStorage", true, false);    // optional      
      
      int octalMode = Integer.parseInt(mode, 8);

      final UserCredentials uc = getGroups();
      final Auth auth = getAuth(JsonRPC.this.adminPassword);

      // Create volume.
      JsonRPC.this.client.createVolume(
          generateSchedulerAddress(), 
          auth,
          uc,
          volume_name,
          octalMode,
          owner,
          ownerGroupname,
          AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,
          StripingPolicyType.STRIPING_POLICY_RAID0,
          1,
          128*1024,          
          new ArrayList<KeyValuePair>(),  // volume attributes
          capacity,
          randomTP,
          seqTP,
          coldStorage);

      // open the volume
      if (policy != null && !policy.trim().equals("")) {
        Integer factor = getLongParam(req, "factor", false, 0l).intValue(); // required
        Integer flag = getLongParam(req, "flag", false, 0l).intValue(); // required

        Volume volume = openVolume(volume_name, JsonRPC.this.sslOptions);

        // and set replication policy
        volume.setDefaultReplicationPolicy(uc, "/", policy, factor, flag);
      }

      // create a string similar to:
      // [<protocol>://]<DIR-server-address>[:<DIR-server-port>]/<Volume Name>
      String[] dirAddressesString = generateDirAdresses();
      StringBuffer normed_volume_names = new StringBuffer();
      for (String s : dirAddressesString) {
        normed_volume_names.append(s);
        normed_volume_names.append(",");
      }
      normed_volume_names.deleteCharAt(normed_volume_names.length() - 1);
      normed_volume_names.append("/" + volume_name);

      HashMap<String, String> volume = new HashMap<String, String>();
      volume.put("volume_name", normed_volume_names.toString());

      return new JSONRPC2Response(volume, req.getID());
    }
  }


  /**
   * Implements a handler for the "deleteVolume volume_name" JSON-RPC method
   */
  public class DeleteVolumeHandler extends AbstractRequestHandler {

    public DeleteVolumeHandler(Client c) {
      super(c, new METHOD[]{METHOD.releaseReservation});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      String volume_name = getStringParam(req, "volume_name", false); // required
      // String password = getStringParam(req, "password", true); // optional

      JsonRPC.this.client.deleteVolume(getAuth(JsonRPC.this.adminPassword),
          getGroups(), volume_name);

      final UserCredentials uc = getGroups();
      final Auth auth = getAuth(JsonRPC.this.adminPassword);
      
      JsonRPC.this.client.deleteReservation(
          generateSchedulerAddress(), 
          auth, 
          uc, 
          volume_name);
    
      return new JSONRPC2Response("volume and reservation " + volume_name + " deleted.", req.getID());
    }
  }


  /**
   * Implements a handler for the
   *    "listVolumes"
   * JSON-RPC method
   */
  public class ListVolumesHandler extends AbstractRequestHandler {

    public ListVolumesHandler(Client c) {
      super(c, new METHOD[]{METHOD.listReservations});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      final UserCredentials uc = getGroups();

      // list volumes
      List<Map<String, Object>> volumesMap = new LinkedList<Map<String,Object>>();
      String[] volumes = JsonRPC.this.client.listVolumeNames();
      for (String volume_name : volumes) {
        // open volume and get Xattr
        Volume v = openVolume(volume_name, JsonRPC.this.sslOptions);
        listxattrResponse response = v.listXAttrs(uc, "/");
        Map<String, Object> keys = new LinkedHashMap<String, Object>();
        keys.put("name", volume_name);
        volumesMap.add(keys);
        for (XAttr xattr : response.getXattrsList()) {
          if (xattr.getName().equals(org.xtreemfs.common.clients.File.XTREEMFS_DEFAULT_RP)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) JSONParser.parseJSON(new JSONString(xattr.getValue()));
            keys.putAll(values);
          }
        }
      }
      return new JSONRPC2Response(volumesMap, req.getID());
    }
  }

    
  /**
   *  Implements a handler for the
   *    "isNodeRunning"
   *  JSON-RPC method
   *  
   *  returns true if it is running, false otherwise
   */
  public class NodeRunningHandler extends AbstractRequestHandler {

    public NodeRunningHandler(Client c) {
      super(c, new METHOD[]{METHOD.isNodeRunning});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {     
      return new JSONRPC2Response(true, req.getID());
    }
  }  
  
  /**
   *  Implements a handler for the
   *    "getStaticNodeInfo"
   *  JSON-RPC method
   *  
   *  returns a JSONString containing static information of the node.
   */
  public class StaticNodeInfoHandler extends AbstractRequestHandler {

    public StaticNodeInfoHandler(Client c) {
      super(c, new METHOD[]{METHOD.getStaticNodeInfo});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {      
      return new JSONRPC2Response("storage", req.getID());
    }
  }
  
  /**
   *  Implements a handler for the
   *    "getDynamicNodeInfo"
   *  JSON-RPC method
   *  
   * a JSONString containing dynamic information of the node
   * 
   */
  public class DynamicNodeInfo extends AbstractRequestHandler {

    public DynamicNodeInfo(Client c) {
      super(c, new METHOD[]{METHOD.getDynamicNodeInfo});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {     
      return new JSONRPC2Response("storage", req.getID());
    }
  }
  
  /**
   *  Implements a handler for the
   *    "getAvailableNodeList"  
   *  JSON-RPC method
   */
  public class AvailableNodeList extends AbstractRequestHandler {

    public AvailableNodeList(Client c) {
      super(c, new METHOD[]{METHOD.getAvailableNodeList});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      
      final UserCredentials uc = getGroups();
      final Auth auth = getAuth(JsonRPC.this.adminPassword);
      
      freeResourcesResponse freeResources
        = JsonRPC.this.client.getFreeResources(
            generateSchedulerAddress(), 
            auth, 
            uc);

      HashMap<String, HashMap<String, Double>> resources = new HashMap<String, HashMap<String, Double>>();
      HashMap<String, Double> freeRes = new HashMap<String, Double>();
      freeRes.put("capacity", freeResources.getCapacity());
      freeRes.put("ranbw", freeResources.getRandomThroughput());
      freeRes.put("seqbw", freeResources.getStreamingThroughput());     
      resources.put("storage", freeRes);
      
      return new JSONRPC2Response(freeRes, req.getID());
    }
  }
  
  
}
