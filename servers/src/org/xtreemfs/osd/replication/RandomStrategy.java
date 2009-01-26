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
    public NextRequest selectNext() {
	NextRequest next = new NextRequest();
	// first fetch a preferred object
	if (!this.preferredObjects.isEmpty()) {
	    next.objectID = this.preferredObjects.remove(getPositiveRandom()
		    % this.preferredObjects.size());
	    this.requiredObjects.remove(Long.valueOf(next.objectID));
	} else { // fetch an object
	    if (!this.requiredObjects.isEmpty()) {
		next.objectID = this.requiredObjects.remove(getPositiveRandom()
			% this.requiredObjects.size());
	    } else
		// nothing to fetch
		return null;
	}
	// use random OSD
	List<ServiceUUID> osds = this.details.locationList
		.getOSDsByObject(next.objectID);
	if (!osds.isEmpty()) {
	    next.osd = osds.get(getPositiveRandom() % osds.size());
	} else
	    return null;

	next.requestObjectList = false;
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
