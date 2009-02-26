package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import org.xtreemfs.interfaces.DIRInterface.*;

import org.xtreemfs.interfaces.utils.*;

import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.buffer.BufferPool;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;


         

public class getAddressMappingsRequest implements Request
{
    public getAddressMappingsRequest() { uuid = ""; }
    public getAddressMappingsRequest( String uuid ) { this.uuid = uuid; }

    public String getUuid() { return uuid; }
    public void setUuid( String uuid ) { this.uuid = uuid; }

    // Object
    public String toString()
    {
        return "getAddressMappingsRequest( " + "\"" + uuid + "\"" + " )";
    }    

    // Serializable
    public String getTypeName() { return "xtreemfs::interfaces::DIRInterface::getAddressMappingsRequest"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        { org.xtreemfs.interfaces.utils.XDRUtils.serializeString(uuid,writer); }        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString(buf); }    
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4 + ( uuid.length() + 4 - ( uuid.length() % 4 ) );
        return my_size;
    }

    private String uuid;
    

    // Request
    public int getInterfaceVersion() { return 1; }    
    public int getOperationNumber() { return 1; }
    public Response createDefaultResponse() { return new getAddressMappingsResponse(); }

}

