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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.Scheduler.freeResourcesResponse;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

@Controller
public class JsonRPC implements ResourceLoaderAware {

  private static final int DEFAULT_DIR_PORT = 32638;

  protected static final String OSD_SELECTION_POLICY = "xtreemfs.osel_policy";
  protected static final String REPLICA_SELECTION_POLICY = "xtreemfs.rsel_policy";
  protected String DEFAULT_DIR_CONFIG = "default_dir";
  protected static final String SECONDS_SINCE_LAST_UPDATE = "seconds_since_last_update";

  protected Client client = null;

  protected Dispatcher dispatcher = null;
  protected InetSocketAddress[] dirAddresses = null;
  protected InetSocketAddress schedulerAddress = null;

  protected int dir_port = DEFAULT_DIR_PORT;
  private String adminPassword = "";

  protected ResourceLoader resourceLoader; 

  protected SSLOptions sslOptions = null;

  enum METHOD {
    listReservations, 

    createReservation, 

    releaseReservation, 

    checkReservation,

    isNodeRunning,

    getAvailableResources,

    createVolume
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

      String[] dirAddressesString = generateDirAddresses();

      this.client = ClientFactory.createClient(dirAddressesString, AbstractRequestHandler.getGroups(), this.sslOptions, options);
      this.client.start();            

      // Create a new JSON-RPC 2.0 request dispatcher
      // Register the Volume, OSP/RSP, ACL, etc. handlers
      this.dispatcher =  new Dispatcher();

      // Volume handlers
      this.dispatcher.register(new ListReservationsHandler(this.client));
      this.dispatcher.register(new CreateReservationHandler(this.client));
      this.dispatcher.register(new ReleaseReservationHandler(this.client));
      this.dispatcher.register(new CheckReservationHandler(this.client));

