package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class writeResponse implements org.xtreemfs.interfaces.utils.Response
{
    public writeResponse() { osd_write_response = new OSDWriteResponse(); }
    public writeResponse( OSDWriteResponse osd_write_response ) { this.osd_write_response = osd_write_response; }
    public writeResponse( Object from_hash_map ) { osd_write_response = new OSDWriteResponse(); this.deserialize( from_hash_map ); }
    public writeResponse( Object[] from_array ) { osd_write_response = new OSDWriteResponse();this.deserialize( from_array ); }

    public OSDWriteResponse getOsd_write_response() { return osd_write_response; }
    public void setOsd_write_response( OSDWriteResponse osd_write_response ) { this.osd_write_response = osd_write_response; }

    public long getTag() { return 1304; }
    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::writeResponse"; }

    public String toString()
    {
        return "writeResponse( " + osd_write_response.toString() + " )";
    }


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

    // Response
    public int getOperationNumber() { return 1304; }


    private OSDWriteResponse osd_write_response;    

}

