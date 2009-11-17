/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Felix Langner (ZIB)
 */
package org.xtreemfs.sandbox;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.ErrorCodes;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.DIRInterface.DIRException;
import org.xtreemfs.interfaces.DIRInterface.RedirectException;

/**
 * <p>
 * Test of the DIR master-slave replication as an feature of BabuDB,
 * without SSL.
 * </p>
 * 
 * <p>
 * To use this test, setup some DIRs with replication enabled and 
 * give their addresses to this application. Define which of them should 
 * be the master.
 * </p>
 * 
 * @since 09/21/2009
 * @author flangner
 */
public class dir_replication_test {
        
    private final static Random random = new Random();
    private static DIRClient masterClient;
    
    private final static int LOOKUP_MODE = 1;
    private final static int LOOKUP_PERCENTAGE = 50;
    private final static int INSERT_MODE = 2;
    private final static int INSERT_PERCENTAGE = 40;
    private final static int DELETE_MODE = 3;
    private final static int DELETE_PERCENTAGE = 10;
            
    private final static long CHECK_INTERVAL = 15*60*1000;
    
    private final static int MAX_HISTORY_SIZE = 10000;
    
    private static int registeredServices = 0;
    private static int registeredAddressMappings = 0;
    
    private static Map<String,Service> availableServices = 
        new HashMap<String, Service>();
    private static Map<String,AddressMapping> availableAddressMappings = 
        new HashMap<String,AddressMapping>();
        
    private static List<DIRClient> participants = 
        new LinkedList<DIRClient>();
    
    private static UserCredentials creds = null;
        
    private static int masterChanges = 0;
    
    // XXX
    private static long time = 0;
    private static Map<String,Integer> t = new HashMap<String, Integer>();
    private static int viewID = 1;
    private static long lastSeq = 0;
    private static long actSeq = 0;
    
    // 0 - set addressMapping
    // 1 - register service
    // 2 - remove addressMapping
    // 3 - deregister service
    private static int kind = -1;
    private static String uuid = null;
    
    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        System.out.println("LONGRUNTEST OF THE DIR master-slave replication");
        
        Logging.start(Logging.LEVEL_ERROR);
        try {
            TimeSync.initialize(null, 60000, 50);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
                
        // administrator details
        StringSet gids = new StringSet();
        gids.add("");
        creds = new UserCredentials("", gids, "");
        
        // master connection setup
        RPCNIOSocketClient rpcClient = 
            new RPCNIOSocketClient(null, 2*60*1000, 5*60*1000);
        rpcClient.start();
        
        // get the parameters
        if (args.length!=1) usage(); 
        
        if (args[0].indexOf(",") == -1) {
            error("not enough participants to perform the test!");
        } else {
            for (String adr : args[0].split(",")) {
                participants.add(new DIRClient(rpcClient, parseAddress(adr)));
            }
        } 

        setupRandomMasterDIR();
        
        assert(LOOKUP_PERCENTAGE+DELETE_PERCENTAGE+INSERT_PERCENTAGE == 100);
                
        long start = System.currentTimeMillis();
        long operationCount = 0;
        long s = start;
        System.out.println( "Test started [00:00:00]");
        try {
            while (true) {
                // perform a consistency check
                long now = System.currentTimeMillis();
                if ((start+CHECK_INTERVAL) < now) {
                    long diff = (System.currentTimeMillis()-s)/1000;
                    System.out.println( "Consistency-check ["+(diff-diff%60-((diff-diff%60)/60)%60)/3600+":"+((diff-diff%60)/60)%60+":"+diff%60+"]");
                    System.out.println("Throughput: "+((double) (operationCount/
                            (CHECK_INTERVAL/1000)))+" operations/second");
                    operationCount = 0;
                    
                    System.out.println("The master has changed about '" + 
                            masterChanges + "' times.");
                    
                    masterChanges = 0;
                    
                    performConsistencyCheck();
                    start = System.currentTimeMillis();
                    setupRandomMasterDIR();
                } else {
                    try {
                        if (random.nextBoolean())
                            performAddressMappingOperation();
                        else  
                            performServiceOperation();
                        operationCount++;
                    } catch (RedirectException e) {
                        System.out.println("... Master failover ...@LSN("+viewID+":"+actSeq+")");
                        setupRandomMasterDIR();
                    } catch (IOException e) {
                        if (e.getMessage().equals("request timed out")) {
                            System.err.println("Operation ("+kind+") on UUID: "+uuid+" timed out.");
                            for (DIRClient c : participants) {
                                switch (kind) {
                                case 0 : 
                                    try {
                                        performAddressMappingLookup(uuid, c);
                                        System.err.println("But succeeded on "+c.getDefaultServerAddress());
                                    } catch (Exception e1) { 
                                        System.err.println("And failed on "+c.getDefaultServerAddress());
                                    }
                                    break;
                                case 1 : 
                                    try {
                                        performServiceLookup(uuid, c);
                                        System.err.println("But succeeded on "+c.getDefaultServerAddress());
                                    } catch (Exception e1) {
                                        System.err.println("And failed on "+c.getDefaultServerAddress());
                                    }
                                    break;
                                case 2 : 
                                    try {
                                        performAddressMappingLookup(uuid, c);
                                        System.err.println("And failed on "+c.getDefaultServerAddress());
                                    } catch (Exception e1) { 
                                        System.err.println("But succeeded on "+c.getDefaultServerAddress());
                                    }
                                    break;
                                case 3 : 
                                    try {
                                        performServiceLookup(uuid, c);
                                        System.err.println("And failed on "+c.getDefaultServerAddress());
                                    } catch (Exception e1) { 
                                        System.err.println("But succeeded on "+c.getDefaultServerAddress());
                                    }
                                    break;
                                default : throw e;
                                }
                            }
                        } else throw e;
                    }
                }
            }
        } catch (FailureException e) {
            System.err.println("An insert ("+e.kind+"/"+t.get(e.getMessage())+
                    "/LSN("+(viewID-1)+":"+(t.get(e.getMessage())-(time-lastSeq))+")) was lost @"+time+": "+e.getMessage());
            String data = (e.kind == 0) ? availableAddressMappings.get(
                    e.getMessage()).toString() : 
                availableServices.get(e.getMessage()).toString();
            System.err.println("Data: "+data);
            for (DIRClient c : participants) {
                try {
                    switch (e.kind) {
                        case 0:
                            performAddressMappingLookup(e.getMessage(), c);
                            break;
                        case 1: 
                            performServiceLookup(e.getMessage(), c);
                            break;
                        default: assert(false);
                    }
                    System.err.println("But available on: "+c.getDefaultServerAddress());
                } catch (Exception e2) {
                    System.err.println("And lost on: "+c.getDefaultServerAddress());
                }
            }
            
        }
    }
    
