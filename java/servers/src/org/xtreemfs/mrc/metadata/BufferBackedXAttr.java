/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BufferBackedXAttr extends BufferBackedIndexMetadata implements XAttr {
    
    private final short ownerOffset;
    
    private final short keyOffset;
    
    private final short valOffset;
    
    public BufferBackedXAttr(byte[] key, byte[] val) {
        
        super(key, 0, key.length, val, 0, val.length);
        
        ByteBuffer tmp = ByteBuffer.wrap(val);
        
        ownerOffset = 4;
        keyOffset = tmp.getShort();
        valOffset = tmp.getShort();
    }
    
    public BufferBackedXAttr(long fileId, String owner, String key, byte[] value, short collCount) {
        
        super(null, 0, 0, null, 0, 0);
        
        byte[] ownerBytes = owner.getBytes();
        byte[] keyBytes = key.getBytes();
        byte[] valBytes = value == null ? new byte[0] : value;
        
        keyLen = 18;
        keyBuf = new byte[keyLen];
        ByteBuffer tmp = ByteBuffer.wrap(keyBuf);
        tmp.putLong(fileId).putInt(owner.hashCode()).putInt(key.hashCode()).putShort(collCount);
        
        ownerOffset = 4;
        keyOffset = (short) (4 + ownerBytes.length);
        valOffset = (short) (4 + ownerBytes.length + keyBytes.length);
        
        valLen = 4 + ownerBytes.length + keyBytes.length + valBytes.length;
        valBuf = new byte[valLen];
        tmp = ByteBuffer.wrap(valBuf).putShort(keyOffset).putShort(valOffset).put(ownerBytes).put(
            keyBytes).put(valBytes);
    }
    
    public String getKey() {
        return new String(valBuf, keyOffset, valOffset - keyOffset);
    }
    
    public String getOwner() {
        return new String(valBuf, ownerOffset, keyOffset - ownerOffset);
    }
    
    public byte[] getValue() {
        return Arrays.copyOfRange(valBuf, valOffset, valBuf.length);
    }
    
    public void setCollisionNumber(short collisionNumber) {
        ByteBuffer tmp = ByteBuffer.wrap(keyBuf);
        tmp.putShort(16, collisionNumber);
    }
    
    public String toString() {
        return "(" + getKey() + " = " + getValue() + " [" + getOwner() + "])";
    }
    
}
