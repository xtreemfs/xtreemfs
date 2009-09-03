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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 * A tool to manage your Replicas. File can be marked as read-only, replicas can be added, ... <br>
 * 06.04.2009
 */
public class xtfs_repl {
    public final static String      OPTION_HELP                               = "h";

    public final static String      OPTION_HELP_LONG                          = "-help";

    public final static String      OPTION_ADD_REPLICA                        = "a";

    public final static String      OPTION_ADD_AUTOMATIC_REPLICA              = "-add_auto";

    public final static String      OPTION_REMOVE_REPLICA                     = "r";

    public final static String      OPTION_REMOVE_AUTOMATIC_REPLICA           = "-remove_auto";

    public final static String      OPTION_SET_READ_ONLY                      = "-set_readonly";

    public final static String      OPTION_SET_WRITABLE                       = "-set_writable";

    public final static String      OPTION_LIST_REPLICAS                      = "l";

    public final static String      OPTION_LIST_SUITABLE_OSDS_FOR_A_REPLICA   = "o";

    public final static String      OPTION_STRIPE_WIDTH                       = "w";

    public final static String      OPTION_RSEL_POLICY_GET                    = "-rsp_get";

    public final static String      OPTION_RSEL_POLICY_SET                    = "-rsp_set";

    public final static String      RSEL_POLICY_DEFAULT                       = "default";

    public final static String      RSEL_POLICY_FQDN                          = "fqdn";

    public final static String      RSEL_POLICY_DCMAP                         = "dcmap";

    public final static String      OPTION_REPLICATION_FLAG_FULL_REPLICA      = "-full";

    public final static String      OPTION_REPLICATION_FLAG_TRANSFER_STRATEGY = "-strategy";

    public final static String      TRANSFER_STRATEGY_RANDOM                  = "random";

    public final static String      TRANSFER_STRATEGY_SEQUENTIAL              = "sequential";

    public final static String      OPTION_SSL_CREDS_FILE                     = "c";

    public final static String      OPTION_SSL_CREDS_PASSWORD                 = "cpass";

    public final static String      OPTION_SSL_TRUSTED_CA_FILE                = "t";

    public final static String      OPTION_SSL_TRUSTED_CA_PASSWORD            = "tpass";

    public final static int         DEFAULT_REPLICATION_FLAGS                 = ReplicationFlags
                                                                                      .setPartialReplica(ReplicationFlags
                                                                                              .setRandomStrategy(0));

    private final String            relPath;

    private final String            volPath;

    private RandomAccessFile        file;

    private MRCClient               mrcClient;

    public final UserCredentials    credentials;

    public final String             volume;

    private final InetSocketAddress dirAddress;

    private final DIRClient         dirClient;

    private InetSocketAddress       mrcAddress;

    private XLocations              xLoc;

    private RPCNIOSocketClient      client;

    private TimeSync                timeSync;

    public static final Pattern     IPV4_PATTERN                              = Pattern
                                                                                      .compile("b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)"
                                                                                              + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)b");

    public static final Pattern     IPV6_PATTERN                              = Pattern
                                                                                      .compile(
                                                                                              "((([0-9a-f]{1,4}+:){7}+[0-9a-f]{1,4}+)|(:(:[0-9a-f]"
                                                                                                      + "{1,4}+){1,6}+)|(([0-9a-f]{1,4}+:){1,6}+:)|(::)|(([0-9a-f]"
                                                                                                      + "{1,4}+:)(:[0-9a-f]{1,4}+){1,5}+)|(([0-9a-f]{1,4}+:){1,2}"
                                                                                                      + "+(:[0-9a-f]{1,4}+){1,4}+)|(([0-9a-f]{1,4}+:){1,3}+(:[0-9a-f]{1,4}+)"
                                                                                                      + "{1,3}+)|(([0-9a-f]{1,4}+:){1,4}+(:[0-9a-f]{1,4}+){1,2}+)|(([0-9a-f]"
                                                                                                      + "{1,4}+:){1,5}+(:[0-9a-f]{1,4}+))|(((([0-9a-f]{1,4}+:)?([0-9a-f]"
                                                                                                      + "{1,4}+:)?([0-9a-f]{1,4}+:)?([0-9a-f]{1,4}+:)?)|:)(:(([0-9]{1,3}+\\.)"
                                                                                                      + "{3}+[0-9]{1,3}+)))|(:(:[0-9a-f]{1,4}+)*:([0-9]{1,3}+\\.){3}+[0-9]"
                                                                                                      + "{1,3}+))(/[0-9]+)?",
                                                                                              Pattern.CASE_INSENSITIVE);

