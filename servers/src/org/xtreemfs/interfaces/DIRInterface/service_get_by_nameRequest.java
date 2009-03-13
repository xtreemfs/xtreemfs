package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class service_get_by_nameRequest implements org.xtreemfs.interfaces.utils.Request
{
    public service_get_by_nameRequest() { service_name = ""; }
    public service_get_by_nameRequest( String service_name ) { this.service_name = service_name; }
    public service_get_by_nameRequest( Object from_hash_map ) { service_name = ""; this.deserialize( from_hash_map ); }
    public service_get_by_nameRequest( Object[] from_array ) { service_name = "";this.deserialize( from_array ); }

    public String getService_name() { return service_name; }
    public void setService_name( String service_name ) { this.service_name = service_name; }

    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::service_get_by_nameRequest"; }    
    public long getTypeId() { return 9; }

    public String toString()
    {
        return "service_get_by_nameRequest( " + "\"" + service_name + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.service_name = ( String )from_hash_map.get( "service_name" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.service_name = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        service_name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "service_name", service_name );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( service_name, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(service_name);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 9; }
    public Response createDefaultResponse() { return new service_get_by_nameResponse(); }


    private String service_name;

}

