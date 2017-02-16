package org.xtreemfs.mrc.osdselection;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * OSD selection policy that allows to specify one preferred UUID. The OSDs
 * are sorted in such a way that the OSD with the preferred UUID is first;
 * all other OSDs are appended to the list in random order.
 * <p>
 * Created by felix on 10/5/16.
 */
public class PreferredUUIDPolicy implements OSDSelectionPolicy {

    public static final short POLICY_ID = (short) GlobalTypes
            .OSDSelectionPolicyType
            .OSD_SELECTION_POLICY_PREFERRED_UUID
            .getNumber();

    /*
     identifier for setting the preferred UUID attribute
    */
    private static final String attributeKeyString = "preferredUUID";

    /**
     * the UUID of the preferred object storage device
     */
    private String preferredUUID;

    @Override
    public DIR.ServiceSet.Builder getOSDs(DIR.ServiceSet.Builder allOSDs,
                                          InetAddress clientIP,
                                          GlobalTypes.VivaldiCoordinates
                                                  clientCoords,
                                          XLocList currentXLoc,
                                          int numOSDs,
                                          String path) {
        return getOSDs(allOSDs);
    }

    @Override
    public DIR.ServiceSet.Builder getOSDs(DIR.ServiceSet.Builder allOSDs) {
        List<DIR.Service> allServices = allOSDs.getServicesList();

        List<DIR.Service> nonPreferredServices = new LinkedList<DIR.Service>();
        DIR.Service preferredService = null;

        for (DIR.Service osd : allServices) {
            String currentUUID = new ServiceUUID(osd.getUuid()).toString();
            if (currentUUID.equals(preferredUUID)) {
                preferredService = osd;
            } else {
                nonPreferredServices.add(osd);
            }
        }

        List<DIR.Service> OSDsInCorrectOrder = new LinkedList<DIR.Service>();

        if (preferredService != null) {
            OSDsInCorrectOrder.add(0, preferredService);
        }
        
        Collections.shuffle(nonPreferredServices);
        OSDsInCorrectOrder.addAll(nonPreferredServices);

        return DIR.ServiceSet.newBuilder().addAllServices(OSDsInCorrectOrder);
    }

    @Override
    public void setAttribute(String key, String value) {
        if (key.equals(attributeKeyString)) {
            this.preferredUUID = value;
        }
    }
}
