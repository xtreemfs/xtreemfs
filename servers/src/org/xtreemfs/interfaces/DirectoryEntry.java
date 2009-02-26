package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class DirectoryEntry implements org.xtreemfs.interfaces.utils.Serializable
{
    public DirectoryEntry() { path = ""; stbuf = new org.xtreemfs.interfaces.stat_(); }
    public DirectoryEntry( String path, stat_ stbuf ) { this.path = path; this.stbuf = stbuf; }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public stat_ getStbuf() { return stbuf; }
    public void setStbuf( stat_ stbuf ) { this.stbuf = stbuf; }

    // Object
    public String toString()
    {
        return "DirectoryEntry( " + "\"" + path + "\"" + ", " + stbuf.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::DirectoryEntry"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(path,writer); }
        stbuf.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        stbuf = new org.xtreemfs.interfaces.stat_(); stbuf.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( path.length() + 4 - ( path.length() % 4 ) );
        my_size += stbuf.calculateSize();
        return my_size;
    }

    private String path;
    private stat_ stbuf;

}

