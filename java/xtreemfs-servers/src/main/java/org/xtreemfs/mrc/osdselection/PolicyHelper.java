/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;

/**
 * Class with static helper methods for policies.
 * 
 * @author stender
 * 
 */
public class PolicyHelper {
    
    /**
     * Removes all OSDs from the given serivce set that are already included in
     * the given XLoc list.
     * 
     * @param allOSDs
     *            the set of OSDs
     * @param xLocList
     *            the XLoc list containing all OSDs to remove
     */
    public static ServiceSet.Builder removeUsedOSDs(ServiceSet.Builder allOSDs, XLocList xLocList) {
        
        if (xLocList == null)
            return allOSDs;
        
        Set<Service> newOSDs = new HashSet<Service>(allOSDs.getServicesList());
        
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            XLoc currentRepl = xLocList.getReplica(i);
            for (int j = 0; j < currentRepl.getOSDCount(); j++)
                for (Service osd : allOSDs.getServicesList())
                    if (currentRepl.getOSD(j).equals(osd.getUuid())) {
                        newOSDs.remove(osd);
                    }
        }
        
        return ServiceSet.newBuilder().addAllServices(newOSDs);
    }
    
    public static ServiceSet.Builder sortServiceSet(ServiceSet.Builder set, Comparator<Service> comp) {
        
        List<Service> immutableList = set.getServicesList();
        List<Service> list = new ArrayList<Service>(immutableList);
        Collections.sort(list, comp);
        
        return ServiceSet.newBuilder().addAllServices(list);
    }
    
    public static ServiceSet.Builder shuffleServiceSet(ServiceSet.Builder set) {
        
        List<Service> immutableList = set.getServicesList();
        List<Service> list = new ArrayList<Service>(immutableList);
        Collections.shuffle(list);
        
        return ServiceSet.newBuilder().addAllServices(list);
    }

    public static ServiceSet.Builder reverseServiceSet(ServiceSet.Builder set) {

        List<Service> immutableList = set.getServicesList();
        List<Service> list = new ArrayList<Service>(immutableList);
        Collections.reverse(list);

        return ServiceSet.newBuilder().addAllServices(list);
    }
}
