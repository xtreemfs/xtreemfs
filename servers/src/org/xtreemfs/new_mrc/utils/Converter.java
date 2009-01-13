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

package org.xtreemfs.new_mrc.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.metadata.ACLEntry;
import org.xtreemfs.new_mrc.metadata.StripingPolicy;
import org.xtreemfs.new_mrc.metadata.XAttr;
import org.xtreemfs.new_mrc.metadata.XLoc;
import org.xtreemfs.new_mrc.metadata.XLocList;

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
            aclMap.put(next.getEntity(), next.getRights());
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
            acl[i] = sMan.createACLEntry(fileId, entry.getKey(), ((Long) entry.getValue())
                    .shortValue());
        }
        
        Arrays.sort(acl, new Comparator<ACLEntry>() {
            public int compare(ACLEntry o1, ACLEntry o2) {
                return o1.getEntity().compareTo(o2.getEntity());
            }
        });
        
        return acl;
    }
    
    /**
     * Converts an <code>XLocationsList</code> object to a list containing
     * X-Locations data.
     * 
     * @param xLocList
     * @return
     */
    public static List<Object> xLocListToList(XLocList xLocList) {
        
        if (xLocList == null)
            return null;
        
        List<Object> replicaList = new LinkedList<Object>();
        List<Object> list = new LinkedList<Object>();
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            
            XLoc replica = xLocList.getReplica(i);
            
            List<Object> replicaAsList = new ArrayList<Object>(2);
            Map<String, Object> policyMap = stripingPolicyToMap(replica.getStripingPolicy());
            List<String> osdList = new ArrayList<String>(replica.getOSDCount());
            for (int j = 0; j < osdList.size(); j++)
                osdList.add(replica.getOSD(i));
            
            replicaAsList.add(policyMap);
            replicaAsList.add(osdList);
            
            replicaList.add(replicaAsList);
        }
        
        list.add(replicaList);
        list.add(xLocList.getVersion());
        
        return list;
    }
    
    /**
     * Converts a list containing X-Locations data to an
     * <code>XLocationsList</code> object.
     * 
     * @param xLocs
     * @return
     */
    public static XLocList listToXLocList(StorageManager sMan, List<Object> list) {
        
        if (list == null)
            return null;
        
        List<Object> xLocs = (List<Object>) list.get(0);
        
        XLoc[] xLocations = new XLoc[xLocs.size()];
        for (int i = 0; i < xLocs.size(); i++) {
            
            List<Object> replicaAsList = (List<Object>) xLocs.get(i);
            Map<String, Object> policyMap = (Map<String, Object>) replicaAsList.get(0);
            List<String> osdList = (List<String>) replicaAsList.get(1);
            
            xLocations[i] = sMan.createXLoc(mapToStripingPolicy(sMan, policyMap), osdList
                    .toArray(new String[osdList.size()]));
        }
        
        long version = (Long) list.get(1);
        
        return sMan.createXLocList(xLocations, (int) version);
    }
    
    /**
     * Converts a map containing striping policy information to a
     * <code>StripingPolicy</code> object.
     * 
     * @param policyMap
     * @return
     */
    public static StripingPolicy mapToStripingPolicy(StorageManager sMan,
        Map<String, Object> policyMap) {
        
        if (policyMap == null || policyMap.isEmpty())
            return null;
        
        StripingPolicy policy = sMan.createStripingPolicy((String) policyMap.get("policy"),
            ((Long) policyMap.get("stripe-size")).intValue(), ((Long) policyMap.get("width"))
                    .intValue());
        
        return policy;
    }
    
    /**
     * Converts a <code>StripingPolicy</code> object to a map containing
     * striping policy information.
     * 
     * @param policy
     * @return
     */
    public static Map<String, Object> stripingPolicyToMap(StripingPolicy policy) {
        
        if (policy == null)
            return null;
        
        Map<String, Object> policyMap = new HashMap<String, Object>();
        policyMap.put("policy", policy.getPattern());
        policyMap.put("stripe-size", policy.getStripeSize());
        policyMap.put("width", policy.getWidth());
        
        return policyMap;
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
            list.add(sMan.createXAttr(fileId, (String) attr.get("userId"),
                (String) attr.get("key"), (String) attr.get("value")));
        
        return list;
    }
    
}
