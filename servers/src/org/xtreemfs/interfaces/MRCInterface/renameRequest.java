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


         

public class renameRequest implements Request
{
    public renameRequest() { context = new org.xtreemfs.interfaces.Context(); source_path = ""; target_path = ""; }
    public renameRequest( Context context, String source_path, String target_path ) { this.context = context; this.source_path = source_path; this.target_path = target_path; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getSource_path() { return source_path; }
    public void setSource_path( String source_path ) { this.source_path = source_path; }
    public String getTarget_path() { return target_path; }
    public void setTarget_path( String target_path ) { this.target_path = target_path; }

    // Object
    public String toString()
    {
        return "renameRequest( " + context.toString() + ", " + "\"" + source_path + "\"" + ", " + "\"" + target_path + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::renameRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(source_path,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(target_path,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { source_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { target_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( source_path.length() + 4 - ( source_path.length() % 4 ) );
        my_size += 4 + ( target_path.length() + 4 - ( target_path.length() % 4 ) );
        return my_size;
    }

    private Context context;
    private String source_path;
    private String target_path;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 14; }
    public Response createDefaultResponse() { return new renameResponse(); }

}

