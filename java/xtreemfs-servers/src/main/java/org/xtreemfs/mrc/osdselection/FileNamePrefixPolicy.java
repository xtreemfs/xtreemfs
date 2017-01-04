/*
 * Copyright (c) 2017 by Felix Seibert,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

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
 * currently handled file. You may set an OSD explicitly for a directory into an
 * xtreemfs volume (using volume_name/.../some_dir); xtreemfs will then choose
 * the given OSD for all files in some_dir or subdirectories of some_dir.
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

        if (path == null) {
            return allOSDs;
        }

        List<DIR.Service> allServices = allOSDs.getServicesList();
        List<DIR.Service> returnList = new LinkedList<DIR.Service>();

        if (!this.prefixToOSDMap.isEmpty()) {
            int locationLength = path.lastIndexOf("/");
            String fileLocationOnVolume = path.substring(0, locationLength);

            // check whether the prefix map has an entry for the file,
            // and if yes, whether the corresponding OSD exists
            for (String prefix : this.prefixToOSDMap.keySet()) {
                if (fileLocationOnVolume.startsWith(prefix)) {
                    for (DIR.Service osd : allServices) {
                        String currentUUID =
                                new ServiceUUID(osd.getUuid()).toString();
                        if (currentUUID.equals(this.prefixToOSDMap.get
                                (prefix))) {
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
                if (!(command[1].length() == 0 || command[2].length() == 0)) {
                    this.prefixToOSDMap.put(removeLeadingTrailingSlashes(command[1]),
                                            command[2]);
                }
            } else if (command.length >= 2 && command[0].equals("remove")) {
                this.prefixToOSDMap.remove(removeLeadingTrailingSlashes
                                                   (command[1]));
            } else if (command.length == 1 && command[0].equals("clear")) {
                this.prefixToOSDMap.clear();
            }
        }
    }

    private static String removeLeadingTrailingSlashes(String path) {
        while (path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        while (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }
}
