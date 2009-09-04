/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.osdselection;

import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;

/**
 * Class with static helper methods for policies.
 * 
 * @author stender
 * 
 */
public class PolicyHelper {
    
    /**
     * Removes all OSDs from the given serivce set that are already included in
     * the given XLoc list.
     * 
     * @param allOSDs
     *            the set of OSDs
     * @param xLocList
     *            the XLoc list containing all OSDs to remove
     */
    public static void removeUsedOSDs(ServiceSet allOSDs, XLocList xLocList) {
        
        if(xLocList == null)
            return;
        
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            XLoc currentRepl = xLocList.getReplica(i);
            for (int j = 0; j < currentRepl.getOSDCount(); j++)
                for (int k = 0; k < allOSDs.size(); k++)
                    if (currentRepl.getOSD(j).equals(allOSDs.get(k).getUuid())) {
                        allOSDs.remove(k);
                        break;
                    }
        }
    }
    
}
