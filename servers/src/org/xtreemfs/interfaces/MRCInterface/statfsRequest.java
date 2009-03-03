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


         

public class statfsRequest implements Request
{
    public statfsRequest() { context = new org.xtreemfs.interfaces.Context(); volume_name = ""; }
    public statfsRequest( Context context, String volume_name ) { this.context = context; this.volume_name = volume_name; }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getVolume_name() { return volume_name; }
    public void setVolume_name( String volume_name ) { this.volume_name = volume_name; }

    // Object
    public String toString()
    {
        return "statfsRequest( " + context.toString() + ", " + "\"" + volume_name + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::MRCInterface::statfsRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        context.serialize( writer );
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(volume_name,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        context = new org.xtreemfs.interfaces.Context(); context.deserialize( buf );
        { volume_name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += 4 + ( volume_name.length() + 4 - ( volume_name.length() % 4 ) );
        return my_size;
    }

    private Context context;
    private String volume_name;
    

    // Request
    public int getInterfaceVersion() { return 2; }    
    public int getOperationNumber() { return 19; }
    public Response createDefaultResponse() { return new statfsResponse(); }

}

