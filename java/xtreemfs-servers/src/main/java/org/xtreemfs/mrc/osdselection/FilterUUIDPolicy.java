/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes
        .OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Filters all OSDs that have a matching UUID. attributeKeyString are
 * specified via an * extended attribute value, as defined in
 * attributeKeyString. Attribute * values may contain * '*'s to indicate that
 * parts of the UUID also match.
 * By default, the list of * attributeKeyString contains '*' to indicate that
 * any UUID matches.
 *
 * @author stender, seibert
 */
public class FilterUUIDPolicy implements OSDSelectionPolicy {

    public static final short POLICY_ID = (short) OSDSelectionPolicyType
            .OSD_SELECTION_POLICY_FILTER_UUID
            .getNumber();

    /**
     * identifier for setting the allowedUUIDs attribute
     */
    private static final String attributeKeyString = "allowedUUIDs";

    /**
     * set of possible UUIDs to be returned by getOSDs
     */
    private Set<String> allowedUUIDs = new HashSet<String>();

    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs,
                                      InetAddress clientIP,
                                      VivaldiCoordinates clientCoords,
                                      XLocList currentXLoc, int numOSDs) {
        return getOSDs(allOSDs);
    }

    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {

        if (allOSDs == null) {
            return null;
        }

        ServiceSet.Builder filteredOSDs = ServiceSet.newBuilder();
        for (Service osd : allOSDs.getServicesList()) {
            if (isInUUIDs(osd)) {
                filteredOSDs.addServices(osd);
            }
        }

        return filteredOSDs;
    }

    @Override
    public void setAttribute(String key, String value) {

        if (key.equals(attributeKeyString)) {

            allowedUUIDs.clear();

            if (value != null) {
                StringTokenizer st = new StringTokenizer(value, " ,;\t\n");
                while (st.hasMoreTokens())
                    allowedUUIDs.add(st.nextToken());
            }
        }
    }

    /**
     * checks whether a given object storage device's UUID is within the set
     * of allowed UUIDs
     *
     * @param osd the object storage device to be checked
     * @return true if the given OSD is allowed
     */
    private boolean isInUUIDs(Service osd) {

        final String osdUUID = new ServiceUUID(osd.getUuid()).toString();

        if (allowedUUIDs.size() == 0)
            return true;

        for (String uuid : allowedUUIDs) {

            if (uuid.endsWith("*") &&
                    osdUUID.startsWith(uuid.substring(0, uuid.length() - 1))) {
                return true;
            }

            if (uuid.startsWith("*") &&
                    osdUUID.endsWith(uuid.substring(1, uuid.length()))) {
                return true;
            }

            if (uuid.equals(osdUUID))
                return true;
        }

        return false;

    }

}
