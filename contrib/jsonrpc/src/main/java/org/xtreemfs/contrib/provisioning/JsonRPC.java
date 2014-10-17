/**
 * Copyright 2011-2012 Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 * Authors: Michael Berlin, Patrick Schäfer
 */
package org.xtreemfs.contrib.provisioning;

import com.goebl.david.Webb;
import com.google.gson.Gson;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import net.minidev.json.JSONObject;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.contrib.provisioning.LibJSON.*;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    protected String adminPassword = "";

    protected ResourceLoader resourceLoader;

    protected SSLOptions sslOptions = null;

    protected IRMConfig config = null;

    protected Gson gson = new Gson();

    enum METHOD {
        getResourceTypes,

        calculateResourceCapacity,

        calculateResourceAgg,

        getAvailableResources,

        reserveResources,

        verifyResources,

        releaseResources,

        listReservations
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
        File defaultIrmConfigFile = new File("/etc/xos/xtreemfs/irm.properties");
        if(!defaultIrmConfigFile.exists()) {
            defaultIrmConfigFile = getResource("WEB-INF/" + this.DEFAULT_DIR_CONFIG);
            if (defaultIrmConfigFile == null) { // for j-unit tests check this path too
                defaultIrmConfigFile = new File("src/main/webapp/WEB-INF/" + this.DEFAULT_DIR_CONFIG);
            }
        }

        if (defaultIrmConfigFile != null && defaultIrmConfigFile.exists()) {
            Logger.getLogger(JsonRPC.class.getName()).log(Level.INFO, "Found a config file: " + defaultIrmConfigFile.getAbsolutePath());
            // the DIRConfig does not contain a "dir_service." property
            config = new IRMConfig(defaultIrmConfigFile.getAbsolutePath());
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

        if(config != null) {
            LibJSON.setIrmConfig(config);
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
            this.dispatcher.register(new ReserveResourcesHandler(this.client));
            this.dispatcher.register(new ReleaseResourcesHandler(this.client));
            this.dispatcher.register(new VerifyResources(this.client));
            this.dispatcher.register(new AvailableResources(this.client));

            this.dispatcher.register(new ResourceTypesHandler(this.client));
            this.dispatcher.register(new CalculateResourceCapacityHandler(this.client));
            this.dispatcher.register(new CalculateResourceAggHandler(this.client));

        } catch (Exception e) {
            e.printStackTrace();

            // stop client
            stop();

            throw new IOException("client.start() failed (threw an exception)");
        }

        if(config.getCrsUrl() != null && !config.getCrsUrl().equals("")) {
            addMangerToCrs(config.getCrsUrl());
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
    public String execute(String json_string) {
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
    public class CalculateResourceAggHandler extends AbstractRequestHandler {

        public CalculateResourceAggHandler(Client c) {
            super(c, new METHOD[]{METHOD.calculateResourceAgg});
        }

        // Processes the requests
        @SuppressWarnings("unchecked")
        @Override
        public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {

            Resources res = gson.fromJson(JSONObject.toJSONString((Map<String, ?>)req.getParams()), Resources.class);
            Resource resource = LibJSON.calculateResourceAgg(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    getGroups(),
                    getAuth(JsonRPC.this.adminPassword),
                    client);

            JSONString json = new JSONString(gson.toJson(resource));
            return new JSONRPC2Response(JSONParser.parseJSON(json), req.getID());
        }
    }


    /**
     * Implements a handler for the
     */
    public class CalculateResourceCapacityHandler extends AbstractRequestHandler {

        public CalculateResourceCapacityHandler(Client c) {
            super(c, new METHOD[]{METHOD.calculateResourceCapacity});
        }

        // Processes the requests
        @SuppressWarnings("unchecked")
        @Override
        public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {

            ResourceCapacity res = gson.fromJson(JSONObject.toJSONString((Map<String, ?>)req.getParams()), ResourceCapacity.class);

            ResourceMapper resource = LibJSON.calculateResourceCapacity(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    getGroups(),
                    getAuth(JsonRPC.this.adminPassword),
                    client);

            JSONString json = new JSONString(gson.toJson(resource));
            return new JSONRPC2Response(JSONParser.parseJSON(json), req.getID());
        }
    }

    /**
     * Implements a handler for the
     */
    public class ResourceTypesHandler extends AbstractRequestHandler {

        public ResourceTypesHandler(Client c) {
            super(c, new METHOD[]{METHOD.getResourceTypes});
        }

        // Processes the requests
        @Override
        public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
            Types resource = LibJSON.getResourceTypes();
            JSONString json = new JSONString(gson.toJson(resource));
            return new JSONRPC2Response(JSONParser.parseJSON(json), req.getID());
        }
    }


    /**
     * Implements a handler for the
     */
    public class ReserveResourcesHandler extends AbstractRequestHandler {

        public ReserveResourcesHandler(Client c) {
            super(c, new METHOD[]{METHOD.reserveResources});
        }

        // Processes the requests
        @SuppressWarnings("unchecked")
        @Override
        public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {

            Resources res = gson.fromJson(JSONObject.toJSONString((Map<String, ?>)req.getParams()), Resources.class);

            Reservations reservations = LibJSON.reserveResources(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    dirAddresses,
                    getGroups(),
                    getAuth(JsonRPC.this.adminPassword),
                    client);

            JSONString json = new JSONString(gson.toJson(reservations));
            return new JSONRPC2Response(JSONParser.parseJSON(json), req.getID());
        }
    }



    /**
     * Implements a handler for the "releaseResources" JSON-RPC method
     */
    public class ReleaseResourcesHandler extends AbstractRequestHandler {

        public ReleaseResourcesHandler(Client c) {
            super(c, new METHOD[]{METHOD.releaseResources});
        }

        // Processes the requests
        @Override
        @SuppressWarnings("unchecked")
        public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
            Reservations res = gson.fromJson(JSONObject.toJSONString((Map<String, ?>)req.getParams()), Reservations.class);

            LibJSON.releaseResources(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    getGroups(),
                    getAuth(JsonRPC.this.adminPassword),
                    client
            );

            return new JSONRPC2Response("", req.getID());
        }
    }

    /**
     * Implements a handler for the "checkReservation" JSON-RPC method
     */
    public class VerifyResources extends AbstractRequestHandler {

        public VerifyResources(Client c) {
            super(c, new METHOD[]{METHOD.verifyResources});
        }

        // Processes the requests
        @Override
        @SuppressWarnings("unchecked")
        public JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception {
            Reservations res = gson.fromJson(JSONObject.toJSONString((Map<String, ?>)req.getParams()), Reservations.class);


            ReservationStati reservStat = LibJSON.verifyResources(
                    res,
                    LibJSON.generateSchedulerAddress(schedulerAddress),
                    dirAddresses,
                    sslOptions,
                    getGroups(),
                    getAuth(JsonRPC.this.adminPassword),
                    this.client);

            JSONString json = new JSONString(gson.toJson(reservStat));
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

    private void addMangerToCrs(String crsUrl) {
        for(int errorCount = 0; errorCount < 15; errorCount++) {
            try {
                tryAddManagerToCrs(crsUrl);
                break;
            } catch (Exception e) {
                Logger.getLogger(JsonRPC.class.getName()).log(Level.WARNING, "Adding manager to CRS failed: " + e.getMessage());
                try{
                    Thread.sleep(10000);
                } catch (InterruptedException ex){
                    break;
                }
            }
        }
    }

    private void tryAddManagerToCrs(String crsUrl) throws Exception {
        String url = getAddManagerURL(crsUrl);
        Webb webb = Webb.create();
        com.goebl.david.Response response = webb.post(url).header("Content-Type", "application/json")
                .body("{\"Manager\": \"IRM\", \"Hostname\": \"" + LibJSON.getMyAddress() + "\", \"Port\": \"8080\"}")
                .asJsonObject();
        response.getBody();
    }

    private String getAddManagerURL(String crsUrl) {
        if(crsUrl.endsWith("/method/addManager")) {
            return crsUrl;
        } else if (crsUrl.endsWith("/")) {
            return crsUrl + "method/addManager";
        } else {
            return crsUrl + "/method/addManager";
        }
    }
}
