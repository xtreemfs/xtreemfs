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
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;

/** TODO: Brief description of the purpose of this type and its relation to other types. */
public class MRCQuotaManager {

    private final Map<String, VolumeQuotaManager> volQuotaManMap = new HashMap<String, VolumeQuotaManager>();

    /**
     * 
     */
    public MRCQuotaManager() {
    }

    /**
     * Loads all existing volume quota manager from the database and initiates them.
     * 
     * @param volumeManager
     * @throws Exception
     */
    public void initializeVolumeQuotaManager(VolumeManager volumeManager) throws Exception {
        for (StorageManager storageManager : volumeManager.getStorageManagers()) {

            VolumeQuotaManager volumeQuotaManager = new VolumeQuotaManager(this, storageManager, storageManager
                    .getVolumeInfo().getId());
            volumeQuotaManager.init();
        }
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

    public void removeVolumeQuotaManager(String volumeId) {
        volQuotaManMap.remove(volumeId);
    }

    @Override
    public String toString() {
        return "MRCQuotaManager [volQuotaManMap=" + volQuotaManMap + "]";
    }
}
