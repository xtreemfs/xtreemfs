package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class admin_shutdownRequest implements org.xtreemfs.interfaces.utils.Request
{
    public admin_shutdownRequest() { password = ""; }
    public admin_shutdownRequest( String password ) { this.password = password; }
    public admin_shutdownRequest( Object from_hash_map ) { password = ""; this.deserialize( from_hash_map ); }
    public admin_shutdownRequest( Object[] from_array ) { password = "";this.deserialize( from_array ); }

    public String getPassword() { return password; }
    public void setPassword( String password ) { this.password = password; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::admin_shutdownRequest"; }    
    public long getTypeId() { return 50; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.password = ( String )from_hash_map.get( "password" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.password = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        password = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "password", password );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( password, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(password);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 50; }
    public Response createDefaultResponse() { return new admin_shutdownResponse(); }


    private String password;

}

