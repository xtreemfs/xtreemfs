/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database.babudb;

import org.xtreemfs.babudb.api.database.DatabaseRequestListener;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;

public class AtomicBabuDBSnapshotUpdate implements AtomicDBUpdate {
    
    private DatabaseRequestListener<Object> listener;
    
    private Object                context;
    
    public AtomicBabuDBSnapshotUpdate(DatabaseRequestListener<Object> listener, Object context)
        throws BabuDBException {
        
        this.listener = listener;
        this.context = context;
    }
    
    @Override
    public void addUpdate(Object... update) {
    }
    
    @Override
    public void execute() throws DatabaseException {
        if (listener != null)
            listener.finished(null, context);
    }
    
}
