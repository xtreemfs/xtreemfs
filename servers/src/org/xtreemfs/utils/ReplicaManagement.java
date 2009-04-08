/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.client.MRCClient;

/**
 * A tool to manage your Replicas. File can be marked as read-only, replicas can be added, ...
 * 06.04.2009
 */
public class ReplicaManagement {
    public final static String ADD_REPLICA = "ADD";
    public final static String REMOVE_REPLICA = "REMOVE";
    public final static String SET_READ_ONLY = "SET_READ-ONLY";
    public final static String SET_WRITABLE = "SET_WRITABLE";
    public final static String IS_READ_ONLY = "IS_READ-ONLY";
    public final static String LIST_REPLICAS = "LIST_REPLICAS";

    /**
     * hidden command
     * creates a volume (named "test"), dir (named "test") and a file (named "/test/test.txt") with some data
     */
    public final static String CREATE_TEST_ENV = "CREATE_TEST_ENV";
    public final static String LIST_SUITABLE_OSDS_FOR_REPLICA = "LIST_OSDS";

    private final String filepath;
    private RandomAccessFile file;
    private MRCClient mrcClient;

    public final UserCredentials credentials;
    public final String volume;
    private final InetSocketAddress dirAddress;
    private final DIRClient dirClient;
    private InetSocketAddress mrcAddress;

    private XLocations xLoc;
    private RPCNIOSocketClient client;
    private TimeSync timeSync;

    /**
     * @throws IOException
     * @throws InterruptedException
     * @throws ONCRPCException
     * 
     */
    public ReplicaManagement(InetSocketAddress dirAddress, String volume, String filepath)
            throws Exception {
        try {
            Logging.start(Logging.LEVEL_ERROR);
            
            this.filepath = filepath;
            this.volume = volume;
            this.dirAddress = dirAddress;

            StringSet groupIDs = new StringSet();
            groupIDs.add("root");
            this.credentials = new UserCredentials("root", groupIDs, "");

            // client
            client = new RPCNIOSocketClient(null, 10000, 5 * 60 * 1000);
            client.start();
            client.waitForStartup();
            dirClient = new DIRClient(client, dirAddress);
            
            // start services
            timeSync = TimeSync.initialize(dirClient, 60 * 1000, 50);
            timeSync.waitForStartup();

            UUIDResolver.start(dirClient, 1000, 10 * 10 * 1000);
        } catch (Exception e) {
            shutdown();
            throw e;
        }
    }

    /**
     * @throws ONCRPCException
     * @throws IOException
     * @throws InterruptedException
     */
    public void initialize() throws ONCRPCException, IOException, InterruptedException {
        ServiceSet sSet;
        // get MRC address
        RPCResponse<ServiceSet> r = dirClient.xtreemfs_service_get_by_name(dirAddress, volume);
        sSet = r.get();
        r.freeBuffers();

        if(sSet.size() != 0)
            mrcAddress = new ServiceUUID(sSet.get(0).getData().get("mrc")).getAddress();
        else
            throw new IOException("cannot find volume.");

        this.mrcClient = new MRCClient(client, mrcAddress);
        this.file = new RandomAccessFile("r", mrcAddress, volume + filepath, client, credentials);

        xLoc = new XLocations(file.getCredentials().getXlocs());
    }

