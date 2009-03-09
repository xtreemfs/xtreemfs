package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class check_objectResponse implements org.xtreemfs.interfaces.utils.Response
{
    public check_objectResponse() { returnValue = new ObjectData(); }
    public check_objectResponse( ObjectData returnValue ) { this.returnValue = returnValue; }
    public check_objectResponse( Object from_hash_map ) { returnValue = new ObjectData(); this.deserialize( from_hash_map ); }
    public check_objectResponse( Object[] from_array ) { returnValue = new ObjectData();this.deserialize( from_array ); }

    public ObjectData getReturnValue() { return returnValue; }
    public void setReturnValue( ObjectData returnValue ) { this.returnValue = returnValue; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::check_objectResponse"; }    
    public long getTypeId() { return 103; }

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
        returnValue = new ObjectData(); returnValue.deserialize( buf );
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
    public int getOperationNumber() { return 103; }


    private ObjectData returnValue;

}

