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
 * 
 * 09.09.2008
 * 
 * @author clorenz
 */
public abstract class TransferStrategy {
    protected class ReplicationDetails {
	String fileId;
	Capability capability;
	Locations locationList;
	Location currentReplica;

	ReplicationDetails(String fileId, Capability capability,
		Locations locationList, Location currentReplica) {
	    super();
	    this.fileId = fileId;
	    this.capability = capability;
	    this.locationList = locationList;
	    this.currentReplica = currentReplica;
	}
    }

    public class NextRequest {
	public ServiceUUID osd;
	public long objectID;
	public boolean requestObjectList;
    }

    protected ReplicationDetails details;

    protected ArrayList<Long> requiredObjects; // maybe additionally current

    protected ArrayList<Long> preferredObjects; // requested objects

    protected HashMap<String, List<Long>> aviableObjectsOnOSD;
    protected HashMap<Long, List<String>> aviableOSDsForObject;

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
	this.aviableObjectsOnOSD = new HashMap<String, List<Long>>();
	this.aviableOSDsForObject = new HashMap<Long, List<String>>();
    }

    /**
     * 
     * @return null, if no object to fetch exists
     */
    public abstract NextRequest selectNext();

    /**
     * @param e
     * @return
     */
    public boolean addRequiredObject(long objectID) {
	return (this.requiredObjects.contains(Long.valueOf(objectID))) ? true
		: this.requiredObjects.add(Long.valueOf(objectID));
    }

    /**
     * @param o
     * @return true, if the list doesn't contain the element anymore
     */
    public boolean removeRequiredObject(long objectID) {
	boolean removed = true;
	if (this.requiredObjects.contains(Long.valueOf(objectID)))
	    removed = this.requiredObjects.remove(Long.valueOf(objectID));
	return removed;
    }

    /**
     * caution: can break the consistency between required and preferred objects
     * 
     * @param e
     * @return
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public boolean addPreferredObject(long objectID) {
	boolean added = true;
	if (!this.preferredObjects.contains(Long.valueOf(objectID))) {
	    added = this.preferredObjects.add(Long.valueOf(objectID));
	}
	if (!addRequiredObject(objectID)) { // rollback
	    added = false;
	    this.preferredObjects.remove(Long.valueOf(objectID));
	}
	return added;
    }

    /**
     * caution: can break the consistency between required and preferred objects
     * 
     * @param o
     * @return
     * @see java.util.ArrayList#remove(java.lang.Object)
     */
    public boolean removePreferredObject(long objectID) {
	boolean removed = true;
	if (this.preferredObjects.contains(Long.valueOf(objectID)))
	    removed = this.preferredObjects.remove(Long.valueOf(objectID));
	if (removed)
	    if (!removeRequiredObject(objectID)) { // rollback
		this.preferredObjects.add(Long.valueOf(objectID));
		removed = false;
	    }
	return removed;
    }

    /**
     * @return
     * @see java.util.ArrayList#size()
     */
    public int getRequiredObjectsCount() {
	return this.requiredObjects.size();
    }

    /**
     * @return
     * @see java.util.ArrayList#size()
     */
    public int getPreferredObjectsCount() {
	return this.preferredObjects.size();
    }
}
