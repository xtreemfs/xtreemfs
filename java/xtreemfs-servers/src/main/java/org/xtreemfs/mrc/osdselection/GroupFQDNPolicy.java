/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.Comparator;

import org.xtreemfs.common.util.NetUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes
        .OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Determines a subgroup of OSDs that can be assigned to a new replica, by means
 * of the fully qualified distinguished server names. The selection will be
 * restricted to OSDs that are located in the same domain. If multiple such
 * possible subgroups exist, the one closest to the client will be returned.
 *
 * @author bjko, stender
 */
public class GroupFQDNPolicy extends FQDNPolicyBase {

    public static final short POLICY_ID = (short) OSDSelectionPolicyType
            .OSD_SELECTION_POLICY_GROUP_FQDN
            .getNumber();

    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs,
                                      final InetAddress clientIP,
                                      VivaldiCoordinates clientCoords,
                                      XLocList currentXLoc,
                                      int numOSDs,
                                      String path) {

        if (allOSDs == null)
            return null;

        // sort the list by their FQDN distance to the client
        allOSDs = PolicyHelper.sortServiceSet(allOSDs, new
                Comparator<Service>() {
                    public int compare(Service o1, Service o2) {

                        try {
                            final String host1 = new ServiceUUID(o1.getUuid())
                                    .getAddress().getHostName();
                            final String host2 = new ServiceUUID(o2.getUuid())
                                    .getAddress().getHostName();

                            return getMatch(host2, clientIP.getHostName()) -
                                    getMatch
                                    (host1, clientIP.getHostName());

                        } catch (UnknownUUIDException exc) {
                            Logging.logError(Logging.LEVEL_ERROR, this, exc);
                            return 0;
                        }
                    }
                });

        // find the closest group to the client that is large enough
        String currentDomain = "";
        int currentDomainSize = 0;
        int currentIndex = 0;

        int bestClientMatch = 0;
        int bestIndex = -1;

        for (int i = 0; i < allOSDs.getServicesCount(); i++) {

            try {
                Service s = allOSDs.getServices(i);
                String hostName = new ServiceUUID(s.getUuid()).getAddress()
                        .getHostName();
                String domain = NetUtils.getDomain(hostName);

                if (domain.equals(currentDomain)) {

                    currentDomainSize++;
                    if (currentDomainSize == numOSDs) {
                        int cd = getMatch(hostName, clientIP
                                .getCanonicalHostName());
                        if (cd > bestClientMatch) {
                            bestClientMatch = cd;
                            bestIndex = currentIndex;
                        }
                    }
                } else {
                    currentDomainSize = 1;
                    currentDomain = domain;
                    currentIndex = i;

                    if (currentDomainSize == numOSDs) {
                        int cd = getMatch(hostName, clientIP
                                .getCanonicalHostName());
                        if (cd > bestClientMatch) {
                            bestClientMatch = cd;
                            bestIndex = currentIndex;
                        }
                    }
                }

            } catch (UnknownUUIDException exc) {
                Logging.logError(Logging.LEVEL_ERROR, this, exc);
                break;
            }

        }

        ServiceSet.Builder result = ServiceSet.newBuilder();

        if (bestIndex != -1)
            for (int i = 0; i < numOSDs; i++)
                result.addServices(allOSDs.getServices(bestIndex + i));

        return result;

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
