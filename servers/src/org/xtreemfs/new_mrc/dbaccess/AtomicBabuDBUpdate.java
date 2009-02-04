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
package org.xtreemfs.new_mrc.dbaccess;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBInsertGroup;
import org.xtreemfs.babudb.BabuDBRequestListener;

public class AtomicBabuDBUpdate implements AtomicDBUpdate {
    
    private BabuDBInsertGroup     ig;
    
    private BabuDB                database;
    
    private BabuDBRequestListener listener;
    
    private Object                context;
    
    public AtomicBabuDBUpdate(BabuDB database, String dbName, BabuDBRequestListener listener,
        Object context) throws BabuDBException {
        
        ig = database.createInsertGroup(dbName);
        
        this.database = database;
        this.listener = listener;
        this.context = context;
    }
    
    @Override
    public void addUpdate(Object... update) {
        ig.addInsert((Integer) update[0], (byte[]) update[1], (byte[]) update[2]);
    }
    
    @Override
    public void execute() throws DatabaseException {
        try {
            if (listener != null)
                database.asyncInsert(ig, listener, context);
            else
                database.syncInsert(ig);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public String toString() {
        return ig.toString();
    }
    
}
