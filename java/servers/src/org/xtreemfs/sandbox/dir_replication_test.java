///*  Copyright (c) 2009-2010 Barcelona Supercomputing Center - Centro Nacional
//    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
//
//    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
//    Grid Operating System, see <http://www.xtreemos.eu> for more details.
//    The XtreemOS project has been developed with the financial support of the
//    European Commission's IST program under contract #FP6-033576.
//
//    XtreemFS is free software: you can redistribute it and/or modify it under
//    the terms of the GNU General Public License as published by the Free
//    Software Foundation, either version 2 of the License, or (at your option)
//    any later version.
//
//    XtreemFS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
// */
///*
// * AUTHORS: Felix Langner (ZIB)
// */
//package org.xtreemfs.sandbox;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.UUID;
//
//import org.xtreemfs.common.clients.Client;
//import org.xtreemfs.dir.client.DIRClient;
//import org.xtreemfs.foundation.TimeSync;
//import org.xtreemfs.foundation.logging.Logging;
//import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
//import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
//import org.xtreemfs.interfaces.AddressMapping;
//import org.xtreemfs.interfaces.AddressMappingSet;
//import org.xtreemfs.interfaces.Service;
//import org.xtreemfs.interfaces.ServiceDataMap;
//import org.xtreemfs.interfaces.ServiceSet;
//import org.xtreemfs.interfaces.ServiceType;
//import org.xtreemfs.interfaces.DIRInterface.RedirectException;
//
///**
// * <p>
// * Test of the DIR master-slave replication as an feature of BabuDB,
// * without SSL.
// * </p>
// *
// * <p>
// * To use this test, setup some DIRs with replication enabled and
// * give their addresses to this application.
// * </p>
// *
// * @since 09/21/2009
// * @author flangner
// */
//public class dir_replication_test {
//
//    private final static Random random = new Random();
//    private static DIRClient masterClient;
//
//    private final static int LOOKUP_MODE = 1;
//    private final static int LOOKUP_PERCENTAGE = 50;
//    private final static int INSERT_MODE = 2;
//    private final static int INSERT_PERCENTAGE = 40;
//    private final static int DELETE_MODE = 3;
//    private final static int DELETE_PERCENTAGE = 10;
//
//    private final static long CHECK_INTERVAL = 15*60*1000;
//
//    private final static int MAX_HISTORY_SIZE = 10000;
//
//    private static int registeredServices = 0;
//    private static int registeredAddressMappings = 0;
//
//    private static Map<String, Service> availableServices =
//        new HashMap<String, Service>();
//    private static Map<String,AddressMapping> availableAddressMappings =
//        new HashMap<String,AddressMapping>();
//
//    private static List<DIRClient> participants =
//        new LinkedList<DIRClient>();
//
//    private static long time = 0;
//    private static Map<String,Integer> t = new HashMap<String, Integer>();
//    private static int viewID = 1;
//    private static long lastSeq = 0;
//    private static long actSeq = 0;
//
//    // 0 - set addressMapping
//    // 1 - register service
//    // 2 - remove addressMapping
//    // 3 - deregister service
//    // 4 - lookup mapping
//    // 5 - lookup service
//    private static int kind = -1;
//    private static String uuid = null;
//
//    /**
//     * @param args
//     * @throws Exception
//     */
//    public static void main(String[] args) throws Exception {
//        Logging.start(Logging.LEVEL_INFO);
//
//        Logging.logMessage(Logging.LEVEL_INFO,null,"LONGRUNTEST OF THE DIR master-slave replication");
//        try {
//            TimeSync.initializeLocal(60000, 50);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            System.exit(1);
//        }
//
//        // master connection setup
//        RPCNIOSocketClient rpcClient =
//            new RPCNIOSocketClient(null, 2*60*1000, 5*60*1000, Client.getExceptionParsers());
//        rpcClient.start();
//        rpcClient.waitForStartup();
//
//        // get the parameters
//        if (args.length!=1) usage();
//
//        if (args[0].indexOf(",") == -1) {
//            error("not enough participants to perform the test!");
//        } else {
//            String[] adrs = args[0].split(",");
//            InetSocketAddress[] servers = new InetSocketAddress[adrs.length];
//            for (int i=0;i<adrs.length;i++){
//                servers[i] = parseAddress(adrs[i]);
//                participants.add(new DIRClient(rpcClient, servers[i]));
//            }
//
//            masterClient = new DIRClient(rpcClient,servers);
//        }
//        assert(LOOKUP_PERCENTAGE+DELETE_PERCENTAGE+INSERT_PERCENTAGE == 100);
//
//        long start = System.currentTimeMillis();
//        long operationCount = 0;
//        long s = start;
//        Logging.logMessage(Logging.LEVEL_INFO,masterClient, "Test started [00:00:00]");
//        while (true) {
//            try {
//                // perform a consistency check
//                long now = System.currentTimeMillis();
//                if ((start+CHECK_INTERVAL) < now) {
//                    long diff = System.currentTimeMillis()-s;
//                    Logging.logMessage(Logging.LEVEL_INFO,masterClient, "Consistency-check " + getTimeStamp(diff) + "\n"+
//                            "Throughput: "+((double) (operationCount/
//                            (CHECK_INTERVAL/1000)))+" operations/second");
//                    operationCount = 0;
//
//                    performConsistencyCheck();
//                    diff = System.currentTimeMillis()-s;
//                    Logging.logMessage(Logging.LEVEL_INFO,masterClient,"Consistency-check finished: " + getTimeStamp(diff));
//                    start = System.currentTimeMillis();
//                // perform an operation on the DIR(s)
//                } else {
//                    try {
//                        if (random.nextBoolean()) {
//                            performAddressMappingOperation();
//                        } else {
//                            performServiceOperation();
//                        }
//                        operationCount++;
//                    } catch (RedirectException e) {
//                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"caught an unexpected redirect exception!" +
//                        		"  ...@LSN("+viewID+":"+actSeq+"): "+
//                        		e.getMessage());
//                    } catch (IOException e) {
//                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"request failed: "+e.getMessage());
//                        if ("request timed out".equals(e.getMessage())) {
//                            Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"Operation ("+kind+") on UUID: "+uuid+" timed out.");
//                            for (DIRClient c : participants) {
//                                switch (kind) {
//                                case 0 :
//                                    try {
//                                        performAddressMappingLookup(uuid, c);
//                                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"But succeeded on "+c.getDefaultServerAddress());
//                                    } catch (Exception e1) {
//                                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"And failed on "+c.getDefaultServerAddress());
//                                    }
//                                    break;
//                                case 1 :
//                                    try {
//                                        performServiceLookup(uuid, c);
//                                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"But succeeded on "+c.getDefaultServerAddress());
//                                    } catch (Exception e1) {
//                                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"And failed on "+c.getDefaultServerAddress());
//                                    }
//                                    break;
//                                case 2 :
//                                    try {
//                                        performAddressMappingLookup(uuid, c);
//                                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"And failed on "+c.getDefaultServerAddress());
//                                    } catch (Exception e1) {
//                                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"But succeeded on "+c.getDefaultServerAddress());
//                                    }
//                                    break;
//                                case 3 :
//                                    try {
//                                        performServiceLookup(uuid, c);
//                                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"And failed on "+c.getDefaultServerAddress());
//                                    } catch (Exception e1) {
//                                        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"But succeeded on "+c.getDefaultServerAddress());
//                                    }
//                                    break;
//                                default : assert(false);
//                                }
//                            }
//                        } else throw e;
//                    }
//                }
//            } catch (FailureException e) {
//                Logging.logMessage(Logging.LEVEL_ERROR,masterClient,getTimeStamp(System.currentTimeMillis()-s)+
//                        " An insert ("+e.kind+"/"+t.get(e.getMessage())+
//                        "/LSN("+(viewID-1)+":"+(t.get(e.getMessage())-(time-lastSeq))+
//                        ")) was lost @"+time+": "+e.getMessage());
//
//                String data = (e.kind == 0 || e.kind == 4) ?
//                    availableAddressMappings.get(e.getMessage()).toString() :
//                    availableServices.get(e.getMessage()).toString();
//                Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"Data: "+data);
//                Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"retrying ... ");
//                uuid = e.getMessage();
//                try {
//                    switch (e.kind) {
//                        case 0:
//                            performAddressMappingInsert(true);
//                            break;
//                        case 1:
//                            performServiceInsert(true);
//                            break;
//                        case 2:
//                            performAddressMappingDelete(true);
//                            break;
//                        case 3:
//                            performServiceDelete(true);
//                            break;
//                        case 4:
//                            Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"... failed (addressMappingLookup)");
//                            break;
//                        case 5:
//                            Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"... failed (serviceLookup)");
//                            break;
//                        default: assert(false);
//                    }
//                    Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"Successful");
//                } catch (Exception e2) {
//                    Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"Failed");
//                }
//            }
//        }
//    }
//
//    /**
//     * <p>
//     * Checks the data on synchronous slave-DIRs.
//     * </p>
//     */
//    private static void performConsistencyCheck() {
//        for (DIRClient participant : participants) {
//            performConsistencyCheck(participant);
//        }
//    }
//
//    /**
//     * <p>
//     * Checks the consistency of the DIR service given by its client.
//     * </p>
//     * @param client
//     */
//    private static void performConsistencyCheck(DIRClient client) {
//        try {
//            for (String uuid : availableAddressMappings.keySet()) {
//                performAddressMappingLookup(uuid, client);
//            }
//            for (String uuid : availableServices.keySet()) {
//                performServiceLookup(uuid, client);
//            }
//            Logging.logMessage(Logging.LEVEL_INFO,masterClient,"Participant '"+client.getDefaultServerAddress()+
//                    "' is up-to-date and consistent.");
//        } catch (Exception e) {
//            Logging.logMessage(Logging.LEVEL_INFO,masterClient,"Participant '"+client.getDefaultServerAddress()+
//            "' is NOT up-to-date or inconsistent, because: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Performs an AddressMapping DIR operation.
//     * @throws Exception
//     */
//    private static void performAddressMappingOperation() throws Exception {
//        if (registeredAddressMappings != 0) {
//            switch (mode()) {
//            case LOOKUP_MODE :
//                performAddressMappingLookup(randomMappingKey(), masterClient);
//                break;
//
//            case INSERT_MODE :
//                performAddressMappingInsert(false);
//                break;
//
//            case DELETE_MODE :
//                performAddressMappingDelete(false);
//                break;
//
//            default : assert (false);
//            }
//        } else {
//            performAddressMappingInsert(false);
//        }
//    }
//
//    /**
//     * Performs an Service DIR operation.
//     * @throws Exception
//     */
//    private static void performServiceOperation() throws Exception {
//        if (registeredServices != 0) {
//            switch (mode()) {
//            case LOOKUP_MODE :
//                performServiceLookup(randomServiceKey(), masterClient);
//                break;
//
//            case INSERT_MODE :
//                performServiceInsert(false);
//                break;
//
//            case DELETE_MODE :
//                performServiceDelete(false);
//                break;
//
//            default : assert (false);
//            }
//        } else {
//            performServiceInsert(false);
//        }
//    }
//
//    /**
//     * Performs an AddressMapping lookup for the given UUID.
//     * @param uuid
//     * @param c
//     * @throws Exception
//     */
//    private static void performAddressMappingLookup(String uuid, DIRClient c)
//        throws Exception{
//
//        RPCResponse<AddressMappingSet> rp = null;
//        try {
//            rp = c.xtreemfs_address_mappings_get(null, uuid);
//            AddressMappingSet result = rp.get();
//
//            if (result.size() == 0){
//                Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"AddressMappingLookup: result.size() == 0" +
//                		" on client: "+c.getDefaultServerAddress());
//                throw new FailureException(uuid,4);
//            }
//            if (result.size() > 1) throw new Exception ("UUID not unique!");
//
//            if (!equals(availableAddressMappings.get(uuid), result.get(0))) {
//                Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"Not the same Services! expected: "+
//                        availableAddressMappings.get(uuid)+" received: "+result.get(0));
//                throw new FailureException(uuid,4);
//            }
//        } finally {
//            if (rp!=null) rp.freeBuffers();
//        }
//    }
//
//    /**
//     * Performs a Service lookup for the given UUID.
//     * @param uuid
//     * @param c
//     * @throws Exception
//     */
//    private static void performServiceLookup(String uuid, DIRClient c)
//        throws Exception{
//
//        RPCResponse<ServiceSet> rp = null;
//        try {
//            rp = c.xtreemfs_service_get_by_uuid(null, uuid);
//            ServiceSet result = rp.get();
//
//            if (result.size() == 0) {
//                Logging.logMessage(Logging.LEVEL_INFO,masterClient,"ServiceGet: result.size() == 0" +
//                		" on client: "+c.getDefaultServerAddress());
//                throw new FailureException(uuid,5);
//            }
//            if (result.size() > 1) throw new Exception ("UUID not unique!");
//
//            if (!equals(availableServices.get(uuid), result.get(0))) {
//                Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"Not the same Services! expected: "+
//                        availableServices.get(uuid)+" received: "+result.get(0));
//                throw new FailureException(uuid,5);
//            }
//        } finally {
//            if (rp!=null) rp.freeBuffers();
//        }
//    }
//
//    /**
//     * Performs an AdressMapping insert.
//     * @throws Exception
//     * @param retry
//     */
//    private static void performAddressMappingInsert(boolean retry) throws Exception {
//        AddressMappingSet load = new AddressMappingSet();
//
//        AddressMapping mapping;
//        if (retry) {
//            assert (availableAddressMappings.containsKey(uuid));
//            mapping = availableAddressMappings.get(uuid);
//        } else {
//            do {
//                mapping = randomMapping();
//                kind = 0;
//                uuid = mapping.getUuid();
//            } while (availableAddressMappings.containsKey(uuid));
//        }
//        load.add(mapping);
//
//        if (!retry) {
//            if (availableAddressMappings.size() == MAX_HISTORY_SIZE)
//                availableAddressMappings.remove(randomMappingKey());
//
//            availableAddressMappings.put(uuid, mapping);
//            registeredAddressMappings++;
//        }
//
//        RPCResponse<Long> rp = null;
//        try {
//            rp = masterClient.xtreemfs_address_mappings_set(null, load);
//            long result = rp.get();
//            if(result != 1) {
//                Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"AddressMappingSet: result != 1, result == "+result);
//                throw new FailureException("A previous entry was modified unexpectedly.", kind);
//            }
//        } finally {
//            if (rp != null) rp.freeBuffers();
//        }
//        if (!retry) {
//            t.put(mapping.getUuid(), (int) time);
//            time++;
//            actSeq++;
//        }
//    }
//
//    /**
//     * Performs a Service insert.
//     * @throws Exception
//     * @param retry
//     */
//    private static void performServiceInsert(boolean retry) throws Exception{
//        Service service;
//        if (retry) {
//            assert (availableServices.containsKey(uuid));
//            service = availableServices.get(uuid);
//        } else {
//            do {
//                service = randomService();
//                kind = 1;
//                uuid = service.getUuid();
//            } while (availableServices.containsKey(uuid));
//        }
//        if (!retry) {
//            if (availableServices.size() == MAX_HISTORY_SIZE)
//                availableServices.remove(randomServiceKey());
//
//            availableServices.put(uuid, service);
//            registeredServices++;
//        }
//
//        RPCResponse<Long> rp = null;
//        try {
//            rp = masterClient.xtreemfs_service_register(null, service);
//            long result = rp.get();
//            if(result != 1) {
//                Logging.logMessage(Logging.LEVEL_ERROR,masterClient,"ServiceRegister: result != 1, result == "+result);
//                throw new FailureException("A previous entry was modified unexpectedly.("+result+")", kind);
//            }
//        } finally {
//            if (rp != null) rp.freeBuffers();
//        }
//        if (!retry) {
//            t.put(service.getUuid(), (int) time);
//            time++;
//            actSeq++;
//        }
//    }
//
//    /**
//     * Performs an AddressMapping delete.
//     * @throws Exception
//     * @param retry
//     */
//    private static void performAddressMappingDelete(boolean retry) throws Exception{
//        if (!retry) {
//            uuid = new LinkedList<String>(availableAddressMappings.keySet())
//            .get(random.nextInt(availableAddressMappings.size()));
//
//            kind = 2;
//        }
//
//        if (!retry) {
//            availableAddressMappings.remove(uuid);
//            registeredAddressMappings--;
//        }
//
//        RPCResponse<?> rp = null;
//        try {
//            rp = masterClient.xtreemfs_address_mappings_remove(null, uuid);
//            rp.get();
//        } finally {
//            if (rp != null) rp.freeBuffers();
//        }
//        if (!retry) {
//            time++;
//            actSeq++;
//        }
//    }
//
//    /**
//     * Performs a Service delete.
//     * @throws Exception
//     * @param retry
//     */
//    private static void performServiceDelete(boolean retry) throws Exception{
//        if (!retry) {
//            uuid = new LinkedList<String>(availableServices.keySet())
//            .get(random.nextInt(availableServices.size()));
//
//            kind = 3;
//        }
//
//        if (!retry) {
//            availableServices.remove(uuid);
//            registeredServices--;
//        }
//
//        RPCResponse<?> rp = null;
//        try {
//            rp = masterClient.xtreemfs_service_deregister(null, uuid);
//            rp.get();
//        } finally {
//            if (rp != null) rp.freeBuffers();
//        }
//        if (!retry) {
//            actSeq++;
//            time++;
//        }
//    }
//
//    /**
//     * @return a random mode for the next step.
//     */
//    private static int mode() {
//        int per = random.nextInt(100);
//        if (per<LOOKUP_PERCENTAGE) {
//            return LOOKUP_MODE;
//        } else if (per<LOOKUP_PERCENTAGE+INSERT_PERCENTAGE) {
//            return INSERT_MODE;
//        } else {
//            return DELETE_MODE;
//        }
//    }
//
//    /**
//     * @return a random generated addressMapping.
//     */
//    private static AddressMapping randomMapping(){
//        UUID uuid = UUID.randomUUID();
//        long version = 0;
//        String protocol = "oncrpc";
//        byte[] addr = new byte[4];
//        random.nextBytes(addr);
//        String address = String.valueOf(addr[0])+"."+String.valueOf(addr[1])+
//            "."+String.valueOf(addr[2])+"."+String.valueOf(addr[3]);
//        int port = random.nextInt(65534)+1;
//        String match_network = "*";
//        int ttl_s = 3600;
//        String uri = protocol+"://"+address+":"+port;
//        return new AddressMapping(uuid.toString(),version,protocol,
//                address,port,match_network,ttl_s,uri);
//    }
//
//    /**
//     * @return UUID of a random key identifying an address mapping.
//     */
//    private static String randomMappingKey() {
//        return new LinkedList<String>(availableAddressMappings.keySet()).get(
//                random.nextInt(availableAddressMappings.size()));
//    }
//
//    /**
//     * @return a random generated service.
//     */
//    private static Service randomService() {
//        ServiceType[] types = ServiceType.values();
//        ServiceType type = types[random.nextInt(types.length)];
//        UUID uuid = UUID.randomUUID();
//        long version = 0;
//        String name = uuid.toString();
//        long time = System.currentTimeMillis();
//        ServiceDataMap data = new ServiceDataMap();
//        return new Service(type, uuid.toString(), version, name, time, data);
//    }
//
//    /**
//     * @return UUID of a random key identifying a service.
//     */
//    private static String randomServiceKey() {
//        return new LinkedList<String>(availableServices.keySet()).get(random.
//                        nextInt(availableServices.size()));
//    }
//
//    /**
//     *
//     * @param org
//     * @param onSrv
//     * @return true, if the original mapping equals the mapping on the DIR for
//     *         the given values.
//     */
//    private static boolean equals(AddressMapping org, AddressMapping onSrv) {
//        return (org.getAddress().equals(onSrv.getAddress()) &&
//                org.getMatch_network().equals(onSrv.getMatch_network()) &&
//                org.getPort() == onSrv.getPort() &&
//                org.getProtocol().equals(onSrv.getProtocol()) &&
//                org.getTtl_s() == onSrv.getTtl_s() &&
//                org.getUri().equals(onSrv.getUri()) &&
//                org.getVersion()+1 == onSrv.getVersion());
//    }
//
//    /**
//     *
//     * @param org
//     * @param onSrv
//     * @return true, if the original service equals the service on the DIR for
//     *         the given values.
//     */
//    private static boolean equals(Service org, Service onSrv) {
//        return (org.getName().equals(onSrv.getName()) &&
//                org.getType().equals(onSrv.getType()) &&
//                org.getData().equals(onSrv.getData()) &&
//                org.getVersion()+1 == onSrv.getVersion());
//    }
//
//    /**
//     * Can exit with an error, if the given string was illegal.
//     *
//     * @param adr
//     * @return the parsed {@link InetSocketAddress}.
//     */
//    private static InetSocketAddress parseAddress (String adr){
//        String[] comp = adr.split(":");
//        if (comp.length!=2){
//            error("Address '"+adr+"' is illegal!");
//            return null;
//        }
//
//        try {
//            int port = Integer.parseInt(comp[1]);
//            return new InetSocketAddress(comp[0],port);
//        } catch (NumberFormatException e) {
//            error("Address '"+adr+"' is illegal! Because: "+comp[1]+" is not a number.");
//            return null;
//        }
//    }
//
//    private static String getTimeStamp(long utime) {
//        long time = utime/1000;
//        return "["+(time - time % 60 - ((time-time%60)/60)%60)/3600 + ":" +
//                   ((time - time % 60)/60)%60 + ":" +
//                   time % 60 + "]";
//    }
//
//    /**
//     * Prints the error <code>message</code> and delegates to usage().
//     * @param message
//     */
//    private static void error(String message) {
//        Logging.logMessage(Logging.LEVEL_ERROR,masterClient,message);
//        usage();
//    }
//       
//    /**
//     *  Prints out usage information and terminates the application.
//     */
//    public static void usage(){
//        Logging.logMessage(Logging.LEVEL_INFO,masterClient,
//                "dir_replication_test <participant_address:port>,<participant_address:port>[,<participant_address:port>]");
//        Logging.logMessage(Logging.LEVEL_INFO,masterClient,
//                "  "+"<participant_address:port> participants of the replication separated by ','");
//        System.exit(1);
//    }
//
//    private static class FailureException extends Exception{
//        private static final long serialVersionUID = -5567189559458859258L;
//        public final int kind;
//
//        public FailureException(String message,int kind) {
//            super(message);
//            this.kind = kind;
//        }
//    }
//}
