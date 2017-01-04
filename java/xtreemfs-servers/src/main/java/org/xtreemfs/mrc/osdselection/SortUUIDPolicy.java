/*
 * Copyright (c) 2013 Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.Comparator;

import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Sorts the list of OSDs based on their UUID. This is mainly used by internal tests.
 */
public class SortUUIDPolicy implements OSDSelectionPolicy {

    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_UUID.getNumber();

    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs,
                                      final InetAddress clientIP,
                                      VivaldiCoordinates clientCoords,
                                      XLocList currentXLoc,
                                      int numOSDs,
                                      String path) {

        if (allOSDs == null) {
            return null;
        }

        allOSDs = PolicyHelper.sortServiceSet(allOSDs, new Comparator<Service>() {
            @Override
            public int compare(Service o1, Service o2) {
                return o1.getUuid().compareTo(o2.getUuid());
            }
        });

        return allOSDs;

    }

    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {
        return allOSDs;
    }

    @Override
    public void setAttribute(String key, String value) {
        // don't accept any attributes
    }

}
