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

import org.xtreemfs.mrc.brain.storage.entities.ACLEntry;
import org.xtreemfs.mrc.brain.storage.entities.AbstractFileEntity;
import org.xtreemfs.mrc.brain.storage.entities.DirEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileAttributeEntity;
import org.xtreemfs.mrc.brain.storage.entities.FileEntity;
import org.xtreemfs.mrc.brain.storage.entities.StripingPolicy;
import org.xtreemfs.mrc.brain.storage.entities.XLocation;
import org.xtreemfs.mrc.brain.storage.entities.XLocationsList;
import org.xtreemfs.new_mrc.metadata.BufferBackedStripingPolicy;

/**
 * Contains static methods for converting Java objects to JSON-compliant data
 * structures and vice versa.
 * 
 * @author stender
 * 
 */
public class Converter {
    
    /**
     * Converts an <code>ACLEntry</code> array to a mapping: userID:String ->
     * rights:Long.
     * 
     * @param acl
     * @return
     */
    public static Map<String, Object> aclToMap(ACLEntry[] acl) {
        
        if (acl == null)
            return null;
        
        Map<String, Object> aclMap = new HashMap<String, Object>();
        for (ACLEntry entry : acl)
            aclMap.put(entry.getEntity(), entry.getRights());
        
        return aclMap;
    }
    
