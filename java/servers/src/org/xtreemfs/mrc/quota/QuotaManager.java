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
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;

/** TODO: Brief description of the purpose of this type and its relation to other types. */
public class QuotaManager {

    private final Map<String, VolumeQuotaManager> volQuotaManMap = new HashMap<String, VolumeQuotaManager>();

    /**
     * 
     */
    public QuotaManager() {
    }

    /**
     * Loads all existing volume quota manager from the database and initiates them.
     * 
     * @param volumeManager
     * @throws MRCException
     */
    public void initializeVolumeQuotaManager(VolumeManager volumeManager) throws MRCException {
        for (StorageManager storageManager : volumeManager.getStorageManagers()) {

            VolumeQuotaManager volumeQuotaManager = new VolumeQuotaManager(this, storageManager, storageManager
                    .getVolumeInfo().getId());
            volumeQuotaManager.init();
        }
    }

    public void addVolumeQuotaManager(VolumeQuotaManager volumeQuotaManager) throws MRCException {

        String volumeId = volumeQuotaManager.getVolumeId();
        if (!volQuotaManMap.containsKey(volumeId)) {

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Registered new VolumeQuotaManager with volumeId: "
                    + volumeId);

            volQuotaManMap.put(volumeId, volumeQuotaManager);
        } else {
            throw new MRCException("There's already a VolumeQuotaManager registered for the volumeId " + volumeId);
        }
    }

    public VolumeQuotaManager getVolumeQuotaManagerById(String volumeId) throws MRCException {

        if (volQuotaManMap.containsKey(volumeId)) {
            return volQuotaManMap.get(volumeId);
        } else {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Return VolumeQuotaManager for volumeId: " + volumeId);
            throw new MRCException("There's no VolumeQuotaManager registered for the volumeId " + volumeId);
        }
    }

    public void removeVolumeQuotaManager(VolumeQuotaManager volumeQuotaManager) throws MRCException {

        String volumeId = volumeQuotaManager.getVolumeId();
        if (volQuotaManMap.containsKey(volumeId)) {

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Unregister VolumeQuotaManager with volumeId: " + volumeId);

            volQuotaManMap.remove(volumeId);
        } else {
            throw new MRCException("There's no VolumeQuotaManager registered for the volumeId " + volumeId);
        }
    }

    @Override
    public String toString() {
        return "MRCQuotaManager [volQuotaManMap=" + volQuotaManMap + "]";
    }
}
