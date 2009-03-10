package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class global_time_getRequest implements org.xtreemfs.interfaces.utils.Request
{
    public global_time_getRequest() {  }
    public global_time_getRequest( Object from_hash_map ) {  this.deserialize( from_hash_map ); }
    public global_time_getRequest( Object[] from_array ) { this.deserialize( from_array ); }

    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::global_time_getRequest"; }    
    public long getTypeId() { return 8; }

    public String toString()
    {
        return "global_time_getRequest()"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {

    }
    
    public void deserialize( Object[] from_array )
    {
        
    }

    public void deserialize( ReusableBuffer buf )
    {

    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {

    }
    
    public int calculateSize()
    {
        int my_size = 0;

        return my_size;
    }

    // Request
    public int getOperationNumber() { return 8; }
    public Response createDefaultResponse() { return new global_time_getResponse(); }


}

