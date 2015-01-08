///*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
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
//import java.net.InetSocketAddress;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.UUID;
//
//import org.xtreemfs.common.clients.Client;
//import org.xtreemfs.dir.ErrorCodes;
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
//import org.xtreemfs.interfaces.StringSet;
//import org.xtreemfs.interfaces.UserCredentials;
//import org.xtreemfs.interfaces.DIRInterface.DIRException;
//import org.xtreemfs.mrc.client.MRCClient;
//
///**
// * <p>
// * Test of the MRC master-slave replication as an feature of BabuDB,
// * without SSL.
// * </p>
// *
// * <p>
// * To use this test, setup some MRCs with replication enabled and
// * give their addresses to this application. Define which of them should
// * be the master.
// * </p>
// *
// * @since 10/14/2009
// * @author flangner
// */
//public class mrc_replication_test {
//
//    private final static Random random = new Random();
//    private static MRCClient masterClient;
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
//    private static int registeredFiles = 0;
//
//    private static Map<String,Service> availableServices =
//        new HashMap<String, Service>();
//    private static Map<String,AddressMapping> availableAddressMappings =
//        new HashMap<String,AddressMapping>();
//
//    private static List<MRCClient> participants =
//        new LinkedList<MRCClient>();
//
//    /**
//     * @param args
//     * @throws Exception
//     */
//    public static void main(String[] args) throws Exception {
//        System.out.println("LONGRUNTEST OF THE DIR master-slave replication");
//
//        Logging.start(Logging.LEVEL_ERROR);
//        try {
//            TimeSync.initialize(null, 60000, 50);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            System.exit(1);
//        }
//
//        // administrator details
//        StringSet gids = new StringSet();
//        gids.add("");
//        UserCredentials creds = new UserCredentials("", gids, "");
//
//        // master connection setup
//        RPCNIOSocketClient rpcClient =
//            new RPCNIOSocketClient(null, (int) CHECK_INTERVAL, (int) (CHECK_INTERVAL+20000), Client.getExceptionParsers());
//        rpcClient.start();
//
//        // get the parameters
//        if (args.length!=2) usage();
//        masterClient = new MRCClient(rpcClient,parseAddress(args[0]));
//
//        if (args[1].indexOf(",") == -1) {
//            participants.add(new MRCClient(rpcClient, parseAddress(args[1])));
//        } else {
//            for (String adr : args[1].split(",")) {
//                participants.add(new MRCClient(rpcClient, parseAddress(adr)));
//            }
//        }
//
//        assert(LOOKUP_PERCENTAGE+DELETE_PERCENTAGE+INSERT_PERCENTAGE == 100);
//
//        // run the test
//        System.out.println("Setting up the configuration ...");
//
//        RPCResponse<Object> rp = null;
//        try {
//            // TODO rebuild test
//            //rp = masterClient.replication_toMaster(null, creds);
//            rp.get();
//        } catch (DIRException e) {
//            if (e.getError_code() == ErrorCodes.NOT_ENOUGH_PARTICIPANTS)
//                error("There where not enough participants available to" +
//                		" perform this operation.");
//            else if (e.getError_code() == ErrorCodes.AUTH_FAILED)
//                error("You are not authorized to perform this operation.");
//            else throw e;
//        } finally {
//            if (rp!=null) rp.freeBuffers();
//        }
//
//        long start = System.currentTimeMillis();
//        long operationCount = 0;
//        System.out.println("Done! The test begins:");
//
//        while (true) {
//            // perform a consistency check
//            long now = System.currentTimeMillis();
//            if ((start+CHECK_INTERVAL) < now) {
//                System.out.println("Throughput: "+((double) (operationCount/
//                        (CHECK_INTERVAL/1000)))+" operations/second");
//                operationCount = 0;
//
//                performConsistencyCheck();
//                start = System.currentTimeMillis();
//            } else {
//                if (random.nextBoolean())
//                    performFileOperation();
//                else
//                    performServiceOperation();
//                operationCount++;
//            }
//        }
//    }
//
//    /**
//     * <p>
//     * Checks the data on synchronous slave-DIRs.
//     * </p>
//     * @throws Exception
//     */
//    private static void performConsistencyCheck() throws Exception {
//        performConsistencyCheck(masterClient);
//        for (MRCClient participant : participants) {
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
//    private static void performConsistencyCheck(MRCClient client) {
//        try {
//            for (String uuid : availableAddressMappings.keySet()) {
//                performFileLookup(uuid, client);
//            }
//            for (String uuid : availableServices.keySet()) {
//                performServiceLookup(uuid, client);
//            }
//            System.out.println("Participant '"+client.getDefaultServerAddress()+
//                    "' is up-to-date and consistent.");
//        } catch (Exception e) {
//            System.out.println("Participant '"+client.getDefaultServerAddress()+
//            "' is NOT up-to-date or inconsistent, because: " +
//            e.getMessage());
//        }
//    }
//
//    /**
//     * Performs an file operation on the MRC.
//     * @throws Exception
//     */
//    private static void performFileOperation() throws Exception {
//        if (registeredFiles != 0) {
//            switch (mode()) {
//            case LOOKUP_MODE :
//                performFileLookup(randomMappingKey(), masterClient);
//                break;
//
//            case INSERT_MODE :
//                performFileInsert();
//                break;
//
//            case DELETE_MODE :
//                performFileDelete();
//                break;
//
//            default : assert (false);
//            }
//        } else {
//            performFileInsert();
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
//                performServiceInsert();
//                break;
//
//            case DELETE_MODE :
//                performServiceDelete();
//                break;
//
//            default : assert (false);
//            }
//        } else {
//            performServiceInsert();
//        }
//    }
//
//    /**
//     * Performs an AddressMapping lookup for the given UUID.
//     * @param uuid
//     * @param c
//     * @throws Exception
//     */
//    private static void performFileLookup(String uuid, MRCClient c)
//        throws Exception{
//
//        RPCResponse<AddressMappingSet> rp = null;
//        try {
//            /* TODO
//            c.open(null, credentials, path, flags, mode, w32attrs, coordinates);
//            rp = c.xtreemfs_address_mappings_get(null, uuid); */
//            AddressMappingSet result = rp.get();
//
//            if (result.size() != 1) throw new Exception ((result.size() == 0) ?
//                    "Mapping lost!" : "UUID not unique!");
//
//            if (!equals(availableAddressMappings.get(uuid), result.get(0)))
//                throw new Exception("Unequal address mapping detected!");
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
//    private static void performServiceLookup(String uuid, MRCClient c)
//        throws Exception{
//
//        RPCResponse<ServiceSet> rp = null;
//        try {
//            // TODO rp = c.xtreemfs_service_get_by_uuid(null, uuid);
//            ServiceSet result = rp.get();
//
//            if (result.size() != 1) throw new Exception ((result.size() == 0) ?
//                    "Service lost!" : "UUID not unique!");
//
//            if (!equals(availableServices.get(uuid), result.get(0)))
//                throw new Exception("Unequal service detected!");
//        } finally {
//            if (rp!=null) rp.freeBuffers();
//        }
//    }
//
//    /**
//     * Performs an AdressMapping insert.
//     * @throws Exception
//     */
//    private static void performFileInsert() throws Exception{
//
//        if (availableAddressMappings.size() == MAX_HISTORY_SIZE)
//            availableAddressMappings.remove(randomMappingKey());
//
//        AddressMappingSet load = new AddressMappingSet();
//        AddressMapping mapping = randomMapping();
//        availableAddressMappings.put(mapping.getUuid(), mapping);
//        load.add(mapping);
//        RPCResponse<Long> rp = null;
//        try {
//            // TODO rp = masterClient.xtreemfs_address_mappings_set(null, load);
//            long result = rp.get();
//            assert(result == 1) : "A previous entry was modified unexpectedly.";
//        } finally {
//            if (rp != null) rp.freeBuffers();
//        }
//        registeredFiles++;
//    }
//
//    /**
//     * Performs a Service insert.
//     * @throws Exception
//     */
//    private static void performServiceInsert() throws Exception{
//
//        if (availableServices.size() == MAX_HISTORY_SIZE)
//            availableServices.remove(randomServiceKey());
//
//        Service service = randomService();
//        availableServices.put(service.getUuid(), service);
//        RPCResponse<Long> rp = null;
//        try {
//            // TODO rp = masterClient.xtreemfs_service_register(null, service);
//            long result = rp.get();
//            assert(result == 1) : "A previous entry was modified unexpectedly.";
//        } finally {
//            if (rp != null) rp.freeBuffers();
//        }
//        registeredServices++;
//    }
//
//    /**
//     * Performs an AddressMapping delete.
//     * @throws Exception
//     */
//    private static void performFileDelete() throws Exception{
//        String uuid = new LinkedList<String>(availableAddressMappings.keySet())
//        .get(random.nextInt(availableAddressMappings.size()));
//        RPCResponse<?> rp = null;
//        try {
//            //TODO  rp = masterClient.xtreemfs_address_mappings_remove(null, uuid);
//            rp.get();
//        } finally {
//            if (rp != null) rp.freeBuffers();
//        }
//        availableAddressMappings.remove(uuid);
//        registeredFiles--;
//    }
//
//    /**
//     * Performs a Service delete.
//     * @throws Exception
//     */
//    private static void performServiceDelete() throws Exception{
//        String uuid = new LinkedList<String>(availableServices.keySet())
//        .get(random.nextInt(availableServices.size()));
//        RPCResponse<?> rp = null;
//        try {
//            // TODO rp = masterClient.xtreemfs_service_deregister(null, uuid);
//            rp.get();
//        } finally {
//            if (rp != null) rp.freeBuffers();
//        }
//        availableServices.remove(uuid);
//        registeredServices--;
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
//    /**
//     * Prints the error <code>message</code> and delegates to usage().
//     * @param message
//     */
//    private static void error(String message) {
//        System.err.println(message);
//        usage();
//    }
//
//    /**
//     *  Prints out usage information and terminates the application.
//     */
//    public static void usage(){
//        System.out.println("dir_replication_test <master_address:port> <slave_address:port>[,<slave_address:port>]");
//        System.out.println("  "+"<master_address:port> address of the DIR that has to be declared to master");
//        System.out.println("  "+"<slave_address:port> participants of the replication separated by ','");
//        System.exit(1);
//    }
//}
