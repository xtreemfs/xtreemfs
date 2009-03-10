package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class symlinkRequest implements org.xtreemfs.interfaces.utils.Request
{
    public symlinkRequest() { context = new Context(); target_path = ""; link_path = ""; }
    public symlinkRequest( Context context, String target_path, String link_path ) { this.context = context; this.target_path = target_path; this.link_path = link_path; }
    public symlinkRequest( Object from_hash_map ) { context = new Context(); target_path = ""; link_path = ""; this.deserialize( from_hash_map ); }
    public symlinkRequest( Object[] from_array ) { context = new Context(); target_path = ""; link_path = "";this.deserialize( from_array ); }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getTarget_path() { return target_path; }
    public void setTarget_path( String target_path ) { this.target_path = target_path; }
    public String getLink_path() { return link_path; }
    public void setLink_path( String link_path ) { this.link_path = link_path; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::symlinkRequest"; }    
    public long getTypeId() { return 20; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.context.deserialize( from_hash_map.get( "context" ) );
        this.target_path = ( String )from_hash_map.get( "target_path" );
        this.link_path = ( String )from_hash_map.get( "link_path" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.context.deserialize( from_array[0] );
        this.target_path = ( String )from_array[1];
        this.link_path = ( String )from_array[2];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        context = new Context(); context.deserialize( buf );
        target_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        link_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "context", context.serialize() );
        to_hash_map.put( "target_path", target_path );
        to_hash_map.put( "link_path", link_path );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        context.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( target_path, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( link_path, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(target_path);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(link_path);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 20; }
    public Response createDefaultResponse() { return new symlinkResponse(); }


    private Context context;
    private String target_path;
    private String link_path;

}

