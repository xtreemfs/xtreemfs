package org.xtreemfs.interfaces.MRCInterface;

import org.xtreemfs.interfaces.*;
import java.util.HashMap;
import org.xtreemfs.interfaces.utils.*;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.common.buffer.ReusableBuffer;




public class xtreemfs_replica_removeRequest implements org.xtreemfs.interfaces.utils.Request
{
    public xtreemfs_replica_removeRequest() { context = new Context(); file_id = ""; osd_uuid = ""; }
    public xtreemfs_replica_removeRequest( Context context, String file_id, String osd_uuid ) { this.context = context; this.file_id = file_id; this.osd_uuid = osd_uuid; }
    public xtreemfs_replica_removeRequest( Object from_hash_map ) { context = new Context(); file_id = ""; osd_uuid = ""; this.deserialize( from_hash_map ); }
    public xtreemfs_replica_removeRequest( Object[] from_array ) { context = new Context(); file_id = ""; osd_uuid = "";this.deserialize( from_array ); }

    public Context getContext() { return context; }
    public void setContext( Context context ) { this.context = context; }
    public String getFile_id() { return file_id; }
    public void setFile_id( String file_id ) { this.file_id = file_id; }
    public String getOsd_uuid() { return osd_uuid; }
    public void setOsd_uuid( String osd_uuid ) { this.osd_uuid = osd_uuid; }

    // Serializable
    public String getTypeName() { return "org::xtreemfs::interfaces::MRCInterface::xtreemfs_replica_removeRequest"; }    
    public long getTypeId() { return 27; }

    public void deserialize( Object from_hash_map )
    {
        this.deserialize( ( HashMap<String, Object> )from_hash_map );
    }
        
    public void deserialize( HashMap<String, Object> from_hash_map )
    {
        this.context.deserialize( from_hash_map.get( "context" ) );
        this.file_id = ( String )from_hash_map.get( "file_id" );
        this.osd_uuid = ( String )from_hash_map.get( "osd_uuid" );
    }
    
    public void deserialize( Object[] from_array )
    {
        this.context.deserialize( from_array[0] );
        this.file_id = ( String )from_array[1];
        this.osd_uuid = ( String )from_array[2];        
    }

    public void deserialize( ReusableBuffer buf )
    {
        context = new Context(); context.deserialize( buf );
        file_id = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
        osd_uuid = org.xtreemfs.interfaces.utils.XDRUtils.deserializeString( buf );
    }

    public Object serialize()
    {
        HashMap<String, Object> to_hash_map = new HashMap<String, Object>();
        to_hash_map.put( "context", context.serialize() );
        to_hash_map.put( "file_id", file_id );
        to_hash_map.put( "osd_uuid", osd_uuid );
        return to_hash_map;        
    }

    public void serialize( ONCRPCBufferWriter writer ) 
    {
        context.serialize( writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( file_id, writer );
        org.xtreemfs.interfaces.utils.XDRUtils.serializeString( osd_uuid, writer );
    }
    
    public int calculateSize()
    {
        int my_size = 0;
        my_size += context.calculateSize();
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(file_id);
        my_size += org.xtreemfs.interfaces.utils.XDRUtils.stringLengthPadded(osd_uuid);
        return my_size;
    }

    // Request
    public int getOperationNumber() { return 27; }
    public Response createDefaultResponse() { return new xtreemfs_replica_removeResponse(); }


    private Context context;
    private String file_id;
    private String osd_uuid;

}