    /**
     * required for METHOD_DNS <br>
     * 13.05.2009
     */
    private static final class UsableOSD implements Comparable {
        public ServiceUUID osd;

        public int         match;

        public UsableOSD(ServiceUUID uuid, int match) {
            this.match = match;
            this.osd = uuid;
        }

        @Override
        public int compareTo(Object o) {
            UsableOSD other = (UsableOSD) o;
            return other.match - this.match;
        }
    }

    /**
     * @param sslOptions
     * @throws IOException
     * @throws InterruptedException
     * @throws ONCRPCException
     * 
     */
    public xtfs_repl(String relPath, InetSocketAddress dirAddress, String volume, String volPath,
            SSLOptions sslOptions) throws Exception {
        try {
            Logging.start(Logging.LEVEL_ERROR, Category.tool);

            this.relPath = relPath;
            this.volPath = volPath;
            this.volume = volume;
            this.dirAddress = dirAddress;

            // TODO: use REAL user credentials (this is a SECURITY HOLE)
            StringSet groupIDs = new StringSet();
            groupIDs.add("root");
            this.credentials = new UserCredentials("root", groupIDs, "");

            // client
            client = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000);
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
    public void initialize(boolean fileRequired) throws ONCRPCException, IOException, InterruptedException {
        ServiceSet sSet;
        // get MRC address
        RPCResponse<ServiceSet> r = dirClient.xtreemfs_service_get_by_name(dirAddress, volume);
        sSet = r.get();
        r.freeBuffers();

        if (sSet.size() != 0)
            mrcAddress = new ServiceUUID(sSet.get(0).getData().get("mrc")).getAddress();
        else {
            System.err.println("unknown volume");
            System.exit(1);
        }

        this.mrcClient = new MRCClient(client, mrcAddress);

        File f = new File(relPath);
        if (fileRequired) {
            if (!f.isFile()) {
                System.err.println("'" + relPath + "' is not a file");
                System.exit(1);
            }
            this.file = new RandomAccessFile("r", mrcAddress, volume + volPath, client, credentials);
            xLoc = new XLocations(file.getCredentials().getXlocs());
        } else {
            if (!f.exists()) {
                System.err.println("'" + relPath + "' does not exist");
                System.exit(1);
            }
        }
    }

    /*
     * add replica
     */
    public void addReplica(List<ServiceUUID> osds, int replicationFlags, int stripeWidth) throws Exception {
        if (file.isReadOnly()) {
            if (stripeWidth == 0) // not set => policy from replica 1
                stripeWidth = osds.size();

            StripingPolicy sp = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, (int) file
                    .getStripeSize(), stripeWidth);
            file.addReplica(osds, sp, replicationFlags);

            // start replication of full replicas
            if (ReplicationFlags.isFullReplica(replicationFlags))
                startReplicationOnOSDs(osds.get(0));
        } else
            System.err.println("File is not marked as read-only.");
    }

    // automatic
    public void addReplicaAutomatically(int replicationFlags, int stripeWidth)
            throws Exception {
        if (file.isReadOnly()) {
            if (stripeWidth == 0) // not set => policy from replica 1
                stripeWidth = file.getStripingPolicy().getWidth();

            List<ServiceUUID> suitableOSDs = file.getSuitableOSDsForAReplica();
            if (suitableOSDs.size() < stripeWidth) {
                System.err.println("could not create replica: not enough suitable OSDs available");
                System.exit(1);
            }

            addReplica(suitableOSDs.subList(0, stripeWidth), replicationFlags,
                    stripeWidth);
        } else
            System.err.println("File is not marked as read-only.");
    }

