package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class ftruncateResponse implements org.xtreemfs.interfaces.utils.Response
{
    public ftruncateResponse() { truncate_xcap = new XCap(); }
    public ftruncateResponse( XCap truncate_xcap ) { this.truncate_xcap = truncate_xcap; }
    public ftruncateResponse( Object from_hash_map ) { truncate_xcap = new XCap(); this.deserialize( from_hash_map ); }
    public ftruncateResponse( Object[] from_array ) { truncate_xcap = new XCap();this.deserialize( from_array ); }

    public XCap getTruncate_xcap() { return truncate_xcap; }
    public void setTruncate_xcap( XCap truncate_xcap ) { this.truncate_xcap = truncate_xcap; }

    public long getTag() { return 1230; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::ftruncateResponse"; }

    public String toString()
    {
        return "ftruncateResponse( " + truncate_xcap.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.truncate_xcap.deserialize( from_hash_map.get( "truncate_xcap" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.truncate_xcap.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        truncate_xcap = new XCap(); truncate_xcap.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "truncate_xcap", truncate_xcap.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        truncate_xcap.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += truncate_xcap.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 1230; }


    private XCap truncate_xcap;    

}

