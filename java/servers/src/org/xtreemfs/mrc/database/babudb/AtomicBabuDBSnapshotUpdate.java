/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database.babudb;

import org.xtreemfs.common.olp.RequestMetadata;
import org.xtreemfs.common.stage.BabuDBPostprocessing;
import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.mrc.database.AtomicDBUpdate;

public class AtomicBabuDBSnapshotUpdate implements AtomicDBUpdate {
    
    private BabuDBPostprocessing<Object> postprocessing;
    
    public AtomicBabuDBSnapshotUpdate(BabuDBPostprocessing<Object> postprocessing) {
        
        this.postprocessing = postprocessing;
    }
    
    @Override
    public void addUpdate(Object... update) {
    }
    
    @Override
    public void execute(Callback callback, RequestMetadata metadata) {
        
        try {
            callback.success(postprocessing.execute(null, null));
        } catch (Exception e) {
            callback.failed(e);
        }
    }
    
}
