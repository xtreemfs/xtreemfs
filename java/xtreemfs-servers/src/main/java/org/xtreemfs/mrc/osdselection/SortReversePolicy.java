/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;

import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Reverses the list of replicas. Mainly used internally in unit tests.
 */
public class SortReversePolicy implements OSDSelectionPolicy {

    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_REVERSE.getNumber();

    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs,
                                      InetAddress clientIP,
                                      VivaldiCoordinates clientCoords,
                                      XLocList currentXLoc,
                                      int numOSDs,
                                      String path) {
        return getOSDs(allOSDs);
    }

    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {
        if (allOSDs == null)
            return null;

        allOSDs = PolicyHelper.reverseServiceSet(allOSDs);
        return allOSDs;
    }

    @Override
    public void setAttribute(String key, String value) {
        // don't accept any attributes
    }

}
