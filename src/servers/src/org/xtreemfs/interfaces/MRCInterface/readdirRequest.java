package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class readdirRequest implements org.xtreemfs.interfaces.utils.Request
{
    public readdirRequest() { path = ""; }
    public readdirRequest( String path ) { this.path = path; }
    public readdirRequest( Object from_hash_map ) { path = ""; this.deserialize( from_hash_map ); }
    public readdirRequest( Object[] from_array ) { path = "";this.deserialize( from_array ); }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }

    public long getTag() { return 1212; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::readdirRequest"; }

    public String toString()
    {
        return "readdirRequest( " + "\"" + path + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.path = ( String )from_hash_map.get( "path" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.path = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "path", path );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 1212; }
    public Response createDefaultResponse() { return new readdirResponse(); }


    private String path;    

}

