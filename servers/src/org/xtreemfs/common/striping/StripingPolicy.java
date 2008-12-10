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
 * AUTHORS: Christian Lorenz (ZIB), Jesús Malo (BSC), Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.common.striping;

import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

/**
 * It models the StripingPolicy.
 * 
 * @author clorenz
 */
public abstract class StripingPolicy {
    /**
     * used as key for JSON
     */
    protected static final String JSON_STRIPING_POLICY_TOKEN = "policy";
    /**
     * used as key for JSON
     */
    protected static final String JSON_WIDTH_TOKEN = "width";

    /**
     * width (number of involved OSDs)
     */
    protected final long width;

    /**
     * Creates an object with the specific width.
     * 
     * @param w
     *            Number of OSDs where this object will be related to
     * @pre w > 0
     */
    protected StripingPolicy(long w) {
	if (w <= 0)
	    throw new IllegalArgumentException("width must be > 0");

	this.width = w;
    }

    /**
     * It provides the number of OSDs of this striping policy object
     * 
     * @return The number of OSDs. It will always be greater than zero.
     */
    public long getWidth() {
	return width;
    }

    /**
     * Returns the name of the policy.
     * 
     * @return
     */
    public abstract String getPolicyName();

    /**
     * Returns the last objectID of the file with this filesize.
     * 
     * @param fileSize
     *            filesize in bytes
     * @return
     */
    public long calculateLastObject(long fileSize) {
	return getObject(fileSize - 1);
    }

    /**
     * Convenient method for getting the size of a stripe in bytes.
     * 
     * @param objectNumber
     *            Number of the object to get the stripe size.
     * @return The number of bytes of the stripe
     */
    public long getStripeSize(long objID) {
	return 1 + getFirstByte(objID) - getLastByte(objID);
    }

    public int hashCode() {
	return asMap().hashCode();
    }

    public boolean equals(Object obj) {

	if (this == obj)
	    return true;
	if ((obj == null) || (obj.getClass() != this.getClass()))
	    return false;

	StripingPolicy other = (StripingPolicy) obj;

	JSONString Iam, ItIs;
	try {
	    Iam = asJSONString();
	    ItIs = other.asJSONString();
	} catch (JSONException ex) {
	    throw new IllegalArgumentException();
	}

	return Iam.equals(ItIs);
    }

    /**
     * It generates a mapped representation of this object
     * 
     * @return The mapped representation of the object
     */
    public abstract Map<String, Object> asMap();

    /**
     * It gives a JSON string which represents the object.
     * 
     * @return The string representing the object
     */
    public abstract JSONString asJSONString() throws JSONException;

    /**
     * It parses a string and recovers the striping policy contained in it
     * 
     * @param plain
     *            The string containing a striping policy
     * @return The object contained in "plain"
     */
    public static StripingPolicy readFromJSON(JSONString plain)
	    throws JSONException {
	Map<String, Object> translater = (Map<String, Object>) JSONParser
		.parseJSON(plain);

	return readFromJSON(translater);
    }

    /**
     * It parses a string and recovers the striping policy contained in it
     * 
     * @param mappedObject
     *            The map containing a striping policy
     * @return The object contained in mappedObject
     */
    public static StripingPolicy readFromJSON(Map<String, Object> mappedObject)
	    throws JSONException {
	StripingPolicy translation;

	if (mappedObject.containsKey(JSON_STRIPING_POLICY_TOKEN)) {
	    String selector = (String) mappedObject
		    .get(JSON_STRIPING_POLICY_TOKEN);

	    // add here additional striping policies
	    if (selector.equals(RAID0.POLICY_NAME))
		translation = RAID0.readFromJSON(mappedObject);
	    else
		throw new JSONException("Unknown striping policy: " + selector);
	} else
	    throw new JSONException("There is no striping policy in the object");

	return translation;
    }

    /**
     * Provides the corresponding object for this byte-offset.
     * @param offset
     * @return
     */
    public abstract long getObject(long offset);

    /**
     * Provides the first byte of this object.
     * @param offset
     * @return
     */
    public abstract long getFirstByte(long object);

    /**
     * Provides the last byte of this object.
     * @param offset
     * @return
     */
    public abstract long getLastByte(long object);

    /**
     * Returns a list of all needed information about the objects which
     * represents this byte-range. If the byte-range only covers one object,
     * there will be only one entry, otherwise 2 entries. On this case the first
     * contains the object where the byte-range starts and the second the object
     * where it ends.
     */
    public abstract List<StripeInfo> getObjects(long firstByte, long lastByte);

    /**
     * Provides the OSD position in this row for the given offset.
     * @param offset
     * @return
     */
    public abstract int getOSDByOffset(long offset);

    /**
     * Provides the OSD position in this row for the given object.
     * @param object
     * @return
     */
    public abstract int getOSDByObject(long object);

    /**
     * Provides the containing row of the object.
     * 
     * @param absObjId
     * @return
     */
    public abstract long getRow(long absObjId);

    /**
     * Returns all needed information where the data of the given object is positioned in the other striping policy.
     * Useful for Re-Striping.
     * @param localObjectID objectID for THIS striping policy
     * @param otherPolicy striping policy for which the data should be converted
     * @return see method "getObjects(long firstByte, long lastByte)"
     */
    public List<StripeInfo> getOtherObjects(long localObjectID,
	    StripingPolicy otherPolicy) {
	return otherPolicy.getObjects(this.getFirstByte(localObjectID), this
		.getLastByte(localObjectID));
    }
    
    /*
     * old code 
     */
    public abstract boolean isLocalObject(long absObjId, long relOsdNo);
}
