package org.xtreemfs.interfaces;

import org.xtreemfs.interfaces.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         
   
public class stat_ implements org.xtreemfs.interfaces.utils.Serializable
{
    public stat_() { mode = 0; nlink = 0; uid = 0; gid = 0; dev = 0; size = 0; atime = 0; mtime = 0; ctime = 0; attributes = 0; }
    public stat_( int mode, int nlink, int uid, int gid, int dev, long size, long atime, long mtime, long ctime, int attributes ) { this.mode = mode; this.nlink = nlink; this.uid = uid; this.gid = gid; this.dev = dev; this.size = size; this.atime = atime; this.mtime = mtime; this.ctime = ctime; this.attributes = attributes; }

    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }
    public int getNlink() { return nlink; }
    public void setNlink( int nlink ) { this.nlink = nlink; }
    public int getUid() { return uid; }
    public void setUid( int uid ) { this.uid = uid; }
    public int getGid() { return gid; }
    public void setGid( int gid ) { this.gid = gid; }
    public int getDev() { return dev; }
    public void setDev( int dev ) { this.dev = dev; }
    public long getSize() { return size; }
    public void setSize( long size ) { this.size = size; }
    public long getAtime() { return atime; }
    public void setAtime( long atime ) { this.atime = atime; }
    public long getMtime() { return mtime; }
    public void setMtime( long mtime ) { this.mtime = mtime; }
    public long getCtime() { return ctime; }
    public void setCtime( long ctime ) { this.ctime = ctime; }
    public int getAttributes() { return attributes; }
    public void setAttributes( int attributes ) { this.attributes = attributes; }

    // Object
    public String toString()
    {
        return "stat_( " + Integer.toString( mode ) + ", " + Integer.toString( nlink ) + ", " + Integer.toString( uid ) + ", " + Integer.toString( gid ) + ", " + Integer.toString( dev ) + ", " + Long.toString( size ) + ", " + Long.toString( atime ) + ", " + Long.toString( mtime ) + ", " + Long.toString( ctime ) + ", " + Integer.toString( attributes ) + " )";
    }    

    // Serializable
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt( mode );
        writer.putInt( nlink );
        writer.putInt( uid );
        writer.putInt( gid );
        writer.putInt( dev );
        writer.putLong( size );
        writer.putLong( atime );
        writer.putLong( mtime );
        writer.putLong( ctime );
        writer.putInt( attributes );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        mode = buf.getInt();
        nlink = buf.getInt();
        uid = buf.getInt();
        gid = buf.getInt();
        dev = buf.getInt();
        size = buf.getLong();
        atime = buf.getLong();
        mtime = buf.getLong();
        ctime = buf.getLong();
        attributes = buf.getInt();    
    }
    
    public int getSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    private int mode;
    private int nlink;
    private int uid;
    private int gid;
    private int dev;
    private long size;
    private long atime;
    private long mtime;
    private long ctime;
    private int attributes;

}

