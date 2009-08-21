package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_broadcast_gmaxResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 1320;

    
    public xtreemfs_broadcast_gmaxResponse() {  }
    public xtreemfs_broadcast_gmaxResponse( Object from_hash_map ) {  this.deserialize( from_hash_map ); }
    public xtreemfs_broadcast_gmaxResponse( Object[] from_array ) { this.deserialize( from_array ); }

    // Object
    public String toString()
    {
        return "xtreemfs_broadcast_gmaxResponse()";
    }

    // Serializable
    public int getTag() { return 1320; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_broadcast_gmaxResponse"; }

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

