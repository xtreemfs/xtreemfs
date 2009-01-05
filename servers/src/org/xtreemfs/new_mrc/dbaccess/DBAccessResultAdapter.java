package org.xtreemfs.new_mrc.dbaccess;

import java.util.Iterator;
import java.util.Map.Entry;

public class DBAccessResultAdapter implements DBAccessResultListener {
    
    @Override
    public void insertFinished(Object context) {
    }
    
    @Override
    public void lookupFinished(Object context, byte[] value) {
    }
    
    @Override
    public void prefixLookupFinished(Object context, Iterator<Entry<byte[], byte[]>> iterator) {
    }
    
    @Override
    public void requestFailed(Object context, Throwable error) {
    }
    
}
