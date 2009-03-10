package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class getattrResponse implements org.xtreemfs.interfaces.utils.Response
{
    public getattrResponse() { stbuf = new stat_(); }
    public getattrResponse( stat_ stbuf ) { this.stbuf = stbuf; }
    public getattrResponse( Object from_hash_map ) { stbuf = new stat_(); this.deserialize( from_hash_map ); }
    public getattrResponse( Object[] from_array ) { stbuf = new stat_();this.deserialize( from_array ); }

    public stat_ getStbuf() { return stbuf; }
    public void setStbuf( stat_ stbuf ) { this.stbuf = stbuf; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::getattrResponse"; }    
    public long getTypeId() { return 5; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.stbuf.deserialize( from_hash_map.get( "stbuf" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.stbuf.deserialize( from_array[0] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        stbuf = new stat_(); stbuf.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "stbuf", stbuf.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        stbuf.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += stbuf.calculateSize();
        return my_size;
    }

    // Response
    public int getOperationNumber() { return 5; }


    private stat_ stbuf;

}

