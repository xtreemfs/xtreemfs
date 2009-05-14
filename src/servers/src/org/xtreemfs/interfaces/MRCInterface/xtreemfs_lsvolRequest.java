package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_lsvolRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_lsvolRequest() {  }
    public xtreemfs_lsvolRequest( Object from_hash_map ) {  this.deserialize( from_hash_map ); }
    public xtreemfs_lsvolRequest( Object[] from_array ) { this.deserialize( from_array ); }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_lsvolRequest"; }    
    public long getTypeId() { return 31; }

    public String toString()
    {
        return "xtreemfs_lsvolRequest()";
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
    public int getOperationNumber() { return 31; }
    public Response createDefaultResponse() { return new xtreemfs_lsvolResponse(); }


}

