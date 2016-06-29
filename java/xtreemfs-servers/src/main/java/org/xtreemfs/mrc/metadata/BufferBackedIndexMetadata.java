/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

import org.xtreemfs.foundation.util.OutputUtils;

public abstract class BufferBackedIndexMetadata {
    
    protected byte[] keyBuf;
    
    protected byte[] valBuf;
    
    protected int    keyOffs;
    
    protected int    keyLen;
    
    protected int    valOffs;
    
    protected int    valLen;
    
    protected BufferBackedIndexMetadata(byte[] keyBuf, int keyOffs, int keyLen, byte[] valBuf,
        int valOffs, int valLen) {
        this.keyBuf = keyBuf;
        this.keyOffs = keyOffs;
        this.valBuf = valBuf;
        this.valOffs = valOffs;
        this.keyLen = keyLen;
        this.valLen = valLen;
    }
    
    public byte[] getKeyBuf() {
        return keyBuf;
    }
    
    public int getKeyOffs() {
        return keyOffs;
    }
    
    public int getKeyLen() {
        return keyLen;
    }
    
    public byte[] getValBuf() {
        return valBuf;
    }
    
    public int getValOffs() {
        return valOffs;
    }
    
    public int getValLen() {
        return valLen;
    }
    
    public String toString() {
        return "key: " + OutputUtils.byteArrayToFormattedHexString(keyBuf) + ", val: "
            + OutputUtils.byteArrayToFormattedHexString(valBuf);
    }
    
}
