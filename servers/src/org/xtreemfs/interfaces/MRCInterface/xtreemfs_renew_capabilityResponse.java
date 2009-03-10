package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_renew_capabilityResponse implements org.xtreemfs.interfaces.utils.Response
{
    public xtreemfs_renew_capabilityResponse() { xcap = new XCap(); }
    public xtreemfs_renew_capabilityResponse( XCap xcap ) { this.xcap = xcap; }
    public xtreemfs_renew_capabilityResponse( Object from_hash_map ) { xcap = new XCap(); this.deserialize( from_hash_map ); }
    public xtreemfs_renew_capabilityResponse( Object[] from_array ) { xcap = new XCap();this.deserialize( from_array ); }

    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityResponse"; }    
    public long getTypeId() { return 25; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.xcap.deserialize( from_hash_map.get( "xcap" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.xcap.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        xcap = new XCap(); xcap.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "xcap", xcap.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        xcap.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += xcap.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 25; }


    private XCap xcap;

}

