/*
 * Copyright (c) 2009-2011 by Jan Stender, Juan Gonzalez de Benito,
 *               Zuse Institute Berlin, Barcelona Supercomputing Center
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * 
 * @author Juan González (BSC)
 */
public class SortVivaldiPolicy implements OSDSelectionPolicy {
    
    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_VIVALDI
                                                .getNumber();
    
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs, InetAddress clientIP,
        VivaldiCoordinates clientCoords, XLocList currentXLoc, int numOSDs) {
        
        if (allOSDs == null)
            return null;
        
        // Calculate the distances from the client to all the OSDs
        Hashtable<String, Double> distances = new Hashtable<String, Double>();
        
        for (Service oneOSD : allOSDs.getServicesList()) {
            
            ServiceDataMap sdm = oneOSD.getData();
            String strCoords = org.xtreemfs.common.KeyValuePairs.getValue(sdm.getDataList(),
                "vivaldi_coordinates");
            
            if (strCoords != null) {
                
                VivaldiCoordinates osdCoords = VivaldiNode.stringToCoordinates(strCoords);
                if (osdCoords != null) {
                    
                    double currentDistance = VivaldiNode.calculateDistance(clientCoords, osdCoords);
                    
                    distances.put(oneOSD.getUuid(), currentDistance);
                }
            }
        }
        
        // Create a new ServiceSet and add the sorted services to it
        List<Service> retSet = new LinkedList<Service>();
        for (Service oneOSD : allOSDs.getServicesList()) {
            
            Double oneOSDDistance = distances.get(oneOSD.getUuid());
            if (oneOSDDistance != null) { // Does the DS contain the info for
                // this service?
                
                boolean inserted = false;
                int i = 0;
                
                while (!inserted) {
                    
                    if (i >= retSet.size()) {
                        
                        retSet.add(oneOSD);
                        inserted = true;
                        
                    } else {
                        
                        Double iDistance = distances.get(retSet.get(i).getUuid());
                        
                        if ((iDistance == null) || // The OSDs without
                            // coordinates must be left
                            // at the end of the list
                            (oneOSDDistance.doubleValue() < iDistance.doubleValue())) {
                            
                            retSet.add(i, oneOSD);
                            inserted = true;
                            
                        } else {
                            i++;
                        }
                    }
                }
            } else {
                // If the OSD cannot be sorted because the DS does not provide
                // all its information, we append it to the end of the list
                retSet.add(oneOSD);
            }
        }
        
        return ServiceSet.newBuilder().addAllServices(retSet);
    }
    
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {
        // It's not possible to calculate the most appropiate OSD without
        // knowing the client's coordinates
        return allOSDs;
    }
    
    public void setAttribute(String key, String value) {
        // No attribute defined yet
    }
    
}
