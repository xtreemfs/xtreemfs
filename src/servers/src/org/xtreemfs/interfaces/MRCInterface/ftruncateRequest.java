package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ftruncateRequest implements org.xtreemfs.interfaces.utils.Request
{
    public ftruncateRequest() { write_xcap = new XCap(); }
    public ftruncateRequest( XCap write_xcap ) { this.write_xcap = write_xcap; }
    public ftruncateRequest( Object from_hash_map ) { write_xcap = new XCap(); this.deserialize( from_hash_map ); }
    public ftruncateRequest( Object[] from_array ) { write_xcap = new XCap();this.deserialize( from_array ); }

    public XCap getWrite_xcap() { return write_xcap; }
    public void setWrite_xcap( XCap write_xcap ) { this.write_xcap = write_xcap; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::ftruncateRequest"; }    
    public long getTypeId() { return 30; }

    public String toString()
    {
        return "ftruncateRequest( " + write_xcap.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.write_xcap.deserialize( from_hash_map.get( "write_xcap" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.write_xcap.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        write_xcap = new XCap(); write_xcap.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "write_xcap", write_xcap.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        write_xcap.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += write_xcap.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 30; }
    public Response createDefaultResponse() { return new ftruncateResponse(); }


    private XCap write_xcap;

}

