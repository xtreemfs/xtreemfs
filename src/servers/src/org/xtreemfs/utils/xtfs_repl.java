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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Replica;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 * A tool to manage your Replicas. File can be marked as read-only, replicas can
 * be added, ... <br>
 * 06.04.2009
 */
public class xtfs_repl {
    
    public final static String  OPTION_HELP                               = "h";
    
    public final static String  OPTION_HELP_LONG                          = "-help";
    
    public final static String  OPTION_ADD_REPLICA                        = "a";
    
    public final static String  OPTION_ADD_AUTOMATIC_REPLICA              = "-add_auto";
    
    public final static String  OPTION_REMOVE_REPLICA                     = "r";
    
    public final static String  OPTION_SET_READ_ONLY                      = "-set_readonly";
    
    public final static String  OPTION_SET_WRITABLE                       = "-set_writable";
    
    public final static String  OPTION_LIST_REPLICAS                      = "l";
    
    public final static String  OPTION_LIST_SUITABLE_OSDS_FOR_A_REPLICA   = "o";
    
    public final static String  OPTION_STRIPE_WIDTH                       = "w";
    
    public final static String  OPTION_RSEL_POLICY_GET                    = "-rsp_get";
    
    public final static String  OPTION_RSEL_POLICY_SET                    = "-rsp_set";
    
    public final static String  OPTION_OSEL_POLICY_GET                    = "-osp_get";
    
    public final static String  OPTION_OSEL_POLICY_SET                    = "-osp_set";
    
    public final static String  OPTION_POLICY_ATTR_SET                    = "-pol_attr_set";
    
    public final static String  OPTION_POLICY_ATTRS_GET                   = "-pol_attrs_get";
    
    public final static String  OPTION_ON_CLOSE_REPL_FACTOR_SET           = "-ocr_factor_set";
    
    public final static String  OPTION_ON_CLOSE_REPL_FACTOR_GET           = "-ocr_factor_get";
    
    public final static String  OPTION_ON_CLOSE_REPL_FULL_SET             = "-ocr_full_set";
    
    public final static String  OPTION_ON_CLOSE_REPL_FULL_GET             = "-ocr_full_get";
    
    public final static String  OPTION_REPLICATION_FLAG_FULL_REPLICA      = "-full";
    
    public final static String  OPTION_REPLICATION_FLAG_TRANSFER_STRATEGY = "-strategy";
    
    public final static String  OPTION_CHECK_IF_COMPLETE                  = "-is_complete";
    
    public final static String  SEL_POLICY_DEFAULT                        = "default";
    
    public final static String  SEL_POLICY_UUID                           = "uuid";
    
    public final static String  SEL_POLICY_FQDN                           = "fqdn";
    
    public final static String  SEL_POLICY_DCMAP                          = "dcmap";
    
    public final static String  SEL_POLICY_RANDOM                         = "random";
    
    public final static String  SEL_POLICY_VIVALDI                        = "vivaldi";
    
    public final static String  TRANSFER_STRATEGY_RANDOM                  = "random";
    
    public final static String  TRANSFER_STRATEGY_SEQUENTIAL              = "sequential";
    
    public final static String  OPTION_SSL_CREDS_FILE                     = "c";
    
    public final static String  OPTION_SSL_CREDS_PASSWORD                 = "cpass";
    
    public final static String  OPTION_SSL_TRUSTED_CA_FILE                = "t";
    
    public final static String  OPTION_SSL_TRUSTED_CA_PASSWORD            = "tpass";
    
    public final static int     DEFAULT_REPLICATION_FLAGS                 = ReplicationFlags
                                                                                  .setPartialReplica(ReplicationFlags
                                                                                          .setRandomStrategy(0));
    
    private String              volName;
    
    private String              volPath;
    
    private String              localPath;
    
    private Client              client;
    
    private File                file;
    
    private RandomAccessFile    raFile;
    
    private UserCredentials     credentials;
    
    private InetSocketAddress[] dirAddrs;
    
    private SSLOptions          sslOptions;
    
    private UUIDResolver        resolver;
    
    private RPCNIOSocketClient  resolverClient;
    
    public static final Pattern IPV4_PATTERN                              = Pattern
                                                                                  .compile("b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)"
                                                                                      + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)b");
    
