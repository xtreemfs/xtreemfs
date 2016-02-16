/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.mrc.ac.FileAccessPolicy;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.osdselection.OSDStatusManager;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;

/**
 * Contains static methods for converting Java objects to JSON-compliant data
 * structures and vice versa.
 *
 * @author stender
 */
public class Converter {

    /**
     * Converts an <code>ACLEntry</code> iterator to a mapping: userID:String ->
     * rights:Long.
     *
     * @param acl
     * @return
     */
    public static Map<String, Object> aclToMap(Iterator<ACLEntry> acl, FileAccessPolicy policy) {

        if (acl == null)
            return null;

        Map<String, Object> aclMap = new HashMap<String, Object>();
        while (acl.hasNext()) {
            ACLEntry next = acl.next();
            aclMap.put(next.getEntity(), policy.translatePermissions(next.getRights()));
        }

        return aclMap;
    }

    /**
     * Converts a mapping: userID:String -> rights:Long to an
     * <code>ACLEntry</code> array sorted by userID.
     *
     * @param aclMap
     * @return
     */
    public static ACLEntry[] mapToACL(StorageManager sMan, long fileId, Map<String, Object> aclMap) {

        if (aclMap == null)
            return null;

        ACLEntry[] acl = new ACLEntry[aclMap.size()];
        Iterator<Entry<String, Object>> entries = aclMap.entrySet().iterator();
        for (int i = 0; i < acl.length; i++) {
            assert (entries.hasNext());
            Entry<String, Object> entry = entries.next();
            acl[i] = sMan.createACLEntry(fileId, entry.getKey(), ((Long) entry.getValue()).shortValue());
        }

        Arrays.sort(acl, new Comparator<ACLEntry>() {
            public int compare(ACLEntry o1, ACLEntry o2) {
                return o1.getEntity().compareTo(o2.getEntity());
            }
        });

        return acl;
    }

    /**
     * Converts an XLocList to a String
     *
     * @param xLocList the XLocList
     * @return the string representation of the XLocList
     */
    public static String xLocListToString(XLocList xLocList) {

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            sb.append("[<");
            XLoc replica = xLocList.getReplica(i);
            StripingPolicy sp = replica.getStripingPolicy();

            sb.append(stripingPolicyToString(sp)).append(">, (");
            for (int j = 0; j < replica.getOSDCount(); j++)
                sb.append(replica.getOSD(j)).append(j == replica.getOSDCount() - 1 ? "" : ", ");
            sb.append(")]").append(i == xLocList.getReplicaCount() - 1 ? "" : ", ");
        }
        sb.append(", ").append(xLocList.getVersion()).append(", ").append(xLocList.getReplUpdatePolicy())
                .append("]");

