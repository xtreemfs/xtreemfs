package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.MRCInterface.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class utimeRequest implements Request
{
    public utimeRequest() { context = new org.xtreemfs.interfaces.Context(); path = ""; ctime = 0; atime = 0; mtime = 0; }
    public utimeRequest( Context context, String path, long ctime, long atime, long mtime ) { this.context = context; this.path = path; this.ctime = ctime; this.atime = atime; this.mtime = mtime; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public long getCtime() { return ctime; }
    public void setCtime( long ctime ) { this.ctime = ctime; }
    public long getAtime() { return atime; }
    public void setAtime( long atime ) { this.atime = atime; }
    public long getMtime() { return mtime; }
    public void setMtime( long mtime ) { this.mtime = mtime; }

    // Object
    public String toString()
    {
        return "utimeRequest( " + context.toString() + ", " + "\"" + path + "\"" + ", " + Long.toString( ctime ) + ", " + Long.toString( atime ) + ", " + Long.toString( mtime ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::utimeRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(path,writer); }
        writer.putLong( ctime );
        writer.putLong( atime );
        writer.putLong( mtime );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        ctime = buf.getLong();
        atime = buf.getLong();
        mtime = buf.getLong();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( path.length() + 4 - ( path.length() % 4 ) );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        my_size += ( Long.SIZE / 8 );
        return my_size;
    }

    private Context context;
    private String path;
    private long ctime;
    private long atime;
    private long mtime;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 22; }
    public Response createDefaultResponse() { return new utimeResponse(); }

}

