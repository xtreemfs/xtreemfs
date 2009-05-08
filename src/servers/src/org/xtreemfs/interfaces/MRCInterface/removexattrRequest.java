package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class removexattrRequest implements org.xtreemfs.interfaces.utils.Request
{
    public removexattrRequest() { path = ""; name = ""; }
    public removexattrRequest( String path, String name ) { this.path = path; this.name = name; }
    public removexattrRequest( Object from_hash_map ) { path = ""; name = ""; this.deserialize( from_hash_map ); }
    public removexattrRequest( Object[] from_array ) { path = ""; name = "";this.deserialize( from_array ); }

    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::removexattrRequest"; }    
    public long getTypeId() { return 13; }

    public String toString()
    {
        return "removexattrRequest( " + "\"" + path + "\"" + ", " + "\"" + name + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.path = ( String )from_hash_map.get( "path" );
        this.name = ( String )from_hash_map.get( "name" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.path = ( String )from_array[0];
        this.name = ( String )from_array[1];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "path", path );
        to_hash_map.put( "name", name );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( name, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(name);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 13; }
    public Response createDefaultResponse() { return new removexattrResponse(); }


    private String path;
    private String name;

}

