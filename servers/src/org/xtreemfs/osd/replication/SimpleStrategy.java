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
package org.xtreemfs.osd.replication;

import java.util.List;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.osd.RequestDetails;

/**
 * A simple transfer strategy, which fetches the next object and iterates
 * sequentially through the replicas (like Round-Robin).
 * 
 * 13.10.2008
 * 
 * @author clorenz
 */
public class SimpleStrategy extends TransferStrategy {
    private int indexOfLastUsedOSD = -1;

    /**
     * @param rqDetails
     */
    public SimpleStrategy(RequestDetails rqDetails) {
	super(rqDetails);
    }

    @Override
    public void selectNext() {
	// prepare
	super.selectNext();
	
	NextRequest next = new NextRequest();
	// first fetch a preferred object
	if (!this.preferredObjects.isEmpty()) {
	    next.objectID = this.preferredObjects.remove(0);
	    this.requiredObjects.remove(Long.valueOf(next.objectID));
	} else { // fetch an object
	    if (!this.requiredObjects.isEmpty()) {
		next.objectID = this.requiredObjects.remove(0);
	    } else {
		// nothing to fetch
		next = null;
	    }
	}
	
	if(next!=null) {
	    // use the next replica relative to the last used replica
	    List<ServiceUUID> osds = this.details.otherReplicas
		    .getOSDsByObject(next.objectID);
	    this.indexOfLastUsedOSD = ++indexOfLastUsedOSD % osds.size();
	    next.osd = osds.get(this.indexOfLastUsedOSD);

	    next.requestObjectList = false;
	    this.next = next;
	}
    }
}
