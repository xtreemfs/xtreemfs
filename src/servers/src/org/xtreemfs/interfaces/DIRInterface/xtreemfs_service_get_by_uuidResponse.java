package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_service_get_by_uuidResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 1107;

    
    public xtreemfs_service_get_by_uuidResponse() { services = new ServiceSet(); }
    public xtreemfs_service_get_by_uuidResponse( ServiceSet services ) { this.services = services; }
    public xtreemfs_service_get_by_uuidResponse( Object from_hash_map ) { services = new ServiceSet(); this.deserialize( from_hash_map ); }
    public xtreemfs_service_get_by_uuidResponse( Object[] from_array ) { services = new ServiceSet();this.deserialize( from_array ); }

    public ServiceSet getServices() { return services; }
    public void setServices( ServiceSet services ) { this.services = services; }

    // Object
    public String toString()
    {
        return "xtreemfs_service_get_by_uuidResponse( " + services.toString() + " )";
    }

    // Serializable
    public int getTag() { return 1107; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_uuidResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.services.deserialize( ( Object[] )from_hash_map.get( "services" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.services.deserialize( ( Object[] )from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        services = new ServiceSet(); services.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "services", services.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        services.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += services.calculateSize();
        return my_size;
    }


    private ServiceSet services;    

}

