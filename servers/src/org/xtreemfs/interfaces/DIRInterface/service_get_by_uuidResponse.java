package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class service_get_by_uuidResponse implements org.xtreemfs.interfaces.utils.Response
{
    public service_get_by_uuidResponse() { services = new ServiceRegistrySet(); }
    public service_get_by_uuidResponse( ServiceRegistrySet services ) { this.services = services; }
    public service_get_by_uuidResponse( Object from_hash_map ) { services = new ServiceRegistrySet(); this.deserialize( from_hash_map ); }
    public service_get_by_uuidResponse( Object[] from_array ) { services = new ServiceRegistrySet();this.deserialize( from_array ); }

    public ServiceRegistrySet getServices() { return services; }
    public void setServices( ServiceRegistrySet services ) { this.services = services; }

    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::service_get_by_uuidResponse"; }    
    public long getTypeId() { return 7; }

    public String toString()
    {
        return "service_get_by_uuidResponse( " + services.toString() + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.services.deserialize( from_hash_map.get( "services" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.services.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        services = new ServiceRegistrySet(); services.deserialize( buf );
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

    // Response
    public int getOperationNumber() { return 7; }


    private ServiceRegistrySet services;

}

