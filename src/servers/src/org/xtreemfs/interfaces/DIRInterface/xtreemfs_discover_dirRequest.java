package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_discover_dirRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082723;

    
    public xtreemfs_discover_dirRequest() {  }
    public xtreemfs_discover_dirRequest( Object from_hash_map ) {  this.deserialize( from_hash_map ); }
    public xtreemfs_discover_dirRequest( Object[] from_array ) { this.deserialize( from_array ); }

    // Object
    public String toString()
    {
        return "xtreemfs_discover_dirRequest()";
    }

    // Serializable
    public int getTag() { return 2009082723; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_discover_dirRequest"; }

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
    public Response createDefaultResponse() { return new xtreemfs_discover_dirResponse(); }
    

}

