package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class NewFileSize implements org.xtreemfs.interfaces.utils.Serializable
{
    public NewFileSize() { size_in_bytes = 0; truncate_epoch = 0; }
    public NewFileSize( long size_in_bytes, int truncate_epoch ) { this.size_in_bytes = size_in_bytes; this.truncate_epoch = truncate_epoch; }

    public long getSize_in_bytes() { return size_in_bytes; }
    public void setSize_in_bytes( long size_in_bytes ) { this.size_in_bytes = size_in_bytes; }
    public int getTruncate_epoch() { return truncate_epoch; }
    public void setTruncate_epoch( int truncate_epoch ) { this.truncate_epoch = truncate_epoch; }

    // Object
    public String toString()
    {
        return "NewFileSize( " + Long.toString( size_in_bytes ) + ", " + Integer.toString( truncate_epoch ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::NewFileSize"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putLong( size_in_bytes );
        writer.putInt( truncate_epoch );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        size_in_bytes = buf.getLong();
        truncate_epoch = buf.getInt();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Long.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    private long size_in_bytes;
    private int truncate_epoch;

}

