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


         

public class openRequest implements Request
{
    public openRequest() { context = new org.xtreemfs.interfaces.Context(); path = ""; flags = 0; mode = 0; }
    public openRequest( Context context, String path, int flags, int mode ) { this.context = context; this.path = path; this.flags = flags; this.mode = mode; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public int getFlags() { return flags; }
    public void setFlags( int flags ) { this.flags = flags; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }

    // Object
    public String toString()
    {
        return "openRequest( " + context.toString() + ", " + "\"" + path + "\"" + ", " + Integer.toString( flags ) + ", " + Integer.toString( mode ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::openRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(path,writer); }
        writer.putInt( flags );
        writer.putInt( mode );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        flags = buf.getInt();
        mode = buf.getInt();    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( path.length() + 4 - ( path.length() % 4 ) );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    private Context context;
    private String path;
    private int flags;
    private int mode;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 11; }
    public Response createDefaultResponse() { return new openResponse(); }

}

