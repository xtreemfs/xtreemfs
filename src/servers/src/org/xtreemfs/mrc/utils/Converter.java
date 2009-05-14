/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicyType;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.BufferBackedStripingPolicy;
import org.xtreemfs.mrc.metadata.BufferBackedXLoc;
import org.xtreemfs.mrc.metadata.BufferBackedXLocList;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;

/**
 * Contains static methods for converting Java objects to JSON-compliant data
 * structures and vice versa.
 * 
 * @author stender
 * 
 */
public class Converter {
    
    /**
     * Converts an <code>ACLEntry</code> iterator to a mapping: userID:String ->
     * rights:Long.
     * 
     * @param acl
     * @return
     */
    public static Map<String, Object> aclToMap(Iterator<ACLEntry> acl) {
        
        if (acl == null)
            return null;
        
        Map<String, Object> aclMap = new HashMap<String, Object>();
        while (acl.hasNext()) {
            ACLEntry next = acl.next();
            aclMap.put(next.getEntity(), (long) next.getRights());
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
     * @param xLocList
     *            the XLocList
     * @return the string representation of the XLocList
     */
    public static String xLocListToString(XLocList xLocList) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            sb.append("[<");
            XLoc repl = xLocList.getReplica(i);
            StripingPolicy sp = repl.getStripingPolicy();
            
            sb.append(stripingPolicyToString(sp)).append(">, (");
            for (int j = 0; j < repl.getOSDCount(); j++)
                sb.append(repl.getOSD(j)).append(j == repl.getOSDCount() - 1 ? "" : ", ");
            sb.append(")]").append(i == xLocList.getReplicaCount() - 1 ? "" : ", ");
        }
        sb.append(", ").append(xLocList.getVersion()).append(", ").append(xLocList.getReplUpdatePolicy())
                .append("]");
        
        return sb.toString();
    }

    public static String xLocListToJSON(XLocList xLocList) throws JSONException, UnknownUUIDException {

        Map<String,Object> list = new HashMap();
        list.put("update-policy",xLocList.getReplUpdatePolicy());
        list.put("version",Long.valueOf(xLocList.getVersion()));
        List<Map<String,Object>> replicas = new ArrayList(xLocList.getReplicaCount());
        Iterator<XLoc> iter = xLocList.iterator();
        while (iter.hasNext()) {
            XLoc l = iter.next();
            Map<String,Object> replica = new HashMap();
            replica.put("striping-policy", getStripingPolicyAsJSON(l.getStripingPolicy()));
            replica.put("replication-flags", new Long(0));
            List<Map<String,String>> osds = new ArrayList(l.getOSDCount());
            for (int i = 0; i < l.getOSDCount(); i++) {
                Map<String,String> osd = new HashMap();
                final ServiceUUID uuid = new ServiceUUID(l.getOSD(i));
                osd.put("uuid", uuid.toString());
                osd.put("address",uuid.getAddress().getHostName());
                osds.add(osd);
            }
            replica.put("osds", osds);
            replicas.add(replica);
        }
        list.put("replicas",replicas);

        return JSONParser.writeJSON(list);
    }
    
//    public static void main(String[] args) {
//        BufferBackedStripingPolicy sp = new BufferBackedStripingPolicy("RAID0", 256, 2);
//        BufferBackedXLoc repl1 = new BufferBackedXLoc(sp, new String[] { "osd1", "osd2" }, 0);
//        BufferBackedXLoc repl2 = new BufferBackedXLoc(sp, new String[] { "osd4" }, 0);
//        XLocList xLocList = new BufferBackedXLocList(new BufferBackedXLoc[] { repl1, repl2 }, "policy", 3);
//        
//        System.out.println(xLocListToString(xLocList));
//    }
    
    /**
     * Converts an XLocSet to an XLocList
     * 
     * @param sMan
     *            the storage manager
     * @param xLocSet
     *            the XLocSet
     * @return the XLocList
     */
    public static XLocList xLocSetToXLocList(StorageManager sMan, XLocSet xLocSet) {
        
        XLoc[] replicas = new XLoc[xLocSet.getReplicas().size()];
        for (int i = 0; i < xLocSet.getReplicas().size(); i++) {
            
            Replica repl = xLocSet.getReplicas().get(i);
            org.xtreemfs.interfaces.StripingPolicy sp = repl.getStriping_policy();
            
            replicas[i] = sMan.createXLoc(sMan.createStripingPolicy(sp.getType().toString(), sp
                    .getStripe_size(), sp.getWidth()), repl.getOsd_uuids().toArray(
                new String[repl.getOsd_uuids().size()]), repl.getReplication_flags());
        }
        
        return sMan.createXLocList(replicas, xLocSet.getRepUpdatePolicy(), xLocSet.getVersion());
    }
    
