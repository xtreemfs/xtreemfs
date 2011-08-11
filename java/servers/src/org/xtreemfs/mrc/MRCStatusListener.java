package org.xtreemfs.mrc;

import java.util.Map;

import org.xtreemfs.mrc.database.VolumeInfo;

public interface MRCStatusListener {
    public void MRCConfigChanged(MRCConfig config);
    
    public void volumeCreated();
    
    public void volumeDeleted();
    
}
