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


         

public class setattrRequest implements Request
{
    public setattrRequest() { context = new org.xtreemfs.interfaces.Context(); path = ""; stbuf = new org.xtreemfs.interfaces.stat_(); }
    public setattrRequest( Context context, String path, stat_ stbuf ) { this.context = context; this.path = path; this.stbuf = stbuf; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public stat_ getStbuf() { return stbuf; }
    public void setStbuf( stat_ stbuf ) { this.stbuf = stbuf; }

    // Object
    public String toString()
    {
        return "setattrRequest( " + context.toString() + ", " + "\"" + path + "\"" + ", " + stbuf.toString() + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::setattrRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(path,writer); }
        stbuf.serialize( writer );        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        stbuf = new org.xtreemfs.interfaces.stat_(); stbuf.deserialize( buf );    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += stbuf.calculateSize();
        return my_size;
    }

    private Context context;
    private String path;
    private stat_ stbuf;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 17; }
    public Response createDefaultResponse() { return new setattrResponse(); }

}

