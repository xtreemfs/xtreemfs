package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class statvfsResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 1218;

    
    public statvfsResponse() { stbuf = new StatVFS(); }
    public statvfsResponse( StatVFS stbuf ) { this.stbuf = stbuf; }
    public statvfsResponse( Object from_hash_map ) { stbuf = new StatVFS(); this.deserialize( from_hash_map ); }
    public statvfsResponse( Object[] from_array ) { stbuf = new StatVFS();this.deserialize( from_array ); }

    public StatVFS getStbuf() { return stbuf; }
    public void setStbuf( StatVFS stbuf ) { this.stbuf = stbuf; }

    // Object
    public String toString()
    {
        return "statvfsResponse( " + stbuf.toString() + " )";
    }

    // Serializable
    public int getTag() { return 1218; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::statvfsResponse"; }

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
        stbuf = new StatVFS(); stbuf.deserialize( buf );
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


    private StatVFS stbuf;    

}

