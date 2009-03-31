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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;
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
        sb.append(", ").append(xLocList.getVersion()).append("]");
        
        return sb.toString();
    }
    
    public static void main(String[] args) {
        BufferBackedStripingPolicy sp = new BufferBackedStripingPolicy("RAID0", 256, 2);
        BufferBackedXLoc repl1 = new BufferBackedXLoc(sp, new String[] { "osd1", "osd2" });
        BufferBackedXLoc repl2 = new BufferBackedXLoc(sp, new String[] { "osd4" });
        XLocList xLocList = new BufferBackedXLocList(new BufferBackedXLoc[] { repl1, repl2 }, 3);
        
        System.out.println(xLocListToString(xLocList));
    }
    
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
            
            replicas[i] = sMan.createXLoc(sMan.createStripingPolicy(intToPolicyName(sp.getPolicy()), sp
                    .getStripe_size(), sp.getWidth()), repl.getOsd_uuids().toArray(
                new String[repl.getOsd_uuids().size()]));
        }
        
        return sMan.createXLocList(replicas, xLocSet.getVersion());
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
                policyNameToInt(xSP.getPattern()), xSP.getStripeSize(), xSP.getWidth());
            
            Replica repl = new Replica(sp, 0, osds); // TODO: replication flags
            replicas.add(repl);
        }
        
        XLocSet xLocSet = new XLocSet();
        xLocSet.setReplicas(replicas);
        xLocSet.setRepUpdatePolicy(""); // TODO
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
    public static org.xtreemfs.interfaces.StripingPolicy stringToStripingPolicy(String spString) {
        
        StringTokenizer st = new StringTokenizer(spString, " ,\t");
        String policy = st.nextToken();
        int size = Integer.parseInt(st.nextToken());
        int width = Integer.parseInt(st.nextToken());
        
        return new org.xtreemfs.interfaces.StripingPolicy(Converter.policyNameToInt(policy), size, width);
    }
    
    /**
     * Converts an integer to a striping policy name.
     * 
     * @param policy
     *            the integer for the policy
     * @return the name of the policy
     */
    public static String intToPolicyName(int policy) {
        switch (policy) {
        case Constants.STRIPING_POLICY_DEFAULT:
        case Constants.STRIPING_POLICY_RAID0:
            return "RAID0";
        default:
            return "";
        }
    }
    
    public static int policyNameToInt(String policyName) {
        if (policyName.equals("RAID0"))
            return Constants.STRIPING_POLICY_RAID0;
        return Constants.STRIPING_POLICY_DEFAULT;
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
        return Converter.intToPolicyName(sp.getPolicy()) + ", " + sp.getStripe_size() + ", " + sp.getWidth();
    }
    
    public static org.xtreemfs.interfaces.StripingPolicy stripingPolicyToStripingPolicy(StripingPolicy sp) {
        return new org.xtreemfs.interfaces.StripingPolicy(policyNameToInt(sp.getPattern()), sp
                .getStripeSize(), sp.getWidth());
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
