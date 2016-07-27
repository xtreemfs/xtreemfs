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
import org.xtreemfs.mrc.database.DBAccessResultListener;

/**
 * @author stender
 * @parma <T>
 */
public class BabuDBRequestListenerWrapper<T> implements DatabaseRequestListener<T> {
    
    private DBAccessResultListener<T> listener;
    
    public BabuDBRequestListenerWrapper(DBAccessResultListener<T> listener) {
        this.listener = listener;
    }

    /*
     * (non-Javadoc)
     * @see org.xtreemfs.babudb.BabuDBRequestListener#failed(org.xtreemfs.babudb.BabuDBException, java.lang.Object)
     */
    @Override
    public void failed(BabuDBException arg0, Object arg1) {
        listener.failed(arg0,arg1);
    }

    /*
     * (non-Javadoc)
     * @see org.xtreemfs.babudb.BabuDBRequestListener#finished(java.lang.Object, java.lang.Object)
     */
    @Override
    public void finished(T arg0, Object arg1) {
        listener.finished(arg0, arg1);
    }
}