    public static final Pattern IPV6_PATTERN                              = Pattern
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

    public void setReplicaUpdatePolicy(String policyName) throws IOException {
        file.setxattr("xtreemfs.set_repl_update_policy", policyName);
    }
    
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
    public xtfs_repl(String localPath, InetSocketAddress dirAddress, String volName, String volPath,
        SSLOptions sslOptions) throws Exception {
        try {
            Logging.start(Logging.LEVEL_ERROR, Category.tool);
            
            this.sslOptions = sslOptions;
            this.dirAddrs = new InetSocketAddress[] { dirAddress };
            this.localPath = localPath;
            
            // this.volPath = volPath;
            this.volPath = volPath;
            this.volName = volName;
            
            // TODO: use REAL user credentials (this is a SECURITY HOLE)
            StringSet groupIDs = new StringSet();
            groupIDs.add("root");
            this.credentials = new UserCredentials("root", groupIDs, "");
            
            // client
            resolverClient = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000, Client.getExceptionParsers());
            resolverClient.start();
            resolverClient.waitForStartup();
            resolver = UUIDResolver.startNonSingelton(new DIRClient(resolverClient, dirAddress), 1000,
                10 * 10 * 1000);
            
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
    public void initialize(boolean fileRequired) throws Exception {
        
        client = new Client(dirAddrs, 10000, 30000, sslOptions);
        client.start();
        
        Volume volume = client.getVolume(volName, credentials);
        file = volume.getFile(volPath);
        
        if (fileRequired) {
            if (!file.isFile()) {
                System.err.println("'" + localPath + "' is not a file");
                System.exit(1);
            }
            this.raFile = file.open("r", 0);
            
        } else {
            if (!file.exists()) {
                System.err.println("'" + localPath + "' does not exist");
                System.exit(1);
            }
        }
    }
    
    /*
     * add replica
     */
    public void addReplica(List<ServiceUUID> osds, int replicationFlags, int stripeWidth) throws Exception {
        if (file.isReplicated()) {
            if (stripeWidth == 0) // not set => policy from replica 1
                stripeWidth = osds.size();
            
            StripingPolicy sp = new StripingPolicy(StripingPolicyType.STRIPING_POLICY_RAID0, raFile
                    .getCurrentReplicaStripeSize(), stripeWidth);
            
            String[] osdArray = new String[osds.size()];
            int i = 0;
            for (ServiceUUID osd : osds) {
                osdArray[i] = osd.toString();
                i++;
            }
            
            file.addReplica(osdArray.length, osdArray, replicationFlags);
            
            // start replication of full replicas
            /*if (ReplicationFlags.isFullReplica(replicationFlags))
                startReplicationOnOSDs(osds.get(0));*/

        } else
            System.err.println("File is not replicated.");
    }
    
    // automatic
    public void addReplicaAutomatically(int replicationFlags, int stripeWidth) throws Exception {
        if (file.isReplicated()) {
            if (stripeWidth == 0) // not set => policy from replica 1
                stripeWidth = raFile.getCurrentReplicaStripeingWidth();
            
            String[] suitableOSDs = file.getSuitableOSDs(stripeWidth);
            if (suitableOSDs.length < stripeWidth) {
                System.err.println("could not create replica: not enough suitable OSDs available");
                System.exit(1);
            }
            
            String[] osdList = new String[stripeWidth];
            System.arraycopy(suitableOSDs, 0, osdList, 0, stripeWidth);
            
            List<ServiceUUID> uuidList = new LinkedList<ServiceUUID>();
            for (String osd : osdList)
                uuidList.add(new ServiceUUID(osd, resolver));
            
            addReplica(uuidList, replicationFlags, stripeWidth);
            
        } else
            System.err.println("File is not replicated.");
    }
    
    /**
     * contacts the OSDs so they begin to replicate the file
     * 
     * @param addedOSD
     * @throws IOException
     */
    private void startReplicationOnOSDs(ServiceUUID addedOSD) throws IOException {
        String uuid = addedOSD.toString();
        raFile.forceReplica(uuid);
        raFile.triggerInitialReplication();
    }
    
    public void getOSDSelectionPolicy() {
        try {
            String v = file.getxattr("xtreemfs.osel_policy");
            
            short[] policies = Converter.stringToShortArray(v);
            if (policies.length == 2
                && policies[0] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue()
                && policies[1] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_RANDOM.intValue()) {
                System.out.println("OSD selection policy: default");
            } else if (policies.length == 2
                && policies[0] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue()
                && policies[1] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_GROUP_FQDN.intValue()) {
                System.out.println("OSD selection policy: FQDN");
            } else if (policies.length == 2
                && policies[0] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue()
                && policies[1] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_UUID.intValue()) {
                System.out.println("OSD selection policy: UUID");
            } else if (policies.length == 2
                && policies[0] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue()
                && policies[1] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_GROUP_DCMAP.intValue()) {
                System.out.println("OSD selection policy: DCMap");
            } else if (policies.length == 2
                && policies[0] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue()
                && policies[1] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_VIVALDI.intValue()) {
                System.out.println("OSD selection policy: vivaldi");
            } else {
                System.out.println("OSD selection policy: custom (" + v + ")");
            }
        } catch (Exception ex) {
            System.err.println("could not retrieve OSD selection policy");
            ex.printStackTrace();
        }
    }
    
    public void setOSDSelectionPolicy(String rsp) {
        try {
            
            String pol = "";
            if (rsp.equalsIgnoreCase(SEL_POLICY_DEFAULT)) {
                pol = String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue())
                    + ","
                    + String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_RANDOM.intValue());
            } else if (rsp.equalsIgnoreCase(SEL_POLICY_FQDN)) {
                pol = String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue())
                    + "," + String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_GROUP_FQDN.intValue());
            } else if (rsp.equalsIgnoreCase(SEL_POLICY_UUID)) {
                pol = String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue())
                    + ","
                    + String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_UUID.intValue());
            } else if (rsp.equalsIgnoreCase(SEL_POLICY_DCMAP)) {
                pol = String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue())
                    + ","
                    + String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_GROUP_DCMAP.intValue());
            } else if (rsp.equalsIgnoreCase(SEL_POLICY_VIVALDI)) {
                pol = String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT.intValue())
                    + ","
                    + String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_VIVALDI.intValue());
            } else {
                pol = rsp;
            }
            
            file.setxattr("xtreemfs.osel_policy", pol);
            System.out.println("OSD selection policy changed");
        } catch (Exception ex) {
            System.err.println("could not set OSD selection policy");
            ex.printStackTrace();
        }
    }
    
    public void getReplicaSelectionPolicy() {
        try {
            String v = file.getxattr("xtreemfs.rsel_policy");
            
            short[] policies = Converter.stringToShortArray(v);
            if (policies.length == 0) {
                System.out.println("replica selection policy: default");
            } else if (policies.length == 1
                && policies[0] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_RANDOM.intValue()) {
                System.out.println("replica selection policy: random");
            } else if (policies.length == 1
                && policies[0] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_FQDN.intValue()) {
                System.out.println("replica selection policy: FQDN");
            } else if (policies.length == 1
                && policies[0] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_DCMAP.intValue()) {
                System.out.println("replica selection policy: DCMap");
                // } else if(policies.length == 1
                // && policies[0] ==
                // OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_VIVALDI.intValue()){
                // System.out.println("replica selection policy: Vivaldi");
            } else if (policies.length == 1
                && policies[0] == OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_VIVALDI.intValue()) {
                System.out.println("replica selection policy: vivaldi");
            } else {
                System.out.println("replica selection policy: custom (" + v + ")");
            }
        } catch (Exception ex) {
            System.err.println("could not retrieve replica selection policy");
            ex.printStackTrace();
        }
    }
    
    public void setReplicaSelectionPolicy(String rsp) {
        try {
            
            String pol = "";
            if (rsp.equalsIgnoreCase(SEL_POLICY_DEFAULT)) {
                // empty pol
            } else if (rsp.equalsIgnoreCase(SEL_POLICY_RANDOM)) {
                pol = String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_RANDOM.intValue());
            } else if (rsp.equalsIgnoreCase(SEL_POLICY_FQDN)) {
                pol = String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_FQDN.intValue());
            } else if (rsp.equalsIgnoreCase(SEL_POLICY_DCMAP)) {
                pol = String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_DCMAP.intValue());
            } else if (rsp.equalsIgnoreCase(SEL_POLICY_VIVALDI)) {
                pol = String.valueOf(OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_VIVALDI.intValue());
            } else {
                pol = rsp;
            }
            
            file.setxattr("xtreemfs.rsel_policy", pol);
            System.out.println("replica selection policy changed");
        } catch (Exception ex) {
            System.err.println("could not set replica selection policy");
            ex.printStackTrace();
        }
    }
    
    public void getOnCloseReplFactor() {
        try {
            String v = file.getxattr("xtreemfs.repl_factor");
            System.out.println("on-close replication factor: " + v);
        } catch (Exception ex) {
            System.err.println("could not retrieve on-close replication factor");
            ex.printStackTrace();
        }
    }
    
    public void setOnCloseReplFactor(String f) {
        try {
            
            int factor = Integer.parseInt(f.trim());
            if (factor <= 0)
                factor = 1;
            
            file.setxattr("xtreemfs.repl_factor", String.valueOf(factor));
            System.out.println("on-close replication factor changed");
        } catch (Exception ex) {
            System.err.println("could not set on-close replication factor");
            ex.printStackTrace();
        }
    }
    
    public void getPolicyAttrs() {
        try {
            System.out.println("policy attributes:");
            String[] keys = file.listXAttrs();
            for (String key : keys)
                if (key.startsWith("xtreemfs.policies."))
                    System.out.println(key + " = " + file.getxattr(key));
            
        } catch (Exception ex) {
            System.err.println("could not retrieve policy attributes");
            ex.printStackTrace();
        }
    }
    
    public void setPolicyAttr(String kvPair) {
        try {
            
            StringTokenizer st = new StringTokenizer(kvPair, "=;,");
            if (st.countTokens() != 2) {
                System.err.println("invalid policy attribute: " + kvPair + ", key-value pair required");
                return;
            }
            
            String key = st.nextToken();
            String value = st.nextToken();
            
            file.setxattr("xtreemfs.policies." + key, value);
            System.out.println("policy attribute '" + key + "' changed");
        } catch (Exception ex) {
            System.err.println("could not change policy attribute");
            ex.printStackTrace();
        }
    }
    
    public void getOnCloseFull() {
        try {
            System.out.print("create full replicas on close: ");
            String v = file.getxattr("xtreemfs.repl_full");
            System.out.println(v);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void setOnCloseFull(boolean full) {
        try {
            file.setxattr("xtreemfs.repl_full", String.valueOf(full));
            System.out.println("create full replicas on close was set to " + full);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /*
     * remove replica
     */
    public void removeReplica(ServiceUUID osd) throws Exception {
        if (file.isReplicated()) {
            file.getReplica(raFile.getReplicaNumber(osd.toString())).removeReplica(true);
        } else
            System.err.println("File is not replicated.");
    }
    
    /*
     * other commands
     */
    public void setReadOnly(boolean mode) throws Exception {
        file.setReadOnly(mode);
    }

    public void isReadOnly() throws Exception {
        if (file.isReadOnlyReplicated()) {
            System.out.println("read-only: on");
        } else {
            System.out.println("read-only: off");
        }
    }
    
    public void listReplicas() throws UnknownUUIDException, IOException {
        printListOfReplicas(file.getReplicas());
    }
    
    public void listSuitableOSDs() throws Exception {
        String[] osds = file.getSuitableOSDs(1000);
        printListOfOSDs(osds);
    }
    
    /*
     * outputs
     */
    private void printListOfReplicas(Replica[] replicas) throws IOException, UnknownUUIDException {
        StringBuffer out = new StringBuffer();
        
        // read-only?
        if (file.isReadOnly())
            out.append("File is read-only.\n");
        else
            out.append("File is writable. No replicas possible.\n");
        
        int replicaNumber = 0;
        for (Replica r : replicas) {
            // head line
            out.append("REPLICA " + (replicaNumber++) + ":\n");
            out.append("\t striping policy: " + r.getStripingPolicy().toString() + "\n");
            out.append("\t stripe size: " + OutputUtils.formatBytes(r.getStripeSize()) + "\n");
            out.append("\t stripe width: " + r.getStripeWidth() + " (OSDs)\n");
            
            if (file.isReadOnly()) {
                out.append("\t Replication Flags:\n");
                out.append("\t\t Complete: " + r.isCompleteReplica() + "\n");
                out.append("\t\t Replica Type: " + (r.isFullReplica() ? "full" : "partial") + "\n");
                String transferStrategy = "unknown";
                if (r.isRandomStrategy())
                    transferStrategy = "random";
                else if (r.isSequentialStrategy())
                    transferStrategy = "sequential";
                else if (r.isSequentialPrefetchingStrategy())
                    transferStrategy = "sequential prefetching";
                else if (r.isRarestFirstStrategy())
                    transferStrategy = "rarest first";
                out.append("\t\t Transfer-Strategy: " + transferStrategy + "\n");
            }
            
            out.append("\t OSDs:\n");
            
            // OSDs of this replica
            for (int osdNumber = 0; osdNumber < r.getStripeWidth(); osdNumber++) {
                ServiceUUID osd = new ServiceUUID(r.getOSDUuid(osdNumber), resolver);
                if (osdNumber == 0)
                    out.append("\t\t [Head-OSD]\t");
                else
                    out.append("\t\t [OSD " + (osdNumber + 1) + "]\t");
                out.append("UUID: " + osd.toString() + ", URL: " + osd.getAddress().toString() + "\n");
            }
        }
        System.out.print(out.toString());
    }
    
    private void printListOfOSDs(String[] osds) throws UnknownUUIDException {
        StringBuffer out = new StringBuffer();
        if (osds.length > 0) {
            int number = 1;
            for (String osd : osds) {
                out.append("[" + number++ + "] ");
                out.append("UUID: " + osd.toString() + ", URL: "
                    + new ServiceUUID(osd, resolver).getAddress().toString() + "\n");
            }
        } else
            out.append("no suitable OSDs available\n");
        System.out.print(out.toString());
    }
    
    private void checkIfComplete(String headOSD) throws UnknownUUIDException, ONCRPCException, IOException,
        InterruptedException, ClassCastException, ClassNotFoundException {
        System.out.println(file.getReplica(raFile.getReplicaNumber(headOSD)).isCompleteReplica());
    }
    
    // }
    
    /**
     * 
     */
    private void shutdown() {
        try {
            if (raFile != null)
                raFile.close();
            client.stop();
            
            UUIDResolver.shutdown(resolver);
            resolverClient.shutdown();
            
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

    public UUIDResolver getResolver() {
        return this.resolver;
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
        options.put(OPTION_OSEL_POLICY_GET, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_OSEL_POLICY_SET, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_ON_CLOSE_REPL_FACTOR_GET, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_ON_CLOSE_REPL_FACTOR_SET, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_ON_CLOSE_REPL_FULL_GET, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_ON_CLOSE_REPL_FULL_SET, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_POLICY_ATTRS_GET, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put(OPTION_POLICY_ATTR_SET, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(OPTION_CHECK_IF_COMPLETE, new CliOption(CliOption.OPTIONTYPE.STRING));
        
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
            final String localPath = utils.expandPath(arguments.get(0));
            final String url = utils.getxattr(localPath, "xtreemfs.url");
            
            if (url == null) {
                System.err.println("could not retrieve XtreemFS URL for file '" + localPath + "'");
                System.exit(1);
            }
            
            final int i0 = url.indexOf("://") + 2;
            final int i1 = url.indexOf(':', i0);
            final int i2 = url.indexOf('/', i1);
            final int i3 = url.indexOf('/', i2 + 1);
            
            final String dirURL = url.substring(i0 + 1, i1);
            final int dirPort = Integer.parseInt(url.substring(i1 + 1, i2));
            final String volName = url.substring(i2 + 1, i3 == -1 ? url.length() : i3);
            final String volPath = i3 == -1 ? "" : url.substring(i3);
            final InetSocketAddress dirAddress = new InetSocketAddress(dirURL, dirPort);
            
            // create SSL options (if set)
            SSLOptions sslOptions = null;
            if ((options.get(OPTION_SSL_CREDS_FILE).stringValue != null)
                && (options.get(OPTION_SSL_CREDS_PASSWORD).stringValue != null)
                && (options.get(OPTION_SSL_TRUSTED_CA_FILE).stringValue != null)
                && (options.get(OPTION_SSL_TRUSTED_CA_PASSWORD).stringValue != null)) { // SSL
                // set
                final boolean gridSSL = url.startsWith(Constants.ONCRPCG_SCHEME);
                sslOptions = new SSLOptions(new FileInputStream(
                    options.get(OPTION_SSL_CREDS_FILE).stringValue),
                    options.get(OPTION_SSL_CREDS_PASSWORD).stringValue, new FileInputStream(options
                            .get(OPTION_SSL_TRUSTED_CA_FILE).stringValue), options
                            .get(OPTION_SSL_TRUSTED_CA_PASSWORD).stringValue, false, gridSSL);
            }
            
            system = new xtfs_repl(localPath, dirAddress, volName, volPath, sslOptions);
            if ((options.get(OPTION_RSEL_POLICY_GET).switchValue)
                || (options.get(OPTION_RSEL_POLICY_SET).stringValue != null)
                || options.get(OPTION_OSEL_POLICY_GET).switchValue
                || options.get(OPTION_OSEL_POLICY_SET).stringValue != null
                || options.get(OPTION_ON_CLOSE_REPL_FACTOR_GET).switchValue
                || options.get(OPTION_ON_CLOSE_REPL_FACTOR_SET).stringValue != null
                || options.get(OPTION_POLICY_ATTRS_GET).switchValue
                || options.get(OPTION_POLICY_ATTR_SET).stringValue != null
                || options.get(OPTION_ON_CLOSE_REPL_FULL_GET).switchValue
                || options.get(OPTION_ON_CLOSE_REPL_FULL_SET).stringValue != null) {
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
                            osds.add(new ServiceUUID(st.nextToken(), system.resolver));
                        system.addReplica(osds, replicationFlags, stripeWidth);
                    } else
                        usage();
                    
                } else if (e.getKey().equals(OPTION_ADD_AUTOMATIC_REPLICA) && e.getValue().switchValue) {
                    
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
                        ServiceUUID osd = new ServiceUUID(headOSD, system.resolver);
                        system.removeReplica(osd);
                    } else
                        usage();
                    
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
                } else if (e.getKey().equals(OPTION_OSEL_POLICY_GET) && e.getValue().switchValue) {
                    system.getOSDSelectionPolicy();
                } else if (e.getKey().equals(OPTION_OSEL_POLICY_SET) && (e.getValue().stringValue != null)) {
                    system.setOSDSelectionPolicy(e.getValue().stringValue.trim());
                } else if (e.getKey().equals(OPTION_ON_CLOSE_REPL_FACTOR_GET) && e.getValue().switchValue) {
                    system.getOnCloseReplFactor();
                } else if (e.getKey().equals(OPTION_ON_CLOSE_REPL_FACTOR_SET)
                    && (e.getValue().stringValue != null)) {
                    system.setOnCloseReplFactor(e.getValue().stringValue.trim());
                } else if (e.getKey().equals(OPTION_POLICY_ATTRS_GET) && e.getValue().switchValue) {
                    system.getPolicyAttrs();
                } else if (e.getKey().equals(OPTION_POLICY_ATTR_SET) && (e.getValue().stringValue != null)) {
                    system.setPolicyAttr(e.getValue().stringValue.trim());
                } else if (e.getKey().equals(OPTION_ON_CLOSE_REPL_FULL_GET) && e.getValue().switchValue) {
                    system.getOnCloseFull();
                } else if (e.getKey().equals(OPTION_ON_CLOSE_REPL_FULL_SET)
                    && e.getValue().stringValue != null) {
                    system.setOnCloseFull(Boolean.valueOf(e.getValue().stringValue.trim()));
                } else if (e.getKey().equals(OPTION_CHECK_IF_COMPLETE) && e.getValue().stringValue != null) {
                    system.checkIfComplete(e.getValue().stringValue.trim());
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
        out.append("\n");
        
        out.append("\tgeneral:\n");
        out.append("\t-" + OPTION_HELP + "/-" + OPTION_HELP_LONG + ": show usage info\n");
        out.append("\t-" + OPTION_SET_READ_ONLY + ": marks the file as read-only\n");
        out.append("\t-" + OPTION_SET_WRITABLE + ": marks the file as writable (normal file)\n");
        out.append("\t-" + OPTION_LIST_REPLICAS + ": lists all replicas of this file\n");
        out.append("\t-" + OPTION_LIST_SUITABLE_OSDS_FOR_A_REPLICA
            + ": lists all suitable OSDs for this file, which can be used for a new replica\n");
        out.append("\t-" + OPTION_CHECK_IF_COMPLETE
            + " <UUID of head OSD>): checks if a replica is complete, i.e. has no more missing objects\n");
        out.append("\n");
        
        out.append("\tpolicy management:\n");
        out.append("\t-" + OPTION_OSEL_POLICY_SET + " { " + SEL_POLICY_DEFAULT + " | " + SEL_POLICY_FQDN
            + " | " + SEL_POLICY_UUID + " | " + SEL_POLICY_DCMAP + " | <policy_ID, [policy_ID, ...]> }"
            + ": sets a list of successively applied OSD selection policies\n");
        out.append("\t-" + OPTION_OSEL_POLICY_GET + ": prints the list of OSD selection policies\n");
        out.append("\t-" + OPTION_RSEL_POLICY_SET + " { " + SEL_POLICY_DEFAULT + " | " + SEL_POLICY_RANDOM
            + " | " + SEL_POLICY_FQDN + " | " + SEL_POLICY_DCMAP + " | " + SEL_POLICY_VIVALDI
            + " | <policy_ID, [policy_ID, ...]> }"
            + ": sets a list of successively applied replica selection policies\n");
        out.append("\t-" + OPTION_RSEL_POLICY_GET + ": prints the list of replica selection policies\n");
        out.append("\t-" + OPTION_POLICY_ATTR_SET + " <name=value>: sets a policy-specific attribute\n");
        out.append("\t-" + OPTION_POLICY_ATTRS_GET + ": prints all policy-specific attributes\n");
        out.append("\n");
        
        out.append("\ton-close replication:\n");
        out
                .append("\t-"
                    + OPTION_ON_CLOSE_REPL_FACTOR_SET
                    + " <number>"
                    + ": adjusts the number of replicas assigned to a file when the file is closed after a write. A value of 1 indicates that no replicas will be added and the file remains writable.\n");
        out.append("\t-" + OPTION_ON_CLOSE_REPL_FACTOR_GET
            + ": prints the current on-close replication factor\n");
        out.append("\t-" + OPTION_ON_CLOSE_REPL_FULL_SET + " { true | false }"
            + ": sets whether full replicas will be created when on-close replication is in effect\n");
        out
                .append("\t-"
                    + OPTION_ON_CLOSE_REPL_FULL_GET
                    + ": prints whether full replicas will be created by means of the on-close replication mechanism\n");
        out.append("\n");
        
        out.append("\tadding and removing replicas:\n");
        out.append("\t-" + OPTION_ADD_REPLICA
            + " <UUID_of_OSD1,UUID_of_OSD2 ...>: Adds a replica with the given OSDs. "
            + "The number of OSDs must be the same as in the file's striping policy. "
            + "Use comma as seperator.\n");
        out.append("\t-" + OPTION_REMOVE_REPLICA + " <UUID_of_head-OSD>"
            + ": removes the replica with the given head OSD\n");
        out
                .append("\t-"
                    + OPTION_ADD_AUTOMATIC_REPLICA
                    + ": adds a replica and automatically selects the best OSDs (according to the volume's osd selection policy)\n");
        out.append("\t-" + OPTION_STRIPE_WIDTH + " <number of OSDs>"
            + ": specifies how many OSDs will be used for this replica (stripe-width)\n");
        out
                .append("\t-"
                    + OPTION_REPLICATION_FLAG_FULL_REPLICA
                    + ": if set, the replica will be a complete copy of the file; otherwise only requested data will be replicated\n");
        out.append("\t-" + OPTION_REPLICATION_FLAG_TRANSFER_STRATEGY + " { " + TRANSFER_STRATEGY_RANDOM
            + " | " + TRANSFER_STRATEGY_SEQUENTIAL + " }: the replica to add will use the chosen strategy\n");
        out.append("\n");
        
        out.append("\tSSL settings:\n");
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
