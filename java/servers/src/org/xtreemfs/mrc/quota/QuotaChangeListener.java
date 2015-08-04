/*
 * Copyright (c) 2015 by rob, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

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
        } catch (DatabaseException e) {
            // FIXME(baerhold): Handle -> Debug
            e.printStackTrace();
        }
    }

    @Override
    public void volumeDeleted(String volumeId) {
        volumeQuotaManager.setActive(false);
    }

    @Override
    public void attributeSet(String volumeId, String key, String value) {
        // TODO Auto-generated method stub: check, when it will be called
    }

}
