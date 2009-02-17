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

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.osd.RequestDetails;

/**
 * This class provides the basic functionality needed by the different transfer
 * strategies.
 * One TransferStrategy manages a whole file (all objects of this file).
 * warning: this class is NOT thread-safe
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
     * @author user
     */
    public class NextRequest {
        public ServiceUUID osd;
        public long objectID;
        /**
         * if true, the OSD must return a list of all local available objects
         */
        public boolean requestObjectList;
    }

    /**
     * Encapsulates the most important infos.
     *
     * 12.02.2009
     * @author user
     */
    protected class ReplicationDetails {
	String fileId;
	Capability capability;
	Location currentReplica;
	Locations otherReplicas; // do not contain current replica

	ReplicationDetails(String fileId, Capability capability,
		Locations locationList, Location currentReplica) {
	    this.fileId = fileId;
	    this.capability = capability;
	    this.currentReplica = currentReplica;
	    
	    // get other Replicas without current
	    ArrayList<Location> locList = new ArrayList<Location>();
	    for(Location loc : locationList){
		if(loc != currentReplica)
		    locList.add(loc);
	    }
	    this.otherReplicas = new Locations(locList);
	}
    }

    /**
     * contains the chosen values for the next replication request
     */
    protected NextRequest next;

    /**
     * contains necessary information
     */
    protected ReplicationDetails details;

    /**
     * contains all objects which must be replicated (e.g. background-replication)
     */
    protected ArrayList<Long> requiredObjects; // maybe additionally current

    /**
     * contains all objects which must be replicated first (e.g. client-request)
     */
    protected ArrayList<Long> preferredObjects; // requested objects

    /**
     * contains a list of local available objects for each OSD
     */
    protected HashMap<ServiceUUID, List<Long>> availableObjectsOnOSD;
    /**
     * contains a list of possible OSDs for each object
     * used to notice which OSDs were already requested
     */
    protected HashMap<Long, List<ServiceUUID>> availableOSDsForObject;
    
    /**
     * known filesize up to now
     */
    protected long knownFilesize;

    /**
     * @param rqDetails
     */
    protected TransferStrategy(RequestDetails rqDetails) {
	super();
	this.details = new ReplicationDetails(rqDetails.getFileId(), rqDetails
		.getCapability(), rqDetails.getLocationList(), rqDetails
		.getCurrentReplica());
	this.requiredObjects = new ArrayList<Long>();
	this.preferredObjects = new ArrayList<Long>();
	this.availableObjectsOnOSD = new HashMap<ServiceUUID, List<Long>>();
	this.availableOSDsForObject = new HashMap<Long, List<ServiceUUID>>();
	this.knownFilesize = -1;
	this.next = null;
    }

    /**
     * chooses the next object, which will be replicated
     */
    public void selectNext() {
	if(next != null) {
//	    removePreferredObject(next.objectID);
//	    removeRequiredObject(next.objectID);
	    next = null;
	}
    }
    
    /**
     * 
     */
    public void selectNextOSD(long objectID) {
	if(next != null) {
//	    removePreferredObject(next.objectID);
//	    removeRequiredObject(next.objectID);
	    next = null;
	}
    }

    /**
     * Returns the "result" from selectNext().
     * @return null, if selectNext() has not been executed before or no object to fetch exists
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public NextRequest getNext() {
	return next;
    }

    /**
     * add an object which must be replicated
     * @param e
     * @return
     */
    public boolean addRequiredObject(long objectID) {
	Long object = Long.valueOf(objectID);
	if(!this.requiredObjects.contains(object)) {
	    boolean added = this.requiredObjects.add(object);
	    if(added) {
		// do more
		if(!availableOSDsForObject.containsKey(object))
		    availableOSDsForObject.put(object, details.otherReplicas.getOSDsByObject(objectID));
	    }
	    return added;
	}
	else return false;
    }

    /**
     * remove an object which need not be replicated anymore
     * @param o
     * @see java.util.ArrayList#remove(java.lang.Object)
     */
    public boolean removeRequiredObject(long objectID) {
	boolean removed = this.requiredObjects.remove(Long.valueOf(objectID));
	// do more
//	availableOSDsForObject.remove(Long.valueOf(objectID));
	return removed;
    }

    /**
     * Add an object which must be replicated first.
     * Note: Adds the object also to required objects list.
     * @param e
     * @return
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public boolean addPreferredObject(long objectID) {
	boolean added = true;
	Long object = Long.valueOf(objectID);
	if (!this.preferredObjects.contains(object)) {
	    if (this.preferredObjects.add(object))
		if (!addRequiredObject(objectID))
		    if (!this.requiredObjects.contains(object)) {
			// rollback
			this.preferredObjects.remove(object);
			added = false;
		    }
	    return added;
	} else
	    return false;
    }

    /**
     * Remove an object which must be replicated first.
     * Note: Removes the object also from required objects list.
     * @param o
     * @return
     * @see java.util.ArrayList#remove(java.lang.Object)
     */
    public boolean removePreferredObject(long objectID) {
	boolean removed;
	removed = this.preferredObjects.remove(Long.valueOf(objectID));
	removeRequiredObject(objectID);
	return removed;
    }

    /**
     * Returns how much objects still must be replicated.
     * @return
     * @see java.util.ArrayList#size()
     */
    public int getRequiredObjectsCount() {
	return this.requiredObjects.size();
    }

    /**
     * Returns how much objects will be replicated preferred.
     * @return
     * @see java.util.ArrayList#size()
     */
    public int getPreferredObjectsCount() {
	return this.preferredObjects.size();
    }

    /**
     * Sets the new filesize, if it is larger than the older one.
     * 
     * @param filesize
     */
    public synchronized void setKnownFilesize(long filesize) {
	if (knownFilesize < filesize) 
	    knownFilesize = filesize;
    }
    
    /**
     * @return the knownFilesize
     */
    public synchronized long getKnownFilesize() {
	return knownFilesize;
    }

    /**
     * Removes the OSD from the list that is used for knowing which OSDs could
     * be used for fetching this object.
     * 
     * @param objectID
     */
    public void removeOSDForObject(long objectID, ServiceUUID osd) {
	availableOSDsForObject.get(Long.valueOf(objectID)).remove(osd);
    }

    /*
     * FIXME: internal-handling would be better
     */
    public void removeOSDListForObject(long objectID) {
	availableOSDsForObject.remove(Long.valueOf(objectID));
    }
}
