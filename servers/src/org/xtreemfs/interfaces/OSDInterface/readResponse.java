package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class readResponse implements org.xtreemfs.interfaces.utils.Response
{
    public readResponse() { returnValue = new ObjectData(); }
    public readResponse( ObjectData returnValue ) { this.returnValue = returnValue; }
    public readResponse( Object from_hash_map ) { returnValue = new ObjectData(); this.deserialize( from_hash_map ); }
    public readResponse( Object[] from_array ) { returnValue = new ObjectData();this.deserialize( from_array ); }

    public ObjectData getReturnValue() { return returnValue; }
    public void setReturnValue( ObjectData returnValue ) { this.returnValue = returnValue; }

    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::readResponse"; }    
    public long getTypeId() { return 1; }

    public String toString()
    {
        return "readResponse( " + returnValue.toString() + " )"; 
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
    public int getOperationNumber() { return 1; }


    private ObjectData returnValue;

}

