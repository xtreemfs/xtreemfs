/*
 * Copyright (c) 2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.database.babudb;

import java.net.InetSocketAddress;
import java.util.StringTokenizer;

import org.xtreemfs.babudb.api.DatabaseManager;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.babudb.api.exception.BabuDBException.ErrorCode;
import org.xtreemfs.babudb.api.transaction.Transaction;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;

public class TransactionalBabuDBUpdate implements AtomicDBUpdate {

    private final DatabaseManager dbMan;

    private final Transaction     txn;

    private String                databaseName;

    public TransactionalBabuDBUpdate(DatabaseManager dbMan) {
        this.dbMan = dbMan;
        this.txn = dbMan.createTransaction();
    }

    public void createDatabase(String databaseName, int numIndices) {
        this.databaseName = databaseName;
        txn.createDatabase(databaseName, numIndices);
    }

    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public void addUpdate(Object... update) {
        assert (databaseName != null);
        txn.insertRecord(databaseName, (Integer) update[0], (byte[]) update[1], (byte[]) update[2]);
    }

    @Override
    public void execute() throws DatabaseException {
        
        try {
            dbMan.executeTransaction(txn);
        } catch (BabuDBException exc) {
            
            // handle REDIRECTs (only relevant if replication is enabled)
            if (exc.getErrorCode() == ErrorCode.REDIRECT) {
                StringTokenizer st = new StringTokenizer(exc.getMessage(), ": ");
                InetSocketAddress target = new InetSocketAddress(st.nextToken(), Integer.parseInt(st.nextToken()));
                throw new DatabaseException(ExceptionType.REDIRECT, target);
            
            } else
                throw new DatabaseException(exc);
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }

    public String toString() {
        return txn.toString();
    }

}
