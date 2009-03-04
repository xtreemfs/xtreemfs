package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class OSDtoMRCData implements org.xtreemfs.interfaces.utils.Serializable
{
    public OSDtoMRCData() { caching_policy = 0; data = ""; }
    public OSDtoMRCData( int caching_policy, String data ) { this.caching_policy = caching_policy; this.data = data; }

    public int getCaching_policy() { return caching_policy; }
    public void setCaching_policy( int caching_policy ) { this.caching_policy = caching_policy; }
    public String getData() { return data; }
    public void setData( String data ) { this.data = data; }

    // Object
    public String toString()
    {
        return "OSDtoMRCData( " + Integer.toString( caching_policy ) + ", " + "\"" + data + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::OSDtoMRCData"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt( caching_policy );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(data,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        caching_policy = buf.getInt();
        { data = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += 4 + ( data.length() + 4 - ( data.length() % 4 ) );
        return my_size;
    }

    private int caching_policy;
    private String data;

}