    /**
     * Converts a mapping: userID:String -> rights:Long to an
     * <code>ACLEntry</code> array sorted by userID.
     * 
     * @param aclMap
     * @return
     */
    public static ACLEntry[] mapToACL(Map<String, Object> aclMap) {
        
        if (aclMap == null)
            return null;
        
        ACLEntry[] acl = new ACLEntry[aclMap.size()];
        Iterator<String> keys = aclMap.keySet().iterator();
        for (int i = 0; i < acl.length; i++) {
            String userId = keys.next();
            acl[i] = new ACLEntry(userId, (Long) aclMap.get(userId));
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
    public static List<Object> xLocListToList(XLocationsList xLocList) {
        
        if (xLocList == null)
            return null;
        
        List<Object> replicaList = new LinkedList<Object>();
        List<Object> list = new LinkedList<Object>();
        for (XLocation replica : xLocList.getReplicas()) {
            
            List<Object> replicaAsList = new ArrayList<Object>(2);
            Map<String, Object> policyMap = stripingPolicyToMap(replica.getStripingPolicy());
            List<String> osdList = stringArrayToList(replica.getOsdList());
            
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
    public static XLocationsList listToXLocList(List<Object> list) {
        
        if (list == null)
            return null;
        
        List<Object> xLocs = (List<Object>) list.get(0);
        
        XLocation[] xLocations = new XLocation[xLocs.size()];
        for (int i = 0; i < xLocs.size(); i++) {
            
            List<Object> replicaAsList = (List<Object>) xLocs.get(i);
            Map<String, Object> policyMap = (Map<String, Object>) replicaAsList.get(0);
            List<String> osdList = (List<String>) replicaAsList.get(1);
            
            xLocations[i] = new XLocation(mapToStripingPolicy(policyMap), osdList
                    .toArray(new String[osdList.size()]));
        }
        
        long version = (Long) list.get(1);
        
        return new XLocationsList(xLocations, version);
    }
    
    /**
     * Converts a map containing striping policy information to a
     * <code>StripingPolicy</code> object.
     * 
     * @param policyMap
     * @return
     */
    public static StripingPolicy mapToStripingPolicy(Map<String, Object> policyMap) {
        
        if (policyMap == null || policyMap.isEmpty())
            return null;
        
        StripingPolicy policy = new StripingPolicy((String) policyMap.get("policy"),
            (Long) policyMap.get("stripe-size"), (Long) policyMap.get("width"));
        
        return policy;
    }
    
    /**
     * Converts a map containing striping policy information to a
     * <code>StripingPolicy</code> object.
     * 
     * @param policyMap
     * @return
     */
    public static BufferBackedStripingPolicy mapToBufferBackedStripingPolicy(
        Map<String, Object> policyMap) {
        
        if (policyMap == null || policyMap.isEmpty())
            return null;
        
        BufferBackedStripingPolicy policy = new BufferBackedStripingPolicy((String) policyMap
                .get("policy"), (Integer) policyMap.get("stripe-size"), (Integer) policyMap
                .get("width"));
        
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
        policyMap.put("policy", policy.getPolicy());
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
    
    // /**
    // * Converts an entire file tree to a list containing a hierarchically
    // * organized representation of all files in the tree.
    // *
    // * @param sMan
    // * @param source
    // * @return
    // * @throws BrainException
    // */
    // public static List<Object> fileTreeToList(StorageManager sMan,
    // AbstractFileEntity source) throws BrainException {
    //
    // try {
    //
    // List<FileAttributeEntity> attrs = sMan.getAllAttributes(source
    // .getId());
    //
    // Map<String, Object> file = new HashMap<String, Object>();
    // file.put("atime", source.getAtime());
    // file.put("ctime", source.getCtime());
    // file.put("mtime", source.getMtime());
    // file.put("ownerId", source.getUserId());
    // file.put("groupId", source.getGroupId());
    // file.put("isDirectory", source.isDirectory());
    // file.put("acl", Converter.aclToMap(source.getAcl()));
    // file.put("linkCount", source.getLinkCount());
    //
    // if (!source.isDirectory()) {
    // FileEntity tmp = (FileEntity) source;
    // file.put("size", tmp.getSize());
    // file.put("xLocList", Converter.xLocListToList(tmp
    // .getXLocationsList()));
    // }
    //
    // List<Map<String, Object>> attributes = new LinkedList<Map<String,
    // Object>>();
    // for (FileAttributeEntity attr : attrs) {
    // Map<String, Object> map = new HashMap<String, Object>();
    // map.put("key", attr.getKey());
    // map
    // .put(
    // "value",
    // attr.getValue() instanceof StripingPolicy ?
    // stripingPolicyToMap((StripingPolicy) attr
    // .getValue())
    // : attr.getValue());
    // map.put("type", attr.getType());
    // map.put("userId", attr.getUserId());
    // attributes.add(map);
    // }
    //
    // List<List<Object>> subElements = new LinkedList<List<Object>>();
    // for (AbstractFileEntity child :
    // sMan.getChildData(source.getId()).values())
    // subElements.add(fileTreeToList(sMan, child));
    //
    // List<Object> node = new LinkedList<Object>();
    // node.add(file);
    // node.add(attributes);
    // node.add(subElements);
    //
    // return node;
    //
    // } catch (Exception exc) {
    // throw new BrainException(exc);
    // }
    // }
    
    /**
     * Converts a list of <code>FileAttributeEntity</code>s to a list containing
     * maps storing file attribute information.
     * 
     * @param mappedData
     * @return
     */
    public static List<FileAttributeEntity> attrMapsToAttrList(List<Map<String, Object>> mappedData) {
        
        List<FileAttributeEntity> list = new LinkedList<FileAttributeEntity>();
        for (Map<String, Object> attr : mappedData)
            list.add(new FileAttributeEntity<Object>((String) attr.get("key"), attr.get("value"),
                (Long) attr.get("type"), 0, (String) attr.get("userId")));
        
        return list;
    }
    
    /**
     * Converts a map containing file or directory metadata to a file or
     * directory entity.
     * 
     * @param mappedData
     *            the mapped file or directory data
     * @return a corresponding object that can be stored by the MRC backend
     */
    public static AbstractFileEntity mapToFile(Map<String, Object> mappedData) {
        
        boolean isDirectory = (Boolean) mappedData.get("isDirectory");
        
        ACLEntry[] acl = Converter.mapToACL((Map<String, Object>) mappedData.get("acl"));
        
        if (isDirectory)
            return new DirEntity(0, (String) mappedData.get("ownerId"), (String) mappedData
                    .get("groupId"), (Long) mappedData.get("atime"),
                (Long) mappedData.get("ctime"), (Long) mappedData.get("mtime"), acl,
                (Long) mappedData.get("linkCount"));
        else {
            
            XLocationsList xLocList = Converter.listToXLocList((List<Object>) mappedData
                    .get("xLocList"));
            
            return new FileEntity(0, (String) mappedData.get("ownerId"), (String) mappedData
                    .get("groupId"), (Long) mappedData.get("atime"),
                (Long) mappedData.get("ctime"), (Long) mappedData.get("mtime"), (Long) mappedData
                        .get("size"), xLocList, acl, (Long) mappedData.get("linkCount"),
                (Long) mappedData.get("writeEpoch"), (Long) mappedData.get("truncEpoch"));
        }
    }
    
}
