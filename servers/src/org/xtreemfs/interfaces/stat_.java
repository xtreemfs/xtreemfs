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
    public stat_() { mode = 0; nlink = 0; uid = 0; gid = 0; dev = 0; size = 0; atime = 0; mtime = 0; ctime = 0; user_id = ""; group_id = ""; file_id = ""; link_target = ""; object_type = 0; truncate_epoch = 0; attributes = 0; }
    public stat_( int mode, int nlink, int uid, int gid, int dev, long size, long atime, long mtime, long ctime, String user_id, String group_id, String file_id, String link_target, int object_type, int truncate_epoch, int attributes ) { this.mode = mode; this.nlink = nlink; this.uid = uid; this.gid = gid; this.dev = dev; this.size = size; this.atime = atime; this.mtime = mtime; this.ctime = ctime; this.user_id = user_id; this.group_id = group_id; this.file_id = file_id; this.link_target = link_target; this.object_type = object_type; this.truncate_epoch = truncate_epoch; this.attributes = attributes; }

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
    public String getUser_id() { return user_id; }
    public void setUser_id( String user_id ) { this.user_id = user_id; }
    public String getGroup_id() { return group_id; }
    public void setGroup_id( String group_id ) { this.group_id = group_id; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public String getLink_target() { return link_target; }
    public void setLink_target( String link_target ) { this.link_target = link_target; }
    public int getObject_type() { return object_type; }
    public void setObject_type( int object_type ) { this.object_type = object_type; }
    public int getTruncate_epoch() { return truncate_epoch; }
    public void setTruncate_epoch( int truncate_epoch ) { this.truncate_epoch = truncate_epoch; }
    public int getAttributes() { return attributes; }
    public void setAttributes( int attributes ) { this.attributes = attributes; }

    // Object
    public String toString()
    {
        return "stat_( " + Integer.toString( mode ) + ", " + Integer.toString( nlink ) + ", " + Integer.toString( uid ) + ", " + Integer.toString( gid ) + ", " + Integer.toString( dev ) + ", " + Long.toString( size ) + ", " + Long.toString( atime ) + ", " + Long.toString( mtime ) + ", " + Long.toString( ctime ) + ", " + "\"" + user_id + "\"" + ", " + "\"" + group_id + "\"" + ", " + "\"" + file_id + "\"" + ", " + "\"" + link_target + "\"" + ", " + Integer.toString( object_type ) + ", " + Integer.toString( truncate_epoch ) + ", " + Integer.toString( attributes ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::stat_"; }    
    
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
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(user_id,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(group_id,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(file_id,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(link_target,writer); }
        writer.putInt( object_type );
        writer.putInt( truncate_epoch );
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
        { user_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { group_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { link_target = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        object_type = buf.getInt();
        truncate_epoch = buf.getInt();
        attributes = buf.getInt();    
    }
    
    public int calculateSize()
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
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(user_id);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(group_id);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(link_target);
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
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
    private String user_id;
    private String group_id;
    private String file_id;
    private String link_target;
    private int object_type;
    private int truncate_epoch;
    private int attributes;

}

