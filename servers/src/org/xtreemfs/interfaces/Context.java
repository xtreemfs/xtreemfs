package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class Context implements org.xtreemfs.interfaces.utils.Serializable
{
    public Context() { user_id = ""; group_ids = new org.xtreemfs.interfaces.StringSet(); }
    public Context( String user_id, StringSet group_ids ) { this.user_id = user_id; this.group_ids = group_ids; }

    public String getUser_id() { return user_id; }
    public void setUser_id( String user_id ) { this.user_id = user_id; }
    public StringSet getGroup_ids() { return group_ids; }
    public void setGroup_ids( StringSet group_ids ) { this.group_ids = group_ids; }

    // Object
    public String toString()
    {
        return "Context( " + "\"" + user_id + "\"" + ", " + group_ids.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::Context"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(user_id,writer); }
        group_ids.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { user_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        group_ids = new org.xtreemfs.interfaces.StringSet(); group_ids.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( user_id.length() + 4 - ( user_id.length() % 4 ) );
        my_size += group_ids.calculateSize();
        return my_size;
    }

    private String user_id;
    private StringSet group_ids;

}