    /**
     * Converts an XLocList to an XLocSet
     * 
     * @param xLocList
     *            the XLocList
     * @return the xLocSet
     */
    public static XLocSet xLocListToXLocSet(XLocList xLocList) {
        
        ReplicaSet replicas = new ReplicaSet();
        
        if (xLocList == null)
            return new XLocSet();
        
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            
            XLoc xRepl = xLocList.getReplica(i);
            StripingPolicy xSP = xRepl.getStripingPolicy();
            
            StringSet osds = new StringSet();
            for (int j = 0; j < xRepl.getOSDCount(); j++)
                osds.add(xRepl.getOSD(j));
            
            org.xtreemfs.interfaces.StripingPolicy sp = new org.xtreemfs.interfaces.StripingPolicy(
                StripingPolicyType.valueOf(xSP.getPattern()), xSP.getStripeSize(), xSP.getWidth());
            
            Replica repl = new Replica(sp, 0, osds); // TODO: replication flags
            replicas.add(repl);
        }

        XLocSet xLocSet = new XLocSet();
        xLocSet.setReplicas(replicas);
        xLocSet.setRepUpdatePolicy(xLocList.getReplUpdatePolicy());
        xLocSet.setVersion(xLocList.getVersion());
        
        return xLocSet;
    }
    
    /**
     * Converts a string containing striping policy information to a
     * <code>StripingPolicy</code> object.
     * 
     * @param sMan
     *            the storage manager
     * @param spString
     *            the striping policy string
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
     * @param sMan
     *            the storage manager
     * @param spString
     *            the striping policy string
     * @return the striping policy
     */
    public static org.xtreemfs.interfaces.StripingPolicy jsonStringToStripingPolicy(String spString)
        throws JSONException {
        
        Map<String, Object> spMap = (Map<String, Object>) JSONParser.parseJSON(new JSONString(spString));
        
        if (spMap == null || spMap.isEmpty())
            return null;
        
        String pattern = (String) spMap.get("pattern");
        long size = (Long) spMap.get("size");
        long width = (Long) spMap.get("width");
        
        return new org.xtreemfs.interfaces.StripingPolicy(StripingPolicyType.valueOf(pattern), (int)size, (int)width);
    }
    
    /**
     * Converts a striping policy object to a string.
     * 
     * @param sp
     *            the striping policy object
     * @return a string containing the striping policy information
     */
    public static String stripingPolicyToString(StripingPolicy sp) {
        return sp.getPattern() + ", " + sp.getStripeSize() + ", " + sp.getWidth();
    }
    
    /**
     * Converts a striping policy object to a string.
     * 
     * @param sp
     *            the striping policy object
     * @return a string containing the striping policy information
     */
    public static String stripingPolicyToString(org.xtreemfs.interfaces.StripingPolicy sp) {
        return sp.getType().toString() + ", " + sp.getStripe_size() + ", " + sp.getWidth();
    }
    
    public static org.xtreemfs.interfaces.StripingPolicy stripingPolicyToStripingPolicy(StripingPolicy sp) {
        return new org.xtreemfs.interfaces.StripingPolicy(StripingPolicyType.valueOf(sp.getPattern()), sp
                .getStripeSize(), sp.getWidth());
    }
    
    public static String stripingPolicyToJSONString(StripingPolicy sp)
        throws JSONException {
        return JSONParser.writeJSON(getStripingPolicyAsJSON(sp));
    }

    static Replica replicaFromJSON(String value) throws JSONException {
        Map<String,Object> jsonObj = (Map<String, Object>) JSONParser.parseJSON(new JSONString(value));
        long rf = (Long)jsonObj.get("replication-flags");
        Map<String,Object> jsonSP = (Map<String, Object>) jsonObj.get("striping-policy");
        final String spName = (String) jsonSP.get("pattern");
        StripingPolicyType spType = StripingPolicyType.STRIPING_POLICY_RAID0;
        final long width = (Long) jsonSP.get("width");
        final long size = (Long) jsonSP.get("size");
        org.xtreemfs.interfaces.StripingPolicy sp = new org.xtreemfs.interfaces.StripingPolicy(spType, (int)size, (int)width);
        List<String> osds = (List<String>) jsonObj.get("osds");
        StringSet osdUuids = new StringSet();
        for (String osd : osds)
            osdUuids.add(osd);
        return new Replica(sp, (int)rf, osdUuids);
    }

    private static Map<String,Object> getStripingPolicyAsJSON(StripingPolicy sp) {
        Map<String, Object> spMap = new HashMap<String, Object>();
        spMap.put("pattern", sp.getPattern());
        spMap.put("size", sp.getStripeSize());
        spMap.put("width", sp.getWidth());
        return spMap;
    }
    
    /**
     * Converts a String array to a list of Strings.
     * 
     * @param array
     * @return
     */
    public static List<String> stringArrayToList(String[] array) {
        
        if (array == null)
            return null;
        
        List<String> list = new ArrayList<String>(array.length);
        
        for (String s : array)
            list.add(s);
        
        return list;
    }
    
    /**
     * Converts a list of <code>FileAttributeEntity</code>s to a list containing
     * maps storing file attribute information.
     * 
     * @param mappedData
     * @return
     */
    public static List<XAttr> attrMapsToAttrList(StorageManager sMan, long fileId,
        List<Map<String, Object>> mappedData) {
        
        List<XAttr> list = new LinkedList<XAttr>();
        for (Map<String, Object> attr : mappedData)
            list.add(sMan.createXAttr(fileId, (String) attr.get("userId"), (String) attr.get("key"),
                (String) attr.get("value")));
        
        return list;
    }
    
}
