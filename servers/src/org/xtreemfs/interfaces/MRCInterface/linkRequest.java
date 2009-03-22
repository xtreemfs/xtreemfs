package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class linkRequest implements org.xtreemfs.interfaces.utils.Request
{
    public linkRequest() { target_path = ""; link_path = ""; }
    public linkRequest( String target_path, String link_path ) { this.target_path = target_path; this.link_path = link_path; }
    public linkRequest( Object from_hash_map ) { target_path = ""; link_path = ""; this.deserialize( from_hash_map ); }
    public linkRequest( Object[] from_array ) { target_path = ""; link_path = "";this.deserialize( from_array ); }

    public String getTarget_path() { return target_path; }
    public void setTarget_path( String target_path ) { this.target_path = target_path; }
    public String getLink_path() { return link_path; }
    public void setLink_path( String link_path ) { this.link_path = link_path; }

    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::linkRequest"; }    
    public long getTypeId() { return 7; }

    public String toString()
    {
        return "linkRequest( " + "\"" + target_path + "\"" + ", " + "\"" + link_path + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.target_path = ( String )from_hash_map.get( "target_path" );
        this.link_path = ( String )from_hash_map.get( "link_path" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.target_path = ( String )from_array[0];
        this.link_path = ( String )from_array[1];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        target_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        link_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "target_path", target_path );
        to_hash_map.put( "link_path", link_path );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( target_path, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( link_path, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(target_path);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(link_path);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 7; }
    public Response createDefaultResponse() { return new linkResponse(); }


    private String target_path;
    private String link_path;

}

