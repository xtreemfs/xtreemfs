/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */
package org.xtreemfs.new_mrc.database.babudb;

import java.util.Iterator;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBRequestListener;
import org.xtreemfs.new_mrc.database.DBAccessResultListener;

/**
 * @author stender
 *
 */
public class BabuDBRequestListenerWrapper implements BabuDBRequestListener {
    
    private DBAccessResultListener listener;
    
    public BabuDBRequestListenerWrapper(DBAccessResultListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void insertFinished(Object context) {
        listener.insertFinished(context);
    }
    
    @Override
    public void lookupFinished(Object context, byte[] value) {
        listener.lookupFinished(context, value);
    }
    
    @Override
    public void prefixLookupFinished(Object context, Iterator<Entry<byte[], byte[]>> iterator) {
        listener.prefixLookupFinished(context, iterator);
    }
    
    @Override
    public void requestFailed(Object context, BabuDBException error) {
        listener.requestFailed(context, error);
    }

    @Override
    public void userDefinedLookupFinished(Object context, Object result) {
        listener.userDefinedLookupFinished(context, result);
    }
    
}
