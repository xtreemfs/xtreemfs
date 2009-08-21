package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_lock_checkResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 1351;

    
    public xtreemfs_lock_checkResponse() { returnValue = new Lock(); }
    public xtreemfs_lock_checkResponse( Lock returnValue ) { this.returnValue = returnValue; }
    public xtreemfs_lock_checkResponse( Object from_hash_map ) { returnValue = new Lock(); this.deserialize( from_hash_map ); }
    public xtreemfs_lock_checkResponse( Object[] from_array ) { returnValue = new Lock();this.deserialize( from_array ); }

    public Lock getReturnValue() { return returnValue; }
    public void setReturnValue( Lock returnValue ) { this.returnValue = returnValue; }

    // Object
    public String toString()
    {
        return "xtreemfs_lock_checkResponse( " + returnValue.toString() + " )";
    }

    // Serializable
    public int getTag() { return 1351; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_lock_checkResponse"; }

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
        returnValue = new Lock(); returnValue.deserialize( buf );
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


    private Lock returnValue;    

}

