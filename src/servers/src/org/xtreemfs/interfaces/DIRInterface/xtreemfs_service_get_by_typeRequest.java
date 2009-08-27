package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_service_get_by_typeRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082725;

    
    public xtreemfs_service_get_by_typeRequest() { type = ServiceType.SERVICE_TYPE_MIXED; }
    public xtreemfs_service_get_by_typeRequest( ServiceType type ) { this.type = type; }
    public xtreemfs_service_get_by_typeRequest( Object from_hash_map ) { type = ServiceType.SERVICE_TYPE_MIXED; this.deserialize( from_hash_map ); }
    public xtreemfs_service_get_by_typeRequest( Object[] from_array ) { type = ServiceType.SERVICE_TYPE_MIXED;this.deserialize( from_array ); }

    public ServiceType getType() { return type; }
    public void setType( ServiceType type ) { this.type = type; }

    // Object
    public String toString()
    {
        return "xtreemfs_service_get_by_typeRequest( " + type.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082725; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_typeRequest"; }

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
        type = ServiceType.parseInt( buf.getInt() );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "type", type );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        writer.putInt( type.intValue() );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += 4;
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_service_get_by_typeResponse(); }


    private ServiceType type;    

}

