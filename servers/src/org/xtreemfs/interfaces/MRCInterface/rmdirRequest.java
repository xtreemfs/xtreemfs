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


         

public class rmdirRequest implements Request
{
    public rmdirRequest() { context = new org.xtreemfs.interfaces.Context(); path = ""; }
    public rmdirRequest( Context context, String path ) { this.context = context; this.path = path; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }

    // Object
    public String toString()
    {
        return "rmdirRequest( " + context.toString() + ", " + "\"" + path + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::rmdirRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(path,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( path.length() + 4 - ( path.length() % 4 ) );
        return my_size;
    }

    private Context context;
    private String path;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 15; }
    public Response createDefaultResponse() { return new rmdirResponse(); }

}

