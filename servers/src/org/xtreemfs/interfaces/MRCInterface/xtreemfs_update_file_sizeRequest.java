package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_update_file_sizeRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_update_file_sizeRequest() { xcap = new XCap(); new_file_size = new OSDWriteResponse(); }
    public xtreemfs_update_file_sizeRequest( XCap xcap, OSDWriteResponse new_file_size ) { this.xcap = xcap; this.new_file_size = new_file_size; }
    public xtreemfs_update_file_sizeRequest( Object from_hash_map ) { xcap = new XCap(); new_file_size = new OSDWriteResponse(); this.deserialize( from_hash_map ); }
    public xtreemfs_update_file_sizeRequest( Object[] from_array ) { xcap = new XCap(); new_file_size = new OSDWriteResponse();this.deserialize( from_array ); }

    public XCap getXcap() { return xcap; }
    public void setXcap( XCap xcap ) { this.xcap = xcap; }
    public OSDWriteResponse getNew_file_size() { return new_file_size; }
    public void setNew_file_size( OSDWriteResponse new_file_size ) { this.new_file_size = new_file_size; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_update_file_sizeRequest"; }    
    public long getTypeId() { return 29; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.xcap.deserialize( from_hash_map.get( "xcap" ) );
        this.new_file_size.deserialize( from_hash_map.get( "new_file_size" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.xcap.deserialize( from_array[0] );
        this.new_file_size.deserialize( from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        xcap = new XCap(); xcap.deserialize( buf );
        new_file_size = new OSDWriteResponse(); new_file_size.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "xcap", xcap.serialize() );
        to_hash_map.put( "new_file_size", new_file_size.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        xcap.serialize( writer );
        new_file_size.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += xcap.calculateSize();
        my_size += new_file_size.calculateSize();
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 29; }
    public Response createDefaultResponse() { return new xtreemfs_update_file_sizeResponse(); }


    private XCap xcap;
    private OSDWriteResponse new_file_size;

}

