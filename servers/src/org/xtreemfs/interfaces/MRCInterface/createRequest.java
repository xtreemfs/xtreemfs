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


         

public class createRequest implements Request
{
    public createRequest() { context = new org.xtreemfs.interfaces.Context(); path = ""; mode = 0; open_ = false; }
    public createRequest( Context context, String path, int mode, boolean open_ ) { this.context = context; this.path = path; this.mode = mode; this.open_ = open_; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }
    public boolean getOpen_() { return open_; }
    public void setOpen_( boolean open_ ) { this.open_ = open_; }

    // Object
    public String toString()
    {
        return "createRequest( " + context.toString() + ", " + "\"" + path + "\"" + ", " + Integer.toString( mode ) + ", " + Boolean.toString( open_ ) + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::createRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(path,writer); }
        writer.putInt( mode );
        writer.putInt( open_ ? 1 : 0 );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        mode = buf.getInt();
        open_ = buf.getInt() != 0;    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( path.length() + 4 - ( path.length() % 4 ) );
        my_size += ( Integer.SIZE / 8 );
        my_size += 4;
        return my_size;
    }

    private Context context;
    private String path;
    private int mode;
    private boolean open_;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 4; }
    public Response createDefaultResponse() { return new createResponse(); }

}

