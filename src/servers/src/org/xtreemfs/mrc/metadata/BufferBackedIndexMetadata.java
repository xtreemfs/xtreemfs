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

package org.xtreemfs.mrc.metadata;

import org.xtreemfs.common.util.OutputUtils;

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
