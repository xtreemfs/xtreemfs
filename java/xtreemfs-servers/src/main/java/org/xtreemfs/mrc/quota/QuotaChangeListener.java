/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.VolumeChangeListener;
import org.xtreemfs.mrc.database.VolumeInfo;

public class QuotaChangeListener implements VolumeChangeListener {

    private final VolumeQuotaManager volumeQuotaManager;

    public QuotaChangeListener(VolumeQuotaManager volumeQuotaManager) {
        this.volumeQuotaManager = volumeQuotaManager;
    }

    @Override
    public void volumeChanged(VolumeInfo vol) {

        try {
            volumeQuotaManager.setVolumeQuota(vol.getVolumeQuota());
            volumeQuotaManager.setVolumeVoucherSize(vol.getVoucherSize());
            volumeQuotaManager.setVolumeDefaultGroupQuota(vol.getDefaultGroupQuota());
            volumeQuotaManager.setVolumeDefaultUserQuota(vol.getDefaultUserQuota());
        } catch (DatabaseException e) {
            Logging.logError(Logging.LEVEL_ERROR, this, e);
        }
    }

    @Override
    public void volumeDeleted(String volumeId) {
        try {
            volumeQuotaManager.delete();
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_ERROR, this, e);
        }
    }

    @Override
    public void attributeSet(String volumeId, String key, String value) {
        // nothing to do
    }
}
