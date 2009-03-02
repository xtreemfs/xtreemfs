package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class InternalGmax implements org.xtreemfs.interfaces.utils.Serializable
{
    public InternalGmax() { epoch = 0; last_object_id = 0; file_size = 0; }
    public InternalGmax( long epoch, long last_object_id, long file_size ) { this.epoch = epoch; this.last_object_id = last_object_id; this.file_size = file_size; }

    public long getEpoch() { return epoch; }
    public void setEpoch( long epoch ) { this.epoch = epoch; }
    public long getLast_object_id() { return last_object_id; }
    public void setLast_object_id( long last_object_id ) { this.last_object_id = last_object_id; }
    public long getFile_size() { return file_size; }
    public void setFile_size( long file_size ) { this.file_size = file_size; }

    // Object
    public String toString()
    {
        return "InternalGmax( " + Long.toString( epoch ) + ", " + Long.toString( last_object_id ) + ", " + Long.toString( file_size ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::InternalGmax"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putLong( epoch );
        writer.putLong( last_object_id );
        writer.putLong( file_size );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        epoch = buf.getLong();
        last_object_id = buf.getLong();
        file_size = buf.getLong();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    private long epoch;
    private long last_object_id;
    private long file_size;

}