    // interactive
    public void addReplica() throws Exception {
        if (file.isReadOnly()) {
            List<ServiceUUID> suitableOSDs = listSuitableOSDs();

            String[] osdNumbers = null;
            BufferedReader in = null;
            while (true) {
                try {
                    // at the moment all replicas must have the same StripingPolicy
                    in = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Please select " + file.getStripingPolicy().getWidth()
                            + " OSD(s) which should be used for the replica.");
                    System.out.println("# Select the OSD(s) through the prefix-numbers and use ',' as seperator. #");
                    osdNumbers = in.readLine().split(",");
                    
                    break;
                } catch (IOException e) {
                    System.out.println("Please try it again");
                    continue;
                } finally {
                    if (in != null)
                        in.close();
                }
            }

            // create list with selected OSDs for replica
            List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
            for (String osdNumber : osdNumbers) {
                osds.add(suitableOSDs.get(Integer.parseInt(osdNumber)-1));
            }

            addReplica(suitableOSDs);
        } else
            throw new IOException("File is not marked as read-only.");
    }

    public void addReplica(List<ServiceUUID> osds) throws Exception {
        if (file.isReadOnly()) {
            // at the moment all replicas must have the same StripingPolicy
            file.addReplica(osds, file.getStripingPolicy());
        } else
            throw new IOException("File is not marked as read-only.");
    }

    // interactive
    public void removeReplica() throws Exception {
        if (file.isReadOnly()) {
            printListOfReplicas(xLoc.getReplicas());           

            int replicaNumber;
            BufferedReader in = null;
            while (true) {
                try {
                    // at the moment all replicas must have the same StripingPolicy
                    in = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Please select a replica which should be removed.");
                    System.out.println("# Select the replica through the prefix-number. #");
                    replicaNumber = Integer.parseInt(in.readLine());

                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Please try it again");
                    continue;
                } catch (IOException e) {
                    System.out.println("Please try it again");
                    continue;
                } finally {
                    if (in != null)
                        in.close();
                }
            }
            
            file.removeReplica(xLoc.getReplicas().get(replicaNumber-1));
        } else
            throw new IOException("File is not marked as read-only.");
    }

    public void removeReplica(ServiceUUID osd) throws Exception {
        if (file.isReadOnly())
            file.removeReplica(osd);
        else
            throw new IOException("File is not marked as read-only.");
    }

    public void setReadOnly(boolean mode) throws Exception {
        file.setReadOnly(mode);
    }

    public void isReadOnly() throws Exception {
        if (file.isReadOnly())
            System.out.println("File is marked as read-only.");
        else
            System.out.println("File is NOT marked as read-only.");
    }

    public void listReplicas() throws UnknownUUIDException {
        printListOfReplicas(xLoc.getReplicas());
    }

    private List<ServiceUUID> listSuitableOSDs() throws Exception {
        List<ServiceUUID> osds = file.getSuitableOSDsForAReplica();
        printListOfOSDs(osds);
        return osds;
    }

    private void printListOfReplicas(List<Replica> replicas) throws UnknownUUIDException {
        StringBuffer out = new StringBuffer();
        int replicaNumber = 1;
        for (Replica r : replicas) {
            // head line
            out.append("[" + replicaNumber + "] ");
            out.append("REPLICA " + (replicaNumber++) + ": " + r.getStripingPolicy().toString() + "\n");

            int osdNumber = 1;
            // OSDs of this replica
            for (ServiceUUID osd : r.getOSDs()) {
                out.append("\t OSD " + (osdNumber++) + ": " + osd.toString() + " (" + osd.getAddress().toString() + ")\n");
            }
        }
        System.out.print(out.toString());
    }

    private void printListOfOSDs(List<ServiceUUID> osds) throws UnknownUUIDException {
        StringBuffer out = new StringBuffer();
        if (osds.size() != 0) {
            int number = 1;
            for (ServiceUUID osd : osds) {
                out.append("[" + number + "] ");
                out.append(osd.toString() + " (" + osd.getAddress().toString() + ")\n");
            }
        } else
            out.append("no suitable OSDs available\n");
        System.out.print(out.toString());
    }

    /**
     * 
     */
    private void shutdown() {
        try {
            if (client != null) {
                client.shutdown();
                client.waitForShutdown();
            }

            UUIDResolver.shutdown();

            if (timeSync != null) {
                timeSync.shutdown();
                timeSync.waitForShutdown();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        final int NUMBER_OF_MANDATORY_ARGS = 4;
        
        if (args.length < NUMBER_OF_MANDATORY_ARGS)
            usage();

        int argNumber = 0;
        InetSocketAddress dirAddress = new InetSocketAddress(args[argNumber].split(":")[0], Integer
                .parseInt(args[argNumber].split(":")[1]));
        argNumber++;
        String volume = args[argNumber++];
        String filepath = args[argNumber++];
        String command = args[argNumber++];

        ReplicaManagement system = null;
        try {
            system = new ReplicaManagement(dirAddress, volume, filepath);

            if (command.equals(ADD_REPLICA)) {
                system.initialize();
                if (args.length > NUMBER_OF_MANDATORY_ARGS) {
                    List<ServiceUUID> osds = new ArrayList<ServiceUUID>(args.length-NUMBER_OF_MANDATORY_ARGS);
                    while(argNumber < args.length) {
                        osds.add(new ServiceUUID(args[argNumber++]));
                    }
                    system.addReplica(osds);
                } else
                    // interactive mode
                    system.addReplica();
            } else if (command.equals(REMOVE_REPLICA)) {
                system.initialize();
                if (args.length > NUMBER_OF_MANDATORY_ARGS) {
                    ServiceUUID osd = new ServiceUUID(args[argNumber++]);
                    system.removeReplica(osd);
                } else
                    // interactive mode
                    system.removeReplica();
            } else if (command.equals(SET_READ_ONLY)) {
                system.initialize();
                system.setReadOnly(true);
            } else if (command.equals(SET_WRITABLE)) {
                system.initialize();
                system.setReadOnly(false);
            } else if (command.equals(IS_READ_ONLY)) {
                system.initialize();
                system.isReadOnly();
            } else if (command.equals(LIST_REPLICAS)) {
                system.initialize();
                system.listReplicas();
            } else if (command.equals(LIST_SUITABLE_OSDS_FOR_REPLICA)) {
                system.initialize();
                system.listSuitableOSDs();
            } else if (command.equals(CREATE_TEST_ENV)) { // hidden command
                if (args.length > NUMBER_OF_MANDATORY_ARGS) {
                    InetSocketAddress mrc = new InetSocketAddress(args[argNumber].split(":")[0], Integer
                            .parseInt(args[argNumber].split(":")[1]));
                    argNumber++;
                    system.createTestEnv(mrc);
                }
            } else {
                usage();
            }
            
            System.out.println("---------------------------------");
            System.out.println("Command processed successfully!");
        } catch (Exception e) {
            // TODO: better Exception handling
            System.out.println("---------------------------------");
            System.out.println("ERROR: Something went wrong!");
            e.printStackTrace();
        } finally {
            if (system != null)
                system.shutdown();
        }
    }

    public static void usage() {
        StringBuffer out = new StringBuffer();
        out.append("Usage: java -cp <xtreemfs-jar> org.xtreemfs.utils.ReplicaManagement ");
        out.append("<DIR-address> <volume> <filepath> <command> [parameters]\n");
        out.append("commands:\n");
        out.append("\t" + IS_READ_ONLY + ": Checks if the file is already marked as read-only.\n");
        out.append("\t" + SET_READ_ONLY + ": Marks the file as read-only.\n");
        out.append("\t" + SET_WRITABLE + ": Marks the file as writable (normal file).\n");
        out.append("\t" + LIST_REPLICAS + ": Lists all replicas of this file.\n");
        out.append("\t" + LIST_SUITABLE_OSDS_FOR_REPLICA
                + ": Lists all suitable OSDs for this file, which can be used for a new replica. \n");
        out.append("\t" + ADD_REPLICA + ": An interactive mode for adding a replica.\n");
        out.append("\t" + REMOVE_REPLICA + ": An interactive mode for removing a replica.\n");
        out.append("\t" + ADD_REPLICA + " [<UUID_of_OSD1 UUID_of_OSD2, ...>]: Adds a replica with these OSDs. "
                + "The given number of OSDs must be the same as in the striping policy. "
                + "Use space as seperator.\n");
        out.append("\t" + REMOVE_REPLICA + " [<UUID_of_head-OSD>]"
                + ": Removes the replica where the given OSD is the head-OSD. \n");
        System.out.println(out.toString());
        System.exit(1);
    }
    
    /**
     * creates a volume (named "test"), dir (named "test") and a file (named "/test/test.txt") with some data
     * @throws InterruptedException 
     * @throws IOException 
     * @throws ONCRPCException 
     */
    private void createTestEnv(InetSocketAddress mrcAddress) throws ONCRPCException, IOException, InterruptedException{
        mrcClient = new MRCClient(client, mrcAddress);

        // create a volume (no access control)
        RPCResponse r = mrcClient.mkvol(mrcAddress, credentials, "test",
                Constants.OSD_SELECTION_POLICY_SIMPLE,
                new StripingPolicy(Constants.STRIPING_POLICY_RAID0, 64, 1),
                Constants.ACCESS_CONTROL_POLICY_NULL, 0);
        r.get();
        r.freeBuffers();

        // create a directories
        r = mrcClient.mkdir(mrcAddress, credentials, volume + "/test", 0);
        r.get();
        r.freeBuffers();

        // create a file
        r = mrcClient.create(mrcAddress, credentials, volume + "/test/test.txt", 0);
        r.get();
        r.freeBuffers();
        
        // fill file with some data
        byte[] bytesIn = new String("Hello Test").getBytes();
        int length = bytesIn.length;
        ReusableBuffer data = ReusableBuffer.wrap(bytesIn);
        
        file = new RandomAccessFile("rw", mrcAddress, volume + "/test/test.txt", client , credentials);
        file.writeObject(0, 0, data);
    }
}
