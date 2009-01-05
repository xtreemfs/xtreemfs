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
