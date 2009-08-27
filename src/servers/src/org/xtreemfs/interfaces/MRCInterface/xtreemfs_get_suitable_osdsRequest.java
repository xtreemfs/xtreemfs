package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_get_suitable_osdsRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 2009082851;

    
    public xtreemfs_get_suitable_osdsRequest() { file_id = ""; }
    public xtreemfs_get_suitable_osdsRequest( String file_id ) { this.file_id = file_id; }
    public xtreemfs_get_suitable_osdsRequest( Object from_hash_map ) { file_id = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_get_suitable_osdsRequest( Object[] from_array ) { file_id = "";this.deserialize( from_array ); }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }

    // Object
    public String toString()
    {
        return "xtreemfs_get_suitable_osdsRequest( " + "\"" + file_id + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 2009082851; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_get_suitable_osdsRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_id = ( String )from_hash_map.get( "file_id" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_id = ( String )from_array[0];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_id", file_id );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_get_suitable_osdsResponse(); }


    private String file_id;    

}

