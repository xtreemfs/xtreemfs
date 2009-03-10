package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class removexattrRequest implements org.xtreemfs.interfaces.utils.Request
{
    public removexattrRequest() { context = new Context(); path = ""; name = ""; }
    public removexattrRequest( Context context, String path, String name ) { this.context = context; this.path = path; this.name = name; }
    public removexattrRequest( Object from_hash_map ) { context = new Context(); path = ""; name = ""; this.deserialize( from_hash_map ); }
    public removexattrRequest( Object[] from_array ) { context = new Context(); path = ""; name = "";this.deserialize( from_array ); }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getPath() { return path; }
    public void setPath( String path ) { this.path = path; }
    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::removexattrRequest"; }    
    public long getTypeId() { return 13; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.context.deserialize( from_hash_map.get( "context" ) );
        this.path = ( String )from_hash_map.get( "path" );
        this.name = ( String )from_hash_map.get( "name" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.context.deserialize( from_array[0] );
        this.path = ( String )from_array[1];
        this.name = ( String )from_array[2];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        context = new Context(); context.deserialize( buf );
        path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        name = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "context", context.serialize() );
        to_hash_map.put( "path", path );
        to_hash_map.put( "name", name );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        context.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( path, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( name, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(path);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(name);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 13; }
    public Response createDefaultResponse() { return new removexattrResponse(); }


    private Context context;
    private String path;
    private String name;

}

