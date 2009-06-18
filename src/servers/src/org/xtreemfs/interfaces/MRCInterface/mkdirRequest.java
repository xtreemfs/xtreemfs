package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class mkdirRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 1210;

    
    public mkdirRequest() { path = ""; mode = 0; }
    public mkdirRequest( String path, int mode ) { this.path = path; this.mode = mode; }
    public mkdirRequest( Object from_hash_map ) { path = ""; mode = 0; this.deserialize( from_hash_map ); }
    public mkdirRequest( Object[] from_array ) { path = ""; mode = 0;this.deserialize( from_array ); }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }

    // Object
    public String toString()
    {
        return "mkdirRequest( " + "\"" + path + "\"" + ", " + Integer.toString( mode ) + " )";
    }

    // Serializable
    public int getTag() { return 1210; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::mkdirRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.path = ( String )from_hash_map.get( "path" );
        this.mode = ( ( Integer )from_hash_map.get( "mode" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.path = ( String )from_array[0];
        this.mode = ( ( Integer )from_array[1] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        mode = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "path", path );
        to_hash_map.put( "mode", new Integer( mode ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
        writer.putInt( mode );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new mkdirResponse(); }


    private String path;
    private int mode;    

}

