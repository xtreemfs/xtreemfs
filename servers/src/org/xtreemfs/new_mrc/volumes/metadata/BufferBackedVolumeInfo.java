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

package org.xtreemfs.new_mrc.volumes.metadata;

import java.nio.ByteBuffer;

/**
 * A <code>VolumeInfo</code> implementation backed by a byte buffer.
 * 
 * @author stender
 * 
 */
public class BufferBackedVolumeInfo implements VolumeInfo {
    
    private ByteBuffer buf;
    
    public BufferBackedVolumeInfo(byte[] buf) {
        this.buf = ByteBuffer.wrap(buf);
    }
    
    public BufferBackedVolumeInfo(String id, String name, short fileAccessPolicyId,
        short osdPolicyId, String osdPolicyArgs) {
        
        final byte[] idBytes = id.getBytes();
        final byte[] nameBytes = name.getBytes();
        final byte[] osdPolArgsBytes = osdPolicyArgs.getBytes();
        
        byte[] tmp = new byte[8 + idBytes.length + nameBytes.length + osdPolArgsBytes.length];
        buf = ByteBuffer.wrap(tmp);
        buf.putShort(fileAccessPolicyId).putShort(osdPolicyId).putShort(
            (short) (4 + idBytes.length)).putShort((short) (4 + idBytes.length + nameBytes.length))
                .put(idBytes).put(nameBytes).put(osdPolArgsBytes);
    }
    
    public String getId() {
        byte[] bytes = buf.array();
        return new String(bytes, 4, buf.getShort(4) - 4);
    }
    
    public String getName() {
        byte[] bytes = buf.array();
        short offs = buf.getShort(4);
        return new String(bytes, offs, buf.getShort(6) - offs);
    }
    
    public short getOsdPolicyId() {
        return buf.getShort(2);
    }
    
    public String getOsdPolicyArgs() {
        byte[] bytes = buf.array();
        short offs = buf.getShort(6);
        return new String(bytes, offs, buf.limit() - offs);
    }
    
    public short getAcPolicyId() {
        return buf.getShort(0);
    }
    
    public byte[] getBuffer() {
        return buf.array();
    }
    
}
