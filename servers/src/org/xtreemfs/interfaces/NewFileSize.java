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
    public NewFileSize() { sizeInBytes = 0; epoch = 0; __json = ""; }
    public NewFileSize( long sizeInBytes, long epoch, String __json ) { this.sizeInBytes = sizeInBytes; this.epoch = epoch; this.__json = __json; }

    public long getSizeInBytes() { return sizeInBytes; }
    public void setSizeInBytes( long sizeInBytes ) { this.sizeInBytes = sizeInBytes; }
    public long getEpoch() { return epoch; }
    public void setEpoch( long epoch ) { this.epoch = epoch; }
    public String get__json() { return __json; }
    public void set__json( String __json ) { this.__json = __json; }

    // Object
    public String toString()
    {
        return "NewFileSize( " + Long.toString( sizeInBytes ) + ", " + Long.toString( epoch ) + ", " + "\"" + __json + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::NewFileSize"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putLong( sizeInBytes );
        writer.putLong( epoch );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(__json,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        sizeInBytes = buf.getLong();
        epoch = buf.getLong();
        { __json = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += 4 + ( __json.length() + 4 - ( __json.length() % 4 ) );
        return my_size;
    }

    private long sizeInBytes;
    private long epoch;
    private String __json;

}