        return sb.toString();
    }

    public static String xLocListToJSON(XLocList xLocList, OSDStatusManager osdMan) throws JSONException,
            UnknownUUIDException {

        Map<String, Object> list = new HashMap();
        list.put("update-policy", xLocList.getReplUpdatePolicy());
        list.put("version", Long.valueOf(xLocList.getVersion()));
        List<Map<String, Object>> replicas = new ArrayList(xLocList.getReplicaCount());
        Iterator<XLoc> iter = xLocList.iterator();
        while (iter.hasNext()) {
            XLoc l = iter.next();
            Map<String, Object> replica = new HashMap();
            replica.put("striping-policy", getStripingPolicyAsJSON(l.getStripingPolicy()));
            replica.put("replication-flags", l.getReplicationFlags());
            List<Map<String, String>> osds = new ArrayList(l.getOSDCount());
            for (int i = 0; i < l.getOSDCount(); i++) {
                Map<String, String> osd = new HashMap();
                final ServiceUUID uuid = new ServiceUUID(l.getOSD(i));
                final Service osdData = osdMan.getOSDService(uuid.toString());
                final String coords = osdData == null ? "" : KeyValuePairs.getValue(osdData.getData()
                        .getDataList(), "vivaldi_coordinates");
                osd.put("uuid", uuid.toString());
                osd.put("vivaldi_coordinates", coords);
                osd.put("address", uuid.getAddressString());
                osds.add(osd);
            }
            replica.put("osds", osds);
            replicas.add(replica);
        }
        list.put("replicas", replicas);

        return JSONParser.writeJSON(list);
    }

    /**
     * Converts an XLocList to an XLocSet
     *
     * @param xLocList the XLocList
     * @return the xLocSet
     */
    public static XLocSet.Builder xLocListToXLocSet(XLocList xLocList) {

        if (xLocList == null)
            return null;

        XLocSet.Builder xLocSetBuilder = XLocSet.newBuilder().setReplicaUpdatePolicy(
                xLocList.getReplUpdatePolicy()).setVersion(xLocList.getVersion()).setReadOnlyFileSize(0);

        for (int i = 0; i < xLocList.getReplicaCount(); i++) {

            XLoc xRepl = xLocList.getReplica(i);
            StripingPolicy xSP = xRepl.getStripingPolicy();

            org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy.Builder sp = org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy
                    .newBuilder().setType(StripingPolicyType.valueOf(xSP.getPattern())).setStripeSize(
                            xSP.getStripeSize()).setWidth(xSP.getWidth());

            Replica.Builder replBuilder = Replica.newBuilder().setReplicationFlags(
                    xRepl.getReplicationFlags()).setStripingPolicy(sp);
            for (int j = 0; j < xRepl.getOSDCount(); j++)
                replBuilder.addOsdUuids(xRepl.getOSD(j));

            xLocSetBuilder.addReplicas(replBuilder);
        }

        return xLocSetBuilder;
    }

    /**
     * Converts a string containing striping policy information to a
     * <code>StripingPolicy</code> object.
     *
     * @param sMan     the storage manager
     * @param spString the striping policy string
     * @return the striping policy
     */
    public static StripingPolicy stringToStripingPolicy(StorageManager sMan, String spString) {

        StringTokenizer st = new StringTokenizer(spString, " ,\t");
        String policy = st.nextToken();
        if (policy.equals("RAID0"))
            policy = StripingPolicyType.STRIPING_POLICY_RAID0.toString();

        int size = Integer.parseInt(st.nextToken());
        int width = Integer.parseInt(st.nextToken());

        return sMan.createStripingPolicy(policy, size, width);
    }

    /**
     * Converts a string containing striping policy information to a
     * <code>StripingPolicy</code> object.
     *
     * @param spString the striping policy string
     * @return the striping policy
     */
    public static org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy jsonStringToStripingPolicy(
            String spString) throws JSONException {

        Map<String, Object> spMap = (Map<String, Object>) JSONParser.parseJSON(new JSONString(spString));

        if (spMap == null || spMap.isEmpty())
            return null;

        String pattern = (String) spMap.get("pattern");
        long size = (Long) spMap.get("size");
        long width = (Long) spMap.get("width");

        return org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy.newBuilder().setType(
                StripingPolicyType.valueOf(pattern)).setStripeSize((int) size).setWidth((int) width).build();
    }

    /**
     * Converts a striping policy object to a string.
     *
     * @param sp the striping policy object
     * @return a string containing the striping policy information
     */
    public static String stripingPolicyToString(StripingPolicy sp) {
        return sp.getPattern() + ", " + sp.getStripeSize() + ", " + sp.getWidth();
    }

    /**
     * Converts a striping policy object to a string.
     *
     * @param sp the striping policy object
     * @return a string containing the striping policy information
     */
    public static String stripingPolicyToString(
            org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy sp) {
        return sp.getType().toString() + ", " + sp.getStripeSize() + ", " + sp.getWidth();
    }

    public static org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy.Builder stripingPolicyToStripingPolicy(
            StripingPolicy sp) {
        return org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy.newBuilder().setType(
                StripingPolicyType.valueOf(sp.getPattern())).setStripeSize(sp.getStripeSize()).setWidth(
                sp.getWidth());
    }

    public static String stripingPolicyToJSONString(StripingPolicy sp) throws JSONException {
        return JSONParser.writeJSON(getStripingPolicyAsJSON(sp));
    }

    private static Map<String, Object> getStripingPolicyAsJSON(StripingPolicy sp) {
        Map<String, Object> spMap = new HashMap<String, Object>();
        spMap.put("pattern", sp.getPattern());
        spMap.put("size", sp.getStripeSize());
        spMap.put("width", sp.getWidth());
        return spMap;
    }

    public static short[] stringToShortArray(String shortList) {

        StringTokenizer st = new StringTokenizer(shortList, " \t,;");
        short[] result = new short[st.countTokens()];

        for (int i = 0; st.hasMoreTokens(); i++)
            result[i] = Short.parseShort(st.nextToken());

        return result;
    }

    public static String shortArrayToString(short[] shorts) {

        String result = "";
        for (int i = 0; i < shorts.length; i++) {
            result += shorts[i];
            if (i < shorts.length - 1)
                result += ",";
        }

        return result;
    }

    /**
     * Converts a replication policy object to a string.
     *
     * @param rp the replication policy object
     * @return a string containing the replication policy information
     */
    public static String replicationPolicyToString(ReplicationPolicy rp) {
        return rp.getName() + ", " + rp.getFactor() + ", " + rp.getFlags();
    }

    /**
     * Converts a string containing replication policy information to a
     * <code>ReplicationPolicy</code> object.
     *
     * @param rpString the replication policy string
     * @return the replication policy
     */
    public static ReplicationPolicy stringToReplicationPolicy(String rpString) {

        StringTokenizer st = new StringTokenizer(rpString, " ,\t");

        final String policy = rpString.startsWith(",") ? "" : st.nextToken();
        final int numRepls = Integer.parseInt(st.nextToken());
        final int flags = Integer.parseInt(st.nextToken());

        return new ReplicationPolicy() {

            @Override
            public String getName() {
                return policy;
            }

            @Override
            public int getFactor() {
                return numRepls;
            }

            @Override
            public int getFlags() {
                return flags;
            }

        };
    }

    /**
     * Converts a string containing replication policy information to a
     * <code>ReplicationPolicy</code> object.
     *
     * @param rpString the replication policy string
     * @return the replication policy
     */
    public static ReplicationPolicy jsonStringToReplicationPolicy(String rpString) throws JSONException {

        Map<String, Object> rpMap = (Map<String, Object>) JSONParser.parseJSON(new JSONString(rpString));

        if (rpMap == null || rpMap.isEmpty())
            return null;

        final String name = (String) rpMap.get("update-policy");
        if (name == null)
            return null;

        Long factor = (Long) rpMap.get("replication-factor");
        final int replicationFactor = factor == null ? 1 : factor.intValue();

        Long flags = (Long) rpMap.get("replication-flags");
        final int replicationFlags = flags == null ? 0 : flags.intValue();

        return new ReplicationPolicy() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getFactor() {
                return replicationFactor;
            }

            @Override
            public int getFlags() {
                return replicationFlags;
            }

        };
    }

    public static String replicationPolicyToJSONString(ReplicationPolicy rp) throws JSONException {
        return JSONParser.writeJSON(getReplicationPolicyAsJSON(rp));
    }

    private static Map<String, Object> getReplicationPolicyAsJSON(ReplicationPolicy rp) {
        Map<String, Object> rpMap = new HashMap<String, Object>();
        rpMap.put("update-policy", rp.getName());
        rpMap.put("replication-factor", rp.getFactor());
        rpMap.put("replication-flags", rp.getFlags());
        return rpMap;
    }

}
