/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
    Barcelona Supercomputing Center - Centro Nacional de Supercomputacion.

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
 * AUTHORS: Christian Lorenz (ZIB), Jan Stender (ZIB), Björn Kolbeck (ZIB), Jesús Malo (BSC)
 */

package org.xtreemfs.common.striping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

/**
 * It models the locations of one replica. Every instance of this class will be
 * an object oriented representation of one replica
 * 
 * @author clorenz
 */
public class Location {
    /**
     * update policies
     */
    public static final String REPLICA_UPDATE_SYNC = "sync";
    public static final String REPLICA_UPDATE_ONDEMAND = "lazy";

    /**
     * used update policy for this replica
     */
    private String replicaUpdatePolicy;

    /**
     * striping policy which is used for this replica
     */
    private StripingPolicy policy;
    /**
     * involved osds
     */
    private List<ServiceUUID> osdList;

    /**
     * It creates a new instance of Location
     * 
     * @param sp
     *            Striping policy of the replica
     * @param osds
     *            OSDs containing the pieces of the same replica
     */
    public Location(StripingPolicy sp, List<ServiceUUID> osds) {
	if ((sp != null) && (osds != null)) {
	    if (sp.getWidth() == osds.size()) {
		policy = sp;
		osdList = osds;
	    } else
		throw new IllegalArgumentException(
			"The striping policy is for " + sp.getWidth()
				+ " OSDs but the list of OSDs contains "
				+ osds.size());
	} else if (sp == null)
	    throw new IllegalArgumentException("The policy is null");
	else
	    throw new IllegalArgumentException("The osdList is null");
    }

    /**
     * It creates a new instance from a list containing the object
     * 
     * @param listedObject
     *            The object contained in the general way (as the JSON parser
     *            gives us)
     */
    public Location(List<Object> listedObject) throws JSONException {
	initLocation(listedObject);
    }

    /**
     * Creates an instance of this class from a JSON representation
     * 
     * @param plain
     *            JSON representation of an object of this class
     */
    public Location(JSONString plain) throws JSONException {
	List<Object> parsed = (List<Object>) JSONParser.parseJSON(plain);

	if (parsed == null)
	    throw new IllegalArgumentException(
		    "The location specification is null");
	else {
	    initLocation(parsed);
	}
    }

    /**
     * Convenience method that initializes the Location
     */
    private void initLocation(List<Object> listedObject) throws JSONException {
	if (listedObject.size() != 2)
	    throw new IllegalArgumentException("Incorrect list's length");

	// It gets the striping policy
	Map<String, Object> policyCandidate = (Map<String, Object>) listedObject
		.get(0);
	if (policyCandidate == null)
	    throw new IllegalArgumentException("The striping policy is null");

	policy = StripingPolicy.readFromJSON(policyCandidate);

	// It gets the OSD list
	List<String> osdListCandidate = (List<String>) listedObject.get(1);

	if (osdListCandidate == null)
	    throw new IllegalArgumentException("The list of replicas is null");
	else if (osdListCandidate.size() != policy.getWidth())
	    throw new IllegalArgumentException(
		    "The number of replicas in the list is wrong");

	osdList = new ArrayList<ServiceUUID>(osdListCandidate.size());
	for (String osdUUID : osdListCandidate) {
	    osdList.add(new ServiceUUID(osdUUID));
	}
    }

    /**
     * Provides the responsible OSD for this object.
     * 
     * @param objectID
     * @return
     */
    public ServiceUUID getOSDByObject(long objectID) {
	return osdList.get(policy.getOSDByObject(objectID));
    }

    /**
     * Provides the responsible OSD for this offset.
     * 
     * @param objectID
     * @return
     */
    public ServiceUUID getOSDByOffset(long offset) {
	return osdList.get(policy.getOSDByOffset(offset));
    }

    /**
     * Provides the responsible OSD for this byte-range. Returns only a value,
     * if the byte-range is saved on one OSD.
     * 
     * @param firstByte
     * @param lastByte
     * @return null, if the byte-range covers multiple objects on different OSDs
     */
    public ServiceUUID getOSDByByteRange(long firstByte, long lastByte) {
	List<StripeInfo> objectRange = policy.getObjects(firstByte, lastByte);
	if (objectRange.size() > 1) {
	    // throw exception, because byte range covers multiple objects
	    // throw new
	    // NoSuchElementException("byte range covers multiple objects");
	    return null;
	} else
	    return getOSDByObject(objectRange.get(0).objectNumber);
    }

    /**
     * It provides the list of OSDs of the location
     * 
     * @return The list of OSDs of the object
     */
    public List<ServiceUUID> getOSDs() {
	return osdList;
    }

    /**
     * Number of OSDs which contain data of this replica.
     * 
     * @return
     */
    public int getWidth() {
	return this.osdList.size();
    }

    /**
     * Resolves the UUID of all OSDs
     * 
     * @throws UnknownUUIDException
     */
    void resolve() throws UnknownUUIDException {
	for (ServiceUUID uuid : osdList) {
	    uuid.resolve();
	}
    }

    /**
     * checks if this replica location belongs to the OSD
     * 
     * @param uuid
     * @return
     */
    public boolean containsOSD(ServiceUUID uuid) {
	return osdList.contains(uuid);
    }

    /**
     * It provides the striping policy of this object
     * 
     * @return The striping policy of the object
     */
    public StripingPolicy getStripingPolicy() {
	return policy;
    }

    /**
     * It provides a listed representation of the object
     * 
     * @return The representation of this object like a list suitable for JSON
     */
    public List<Object> asList() {
	List<Object> returnValue = new ArrayList<Object>(2);
	returnValue.add(policy.asMap());

	List<String> osds = new ArrayList<String>(osdList.size());
	for (ServiceUUID osd : osdList) {
	    osds.add(osd.toString());
	}

	returnValue.add(osds);

	return returnValue;
    }

    /**
     * It gives a JSON string which represents the object.
     * 
     * @return The string representing the object
     */
    public JSONString asJSONString() throws JSONException {
	return new JSONString(JSONParser.writeJSON(asList()));
    }

    /**
     * Provides the used update policy.
     */
    public String getReplicaUpdatePolicy() {
	return this.replicaUpdatePolicy;
    }

    /**
     * @param replicaUpdatePolicy
     *            the replicaUpdatePolicy to set
     */
    public void setReplicaUpdatePolicy(String replicaUpdatePolicy) {
	assert (replicaUpdatePolicy.equals(REPLICA_UPDATE_SYNC) || replicaUpdatePolicy
		.equals(REPLICA_UPDATE_ONDEMAND));
	this.replicaUpdatePolicy = replicaUpdatePolicy;
    }

    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if ((obj == null) || (obj.getClass() != this.getClass()))
	    return false;

	Location other = (Location) obj;
	return policy.equals(other.policy) && osdList.equals(other.osdList);
    }

    public int hashCode() {
	return policy.hashCode() + osdList.hashCode();
    }

    @Override
    public String toString() {
	return osdList.toString() + " ; " + policy;
    }

    /*
     * old code
     */
    public int indexOf(ServiceUUID osdId) {
	return osdList.indexOf(osdId);
    }

}
