package org.xtreemfs.osd.replication;

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
 * AUTHORS: Christian Lorenz (ZIB)
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;

/**
 * This class provides the basic functionality needed by the different transfer strategies. One
 * TransferStrategy manages a whole file (all objects of this file). warning: this class is NOT thread-safe
 * 
 * 09.09.2008
 * 
 * @author clorenz
 */
public abstract class TransferStrategy {
    /**
     * Encapsulates the "returned"/chosen values.
     * 
     * 12.02.2009
     */
    public class NextRequest {
        public ServiceUUID osd;
        public long objectNo;
        /**
         * if true, the OSD must return a list of all local available objects
         */
        public boolean requestObjectList;
    }

    protected String fileID;
    protected XLocations xLoc; // does not contain current replica

    /**
     * contains the chosen values for the next replication request
     */
    protected NextRequest next;

    /**
     * contains all objects which must be replicated (e.g. background-replication)
     */
    protected ArrayList<Long> requiredObjects; // maybe additionally current

    /**
     * contains all objects which must be replicated first (e.g. client-request)
     */
    protected ArrayList<Long> preferredObjects; // requested objects

    /**
     * checks if the OSD is available (e.g. network interrupt)
     */
    protected final ServiceAvailability osdAvailability;

    /**
     * contains a list of possible OSDs for each object used to notice which OSDs were already requested
     * key: objectNo
     */
    protected HashMap<Long, List<ServiceUUID>> availableOSDsForObject;

    /**
     * contains a list of local available objects for each OSD
     * key: ServiceUUID of a OSD
     */
//    protected HashMap<ServiceUUID, List<Long>> availableObjectsOnOSD;

    /**
     * @param rqDetails
     */
    protected TransferStrategy(String fileID, XLocations xLoc, long filesize,
            ServiceAvailability osdAvailability) {
        super();
        this.xLoc = xLoc;
        this.fileID = fileID;
        this.requiredObjects = new ArrayList<Long>();
        this.preferredObjects = new ArrayList<Long>();
        this.osdAvailability = osdAvailability;
        this.availableOSDsForObject = new HashMap<Long, List<ServiceUUID>>();
//        this.availableObjectsOnOSD = new HashMap<ServiceUUID, List<Long>>();
        this.next = null;
    }

    /**
     * chooses the next object, which will be replicated
     */
    public void selectNext() {
        next = null;
    }

    /**
     * 
     */
    public void selectNextOSD(long objectID) {
        next = null;
    }

    /**
     * Returns the "result" from selectNext().
     * 
     * @return null, if selectNext() has not been executed before or no object to fetch exists
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public NextRequest getNext() {
        if (next != null) {
            // remove object from lists, so it can't be chosen twice
            removeObjectFromList(next.objectNo);
            // remove used OSD for this object, because the OSD will not be used a second time
            availableOSDsForObject.get(next.objectNo).remove(next.osd);
        }
        return next;
    }

    /**
     * add an object which must be replicated
     * 
     * @param objectNo
     * @param preferred
     * @return
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public boolean addObject(long objectNo, boolean preferred) {
        // add existing OSDs containing the object
        if (!availableOSDsForObject.containsKey(objectNo))
            availableOSDsForObject.put(objectNo, xLoc.getOSDsForObject(objectNo, xLoc.getLocalReplica()));

        if (preferred) {
            // object must not contained in both lists
            if (requiredObjects.contains(objectNo))
                requiredObjects.remove(objectNo);
            // no multiple entries
            if (!preferredObjects.contains(objectNo))
                return preferredObjects.add(objectNo);
        } else {
            // object must not contained in both lists
            if (preferredObjects.contains(objectNo))
                preferredObjects.remove(objectNo);
            // no multiple entries
            if (!requiredObjects.contains(objectNo))
                return requiredObjects.add(objectNo);
        }
        return false;
    }

    /**
     * removes the objectNo only from the list of replicating objects
     * 
     * @param objectNo
     * @return
     * @see java.util.ArrayList#remove(java.lang.Object)
     */
    protected boolean removeObjectFromList(long objectNo) {
        boolean contained = preferredObjects.remove(objectNo);
        contained = contained || requiredObjects.remove(objectNo);
        return contained;
    }

    /**
     * remove an object which need not be replicated anymore
     * 
     * @param objectNo
     * @return
     * @see java.util.ArrayList#remove(java.lang.Object)
     */
    public boolean removeObject(long objectNo) {
        boolean contained = (null != availableOSDsForObject.remove(objectNo));
        contained = contained || removeObjectFromList(objectNo);
        return contained;
    }

    /**
     * Returns how much objects will be replicated.
     * 
     * @return
     */
    public int getObjectsCount() {
        return preferredObjects.size() + requiredObjects.size();
    }

    /**
     * checks if the object is a hole
     * 
     * @param objectNo
     * @return true: it is a hole
     * false: maybe it is a hole
     */
    public boolean isHole(long objectNo){
        if(availableOSDsForObject.containsKey(objectNo))
            return (availableOSDsForObject.get(objectNo).size() == 0);
        else
            return false;
    }
}
