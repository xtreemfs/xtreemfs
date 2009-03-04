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


         

public class symlinkRequest implements Request
{
    public symlinkRequest() { context = new org.xtreemfs.interfaces.Context(); target_path = ""; link_path = ""; }
    public symlinkRequest( Context context, String target_path, String link_path ) { this.context = context; this.target_path = target_path; this.link_path = link_path; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getTarget_path() { return target_path; }
    public void setTarget_path( String target_path ) { this.target_path = target_path; }
    public String getLink_path() { return link_path; }
    public void setLink_path( String link_path ) { this.link_path = link_path; }

    // Object
    public String toString()
    {
        return "symlinkRequest( " + context.toString() + ", " + "\"" + target_path + "\"" + ", " + "\"" + link_path + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::symlinkRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(target_path,writer); }
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(link_path,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { target_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }
        { link_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( target_path.length() + 4 - ( target_path.length() % 4 ) );
        my_size += 4 + ( link_path.length() + 4 - ( link_path.length() % 4 ) );
        return my_size;
    }

    private Context context;
    private String target_path;
    private String link_path;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 20; }
    public Response createDefaultResponse() { return new symlinkResponse(); }

}

