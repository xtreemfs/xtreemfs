package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class service_get_by_uuidRequest implements org.xtreemfs.interfaces.utils.Request
{
    public service_get_by_uuidRequest() { uuid = ""; }
    public service_get_by_uuidRequest( String uuid ) { this.uuid = uuid; }
    public service_get_by_uuidRequest( Object from_hash_map ) { uuid = ""; this.deserialize( from_hash_map ); }
    public service_get_by_uuidRequest( Object[] from_array ) { uuid = "";this.deserialize( from_array ); }

    public String getUuid() { return uuid; }
    public void setUuid( String uuid ) { this.uuid = uuid; }

    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::service_get_by_uuidRequest"; }    
    public long getTypeId() { return 7; }

    public String toString()
    {
        return "service_get_by_uuidRequest( " + "\"" + uuid + "\"" + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.uuid = ( String )from_hash_map.get( "uuid" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.uuid = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "uuid", uuid );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( uuid, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(uuid);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 7; }
    public Response createDefaultResponse() { return new service_get_by_uuidResponse(); }


    private String uuid;

}

