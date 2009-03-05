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
    public DirectoryEntry() { entry_name = ""; stbuf = new org.xtreemfs.interfaces.stat_(); link_target = ""; }
    public DirectoryEntry( String entry_name, stat_ stbuf, String link_target ) { this.entry_name = entry_name; this.stbuf = stbuf; this.link_target = link_target; }

    public String getEntry_name() { return entry_name; }
    public void setEntry_name( String entry_name ) { this.entry_name = entry_name; }
    public stat_ getStbuf() { return stbuf; }
    public void setStbuf( stat_ stbuf ) { this.stbuf = stbuf; }
    public String getLink_target() { return link_target; }
    public void setLink_target( String link_target ) { this.link_target = link_target; }

    // Object
    public String toString()
    {
        return "DirectoryEntry( " + "\"" + entry_name + "\"" + ", " + stbuf.toString() + ", " + "\"" + link_target + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::DirectoryEntry"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(entry_name,writer); }
        stbuf.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(link_target,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { entry_name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        stbuf = new org.xtreemfs.interfaces.stat_(); stbuf.deserialize( buf );
        { link_target = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(entry_name);
        my_size += stbuf.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(link_target);
        return my_size;
    }

    private String entry_name;
    private stat_ stbuf;
    private String link_target;

}

