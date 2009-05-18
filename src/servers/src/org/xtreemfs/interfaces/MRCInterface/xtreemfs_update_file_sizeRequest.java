package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_update_file_sizeRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_update_file_sizeRequest() { xcap = new XCap(); osd_write_response = new OSDWriteResponse(); }
    public xtreemfs_update_file_sizeRequest( XCap xcap, OSDWriteResponse osd_write_response ) { this.xcap = xcap; this.osd_write_response = osd_write_response; }
    public xtreemfs_update_file_sizeRequest( Object from_hash_map ) { xcap = new XCap(); osd_write_response = new OSDWriteResponse(); this.deserialize( from_hash_map ); }
    public xtreemfs_update_file_sizeRequest( Object[] from_array ) { xcap = new XCap(); osd_write_response = new OSDWriteResponse();this.deserialize( from_array ); }

    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }
    public OSDWriteResponse getOsd_write_response() { return osd_write_response; }
    public void setOsd_write_response( OSDWriteResponse osd_write_response ) { this.osd_write_response = osd_write_response; }

    public long getTag() { return 1229; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeRequest"; }

    public String toString()
    {
        return "xtreemfs_update_file_sizeRequest( " + xcap.toString() + ", " + osd_write_response.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.xcap.deserialize( from_hash_map.get( "xcap" ) );
        this.osd_write_response.deserialize( from_hash_map.get( "osd_write_response" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.xcap.deserialize( from_array[0] );
        this.osd_write_response.deserialize( from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        xcap = new XCap(); xcap.deserialize( buf );
        osd_write_response = new OSDWriteResponse(); osd_write_response.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "xcap", xcap.serialize() );
        to_hash_map.put( "osd_write_response", osd_write_response.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        xcap.serialize( writer );
        osd_write_response.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += xcap.calculateSize();
        my_size += osd_write_response.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 1229; }
    public Response createDefaultResponse() { return new xtreemfs_update_file_sizeResponse(); }


    private XCap xcap;
    private OSDWriteResponse osd_write_response;    

}

