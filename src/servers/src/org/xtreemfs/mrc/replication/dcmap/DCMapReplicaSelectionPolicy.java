/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.mrc.replication.dcmap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.regex.Pattern;

import org.xtreemfs.common.LRUCache;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.mrc.replication.FQDNReplicaSelectionPolicy;
import org.xtreemfs.mrc.replication.ReplicaSelectionPolicy;

/**
 *
 * @author bjko
 */
public class DCMapReplicaSelectionPolicy implements ReplicaSelectionPolicy {

    public static final short POLICY_ID = (short) 4;

    public static final String CONFIG_FILE_PATH = "/etc/xos/xtreemfs/datacentermap";

    private int[][] distMap;

    private Inet4AddressMatcher[][] matchers;

    private LRUCache<Inet4Address, Integer> matchingDCcache;

    public DCMapReplicaSelectionPolicy() {
        try {
            File f = new File(CONFIG_FILE_PATH);
            Properties p = new Properties();
            p.load(new FileInputStream(f));
            readConfig(p);
            int maxCacheSize = Integer.valueOf(p.getProperty("max_cache_size", "1000"));
            matchingDCcache = new LRUCache<Inet4Address, Integer>(maxCacheSize);
        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "cannot load %s, DCMap replica selection policy will not work", CONFIG_FILE_PATH);
        }

    }

    public DCMapReplicaSelectionPolicy(Properties p) {
        readConfig(p);
        int maxCacheSize = Integer.valueOf(p.getProperty("max_cache_size", "1000"));
        matchingDCcache = new LRUCache<Inet4Address, Integer>(maxCacheSize);
    }

    private void readConfig(Properties p) {
        String tmp = p.getProperty("datacenters");
        if (tmp == null) {
            throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName() + ": a list of datacenters must be specified in the configuration");
        }
        final String[] dcs = tmp.split(",");
        if (dcs.length == 0) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "no datacenters specified");
            return;
        }
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9_]");
        for (int i = 0; i < dcs.length; i++) {
            if (!dcs[i].matches("[a-zA-Z0-9_]+")) {
                throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName() + ": datacenter name '" + dcs[i] + "' is invalid");
            }
        }

        distMap = new int[dcs.length][dcs.length];
        matchers = new Inet4AddressMatcher[dcs.length][];
        for (int i = 0; i < dcs.length; i++) {
            for (int j = i + 1; j < dcs.length; j++) {
                String distStr = p.getProperty("distance." + dcs[i] + "-" + dcs[j]);
                if (distStr == null) {
                    distStr = p.getProperty("distance." + dcs[j] + "-" + dcs[i]);
                }
                if (distStr == null) {
                    throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName() + ": distance between datacenter '" + dcs[i] + "' and '" + dcs[j] + "' is not specified");
                }
                try {
                    distMap[i][j] = distMap[j][i] = Integer.valueOf(distStr);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName() + ": distance '" + distStr + "' between datacenter '" + dcs[i] + "' and '" + dcs[j] + "' is not a valid integer");
                }
            }
            String dcMatch = p.getProperty(dcs[i] + ".addresses");
            if (dcMatch != null) {
                String entries[] = dcMatch.split(",");
                matchers[i] = new Inet4AddressMatcher[entries.length];
                for (int e = 0; e < entries.length; e++) {
                    final String entry = entries[e];
                    if (entry.contains("/")) {
                        //network match
                        try {
                            String ipAddr = entry.substring(0, entry.indexOf("/"));
                            String prefix = entry.substring(entry.indexOf("/") + 1);
                            Inet4Address ia = (Inet4Address) InetAddress.getByName(ipAddr);
                            matchers[i][e] = new Inet4AddressMatcher(ia, Integer.valueOf(prefix));
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName() + ": netmask in '" + entry + "' for datacenter '" + dcs[i] + "' is not a valid integer");
                        } catch (Exception ex) {
                            throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName() + ": address '" + entry + "' for datacenter '" + dcs[i] + "' is not a valid IPv4 address");
                        }
                    } else {
                        //IP match
                        try {
                            Inet4Address ia = (Inet4Address) InetAddress.getByName(entry);
                            matchers[i][e] = new Inet4AddressMatcher(ia);
                        } catch (Exception ex) {
                            throw new IllegalArgumentException("Cannot initialize " + this.getClass().getSimpleName() + ": address '" + entry + "' for datacenter '" + dcs[i] + "' is not a valid IPv4 address");
                        }
                    }
                }

            } else {
                //allow empty datacenters
                Logging.logMessage(Logging.LEVEL_WARN, this, "datacenter '" + dcs[i] + "' has no entries!");
            }
        }

    }

    public int getMatchingDC(Inet4Address addr) {
        Integer cached = matchingDCcache.get(addr);
        if (cached == null) {
            for (int i = 0; i < matchers.length; i++) {
                for (int j = 0; j < matchers[i].length; j++) {
                    if (matchers[i][j].matches(addr)) {
                        matchingDCcache.put(addr, i);
                        return i;
                    }
                }
            }
            matchingDCcache.put(addr, -1);
            return -1;
        } else {
            return cached;
        }
    }

    public int getDistance(Inet4Address addr1, Inet4Address addr2) {
        final int dc1 = getMatchingDC(addr1);
        final int dc2 = getMatchingDC(addr2);

        if ((dc1 != -1) && (dc2 != -1)) {
            return distMap[dc1][dc2];
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public ReplicaSet getSortedReplicaList(ReplicaSet replicas, InetAddress clientAddr) {
        if (matchers == null) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "%s was not initialized properly but is used by some volume. Please check your DCMap configuration!", this.getClass().getSimpleName());
            return replicas;
        }
        if (clientAddr instanceof Inet4Address) {
            Inet4Address client = (Inet4Address) clientAddr;
            PriorityQueue<FQDNReplicaSelectionPolicy.SortedReplica> list = new PriorityQueue<FQDNReplicaSelectionPolicy.SortedReplica>();

            for (Replica r : replicas) {
                try {
                    ServiceUUID uuid = new ServiceUUID(r.getOsd_uuids().get(0));
                    Inet4Address osdAddr = (Inet4Address) uuid.getAddress().getAddress();

                    int match = -getDistance(client, osdAddr);

                    list.add(new FQDNReplicaSelectionPolicy.SortedReplica(r, match));
                } catch (UnknownUUIDException ex) {
                } catch (ClassCastException ex) {
                    return replicas;
                }
            }

            ReplicaSet sortedSet = new ReplicaSet();

            for (int i = 0; i < replicas.size(); i++) {
                sortedSet.add(list.poll().replica);
            }

            return sortedSet;
        } else {
            return replicas;
        }
    }

    @Override
    public StringSet getSortedOSDList(StringSet osdIDs, InetAddress clientAddr) {
        
        final Inet4Address cAddr = (Inet4Address) clientAddr;
        
        Collections.sort(osdIDs, new Comparator<String>() {
            public int compare(String o1, String o2) {
                try {
                    ServiceUUID uuid1 = new ServiceUUID(o1);
                    ServiceUUID uuid2 = new ServiceUUID(o2);
                    Inet4Address osdAddr1 = (Inet4Address) uuid1.getAddress().getAddress();
                    Inet4Address osdAddr2 = (Inet4Address) uuid2.getAddress().getAddress();
                    
                    return getDistance(osdAddr1, cAddr) - getDistance(osdAddr2, cAddr);
                    
                } catch (UnknownUUIDException e) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, "cannot compare UUIDs");
                    Logging.logMessage(Logging.LEVEL_WARN, this, OutputUtils.stackTraceToString(e));
                    return 0;
                }
            }
        });
        
        return osdIDs;
    }
}
