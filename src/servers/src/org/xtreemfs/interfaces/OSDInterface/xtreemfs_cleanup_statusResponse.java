package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_cleanup_statusResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082951;

    
    public xtreemfs_cleanup_statusResponse() { status = ""; }
    public xtreemfs_cleanup_statusResponse( String status ) { this.status = status; }
    public xtreemfs_cleanup_statusResponse( Object from_hash_map ) { status = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_cleanup_statusResponse( Object[] from_array ) { status = "";this.deserialize( from_array ); }

    public String getStatus() { return status; }
    public void setStatus( String status ) { this.status = status; }

    // Object
    public String toString()
    {
        return "xtreemfs_cleanup_statusResponse( " + "\"" + status + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 2009082951; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_cleanup_statusResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.status = ( String )from_hash_map.get( "status" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.status = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        status = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "status", status );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( status, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(status);
        return my_size;
    }


    private String status;    

}

