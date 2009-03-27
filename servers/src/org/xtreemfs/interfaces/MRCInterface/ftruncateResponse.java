package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ftruncateResponse implements org.xtreemfs.interfaces.utils.Response
{
    public ftruncateResponse() { truncate_cap = new XCap(); }
    public ftruncateResponse( XCap truncate_cap ) { this.truncate_cap = truncate_cap; }
    public ftruncateResponse( Object from_hash_map ) { truncate_cap = new XCap(); this.deserialize( from_hash_map ); }
    public ftruncateResponse( Object[] from_array ) { truncate_cap = new XCap();this.deserialize( from_array ); }

    public XCap getTruncate_cap() { return truncate_cap; }
    public void setTruncate_cap( XCap truncate_cap ) { this.truncate_cap = truncate_cap; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::ftruncateResponse"; }    
    public long getTypeId() { return 30; }

    public String toString()
    {
        return "ftruncateResponse( " + truncate_cap.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.truncate_cap.deserialize( from_hash_map.get( "truncate_cap" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.truncate_cap.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        truncate_cap = new XCap(); truncate_cap.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "truncate_cap", truncate_cap.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        truncate_cap.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += truncate_cap.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 30; }


    private XCap truncate_cap;

}

