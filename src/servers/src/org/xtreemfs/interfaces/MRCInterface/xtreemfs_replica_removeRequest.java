package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_replica_removeRequest implements org.xtreemfs.interfaces.utils.Request
{
    public static final int TAG = 1241;

    
    public xtreemfs_replica_removeRequest() { file_id = ""; osd_uuid = ""; }
    public xtreemfs_replica_removeRequest( String file_id, String osd_uuid ) { this.file_id = file_id; this.osd_uuid = osd_uuid; }
    public xtreemfs_replica_removeRequest( Object from_hash_map ) { file_id = ""; osd_uuid = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_replica_removeRequest( Object[] from_array ) { file_id = ""; osd_uuid = "";this.deserialize( from_array ); }

    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public String getOsd_uuid() { return osd_uuid; }
    public void setOsd_uuid( String osd_uuid ) { this.osd_uuid = osd_uuid; }

    // Object
    public String toString()
    {
        return "xtreemfs_replica_removeRequest( " + "\"" + file_id + "\"" + ", " + "\"" + osd_uuid + "\"" + " )";
    }

    // Serializable
    public int getTag() { return 1241; }
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeRequest"; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.osd_uuid = ( String )from_hash_map.get( "osd_uuid" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.file_id = ( String )from_array[0];
        this.osd_uuid = ( String )from_array[1];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        osd_uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "osd_uuid", osd_uuid );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( osd_uuid, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(osd_uuid);
        return my_size;
    }

    // Request
    public Response createDefaultResponse() { return new xtreemfs_replica_removeResponse(); }


    private String file_id;
    private String osd_uuid;    

}

