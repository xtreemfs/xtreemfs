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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import net.minidev.json.JSONObject;

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
import org.xtreemfs.contrib.provisioning.LibJSON.Addresses;
import org.xtreemfs.contrib.provisioning.LibJSON.Reservation;
import org.xtreemfs.contrib.provisioning.LibJSON.Resource;
import org.xtreemfs.contrib.provisioning.LibJSON.Resources;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.mrc.MRCConfig;

import com.google.gson.Gson;
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

  protected Gson gson = new Gson();

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

      String[] dirAddressesString = LibJSON.generateDirAddresses(dirAddresses);

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

      Resources res = gson.fromJson(JSONObject.toJSONString((Map<String, ?>)req.getParams()), Resources.class);      

      // search for storage resource
      for (Resource resource : res.Resources) {
        if (resource.Type.toLowerCase().equals("storage")) {          
          // check for datacenter ID to match DIR ID:
          if (resource.ID.contains(dirAddresses[0].getAddress().getCanonicalHostName())) {

            Reservation result = LibJSON.createReservation(
                resource, 
                LibJSON.generateSchedulerAddress(schedulerAddress), 
                dirAddresses,
                getGroups(), 
                getAuth(JsonRPC.this.adminPassword), 
                client);

            JSONString json = new JSONString(gson.toJson(result));            
            return new JSONRPC2Response(JSONParser.parseJSON(json), req.getID());                
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
      Reservation res = gson.fromJson(JSONObject.toJSONString((Map<String, ?>)req.getParams()), Reservation.class);      

      Addresses addresses = LibJSON.releaseReservation(
          res,
          LibJSON.generateSchedulerAddress(schedulerAddress),
          getGroups(),
          getAuth(JsonRPC.this.adminPassword),
          client
          );

      JSONString json = new JSONString(gson.toJson(addresses));      
      return new JSONRPC2Response(JSONParser.parseJSON(json), req.getID());      
    }   
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
      Reservation res = gson.fromJson(JSONObject.toJSONString((Map<String, ?>)req.getParams()), Reservation.class);      

      Addresses addresses = LibJSON.checkReservation(
          res, 
          dirAddresses, 
          sslOptions, 
          this.client);

      JSONString json = new JSONString(gson.toJson(addresses));      
      return new JSONRPC2Response(JSONParser.parseJSON(json), req.getID());     
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
      Addresses addresses = LibJSON.listReservations(dirAddresses, client);

      JSONString json = new JSONString(gson.toJson(addresses));      
      return new JSONRPC2Response(JSONParser.parseJSON(json), req.getID());
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

      Resources res = LibJSON.getAvailableResources(
          LibJSON.generateSchedulerAddress(schedulerAddress),
          dirAddresses,
          getGroups(), 
          getAuth(adminPassword),
          client);

      JSONString json = new JSONString(gson.toJson(res));

      return new JSONRPC2Response(JSONParser.parseJSON(json), req.getID());
    }
   
  }
}
