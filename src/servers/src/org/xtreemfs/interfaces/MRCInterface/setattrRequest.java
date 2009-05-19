package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class setattrRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 1217;

    
    public setattrRequest() { path = ""; stbuf = new Stat(); }
    public setattrRequest( String path, Stat stbuf ) { this.path = path; this.stbuf = stbuf; }
    public setattrRequest( Object from_hash_map ) { path = ""; stbuf = new Stat(); this.deserialize( from_hash_map ); }
    public setattrRequest( Object[] from_array ) { path = ""; stbuf = new Stat();this.deserialize( from_array ); }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public Stat getStbuf() { return stbuf; }
    public void setStbuf( Stat stbuf ) { this.stbuf = stbuf; }

    // Object
    public String toString()
    {
        return "setattrRequest( " + "\"" + path + "\"" + ", " + stbuf.toString() + " )";
    }

    // Serializable
    public int getTag() { return 1217; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::setattrRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.path = ( String )from_hash_map.get( "path" );
        this.stbuf.deserialize( from_hash_map.get( "stbuf" ) );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.path = ( String )from_array[0];
        this.stbuf.deserialize( from_array[1] );        
    }

    public void deserialize( ReusableBuffer buf )
    {
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        stbuf = new Stat(); stbuf.deserialize( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "path", path );
        to_hash_map.put( "stbuf", stbuf.serialize() );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
        stbuf.serialize( writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += stbuf.calculateSize();
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new setattrResponse(); }


    private String path;
    private Stat stbuf;    

}

