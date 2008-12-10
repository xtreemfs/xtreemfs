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
import java.util.Iterator;
import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

/**
 * It models the list of locations of replicas
 * 
 * @author clorenz
 */
public class Locations implements Iterable<Location> {
    /**
     * update policies
     */
    public static final String REPLICA_UPDATE_POLICY_SYNC = "sync";
    public static final String REPLICA_UPDATE_POLICY_ONDEMAND = "lazy";

    /**
     * used update policy for all replicas
     */
    private String replicaUpdatePolicy;
    /**
     * it defines how many replicas must be updated synchronous, if update
     * policy is "sync",
     */
    private int replicaUpdatePolicySyncLevel;

    /**
     * version of the Locations-list
     */
    private final long version;

    /**
     * list of replicas
     */
    private final List<Location> replicas;

    /**
     * It creates an instance of Locations with an existing list. It uses the
     * "ondemand"-policy as default.
     * 
     * @param locations
     *            List of replica's location
     */
    public Locations(List<Location> locations) {
	this(locations, 1, REPLICA_UPDATE_POLICY_ONDEMAND, 0);
    }

    /**
     * It creates an instance of Locations with an existing list.
     * 
     * @param locations
     *            List of replica's location
     * @param version
     *            version of locations-list
     * @param replicaUpdatePolicy
     *            policy which will be used
     * @param replicaSyncLevel
     *            how many replicas must be updated synchronous, if update
     *            policy is "sync"
     */
    public Locations(List<Location> locations, long version,
	    String replicaUpdatePolicy, int replicaSyncLevel) {
	if (locations == null)
	    throw new IllegalArgumentException("The list of replicas is null");
	else if (locations.size() == 0)
	    throw new IllegalArgumentException(
		    "There is no replicas in the list");

	this.replicas = locations;
	this.version = version;
	this.replicaUpdatePolicy = replicaUpdatePolicy;
	this.replicaUpdatePolicySyncLevel = replicaSyncLevel;
    }

    /**
     * Creates an instance of this class from a JSON representation
     * 
     * @param plain
     *            JSON representation of an object of this class
     */
    public Locations(JSONString plain) throws JSONException {
	List<Object> list = (List<Object>) JSONParser.parseJSON(plain);

	if (list == null)
	    throw new IllegalArgumentException("The list of replicas is null");
	if (list.size() < 2)
	    throw new IllegalArgumentException("Locations list is not valid.");

	this.version = (Long) list.get(1);

	List<List<Object>> xLocList = (List<List<Object>>) list.get(0);
	this.replicas = new ArrayList<Location>(xLocList.size());
	for (int i = 0; i < xLocList.size(); i++)
	    this.replicas.add(new Location((List<Object>) xLocList.get(i)));

	if (list.size() >= 3)
	    parseRepUpdatePolicy((String) list.get(2));
	else
	    replicaUpdatePolicy = REPLICA_UPDATE_POLICY_ONDEMAND;
    }

    /**
     * parses the JSON-update-policy-string
     * 
     * @param rp
     */
    private void parseRepUpdatePolicy(String rp) {
	// parse the replication policy
	int sepIndex = rp.indexOf(':');
	if (sepIndex == -1) {
	    replicaUpdatePolicy = rp;
	    replicaUpdatePolicySyncLevel = replicas.size();
	} else {
	    // TODO: conform to the specification: don't allow "lazy:5"
	    replicaUpdatePolicy = rp.substring(0, sepIndex);
	    replicaUpdatePolicySyncLevel = Integer.parseInt(rp
		    .substring(sepIndex + 1));
	    if (replicaUpdatePolicySyncLevel > replicas.size()) // all sync
		replicaUpdatePolicySyncLevel = replicas.size();
	}
    }

    /**
     * It provides a list representing the object
     * 
     * @return The listed representation of the object
     */
    public List<Object> asList() {
	List<Object> returnValue = new ArrayList<Object>(replicas.size());
	for (Location loc : replicas) {
	    returnValue.add(loc.asList());
	}
	return returnValue;
    }

    /**
     * It provides a JSONString representing the object
     * 
     * @return The JSONString representation of the object
     */
    public JSONString asJSONString() throws JSONException {
	List<Object> args = new ArrayList(3);
	args.add(asList());
	args.add(version);
	if (replicaUpdatePolicy.equals(REPLICA_UPDATE_POLICY_SYNC)
		&& replicaUpdatePolicySyncLevel != replicas.size())
	    args.add(replicaUpdatePolicy + ":" + replicaUpdatePolicySyncLevel);
	else
	    args.add(replicaUpdatePolicy);
	return new JSONString(JSONParser.writeJSON(args));
    }

    /**
     * It provides the location related to an OSD
     * 
     * @param osd
     *            OSD to locate
     * @return The replica location where the osd is taking part.
     */
    public Location getLocation(ServiceUUID osd) {
	for (Location loc : replicas) {
	    if (loc.containsOSD(osd))
		return loc;
	}
	return null;
    }

    /**
     * It provides the location of the specified index
     * 
     * @param index
     * @return
     */
    public Location getLocation(int index) {
	return replicas.get(index);
    }
    
    /**
     * Provides a list of OSDs which are containing replicas of the given object.
     * NOTE: If the replicas use different striping policies the same object must not contain the same data.
     * @param objectID
     * @return
     */
    public List<ServiceUUID> getOSDsByObject(long objectID){
	List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
	for(Location loc : replicas){
	    osds.add(loc.getOSDByObject(objectID));
	}
	return osds;
    }

    /**
     * Resolves the UUID of all OSDs
     * 
     * @throws UnknownUUIDException
     */
    public void resolveAll() throws UnknownUUIDException {
	for (Location loc : this.replicas) {
	    loc.resolve();
	}
    }

    /**
     * Provides the number, how many replicas are used.
     * 
     * @return
     */
    public int getNumberOfReplicas() {
	return replicas.size();
    }

    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if ((obj == null) || (obj.getClass() != this.getClass()))
	    return false;

	Locations other = (Locations) obj;
	return replicas.equals(other.replicas);
    }

    public int hashCode() {
	return replicas.hashCode();
    }

    /**
     * Provides the version of the locations-list.
     */
    public long getVersion() {
	return version;
    }

    /**
     * Provides the used update policy.
     * @return
     */
    public String getReplicaUpdatePolicy() {
	return replicaUpdatePolicy;
    }

    /**
     * Provides how many replicas must be updated synchronous, if the used update policy is "sync".
     * @return
     */
    public int getReplicaSyncLevel() {
	return replicaUpdatePolicySyncLevel;
    }

    @Override
    public Iterator<Location> iterator() {
	return replicas.iterator();
    }

    @Override
    public String toString() {
	return "version: " + version + " ; " + replicas.toString() + " ; "
		+ replicaUpdatePolicy + ":" + replicaUpdatePolicySyncLevel;
    }
}