    /**
     * contacts the OSDs so they begin to replicate the file
     * 
     * @param addedOSD
     * @throws IOException
     */
    private void startReplicationOnOSDs(ServiceUUID addedOSD) throws IOException {
        // get just added replica
        Replica addedReplica = file.getXLoc().getReplica(addedOSD);
        if (addedReplica.isPartialReplica()) // break, because replica should not be filled
            return;
        StripingPolicyImpl sp = addedReplica.getStripingPolicy();
        String fileID = file.getFileId();
        FileCredentials cred = file.getCredentials();

        OSDClient osdClient = new OSDClient(client);
        // send requests to all OSDs of this replica
        try {
            List<ServiceUUID> osdList = addedReplica.getOSDs();
            List<ServiceUUID> osdListCopy = new ArrayList<ServiceUUID>(addedReplica.getOSDs());
            // take lowest objects of file
            for (int objectNo = 0; osdListCopy.size() != 0; objectNo++) {
                // get index of OSD for this object
                int indexOfOSD = sp.getOSDforObject(objectNo);
                // remove OSD
                ServiceUUID osd = osdList.get(indexOfOSD);
                osdListCopy.remove(osd);
                // send request (read only 1 byte)
                RPCResponse<ObjectData> r = osdClient.read(osd.getAddress(), fileID, cred, objectNo, 0, 0, 1);
                r.get();
                r.freeBuffers();
            }
        } catch (UnknownUUIDException e) {
            // ignore; should not happen
        } catch (ONCRPCException e) {
            throw new IOException("At least one OSD could not be contacted to replicate the file.", e);
        } catch (IOException e) {
            throw new IOException("At least one OSD could not be contacted to replicate the file.", e);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void getReplicaSelectionPolicy() {
        try {
            RPCResponse<String> r = mrcClient.getxattr(null, credentials, volume + "/",
                    "xtreemfs.repl_policy_id");
            String v = r.get();
            if (v.equals("1")) {
                System.out.println("replica selection policy: default");
            } else if (v.equals("3")) {
                System.out.println("replica selection policy: FQDN");
            } else if (v.equals("4")) {
                System.out.println("replica selection policy: DCMap");
            } else {
                System.out.println("replica selection policy: custom (#" + v + ")");
            }
        } catch (Exception ex) {
            System.err.println("cannot read replica selection policy");
            ex.printStackTrace();
        }
    }

    public void setReplicaSelectionPolicy(String rsp) {
        try {
    
            int id = 0;
            if (rsp.equalsIgnoreCase(RSEL_POLICY_DEFAULT)) {
                id = 1;
            } else if (rsp.equalsIgnoreCase(RSEL_POLICY_FQDN)) {
                id = 3;
            } else if (rsp.equalsIgnoreCase(RSEL_POLICY_DCMAP)) {
                id = 4;
            } else {
                id = Integer.valueOf(rsp);
            }
    
            RPCResponse r = mrcClient.setxattr(null, credentials, volume + "/", "xtreemfs.repl_policy_id", id
                    + "", 0);
            r.get();
            System.out.println("replication policy changed");
        } catch (Exception ex) {
            System.err.println("cannot read replica selection policy");
            ex.printStackTrace();
        }
    }

    /*
     * remove replica
     */
    public void removeReplica(ServiceUUID osd) throws Exception {
        if (file.isReadOnly()) {
            file.removeReplica(osd);
        } else
            System.err.println("File is not marked as read-only.");
    }

    // automatic
    public void removeReplicaAutomatically() throws Exception {
        if (file.isReadOnly()) {
            if (file.getXLoc().getNumReplicas() <= 1) {
                System.out.println("No replica exists for removing.");
                return;
            }
            ServiceUUID osd = null;
            Random random = new Random();
            while (true) {
                try {
                    Replica replica = file.getXLoc().getReplicas().get(
                            random.nextInt(file.getXLoc().getNumReplicas()));
                    osd = replica.getHeadOsd();
                    file.removeReplica(osd);
                    break;
                } catch (IOException e) {
                    continue;
                }
            }
        } else
            System.err.println("File is not marked as read-only.");
    }

    /*
     * other commands
     */
    public void setReadOnly(boolean mode) throws Exception {
        file.setReadOnly(mode);
    }

    public void listReplicas() throws UnknownUUIDException {
        printListOfReplicas(xLoc.getReplicas());
    }

    private List<ServiceUUID> listSuitableOSDs() throws Exception {
        List<ServiceUUID> osds = file.getSuitableOSDsForAReplica();
        printListOfOSDs(osds);
        return osds;
    }

    /*
     * outputs
     */
    private void printListOfReplicas(List<Replica> replicas) throws UnknownUUIDException {
        StringBuffer out = new StringBuffer();

        // read-only?
        if (file.isReadOnly())
            out.append("File is read-only.\n");
        else
            out.append("File is writable. No replicas possible.\n");

        int replicaNumber = 1;
        for (Replica r : replicas) {
            // head line
            out.append("REPLICA " + (replicaNumber++) + ":\n");
            out
                    .append("\t Striping Policy: " + r.getStripingPolicy().getPolicy().getType().toString()
                            + "\n");
            out.append("\t Stripe-Size: "
                    + OutputUtils.formatBytes(r.getStripingPolicy().getStripeSizeForObject(0)) + "\n");
            out.append("\t Stripe-Width: " + r.getStripingPolicy().getWidth() + " (OSDs)\n");

            if (file.isReadOnly()) {
                out.append("\t Replication Flags:\n");
                out.append("\t\t Complete: " + r.isComplete() + "\n");
                out.append("\t\t Replica Type: " + (r.isPartialReplica() ? "partial" : "full") + "\n");
                String transferStrategy = "unknown";
                if (ReplicationFlags.isRandomStrategy(r.getTransferStrategyFlags()))
                    transferStrategy = "random";
                else if (ReplicationFlags.isSequentialStrategy(r.getTransferStrategyFlags()))
                    transferStrategy = "sequential";
                else if (ReplicationFlags.isSequentialPrefetchingStrategy(r.getTransferStrategyFlags()))
                    transferStrategy = "sequential prefetching";
                else if (ReplicationFlags.isRarestFirstStrategy(r.getTransferStrategyFlags()))
                    transferStrategy = "rarest first";
                out.append("\t\t Transfer-Strategy: " + transferStrategy + "\n");
            }

            out.append("\t OSDs:\n");

            int osdNumber = 1;
            // OSDs of this replica
            for (ServiceUUID osd : r.getOSDs()) {
                if (osdNumber == 1) {
                    out.append("\t\t [Head-OSD]\t");
                    osdNumber++;
                } else
                    out.append("\t\t [OSD " + (osdNumber++) + "]\t");
                out.append("UUID: " + osd.toString() + ", URL: " + osd.getAddress().toString() + "\n");
            }
        }
        System.out.print(out.toString());
    }

    private void printListOfOSDs(List<ServiceUUID> osds) throws UnknownUUIDException {
        StringBuffer out = new StringBuffer();
        if (osds.size() != 0) {
            int number = 1;
            for (ServiceUUID osd : osds) {
                out.append("[" + number++ + "] ");
                out.append("UUID: " + osd.toString() + ", URL: " + osd.getAddress().toString() + "\n");
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
            e.printStackTrace();
        }
    }

    /**
     * @param options
     * @return
     */
    private static int parseReplicationFlags(Map<String, CliOption> options) {
        int replicationFlags = DEFAULT_REPLICATION_FLAGS;
        CliOption option = options.get(OPTION_REPLICATION_FLAG_FULL_REPLICA);
        if (option != null && option.switchValue)
            replicationFlags = ReplicationFlags.setFullReplica(replicationFlags);

        option = options.get(OPTION_REPLICATION_FLAG_TRANSFER_STRATEGY);
        if (option != null && option.stringValue != null) {
            String method = option.stringValue.replace('\"', ' ').trim();

            if (method.equals(TRANSFER_STRATEGY_RANDOM))
                replicationFlags = ReplicationFlags.setRandomStrategy(replicationFlags);
            else if (method.equals(TRANSFER_STRATEGY_SEQUENTIAL))
                replicationFlags = ReplicationFlags.setSequentialStrategy(replicationFlags);
        }
        return replicationFlags;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(3);
        options.put(OPTION_HELP, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_HELP_LONG, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_SET_READ_ONLY, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_SET_WRITABLE, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_LIST_REPLICAS, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_LIST_SUITABLE_OSDS_FOR_A_REPLICA, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_ADD_REPLICA, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_ADD_AUTOMATIC_REPLICA, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_REMOVE_REPLICA, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_REMOVE_AUTOMATIC_REPLICA, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_SSL_CREDS_FILE, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_SSL_CREDS_PASSWORD, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_SSL_TRUSTED_CA_FILE, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_SSL_TRUSTED_CA_PASSWORD, new CliOption(CliOption.OPTIONTYPE.STRING));
        // options.put("p", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_REPLICATION_FLAG_FULL_REPLICA, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_REPLICATION_FLAG_TRANSFER_STRATEGY, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_STRIPE_WIDTH, new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put(OPTION_RSEL_POLICY_GET, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_RSEL_POLICY_SET, new CliOption(CliOption.OPTIONTYPE.STRING));

        try {
            CLIParser.parseCLI(args, options, arguments);
        } catch (Exception exc) {
            System.out.println(exc);
            usage();
            return;
        }

        CliOption h = options.get(OPTION_HELP);
        if (h.switchValue) {
            usage();
            return;
        }

        h = options.get(OPTION_HELP_LONG);
        if (h.switchValue) {
            usage();
            return;
        }

        if (arguments.size() < 1) {
            usage();
            return;
        }

        xtfs_repl system = null;

        try {
            // resolve the path
            final String filePath = utils.expandPath(arguments.get(0));
            final String url = utils.getxattr(filePath, "xtreemfs.url");

            if (url == null) {
                System.err.println("could not retrieve XtreemFS URL for file '" + filePath + "'");
                System.exit(1);
            }

            final int i0 = url.indexOf("://") + 2;
            final int i1 = url.indexOf(':', i0);
            final int i2 = url.indexOf('/', i1);
            final int i3 = url.indexOf('/', i2 + 1);

            final String dirURL = url.substring(i0 + 1, i1);
            final int dirPort = Integer.parseInt(url.substring(i1 + 1, i2));
            final String volume = url.substring(i2 + 1, i3 == -1 ? url.length() : i3);
            final String volPath = i3 == -1 ? "" : url.substring(i3);
            final InetSocketAddress dirAddress = new InetSocketAddress(dirURL, dirPort);

            // create SSL options (if set)
            SSLOptions sslOptions = null;
            if ((options.get(OPTION_SSL_CREDS_FILE).stringValue != null)
                    && (options.get(OPTION_SSL_CREDS_PASSWORD).stringValue != null)
                    && (options.get(OPTION_SSL_TRUSTED_CA_FILE).stringValue != null)
                    && (options.get(OPTION_SSL_TRUSTED_CA_PASSWORD).stringValue != null)) { // SSL set
                sslOptions = new SSLOptions(new FileInputStream(
                        options.get(OPTION_SSL_CREDS_FILE).stringValue), options
                        .get(OPTION_SSL_CREDS_PASSWORD).stringValue, new FileInputStream(options
                        .get(OPTION_SSL_TRUSTED_CA_FILE).stringValue), options
                        .get(OPTION_SSL_TRUSTED_CA_PASSWORD).stringValue);
            }

            system = new xtfs_repl(filePath, dirAddress, volume, volPath, sslOptions);
            if ((options.get(OPTION_RSEL_POLICY_GET).switchValue)
                    || (options.get(OPTION_RSEL_POLICY_SET).stringValue != null)) {
                system.initialize(false);
            } else {
                system.initialize(true);
            }

            for (Entry<String, CliOption> e : options.entrySet()) {
                if (e.getKey().equals(OPTION_ADD_REPLICA) && e.getValue().stringValue != null) {

                    // parse replication flags and striping policy
                    int replicationFlags = parseReplicationFlags(options);
                    // parse stripe width
                    int stripeWidth = 0;
                    CliOption option = options.get(OPTION_STRIPE_WIDTH);
                    if (option != null && option.numValue != null)
                        stripeWidth = option.numValue.intValue();

                    StringTokenizer st = new StringTokenizer(e.getValue().stringValue, "\", \t");
                    List<ServiceUUID> osds = new ArrayList<ServiceUUID>(st.countTokens());

                    if (st.countTokens() > 0) {
                        while (st.hasMoreTokens())
                            osds.add(new ServiceUUID(st.nextToken()));
                        system.addReplica(osds, replicationFlags, stripeWidth);
                    } else
                        usage();

                } else if (e.getKey().equals(OPTION_ADD_AUTOMATIC_REPLICA)
                        && e.getValue().switchValue) {

                    // parse replication flags and striping policy
                    int replicationFlags = parseReplicationFlags(options);
                    // parse stripe width
                    int stripeWidth = 0;
                    CliOption option = options.get(OPTION_STRIPE_WIDTH);
                    if (option != null && option.numValue != null)
                        stripeWidth = option.numValue.intValue();

                    system.addReplicaAutomatically(replicationFlags, stripeWidth);

                } else if (e.getKey().equals(OPTION_REMOVE_REPLICA) && e.getValue().stringValue != null) {

                    String headOSD = e.getValue().stringValue.replace('\"', ' ').trim();

                    if (headOSD.length() > 0) {
                        ServiceUUID osd = new ServiceUUID(headOSD);
                        system.removeReplica(osd);
                    } else
                        usage();

                } else if (e.getKey().equals(OPTION_REMOVE_AUTOMATIC_REPLICA) && e.getValue().switchValue) {

                    system.removeReplicaAutomatically();

                } else if (e.getKey().equals(OPTION_SET_READ_ONLY) && e.getValue().switchValue) {
                    system.setReadOnly(true);
                } else if (e.getKey().equals(OPTION_SET_WRITABLE) && e.getValue().switchValue) {
                    system.setReadOnly(false);
                } else if (e.getKey().equals(OPTION_LIST_REPLICAS) && e.getValue().switchValue) {
                    system.listReplicas();
                } else if (e.getKey().equals(OPTION_LIST_SUITABLE_OSDS_FOR_A_REPLICA)
                        && e.getValue().switchValue) {
                    system.listSuitableOSDs();
                } else if (e.getKey().equals(OPTION_RSEL_POLICY_GET) && e.getValue().switchValue) {
                    system.getReplicaSelectionPolicy();
                } else if (e.getKey().equals(OPTION_RSEL_POLICY_SET) && (e.getValue().stringValue != null)) {
                    system.setReplicaSelectionPolicy(e.getValue().stringValue.trim());
                }
            }
        } catch (Exception e) {
            System.err.println("an error has occurred");
            e.printStackTrace();
        } finally {
            if (system != null)
                system.shutdown();
        }
    }

    public static void usage() {
        StringBuffer out = new StringBuffer();
        out.append("Usage: " + xtfs_repl.class.getSimpleName());
        out.append(" [options] <path>\n");
        out.append("options:\n");
        out.append("\t-" + OPTION_HELP + "/-" + OPTION_HELP_LONG + ": show usage info\n");
        out.append("\t-" + OPTION_SET_READ_ONLY + ": marks the file as read-only\n");
        out.append("\t-" + OPTION_SET_WRITABLE + ": marks the file as writable (normal file)\n");
        out.append("\t-" + OPTION_LIST_REPLICAS + ": lists all replicas of this file\n");
        out.append("\t-" + OPTION_LIST_SUITABLE_OSDS_FOR_A_REPLICA
                + ": lists all suitable OSDs for this file, which can be used for a new replica\n");
        out.append("\t-" + OPTION_RSEL_POLICY_GET + " : show the volume's current replica selection policy\n");
        out.append("\t-" + OPTION_RSEL_POLICY_SET + " { " + RSEL_POLICY_DEFAULT + " | " + RSEL_POLICY_FQDN + " | "
                + RSEL_POLICY_DCMAP + " | <policy id> } " + ": set the volume's replica selection policy\n");
        out.append("\t-" + OPTION_ADD_REPLICA
                + " <UUID_of_OSD1,UUID_of_OSD2 ...>: Adds a replica with the given OSDs. "
                + "The number of OSDs must be the same as in the file's striping policy. "
                + "Use comma as seperator.\n");
        out.append("\t-" + OPTION_ADD_AUTOMATIC_REPLICA + ": adds a replica and automatically selects the best OSDs (according to the volume's replica selection policy)\n");
        out.append("\t-" + OPTION_STRIPE_WIDTH + " <number of OSDs>"
                + ": specifies how many OSDs will be used for this replica (stripe-width)\n");
        out.append("\t-" + OPTION_REPLICATION_FLAG_TRANSFER_STRATEGY + " { " + TRANSFER_STRATEGY_RANDOM + " | "
                + TRANSFER_STRATEGY_SEQUENTIAL + " }: the replica to add will use the chosen strategy\n");
        out.append("\t-" + OPTION_REMOVE_REPLICA + " <UUID_of_head-OSD>"
                + ": removes the replica with the given head OSD\n");
        out.append("\t-" + OPTION_REMOVE_AUTOMATIC_REPLICA + ": removes a randomly selected replica\n");
        out.append("\t-" + OPTION_REPLICATION_FLAG_FULL_REPLICA
                        + ": if set, the replica will be a complete copy of the file; otherwise only requested data will be replicated\n");
        out.append("\n");
        out.append("\tTo use SSL it is necessary to also specify credentials:\n");
        out.append("\t-" + OPTION_SSL_CREDS_FILE
                + " <creds_file>: a PKCS#12 file containing user credentials\n");
        out.append("\t-" + OPTION_SSL_CREDS_PASSWORD
                + " <creds_passphrase>: a pass phrase to decrypt the the user credentials file\n");
        out.append("\t-" + OPTION_SSL_TRUSTED_CA_FILE
                + " <trusted_CAs>: a PKCS#12 file containing a set of certificates from trusted CAs\n");
        out.append("\t-" + OPTION_SSL_TRUSTED_CA_PASSWORD
                + " <trusted_passphrase>: a pass phrase to decrypt the trusted CAs file\n");

        System.out.println(out.toString());
    }
}