package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class stat_ implements Serializable
{
    public stat_() { mode = 0; nlink = 0; uid = 0; gid = 0; dev = 0; size = 0; atime = 0; mtime = 0; ctime = 0; attributes = 0; }
    public stat_( long mode, long nlink, long uid, long gid, long dev, long size, long atime, long mtime, long ctime, long attributes ) { this.mode = mode; this.nlink = nlink; this.uid = uid; this.gid = gid; this.dev = dev; this.size = size; this.atime = atime; this.mtime = mtime; this.ctime = ctime; this.attributes = attributes; }


    // Object
    public String toString()
    {
        return "stat_( " + Long.toString( mode ) + ", " + Long.toString( nlink ) + ", " + Long.toString( uid ) + ", " + Long.toString( gid ) + ", " + Long.toString( dev ) + ", " + Long.toString( size ) + ", " + Long.toString( atime ) + ", " + Long.toString( mtime ) + ", " + Long.toString( ctime ) + ", " + Long.toString( attributes ) + " )";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putLong( mode );
        writer.putLong( nlink );
        writer.putLong( uid );
        writer.putLong( gid );
        writer.putLong( dev );
        writer.putLong( size );
        writer.putLong( atime );
        writer.putLong( mtime );
        writer.putLong( ctime );
        writer.putLong( attributes );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        mode = buf.getLong();
        nlink = buf.getLong();
        uid = buf.getLong();
        gid = buf.getLong();
        dev = buf.getLong();
        size = buf.getLong();
        atime = buf.getLong();
        mtime = buf.getLong();
        ctime = buf.getLong();
        attributes = buf.getLong();    
    }
    
    public int getSize()
    {
        int my_size = 0;
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    public long mode;
    public long nlink;
    public long uid;
    public long gid;
    public long dev;
    public long size;
    public long atime;
    public long mtime;
    public long ctime;
    public long attributes;

}

