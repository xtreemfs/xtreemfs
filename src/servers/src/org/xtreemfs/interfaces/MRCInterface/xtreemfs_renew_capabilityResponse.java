package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_renew_capabilityResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082856;

    
    public xtreemfs_renew_capabilityResponse() { renewed_xcap = new XCap(); }
    public xtreemfs_renew_capabilityResponse( XCap renewed_xcap ) { this.renewed_xcap = renewed_xcap; }
    public xtreemfs_renew_capabilityResponse( Object from_hash_map ) { renewed_xcap = new XCap(); this.deserialize( from_hash_map ); }
    public xtreemfs_renew_capabilityResponse( Object[] from_array ) { renewed_xcap = new XCap();this.deserialize( from_array ); }

    public XCap getRenewed_xcap() { return renewed_xcap; }
    public void setRenewed_xcap( XCap renewed_xcap ) { this.renewed_xcap = renewed_xcap; }

    // Object
    public String toString()
    {
        return "xtreemfs_renew_capabilityResponse( " + renewed_xcap.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082856; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_renew_capabilityResponse"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.renewed_xcap.deserialize( from_hash_map.get( "renewed_xcap" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.renewed_xcap.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        renewed_xcap = new XCap(); renewed_xcap.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "renewed_xcap", renewed_xcap.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        renewed_xcap.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += renewed_xcap.calculateSize();
        return my_size;
    }


    private XCap renewed_xcap;    

}