    /**
     * <p>
     * Checks the data on synchronous slave-DIRs.
     * </p>
     * @throws Exception 
     */
    private static void performConsistencyCheck() throws Exception {
        performConsistencyCheck(masterClient);
        for (DIRClient participant : participants) {
            performConsistencyCheck(participant);
        }
    }
    
    /**
     * <p>
     * Checks the consistency of the DIR service given by its client.
     * </p>
     * @param client
     */
    private static void performConsistencyCheck(DIRClient client) {
        try {
            for (String uuid : availableAddressMappings.keySet()) {
                performAddressMappingLookup(uuid, client);
            }
            for (String uuid : availableServices.keySet()) {
                performServiceLookup(uuid, client);
            }
            System.out.println("Participant '"+client.getDefaultServerAddress()+
                    "' is up-to-date and consistent.");
        } catch (Exception e) {
            System.out.println("Participant '"+client.getDefaultServerAddress()+
            "' is NOT up-to-date or inconsistent, because: " +
            e.getMessage());
        }
    }

    /**
     * Performs an AddressMapping DIR operation.
     * @throws Exception 
     */
    private static void performAddressMappingOperation() throws Exception {
        if (registeredAddressMappings != 0) {
            switch (mode()) {
            case LOOKUP_MODE :
                performAddressMappingLookup(randomMappingKey(), masterClient);
                break;
                
            case INSERT_MODE :
                performAddressMappingInsert();
                break;
                
            case DELETE_MODE :
                performAddressMappingDelete();
                break;
                
            default : assert (false);
            }
        } else {
            performAddressMappingInsert();
        }
    }
    
    /**
     * Performs an Service DIR operation.
     * @throws Exception 
     */
    private static void performServiceOperation() throws Exception {
        if (registeredServices != 0) {
            switch (mode()) {
            case LOOKUP_MODE :
                performServiceLookup(randomServiceKey(), masterClient);
                break;
                
            case INSERT_MODE :
                performServiceInsert();
                break;
                
            case DELETE_MODE :
                performServiceDelete();
                break;
                
            default : assert (false);
            }
        } else {
            performServiceInsert();
        }
    }

    /**
     * Performs an AddressMapping lookup for the given UUID.
     * @param uuid
     * @param c
     * @throws Exception 
     */
    private static void performAddressMappingLookup(String uuid, DIRClient c) 
        throws Exception{
        
        RPCResponse<AddressMappingSet> rp = null;
        try {
            rp = c.xtreemfs_address_mappings_get(null, uuid);
            AddressMappingSet result = rp.get();
        
            if (result.size() == 0) throw new FailureException(uuid,0);
            if (result.size() > 1) throw new Exception ("UUID not unique!");
            
            if (!equals(availableAddressMappings.get(uuid), result.get(0)))
                throw new FailureException(uuid,0);
        } finally {
            if (rp!=null) rp.freeBuffers();
        }
    }
    
