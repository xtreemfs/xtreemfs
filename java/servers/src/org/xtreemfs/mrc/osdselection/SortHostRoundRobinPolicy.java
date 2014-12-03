package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet.Builder;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

public class SortHostRoundRobinPolicy implements OSDSelectionPolicy {

    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_HOST_ROUND_ROBIN
                                                .getNumber();
    @Override
    public Builder getOSDs(Builder allOSDs, InetAddress clientIP, VivaldiCoordinates clientCoords,
            XLocList currentXLoc, int numOSDs) {
        return getOSDs(allOSDs);
    }

    @Override
    public Builder getOSDs(Builder allOSDs) {

        // Map OSDs to hosts
        HashMap<String, LinkedList<Service>> hostToOsdsMap = new HashMap<String, LinkedList<Service>>();
        for (Service osd : allOSDs.getServicesList()) {

            try {
                String host = new ServiceUUID(osd.getUuid()).getAddress().getHostName();
                if (hostToOsdsMap.containsKey(host)) {
                    hostToOsdsMap.get(host).add(osd);
                } else {
                    hostToOsdsMap.put(host, new LinkedList<Service>());
                    hostToOsdsMap.get(host).add(osd);
                }

            } catch (UnknownUUIDException exc) {
                Logging.logError(Logging.LEVEL_ERROR, this, exc);
                continue;
            }
        }

        // Create result ServiceSet
        Builder result = ServiceSet.newBuilder();
        while (result.getServicesCount() < allOSDs.getServicesCount()) {
            for (LinkedList<Service> osds : hostToOsdsMap.values()) {
                if (!osds.isEmpty()) {
                    result.addServices(osds.pop());
                }
            }
        }

        return result;
    }

    @Override
    public void setAttribute(String key, String value) {
        // don't accept any attributes
    }
}
