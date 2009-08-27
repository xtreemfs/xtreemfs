package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_internal_truncateResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082959;

    
    public xtreemfs_internal_truncateResponse() { osd_write_response = new OSDWriteResponse(); }
    public xtreemfs_internal_truncateResponse( OSDWriteResponse osd_write_response ) { this.osd_write_response = osd_write_response; }
    public xtreemfs_internal_truncateResponse( Object from_hash_map ) { osd_write_response = new OSDWriteResponse(); this.deserialize( from_hash_map ); }
    public xtreemfs_internal_truncateResponse( Object[] from_array ) { osd_write_response = new OSDWriteResponse();this.deserialize( from_array ); }

    public OSDWriteResponse getOsd_write_response() { return osd_write_response; }
    public void setOsd_write_response( OSDWriteResponse osd_write_response ) { this.osd_write_response = osd_write_response; }

    // Object
    public String toString()
    {
        return "xtreemfs_internal_truncateResponse( " + osd_write_response.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082959; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::xtreemfs_internal_truncateResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.osd_write_response.deserialize( from_hash_map.get( "osd_write_response" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.osd_write_response.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        osd_write_response = new OSDWriteResponse(); osd_write_response.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "osd_write_response", osd_write_response.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        osd_write_response.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += osd_write_response.calculateSize();
        return my_size;
    }


    private OSDWriteResponse osd_write_response;    

}

