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
import org.xtreemfs.babudb.api.database.DatabaseRequestListener;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;

public class AtomicBabuDBUpdate implements AtomicDBUpdate {
    
    private DatabaseInsertGroup             ig;
    
    private Database                        database;
    
    private DatabaseRequestListener<Object> listener;
    
    private Object                          context;
    
    // private List<Object[]> updates;
    //    
    // private String dbName;
    
    public AtomicBabuDBUpdate(Database database, DatabaseRequestListener<Object> listener, Object context)
        throws BabuDBException {
        
        ig = database.createInsertGroup();
        
        this.database = database;
        this.listener = listener;
        this.context = context;
        
        // updates = new LinkedList<Object[]>();
        // this.dbName = dbName;
    }
    
    @Override
    public void addUpdate(Object... update) {
        ig.addInsert((Integer) update[0], (byte[]) update[1], (byte[]) update[2]);
        // updates.add(update);
    }
    
    @Override
    public void execute() throws DatabaseException {
        try {
            
            // checkDBConsistency();
            
            if (listener != null) {
                database.insert(ig, context).registerListener(listener);
            } else
                database.insert(ig, context).get();
            
        } catch (Exception exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public String toString() {
        return ig.toString();
    }
    
    // private void checkDBConsistency() {
    //        
    // Map<String, byte[][]> prefixEntries = new HashMap<String, byte[][]>();
    //        
    // for (Object[] update : updates) {
    // if ((Integer) update[0] == BabuDBStorageManager.FILE_INDEX) {
    //                
    // byte[] bytes = (byte[]) update[1];
    // byte type = bytes[bytes.length - 1];
    //                
    // byte[] prefix = new byte[bytes.length - 1];
    // System.arraycopy(bytes, 0, prefix, 0, prefix.length);
    //                
    // byte[][] entry = prefixEntries.get(new String(prefix));
    // if (entry == null) {
    //                    
    // entry = new byte[3][];
    // prefixEntries.put(new String(prefix), entry);
    //                    
    // Iterator<Entry<byte[], byte[]>> entries = null;
    // try {
    // entries = database
    // .directPrefixLookup(dbName, BabuDBStorageManager.FILE_INDEX, prefix);
    // } catch (BabuDBException e) {
    // e.printStackTrace();
    // System.exit(1);
    // }
    //                    
    // while (entries.hasNext()) {
    // byte[] key = entries.next().getKey();
    // byte entryType = key[key.length - 1];
    // entry[entryType] = key;
    // }
    //                    
    // }
    //                
    // // do the update in memory
    // entry[type] = update[2] == null ? null : (byte[]) update[1];
    // }
    // }
    //        
    // // check the data structure
    // for (byte[][] entry : prefixEntries.values()) {
    // if ((entry[0] != null || entry[2] != null) && entry[1] == null) {
    // System.err.println("CORRUPTED DATABASE!");
    // System.err.println("updated entry:");
    // for (int i = 0; i < entry.length; i++)
    // System.out.println(i + ": " + Arrays.toString(entry[i]));
    //                
    // System.err.println("all updates:");
    // for (Object[] update : updates) {
    // System.out.println(update[0] + ": " + Arrays.toString((byte[]) update[1])
    // + " = "
    // + Arrays.toString((byte[]) update[2]));
    // }
    //                
    // try {
    // throw new Exception();
    // } catch (Exception exc) {
    // exc.printStackTrace();
    // }
    //                
    // System.exit(1);
    // }
    // }
    //        
    // }
    
}