    /**
     * Performs a Service lookup for the given UUID.
     * @param uuid
     * @param c
     * @throws Exception 
     */
    private static void performServiceLookup(String uuid, DIRClient c) 
        throws Exception{
        
        RPCResponse<ServiceSet> rp = null;
        try {
            rp = c.xtreemfs_service_get_by_uuid(null, uuid);
            ServiceSet result = rp.get();
        
            if (result.size() == 0) throw new FailureException(uuid,1);
            if (result.size() > 1) throw new Exception ("UUID not unique!");
            
            if (!equals(availableServices.get(uuid), result.get(0)))
                throw new FailureException(uuid,1);
        } finally {
            if (rp!=null) rp.freeBuffers();
        }
    }
    
    /**
     * Performs an AdressMapping insert.
     * @throws Exception 
     */
    private static void performAddressMappingInsert() throws Exception {
        
        AddressMappingSet load = new AddressMappingSet();
        AddressMapping mapping = randomMapping();        
        load.add(mapping);
        
        kind = 0;
        uuid = mapping.getUuid();
        
        RPCResponse<Long> rp = null;
        try {
            rp = masterClient.xtreemfs_address_mappings_set(null, load);
            long result = rp.get();
            assert(result == 1) : "A previous entry was modified unexpectedly.";
            
            if (availableAddressMappings.size() == MAX_HISTORY_SIZE)
                availableAddressMappings.remove(randomMappingKey());
            
            availableAddressMappings.put(mapping.getUuid(), mapping);
            registeredAddressMappings++;
        } finally {
            if (rp != null) rp.freeBuffers();
        }
        t.put(mapping.getUuid(), (int) time);
        time++;
        actSeq++;
    }
    
    /**
     * Performs a Service insert.
     * @throws Exception 
     */
    private static void performServiceInsert() throws Exception{
        Service service = randomService();

        kind = 1;
        uuid = service.getUuid();
        
        RPCResponse<Long> rp = null;
        try {
            rp = masterClient.xtreemfs_service_register(null, service);
            long result = rp.get();
            assert(result == 1) : "A previous entry was modified unexpectedly.("+result+")";
            
            if (availableServices.size() == MAX_HISTORY_SIZE)
                availableServices.remove(randomServiceKey());
            
            availableServices.put(service.getUuid(), service);
            registeredServices++;
        } finally {
            if (rp != null) rp.freeBuffers();
        }
        t.put(service.getUuid(), (int) time);
        time++;
        actSeq++;
    }
    
    /**
     * Performs an AddressMapping delete.
     * @throws Exception 
     */
    private static void performAddressMappingDelete() throws Exception{
        String uuid = new LinkedList<String>(availableAddressMappings.keySet())
        .get(random.nextInt(availableAddressMappings.size()));
        
        kind = 2;
        dir_replication_test.uuid = uuid;
        
        RPCResponse<?> rp = null;
        try {
            rp = masterClient.xtreemfs_address_mappings_remove(null, uuid);
            rp.get();
            
            availableAddressMappings.remove(uuid);
            registeredAddressMappings--;
        } finally {
            if (rp != null) rp.freeBuffers();
        }
        time++;
        actSeq++;
    }
    
    /**
     * Performs a Service delete.
     * @throws Exception 
     */
    private static void performServiceDelete() throws Exception{
        String uuid = new LinkedList<String>(availableServices.keySet())
        .get(random.nextInt(availableServices.size()));
        
        kind = 3;
        dir_replication_test.uuid = uuid;
        
        RPCResponse<?> rp = null;
        try {
            rp = masterClient.xtreemfs_service_deregister(null, uuid);
            rp.get();
            
            availableServices.remove(uuid);
            registeredServices--;
        } finally {
            if (rp != null) rp.freeBuffers();
        }
        actSeq++;
        time++;
    }
    
    /**
     * @return a random mode for the next step.
     */
    private static int mode() {
        int per = random.nextInt(100);
        if (per<LOOKUP_PERCENTAGE) {
            return LOOKUP_MODE;
        } else if (per<LOOKUP_PERCENTAGE+INSERT_PERCENTAGE) {
            return INSERT_MODE;
        } else {
            return DELETE_MODE;
        }
    }
    
    /**
     * @return a random generated addressMapping.
     */
    private static AddressMapping randomMapping(){
        UUID uuid = UUID.randomUUID();
        long version = 0;
        String protocol = "oncrpc";
        byte[] addr = new byte[4];
        random.nextBytes(addr);
        String address = String.valueOf(addr[0])+"."+String.valueOf(addr[1])+
            "."+String.valueOf(addr[2])+"."+String.valueOf(addr[3]);
        int port = random.nextInt(65534)+1;
        String match_network = "*";
        int ttl_s = 3600;
        String uri = protocol+"://"+address+":"+port;
        return new AddressMapping(uuid.toString(),version,protocol,
                address,port,match_network,ttl_s,uri);
    }

