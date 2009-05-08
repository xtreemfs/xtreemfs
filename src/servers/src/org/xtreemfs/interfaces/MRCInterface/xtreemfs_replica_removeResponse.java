package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_replica_removeResponse implements org.xtreemfs.interfaces.utils.Response
{
    public xtreemfs_replica_removeResponse() { delete_xcap = new XCap(); }
    public xtreemfs_replica_removeResponse( XCap delete_xcap ) { this.delete_xcap = delete_xcap; }
    public xtreemfs_replica_removeResponse( Object from_hash_map ) { delete_xcap = new XCap(); this.deserialize( from_hash_map ); }
    public xtreemfs_replica_removeResponse( Object[] from_array ) { delete_xcap = new XCap();this.deserialize( from_array ); }

    public XCap getDelete_xcap() { return delete_xcap; }
    public void setDelete_xcap( XCap delete_xcap ) { this.delete_xcap = delete_xcap; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeResponse"; }    
    public long getTypeId() { return 27; }

    public String toString()
    {
        return "xtreemfs_replica_removeResponse( " + delete_xcap.toString() + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.delete_xcap.deserialize( from_hash_map.get( "delete_xcap" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.delete_xcap.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        delete_xcap = new XCap(); delete_xcap.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "delete_xcap", delete_xcap.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        delete_xcap.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += delete_xcap.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 27; }


    private XCap delete_xcap;

}

