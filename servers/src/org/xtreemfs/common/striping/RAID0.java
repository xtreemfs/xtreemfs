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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

/**
 * RAID 0
 * 
 * @author clorenz
 */
public final class RAID0 extends StripingPolicy {
    protected static final long KILOBYTE = 1024L;

    /**
     * used as key for JSON
     */
    public static final String POLICY_NAME = "RAID0";
    /**
     * used as key for JSON
     */
    protected static final String JSON_STRIPE_SIZE_TOKEN = "stripe-size";

    protected final long stripeSize;

    /**
     * Creates a new instance of RAID0
     * 
     * @param size
     *            Size of the stripes in kilobytes (1kB == 1024 bytes)
     * @param width
     *            Number of OSDs where the file will be striped
     */
    public RAID0(long size, long width) {
	super(width);

	if (size <= 0)
	    throw new IllegalArgumentException("size must be > 0");

	this.stripeSize = size;
    }

    @Override
    public long getStripeSize(long objID) {
	return this.stripeSize * KILOBYTE;
    }

    @Override
    public long getRow(long objId) {
	return objId / this.width;
    }

    /**
     * It generates an object from a given map of names and values
     * 
     * @param translater
     *            Map containing a RAID0 object like a set of pairs (name,
     *            value)
     * @return The object contained in the map
     */
    public static RAID0 readFromJSON(Map<String, Object> translater)
	    throws JSONException {
	String name = (String) translater.get(JSON_STRIPING_POLICY_TOKEN);

	if (name.equals(POLICY_NAME)) {
	    Object tmp = translater.get(JSON_STRIPE_SIZE_TOKEN);
	    if (tmp == null)
		throw new JSONException(JSON_STRIPE_SIZE_TOKEN
			+ " argument is missing");
	    long size = (Long) tmp;

	    tmp = translater.get(JSON_WIDTH_TOKEN);
	    if (tmp == null)
		throw new JSONException(JSON_WIDTH_TOKEN
			+ " argument is missing");
	    long width = (Long) tmp;

	    return new RAID0(size, width);
	} else
	    throw new JSONException("[ E | RAID0 ] Bad striping policy name");
    }

    @Override
    public JSONString asJSONString() throws JSONException {
	return new JSONString(JSONParser.writeJSON(asMap()));
    }

    @Override
    public Map<String, Object> asMap() {
	Map<String, Object> returnValue = new HashMap<String, Object>();
	returnValue.put(JSON_STRIPING_POLICY_TOKEN, POLICY_NAME);
	returnValue.put(JSON_STRIPE_SIZE_TOKEN, stripeSize);
	returnValue.put(JSON_WIDTH_TOKEN, getWidth());

	return returnValue;
    }

    @Override
    public String toString() {
	return POLICY_NAME + " with " + this.width + " width and "
		+ this.stripeSize + "kb stripe-size";
    }

    @Override
    public long getObject(long offset) {
	return (offset / this.stripeSize) / KILOBYTE;
    }

    @Override
    public long getFirstByte(long object) {
	return object * this.stripeSize * KILOBYTE;
    }

    @Override
    public long getLastByte(long object) {
	return getFirstByte(object + 1) - 1;
    }

    @Override
    public List<StripeInfo> getObjects(long firstByte, long lastByte) {
	ArrayList<StripeInfo> list = new ArrayList<StripeInfo>(2);
	long objectID, relativeFirstByte, relativeLastByte, osd;

	// first object
	objectID = getObject(firstByte);
	relativeFirstByte = firstByte - getFirstByte(objectID);
	relativeLastByte = ((relativeFirstByte + (lastByte - firstByte)) < stripeSize
		* KILOBYTE) ? (relativeFirstByte + (lastByte - firstByte))
		: (stripeSize * KILOBYTE - 1);
	osd = getOSDByObject(objectID);

	StripeInfo start = new StripeInfo(objectID, osd, relativeFirstByte,
		relativeLastByte);
	list.add(start);

	if ((objectID = getObject(lastByte)) != start.objectNumber) { // multiple
								      // objects
	    // last object
	    relativeFirstByte = 0L;
	    relativeLastByte = lastByte - getFirstByte(objectID);
	    osd = getOSDByObject(objectID);

	    StripeInfo end = new StripeInfo(objectID, osd, relativeFirstByte,
		    relativeLastByte);
	    list.add(end);
	}
	return list;
    }

    @Override
    public int getOSDByObject(long object) {
	return (int) (object % this.width);
    }

    @Override
    public int getOSDByOffset(long offset) {
	return getOSDByObject(getObject(offset));
    }

    @Override
    public String getPolicyName() {
	return POLICY_NAME;
    }

    /*
     * old code
     */
    @Override
    public boolean isLocalObject(long objId, long osdNo) {
	return objId % getWidth() == osdNo - 1;
    }
}
