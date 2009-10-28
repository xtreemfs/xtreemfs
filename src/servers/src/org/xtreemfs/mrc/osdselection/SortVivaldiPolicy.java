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
 * AUTHORS: John Doe (organisation)
 */
package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.Hashtable;

import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;

import org.xtreemfs.mrc.metadata.XLocList;

import org.xtreemfs.osd.vivaldi.VivaldiNode;


/**
 *
 * 28/10/2009
 * @author jgonz
 */
public class SortVivaldiPolicy implements OSDSelectionPolicy {
    
    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_VIVALDI.intValue();
    
    public ServiceSet getOSDs(ServiceSet allOSDs, InetAddress clientIP, VivaldiCoordinates clientCoords,
            XLocList currentXLoc, int numOSDs){
        
        //TOFIX:What's up with numOSDs?
        //int numOSDs = allOSDs.size();
        
        //Calculate the distances from the client to all the OSDs
        Hashtable<String,Double> distances = new Hashtable<String,Double>();
        
        for(Service oneOSD:allOSDs){
            ServiceDataMap sdm = oneOSD.getData();
            String strCoords = sdm.get("vivaldi_coordinates");
            System.out.println("OSD->"+oneOSD.getUuid());
            if(strCoords!=null){
                VivaldiCoordinates osdCoords = VivaldiNode.stringToCoordinates(strCoords);
                System.out.println("strCoords->"+strCoords+" coords:"+osdCoords);
                if(osdCoords!=null){
                    
                    double currentDistance = VivaldiNode.calculateDistance(clientCoords, osdCoords);
                    
                    distances.put(oneOSD.getUuid(), currentDistance);
                    System.out.println("dist:"+currentDistance);
                }
            }
        }
        
        return allOSDs;
    }
    public ServiceSet getOSDs(ServiceSet allOSDs){
        //It's not possible to calculate the most appropiate OSD without knowing the client's coordinates
        return allOSDs;
    }
    public void setAttribute(String key, String value){
        //No attribute defined yet
    }
    

}
