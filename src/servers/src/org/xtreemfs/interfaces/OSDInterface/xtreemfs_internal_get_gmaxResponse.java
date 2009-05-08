package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_internal_get_gmaxResponse implements org.xtreemfs.interfaces.utils.Response
{
    public xtreemfs_internal_get_gmaxResponse() { returnValue = new InternalGmax(); }
    public xtreemfs_internal_get_gmaxResponse( InternalGmax returnValue ) { this.returnValue = returnValue; }
    public xtreemfs_internal_get_gmaxResponse( Object from_hash_map ) { returnValue = new InternalGmax(); this.deserialize( from_hash_map ); }
    public xtreemfs_internal_get_gmaxResponse( Object[] from_array ) { returnValue = new InternalGmax();this.deserialize( from_array ); }

    public InternalGmax getReturnValue() { return returnValue; }
    public void setReturnValue( InternalGmax returnValue ) { this.returnValue = returnValue; }

    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_get_gmaxResponse"; }    
    public long getTypeId() { return 100; }

    public String toString()
    {
        return "xtreemfs_internal_get_gmaxResponse( " + returnValue.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.returnValue.deserialize( from_hash_map.get( "returnValue" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.returnValue.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        returnValue = new InternalGmax(); returnValue.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "returnValue", returnValue.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        returnValue.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += returnValue.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 100; }


    private InternalGmax returnValue;

}

