package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_renew_capabilityRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082856;

    
    public xtreemfs_renew_capabilityRequest() { old_xcap = new XCap(); }
    public xtreemfs_renew_capabilityRequest( XCap old_xcap ) { this.old_xcap = old_xcap; }
    public xtreemfs_renew_capabilityRequest( Object from_hash_map ) { old_xcap = new XCap(); this.deserialize( from_hash_map ); }
    public xtreemfs_renew_capabilityRequest( Object[] from_array ) { old_xcap = new XCap();this.deserialize( from_array ); }

    public XCap getOld_xcap() { return old_xcap; }
    public void setOld_xcap( XCap old_xcap ) { this.old_xcap = old_xcap; }

    // Object
    public String toString()
    {
        return "xtreemfs_renew_capabilityRequest( " + old_xcap.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082856; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.old_xcap.deserialize( from_hash_map.get( "old_xcap" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.old_xcap.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        old_xcap = new XCap(); old_xcap.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "old_xcap", old_xcap.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        old_xcap.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += old_xcap.calculateSize();
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_renew_capabilityResponse(); }


    private XCap old_xcap;    

}

