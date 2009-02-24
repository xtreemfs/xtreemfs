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
        { final byte[] bytes = uuid.getBytes(); writer.putInt( bytes.length ); writer.put( bytes );  if (bytes.length % 4 > 0) {for (int k = 0; k < (4 - (bytes.length % 4)); k++) { writer.put((byte)0); } }}        
    }
    
    public void deserialize( ReusableBuffer buf )
    {
        { int uuid_new_length = buf.getInt(); byte[] uuid_new_bytes = new byte[uuid_new_length]; buf.get( uuid_new_bytes ); uuid = new String( uuid_new_bytes ); if (uuid_new_length % 4 > 0) {for (int k = 0; k < (4 - (uuid_new_length % 4)); k++) { buf.get(); } } }    
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

