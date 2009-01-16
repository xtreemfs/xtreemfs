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

package org.xtreemfs.new_mrc.metadata;

import java.nio.ByteBuffer;

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
    
    public BufferBackedXAttr(long fileId, String owner, String key, String value, short collCount) {
        
        super(null, 0, 0, null, 0, 0);
        
        byte[] ownerBytes = owner.getBytes();
        byte[] keyBytes = key.getBytes();
        byte[] valBytes = value == null ? new byte[0] : value.getBytes();
        
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
    
    public String getValue() {
        return new String(valBuf, valOffset, valBuf.length - valOffset);
    }
    
    public void setCollisionNumber(short collisionNumber) {
        ByteBuffer tmp = ByteBuffer.wrap(keyBuf);
        tmp.putShort(16, collisionNumber);
    }
    
    public String toString() {
        return "(" + getKey() + " = " + getValue() + " [" + getOwner() + "])";
    }
    
}
