package org.xtreemfs.new_mrc.dbaccess;

import java.util.Iterator;
import java.util.Map.Entry;

public interface DBAccessResultListener {
    
    public void insertFinished(Object context);
    
    public void lookupFinished(Object context, byte[] value);
    
    public void prefixLookupFinished(Object context, Iterator<Entry<byte[], byte[]>> iterator);
    
    public void requestFailed(Object context, Throwable error);
    
}
