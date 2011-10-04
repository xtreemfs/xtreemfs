/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database;

import org.xtreemfs.common.stage.Callback;
import org.xtreemfs.mrc.MRCRequest;

/**
 * Defines a collection of database updates that have to be executed atomically.
 * 
 * @author stender
 * 
 */
public interface AtomicDBUpdate {
    
    /**
     * Adds a new update to the collection.
     * 
     * @param update
     *            an array of objects describing the update
     */
    public void addUpdate(Object... update);
    
    /**
     * Atomically executes all updates.
     * 
     * @param callback - for the request.
     * @param request - request relevant meta-information.
     * 
     * @throws DatabaseException
     */
    public void execute(Callback callback, MRCRequest request) throws DatabaseException;
    
}
