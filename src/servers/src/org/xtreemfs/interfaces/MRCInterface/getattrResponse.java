package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class getattrResponse implements org.xtreemfs.interfaces.utils.Response
{
    public static final int TAG = 2009082824;

    
    public getattrResponse() { stbuf = new Stat(); }
    public getattrResponse( Stat stbuf ) { this.stbuf = stbuf; }
    public getattrResponse( Object from_hash_map ) { stbuf = new Stat(); this.deserialize( from_hash_map ); }
    public getattrResponse( Object[] from_array ) { stbuf = new Stat();this.deserialize( from_array ); }

    public Stat getStbuf() { return stbuf; }
    public void setStbuf( Stat stbuf ) { this.stbuf = stbuf; }

    // Object
    public String toString()
    {
        return "getattrResponse( " + stbuf.toString() + " )";
    }

    // Serializable
    public int getTag() { return 2009082824; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::getattrResponse"; }

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
        stbuf = new Stat(); stbuf.deserialize( buf );
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


    private Stat stbuf;    

}

