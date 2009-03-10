package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class service_get_by_typeRequest implements org.xtreemfs.interfaces.utils.Request
{
    public service_get_by_typeRequest() { type = 0; }
    public service_get_by_typeRequest( int type ) { this.type = type; }
    public service_get_by_typeRequest( Object from_hash_map ) { type = 0; this.deserialize( from_hash_map ); }
    public service_get_by_typeRequest( Object[] from_array ) { type = 0;this.deserialize( from_array ); }

    public int getType() { return type; }
    public void setType( int type ) { this.type = type; }

    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::service_get_by_typeRequest"; }    
    public long getTypeId() { return 6; }

    public String toString()
    {
        return "service_get_by_typeRequest( " + Integer.toString( type ) + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.type = ( ( Integer )from_hash_map.get( "type" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.type = ( ( Integer )from_array[0] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        type = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "type", new Integer( type ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( type );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 6; }
    public Response createDefaultResponse() { return new service_get_by_typeResponse(); }


    private int type;

}

