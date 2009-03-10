package org.xtreemfs.interfaces.OSDInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class truncateResponse implements org.xtreemfs.interfaces.utils.Response
{
    public truncateResponse() { osd_response = new OSDWriteResponse(); }
    public truncateResponse( OSDWriteResponse osd_response ) { this.osd_response = osd_response; }
    public truncateResponse( Object from_hash_map ) { osd_response = new OSDWriteResponse(); this.deserialize( from_hash_map ); }
    public truncateResponse( Object[] from_array ) { osd_response = new OSDWriteResponse();this.deserialize( from_array ); }

    public OSDWriteResponse getOsd_response() { return osd_response; }
    public void setOsd_response( OSDWriteResponse osd_response ) { this.osd_response = osd_response; }

    public String getTypeName() { return "org::xtreemfs::interfaces::OSDInterface::truncateResponse"; }    
    public long getTypeId() { return 2; }

    public String toString()
    {
        return "truncateResponse( " + osd_response.toString() + " )"; 
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.osd_response.deserialize( from_hash_map.get( "osd_response" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.osd_response.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        osd_response = new OSDWriteResponse(); osd_response.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "osd_response", osd_response.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        osd_response.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += osd_response.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 2; }


    private OSDWriteResponse osd_response;

}

