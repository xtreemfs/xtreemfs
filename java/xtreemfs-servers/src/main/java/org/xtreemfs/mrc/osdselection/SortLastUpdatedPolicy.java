package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet.Builder;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

public class SortLastUpdatedPolicy implements OSDSelectionPolicy {

    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_SORT_LAST_UPDATED
                                                .getNumber();
    @Override
    public Builder getOSDs(Builder allOSDs,
                           InetAddress clientIP,
                           VivaldiCoordinates clientCoords,
                           XLocList currentXLoc,
                           int numOSDs,
                           String path) {
        return getOSDs(allOSDs);
    }

    @Override
    public Builder getOSDs(Builder allOSDs) {
        // Sort according to last updated time stamp
        List<Service> sortedOSDs = new ArrayList<Service>(allOSDs.getServicesList());

        // Use Collections.sort because it is stable
        Collections.sort(sortedOSDs, new Comparator<Service>() {
            @Override
            public int compare(Service osd1, Service osd2) {
                return Long.compare(
                    Long.parseLong(KeyValuePairs.getValue(osd1.getData().getDataList(), "seconds_since_last_update")),
                    Long.parseLong(KeyValuePairs.getValue(osd2.getData().getDataList(), "seconds_since_last_update"))
                );

            }
        });

        // Create result ServiceSet
        Builder result = ServiceSet.newBuilder();
        for (Service osd : sortedOSDs) {
            result.addServices(osd);
        }
        return result;
    }

    @Override
    public void setAttribute(String key, String value) {
        // don't accept any attributes
    }
}
