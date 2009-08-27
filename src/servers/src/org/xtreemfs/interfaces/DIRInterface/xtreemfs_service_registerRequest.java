package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_service_registerRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082728;

    
    public xtreemfs_service_registerRequest() { service = new Service(); }
    public xtreemfs_service_registerRequest( Service service ) { this.service = service; }
    public xtreemfs_service_registerRequest( Object from_hash_map ) { service = new Service(); this.deserialize( from_hash_map ); }
    public xtreemfs_service_registerRequest( Object[] from_array ) { service = new Service();this.deserialize( from_array ); }

    public Service getService() { return service; }
    public void setService( Service service ) { this.service = service; }

    // Object
    public String toString()
    {
        return "xtreemfs_service_registerRequest( " + service.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082728; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_registerRequest"; }

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
        service = new Service(); service.deserialize( buf );
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
    public Response createDefaultResponse() { return new xtreemfs_service_registerResponse(); }


    private Service service;    

}

