package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_service_offlineRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082730;

    
    public xtreemfs_service_offlineRequest() { uuid = ""; }
    public xtreemfs_service_offlineRequest( String uuid ) { this.uuid = uuid; }
    public xtreemfs_service_offlineRequest( Object from_hash_map ) { uuid = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_service_offlineRequest( Object[] from_array ) { uuid = "";this.deserialize( from_array ); }

    public String getUuid() { return uuid; }
    public void setUuid( String uuid ) { this.uuid = uuid; }

    // Object
    public String toString()
    {
        return "xtreemfs_service_offlineRequest( " + "\"" + uuid + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 2009082730; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_offlineRequest"; }

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
    public Response createDefaultResponse() { return new xtreemfs_service_offlineResponse(); }


    private String uuid;    

}

