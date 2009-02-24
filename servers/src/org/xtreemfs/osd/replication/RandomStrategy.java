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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.osd.replication;

import java.util.List;
import java.util.Random;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.osd.RequestDetails;
import org.xtreemfs.osd.replication.TransferStrategy.NextRequest;

/**
 * A simple strategy, which selects objects and replicas randomly.
 * 
 * 08.12.2008
 * 
 * @author clorenz
 */
public class RandomStrategy extends TransferStrategy {
    private Random random;

    /**
     * @param rqDetails
     */
    public RandomStrategy(RequestDetails rqDetails) {
	super(rqDetails);
	this.random = new Random();
    }

    @Override
    public void selectNext() {
	// prepare
	super.selectNext();

	long objectID = -1;

	// first fetch a preferred object
	if (!this.preferredObjects.isEmpty()) {
	    objectID = this.preferredObjects.remove(getPositiveRandom()
		    % this.preferredObjects.size());
	    this.requiredObjects.remove(Long.valueOf(objectID));
	} else { // fetch any object
	    if (!this.requiredObjects.isEmpty()) {
		objectID = this.requiredObjects.remove(getPositiveRandom()
			% this.requiredObjects.size());
	    }
	}

	// select OSD
	if(objectID != -1)
	    next = selectNextOSDhelper(objectID);
	else
	    // nothing to fetch
	    next = null;
    }

    @Override
    public void selectNextOSD(long objectID) {
	// prepare
	super.selectNext();
	// select OSD
	next = selectNextOSDhelper(objectID);
    }

    private NextRequest selectNextOSDhelper(long objectID) {
	NextRequest next = new NextRequest();
	next.objectID = objectID;

	List<ServiceUUID> osds = this.availableOSDsForObject.get(objectID);
	if(osds.size() > 0) {
	    // use random OSD
	    next.osd = osds.get(getPositiveRandom() % osds.size());

	    // no object list
	    next.requestObjectList = false;
	} else
	    next = null;
	return next;
    }

    /**
     * returns a random positive integer
     * @return
     */
    private int getPositiveRandom(){
	int result = random.nextInt();
	return (result > 0) ? result : 0-result; 
    }
}
