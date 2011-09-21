/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database.babudb;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.DatabaseInsertGroup;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.common.olp.RequestMetadata;
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.BabuDBPostprocessing;
import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;

public class AtomicBabuDBUpdate implements AtomicDBUpdate {
    
    private DatabaseInsertGroup             ig;
    
    private BabuDBComponent                 component;
    
    private Database                        database;
    
    private BabuDBPostprocessing<Object>    postprocessing;
        
    public AtomicBabuDBUpdate(Database database, BabuDBComponent component, BabuDBPostprocessing<Object> postprocessing) {
        
        assert (postprocessing != null);
        
        ig = database.createInsertGroup();
        
        this.database = database;
        this.component = component;
        this.postprocessing = postprocessing;
    }
    
    @Override
    public void addUpdate(Object... update) {
        ig.addInsert((Integer) update[0], (byte[]) update[1], (byte[]) update[2]);
    }
    
    @Override
    public void execute(Callback callback, RequestMetadata metadata) throws DatabaseException {
        
        if (callback != null) {
            component.insert(database, callback, ig, metadata, postprocessing);        
        } else {
            try {
                database.insert(ig, null).get();
            } catch (BabuDBException e) {
                throw new DatabaseException(e);
            }
        }
    }
    
    public String toString() {
        return ig.toString();
    }
}
