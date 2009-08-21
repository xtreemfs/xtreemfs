package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class readResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 1310;

    
    public readResponse() { object_data = new ObjectData(); }
    public readResponse( ObjectData object_data ) { this.object_data = object_data; }
    public readResponse( Object from_hash_map ) { object_data = new ObjectData(); this.deserialize( from_hash_map ); }
    public readResponse( Object[] from_array ) { object_data = new ObjectData();this.deserialize( from_array ); }

    public ObjectData getObject_data() { return object_data; }
    public void setObject_data( ObjectData object_data ) { this.object_data = object_data; }

    // Object
    public String toString()
    {
        return "readResponse( " + object_data.toString() + " )";
    }

    // Serializable
    public int getTag() { return 1310; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::readResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.object_data.deserialize( from_hash_map.get( "object_data" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.object_data.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        object_data = new ObjectData(); object_data.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "object_data", object_data.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        object_data.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += object_data.calculateSize();
        return my_size;
    }


    private ObjectData object_data;    

}

