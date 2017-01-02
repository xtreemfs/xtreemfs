/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.Random;

import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes
        .OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Randomly shuffles the list of OSDs.
 *
 * @author stender
 */
public class SortRandomPolicy implements OSDSelectionPolicy {

    public static final short POLICY_ID = (short) OSDSelectionPolicyType
            .OSD_SELECTION_POLICY_SORT_RANDOM
            .getNumber();

    /*
    identifier for setting the preferred UUID attribute
    */
    private static final String attributeKeyString = "randomseed";

    /*
    the source of randomness
     */
    private Random random = null;

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

        allOSDs = PolicyHelper.shuffleServiceSet(allOSDs, this.random);
        return allOSDs;
    }

    @Override
    public void setAttribute(String key, String value) {
        // take care: using a specific random seed does not imply deterministic
        // osd selection, only approximately.
        if (key.equals(attributeKeyString)) {
            try {
                long seed = Long.parseLong(value);
                this.random = new Random(seed);
            } catch (NumberFormatException e) {
                this.random = null;
            }
        }
    }

}
