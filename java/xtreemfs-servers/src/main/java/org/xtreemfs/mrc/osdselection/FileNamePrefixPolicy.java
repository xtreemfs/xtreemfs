package org.xtreemfs.mrc.osdselection;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * OSDSelectionPolicy that allows to choose an OSD based on the path of the
 * currently handled file.
 */
public class FileNamePrefixPolicy implements OSDSelectionPolicy {

    public static final short POLICY_ID = (short) GlobalTypes
            .OSDSelectionPolicyType
            .OSD_SELECTION_POLICY_FILENAME_PREFIX
            .getNumber();

    /*
     identifier for setting prefixes and OSD ids
    */
    private static final String attributeKeyString = "filenamePrefix";

    /*
      prefixes with an explicit OSD
     */
    private HashMap<String, String> prefixToOSDMap =
            new HashMap<String, String>();

    @Override
    public DIR.ServiceSet.Builder getOSDs(DIR.ServiceSet.Builder allOSDs,
                                          InetAddress clientIP,
                                          GlobalTypes.VivaldiCoordinates
                                                  clientCoords,
                                          XLocList currentXLoc,
                                          int numOSDs,
                                          String path) {

        List<DIR.Service> allServices = allOSDs.getServicesList();
        List<DIR.Service> returnList = new LinkedList<DIR.Service>();

        if (!this.prefixToOSDMap.isEmpty()) {
            int prefixLength = path.lastIndexOf("/");
            String filePrefix = path.substring(0, prefixLength);

            // check whether the prefix map has an entry for the file,
            // and if yes, whether the corresponding OSD exists
            for (String prefix : this.prefixToOSDMap.keySet()) {
                // maybe this is not strict enough?
                if (prefix.endsWith(filePrefix) || filePrefix.endsWith(prefix)) {
                    for (DIR.Service osd : allServices) {
                        String currentUUID =
                                new ServiceUUID(osd.getUuid()).toString();
                        if (currentUUID.equals(this.prefixToOSDMap.get(prefix))) {
                            returnList.add(osd);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        if (returnList.isEmpty()) {
            returnList.addAll(allServices);
        }

        return DIR.ServiceSet.newBuilder().addAllServices(returnList);
    }

    @Override
    public DIR.ServiceSet.Builder getOSDs(DIR.ServiceSet.Builder allOSDs) {
        return allOSDs;
    }

    @Override
    public void setAttribute(String key, String value) {
        if (key.equals(attributeKeyString)) {
            String command[] = value.split(" ");
            if (command.length == 3 && command[0].equals("add")) {
                while (command[1].charAt(command[1].length() - 1) == '/') {
                    command[1] = command[1].substring(0, command[1].length() - 1);
                }
                this.prefixToOSDMap.put(command[1], command[2]);
            } else if (command.length >= 2 && command[0].equals("remove")) {
                this.prefixToOSDMap.remove(command[1]);
            } else if (command.length == 1 && command[0].equals("clear")) {
                this.prefixToOSDMap.clear();
            }
        }
    }
}
