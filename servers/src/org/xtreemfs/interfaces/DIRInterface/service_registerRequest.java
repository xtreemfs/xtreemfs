package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class service_registerRequest implements org.xtreemfs.interfaces.utils.Request
{
    public service_registerRequest() { service = new ServiceRegistry(); }
    public service_registerRequest( ServiceRegistry service ) { this.service = service; }
    public service_registerRequest( Object from_hash_map ) { service = new ServiceRegistry(); this.deserialize( from_hash_map ); }
    public service_registerRequest( Object[] from_array ) { service = new ServiceRegistry();this.deserialize( from_array ); }

    public ServiceRegistry getService() { return service; }
    public void setService( ServiceRegistry service ) { this.service = service; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::service_registerRequest"; }    
    public long getTypeId() { return 4; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.service.deserialize( from_hash_map.get( "service" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.service.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        service = new ServiceRegistry(); service.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "service", service.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        service.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += service.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 4; }
    public Response createDefaultResponse() { return new service_registerResponse(); }


    private ServiceRegistry service;

}