      // Harness handlers
      this.dispatcher.register(new NodeRunningHandler(this.client));
      this.dispatcher.register(new AvailableResources(this.client));


    } catch (Exception e) {
      e.printStackTrace();

      // stop client
      stop();

      throw new IOException("client.start() failed (threw an exception)");
    }
  }

  public String createNormedVolumeName(String volume_name) {
    String[] dirAddressesString = generateDirAddresses();
    StringBuffer normed_volume_names = new StringBuffer();
    for (String s : dirAddressesString) {
      normed_volume_names.append(s);
      normed_volume_names.append(",");
    }
    normed_volume_names.deleteCharAt(normed_volume_names.length() - 1);
    normed_volume_names.append("/" + volume_name);

    return normed_volume_names.toString();
  }

  public String[] generateDirAddresses() {
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
   * Implements a handler for the
   */
  public class CreateReservationHandler extends AbstractRequestHandler {

    public CreateReservationHandler(Client c) {
      super(c, new METHOD[]{METHOD.createReservation});
    }

    // Processes the requests
    @SuppressWarnings("unchecked")
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {

      List<Map<String, Object>> resources = (List<Map<String, Object>>) getObjectParam(req, "Resources", false); // required

      // search for storage resource
      for (Map<String, Object> resource : resources) {
        if (resource.get("Type").equals("Storage")) {          
          // check for datacenter ID to match DIR ID:
          String datacenterID = getStringParam(resource, "ID", false);
          if (datacenterID.contains(dirAddresses[0].getAddress().getCanonicalHostName())) {

            //          String volume_name = getStringParam(req, "volume_name", false);           // required
            //          String password = getStringParam(req, "password", true);                       // optional
            //          String owner = getStringParam(req, "owner", true, "user");                     // optional
            //          String ownerGroupname = getStringParam(req, "owner_groupname", true, "user");  // optional
            //          String mode = getStringParam(req, "mode", true, "700");                        // optional
            //          String policy = getStringParam(req, "policy", true);                           // optional

            String volume_name = "volume-"+UUID.randomUUID().toString();            
            Integer capacity = getIntParam(resource, "Attributes/Capacity", false, 0);          // required
            Integer throughput = getIntParam(resource, "Attributes/Throughput", false, 0);      // required
            String accessType = getStringParam(resource, "Attributes/AccessType", false);       // required

            boolean randomAccess = accessType.equals("RANDOM");

            int octalMode = Integer.parseInt("700", 8);

            final UserCredentials uc = getGroups();
            final Auth auth = getAuth(JsonRPC.this.adminPassword);

            // Create volume.
            JsonRPC.this.client.createVolume(
                generateSchedulerAddress(), 
                auth,
                uc,
                volume_name,
                octalMode,
                "user",
                "user",
                AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX,          
                128*1024,          
                new ArrayList<KeyValuePair>(),  // volume attributes
                capacity,
                randomAccess? throughput : 0,
                    !randomAccess? throughput : 0,
                        false);

            //          // open the volume
            //          if (policy != null && !policy.trim().equals("")) {
            //            Integer factor = getLongParam(req, "factor", false, 0l).intValue(); // required
            //            Integer flag = getLongParam(req, "flag", false, 0l).intValue(); // required
            //            Volume volume = openVolume(volume_name, JsonRPC.this.sslOptions);
            //            // and set replication policy
            //            volume.setDefaultReplicationPolicy(uc, "/", policy, factor, flag);
            //          }

            // create a string similar to:
            // [<protocol>://]<DIR-server-address>[:<DIR-server-port>]/<Volume Name>
            Map<String, Object> result = new HashMap<String, Object>();
            add(result, "InfReservID", createNormedVolumeName(volume_name));
            return new JSONRPC2Response(result, req.getID());          
          }
        }
      }
      throw new JSONRPC2Error(-1, "No resource type 'Storage' found");
    }
  }



  /**
   * Implements a handler for the "releaseReservation" JSON-RPC method
   */
  public class ReleaseReservationHandler extends AbstractRequestHandler {

    public ReleaseReservationHandler(Client c) {
      super(c, new METHOD[]{METHOD.releaseReservation});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
      String volume_name = getStringParam(req, "InfReservID", false); // required
      volume_name = stripVolumeName(volume_name);
      
      final Auth auth = getAuth(JsonRPC.this.adminPassword);
      final UserCredentials uc = getGroups();

      JsonRPC.this.client.deleteVolume(
          auth,
          getGroups(), 
          volume_name);


      JsonRPC.this.client.deleteReservation(
          generateSchedulerAddress(), 
          auth, 
          uc, 
          volume_name);

      Map<String, String[]> success = new HashMap<String, String[]>();
      success.put("Addresses", new String[]{volume_name});

      return new JSONRPC2Response(success, req.getID());
    }

  }


  public static String stripVolumeName(String volume_name) {
    if (volume_name.contains("/")) {
      String[] parts = volume_name.split("/");
      volume_name = parts[parts.length-1];
    }
    return volume_name;
  }
  
  /**
   * Implements a handler for the "checkReservation" JSON-RPC method
   */
  public class CheckReservationHandler extends AbstractRequestHandler {

    public CheckReservationHandler(Client c) {
      super(c, new METHOD[]{METHOD.checkReservation});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {

      String volume_name = getStringParam(req, "InfReservID", false); // required
      volume_name = stripVolumeName(volume_name);


      openVolume(volume_name, sslOptions);

      // return a string like
      // [<protocol>://]<DIR-server-address>[:<DIR-server-port>]/<Volume Name>
      Map<String, String[]> success = new HashMap<String, String[]>();
      success.put("Addresses", new String[]{createNormedVolumeName(volume_name)});

      return new JSONRPC2Response(success, req.getID());
    }
  }


  /**
   * Implements a handler for the
   *    "listReservations"
   * JSON-RPC method
   */
  public class ListReservationsHandler extends AbstractRequestHandler {

    public ListReservationsHandler(Client c) {
      super(c, new METHOD[]{METHOD.listReservations});
    }

    // Processes the requests
    @Override
    public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
//      final UserCredentials uc = getGroups();

  
      // list volumes
      Map<String, ArrayList<String>> volumesMap = new HashMap<String, ArrayList<String>>();
      ArrayList<String> volumeNames = new ArrayList<String>();
      volumesMap.put("Addresses", volumeNames);
      
      String[] volumes = JsonRPC.this.client.listVolumeNames();
      for (String volume_name : volumes) {
        volumeNames.add(createNormedVolumeName(volume_name));
        
        // open volume and get Xattr
//        Volume v = openVolume(volume_name, JsonRPC.this.sslOptions);
//        listxattrResponse response = v.listXAttrs(uc, "/");
//        for (XAttr xattr : response.getXattrsList()) {
//          if (xattr.getName().equals(org.xtreemfs.common.clients.File.XTREEMFS_DEFAULT_RP)) {
//            Map<String, Object> values = (Map<String, Object>) JSONParser.parseJSON(new JSONString(xattr.getValue()));
//            keys.putAll(values);
//          }
//        }
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
   *    "getAvailableResources"  
   *  JSON-RPC method
   */
  public class AvailableResources extends AbstractRequestHandler {

    public AvailableResources(Client c) {
      super(c, new METHOD[]{METHOD.getAvailableResources});
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

      Map<String,Object> resources = new HashMap<String,Object>();

      add(resources, "Resources[0]/ID", dirAddresses[0].getAddress().getCanonicalHostName()+"/storage/random");
      add(resources, "Resources[0]/IP", dirAddresses[0].getAddress().getHostAddress());
      add(resources, "Resources[0]/Type", "Storage");

      // attributes
      add(resources, "Resources[0]/Attributes/Capacity", freeResources.getRandomCapacity());
      add(resources, "Resources[0]/Attributes/Throughput", freeResources.getRandomThroughput());
      add(resources, "Resources[0]/Attributes/ReservationType", "RANDOM");

      // costs
      add(resources, "Resources[0]/Cost/Capacity", 0);
      add(resources, "Resources[0]/Cost/Throughput", 0);


      add(resources, "Resources[1]/ID", dirAddresses[0].getAddress().getCanonicalHostName()+"/storage/sequential");
      add(resources, "Resources[1]/IP", dirAddresses[0].getAddress().getHostAddress());
      add(resources, "Resources[1]/Type", "Storage");

      // attributes
      add(resources, "Resources[1]/Attributes/Capacity", freeResources.getStreamingCapacity());
      add(resources, "Resources[1]/Attributes/Throughput", freeResources.getStreamingThroughput());
      add(resources, "Resources[1]/Attributes/ReservationType", "SEQUENTIAL");

      // costs
      add(resources, "Resources[1]/Cost/Capacity", 0);
      add(resources, "Resources[1]/Cost/Throughput", 0);

      return new JSONRPC2Response(resources, req.getID());
    }  
  }

  /**
   * Parses an attribute string of the form array[index]/key/.../key and sets the value
   * @param map
   * @param attributes
   * @param value
   */
  public static void add(Map<String,Object> map, String attributes, Object value) {
    add(map, value, attributes.split("/"));
  }

  @SuppressWarnings("unchecked")
  public static void add(Map<String,Object> map, Object value, String... attributes) {
    Map<String, Object> currentMap = map;
    for (int i = 0; i < attributes.length-1; i++) {
      String att = attributes[i];
      // map?
      if (!att.contains("[")) {
        if (!currentMap.containsKey(att)) {
          currentMap.put(att, new LinkedHashMap<String, Object>());
        }
        currentMap = (Map<String, Object>) currentMap.get(att);
      }
      // array
      else {
        int startOff = att.indexOf("[");
        int endOff = att.indexOf("]");
        String name = att.substring(0, startOff);
        String id = att.substring(startOff+1, endOff);
        int index = Integer.valueOf(id);

        if (!currentMap.containsKey(name)) {
          currentMap.put(name, new ArrayList<Object>());
        }
        ArrayList<Object> array = (ArrayList<Object>) currentMap.get(name);

        if (array instanceof List) {
          try {
            currentMap = (Map<String, Object>) array.get(index);                        
          } catch (IndexOutOfBoundsException e) {
            array.add(index, new LinkedHashMap<String, Object>());
          }
          if (currentMap == null) {
            array.add(index, new LinkedHashMap<String, Object>());
          }
          
          currentMap = (Map<String, Object>) array.get(index);
        }
        else {
          System.err.println("Error no Array found! " + attributes);
        }
      }
    }   
    currentMap.put(attributes[attributes.length-1], value);
  }

}
