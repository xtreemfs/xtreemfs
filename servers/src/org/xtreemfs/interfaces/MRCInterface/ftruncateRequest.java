package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ftruncateRequest implements org.xtreemfs.interfaces.utils.Request
{
    public ftruncateRequest() { write_cap = new XCap(); }
    public ftruncateRequest( XCap write_cap ) { this.write_cap = write_cap; }
    public ftruncateRequest( Object from_hash_map ) { write_cap = new XCap(); this.deserialize( from_hash_map ); }
    public ftruncateRequest( Object[] from_array ) { write_cap = new XCap();this.deserialize( from_array ); }

    public XCap getWrite_cap() { return write_cap; }
    public void setWrite_cap( XCap write_cap ) { this.write_cap = write_cap; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::ftruncateRequest"; }    
    public long getTypeId() { return 30; }

    public String toString()
    {
        return "ftruncateRequest( " + write_cap.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.write_cap.deserialize( from_hash_map.get( "write_cap" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.write_cap.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        write_cap = new XCap(); write_cap.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "write_cap", write_cap.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        write_cap.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += write_cap.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 30; }
    public Response createDefaultResponse() { return new ftruncateResponse(); }


    private XCap write_cap;

}

