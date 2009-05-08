package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class openRequest implements org.xtreemfs.interfaces.utils.Request
{
    public openRequest() { path = ""; flags = 0; mode = 0; attributes = 0; }
    public openRequest( String path, int flags, int mode, int attributes ) { this.path = path; this.flags = flags; this.mode = mode; this.attributes = attributes; }
    public openRequest( Object from_hash_map ) { path = ""; flags = 0; mode = 0; attributes = 0; this.deserialize( from_hash_map ); }
    public openRequest( Object[] from_array ) { path = ""; flags = 0; mode = 0; attributes = 0;this.deserialize( from_array ); }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public int getFlags() { return flags; }
    public void setFlags( int flags ) { this.flags = flags; }
    public int getMode() { return mode; }
    public void setMode( int mode ) { this.mode = mode; }
    public int getAttributes() { return attributes; }
    public void setAttributes( int attributes ) { this.attributes = attributes; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::openRequest"; }    
    public long getTypeId() { return 11; }

    public String toString()
    {
        return "openRequest( " + "\"" + path + "\"" + ", " + Integer.toString( flags ) + ", " + Integer.toString( mode ) + ", " + Integer.toString( attributes ) + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.path = ( String )from_hash_map.get( "path" );
        this.flags = ( ( Integer )from_hash_map.get( "flags" ) ).intValue();
        this.mode = ( ( Integer )from_hash_map.get( "mode" ) ).intValue();
        this.attributes = ( ( Integer )from_hash_map.get( "attributes" ) ).intValue();
    }
    
    public void deserialize( Object[] from_array )
    {
        this.path = ( String )from_array[0];
        this.flags = ( ( Integer )from_array[1] ).intValue();
        this.mode = ( ( Integer )from_array[2] ).intValue();
        this.attributes = ( ( Integer )from_array[3] ).intValue();        
    }

    public void deserialize( ReusableBuffer buf )
    {
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        flags = buf.getInt();
        mode = buf.getInt();
        attributes = buf.getInt();
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "path", path );
        to_hash_map.put( "flags", new Integer( flags ) );
        to_hash_map.put( "mode", new Integer( mode ) );
        to_hash_map.put( "attributes", new Integer( attributes ) );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
        writer.putInt( flags );
        writer.putInt( mode );
        writer.putInt( attributes );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        my_size += ( Integer.SIZE / 8 );
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 11; }
    public Response createDefaultResponse() { return new openResponse(); }


    private String path;
    private int flags;
    private int mode;
    private int attributes;

}

