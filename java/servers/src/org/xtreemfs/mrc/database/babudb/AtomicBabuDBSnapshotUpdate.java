/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database.babudb;

import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.database.AtomicDBUpdate;

public class AtomicBabuDBSnapshotUpdate implements AtomicDBUpdate {
    
    @Override
    public void addUpdate(Object... update) {
    }
    
    @Override
    public void execute(Callback callback, MRCRequest request) {
        
        try {
            callback.success(null, null);
        } catch (Exception e) {
            callback.failed(e);
        }
    }    
}