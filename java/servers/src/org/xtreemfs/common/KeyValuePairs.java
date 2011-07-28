/*
 * Copyright (c) 2010 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;

/**
 * 
 * @author bjko
 */
public class KeyValuePairs {
    
    public static String getValue(List<KeyValuePair> list, String key) {
        for (KeyValuePair pair : list) {
            if (pair.getKey().equals(key))
                return pair.getValue();
        }
        return null;
    }
    
    public static void putValue(List<KeyValuePair> list, String key, String value) {
        Iterator<KeyValuePair> iter = list.iterator();
        while (iter.hasNext()) {
            KeyValuePair pair = iter.next();
            if (pair.getKey().equals(key))
                iter.remove();
        }
        list.add(KeyValuePair.newBuilder().setKey(key).setValue(value).build());
    }
    
    public static List<KeyValuePair> fromMap(Map<String, String> map) {
        List<KeyValuePair> list = new ArrayList(map.size());
        for (Entry<String, String> e : map.entrySet()) {
            list.add(KeyValuePair.newBuilder().setKey(e.getKey()).setValue(e.setValue(null)).build());
        }
        return list;
    }
    
    public static Map<String, String> toMap(List<KeyValuePair> list) {
        Map<String, String> map = new HashMap<String, String>();
        for (KeyValuePair kv : list)
            map.put(kv.getKey(), kv.getValue());
        return map;
    }
    
}
