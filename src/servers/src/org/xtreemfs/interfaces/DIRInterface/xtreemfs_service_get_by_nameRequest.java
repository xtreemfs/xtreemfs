package org.xtreemfs.interfaces.DIRInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_service_get_by_nameRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_service_get_by_nameRequest() { name = ""; }
    public xtreemfs_service_get_by_nameRequest( String name ) { this.name = name; }
    public xtreemfs_service_get_by_nameRequest( Object from_hash_map ) { name = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_service_get_by_nameRequest( Object[] from_array ) { name = "";this.deserialize( from_array ); }

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    public long getTag() { return 1109; }
    public String getTypeName() { return "org::xtreemfs::interfaces::DIRInterface::xtreemfs_service_get_by_nameRequest"; }

    public String toString()
    {
        return "xtreemfs_service_get_by_nameRequest( " + "\"" + name + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.name = ( String )from_hash_map.get( "name" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.name = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "name", name );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( name, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(name);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 1109; }
    public Response createDefaultResponse() { return new xtreemfs_service_get_by_nameResponse(); }


    private String name;    

}

