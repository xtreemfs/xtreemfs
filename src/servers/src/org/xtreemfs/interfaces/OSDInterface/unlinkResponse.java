package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class unlinkResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082930;

    
    public unlinkResponse() {  }
    public unlinkResponse( Object from_hash_map ) {  this.deserialize( from_hash_map ); }
    public unlinkResponse( Object[] from_array ) { this.deserialize( from_array ); }

    // Object
    public String toString()
    {
        return "unlinkResponse()";
    }

    // Serializable
    public int getTag() { return 2009082930; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::unlinkResponse"; }

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
    

}

