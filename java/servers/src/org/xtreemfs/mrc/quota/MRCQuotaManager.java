/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.foundation.logging.Logging;

/** TODO: Brief description of the purpose of this type and its relation to other types. */
public class MRCQuotaManager {

    private final Map<String, VolumeQuotaManager> volQuotaManMap = new HashMap<String, VolumeQuotaManager>();

    /**
     * 
     */
    public MRCQuotaManager() {
    }

    public void loadVolumeQuotaManager() {
        // TODO(baerhold): load from DB
    }

    public void addVolumeQuotaManager(VolumeQuotaManager volumeQuotaManager) throws Exception {

        String volumeId = volumeQuotaManager.getVolumeId();
        if (!volQuotaManMap.containsKey(volumeId)) {

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Registered new VolumeQuotaManager with volumeId: "
                    + volumeId);

            volQuotaManMap.put(volumeId, volumeQuotaManager);
        } else {
            throw new Exception("There's already a " + VolumeQuotaManager.class.getName() + " registered for the Id "
                    + volumeId);
        }
    }

    public VolumeQuotaManager getVolumeQuotaManagerById(String volumeId) {
        return volQuotaManMap.get(volumeId);
    }

    public boolean hasActiveVolumeQuotaManager(String volumeName) {
        VolumeQuotaManager volumeQuotaManager = getVolumeQuotaManagerById(volumeName);
        return (volumeQuotaManager == null) ? false : volumeQuotaManager.isActive();
    }

    public void removeVolumeQuotaManager(String volumeName) {
        volQuotaManMap.remove(volumeName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MRCQuotaManager [volQuotaManMap=" + volQuotaManMap + "]";
    }
}