    /**
     * @return UUID of a random key identifying an address mapping.
     */
    private static String randomMappingKey() {
        return new LinkedList<String>(availableAddressMappings.keySet()).get(
                random.nextInt(availableAddressMappings.size()));
    }

    /**
     * @return a random generated service.
     */
    private static Service randomService() {
        ServiceType[] types = ServiceType.values();
        ServiceType type = types[random.nextInt(types.length)];
        UUID uuid = UUID.randomUUID();
        long version = 0;
        String name = uuid.toString();
        long time = System.currentTimeMillis();
        ServiceDataMap data = new ServiceDataMap();
        return new Service(type, uuid.toString(), version, name, time, data);
    }
    
    /**
     * @return UUID of a random key identifying a service.
     */
    private static String randomServiceKey() {
        return new LinkedList<String>(availableServices.keySet()).get(random.
                        nextInt(availableServices.size()));
    }
    
    /**
     * 
     * @param org
     * @param onSrv
     * @return true, if the original mapping equals the mapping on the DIR for
     *         the given values.
     */
    private static boolean equals(AddressMapping org, AddressMapping onSrv) {
        return (org.getAddress().equals(onSrv.getAddress()) && 
                org.getMatch_network().equals(onSrv.getMatch_network()) &&
                org.getPort() == onSrv.getPort() &&
                org.getProtocol().equals(onSrv.getProtocol()) &&
                org.getTtl_s() == onSrv.getTtl_s() &&
                org.getUri().equals(onSrv.getUri()) &&
                org.getVersion()+1 == onSrv.getVersion());
    }

    /**
     * 
     * @param org
     * @param onSrv
     * @return true, if the original service equals the service on the DIR for
     *         the given values.
     */
    private static boolean equals(Service org, Service onSrv) {
        return (org.getName().equals(onSrv.getName()) && 
                org.getType().equals(onSrv.getType()) &&
                org.getData().equals(onSrv.getData()) &&
                org.getVersion()+1 == onSrv.getVersion());
    }
    
    /**
     * Can exit with an error, if the given string was illegal.
     * 
     * @param adr
     * @return the parsed {@link InetSocketAddress}.
     */
    private static InetSocketAddress parseAddress (String adr){
        String[] comp = adr.split(":");
        if (comp.length!=2){
            error("Address '"+adr+"' is illegal!");
            return null;
        }
        
        try {
            int port = Integer.parseInt(comp[1]);
            return new InetSocketAddress(comp[0],port);
        } catch (NumberFormatException e) {
            error("Address '"+adr+"' is illegal! Because: "+comp[1]+" is not a number.");
            return null;
        }      
    }
    
    /**
     * Declares a master randomly from the list of participants.
     * @throws Exception
     */
    private static void setupRandomMasterDIR() throws Exception{    
        int r;
        do 
            { r = random.nextInt(participants.size()); } 
        while (masterClient!=null && masterClient.equals(participants.get(r)));

        masterClient = participants.get(r);
        System.out.println("to "+masterClient.getDefaultServerAddress().toString());
        
        RPCResponse<Object> rp = null;
        try {
            rp = masterClient.replication_toMaster(null, creds);
            rp.get();
        } catch (DIRException e) {
            if (e.getError_code() == ErrorCodes.NOT_ENOUGH_PARTICIPANTS)
                error("There where not enough participants available to" +
                                " perform this operation.");
            else if (e.getError_code() == ErrorCodes.AUTH_FAILED)
                error("You are not authorized to perform this operation.");
            else throw e;
        } finally {
            if (rp!=null) rp.freeBuffers();
        } 
        System.out.println("New master: " + 
                masterClient.getDefaultServerAddress().toString());
        
        masterChanges++;
        viewID++;
        lastSeq = actSeq;
        actSeq = 0;
    }
    
    /**
     * Prints the error <code>message</code> and delegates to usage().
     * @param message
     */
    private static void error(String message) {
        System.err.println(message);
        usage();
    }
       
    /**
     *  Prints out usage informations and terminates the application.
     */
    public static void usage(){
        System.out.println("dir_replication_test <participant_address:port>,<participant_address:port>[,<participant_address:port>]");
        System.out.println("  "+"<participant_address:port> participants of the replication separated by ','");
        System.exit(1);
    }
    
    private static class FailureException extends Exception{ 
        private static final long serialVersionUID = -5567189559458859258L;
        public final int kind;
        
        public FailureException(String message,int kind) {
            super(message);
            this.kind = kind;
        }
    }
}
