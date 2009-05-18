package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class renameRequest implements org.xtreemfs.interfaces.utils.Request
{
    public renameRequest() { source_path = ""; target_path = ""; }
    public renameRequest( String source_path, String target_path ) { this.source_path = source_path; this.target_path = target_path; }
    public renameRequest( Object from_hash_map ) { source_path = ""; target_path = ""; this.deserialize( from_hash_map ); }
    public renameRequest( Object[] from_array ) { source_path = ""; target_path = "";this.deserialize( from_array ); }

    public String getSource_path() { return source_path; }
    public void setSource_path( String source_path ) { this.source_path = source_path; }
    public String getTarget_path() { return target_path; }
    public void setTarget_path( String target_path ) { this.target_path = target_path; }

    public long getTag() { return 1214; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::renameRequest"; }

    public String toString()
    {
        return "renameRequest( " + "\"" + source_path + "\"" + ", " + "\"" + target_path + "\"" + " )";
    }


    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.source_path = ( String )from_hash_map.get( "source_path" );
        this.target_path = ( String )from_hash_map.get( "target_path" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.source_path = ( String )from_array[0];
        this.target_path = ( String )from_array[1];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        source_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        target_path = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "source_path", source_path );
        to_hash_map.put( "target_path", target_path );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( source_path, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( target_path, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(source_path);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(target_path);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 1214; }
    public Response createDefaultResponse() { return new renameResponse(); }


    private String source_path;
    private String target_path;    

}

