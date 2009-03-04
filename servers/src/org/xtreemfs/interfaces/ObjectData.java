package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class ObjectData implements org.xtreemfs.interfaces.utils.Serializable
{
    public ObjectData() { checksum = ""; zero_padding = 0; data = null; }
    public ObjectData( String checksum, int zero_padding, ReusableBuffer data ) { this.checksum = checksum; this.zero_padding = zero_padding; this.data = data; }

    public String getChecksum() { return checksum; }
    public void setChecksum( String checksum ) { this.checksum = checksum; }
    public int getZero_padding() { return zero_padding; }
    public void setZero_padding( int zero_padding ) { this.zero_padding = zero_padding; }
    public ReusableBuffer getData() { return data; }
    public void setData( ReusableBuffer data ) { this.data = data; }

    // Object
    public String toString()
    {
        return "ObjectData( " + "\"" + checksum + "\"" + ", " + Integer.toString( zero_padding ) + ", " + "\"" + data + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::ObjectData"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(checksum,writer); }
        writer.putInt( zero_padding );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeSerializableBuffer(data,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { checksum = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        zero_padding = buf.getInt();
        { data = org.xtreemfs.interfaces.utils.XDRUtils.deserializeSerializableBuffer(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( checksum.length() + 4 - ( checksum.length() % 4 ) );
        my_size += ( Integer.SIZE / 8 );
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.serializableBufferLength(data);
        return my_size;
    }

    private String checksum;
    private int zero_padding;
    private ReusableBuffer data;

}

