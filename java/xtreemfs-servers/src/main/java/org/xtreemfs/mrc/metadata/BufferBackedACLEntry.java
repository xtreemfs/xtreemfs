/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;

public class BufferBackedACLEntry extends BufferBackedIndexMetadata implements ACLEntry {
    
    private String entity;
    
    private short  rights;
    
    public BufferBackedACLEntry(byte[] key, byte[] val) {
        
        super(key, 0, key.length, val, 0, val.length);

        ByteBuffer tmp = ByteBuffer.wrap(val);
        entity = new String(key, 8, key.length - 8);
        rights = tmp.getShort(0);
    }
    
    public BufferBackedACLEntry(long fileId, String entity, short rights) {
        
        super(null, 0, 0, null, 0, 0);
        
        keyLen = 8 + entity.getBytes().length;
        keyBuf = new byte[keyLen];
                       
        ByteBuffer tmp = ByteBuffer.wrap(keyBuf);
        tmp.putLong(fileId);
        tmp.put(entity.getBytes());
        
        valLen = 2;
        valBuf = new byte[valLen];
        tmp = ByteBuffer.wrap(valBuf);
        tmp.putShort(rights);
        
        this.entity = entity;
        this.rights = rights;
    }
    
    @Override
    public String getEntity() {
        return entity;
    }
    
    @Override
    public short getRights() {
        return rights;
    }
    
}
